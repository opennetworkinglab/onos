package org.projectfloodlight.openflow.types;

import java.util.Arrays;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import org.jboss.netty.buffer.ChannelBuffer;
import org.projectfloodlight.openflow.exceptions.OFParseError;

import com.google.common.hash.PrimitiveSink;
import com.google.common.primitives.Longs;

/**
 * IPv6 address object. Instance controlled, immutable. Internal representation:
 * two 64 bit longs (not that you'd have to know).
 *
 * @author Andreas Wundsam <andreas.wundsam@teleteach.de>
 */
public class IPv6Address extends IPAddress<IPv6Address> {
    static final int LENGTH = 16;
    private final long raw1;
    private final long raw2;

    private static final int NOT_A_CIDR_MASK = -1;
    private static final int CIDR_MASK_CACHE_UNSET = -2;
    // Must appear before the static IPv4Address constant assignments
    private volatile int cidrMaskLengthCache = CIDR_MASK_CACHE_UNSET;

    private final static long NONE_VAL1 = 0x0L;
    private final static long NONE_VAL2 = 0x0L;
    public static final IPv6Address NONE = new IPv6Address(NONE_VAL1, NONE_VAL2);


    public static final IPv6Address NO_MASK = IPv6Address.of(0xFFFFFFFFFFFFFFFFl, 0xFFFFFFFFFFFFFFFFl);
    public static final IPv6Address FULL_MASK = IPv6Address.of(0x0, 0x0);

    private IPv6Address(final long raw1, final long raw2) {
        this.raw1 = raw1;
        this.raw2 = raw2;
    }

    @Override
    public IPVersion getIpVersion() {
        return IPVersion.IPv6;
    }


    private int computeCidrMask64(long raw) {
        long mask = raw;
        if (raw == 0)
            return 0;
        else if (Long.bitCount((~mask) + 1) == 1) {
            // represent a true CIDR prefix length
            return Long.bitCount(mask);
        }
        else {
            // Not a true prefix
            return NOT_A_CIDR_MASK;
        }
    }

    private int asCidrMaskLengthInternal() {
        if (cidrMaskLengthCache == CIDR_MASK_CACHE_UNSET) {
            // No synchronization needed. Writing cidrMaskLengthCache only once
            if (raw1 == 0 && raw2 == 0) {
                cidrMaskLengthCache = 0;
            } else if (raw1 == -1L) {
                // top half is all 1 bits
                int tmpLength = computeCidrMask64(raw2);
                if (tmpLength != NOT_A_CIDR_MASK)
                    tmpLength += 64;
                cidrMaskLengthCache = tmpLength;
            } else if (raw2 == 0) {
                cidrMaskLengthCache = computeCidrMask64(raw1);
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
    public IPv6Address and(IPv6Address other) {
        if (other == null) {
            throw new NullPointerException("Other IP Address must not be null");
        }
        IPv6Address otherIp = (IPv6Address) other;
        return IPv6Address.of((raw1 & otherIp.raw1), (raw2 & otherIp.raw2));
    }

    @Override
    public IPv6Address or(IPv6Address other) {
        if (other == null) {
            throw new NullPointerException("Other IP Address must not be null");
        }
        IPv6Address otherIp = (IPv6Address) other;
        return IPv6Address.of((raw1 | otherIp.raw1), (raw2 | otherIp.raw2));
    }

    @Override
    public IPv6Address not() {
        return IPv6Address.of(~raw1, ~raw2);
    }

    public static IPv6Address of(final byte[] address) {
        if (address == null) {
            throw new NullPointerException("Address must not be null");
        }
        if (address.length != LENGTH) {
            throw new IllegalArgumentException(
                    "Invalid byte array length for IPv6 address: " + address.length);
        }

        long raw1 =
                (address[0] & 0xFFL) << 56 | (address[1] & 0xFFL) << 48
                        | (address[2] & 0xFFL) << 40 | (address[3] & 0xFFL) << 32
                        | (address[4] & 0xFFL) << 24 | (address[5] & 0xFFL) << 16
                        | (address[6] & 0xFFL) << 8 | (address[7]);

        long raw2 =
                (address[8] & 0xFFL) << 56 | (address[9] & 0xFFL) << 48
                        | (address[10] & 0xFFL) << 40 | (address[11] & 0xFFL) << 32
                        | (address[12] & 0xFFL) << 24 | (address[13] & 0xFFL) << 16
                        | (address[14] & 0xFFL) << 8 | (address[15]);

        return IPv6Address.of(raw1, raw2);
    }

    private static class IPv6Builder {
        private long raw1, raw2;

        public void setUnsignedShortWord(final int i, final int value) {
            int shift = 48 - (i % 4) * 16;

            if (value < 0 || value > 0xFFFF)
                throw new IllegalArgumentException("16 bit word must be in [0, 0xFFFF]");

            if (i >= 0 && i < 4)
                raw1 = raw1 & ~(0xFFFFL << shift) | (value & 0xFFFFL) << shift;
            else if (i >= 4 && i < 8)
                raw2 = raw2 & ~(0xFFFFL << shift) | (value & 0xFFFFL) << shift;
            else
                throw new IllegalArgumentException("16 bit word index must be in [0,7]");
        }

        public IPv6Address getIPv6() {
            return IPv6Address.of(raw1, raw2);
        }
    }

    private final static Pattern colonPattern = Pattern.compile(":");

    /** parse an IPv6Address from its conventional string representation.
     *  <p>
     *  Expects up to 8 groups of 16-bit hex words seperated by colons
     *  (e.g., 2001:db8:85a3:8d3:1319:8a2e:370:7348).
     *  <p>
     *  Supports zero compression (e.g., 2001:db8::7348).
     *  Does <b>not</b> currently support embedding a dotted-quad IPv4 address
     *  into the IPv6 address (e.g., 2001:db8::192.168.0.1).
     *
     * @param string a string representation of an IPv6 address
     * @return the parsed IPv6 address
     * @throws NullPointerException if string is null
     * @throws IllegalArgumentException if string is not a valid IPv6Address
     */
    @Nonnull
    public static IPv6Address of(@Nonnull final String string) throws IllegalArgumentException {
        if (string == null) {
            throw new NullPointerException("String must not be null");
        }
        IPv6Builder builder = new IPv6Builder();
        String[] parts = colonPattern.split(string, -1);

        int leftWord = 0;
        int leftIndex = 0;

        boolean hitZeroCompression = false;

        for (leftIndex = 0; leftIndex < parts.length; leftIndex++) {
            String part = parts[leftIndex];
            if (part.length() == 0) {
                // hit empty group of zero compression
                hitZeroCompression = true;
                break;
            }
            builder.setUnsignedShortWord(leftWord++, Integer.parseInt(part, 16));
        }

        if (hitZeroCompression) {
            if (leftIndex == 0) {
                // if colon is at the start, two columns must be at the start,
                // move to the second empty group
                leftIndex = 1;
                if (parts.length < 2 || parts[1].length() > 0)
                    throw new IllegalArgumentException("Malformed IPv6 address: " + string);
            }

            int rightWord = 7;
            int rightIndex;
            for (rightIndex = parts.length - 1; rightIndex > leftIndex; rightIndex--) {
                String part = parts[rightIndex];
                if (part.length() == 0)
                    break;
                builder.setUnsignedShortWord(rightWord--, Integer.parseInt(part, 16));
            }
            if (rightIndex == parts.length - 1) {
                // if colon is at the end, two columns must be at the end, move
                // to the second empty group
                if (rightIndex < 1 || parts[rightIndex - 1].length() > 0)
                    throw new IllegalArgumentException("Malformed IPv6 address: " + string);
                rightIndex--;
            }
            if (leftIndex != rightIndex)
                throw new IllegalArgumentException("Malformed IPv6 address: " + string);
        } else {
            if (leftIndex != 8) {
                throw new IllegalArgumentException("Malformed IPv6 address: " + string);
            }
        }
        return builder.getIPv6();
    }

    /** construct an IPv6 adress from two 64 bit integers representing the first and
     *  second 8-byte blocks of the address.
     *
     * @param raw1 - the first 8 byte block of the address
     * @param raw2 - the second 8 byte block of the address
     * @return the constructed IPv6Address
     */
    public static IPv6Address of(final long raw1, final long raw2) {
        if(raw1==NONE_VAL1 && raw2 == NONE_VAL2)
            return NONE;
        return new IPv6Address(raw1, raw2);
    }

    private volatile byte[] bytesCache = null;

    public byte[] getBytes() {
        if (bytesCache == null) {
            synchronized (this) {
                if (bytesCache == null) {
                    bytesCache =
                            new byte[] { (byte) ((raw1 >> 56) & 0xFF),
                                    (byte) ((raw1 >> 48) & 0xFF),
                                    (byte) ((raw1 >> 40) & 0xFF),
                                    (byte) ((raw1 >> 32) & 0xFF),
                                    (byte) ((raw1 >> 24) & 0xFF),
                                    (byte) ((raw1 >> 16) & 0xFF),
                                    (byte) ((raw1 >> 8) & 0xFF),
                                    (byte) ((raw1 >> 0) & 0xFF),

                                    (byte) ((raw2 >> 56) & 0xFF),
                                    (byte) ((raw2 >> 48) & 0xFF),
                                    (byte) ((raw2 >> 40) & 0xFF),
                                    (byte) ((raw2 >> 32) & 0xFF),
                                    (byte) ((raw2 >> 24) & 0xFF),
                                    (byte) ((raw2 >> 16) & 0xFF),
                                    (byte) ((raw2 >> 8) & 0xFF),
                                    (byte) ((raw2 >> 0) & 0xFF) };
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
        return toString(true, false);
    }

    public int getUnsignedShortWord(final int i) {
        if (i >= 0 && i < 4)
            return (int) ((raw1 >>> (48 - i * 16)) & 0xFFFF);
        else if (i >= 4 && i < 8)
            return (int) ((raw2 >>> (48 - (i - 4) * 16)) & 0xFFFF);
        else
            throw new IllegalArgumentException("16 bit word index must be in [0,7]");
    }

    /** get the index of the first word where to apply IPv6 zero compression */
    public int getZeroCompressStart() {
        int start = Integer.MAX_VALUE;
        int maxLength = -1;

        int candidateStart = -1;

        for (int i = 0; i < 8; i++) {
            if (candidateStart >= 0) {
                // in a zero octect
                if (getUnsignedShortWord(i) != 0) {
                    // end of this candidate word
                    int candidateLength = i - candidateStart;
                    if (candidateLength >= maxLength) {
                        start = candidateStart;
                        maxLength = candidateLength;
                    }
                    candidateStart = -1;
                }
            } else {
                // not in a zero octect
                if (getUnsignedShortWord(i) == 0) {
                    candidateStart = i;
                }
            }
        }

        if (candidateStart >= 0) {
            int candidateLength = 8 - candidateStart;
            if (candidateLength >= maxLength) {
                start = candidateStart;
                maxLength = candidateLength;
            }
        }

        return start;
    }

    public String toString(final boolean zeroCompression, final boolean leadingZeros) {
        StringBuilder res = new StringBuilder();

        int compressionStart = zeroCompression ? getZeroCompressStart() : Integer.MAX_VALUE;
        boolean inCompression = false;
        boolean colonNeeded = false;

        for (int i = 0; i < 8; i++) {
            int word = getUnsignedShortWord(i);

            if (word == 0) {
                if (inCompression)
                    continue;
                else if (i == compressionStart) {
                    res.append(':').append(':');
                    inCompression = true;
                    colonNeeded = false;
                    continue;
                }
            } else {
                inCompression = false;
            }

            if (colonNeeded) {
                res.append(':');
                colonNeeded = false;
            }

            res.append(leadingZeros ? String.format("%04x", word) : Integer.toString(word,
                    16));
            colonNeeded = true;
        }
        return res.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (raw1 ^ (raw1 >>> 32));
        result = prime * result + (int) (raw2 ^ (raw2 >>> 32));
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
        IPv6Address other = (IPv6Address) obj;
        if (raw1 != other.raw1)
            return false;
        if (raw2 != other.raw2)
            return false;
        return true;
    }

    public void write16Bytes(ChannelBuffer c) {
        c.writeLong(this.raw1);
        c.writeLong(this.raw2);
    }

    public static IPv6Address read16Bytes(ChannelBuffer c) throws OFParseError {
        return IPv6Address.of(c.readLong(), c.readLong());
    }

    @Override
    public IPv6Address applyMask(IPv6Address mask) {
        return and(mask);
    }

    @Override
    public int compareTo(IPv6Address o) {
        int res = Longs.compare(raw1, o.raw1);
        if(res != 0)
            return res;
        else
            return Longs.compare(raw2, o.raw2);
    }

    @Override
    public void putTo(PrimitiveSink sink) {
        sink.putLong(raw1);
        sink.putLong(raw2);
    }
}
