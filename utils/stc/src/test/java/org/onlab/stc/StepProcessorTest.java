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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onlab.util.Tools;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.onlab.stc.Coordinator.Status.SUCCEEDED;

/**
 * Test of the step processor.
 */
public class StepProcessorTest {

    static final File DIR = new File("/tmp/stc/foo");

    private final Listener delegate = new Listener();

    @BeforeClass
    public static void setUpClass() {
        StepProcessor.launcher = "echo";
        DIR.mkdirs();
    }

    @AfterClass
    public static void tearDownClass() throws IOException {
        Tools.removeDirectory(DIR.getPath());
    }

    @Test
    public void basics() {
        Step step = new Step("foo", "ls /tmp", null, null, null);
        StepProcessor processor = new StepProcessor(step, DIR, delegate);
        processor.run();
        assertTrue("should be started", delegate.started);
        assertTrue("should have output", delegate.output);
        assertTrue("should be stopped", delegate.stopped);
        assertEquals("incorrect status", SUCCEEDED, delegate.status);
    }

    private class Listener implements StepProcessListener {

        private Coordinator.Status status;
        private boolean started, stopped, output;

        @Override
        public void onStart(Step step) {
            started = true;
        }

        @Override
        public void onCompletion(Step step, Coordinator.Status status) {
            stopped = true;
            this.status = status;
        }

        @Override
        public void onOutput(Step step, String line) {
            output = true;
        }
    }

}