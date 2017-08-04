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
 * The class representing an IPv4 network address.
 * This class is immutable.
 */
public final class Ip4Prefix extends IpPrefix {
    public static final IpAddress.Version VERSION = IpAddress.Version.INET;
    // Maximum network mask length
    public static final int MAX_MASK_LENGTH = IpPrefix.MAX_INET_MASK_LENGTH;

    /**
     * Constructor for given IPv4 address, and a prefix length.
     *
     * @param address the IPv4 address
     * @param prefixLength the prefix length
     * @throws IllegalArgumentException if the prefix length value is invalid
     */
    private Ip4Prefix(Ip4Address address, int prefixLength) {
        super(address, prefixLength);
    }

    /**
     * Returns the IPv4 address value of the prefix.
     *
     * @return the IPv4 address value of the prefix
     */
    public Ip4Address address() {
        IpAddress a = super.address();
        return (Ip4Address) a;
    }

    /**
     * Converts an integer and a prefix length into an IPv4 prefix.
     *
     * @param address an integer representing the IPv4 address
     * @param prefixLength the prefix length
     * @return an IPv4 prefix
     * @throws IllegalArgumentException if the prefix length value is invalid
     */
    public static Ip4Prefix valueOf(int address, int prefixLength) {
        return new Ip4Prefix(Ip4Address.valueOf(address), prefixLength);
    }

    /**
     * Converts a byte array and a prefix length into an IPv4 prefix.
     *
     * @param address the IPv4 address value stored in network byte order
     * @param prefixLength the prefix length
     * @return an IPv4 prefix
     * @throws IllegalArgumentException if the prefix length value is invalid
     */
    public static Ip4Prefix valueOf(byte[] address, int prefixLength) {
        return new Ip4Prefix(Ip4Address.valueOf(address), prefixLength);
    }

    /**
     * Converts an IPv4 address and a prefix length into an IPv4 prefix.
     *
     * @param address the IPv4 address
     * @param prefixLength the prefix length
     * @return an IPv4 prefix
     * @throws IllegalArgumentException if the prefix length value is invalid
     */
    public static Ip4Prefix valueOf(Ip4Address address, int prefixLength) {
        return new Ip4Prefix(address, prefixLength);
    }

    /**
     * Converts a CIDR (slash) notation string (e.g., "10.1.0.0/16")
     * into an IPv4 prefix.
     *
     * @param address an IP prefix in string form (e.g., "10.1.0.0/16")
     * @return an IPv4 prefix
     * @throws IllegalArgumentException if the arguments are invalid
     */
    public static Ip4Prefix valueOf(String address) {
        final String[] parts = address.split("/");
        if (parts.length != 2) {
            String msg = "Malformed IPv4 prefix string: " + address + ". " +
                "Address must take form \"x.x.x.x/y\"";
            throw new IllegalArgumentException(msg);
        }
        Ip4Address ipAddress = Ip4Address.valueOf(parts[0]);
        int prefixLength = Integer.parseInt(parts[1]);

        return new Ip4Prefix(ipAddress, prefixLength);
    }
}
