/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.onlab.packet;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Objects;

import com.google.common.net.InetAddresses;
import com.google.common.primitives.UnsignedLongs;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * The class representing an IPv6 address.
 * This class is immutable.
 */
public final class Ip6Address implements Comparable<Ip6Address> {
    private final long valueHigh;    // The higher (more significant) 64 bits
    private final long valueLow;     // The lower (less significant) 64 bits

    /** The length of the address in bytes (octets). */
    public static final int BYTE_LENGTH = 16;

    /** The length of the address in bits. */
    public static final int BIT_LENGTH = BYTE_LENGTH * Byte.SIZE;

    /**
     * Default constructor.
     */
    public Ip6Address() {
        this.valueHigh = 0;
        this.valueLow = 0;
    }

    /**
     * Copy constructor.
     *
     * @param other the object to copy from
     */
    public Ip6Address(Ip6Address other) {
        this.valueHigh = other.valueHigh;
        this.valueLow = other.valueLow;
    }

    /**
     * Constructor from integer values.
     *
     * @param valueHigh the higher (more significant) 64 bits of the address
     * @param valueLow  the lower (less significant) 64 bits of the address
     */
    public Ip6Address(long valueHigh, long valueLow) {
        this.valueHigh = valueHigh;
        this.valueLow = valueLow;
    }

    /**
     * Constructor from a byte array with the IPv6 address stored in network
     * byte order (i.e., the most significant byte first).
     *
     * @param value the value to use
     */
    public Ip6Address(byte[] value) {
        this(value, 0);
    }

    /**
     * Constructor from a byte array with the IPv6 address stored in network
     * byte order (i.e., the most significant byte first), and a given offset
     * from the beginning of the byte array.
     *
     * @param value the value to use
     * @param offset the offset in bytes from the beginning of the byte array
     */
    public Ip6Address(byte[] value, int offset) {
        checkNotNull(value);

        // Verify the arguments
        if ((offset < 0) || (offset + BYTE_LENGTH > value.length)) {
            String msg;
            if (value.length < BYTE_LENGTH) {
                msg = "Invalid IPv6 address array: array length: " +
                    value.length + ". Must be at least " + BYTE_LENGTH;
            } else {
                msg = "Invalid IPv6 address array: array offset: " +
                    offset + ". Must be in the interval [0, " +
                    (value.length - BYTE_LENGTH) + "]";
            }
            throw new IllegalArgumentException(msg);
        }

        // Read the address
        ByteBuffer bb = ByteBuffer.wrap(value);
        bb.position(offset);
        this.valueHigh = bb.getLong();
        this.valueLow = bb.getLong();
    }

    /**
     * Constructs an IPv6 address from a string representation of the address.
     *<p>
     * Example: "1111:2222::8888"
     *
     * @param value the value to use
     */
    public Ip6Address(String value) {
        checkNotNull(value);

        if (value.isEmpty()) {
            final String msg = "Specified IPv6 cannot be an empty string";
            throw new IllegalArgumentException(msg);
        }
        InetAddress addr = null;
        try {
            addr = InetAddresses.forString(value);
        } catch (IllegalArgumentException e) {
            final String msg = "Invalid IPv6 address string: " + value;
            throw new IllegalArgumentException(msg);
        }
        byte[] bytes = addr.getAddress();
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        this.valueHigh = bb.getLong();
        this.valueLow = bb.getLong();
    }

    /**
     * Gets the IPv6 address as a byte array.
     *
     * @return a byte array with the IPv6 address stored in network byte order
     * (i.e., the most significant byte first).
     */
    public byte[] toOctets() {
        return ByteBuffer.allocate(BYTE_LENGTH)
            .putLong(valueHigh).putLong(valueLow).array();
    }

    /**
     * Creates an IPv6 network mask prefix.
     *
     * @param prefixLen the length of the mask prefix. Must be in the interval
     * [0, 128].
     * @return a new IPv6 address that contains a mask prefix of the
     * specified length
     */
    public static Ip6Address makeMaskPrefix(int prefixLen) {
        long vh, vl;

        // Verify the prefix length
        if ((prefixLen < 0) || (prefixLen > Ip6Address.BIT_LENGTH)) {
            final String msg = "Invalid IPv6 prefix length: " + prefixLen +
                ". Must be in the interval [0, 128].";
            throw new IllegalArgumentException(msg);
        }

        if (prefixLen == 0) {
            //
            // NOTE: Apparently, the result of "<< 64" shifting to the left
            // results in all 1s instead of all 0s, hence we handle it as
            // a special case.
            //
            vh = 0;
            vl = 0;
        } else if (prefixLen <= 64) {
            vh = (0xffffffffffffffffL << (64 - prefixLen)) & 0xffffffffffffffffL;
            vl = 0;
        } else {
            vh = -1L;           // All 1s
            vl = (0xffffffffffffffffL << (128 - prefixLen)) & 0xffffffffffffffffL;
        }
        return new Ip6Address(vh, vl);
    }

    /**
     * Creates an IPv6 address by masking it with a network mask of given
     * mask length.
     *
     * @param addr the address to mask
     * @param prefixLen the length of the mask prefix. Must be in the interval
     * [0, 128].
     * @return a new IPv6 address that is masked with a mask prefix of the
     * specified length
     */
    public static Ip6Address makeMaskedAddress(final Ip6Address addr,
                                               int prefixLen) {
        Ip6Address mask = Ip6Address.makeMaskPrefix(prefixLen);
        long vh = addr.valueHigh & mask.valueHigh;
        long vl = addr.valueLow & mask.valueLow;

        return new Ip6Address(vh, vl);
    }

    /**
     * Gets the value of the higher (more significant) 64 bits of the address.
     *
     * @return the value of the higher (more significant) 64 bits of the
     * address
     */
    public long getValueHigh() {
        return valueHigh;
    }

    /**
     * Gets the value of the lower (less significant) 64 bits of the address.
     *
     * @return the value of the lower (less significant) 64 bits of the
     * address
     */
    public long getValueLow() {
        return valueLow;
    }

    /**
     * Converts the IPv6 value to a ':' separated string.
     *
     * @return the IPv6 value as a ':' separated string
     */
    @Override
    public String toString() {
        ByteBuffer bb = ByteBuffer.allocate(Ip6Address.BYTE_LENGTH);
        bb.putLong(valueHigh);
        bb.putLong(valueLow);
        InetAddress inetAddr = null;
        try {
            inetAddr = InetAddress.getByAddress(bb.array());
        } catch (UnknownHostException e) {
            // Should never happen
            checkState(false, "Internal error: Ip6Address.toString()");
            return "::";
        }
        return InetAddresses.toAddrString(inetAddr);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Ip6Address)) {
            return false;
        }
        Ip6Address other = (Ip6Address) o;
        return this.valueHigh == other.valueHigh
                && this.valueLow == other.valueLow;
    }

    @Override
    public int hashCode() {
        return Objects.hash(valueHigh, valueLow);
    }

    @Override
    public int compareTo(Ip6Address o) {
        // Compare the high-order 64-bit value
        if (this.valueHigh != o.valueHigh) {
            return UnsignedLongs.compare(this.valueHigh, o.valueHigh);
        }
        // Compare the low-order 64-bit value
        if (this.valueLow != o.valueLow) {
            return UnsignedLongs.compare(this.valueLow, o.valueLow);
        }
        return 0;
    }
}
