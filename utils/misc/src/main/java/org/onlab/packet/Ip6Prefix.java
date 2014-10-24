package org.onlab.packet;

import java.util.Objects;

/**
 * The class representing an IPv6 network address.
 * This class is immutable.
 */
public final class Ip6Prefix {
    private final Ip6Address address;           // The IPv6 address
    private final short prefixLen;              // The prefix length

    /**
     * Default constructor.
     */
    public Ip6Prefix() {
        this.address = new Ip6Address();
        this.prefixLen = 0;
    }

    /**
     * Copy constructor.
     *
     * @param other the object to copy from
     */
    public Ip6Prefix(Ip6Prefix other) {
        this.address = new Ip6Address(other.address);
        this.prefixLen = other.prefixLen;
    }

    /**
     * Constructor for a given address and prefix length.
     *
     * @param address   the address to use
     * @param prefixLen the prefix length to use
     */
    public Ip6Prefix(Ip6Address address, short prefixLen) {
        this.address = Ip6Address.makeMaskedAddress(address, prefixLen);
        this.prefixLen = prefixLen;
    }

    /**
     * Constructs an IPv6 prefix from a string representation of the
     * prefix.
     *<p>
     * Example: "1111:2222::/32"
     *
     * @param value the value to use
     */
    public Ip6Prefix(String value) {
        String[] splits = value.split("/");
        if (splits.length != 2) {
            throw new IllegalArgumentException("Specified IPv6 prefix must contain an IPv6 " +
                    "address and a prefix length separated by '/'");
        }
        this.prefixLen = Short.decode(splits[1]);
        this.address = Ip6Address.makeMaskedAddress(new Ip6Address(splits[0]),
                this.prefixLen);
    }

    /**
     * Gets the address value of the IPv6 prefix.
     *
     * @return the address value of the IPv6 prefix
     */
    public Ip6Address getAddress() {
        return address;
    }

    /**
     * Gets the prefix length value of the IPv6 prefix.
     *
     * @return the prefix length value of the IPv6 prefix
     */
    public short getPrefixLen() {
        return prefixLen;
    }

    /**
     * Converts the IPv6 prefix value to an "address/prefixLen" string.
     *
     * @return the IPv6 prefix value as an "address/prefixLen" string
     */
    @Override
    public String toString() {
        return this.address.toString() + "/" + this.prefixLen;
    }

    /**
     * Compares the value of two Ip6Prefix objects.
     * <p/>
     * Note the value of the IPv6 address is compared directly between the
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

        if (!(other instanceof Ip6Prefix)) {
            return false;
        }

        Ip6Prefix otherIp6Prefix = (Ip6Prefix) other;

        return Objects.equals(this.address, otherIp6Prefix.address)
                && this.prefixLen == otherIp6Prefix.prefixLen;
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, prefixLen);
    }
}
