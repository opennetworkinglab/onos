package org.projectfloodlight.openflow.types;

import org.jboss.netty.buffer.ChannelBuffer;
import org.projectfloodlight.openflow.exceptions.OFParseError;

import com.google.common.hash.PrimitiveSink;

public enum IpEcn implements OFValueType<IpEcn> {
    ECN_00((byte)0),
    ECN_01((byte)1),
    ECN_10((byte)2),
    ECN_11((byte)3),
    ECN_NO_MASK((byte)0xFF);

    public static final IpEcn NONE = ECN_00;
    public static final IpEcn NO_MASK = ECN_NO_MASK;
    public static final IpEcn FULL_MASK = ECN_00;

    static final int LENGTH = 1;

    private final byte ecn;

    private IpEcn(byte ecn) {
        this.ecn = ecn;
    }

    public static IpEcn of(byte ecn) {
        switch (ecn) {
            case 0:
                return ECN_00;
            case 1:
                return ECN_01;
            case 2:
                return ECN_10;
            case 3:
                return ECN_11;
            default:
                throw new IllegalArgumentException("Illegal IP ECN value: " + ecn);
        }
    }

    @Override
    public int getLength() {
        return LENGTH;
    }

    @Override
    public String toString() {
        return (ecn < 3 ? "0" : "") + Integer.toBinaryString(ecn);
    }

    public void writeByte(ChannelBuffer c) {
        c.writeByte(this.ecn);
    }

    public static IpEcn readByte(ChannelBuffer c) throws OFParseError {
        return IpEcn.of((byte)(c.readUnsignedByte()));
    }

    @Override
    public IpEcn applyMask(IpEcn mask) {
        return IpEcn.of((byte)(this.ecn & mask.ecn));
    }

    public byte getEcnValue() {
        return ecn;
    }

    @Override
    public void putTo(PrimitiveSink sink) {
        sink.putByte(ecn);
    }
}
