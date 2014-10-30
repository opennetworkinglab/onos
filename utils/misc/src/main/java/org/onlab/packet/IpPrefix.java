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

import java.util.Arrays;

/**
 * A class representing an IPv4 prefix.
 * <p/>
 * A prefix consists of an IP address and a subnet mask.
 */
public final class IpPrefix {

    // TODO a comparator for netmasks? E.g. for sorting by prefix match order.

    // IP Versions: IPv4 and IPv6
    public enum Version { INET, INET6 };

    // Maximum network mask length
    public static final int MAX_INET_MASK_LENGTH = IpAddress.INET_BIT_LENGTH;
    public static final int MAX_INET6_MASK_LENGTH = IpAddress.INET6_BIT_LENGTH;

    //no mask (no network), e.g. a simple address
    private static final int DEFAULT_MASK = 0;

    /**
     * Default value indicating an unspecified address.
     */
    private static final byte[] ANY = new byte[] {0, 0, 0, 0};

    private final Version version;
    private final byte[] octets;
    private final int netmask;

    /**
     * Constructor for given IP address version, prefix address octets,
     * and network mask length.
     *
     * @param ver the IP address version
     * @param octets the IP prefix address octets
     * @param netmask the network mask length
     */
    private IpPrefix(Version ver, byte[] octets, int netmask) {
        this.version = ver;
        this.octets = Arrays.copyOf(octets, IpAddress.INET_BYTE_LENGTH);
        this.netmask = netmask;
    }

    /**
     * Converts a byte array into an IP address.
     *
     * @param address a byte array
     * @param netmask the CIDR value subnet mask
     * @return an IP address
     */
    public static IpPrefix valueOf(byte[] address, int netmask) {
        return new IpPrefix(Version.INET, address, netmask);
    }

    /**
     * Helper to convert an integer into a byte array.
     *
     * @param address the integer to convert
     * @return a byte array
     */
    private static byte[] bytes(int address) {
        byte[] bytes = new byte [IpAddress.INET_BYTE_LENGTH];
        for (int i = 0; i < IpAddress.INET_BYTE_LENGTH; i++) {
            bytes[i] = (byte) ((address >> (IpAddress.INET_BYTE_LENGTH
                                            - (i + 1)) * 8) & 0xff);
        }

        return bytes;
    }

    /**
     * Converts an integer into an IPv4 address.
     *
     * @param address an integer representing an IP value
     * @param netmask the CIDR value subnet mask
     * @return an IP address
     */
    public static IpPrefix valueOf(int address, int netmask) {
        return new IpPrefix(Version.INET, bytes(address), netmask);
    }

    /**
     * Converts a dotted-decimal string (x.x.x.x) into an IPv4 address. The
     * string can also be in CIDR (slash) notation. If the netmask is omitted,
     * it will be set to DEFAULT_MASK (0).
     *
     * @param address a IP address in string form, e.g. "10.0.0.1", "10.0.0.1/24"
     * @return an IP address
     */
    public static IpPrefix valueOf(String address) {

        final String[] parts = address.split("\\/");
        if (parts.length > 2) {
            throw new IllegalArgumentException("Malformed IP address string; "
                    + "Address must take form \"x.x.x.x\" or \"x.x.x.x/y\"");
        }

        int mask = DEFAULT_MASK;
        if (parts.length == 2) {
            mask = Integer.parseInt(parts[1]);
            if (mask > MAX_INET_MASK_LENGTH) {
                throw new IllegalArgumentException(
                        "Value of subnet mask cannot exceed "
                                + MAX_INET_MASK_LENGTH);
            }
        }

        final String[] net = parts[0].split("\\.");
        if (net.length != IpAddress.INET_BYTE_LENGTH) {
            throw new IllegalArgumentException("Malformed IP address string; "
                    + "Address must have four decimal values separated by dots (.)");
        }
        final byte[] bytes = new byte[IpAddress.INET_BYTE_LENGTH];
        for (int i = 0; i < IpAddress.INET_BYTE_LENGTH; i++) {
            bytes[i] = (byte) Short.parseShort(net[i], 10);
        }
        return new IpPrefix(Version.INET, bytes, mask);
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
     * Returns the IP address as a byte array.
     *
     * @return a byte array
     */
    public byte[] toOctets() {
        return Arrays.copyOf(this.octets, IpAddress.INET_BYTE_LENGTH);
    }

    /**
     * Returns the IP address prefix length.
     *
     * @return prefix length
     */
    public int prefixLength() {
        return netmask;
    }

    /**
     * Returns the integral value of this IP address.
     *
     * @return the IP address's value as an integer
     */
    public int toInt() {
        int val = 0;
        for (int i = 0; i < octets.length; i++) {
          val <<= 8;
          val |= octets[i] & 0xff;
        }
        return val;
    }

    /**
     * Helper for computing the mask value from CIDR.
     *
     * @return an integer bitmask
     */
    private int mask() {
        int shift = MAX_INET_MASK_LENGTH - this.netmask;
        return ((Integer.MAX_VALUE >>> (shift - 1)) << shift);
    }

    /**
     * Returns the subnet mask in IpAddress form.
     *
     * @return the subnet mask as an IpAddress
     */
    public IpAddress netmask() {
        return IpAddress.valueOf(mask());
    }

    /**
     * Returns the network portion of this address as an IpAddress.
     * The netmask of the returned IpAddress is the current mask. If this
     * address doesn't have a mask, this returns an all-0 IpAddress.
     *
     * @return the network address or null
     */
    public IpPrefix network() {
        if (netmask == DEFAULT_MASK) {
            return new IpPrefix(version, ANY, DEFAULT_MASK);
        }

        byte[] net = new byte [4];
        byte[] mask = bytes(mask());
        for (int i = 0; i < IpAddress.INET_BYTE_LENGTH; i++) {
            net[i] = (byte) (octets[i] & mask[i]);
        }
        return new IpPrefix(version, net, netmask);
    }

    /**
     * Returns the host portion of the IPAddress, as an IPAddress.
     * The netmask of the returned IpAddress is the current mask. If this
     * address doesn't have a mask, this returns a copy of the current
     * address.
     *
     * @return the host address
     */
    public IpPrefix host() {
        if (netmask == DEFAULT_MASK) {
            new IpPrefix(version, octets, netmask);
        }

        byte[] host = new byte [IpAddress.INET_BYTE_LENGTH];
        byte[] mask = bytes(mask());
        for (int i = 0; i < IpAddress.INET_BYTE_LENGTH; i++) {
            host[i] = (byte) (octets[i] & ~mask[i]);
        }
        return new IpPrefix(version, host, netmask);
    }

    /**
     * Returns an IpAddress of the bytes contained in this prefix.
     * FIXME this is a hack for now and only works because IpPrefix doesn't
     * mask the input bytes on creation.
     *
     * @return the IpAddress
     */
    public IpAddress toIpAddress() {
        return IpAddress.valueOf(octets);
    }

    public boolean isMasked() {
        return mask() != 0;
    }

    /**
     * Determines whether a given address is contained within this IpAddress'
     * network.
     *
     * @param other another IP address that could be contained in this network
     * @return true if the other IP address is contained in this address'
     * network, otherwise false
     */
    public boolean contains(IpPrefix other) {
        if (this.netmask <= other.netmask) {
            // Special case where they're both /32 addresses
            if (this.netmask == MAX_INET_MASK_LENGTH) {
                return Arrays.equals(octets, other.octets);
            }

            // Mask the other address with our network mask
            IpPrefix otherMasked =
                    IpPrefix.valueOf(other.octets, netmask).network();

            return network().equals(otherMasked);
        }
        return false;
    }

    public boolean contains(IpAddress address) {
        // Need to get the network address because prefixes aren't automatically
        // masked on creation
        IpPrefix meMasked = network();

        IpPrefix otherMasked =
            IpPrefix.valueOf(address.toOctets(), netmask).network();

        return Arrays.equals(meMasked.octets, otherMasked.octets);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + netmask;
        result = prime * result + Arrays.hashCode(octets);
        result = prime * result + ((version == null) ? 0 : version.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        IpPrefix other = (IpPrefix) obj;
        if (netmask != other.netmask) {
            return false;
        }
        // TODO not quite right until we mask the input
        if (!Arrays.equals(octets, other.octets)) {
            return false;
        }
        if (version != other.version) {
            return false;
        }
        return true;
    }

    @Override
    /*
     * (non-Javadoc)
     * format is "x.x.x.x" for non-masked (netmask 0) addresses,
     * and "x.x.x.x/y" for masked addresses.
     *
     * @see java.lang.Object#toString()
     */
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        for (final byte b : this.octets) {
            if (builder.length() > 0) {
                builder.append(".");
            }
            builder.append(String.format("%d", b & 0xff));
        }
        if (netmask != DEFAULT_MASK) {
            builder.append("/");
            builder.append(String.format("%d", netmask));
        }
        return builder.toString();
    }
}
