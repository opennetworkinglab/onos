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

import org.onlab.packet.Ethernet;
import org.onlab.packet.ICMP6;
import org.onlab.packet.IPv6;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketInClassifier;

public class NAPacketClassifier implements PacketInClassifier {

    @Override
    public boolean match(PacketContext packet) {

        Ethernet eth = packet.inPacket().parsed();

        if (eth.getEtherType() == Ethernet.TYPE_IPV6) {
            IPv6 ipv6Packet = (IPv6) eth.getPayload();
            if (ipv6Packet.getNextHeader() == IPv6.PROTOCOL_ICMP6) {
                ICMP6 icmp6 = (ICMP6) ipv6Packet.getPayload();
                if (icmp6.getIcmpType() == ICMP6.NEIGHBOR_ADVERTISEMENT) {
                    return true;
                }
            }
        }
        return false;
    }
}
