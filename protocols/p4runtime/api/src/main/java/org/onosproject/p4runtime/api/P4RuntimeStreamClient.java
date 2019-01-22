/*
 * Copyright 2019-present Open Networking Foundation
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

package org.onosproject.p4runtime.api;

import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.runtime.PiPacketOperation;

/**
 * P4Runtime client interface for the StreamChannel RPC. It allows management of
 * the P4Runtime session (open/close, mastership arbitration) as well as sending
 * packet-outs. All messages received from the server via the stream channel,
 * such as master arbitration updates, or packet-ins, are handled by the
 * P4RuntimeController. Anyone interested in those messages should register a
 * listener with the latter.
 */
public interface P4RuntimeStreamClient {

    /**
     * Opens a session to the server by starting the Stream RPC and sending a
     * mastership arbitration update message with an election ID that is
     * expected to be unique among all available clients. If a client has been
     * requested to become master via {@link #runForMastership()}, then this
     * method should pick an election ID that is lower than the one currently
     * associated with the master client.
     * <p>
     * If the server acknowledges the session to this client as open, the {@link
     * P4RuntimeController} is expected to generate a {@link
     * org.onosproject.net.device.DeviceAgentEvent} with type {@link
     * org.onosproject.net.device.DeviceAgentEvent.Type#CHANNEL_OPEN}.
     */
    void openSession();

    /**
     * Returns true if the Stream RPC is active and the P4Runtime session is
     * open, false otherwise.
     *
     * @return boolean
     */
    boolean isSessionOpen();

    /**
     * Closes the session to the server by terminating the Stream RPC.
     */
    void closeSession();

    /**
     * Sends a master arbitration update to the device with a new election ID
     * that is expected to be the highest one between all clients.
     * <p>
     * If the server acknowledges this client as master, the {@link
     * P4RuntimeController} is expected to generate a {@link
     * org.onosproject.net.device.DeviceAgentEvent} with type {@link
     * org.onosproject.net.device.DeviceAgentEvent.Type#ROLE_MASTER}.
     */
    void runForMastership();

    /**
     * Returns true if this client is master for the server, false otherwise.
     *
     * @return boolean
     */
    boolean isMaster();

    /**
     * Sends a packet-out for the given pipeconf.
     *
     * @param packet   packet-out operation to be performed by the device
     * @param pipeconf pipeconf currently deployed on the device
     */
    void packetOut(PiPacketOperation packet, PiPipeconf pipeconf);
}
