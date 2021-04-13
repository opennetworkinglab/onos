/*
 * Copyright 2021-present Open Networking Foundation
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
package org.onosproject.kubevirtnetworking.api;

import org.onosproject.store.Store;

import java.util.Set;

/**
 * Manages inventory of kubevirt load balancer states; not intended for direct use.
 */
public interface KubevirtLoadBalancerStore
        extends Store<KubevirtLoadBalancerEvent, KubevirtLoadBalancerStoreDelegate> {

    /**
     * Creates a new kubevirt load balancer.
     *
     * @param lb kubevirt load balancer
     */
    void createLoadBalancer(KubevirtLoadBalancer lb);

    /**
     * Updates the kubevirt load balancer.
     *
     * @param lb kubevirt load balancer
     */
    void updateLoadBalancer(KubevirtLoadBalancer lb);

    /**
     * Removes the kubevirt load balancer with the given load balancer name.
     *
     * @param name load balancer name
     * @return remove kubevirt load balancer; null if failed
     */
    KubevirtLoadBalancer removeLoadBalancer(String name);

    /**
     * Returns the kubevirt load balancer with the given load balancer name.
     *
     * @param name load balancer name
     * @return load balancer; null if failed
     */
    KubevirtLoadBalancer loadBalancer(String name);

    /**
     * Returns all kubevirt load balancers.
     *
     * @return set of kubevirt load balancers
     */
    Set<KubevirtLoadBalancer> loadBalancers();

    /**
     * Removes all kubevirt load balancers.
     */
    void clear();
}
