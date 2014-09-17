package org.projectfloodlight.openflow.types;

import org.jboss.netty.buffer.ChannelBuffer;
import org.projectfloodlight.openflow.exceptions.OFParseError;

import com.google.common.hash.PrimitiveSink;

public enum IpDscp implements OFValueType<IpDscp> {
    DSCP_0((byte)0),
    DSCP_1((byte)1),
    DSCP_2((byte)2),
    DSCP_3((byte)3),
    DSCP_4((byte)4),
    DSCP_5((byte)5),
    DSCP_6((byte)6),
    DSCP_7((byte)7),
    DSCP_8((byte)8),
    DSCP_9((byte)9),
    DSCP_10((byte)10),
    DSCP_11((byte)11),
    DSCP_12((byte)12),
    DSCP_13((byte)13),
    DSCP_14((byte)14),
    DSCP_15((byte)15),
    DSCP_16((byte)16),
    DSCP_17((byte)17),
    DSCP_18((byte)18),
    DSCP_19((byte)19),
    DSCP_20((byte)20),
    DSCP_21((byte)21),
    DSCP_22((byte)22),
    DSCP_23((byte)23),
    DSCP_24((byte)24),
    DSCP_25((byte)25),
    DSCP_26((byte)26),
    DSCP_27((byte)27),
    DSCP_28((byte)28),
    DSCP_29((byte)29),
    DSCP_30((byte)30),
    DSCP_31((byte)31),
    DSCP_32((byte)32),
    DSCP_33((byte)33),
    DSCP_34((byte)34),
    DSCP_35((byte)35),
    DSCP_36((byte)36),
    DSCP_37((byte)37),
    DSCP_38((byte)38),
    DSCP_39((byte)39),
    DSCP_40((byte)40),
    DSCP_41((byte)41),
    DSCP_42((byte)42),
    DSCP_43((byte)43),
    DSCP_44((byte)44),
    DSCP_45((byte)45),
    DSCP_46((byte)46),
    DSCP_47((byte)47),
    DSCP_48((byte)48),
    DSCP_49((byte)49),
    DSCP_50((byte)50),
    DSCP_51((byte)51),
    DSCP_52((byte)52),
    DSCP_53((byte)53),
    DSCP_54((byte)54),
    DSCP_55((byte)55),
    DSCP_56((byte)56),
    DSCP_57((byte)57),
    DSCP_58((byte)58),
    DSCP_59((byte)59),
    DSCP_60((byte)60),
    DSCP_61((byte)61),
    DSCP_62((byte)62),
    DSCP_63((byte)63),
    DSCP_NO_MASK((byte)0xFF);

    static final int LENGTH = 1;

    public static final IpDscp NONE = DSCP_0;

    public static final IpDscp NO_MASK = DSCP_NO_MASK;
    public static final IpDscp FULL_MASK = DSCP_0;

    private final byte dscp;

    private IpDscp(byte dscp) {
        this.dscp = dscp;
    }

    public static IpDscp of(byte dscp) {
        switch (dscp) {
            case 0:
                return DSCP_0;
            case 1:
                return DSCP_1;
            case 2:
                return DSCP_2;
            case 3:
                return DSCP_3;
            case 4:
                return DSCP_4;
            case 5:
                return DSCP_5;
            case 6:
                return DSCP_6;
            case 7:
                return DSCP_7;
            case 8:
                return DSCP_8;
            case 9:
                return DSCP_9;
            case 10:
                return DSCP_10;
            case 11:
                return DSCP_11;
            case 12:
                return DSCP_12;
            case 13:
                return DSCP_13;
            case 14:
                return DSCP_14;
            case 15:
                return DSCP_15;
            case 16:
                return DSCP_16;
            case 17:
                return DSCP_17;
            case 18:
                return DSCP_18;
            case 19:
                return DSCP_19;
            case 20:
                return DSCP_20;
            case 21:
                return DSCP_21;
            case 22:
                return DSCP_22;
            case 23:
                return DSCP_23;
            case 24:
                return DSCP_24;
            case 25:
                return DSCP_25;
            case 26:
                return DSCP_26;
            case 27:
                return DSCP_27;
            case 28:
                return DSCP_28;
            case 29:
                return DSCP_29;
            case 30:
                return DSCP_30;
            case 31:
                return DSCP_31;
            case 32:
                return DSCP_32;
            case 33:
                return DSCP_33;
            case 34:
                return DSCP_34;
            case 35:
                return DSCP_35;
            case 36:
                return DSCP_36;
            case 37:
                return DSCP_37;
            case 38:
                return DSCP_38;
            case 39:
                return DSCP_39;
            case 40:
                return DSCP_40;
            case 41:
                return DSCP_41;
            case 42:
                return DSCP_42;
            case 43:
                return DSCP_43;
            case 44:
                return DSCP_44;
            case 45:
                return DSCP_45;
            case 46:
                return DSCP_46;
            case 47:
                return DSCP_47;
            case 48:
                return DSCP_48;
            case 49:
                return DSCP_49;
            case 50:
                return DSCP_50;
            case 51:
                return DSCP_51;
            case 52:
                return DSCP_52;
            case 53:
                return DSCP_53;
            case 54:
                return DSCP_54;
            case 55:
                return DSCP_55;
            case 56:
                return DSCP_56;
            case 57:
                return DSCP_57;
            case 58:
                return DSCP_58;
            case 59:
                return DSCP_59;
            case 60:
                return DSCP_60;
            case 61:
                return DSCP_61;
            case 62:
                return DSCP_62;
            case 63:
                return DSCP_63;
            default:
                throw new IllegalArgumentException("Illegal IPv4 DSCP value: " + dscp);
        }
    }

    @Override
    public int getLength() {
        return LENGTH;
    }

    @Override
    public String toString() {
        return Integer.toHexString(dscp);
    }

    public void writeByte(ChannelBuffer c) {
        c.writeByte(this.dscp);
    }

    public static IpDscp readByte(ChannelBuffer c) throws OFParseError {
        return IpDscp.of((byte)(c.readUnsignedByte()));
    }

    @Override
    public IpDscp applyMask(IpDscp mask) {
        return IpDscp.of((byte)(this.dscp & mask.dscp));
    }

    public byte getDscpValue() {
        return dscp;
    }

    @Override
    public void putTo(PrimitiveSink sink) {
        sink.putByte(dscp);
    }
}
