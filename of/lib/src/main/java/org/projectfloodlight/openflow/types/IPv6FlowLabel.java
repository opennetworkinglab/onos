package org.projectfloodlight.openflow.types;

import org.jboss.netty.buffer.ChannelBuffer;
import org.projectfloodlight.openflow.exceptions.OFParseError;

import com.google.common.hash.PrimitiveSink;
import com.google.common.primitives.UnsignedInts;

public class IPv6FlowLabel implements OFValueType<IPv6FlowLabel> {

    static final int LENGTH = 4;

    private final int label;

    private final static int NONE_VAL = 0x0;
    public static final IPv6FlowLabel NONE = new IPv6FlowLabel(NONE_VAL);

    public static final IPv6FlowLabel NO_MASK = IPv6FlowLabel.of(0xFFFFFFFF);
    public static final IPv6FlowLabel FULL_MASK = IPv6FlowLabel.of(0x0);

    private IPv6FlowLabel(int label) {
        this.label = label;
    }

    public static IPv6FlowLabel of(int label) {
        if(label == NONE_VAL)
            return NONE;
        return new IPv6FlowLabel(label);
    }

    @Override
    public int getLength() {
        return LENGTH;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof IPv6FlowLabel))
            return false;
        IPv6FlowLabel other = (IPv6FlowLabel)obj;
        if (other.label != this.label)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 59;
        int result = 1;
        result = prime * result + label;
        return result;
    }

    @Override
    public String toString() {
        return Integer.toHexString(label);
    }

    public void write4Bytes(ChannelBuffer c) {
        c.writeInt(this.label);
    }

    public static IPv6FlowLabel read4Bytes(ChannelBuffer c) throws OFParseError {
        return IPv6FlowLabel.of((int)(c.readUnsignedInt() & 0xFFFFFFFF));
    }

    @Override
    public IPv6FlowLabel applyMask(IPv6FlowLabel mask) {
        return IPv6FlowLabel.of(this.label & mask.label);
    }

    public int getIPv6FlowLabelValue() {
        return label;
    }

    @Override
    public int compareTo(IPv6FlowLabel o) {
        return UnsignedInts.compare(label, o.label);
    }

    @Override
    public void putTo(PrimitiveSink sink) {
        sink.putInt(this.label);
    }
}
