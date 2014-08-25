package org.projectfloodlight.openflow.types;

import org.jboss.netty.buffer.ChannelBuffer;
import org.projectfloodlight.openflow.exceptions.OFParseError;

import com.google.common.hash.PrimitiveSink;
import com.google.common.primitives.Shorts;

public class OFAuxId implements Comparable<OFAuxId>, PrimitiveSinkable {
    
    private static final short VALIDATION_MASK = 0xFF;
    
    private static final short MAIN_VAL = 0x0000;
    
    public static final OFAuxId MAIN = new OFAuxId(MAIN_VAL);
            
    private final short id;

    private OFAuxId(short id) {
        this.id = id;
    }

    public static OFAuxId of(short id) {
        switch(id) {
            case MAIN_VAL:
                return MAIN;
            default:
                if ((id & VALIDATION_MASK) != id)
                    throw new IllegalArgumentException("Illegal Aux id value: " + id);
                return new OFAuxId(id);
        }
    }

    public static OFAuxId of(int id) {
        if((id & VALIDATION_MASK) != id)
            throw new IllegalArgumentException("Illegal Aux id value: "+id);
        return of((short) id);
    }

    @Override
    public String toString() {
        return "0x" + Integer.toHexString(id);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + id;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        OFAuxId other = (OFAuxId) obj;
        if (id != other.id) return false;
        return true;
    }

    public short getValue() {
        return id;
    }

    public void writeByte(ChannelBuffer c) {
        c.writeByte(this.id);
    }

    public static OFAuxId readByte(ChannelBuffer c) throws OFParseError {
        return OFAuxId.of(c.readUnsignedByte());
    }


    @Override
    public int compareTo(OFAuxId other) {
        return Shorts.compare(this.id, other.id);
    }

    @Override
    public void putTo(PrimitiveSink sink) {
        sink.putByte((byte) id);
    }

}
