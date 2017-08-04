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
package org.onosproject.incubator.net.virtual.provider;

import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.net.packet.OutboundPacket;

/**
 * Abstraction of a virtual packet provider capable of emitting packets
 * from virtual network core services to the underlay network.
 * This provider de-virtualizes and virtualize PacketContext.
 */
public interface VirtualPacketProvider extends VirtualProvider {
    /**
     * Emits the specified outbound packet onto the underlay physical network.
     * This provider maps the requested packets for physical network.
     *
     * @param networkId the virtual network ID
     * @param packet outbound packet in the context of virtual network
     */
    void emit(NetworkId networkId, OutboundPacket packet);
}
