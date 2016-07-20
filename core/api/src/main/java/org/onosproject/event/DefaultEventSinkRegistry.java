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

import com.google.common.collect.ImmutableSet;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Base implementation of event sink registry.
 */
public class DefaultEventSinkRegistry implements EventSinkRegistry {

    private final Map<Class<? extends Event>, EventSink<? extends Event>>
            sinks = new ConcurrentHashMap<>();

    @Override
    public <E extends Event> void addSink(Class<E> eventClass, EventSink<E> sink) {
        checkNotNull(eventClass, "Event class cannot be null");
        checkNotNull(sink, "Event sink cannot be null");
        checkArgument(!sinks.containsKey(eventClass),
                      "Event sink already registered for %s", eventClass.getName());
        sinks.put(eventClass, sink);
    }

    @Override
    public <E extends Event> void removeSink(Class<E> eventClass) {
        checkNotNull(eventClass, "Event class cannot be null");
        checkArgument(sinks.remove(eventClass) != null,
                      "Event sink not registered for %s", eventClass.getName());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E extends Event> EventSink<E> getSink(Class<E> eventClass) {
        checkNotNull(eventClass, "Event class cannot be null");
        return (EventSink<E>) sinks.get(eventClass);
    }

    @Override
    public Set<Class<? extends Event>> getSinks() {
        return ImmutableSet.copyOf(sinks.keySet());
    }
}
