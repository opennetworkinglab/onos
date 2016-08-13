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

import java.util.Objects;

import org.onlab.packet.Ip6Address;

import com.google.common.base.MoreObjects;

/**
 * Implementation of IPv6 address as an ElementType.
 */
public class TeIpv6 implements ElementType {
    private Ip6Address v6Address;
    private short v6PrefixLength;
    private boolean v6Loose;

    /**
     * Creates an instance of TeIpv6.
     */
    public TeIpv6() {
    }

    /**
     * Sets the v6 address.
     *
     * @param v6Address the v6Address to set
     */
    public void setV6Address(Ip6Address v6Address) {
        this.v6Address = v6Address;
    }

    /**
     * Sets the prefix length.
     *
     * @param v6PrefixLength the v6PrefixLength to set
     */
    public void setV6PrefixLength(short v6PrefixLength) {
        this.v6PrefixLength = v6PrefixLength;
    }

    /**
     * Sets the loose flag.
     *
     * @param v6Loose the v6Loose to set
     */
    public void setv6Loose(boolean v6Loose) {
        this.v6Loose = v6Loose;
    }

    /**
     * Returns the v6Address.
     *
     * @return IPv6 address
     */
    public Ip6Address v6Address() {
        return v6Address;
    }

    /**
     * Returns the v6PrefixLength.
     *
     * @return IPv6 address prefix length
     */
    public short v6PrefixLength() {
        return v6PrefixLength;
    }

    /**
     * Returns the v6Loose.
     *
     * @return true if the address specifies a loose hop; false otherwise
     */
    public boolean v6Loose() {
        return v6Loose;
    }

    @Override
    public int hashCode() {
        return Objects.hash(v6Address, v6PrefixLength, v6Loose);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof TeIpv6) {
            TeIpv6 other = (TeIpv6) obj;
            return Objects.equals(v6Address, other.v6Address) &&
                 Objects.equals(v6PrefixLength, other.v6PrefixLength) &&
                 Objects.equals(v6Loose, other.v6Loose);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
            .add("v6Address", v6Address)
            .add("v6PrefixLength", v6PrefixLength)
            .add("v6Loose", v6Loose)
            .toString();
    }

}
