/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.event;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.onlab.junit.TestTools.delay;
import static org.onosproject.event.TestEvent.Type.FOO;

import java.util.List;
import java.util.Timer;

import org.junit.Ignore;
import org.junit.Test;

/**
 * Tests the operation of the accumulator.
 */
public class AbstractEventAccumulatorTest {

    private final Timer timer = new Timer();

    @Test
    public void basics() throws Exception {
        TestAccumulator accumulator = new TestAccumulator();
        assertEquals("incorrect timer", timer, accumulator.timer());
        assertEquals("incorrect max events", 5, accumulator.maxEvents());
        assertEquals("incorrect max ms", 100, accumulator.maxBatchMillis());
        assertEquals("incorrect idle ms", 50, accumulator.maxIdleMillis());
    }

    @Test
    public void eventTrigger() {
        TestAccumulator accumulator = new TestAccumulator();
        accumulator.add(new TestEvent(FOO, "a"));
        accumulator.add(new TestEvent(FOO, "b"));
        accumulator.add(new TestEvent(FOO, "c"));
        accumulator.add(new TestEvent(FOO, "d"));
        assertTrue("should not have fired yet", accumulator.batch.isEmpty());
        accumulator.add(new TestEvent(FOO, "e"));
        delay(10);
        assertFalse("should have fired", accumulator.batch.isEmpty());
        assertEquals("incorrect batch", "abcde", accumulator.batch);
    }

    @Ignore("FIXME: timing sensitive test failing randomly.")
    @Test
    public void timeTrigger() {
        TestAccumulator accumulator = new TestAccumulator();
        accumulator.add(new TestEvent(FOO, "a"));
        delay(30);
        assertTrue("should not have fired yet", accumulator.batch.isEmpty());
        accumulator.add(new TestEvent(FOO, "b"));
        delay(30);
        assertTrue("should not have fired yet", accumulator.batch.isEmpty());
        accumulator.add(new TestEvent(FOO, "c"));
        delay(30);
        assertTrue("should not have fired yet", accumulator.batch.isEmpty());
        accumulator.add(new TestEvent(FOO, "d"));
        delay(30);
        assertFalse("should have fired", accumulator.batch.isEmpty());
        assertEquals("incorrect batch", "abcd", accumulator.batch);
    }

    @Test
    public void idleTrigger() {
        TestAccumulator accumulator = new TestAccumulator();
        accumulator.add(new TestEvent(FOO, "a"));
        assertTrue("should not have fired yet", accumulator.batch.isEmpty());
        accumulator.add(new TestEvent(FOO, "b"));
        delay(80);
        assertFalse("should have fired", accumulator.batch.isEmpty());
        assertEquals("incorrect batch", "ab", accumulator.batch);
    }

    private class TestAccumulator extends AbstractEventAccumulator {

        String batch = "";

        protected TestAccumulator() {
            super(timer, 5, 100, 50);
        }

        @Override
        public void processEvents(List<Event> events) {
            for (Event event : events) {
                batch += event.subject();
            }
        }
    }
}
