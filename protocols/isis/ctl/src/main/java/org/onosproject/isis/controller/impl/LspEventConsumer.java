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
package org.onosproject.isis.controller.impl;

import org.onlab.packet.Ip4Address;
import org.onlab.util.Bandwidth;
import org.onosproject.isis.controller.IsisNetworkType;
import org.onosproject.isis.controller.LspWrapper;
import org.onosproject.isis.controller.impl.topology.DefaultIsisLink;
import org.onosproject.isis.controller.impl.topology.DefaultIsisLinkInformation;
import org.onosproject.isis.controller.impl.topology.DefaultIsisLinkTed;
import org.onosproject.isis.controller.impl.topology.DefaultIsisRouter;
import org.onosproject.isis.controller.impl.topology.TopologyForDeviceAndLinkImpl;
import org.onosproject.isis.controller.topology.IsisLink;
import org.onosproject.isis.controller.topology.IsisLinkTed;
import org.onosproject.isis.controller.topology.IsisRouter;
import org.onosproject.isis.controller.topology.LinkInformation;
import org.onosproject.isis.io.isispacket.pdu.LsPdu;
import org.onosproject.isis.io.isispacket.tlv.IsExtendedReachability;
import org.onosproject.isis.io.isispacket.tlv.IsisTlv;
import org.onosproject.isis.io.isispacket.tlv.NeighborForExtendedIs;
import org.onosproject.isis.io.isispacket.tlv.subtlv.AdministrativeGroup;
import org.onosproject.isis.io.isispacket.tlv.subtlv.InterfaceIpAddress;
import org.onosproject.isis.io.isispacket.tlv.subtlv.MaximumBandwidth;
import org.onosproject.isis.io.isispacket.tlv.subtlv.MaximumReservableBandwidth;
import org.onosproject.isis.io.isispacket.tlv.subtlv.NeighborIpAddress;
import org.onosproject.isis.io.isispacket.tlv.subtlv.TrafficEngineeringMetric;
import org.onosproject.isis.io.isispacket.tlv.subtlv.TrafficEngineeringSubTlv;
import org.onosproject.isis.io.isispacket.tlv.subtlv.UnreservedBandwidth;
import org.onosproject.isis.io.util.IsisConstants;
import org.onosproject.isis.io.util.IsisUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

/**
 * Representation of LSP event consumer.
 */
public class LspEventConsumer implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(LspEventConsumer.class);
    private BlockingQueue queue = null;
    private Controller controller = null;
    private TopologyForDeviceAndLinkImpl deviceAndLink = new TopologyForDeviceAndLinkImpl();
    private Map<String, IsisRouter> isisRouterDetails = new LinkedHashMap<>();

    /**
     * Creates an instance of this.
     *
     * @param queue      blocking queue instance
     * @param controller controller instance
     */
    public LspEventConsumer(BlockingQueue queue, Controller controller) {
        this.queue = queue;
        this.controller = controller;
    }

    @Override
    public void run() {
        try {
            while (true) {
                if (!queue.isEmpty()) {
                    LspWrapper wrapper = (LspWrapper) queue.take();
                    LsPdu lsPdu = (LsPdu) wrapper.lsPdu();
                    if (wrapper.lspProcessing().equals(IsisConstants.LSPREMOVED)) {
                        callTopologyToRemoveInfo(lsPdu);
                    } else if (wrapper.lspProcessing().equals(IsisConstants.LSPADDED)) {
                        callTopologyToSendInfo(lsPdu, wrapper.isisInterface().networkType(),
                                               wrapper.isisInterface().systemId() + ".00");
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Error::LspsForProvider::{}", e.getMessage());
        }
    }

    /**
     * Sends topology information to core.
     *
     * @param lsPdu           ls pdu instance
     * @param isisNetworkType ISIS network type
     * @param ownSystemId own system ID
     */
    private void callTopologyToSendInfo(LsPdu lsPdu, IsisNetworkType isisNetworkType,
                                        String ownSystemId) {
        if ((lsPdu.lspId().equals(ownSystemId + "-00"))) {
            return;
        }
        sendDeviceInfo(createDeviceInfo(lsPdu));

        for (IsisTlv isisTlv : lsPdu.tlvs()) {
            if (isisTlv instanceof IsExtendedReachability) {
                IsExtendedReachability isExtendedReachability = (IsExtendedReachability) isisTlv;
                List<NeighborForExtendedIs> neighbours = isExtendedReachability.neighbours();
                for (NeighborForExtendedIs teTlv : neighbours) {
                    String neighbor = teTlv.neighborId();
                    IsisRouter isisRouter = isisRouterDetails.get(neighbor);
                    if (isisRouter != null) {
                        IsisRouter sourceRouter = isisRouterDetails.get(IsisUtil.removeTailingZeros(lsPdu.lspId()));
                        IsisRouter destinationRouter = isisRouter;
                        if (sourceRouter.isDis()) {
                            LinkInformation linkInformation = createLinkInfo(sourceRouter.systemId(),
                                                                             destinationRouter.systemId(),
                                                                             sourceRouter.interfaceId(),
                                                                             destinationRouter.interfaceId(), lsPdu);
                            controller.addLinkDetails(createIsisLink(linkInformation, lsPdu));
                        } else if (destinationRouter.isDis()) {
                            LinkInformation linkInformation1 = createLinkInfo(destinationRouter.systemId(),
                                                                              sourceRouter.systemId(),
                                                                              destinationRouter.interfaceId(),
                                                                              sourceRouter.interfaceId(), lsPdu);
                            controller.addLinkDetails(createIsisLink(linkInformation1, lsPdu));
                        } else {
                            LinkInformation linkInformation = createLinkInfo(sourceRouter.systemId(),
                                                                             destinationRouter.systemId(),
                                                                             sourceRouter.interfaceId(),
                                                                             destinationRouter.interfaceId(), lsPdu);
                            controller.addLinkDetails(createIsisLink(linkInformation, lsPdu));
                            LinkInformation linkInformation1 = createLinkInfo(destinationRouter.systemId(),
                                                                              sourceRouter.systemId(),
                                                                              destinationRouter.interfaceId(),
                                                                              sourceRouter.interfaceId(), lsPdu);
                            controller.addLinkDetails(createIsisLink(linkInformation1, lsPdu));
                        }
                    }
                }
            }
        }
    }

    /**
     * Removes topology information from core.
     *
     * @param lsPdu ls pdu instance
     */
    private void callTopologyToRemoveInfo(LsPdu lsPdu) {
        String routerId = IsisUtil.removeTailingZeros(lsPdu.lspId());
        IsisRouter isisRouter = isisRouterDetails.get(routerId);
        removeDeviceInfo(isisRouter);
        removeLinkInfo(lsPdu);
    }

    /**
     * Sends the device information to topology provider.
     *
     * @param isisRouter ISIS router instance
     */
    private void sendDeviceInfo(IsisRouter isisRouter) {
        if (isisRouter.systemId() != null) {
            controller.addDeviceDetails(isisRouter);
        }
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
            isisRouterDetails.put(isisRouter.systemId(), isisRouter);
        }
        return isisRouter;
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
        if (isisRouter.interfaceId() == null) {
            isisRouter.setInterfaceId(IsisConstants.DEFAULTIP);
        }
        if (isisRouter.neighborRouterId() == null) {
            isisRouter.setNeighborRouterId(IsisConstants.DEFAULTIP);
        }
        return isisRouter;
    }

    /**
     * Creates link information.
     *
     * @param localSystemId  local system ID
     * @param remoteSystemId remote system ID
     * @param interfaceIp interface address
     * @param neighborIp neighbor address
     * @param lsPdu link state PDU instance
     * @return link information instance
     */
    public LinkInformation createLinkInfo(String localSystemId, String remoteSystemId,
                                          Ip4Address interfaceIp, Ip4Address neighborIp,
                                          LsPdu lsPdu) {

        String linkId = "link:" + localSystemId + "-" + remoteSystemId;
        LinkInformation linkInformation = new DefaultIsisLinkInformation();
        linkInformation.setInterfaceIp(interfaceIp);
        linkInformation.setNeighborIp(neighborIp);
        linkInformation.setLinkId(linkId);
        linkInformation.setAlreadyCreated(false);
        linkInformation.setLinkDestinationId(remoteSystemId);
        linkInformation.setLinkSourceId(localSystemId);

        return linkInformation;
    }

    /**
     * Removes the device information from topology provider.
     *
     * @param isisRouter ISIS router instance
     */
    private void removeDeviceInfo(IsisRouter isisRouter) {
        if (isisRouter.systemId() != null) {
            controller.removeDeviceDetails(isisRouter);
        }
        isisRouterDetails.remove(isisRouter.systemId());
    }


    /**
     * Removes the link information from topology provider.
     *
     * @param lsPdu ls pdu instance
     */
    private void removeLinkInfo(LsPdu lsPdu) {
        Map<String, LinkInformation> linkInformationList = deviceAndLink.removeLinkInfo(lsPdu.lspId());
        for (String key : linkInformationList.keySet()) {
            LinkInformation linkInformation = linkInformationList.get(key);
            controller.removeLinkDetails(createIsisLink(linkInformation, lsPdu));
        }
    }

    /**
     * Creates ISIS link instance.
     *
     * @param linkInformation link information instance
     * @return isisLink instance
     */
    private IsisLink createIsisLink(LinkInformation linkInformation, LsPdu lsPdu) {
        IsisLink isisLink = new DefaultIsisLink();
        isisLink.setLocalSystemId(linkInformation.linkSourceId());
        isisLink.setRemoteSystemId(linkInformation.linkDestinationId());
        isisLink.setInterfaceIp(linkInformation.interfaceIp());
        isisLink.setNeighborIp(linkInformation.neighborIp());
        isisLink.setLinkTed(createIsisLinkTedInfo(lsPdu));
        return isisLink;
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
