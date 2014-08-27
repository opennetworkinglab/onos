package org.onlab.onos.event;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.onlab.onos.event.TestEvent.Type.FOO;

/**
 * Tests of the base event abstraction.
 */
public class AbstractEventTest {

    @Test
    public void withTime() {
        TestEvent event = new TestEvent(FOO, "foo", 123L);
        assertEquals("incorrect type", FOO, event.type());
        assertEquals("incorrect subject", "foo", event.subject());
        assertEquals("incorrect time", 123L, event.time());
    }

    @Test
    public void withoutTime() {
        long before = System.currentTimeMillis();
        TestEvent event = new TestEvent(FOO, "foo");
        long after = System.currentTimeMillis();
        assertEquals("incorrect type", FOO, event.type());
        assertEquals("incorrect subject", "foo", event.subject());
        assertTrue("incorrect time", before <= event.time() && event.time() <= after);
    }
}
