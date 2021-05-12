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

import org.onlab.packet.IpAddress;
import org.onosproject.event.AbstractEvent;

import java.util.Set;

public class KubevirtLoadBalancerEvent
        extends AbstractEvent<KubevirtLoadBalancerEvent.Type, KubevirtLoadBalancer> {

    private final KubevirtLoadBalancer old;
    private final Set<IpAddress> members;

    /**
     * LoadBalancerEvent constructor.
     *
     * @param type LoadBalancerEvent type
     * @param lb LoadBalancer object
     */
    public KubevirtLoadBalancerEvent(Type type, KubevirtLoadBalancer lb) {
        super(type, lb);
        this.old = null;
        this.members = null;
    }

    /**
     * Creates and event of a given type for the specified kubevirt loadbalancer.
     *
     * @param type kubevirt loadbalancer event type
     * @param lb kubevirt loadbalancer
     * @param old old kubevirt loadbalancer
     */
    public KubevirtLoadBalancerEvent(Type type, KubevirtLoadBalancer lb, KubevirtLoadBalancer old) {
        super(type, lb);
        this.old = old;
        this.members = null;
    }

    /**
     * Creates and event of a given type for the specified kubevirt loadbalancer.
     *
     * @param type kubevirt loadbalancer event type
     * @param lb kubevirt loadbalancer
     * @param members kubevirt loadbalancer members
     */
    public KubevirtLoadBalancerEvent(Type type, KubevirtLoadBalancer lb, Set<IpAddress> members) {
        super(type, lb);
        this.old = null;
        this.members = members;
    }

    /**
     * Returns the old kubevirt loadbalancer of the event.
     *
     * @return old kubevirt loadbalancer
     */
    public KubevirtLoadBalancer oldLb() {
        return old;
    }

    /**
     * Returns members of kubevirt loadbalancer of the event.
     *
     * @return kubevirt loadbalancer members
     */
    public Set<IpAddress> members() {
        return members;
    }

    public enum Type {
        /**
         * Signifies that a new kubevirt load balancer is created.
         */
        KUBEVIRT_LOAD_BALANCER_CREATED,

        /**
         * Signifies that a kubevirt load balancer is removed.
         */
        KUBEVIRT_LOAD_BALANCER_REMOVED,

        /**
         * Signifies that a kubevirt load balancer is updated.
         */
        KUBEVIRT_LOAD_BALANCER_UPDATED,

        /**
         * Signifies that a kubevirt load balancer member is added.
         */
        KUBEVIRT_LOAD_BALANCER_MEMBER_ADDED,

        /**
         * Signifies that a kubevirt load balancer member is added.
         */
        KUBEVIRT_LOAD_BALANCER_MEMBER_REMOVED,
    }
}
