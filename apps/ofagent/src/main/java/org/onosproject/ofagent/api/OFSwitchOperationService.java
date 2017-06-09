/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.ofagent.api;

import io.netty.channel.Channel;
import org.onosproject.net.Port;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.packet.InboundPacket;
import org.projectfloodlight.openflow.protocol.OFMessage;

/**
 * Service for providing OpenFlow operations.
 */
public interface OFSwitchOperationService {

    /**
     * Processes a new port of the switch.
     * It sends out FEATURE_REPLY message to the controllers.
     *
     * @param port virtual port
     */
    void processPortAdded(Port port);

    /**
     * Processes port link down.
     * It sends out PORT_STATUS asynchronous message to the controllers.
     *
     * @param port virtual port
     */
    void processPortDown(Port port);

    /**
     * Processes port link down.
     * It sends out PORT_STATUS asynchronous message to the controllers.
     *
     * @param port virtual port
     */
    void processPortUp(Port port);

    /**
     * Processes flow removed.
     * It sends out FLOW_REMOVED asynchronous message to the controllers.
     *
     * @param flowRule removed flow rule
     */
    void processFlowRemoved(FlowRule flowRule);

    /**
     * Processes packet in.
     * It sends out PACKET_IN asynchronous message to the controllers.
     *
     * @param packet inbound packet
     */
    void processPacketIn(InboundPacket packet);

    /**
     * Processes commands from the controllers that modify the state of the switch.
     * Possible message types include PACKET_OUT, FLOW_MOD, GROUP_MOD,
     * PORT_MOD, TABLE_MOD. These types of messages can be denied based on a
     * role of the request controller.
     *
     * @param channel received channel
     * @param msg     command message received
     */
    void processControllerCommand(Channel channel, OFMessage msg);

    /**
     * Processes a stats request from the controllers.
     * Targeted message type is MULTIPART_REQUEST with FLOW, PORT, GROUP,
     * GROUP_DESC subtypes.
     *
     * @param channel received channel
     * @param msg     stats request message received
     */
    void processStatsRequest(Channel channel, OFMessage msg);

    /**
     * Processes a role request from the controllers.
     * Targeted message type is ROLE_REQUEST.
     *
     * @param channel received channel
     * @param msg     role request message received
     */
    void processRoleRequest(Channel channel, OFMessage msg);

    /**
     * Processes a features request from the controllers.
     *
     * @param channel received channel
     * @param msg     received features request
     */
    void processFeaturesRequest(Channel channel, OFMessage msg);

    /**
     * Processes LLDP packets from the controller.
     *
     * @param channel received channel
     * @param msg     packet out message with lldp
     */
    void processLldp(Channel channel, OFMessage msg);

    /**
     * Sends hello to the controller.
     *
     * @param channel received channel
     */
    void sendOfHello(Channel channel);

    /**
     * Processes echo request from the controllers.
     *
     * @param channel received channel
     * @param msg     echo request message
     */
    void processEchoRequest(Channel channel, OFMessage msg);

    /**
     * Processes GetConfig request from the controllers.
     *
     * @param channel received channel
     * @param msg     GetConfig request message
     */
    void processGetConfigRequest(Channel channel, OFMessage msg);

    /**
     * Processes SetConfig message from the controllers.
     *
     * @param channel received channel
     * @param msg     SetConfig message
     */
    void processSetConfigMessage(Channel channel, OFMessage msg);

    /**
     * Processes barrier request from the controllers.
     *
     * @param channel received channel
     * @param msg     barrier request message
     */
    void processBarrierRequest(Channel channel, OFMessage msg);
}
