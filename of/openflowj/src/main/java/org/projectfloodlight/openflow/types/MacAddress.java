package org.projectfloodlight.openflow.types;

import java.util.Arrays;

import javax.annotation.Nonnull;

import org.jboss.netty.buffer.ChannelBuffer;
import org.projectfloodlight.openflow.exceptions.OFParseError;
import org.projectfloodlight.openflow.util.HexString;

import com.google.common.hash.PrimitiveSink;
import com.google.common.primitives.Longs;

/**
 * Wrapper around a 6 byte mac address.
 *
 * @author Andreas Wundsam <andreas.wundsam@bigswitch.com>
 */

public class MacAddress implements OFValueType<MacAddress> {
    static final int MacAddrLen = 6;
    private final long rawValue;

    private final static long NONE_VAL = 0x0L;
    public static final MacAddress NONE = new MacAddress(NONE_VAL);

    private final static long BROADCAST_VAL = 0x0000FFFFFFFFFFFFL;
    public static final MacAddress BROADCAST = new MacAddress(BROADCAST_VAL);

    public static final MacAddress NO_MASK = MacAddress.of(0xFFFFFFFFFFFFFFFFl);
    public static final MacAddress FULL_MASK = MacAddress.of(0x0);

    private static final long LLDP_MAC_ADDRESS_MASK = 0xfffffffffff0L;
    private static final long LLDP_MAC_ADDRESS_VALUE = 0x0180c2000000L;

    private MacAddress(final long rawValue) {
        this.rawValue = rawValue;
    }

    public static MacAddress of(final byte[] address) {
        if (address.length != MacAddrLen)
            throw new IllegalArgumentException(
                    "Mac address byte array must be exactly 6 bytes long; length = " + address.length);
        long raw =
                (address[0] & 0xFFL) << 40 | (address[1] & 0xFFL) << 32
                        | (address[2] & 0xFFL) << 24 | (address[3] & 0xFFL) << 16
                        | (address[4] & 0xFFL) << 8 | (address[5] & 0xFFL);
        return MacAddress.of(raw);
    }

    public static MacAddress of(long raw) {
        raw &= BROADCAST_VAL;
        if(raw == NONE_VAL)
            return NONE;
        if (raw == BROADCAST_VAL)
            return BROADCAST;
        return new MacAddress(raw);
    }

    /** Parse a mac adress from the canonical string representation as
     *  6 hex bytes separated by colons (01:02:03:04:05:06).
     *
     * @param macString - a mac address in canonical string representation
     * @return the parsed MacAddress
     * @throws IllegalArgumentException if macString is not a valid mac adddress
     */
    @Nonnull
    public static MacAddress of(@Nonnull final String macString) throws IllegalArgumentException {
        if (macString == null) {
            throw new NullPointerException("macString must not be null");
        }
        int index = 0;
        int shift = 40;
        final String FORMAT_ERROR = "Mac address is not well-formed. " +
                "It must consist of 6 hex digit pairs separated by colons: ";

        long raw = 0;
        if (macString.length() != 6 * 2 + 5)
            throw new IllegalArgumentException(FORMAT_ERROR + macString);

        while (shift >= 0) {
            int digit1 = Character.digit(macString.charAt(index++), 16);
            int digit2 = Character.digit(macString.charAt(index++), 16);
            if ((digit1 < 0) || (digit2 < 0))
                throw new IllegalArgumentException(FORMAT_ERROR + macString);
            raw |= ((long) (digit1 << 4 | digit2)) << shift;

            if (shift == 0)
                break;
            if (macString.charAt(index++) != ':')
                throw new IllegalArgumentException(FORMAT_ERROR + macString);
            shift -= 8;
        }
        return MacAddress.of(raw);
    }

    private volatile byte[] bytesCache = null;

    public byte[] getBytes() {
        if (bytesCache == null) {
            synchronized (this) {
                if (bytesCache == null) {
                    bytesCache =
                            new byte[] { (byte) ((rawValue >> 40) & 0xFF),
                                    (byte) ((rawValue >> 32) & 0xFF),
                                    (byte) ((rawValue >> 24) & 0xFF),
                                    (byte) ((rawValue >> 16) & 0xFF),
                                    (byte) ((rawValue >> 8) & 0xFF),
                                    (byte) ((rawValue >> 0) & 0xFF) };
                }
            }
        }
        return Arrays.copyOf(bytesCache, bytesCache.length);
    }

    /**
     * Returns {@code true} if the MAC address is the broadcast address.
     * @return {@code true} if the MAC address is the broadcast address.
     */
    public boolean isBroadcast() {
        return this == BROADCAST;
    }

    /**
     * Returns {@code true} if the MAC address is a multicast address.
     * @return {@code true} if the MAC address is a multicast address.
     */
    public boolean isMulticast() {
        if (isBroadcast()) {
            return false;
        }
        return (rawValue & (0x01L << 40)) != 0;
    }

    /**
     * Returns {@code true} if the MAC address is an LLDP mac address.
     * @return {@code true} if the MAC address is an LLDP mac address.
     */
    public boolean isLLDPAddress() {
        return (rawValue & LLDP_MAC_ADDRESS_MASK) == LLDP_MAC_ADDRESS_VALUE;
    }

    @Override
    public int getLength() {
        return MacAddrLen;
    }

    @Override
    public String toString() {
        return HexString.toHexString(rawValue, 6);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (rawValue ^ (rawValue >>> 32));
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MacAddress other = (MacAddress) obj;
        if (rawValue != other.rawValue)
            return false;
        return true;
    }

    public long getLong() {
        return rawValue;
    }

    public void write6Bytes(ChannelBuffer c) {
        c.writeInt((int) (this.rawValue >> 16));
        c.writeShort((int) this.rawValue & 0xFFFF);
    }

    public static MacAddress read6Bytes(ChannelBuffer c) throws OFParseError {
        long raw = c.readUnsignedInt() << 16 | c.readUnsignedShort();
        return MacAddress.of(raw);
    }

    @Override
    public MacAddress applyMask(MacAddress mask) {
        return MacAddress.of(this.rawValue & mask.rawValue);
    }

    @Override
    public int compareTo(MacAddress o) {
        return Longs.compare(rawValue, o.rawValue);
    }

    @Override
    public void putTo(PrimitiveSink sink) {
        sink.putInt((int) (this.rawValue >> 16));
        sink.putShort((short) (this.rawValue & 0xFFFF));
    }



}
