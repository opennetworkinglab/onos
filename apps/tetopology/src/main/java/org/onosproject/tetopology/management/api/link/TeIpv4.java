/**
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

import org.onlab.packet.Ip4Address;

import com.google.common.base.MoreObjects;

/**
 * Implementation of IPv4 address as an ElementType.
 */
public class TeIpv4 implements ElementType {
    private Ip4Address v4Address;
    private short v4PrefixLength;
    private boolean v4Loose;

    /**
     * Creates an instance of TeIpv4.
     */
    public TeIpv4() {
    }

    /**
     * Sets the v4 address.
     *
     * @param v4Address the v4Address to set
     */
    public void setV4Address(Ip4Address v4Address) {
        this.v4Address = v4Address;
    }

    /**
     * Sets the prefix length.
     *
     * @param v4PrefixLength the v4PrefixLength to set
     */
    public void setV4PrefixLength(short v4PrefixLength) {
        this.v4PrefixLength = v4PrefixLength;
    }

    /**
     * Sets the loose flag.
     *
     * @param v4Loose the v4Loose to set
     */
    public void setV4Loose(boolean v4Loose) {
        this.v4Loose = v4Loose;
    }

    /**
     * Returns the v4Address.
     *
     * @return IPv4 address
     */
    public Ip4Address v4Address() {
        return v4Address;
    }

    /**
     * Returns the v4PrefixLength.
     *
     * @return IPv4 address prefix length
     */
    public short v4PrefixLength() {
        return v4PrefixLength;
    }

    /**
     * Returns the v4Loose.
     *
     * @return true if the address specifies a loose hop; false otherwise
     */
    public boolean v4Loose() {
        return v4Loose;
    }

    @Override
    public int hashCode() {
        return Objects.hash(v4Address, v4PrefixLength, v4Loose);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof TeIpv4) {
            TeIpv4 other = (TeIpv4) obj;
            return Objects.equals(v4Address, other.v4Address) &&
                 Objects.equals(v4PrefixLength, other.v4PrefixLength) &&
                 Objects.equals(v4Loose, other.v4Loose);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
            .add("v4Address", v4Address)
            .add("v4PrefixLength", v4PrefixLength)
            .add("v4Loose", v4Loose)
            .toString();
    }

}
