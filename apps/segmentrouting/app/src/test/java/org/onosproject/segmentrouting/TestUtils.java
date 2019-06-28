/*
 * Copyright 2019-present Open Networking Foundation
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

package org.onosproject.segmentrouting;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.host.InterfaceIpAddress;
import org.onosproject.net.intf.Interface;
import org.onosproject.routeservice.ResolvedRoute;
import org.onosproject.routeservice.Route;
import org.onlab.packet.Ethernet;
import org.onlab.packet.ICMP;
import org.onlab.packet.ICMP6;
import org.onlab.packet.IPv4;
import org.onlab.packet.IPv6;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip6Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onlab.packet.ICMPEcho;

import java.util.Map;
import java.util.Set;

import static org.onlab.packet.ICMP.TYPE_ECHO_REQUEST;
import static org.onlab.packet.ICMP6.ECHO_REQUEST;
import static org.onlab.packet.IPv4.PROTOCOL_ICMP;
import static org.onosproject.routeservice.Route.Source.STATIC;

/**
 * Utilities class for unit tests.
 */
public final class TestUtils {

    private TestUtils() {

    }

    // Device configuration section
    static final DeviceId REMOTE_LEAF = DeviceId.deviceId("of:0000000000000001");
    static final int REMOTE_LEAF_SID4 = 1;
    static final String REMOTE_LEAF_LB4 = "192.168.0.1";
    static final int REMOTE_LEAF_SID6 = 10;
    static final String REMOTE_LEAF_LB6 = "2000::c0a8:1";
    private static final PortNumber P1 = PortNumber.portNumber(1);
    static final MacAddress REMOTE_MAC = MacAddress.valueOf("00:00:00:00:00:02");

    static final DeviceId LOCAL_LEAF = DeviceId.deviceId("of:0000000000000101");
    static final int LOCAL_LEAF_SID4 = 101;
    static final String LOCAL_LEAF_LB4 = "192.168.0.101";
    static final int LOCAL_LEAF_SID6 = 111;
    static final String LOCAL_LEAF_LB6 = "2000::c0a8:101";
    static final MacAddress LOCAL_MAC = MacAddress.valueOf("00:00:00:00:01:01");

    // Configure a pair
    static final DeviceId LOCAL_LEAF1 = DeviceId.deviceId("of:0000000000000201");
    static final int LOCAL_LEAF1_SID4 = 201;
    static final String LOCAL_LEAF1_LB4 = "192.168.0.201";
    static final int LOCAL_LEAF1_SID6 = 211;
    static final String LOCAL_LEAF1_LB6 = "2000::c0a8:201";
    static final MacAddress LOCAL_MAC1 = MacAddress.valueOf("00:00:00:00:02:01");

    static final DeviceId LOCAL_LEAF2 = DeviceId.deviceId("of:0000000000000202");
    static final int LOCAL_LEAF2_SID4 = 202;
    static final String LOCAL_LEAF2_LB4 = "192.168.0.202";
    static final int LOCAL_LEAF2_SID6 = 212;
    static final String LOCAL_LEAF2_LB6 = "2000::c0a8:202";
    static final MacAddress LOCAL_MAC2 = MacAddress.valueOf("00:00:00:00:02:02");

    // Pair port
    static final PortNumber P3 = PortNumber.portNumber(3);

    // Ports configuration section
    static final ConnectPoint CP11 = new ConnectPoint(REMOTE_LEAF, P1);
    private static final PortNumber P2 = PortNumber.portNumber(2);
    static final ConnectPoint CP12 = new ConnectPoint(REMOTE_LEAF, P2);
    private static final IpAddress IP4_1 = IpAddress.valueOf("10.0.0.254");
    private static final IpPrefix PREFIX4_1 = IpPrefix.valueOf("10.0.0.254/24");
    private static final IpAddress IP6_1 = IpAddress.valueOf("2000::ff");
    private static final IpPrefix PREFIX6_1 = IpPrefix.valueOf("2000::ff/120");
    private static final InterfaceIpAddress INTF_IP4_1 = new InterfaceIpAddress(
            IP4_1, PREFIX4_1);
    private static final InterfaceIpAddress INTF_IP6_1 = new InterfaceIpAddress(
            IP6_1, PREFIX6_1);
    private static final VlanId INTF_VLAN_UNTAGGED = VlanId.vlanId((short) 10);
    static final Interface INTF1 = new Interface(
            "INTF1", CP12, Lists.newArrayList(INTF_IP4_1, INTF_IP6_1), MacAddress.NONE,
            null, INTF_VLAN_UNTAGGED, null, null);
    static final ConnectPoint CP13 = new ConnectPoint(REMOTE_LEAF, P3);
    private static final IpAddress IP4_2 = IpAddress.valueOf("10.0.3.254");
    private static final IpPrefix PREFIX4_2 = IpPrefix.valueOf("10.0.3.254/24");
    private static final IpAddress IP6_2 = IpAddress.valueOf("2000::3ff");
    private static final IpPrefix PREFIX6_2 = IpPrefix.valueOf("2000::3ff/120");
    private static final InterfaceIpAddress INTF_IP4_2 = new InterfaceIpAddress(
            IP4_2, PREFIX4_2);
    private static final InterfaceIpAddress INTF_IP6_2 = new InterfaceIpAddress(
            IP6_2, PREFIX6_2);
    static final Interface INTF2 = new Interface(
            "INTF2", CP13, Lists.newArrayList(INTF_IP4_2, INTF_IP6_2), MacAddress.NONE,
            null, INTF_VLAN_UNTAGGED, null, null);

    static final ConnectPoint CP1011 = new ConnectPoint(LOCAL_LEAF, P1);
    private static final IpAddress IP4_11 = IpAddress.valueOf("10.0.1.254");
    private static final IpPrefix PREFIX4_11 = IpPrefix.valueOf("10.0.1.254/24");
    private static final InterfaceIpAddress INTF_IP4_11 = new InterfaceIpAddress(
            IP4_11, PREFIX4_11);
    private static final IpAddress IP6_11 = IpAddress.valueOf("2000::1ff");
    private static final IpPrefix PREFIX6_11 = IpPrefix.valueOf("2000::1ff/120");
    private static final InterfaceIpAddress INTF_IP6_11 = new InterfaceIpAddress(
            IP6_11, PREFIX6_11);
    static final Interface INTF111 = new Interface(
            "INTF111", CP1011, Lists.newArrayList(INTF_IP4_11, INTF_IP6_11), MacAddress.NONE, null,
            INTF_VLAN_UNTAGGED, null, null);

    static final ConnectPoint CP2011 = new ConnectPoint(LOCAL_LEAF1, P1);
    private static final IpAddress IP4_21 = IpAddress.valueOf("10.0.2.254");
    private static final IpPrefix PREFIX4_21 = IpPrefix.valueOf("10.0.2.254/24");
    private static final InterfaceIpAddress INTF_IP4_21 = new InterfaceIpAddress(
            IP4_21, PREFIX4_21);
    private static final IpAddress IP6_21 = IpAddress.valueOf("2000::2ff");
    private static final IpPrefix PREFIX6_21 = IpPrefix.valueOf("2000::2ff/120");
    private static final InterfaceIpAddress INTF_IP6_21 = new InterfaceIpAddress(
            IP6_21, PREFIX6_21);
    static final Interface INTF211 = new Interface(
            "INTF211", CP2011, Lists.newArrayList(INTF_IP4_21, INTF_IP6_21), MacAddress.NONE, null,
            INTF_VLAN_UNTAGGED, null, null);

    static final ConnectPoint CP2021 = new ConnectPoint(LOCAL_LEAF2, P1);
    private static final IpAddress IP4_22 = IpAddress.valueOf("10.0.2.254");
    private static final IpPrefix PREFIX4_22 = IpPrefix.valueOf("10.0.2.254/24");
    private static final InterfaceIpAddress INTF_IP4_22 = new InterfaceIpAddress(
            IP4_22, PREFIX4_22);
    private static final IpAddress IP6_22 = IpAddress.valueOf("2000::2ff");
    private static final IpPrefix PREFIX6_22 = IpPrefix.valueOf("2000::2ff/120");
    private static final InterfaceIpAddress INTF_IP6_22 = new InterfaceIpAddress(
            IP6_22, PREFIX6_22);
    static final Interface INTF212 = new Interface(
            "INTF212", CP2021, Lists.newArrayList(INTF_IP4_22, INTF_IP6_22), MacAddress.NONE, null,
            INTF_VLAN_UNTAGGED, null, null);
    private static final PortNumber P4 = PortNumber.portNumber(4);
    static final ConnectPoint CP2024 = new ConnectPoint(LOCAL_LEAF2, P4);
    private static final PortNumber P5 = PortNumber.portNumber(5);
    static final ConnectPoint CP2025 = new ConnectPoint(LOCAL_LEAF2, P5);
    private static final IpAddress IP4_23 = IpAddress.valueOf("10.0.4.254");
    private static final IpPrefix PREFIX4_23 = IpPrefix.valueOf("10.0.4.254/24");
    private static final InterfaceIpAddress INTF_IP4_23 = new InterfaceIpAddress(
            IP4_23, PREFIX4_23);
    private static final IpAddress IP6_23 = IpAddress.valueOf("2000::4ff");
    private static final IpPrefix PREFIX6_23 = IpPrefix.valueOf("2000::4ff/120");
    private static final InterfaceIpAddress INTF_IP6_23 = new InterfaceIpAddress(
            IP6_23, PREFIX6_23);
    static final Interface INTF213 = new Interface(
            "INTF212", CP2024, Lists.newArrayList(INTF_IP4_23, INTF_IP6_23), MacAddress.NONE, null,
            INTF_VLAN_UNTAGGED, null, null);

    // Packet-ins section
    private static final MacAddress SRC_MAC = MacAddress.valueOf("00:00:00:00:00:01");

    private static final ICMPEcho ICMP_ECHO = new ICMPEcho()
            .setIdentifier((short) 0)
            .setSequenceNum((short) 0);

    private static final ICMP ICMP_REQUEST = (ICMP) new ICMP()
            .setIcmpType(TYPE_ECHO_REQUEST)
            .setPayload(ICMP_ECHO);

    private static final Ip4Address SRC_IPV4 = Ip4Address.valueOf("10.0.1.1");
    static final Ip4Address DST_IPV4 = Ip4Address.valueOf("10.0.0.254");

    private static final IPv4 IPV4_REQUEST = (IPv4) new IPv4()
            .setDestinationAddress(DST_IPV4.toInt())
            .setSourceAddress(SRC_IPV4.toInt())
            .setTtl((byte) 64)
            .setProtocol(PROTOCOL_ICMP)
            .setPayload(ICMP_REQUEST);

    static final Ethernet ETH_REQ_IPV4 = (Ethernet) new Ethernet()
            .setEtherType(Ethernet.TYPE_IPV4)
            .setDestinationMACAddress(REMOTE_MAC)
            .setSourceMACAddress(SRC_MAC)
            .setPayload(IPV4_REQUEST);

    private static final ICMP6 ICMP6_REQUEST = new ICMP6()
            .setIcmpType(ECHO_REQUEST);

    private static final Ip6Address SRC_IPV6 = Ip6Address.valueOf("2000::101");
    static final Ip6Address DST_IPV6 = Ip6Address.valueOf("2000::ff");

    private static final IPv6 IPV6_REQUEST = (IPv6) new IPv6()
            .setDestinationAddress(DST_IPV6.toOctets())
            .setSourceAddress(SRC_IPV6.toOctets())
            .setHopLimit((byte) 255)
            .setNextHeader(IPv6.PROTOCOL_ICMP6)
            .setPayload(ICMP6_REQUEST);

    static final Ethernet ETH_REQ_IPV6 = (Ethernet) new Ethernet()
            .setEtherType(Ethernet.TYPE_IPV6)
            .setDestinationMACAddress(REMOTE_MAC)
            .setSourceMACAddress(SRC_MAC)
            .setPayload(IPV6_REQUEST);

    static final Ip4Address SRC_IPV41 = Ip4Address.valueOf("10.0.2.1");

    private static final IPv4 IPV41_REQUEST = (IPv4) new IPv4()
            .setDestinationAddress(DST_IPV4.toInt())
            .setSourceAddress(SRC_IPV41.toInt())
            .setTtl((byte) 64)
            .setProtocol(PROTOCOL_ICMP)
            .setPayload(ICMP_REQUEST);

    static final Ethernet ETH_REQ_IPV41 = (Ethernet) new Ethernet()
            .setEtherType(Ethernet.TYPE_IPV4)
            .setDestinationMACAddress(REMOTE_MAC)
            .setSourceMACAddress(SRC_MAC)
            .setPayload(IPV41_REQUEST);

    static final Ip6Address SRC_IPV61 = Ip6Address.valueOf("2000::201");

    private static final IPv6 IPV61_REQUEST = (IPv6) new IPv6()
            .setDestinationAddress(DST_IPV6.toOctets())
            .setSourceAddress(SRC_IPV61.toOctets())
            .setHopLimit((byte) 255)
            .setNextHeader(IPv6.PROTOCOL_ICMP6)
            .setPayload(ICMP6_REQUEST);

    static final Ethernet ETH_REQ_IPV61 = (Ethernet) new Ethernet()
            .setEtherType(Ethernet.TYPE_IPV6)
            .setDestinationMACAddress(REMOTE_MAC)
            .setSourceMACAddress(SRC_MAC)
            .setPayload(IPV61_REQUEST);

    private static final MacAddress SRC_MAC_MY = MacAddress.valueOf("00:01:00:00:00:01");
    static final Ip4Address SRC_IPV4_MY = Ip4Address.valueOf("10.0.0.1");

    private static final IPv4 IPV4_REQUEST_MY = (IPv4) new IPv4()
            .setDestinationAddress(DST_IPV4.toInt())
            .setSourceAddress(SRC_IPV4_MY.toInt())
            .setTtl((byte) 64)
            .setProtocol(PROTOCOL_ICMP)
            .setPayload(ICMP_REQUEST);

    static final Ethernet ETH_REQ_IPV4_MY = (Ethernet) new Ethernet()
            .setEtherType(Ethernet.TYPE_IPV4)
            .setDestinationMACAddress(REMOTE_MAC)
            .setSourceMACAddress(SRC_MAC_MY)
            .setPayload(IPV4_REQUEST_MY);

    static final Ip6Address SRC_IPV6_MY = Ip6Address.valueOf("2000::1");

    private static final IPv6 IPV6_REQUEST_MY = (IPv6) new IPv6()
            .setDestinationAddress(DST_IPV6.toOctets())
            .setSourceAddress(SRC_IPV6_MY.toOctets())
            .setHopLimit((byte) 255)
            .setNextHeader(IPv6.PROTOCOL_ICMP6)
            .setPayload(ICMP6_REQUEST);

    static final Ethernet ETH_REQ_IPV6_MY = (Ethernet) new Ethernet()
            .setEtherType(Ethernet.TYPE_IPV6)
            .setDestinationMACAddress(REMOTE_MAC)
            .setSourceMACAddress(SRC_MAC_MY)
            .setPayload(IPV6_REQUEST_MY);

    static final Ip4Address DST_IPV4_LOCAL = Ip4Address.valueOf("10.0.3.254");

    private static final IPv4 IPV4_REQUEST_LOCAL = (IPv4) new IPv4()
            .setDestinationAddress(DST_IPV4_LOCAL.toInt())
            .setSourceAddress(SRC_IPV4_MY.toInt())
            .setTtl((byte) 64)
            .setProtocol(PROTOCOL_ICMP)
            .setPayload(ICMP_REQUEST);

    static final Ethernet ETH_REQ_IPV4_LOCAL = (Ethernet) new Ethernet()
            .setEtherType(Ethernet.TYPE_IPV4)
            .setDestinationMACAddress(REMOTE_MAC)
            .setSourceMACAddress(SRC_MAC_MY)
            .setPayload(IPV4_REQUEST_LOCAL);

    static final Ip6Address DST_IPV6_LOCAL = Ip6Address.valueOf("2000::3ff");

    private static final IPv6 IPV6_REQUEST_LOCAL = (IPv6) new IPv6()
            .setDestinationAddress(DST_IPV6_LOCAL.toOctets())
            .setSourceAddress(SRC_IPV6_MY.toOctets())
            .setHopLimit((byte) 255)
            .setNextHeader(IPv6.PROTOCOL_ICMP6)
            .setPayload(ICMP6_REQUEST);

    static final Ethernet ETH_REQ_IPV6_LOCAL = (Ethernet) new Ethernet()
            .setEtherType(Ethernet.TYPE_IPV6)
            .setDestinationMACAddress(REMOTE_MAC)
            .setSourceMACAddress(SRC_MAC_MY)
            .setPayload(IPV6_REQUEST_LOCAL);

    static final Ip4Address DST_IPV4_SAME = Ip4Address.valueOf("10.0.4.254");

    private static final IPv4 IPV4_REQUEST_SAME = (IPv4) new IPv4()
            .setDestinationAddress(DST_IPV4_SAME.toInt())
            .setSourceAddress(SRC_IPV41.toInt())
            .setTtl((byte) 64)
            .setProtocol(PROTOCOL_ICMP)
            .setPayload(ICMP_REQUEST);

    static final Ethernet ETH_REQ_IPV4_SAME = (Ethernet) new Ethernet()
            .setEtherType(Ethernet.TYPE_IPV4)
            .setDestinationMACAddress(LOCAL_MAC2)
            .setSourceMACAddress(SRC_MAC)
            .setPayload(IPV4_REQUEST_SAME);

    static final Ip6Address DST_IPV6_SAME = Ip6Address.valueOf("2000::4ff");

    private static final IPv6 IPV6_REQUEST_SAME = (IPv6) new IPv6()
            .setDestinationAddress(DST_IPV6_SAME.toOctets())
            .setSourceAddress(SRC_IPV61.toOctets())
            .setHopLimit((byte) 255)
            .setNextHeader(IPv6.PROTOCOL_ICMP6)
            .setPayload(ICMP6_REQUEST);

    static final Ethernet ETH_REQ_IPV6_SAME = (Ethernet) new Ethernet()
            .setEtherType(Ethernet.TYPE_IPV6)
            .setDestinationMACAddress(LOCAL_MAC2)
            .setSourceMACAddress(SRC_MAC)
            .setPayload(IPV6_REQUEST_SAME);

    static final Ip6Address DST_IPV6_LL = Ip6Address.valueOf(
            IPv6.getLinkLocalAddress(MacAddress.NONE.toBytes()));
    static final Ip6Address SRC_IPV6_LL = Ip6Address.valueOf(
            IPv6.getLinkLocalAddress(SRC_MAC_MY.toBytes()));

    private static final IPv6 IPV6_REQUEST_LL = (IPv6) new IPv6()
            .setDestinationAddress(DST_IPV6_LL.toOctets())
            .setSourceAddress(SRC_IPV6_LL.toOctets())
            .setHopLimit((byte) 255)
            .setNextHeader(IPv6.PROTOCOL_ICMP6)
            .setPayload(ICMP6_REQUEST);

    static final Ethernet ETH_REQ_IPV6_LL = (Ethernet) new Ethernet()
            .setEtherType(Ethernet.TYPE_IPV6)
            .setDestinationMACAddress(MacAddress.NONE)
            .setSourceMACAddress(SRC_MAC_MY)
            .setPayload(IPV6_REQUEST_LL);

    static final Ip4Address DST_IPV4_LOOPBACK = Ip4Address.valueOf(REMOTE_LEAF_LB4);

    private static final IPv4 IPV4_REQUEST_LOOPBACK = (IPv4) new IPv4()
            .setDestinationAddress(DST_IPV4_LOOPBACK.toInt())
            .setSourceAddress(SRC_IPV4_MY.toInt())
            .setTtl((byte) 64)
            .setProtocol(PROTOCOL_ICMP)
            .setPayload(ICMP_REQUEST);

    static final Ethernet ETH_REQ_IPV4_LOOPBACK = (Ethernet) new Ethernet()
            .setEtherType(Ethernet.TYPE_IPV4)
            .setDestinationMACAddress(REMOTE_MAC)
            .setSourceMACAddress(SRC_MAC_MY)
            .setPayload(IPV4_REQUEST_LOOPBACK);

    static final Ip6Address DST_IPV6_LOOPBACK = Ip6Address.valueOf(REMOTE_LEAF_LB6);

    private static final IPv6 IPV6_REQUEST_LOOPBACK = (IPv6) new IPv6()
            .setDestinationAddress(DST_IPV6_LOOPBACK.toOctets())
            .setSourceAddress(SRC_IPV6_MY.toOctets())
            .setHopLimit((byte) 255)
            .setNextHeader(IPv6.PROTOCOL_ICMP6)
            .setPayload(ICMP6_REQUEST);

    static final Ethernet ETH_REQ_IPV6_LOOPBACK = (Ethernet) new Ethernet()
            .setEtherType(Ethernet.TYPE_IPV6)
            .setDestinationMACAddress(REMOTE_MAC)
            .setSourceMACAddress(SRC_MAC_MY)
            .setPayload(IPV6_REQUEST_LOOPBACK);

    static final Ip4Address DST_IPV4_LOOPBACK_PAIR = Ip4Address.valueOf(LOCAL_LEAF1_LB4);

    private static final IPv4 IPV4_REQUEST_LOOPBACK_PAIR = (IPv4) new IPv4()
            .setDestinationAddress(DST_IPV4_LOOPBACK_PAIR.toInt())
            .setSourceAddress(SRC_IPV41.toInt())
            .setTtl((byte) 64)
            .setProtocol(PROTOCOL_ICMP)
            .setPayload(ICMP_REQUEST);

    static final Ethernet ETH_REQ_IPV4_LOOPBACK_PAIR = (Ethernet) new Ethernet()
            .setEtherType(Ethernet.TYPE_IPV4)
            .setDestinationMACAddress(LOCAL_MAC1)
            .setSourceMACAddress(SRC_MAC)
            .setPayload(IPV4_REQUEST_LOOPBACK_PAIR);

    static final Ip6Address DST_IPV6_LOOPBACK_PAIR = Ip6Address.valueOf(LOCAL_LEAF2_LB6);

    private static final IPv6 IPV6_REQUEST_LOOPBACK_PAIR = (IPv6) new IPv6()
            .setDestinationAddress(DST_IPV6_LOOPBACK_PAIR.toOctets())
            .setSourceAddress(SRC_IPV61.toOctets())
            .setHopLimit((byte) 255)
            .setNextHeader(IPv6.PROTOCOL_ICMP6)
            .setPayload(ICMP6_REQUEST);

    static final Ethernet ETH_REQ_IPV6_LOOPBACK_PAIR = (Ethernet) new Ethernet()
            .setEtherType(Ethernet.TYPE_IPV6)
            .setDestinationMACAddress(LOCAL_MAC2)
            .setSourceMACAddress(SRC_MAC)
            .setPayload(IPV6_REQUEST_LOOPBACK_PAIR);

    static final Ip4Address DST_IPV4_GATEWAY_PAIR = Ip4Address.valueOf("10.0.2.254");

    private static final IPv4 IPV4_REQUEST_GATEWAY_PAIR = (IPv4) new IPv4()
            .setDestinationAddress(DST_IPV4_GATEWAY_PAIR.toInt())
            .setSourceAddress(SRC_IPV41.toInt())
            .setTtl((byte) 64)
            .setProtocol(PROTOCOL_ICMP)
            .setPayload(ICMP_REQUEST);

    static final Ethernet ETH_REQ_IPV4_GATEWAY_PAIR = (Ethernet) new Ethernet()
            .setEtherType(Ethernet.TYPE_IPV4)
            .setDestinationMACAddress(LOCAL_MAC1)
            .setSourceMACAddress(SRC_MAC)
            .setPayload(IPV4_REQUEST_GATEWAY_PAIR);

    static final Ip6Address DST_IPV6_GATEWAY_PAIR = Ip6Address.valueOf("2000::2ff");

    private static final IPv6 IPV6_REQUEST_GATEWAY_PAIR = (IPv6) new IPv6()
            .setDestinationAddress(DST_IPV6_GATEWAY_PAIR.toOctets())
            .setSourceAddress(SRC_IPV61.toOctets())
            .setHopLimit((byte) 255)
            .setNextHeader(IPv6.PROTOCOL_ICMP6)
            .setPayload(ICMP6_REQUEST);

    static final Ethernet ETH_REQ_IPV6_GATEWAY_PAIR = (Ethernet) new Ethernet()
            .setEtherType(Ethernet.TYPE_IPV6)
            .setDestinationMACAddress(LOCAL_MAC2)
            .setSourceMACAddress(SRC_MAC)
            .setPayload(IPV6_REQUEST_GATEWAY_PAIR);

    // Resolved route
    private static final ResolvedRoute IPV4_ROUTE = new ResolvedRoute(
            new Route(STATIC, SRC_IPV4.toIpPrefix(), SRC_IPV4), MacAddress.NONE);
    private static final ResolvedRoute IPV6_ROUTE = new ResolvedRoute(
            new Route(STATIC, SRC_IPV6.toIpPrefix(), SRC_IPV6), MacAddress.NONE);
    static final Map<IpPrefix, Set<ResolvedRoute>> ROUTE_STORE = ImmutableMap.of(SRC_IPV4.toIpPrefix(),
                                                                                 Sets.newHashSet(IPV4_ROUTE),
                                                                                 SRC_IPV6.toIpPrefix(),
                                                                                 Sets.newHashSet(IPV6_ROUTE));
}
