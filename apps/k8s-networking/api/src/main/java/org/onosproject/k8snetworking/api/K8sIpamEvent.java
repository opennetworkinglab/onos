/*
 * Copyright 2019-present Open Networking Foundation
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
package org.onosproject.k8snetworking.api;

import org.onosproject.event.AbstractEvent;

/**
 * Describes kubernetes IPAM service event.
 */
public class K8sIpamEvent extends AbstractEvent<K8sIpamEvent.Type, K8sIpam> {

    /**
     * Creates an event of a given type for the specified IPAM.
     *
     * @param type      kubernetes IPAM event type
     * @param subject   kubernetes IPAM
     */
    protected K8sIpamEvent(Type type, K8sIpam subject) {
        super(type, subject);
    }

    /**
     * kubernetes IPAM events.
     */
    public enum Type {
        /**
         * Signifies that a new IP address was allocated.
         */
        K8S_IP_ALLOCATED,

        /**
         * Signifies that an existing IP address was released.
         */
        K8S_IP_RELEASED,
    }
}
