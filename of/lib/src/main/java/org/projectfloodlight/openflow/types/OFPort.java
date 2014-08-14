package org.projectfloodlight.openflow.types;

import org.jboss.netty.buffer.ChannelBuffer;
import org.projectfloodlight.openflow.annotations.Immutable;
import org.projectfloodlight.openflow.exceptions.OFParseError;

import com.google.common.hash.PrimitiveSink;
import com.google.common.primitives.UnsignedInts;

/**
 * Abstraction of an logical / OpenFlow switch port (ofp_port_no) in OpenFlow.
 * Immutable. Note: Switch port numbers were changed in OpenFlow 1.1 from uint16
 * to uint32. This class uses a 32 bit representation internally. Port numbers
 * are converted from/to uint16 when constructed / getPortNumberasShort is
 * called. If this port is not representable in OpenFlow 1.0, an
 * IllegalStateException is raised.
 *
 * @author Andreas Wundsam <andreas.wundsam@bigswitch.com>
 */
@Immutable
public class OFPort implements OFValueType<OFPort> {
    static final int LENGTH = 4;

    // private int constants (OF1.1+) to avoid duplication in the code
    // should not have to use these outside this class
    private static final int OFPP_ANY_INT = 0xFFffFFff;
    private static final int OFPP_LOCAL_INT = 0xFFffFFfe;
    private static final int OFPP_CONTROLLER_INT = 0xFFffFFfd;
    private static final int OFPP_ALL_INT = 0xFFffFFfc;
    private static final int OFPP_FLOOD_INT = 0xFFffFFfb;
    private static final int OFPP_NORMAL_INT = 0xFFffFFfa;
    private static final int OFPP_TABLE_INT = 0xFFffFFf9;
    private static final int OFPP_MAX_INT = 0xFFffFF00;
    private static final int OFPP_IN_PORT_INT = 0xFFffFFf8;

    // private short constants (OF1.0) to avoid duplication in the code
    // should not have to use these outside this class
    private static final short OFPP_ANY_SHORT = (short) 0xFFff;
    private static final short OFPP_LOCAL_SHORT = (short) 0xFFfe;
    private static final short OFPP_CONTROLLER_SHORT = (short) 0xFFfd;
    private static final short OFPP_ALL_SHORT = (short) 0xFFfc;
    private static final short OFPP_FLOOD_SHORT = (short) 0xFFfb;
    private static final short OFPP_NORMAL_SHORT = (short) 0xFFfa;
    private static final short OFPP_TABLE_SHORT = (short) 0xFFf9;
    private static final short OFPP_IN_PORT_SHORT = (short) 0xFFf8;
    private static final short OFPP_MAX_SHORT = (short) 0xFF00;
    private static final int OFPP_MAX_SHORT_UNSIGNED = 0xFF00;

    // ////////////// public constants - use to access well known OpenFlow ports

    /** Maximum number of physical and logical switch ports. */
    public final static OFPort MAX = new NamedPort(OFPP_MAX_INT, "max");

    /**
     * Send the packet out the input port. This reserved port must be explicitly
     * used in order to send back out of the input port.
     */
    public final static OFPort IN_PORT = new NamedPort(OFPP_IN_PORT_INT, "in_port");

    /**
     * Submit the packet to the first flow table NB: This destination port can
     * only be used in packet-out messages.
     */
    public final static OFPort TABLE = new NamedPort(OFPP_TABLE_INT, "table");

    /** Process with normal L2/L3 switching. */
    public final static OFPort NORMAL = new NamedPort(OFPP_NORMAL_INT, "normal");

    /**
     * All physical ports in VLAN, except input port and those blocked or link
     * down
     */
    public final static OFPort FLOOD = new NamedPort(OFPP_FLOOD_INT, "flood");

    /** All physical ports except input port */
    public final static OFPort ALL = new NamedPort(OFPP_ALL_INT, "all");

    /** Send to controller */
    public final static OFPort CONTROLLER =
            new NamedPort(OFPP_CONTROLLER_INT, "controller");

    /** local openflow "port" */
    public final static OFPort LOCAL = new NamedPort(OFPP_LOCAL_INT, "local");

    /**
     * Wildcard port used only for flow mod (delete) and flow stats requests.
     * Selects all flows regardless of output port (including flows with no
     * output port). NOTE: OpenFlow 1.0 calls this 'NONE'
     */
    public final static OFPort ANY = new NamedPort(OFPP_ANY_INT, "any");
    /** the wildcarded default for OpenFlow 1.0 (value: 0). Elsewhere in OpenFlow
     *  we need "ANY" as the default
     */
    public static final OFPort ZERO = OFPort.of(0);

    public static final OFPort NO_MASK = OFPort.of(0xFFFFFFFF);
    public static final OFPort FULL_MASK = ZERO;

    /** cache of frequently used ports */
    private static class PrecachedPort {
        private final static OFPort p0 = new OFPort(0);
        private final static OFPort p1 = new OFPort(1);
        private final static OFPort p2 = new OFPort(2);
        private final static OFPort p3 = new OFPort(3);
        private final static OFPort p4 = new OFPort(4);
        private final static OFPort p5 = new OFPort(5);
        private final static OFPort p6 = new OFPort(6);
        private final static OFPort p7 = new OFPort(7);
        private final static OFPort p8 = new OFPort(8);
        private final static OFPort p9 = new OFPort(9);
        private final static OFPort p10 = new OFPort(10);
        private final static OFPort p11 = new OFPort(11);
        private final static OFPort p12 = new OFPort(12);
        private final static OFPort p13 = new OFPort(13);
        private final static OFPort p14 = new OFPort(14);
        private final static OFPort p15 = new OFPort(15);
        private final static OFPort p16 = new OFPort(16);
        private final static OFPort p17 = new OFPort(17);
        private final static OFPort p18 = new OFPort(18);
        private final static OFPort p19 = new OFPort(19);
        private final static OFPort p20 = new OFPort(20);
        private final static OFPort p21 = new OFPort(21);
        private final static OFPort p22 = new OFPort(22);
        private final static OFPort p23 = new OFPort(23);
        private final static OFPort p24 = new OFPort(24);
        private final static OFPort p25 = new OFPort(25);
        private final static OFPort p26 = new OFPort(26);
        private final static OFPort p27 = new OFPort(27);
        private final static OFPort p28 = new OFPort(28);
        private final static OFPort p29 = new OFPort(29);
        private final static OFPort p31 = new OFPort(31);
        private final static OFPort p32 = new OFPort(32);
        private final static OFPort p33 = new OFPort(33);
        private final static OFPort p34 = new OFPort(34);
        private final static OFPort p35 = new OFPort(35);
        private final static OFPort p36 = new OFPort(36);
        private final static OFPort p37 = new OFPort(37);
        private final static OFPort p38 = new OFPort(38);
        private final static OFPort p39 = new OFPort(39);
        private final static OFPort p40 = new OFPort(40);
        private final static OFPort p41 = new OFPort(41);
        private final static OFPort p42 = new OFPort(42);
        private final static OFPort p43 = new OFPort(43);
        private final static OFPort p44 = new OFPort(44);
        private final static OFPort p45 = new OFPort(45);
        private final static OFPort p46 = new OFPort(46);
        private final static OFPort p47 = new OFPort(47);
        private final static OFPort p48 = new OFPort(48);
    }

    /** raw openflow port number as a signed 32 bit integer */
    private final int portNumber;

    /** private constructor. use of*-Factory methods instead */
    private OFPort(final int portNumber) {
        this.portNumber = portNumber;
    }

    /**
     * get an OFPort object corresponding to a raw 32-bit integer port number.
     * NOTE: The port object may either be newly allocated or cached. Do not
     * rely on either behavior.
     *
     * @param portNumber
     * @return a corresponding OFPort
     */
    public static OFPort ofInt(final int portNumber) {
        switch (portNumber) {
            case 0:
                return PrecachedPort.p0;
            case 1:
                return PrecachedPort.p1;
            case 2:
                return PrecachedPort.p2;
            case 3:
                return PrecachedPort.p3;
            case 4:
                return PrecachedPort.p4;
            case 5:
                return PrecachedPort.p5;
            case 6:
                return PrecachedPort.p6;
            case 7:
                return PrecachedPort.p7;
            case 8:
                return PrecachedPort.p8;
            case 9:
                return PrecachedPort.p9;
            case 10:
                return PrecachedPort.p10;
            case 11:
                return PrecachedPort.p11;
            case 12:
                return PrecachedPort.p12;
            case 13:
                return PrecachedPort.p13;
            case 14:
                return PrecachedPort.p14;
            case 15:
                return PrecachedPort.p15;
            case 16:
                return PrecachedPort.p16;
            case 17:
                return PrecachedPort.p17;
            case 18:
                return PrecachedPort.p18;
            case 19:
                return PrecachedPort.p19;
            case 20:
                return PrecachedPort.p20;
            case 21:
                return PrecachedPort.p21;
            case 22:
                return PrecachedPort.p22;
            case 23:
                return PrecachedPort.p23;
            case 24:
                return PrecachedPort.p24;
            case 25:
                return PrecachedPort.p25;
            case 26:
                return PrecachedPort.p26;
            case 27:
                return PrecachedPort.p27;
            case 28:
                return PrecachedPort.p28;
            case 29:
                return PrecachedPort.p29;
            case 31:
                return PrecachedPort.p31;
            case 32:
                return PrecachedPort.p32;
            case 33:
                return PrecachedPort.p33;
            case 34:
                return PrecachedPort.p34;
            case 35:
                return PrecachedPort.p35;
            case 36:
                return PrecachedPort.p36;
            case 37:
                return PrecachedPort.p37;
            case 38:
                return PrecachedPort.p38;
            case 39:
                return PrecachedPort.p39;
            case 40:
                return PrecachedPort.p40;
            case 41:
                return PrecachedPort.p41;
            case 42:
                return PrecachedPort.p42;
            case 43:
                return PrecachedPort.p43;
            case 44:
                return PrecachedPort.p44;
            case 45:
                return PrecachedPort.p45;
            case 46:
                return PrecachedPort.p46;
            case 47:
                return PrecachedPort.p47;
            case 48:
                return PrecachedPort.p48;
            case OFPP_MAX_INT:
                return MAX;
            case OFPP_IN_PORT_INT:
                return IN_PORT;
            case OFPP_TABLE_INT:
                return TABLE;
            case OFPP_NORMAL_INT:
                return NORMAL;
            case OFPP_FLOOD_INT:
                return FLOOD;
            case OFPP_ALL_INT:
                return ALL;
            case OFPP_CONTROLLER_INT:
                return CONTROLLER;
            case OFPP_LOCAL_INT:
                return LOCAL;
            case OFPP_ANY_INT:
                return ANY;
            default:
                // note: This means effectively : portNumber > OFPP_MAX_SHORT
                // accounting for
                // signedness of both portNumber and OFPP_MAX_INT(which is
                // -256).
                // Any unsigned integer value > OFPP_MAX_INT will be ]-256:0[
                // when read signed
                if (portNumber < 0 && portNumber > OFPP_MAX_INT)
                    throw new IllegalArgumentException("Unknown special port number: "
                            + portNumber);
                return new OFPort(portNumber);
        }
    }

    /** convenience function: delegates to ofInt */
    public static OFPort of(final int portNumber) {
        return ofInt(portNumber);
    }

    /**
     * get an OFPort object corresponding to a raw signed 16-bit integer port
     * number (OF1.0). Note that the port returned will have the corresponding
     * 32-bit integer value allocated as its port number. NOTE: The port object
     * may either be newly allocated or cached. Do not rely on either behavior.
     *
     * @param portNumber
     * @return a corresponding OFPort
     */
    public static OFPort ofShort(final short portNumber) {
        switch (portNumber) {
            case 0:
                return PrecachedPort.p0;
            case 1:
                return PrecachedPort.p1;
            case 2:
                return PrecachedPort.p2;
            case 3:
                return PrecachedPort.p3;
            case 4:
                return PrecachedPort.p4;
            case 5:
                return PrecachedPort.p5;
            case 6:
                return PrecachedPort.p6;
            case 7:
                return PrecachedPort.p7;
            case 8:
                return PrecachedPort.p8;
            case 9:
                return PrecachedPort.p9;
            case 10:
                return PrecachedPort.p10;
            case 11:
                return PrecachedPort.p11;
            case 12:
                return PrecachedPort.p12;
            case 13:
                return PrecachedPort.p13;
            case 14:
                return PrecachedPort.p14;
            case 15:
                return PrecachedPort.p15;
            case 16:
                return PrecachedPort.p16;
            case 17:
                return PrecachedPort.p17;
            case 18:
                return PrecachedPort.p18;
            case 19:
                return PrecachedPort.p19;
            case 20:
                return PrecachedPort.p20;
            case 21:
                return PrecachedPort.p21;
            case 22:
                return PrecachedPort.p22;
            case 23:
                return PrecachedPort.p23;
            case 24:
                return PrecachedPort.p24;
            case 25:
                return PrecachedPort.p25;
            case 26:
                return PrecachedPort.p26;
            case 27:
                return PrecachedPort.p27;
            case 28:
                return PrecachedPort.p28;
            case 29:
                return PrecachedPort.p29;
            case 31:
                return PrecachedPort.p31;
            case 32:
                return PrecachedPort.p32;
            case 33:
                return PrecachedPort.p33;
            case 34:
                return PrecachedPort.p34;
            case 35:
                return PrecachedPort.p35;
            case 36:
                return PrecachedPort.p36;
            case 37:
                return PrecachedPort.p37;
            case 38:
                return PrecachedPort.p38;
            case 39:
                return PrecachedPort.p39;
            case 40:
                return PrecachedPort.p40;
            case 41:
                return PrecachedPort.p41;
            case 42:
                return PrecachedPort.p42;
            case 43:
                return PrecachedPort.p43;
            case 44:
                return PrecachedPort.p44;
            case 45:
                return PrecachedPort.p45;
            case 46:
                return PrecachedPort.p46;
            case 47:
                return PrecachedPort.p47;
            case 48:
                return PrecachedPort.p48;
            case OFPP_MAX_SHORT:
                return MAX;
            case OFPP_IN_PORT_SHORT:
                return IN_PORT;
            case OFPP_TABLE_SHORT:
                return TABLE;
            case OFPP_NORMAL_SHORT:
                return NORMAL;
            case OFPP_FLOOD_SHORT:
                return FLOOD;
            case OFPP_ALL_SHORT:
                return ALL;
            case OFPP_CONTROLLER_SHORT:
                return CONTROLLER;
            case OFPP_LOCAL_SHORT:
                return LOCAL;
            case OFPP_ANY_SHORT:
                return ANY;
            default:
                // note: This means effectively : portNumber > OFPP_MAX_SHORT
                // accounting for
                // signedness of both portNumber and OFPP_MAX_SHORT (which is
                // -256).
                // Any unsigned integer value > OFPP_MAX_SHORT will be ]-256:0[
                // when read signed
                if (portNumber < 0 && portNumber > OFPP_MAX_SHORT)
                    throw new IllegalArgumentException("Unknown special port number: "
                            + portNumber);
                return new OFPort(portNumber);
        }
    }

    /** return the port number as a int32 */
    public int getPortNumber() {
        return portNumber;
    }

    /**
     * return the port number as int16. Special ports as defined by the OpenFlow
     * spec will be converted to their OpenFlow 1.0 equivalent. port numbers >=
     * FF00 will cause a IllegalArgumentException to be thrown
     *
     * @throws IllegalArgumentException
     *             if a regular port number exceeds the maximum value in OF1.0
     **/
    public short getShortPortNumber() {

        switch (portNumber) {
            case OFPP_MAX_INT:
                return OFPP_MAX_SHORT;
            case OFPP_IN_PORT_INT:
                return OFPP_IN_PORT_SHORT;
            case OFPP_TABLE_INT:
                return OFPP_TABLE_SHORT;
            case OFPP_NORMAL_INT:
                return OFPP_NORMAL_SHORT;
            case OFPP_FLOOD_INT:
                return OFPP_FLOOD_SHORT;
            case OFPP_ALL_INT:
                return OFPP_ALL_SHORT;
            case OFPP_CONTROLLER_INT:
                return OFPP_CONTROLLER_SHORT;
            case OFPP_LOCAL_INT:
                return OFPP_LOCAL_SHORT;
            case OFPP_ANY_INT:
                return OFPP_ANY_SHORT;

            default:
                if (portNumber >= OFPP_MAX_SHORT_UNSIGNED || portNumber < 0)
                    throw new IllegalArgumentException("32bit Port number "
                            + U32.f(portNumber)
                            + " cannot be represented as uint16 (OF1.0)");

                return (short) portNumber;
        }
    }

    @Override
    public String toString() {
        return Long.toString(U32.f(portNumber));
    }

    /** Extension of OFPort for named ports */
    static class NamedPort extends OFPort {
        private final String name;

        NamedPort(final int portNo, final String name) {
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
        if (!(obj instanceof OFPort))
            return false;
        OFPort other = (OFPort)obj;
        if (other.portNumber != this.portNumber)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 53;
        int result = 1;
        result = prime * result + portNumber;
        return result;
    }

    public void write2Bytes(ChannelBuffer c) {
        c.writeShort(this.portNumber);
    }

    public static OFPort read2Bytes(ChannelBuffer c) throws OFParseError {
        return OFPort.ofShort(c.readShort());
    }

    public void write4Bytes(ChannelBuffer c) {
        c.writeInt(this.portNumber);
    }

    public static OFPort read4Bytes(ChannelBuffer c) throws OFParseError {
        return OFPort.of((int)(c.readUnsignedInt() & 0xFFFFFFFF));
    }

    @Override
    public OFPort applyMask(OFPort mask) {
        return OFPort.of(this.portNumber & mask.portNumber);
    }

    @Override
    public int compareTo(OFPort o) {
        return UnsignedInts.compare(this.portNumber, o.portNumber);
    }

    @Override
    public void putTo(PrimitiveSink sink) {
        sink.putInt(portNumber);
    }
}
