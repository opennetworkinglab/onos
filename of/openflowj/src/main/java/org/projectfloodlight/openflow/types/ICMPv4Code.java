package org.projectfloodlight.openflow.types;

import org.jboss.netty.buffer.ChannelBuffer;

import com.google.common.hash.PrimitiveSink;
import com.google.common.primitives.Shorts;

/**
 *
 * @author Yotam Harchol (yotam.harchol@bigswitch.com)
 *
 */
public class ICMPv4Code implements OFValueType<ICMPv4Code> {

    final static int LENGTH = 1;
    final static short MAX_CODE = 0xFF;

    private final short code;

    private static final short NONE_VAL = 0;
    public static final ICMPv4Code NONE = new ICMPv4Code(NONE_VAL);

    public static final ICMPv4Code NO_MASK = new ICMPv4Code((short)0xFFFF);
    public static final ICMPv4Code FULL_MASK = new ICMPv4Code((short)0x0000);

    private ICMPv4Code(short code) {
        this.code = code;
    }

    public static ICMPv4Code of(short code) {
        if(code == NONE_VAL)
            return NONE;

        if (code > MAX_CODE || code < 0)
            throw new IllegalArgumentException("Illegal ICMPv4 code: " + code);
        return new ICMPv4Code(code);
    }

    @Override
    public int getLength() {
        return LENGTH;
    }

    public short getCode() {
        return code;
    }

    public void writeByte(ChannelBuffer c) {
        c.writeByte(this.code);
    }

    public static ICMPv4Code readByte(ChannelBuffer c) {
        return ICMPv4Code.of(c.readUnsignedByte());
    }

    @Override
    public ICMPv4Code applyMask(ICMPv4Code mask) {
        return ICMPv4Code.of((short)(this.code & mask.code));
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + code;
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
        ICMPv4Code other = (ICMPv4Code) obj;
        if (code != other.code)
            return false;
        return true;
    }

    @Override
    public int compareTo(ICMPv4Code o) {
        return Shorts.compare(code, o.code);
    }

    @Override
    public void putTo(PrimitiveSink sink) {
        sink.putShort(code);
    }

    @Override
    public String toString() {
        return String.valueOf(this.code);
    }
}
