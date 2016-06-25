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

import org.jboss.netty.channel.Channel;
import org.onosproject.pcep.controller.PccId;
import org.onosproject.pcep.controller.PcepClient;
import org.onosproject.pcep.controller.PcepPacketStats;
import org.onosproject.pcepio.protocol.PcepVersion;


/**
 * Represents the driver side of an Path computation client(pcc).
 *
 */
public interface PcepClientDriver extends PcepClient {

    /**
     * Sets the Pcep agent to be used. This method
     * can only be called once.
     *
     * @param agent the agent to set.
     */
    void setAgent(PcepAgent agent);

    /**
     * Announce to the Pcep agent that this pcc client has connected.
     *
     * @return true if successful, false if duplicate switch.
     */
    boolean connectClient();

    /**
     * Remove this pcc client from the Pcep agent.
     */
    void removeConnectedClient();

    /**
     * Sets the PCEP version for this pcc.
     *
     * @param pcepVersion the version to set.
     */
    void setPcVersion(PcepVersion pcepVersion);

    /**
     * Sets the associated Netty channel for this pcc.
     *
     * @param channel the Netty channel
     */
    void setChannel(Channel channel);


    /**
     * Sets the keep alive time for this pcc.
     *
     * @param keepAliveTime the keep alive time to set.
     */
    void setPcKeepAliveTime(byte keepAliveTime);

    /**
     * Sets the dead time for this pcc.
     *
     * @param deadTime the dead timer value to set.
     */
    void setPcDeadTime(byte deadTime);

    /**
     * Sets the session id for this pcc.
     *
     * @param sessionId the session id value to set.
     */
    void setPcSessionId(byte sessionId);

    /**
     * Sets whether the pcc is connected.
     *
     * @param connected whether the pcc is connected
     */
    void setConnected(boolean connected);

    /**
     * Initializes the behavior.
     *
     * @param pccId id of pcc
     * @param pcepVersion Pcep version
     * @param pktStats Pcep Packet Stats
     */
    void init(PccId pccId, PcepVersion pcepVersion, PcepPacketStats pktStats);

    /**
     * Checks whether the handshake is complete.
     *
     * @return true is finished, false if not.
     */
    boolean isHandshakeComplete();

}
