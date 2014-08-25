package org.projectfloodlight.openflow.types;

import org.jboss.netty.buffer.ChannelBuffer;

import com.google.common.hash.PrimitiveSink;
import com.google.common.primitives.UnsignedInts;

public class ArpOpcode implements OFValueType<ArpOpcode> {

    final static int LENGTH = 2;

    private static final int VAL_REQUEST   = 1;
    private static final int VAL_REPLY = 2;
    private static final int VAL_REQUEST_REVERSE   = 3;
    private static final int VAL_REPLY_REVERSE = 4;
    private static final int VAL_DRARP_REQUEST = 5;
    private static final int VAL_DRARP_REPLY   = 6;
    private static final int VAL_DRARP_ERROR   = 7;
    private static final int VAL_INARP_REQUEST = 8;
    private static final int VAL_INARP_REPLY   = 9;
    private static final int VAL_ARP_NAK   = 10;
    private static final int VAL_MARS_REQUEST  = 11;
    private static final int VAL_MARS_MULTI    = 12;
    private static final int VAL_MARS_MSERV    = 13;
    private static final int VAL_MARS_JOIN = 14;
    private static final int VAL_MARS_LEAVE    = 15;
    private static final int VAL_MARS_NAK  = 16;
    private static final int VAL_MARS_UNSERV   = 17;
    private static final int VAL_MARS_SJOIN    = 18;
    private static final int VAL_MARS_SLEAVE   = 19;
    private static final int VAL_MARS_GROUPLIST_REQUEST    = 20;
    private static final int VAL_MARS_GROUPLIST_REPLY  = 21;
    private static final int VAL_MARS_REDIRECT_MAP = 22;
    private static final int VAL_MAPOS_UNARP   = 23;
    private static final int VAL_OP_EXP1   = 24;
    private static final int VAL_OP_EXP2   = 25;

    public static final ArpOpcode REQUEST  = new ArpOpcode(VAL_REQUEST);
    public static final ArpOpcode REPLY    = new ArpOpcode(VAL_REPLY);
    public static final ArpOpcode REQUEST_REVERSE  = new ArpOpcode(VAL_REQUEST_REVERSE);
    public static final ArpOpcode REPLY_REVERSE    = new ArpOpcode(VAL_REPLY_REVERSE);
    public static final ArpOpcode DRARP_REQUEST    = new ArpOpcode(VAL_DRARP_REQUEST);
    public static final ArpOpcode DRARP_REPLY  = new ArpOpcode(VAL_DRARP_REPLY);
    public static final ArpOpcode DRARP_ERROR  = new ArpOpcode(VAL_DRARP_ERROR);
    public static final ArpOpcode INARP_REQUEST    = new ArpOpcode(VAL_INARP_REQUEST);
    public static final ArpOpcode INARP_REPLY  = new ArpOpcode(VAL_INARP_REPLY);
    public static final ArpOpcode ARP_NAK  = new ArpOpcode(VAL_ARP_NAK);
    public static final ArpOpcode MARS_REQUEST = new ArpOpcode(VAL_MARS_REQUEST);
    public static final ArpOpcode MARS_MULTI   = new ArpOpcode(VAL_MARS_MULTI);
    public static final ArpOpcode MARS_MSERV   = new ArpOpcode(VAL_MARS_MSERV);
    public static final ArpOpcode MARS_JOIN    = new ArpOpcode(VAL_MARS_JOIN);
    public static final ArpOpcode MARS_LEAVE   = new ArpOpcode(VAL_MARS_LEAVE);
    public static final ArpOpcode MARS_NAK = new ArpOpcode(VAL_MARS_NAK);
    public static final ArpOpcode MARS_UNSERV  = new ArpOpcode(VAL_MARS_UNSERV);
    public static final ArpOpcode MARS_SJOIN   = new ArpOpcode(VAL_MARS_SJOIN);
    public static final ArpOpcode MARS_SLEAVE  = new ArpOpcode(VAL_MARS_SLEAVE);
    public static final ArpOpcode MARS_GROUPLIST_REQUEST   = new ArpOpcode(VAL_MARS_GROUPLIST_REQUEST);
    public static final ArpOpcode MARS_GROUPLIST_REPLY = new ArpOpcode(VAL_MARS_GROUPLIST_REPLY);
    public static final ArpOpcode MARS_REDIRECT_MAP    = new ArpOpcode(VAL_MARS_REDIRECT_MAP);
    public static final ArpOpcode MAPOS_UNARP  = new ArpOpcode(VAL_MAPOS_UNARP);
    public static final ArpOpcode OP_EXP1  = new ArpOpcode(VAL_OP_EXP1);
    public static final ArpOpcode OP_EXP2  = new ArpOpcode(VAL_OP_EXP2);

    private static final int MIN_OPCODE = 0;
    private static final int MAX_OPCODE = 0xFFFF;

    private static final int NONE_VAL = 0;
    public static final ArpOpcode NONE = new ArpOpcode(NONE_VAL);

    public static final ArpOpcode NO_MASK = new ArpOpcode(0xFFFFFFFF);
    public static final ArpOpcode FULL_MASK = new ArpOpcode(0x00000000);

    private final int opcode;

    private ArpOpcode(int opcode) {
        this.opcode = opcode;
    }

    @Override
    public int getLength() {
        return LENGTH;
    }

    public int getOpcode() {
        return this.opcode;
    }

    public static ArpOpcode of(int opcode) {
        if (opcode < MIN_OPCODE || opcode > MAX_OPCODE)
            throw new IllegalArgumentException("Invalid ARP opcode: " + opcode);
        switch (opcode) {
            case NONE_VAL:
                return NONE;
            case VAL_REQUEST:
                return REQUEST;
            case VAL_REPLY:
                return REPLY;
            case VAL_REQUEST_REVERSE:
                return REQUEST_REVERSE;
            case VAL_REPLY_REVERSE:
                return REPLY_REVERSE;
            case VAL_DRARP_REQUEST:
                return DRARP_REQUEST;
            case VAL_DRARP_REPLY:
                return DRARP_REPLY;
            case VAL_DRARP_ERROR:
                return DRARP_ERROR;
            case VAL_INARP_REQUEST:
                return INARP_REQUEST;
            case VAL_INARP_REPLY:
                return INARP_REPLY;
            case VAL_ARP_NAK:
                return ARP_NAK;
            case VAL_MARS_REQUEST:
                return MARS_REQUEST;
            case VAL_MARS_MULTI:
                return MARS_MULTI;
            case VAL_MARS_MSERV:
                return MARS_MSERV;
            case VAL_MARS_JOIN:
                return MARS_JOIN;
            case VAL_MARS_LEAVE:
                return MARS_LEAVE;
            case VAL_MARS_NAK:
                return MARS_NAK;
            case VAL_MARS_UNSERV:
                return MARS_UNSERV;
            case VAL_MARS_SJOIN:
                return MARS_SJOIN;
            case VAL_MARS_SLEAVE:
                return MARS_SLEAVE;
            case VAL_MARS_GROUPLIST_REQUEST:
                return MARS_GROUPLIST_REQUEST;
            case VAL_MARS_GROUPLIST_REPLY:
                return MARS_GROUPLIST_REPLY;
            case VAL_MARS_REDIRECT_MAP:
                return MARS_REDIRECT_MAP;
            case VAL_MAPOS_UNARP:
                return MAPOS_UNARP;
            case VAL_OP_EXP1:
                return OP_EXP1;
            case VAL_OP_EXP2:
                return OP_EXP2;
            default:
                return new ArpOpcode(opcode);
        }
    }

    public void write2Bytes(ChannelBuffer c) {
        c.writeShort(this.opcode);
    }

    public static ArpOpcode read2Bytes(ChannelBuffer c) {
        return ArpOpcode.of(c.readUnsignedShort());
    }

    @Override
    public ArpOpcode applyMask(ArpOpcode mask) {
        return ArpOpcode.of(this.opcode & mask.opcode);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + opcode;
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
        ArpOpcode other = (ArpOpcode) obj;
        if (opcode != other.opcode)
            return false;
        return true;
    }

    @Override
    public int compareTo(ArpOpcode o) {
        return UnsignedInts.compare(opcode, o.opcode);
    }

    @Override
    public void putTo(PrimitiveSink sink) {
        sink.putShort((short) this.opcode);
    }

    @Override
    public String toString() {
        return String.valueOf(this.opcode);
    }
}
