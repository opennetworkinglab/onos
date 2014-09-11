package org.onlab.packet;

import java.util.Arrays;

/**
 * A class representing an IPv4 address.
 */
public class IPAddress {

    //IP Versions
    public enum Version { INET, INET6 };

    //lengths of addresses, in bytes
    public static final int INET_LEN = 4;
    public static final int INET6_LEN = 6;

    protected Version version;
    //does it make more sense to have a integral address?
    protected byte[] octets;

    protected IPAddress(Version ver, byte[] octets) {
        this.version = ver;
        this.octets = Arrays.copyOf(octets, INET_LEN);
    }

    /**
     * Converts a byte array into an IP address.
     *
     * @param address a byte array
     * @return an IP address
     */
    public static IPAddress valueOf(byte [] address) {
        return new IPAddress(Version.INET, address);
    }

    /**
     * Converts an integer into an IPv4 address.
     *
     * @param address an integer representing an IP value
     * @return an IP address
     */
    public static IPAddress valueOf(int address) {
        byte [] bytes = new byte [] {
                (byte) ((address >> 24) & 0xff),
                (byte) ((address >> 16) & 0xff),
                (byte) ((address >> 8) & 0xff),
                (byte) ((address >> 0) & 0xff)
        };
        return new IPAddress(Version.INET, bytes);
    }

    /**
     * Converts a string in dotted-decimal notation (x.x.x.x) into
     * an IPv4 address.
     *
     * @param address a string representing an IP address, e.g. "10.0.0.1"
     * @return an IP address
     */
    public static IPAddress valueOf(String address) {
        final String [] parts = address.split(".");
        if (parts.length != INET_LEN) {
            throw new IllegalArgumentException("Malformed IP address string; "
                    + "Addres must have four decimal values separated by dots (.)");
        }
        final byte [] bytes = new byte[INET_LEN];
        for (int i = 0; i < INET_LEN; i++) {
            bytes[i] = Byte.parseByte(parts[i], 10);
        }
        return new IPAddress(Version.INET, bytes);
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
    public byte [] toOctets() {
        return Arrays.copyOf(this.octets, INET_LEN);
    }

    public int toInt() {
        int address =
                ((octets[0] << 24) |
                (octets[1] << 16) |
                (octets[2] << 8) |
                (octets[3] << 0));
        return address;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        for (final byte b : this.octets) {
            if (builder.length() > 0) {
                builder.append(".");
            }
            builder.append(String.format("%02d", b));
        }
        return builder.toString();
    }

    @Override
    public int hashCode() {
        return octets.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof IPAddress) {
            IPAddress other = (IPAddress) obj;
            if (!(this.version.equals(other.version))) {
                return false;
            }
            if (!(Arrays.equals(this.octets, other.octets))) {
                return false;
            }
        }
        return true;
    }
}
