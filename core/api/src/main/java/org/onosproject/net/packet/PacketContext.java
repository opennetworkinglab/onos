/*
 * Copyright 2014-present Open Networking Foundation
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

import org.onosproject.net.flow.TrafficTreatment;

/**
 * Represents context for processing an inbound packet, and (optionally)
 * emitting a corresponding outbound packet.
 */
public interface PacketContext {

    /**
     * Returns the time when the packet was received.
     *
     * @return the time in millis since start of epoch
     */
    long time();

    /**
     * Returns the inbound packet being processed.
     *
     * @return inbound packet
     */
    InboundPacket inPacket();

    /**
     * Returns the view of the outbound packet.
     *
     * @return outbound packet
     */
    OutboundPacket outPacket();

    /**
     * Returns a builder for constructing traffic treatment.
     *
     * @return traffic treatment builder
     */
    TrafficTreatment.Builder treatmentBuilder();

    /**
     * Triggers the outbound packet to be sent.
     */
    void send();

    /**
     * Blocks the outbound packet from being sent from this point onward.
     *
     * @return whether the outbound packet is blocked.
     */
    boolean block();

    /**
     * Indicates whether the outbound packet is handled, i.e. sent or blocked.
     *
     * @return true uf the packed is handled
     */
    boolean isHandled();

}
