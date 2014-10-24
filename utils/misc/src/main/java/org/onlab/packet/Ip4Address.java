package org.onlab.packet;

import java.nio.ByteBuffer;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The class representing an IPv4 address.
 * This class is immutable.
 */
public final class Ip4Address implements Comparable<Ip4Address> {
    private final int value;

    /** The length of the address in bytes (octets). */
    public static final int BYTE_LENGTH = 4;

    /** The length of the address in bits. */
    public static final int BIT_LENGTH = BYTE_LENGTH * Byte.SIZE;

    /**
     * Default constructor.
     */
    public Ip4Address() {
        this.value = 0;
    }

    /**
     * Copy constructor.
     *
     * @param other the object to copy from
     */
    public Ip4Address(Ip4Address other) {
        this.value = other.value;
    }

    /**
     * Constructor from an integer value.
     *
     * @param value the value to use
     */
    public Ip4Address(int value) {
        this.value = value;
    }

    /**
     * Constructor from a byte array with the IPv4 address stored in network
     * byte order (i.e., the most significant byte first).
     *
     * @param value the value to use
     */
    public Ip4Address(byte[] value) {
        this(value, 0);
    }

    /**
     * Constructor from a byte array with the IPv4 address stored in network
     * byte order (i.e., the most significant byte first), and a given offset
     * from the beginning of the byte array.
     *
     * @param value the value to use
     * @param offset the offset in bytes from the beginning of the byte array
     */
    public Ip4Address(byte[] value, int offset) {
        checkNotNull(value);

        // Verify the arguments
        if ((offset < 0) || (offset + BYTE_LENGTH > value.length)) {
            String msg;
            if (value.length < BYTE_LENGTH) {
                msg = "Invalid IPv4 address array: array length: " +
                    value.length + ". Must be at least " + BYTE_LENGTH;
            } else {
                msg = "Invalid IPv4 address array: array offset: " +
                    offset + ". Must be in the interval [0, " +
                    (value.length - BYTE_LENGTH) + "]";
            }
            throw new IllegalArgumentException(msg);
        }

        // Read the address
        ByteBuffer bb = ByteBuffer.wrap(value);
        this.value = bb.getInt(offset);
    }

    /**
     * Constructs an IPv4 address from a string representation of the address.
     *<p>
     * Example: "1.2.3.4"
     *
     * @param value the value to use
     */
    public Ip4Address(String value) {
        checkNotNull(value);

        String[] splits = value.split("\\.");
        if (splits.length != 4) {
            final String msg = "Invalid IPv4 address string: " + value;
            throw new IllegalArgumentException(msg);
        }

        int result = 0;
        for (int i = 0; i < BYTE_LENGTH; i++) {
            result |= Integer.parseInt(splits[i]) <<
                ((BYTE_LENGTH - (i + 1)) * Byte.SIZE);
        }
        this.value = result;
    }

    /**
     * Gets the IPv4 address as a byte array.
     *
     * @return a byte array with the IPv4 address stored in network byte order
     * (i.e., the most significant byte first).
     */
    public byte[] toOctets() {
        return ByteBuffer.allocate(BYTE_LENGTH).putInt(value).array();
    }

    /**
     * Creates an IPv4 network mask prefix.
     *
     * @param prefixLen the length of the mask prefix. Must be in the interval
     * [0, 32].
     * @return a new IPv4 address that contains a mask prefix of the
     * specified length
     */
    public static Ip4Address makeMaskPrefix(int prefixLen) {
        // Verify the prefix length
        if ((prefixLen < 0) || (prefixLen > Ip4Address.BIT_LENGTH)) {
            final String msg = "Invalid IPv4 prefix length: " + prefixLen +
                ". Must be in the interval [0, 32].";
            throw new IllegalArgumentException(msg);
        }

        long v =
            (0xffffffffL << (Ip4Address.BIT_LENGTH - prefixLen)) & 0xffffffffL;
        return new Ip4Address((int) v);
    }

    /**
     * Creates an IPv4 address by masking it with a network mask of given
     * mask length.
     *
     * @param addr the address to mask
     * @param prefixLen the length of the mask prefix. Must be in the interval
     * [0, 32].
     * @return a new IPv4 address that is masked with a mask prefix of the
     * specified length
     */
    public static Ip4Address makeMaskedAddress(final Ip4Address addr,
                                               int prefixLen) {
        Ip4Address mask = Ip4Address.makeMaskPrefix(prefixLen);
        long v = addr.value & mask.value;

        return new Ip4Address((int) v);
    }

    /**
     * Gets the value of the IPv4 address.
     *
     * @return the value of the IPv4 address
     */
    public int getValue() {
        return value;
    }

    /**
     * Converts the IPv4 value to a '.' separated string.
     *
     * @return the IPv4 value as a '.' separated string
     */
    @Override
    public String toString() {
        return ((this.value >> 24) & 0xff) + "." +
                ((this.value >> 16) & 0xff) + "." +
                ((this.value >> 8) & 0xff) + "." +
                (this.value & 0xff);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Ip4Address)) {
            return false;
        }
        Ip4Address other = (Ip4Address) o;
        if (this.value != other.value) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return this.value;
    }

    @Override
    public int compareTo(Ip4Address o) {
        Long lv = ((long) this.value) & 0xffffffffL;
        Long rv = ((long) o.value) & 0xffffffffL;
        return lv.compareTo(rv);
    }
}
