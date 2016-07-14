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

import java.util.Set;

/**
 * Abstraction of an event sink registry capable of tracking sinks based on
 * their event class.
 */
public interface EventSinkRegistry {

    /**
     * Adds the specified sink for the given event class.
     *
     * @param eventClass event class
     * @param sink       event sink
     * @param <E>        type of event
     */
    <E extends Event> void addSink(Class<E> eventClass, EventSink<E> sink);

    /**
     * Removes the sink associated with the given event class.
     *
     * @param eventClass event class
     * @param <E>        type of event
     */
    <E extends Event> void removeSink(Class<E> eventClass);

    /**
     * Returns the event sink associated with the specified event class.
     *
     * @param eventClass event class
     * @param <E>        type of event
     * @return event sink or null if none found
     */
    <E extends Event> EventSink<E> getSink(Class<E> eventClass);

    /**
     * Returns the set of all event classes for which sinks are presently
     * registered.
     *
     * @return set of event classes
     */
    Set<Class<? extends Event>> getSinks();

}
