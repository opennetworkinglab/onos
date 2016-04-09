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

import org.onlab.packet.Ip4Address;

import java.util.HashMap;

/**
 * Represents an OSPF Interface.
 */
public interface OspfInterface {

    /**
     * Gets network mask of the interface.
     *
     * @return network mask
     */
    Ip4Address ipNetworkMask();

    /**
     * Sets area id, to which the interface belongs.
     *
     * @param areaId area identifier
     */
    void setAreaId(int areaId);

    /**
     * Sets the authentication key.
     * Interface uses this to authenticate while establishing communication with other routers.
     *
     * @param authKey represents authentication key
     */
    void setAuthKey(String authKey);

    /**
     * Sets the authentication type,
     * Interface uses this to authenticate while establishing communication with other routers.
     *
     * @param authType authType represents authentication type
     */
    void setAuthType(String authType);

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
     * Sets the interface cost which is the cost of sending a data packet onto the network.
     *
     * @param interfaceCost an integer represents interface cost
     */
    void setInterfaceCost(int interfaceCost);

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
     * Sets the polling interval.
     * Polling interval indicates the interval until when the Hello Packets are
     * sent to a dead neighbor.
     *
     * @param pollInterval an integer represents poll interval
     */
    void setPollInterval(int pollInterval);

    /**
     * Sets transmission delay.
     *
     * @param transmitDelay an integer represents delay
     */
    void setTransmitDelay(int transmitDelay);

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
     * Gets the area id to which router belongs.
     *
     * @return areaId an integer value
     */
    int areaId();

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
     * Gets interface cost.
     *
     * @return an integer representing interface cost
     */
    int interfaceCost();

    /**
     * Gets the list of neighbors associated with the interface.
     *
     * @return listOfNeighbors as key value pair
     */
    HashMap<String, OspfNbr> listOfNeighbors();

    /**
     * Gets poll interval.
     *
     * @return pollInterval an integer representing poll interval
     */
    int pollInterval();

    /**
     * Gets transmission delay.
     *
     * @return transmitDelay an integer representing delay
     */
    int transmitDelay();

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
     * Gets authentication key.
     *
     * @return authKey represents authentication key
     */
    String authKey();

    /**
     * Gets authentication type.
     *
     * @return authType represents authentication type
     */
    String authType();

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
}