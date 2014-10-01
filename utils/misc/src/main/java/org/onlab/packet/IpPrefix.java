package org.onlab.packet;

import java.util.Arrays;

/**
 * A class representing an IPv4 prefix.
 * <p/>
 * A prefix consists of an IP address and a subnet mask.
 */
public final class IpPrefix {

    // TODO a comparator for netmasks? E.g. for sorting by prefix match order.

    //IP Versions
    public enum Version { INET, INET6 };

    //lengths of address, in bytes
    public static final int INET_LEN = 4;
    public static final int INET6_LEN = 16;

    //maximum CIDR value
    public static final int MAX_INET_MASK = 32;
    //no mask (no network), e.g. a simple address
    public static final int DEFAULT_MASK = 0;

    /**
     * Default value indicating an unspecified address.
     */
    static final byte[] ANY = new byte [] {0, 0, 0, 0};

    protected Version version;

    protected byte[] octets;
    protected int netmask;

    private IpPrefix(Version ver, byte[] octets, int netmask) {
        this.version = ver;
        this.octets = Arrays.copyOf(octets, INET_LEN);
        this.netmask = netmask;
    }

    private IpPrefix(Version ver, byte[] octets) {
        this.version = ver;
        this.octets = Arrays.copyOf(octets, INET_LEN);
        this.netmask = DEFAULT_MASK;
    }

    /**
     * Converts a byte array into an IP address.
     *
     * @param address a byte array
     * @return an IP address
     */
    public static IpPrefix valueOf(byte [] address) {
        return new IpPrefix(Version.INET, address);
    }

    /**
     * Converts a byte array into an IP address.
     *
     * @param address a byte array
     * @param netmask the CIDR value subnet mask
     * @return an IP address
     */
    public static IpPrefix valueOf(byte [] address, int netmask) {
        return new IpPrefix(Version.INET, address, netmask);
    }

    /**
     * Helper to convert an integer into a byte array.
     *
     * @param address the integer to convert
     * @return a byte array
     */
    private static byte [] bytes(int address) {
        byte [] bytes = new byte [INET_LEN];
        for (int i = 0; i < INET_LEN; i++) {
            bytes[i] = (byte) ((address >> (INET_LEN - (i + 1)) * 8) & 0xff);
        }

        return bytes;
    }

    /**
     * Converts an integer into an IPv4 address.
     *
     * @param address an integer representing an IP value
     * @return an IP address
     */
    public static IpPrefix valueOf(int address) {
        return new IpPrefix(Version.INET, bytes(address));
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

        final String [] parts = address.split("\\/");
        if (parts.length > 2) {
            throw new IllegalArgumentException("Malformed IP address string; "
                    + "Address must take form \"x.x.x.x\" or \"x.x.x.x/y\"");
        }

        int mask = DEFAULT_MASK;
        if (parts.length == 2) {
            mask = Integer.valueOf(parts[1]);
            if (mask > MAX_INET_MASK) {
                throw new IllegalArgumentException(
                        "Value of subnet mask cannot exceed "
                                + MAX_INET_MASK);
            }
        }

        final String [] net = parts[0].split("\\.");
        if (net.length != INET_LEN) {
            throw new IllegalArgumentException("Malformed IP address string; "
                    + "Address must have four decimal values separated by dots (.)");
        }
        final byte [] bytes = new byte[INET_LEN];
        for (int i = 0; i < INET_LEN; i++) {
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
        return Arrays.copyOf(this.octets, INET_LEN);
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
        int address = 0;
        for (int i = 0; i < INET_LEN; i++) {
            address |= octets[i] << ((INET_LEN - (i + 1)) * 8);
        }
        return address;
    }

    public int toRealInt() {
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
        int shift = MAX_INET_MASK - this.netmask;
        return ((Integer.MAX_VALUE >>> (shift - 1)) << shift);
    }

    /**
     * Returns the subnet mask in IpAddress form. The netmask value for
     * the returned IpAddress is 0, as the address itself is a mask.
     *
     * @return the subnet mask
     */
    public IpPrefix netmask() {
        return new IpPrefix(Version.INET, bytes(mask()));
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

        byte [] net = new byte [4];
        byte [] mask = bytes(mask());
        for (int i = 0; i < INET_LEN; i++) {
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

        byte [] host = new byte [INET_LEN];
        byte [] mask = bytes(mask());
        for (int i = 0; i < INET_LEN; i++) {
            host[i] = (byte) (octets[i] & ~mask[i]);
        }
        return new IpPrefix(version, host, netmask);
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
            if (this.netmask == MAX_INET_MASK) {
                return Arrays.equals(octets, other.octets);
            }

            // Mask the other address with our network mask
            IpPrefix otherMasked =
                    IpPrefix.valueOf(other.octets, netmask).network();

            return network().equals(otherMasked);
        }
        return false;
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
