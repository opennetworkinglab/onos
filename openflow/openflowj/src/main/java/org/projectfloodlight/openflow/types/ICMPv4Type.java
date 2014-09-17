package org.projectfloodlight.openflow.types;

import org.jboss.netty.buffer.ChannelBuffer;

import com.google.common.hash.PrimitiveSink;
import com.google.common.primitives.Shorts;

public class ICMPv4Type implements OFValueType<ICMPv4Type> {
    final static int LENGTH = 1;

    private static final short VAL_ECHO_REPLY    = 0;
    private static final short VAL_DESTINATION_UNREACHABLE   = 3;
    private static final short VAL_SOURCE_QUENCH = 4;
    private static final short VAL_REDIRECT  = 5;
    private static final short VAL_ALTERNATE_HOST_ADDRESS    = 6;
    private static final short VAL_ECHO  = 8;
    private static final short VAL_ROUTER_ADVERTISEMENT  = 9;
    private static final short VAL_ROUTER_SOLICITATION   = 10;
    private static final short VAL_TIME_EXCEEDED = 11;
    private static final short VAL_PARAMETER_PROBLEM = 12;
    private static final short VAL_TIMESTAMP = 13;
    private static final short VAL_TIMESTAMP_REPLY   = 14;
    private static final short VAL_INFORMATION_REQUEST   = 15;
    private static final short VAL_INFORMATION_REPLY = 16;
    private static final short VAL_ADDRESS_MASK_REQUEST  = 17;
    private static final short VAL_ADDRESS_MASK_REPLY    = 18;
    private static final short VAL_TRACEROUTE    = 30;
    private static final short VAL_DATAGRAM_CONVERSION_ERROR = 31;
    private static final short VAL_MOBILE_HOST_REDIRECT  = 32;
    private static final short VAL_IPV6_WHERE_ARE_YOU    = 33;
    private static final short VAL_IPV6_I_AM_HERE    = 34;
    private static final short VAL_MOBILE_REGISTRATION_REQUEST   = 35;
    private static final short VAL_MOBILE_REGISTRATION_REPLY = 36;
    private static final short VAL_DOMAIN_NAME_REQUEST   = 37;
    private static final short VAL_DOMAIN_NAME_REPLY = 38;
    private static final short VAL_SKIP  = 39;
    private static final short VAL_PHOTURIS  = 40;
    private static final short VAL_EXPERIMENTAL_MOBILITY = 41;

    public static final ICMPv4Type ECHO_REPLY   = new ICMPv4Type(VAL_ECHO_REPLY);
    public static final ICMPv4Type DESTINATION_UNREACHABLE  = new ICMPv4Type(VAL_DESTINATION_UNREACHABLE);
    public static final ICMPv4Type SOURCE_QUENCH    = new ICMPv4Type(VAL_SOURCE_QUENCH);
    public static final ICMPv4Type REDIRECT = new ICMPv4Type(VAL_REDIRECT);
    public static final ICMPv4Type ALTERNATE_HOST_ADDRESS   = new ICMPv4Type(VAL_ALTERNATE_HOST_ADDRESS);
    public static final ICMPv4Type ECHO = new ICMPv4Type(VAL_ECHO);
    public static final ICMPv4Type ROUTER_ADVERTISEMENT = new ICMPv4Type(VAL_ROUTER_ADVERTISEMENT);
    public static final ICMPv4Type ROUTER_SOLICITATION  = new ICMPv4Type(VAL_ROUTER_SOLICITATION);
    public static final ICMPv4Type TIME_EXCEEDED    = new ICMPv4Type(VAL_TIME_EXCEEDED);
    public static final ICMPv4Type PARAMETER_PROBLEM    = new ICMPv4Type(VAL_PARAMETER_PROBLEM);
    public static final ICMPv4Type TIMESTAMP    = new ICMPv4Type(VAL_TIMESTAMP);
    public static final ICMPv4Type TIMESTAMP_REPLY  = new ICMPv4Type(VAL_TIMESTAMP_REPLY);
    public static final ICMPv4Type INFORMATION_REQUEST  = new ICMPv4Type(VAL_INFORMATION_REQUEST);
    public static final ICMPv4Type INFORMATION_REPLY    = new ICMPv4Type(VAL_INFORMATION_REPLY);
    public static final ICMPv4Type ADDRESS_MASK_REQUEST = new ICMPv4Type(VAL_ADDRESS_MASK_REQUEST);
    public static final ICMPv4Type ADDRESS_MASK_REPLY   = new ICMPv4Type(VAL_ADDRESS_MASK_REPLY);
    public static final ICMPv4Type TRACEROUTE   = new ICMPv4Type(VAL_TRACEROUTE);
    public static final ICMPv4Type DATAGRAM_CONVERSION_ERROR    = new ICMPv4Type(VAL_DATAGRAM_CONVERSION_ERROR);
    public static final ICMPv4Type MOBILE_HOST_REDIRECT = new ICMPv4Type(VAL_MOBILE_HOST_REDIRECT);
    public static final ICMPv4Type IPV6_WHERE_ARE_YOU  = new ICMPv4Type(VAL_IPV6_WHERE_ARE_YOU);
    public static final ICMPv4Type IPV6_I_AM_HERE = new ICMPv4Type(VAL_IPV6_I_AM_HERE);
    public static final ICMPv4Type MOBILE_REGISTRATION_REQUEST  = new ICMPv4Type(VAL_MOBILE_REGISTRATION_REQUEST);
    public static final ICMPv4Type MOBILE_REGISTRATION_REPLY    = new ICMPv4Type(VAL_MOBILE_REGISTRATION_REPLY);
    public static final ICMPv4Type DOMAIN_NAME_REQUEST  = new ICMPv4Type(VAL_DOMAIN_NAME_REQUEST);
    public static final ICMPv4Type DOMAIN_NAME_REPLY    = new ICMPv4Type(VAL_DOMAIN_NAME_REPLY);
    public static final ICMPv4Type SKIP = new ICMPv4Type(VAL_SKIP);
    public static final ICMPv4Type PHOTURIS = new ICMPv4Type(VAL_PHOTURIS);
    public static final ICMPv4Type EXPERIMENTAL_MOBILITY    = new ICMPv4Type(VAL_EXPERIMENTAL_MOBILITY);

    // HACK alert - we're disapproriating ECHO_REPLY (value 0) as 'none' as well
    public static final ICMPv4Type NONE   = ECHO_REPLY;

    public static final ICMPv4Type NO_MASK = new ICMPv4Type((short)0xFFFF);
    public static final ICMPv4Type FULL_MASK = new ICMPv4Type((short)0x0000);

    private final short type;

    private static final int MIN_TYPE = 0;
    private static final int MAX_TYPE = 0xFF;

    private ICMPv4Type(short type) {
        this.type = type;
    }

    public static ICMPv4Type of(short type) {
        if (type < MIN_TYPE || type > MAX_TYPE)
            throw new IllegalArgumentException("Invalid ICMPv4 type: " + type);
        switch (type) {
            case VAL_ECHO_REPLY:
                return ECHO_REPLY;
            case VAL_DESTINATION_UNREACHABLE:
                return DESTINATION_UNREACHABLE;
            case VAL_SOURCE_QUENCH:
                return SOURCE_QUENCH;
            case VAL_REDIRECT:
                return REDIRECT;
            case VAL_ALTERNATE_HOST_ADDRESS:
                return ALTERNATE_HOST_ADDRESS;
            case VAL_ECHO:
                return ECHO;
            case VAL_ROUTER_ADVERTISEMENT:
                return ROUTER_ADVERTISEMENT;
            case VAL_ROUTER_SOLICITATION:
                return ROUTER_SOLICITATION;
            case VAL_TIME_EXCEEDED:
                return TIME_EXCEEDED;
            case VAL_PARAMETER_PROBLEM:
                return PARAMETER_PROBLEM;
            case VAL_TIMESTAMP:
                return TIMESTAMP;
            case VAL_TIMESTAMP_REPLY:
                return TIMESTAMP_REPLY;
            case VAL_INFORMATION_REQUEST:
                return INFORMATION_REQUEST;
            case VAL_INFORMATION_REPLY:
                return INFORMATION_REPLY;
            case VAL_ADDRESS_MASK_REQUEST:
                return ADDRESS_MASK_REQUEST;
            case VAL_ADDRESS_MASK_REPLY:
                return ADDRESS_MASK_REPLY;
            case VAL_TRACEROUTE:
                return TRACEROUTE;
            case VAL_DATAGRAM_CONVERSION_ERROR:
                return DATAGRAM_CONVERSION_ERROR;
            case VAL_MOBILE_HOST_REDIRECT:
                return MOBILE_HOST_REDIRECT;
            case VAL_IPV6_WHERE_ARE_YOU:
                return IPV6_WHERE_ARE_YOU;
            case VAL_IPV6_I_AM_HERE:
                return IPV6_I_AM_HERE;
            case VAL_MOBILE_REGISTRATION_REQUEST:
                return MOBILE_REGISTRATION_REQUEST;
            case VAL_MOBILE_REGISTRATION_REPLY:
                return MOBILE_REGISTRATION_REPLY;
            case VAL_DOMAIN_NAME_REQUEST:
                return DOMAIN_NAME_REQUEST;
            case VAL_DOMAIN_NAME_REPLY:
                return DOMAIN_NAME_REPLY;
            case VAL_SKIP:
                return SKIP;
            case VAL_PHOTURIS:
                return PHOTURIS;
            case VAL_EXPERIMENTAL_MOBILITY:
                return EXPERIMENTAL_MOBILITY;
            default:
                return new ICMPv4Type(type);
        }
    }

    @Override
    public int getLength() {
        return LENGTH;
    }

    public short getType() {
        return type;
    }

    public void writeByte(ChannelBuffer c) {
        c.writeByte(this.type);
    }

    public static ICMPv4Type readByte(ChannelBuffer c) {
        return ICMPv4Type.of(c.readUnsignedByte());
    }

    @Override
    public ICMPv4Type applyMask(ICMPv4Type mask) {
        return ICMPv4Type.of((short)(this.type & mask.type));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + type;
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
        ICMPv4Type other = (ICMPv4Type) obj;
        if (type != other.type)
            return false;
        return true;
    }

    @Override
    public int compareTo(ICMPv4Type o) {
        return Shorts.compare(type, o.type);
    }

    @Override
    public void putTo(PrimitiveSink sink) {
        sink.putShort(type);
    }

    @Override
    public String toString() {
        return String.valueOf(this.type);
    }
}
