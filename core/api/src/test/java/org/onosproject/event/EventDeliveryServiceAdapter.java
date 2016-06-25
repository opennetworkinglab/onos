/*
 * Copyright 2015-present Open Networking Laboratory
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
 * Testing adapter for the event delivery service.
 */
public class EventDeliveryServiceAdapter implements EventDeliveryService {
    @Override
    public void setDispatchTimeLimit(long millis) {

    }

    @Override
    public long getDispatchTimeLimit() {
        return 0;
    }

    @Override
    public void post(Event event) {

    }

    @Override
    public <E extends Event> void addSink(Class<E> eventClass, EventSink<E> sink) {

    }

    @Override
    public <E extends Event> void removeSink(Class<E> eventClass) {

    }

    @Override
    public <E extends Event> EventSink<E> getSink(Class<E> eventClass) {
        return null;
    }

    @Override
    public Set<Class<? extends Event>> getSinks() {
        return null;
    }
}
