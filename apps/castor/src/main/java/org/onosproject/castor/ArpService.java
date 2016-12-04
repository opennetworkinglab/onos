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
package org.onosproject.castor;

import org.onosproject.net.packet.PacketContext;

/**
 * Interface for processing and handling ARP related events.
 */
public interface ArpService {

    /**
     * Creates an ARP packet to probe for the peer's mac address.
     *
     * @param peer A Peer
     */
    void createArp(Peer peer);

    /**
     * Handles the ARP packet in.
     *
     * @param context packet context to handle
     * @return true if handled
     */
    boolean handlePacket(PacketContext context);
}
