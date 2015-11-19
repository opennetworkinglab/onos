/*
 * Copyright 2015 Open Networking Laboratory
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
     * To set the status of state synchronization.
     *
     * @param value to set the synchronization status
     */
    void setIsSyncComplete(boolean value);

    /**
     * Indicates the state synchronization status of this pcc.
     *
     * @return true/false if the synchronization is completed/not completed
     */
    boolean isSyncComplete();
}
