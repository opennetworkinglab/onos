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

import static com.google.common.base.Preconditions.checkState;

/**
 * Context of an incoming neighbor message (e.g. ARP, NDP).
 *
 * <p>This includes information about the message accessible through a
 * protocol-agnostic interface, as well as mechanisms to perform an action in
 * response to the incoming message.</p>
 */
@Beta
public class NeighbourMessageContext {

    private final NeighbourProtocol protocol;
    private final NeighbourMessageType type;

    private final IpAddress target;
    private final IpAddress sender;

    private final Ethernet eth;
    private final ConnectPoint inPort;

    private final NeighbourMessageActions actions;

    /**
     * Creates a new neighbour message context.
     *
     * @param actions actions
     * @param eth ethernet frame
     * @param inPort incoming port
     * @param protocol message protocol
     * @param type message type
     * @param target target IP address
     * @param sender sender IP address
     */
    public NeighbourMessageContext(NeighbourMessageActions actions,
                                   Ethernet eth, ConnectPoint inPort,
                                   NeighbourProtocol protocol, NeighbourMessageType type,
                                   IpAddress target, IpAddress sender) {
        this.actions = actions;
        this.eth = eth;
        this.inPort = inPort;
        this.protocol = protocol;
        this.type = type;
        this.target = target;
        this.sender = sender;
    }

    /**
     * Gets the port where the packet came in to the network.
     *
     * @return connect point
     */
    public ConnectPoint inPort() {
        return inPort;
    }

    /**
     * Gets the full parsed representation of the packet.
     *
     * @return ethernet header
     */
    public Ethernet packet() {
        return eth;
    }

    /**
     * Gets the protocol of the packet.
     *
     * @return protocol
     */
    public NeighbourProtocol protocol() {
        return protocol;
    }

    /**
     * Gets the message type of the packet.
     *
     * @return message type
     */
    public NeighbourMessageType type() {
        return type;
    }

    /**
     * Gets the vlan of the packet, if any.
     *
     * @return vlan
     */
    public VlanId vlan() {
        return VlanId.vlanId(eth.getVlanID());
    }

    /**
     * Gets the source MAC address of the message.
     *
     * @return source MAC address
     */
    public MacAddress srcMac() {
        return MacAddress.valueOf(eth.getSourceMACAddress());
    }

    /**
     * Gets the target IP address of the message.
     *
     * @return target IP address
     */
    public IpAddress target() {
        return target;
    }

    /**
     * Gets the source IP address of the message.
     *
     * @return source IP address
     */
    public IpAddress sender() {
        return sender;
    }

    /**
     * Proxies the message to a given output port.
     *
     * @param outPort output port
     */
    public void proxy(ConnectPoint outPort) {
        actions.proxy(this, outPort);
    }

    /**
     * Proxies the message to a given interface.
     *
     * @param outIntf output interface
     */
    public void proxy(Interface outIntf) {
        actions.proxy(this, outIntf);
    }

    /**
     * Replies to the request message with a given MAC address.
     *
     * @param targetMac target MAC address
     */
    public void reply(MacAddress targetMac) {
        checkState(type == NeighbourMessageType.REQUEST, "can only reply to requests");

        actions.reply(this, targetMac);
    }

    /**
     * Floods the incoming message out all ports except the input port.
     */
    public void flood() {
        actions.flood(this);
    }

    /**
     * Drops the incoming message.
     */
    public void drop() {
        actions.drop(this);
    }

}
