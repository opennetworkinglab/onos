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
package org.onosproject.isis.controller;

import org.jboss.netty.channel.Channel;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.MacAddress;

import java.util.Set;

/**
 * Representation of an ISIS interface.
 */
public interface IsisInterface {

    /**
     * Returns interface index.
     *
     * @return interface index
     */
    int interfaceIndex();

    /**
     * Sets interface index.
     *
     * @param interfaceIndex interface index
     */
    void setInterfaceIndex(int interfaceIndex);

    /**
     * Returns the interface IP address.
     *
     * @return interface IP address
     */
    Ip4Address interfaceIpAddress();

    /**
     * Sets the interface IP address.
     *
     * @param interfaceIpAddress interface IP address interface IP address
     */
    void setInterfaceIpAddress(Ip4Address interfaceIpAddress);

    /**
     * Returns the network mask.
     *
     * @return network mask
     */
    byte[] networkMask();

    /**
     * Sets the network mask.
     *
     * @param networkMask network mask
     */
    void setNetworkMask(byte[] networkMask);

    /**
     * Sets the interface MAC address.
     *
     * @param interfaceMacAddress interface MAC address
     */
    void setInterfaceMacAddress(MacAddress interfaceMacAddress);

    /**
     * Returns the neighbors list.
     *
     * @return neighbors list
     */
    Set<MacAddress> neighbors();

    /**
     * Sets intermediate system name.
     *
     * @param intermediateSystemName intermediate system name
     */
    void setIntermediateSystemName(String intermediateSystemName);

    /**
     * Returns system ID.
     *
     * @return systemID system ID
     */
    String systemId();

    /**
     * Sets system ID.
     *
     * @param systemId system ID
     */
    void setSystemId(String systemId);

    /**
     * Returns LAN ID.
     *
     * @return LAN ID
     */
    String l1LanId();

    /**
     * Sets LAN ID.
     *
     * @param lanId LAN ID
     */
    void setL1LanId(String lanId);

    /**
     * Returns LAN ID.
     *
     * @return LAN ID
     */
    String l2LanId();

    /**
     * Sets LAN ID.
     *
     * @param lanId LAN ID
     */
    void setL2LanId(String lanId);

    /**
     * Sets ID length.
     *
     * @param idLength ID length
     */
    void setIdLength(int idLength);

    /**
     * Sets max area addresses.
     *
     * @param maxAreaAddresses max area addresses
     */
    void setMaxAreaAddresses(int maxAreaAddresses);

    /**
     * Returns reserved packet circuit type.
     *
     * @return reserved packet circuit type
     */
    int reservedPacketCircuitType();

    /**
     * Sets reserved packet circuit type.
     *
     * @param reservedPacketCircuitType reserved packet circuit type
     */
    void setReservedPacketCircuitType(int reservedPacketCircuitType);

    /**
     * Returns point to point or broadcast.
     *
     * @return 1 if point to point, 2 broadcast
     */
    IsisNetworkType networkType();

    /**
     * Sets point to point.
     *
     * @param networkType point to point
     */
    void setNetworkType(IsisNetworkType networkType);

    /**
     * Returns area address.
     *
     * @return area address
     */
    String areaAddress();

    /**
     * Sets area address.
     *
     * @param areaAddress area address
     */
    void setAreaAddress(String areaAddress);

    /**
     * Sets area length.
     *
     * @param areaLength area length
     */
    void setAreaLength(int areaLength);

    /**
     * Returns holding time.
     *
     * @return holding time
     */
    int holdingTime();

    /**
     * Sets holding time.
     *
     * @param holdingTime holding time
     */
    void setHoldingTime(int holdingTime);

    /**
     * Returns priority.
     *
     * @return priority
     */
    int priority();

    /**
     * Sets priority.
     *
     * @param priority priority
     */
    void setPriority(int priority);

    /**
     * Returns hello interval.
     *
     * @return hello interval
     */
    public int helloInterval();

    /**
     * Sets hello interval.
     *
     * @param helloInterval hello interval
     */
    void setHelloInterval(int helloInterval);

    /**
     * Starts the hello timer which sends hello packet every configured seconds.
     *
     * @param channel netty channel instance
     */
    void startHelloSender(Channel channel);

    /**
     * Stops the hello timer which sends hello packet every configured seconds.
     */
    void stopHelloSender();

    /**
     * Processes an ISIS message which is received on this interface.
     *
     * @param isisMessage ISIS message instance
     * @param isisLsdb    ISIS LSDB instance
     * @param channel     channel instance
     */
    void processIsisMessage(IsisMessage isisMessage, IsisLsdb isisLsdb, Channel channel);

    /**
     * Returns the interface state.
     *
     * @return interface state
     */
    IsisInterfaceState interfaceState();

    /**
     * Sets the interface state.
     *
     * @param interfaceState the interface state
     */
    void setInterfaceState(IsisInterfaceState interfaceState);

    /**
     * Returns the LSDB instance.
     *
     * @return LSDB instance
     */
    IsisLsdb isisLsdb();

    /**
     * Returns intermediate system name.
     *
     * @return intermediate system name
     */
    String intermediateSystemName();

    /**
     * Returns the ISIS neighbor instance if exists.
     *
     * @param isisNeighborMac mac address of the neighbor router
     * @return ISIS neighbor instance if exists else null
     */
    IsisNeighbor lookup(MacAddress isisNeighborMac);

    /**
     * Returns circuit ID.
     *
     * @return circuit ID
     */
    String circuitId();

    /**
     * Sets circuit ID.
     *
     * @param circuitId circuit ID
     */
    void setCircuitId(String circuitId);

    /**
     * Removes neighbor from the interface neighbor map.
     *
     * @param isisNeighbor ISIS neighbor instance
     */
    void removeNeighbor(IsisNeighbor isisNeighbor);

    /**
     * Removes all the neighbors.
     */
    void removeNeighbors();
}