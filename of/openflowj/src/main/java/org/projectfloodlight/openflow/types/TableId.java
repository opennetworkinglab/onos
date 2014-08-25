package org.projectfloodlight.openflow.types;

import org.jboss.netty.buffer.ChannelBuffer;
import org.projectfloodlight.openflow.exceptions.OFParseError;

import com.google.common.hash.PrimitiveSink;
import com.google.common.primitives.Shorts;

public class TableId implements OFValueType<TableId>, Comparable<TableId> {

    final static int LENGTH = 1;

    private static final short VALIDATION_MASK = 0x00FF;

    private static final short ALL_VAL = 0x00FF;
    private static final short NONE_VAL = 0x0000;
    public static final TableId NONE = new TableId(NONE_VAL);

    public static final TableId ALL = new TableId(ALL_VAL);
    public static final TableId ZERO = NONE;

    private final short id;

    private TableId(short id) {
        this.id = id;
    }

    public static TableId of(short id) {
        switch(id) {
            case NONE_VAL:
                return NONE;
            case ALL_VAL:
                return ALL;
            default:
                if ((id & VALIDATION_MASK) != id)
                    throw new IllegalArgumentException("Illegal Table id value: " + id);
                return new TableId(id);
        }
    }

    public static TableId of(int id) {
        if((id & VALIDATION_MASK) != id)
            throw new IllegalArgumentException("Illegal Table id value: "+id);
        return of((short) id);
    }

    @Override
    public String toString() {
        return "0x" + Integer.toHexString(id);
    }

    public short getValue() {
        return id;
    }

    @Override
    public int getLength() {
        return LENGTH;
    }

    public void writeByte(ChannelBuffer c) {
        c.writeByte(this.id);
    }

    public static TableId readByte(ChannelBuffer c) throws OFParseError {
        return TableId.of(c.readUnsignedByte());
    }

    @Override
    public TableId applyMask(TableId mask) {
        return TableId.of((short)(this.id & mask.id));
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof TableId))
            return false;
        TableId other = (TableId)obj;
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
    public int compareTo(TableId other) {
        return Shorts.compare(this.id, other.id);
    }

    @Override
    public void putTo(PrimitiveSink sink) {
        sink.putByte((byte) id);
    }

}
