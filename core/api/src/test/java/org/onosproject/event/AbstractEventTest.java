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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.onosproject.event.TestEvent.Type.FOO;

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
