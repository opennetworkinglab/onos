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
 * The class representing an IPv4 network address.
 * This class is immutable.
 */
public final class Ip4Prefix {
    private final Ip4Address address;           // The IPv4 address
    private final short prefixLen;              // The prefix length

    /**
     * Default constructor.
     */
    public Ip4Prefix() {
        this.address = new Ip4Address();
        this.prefixLen = 0;
    }

    /**
     * Copy constructor.
     *
     * @param other the object to copy from
     */
    public Ip4Prefix(Ip4Prefix other) {
        this.address = new Ip4Address(other.address);
        this.prefixLen = other.prefixLen;
    }

    /**
     * Constructor for a given address and prefix length.
     *
     * @param address   the address to use
     * @param prefixLen the prefix length to use
     */
    public Ip4Prefix(Ip4Address address, short prefixLen) {
        this.address = Ip4Address.makeMaskedAddress(address, prefixLen);
        this.prefixLen = prefixLen;
    }

    /**
     * Constructs an IPv4 prefix from a string representation of the
     * prefix.
     *<p>
     * Example: "1.2.0.0/16"
     *
     * @param value the value to use
     */
    public Ip4Prefix(String value) {
        String[] splits = value.split("/");
        if (splits.length != 2) {
            throw new IllegalArgumentException("Specified IPv4 prefix must contain an IPv4 " +
                    "address and a prefix length separated by '/'");
        }
        this.prefixLen = Short.decode(splits[1]);
        this.address = Ip4Address.makeMaskedAddress(new Ip4Address(splits[0]),
                this.prefixLen);
    }

    /**
     * Gets the address value of the IPv4 prefix.
     *
     * @return the address value of the IPv4 prefix
     */
    public Ip4Address getAddress() {
        return address;
    }

    /**
     * Gets the prefix length value of the IPv4 prefix.
     *
     * @return the prefix length value of the IPv4 prefix
     */
    public short getPrefixLen() {
        return prefixLen;
    }

    /**
     * Converts the IPv4 prefix value to an "address/prefixLen" string.
     *
     * @return the IPv4 prefix value as an "address/prefixLen" string
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

        if (!(other instanceof Ip4Prefix)) {
            return false;
        }

        Ip4Prefix otherIp4Prefix = (Ip4Prefix) other;

        return Objects.equals(this.address, otherIp4Prefix.address)
                && this.prefixLen == otherIp4Prefix.prefixLen;
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, prefixLen);
    }
}
