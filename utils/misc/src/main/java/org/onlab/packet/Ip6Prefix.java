/*
 * Copyright 2014-present Open Networking Foundation
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

/**
 * The class representing an IPv6 network address.
 * This class is immutable.
 */
public final class Ip6Prefix extends IpPrefix {
    public static final IpAddress.Version VERSION = IpAddress.Version.INET6;
    // Maximum network mask length
    public static final int MAX_MASK_LENGTH = IpPrefix.MAX_INET6_MASK_LENGTH;

    /**
     * Constructor for given IPv6 address, and a prefix length.
     *
     * @param address the IPv6 address
     * @param prefixLength the prefix length
     * @throws IllegalArgumentException if the prefix length value is invalid
     */
    private Ip6Prefix(Ip6Address address, int prefixLength) {
        super(address, prefixLength);
    }

    /**
     * Returns the IPv6 address value of the prefix.
     *
     * @return the IPv6 address value of the prefix
     */
    public Ip6Address address() {
        IpAddress a = super.address();
        return (Ip6Address) a;
    }

    /**
     * Converts a byte array and a prefix length into an IPv6 prefix.
     *
     * @param address the IPv6 address value stored in network byte order
     * @param prefixLength the prefix length
     * @return an IPv6 prefix
     * @throws IllegalArgumentException if the prefix length value is invalid
     */
    public static Ip6Prefix valueOf(byte[] address, int prefixLength) {
        return new Ip6Prefix(Ip6Address.valueOf(address), prefixLength);
    }

    /**
     * Converts an IPv6 address and a prefix length into an IPv6 prefix.
     *
     * @param address the IPv6 address
     * @param prefixLength the prefix length
     * @return an IPv6 prefix
     * @throws IllegalArgumentException if the prefix length value is invalid
     */
    public static Ip6Prefix valueOf(Ip6Address address, int prefixLength) {
        return new Ip6Prefix(address, prefixLength);
    }

    /**
     * Converts a CIDR (slash) notation string (e.g., "1111:2222::/64")
     * into an IPv6 prefix.
     *
     * @param address an IP prefix in string form (e.g.,"1111:2222::/64")
     * @return an IPv6 prefix
     * @throws IllegalArgumentException if the arguments are invalid
     */
    public static Ip6Prefix valueOf(String address) {
        final String[] parts = address.split("/");
        if (parts.length != 2) {
            String msg = "Malformed IPv6 prefix string: " + address + ". " +
                "Address must take form " +
                "\"xxxx:xxxx:xxxx:xxxx:xxxx:xxxx:xxxx:xxxx/y\"";
            throw new IllegalArgumentException(msg);
        }
        Ip6Address ipAddress = Ip6Address.valueOf(parts[0]);
        int prefixLength = Integer.parseInt(parts[1]);

        return new Ip6Prefix(ipAddress, prefixLength);
    }
}
