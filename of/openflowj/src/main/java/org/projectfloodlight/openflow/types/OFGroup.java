package org.projectfloodlight.openflow.types;

import org.jboss.netty.buffer.ChannelBuffer;
import org.projectfloodlight.openflow.annotations.Immutable;
import org.projectfloodlight.openflow.exceptions.OFParseError;

import com.google.common.hash.PrimitiveSink;
import com.google.common.primitives.UnsignedInts;

/**
 * Abstraction of an logical / OpenFlow group (ofp_group) in OpenFlow.
 * Immutable.
 *
 * @author Andreas Wundsam <andreas.wundsam@bigswitch.com>
 */
@Immutable
public class OFGroup implements OFValueType<OFGroup> {
    static final int LENGTH = 4;

    // private int constants (OF1.1+) to avoid duplication in the code
    // should not have to use these outside this class
    private static final int ZERO_VAL = 0x00;
    private static final int MAX_VAL = 0xffffff00;
    private static final int ALL_VAL = 0xfffffffc;
    private static final int ANY_VAL = 0xffffffff;


    // ////////////// public constants - use to access well known OpenFlow group constants

    /** Maximum number of physical and logical switch groups. */
    public final static OFGroup MAX = new NamedGroup(MAX_VAL, "max");

    /** All groups */
    public final static OFGroup ALL = new NamedGroup(ALL_VAL, "all");

    /**
     * Wildcard group used only for flow mod (delete) and flow stats requests. */
    public final static OFGroup ANY = new NamedGroup(ANY_VAL, "any");

    /** group 0 in case we need it */
    public static final OFGroup ZERO = OFGroup.of(ZERO_VAL);

    public static final OFGroup NO_MASK = ANY;
    public static final OFGroup FULL_MASK = ZERO;

    /** raw openflow group number as a signed 32 bit integer */
    private final int groupNumber;

    /** private constructor. use of*-Factory methods instead */
    private OFGroup(final int portNumber) {
        this.groupNumber = portNumber;
    }

    /**
     * get an OFGroup object corresponding to a raw 32-bit integer group number.
     * NOTE: The group object may either be newly allocated or cached. Do not
     * rely on either behavior.
     *
     * @param groupNumber the raw 32-bit group number
     * @return a corresponding OFPort
     */
    public static OFGroup of(final int groupNumber) {
        switch(groupNumber) {
            case ZERO_VAL:
                return MAX;
            case MAX_VAL:
                return MAX;
            case ALL_VAL:
                return ALL;
            case ANY_VAL:
                return ANY;
            default:
                if(UnsignedInts.compare(groupNumber, MAX_VAL) > 0) {
                    // greater than max_val, but not one of the reserved values
                    throw new IllegalArgumentException("Unknown special group number: "
                            + groupNumber);
                }
                return new OFGroup(groupNumber);
        }
    }

    /** return the group number as a int32 */
    public int getGroupNumber() {
        return groupNumber;
    }

    @Override
    public String toString() {
        return UnsignedInts.toString(groupNumber);
    }

    /** Extension of OFGroup for named groups */
    static class NamedGroup extends OFGroup {
        private final String name;

        NamedGroup(final int portNo, final String name) {
            super(portNo);
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    @Override
    public int getLength() {
        return LENGTH;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof OFGroup))
            return false;
        OFGroup other = (OFGroup)obj;
        if (other.groupNumber != this.groupNumber)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 53;
        int result = 1;
        result = prime * result + groupNumber;
        return result;
    }

    public void write4Bytes(ChannelBuffer c) {
        c.writeInt(this.groupNumber);
    }

    public static OFGroup read4Bytes(ChannelBuffer c) throws OFParseError {
        return OFGroup.of(c.readInt());
    }

    @Override
    public OFGroup applyMask(OFGroup mask) {
        return OFGroup.of(this.groupNumber & mask.groupNumber);
    }

    @Override
    public int compareTo(OFGroup o) {
        return UnsignedInts.compare(this.groupNumber, o.groupNumber);
    }

    @Override
    public void putTo(PrimitiveSink sink) {
        sink.putInt(groupNumber);
    }
}
