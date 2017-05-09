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
package org.onosproject.ospf.controller.impl;

import org.onlab.packet.Ip4Address;
import org.onlab.util.Bandwidth;
import org.onosproject.ospf.controller.DeviceInformation;
import org.onosproject.ospf.controller.LinkInformation;
import org.onosproject.ospf.controller.OspfArea;
import org.onosproject.ospf.controller.OspfInterface;
import org.onosproject.ospf.controller.OspfLinkTed;
import org.onosproject.ospf.controller.OspfLsa;
import org.onosproject.ospf.controller.OspfLsaType;
import org.onosproject.ospf.controller.TopologyForDeviceAndLink;
import org.onosproject.ospf.protocol.lsa.linksubtype.LinkSubType;
import org.onosproject.ospf.protocol.lsa.linksubtype.LocalInterfaceIpAddress;
import org.onosproject.ospf.protocol.lsa.linksubtype.MaximumBandwidth;
import org.onosproject.ospf.protocol.lsa.linksubtype.MaximumReservableBandwidth;
import org.onosproject.ospf.protocol.lsa.linksubtype.RemoteInterfaceIpAddress;
import org.onosproject.ospf.protocol.lsa.linksubtype.TrafficEngineeringMetric;
import org.onosproject.ospf.protocol.lsa.linksubtype.UnreservedBandwidth;
import org.onosproject.ospf.protocol.lsa.subtypes.OspfLsaLink;
import org.onosproject.ospf.protocol.lsa.tlvtypes.LinkTlv;
import org.onosproject.ospf.protocol.lsa.types.NetworkLsa;
import org.onosproject.ospf.protocol.lsa.types.OpaqueLsa10;
import org.onosproject.ospf.protocol.lsa.types.RouterLsa;
import org.onosproject.ospf.protocol.lsa.types.TopLevelTlv;
import org.onosproject.ospf.protocol.util.OspfUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Represents device and link topology information.
 */
public class TopologyForDeviceAndLinkImpl implements TopologyForDeviceAndLink {

    private static final Logger log = LoggerFactory.getLogger(TopologyForDeviceAndLinkImpl.class);
    Set<Ip4Address> routerSet = new TreeSet<Ip4Address>();
    Ip4Address firstValue = Ip4Address.valueOf("0.0.0.0");
    private Map<String, DeviceInformation> deviceInformationMap = new LinkedHashMap();
    private Map<Ip4Address, List<Ip4Address>> networkLsaMap = new LinkedHashMap();
    private Map<String, DeviceInformation> deviceInformationMapForPointToPoint = new LinkedHashMap();
    private Map<String, DeviceInformation> deviceInformationMapToDelete = new LinkedHashMap();
    private HashMap<String, Set<OspfLsaLink>> deviceAndLinkInformation = new HashMap();
    private HashMap<String, OspfLinkTed> ospfLinkTedHashMap = new LinkedHashMap();
    private Ip4Address drRouter = Ip4Address.valueOf("0.0.0.0");
    private Ip4Address drRouterOld = Ip4Address.valueOf("0.0.0.0");
    private Ip4Address adRouterId = Ip4Address.valueOf("0.0.0.0");
    private Map<String, LinkInformation> linkInformationMap = new LinkedHashMap();
    private Map<String, LinkInformation> linkInformationMapForPointToPoint = new LinkedHashMap();
    private List<String> toRemove = new ArrayList<>();

    /**
     * Gets device information.
     *
     * @return device information
     */
    public Map<String, DeviceInformation> deviceInformationMap() {
        return deviceInformationMap;
    }

    /**
     * Sets device information.
     *
     * @param key                  key used to add in map
     * @param deviceInformationMap device information instance
     */
    public void setDeviceInformationMap(String key, DeviceInformation deviceInformationMap) {
        if (deviceInformationMap != null) {
            this.deviceInformationMap.put(key, deviceInformationMap);
        }

    }

    /**
     * Gets deviceInformation as map for Point-To-Point.
     *
     * @return deviceInformationMap
     */
    public Map<String, DeviceInformation> deviceInformationMapForPointToPoint() {
        return deviceInformationMapForPointToPoint;
    }

    /**
     * Sets deviceInformation as map for Point-To-Point..
     *
     * @param key                  key to store device information
     * @param deviceInformationMap device information instance
     */
    public void setDeviceInformationMapForPointToPoint(String key, DeviceInformation deviceInformationMap) {
        if (deviceInformationMap != null) {
            this.deviceInformationMapForPointToPoint.put(key, deviceInformationMap);
        }

    }

    /**
     * Gets deviceInformation as map.
     *
     * @return deviceInformationMap to delete from core
     */
    public Map<String, DeviceInformation> deviceInformationMapToDelete() {
        return deviceInformationMapToDelete;
    }

    /**
     * Sets device information for removal.
     *
     * @param key                          ket used to add in map
     * @param deviceInformationMapToDelete map from device information to remove
     */
    public void setDeviceInformationMapToDelete(String key, DeviceInformation deviceInformationMapToDelete) {
        if (deviceInformationMapToDelete != null) {
            this.deviceInformationMapToDelete.put(key, deviceInformationMapToDelete);
        }
    }

    /**
     * Removes Device Information.
     *
     * @param key ket used to remove from map
     */
    public void removeDeviceInformationMapFromDeleteMap(String key) {
        removeDeviceInformationMap(key);
        if (this.deviceInformationMapToDelete.containsKey(key)) {
            this.deviceInformationMapToDelete.remove(key);
        }
    }

    /**
     * Gets Device Information.
     *
     * @param key key to store in map
     * @return Device Information
     */
    public DeviceInformation deviceInformation(String key) {
        DeviceInformation deviceInformation = this.deviceInformationMap.get(key);
        return deviceInformation;
    }

    /**
     * Removes Device Information from map.
     *
     * @param key key used to remove from map
     */
    public void removeDeviceInformationMap(String key) {
        if (this.deviceInformationMap.containsKey(key)) {
            this.deviceInformationMap.remove(key);
        }
    }

    /**
     * Gets link information as map.
     *
     * @return link information as map
     */
    public Map<String, LinkInformation> linkInformationMap() {
        return linkInformationMap;
    }

    private LinkInformation getLinkInformation(String key) {
        LinkInformation linkInformation = this.linkInformationMap.get(key);
        return linkInformation;
    }

    /**
     * Sets link information in map.
     *
     * @param key                key used to add in map
     * @param linkInformationMap link information instance
     */
    public void setLinkInformationMap(String key, LinkInformation linkInformationMap) {
        if (!this.linkInformationMap.containsKey(key)) {
            this.linkInformationMap.put(key, linkInformationMap);
        }
    }

    /**
     * Gets linkInformation as map for PointToPoint.
     *
     * @return linkInformationMap
     */
    public Map<String, LinkInformation> linkInformationMapForPointToPoint() {
        return linkInformationMap;
    }

    /**
     * Sets linkInformation as map for PointToPoint.
     *
     * @param key                key to store link information
     * @param linkInformationMap link information instance
     */
    public void setLinkInformationMapForPointToPoint(String key, LinkInformation linkInformationMap) {
        if (!this.linkInformationMap.containsKey(key)) {
            this.linkInformationMap.put(key, linkInformationMap);
        }
    }

    /**
     * Removes Link Information from linkInformationMap.
     *
     * @param key key to remove link information
     */
    public void removeLinkInformationMap(String key) {
        if (this.linkInformationMap.containsKey(key)) {
            this.linkInformationMap.remove(key);
        }
    }


    /**
     * Gets OSPF Link TED details from the map.
     *
     * @param key key used to retreive from map
     * @return OSPF link ted instance
     */
    public OspfLinkTed getOspfLinkTedHashMap(String key) {
        OspfLinkTed ospfLinkTed = ospfLinkTedHashMap.get(key);
        return ospfLinkTed;
    }

    /**
     * Adds device information to map.
     *
     * @param ospfLsa       OSPF LSA instance
     * @param ospfInterface OSPF interface instance
     * @param ospfArea      OSPF area instance
     */
    public void addLocalDevice(OspfLsa ospfLsa, OspfInterface ospfInterface, OspfArea ospfArea) {
        if (ospfLsa.getOspfLsaType().equals(OspfLsaType.ROUTER)) {
            createDeviceAndLinkFromRouterLsa(ospfLsa, ospfArea);
        } else if (ospfLsa.getOspfLsaType().equals(OspfLsaType.NETWORK)) {
            createDeviceAndLinkFromNetworkLsa(ospfLsa, ospfArea);
        } else if (ospfLsa.getOspfLsaType().equals(OspfLsaType.AREA_LOCAL_OPAQUE_LSA)) {
            createDeviceAndLinkFromOpaqueLsa(ospfLsa, ospfArea);
        }
    }

    /**
     * Creates device object from parameters.
     *
     * @param alreadyCreated device already created or not
     * @param deviceId       device id
     * @param neighborId     neighbor's id
     * @param routerId       router's id
     * @param interfaceId    interface id
     * @param areaId         area id
     * @param isDr           true if router is DR else false
     */
    private DeviceInformation createDeviceInformation(boolean alreadyCreated, Ip4Address deviceId,
                                                      Ip4Address neighborId, Ip4Address routerId,
                                                      Ip4Address interfaceId, Ip4Address areaId,
                                                      boolean isDr) {
        DeviceInformation deviceInformation = new DeviceInformationImpl();
        deviceInformation.setAlreadyCreated(alreadyCreated);
        deviceInformation.setDeviceId(deviceId);
        deviceInformation.setNeighborId(neighborId);
        deviceInformation.setRouterId(routerId);
        deviceInformation.addInterfaceId(interfaceId);
        deviceInformation.setAreaId(areaId);
        deviceInformation.setDr(isDr);
        return deviceInformation;
    }

    /**
     * Creates Device and Link instance from the RouterLsa parameters.
     *
     * @param ospfLsa  OSPF LSA instance
     * @param ospfArea OSPF area
     */
    private void createDeviceAndLinkFromRouterLsa(OspfLsa ospfLsa, OspfArea ospfArea) {
        RouterLsa routerLsa = (RouterLsa) ospfLsa;
        List<OspfLsaLink> ospfLsaLinkList = routerLsa.routerLink();
        Iterator iterator = ospfLsaLinkList.iterator();
        Ip4Address advertisingRouterId = routerLsa.advertisingRouter();
        while (iterator.hasNext()) {
            OspfLsaLink ospfLsaLink = (OspfLsaLink) iterator.next();
            Ip4Address linkId = Ip4Address.valueOf(ospfLsaLink.linkId());
            Ip4Address linkData = Ip4Address.valueOf(ospfLsaLink.linkData());
            if (ospfLsaLink.linkType() == 1) {
                if ((advertisingRouterId.equals(ospfArea.routerId())) || (linkId.equals(ospfArea.routerId()))) {
                    if (!advertisingRouterId.equals(ospfArea.routerId())) {
                        DeviceInformation deviceInformationPointToPoint =
                                createDeviceInformation(false, linkId, linkId, advertisingRouterId, linkData,
                                                        ospfArea.areaId(), false);
                        String key = "device:" + advertisingRouterId;
                        setDeviceInformationMapForPointToPoint(key, deviceInformationPointToPoint);
                    }
                } else {
                    DeviceInformation deviceInformationPointToPoint =
                            createDeviceInformation(false, linkId, linkId, advertisingRouterId,
                                                    linkData, ospfArea.areaId(), false);
                    String key = "device:" + advertisingRouterId;
                    setDeviceInformationMapForPointToPoint(key, deviceInformationPointToPoint);
                    String linkIdKey = "linkId:" + advertisingRouterId + "-" + linkId;
                    addLocalLinkForPointToPoint(linkIdKey, linkData, advertisingRouterId, linkId, true, false);
                }
            }
        }
    }

    /**
     * Creates device and link instance from the network LSA parameters.
     *
     * @param ospfLsa  OSPF LSA instance
     * @param ospfArea OSPF area instance
     */
    private void createDeviceAndLinkFromNetworkLsa(OspfLsa ospfLsa, OspfArea ospfArea) {
        NetworkLsa networkLsa = (NetworkLsa) ospfLsa;
        Ip4Address linkStateId = Ip4Address.valueOf(networkLsa.linkStateId());
        Set<Ip4Address> drList = networkLsaMap.keySet();
        try {
            Ip4Address drToReplace = null;
            for (Ip4Address drIp : drList) {
                if (!drIp.equals(linkStateId)) {
                    if (OspfUtil.sameNetwork(drIp, linkStateId, networkLsa.networkMask())) {
                        drToReplace = drIp;
                        String key = "device:" + drToReplace;
                        DeviceInformation deleteDr = deviceInformation(key);
                        if (deleteDr != null) {
                            deleteDr.setAlreadyCreated(true);
                            setDeviceInformationMapToDelete(key, deleteDr);
                        }

                        networkLsaMap.remove(drToReplace);
                        break;
                    }
                }
            }
            networkLsaMap.put(linkStateId, networkLsa.attachedRouters());

        } catch (Exception e) {
            log.debug("Error::TopologyForDeviceAndLinkImpl:: {}", e.getMessage());
        }
        constructDeviceForBroadCastTopology(ospfArea);
        disp();

    }

    private void constructDeviceForBroadCastTopology(OspfArea ospfArea) {

        for (Map.Entry<Ip4Address, List<Ip4Address>> entry : networkLsaMap.entrySet()) {
            Ip4Address key = entry.getKey();
            DeviceInformation deviceInformationForDr = createDeviceInformation(false, key, key, key,
                                                                               key, ospfArea.areaId(), true);
            String dr = "device:" + key;
            setDeviceInformationMap(dr, deviceInformationForDr);
            List<Ip4Address> value = entry.getValue();
            for (Ip4Address connectedRouter : value) {
                if (!connectedRouter.equals(ospfArea.routerId())) {
                    DeviceInformation deviceInformationAttachedRouters =
                            createDeviceInformation(false, connectedRouter, key, connectedRouter,
                                                    key, ospfArea.areaId(), false);
                    String attachedRouters = "device:" + connectedRouter;
                    setDeviceInformationMap(attachedRouters, deviceInformationAttachedRouters);
                    String linkIdKey = "linkId:" + key + "-" + connectedRouter;
                    addLocalLink(linkIdKey, key, key, connectedRouter, true, false);
                }
            }
        }

    }

    private void disp() {
        for (String key : deviceInformationMap.keySet()) {
            DeviceInformation deviceInformation = deviceInformationMap.get(key);
            log.debug("************************************************************************");
            log.debug("DeviceInfoList RouterId is : {} and neighbour is  {} and linkdata {}",
                      deviceInformation.routerId(), deviceInformation.neighborId(), deviceInformation.interfaceId());
        }

        for (Map.Entry<String, LinkInformation> entry : linkInformationMap.entrySet()) {
            String linkDetail = entry.getKey();
            log.debug("Link From and to is  " + linkDetail);
        }
        log.debug("Devices Needs to delete from Core are  : " + deviceInformationMapToDelete.size());
        for (String key : deviceInformationMapToDelete.keySet()) {
            DeviceInformation value = deviceInformationMapToDelete.get(key);
            if (value.isAlreadyCreated()) {
                log.debug("Device is deleted from list " + value.routerId());
            }
        }
    }

    private void getLinksToDelete(Set<Ip4Address> list, Ip4Address value, OspfArea ospfArea) {

        Iterator iterator = list.iterator();

        while (iterator.hasNext()) {
            Ip4Address secondValue = (Ip4Address) iterator.next();
            if (!value.toString().equals("0.0.0.0")) {
                if ((!value.equals(secondValue))) {
                    if ((!secondValue.equals(ospfArea.routerId()))) {
                        String key = "link:" + value.toString() + "-" + secondValue.toString();
                        String key1 = "link:" + secondValue.toString() + "-" + value.toString();
                        LinkInformation linkDetails = getLinkInformation(key);
                        LinkInformation linkDetailsOther = getLinkInformation(key1);
                        linkInformationMapForPointToPoint.put(key, linkDetails);
                        linkInformationMapForPointToPoint.put(key1, linkDetailsOther);
                    }
                }
            }

        }

    }

    /**
     * Creates Device and Link instance from the OpaqueLsa parameters.
     *
     * @param ospfLsa  OSPF LSA instance
     * @param ospfArea OSPF area instance
     */
    private void createDeviceAndLinkFromOpaqueLsa(OspfLsa ospfLsa, OspfArea ospfArea) {
        OspfLinkTed ospfLinkTed = new OspfLinkTedImpl();
        OpaqueLsa10 opaqueLsa10 = (OpaqueLsa10) ospfLsa;
        List<TopLevelTlv> topLevelTlvList = opaqueLsa10.topLevelValues();
        for (TopLevelTlv topLevelTlv : topLevelTlvList) {
            if (topLevelTlv instanceof LinkTlv) {
                LinkTlv linkTlv = (LinkTlv) topLevelTlv;
                List<LinkSubType> subTypes = linkTlv.subTlvList();
                for (LinkSubType type : subTypes) {
                    if (type instanceof UnreservedBandwidth) {
                        UnreservedBandwidth unreservedBandwidth = (UnreservedBandwidth) type;
                        List<Float> bandwidthFloatValues = unreservedBandwidth.getUnReservedBandwidthValue();
                        List<Bandwidth> bandwidthList = new ArrayList<>();
                        for (Float value : bandwidthFloatValues) {
                            Bandwidth bandwidth = Bandwidth.bps((double) value);
                            ospfLinkTed.setMaxUnResBandwidth(bandwidth);
                            bandwidthList.add(bandwidth);
                        }
                    }
                    if (type instanceof MaximumBandwidth) {
                        MaximumBandwidth maximumBandwidth = (MaximumBandwidth) type;
                        float maxBandValue = maximumBandwidth.getMaximumBandwidthValue();
                        Bandwidth bandwidth = Bandwidth.bps((double) maxBandValue);
                        ospfLinkTed.setMaximumLink(bandwidth);
                    }
                    if (type instanceof MaximumReservableBandwidth) {
                        MaximumReservableBandwidth maximumReservableBandwidth = (MaximumReservableBandwidth) type;
                        float maxResBandValue = maximumReservableBandwidth.getMaximumBandwidthValue();
                        Bandwidth bandwidth = Bandwidth.bps((double) maxResBandValue);
                        ospfLinkTed.setMaxReserved(bandwidth);
                    }
                    if (type instanceof TrafficEngineeringMetric) {
                        TrafficEngineeringMetric trafficEngineeringMetric = (TrafficEngineeringMetric) type;
                        long teMetric = trafficEngineeringMetric.getTrafficEngineeringMetricValue();
                        ospfLinkTed.setTeMetric((Integer) (int) teMetric);
                    }
                    if (type instanceof LocalInterfaceIpAddress) {
                        LocalInterfaceIpAddress localInterfaceIpAddress = (LocalInterfaceIpAddress) type;
                        List<String> stringValue = localInterfaceIpAddress.getLocalInterfaceIPAddress();
                        List<Ip4Address> localIp4Address = new ArrayList<>();
                        for (String value : stringValue) {
                            Ip4Address ip4Address = Ip4Address.valueOf(value);
                            localIp4Address.add(ip4Address);
                        }
                        ospfLinkTed.setIpv4LocRouterId(localIp4Address);
                    }
                    if (type instanceof RemoteInterfaceIpAddress) {
                        RemoteInterfaceIpAddress remoteInterfaceIpAddress = (RemoteInterfaceIpAddress) type;
                        List<String> stringValue = remoteInterfaceIpAddress.getRemoteInterfaceAddress();
                        List<Ip4Address> remoteIp4Address = new ArrayList<>();
                        for (String value : stringValue) {
                            Ip4Address ip4Address = Ip4Address.valueOf(value);
                            remoteIp4Address.add(ip4Address);
                        }
                        ospfLinkTed.setIpv4RemRouterId(remoteIp4Address);
                    }
                }
            }

        }
        ospfLinkTedHashMap.put(adRouterId.toString(), ospfLinkTed);
    }


    /**
     * Adds link information to LinkInformationMap.
     *
     * @param advertisingRouter    advertising router
     * @param linkData             link data address
     * @param linkSrc              link source address
     * @param linkDest             link destination address
     * @param opaqueEnabled        opaque enabled or not
     * @param linkSrcIdNotRouterId link source id or not
     */
    public void addLocalLink(String advertisingRouter, Ip4Address linkData, Ip4Address linkSrc, Ip4Address linkDest,
                             boolean opaqueEnabled, boolean linkSrcIdNotRouterId) {
        String linkKey = "link:";
        LinkInformation linkInformation = new LinkInformationImpl();
        linkInformation.setLinkId(advertisingRouter);
        linkInformation.setLinkSourceId(linkSrc);
        linkInformation.setLinkDestinationId(linkDest);
        linkInformation.setAlreadyCreated(false);
        linkInformation.setLinkSrcIdNotRouterId(linkSrcIdNotRouterId);
        linkInformation.setInterfaceIp(linkData);
        if (linkDest != null) {
            linkInformation.setLinkSrcIdNotRouterId(false);
        }
        linkKey = linkKey + "-" + linkSrc + "-" + linkDest;
        setLinkInformationMap(linkKey, linkInformation);
    }

    /**
     * Adds link information to LinkInformationMap for PointToPoint.
     *
     * @param advertisingRouter    advertising router
     * @param linkData             link data
     * @param linkSrc              link source
     * @param linkDest             link destination
     * @param opaqueEnabled        whether opaque is enabled or not
     * @param linkSrcIdNotRouterId whether link is source id or router id
     */
    public void addLocalLinkForPointToPoint(String advertisingRouter, Ip4Address linkData, Ip4Address linkSrc,
                                            Ip4Address linkDest, boolean opaqueEnabled, boolean linkSrcIdNotRouterId) {
        String linkKey = "link:";
        LinkInformation linkInformation = new LinkInformationImpl();
        linkInformation.setLinkId(advertisingRouter);
        linkInformation.setLinkSourceId(linkSrc);
        linkInformation.setLinkDestinationId(linkDest);
        linkInformation.setAlreadyCreated(false);
        linkInformation.setLinkSrcIdNotRouterId(linkSrcIdNotRouterId);
        linkInformation.setInterfaceIp(linkData);
        if (linkDest != null) {
            linkInformation.setLinkSrcIdNotRouterId(false);
        }
        linkKey = linkKey + "-" + linkSrc + "-" + linkDest;
        setLinkInformationMapForPointToPoint(linkKey, linkInformation);
    }

    /**
     * Removes links from LinkInformationMap.
     *
     * @param routerId router id
     */
    public void removeLinks(Ip4Address routerId) {
        Map<String, LinkInformation> linkInformationMapLocal = linkInformationMap;
        if (linkInformationMapLocal != null) {
            for (Map.Entry<String, LinkInformation> entry : linkInformationMap.entrySet()) {
                String key = entry.getKey();
                boolean check = key.contains(routerId.toString());
                LinkInformation linkInformation = linkInformationMap.get(key);
                boolean check1 = (linkInformation.linkDestinationId() == routerId) ? true : false;
                if (check || check1) {
                    toRemove.add(key);
                }
            }
            removeLinkFromMap();
        }
    }

    /**
     * Removes Device from DeviceInformationMap.
     *
     * @param routerId router id
     */
    public void removeDevice(Ip4Address routerId) {
        String key = "device:" + routerId;
        this.deviceInformationMap.remove(key);
    }

    /**
     * Removes link information from Map.
     */
    private void removeLinkFromMap() {
        Iterator iterator = toRemove.iterator();
        while (iterator.hasNext()) {
            String key = (String) iterator.next();
            removeLinkInformationMap(key);
        }
    }

    /**
     * Updates the deviceAndLinkInformation list for received OSPF LSA.
     *
     * @param ospfLsa  OSPF LSA  instance
     * @param ospfArea OSPF area instance
     */
    public void updateLinkInformation(OspfLsa ospfLsa, OspfArea ospfArea) {
        if (ospfLsa.getOspfLsaType().equals(OspfLsaType.ROUTER)) {
            RouterLsa routerLsa = (RouterLsa) ospfLsa;
            routerLsa.lsType();
            List<OspfLsaLink> ospfLsaLinkList = routerLsa.routerLink();
            for (OspfLsaLink link : ospfLsaLinkList) {
                if (link.linkType() == 1 || link.linkType() == 2) {
                    if ((routerLsa.advertisingRouter().equals(ospfArea.routerId())) ||
                            (link.equals(ospfArea.routerId()))) {
                        log.debug("OspfInterface information will not display in web ");
                    } else {
                        String key = routerLsa.advertisingRouter() + "-" + link.linkData();
                        Set<OspfLsaLink> linkInformations = new HashSet<>();
                        if (deviceAndLinkInformation.containsKey(key)) {
                            linkInformations = deviceAndLinkInformation.get(key);
                            linkInformations.add(link);
                            deviceAndLinkInformation.put(key, linkInformations);
                        } else {
                            linkInformations.add(link);
                            deviceAndLinkInformation.put(key, linkInformations);
                        }
                    }
                }
            }
        }
    }

    /**
     * Gets all the router information which needs to delete from deviceList.
     *
     * @param ospfLsa  OSPF LSA instance
     * @param ospfArea OSPF area instance
     * @return list of deleted router information
     */
    public List<String> getDeleteRouterInformation(OspfLsa ospfLsa, OspfArea ospfArea) {
        List<String> removedLinkList = new ArrayList<>();
        if (ospfLsa.getOspfLsaType().equals(OspfLsaType.ROUTER)) {

            RouterLsa routerLsa = (RouterLsa) ospfLsa;
            List<OspfLsaLink> ospfLsaLinkList = routerLsa.routerLink();
            for (OspfLsaLink link : ospfLsaLinkList) {
                if (link.linkType() == 1 || link.linkType() == 2) {
                    if ((routerLsa.advertisingRouter().equals(ospfArea.routerId())) ||
                            (link.equals(ospfArea.routerId()))) {
                        log.debug("OspfInterface information will not display in web ");
                    } else {
                        String key = routerLsa.advertisingRouter() + "-" + link.linkData();
                        Set<OspfLsaLink> linkInformations = deviceAndLinkInformation.get(key);
                        if (linkInformations.contains(link)) {
                            linkInformations.remove(link);
                            deviceAndLinkInformation.put(key, linkInformations);
                        }
                    }
                }
                Set<String> keys = deviceAndLinkInformation.keySet();
                for (String key : keys) {
                    Set<OspfLsaLink> linkInformations = deviceAndLinkInformation.get(key);
                    for (OspfLsaLink link1 : linkInformations) {
                        String removedLink = link1.linkId();
                        removedLinkList.add(removedLink);
                    }
                }
            }
        }
        return removedLinkList;
    }
}
