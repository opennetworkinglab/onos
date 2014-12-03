/*
 * Copyright 2014 Open Networking Laboratory
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
