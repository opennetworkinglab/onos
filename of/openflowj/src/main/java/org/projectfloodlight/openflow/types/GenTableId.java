package org.projectfloodlight.openflow.types;

import org.jboss.netty.buffer.ChannelBuffer;
import org.projectfloodlight.openflow.exceptions.OFParseError;

import com.google.common.hash.PrimitiveSink;
import com.google.common.primitives.UnsignedInts;

public class GenTableId implements OFValueType<GenTableId>, Comparable<GenTableId> {
    final static int LENGTH = 2;

    private static final int VALIDATION_MASK = 0xFFFF;

    private static final int ALL_VAL = 0xFFFF;
    private static final int NONE_VAL = 0x0000;
    public static final GenTableId NONE = new GenTableId(NONE_VAL);

    public static final GenTableId ALL = new GenTableId(ALL_VAL);
    public static final GenTableId ZERO = NONE;

    private final int id;

    private GenTableId(int id) {
        this.id = id;
    }

    public static GenTableId of(int id) {
        switch(id) {
            case NONE_VAL:
                return NONE;
            case ALL_VAL:
                return ALL;
            default:
                if ((id & VALIDATION_MASK) != id)
                    throw new IllegalArgumentException("Illegal Table id value: " + id);
                return new GenTableId(id);
        }
    }

    @Override
    public String toString() {
        return "0x" + Integer.toHexString(id);
    }

    public int getValue() {
        return id;
    }

    @Override
    public int getLength() {
        return LENGTH;
    }

    public void write2Bytes(ChannelBuffer c) {
        c.writeShort(this.id);
    }

    public static GenTableId read2Bytes(ChannelBuffer c) throws OFParseError {
        return GenTableId.of(c.readUnsignedShort());
    }

    @Override
    public GenTableId applyMask(GenTableId mask) {
        return GenTableId.of(this.id & mask.id);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof GenTableId))
            return false;
        GenTableId other = (GenTableId)obj;
        if (other.id != this.id)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        int prime = 13873;
        return this.id * prime;
    }

    @Override
    public int compareTo(GenTableId other) {
        return UnsignedInts.compare(this.id, other.id);
    }

    @Override
    public void putTo(PrimitiveSink sink) {
        sink.putShort((byte) id);
    }

}
