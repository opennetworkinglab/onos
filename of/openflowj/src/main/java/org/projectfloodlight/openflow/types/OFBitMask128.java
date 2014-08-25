package org.projectfloodlight.openflow.types;

import org.jboss.netty.buffer.ChannelBuffer;

import com.google.common.hash.PrimitiveSink;

public class OFBitMask128 implements OFValueType<OFBitMask128> {

    static final int LENGTH = 16;

    private final long raw1; // MSBs (ports 64-127)
    private final long raw2; // LSBs (ports 0-63)

    public static final OFBitMask128 ALL = new OFBitMask128(-1, -1);
    public static final OFBitMask128 NONE = new OFBitMask128(0, 0);

    public static final OFBitMask128 NO_MASK = ALL;
    public static final OFBitMask128 FULL_MASK = NONE;

    private OFBitMask128(long raw1, long raw2) {
        this.raw1 = raw1;
        this.raw2 = raw2;
    }

    public static OFBitMask128 of(long raw1, long raw2) {
        if (raw1 == -1 && raw2 == -1)
            return ALL;
        if (raw1 == 0 && raw2 == 0)
            return NONE;
        return new OFBitMask128(raw1, raw2);
    }

    @Override
    public int getLength() {
        return LENGTH;
    }

    @Override
    public OFBitMask128 applyMask(OFBitMask128 mask) {
        return of(this.raw1 & mask.raw1, this.raw2 & mask.raw2);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof OFBitMask128))
            return false;
        OFBitMask128 other = (OFBitMask128)obj;
        return (other.raw1 == this.raw1 && other.raw2 == this.raw2);
    }

    @Override
    public int hashCode() {
        return (int)(31 * raw1 + raw2);
    }

    protected static boolean isBitOn(long raw1, long raw2, int bit) {
        if (bit < 0 || bit >= 128)
            throw new IndexOutOfBoundsException();
        long word;
        if (bit < 64) {
            word = raw2; // ports 0-63
        } else {
            word = raw1; // ports 64-127
            bit -= 64;
        }
        return (word & ((long)1 << bit)) != 0;
    }

    public void write16Bytes(ChannelBuffer cb) {
        cb.writeLong(raw1);
        cb.writeLong(raw2);
    }

    public static OFBitMask128 read16Bytes(ChannelBuffer cb) {
        long raw1 = cb.readLong();
        long raw2 = cb.readLong();
        return of(raw1, raw2);
    }

    public boolean isOn(int bit) {
        return isBitOn(raw1, raw2, bit);
    }

    @Override
    public String toString() {
        return (String.format("%64s", Long.toBinaryString(raw2)) + String.format("%64s", Long.toBinaryString(raw1))).replaceAll(" ", "0");
    }

    @Override
    public int compareTo(OFBitMask128 o) {
        long c = this.raw1 - o.raw1;
        if (c != 0)
            return Long.signum(c);
        return Long.signum(this.raw2 - o.raw2);
    }

    @Override
    public void putTo(PrimitiveSink sink) {
        sink.putLong(raw1);
        sink.putLong(raw2);
    }

}
