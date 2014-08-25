package org.projectfloodlight.openflow.types;

import javax.annotation.concurrent.Immutable;

import org.jboss.netty.buffer.ChannelBuffer;

import com.google.common.hash.PrimitiveSink;
import com.google.common.primitives.UnsignedInts;

@Immutable
public class UDF implements OFValueType<UDF> {
    static final int LENGTH = 4;
    private final int rawValue;

    public static final UDF ZERO = UDF.of(0x0);
    public static final UDF NO_MASK = UDF.of(0xFFFFFFFF);
    public static final UDF FULL_MASK = UDF.of(0x00000000);

    private UDF(final int rawValue) {
        this.rawValue = rawValue;
    }

    public static UDF of(final int raw) {
        return new UDF(raw);
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
        UDF other = (UDF) obj;
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

    public static UDF read4Bytes(ChannelBuffer c) {
        return UDF.of(c.readInt());
    }

    @Override
    public UDF applyMask(UDF mask) {
        return UDF.of(this.rawValue & mask.rawValue);
    }

    @Override
    public int compareTo(UDF o) {
        return UnsignedInts.compare(rawValue, o.rawValue);
    }

    @Override
    public void putTo(PrimitiveSink sink) {
        sink.putInt(rawValue);
    }
}
