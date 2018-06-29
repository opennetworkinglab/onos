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

package org.onosproject.net.neighbour;

import org.onlab.packet.Ethernet;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.intf.Interface;
import org.onosproject.net.ConnectPoint;

/**
 * Context of an incoming neighbor message (e.g. ARP, NDP).
 *
 * <p>This includes information about the message accessible through a
 * protocol-agnostic interface, as well as mechanisms to perform an action in
 * response to the incoming message.</p>
 */
public interface NeighbourMessageContext {
    /**
     * Gets the port where the packet came in to the network.
     *
     * @return connect point
     */
    ConnectPoint inPort();

    /**
     * Gets the full parsed representation of the packet.
     *
     * @return ethernet header
     */
    Ethernet packet();

    /**
     * Gets the protocol of the packet.
     *
     * @return protocol
     */
    NeighbourProtocol protocol();

    /**
     * Gets the message type of the packet.
     *
     * @return message type
     */
    NeighbourMessageType type();

    /**
     * Gets the vlan of the packet, if any.
     *
     * @return vlan
     */
    VlanId vlan();

    /**
     * Gets the source MAC address of the message.
     *
     * @return source MAC address
     */
    MacAddress srcMac();

    /**
     * Gets the destination MAC address of the message.
     * <p>
     * Only valid for reply packets, will be null for request packets.
     * </p>
     *
     * @return target MAC address
     */
    MacAddress dstMac();

    /**
     * Gets the target IP address of the message.
     *
     * @return target IP address
     */
    IpAddress target();

    /**
     * Gets the source IP address of the message.
     *
     * @return source IP address
     */
    IpAddress sender();

    /**
     * Forwards the message to a given output port.
     *
     * @param outPort output port
     */
    void forward(ConnectPoint outPort);

    /**
     * Forwards the message to a given interface.
     * <p>
     * The message will be modified to fit the parameters of the outgoing
     * interface. For example, if the interface has a VLAN configured, the
     * outgoing packet will have that VLAN tag added.
     * </p>
     * @param outIntf output interface
     */
    void forward(Interface outIntf);

    /**
     * Replies to the request message with a given MAC address.
     *
     * @param targetMac target MAC address
     */
    void reply(MacAddress targetMac);

    /**
     * Floods the incoming message out all ports except the input port.
     */
    void flood();

    /**
     * Drops the incoming message.
     */
    void drop();

    /**
     * Gets whether this neighbour message context involves a router.
     *
     * @return true if this neighbour message context involves a router
     */
    default boolean isRouter() {
        return false;
    }

    /**
     * Sets whether this neighbour message context involves a router.
     *
     * @param isRouter true if this neighbour message context involves a router
     */
    default void setIsRouter(boolean isRouter) {
    }
}
