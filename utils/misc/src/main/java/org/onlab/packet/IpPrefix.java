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

import java.util.Objects;

/**
 * A class representing an IP prefix. A prefix consists of an IP address and
 * a subnet mask.
 * This class is immutable.
 * <p>
 * NOTE: The stored IP address in the result IP prefix is masked to
 * contain zeroes in all bits after the prefix length.
 * </p>
 */
public class IpPrefix {
    /**
     * Longest IPv4 network prefix.
     */
    public static final int MAX_INET_MASK_LENGTH = IpAddress.INET_BIT_LENGTH;
    /**
     * Longest IPv6 network prefix.
     */
    public static final int MAX_INET6_MASK_LENGTH = IpAddress.INET6_BIT_LENGTH;
    /**
     * An IpPrefix that contains all IPv4 multicast addresses.
     */
    public static final IpPrefix IPV4_MULTICAST_PREFIX = IpPrefix.valueOf("224.0.0.0/4");
    /**
     * An IpPrefix that contains all IPv6 multicast addresses.
     */
    public static final IpPrefix IPV6_MULTICAST_PREFIX = IpPrefix.valueOf("ff00::/8");
    /**
     * An IpPrefix that contains all IPv4 link local addresses.
     */
    public static final IpPrefix IPV4_LINK_LOCAL_PREFIX = IpPrefix.valueOf("169.254.0.0/16");
    /**
     * An IpPrefix that contains all IPv6 link local addresses.
     */
    public static final IpPrefix IPV6_LINK_LOCAL_PREFIX = IpPrefix.valueOf("fe80::/64");

    private final IpAddress address;
    private final short prefixLength;

    /**
     * Constructor for given IP address, and a prefix length.
     *
     * @param address the IP address
     * @param prefixLength the prefix length
     * @throws IllegalArgumentException if the prefix length value is invalid
     */
    protected IpPrefix(IpAddress address, int prefixLength) {
        checkPrefixLength(address.version(), prefixLength);
        this.address = IpAddress.makeMaskedAddress(address, prefixLength);
        this.prefixLength = (short) prefixLength;
    }

    /**
     * Default constructor for Kryo serialization.
     */
    protected IpPrefix() {
        this.address = null;
        this.prefixLength = 0;
    }

    /**
     * Returns the IP version of the prefix.
     *
     * @return the IP version of the prefix
     */
    public IpAddress.Version version() {
        return address.version();
    }

    /**
     * Tests whether the IP version of this prefix is IPv4.
     *
     * @return true if the IP version of this prefix is IPv4, otherwise false.
     */
    public boolean isIp4() {
        return address.isIp4();
    }

    /**
     * Tests whether the IP version of this prefix is IPv6.
     *
     * @return true if the IP version of this prefix is IPv6, otherwise false.
     */
    public boolean isIp6() {
        return address.isIp6();
    }

    /**
     * Check if this IP prefix is a multicast prefix.
     *
     * @return true if this prefix a multicast prefix
     */
    public boolean isMulticast() {
        return isIp4() ?
                IPV4_MULTICAST_PREFIX.contains(this.getIp4Prefix()) :
                IPV6_MULTICAST_PREFIX.contains(this.getIp6Prefix());
    }

    /**
     * Returns the IP address value of the prefix.
     *
     * @return the IP address value of the prefix
     */
    public IpAddress address() {
        return address;
    }

    /**
     * Returns the IP address prefix length.
     *
     * @return the IP address prefix length
     */
    public int prefixLength() {
        return prefixLength;
    }

    /**
     * Gets the {@link Ip4Prefix} view of the IP prefix.
     *
     * @return the {@link Ip4Prefix} view of the IP prefix if it is IPv4,
     * otherwise null
     */
    public Ip4Prefix getIp4Prefix() {
        if (!isIp4()) {
            return null;
        }

        // Return this object itself if it is already instance of Ip4Prefix
        if (this instanceof Ip4Prefix) {
            return (Ip4Prefix) this;
        }
        return Ip4Prefix.valueOf(address.getIp4Address(), prefixLength);
    }

    /**
     * Gets the {@link Ip6Prefix} view of the IP prefix.
     *
     * @return the {@link Ip6Prefix} view of the IP prefix if it is IPv6,
     * otherwise null
     */
    public Ip6Prefix getIp6Prefix() {
        if (!isIp6()) {
            return null;
        }

        // Return this object itself if it is already instance of Ip6Prefix
        if (this instanceof Ip6Prefix) {
            return (Ip6Prefix) this;
        }
        return Ip6Prefix.valueOf(address.getIp6Address(), prefixLength);
    }

    /**
     * Converts an integer and a prefix length into an IPv4 prefix.
     *
     * @param address an integer representing the IPv4 address
     * @param prefixLength the prefix length
     * @return an IP prefix
     * @throws IllegalArgumentException if the prefix length value is invalid
     */
    public static IpPrefix valueOf(int address, int prefixLength) {
        return new IpPrefix(IpAddress.valueOf(address), prefixLength);
    }

    /**
     * Converts a byte array and a prefix length into an IP prefix.
     *
     * @param version the IP address version
     * @param address the IP address value stored in network byte order
     * @param prefixLength the prefix length
     * @return an IP prefix
     * @throws IllegalArgumentException if the prefix length value is invalid
     */
    public static IpPrefix valueOf(IpAddress.Version version, byte[] address,
                                   int prefixLength) {
        return new IpPrefix(IpAddress.valueOf(version, address), prefixLength);
    }

    /**
     * Converts an IP address and a prefix length into an IP prefix.
     *
     * @param address the IP address
     * @param prefixLength the prefix length
     * @return an IP prefix
     * @throws IllegalArgumentException if the prefix length value is invalid
     */
    public static IpPrefix valueOf(IpAddress address, int prefixLength) {
        return new IpPrefix(address, prefixLength);
    }

    /**
     * Converts a CIDR (slash) notation string (e.g., "10.1.0.0/16" or
     * "1111:2222::/64") into an IP prefix.
     *
     * @param address an IP prefix in string form (e.g. "10.1.0.0/16" or
     * "1111:2222::/64")
     * @return an IP prefix
     * @throws IllegalArgumentException if the arguments are invalid
     */
    public static IpPrefix valueOf(String address) {
        final String[] parts = address.split("/");
        if (parts.length != 2) {
            String msg = "Malformed IP prefix string: " + address + ". " +
                "Address must take form \"x.x.x.x/y\" or " +
                "\"xxxx:xxxx:xxxx:xxxx:xxxx:xxxx:xxxx:xxxx/y\"";
            throw new IllegalArgumentException(msg);
        }
        IpAddress ipAddress = IpAddress.valueOf(parts[0]);
        int prefixLength = Integer.parseInt(parts[1]);

        return new IpPrefix(ipAddress, prefixLength);
    }

    /**
     * Determines whether a given IP prefix is contained within this prefix.
     *
     * @param other the IP prefix to test
     * @return true if the other IP prefix is contained in this prefix,
     * otherwise false
     */
    public boolean contains(IpPrefix other) {
        if (version() != other.version()) {
            return false;
        }

        if (this.prefixLength > other.prefixLength) {
            return false;               // This prefix has smaller prefix size
        }

        //
        // Mask the other address with my prefix length.
        // If the other prefix is within this prefix, the masked address must
        // be same as the address of this prefix.
        //
        IpAddress maskedAddr =
            IpAddress.makeMaskedAddress(other.address, this.prefixLength);
        return this.address.equals(maskedAddr);
    }

    /**
     * Determines whether a given IP address is contained within this prefix.
     *
     * @param other the IP address to test
     * @return true if the IP address is contained in this prefix, otherwise
     * false
     */
    public boolean contains(IpAddress other) {
        if (version() != other.version()) {
            return false;
        }

        //
        // Mask the other address with my prefix length.
        // If the other prefix is within this prefix, the masked address must
        // be same as the address of this prefix.
        //
        IpAddress maskedAddr =
            IpAddress.makeMaskedAddress(other, this.prefixLength);
        return this.address.equals(maskedAddr);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, prefixLength);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (!(obj instanceof IpPrefix))) {
            return false;
        }
        IpPrefix other = (IpPrefix) obj;
        return ((prefixLength == other.prefixLength) &&
                address.equals(other.address));
    }

    @Override
    /*
     * (non-Javadoc)
     * The format is "x.x.x.x/y" for IPv4 prefixes.
     *
     * @see java.lang.Object#toString()
     */
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append(address.toString());
        builder.append("/");
        builder.append(String.format("%d", prefixLength));
        return builder.toString();
    }

    /**
     * Checks whether the prefix length is valid.
     *
     * @param version the IP address version
     * @param prefixLength the prefix length value to check
     * @throws IllegalArgumentException if the prefix length value is invalid
     */
    private static void checkPrefixLength(IpAddress.Version version,
                                          int prefixLength) {
        int maxPrefixLen = 0;

        switch (version) {
        case INET:
            maxPrefixLen = MAX_INET_MASK_LENGTH;
            break;
        case INET6:
            maxPrefixLen = MAX_INET6_MASK_LENGTH;
            break;
        default:
            String msg = "Invalid IP version " + version;
            throw new IllegalArgumentException(msg);
        }

        if ((prefixLength < 0) || (prefixLength > maxPrefixLen)) {
            String msg = "Invalid prefix length " + prefixLength + ". " +
                "The value must be in the interval [0, " +
                maxPrefixLen + "]";
            throw new IllegalArgumentException(msg);
        }
    }
}
