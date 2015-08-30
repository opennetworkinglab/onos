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
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.util.log.Logger;
import org.onlab.stc.Coordinator.Status;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static java.lang.System.currentTimeMillis;
import static org.onlab.stc.Coordinator.Status.*;
import static org.onlab.stc.Coordinator.print;

/**
 * Main program for executing system test coordinator.
 */
public final class Main {

    private static final String NONE = "\u001B[0m";
    private static final String GRAY = "\u001B[30;1m";
    private static final String RED = "\u001B[31;1m";
    private static final String GREEN = "\u001B[32;1m";
    private static final String BLUE = "\u001B[36m";

    private static final String SUCCESS_SUMMARY = "%sPassed! %d steps succeeded%s";
    private static final String FAILURE_SUMMARY = "%sFailed! %d steps succeeded; %d steps failed; %d steps skipped%s";

    private enum Command {
        LIST, RUN, RUN_RANGE, HELP
    }

    private final String scenarioFile;

    private Command command = Command.HELP;
    private String runFromPatterns = "";
    private String runToPatterns = "";

    private Coordinator coordinator;
    private Monitor monitor;
    private Listener delegate = new Listener();

    private static boolean useColor = Objects.equals("true", System.getenv("stcColor"));

    // usage: stc [<scenario-file>] [run]
    // usage: stc [<scenario-file>] run [from <from-patterns>] [to <to-patterns>]]
    // usage: stc [<scenario-file>] list

    // Public construction forbidden
    private Main(String[] args) {
        this.scenarioFile = args[0];

        if (args.length <= 1 || args.length == 2 && args[1].equals("run")) {
            command = Command.RUN;
        } else if (args.length == 2 && args[1].equals("list")) {
            command = Command.LIST;
        } else if (args.length >= 4 && args[1].equals("run")) {
            int i = 2;
            if (args[i].equals("from")) {
                command = Command.RUN_RANGE;
                runFromPatterns = args[i + 1];
                i += 2;
            }

            if (args.length >= i + 2 && args[i].equals("to")) {
                command = Command.RUN_RANGE;
                runToPatterns = args[i + 1];
            }
        }
    }

    /**
     * Main entry point for coordinating test scenario execution.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        Main main = new Main(args);
        main.run();
    }

    // Runs the scenario processing
    private void run() {
        try {
            // Load scenario
            Scenario scenario = Scenario.loadScenario(new FileInputStream(scenarioFile));

            // Elaborate scenario
            Compiler compiler = new Compiler(scenario);
            compiler.compile();

            // Setup the process flow coordinator
            coordinator = new Coordinator(scenario, compiler.processFlow(),
                                          compiler.logDir());
            coordinator.addListener(delegate);

            // Prepare the GUI monitor
            monitor = new Monitor(coordinator, compiler);
            startMonitorServer(monitor);

            // Execute process flow
            processCommand();

        } catch (FileNotFoundException e) {
            print("Unable to find scenario file %s", scenarioFile);
        }
    }

    // Initiates a web-server for the monitor GUI.
    private static void startMonitorServer(Monitor monitor) {
        org.eclipse.jetty.util.log.Log.setLog(new NullLogger());
        Server server = new Server(9999);
        ServletHandler handler = new ServletHandler();
        server.setHandler(handler);
        MonitorWebSocketServlet.setMonitor(monitor);
        handler.addServletWithMapping(MonitorWebSocketServlet.class, "/*");
        try {
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Processes the appropriate command
    private void processCommand() {
        switch (command) {
            case RUN:
                processRun();
                break;
            case LIST:
                processList();
                break;
            case RUN_RANGE:
                processRunRange();
                break;
            default:
                print("Unsupported command %s", command);
        }
    }

    // Processes the scenario 'run' command.
    private void processRun() {
        coordinator.reset();
        runCoordinator();
    }

    // Processes the scenario 'run' command for range of steps.
    private void processRunRange() {
        coordinator.reset(list(runFromPatterns), list(runToPatterns));
        runCoordinator();
    }

    // Processes the scenario 'list' command.
    private void processList() {
        coordinator.getRecords()
                .forEach(event -> logStatus(event.time(), event.name(), event.status(), event.command()));
        System.exit(0);
    }

    // Runs the coordinator and waits for it to finish.
    private void runCoordinator() {
        try {
            coordinator.start();
            int exitCode = coordinator.waitFor();
            pause(100); // allow stdout to flush
            printSummary(exitCode);
            System.exit(exitCode);
        } catch (InterruptedException e) {
            print("Unable to execute scenario %s", scenarioFile);
        }
    }

    private void printSummary(int exitCode) {
        Set<Step> steps = coordinator.getSteps();
        int count = steps.size();
        if (exitCode == 0) {
            print(SUCCESS_SUMMARY, color(SUCCEEDED), count, color(null));
        } else {
            long success = steps.stream().filter(s -> coordinator.getStatus(s) == SUCCEEDED).count();
            long failed = steps.stream().filter(s -> coordinator.getStatus(s) == FAILED).count();
            long skipped = steps.stream().filter(s -> coordinator.getStatus(s) == SKIPPED).count();
            print(FAILURE_SUMMARY, color(FAILED), success, failed, skipped, color(null));
        }
    }

    /**
     * Internal delegate to monitor the process execution.
     */
    private static class Listener implements StepProcessListener {
        @Override
        public void onStart(Step step, String command) {
            logStatus(currentTimeMillis(), step.name(), IN_PROGRESS, command);
        }

        @Override
        public void onCompletion(Step step, Status status) {
            logStatus(currentTimeMillis(), step.name(), status, null);
        }

        @Override
        public void onOutput(Step step, String line) {
        }
    }

    // Logs the step status.
    private static void logStatus(long time, String name, Status status, String cmd) {
        if (cmd != null) {
            print("%s  %s%s %s%s -- %s", time(time), color(status), name, action(status), color(null), cmd);
        } else {
            print("%s  %s%s %s%s", time(time), color(status), name, action(status), color(null));
        }
    }

    // Produces a description of event using the specified step status.
    private static String action(Status status) {
        return status == IN_PROGRESS ? "started" :
                (status == SUCCEEDED ? "completed" :
                        (status == FAILED ? "failed" :
                                (status == SKIPPED ? "skipped" : "waiting")));
    }

    // Produces an ANSI escape code for color using the specified step status.
    private static String color(Status status) {
        if (!useColor) {
            return "";
        }
        return status == null ? NONE :
                (status == IN_PROGRESS ? BLUE :
                        (status == SUCCEEDED ? GREEN :
                                (status == FAILED ? RED : GRAY)));
    }

    // Produces a list from the specified comma-separated string.
    private static List<String> list(String patterns) {
        return ImmutableList.copyOf(patterns.split(","));
    }

    // Produces a formatted time stamp.
    private static String time(long time) {
        return new SimpleDateFormat("YYYY-MM-dd HH:mm:ss").format(new Date(time));
    }

    // Pauses for the specified number of millis.
    private static void pause(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            print("Interrupted!");
        }
    }

    // Logger to quiet Jetty down
    private static class NullLogger implements Logger {
        @Override
        public String getName() {
            return "quiet";
        }

        @Override
        public void warn(String msg, Object... args) {
        }

        @Override
        public void warn(Throwable thrown) {
        }

        @Override
        public void warn(String msg, Throwable thrown) {
        }

        @Override
        public void info(String msg, Object... args) {
        }

        @Override
        public void info(Throwable thrown) {
        }

        @Override
        public void info(String msg, Throwable thrown) {
        }

        @Override
        public boolean isDebugEnabled() {
            return false;
        }

        @Override
        public void setDebugEnabled(boolean enabled) {
        }

        @Override
        public void debug(String msg, Object... args) {
        }

        @Override
        public void debug(Throwable thrown) {
        }

        @Override
        public void debug(String msg, Throwable thrown) {
        }

        @Override
        public Logger getLogger(String name) {
            return this;
        }

        @Override
        public void ignore(Throwable ignored) {
        }
    }
}
