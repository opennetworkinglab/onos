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
import org.onosproject.store.Store;

import java.util.Set;

/**
 * Manages inventory of kubernetes network policy; not intended for direct use.
 */
public interface K8sNetworkPolicyStore
        extends Store<K8sNetworkPolicyEvent, K8sNetworkPolicyStoreDelegate> {

    /**
     * Creates the new kubernetes network policy.
     *
     * @param networkPolicy kubernetes network policy
     */
    void createNetworkPolicy(NetworkPolicy networkPolicy);

    /**
     * Updates the new kubernetes network policy.
     *
     * @param networkPolicy kubernetes network policy
     */
    void updateNetworkPolicy(NetworkPolicy networkPolicy);

    /**
     * Removes the kubernetes network policy with the given network policy UID.
     *
     * @param uid kubernetes network policy UID
     * @return removed kubernetes network policy; null if failed
     */
    NetworkPolicy removeNetworkPolicy(String uid);

    /**
     * Returns the kubernetes network policy with the given network policy UID.
     *
     * @param uid kubernetes network policy UID
     * @return kubernetes network policy; null if not found
     */
    NetworkPolicy networkPolicy(String uid);

    /**
     * Returns all kubernetes network policies.
     *
     * @return set of kubernetes network policies
     */
    Set<NetworkPolicy> networkPolicies();

    /**
     * Removes all kubernetes network policies.
     */
    void clear();
}
