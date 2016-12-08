/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.incubator.net.virtual;

import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketEvent;
import org.onosproject.net.packet.PacketRequest;
import org.onosproject.net.packet.PacketStoreDelegate;

import java.util.List;

public interface VirtualNetworkPacketStore
        extends VirtualStore<PacketEvent, PacketStoreDelegate> {
    /**
     * Decides which instance should emit the packet and forwards the packet to
     * that instance. The relevant PacketManager is notified via the
     * PacketStoreDelegate that it should emit the packet.
     *
     * @param networkId a virtual network identifier
     * @param packet the packet to emit
     */
    void emit(NetworkId networkId, OutboundPacket packet);

    /**
     * Requests intercept of packets that match the given selector.
     *
     * @param networkId a virtual network identifier
     * @param request a packet request
     */
    void requestPackets(NetworkId networkId, PacketRequest request);

    /**
     * Cancels intercept of packets that match the given selector.
     *
     * @param networkId a virtual network identifier
     * @param request a packet request
     */
    void cancelPackets(NetworkId networkId, PacketRequest request);

    /**
     * Obtains all existing requests in the system.
     *
     * @param networkId a virtual network identifier
     * @return list of packet requests in order of priority
     */
    List<PacketRequest> existingRequests(NetworkId networkId);
}
