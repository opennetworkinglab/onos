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
package org.onosproject.common.event.impl;

import org.onosproject.event.DefaultEventSinkRegistry;
import org.onosproject.event.Event;
import org.onosproject.event.EventDeliveryService;
import org.onosproject.event.EventSink;

import static com.google.common.base.Preconditions.checkState;

/**
 * Implements event delivery system that delivers events synchronously, or
 * in-line with the post method invocation.
 */
public class TestEventDispatcher extends DefaultEventSinkRegistry
        implements EventDeliveryService {

    @Override
    @SuppressWarnings("unchecked")
    public synchronized void post(Event event) {
        EventSink sink = getSink(event.getClass());
        checkState(sink != null, "No sink for event %s", event);
        sink.process(event);
    }

    @Override
    public void setDispatchTimeLimit(long millis) {
    }

    @Override
    public long getDispatchTimeLimit() {
        return 0;
    }
}
