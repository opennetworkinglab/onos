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

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * A class representing an IPv4 address.
 */
public final class IpAddress implements Comparable<IpAddress> {
    // IP Versions
    public enum Version { INET, INET6 };

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
     * @param ver the IP address version
     * @param octets the IP address octets
     */
    private IpAddress(Version ver, byte[] octets) {
        this.version = ver;
        this.octets = Arrays.copyOf(octets, INET_BYTE_LENGTH);
    }

    /**
     * Converts a byte array into an IP address.
     *
     * @param address the IP address value stored in network byte order
     * (i.e., the most significant byte first)
     * @return an IP address
     */
    public static IpAddress valueOf(byte[] address) {
        return new IpAddress(Version.INET, address);
    }

    /**
     * Converts an integer into an IPv4 address.
     *
     * @param address an integer representing an IPv4 value
     * @return an IP address
     */
    public static IpAddress valueOf(int address) {
        byte[] bytes =
            ByteBuffer.allocate(INET_BYTE_LENGTH).putInt(address).array();
        return new IpAddress(Version.INET, bytes);
    }

    /**
     * Converts a dotted-decimal string (x.x.x.x) into an IPv4 address.
     *
     * @param address a IP address in string form, e.g. "10.0.0.1".
     * @return an IP address
     */
    public static IpAddress valueOf(String address) {
        final String[] net = address.split("\\.");
        if (net.length != INET_BYTE_LENGTH) {
            throw new IllegalArgumentException("Malformed IP address string; "
                    + "Address must have four decimal values separated by dots (.)");
        }
        final byte[] bytes = new byte[INET_BYTE_LENGTH];
        for (int i = 0; i < INET_BYTE_LENGTH; i++) {
            bytes[i] = (byte) Short.parseShort(net[i], 10);
        }
        return new IpAddress(Version.INET, bytes);
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
        return Arrays.copyOf(this.octets, INET_BYTE_LENGTH);
    }

    /**
     * Returns the integral value of this IP address.
     *
     * @return the IP address's value as an integer
     */
    public int toInt() {
        ByteBuffer bb = ByteBuffer.wrap(octets);
        return bb.getInt();
    }

    @Override
    public int compareTo(IpAddress o) {
        Long lv = ((long) this.toInt()) & 0xffffffffL;
        Long rv = ((long) o.toInt()) & 0xffffffffL;
        return lv.compareTo(rv);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
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
        IpAddress other = (IpAddress) obj;
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
     * format is "x.x.x.x" for IPv4 addresses.
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
        return builder.toString();
    }
}
