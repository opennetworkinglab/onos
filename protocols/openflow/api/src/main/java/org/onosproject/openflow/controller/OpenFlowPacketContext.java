/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.openflow.controller;

import org.onlab.packet.Ethernet;
import org.projectfloodlight.openflow.types.OFPort;

import java.util.Optional;

/**
 * A representation of a packet context which allows any provider
 * to view a packet in event, but may block the response to the
 * event if blocked has been called. This packet context can be used
 * to react to the packet in event with a packet out.
 */
public interface OpenFlowPacketContext {

    /**
     * Blocks further responses (ie. send() calls) on this
     * packet in event.
     *
     * @return true if blocks
     */
    boolean block();

    /**
     * Checks whether the packet has been handled.
     *
     * @return true if handled, false otherwise.
     */
    boolean isHandled();

    /**
     * Provided build has been called send the packet
     * out the switch it came in on.
     */
    void send();

    /**
     * Build the packet out in response to this packet in event.
     *
     * @param outPort the out port to send to packet out of.
     */
    void build(OFPort outPort);

    /**
     * Build the packet out in response to this packet in event.
     *
     * @param ethFrame the actual packet to send out.
     * @param outPort the out port to send to packet out of.
     */
    void build(Ethernet ethFrame, OFPort outPort);

    /**
     * Provided a handle onto the parsed payload.
     *
     * @return the parsed form of the payload.
     */
    Ethernet parsed();

    /**
     * Provide an unparsed copy of the data.
     *
     * @return the unparsed form of the payload.
     */
    byte[] unparsed();

    /**
     * Provide the dpid of the switch where the packet in arrived.
     *
     * @return the dpid of the switch.
     */
    Dpid dpid();

    /**
     * Provide the port on which the packet arrived.
     *
     * @return the port
     */
    Integer inPort();

    /**
     * Indicates that this packet is buffered at the switch.
     *
     * @return buffer indication
     */
    boolean isBuffered();

    /**
     * Provide the cookie in the packet in message.
     *
     * @return optional flow cookie
     */
    Optional<Long> cookie();
}
