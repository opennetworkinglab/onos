/*
 * Copyright 2015-present Open Networking Laboratory
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
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.io.File;
import static com.google.common.base.Preconditions.checkState;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.onlab.stc.Coordinator.Status.SUCCEEDED;

/**
 * Test of the step processor.
 */
public class StepProcessorTest {

    @ClassRule
    public static TemporaryFolder testFolder = new TemporaryFolder();

    private static File dir;
    private final Listener delegate = new Listener();

    @BeforeClass
    public static void setUpClass() {
        dir = testFolder.getRoot();
        StepProcessor.launcher = "echo";
        checkState(dir.exists() || dir.mkdirs(), "Unable to create directory");
    }

    @Test
    public void basics() {
        Step step = new Step("foo", "ls " + dir.getAbsolutePath(), null, null, null, 0);
        StepProcessor processor = new StepProcessor(step, dir, delegate, step.command());
        processor.run();
        assertTrue("should be started", delegate.started);
        assertTrue("should be stopped", delegate.stopped);
        assertEquals("incorrect status", SUCCEEDED, delegate.status);
        assertTrue("should have output", delegate.output);
    }

    private class Listener implements StepProcessListener {

        private Coordinator.Status status;
        private boolean started, stopped, output;

        @Override
        public void onStart(Step step, String command) {
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