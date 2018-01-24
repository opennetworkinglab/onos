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

import com.google.common.net.InetAddresses;
import com.google.common.primitives.UnsignedBytes;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;


/**
 * A class representing an IP address.
 * This class is immutable.
 */
public class IpAddress implements Comparable<IpAddress> {
    private static final int BIT_MASK = 0x000000ff;

    // IP Versions
    public enum Version { INET, INET6 }

    // lengths of address, in bytes
    public static final int INET_BYTE_LENGTH = 4;
    public static final int INET_BIT_LENGTH = INET_BYTE_LENGTH * Byte.SIZE;
    public static final int INET6_BYTE_LENGTH = 16;
    public static final int INET6_BIT_LENGTH = INET6_BYTE_LENGTH * Byte.SIZE;

    private final Version version;
    private final byte[] octets;

    /**
     * Constructor for given IP address version and address octets.
     *
     * @param version the IP address version
     * @param value the IP address value stored in network byte order
     * (i.e., the most significant byte first)
     * @throws IllegalArgumentException if the arguments are invalid
     */
    protected IpAddress(Version version, byte[] value) {
        checkArguments(version, value, 0);
        this.version = version;
        switch (version) {
        case INET:
            this.octets = Arrays.copyOf(value, INET_BYTE_LENGTH);
            break;
        case INET6:
            this.octets = Arrays.copyOf(value, INET6_BYTE_LENGTH);
            break;
        default:
            // Should not be reached
            this.octets = null;
            break;
        }
    }

    /**
     * Default constructor for Kryo serialization.
     */
    protected IpAddress() {
        this.version = null;
        this.octets = null;
    }

    /**
     * Returns the IP version of this address.
     *
     * @return the version
     */
    public Version version() {
        return this.version;
    }

    /**
     * Tests whether the IP version of this address is IPv4.
     *
     * @return true if the IP version of this address is IPv4, otherwise false.
     */
    public boolean isIp4() {
        return (version() == Ip4Address.VERSION);
    }

    /**
     * Tests whether the IP version of this address is IPv6.
     *
     * @return true if the IP version of this address is IPv6, otherwise false.
     */
    public boolean isIp6() {
        return (version() == Ip6Address.VERSION);
    }

    /**
     * Gets the {@link Ip4Address} view of the IP address.
     *
     * @return the {@link Ip4Address} view of the IP address if it is IPv4,
     * otherwise null
     */
    public Ip4Address getIp4Address() {
        if (!isIp4()) {
            return null;
        }

        // Return this object itself if it is already instance of Ip4Address
        if (this instanceof Ip4Address) {
            return (Ip4Address) this;
        }
        return Ip4Address.valueOf(octets);
    }

    /**
     * Gets the {@link Ip6Address} view of the IP address.
     *
     * @return the {@link Ip6Address} view of the IP address if it is IPv6,
     * otherwise null
     */
    public Ip6Address getIp6Address() {
        if (!isIp6()) {
            return null;
        }

        // Return this object itself if it is already instance of Ip6Address
        if (this instanceof Ip6Address) {
            return (Ip6Address) this;
        }
        return Ip6Address.valueOf(octets);
    }

    /**
     * Returns the IP address as a byte array.
     *
     * @return a byte array
     */
    public byte[] toOctets() {
        return Arrays.copyOf(octets, octets.length);
    }

    /**
     * Returns the IP address as InetAddress.
     *
     * @return InetAddress
     */
    public InetAddress toInetAddress() {
        try {
            return InetAddress.getByAddress(octets);
        } catch (UnknownHostException e) {
            // Should never reach here
            return null;
        }
    }

    /**
     * Computes the IP address byte length for a given IP version.
     *
     * @param version the IP version
     * @return the IP address byte length for the IP version
     * @throws IllegalArgumentException if the IP version is invalid
     */
    public static int byteLength(Version version) {
        switch (version) {
        case INET:
            return INET_BYTE_LENGTH;
        case INET6:
            return INET6_BYTE_LENGTH;
        default:
            String msg = "Invalid IP version " + version;
            throw new IllegalArgumentException(msg);
        }
    }

    /**
     * Converts an integer into an IPv4 address.
     *
     * @param value an integer representing an IPv4 address value
     * @return an IP address
     */
    public static IpAddress valueOf(int value) {
        byte[] bytes =
            ByteBuffer.allocate(INET_BYTE_LENGTH).putInt(value).array();
        return new IpAddress(Version.INET, bytes);
    }

    /**
     * Converts a byte array into an IP address.
     *
     * @param version the IP address version
     * @param value the IP address value stored in network byte order
     * (i.e., the most significant byte first)
     * @return an IP address
     * @throws IllegalArgumentException if the arguments are invalid
     */
    public static IpAddress valueOf(Version version, byte[] value) {
        return new IpAddress(version, value);
    }

    /**
     * Converts a byte array and a given offset from the beginning of the
     * array into an IP address.
     * <p>
     * The IP address is stored in network byte order (i.e., the most
     * significant byte first).
     * </p>
     * @param version the IP address version
     * @param value the value to use
     * @param offset the offset in bytes from the beginning of the byte array
     * @return an IP address
     * @throws IllegalArgumentException if the arguments are invalid
     */
    public static IpAddress valueOf(Version version, byte[] value,
                                    int offset) {
        checkArguments(version, value, offset);
        byte[] bc = Arrays.copyOfRange(value, offset, value.length);
        return IpAddress.valueOf(version, bc);
    }

    /**
     * Converts an InetAddress into an IP address.
     *
     * @param inetAddress the InetAddress value to use
     * @return an IP address
     * @throws IllegalArgumentException if the argument is invalid
     */
    public static IpAddress valueOf(InetAddress inetAddress) {
        byte[] bytes = inetAddress.getAddress();
        if (inetAddress instanceof Inet4Address) {
            return new IpAddress(Version.INET, bytes);
        }
        if (inetAddress instanceof Inet6Address) {
            return new IpAddress(Version.INET6, bytes);
        }
        // Use the number of bytes as a hint
        if (bytes.length == INET_BYTE_LENGTH) {
            return new IpAddress(Version.INET, bytes);
        }
        if (bytes.length == INET6_BYTE_LENGTH) {
            return new IpAddress(Version.INET6, bytes);
        }
        final String msg = "Unrecognized IP version address string: " +
            inetAddress.toString();
        throw new IllegalArgumentException(msg);
    }

    /**
     * Converts an IPv4 or IPv6 string literal (e.g., "10.2.3.4" or
     * "1111:2222::8888") into an IP address.
     *
     * @param value an IP address value in string form
     * @return an IP address
     * @throws IllegalArgumentException if the argument is invalid
     */
    public static IpAddress valueOf(String value) {
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
     * Creates an IP network mask prefix.
     *
     * @param version the IP address version
     * @param prefixLength the length of the mask prefix. Must be in the
     * interval [0, 32] for IPv4, or [0, 128] for IPv6
     * @return a new IP address that contains a mask prefix of the
     * specified length
     * @throws IllegalArgumentException if the arguments are invalid
     */
    public static IpAddress makeMaskPrefix(Version version, int prefixLength) {
        byte[] mask = makeMaskPrefixArray(version, prefixLength);
        return new IpAddress(version, mask);
    }

    /**
     * Creates an IP address by masking it with a network mask of given
     * mask length.
     *
     * @param address the address to mask
     * @param prefixLength the length of the mask prefix. Must be in the
     * interval [0, 32] for IPv4, or [0, 128] for IPv6
     * @return a new IP address that is masked with a mask prefix of the
     * specified length
     * @throws IllegalArgumentException if the prefix length is invalid
     */
    public static IpAddress makeMaskedAddress(final IpAddress address,
                                              int prefixLength) {
        if (address instanceof Ip4Address) {
            Ip4Address ip4a = (Ip4Address) address;
            return Ip4Address.makeMaskedAddress(ip4a, prefixLength);
        } else if (address instanceof Ip6Address) {
            Ip6Address ip6a = (Ip6Address) address;
            return Ip6Address.makeMaskedAddress(ip6a, prefixLength);
        } else {
            byte[] net = makeMaskedAddressArray(address, prefixLength);
            return IpAddress.valueOf(address.version(), net);
        }
    }

    /**
     * Check if this IP address is zero.
     *
     * @return true if this address is zero
     */
    public boolean isZero() {
        for (byte b : octets) {
            if (b != 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check if this IP address is self-assigned.
     *
     * @return true if this address is self-assigned
     */
    public boolean isSelfAssigned() {
        return isIp4() && octets[0] == (byte) 169 && octets[1] == (byte) 254;
    }

    /**
     * Check if this IP address is a multicast address.
     *
     * @return true if this address is a multicast address
     */
    public boolean isMulticast() {
        return isIp4() ?
                Ip4Prefix.IPV4_MULTICAST_PREFIX.contains(this.getIp4Address()) :
                Ip6Prefix.IPV6_MULTICAST_PREFIX.contains(this.getIp6Address());
    }

    /**
     * Check if this IP address is a link-local address.
     *
     * @return true if this address is a link-local address
     */
    public boolean isLinkLocal() {
        return isIp4() ?
                Ip4Prefix.IPV4_LINK_LOCAL_PREFIX.contains(this.getIp4Address()) :
                Ip6Prefix.IPV6_LINK_LOCAL_PREFIX.contains(this.getIp6Address());
    }

    @Override
    public int compareTo(IpAddress o) {
        // Compare first the version
        if (this.version != o.version) {
            return this.version.compareTo(o.version);
        }

        // Compare the bytes, one-by-one
        for (int i = 0; i < this.octets.length; i++) {
            if (this.octets[i] != o.octets[i]) {
                return UnsignedBytes.compare(this.octets[i], o.octets[i]);
            }
        }
        return 0;       // Equal
    }

    @Override
    public int hashCode() {
        return Objects.hash(version, Arrays.hashCode(octets));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (!(obj instanceof IpAddress))) {
            return false;
        }
        IpAddress other = (IpAddress) obj;
        return (version == other.version) &&
            Arrays.equals(octets, other.octets);
    }

    @Override
    /*
     * (non-Javadoc)
     * The string representation of the IP address: "x.x.x.x" for IPv4
     * addresses, or ':' separated string for IPv6 addresses.
     *
     * @see java.lang.Object#toString()
     */
    public String toString() {
        // FIXME InetAddress is super slow
        switch (version) {
            case INET:
                return String.format("%d.%d.%d.%d", octets[0] & 0xff,
                        octets[1] & 0xff,
                        octets[2] & 0xff,
                        octets[3] & 0xff);
            case INET6:
            default:
                return ipv6ToStringHelper();
        }
    }

    /**
     * Generates an IP prefix.
     *
     * @return the IP prefix of the IP address
     */
    public IpPrefix toIpPrefix() {

        if (isIp4()) {
            return IpPrefix.valueOf(new IpAddress(Version.INET, octets),
                                    Ip4Address.BIT_LENGTH);
        } else {
            return IpPrefix.valueOf(new IpAddress(Version.INET6, octets),
                                    Ip6Address.BIT_LENGTH);
        }
    }

    /**
     * Gets the IP address name for the IP address version.
     *
     * @param version the IP address version
     * @return the IP address name for the IP address version
     */
    private static String addressName(Version version) {
        switch (version) {
        case INET:
            return "IPv4";
        case INET6:
            return "IPv6";
        default:
            break;
        }
        return "UnknownIP(" + version + ")";
    }

    /**
     * Checks whether the arguments are valid.
     *
     * @param version the IP address version
     * @param value the IP address value stored in a byte array
     * @param offset the offset in bytes from the beginning of the byte
     * array with the address
     * @throws IllegalArgumentException if any of the arguments is invalid
     */
    static void checkArguments(Version version, byte[] value, int offset) {
        // Check the offset and byte array length
        int addrByteLength = byteLength(version);
        if ((offset < 0) || (offset + addrByteLength > value.length)) {
            String msg;
            if (value.length < addrByteLength) {
                msg = "Invalid " + addressName(version) +
                    " address array: array length: " + value.length +
                    ". Must be at least " + addrByteLength;
            } else {
                msg = "Invalid " + addressName(version) +
                    " address array: array offset: " + offset +
                    ". Must be in the interval [0, " +
                    (value.length - addrByteLength) + "]";
            }
            throw new IllegalArgumentException(msg);
        }
    }

    /**
     * Creates a byte array for IP network mask prefix.
     *
     * @param version the IP address version
     * @param prefixLength the length of the mask prefix. Must be in the
     * interval [0, 32] for IPv4, or [0, 128] for IPv6
     * @return a byte array that contains a mask prefix of the
     * specified length
     * @throws IllegalArgumentException if the arguments are invalid
     */
    static byte[] makeMaskPrefixArray(Version version, int prefixLength) {
        int addrByteLength = byteLength(version);
        int addrBitLength = addrByteLength * Byte.SIZE;

        // Verify the prefix length
        if ((prefixLength < 0) || (prefixLength > addrBitLength)) {
            final String msg = "Invalid IP prefix length: " + prefixLength +
                ". Must be in the interval [0, " + addrBitLength + "].";
            throw new IllegalArgumentException(msg);
        }

        // Number of bytes and extra bits that should be all 1s
        int maskBytes = prefixLength / Byte.SIZE;
        int maskBits = prefixLength % Byte.SIZE;
        byte[] mask = new byte[addrByteLength];

        // Set the bytes and extra bits to 1s
        for (int i = 0; i < maskBytes; i++) {
            mask[i] = (byte) 0xff;              // Set mask bytes to 1s
        }
        for (int i = maskBytes; i < addrByteLength; i++) {
            mask[i] = 0;                        // Set remaining bytes to 0s
        }
        if (maskBits > 0) {
            mask[maskBytes] = (byte) (0xff << (Byte.SIZE - maskBits));
        }
        return mask;
    }

    /**
     * Creates a byte array that represents an IP address masked with
     * a network mask of given mask length.
     *
     * @param addr the address to mask
     * @param prefixLength the length of the mask prefix. Must be in the
     * interval [0, 32] for IPv4, or [0, 128] for IPv6
     * @return a byte array that represents the IP address masked with
     * a mask prefix of the specified length
     * @throws IllegalArgumentException if the prefix length is invalid
     */
    static byte[] makeMaskedAddressArray(final IpAddress addr,
                                         int prefixLength) {
        byte[] mask = IpAddress.makeMaskPrefixArray(addr.version(),
                                                    prefixLength);
        byte[] net = new byte[mask.length];

        // Mask each byte
        for (int i = 0; i < net.length; i++) {
            net[i] = (byte) (addr.octets[i] & mask[i]);
        }
        return net;
    }

    /**
     * Creates a string based on the IPv6 recommendations for canonical representations found here:
     * https://tools.ietf.org/html/rfc5952#section-1.
     * @return A properly formatted IPv6 canonical representation.
     */
    private String ipv6ToStringHelper() {
        //Populate a buffer with the string of the full address with leading zeros stripped
        StringBuilder buff = new StringBuilder();
        buff.append(String.format("%x:%x:%x:%x:%x:%x:%x:%x",
                (((octets[0] & BIT_MASK) << 8) | (octets[1] & BIT_MASK)),
                (((octets[2] & BIT_MASK) << 8) | (octets[3] & BIT_MASK)),
                (((octets[4] & BIT_MASK) << 8) | (octets[5] & BIT_MASK)),
                (((octets[6] & BIT_MASK) << 8) | (octets[7] & BIT_MASK)),
                (((octets[8] & BIT_MASK) << 8) | (octets[9] & BIT_MASK)),
                (((octets[10] & BIT_MASK) << 8) | (octets[11] & BIT_MASK)),
                (((octets[12] & BIT_MASK) << 8) | (octets[13] & BIT_MASK)),
                (((octets[14] & BIT_MASK) << 8) | (octets[15] & BIT_MASK))));
        //Initialize variables for tracking longest zero subsequence, tiebreaking by first occurence
        int longestSeqStart, longestSeqLen, currSeqStart, currSeqLen;
        longestSeqStart = 0;
        longestSeqLen = 0;
        currSeqStart = 0;
        currSeqLen = 0;

        for (int index = 0; index < buff.length(); index++) {
            if (buff.charAt(index) == ':') {
                if (currSeqLen != 0 && buff.charAt(index + 1) == '0') {
                    currSeqLen += 1;
                }
            } else if (buff.charAt(index) == '0' && ((index == 0) || (buff.charAt(index - 1) == ':'))) {
                if (currSeqLen == 0) {
                    currSeqStart = index;
                }
                currSeqLen += 1;
            } else {
                 if (currSeqLen > longestSeqLen) {
                     longestSeqStart = currSeqStart;
                     longestSeqLen = currSeqLen;
                }
                currSeqLen = 0;
            }
        }

        if (currSeqLen > longestSeqLen) {
            longestSeqLen = currSeqLen;
            longestSeqStart = currSeqStart;
        }
        if (longestSeqLen > 1) {
            if (buff.length() == (longestSeqStart + longestSeqLen)) {
                buff.append(':');
            }

            buff.delete(longestSeqStart, longestSeqStart + longestSeqLen);

            if (longestSeqStart == 0) {
                buff.insert(0, ':');
            }
        }

    return buff.toString();
    }
}
