/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.incubator.net.virtual;

import org.onosproject.event.AbstractEvent;

/**
 * Describes virtual network event.
 */
public class VirtualNetworkEvent extends AbstractEvent<VirtualNetworkEvent.Type, NetworkId> {

    /**
     * Type of virtual network events.
     */
    public enum Type {
        /**
         * Signifies that a new tenant identifier was registered.
         */
        TENANT_REGISTERED,
        /**
         * Signifies that a tenant identifier was unregistered.
         */
        TENANT_UNREGISTERED,
        /**
         * Signifies that a new virtual network was added.
         */
        NETWORK_ADDED,
        /**
         * Signifies that a virtual network was updated.
         */
        NETWORK_UPDATED,
        /**
         * Signifies that a virtual network was removed.
         */
        NETWORK_REMOVED
    }

    /**
     * Creates an event of a given type and for the specified subject and the
     * current time.
     *
     * @param type        event type
     * @param subject     event subject
     */
    public VirtualNetworkEvent(Type type, NetworkId subject) {
        super(type, subject);
    }

    /**
     * Creates an event of a given type and for the specified subject and time.
     *
     * @param type        device event type
     * @param subject     event subject
     * @param time        occurrence time
     */
    public VirtualNetworkEvent(Type type, NetworkId subject, long time) {
        super(type, subject, time);
    }
}
