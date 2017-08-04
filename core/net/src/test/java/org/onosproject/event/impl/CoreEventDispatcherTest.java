/*
 * Copyright 2014-present Open Networking Foundation
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
package org.onosproject.event.impl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.event.AbstractEvent;
import org.onosproject.event.EventSink;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test of the event dispatcher mechanism.
 */
public class CoreEventDispatcherTest {

    private final CoreEventDispatcher dispatcher = new CoreEventDispatcher();
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

    @Test
    public void postEventSinkTakesTooLong() throws Exception {
        SinkProcessTakesTooLong takesTooLong = new SinkProcessTakesTooLong();
        dispatcher.setDispatchTimeLimit(250);
        dispatcher.addSink(TooLongEvent.class, takesTooLong);
        takesTooLong.latch = new CountDownLatch(1);
        dispatcher.post(new TooLongEvent("XYZZY"));
        takesTooLong.latch.await(1000, TimeUnit.MILLISECONDS);
        assertTrue(takesTooLong.interrupted);
    }

    private void validate(Sink sink, String... strings) {
        int i = 0;
        assertEquals("incorrect event count", strings.length, sink.subjects.size());
        for (String string : strings) {
            assertEquals("incorrect event", string, sink.subjects.get(i++));
        }
    }

    private enum Type { FOO }

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

    private static class TooLongEvent extends AbstractEvent<Type, String> {
        protected TooLongEvent(String subject) {
            super(Type.FOO, subject);
        }
    }

    private static class SinkProcessTakesTooLong
                         implements EventSink<TooLongEvent> {
        boolean interrupted = false;
        CountDownLatch latch;

        public void process(TooLongEvent event) {
            try {
                Thread.sleep(5 * 1000);
            } catch (InterruptedException ie) {
                interrupted = true;
            }
            latch.countDown();
        }
    }

}
