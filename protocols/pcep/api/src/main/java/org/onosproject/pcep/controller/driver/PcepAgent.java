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
package org.onosproject.pcep.controller.driver;

import org.onosproject.pcep.controller.PccId;
import org.onosproject.pcep.controller.PcepClient;
import org.onosproject.pcepio.protocol.PcepMessage;

/**
 * Responsible for keeping track of the current set Pcep clients
 * connected to the system.
 *
 */
public interface PcepAgent {

    /**
     * Add a pcc client that has just connected to the system.
     *
     * @param pccId the id of pcc client to add
     * @param pc the actual pce client object.
     * @return true if added, false otherwise.
     */
    boolean addConnectedClient(PccId pccId, PcepClient pc);

    /**
     * Checks if the activation for this pcc client is valid.
     *
     * @param pccId the id of pcc client to check
     * @return true if valid, false otherwise
     */
    boolean validActivation(PccId pccId);

    /**
     * Clear all state in controller client maps for a pcc client that has
     * disconnected from the local controller. Also release control for
     * that pccIds client from the global repository. Notify client listeners.
     *
     * @param pccIds the id of pcc client to remove.
     */
    void removeConnectedClient(PccId pccIds);

    /**
     * Process a message coming from a pcc client.
     *
     * @param pccId the id of pcc client the message was received.
     * @param m the message to process
     */
    void processPcepMessage(PccId pccId, PcepMessage m);

    /**
     * Adds PCEP device when session is successfully established.
     *
     * @param pc PCEP client details
     */
    void addNode(PcepClient pc);

    /**
     * Removes PCEP device when session is disconnected.
     *
     * @param pccId PCEP client ID
     */
    void deleteNode(PccId pccId);

    /**
     * Analyzes report messages received during LSP DB sync again tunnel store and takes necessary actions.
     *
     * @param pccId the id of pcc client
     * @return success or failure
     */
    boolean analyzeSyncMsgList(PccId pccId);
}
