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

package org.onosproject.ospf.controller.area;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import org.onlab.packet.Ip4Address;
import org.onosproject.ospf.controller.OspfInterface;
import org.onosproject.ospf.controller.OspfNbr;
import org.onosproject.ospf.protocol.lsa.LsaHeader;
import org.onosproject.ospf.protocol.lsa.OpaqueLsaHeader;
import org.onosproject.ospf.protocol.util.OspfInterfaceState;
import org.onosproject.ospf.protocol.util.OspfParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * Representation of an OSPF interface.
 */
public class OspfInterfaceImpl implements OspfInterface {
    private static final Logger log = LoggerFactory.getLogger(OspfInterfaceImpl.class);
    private Ip4Address ipAddress;
    private Ip4Address ipNetworkMask;
    private int areaId;
    private int helloIntervalTime;
    private int routerDeadIntervalTime;
    private int transmitDelay;
    private int routerPriority;
    private int systemInterfaceType;
    private int interfaceType;
    private int interfaceCost;
    private String authType;
    private String authKey;
    private int pollInterval;
    private int mtu;
    private int reTransmitInterval;
    private Ip4Address dr;
    private Ip4Address bdr;
    private OspfInterfaceState state;
    private List<LsaHeader> linkStateHeaders = new ArrayList<>();
    private HashMap<String, OspfNbr> listOfNeighbors = new HashMap<>();
    private HashMap<String, LsaHeader> listOfNeighborMap = new HashMap<>();

    /**
     * Gets the interface state.
     *
     * @return interfaceState state of the interface
     */
    public OspfInterfaceState state() {
        return state;
    }

    /**
     * Sets the interface state.
     *
     * @param ospfInterfaceState interface state enum instance
     */
    public void setState(OspfInterfaceState ospfInterfaceState) {
        this.state = ospfInterfaceState;
    }

    /**
     * Gets link state headers.
     *
     * @return get the list of lsa headers
     */
    public List<LsaHeader> linkStateHeaders() {
        Set<String> key = listOfNeighborMap.keySet();
        for (String keys : key) {
            LsaHeader lsaHeader = listOfNeighborMap.get(keys);
            linkStateHeaders.add(lsaHeader);
        }
        return linkStateHeaders;
    }

    /**
     * Gets IP network mask.
     *
     * @return network mask
     */
    public Ip4Address ipNetworkMask() {
        return ipNetworkMask;
    }

    /**
     * Sets IP network mask.
     *
     * @param ipNetworkMask network mask
     */
    @Override
    public void setIpNetworkMask(Ip4Address ipNetworkMask) {
        this.ipNetworkMask = ipNetworkMask;
    }

    /**
     * Adds neighboring router to list.
     *
     * @param ospfNbr ospfNbr instance
     */
    public void addNeighbouringRouter(OspfNbr ospfNbr) {
        listOfNeighbors.put(ospfNbr.neighborId().toString(), ospfNbr);
    }

    /**
     * Gets the neighbour details from listOfNeighbors map.
     *
     * @param neighborId neighbors id
     * @return ospfNbr neighbor instance
     */
    public OspfNbr neighbouringRouter(String neighborId) {
        return listOfNeighbors.get(neighborId);
    }


    /**
     * Adds LSAHeader to map.
     *
     * @param lsaHeader LSA header instance
     */
    public void addLsaHeaderForDelayAck(LsaHeader lsaHeader) {
        String key = lsaHeader.lsType() + "-" + lsaHeader.linkStateId() + "-" +
                lsaHeader.advertisingRouter();
        if (lsaHeader.lsType() == OspfParameters.LINK_LOCAL_OPAQUE_LSA ||
                lsaHeader.lsType() == OspfParameters.AREA_LOCAL_OPAQUE_LSA ||
                lsaHeader.lsType() == OspfParameters.AS_OPAQUE_LSA) {
            OpaqueLsaHeader header = (OpaqueLsaHeader) lsaHeader;
            key = lsaHeader.lsType() + "-" + header.opaqueType() + header.opaqueId()
                    + "-" + lsaHeader.advertisingRouter();
        }

        log.debug("Adding LSA key {} for delayed Ack", key);
        listOfNeighborMap.put(key, lsaHeader);
    }

    /**
     * Removes LSA header from map.
     *
     * @param lsaKey key used to store LSA in map
     */
    public void removeLsaFromNeighborMap(String lsaKey) {
        listOfNeighborMap.remove(lsaKey);
    }

    /**
     * Checks neighbor is in the list or not.
     *
     * @param neighborId neighbors id
     * @return true if neighbor in list else false
     */
    public boolean isNeighborInList(String neighborId) {
        return listOfNeighbors.containsKey(neighborId);
    }

    /**
     * Gets the list of neighbors.
     *
     * @return listOfNeighbors as key value pair
     */
    public HashMap<String, OspfNbr> listOfNeighbors() {
        return listOfNeighbors;
    }

    /**
     * Sets the list of neighbors.
     *
     * @param listOfNeighbors as key value pair
     */
    public void setListOfNeighbors(HashMap<String, OspfNbr> listOfNeighbors) {
        this.listOfNeighbors = listOfNeighbors;
    }

    /**
     * Gets the IP address.
     *
     * @return IP address
     */
    public Ip4Address ipAddress() {
        return ipAddress;
    }

    /**
     * Sets the interface IP address.
     *
     * @param ipAddress interface IP address
     */
    public void setIpAddress(Ip4Address ipAddress) {
        this.ipAddress = ipAddress;
    }

    /**
     * Gets router priority.
     *
     * @return routerPriority value
     */
    public int routerPriority() {
        return routerPriority;
    }

    /**
     * Sets router priority.
     *
     * @param routerPriority value
     */
    public void setRouterPriority(int routerPriority) {
        this.routerPriority = routerPriority;
    }

    /**
     * Gets the area id this interface belongs.
     *
     * @return area id this interface belongs
     */
    public int areaId() {
        return areaId;
    }


    /**
     * Sets the area id this interface belongs.
     *
     * @param areaId the area id this interface belongs
     */
    public void setAreaId(int areaId) {
        this.areaId = areaId;
    }

    /**
     * Gets hello interval time.
     *
     * @return hello interval time
     */
    public int helloIntervalTime() {
        return helloIntervalTime;
    }

    /**
     * Sets hello interval time.
     *
     * @param helloIntervalTime an integer interval time
     */
    public void setHelloIntervalTime(int helloIntervalTime) {
        this.helloIntervalTime = helloIntervalTime;
    }

    /**
     * Gets router dead interval time.
     *
     * @return router dead interval time
     */
    public int routerDeadIntervalTime() {
        return routerDeadIntervalTime;
    }

    /**
     * Sets router dead interval time.
     *
     * @param routerDeadIntervalTime router dead interval time
     */
    public void setRouterDeadIntervalTime(int routerDeadIntervalTime) {
        this.routerDeadIntervalTime = routerDeadIntervalTime;
    }

    /**
     * Gets interface type.
     *
     * @return interfaceType an integer represents interface type
     */
    public int interfaceType() {
        return interfaceType;
    }

    /**
     * Sets interface type.
     *
     * @param interfaceType interface type
     */
    public void setInterfaceType(int interfaceType) {
        this.interfaceType = interfaceType;
    }

    /**
     * Gets interface cost.
     *
     * @return interface cost
     */
    public int interfaceCost() {
        return interfaceCost;
    }

    /**
     * Sets interface cost.
     *
     * @param interfaceCost interface cost
     */
    public void setInterfaceCost(int interfaceCost) {
        this.interfaceCost = interfaceCost;
    }

    /**
     * Gets authentication type.
     *
     * @return authType represents authentication type
     */
    public String authType() {
        return authType;
    }

    /**
     * Sets authentication type.
     *
     * @param authType authType represents authentication type
     */
    public void setAuthType(String authType) {
        this.authType = authType;
    }

    /**
     * Gets authentication key.
     *
     * @return authKey represents authentication key
     */
    public String authKey() {
        return authKey;
    }

    /**
     * Sets authentication key.
     *
     * @param authKey represents authentication key
     */
    public void setAuthKey(String authKey) {
        this.authKey = authKey;
    }

    /**
     * Gets poll interval.
     *
     * @return pollInterval an integer represents poll interval
     */
    public int pollInterval() {
        return pollInterval;
    }

    /**
     * Sets poll interval.
     *
     * @param pollInterval an integer represents poll interval
     */
    public void setPollInterval(int pollInterval) {
        this.pollInterval = pollInterval;
    }

    /**
     * Gets max transfer unit.
     *
     * @return mtu an integer represents max transfer unit
     */
    public int mtu() {
        return mtu;
    }

    /**
     * Sets max transfer unit.
     *
     * @param mtu max transfer unit
     */
    public void setMtu(int mtu) {
        this.mtu = mtu;
    }

    /**
     * Gets retransmit interval.
     *
     * @return retransmit interval
     */
    public int reTransmitInterval() {
        return reTransmitInterval;
    }

    /**
     * Sets retransmit interval.
     *
     * @param reTransmitInterval retransmit interval
     */
    public void setReTransmitInterval(int reTransmitInterval) {
        this.reTransmitInterval = reTransmitInterval;
    }

    /**
     * Gets designated routers IP address.
     *
     * @return dr designated routers IP address
     */
    public Ip4Address dr() {
        return dr;
    }

    /**
     * Sets designated routers IP address.
     *
     * @param dr designated routers IP address
     */
    public void setDr(Ip4Address dr) {
        this.dr = dr;
    }

    /**
     * Gets backup designated routers IP address.
     *
     * @return bdr backup designated routers IP address
     */
    public Ip4Address bdr() {
        return bdr;
    }

    /**
     * Sets backup designated routers IP address.
     *
     * @param bdr backup designated routers IP address
     */
    public void setBdr(Ip4Address bdr) {
        this.bdr = bdr;
    }

    /**
     * Get transmission delay.
     *
     * @return transmission delay
     */
    public int transmitDelay() {
        return transmitDelay;
    }

    /**
     * Sets transmission delay.
     *
     * @param transmitDelay transmission delay
     */
    public void setTransmitDelay(int transmitDelay) {
        this.transmitDelay = transmitDelay;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        OspfInterfaceImpl that = (OspfInterfaceImpl) o;
        return Objects.equal(areaId, that.areaId) &&
                Objects.equal(helloIntervalTime, that.helloIntervalTime) &&
                Objects.equal(routerDeadIntervalTime, that.routerDeadIntervalTime) &&
                Objects.equal(transmitDelay, that.transmitDelay) &&
                Objects.equal(routerPriority, that.routerPriority) &&
                Objects.equal(systemInterfaceType, that.systemInterfaceType) &&
                Objects.equal(interfaceType, that.interfaceType) &&
                Objects.equal(interfaceCost, that.interfaceCost) &&
                Objects.equal(pollInterval, that.pollInterval) &&
                Objects.equal(mtu, that.mtu) &&
                Objects.equal(reTransmitInterval, that.reTransmitInterval) &&
                Objects.equal(ipAddress, that.ipAddress) &&
                Objects.equal(ipNetworkMask, that.ipNetworkMask) &&
                Objects.equal(listOfNeighbors, that.listOfNeighbors) &&
                Objects.equal(authType, that.authType) &&
                Objects.equal(authKey, that.authKey) &&
                Objects.equal(dr, that.dr) &&
                Objects.equal(bdr, that.bdr);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(ipAddress, ipNetworkMask, areaId, helloIntervalTime,
                                routerDeadIntervalTime, transmitDelay, routerPriority, listOfNeighbors,
                                systemInterfaceType, interfaceType, interfaceCost, authType, authKey,
                                pollInterval, mtu, reTransmitInterval, dr, bdr);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("ipAddress", ipAddress)
                .add("routerPriority", routerPriority)
                .add("areaID", areaId)
                .add("helloIntervalTime", helloIntervalTime)
                .add("routerDeadIntervalTime", routerDeadIntervalTime)
                .add("interfaceType", interfaceType)
                .add("interfaceCost", interfaceCost)
                .add("authType", authType)
                .add("authKey", authKey)
                .add("pollInterval", pollInterval)
                .add("mtu", mtu)
                .add("reTransmitInterval", reTransmitInterval)
                .add("dr", dr)
                .add("bdr", bdr)
                .add("transmitDelay", transmitDelay)
                .toString();
    }
}