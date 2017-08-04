/*
 * Copyright 2015-present Open Networking Foundation
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link ListenerRegistry}.
 */
public class ListenerRegistryTest {

    private static final TestEvent FOO_EVENT =
            new TestEvent(TestEvent.Type.FOO, "foo");
    private static final TestEvent BAR_EVENT =
            new TestEvent(TestEvent.Type.BAR, "bar");

    private TestListener listener;
    private TestListener secondListener;
    private TestListenerRegistry manager;

    @Before
    public void setUp() {
        listener = new TestListener();
        secondListener = new TestListener();
        manager = new TestListenerRegistry();
    }

    @Test
    public void basics() {
        manager.addListener(listener);
        manager.addListener(secondListener);

        manager.process(BAR_EVENT);
        assertTrue("BAR not processed", listener.events.contains(BAR_EVENT));
        assertTrue("BAR not processed", secondListener.events.contains(BAR_EVENT));

        manager.removeListener(listener);

        manager.process(FOO_EVENT);
        assertFalse("FOO processed", listener.events.contains(FOO_EVENT));
        assertTrue("FOO not processed", secondListener.events.contains(FOO_EVENT));
    }

    @Test
    public void badListener() {
        listener = new BrokenListener();

        manager.addListener(listener);
        manager.addListener(secondListener);

        manager.process(BAR_EVENT);
        assertFalse("BAR processed", listener.events.contains(BAR_EVENT));
        assertFalse("error not reported", manager.errors.isEmpty());
        assertTrue("BAR not processed", secondListener.events.contains(BAR_EVENT));
    }

}
