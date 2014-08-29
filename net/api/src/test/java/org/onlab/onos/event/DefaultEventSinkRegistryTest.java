package org.onlab.onos.event;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests of the default event sink registry.
 */
public class DefaultEventSinkRegistryTest {

    private DefaultEventSinkRegistry registry;

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
        registry = new DefaultEventSinkRegistry();
    }

    @Test
    public void basics() {
        FooSink fooSink = new FooSink();
        BarSink barSink = new BarSink();
        registry.addSink(FooEvent.class, fooSink);
        registry.addSink(BarEvent.class, barSink);

        assertEquals("incorrect sink count", 2, registry.getSinks().size());
        assertEquals("incorrect sink", fooSink, registry.getSink(FooEvent.class));
        assertEquals("incorrect sink", barSink, registry.getSink(BarEvent.class));

        registry.removeSink(FooEvent.class);
        assertNull("incorrect sink", registry.getSink(FooEvent.class));
        assertEquals("incorrect sink", barSink, registry.getSink(BarEvent.class));

    }
}
