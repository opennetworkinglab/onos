/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onosproject.net.neighbour.impl;

import org.onlab.packet.ARP;
import org.onlab.packet.Ethernet;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.intf.Interface;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.host.InterfaceIpAddress;

import java.util.Collections;
import java.util.List;

/**
 * Test utilities.
 */
public final class NeighbourTestUtils {

    private static final MacAddress MAC1 = MacAddress.valueOf(1);
    private static final MacAddress MAC2 = MacAddress.valueOf(2);
    private static final IpAddress IP2 = IpAddress.valueOf(2);

    private NeighbourTestUtils() {

    }

    /**
     * Creates an ARP request for the given target IP.
     *
     * @param targetIp IP address
     * @return ARP request packet
     */
    public static Ethernet createArpRequest(IpAddress targetIp) {
        Ethernet eth = new Ethernet();
        eth.setDestinationMACAddress(MAC1);
        eth.setSourceMACAddress(MAC2);
        eth.setEtherType(Ethernet.TYPE_ARP);

        ARP arp = new ARP();
        arp.setOpCode(ARP.OP_REPLY);
        arp.setProtocolType(ARP.PROTO_TYPE_IP);
        arp.setHardwareType(ARP.HW_TYPE_ETHERNET);

        arp.setProtocolAddressLength((byte) Ip4Address.BYTE_LENGTH);
        arp.setHardwareAddressLength((byte) Ethernet.DATALAYER_ADDRESS_LENGTH);
        arp.setSenderHardwareAddress(MAC2.toBytes());
        arp.setTargetHardwareAddress(MacAddress.ZERO.toBytes());

        arp.setTargetProtocolAddress(targetIp.toOctets());
        arp.setSenderProtocolAddress(IP2.toOctets());

        eth.setPayload(arp);
        return eth;
    }

    /**
     * Creates an interface with the given parameters. The IP prefix is assumed
     * to be a /24.
     *
     * @param cp connect point
     * @param ip IP address
     * @param mac MAC address
     * @param vlan VLAN ID
     * @return interface
     */
    public static Interface intf(ConnectPoint cp, IpAddress ip, MacAddress mac, VlanId vlan) {
        List<InterfaceIpAddress> ips =
                Collections.singletonList(InterfaceIpAddress.valueOf(ip.toString() + "/24"));
        return new Interface("foo", cp, ips, mac, vlan);
    }
}
