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

import org.onosproject.event.AbstractEvent;

public class KubevirtLoadBalancerEvent
        extends AbstractEvent<KubevirtLoadBalancerEvent.Type, KubevirtLoadBalancer> {

    /**
     * LoadBalancerEvent constructor.
     *
     * @param type LoadBalancerEvent type
     * @param lb LoadBalancer object
     */
    public KubevirtLoadBalancerEvent(Type type, KubevirtLoadBalancer lb) {
        super(type, lb);
    }

    public enum Type {
        /**
         * Signifies that a new kubevirt load balancer is created.
         */
        KUBEVIRT_LOAD_BALANCER_CREATED,

        /**
         * Signifies that a new kubevirt load balancer is removed.
         */
        KUBEVIRT_LOAD_BALANCER_REMOVED,

        /**
         * Signifies that a new kubevirt load balancer is updated.
         */
        KUBEVIRT_LOAD_BALANCER_UPDATED,
    }
}
