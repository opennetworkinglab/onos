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
import org.onosproject.event.ListenerService;

import java.util.Set;

/**
 * Service for interacting with the inventory of kubernetes network policy.
 */
public interface K8sNetworkPolicyService
        extends ListenerService<K8sNetworkPolicyEvent, K8sNetworkPolicyListener> {

    /**
     * Returns the kubernetes network policy with the supplied network policy UID.
     *
     * @param uid kubernetes network policy UID
     * @return kubernetes network policy
     */
    NetworkPolicy networkPolicy(String uid);

    /**
     * Returns all kubernetes network policies registered.
     *
     * @return set of kubernetes network policies
     */
    Set<NetworkPolicy> networkPolicies();
}
