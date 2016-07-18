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

import org.jboss.netty.channel.ChannelHandlerContext;
import org.onlab.packet.Ip4Address;

import java.util.Map;

/**
 * Represents an OSPF Interface.
 */
public interface OspfInterface {

    /**
     * Returns interface index.
     *
     * @return interface index
     */
    public int interfaceIndex();

    /**
     * Sets interface index.
     *
     * @param interfaceIndex interface index
     */
    public void setInterfaceIndex(int interfaceIndex);

    /**
     * Returns OSPF area instance.
     *
     * @return OSPF area instance
     */
    public OspfArea ospfArea();

    /**
     * Sets OSPF area instance.
     *
     * @param ospfArea OSPF area instance
     */
    public void setOspfArea(OspfArea ospfArea);

    /**
     * Gets network mask of the interface.
     *
     * @return network mask
     */
    Ip4Address ipNetworkMask();

    /**
     * Sets the value of BDR.
     * The BDR is calculated during adjacency formation.
     *
     * @param bdr backup designated router's IP address
     */
    void setBdr(Ip4Address bdr);

    /**
     * Sets the value of DR.
     * The DR is calculated during adjacency formation.
     *
     * @param dr designated router's IP address
     */
    void setDr(Ip4Address dr);

    /**
     * Sets the hello interval time.
     * It is the interval at which a hello packet is sent out via this interface.
     *
     * @param helloIntervalTime an integer interval time
     */
    void setHelloIntervalTime(int helloIntervalTime);

    /**
     * Sets router dead interval time.
     * This is the interval after which this interface will trigger a process to kill neighbor.
     *
     * @param routerDeadIntervalTime an integer interval time
     */
    void setRouterDeadIntervalTime(int routerDeadIntervalTime);

    /**
     * Sets interface type.
     * This indicates whether the interface is on point to point mode or broadcast mode.
     *
     * @param interfaceType an integer represents interface type
     */
    void setInterfaceType(int interfaceType);

    /**
     * Sets IP Address of this interface.
     *
     * @param ipAddress IP address
     */
    void setIpAddress(Ip4Address ipAddress);

    /**
     * Sets IP network mask.
     *
     * @param ipNetworkMask network mask
     */
    void setIpNetworkMask(Ip4Address ipNetworkMask);

    /**
     * Sets retransmit interval which indicates the number of seconds between LSA retransmissions.
     *
     * @param reTransmitInterval an integer represents interval
     */
    void setReTransmitInterval(int reTransmitInterval);

    /**
     * Sets MTU.
     *
     * @param mtu an integer represents max transfer unit
     */
    void setMtu(int mtu);

    /**
     * Sets router priority.
     *
     * @param routerPriority value
     */
    void setRouterPriority(int routerPriority);

    /**
     * Gets the IP address.
     *
     * @return an string represents IP address
     */
    Ip4Address ipAddress();

    /**
     * Gets the interface type.
     *
     * @return an integer represents interface type
     */
    int interfaceType();

    /**
     * Gets the MTU.
     *
     * @return an integer representing max transfer unit
     */
    int mtu();

    /**
     * Gets the list of neighbors associated with the interface.
     *
     * @return listOfNeighbors as key value pair
     */
    Map<String, OspfNbr> listOfNeighbors();

    /**
     * Gets the IP address of the BDR.
     *
     * @return bdr BDR's IP address
     */
    Ip4Address bdr();

    /**
     * Gets the ip address of the DR..
     *
     * @return dr DR's IP address
     */
    Ip4Address dr();

    /**
     * Gets hello interval time in seconds, this defines how often we send the hello packet.
     *
     * @return hello interval time in seconds
     */
    int helloIntervalTime();

    /**
     * Gets retransmit interval.
     *
     * @return reTransmitInterval an integer represents interval
     */
    int reTransmitInterval();

    /**
     * Gets router dead interval time.
     * This defines how long we should wait for hello packets before we declare the neighbor is dead.
     *
     * @return routerDeadIntervalTime an integer interval time
     */
    int routerDeadIntervalTime();

    /**
     * Gets router priority.
     *
     * @return routerPriority value
     */
    int routerPriority();

    /**
     * Adds the given neighboring router to the neighbor map.
     *
     * @param ospfNbr neighbor instance
     */
    void addNeighbouringRouter(OspfNbr ospfNbr);

    /**
     * Gets the neighbor instance from listOfNeighbors map for the given neighbor ID.
     *
     * @param neighborId neighbors id
     * @return ospfNbr neighbor instance
     */
    OspfNbr neighbouringRouter(String neighborId);

    /**
     * Checks the given neighbor is in the neighbor list.
     *
     * @param neighborId neighbors id
     * @return true if neighbor in list else false
     */
    boolean isNeighborInList(String neighborId);

    /**
     * Removes LSA headers from the map in which LSA headers are stored.
     *
     * @param lsaKey key used to store lsa in map
     */
    void removeLsaFromNeighborMap(String lsaKey);

    /**
     * When an OSPF message received it is handed over to this method.
     * Based on the type of the OSPF message received it will be handed over
     * to corresponding message handler methods.
     *
     * @param ospfMessage received OSPF message
     * @param ctx         channel handler context instance.
     * @throws Exception might throws exception
     */
    void processOspfMessage(OspfMessage ospfMessage, ChannelHandlerContext ctx) throws Exception;

    /**
     * Represents an interface is up and connected.
     *
     * @throws Exception might throws exception
     */
    void interfaceUp() throws Exception;

    /**
     * Starts the timer which waits for configured seconds and sends Delayed Ack Packet.
     */
    void startDelayedAckTimer();

    /**
     * Stops the delayed acknowledge timer.
     */
    void stopDelayedAckTimer();

    /**
     * Starts the hello timer which sends hello packet every configured seconds.
     */
    void startHelloTimer();

    /**
     * Stops the hello timer.
     */
    void stopHelloTimer();

    /**
     * Gets called when an interface is down.
     * All interface variables are reset, and interface timers disabled.
     * Also all neighbor connections associated with the interface are destroyed.
     */
    void interfaceDown();

    /**
     * Removes all the neighbors.
     */
    void removeNeighbors();
}