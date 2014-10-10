package org.onlab.onos.event;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.onlab.junit.TestTools.delay;
import static org.onlab.onos.event.TestEvent.Type.FOO;

import java.util.List;
import java.util.Timer;

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
