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
package org.onosproject.net.packet.packetfilter;

import org.onlab.packet.BasePacket;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv6;
import org.onlab.packet.Ip6Address;
import org.onlab.packet.DHCP6;
import org.onlab.packet.UDP;
import org.onlab.packet.dhcp.Dhcp6RelayOption;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketInClassifier;
import org.slf4j.Logger;

import java.util.Arrays;

import static org.slf4j.LoggerFactory.getLogger;

public class Dhcp6IndirectPacketClassifier implements PacketInClassifier {
    private final Logger log = getLogger(getClass());

    @Override
    public boolean match(PacketContext packet) {

        Ethernet eth = packet.inPacket().parsed();

        if (eth.getEtherType() == Ethernet.TYPE_IPV6) {
            IPv6 ipv6Packet = (IPv6) eth.getPayload();

            if (ipv6Packet.getNextHeader() == IPv6.PROTOCOL_UDP) {
                UDP udpPacket = (UDP) ipv6Packet.getPayload();
                //Indirectly connected host
                if (udpPacket.getDestinationPort() == UDP.DHCP_V6_SERVER_PORT &&
                        udpPacket.getSourcePort() == UDP.DHCP_V6_SERVER_PORT &&
                        Arrays.equals(ipv6Packet.getDestinationAddress(),
                                Ip6Address.valueOf("ff02::1:2").toOctets())) {
                    DHCP6 relayMessage = (DHCP6) udpPacket.getPayload();
                    DHCP6 dhcp6 = (DHCP6) relayMessage.getOptions().stream()
                            .filter(opt -> opt instanceof Dhcp6RelayOption)
                            .map(BasePacket::getPayload)
                            .map(pld -> (DHCP6) pld)
                            .findFirst()
                            .orElse(null);

                    if (dhcp6.getMsgType() == DHCP6.MsgType.SOLICIT.value()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
