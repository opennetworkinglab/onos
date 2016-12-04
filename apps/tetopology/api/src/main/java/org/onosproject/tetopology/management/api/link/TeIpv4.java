/**
 * Copyright 2016 Open Networking Laboratory
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.tetopology.management.api.link;

import com.google.common.base.MoreObjects;
import org.onlab.packet.Ip4Address;

import java.util.Objects;

/**
 * Implementation of IPv4 address as an element type.
 */
public class TeIpv4 implements ElementType {
    private final Ip4Address v4Address;
    private final short v4PrefixLength;

    /**
     * Creates an IPv4 address.
     *
     * @param v4Address      the IPv4 address
     * @param v4PrefixLength the length of IPv4 prefix
     */
    public TeIpv4(Ip4Address v4Address, short v4PrefixLength) {
        this.v4Address = v4Address;
        this.v4PrefixLength = v4PrefixLength;
    }

    /**
     * Returns the IPv4 address.
     *
     * @return IPv4 address
     */
    public Ip4Address v4Address() {
        return v4Address;
    }

    /**
     * Returns the length of the IPv4 address prefix.
     *
     * @return IPv4 address prefix length
     */
    public short v4PrefixLength() {
        return v4PrefixLength;
    }

    @Override
    public int hashCode() {
        return Objects.hash(v4Address, v4PrefixLength);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof TeIpv4) {
            TeIpv4 other = (TeIpv4) obj;
            return Objects.equals(v4Address, other.v4Address) &&
                    Objects.equals(v4PrefixLength, other.v4PrefixLength);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("v4Address", v4Address)
                .add("v4PrefixLength", v4PrefixLength)
                .toString();
    }

}
