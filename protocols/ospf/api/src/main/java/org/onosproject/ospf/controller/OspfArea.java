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

import java.util.List;

/**
 * Representation of an OSPF area. OSPF areas are collections of network segments.
 * The configuration of OSPF area consists of assigning an area id to each network segment.
 * Each area has its own link state database.
 */
public interface OspfArea {

    /**
     * Gets the router id associated with the area.
     *
     * @return router id
     */
    Ip4Address routerId();

    /**
     * Sets the router id for this area.
     *
     * @param routerId router's ip address
     */
    void setRouterId(Ip4Address routerId);

    /**
     * Gets the area id.
     *
     * @return area id
     */
    Ip4Address areaId();

    /**
     * Sets the area id.
     *
     * @param areaId area id as an IPv4 address
     */
    void setAreaId(Ip4Address areaId);

    /**
     * Gets the LSDB instance for this area.
     *
     * @return LSDB instance for this area
     */
    OspfLsdb database();

    /**
     * Checks whether an instance of the given LSA exists in the database.
     *
     * @param lookupLsa LSA instance to lookup
     * @return LSA wrapper instance which contains the LSA
     */
    LsaWrapper lsaLookup(OspfLsa lookupLsa);

    /**
     * Initializes link state database, this acts as a place holder for storing the received LSA.
     */
    void initializeDb();

    /**
     * Sets the options value.
     *
     * @param options integer value
     */
    void setOptions(int options);

    /**
     * Gets external routing capability.
     * This indicates Whether AS-external-LSAs will be flooded into/throughout the area.
     *
     * @return true if external routing capable, else false
     */
    boolean isExternalRoutingCapability();

    /**
     * Sets external routing capability.
     * This indicates Whether AS-external-LSAs will be flooded into/throughout the area.
     *
     * @param externalRoutingCapability true if external routing capable, else false
     */
    void setExternalRoutingCapability(boolean externalRoutingCapability);

    /**
     * Gets if the router is opaque enabled or not.
     * This indicates whether the router accepts opaque LSA.
     *
     * @return true if opaque enabled else false
     */
    boolean isOpaqueEnabled();

    /**
     * Gets the list of interfaces attached to this area.
     *
     * @return list of interfaces
     */
    List<OspfInterface> ospfInterfaceList();

    /**
     * Sets the list of interfaces attached to this area.
     *
     * @param interfacesLst list of interface instances
     */
    void setOspfInterfaceList(List<OspfInterface> interfacesLst);

    /**
     * Gets the options value, which indicates the supported optional capabilities.
     *
     * @return options value
     */
    int options();

    /**
     * Gets the opaque enabled options value, which indicates support of opaque capabilities.
     *
     * @return opaque enabled options value
     */
    int opaqueEnabledOptions();

    /**
     * Sets opaque enabled to true or false, which indicates whether the router accepts opaque LSA.
     *
     * @param isOpaqueEnable true if opaque enabled else false
     */
    void setIsOpaqueEnabled(boolean isOpaqueEnable);

    /**
     * Refreshes areas, by sending a router LSA and network LSA (in case of DR).
     * with a new sequence number.
     *
     * @param ospfInterface interface instance
     * @throws Exception might throw exception
     */
    void refreshArea(OspfInterface ospfInterface) throws Exception;

    /**
     * Verifies no neighbor is in exchange process.
     *
     * @return boolean indicating that there is no Neighbor in Database Exchange
     */
    boolean noNeighborInLsaExchangeProcess();

    /**
     * Checks whether an instance of the given LSA exists in the database belonging to this area.
     * If so return true else false.
     *
     * @param lsa1 LSA instance to compare
     * @param lsa2 LSA instance to compare
     * @return "same" if both instances are same, "latest" if lsa1 is latest, or "old" if lsa1 is old
     */
    String isNewerOrSameLsa(OspfLsa lsa1, OspfLsa lsa2);

    /**
     * Whenever we receive an LSA with max age - we put it in the max age bin.
     * This is later used to flush LSAs out of the routing domain.
     *
     * @param key     key to add it to LSDB
     * @param wrapper LSA wrapper instance
     */
    void addLsaToMaxAgeBin(String key, LsaWrapper wrapper);

    /**
     * Whenever an LSA is being flushed out or reaches max age, it must be stopped from aging.
     * This achieved by removing it from bin.
     *
     * @param lsaWrapper the LSA wrapper instance to delete
     */
    void removeLsaFromBin(LsaWrapper lsaWrapper);

    /**
     * Adds the received LSA to LSDB, this method creates an LSA wrapper for the LSA.
     * Also adds it to the LSDB of the area. This method is specifically called for
     * the self originated LSAs.
     *
     * @param ospfLsa          LSA instance
     * @param isSelfOriginated true if the LSA is self originated else false
     * @param ospfInterface    interface instance
     * @throws Exception might throws exception
     */
    void addLsa(OspfLsa ospfLsa, boolean isSelfOriginated, OspfInterface ospfInterface)
            throws Exception;

    /**
     * Adds the received LSA to LSDB,this method creates an LSA wrapper for the LSA.
     * Adds it to the LSDB of the area.
     *
     * @param ospfLsa       LSA instance
     * @param ospfInterface interface instance
     * @throws Exception might throws exception
     */
    void addLsa(OspfLsa ospfLsa, OspfInterface ospfInterface) throws Exception;

    /**
     * Sets router sequence number for router LSA.
     *
     * @param newSequenceNumber sequence number
     */
    void setDbRouterSequenceNumber(long newSequenceNumber);

    /**
     * Gets LSA header of all types of LSAs present in the link state database.
     *
     * @param excludeMaxAgeLsa need to include(true) or exclude(false) max age LSA
     * @param isOpaqueCapable  need to include(true) or exclude(false) type 10 Opaque LSA
     * @return list of LSA header in the LSDB
     */
    List getLsaHeaders(boolean excludeMaxAgeLsa, boolean isOpaqueCapable);

    /**
     * Gets the LSA wrapper from link state database based on the parameters passed.
     *
     * @param lsType            type of LSA to form the key
     * @param linkStateID       link state id to form the key
     * @param advertisingRouter advertising router to form the key
     * @return LSA wrapper instance which contains the LSA
     * @throws Exception might throws exception
     */
    LsaWrapper getLsa(int lsType, String linkStateID, String advertisingRouter) throws Exception;

}