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

import org.junit.BeforeClass;
import org.junit.Test;
import org.onlab.util.Tools;

import java.io.IOException;

import static org.onlab.stc.CompilerTest.getStream;
import static org.onlab.stc.Coordinator.print;
import static org.onlab.stc.Scenario.loadScenario;

/**
 * Test of the test coordinator.
 */
public class CoordinatorTest {

    private Coordinator coordinator;
    private StepProcessListener listener = new Listener();

    @BeforeClass
    public static void setUpClass() throws IOException {
        CompilerTest.setUpClass();
        Tools.removeDirectory(StepProcessorTest.DIR);

        StepProcessor.launcher = "true ";
    }

    @Test
    public void simple() throws IOException, InterruptedException {
        executeTest("simple-scenario.xml");
    }

    @Test
    public void complex() throws IOException, InterruptedException {
        executeTest("scenario.xml");
    }

    private void executeTest(String name) throws IOException, InterruptedException {
        Scenario scenario = loadScenario(getStream(name));
        Compiler compiler = new Compiler(scenario);
        compiler.compile();
        Tools.removeDirectory(compiler.logDir());
        coordinator = new Coordinator(scenario, compiler.processFlow(), compiler.logDir());
        coordinator.addListener(listener);
        coordinator.reset();
        coordinator.start();
        coordinator.waitFor();
        coordinator.removeListener(listener);
    }

    private class Listener implements StepProcessListener {
        @Override
        public void onStart(Step step, String command) {
            print("> %s: started; %s", step.name(), command);
        }

        @Override
        public void onCompletion(Step step, Coordinator.Status status) {
            print("< %s: %s", step.name(), status == Coordinator.Status.SUCCEEDED ? "completed" : "failed");
        }

        @Override
        public void onOutput(Step step, String line) {
            print("  %s: %s", step.name(), line);
        }
    }
}