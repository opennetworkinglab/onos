/*
 * Copyright 2014-present Open Networking Laboratory
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

import org.onosproject.event.AbstractEvent;

/**
 * Describes a packet event.
 */
public class PacketEvent extends AbstractEvent<PacketEvent.Type, OutboundPacket> {

    /**
     * Type of packet events.
     */
    public enum Type {
        /**
         * Signifies that the packet should be emitted out a local port.
         */
        EMIT
    }

    /**
     * Creates an event of the given type for the specified packet.
     *
     * @param type the type of the event
     * @param packet the packet the event is about
     */
    public PacketEvent(Type type, OutboundPacket packet) {
        super(type, packet);
    }

    /**
     * Creates an event of the given type for the specified packet at the given
     * time.
     *
     * @param type the type of the event
     * @param packet the packet the event is about
     * @param time the time of the event
     */
    public PacketEvent(Type type, OutboundPacket packet, long time) {
        super(type, packet, time);
    }
}
