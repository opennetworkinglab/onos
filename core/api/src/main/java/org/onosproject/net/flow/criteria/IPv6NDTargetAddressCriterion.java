/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.net.flow.criteria;

import org.onlab.packet.Ip6Address;

import java.util.Objects;

/**
 * Implementation of IPv6 Neighbor Discovery target address criterion.
 */
public final class IPv6NDTargetAddressCriterion implements Criterion {
    private final Ip6Address targetAddress;

    /**
     * Constructor.
     *
     * @param targetAddress the IPv6 target address to match
     */
    IPv6NDTargetAddressCriterion(Ip6Address targetAddress) {
        this.targetAddress = targetAddress;
    }

    @Override
    public Type type() {
        return Type.IPV6_ND_TARGET;
    }

    /**
     * Gets the IPv6 target address to match.
     *
     * @return the IPv6 target address to match
     */
    public Ip6Address targetAddress() {
        return this.targetAddress;
    }

    @Override
    public String toString() {
        return type().toString() + SEPARATOR + targetAddress;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type().ordinal(), targetAddress);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof IPv6NDTargetAddressCriterion) {
            IPv6NDTargetAddressCriterion that =
                (IPv6NDTargetAddressCriterion) obj;
            return Objects.equals(targetAddress, that.targetAddress) &&
                    Objects.equals(type(), that.type());
        }
        return false;
    }
}
