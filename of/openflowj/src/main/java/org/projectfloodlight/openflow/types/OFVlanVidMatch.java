package org.projectfloodlight.openflow.types;

import java.util.Arrays;

import javax.annotation.Nullable;

import org.jboss.netty.buffer.ChannelBuffer;
import org.projectfloodlight.openflow.exceptions.OFParseError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.hash.PrimitiveSink;
import com.google.common.primitives.Shorts;

/** Represents an OpenFlow Vlan VID for use in Matches, as specified by the OpenFlow 1.3 spec.
 *
 *  <b> Note: this is not just the 12-bit vlan tag. OpenFlow defines
 *      the additional mask bits 0x1000 to represent the presence of a vlan
 *      tag. This additional bit will be stripped when writing a OF1.0 value
 *      tag.
 *  </b>
 *
 *
 * @author Andreas Wundsam <andreas.wundsam@bigswitch.com>
 *
 */
public class OFVlanVidMatch implements OFValueType<OFVlanVidMatch> {
    private static final Logger logger = LoggerFactory.getLogger(OFVlanVidMatch.class);

    private static final short VALIDATION_MASK = 0x1FFF;
    private static final short PRESENT_VAL = 0x1000;
    private static final short VLAN_MASK = 0x0FFF;
    private static final short NONE_VAL = 0x0000;
    private static final short UNTAGGED_VAL_OF13 = (short) 0x0000;
    private static final short UNTAGGED_VAL_OF10 = (short) 0xFFFF;
    final static int LENGTH = 2;

    /** presence of a VLAN tag is indicated by the presence of bit 0x1000 */
    public static final OFVlanVidMatch PRESENT = new OFVlanVidMatch(PRESENT_VAL);

    /** this value means 'not set' in OF1.0 (e.g., in a match). not used elsewhere */
    public static final OFVlanVidMatch NONE = new OFVlanVidMatch(NONE_VAL);

    /** for use with masking operations */
    public static final OFVlanVidMatch NO_MASK = new OFVlanVidMatch((short)0xFFFF);
    public static final OFVlanVidMatch FULL_MASK = NONE;

    /** an untagged packet is specified as 0000 in OF 1.0, but 0xFFFF in OF1.0. Special case that. */
    public static final OFVlanVidMatch UNTAGGED = new OFVlanVidMatch(NONE_VAL) {
        @Override
        public void write2BytesOF10(ChannelBuffer c) {
            c.writeShort(UNTAGGED_VAL_OF10);
        }
    };

    private final short vid;

    private OFVlanVidMatch(short vid) {
        this.vid = vid;
    }

    public static OFVlanVidMatch ofRawVid(short vid) {
        if(vid == UNTAGGED_VAL_OF13)
            return UNTAGGED;
        else if(vid == PRESENT_VAL)
            return PRESENT;
        else if(vid == UNTAGGED_VAL_OF10) {
            // workaround for IVS sometimes sending 0F1.0 untagged (0xFFFF) values
            logger.warn("Warning: received OF1.0 untagged vlan value (0xFFFF) in OF1.3 VlanVid. Treating as UNTAGGED");
            return UNTAGGED;
        } else if ((vid & VALIDATION_MASK) != vid)
            throw new IllegalArgumentException(String.format("Illegal VLAN value: %x", vid));
        return new OFVlanVidMatch(vid);
    }

    public static OFVlanVidMatch ofVlanVid(VlanVid vid) {
        if(vid == null)
            return UNTAGGED;
        else if(VlanVid.NO_MASK.equals(vid))
            // NO_MASK is a special value in that it doesn't fit in the
            // allowed value space (0x1FFF) of this type. Do a manual conversion
            return NO_MASK;
        else
            return ofVlan(vid.getVlan());
    }


    public static OFVlanVidMatch ofVlan(int vlan) {
        if( (vlan & VLAN_MASK) != vlan)
            throw new IllegalArgumentException(String.format("Illegal VLAN value: %x", vlan));
        return ofRawVid( (short) (vlan | PRESENT_VAL));
    }

    public static OFVlanVidMatch ofVlanOF10(short of10vlan) {
        if(of10vlan == NONE_VAL) {
            return NONE;
        } else if(of10vlan == UNTAGGED_VAL_OF10) {
            return UNTAGGED;
        } else {
            return ofVlan(of10vlan);
        }
    }

    /** @return whether or not this VlanId has the present (0x1000) bit set */
    public boolean isPresentBitSet() {
       return (vid & PRESENT_VAL) != 0;
    }

    /** @return the actual VLAN tag this vid identifies */
    public short getVlan() {
        return (short) (vid & VLAN_MASK);
    }

    /** @return the actual vlan tag this vid identifies as a VlanVid object, if this
     *  VlanVidMatch has the present bit set (i.e., identifies a tagged VLAN).
     *  Else, returns null.
     */
    @Nullable
    public VlanVid getVlanVid() {
        if(this.equals(NO_MASK))
            return VlanVid.NO_MASK;
        else if(isPresentBitSet())
            return VlanVid.ofVlan((short) (vid & VLAN_MASK));
        else
            return null;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof OFVlanVidMatch))
            return false;
        OFVlanVidMatch other = (OFVlanVidMatch)obj;
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

    public short getRawVid() {
        return vid;
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

    public static OFVlanVidMatch read2Bytes(ChannelBuffer c) throws OFParseError {
        return OFVlanVidMatch.ofRawVid(c.readShort());
    }

    public static OFVlanVidMatch read2BytesOF10(ChannelBuffer c) throws OFParseError {
        return OFVlanVidMatch.ofVlanOF10(c.readShort());
    }

    @Override
    public OFVlanVidMatch applyMask(OFVlanVidMatch mask) {
        return OFVlanVidMatch.ofRawVid((short)(this.vid & mask.vid));
    }

    @Override
    public int compareTo(OFVlanVidMatch o) {
        return Shorts.compare(vid, o.vid);
    }
    @Override
    public void putTo(PrimitiveSink sink) {
        sink.putShort(vid);
    }
}
