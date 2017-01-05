/*
 * Copyright 2017-present Open Networking Laboratory
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

package org.onosproject.provider.te.utils;

import org.onosproject.net.DeviceId;
import org.onosproject.protocol.restconf.RestconfNotificationEventListener;
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of the RESTCONF notification event
 * listener for TE Topology.
 */
public class TeTopologyRestconfEventListener implements
        RestconfNotificationEventListener<String> {
    private static final String TE_TOPOLOGY_NOTIFICATION_PREFIX =
            "{\"ietf-te-topology:ietf-te-topology\":";
    private static final String TE_LINK_EVENT_PREFIX =
            "{\"ietf-te-topology:te-link-event\":";
    private static final String TE_NODE_EVENT_PREFIX =
            "{\"ietf-te-topology:te-node-event\":";

    private final Logger log = getLogger(getClass());

    private Map<TeTopologyRestconfEventType, RestconfNotificationEventProcessor>
            eventCallbackFunctionMap = new ConcurrentHashMap<>();

    @Override
    public void handleNotificationEvent(DeviceId deviceId,
                                        String eventJsonString) {
        log.debug("New notification: {} for device: {}",
                  eventJsonString, deviceId.toString());

        if (!eventJsonString.startsWith(TE_TOPOLOGY_NOTIFICATION_PREFIX)) {
            // This is not a TE topology event.
            return;
        }

        String teEventString = removePrefixTagFromJson(eventJsonString,
                                                       TE_TOPOLOGY_NOTIFICATION_PREFIX);

        TeTopologyRestconfEventType eventType = getEventType(teEventString);

        if (eventType == TeTopologyRestconfEventType.TE_UNKNOWN_EVENT) {
            log.error("handleNotificationEvent: unknown event: {}", eventJsonString);
            return;
        }

        RestconfNotificationEventProcessor eventProcessor =
                eventCallbackFunctionMap.get(eventType);

        if (eventProcessor != null) {
            eventProcessor.processEventPayload(teEventString);
        } else {
            log.info("Event callback not installed for event type: {}", eventType);
        }
    }

    /**
     * Registers an notification event callback function which is called by
     * the listener when it receives an event.
     *
     * @param eventType      notification event type corresponding to the
     *                       callback function
     * @param eventProcessor callback function
     */
    public void addCallbackFunction(TeTopologyRestconfEventType eventType,
                                    RestconfNotificationEventProcessor eventProcessor) {
        if (eventCallbackFunctionMap.containsKey(eventType)) {
            removeCallbackFunction(eventType);
        }

        eventCallbackFunctionMap.put(eventType, eventProcessor);
    }

    /**
     * Removes the callback function associated with the given event type.
     *
     * @param eventType notification event type
     */
    public void removeCallbackFunction(TeTopologyRestconfEventType eventType) {
        eventCallbackFunctionMap.remove(eventType);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TeTopologyRestconfEventListener that = (TeTopologyRestconfEventListener) o;

        return eventCallbackFunctionMap != null ?
                eventCallbackFunctionMap.equals(that.eventCallbackFunctionMap) :
                that.eventCallbackFunctionMap == null;
    }

    @Override
    public int hashCode() {
        return eventCallbackFunctionMap != null ? eventCallbackFunctionMap.hashCode() : 0;
    }

    private String removePrefixTagFromJson(String jsonString, String prefixTag) {
        if (jsonString.startsWith(prefixTag)) {
            return jsonString.substring(prefixTag.length(), jsonString.length() - 1);
        }
        return jsonString;
    }

    private TeTopologyRestconfEventType getEventType(String teEventString) {
        if (teEventString.startsWith(TE_LINK_EVENT_PREFIX)) {
            return TeTopologyRestconfEventType.TE_TOPOLOGY_LINK_NOTIFICATION;
        }

        if (teEventString.startsWith(TE_NODE_EVENT_PREFIX)) {
            return TeTopologyRestconfEventType.TE_TOPOLOGY_NODE_NOTIFICATION;
        }

        return TeTopologyRestconfEventType.TE_UNKNOWN_EVENT;
    }
}

