/*
 * Copyright 2014-2015 Open Networking Laboratory
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
package org.onosproject.net.packet;

import org.onosproject.store.Store;

import java.util.Set;

/**
 * Manages routing of outbound packets.
 */
public interface PacketStore extends Store<PacketEvent, PacketStoreDelegate> {

    /**
     * Decides which instance should emit the packet and forwards the packet to
     * that instance. The relevant PacketManager is notified via the
     * PacketStoreDelegate that it should emit the packet.
     *
     * @param packet the packet to emit
     */
    void emit(OutboundPacket packet);

    /**
     * Requests intercept of packets that match the given selector.
     *
     * @param request a packet request
     * @return true if the first time the given selector was requested
     */
    boolean requestPackets(PacketRequest request);

    /**
     * Cancels intercept of packets that match the given selector.
     *
     * @param request a packet request
     * @return true if there is no other application requesting the given selector
     */
    boolean cancelPackets(PacketRequest request);

    /**
     * Obtains all existing requests in the system.
     *
     * @return a set of packet requests
     */
    Set<PacketRequest> existingRequests();

}
