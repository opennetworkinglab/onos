package org.projectfloodlight.openflow.types;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.Arrays;

import javax.annotation.Nonnull;

import org.jboss.netty.buffer.ChannelBuffer;

import com.google.common.base.Preconditions;
import com.google.common.hash.PrimitiveSink;
import com.google.common.primitives.UnsignedInts;



/**
 * Wrapper around an IPv4Address address
 *
 * @author Andreas Wundsam <andreas.wundsam@bigswitch.com>
 */
public class IPv4Address extends IPAddress<IPv4Address> {
    static final int LENGTH = 4;
    private final int rawValue;

    private static final int NOT_A_CIDR_MASK = -1;
    private static final int CIDR_MASK_CACHE_UNSET = -2;
    // Must appear before the static IPv4Address constant assignments
    private volatile int cidrMaskLengthCache = CIDR_MASK_CACHE_UNSET;

    private final static int NONE_VAL = 0x0;
    public final static IPv4Address NONE = new IPv4Address(NONE_VAL);

    public static final IPv4Address NO_MASK = IPv4Address.of(0xFFFFFFFF);
    public static final IPv4Address FULL_MASK = IPv4Address.of(0x00000000);

    private IPv4Address(final int rawValue) {
        this.rawValue = rawValue;
    }

    @Override
    public IPVersion getIpVersion() {
        return IPVersion.IPv4;
    }

    private int asCidrMaskLengthInternal() {
        if (cidrMaskLengthCache == CIDR_MASK_CACHE_UNSET) {
            // No lock required. We only write cidrMaskLengthCache once
            int maskint = getInt();
            if (maskint == 0) {
                cidrMaskLengthCache = 0;
            } else if (Integer.bitCount((~maskint) + 1) == 1) {
                // IP represents a true CIDR prefix length
                cidrMaskLengthCache = Integer.bitCount(maskint);
            } else {
                cidrMaskLengthCache = NOT_A_CIDR_MASK;
            }
        }
        return cidrMaskLengthCache;
    }

    @Override
    public boolean isCidrMask() {
        return asCidrMaskLengthInternal() != NOT_A_CIDR_MASK;
    }

    @Override
    public int asCidrMaskLength() {
        if (!isCidrMask()) {
            throw new IllegalStateException("IP is not a valid CIDR prefix " +
                    "mask " + toString());
        } else {
            return asCidrMaskLengthInternal();
        }
    }

    @Override
    public boolean isBroadcast() {
        return this.equals(NO_MASK);
    }

    @Override
    public IPv4Address and(IPv4Address other) {
        Preconditions.checkNotNull(other, "other must not be null");

        IPv4Address otherIp = other;
        return IPv4Address.of(rawValue & otherIp.rawValue);
    }

    @Override
    public IPv4Address or(IPv4Address other) {
        Preconditions.checkNotNull(other, "other must not be null");

        IPv4Address otherIp = other;
        return IPv4Address.of(rawValue | otherIp.rawValue);
    }

    @Override
    public IPv4Address not() {
        return IPv4Address.of(~rawValue);
    }

    /**
     * Returns an {@code IPv4Address} object that represents the given
     * IP address. The argument is in network byte order: the highest
     * order byte of the address is in {@code address[0]}.
     * <p>
     * The address byte array must be 4 bytes long (32 bits long).
     * <p>
     * Similar to {@link InetAddress#getByAddress(byte[])}.
     *
     * @param address  the raw IP address in network byte order
     * @return         an {@code IPv4Address} object that represents the given
     *                 raw IP address
     * @throws NullPointerException      if the given address was {@code null}
     * @throws IllegalArgumentException  if the given address was of an invalid
     *                                   byte array length
     * @see InetAddress#getByAddress(byte[])
     */
    @Nonnull
    public static IPv4Address of(@Nonnull final byte[] address) {
        Preconditions.checkNotNull(address, "address must not be null");

        if (address.length != LENGTH) {
            throw new IllegalArgumentException(
                    "Invalid byte array length for IPv4 address: " + address.length);
        }

        int raw =
                (address[0] & 0xFF) << 24 | (address[1] & 0xFF) << 16
                        | (address[2] & 0xFF) << 8 | (address[3] & 0xFF) << 0;
        return IPv4Address.of(raw);
    }

    /**
     * Returns an {@code IPv4Address} object that represents the given
     * IP address.
     *
     * @param raw  the raw IP address represented as a 32-bit integer
     * @return     an {@code IPv4Address} object that represents the given
     *             raw IP address
     */
    @Nonnull
    public static IPv4Address of(final int raw) {
        if(raw == NONE_VAL)
            return NONE;
        return new IPv4Address(raw);
    }

    /**
     * Returns an {@code IPv4Address} object that represents the given
     * IP address. The argument is in the canonical quad-dotted notation.
     * For example, {@code 1.2.3.4}.
     *
     * @param string  the IP address in the canonical quad-dotted notation
     * @return        an {@code IPv4Address} object that represents the given
     *                IP address
     * @throws NullPointerException      if the given string was {@code null}
     * @throws IllegalArgumentException  if the given string was not a valid
     *                                   IPv4 address
     */
    @Nonnull
    public static IPv4Address of(@Nonnull final String string) throws IllegalArgumentException {
        Preconditions.checkNotNull(string, "string must not be null");

        int start = 0;
        int shift = 24;

        int raw = 0;
        while (shift >= 0) {
            int end = string.indexOf('.', start);
            if (end == start || !((shift > 0) ^ (end < 0)))
                throw new IllegalArgumentException("IP Address not well formed: " + string);

            String substr =
                    end > 0 ? string.substring(start, end) : string.substring(start);
            int val = Integer.parseInt(substr);
            if (val < 0 || val > 255)
                throw new IllegalArgumentException("IP Address not well formed: " + string);

            raw |= val << shift;

            shift -= 8;
            start = end + 1;
        }
        return IPv4Address.of(raw);
    }

    /**
     * Returns an {@code IPv4Address} object that represents the given
     * IP address. The argument is given as an {@code Inet4Address} object.
     *
     * @param address  the IP address as an {@code Inet4Address} object
     * @return         an {@code IPv4Address} object that represents the
     *                 given IP address
     * @throws NullPointerException  if the given {@code Inet4Address} was
     *                               {@code null}
     */
    @Nonnull
    public static IPv4Address of(@Nonnull final Inet4Address address) {
        Preconditions.checkNotNull(address, "address must not be null");
        return IPv4Address.of(address.getAddress());
    }

    /**
     * Returns an {@code IPv4Address} object that represents the
     * CIDR subnet mask of the given prefix length.
     *
     * @param cidrMaskLength  the prefix length of the CIDR subnet mask
     *                        (i.e. the number of leading one-bits),
     *                        where {@code 0 <= cidrMaskLength <= 32}
     * @return                an {@code IPv4Address} object that represents the
     *                        CIDR subnet mask of the given prefix length
     * @throws IllegalArgumentException  if the given prefix length was invalid
     */
    @Nonnull
    public static IPv4Address ofCidrMaskLength(final int cidrMaskLength) {
        Preconditions.checkArgument(
                cidrMaskLength >= 0 && cidrMaskLength <= 32,
                "Invalid IPv4 CIDR mask length: %s", cidrMaskLength);

        if (cidrMaskLength == 32) {
            return IPv4Address.NO_MASK;
        } else if (cidrMaskLength == 0) {
            return IPv4Address.FULL_MASK;
        } else {
            int mask = (-1) << (32 - cidrMaskLength);
            return IPv4Address.of(mask);
        }
    }

    /**
     * Returns an {@code IPv4AddressWithMask} object that represents this
     * IP address masked by the given IP address mask.
     *
     * @param mask  the {@code IPv4Address} object that represents the mask
     * @return      an {@code IPv4AddressWithMask} object that represents this
     *              IP address masked by the given mask
     * @throws NullPointerException  if the given mask was {@code null}
     */
    @Nonnull
    @Override
    public IPv4AddressWithMask withMask(@Nonnull final IPv4Address mask) {
        return IPv4AddressWithMask.of(this, mask);
    }

    /**
     * Returns an {@code IPv4AddressWithMask} object that represents this
     * IP address masked by the CIDR subnet mask of the given prefix length.
     *
     * @param cidrMaskLength  the prefix length of the CIDR subnet mask
     *                        (i.e. the number of leading one-bits),
     *                        where {@code 0 <= cidrMaskLength <= 32}
     * @return                an {@code IPv4AddressWithMask} object that
     *                        represents this IP address masked by the CIDR
     *                        subnet mask of the given prefix length
     * @throws IllegalArgumentException  if the given prefix length was invalid
     * @see #ofCidrMaskLength(int)
     */
    @Nonnull
    @Override
    public IPv4AddressWithMask withMaskOfLength(final int cidrMaskLength) {
        return this.withMask(IPv4Address.ofCidrMaskLength(cidrMaskLength));
    }

    public int getInt() {
        return rawValue;
    }

    private volatile byte[] bytesCache = null;

    @Override
    public byte[] getBytes() {
        if (bytesCache == null) {
            synchronized (this) {
                if (bytesCache == null) {
                    bytesCache =
                            new byte[] { (byte) ((rawValue >>> 24) & 0xFF),
                                    (byte) ((rawValue >>> 16) & 0xFF),
                                    (byte) ((rawValue >>> 8) & 0xFF),
                                    (byte) ((rawValue >>> 0) & 0xFF) };
                }
            }
        }
        return Arrays.copyOf(bytesCache, bytesCache.length);
    }

    @Override
    public int getLength() {
        return LENGTH;
    }

    @Override
    public String toString() {
        StringBuilder res = new StringBuilder();
        res.append((rawValue >> 24) & 0xFF).append('.');
        res.append((rawValue >> 16) & 0xFF).append('.');
        res.append((rawValue >> 8) & 0xFF).append('.');
        res.append((rawValue >> 0) & 0xFF);
        return res.toString();
    }

    public void write4Bytes(ChannelBuffer c) {
        c.writeInt(rawValue);
    }

    public static IPv4Address read4Bytes(ChannelBuffer c) {
        return IPv4Address.of(c.readInt());
    }

    @Override
    public IPv4Address applyMask(IPv4Address mask) {
        return and(mask);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + rawValue;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        IPv4Address other = (IPv4Address) obj;
        if (rawValue != other.rawValue)
            return false;
        return true;
    }

    @Override
    public int compareTo(IPv4Address o) {
        return UnsignedInts.compare(rawValue, o.rawValue);
    }

    @Override
    public void putTo(PrimitiveSink sink) {
        sink.putInt(rawValue);
    }

}
