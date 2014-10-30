/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onlab.packet;

import java.util.Objects;

/**
 * The class representing an IPv6 network address.
 * This class is immutable.
 */
public final class Ip6Prefix {
    private final Ip6Address address;           // The IPv6 address
    private final short prefixLen;              // The prefix length

    /**
     * Default constructor.
     */
    public Ip6Prefix() {
        this.address = new Ip6Address();
        this.prefixLen = 0;
    }

    /**
     * Copy constructor.
     *
     * @param other the object to copy from
     */
    public Ip6Prefix(Ip6Prefix other) {
        this.address = new Ip6Address(other.address);
        this.prefixLen = other.prefixLen;
    }

    /**
     * Constructor for a given address and prefix length.
     *
     * @param address   the address to use
     * @param prefixLen the prefix length to use
     */
    public Ip6Prefix(Ip6Address address, short prefixLen) {
        this.address = Ip6Address.makeMaskedAddress(address, prefixLen);
        this.prefixLen = prefixLen;
    }

    /**
     * Constructs an IPv6 prefix from a string representation of the
     * prefix.
     *<p>
     * Example: "1111:2222::/32"
     *
     * @param value the value to use
     */
    public Ip6Prefix(String value) {
        String[] splits = value.split("/");
        if (splits.length != 2) {
            throw new IllegalArgumentException("Specified IPv6 prefix must contain an IPv6 " +
                    "address and a prefix length separated by '/'");
        }
        this.prefixLen = Short.decode(splits[1]);
        this.address = Ip6Address.makeMaskedAddress(new Ip6Address(splits[0]),
                this.prefixLen);
    }

    /**
     * Gets the address value of the IPv6 prefix.
     *
     * @return the address value of the IPv6 prefix
     */
    public Ip6Address getAddress() {
        return address;
    }

    /**
     * Gets the prefix length value of the IPv6 prefix.
     *
     * @return the prefix length value of the IPv6 prefix
     */
    public short getPrefixLen() {
        return prefixLen;
    }

    /**
     * Converts the IPv6 prefix value to an "address/prefixLen" string.
     *
     * @return the IPv6 prefix value as an "address/prefixLen" string
     */
    @Override
    public String toString() {
        return this.address.toString() + "/" + this.prefixLen;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        if (!(other instanceof Ip6Prefix)) {
            return false;
        }

        Ip6Prefix otherIp6Prefix = (Ip6Prefix) other;

        return Objects.equals(this.address, otherIp6Prefix.address)
                && this.prefixLen == otherIp6Prefix.prefixLen;
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, prefixLen);
    }
}
