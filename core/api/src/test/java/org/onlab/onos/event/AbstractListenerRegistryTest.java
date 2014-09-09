package org.onlab.onos.event;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests of the base listener manager.
 */
public class AbstractListenerRegistryTest {

    @Test
    public void basics() {
        TestListener listener = new TestListener();
        TestListener secondListener = new TestListener();
        TestListenerRegistry manager = new TestListenerRegistry();
        manager.addListener(listener);
        manager.addListener(secondListener);

        TestEvent event = new TestEvent(TestEvent.Type.BAR, "bar");
        manager.process(event);
        assertTrue("event not processed", listener.events.contains(event));
        assertTrue("event not processed", secondListener.events.contains(event));

        manager.removeListener(listener);

        TestEvent another = new TestEvent(TestEvent.Type.FOO, "foo");
        manager.process(another);
        assertFalse("event processed", listener.events.contains(another));
        assertTrue("event not processed", secondListener.events.contains(event));
    }

    @Test
    public void badListener() {
        TestListener listener = new BrokenListener();
        TestListener secondListener = new TestListener();
        TestListenerRegistry manager = new TestListenerRegistry();
        manager.addListener(listener);
        manager.addListener(secondListener);

        TestEvent event = new TestEvent(TestEvent.Type.BAR, "bar");
        manager.process(event);
        assertFalse("event processed", listener.events.contains(event));
        assertFalse("error not reported", manager.errors.isEmpty());
        assertTrue("event not processed", secondListener.events.contains(event));
    }

}
