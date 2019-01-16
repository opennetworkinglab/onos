/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.portloadbalancer.api;


import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Represents port load balancer event data.
 */
public class PortLoadBalancerData {

    // We exchange only id and nextid in the events
    private PortLoadBalancerId portLoadBalancerId;
    private int nextId;

    /**
     * Constructs a port load balancer data.
     *
     * @param portLoadBalancerId port load balancer ID
     */
    public PortLoadBalancerData(PortLoadBalancerId portLoadBalancerId) {
        this.portLoadBalancerId = portLoadBalancerId;
        this.nextId = -1;
    }

    /**
     * Constructs a port load balancer data.
     *
     * @param portLoadBalancerId port load balancer ID
     * @param nextId port load balancer next id
     */
    public PortLoadBalancerData(PortLoadBalancerId portLoadBalancerId, int nextId) {
        this.portLoadBalancerId = portLoadBalancerId;
        this.nextId = nextId;
    }

    /**
     * Gets port load balancer ID.
     *
     * @return port load balancer ID
     */
    public PortLoadBalancerId portLoadBalancerId() {
        return portLoadBalancerId;
    }

    /**
     * Gets port load balancer next id.
     *
     * @return port load balancer next id
     */
    public int nextId() {
        return nextId;
    }

    /**
     * Sets port load balancer next id.
     *
     * @param nextId port load balancer next id
     */
    public void setNextId(int nextId) {
        this.nextId = nextId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(portLoadBalancerId, nextId);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PortLoadBalancerData)) {
            return false;
        }
        final PortLoadBalancerData other = (PortLoadBalancerData) obj;

        return Objects.equals(this.portLoadBalancerId, other.portLoadBalancerId) &&
                this.nextId == other.nextId;
    }

    @Override
    public String toString() {
        return toStringHelper(getClass())
                .add("portLoadBalancerId", portLoadBalancerId)
                .add("nextId", nextId)
                .toString();
    }
}
