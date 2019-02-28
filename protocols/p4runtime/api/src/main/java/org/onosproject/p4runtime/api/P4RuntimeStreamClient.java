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

import java.math.BigInteger;

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
     * Opportunistically opens a session with the server by starting a
     * StreamChannel RPC and sends a {@code MasterArbitrationUpdate} message
     * with the given election ID. The {@code master} boolean flag is used to
     * indicated if we are trying to became master or not. If false, the
     * implementation might delay sending the {@code MasterArbitrationUpdate}
     * message until another node becomes master with a higher election ID.
     * <p>
     * If the server acknowledges this client as master, the {@link
     * P4RuntimeController} is expected to generate a {@link
     * org.onosproject.net.device.DeviceAgentEvent} with type {@link
     * org.onosproject.net.device.DeviceAgentEvent.Type#ROLE_MASTER}.
     *
     * @param master true if we are trying to become master
     * @param electionId election ID
     */
    void setMastership(boolean master, BigInteger electionId);

    /**
     * Returns true if the StreamChannel RPC is active and hence the P4Runtime
     * session is open, false otherwise.
     *
     * @return boolean
     */
    boolean isSessionOpen();

    /**
     * Closes the session to the server by terminating the Stream RPC.
     */
    void closeSession();

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
