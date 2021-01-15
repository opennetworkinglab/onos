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

import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicy;
import org.onosproject.event.AbstractEvent;

/**
 * Describes kubernetes network policy event.
 */
public class K8sNetworkPolicyEvent extends
        AbstractEvent<K8sNetworkPolicyEvent.Type, NetworkPolicy> {

    /**
     * Creates an event of a given type for the specified kubernetes network policy.
     *
     * @param type      kubernetes network policy event type
     * @param subject   kubernetes network policy
     */
    public K8sNetworkPolicyEvent(Type type, NetworkPolicy subject) {
        super(type, subject);
    }

    /**
     * Kubernetes network policy events.
     */
    public enum Type {

        /**
         * Signifies that a new kubernetes network policy is created.
         */
        K8S_NETWORK_POLICY_CREATED,

        /**
         * Signifies that a new kubernetes network policy is updated.
         */
        K8S_NETWORK_POLICY_UPDATED,

        /**
         * Signifies that a new kubernetes network policy is removed.
         */
        K8S_NETWORK_POLICY_REMOVED,

    }
}
