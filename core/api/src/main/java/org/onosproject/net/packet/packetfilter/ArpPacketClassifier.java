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

import org.onlab.packet.ARP;
import org.onlab.packet.Ethernet;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketInClassifier;

public class ArpPacketClassifier implements PacketInClassifier {

    @Override
    public boolean match(PacketContext packet) {

        Ethernet eth = packet.inPacket().parsed();
        if (eth != null && (eth.getEtherType() == Ethernet.TYPE_ARP)) {
            ARP arpPacket = (ARP) eth.getPayload();
            if (arpPacket.getOpCode() == ARP.OP_REQUEST) {
                return true;
            }
        }
        return false;
    }
}
