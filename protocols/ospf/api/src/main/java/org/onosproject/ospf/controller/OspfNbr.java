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
package org.onosproject.ospf.controller;

import org.jboss.netty.channel.Channel;
import org.onlab.packet.Ip4Address;

import java.util.Map;

/**
 * Represents an OSPF neighbor.
 */
public interface OspfNbr {

    /**
     * Gets neighbor's id.
     *
     * @return neighbor's id
     */
    public Ip4Address neighborId();

    /**
     * Gets router priority.
     *
     * @return router priority
     */
    public int routerPriority();

    /**
     * Gets the IP address of this neighbor.
     *
     * @return the IP address of this neighbor
     */
    public Ip4Address neighborIpAddr();

    /**
     * Gets the neighbor's DR address.
     *
     * @return neighbor's DR address
     */
    public Ip4Address neighborDr();

    /**
     * Gets the neighbor's BDR address.
     *
     * @return neighbor's BDR address
     */
    Ip4Address neighborBdr();

    /**
     * Determines whether an adjacency should be established/maintained with the neighbor.
     *
     * @param ch netty channel instance
     */
    void adjOk(Channel ch);

    /**
     * Gets the pending re transmit list as a map.
     *
     * @return pending re transmit list as a map
     */
    Map<String, OspfLsa> getPendingReTxList();

    /**
     * Sets the neighbor's id.
     *
     * @param neighborId neighbor's id
     */
    void setNeighborId(Ip4Address neighborId);

    /**
     * Sets the neighbor's BDR address.
     *
     * @param neighborBdr neighbor's BDR address
     */
    void setNeighborBdr(Ip4Address neighborBdr);

    /**
     * Sets the neighbor's DR address.
     *
     * @param neighborDr neighbor's DR address
     */
    void setNeighborDr(Ip4Address neighborDr);

    /**
     * Sets router priority.
     *
     * @param routerPriority router priority
     */
    void setRouterPriority(int routerPriority);

    /**
     * Sets the neighbor is opaque enabled or not.
     *
     * @param isOpaqueCapable true if the neighbor is opaque enabled else false
     */
    void setIsOpaqueCapable(boolean isOpaqueCapable);

    /**
     * Sets neighbor is master or not.
     *
     * @param isMaster neighbor is master or not
     */
    void setIsMaster(int isMaster);

    /**
     * Gets the DD sequence number.
     *
     * @return DD sequence number
     */
    long ddSeqNum();

    /**
     * Sets the DD sequence number.
     *
     * @param ddSeqNum DD sequence number
     */
    void setDdSeqNum(long ddSeqNum);

    /**
     * Gets neighbor is master or not.
     *
     * @return true if neighbor is master else false
     */
    int isMaster();

    /**
     * Gets the options value.
     *
     * @return options value
     */
    int options();

    /**
     * Sets the options value.
     *
     * @param options options value
     */
    void setOptions(int options);

    /**
     * An invalid request for LSA has been received.
     * This indicates an error in the Database Exchange process. Actions to be performed
     * are the same as in seqNumMismatch. In addition, stop the possibly activated
     * retransmission timer.
     *
     * @param ch netty channel instance
     * @throws Exception might throw exception
     */
    void badLSReq(Channel ch) throws Exception;

    /**
     * Gets the LS request list.
     *
     * @return LS request list
     */
    Map getLsReqList();

    /**
     * Gets the reTxList instance.
     *
     * @return reTxList instance
     */
    Map getReTxList();

    /**
     * Gets if the neighbor is opaque enabled or not.
     *
     * @return true if the neighbor is opaque enabled else false.
     */
    public boolean isOpaqueCapable();

    /**
     * Gets the neighbor's state.
     *
     * @return neighbor's state
     */
    OspfNeighborState getState();

    /**
     * Starts the inactivity timer.
     */
    void startInactivityTimeCheck();

    /**
     * Stops the inactivity timer.
     */
    void stopInactivityTimeCheck();

    /**
     * Sets router dead interval.
     *
     * @param routerDeadInterval router dead interval
     */
    void setRouterDeadInterval(int routerDeadInterval);

    /**
     * Stops the flooding timer.
     */
    void stopFloodingTimer();

    /**
     * Stops the Dd Retransmission executor task.
     */
    void stopRxMtDdTimer();

    /**
     * Stops Ls request retransmission executor task.
     */
    void stopRxMtLsrTimer();
}