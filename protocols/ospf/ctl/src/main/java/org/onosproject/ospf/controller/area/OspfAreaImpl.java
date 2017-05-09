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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import org.onlab.packet.Ip4Address;
import org.onosproject.ospf.controller.LsaWrapper;
import org.onosproject.ospf.controller.OspfArea;
import org.onosproject.ospf.controller.OspfInterface;
import org.onosproject.ospf.controller.OspfLsa;
import org.onosproject.ospf.controller.OspfLsaType;
import org.onosproject.ospf.controller.OspfLsdb;
import org.onosproject.ospf.controller.OspfNbr;
import org.onosproject.ospf.controller.OspfNeighborState;
import org.onosproject.ospf.controller.impl.OspfNbrImpl;
import org.onosproject.ospf.controller.lsdb.OspfLsdbImpl;
import org.onosproject.ospf.protocol.lsa.LsaHeader;
import org.onosproject.ospf.protocol.lsa.subtypes.OspfLsaLink;
import org.onosproject.ospf.protocol.lsa.types.NetworkLsa;
import org.onosproject.ospf.protocol.lsa.types.RouterLsa;
import org.onosproject.ospf.protocol.util.ChecksumCalculator;
import org.onosproject.ospf.protocol.util.OspfInterfaceState;
import org.onosproject.ospf.protocol.util.OspfParameters;
import org.onosproject.ospf.protocol.util.OspfUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Representation an OSPF area and related information.
 */
public class OspfAreaImpl implements OspfArea {
    private static final Logger log = LoggerFactory.getLogger(OspfAreaImpl.class);
    /**
     * Whether AS-external-LSAs will be flooded into/throughout the area.
     */
    private boolean externalRoutingCapability;

    /**
     * Represents a list of all router's interfaces associated with this area.
     */
    private List<OspfInterface> ospfInterfaceList;
    /**
     * The LS Database for this area. It includes router-LSAs, network-LSAs and.
     * summary-LSAs. AS-external-LSAs are hold in the OSPF class itself.
     */
    private OspfLsdbImpl database;
    /**
     * A 32-bit number identifying the area.
     */
    private Ip4Address areaId;
    /**
     * Router ID.
     */
    private Ip4Address routerId;
    /**
     * Represents Options like external, opaque capabilities.
     */
    private int options;
    /**
     * Represents Opaque Enable or not.
     */
    private boolean isOpaqueEnable;

    /**
     * Creates an instance of area implementation.
     */
    public OspfAreaImpl() {
        database = new OspfLsdbImpl(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        OspfAreaImpl that = (OspfAreaImpl) o;
        return Objects.equal(areaId, that.areaId) &&
                Objects.equal(routerId, that.routerId) &&
                Objects.equal(externalRoutingCapability, that.externalRoutingCapability) &&
                Objects.equal(ospfInterfaceList.size(), that.ospfInterfaceList.size()) &&
                Objects.equal(database, that.database);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(areaId, routerId, externalRoutingCapability,
                                ospfInterfaceList, database);
    }

    /**
     * Gets the router id.
     *
     * @return router id
     */
    public Ip4Address routerId() {
        return routerId;
    }

    /**
     * Sets the router id.
     *
     * @param routerId router id
     */
    @JsonProperty("routerId")
    public void setRouterId(Ip4Address routerId) {
        this.routerId = routerId;
    }

    /**
     * Sets opaque enabled to true or false.
     *
     * @param isOpaqueEnable true if opaque enabled else false
     */
    @JsonProperty("isOpaqueEnable")
    public void setIsOpaqueEnabled(boolean isOpaqueEnable) {
        this.isOpaqueEnable = isOpaqueEnable;
    }

    /**
     * Gets is opaque enabled or not.
     *
     * @return true if opaque enabled else false
     */
    public boolean isOpaqueEnabled() {
        return this.isOpaqueEnable;
    }

    /**
     * Initializes link state database.
     */
    public void initializeDb() {

        database.initializeDb();
    }

    /**
     * Refreshes the OSPF area information .
     * Gets called as soon as the interface is down or neighbor full Router LSA is updated.
     *
     * @param ospfInterface OSPF interface instance
     */
    @Override
    public void refreshArea(OspfInterface ospfInterface) {
        OspfInterfaceImpl ospfInterfaceImpl = (OspfInterfaceImpl) ospfInterface;
        log.debug("Inside refreshArea...!!!");
        //If interface state is DR build network LSA.
        if (ospfInterfaceImpl.state() == OspfInterfaceState.DR) {
            if (ospfInterface.listOfNeighbors().size() > 0) {
                //Get the NetworkLsa
                NetworkLsa networkLsa = null;
                try {
                    networkLsa = buildNetworkLsa(ospfInterface.ipAddress(), ospfInterface.ipNetworkMask());
                } catch (Exception e) {
                    log.debug("Error while building NetworkLsa {}", e.getMessage());
                }
                //Add the NetworkLsa to lsdb
                database.addLsa(networkLsa, true, ospfInterface);
                addToOtherNeighborLsaTxList(networkLsa);
            } else {
                log.debug("No Neighbors hence not creating  NetworkLSA...!!!");
            }
        }
        //Get the router LSA
        RouterLsa routerLsa = null;
        try {
            routerLsa = buildRouterLsa(ospfInterface);
        } catch (Exception e) {
            log.debug("Error while building RouterLsa {}", e.getMessage());
        }
        //Add the RouterLSA to lsdb
        database.addLsa(routerLsa, true, ospfInterface);
        addToOtherNeighborLsaTxList(routerLsa);
    }

    /**
     * Builds a network LSA.
     *
     * @param interfaceIp interface IP address
     * @param mask        interface network mask
     * @return NetworkLsa instance
     * @throws Exception might throws exception
     */
    public NetworkLsa buildNetworkLsa(Ip4Address interfaceIp, Ip4Address mask) throws Exception {
        // generate the Router-LSA for this Area.
        NetworkLsa networkLsa = new NetworkLsa();
        networkLsa.setAdvertisingRouter(routerId);
        networkLsa.setLinkStateId(interfaceIp.toString());
        networkLsa.setLsType(OspfLsaType.NETWORK.value());
        networkLsa.setAge(1);
        networkLsa.setOptions(2);
        networkLsa.setNetworkMask(mask);
        //Adding our own router.
        networkLsa.addAttachedRouter(routerId());
        Iterator iter = ospfInterfaceList.iterator();
        OspfInterfaceImpl ospfInterface = null;
        while (iter.hasNext()) {
            ospfInterface = (OspfInterfaceImpl) iter.next();
            if (ospfInterface.ipAddress().equals(interfaceIp)) {
                break;
            }
        }
        if (ospfInterface != null) {
            List<OspfNbr> neighborsInFullState = getNeighborsInFullState(ospfInterface);
            if (neighborsInFullState != null) {
                for (OspfNbr ospfnbr : neighborsInFullState) {
                    networkLsa.addAttachedRouter(ospfnbr.neighborId());
                    log.debug("Adding attached neighbor:: {}", ospfnbr.neighborId());
                }
            }
        }
        networkLsa.setLsSequenceNo(database.getLsSequenceNumber(OspfLsaType.NETWORK));
        //Find the byte length and add it in lsa object
        ChecksumCalculator checksum = new ChecksumCalculator();
        byte[] lsaBytes = networkLsa.asBytes();
        networkLsa.setLsPacketLen(lsaBytes.length);
        //Convert lsa object to byte again to reflect the packet length which we added.
        lsaBytes = networkLsa.asBytes();
        //find the checksum
        byte[] twoByteChecksum = checksum.calculateLsaChecksum(lsaBytes,
                                                               OspfUtil.LSAPACKET_CHECKSUM_POS1,
                                                               OspfUtil.LSAPACKET_CHECKSUM_POS2);
        int checkSumVal = OspfUtil.byteToInteger(twoByteChecksum);
        networkLsa.setLsCheckSum(checkSumVal);
        return networkLsa;
    }

    /**
     * Builds Router LSA.
     *
     * @param ospfInterface Interface instance
     * @return routerLsa Router LSA instance
     * @throws Exception might throws exception
     */
    public RouterLsa buildRouterLsa(OspfInterface ospfInterface) throws Exception {
        // generate the Router-LSA for this Area.
        RouterLsa routerLsa = new RouterLsa();
        routerLsa.setAdvertisingRouter(routerId);
        routerLsa.setLinkStateId(routerId.toString());
        routerLsa.setLsType(OspfLsaType.ROUTER.value());
        routerLsa.setAge(1);
        routerLsa.setOptions(options);
        routerLsa.setAreaBorderRouter(false);
        routerLsa.setAsBoundaryRouter(false);
        routerLsa.setVirtualEndPoint(false);
        buildLinkForRouterLsa(routerLsa, ospfInterface);
        routerLsa.setLsSequenceNo(database.getLsSequenceNumber(OspfLsaType.ROUTER));
        //Find the byte length and add it in lsa object
        ChecksumCalculator checksum = new ChecksumCalculator();
        byte[] lsaBytes = routerLsa.asBytes();
        routerLsa.setLsPacketLen(lsaBytes.length);
        //Convert lsa object to byte again to reflect the packet length whic we added.
        lsaBytes = routerLsa.asBytes();
        //find the checksum
        byte[] twoByteChecksum = checksum.calculateLsaChecksum(lsaBytes,
                                                               OspfUtil.LSAPACKET_CHECKSUM_POS1,
                                                               OspfUtil.LSAPACKET_CHECKSUM_POS2);
        int checkSumVal = OspfUtil.byteToInteger(twoByteChecksum);
        routerLsa.setLsCheckSum(checkSumVal);
        return routerLsa;
    }

    /**
     * Builds LSA link for router LSA.
     *
     * @param routerLsa     router LSA instance
     * @param ospfInterface interface instance
     */
    private void buildLinkForRouterLsa(RouterLsa routerLsa, OspfInterface ospfInterface) {
        OspfInterfaceImpl nextInterface;
        Iterator interfaces = ospfInterfaceList.iterator();
        while (interfaces.hasNext()) {
            nextInterface = (OspfInterfaceImpl) interfaces.next();
            if (nextInterface.state() == OspfInterfaceState.DOWN) {
                continue;
            } else if (nextInterface.state() == OspfInterfaceState.LOOPBACK) {
                OspfLsaLink link = new OspfLsaLink();
                link.setLinkData("-1");
                link.setLinkId(nextInterface.ipAddress().toString());
                link.setLinkType(3);
                link.setMetric(0);
                link.setTos(0);
                routerLsa.addRouterLink(link);
                routerLsa.incrementLinkNo();
            } else if (nextInterface.state() == OspfInterfaceState.POINT2POINT) {
                // adding all neighbour routers
                List<OspfNbr> neighborsInFullState = getNeighborsInFullState(nextInterface);
                if (neighborsInFullState != null) {
                    log.debug("Adding OspfLsaLink ::neighborsInFullState {}, InterfaceIP: {}",
                              neighborsInFullState.size(), nextInterface.ipAddress());
                    for (OspfNbr ospfnbr : neighborsInFullState) {
                        OspfLsaLink link = new OspfLsaLink();
                        link.setLinkData(nextInterface.ipAddress().toString());
                        link.setLinkId(ospfnbr.neighborId().toString());
                        link.setLinkType(1);
                        link.setMetric(0);
                        link.setTos(0);
                        routerLsa.addRouterLink(link);
                        routerLsa.incrementLinkNo();
                        log.debug("Added OspfLsaLink :: {}, neighborIP: {}, routerLinks: {}",
                                  ospfnbr.neighborId(), ospfnbr.neighborIpAddr(), routerLsa.noLink());
                    }
                }
                // adding the self address
                OspfLsaLink link = new OspfLsaLink();
                link.setLinkData(nextInterface.ipNetworkMask().toString());
                link.setLinkId(nextInterface.ipAddress().toString());
                link.setLinkType(3);
                link.setMetric(0);
                link.setTos(0);
                routerLsa.addRouterLink(link);
                routerLsa.incrementLinkNo();
            } else {
                buildLinkForRouterLsaBroadcast(routerLsa, nextInterface);
            }
        }
    }

    /**
     * Builds LSA link for router LSA.
     *
     * @param routerLsa     router LSA instance
     * @param ospfInterface interface instance
     */
    private void buildLinkForRouterLsaBroadcast(RouterLsa routerLsa, OspfInterface ospfInterface) {
        OspfInterfaceImpl ospfInterfaceImpl = (OspfInterfaceImpl) ospfInterface;
        if (ospfInterfaceImpl.state() == OspfInterfaceState.WAITING) {
            OspfLsaLink link = new OspfLsaLink();
            link.setLinkData(ospfInterface.ipNetworkMask().toString());
            //Link id should be set to ip network number
            link.setLinkId(ospfInterface.ipAddress().toString());
            link.setLinkType(3);
            link.setMetric(0);
            link.setTos(0);
            routerLsa.addRouterLink(link);
            routerLsa.incrementLinkNo();
        } else if (ospfInterfaceImpl.state() == OspfInterfaceState.DR) {
            OspfLsaLink link = new OspfLsaLink();
            link.setLinkData(ospfInterface.ipAddress().toString());
            link.setLinkId(ospfInterface.ipAddress().toString());
            link.setLinkType(2);
            link.setMetric(0);
            link.setTos(0);
            routerLsa.addRouterLink(link);
            routerLsa.incrementLinkNo();
        } else if (ospfInterfaceImpl.state() == OspfInterfaceState.BDR ||
                ospfInterfaceImpl.state() == OspfInterfaceState.DROTHER) {
            OspfLsaLink link = new OspfLsaLink();
            link.setLinkData(ospfInterface.ipAddress().toString());
            link.setLinkId(ospfInterface.dr().toString());
            link.setLinkType(2);
            link.setMetric(0);
            link.setTos(0);
            routerLsa.addRouterLink(link);
            routerLsa.incrementLinkNo();
        }
    }

    /**
     * Gets the area id.
     *
     * @return area id
     */
    public Ip4Address areaId() {
        return areaId;
    }

    /**
     * Sets the area id.
     *
     * @param areaId area id
     */
    @JsonProperty("areaId")
    public void setAreaId(Ip4Address areaId) {
        this.areaId = areaId;
    }

    /**
     * Gets external routing capability.
     *
     * @return true if external routing capable, else false
     */
    public boolean isExternalRoutingCapability() {
        return externalRoutingCapability;
    }

    /**
     * Sets external routing capability.
     *
     * @param externalRoutingCapability true if external routing capable, else false
     */
    @JsonProperty("externalRoutingCapability")
    public void setExternalRoutingCapability(boolean externalRoutingCapability) {
        this.externalRoutingCapability = externalRoutingCapability;
    }

    /**
     * Gets the list of interfaces in this area.
     *
     * @return list of interfaces
     */
    public List<OspfInterface> ospfInterfaceList() {
        return ospfInterfaceList;
    }

    /**
     * Sets the list of interfaces attached to the area.
     *
     * @param ospfInterfaceList list of OspfInterface instances
     */
    @JsonProperty("interface")
    public void setOspfInterfaceList(List<OspfInterface> ospfInterfaceList) {
        this.ospfInterfaceList = ospfInterfaceList;
    }

    /**
     * Checks all neighbors belonging to this area whether they are in state EXCHANGE or LOADING.
     * Return false if there is at least one, else return true. This Method is used by
     * "processReceivedLsa()" in the neighbor class.
     *
     * @return boolean indicating that there is no Neighbor in Database Exchange
     */
    public boolean noNeighborInLsaExchangeProcess() {
        OspfInterfaceImpl nextInterface;
        OspfNeighborState nextNeighborState;
        Iterator interfaces = ospfInterfaceList.iterator();
        while (interfaces.hasNext()) {
            nextInterface = (OspfInterfaceImpl) interfaces.next();
            Iterator neighbors = nextInterface.listOfNeighbors().values().iterator();
            while (neighbors.hasNext()) {
                nextNeighborState = ((OspfNbrImpl) neighbors.next()).getState();
                if (nextNeighborState == OspfNeighborState.EXCHANGE ||
                        nextNeighborState == OspfNeighborState.LOADING) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Gets header of all types of LSAs.
     *
     * @param excludeMaxAgeLsa need to include(true) or exclude(false) maxage lsa's
     * @param isOpaquecapable  need to include(true) or exclude(false) Type 10 Opaque lsa's
     * @return list of lsa header in the lsdb
     */
    public List getLsaHeaders(boolean excludeMaxAgeLsa, boolean isOpaquecapable) {
        return database.getAllLsaHeaders(excludeMaxAgeLsa, isOpaquecapable);
    }

    /**
     * Gets the LSA from LSDB based on the input.
     *
     * @param lsType            type of lsa to form the key
     * @param linkStateID       link state id to form the key
     * @param advertisingRouter advertising router to form the key
     * @return lsa wrapper instance which contains the Lsa
     * @throws Exception might throws exception
     */
    public LsaWrapper getLsa(int lsType, String linkStateID, String advertisingRouter) throws Exception {
        String lsaKey = lsType + "-" + linkStateID + "-" + advertisingRouter;
        if (lsType == OspfParameters.LINK_LOCAL_OPAQUE_LSA || lsType == OspfParameters.AREA_LOCAL_OPAQUE_LSA ||
                lsType == OspfParameters.AS_OPAQUE_LSA) {
            byte[] linkStateAsBytes = InetAddress.getByName(linkStateID).getAddress();
            int opaqueType = linkStateAsBytes[0];
            int opaqueId = OspfUtil.byteToInteger(Arrays.copyOfRange(linkStateAsBytes, 1,
                                                                     linkStateAsBytes.length));
            lsaKey = lsType + "-" + opaqueType + opaqueId + "-" + advertisingRouter;
        }
        return database.findLsa(lsType, lsaKey);
    }


    /**
     * Checks whether an instance of the given LSA exists in the database belonging to this area.
     * If so return true else false.
     *
     * @param lookupLsa ospf LSA instance to lookup
     * @return LSA wrapper instance which contains the Lsa
     */
    public LsaWrapper lsaLookup(OspfLsa lookupLsa) {
        return database.lsaLookup((LsaHeader) lookupLsa);
    }

    /**
     * Checks whether an instance of the given LSA exists in the database belonging to this area.
     * If so return true else false.
     *
     * @param lsa1 OSPF LSA instance to compare
     * @param lsa2 OSPF LSA instance to compare
     * @return "same" if both instances are same, "latest" if lsa1 is latest, or "old" if lsa1 is old
     */
    public String isNewerOrSameLsa(OspfLsa lsa1, OspfLsa lsa2) {
        return database.isNewerOrSameLsa((LsaHeader) lsa1, (LsaHeader) lsa2);
    }

    /**
     * Methods gets called from ChannelHandler to add the received LSA to LSDB.
     *
     * @param ospfLsa       OSPF LSA instance
     * @param ospfInterface OSPF interface instance
     * @throws Exception on error
     */
    public void addLsa(OspfLsa ospfLsa, OspfInterface ospfInterface) throws Exception {
        //second param is false as lsa from network
        database.addLsa((LsaHeader) ospfLsa, false, ospfInterface);
    }

    /**
     * Methods gets called from ChannelHandler to add the received LSA to LSDB.
     *
     * @param ospfLsa          OSPF LSA instance
     * @param isSelfOriginated true if the LSA is self originated. Else false
     * @param ospfInterface    OSPF interface instance
     * @throws Exception on error
     */
    public void addLsa(OspfLsa ospfLsa, boolean isSelfOriginated, OspfInterface ospfInterface)
            throws Exception {
        database.addLsa((LsaHeader) ospfLsa, isSelfOriginated, ospfInterface);
    }

    /**
     * Adds the LSA to maxAge bin.
     *
     * @param key     key to add it to LSDB
     * @param wrapper LSA wrapper instance
     */
    public void addLsaToMaxAgeBin(String key, LsaWrapper wrapper) {
        database.addLsaToMaxAgeBin(key, wrapper);
    }

    /**
     * Sets router sequence number for router LSA.
     *
     * @param newSequenceNumber sequence number
     */
    public void setDbRouterSequenceNumber(long newSequenceNumber) {
        database.setRouterLsaSeqNo(newSequenceNumber);
    }

    /**
     * Methods gets called from ChannelHandler to delete the LSA.
     *
     * @param ospfLsa the LSA instance to delete
     */
    public void deleteLsa(LsaHeader ospfLsa) {
        database.deleteLsa(ospfLsa);
    }

    /**
     * Removes LSA from bin.
     *
     * @param lsaWrapper the LSA wrapper instance to delete
     */
    public void removeLsaFromBin(LsaWrapper lsaWrapper) {
        database.removeLsaFromBin(lsaWrapper);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("areaID", areaId)
                .add("ospfInterfaceList", ospfInterfaceList)
                .add("externalRoutingCapability", externalRoutingCapability)
                .toString();
    }

    /**
     * Checks all Neighbors belonging to this Area whether they are in state lesser than the EXCHANGE.
     * Creates list of such neighbors
     * Returns list of neighbors who satisfy the conditions
     *
     * @param ospfInterface OSPF interface instance
     * @return List of interfaces having state lesser than exchange
     */
    public List<OspfNbr> getNeighborsInFullState(OspfInterface ospfInterface) {

        List<OspfNbr> listEligibleNeighbors = null;
        OspfNbrImpl ospfNeighbor = null;
        OspfNeighborState nextNeighborState;
        Iterator nbrInterface = ospfInterface.listOfNeighbors().values().iterator();
        while (nbrInterface.hasNext()) {
            ospfNeighbor = (OspfNbrImpl) nbrInterface.next();
            nextNeighborState = ospfNeighbor.getState();
            if (nextNeighborState.getValue() == OspfNeighborState.FULL.getValue()) {
                if (listEligibleNeighbors == null) {
                    listEligibleNeighbors = new ArrayList<OspfNbr>();
                    listEligibleNeighbors.add(ospfNeighbor);
                } else {
                    listEligibleNeighbors.add(ospfNeighbor);
                }
            }
        }
        return listEligibleNeighbors;
    }

    /**
     * Gets the LSDB LSA key from LSA header.
     *
     * @param lsaHeader LSA header instance
     * @return key LSA key
     */
    public String getLsaKey(LsaHeader lsaHeader) {
        return database.getLsaKey(lsaHeader);
    }

    /**
     * Adds the received LSA in other neighbors tx list.
     *
     * @param recLsa LSA Header instance
     */
    public void addToOtherNeighborLsaTxList(LsaHeader recLsa) {
        //Add the received LSA in other neighbors retransmission list.
        log.debug("OspfAreaImpl: addToOtherNeighborLsaTxList");
        List<OspfInterface> ospfInterfaces = ospfInterfaceList();
        for (OspfInterface ospfInterfaceFromArea : ospfInterfaces) {
            Map neighbors = ospfInterfaceFromArea.listOfNeighbors();
            for (Object neighborIP : neighbors.keySet()) {
                OspfNbrImpl nbr = (OspfNbrImpl) neighbors.get(neighborIP);
                if (nbr.getState().getValue() < OspfNeighborState.EXCHANGE.getValue()) {
                    continue;
                }
                String key = database.getLsaKey(recLsa);
                if (nbr.getState() == OspfNeighborState.EXCHANGE || nbr.getState() == OspfNeighborState.LOADING) {
                    if (nbr.getLsReqList().containsKey(key)) {
                        LsaWrapper lsWrapper = lsaLookup(recLsa);
                        if (lsWrapper != null) {
                            LsaHeader ownLsa = (LsaHeader) lsWrapper.ospfLsa();
                            String status = isNewerOrSameLsa(recLsa, ownLsa);
                            if (status.equals("old")) {
                                continue;
                            } else if (status.equals("same")) {
                                log.debug("OspfAreaImpl: addToOtherNeighborLsaTxList: " +
                                                  "Removing lsa from reTxtList {}", key);
                                nbr.getLsReqList().remove(key);
                                continue;
                            } else {
                                log.debug("OspfAreaImpl: addToOtherNeighborLsaTxList: " +
                                                  "Removing lsa from reTxtList {}", key);
                                nbr.getLsReqList().remove(key);
                            }
                        }
                    }
                }
                if (recLsa.advertisingRouter().toString().equals((String) neighborIP)) {
                    continue;
                }
                if ((recLsa.lsType() == OspfParameters.LINK_LOCAL_OPAQUE_LSA ||
                        recLsa.lsType() == OspfParameters.AREA_LOCAL_OPAQUE_LSA)) {
                    if (nbr.isOpaqueCapable()) {
                        log.debug("OspfAreaImpl: addToOtherNeighborLsaTxList: Adding lsa to reTxtList {}",
                                  recLsa);
                        nbr.getReTxList().put(key, recLsa);
                    }
                } else {
                    log.debug("OspfAreaImpl: addToOtherNeighborLsaTxList: Adding lsa to reTxtList {}",
                              recLsa);
                    nbr.getReTxList().put(key, recLsa);
                }
            }
        }
    }

    /**
     * Gets the options value.
     *
     * @return options value
     */
    public int options() {
        return options;
    }

    /**
     * Sets the options value.
     *
     * @param options options value
     */
    public void setOptions(int options) {
        this.options = options;
    }

    /**
     * Gets the opaque enabled options value.
     *
     * @return opaque enabled options value
     */
    public int opaqueEnabledOptions() {
        return Integer.parseInt(OspfParameters.OPAQUE_ENABLED_OPTION_VALUE, 2);
    }

    /**
     * Gets the lsdb instance for this area.
     *
     * @return lsdb instance
     */
    public OspfLsdb database() {
        return database;
    }
}
