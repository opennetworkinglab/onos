package org.projectfloodlight.openflow.types;

import javax.annotation.concurrent.Immutable;

import org.jboss.netty.buffer.ChannelBuffer;

import com.google.common.hash.PrimitiveSink;
import com.google.common.primitives.UnsignedInts;

@Immutable
public class VRF implements OFValueType<VRF> {
    static final int LENGTH = 4;
    private final int rawValue;

    public static final VRF ZERO = VRF.of(0x0);
    public static final VRF NO_MASK = VRF.of(0xFFFFFFFF);
    public static final VRF FULL_MASK = VRF.of(0x00000000);

    private VRF(final int rawValue) {
        this.rawValue = rawValue;
    }

    public static VRF of(final int raw) {
        return new VRF(raw);
    }

    public int getInt() {
        return rawValue;
    }

    @Override
    public int getLength() {
        return LENGTH;
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
        VRF other = (VRF) obj;
        if (rawValue != other.rawValue)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return Integer.toString(rawValue);
    }

    public void write4Bytes(ChannelBuffer c) {
        c.writeInt(rawValue);
    }

    public static VRF read4Bytes(ChannelBuffer c) {
        return VRF.of(c.readInt());
    }

    @Override
    public VRF applyMask(VRF mask) {
        return VRF.of(this.rawValue & mask.rawValue);
    }

    @Override
    public int compareTo(VRF o) {
        return UnsignedInts.compare(rawValue, o.rawValue);
    }

    @Override
    public void putTo(PrimitiveSink sink) {
        sink.putInt(rawValue);
    }
}
