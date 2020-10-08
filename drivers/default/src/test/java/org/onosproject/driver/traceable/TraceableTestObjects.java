/*
 * Copyright 2020-present Open Networking Foundation
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

package org.onosproject.driver.traceable;

import org.onlab.packet.EthType;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.MplsLabel;
import org.onlab.packet.VlanId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.driver.DriverAdapter;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.TrafficSelector;

import static org.onlab.packet.EthType.EtherType.IPV4;
import static org.onlab.packet.EthType.EtherType.MPLS_UNICAST;

/**
 * Helper class for objects related to the Traceable tests.
 */
final class TraceableTestObjects {

    private TraceableTestObjects() {
        // Banning construction
    }

    // Test drivers name
    static final String OFDPA_DRIVER = "ofdpa";
    static final String OVS_OFDPA_DRIVER = "ofdpa-ovs";

    // Test device ids
    static final DeviceId OFDPA_DEVICE = DeviceId.deviceId("ofdpaDevice");

    // Input ports
    static final PortNumber PORT = PortNumber.portNumber("1");
    static final PortNumber OUT_PORT = PortNumber.portNumber("3");
    static final PortNumber UP_PORT = PortNumber.portNumber("10");
    static final PortNumber UP_PORT_1 = PortNumber.portNumber("11");
    static final ConnectPoint OFDPA_CP = ConnectPoint.deviceConnectPoint(OFDPA_DEVICE + "/" + PORT.toLong());
    static final ConnectPoint UP_OFDPA_CP = ConnectPoint.deviceConnectPoint(OFDPA_DEVICE + "/" + UP_PORT.toLong());

    // Misc
    static final VlanId HOST_VLAN = VlanId.vlanId((short) 100);
    static final VlanId DEFAULT_VLAN = VlanId.vlanId((short) 4094);
    static final IpPrefix IP_PUNT = IpPrefix.valueOf("10.0.2.254/32");
    static final MacAddress HOST_MAC = MacAddress.valueOf("00:AA:00:00:00:02");
    static final IpPrefix IP_DST = IpPrefix.valueOf("10.0.2.2/32");
    static final IpPrefix IP_DST_1 = IpPrefix.valueOf("10.0.3.1/32");
    static final MacAddress LEAF_MAC = MacAddress.valueOf("00:00:00:00:02:04");
    static final IpPrefix PREFIX_DST = IpPrefix.valueOf("10.0.3.0/24");
    static final MacAddress SPINE_MAC = MacAddress.valueOf("00:00:00:00:02:26");
    static final MacAddress SPINE_MAC_1 = MacAddress.valueOf("00:00:00:00:02:26");
    static final MplsLabel MPLS_LABEL = MplsLabel.mplsLabel(205);

    // Input packets
    static final TrafficSelector IN_PUNT_IP_PACKET = DefaultTrafficSelector.builder()
            .matchInPort(OFDPA_CP.port())
            .matchEthType(IPV4.ethType().toShort())
            .matchVlanId(VlanId.NONE)
            .matchIPDst(IP_PUNT)
            .build();

    static final TrafficSelector IN_ARP_PACKET = DefaultTrafficSelector.builder()
            .matchInPort(OFDPA_CP.port())
            .matchEthType(EthType.EtherType.ARP.ethType().toShort())
            .matchVlanId(VlanId.NONE)
            .build();

    static final TrafficSelector IN_PUNT_LLDP_PACKET = DefaultTrafficSelector.builder()
            .matchInPort(OFDPA_CP.port())
            .matchEthType(EthType.EtherType.LLDP.ethType().toShort())
            .matchVlanId(VlanId.NONE)
            .build();

    static final TrafficSelector IN_L2_BRIDG_UNTAG_PACKET = DefaultTrafficSelector.builder()
            .matchInPort(OFDPA_CP.port())
            .matchEthType(IPV4.ethType().toShort())
            .matchVlanId(VlanId.NONE)
            .matchEthDst(HOST_MAC)
            .build();

    static final TrafficSelector IN_L2_BROAD_UNTAG_PACKET = DefaultTrafficSelector.builder()
            .matchInPort(OFDPA_CP.port())
            .matchVlanId(VlanId.NONE)
            .build();

    static final TrafficSelector IN_L3_UCAST_UNTAG_PACKET = DefaultTrafficSelector.builder()
            .matchInPort(UP_OFDPA_CP.port())
            .matchEthDst(LEAF_MAC)
            .matchEthType(IPV4.ethType().toShort())
            .matchVlanId(VlanId.NONE)
            .matchIPDst(IP_DST)
            .build();

    static final TrafficSelector IN_L3_ECMP_PACKET = DefaultTrafficSelector.builder()
            .matchInPort(OFDPA_CP.port())
            .matchEthDst(LEAF_MAC)
            .matchEthType(IPV4.ethType().toShort())
            .matchVlanId(VlanId.NONE)
            .matchIPDst(IP_DST_1)
            .build();

    static final TrafficSelector IN_MPLS_ECMP_PACKET = DefaultTrafficSelector.builder()
            .matchInPort(UP_OFDPA_CP.port())
            .matchEthDst(LEAF_MAC)
            .matchEthType(MPLS_UNICAST.ethType().toShort())
            .matchVlanId(VlanId.NONE)
            .matchMplsLabel(MPLS_LABEL)
            .matchMplsBos(true)
            .build();

    static final TrafficSelector IN_MPLS_ECMP_PACKET_OFDPA = DefaultTrafficSelector.builder()
            .matchInPort(UP_OFDPA_CP.port())
            .matchEthDst(LEAF_MAC)
            .matchEthType(MPLS_UNICAST.ethType().toShort())
            .matchVlanId(VlanId.NONE)
            .matchMplsLabel(MPLS_LABEL)
            .matchMplsBos(true)
            .matchMetadata(IPV4.ethType().toShort())
            .build();

    // Egress packets
    static final TrafficSelector OUT_L3_UCAST_UNTAG_PACKET = DefaultTrafficSelector.builder(IN_L3_UCAST_UNTAG_PACKET)
            .matchEthSrc(LEAF_MAC)
            .matchEthDst(HOST_MAC)
            .build();

    static final TrafficSelector OUT_L3_ECMP_PACKET = DefaultTrafficSelector.builder(IN_L3_ECMP_PACKET)
            .matchEthSrc(LEAF_MAC)
            .matchEthDst(SPINE_MAC)
            .matchEthType(MPLS_UNICAST.ethType().toShort())
            .matchMplsLabel(MPLS_LABEL)
            .matchMplsBos(true)
            .build();

    static final TrafficSelector OUT_L3_ECMP_PACKET_1 = DefaultTrafficSelector.builder(IN_L3_ECMP_PACKET)
            .matchEthSrc(LEAF_MAC)
            .matchEthDst(SPINE_MAC_1)
            .matchEthType(MPLS_UNICAST.ethType().toShort())
            .matchMplsLabel(MPLS_LABEL)
            .matchMplsBos(true)
            .build();

    static final TrafficSelector OUT_L3_ECMP_PACKET_OFDPA = DefaultTrafficSelector.builder(IN_L3_ECMP_PACKET)
            .matchEthSrc(LEAF_MAC)
            .matchEthDst(SPINE_MAC)
            .matchEthType(MPLS_UNICAST.ethType().toShort())
            .matchMplsLabel(MPLS_LABEL)
            .matchMplsBos(true)
            .matchMetadata(IPV4.ethType().toShort())
            .build();

    static final TrafficSelector OUT_L3_ECMP_PACKET_OFDPA_1 = DefaultTrafficSelector.builder(IN_L3_ECMP_PACKET)
            .matchEthSrc(LEAF_MAC)
            .matchEthDst(SPINE_MAC_1)
            .matchEthType(MPLS_UNICAST.ethType().toShort())
            .matchMplsLabel(MPLS_LABEL)
            .matchMplsBos(true)
            .matchMetadata(IPV4.ethType().toShort())
            .build();

    static final TrafficSelector OUT_MPLS_ECMP_PACKET = DefaultTrafficSelector.builder()
            .matchInPort(UP_OFDPA_CP.port())
            .matchEthSrc(LEAF_MAC)
            .matchEthDst(SPINE_MAC)
            .matchEthType(IPV4.ethType().toShort())
            .matchVlanId(VlanId.NONE)
            .build();

    static final TrafficSelector OUT_L2_BROAD_EMPTY = DefaultTrafficSelector.builder()
            .matchInPort(OFDPA_CP.port())
            .matchVlanId(HOST_VLAN)
            .build();

    // Test cases
    enum TraceableTest {
        PUNT_IP_OFDPA,
        PUNT_IP_OVS_OFDPA,
        ARP_OFDPA,
        ARP_OVS_OFDPA,
        PUNT_LLDP_OFDPA,
        PUNT_LLDP_OVS_OFDPA,
        L2_BRIDG_UNTAG_OFDPA,
        L2_BRIDG_UNTAG_OVS_OFDPA,
        L2_BROAD_UNTAG_OFDPA,
        L2_BROAD_UNTAG_OVS_OFDPA,
        L3_UCAST_UNTAG_OFDPA,
        L3_UCAST_UNTAG_OVS_OFDPA,
        L3_ECMP_OFDPA,
        L3_ECMP_OVS_OFDPA,
        MPLS_ECMP_OFDPA,
        MPLS_ECMP_OVS_OFDPA,
        L2_BROAD_EMPTY_OFDPA,
        L2_BROAD_EMPTY_OVS_OFDPA,
        L2_BRIDG_NOT_ORDERED_OFDPA,
        L2_BRIDG_NOT_ORDERED_OVS_OFDPA,
    }

    // Test driver class
    static class TestDriver extends DriverAdapter {

        private String name;

        public TestDriver(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }

    }
}
