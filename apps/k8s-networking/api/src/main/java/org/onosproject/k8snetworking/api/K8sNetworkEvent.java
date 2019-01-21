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
 * Describes kubernetes network service event.
 */
public class K8sNetworkEvent extends AbstractEvent<K8sNetworkEvent.Type, K8sNetwork> {

    /**
     * Creates an event of a given type for the specified network.
     *
     * @param type kubernetes network event type
     * @param subject kubernetes network
     */
    protected K8sNetworkEvent(Type type, K8sNetwork subject) {
        super(type, subject);
    }

    /**
     * Kubernetes network events.
     */
    public enum Type {

        /**
         * Signifies that a new kubernetes network is created.
         */
        K8S_NETWORK_CREATED,

        /**
         * Signifies that the kubernetes network is updated.
         */
        K8S_NETWORK_UPDATED,

        /**
         * Signifies that the kubernetes network is removed.
         */
        K8S_NETWORK_REMOVED,
    }
}
