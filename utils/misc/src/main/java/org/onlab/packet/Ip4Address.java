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
import java.nio.ByteBuffer;
import java.util.Arrays;

import com.google.common.net.InetAddresses;

/**
 * A class representing an IPv4 address.
 * This class is immutable.
 */
public final class Ip4Address extends IpAddress {
    public static final IpAddress.Version VERSION = IpAddress.Version.INET;
    public static final int BYTE_LENGTH = IpAddress.INET_BYTE_LENGTH;
    public static final int BIT_LENGTH = IpAddress.INET_BIT_LENGTH;

    /**
     * Constructor for given IP address version and address octets.
     *
     * @param value the IP address value stored in network byte order
     * (i.e., the most significant byte first)
     * @throws IllegalArgumentException if the arguments are invalid
     */
    private Ip4Address(byte[] value) {
        super(VERSION, value);
    }

    /**
     * Returns the integer value of this IPv4 address.
     *
     * @return the IPv4 address's value as an integer
     */
    public int toInt() {
        ByteBuffer bb = ByteBuffer.wrap(super.toOctets());
        return bb.getInt();
    }

    /**
     * Converts an integer into an IPv4 address.
     *
     * @param value an integer representing an IPv4 address value
     * @return an IPv4 address
     */
    public static Ip4Address valueOf(int value) {
        byte[] bytes =
            ByteBuffer.allocate(INET_BYTE_LENGTH).putInt(value).array();
        return new Ip4Address(bytes);
    }

    /**
     * Converts a byte array into an IPv4 address.
     *
     * @param value the IPv4 address value stored in network byte order
     * (i.e., the most significant byte first)
     * @return an IPv4 address
     * @throws IllegalArgumentException if the argument is invalid
     */
    public static Ip4Address valueOf(byte[] value) {
        return new Ip4Address(value);
    }

    /**
     * Converts a byte array and a given offset from the beginning of the
     * array into an IPv4 address.
     * <p>
     * The IP address is stored in network byte order (i.e., the most
     * significant byte first).
     * </p>
     * @param value the value to use
     * @param offset the offset in bytes from the beginning of the byte array
     * @return an IPv4 address
     * @throws IllegalArgumentException if the arguments are invalid
     */
    public static Ip4Address valueOf(byte[] value, int offset) {
        IpAddress.checkArguments(VERSION, value, offset);
        byte[] bc = Arrays.copyOfRange(value, offset, value.length);
        return Ip4Address.valueOf(bc);
    }

    /**
     * Converts an InetAddress into an IPv4 address.
     *
     * @param inetAddress the InetAddress value to use. It must contain an IPv4
     * address
     * @return an IPv4 address
     * @throws IllegalArgumentException if the argument is invalid
     */
    public static Ip4Address valueOf(InetAddress inetAddress) {
        byte[] bytes = inetAddress.getAddress();
        if (inetAddress instanceof Inet4Address) {
            return new Ip4Address(bytes);
        }
        if ((inetAddress instanceof Inet6Address) ||
            (bytes.length == INET6_BYTE_LENGTH)) {
            final String msg = "Invalid IPv4 version address string: " +
                inetAddress.toString();
            throw new IllegalArgumentException(msg);
        }
        // Use the number of bytes as a hint
        if (bytes.length == INET_BYTE_LENGTH) {
            return new Ip4Address(bytes);
        }
        final String msg = "Unrecognized IP version address string: " +
            inetAddress.toString();
        throw new IllegalArgumentException(msg);
    }

    /**
     * Converts an IPv4 string literal (e.g., "10.2.3.4") into an IP address.
     *
     * @param value an IPv4 address value in string form
     * @return an IPv4 address
     * @throws IllegalArgumentException if the argument is invalid
     */
    public static Ip4Address valueOf(String value) {
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
     * Creates an IPv4 network mask prefix.
     *
     * @param prefixLength the length of the mask prefix. Must be in the
     * interval [0, 32]
     * @return a new IPv4 address that contains a mask prefix of the
     * specified length
     * @throws IllegalArgumentException if the argument is invalid
     */
    public static Ip4Address makeMaskPrefix(int prefixLength) {
        byte[] mask = IpAddress.makeMaskPrefixArray(VERSION, prefixLength);
        return new Ip4Address(mask);
    }

    public static Ip4Address makeMaskSuffix(int suffixLength) {
        byte[] mask = IpAddress.makeMaskSuffixArray(VERSION, suffixLength);
        return new Ip4Address(mask);
    }

    /**
     * Creates an IPv4 address by masking it with a network mask of given
     * mask length.
     *
     * @param address the address to mask
     * @param prefixLength the length of the mask prefix. Must be in the
     * interval [0, 32]
     * @return a new IPv4 address that is masked with a mask prefix of the
     * specified length
     * @throws IllegalArgumentException if the prefix length is invalid
     */
    public static Ip4Address makeMaskedAddress(final Ip4Address address,
                                               int prefixLength) {
        byte[] net = makeMaskedAddressArray(address, prefixLength);
        return Ip4Address.valueOf(net);
    }
}
