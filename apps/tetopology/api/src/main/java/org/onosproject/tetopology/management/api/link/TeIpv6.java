/*
 * Copyright 2016 Open Networking Laboratory
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
package org.onosproject.tetopology.management.api.link;

import com.google.common.base.MoreObjects;
import org.onlab.packet.Ip6Address;

import java.util.Objects;

/**
 * Implementation of an IPv6 address as an element type.
 */
public class TeIpv6 implements ElementType {
    private final Ip6Address v6Address;
    private final short v6PrefixLength;

    /**
     * Creates an IPv6 address.
     *
     * @param v6Address      the IP v6 address to set
     * @param v6PrefixLength the length of the IPv6 address prefix
     */
    public TeIpv6(Ip6Address v6Address, short v6PrefixLength) {
        this.v6Address = v6Address;
        this.v6PrefixLength = v6PrefixLength;
    }

    /**
     * Returns the IPv6 address.
     *
     * @return the IPv6 address
     */
    public Ip6Address v6Address() {
        return v6Address;
    }

    /**
     * Returns the length of the IPv6 address prefix.
     *
     * @return IPv6 address prefix length
     */
    public short v6PrefixLength() {
        return v6PrefixLength;
    }

    @Override
    public int hashCode() {
        return Objects.hash(v6Address, v6PrefixLength);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof TeIpv6) {
            TeIpv6 other = (TeIpv6) obj;
            return Objects.equals(v6Address, other.v6Address) &&
                    Objects.equals(v6PrefixLength, other.v6PrefixLength);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("v6Address", v6Address)
                .add("v6PrefixLength", v6PrefixLength)
                .toString();
    }
}
