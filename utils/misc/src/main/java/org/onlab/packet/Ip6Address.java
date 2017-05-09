/*
 * Copyright 2014-present Open Networking Laboratory
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

import java.net.InetAddress;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.util.Arrays;

import com.google.common.net.InetAddresses;

/**
 * A class representing an IPv6 address.
 * This class is immutable.
 */
public final class Ip6Address extends IpAddress {
    public static final IpAddress.Version VERSION = IpAddress.Version.INET6;
    public static final int BYTE_LENGTH = IpAddress.INET6_BYTE_LENGTH;
    public static final int BIT_LENGTH = IpAddress.INET6_BIT_LENGTH;

    /**
     * All-zero unspecified IPv6 address.
     */
    public static final Ip6Address ZERO = Ip6Address.valueOf("::");

    /**
     * Constructor for given IP address version and address octets.
     *
     * @param value the IP address value stored in network byte order
     * (i.e., the most significant byte first)
     * @throws IllegalArgumentException if the arguments are invalid
     */
    private Ip6Address(byte[] value) {
        super(VERSION, value);
    }

    /**
     * Converts a byte array into an IPv6 address.
     *
     * @param value the IPv6 address value stored in network byte order
     * (i.e., the most significant byte first)
     * @return an IPv6 address
     * @throws IllegalArgumentException if the argument is invalid
     */
    public static Ip6Address valueOf(byte[] value) {
        return new Ip6Address(value);
    }

    /**
     * Converts a byte array and a given offset from the beginning of the
     * array into an IPv6 address.
     * <p>
     * The IP address is stored in network byte order (i.e., the most
     * significant byte first).
     * </p>
     * @param value the value to use
     * @param offset the offset in bytes from the beginning of the byte array
     * @return an IPv6 address
     * @throws IllegalArgumentException if the arguments are invalid
     */
    public static Ip6Address valueOf(byte[] value, int offset) {
        IpAddress.checkArguments(VERSION, value, offset);
        byte[] bc = Arrays.copyOfRange(value, offset, value.length);
        return Ip6Address.valueOf(bc);
    }

    /**
     * Converts an InetAddress into an IPv6 address.
     *
     * @param inetAddress the InetAddress value to use. It must contain an IPv6
     * address
     * @return an IPv6 address
     * @throws IllegalArgumentException if the argument is invalid
     */
    public static Ip6Address valueOf(InetAddress inetAddress) {
        byte[] bytes = inetAddress.getAddress();
        if (inetAddress instanceof Inet6Address) {
            return new Ip6Address(bytes);
        }
        if ((inetAddress instanceof Inet4Address) ||
            (bytes.length == INET_BYTE_LENGTH)) {
            final String msg = "Invalid IPv6 version address string: " +
                inetAddress.toString();
            throw new IllegalArgumentException(msg);
        }
        // Use the number of bytes as a hint
        if (bytes.length == INET6_BYTE_LENGTH) {
            return new Ip6Address(bytes);
        }
        final String msg = "Unrecognized IP version address string: " +
            inetAddress.toString();
        throw new IllegalArgumentException(msg);
    }

    /**
     * Converts an IPv6 string literal (e.g., "1111:2222::8888") into an IP
     * address.
     *
     * @param value an IPv6 address value in string form
     * @return an IPv6 address
     * @throws IllegalArgumentException if the argument is invalid
     */
    public static Ip6Address valueOf(String value) {
        InetAddress inetAddress = null;
        try {
            inetAddress = InetAddresses.forString(value);
        } catch (IllegalArgumentException e) {
            final String msg = "Invalid IP address string: " + value;
            throw new IllegalArgumentException(msg);
        }
        return valueOf(inetAddress);
    }

    /**
     * Creates an IPv6 network mask prefix.
     *
     * @param prefixLength the length of the mask prefix. Must be in the
     * interval [0, 128]
     * @return a new IPv6 address that contains a mask prefix of the
     * specified length
     * @throws IllegalArgumentException if the arguments are invalid
     */
    public static Ip6Address makeMaskPrefix(int prefixLength) {
        byte[] mask = IpAddress.makeMaskPrefixArray(VERSION, prefixLength);
        return new Ip6Address(mask);
    }

    /**
     * Creates an IPv6 address by masking it with a network mask of given
     * mask length.
     *
     * @param address the address to mask
     * @param prefixLength the length of the mask prefix. Must be in the
     * interval [0, 128]
     * @return a new IPv6 address that is masked with a mask prefix of the
     * specified length
     * @throws IllegalArgumentException if the prefix length is invalid
     */
    public static Ip6Address makeMaskedAddress(final Ip6Address address,
                                               int prefixLength) {
        byte[] net = makeMaskedAddressArray(address, prefixLength);
        return Ip6Address.valueOf(net);
    }
}
