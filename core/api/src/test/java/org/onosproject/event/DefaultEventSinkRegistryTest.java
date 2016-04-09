/*
 * Copyright 2014-present Open Networking Laboratory
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

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests of the default event sink registry.
 */
public class DefaultEventSinkRegistryTest {

    private DefaultEventSinkRegistry registry;

    private static class FooEvent extends TestEvent {
        public FooEvent(String subject) {
            super(Type.FOO, subject);
        }
    }

    private static class BarEvent extends TestEvent {
        public BarEvent(String subject) {
            super(Type.BAR, subject);
        }
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
