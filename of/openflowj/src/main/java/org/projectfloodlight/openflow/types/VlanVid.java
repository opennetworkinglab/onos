package org.projectfloodlight.openflow.types;

import java.util.Arrays;

import org.jboss.netty.buffer.ChannelBuffer;
import org.projectfloodlight.openflow.exceptions.OFParseError;

import com.google.common.hash.PrimitiveSink;
import com.google.common.primitives.Shorts;

/** Represents an 802.1Q Vlan VID (12 bits).
 *
 * @author Andreas Wundsam <andreas.wundsam@bigswitch.com>
 *
 */
public class VlanVid implements OFValueType<VlanVid> {

    private static final short VALIDATION_MASK = 0x0FFF;
    private static final short ZERO_VAL = 0x0000;
    final static int LENGTH = 2;

    /** this value means 'not set' in OF1.0 (e.g., in a match). not used elsewhere */
    public static final VlanVid ZERO = new VlanVid(ZERO_VAL);

    /** for use with masking operations */
    public static final VlanVid NO_MASK = new VlanVid((short)0xFFFF);
    public static final VlanVid FULL_MASK = ZERO;

    private final short vid;

    private VlanVid(short vid) {
        this.vid = vid;
    }

    public static VlanVid ofVlan(int vid) {
        if (vid == NO_MASK.vid)
            return NO_MASK;
        if ((vid & VALIDATION_MASK) != vid)
            throw new IllegalArgumentException(String.format("Illegal VLAN value: %x", vid));
        return new VlanVid((short) vid);
    }

    /** @return the actual VLAN tag this vid identifies */
    public short getVlan() {
        return vid;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof VlanVid))
            return false;
        VlanVid other = (VlanVid)obj;
        if (other.vid != this.vid)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        int prime = 13873;
        return this.vid * prime;
    }

    @Override
    public String toString() {
        return "0x" + Integer.toHexString(vid);
    }

    @Override
    public int getLength() {
        return LENGTH;
    }

    private volatile byte[] bytesCache = null;

    public byte[] getBytes() {
        if (bytesCache == null) {
            synchronized (this) {
                if (bytesCache == null) {
                    bytesCache =
                            new byte[] { (byte) ((vid >>> 8) & 0xFF),
                                         (byte) ((vid >>> 0) & 0xFF) };
                }
            }
        }
        return Arrays.copyOf(bytesCache, bytesCache.length);
    }

    public void write2Bytes(ChannelBuffer c) {
        c.writeShort(this.vid);
    }

    public void write2BytesOF10(ChannelBuffer c) {
        c.writeShort(this.getVlan());
    }

    public static VlanVid read2Bytes(ChannelBuffer c) throws OFParseError {
        return VlanVid.ofVlan(c.readShort());
    }

    @Override
    public VlanVid applyMask(VlanVid mask) {
        return VlanVid.ofVlan((short)(this.vid & mask.vid));
    }

    @Override
    public int compareTo(VlanVid o) {
        return Shorts.compare(vid, o.vid);
    }

    @Override
    public void putTo(PrimitiveSink sink) {
        sink.putShort(vid);
    }
}
