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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.onlab.stc.Coordinator.print;

/**
 * Main program for executing system test coordinator.
 */
public final class Main {

    private enum Command {
        LIST, RUN, RUN_FROM, RUN_TO
    }

    private final String[] args;
    private final Command command;
    private final String scenarioFile;

    private Scenario scenario;
    private Coordinator coordinator;
    private Listener delegate = new Listener();

    // Public construction forbidden
    private Main(String[] args) {
        this.args = args;
        this.scenarioFile = args[0];
        this.command = Command.valueOf("RUN");
    }

    // usage: stc [<command>] [<scenario-file>]
    // --list
    // [--run]
    // --run-from <step>,...
    // --run-to <step>,...

    /**
     * Main entry point for coordinating test scenario execution.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        Main main = new Main(args);
        main.run();
    }

    private void run() {
        try {
            // Load scenario
            scenario = Scenario.loadScenario(new FileInputStream(scenarioFile));

            // Elaborate scenario
            Compiler compiler = new Compiler(scenario);
            compiler.compile();

            // Execute process flow
            coordinator = new Coordinator(scenario, compiler.processFlow(),
                                          compiler.logDir());
            coordinator.addListener(delegate);
            processCommand();

        } catch (FileNotFoundException e) {
            print("Unable to find scenario file %s", scenarioFile);
        }
    }

    private void processCommand() {
        switch (command) {
            case RUN:
                processRun();
            default:
                print("Unsupported command");
        }
    }

    private void processRun() {
        try {
            coordinator.start();
            int exitCode = coordinator.waitFor();
            pause(100); // allow stdout to flush
            System.exit(exitCode);
        } catch (InterruptedException e) {
            print("Unable to execute scenario %s", scenarioFile);
        }
    }

    private void pause(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            print("Interrupted!");
        }
    }


    /**
     * Internal delegate to monitor the process execution.
     */
    private class Listener implements StepProcessListener {

        @Override
        public void onStart(Step step) {
            print("%s  %s started", now(), step.name());
        }

        @Override
        public void onCompletion(Step step, int exitCode) {
            print("%s  %s %s", now(), step.name(), exitCode == 0 ? "completed" : "failed");
        }

        @Override
        public void onOutput(Step step, String line) {
        }

    }

    private String now() {
        return new SimpleDateFormat("YYYY-MM-dd HH:mm:ss").format(new Date());
    }

}
