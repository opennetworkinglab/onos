package org.onlab.onos.event;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.onlab.onos.event.TestEvent.Type.FOO;

/**
 * Tests of the base event abstraction.
 */
public class AbstractEventTest {

    /**
     * Validates the base attributes of an event.
     *
     * @param event   event to validate
     * @param type    event type
     * @param subject event subject
     * @param time    event time
     * @param <T>     type of event
     * @param <S>     type of subject
     */
    protected static <T extends Enum, S>
    void validateEvent(Event<T, S> event, T type, S subject, long time) {
        assertEquals("incorrect type", type, event.type());
        assertEquals("incorrect subject", subject, event.subject());
        assertEquals("incorrect time", time, event.time());
    }

    /**
     * Validates the base attributes of an event.
     *
     * @param event   event to validate
     * @param type    event type
     * @param subject event subject
     * @param minTime minimum event time inclusive
     * @param maxTime maximum event time inclusive
     * @param <T>     type of event
     * @param <S>     type of subject
     */
    protected static <T extends Enum, S>
    void validateEvent(Event<T, S> event, T type, S subject,
                       long minTime, long maxTime) {
        assertEquals("incorrect type", type, event.type());
        assertEquals("incorrect subject", subject, event.subject());
        assertTrue("incorrect time", minTime <= event.time() && event.time() <= maxTime);
    }

    @Test
    public void withTime() {
        TestEvent event = new TestEvent(FOO, "foo", 123L);
        validateEvent(event, FOO, "foo", 123L);
    }

    @Test
    public void withoutTime() {
        long before = System.currentTimeMillis();
        TestEvent event = new TestEvent(FOO, "foo");
        long after = System.currentTimeMillis();
        validateEvent(event, FOO, "foo", before, after);
    }

}
