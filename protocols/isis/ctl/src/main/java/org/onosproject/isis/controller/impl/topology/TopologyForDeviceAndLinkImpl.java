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
package org.onosproject.isis.controller.impl.topology;

import org.onlab.util.Bandwidth;
import org.onosproject.isis.controller.topology.DeviceInformation;
import org.onosproject.isis.controller.topology.IsisRouter;
import org.onosproject.isis.controller.topology.LinkInformation;
import org.onosproject.isis.controller.topology.TopologyForDeviceAndLink;
import org.onosproject.isis.controller.topology.IsisLinkTed;
import org.onosproject.isis.io.isispacket.pdu.LsPdu;
import org.onosproject.isis.io.isispacket.tlv.IsExtendedReachability;
import org.onosproject.isis.io.isispacket.tlv.IsisTlv;
import org.onosproject.isis.io.isispacket.tlv.NeighborForExtendedIs;

import org.onosproject.isis.io.isispacket.tlv.subtlv.TrafficEngineeringSubTlv;
import org.onosproject.isis.io.isispacket.tlv.subtlv.InterfaceIpAddress;
import org.onosproject.isis.io.isispacket.tlv.subtlv.NeighborIpAddress;
import org.onosproject.isis.io.isispacket.tlv.subtlv.AdministrativeGroup;
import org.onosproject.isis.io.isispacket.tlv.subtlv.TrafficEngineeringMetric;
import org.onosproject.isis.io.isispacket.tlv.subtlv.UnreservedBandwidth;
import org.onosproject.isis.io.isispacket.tlv.subtlv.MaximumReservableBandwidth;
import org.onosproject.isis.io.isispacket.tlv.subtlv.MaximumBandwidth;
import org.onosproject.isis.io.util.IsisConstants;
import org.onosproject.isis.io.util.IsisUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents device and link topology information.
 */
public class TopologyForDeviceAndLinkImpl implements TopologyForDeviceAndLink {

    private static final Logger log = LoggerFactory.getLogger(TopologyForDeviceAndLinkImpl.class);
    private Map<String, DeviceInformation> deviceInformationMap = new LinkedHashMap<>();
    private Map<String, IsisRouter> isisRouterDetails = new LinkedHashMap<>();
    private Map<String, DeviceInformation> deviceInformationMapForPointToPoint = new LinkedHashMap<>();
    private Map<String, DeviceInformation> deviceInformationMapToDelete = new LinkedHashMap<>();
    private Map<String, LinkInformation> addedLinkInformationMap = new LinkedHashMap<>();

    /**
     * Gets device information.
     *
     * @return device information
     */
    public Map<String, DeviceInformation> deviceInformationMap() {
        return deviceInformationMap;
    }

    /**
     * Gets ISIS router list information.
     *
     * @return router information
     */
    public Map<String, IsisRouter> isisDeviceList() {
        return isisRouterDetails;
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
     * @param key                  key used to add in map
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
     * @param key system id as key to store in map
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

    @Override
    public void removeLinks(String linkId) {
        this.addedLinkInformationMap.remove(linkId);
    }

    /**
     * Gets link information as map.
     *
     * @return link information as map
     */
    public Map<String, LinkInformation> linkInformationMap() {
        return addedLinkInformationMap;
    }

    private LinkInformation getLinkInformation(String key) {
        LinkInformation linkInformation = this.addedLinkInformationMap.get(key);
        return linkInformation;
    }

    /**
     * Sets link information in map.
     *
     * @param key                key used to add in map
     * @param linkInformationMap link information instance
     */
    public void setLinkInformationMap(String key, LinkInformation linkInformationMap) {
        if (!this.addedLinkInformationMap.containsKey(key)) {
            this.addedLinkInformationMap.put(key, linkInformationMap);
        }
    }

    /**
     * Gets linkInformation as map for PointToPoint.
     *
     * @return linkInformationMap
     */
    public Map<String, LinkInformation> linkInformationMapForPointToPoint() {
        return addedLinkInformationMap;
    }

    /**
     * Sets linkInformation as map for PointToPoint.
     *
     * @param key                key used to add in map
     * @param linkInformationMap link information instance
     */
    public void setLinkInformationMapForPointToPoint(String key, LinkInformation linkInformationMap) {
        if (!this.addedLinkInformationMap.containsKey(key)) {
            this.addedLinkInformationMap.put(key, linkInformationMap);
        }
    }

    /**
     * Removes Link Information from linkInformationMap.
     *
     * @param key key used to remove in map
     */
    public void removeLinkInformationMap(String key) {
        if (this.addedLinkInformationMap.containsKey(key)) {
            this.addedLinkInformationMap.remove(key);
        }
    }

    /**
     * Returns the ISIS router instance.
     *
     * @param systemId system ID to get router details
     * @return ISIS router instance
     */
    public IsisRouter isisRouter(String systemId) {
        String routerId = IsisUtil.removeTailingZeros(systemId);
        IsisRouter isisRouter = isisRouterDetails.get(routerId);
        if (isisRouter != null) {
            return isisRouter;
        } else {
            log.debug("IsisRouter is not available");
            IsisRouter isisRouterCheck = new DefaultIsisRouter();
            isisRouterCheck.setSystemId(routerId);
            return isisRouterCheck;
        }
    }

    /**
     * Removes the ISIS router instance from map.
     *
     * @param systemId system ID to remove router details
     */
    public void removeRouter(String systemId) {
        String routerId = IsisUtil.removeTailingZeros(systemId);
        isisRouterDetails.remove(systemId);
    }

    /**
     * Creates Device instance.
     *
     * @param lsPdu ISIS LSPDU instance
     * @return isisRouter isisRouter instance
     */
    public IsisRouter createDeviceInfo(LsPdu lsPdu) {
        IsisRouter isisRouter = createIsisRouter(lsPdu);

        if (isisRouter.systemId() != null) {
            if (isisRouter.interfaceId() == null && isisRouter.neighborRouterId() == null) {
                isisRouter.setInterfaceId(IsisConstants.DEFAULTIP);
                isisRouter.setNeighborRouterId(IsisConstants.DEFAULTIP);
                isisRouterDetails.put(isisRouter.systemId(), isisRouter);
            }
        }
        return isisRouter;
    }

    /**
     * Removes Device and Link instance.
     *
     * @param lsPdu ISIS LSPDU instance
     * @return isisRouter isisRouter instance
     */
    /*
    public IsisRouter removeDeviceAndLinkInfo(LsPdu lsPdu) {
        IsisRouter isisRouter = createIsisRouter(lsPdu);
        return isisRouter;
    }*/

    /**
     * Creates link information.
     *
     * @param lsPdu       ls pdu instance
     * @param ownSystemId system ID
     * @return link information
     */
    public Map<String, LinkInformation> createLinkInfo(LsPdu lsPdu, String ownSystemId) {
        for (IsisTlv isisTlv : lsPdu.tlvs()) {
            if (isisTlv instanceof IsExtendedReachability) {
                IsExtendedReachability isExtendedReachability = (IsExtendedReachability) isisTlv;
                List<NeighborForExtendedIs> neighborForExtendedIsList = isExtendedReachability.neighbours();
                for (NeighborForExtendedIs neighbor : neighborForExtendedIsList) {
                    String neighbourId = neighbor.neighborId();
                    String routerId = IsisUtil.removeTailingZeros(lsPdu.lspId());
                    if (!(neighbourId.equals(ownSystemId))) {
                        IsisRouter isisRouter = isisRouterDetails.get(neighbourId);
                        if (isisRouter != null) {
                            String linkId = "link:" + routerId + "-" + neighbourId;
                            addedLinkInformationMap.put(linkId, createLinkInformation(lsPdu, linkId,
                                    routerId, neighbourId));
                        } else {
                            createIsisRouterDummy(neighbourId);
                            String linkId = "link:" + routerId + "-" + neighbourId;
                            LinkInformation linkInformation = createLinkInformation(lsPdu, linkId,
                                    routerId, neighbourId);
                            linkInformation.setAlreadyCreated(true);
                            addedLinkInformationMap.put(linkId, linkInformation);
                        }
                    }

                }
            }
        }
        return addedLinkInformationMap;
    }

    /**
     * Removes link information.
     *
     * @param systemId system ID to remove link information
     * @return updated link information
     */
    public Map<String, LinkInformation> removeLinkInfo(String systemId) {
        String routerId = IsisUtil.removeTailingZeros(systemId);
        Map<String, LinkInformation> removeLinkInformationMap = new LinkedHashMap<>();
        for (String key : addedLinkInformationMap.keySet()) {
            if (key.contains(routerId)) {
                removeLinkInformationMap.put(key, addedLinkInformationMap.get(key));
            }
        }
        return removeLinkInformationMap;
    }

    /**
     * Creates link information.
     *
     * @param lsPdu       link state pdu
     * @param linkId      link id
     * @param localRouter local router system id
     * @param neighborId  destination router system id
     * @return linkInformation instance
     */
    private LinkInformation createLinkInformation(LsPdu lsPdu, String linkId, String localRouter, String neighborId) {
        LinkInformation linkInformation = new DefaultIsisLinkInformation();
        IsisRouter isisRouter = isisRouterDetails.get(neighborId);
        for (IsisTlv isisTlv : lsPdu.tlvs()) {
            if (isisTlv instanceof IsExtendedReachability) {
                IsExtendedReachability isExtendedReachability = (IsExtendedReachability) isisTlv;
                List<NeighborForExtendedIs> neighbours = isExtendedReachability.neighbours();
                for (NeighborForExtendedIs teTlv : neighbours) {
                    List<TrafficEngineeringSubTlv> teSubTlvs = teTlv.teSubTlv();
                    for (TrafficEngineeringSubTlv teSubTlv : teSubTlvs) {
                        if (teSubTlv instanceof InterfaceIpAddress) {
                            InterfaceIpAddress localIpAddress = (InterfaceIpAddress) teSubTlv;
                            linkInformation.setInterfaceIp(localIpAddress.localInterfaceIPAddress());
                        } else if (teSubTlv instanceof NeighborIpAddress) {
                            NeighborIpAddress neighborIpAddress = (NeighborIpAddress) teSubTlv;
                            linkInformation.setNeighborIp(neighborIpAddress.neighborIPAddress());
                        }

                    }
                }
            }
        }
        linkInformation.setLinkId(linkId);
        linkInformation.setAlreadyCreated(false);
        linkInformation.setLinkDestinationId(neighborId);
        linkInformation.setLinkSourceId(localRouter);
        return linkInformation;
    }

    /**
     * Creates ISIS router instance.
     *
     * @param lsPdu lsp instance
     * @return isisRouter instance
     */
    private IsisRouter createIsisRouter(LsPdu lsPdu) {
        IsisRouter isisRouter = new DefaultIsisRouter();
        if (IsisUtil.checkIsDis(lsPdu.lspId())) {
            isisRouter.setDis(true);
        } else {
            isisRouter.setDis(false);
        }
        isisRouter.setSystemId(IsisUtil.removeTailingZeros(lsPdu.lspId()));
        for (IsisTlv isisTlv : lsPdu.tlvs()) {
            if (isisTlv instanceof IsExtendedReachability) {
                IsExtendedReachability isExtendedReachability = (IsExtendedReachability) isisTlv;
                List<NeighborForExtendedIs> neighbours = isExtendedReachability.neighbours();
                for (NeighborForExtendedIs teTlv : neighbours) {
                    List<TrafficEngineeringSubTlv> teSubTlvs = teTlv.teSubTlv();
                    for (TrafficEngineeringSubTlv teSubTlv : teSubTlvs) {
                        if (teSubTlv instanceof InterfaceIpAddress) {
                            InterfaceIpAddress localIpAddress = (InterfaceIpAddress) teSubTlv;
                            isisRouter.setInterfaceId(localIpAddress.localInterfaceIPAddress());
                        } else if (teSubTlv instanceof NeighborIpAddress) {
                            NeighborIpAddress neighborIpAddress = (NeighborIpAddress) teSubTlv;
                            isisRouter.setNeighborRouterId(neighborIpAddress.neighborIPAddress());
                        }

                    }
                }
            }
        }
        return isisRouter;
    }

    /**
     * Creates ISIS router instance.
     *
     * @param systemId system ID
     * @return isisRouter instance
     */
    private IsisRouter createIsisRouterDummy(String systemId) {
        IsisRouter isisRouter = new DefaultIsisRouter();
        isisRouter.setSystemId(systemId);
        isisRouter.setDis(false);
        isisRouter.setInterfaceId(IsisConstants.DEFAULTIP);
        isisRouter.setNeighborRouterId(IsisConstants.DEFAULTIP);

        return isisRouter;
    }

    /**
     * Creates the ISIS link TED information.
     *
     * @param lsPdu link state PDU
     * @return isisLinkTed
     */
    public IsisLinkTed createIsisLinkTedInfo(LsPdu lsPdu) {
        IsisLinkTed isisLinkTed = new DefaultIsisLinkTed();
        for (IsisTlv isisTlv : lsPdu.tlvs()) {
            if (isisTlv instanceof IsExtendedReachability) {
                IsExtendedReachability isExtendedReachability = (IsExtendedReachability) isisTlv;
                List<NeighborForExtendedIs> neighbours = isExtendedReachability.neighbours();
                for (NeighborForExtendedIs teTlv : neighbours) {
                    List<TrafficEngineeringSubTlv> teSubTlvs = teTlv.teSubTlv();
                    for (TrafficEngineeringSubTlv teSubTlv : teSubTlvs) {
                        if (teSubTlv instanceof AdministrativeGroup) {
                            AdministrativeGroup ag = (AdministrativeGroup) teSubTlv;
                            isisLinkTed.setAdministrativeGroup(ag.administrativeGroup());
                        }
                        if (teSubTlv instanceof InterfaceIpAddress) {
                            InterfaceIpAddress localIpAddress = (InterfaceIpAddress) teSubTlv;
                            isisLinkTed.setIpv4InterfaceAddress(localIpAddress.localInterfaceIPAddress());
                        }
                        if (teSubTlv instanceof NeighborIpAddress) {
                            NeighborIpAddress neighborIpAddress = (NeighborIpAddress) teSubTlv;
                            isisLinkTed.setIpv4NeighborAddress(neighborIpAddress.neighborIPAddress());
                        }
                        if (teSubTlv instanceof TrafficEngineeringMetric) {
                            TrafficEngineeringMetric teM = (TrafficEngineeringMetric) teSubTlv;
                            isisLinkTed.setTeDefaultMetric(teM.getTrafficEngineeringMetricValue());
                        }
                        if (teSubTlv instanceof MaximumBandwidth) {
                            MaximumBandwidth maxLinkBandwidth = (MaximumBandwidth) teSubTlv;
                            isisLinkTed.setMaximumLinkBandwidth(
                                    Bandwidth.bps(maxLinkBandwidth.getMaximumBandwidthValue()));
                        }
                        if (teSubTlv instanceof MaximumReservableBandwidth) {
                            MaximumReservableBandwidth maxReservableBw = (MaximumReservableBandwidth) teSubTlv;
                            isisLinkTed.setMaximumReservableLinkBandwidth(
                                    Bandwidth.bps(maxReservableBw.getMaximumBandwidthValue()));
                        }
                        if (teSubTlv instanceof UnreservedBandwidth) {
                            UnreservedBandwidth unReservedBandwidth = (UnreservedBandwidth) teSubTlv;
                            List<Bandwidth> bandwidthList = new ArrayList<>();
                            List<Float> unReservedBandwidthList = unReservedBandwidth.unReservedBandwidthValue();
                            for (Float unReservedBandwidthFloatValue : unReservedBandwidthList) {
                                Bandwidth bandwidth = Bandwidth.bps(unReservedBandwidthFloatValue);
                                bandwidthList.add(bandwidth);
                            }
                            isisLinkTed.setUnreservedBandwidth(bandwidthList);
                        }
                    }
                }
            }
        }
        return isisLinkTed;
    }
}
