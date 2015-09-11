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
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.onlab.stc.Coordinator.Status;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.stc.Coordinator.Status.*;
import static org.onlab.stc.Coordinator.print;

/**
 * Maintains state of scenario execution.
 */
class ScenarioStore {

    private final ProcessFlow processFlow;
    private final File storeFile;
    private final File logDir;

    private final List<StepEvent> events = Lists.newArrayList();
    private final Map<String, Status> statusMap = Maps.newConcurrentMap();

    private long startTime = Long.MAX_VALUE;
    private long endTime = Long.MIN_VALUE;

    /**
     * Creates a new scenario store for the specified process flow.
     *
     * @param processFlow scenario process flow
     * @param logDir      scenario log directory
     * @param name        scenario name
     */
    ScenarioStore(ProcessFlow processFlow, File logDir, String name) {
        this.processFlow = processFlow;
        this.logDir = logDir;
        this.storeFile = new File(logDir, name + ".stc");
        load();
    }

    /**
     * Resets status of all steps to waiting and clears all events.
     */
    void reset() {
        events.clear();
        statusMap.clear();
        processFlow.getVertexes().forEach(step -> statusMap.put(step.name(), WAITING));
        try {
            removeLogs();
            PropertiesConfiguration cfg = new PropertiesConfiguration(storeFile);
            cfg.clear();
            cfg.save();
            startTime = Long.MAX_VALUE;
            endTime = Long.MIN_VALUE;
        } catch (ConfigurationException e) {
            print("Unable to store file %s", storeFile);
        }

    }

    /**
     * Returns set of all test steps.
     *
     * @return set of steps
     */
    Set<Step> getSteps() {
        return processFlow.getVertexes();
    }

    /**
     * Returns a chronological list of step or group records.
     *
     * @return list of events
     */
    synchronized List<StepEvent> getEvents() {
        return ImmutableList.copyOf(events);
    }

    /**
     * Returns the status record of the specified test step.
     *
     * @param step test step or group
     * @return step status record
     */
    Status getStatus(Step step) {
        return checkNotNull(statusMap.get(step.name()), "Step %s not found", step.name());
    }

    /**
     * Marks the specified test step as being in progress.
     *
     * @param step test step or group
     */
    synchronized void markStarted(Step step) {
        add(new StepEvent(step.name(), IN_PROGRESS, step.command()));
        save();
    }

    /**
     * Marks the specified test step as being complete.
     *
     * @param step   test step or group
     * @param status new step status
     */
    synchronized void markComplete(Step step, Status status) {
        add(new StepEvent(step.name(), status, null));
        save();
    }

    /**
     * Returns true if all steps in the store have been marked as completed
     * regardless of the completion status.
     *
     * @return true if all steps completed one way or another
     */
    synchronized boolean isComplete() {
        return !statusMap.values().stream().anyMatch(s -> s == WAITING || s == IN_PROGRESS);
    }

    /**
     * Indicates whether there are any failures.
     *
     * @return true if there are failed steps
     */
    boolean hasFailures() {
        for (Status status : statusMap.values()) {
            if (status == FAILED) {
                return true;
            }
        }
        return false;
    }

    /**
     * Registers a new step record.
     *
     * @param event step event
     */
    private synchronized void add(StepEvent event) {
        events.add(event);
        statusMap.put(event.name(), event.status());
        startTime = Math.min(startTime, event.time());
        endTime = Math.max(endTime, event.time());
    }

    /**
     * Loads the states from disk.
     */
    private void load() {
        try {
            PropertiesConfiguration cfg = new PropertiesConfiguration(storeFile);
            cfg.getKeys().forEachRemaining(prop -> add(StepEvent.fromString(cfg.getString(prop))));
            cfg.save();
        } catch (ConfigurationException e) {
            print("Unable to load file %s", storeFile);
        }
    }

    /**
     * Saves the states to disk.
     */
    private void save() {
        try {
            PropertiesConfiguration cfg = new PropertiesConfiguration(storeFile);
            events.forEach(event -> cfg.setProperty("T" + event.time(), event.toString()));
            cfg.save();
        } catch (ConfigurationException e) {
            print("Unable to store file %s", storeFile);
        }
    }

    /**
     * Removes all scenario log files.
     */
    private void removeLogs() {
        File[] logFiles = logDir.listFiles();
        if (logFiles != null && logFiles.length > 0) {
            for (File file : logFiles) {
                if (!file.delete()) {
                    print("Unable to delete log file %s", file);
                }
            }
        }
    }

    /**
     * Returns the scenario run start time.
     *
     * @return start time in mills since start of epoch
     */
    public long startTime() {
        return startTime;
    }

    /**
     * Returns the scenario run end time or current time if the scenario
     * is still running.
     *
     * @return end time (or current time) in mills since start of epoch
     */
    public long endTime() {
        return endTime > 0 ? endTime : System.currentTimeMillis();
    }
}
