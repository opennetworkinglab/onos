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

package org.onosproject.incubator.net.neighbour;

import com.google.common.annotations.Beta;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.incubator.net.intf.Interface;
import org.onosproject.net.ConnectPoint;

/**
 * Context of an incoming neighbor message (e.g. ARP, NDP).
 *
 * <p>This includes information about the message accessible through a
 * protocol-agnostic interface, as well as mechanisms to perform an action in
 * response to the incoming message.</p>
 */
@Beta
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
     * Proxies the message to a given output port.
     *
     * @param outPort output port
     */
    void proxy(ConnectPoint outPort);

    /**
     * Proxies the message to a given interface.
     *
     * @param outIntf output interface
     */
    void proxy(Interface outIntf);

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
}
