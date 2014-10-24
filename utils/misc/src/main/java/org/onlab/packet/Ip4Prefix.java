package org.onlab.packet;

import java.util.Objects;

/**
 * The class representing an IPv4 network address.
 * This class is immutable.
 */
public final class Ip4Prefix {
    private final Ip4Address address;           // The IPv4 address
    private final short prefixLen;              // The prefix length

    /**
     * Default constructor.
     */
    public Ip4Prefix() {
        this.address = new Ip4Address();
        this.prefixLen = 0;
    }

    /**
     * Copy constructor.
     *
     * @param other the object to copy from
     */
    public Ip4Prefix(Ip4Prefix other) {
        this.address = new Ip4Address(other.address);
        this.prefixLen = other.prefixLen;
    }

    /**
     * Constructor for a given address and prefix length.
     *
     * @param address   the address to use
     * @param prefixLen the prefix length to use
     */
    public Ip4Prefix(Ip4Address address, short prefixLen) {
        this.address = Ip4Address.makeMaskedAddress(address, prefixLen);
        this.prefixLen = prefixLen;
    }

    /**
     * Constructs an IPv4 prefix from a string representation of the
     * prefix.
     *<p>
     * Example: "1.2.0.0/16"
     *
     * @param value the value to use
     */
    public Ip4Prefix(String value) {
        String[] splits = value.split("/");
        if (splits.length != 2) {
            throw new IllegalArgumentException("Specified IPv4 prefix must contain an IPv4 " +
                    "address and a prefix length separated by '/'");
        }
        this.prefixLen = Short.decode(splits[1]);
        this.address = Ip4Address.makeMaskedAddress(new Ip4Address(splits[0]),
                this.prefixLen);
    }

    /**
     * Gets the address value of the IPv4 prefix.
     *
     * @return the address value of the IPv4 prefix
     */
    public Ip4Address getAddress() {
        return address;
    }

    /**
     * Gets the prefix length value of the IPv4 prefix.
     *
     * @return the prefix length value of the IPv4 prefix
     */
    public short getPrefixLen() {
        return prefixLen;
    }

    /**
     * Converts the IPv4 prefix value to an "address/prefixLen" string.
     *
     * @return the IPv4 prefix value as an "address/prefixLen" string
     */
    @Override
    public String toString() {
        return this.address.toString() + "/" + this.prefixLen;
    }

    /**
     * Compares the value of two Ip4Prefix objects.
     * <p/>
     * Note the value of the IPv4 address is compared directly between the
     * objects, and must match exactly for the objects to be considered equal.
     * This may result in objects which represent the same IP prefix being
     * classified as unequal, because the unsignificant bits of the address
     * field don't match (the bits to the right of the prefix length).
     * <p/>
     * TODO Change this behavior so that objects that represent the same prefix
     * are classified as equal according to this equals method.
     *
     * @see Object#equals(Object)
     */
    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        if (!(other instanceof Ip4Prefix)) {
            return false;
        }

        Ip4Prefix otherIp4Prefix = (Ip4Prefix) other;

        return Objects.equals(this.address, otherIp4Prefix.address)
                && this.prefixLen == otherIp4Prefix.prefixLen;
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, prefixLen);
    }
}
