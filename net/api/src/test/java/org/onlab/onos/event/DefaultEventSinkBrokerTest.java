package org.onlab.onos.event;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests of the default event sink broker.
 */
public class DefaultEventSinkBrokerTest {

    private DefaultEventSinkBroker broker;

    private static class FooEvent extends TestEvent {
        public FooEvent(String subject) { super(Type.FOO, subject); }
    }

    private static class BarEvent extends TestEvent {
        public BarEvent(String subject) { super(Type.BAR, subject); }
    }

    private static class FooSink implements EventSink<FooEvent> {
        @Override public void process(FooEvent event) {}
    }

    private static class BarSink implements EventSink<BarEvent> {
        @Override public void process(BarEvent event) {}
    }

    @Before
    public void setUp() {
        broker = new DefaultEventSinkBroker();
    }

    @Test
    public void basics() {
        FooSink fooSink = new FooSink();
        BarSink barSink = new BarSink();
        broker.addSink(FooEvent.class, fooSink);
        broker.addSink(BarEvent.class, barSink);

        assertEquals("incorrect sink count", 2, broker.getSinks().size());
        assertEquals("incorrect sink", fooSink, broker.getSink(FooEvent.class));
        assertEquals("incorrect sink", barSink, broker.getSink(BarEvent.class));

        broker.removeSink(FooEvent.class);
        assertNull("incorrect sink", broker.getSink(FooEvent.class));
        assertEquals("incorrect sink", barSink, broker.getSink(BarEvent.class));

    }
}
