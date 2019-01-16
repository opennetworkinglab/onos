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

import org.onosproject.net.PortNumber;

import java.util.Objects;
import java.util.Set;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Represents port load balancer information.
 */
public class PortLoadBalancer {
    private PortLoadBalancerId portLoadBalancerId;
    private Set<PortNumber> ports;
    private PortLoadBalancerMode mode;

    /**
     * Constructs a port load balancer.
     *
     * @param portLoadBalancerId port load balancer ID
     * @param ports Set of member ports
     * @param mode port load balancer mode
     */
    public PortLoadBalancer(PortLoadBalancerId portLoadBalancerId, Set<PortNumber> ports, PortLoadBalancerMode mode) {
        this.portLoadBalancerId = portLoadBalancerId;
        this.ports = ports;
        this.mode = mode;
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
     * Gets set of member ports.
     *
     * @return Set of member ports
     */
    public Set<PortNumber> ports() {
        return ports;
    }

    /**
     * Gets port load balancer mode.
     *
     * @return port load balancer mode.
     */
    public PortLoadBalancerMode mode() {
        return mode;
    }

    /**
     * Gets port load balancer data.
     *
     * @return port load balancer data
     */
    public PortLoadBalancerData data() {
        return new PortLoadBalancerData(portLoadBalancerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(portLoadBalancerId, ports, mode);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PortLoadBalancer)) {
            return false;
        }
        final PortLoadBalancer other = (PortLoadBalancer) obj;

        return Objects.equals(this.portLoadBalancerId, other.portLoadBalancerId) &&
                Objects.equals(this.ports, other.ports) &&
                this.mode == other.mode;
    }

    @Override
    public String toString() {
        return toStringHelper(getClass())
                .add("id", portLoadBalancerId)
                .add("ports", ports)
                .add("mode", mode)
                .toString();
    }
}
