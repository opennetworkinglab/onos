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
package org.onosproject.kafkaintegration.listener;

import static org.onosproject.kafkaintegration.api.dto.OnosEvent.Type.DEVICE;
import static org.onosproject.kafkaintegration.api.dto.OnosEvent.Type.LINK;

import java.util.HashMap;
import java.util.Map;

import org.onosproject.kafkaintegration.api.dto.OnosEvent.Type;

/**
 * Returns the appropriate listener object based on the ONOS event type.
 *
 */
public final class ListenerFactory {

    // Store listeners for all supported events
    private Map<Type, OnosEventListener> listeners =
            new HashMap<Type, OnosEventListener>() {
                {
                    put(DEVICE, DeviceEventsListener.getInstance());
                    put(LINK, LinkEventsListener.getInstance());
                }
            };

    // Exists to defeat instantiation
    private ListenerFactory() {
    }

    private static class SingletonHolder {
        private static final ListenerFactory INSTANCE = new ListenerFactory();
    }

    /**
     * Returns a static reference to the Listener Factory.
     *
     * @return singleton object
     */
    public static ListenerFactory getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * Returns the listener object for the specified ONOS event type.
     *
     * @param event ONOS Event type
     * @return return listener object
     */
    public OnosEventListener getListener(Type event) {
        return listeners.get(event);
    }

}
