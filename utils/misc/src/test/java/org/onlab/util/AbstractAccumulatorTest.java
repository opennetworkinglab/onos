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
package org.onlab.util;

import org.junit.Test;
import org.junit.Ignore;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.onlab.junit.TestTools.assertAfter;

/**
 * Tests the operation of the accumulator.
 */
public class AbstractAccumulatorTest {


    private final ManuallyAdvancingTimer timer = new ManuallyAdvancingTimer();


    @Test
    public void basics() throws Exception {
        TestAccumulator accumulator = new TestAccumulator();
        assertEquals("incorrect timer", timer, accumulator.timer());
        assertEquals("incorrect max events", 5, accumulator.maxItems());
        assertEquals("incorrect max ms", 100, accumulator.maxBatchMillis());
        assertEquals("incorrect idle ms", 70, accumulator.maxIdleMillis());
    }

    @Test
    public void eventTrigger() {
        TestAccumulator accumulator = new TestAccumulator();
        accumulator.add(new TestItem("a"));
        accumulator.add(new TestItem("b"));
        accumulator.add(new TestItem("c"));
        accumulator.add(new TestItem("d"));
        assertTrue("should not have fired yet", accumulator.batch.isEmpty());
        accumulator.add(new TestItem("e"));
        timer.advanceTimeMillis(20, 10);
        assertFalse("should have fired", accumulator.batch.isEmpty());
        assertEquals("incorrect batch", "abcde", accumulator.batch);
    }

    @Ignore("Ignored when running CircleCI")
    @Test
    public void timeTrigger() {
        TestAccumulator accumulator = new TestAccumulator();
        accumulator.add(new TestItem("a"));
        timer.advanceTimeMillis(30, 1);
        assertTrue("should not have fired yet", accumulator.batch.isEmpty());
        accumulator.add(new TestItem("b"));
        timer.advanceTimeMillis(30, 1);
        assertTrue("should not have fired yet", accumulator.batch.isEmpty());
        accumulator.add(new TestItem("c"));
        timer.advanceTimeMillis(30, 1);
        assertTrue("should not have fired yet", accumulator.batch.isEmpty());
        accumulator.add(new TestItem("d"));
        timer.advanceTimeMillis(10, 10);
        assertFalse("should have fired", accumulator.batch.isEmpty());
        assertEquals("incorrect batch", "abcd", accumulator.batch);
    }

    @Test
    public void idleTrigger() {
        TestAccumulator accumulator = new TestAccumulator();
        accumulator.add(new TestItem("a"));
        assertTrue("should not have fired yet", accumulator.batch.isEmpty());
        accumulator.add(new TestItem("b"));
        timer.advanceTimeMillis(70, 10);
        assertFalse("should have fired", accumulator.batch.isEmpty());
        assertEquals("incorrect batch", "ab", accumulator.batch);
    }

    @Ignore("Ignored when running CircleCI")
    @Test
    public void readyIdleTrigger() {
        TestAccumulator accumulator = new TestAccumulator();
        accumulator.ready = false;
        accumulator.add(new TestItem("a"));
        assertTrue("should not have fired yet", accumulator.batch.isEmpty());
        accumulator.add(new TestItem("b"));
        timer.advanceTimeMillis(80, 1);
        assertTrue("should not have fired yet", accumulator.batch.isEmpty());
        accumulator.ready = true;
        timer.advanceTimeMillis(80, 10);
        assertFalse("should have fired", accumulator.batch.isEmpty());
        assertEquals("incorrect batch", "ab", accumulator.batch);
    }

    @Test
    public void readyLongTrigger() {
        TestAccumulator accumulator = new TestAccumulator();
        accumulator.ready = false;
        timer.advanceTimeMillis(120, 1);
        assertTrue("should not have fired yet", accumulator.batch.isEmpty());
        accumulator.add(new TestItem("a"));
        assertTrue("should not have fired yet", accumulator.batch.isEmpty());
        accumulator.ready = true;
        timer.advanceTimeMillis(120, 10);
        assertFalse("should have fired", accumulator.batch.isEmpty());
        assertEquals("incorrect batch", "a", accumulator.batch);
    }

    @Test
    public void readyMaxTrigger() {
        TestAccumulator accumulator = new TestAccumulator();
        accumulator.ready = false;
        accumulator.add(new TestItem("a"));
        accumulator.add(new TestItem("b"));
        accumulator.add(new TestItem("c"));
        accumulator.add(new TestItem("d"));
        accumulator.add(new TestItem("e"));
        accumulator.add(new TestItem("f"));
        assertTrue("should not have fired yet", accumulator.batch.isEmpty());
        accumulator.ready = true;
        accumulator.add(new TestItem("g"));
        timer.advanceTimeMillis(10, 10);
        assertFalse("should have fired", accumulator.batch.isEmpty());
        assertEquals("incorrect batch", "abcdefg", accumulator.batch);
    }

    @Test
    public void stormTest() {
        TestAccumulator accumulator = new TestAccumulator();
        IntStream.range(0, 1000).forEach(i -> accumulator.add(new TestItem("#" + i)));
        timer.advanceTimeMillis(1);
        assertAfter(100, () -> assertEquals("wrong item count", 1000, accumulator.itemCount));
        assertEquals("wrong batch count", 200, accumulator.batchCount);
    }

    private class TestItem {
        private final String s;

        public TestItem(String s) {
            this.s = s;
        }
    }

    private class TestAccumulator extends AbstractAccumulator<TestItem> {

        String batch = "";
        boolean ready = true;
        int batchCount = 0;
        int itemCount = 0;

        protected TestAccumulator() {
            super(timer, 5, 100, 70);
        }

        @Override
        public void processItems(List<TestItem> items) {
            batchCount++;
            itemCount += items.size();
            for (TestItem item : items) {
                batch += item.s;
            }
        }

        @Override
        public boolean isReady() {
            return ready;
        }
    }
}
