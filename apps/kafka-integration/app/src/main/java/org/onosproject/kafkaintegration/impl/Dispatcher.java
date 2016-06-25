/**
 * Copyright 2016 Open Networking Laboratory
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.kafkaintegration.impl;

import org.onosproject.event.AbstractListenerManager;
import org.onosproject.kafkaintegration.api.ExportableEventListener;
import org.onosproject.kafkaintegration.api.dto.OnosEvent;
import org.onosproject.kafkaintegration.api.dto.OnosEvent.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.GeneratedMessage;

/**
 * Dispatch ONOS Events to all interested Listeners.
 *
 */
public final class Dispatcher
        extends AbstractListenerManager<OnosEvent, ExportableEventListener> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    // Exists to defeat instantiation
    private Dispatcher() {
    }

    private static class SingletonHolder {
        private static final Dispatcher INSTANCE = new Dispatcher();
    }

    /**
     * Returns a static reference to the Listener Factory.
     *
     * @return singleton object
     */
    public static Dispatcher getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * Publish the ONOS Event to all listeners.
     *
     * @param eventType the ONOS eventtype
     * @param message generated Protocol buffer message from ONOS event data
     */
    public void publish(Type eventType, GeneratedMessage message) {
        log.debug("Dispatching ONOS Event {}", eventType);
        post(new OnosEvent(eventType, message));
    }
}
