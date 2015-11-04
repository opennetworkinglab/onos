/*
 * Copyright 2015 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onlab.stc;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.onlab.stc.Compiler.PROP_END;
import static org.onlab.stc.Compiler.PROP_START;
import static org.onlab.stc.Coordinator.Directive.*;
import static org.onlab.stc.Coordinator.Status.*;

/**
 * Coordinates execution of a scenario process flow.
 */
public class Coordinator {

    private static final int MAX_THREADS = 64;

    private final ExecutorService executor = newFixedThreadPool(MAX_THREADS);

    private final ProcessFlow processFlow;

    private final StepProcessListener delegate;
    private final CountDownLatch latch;
    private final ScenarioStore store;

    private static final Pattern PROP_ERE = Pattern.compile("^@stc ([a-zA-Z0-9_.]+)=(.*$)");
    private final Map<String, String> properties = Maps.newConcurrentMap();

    private final Set<StepProcessListener> listeners = Sets.newConcurrentHashSet();
    private File logDir;

    /**
     * Represents action to be taken on a test step.
     */
    public enum Directive {
        NOOP, RUN, SKIP
    }

    /**
     * Represents processor state.
     */
    public enum Status {
        WAITING, IN_PROGRESS, SUCCEEDED, FAILED, SKIPPED
    }

    /**
     * Creates a process flow coordinator.
     *
     * @param scenario    test scenario to coordinate
     * @param processFlow process flow to coordinate
     * @param logDir      scenario log directory
     */
    public Coordinator(Scenario scenario, ProcessFlow processFlow, File logDir) {
        this.processFlow = processFlow;
        this.logDir = logDir;
        this.store = new ScenarioStore(processFlow, logDir, scenario.name());
        this.delegate = new Delegate();
        this.latch = new CountDownLatch(1);
    }

    /**
     * Resets any previously accrued status and events.
     */
    public void reset() {
        store.reset();
    }

    /**
     * Resets all previously accrued status and events for steps that lie
     * in the range between the steps or groups whose names match the specified
     * patterns.
     *
     * @param runFromPatterns list of starting step patterns
     * @param runToPatterns   list of ending step patterns
     */
    public void reset(List<String> runFromPatterns, List<String> runToPatterns) {
        List<Step> fromSteps = matchSteps(runFromPatterns);
        List<Step> toSteps = matchSteps(runToPatterns);

        // FIXME: implement this
    }

    /**
     * Returns number of milliseconds it took to execute.
     *
     * @return number of millis elapsed during the run
     */
    public long duration() {
        return store.endTime() - store.startTime();
    }

    /**
     * Returns a list of steps that match the specified list of patterns.
     *
     * @param runToPatterns list of patterns
     * @return list of steps with matching names
     */
    private List<Step> matchSteps(List<String> runToPatterns) {
        ImmutableList.Builder<Step> builder = ImmutableList.builder();
        store.getSteps().forEach(step -> {
            runToPatterns.forEach(p -> {
                if (step.name().matches(p)) {
                    builder.add(step);
                }
            });
        });
        return builder.build();
    }

    /**
     * Starts execution of the process flow graph.
     */
    public void start() {
        executeRoots(null);
    }

    /**
     * Waits for completion of the entire process flow.
     *
     * @return exit code to use
     * @throws InterruptedException if interrupted while waiting for completion
     */
    public int waitFor() throws InterruptedException {
        while (!store.isComplete()) {
            latch.await(1, TimeUnit.SECONDS);
        }
        return store.hasFailures() ? 1 : 0;
    }

    /**
     * Returns set of all test steps.
     *
     * @return set of steps
     */
    public Set<Step> getSteps() {
        return store.getSteps();
    }

    /**
     * Returns a chronological list of step or group records.
     *
     * @return list of events
     */
    List<StepEvent> getRecords() {
        return store.getEvents();
    }

    /**
     * Returns the status record of the specified test step.
     *
     * @param step test step or group
     * @return step status record
     */
    public Status getStatus(Step step) {
        return store.getStatus(step);
    }

    /**
     * Adds the specified listener.
     *
     * @param listener step process listener
     */
    public void addListener(StepProcessListener listener) {
        listeners.add(checkNotNull(listener, "Listener cannot be null"));
    }

    /**
     * Removes the specified listener.
     *
     * @param listener step process listener
     */
    public void removeListener(StepProcessListener listener) {
        listeners.remove(checkNotNull(listener, "Listener cannot be null"));
    }

    /**
     * Executes the set of roots in the scope of the specified group or globally
     * if no group is given.
     *
     * @param group optional group
     */
    private void executeRoots(Group group) {
        // FIXME: add ability to skip past completed steps
        Set<Step> steps =
                group != null ? group.children() : processFlow.getVertexes();
        steps.forEach(step -> {
            if (processFlow.getEdgesFrom(step).isEmpty() && step.group() == group) {
                execute(step);
            }
        });
    }

    /**
     * Executes the specified step.
     *
     * @param step step to execute
     */
    private synchronized void execute(Step step) {
        Directive directive = nextAction(step);
        if (directive == RUN) {
            store.markStarted(step);
            if (step instanceof Group) {
                Group group = (Group) step;
                delegate.onStart(group, null);
                executeRoots(group);
            } else {
                executor.execute(new StepProcessor(step, logDir, delegate,
                                                   substitute(step.command())));
            }
        } else if (directive == SKIP) {
            skipStep(step);
        }
    }

    /**
     * Recursively skips the specified step or group and any steps/groups within.
     *
     * @param step step or group
     */
    private void skipStep(Step step) {
        if (step instanceof Group) {
            Group group = (Group) step;
            store.markComplete(step, SKIPPED);
            group.children().forEach(this::skipStep);
        }
        delegate.onCompletion(step, SKIPPED);

    }

    /**
     * Determines the state of the specified step.
     *
     * @param step test step
     * @return state of the step process
     */
    private Directive nextAction(Step step) {
        Status status = store.getStatus(step);
        if (status != WAITING) {
            return NOOP;
        }

        for (Dependency dependency : processFlow.getEdgesFrom(step)) {
            Status depStatus = store.getStatus(dependency.dst());
            if (depStatus == WAITING || depStatus == IN_PROGRESS) {
                return NOOP;
            } else if (((depStatus == FAILED || depStatus == SKIPPED) && !dependency.isSoft()) ||
                    (step.group() != null && store.getStatus(step.group()) == SKIPPED)) {
                return SKIP;
            }
        }
        return RUN;
    }

    /**
     * Executes the successors to the specified step.
     *
     * @param step step whose successors are to be executed
     */
    private void executeSucessors(Step step) {
        processFlow.getEdgesTo(step).forEach(dependency -> execute(dependency.src()));
        completeParentIfNeeded(step.group());
    }

    /**
     * Checks whether the specified parent group, if any, should be marked
     * as complete.
     *
     * @param group parent group that should be checked
     */
    private synchronized void completeParentIfNeeded(Group group) {
        if (group != null && getStatus(group) == IN_PROGRESS) {
            boolean done = true;
            boolean failed = false;
            for (Step child : group.children()) {
                Status status = store.getStatus(child);
                done = done && (status == SUCCEEDED || status == FAILED || status == SKIPPED);
                failed = failed || status == FAILED;
            }
            if (done) {
                delegate.onCompletion(group, failed ? FAILED : SUCCEEDED);
            }
        }
    }

    /**
     * Expands the var references with values from the properties map.
     *
     * @param string string to perform substitutions on
     */
    private String substitute(String string) {
        StringBuilder sb = new StringBuilder();
        int start, end, last = 0;
        while ((start = string.indexOf(PROP_START, last)) >= 0) {
            end = string.indexOf(PROP_END, start + PROP_START.length());
            checkArgument(end > start, "Malformed property in %s", string);
            sb.append(string.substring(last, start));
            String prop = string.substring(start + PROP_START.length(), end);
            String value = properties.get(prop);
            sb.append(value != null ? value : "");
            last = end + 1;
        }
        sb.append(string.substring(last));
        return sb.toString().replace('\n', ' ').replace('\r', ' ');
    }

    /**
     * Scrapes the line of output for any variables to be captured and posted
     * in the properties for later use.
     *
     * @param line line of output to scrape for property exports
     */
    private void scrapeForVariables(String line) {
        Matcher matcher = PROP_ERE.matcher(line);
        if (matcher.matches()) {
            String prop = matcher.group(1);
            String value = matcher.group(2);
            properties.put(prop, value);
        }
    }


    /**
     * Prints formatted output.
     *
     * @param format printf format string
     * @param args   arguments to be printed
     */
    public static void print(String format, Object... args) {
        System.out.println(String.format(format, args));
    }

    /**
     * Internal delegate to monitor the process execution.
     */
    private class Delegate implements StepProcessListener {
        @Override
        public void onStart(Step step, String command) {
            listeners.forEach(listener -> listener.onStart(step, command));
        }

        @Override
        public void onCompletion(Step step, Status status) {
            store.markComplete(step, status);
            listeners.forEach(listener -> listener.onCompletion(step, status));
            executeSucessors(step);
            if (store.isComplete()) {
                latch.countDown();
            }
        }

        @Override
        public void onOutput(Step step, String line) {
            scrapeForVariables(line);
            listeners.forEach(listener -> listener.onOutput(step, line));
        }
    }

}
