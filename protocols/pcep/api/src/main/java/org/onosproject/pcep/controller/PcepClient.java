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
package org.onosproject.pcep.controller;

import java.util.List;

import org.onosproject.pcepio.protocol.PcepFactory;
import org.onosproject.pcepio.protocol.PcepMessage;
import org.onosproject.pcepio.protocol.PcepStateReport;

/**
 * Represents to provider facing side of a path computation client(pcc).
 */
public interface PcepClient {

    /**
     * Writes the message to the driver.
     *
     * @param msg the message to write
     */
    void sendMessage(PcepMessage msg);

    /**
     * Writes the PcepMessage list to the driver.
     *
     * @param msgs the messages to be written
     */
    void sendMessage(List<PcepMessage> msgs);

    /**
     * Handle a message from the pcc.
     *
     * @param fromClient the message to handle
     */
    void handleMessage(PcepMessage fromClient);

    /**
     * Provides the factory for this PCEP version.
     *
     * @return PCEP version specific factory.
     */
    PcepFactory factory();

    /**
     * Gets a string version of the ID for this pcc.
     *
     * @return string version of the ID
     */
    String getStringId();

    /**
     * Gets the ipAddress of the client.
     *
     * @return the client pccId in IPAddress format
     */
    PccId getPccId();

    /**
     * Checks if the pcc is still connected.
     *
     * @return true if client is connected, false otherwise
     */
    boolean isConnected();

    /**
     * Disconnects the pcc by closing the TCP connection. Results in a call
     * to the channel handler's channelDisconnected method for cleanup.
     */
    void disconnectClient();

    /**
     * Indicates if this pcc is optical.
     *
     * @return true if optical
     */
    boolean isOptical();

    /**
     * Identifies the channel used to communicate with the pcc.
     *
     * @return string representation of the connection to the client
     */
    String channelId();

    /**
     * Sets the status of LSP state synchronization.
     *
     * @param syncStatus LSP synchronization status to be set
     */
    void setLspDbSyncStatus(PcepSyncStatus syncStatus);

    /**
     * Indicates the LSP state synchronization status of this pcc.
     *
     * @return LSP state synchronization status.
     */
    PcepSyncStatus lspDbSyncStatus();

    /**
     * Sets the status of label DB synchronization.
     *
     * @param syncStatus label DB synchronization status to be set
     */
    void setLabelDbSyncStatus(PcepSyncStatus syncStatus);

    /**
     * Indicates the label DB synchronization status of this pcc.
     *
     * @return label DB synchronization status.
     */
    PcepSyncStatus labelDbSyncStatus();

    /**
     * Sets capability negotiated during open message exchange.
     *
     * @param capability supported by client
     */
    void setCapability(ClientCapability capability);

    /**
     * Obtains capability supported by client.
     *
     * @return capability supported by client
     */
    ClientCapability capability();

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
     * Sets D flag for the given LSP and its LSP info.
     *
     * @param lspKey contains LSP info
     * @param dFlag delegation flag in LSP object
     */
    void setLspAndDelegationInfo(LspKey lspKey, boolean dFlag);

    /**
     * Returns delegation flag for the given LSP info.
     *
     * @param lspKey contains LSP info
     * @return delegation flag
     */
    Boolean delegationInfo(LspKey lspKey);

    /**
     * Creates a temporary cache to hold report messages received during LSPDB sync.
     *
     * @param pccId PCC id which is the key to store report messages
     */
    void initializeSyncMsgList(PccId pccId);

    /**
     * Returns the list of report messages received during LSPDB sync.
     *
     * @param pccId PCC id which is the key for all the report messages
     * @return list of report messages received during LSPDB sync
     */
    List<PcepStateReport> getSyncMsgList(PccId pccId);

    /**
     * Removes the list of report messages received during LSPDB sync.
     *
     * @param pccId PCC id which is the key for all the report messages
     */
    void removeSyncMsgList(PccId pccId);

    /**
     * Adds report message received during LSPDB sync into temporary cache.
     *
     * @param pccId PCC id which is the key to store report messages
     * @param rptMsg the report message to be stored
     */
    void addSyncMsgToList(PccId pccId, PcepStateReport rptMsg);
}
