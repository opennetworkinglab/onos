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
package org.onosproject.l2lb.api;

import org.onosproject.net.PortNumber;

import java.util.Objects;
import java.util.Set;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Represents L2 load balancer information.
 */
public class L2Lb {
    private L2LbId l2LbId;
    private Set<PortNumber> ports;
    private L2LbMode mode;

    /**
     * Constructs a L2 load balancer.
     *
     * @param l2LbId L2 load balancer ID
     * @param ports Set of member ports
     * @param mode L2 load balancer mode
     */
    public L2Lb(L2LbId l2LbId, Set<PortNumber> ports, L2LbMode mode) {
        this.l2LbId = l2LbId;
        this.ports = ports;
        this.mode = mode;
    }

    /**
     * Gets L2 load balancer ID.
     *
     * @return L2 load balancer ID
     */
    public L2LbId l2LbId() {
        return l2LbId;
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
     * Gets L2 load balancer mode.
     *
     * @return L2 load balancer mode.
     */
    public L2LbMode mode() {
        return mode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(l2LbId, ports, mode);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof L2Lb)) {
            return false;
        }
        final L2Lb other = (L2Lb) obj;

        return Objects.equals(this.l2LbId, other.l2LbId) &&
                Objects.equals(this.ports, other.ports) &&
                this.mode == other.mode;
    }

    @Override
    public String toString() {
        return toStringHelper(getClass())
                .add("l2LbId", l2LbId)
                .add("ports", ports)
                .add("mode", mode)
                .toString();
    }
}
