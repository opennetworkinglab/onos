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

import com.google.common.collect.Maps;
import org.apache.commons.lang3.tuple.Pair;
import org.onlab.packet.DeserializationException;
import org.onlab.packet.Ethernet;
import org.onlab.packet.MacAddress;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketServiceAdapter;

import java.util.Map;

/**
 * Mock Packet Service.
 * It is used for tests related to packet-ins management.
 */
public class MockPacketService extends PacketServiceAdapter {

    private final Map<MacAddress, Pair<OutboundPacket, Ethernet>> outBoundPackets = Maps.newHashMap();

    @Override
    public void emit(OutboundPacket packet) {
        try {
            Ethernet ethernetPacket = Ethernet.deserializer().deserialize(packet.data().array(),
                                                                          packet.data().arrayOffset(),
                                                                          packet.data().array().length);
            outBoundPackets.put(ethernetPacket.getDestinationMAC(), Pair.of(packet, ethernetPacket));
        } catch (DeserializationException e) {

        }
    }

    Ethernet getEthernetPacket(MacAddress key) {
        Pair<OutboundPacket, Ethernet> pair = outBoundPackets.get(key);
        return pair != null ? pair.getRight() : null;
    }
}
