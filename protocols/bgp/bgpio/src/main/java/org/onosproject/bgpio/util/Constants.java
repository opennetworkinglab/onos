/*
 * Copyright 2015-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.bgpio.util;

/**
 * Provides Constants usage for BGP.
 */
public final class Constants {
    private Constants() {
    }

    public static final short TYPE_AND_LEN = 4;
    public static final short TYPE_AND_LEN_AS_SHORT = 4;
    public static final short TYPE_AND_LEN_AS_BYTE = 3;
    public static final int ISIS_LEVELONE = 1;
    public static final int ISIS_LEVELTWO = 2;
    public static final int OSPFV2 = 3;
    public static final int DIRECT = 4;
    public static final int STATIC_CONFIGURATION = 5;
    public static final int OSPFV3 = 6;
    public static final short AFI_VALUE = 16388;
    public static final byte VPN_SAFI_VALUE = (byte) 0x80;
    public static final byte SAFI_VALUE = 71;
    public static final short AFI_IPV4_UNICAST = 1;
    public static final byte SAFI_IPV4_UNICAST = 1;
    public static final short AFI_FLOWSPEC_VALUE = 1;
    public static final byte SAFI_FLOWSPEC_VALUE = (byte) 133;
    public static final byte VPN_SAFI_FLOWSPEC_VALUE = (byte) 134;

    /* TODO: The Capability Code
   for this capability is to be specified by the IANA.*/
    public static final short AFI_FLOWSPEC_RPD_VALUE = 1;
    public static final byte SAFI_FLOWSPEC_RPD_VALUE = (byte) 133;
    public static final byte VPN_SAFI_FLOWSPEC_RDP_VALUE = (byte) 134;

    public static final byte RPD_CAPABILITY_RECEIVE_VALUE = 0;
    public static final byte RPD_CAPABILITY_SEND_VALUE = 1;
    public static final byte RPD_CAPABILITY_SEND_RECEIVE_VALUE = 2;

    public static final int EXTRA_TRAFFIC = 0x01;
    public static final int UNPROTECTED = 0x02;
    public static final int SHARED = 0x04;
    public static final int DEDICATED_ONE_ISTO_ONE = 0x08;
    public static final int DEDICATED_ONE_PLUS_ONE = 0x10;
    public static final int ENHANCED = 0x20;
    public static final int RESERVED = 0x40;

    public static final byte BGP_EXTENDED_COMMUNITY = 0x10;

    public static final byte BGP_FLOWSPEC_DST_PREFIX = 0x01;
    public static final byte BGP_FLOWSPEC_SRC_PREFIX = 0x02;
    public static final byte BGP_FLOWSPEC_IP_PROTO = 0x03;
    public static final byte BGP_FLOWSPEC_PORT = 0x04;
    public static final byte BGP_FLOWSPEC_DST_PORT = 0x05;
    public static final byte BGP_FLOWSPEC_SRC_PORT = 0x06;
    public static final byte BGP_FLOWSPEC_ICMP_TP = 0x07;
    public static final byte BGP_FLOWSPEC_ICMP_CD = 0x08;
    public static final byte BGP_FLOWSPEC_TCP_FLAGS = 0x09;
    public static final byte BGP_FLOWSPEC_PCK_LEN = 0x0a;
    public static final byte BGP_FLOWSPEC_DSCP = 0x0b;
    public static final byte BGP_FLOWSPEC_FRAGMENT = 0x0c;

    public static final short BGP_FLOWSPEC_ACTION_TRAFFIC_RATE = (short) 0x8006;
    public static final short BGP_FLOWSPEC_ACTION_TRAFFIC_ACTION = (short) 0x8007;
    public static final short BGP_FLOWSPEC_ACTION_TRAFFIC_REDIRECT = (short) 0x8008;
    public static final short BGP_FLOWSPEC_ACTION_TRAFFIC_MARKING = (short) 0x8009;

    public static final byte BGP_FLOW_SPEC_LEN_MASK = 0x30;
    public static final byte BGP_FLOW_SPEC_END_OF_LIST_MASK = (byte) 0x80;
}