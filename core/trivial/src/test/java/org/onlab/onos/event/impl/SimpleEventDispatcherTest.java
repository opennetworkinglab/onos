package org.onlab.onos.event.impl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.onos.event.AbstractEvent;
import org.onlab.onos.event.EventSink;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * Test of the even dispatcher mechanism.
 */
public class SimpleEventDispatcherTest {

    private final SimpleEventDispatcher dispatcher = new SimpleEventDispatcher();
    private final PrickleSink prickleSink = new PrickleSink();
    private final GooSink gooSink = new GooSink();

    @Before
    public void setUp() {
        dispatcher.activate();
        dispatcher.addSink(Prickle.class, prickleSink);
        dispatcher.addSink(Goo.class, gooSink);
    }

    @After
    public void tearDown() {
        dispatcher.removeSink(Goo.class);
        dispatcher.removeSink(Prickle.class);
        dispatcher.deactivate();
    }

    @Test
    public void post() throws Exception {
        prickleSink.latch = new CountDownLatch(1);
        dispatcher.post(new Prickle("yo"));
        prickleSink.latch.await(100, TimeUnit.MILLISECONDS);
        validate(prickleSink, "yo");
        validate(gooSink);
    }

    @Test
    public void postEventWithBadSink() throws Exception {
        gooSink.latch = new CountDownLatch(1);
        dispatcher.post(new Goo("boom"));
        gooSink.latch.await(100, TimeUnit.MILLISECONDS);
        validate(gooSink, "boom");
        validate(prickleSink);
    }

    @Test
    public void postEventWithNoSink() throws Exception {
        dispatcher.post(new Thing("boom"));
        validate(gooSink);
        validate(prickleSink);
    }

    private void validate(Sink sink, String... strings) {
        int i = 0;
        assertEquals("incorrect event count", strings.length, sink.subjects.size());
        for (String string : strings) {
            assertEquals("incorrect event", string, sink.subjects.get(i++));
        }
    }

    private enum Type { FOO };

    private static class Thing extends AbstractEvent<Type, String> {
        protected Thing(String subject) {
            super(Type.FOO, subject);
        }
    }

    private static class Prickle extends Thing {
        protected Prickle(String subject) {
            super(subject);
        }
    }

    private static class Goo extends Thing {
        protected Goo(String subject) {
            super(subject);
        }
    }

    private static class Sink {
        final List<String> subjects = new ArrayList<>();
        CountDownLatch latch;

        protected void process(String subject) {
            subjects.add(subject);
            latch.countDown();
        }
    }

    private static class PrickleSink extends Sink implements EventSink<Prickle> {
        @Override
        public void process(Prickle event) {
            process(event.subject());
        }
    }

    private static class GooSink extends Sink implements EventSink<Goo> {
        @Override
        public void process(Goo event) {
            process(event.subject());
            throw new IllegalStateException("BOOM!");
        }
    }

}
