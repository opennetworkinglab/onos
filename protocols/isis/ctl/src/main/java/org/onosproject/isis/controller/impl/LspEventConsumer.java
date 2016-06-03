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

import org.onosproject.isis.controller.IsisNetworkType;
import org.onosproject.isis.controller.LspWrapper;
import org.onosproject.isis.controller.impl.topology.DefaultIsisLink;
import org.onosproject.isis.controller.impl.topology.DefaultIsisRouter;
import org.onosproject.isis.controller.impl.topology.TopologyForDeviceAndLinkImpl;
import org.onosproject.isis.controller.topology.IsisLink;
import org.onosproject.isis.controller.topology.IsisLinkTed;
import org.onosproject.isis.controller.topology.IsisRouter;
import org.onosproject.isis.controller.topology.LinkInformation;
import org.onosproject.isis.io.isispacket.pdu.LsPdu;
import org.onosproject.isis.io.isispacket.tlv.IpExtendedReachabilityTlv;
import org.onosproject.isis.io.isispacket.tlv.IsExtendedReachability;
import org.onosproject.isis.io.isispacket.tlv.IsisTlv;
import org.onosproject.isis.io.isispacket.tlv.NeighborForExtendedIs;
import org.onosproject.isis.io.util.IsisConstants;
import org.onosproject.isis.io.util.IsisUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

/**
 * Representation of LSP event consumer.
 */
public class LspEventConsumer implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(LspEventConsumer.class);
    public static List<LsPdu> lsPdus = new ArrayList<>();
    private String lspAdded = "LSP_ADDED";
    private String lspRemoved = "LSP_REMOVED";
    private BlockingQueue queue = null;
    private Controller controller = null;
    private TopologyForDeviceAndLinkImpl deviceAndLink = new TopologyForDeviceAndLinkImpl();

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
        log.debug("LspsForProvider:run...!!!");
        try {
            while (true) {
                if (!queue.isEmpty()) {
                    LspWrapper wrapper = (LspWrapper) queue.take();
                    LsPdu lsPdu = (LsPdu) wrapper.lsPdu();
                    for (IsisTlv isisTlv : lsPdu.tlvs()) {
                        if ((isisTlv instanceof IpExtendedReachabilityTlv) ||
                                (isisTlv instanceof IsExtendedReachability)) {
                            lsPdus.add(lsPdu);
                            if (wrapper.lspProcessing().equals(lspAdded)) {
                                callTopologyToSendInfo(lsPdu, wrapper.isisInterface().networkType(),
                                        wrapper.isisInterface().systemId() + ".00");
                            }
                            if (wrapper.lspProcessing().equals(lspRemoved)) {
                                callTopologyToRemoveInfo(lsPdu);
                            }
                            break;
                        }
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
     */
    private void callTopologyToSendInfo(LsPdu lsPdu, IsisNetworkType isisNetworkType,
                                        String ownSystemId) {
        if (isisNetworkType.equals(IsisNetworkType.BROADCAST)) {
            sendDeviceInfo(lsPdu);
            boolean isDis = IsisUtil.checkIsDis(lsPdu.lspId());
            if (isDis) {
                sendLinkInfo(lsPdu, ownSystemId);
            }
        } else if (isisNetworkType.equals(IsisNetworkType.P2P)) {

            sendDeviceInfo(lsPdu);

            for (LsPdu wrapper : lsPdus) {
                LsPdu lsPduStored = wrapper;
                List<String> neStringList = neighborList(lsPduStored, ownSystemId);
                String lspId = IsisUtil.removeTailingZeros(lsPdu.lspId());
                if (neStringList.contains(lspId)) {
                    sendLinkInfo(lsPduStored, ownSystemId);
                }
            }

            List<String> neStringList = neighborList(lsPdu, ownSystemId);
            Map<String, IsisRouter> routerPresence = deviceAndLink.isisDeviceList();
            for (String neighbor : neStringList) {
                IsisRouter isisRouter = routerPresence.get(neighbor);
                if (isisRouter != null) {
                    sendLinkInfo(lsPdu, ownSystemId);
                } else {
                    lsPdus.add(lsPdu);
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
        removeDeviceInfo(lsPdu);
        removeLinkInfo(lsPdu);
    }

    /**
     * Sends the device information to topology provider.
     *
     * @param lsPdu ls pdu instance
     */
    private void sendDeviceInfo(LsPdu lsPdu) {
        IsisRouter isisRouter = deviceAndLink.createDeviceInfo(lsPdu);
        if (isisRouter.systemId() != null) {
            controller.addDeviceDetails(isisRouter);
        }
    }

    /**
     * Returns the list of neighbors.
     *
     * @param lsPdu link state Pdu
     * @return neighbor list
     */
    private List<String> neighborList(LsPdu lsPdu, String ownSystemId) {
        List<String> neighbourList = new ArrayList<>();
        for (IsisTlv isisTlv : lsPdu.tlvs()) {
            if (isisTlv instanceof IsExtendedReachability) {
                IsExtendedReachability isExtendedReachability = (IsExtendedReachability) isisTlv;
                List<NeighborForExtendedIs> neighborForExtendedIsList = isExtendedReachability.neighbours();
                for (NeighborForExtendedIs neighbor : neighborForExtendedIsList) {
                    String neighbourId = neighbor.neighborId();
                    if (!(neighbourId.equals(ownSystemId))) {
                        neighbourList.add(neighbourId);
                    }

                }
            }
        }
        return neighbourList;
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
     * Removes the device information from topology provider.
     *
     * @param lsPdu ls pdu instance
     */
    private void removeDeviceInfo(LsPdu lsPdu) {
        IsisRouter isisRouter = deviceAndLink.removeDeviceAndLinkInfo(lsPdu);
        if (isisRouter.systemId() != null) {
            controller.removeDeviceDetails(isisRouter);
        }
    }

    /**
     * Sends the link information to topology provider.
     *
     * @param lsPdu ls pdu instance
     */
    private void sendLinkInfo(LsPdu lsPdu, String ownSystemId) {
        Map<String, LinkInformation> linkInformationList = deviceAndLink.createLinkInfo(lsPdu, ownSystemId);
        for (String key : linkInformationList.keySet()) {
            LinkInformation linkInformation = linkInformationList.get(key);
            if (linkInformation.isAlreadyCreated()) {
                controller.addDeviceDetails(createIsisRouterDummy(linkInformation.linkDestinationId()));
                controller.addLinkDetails(createIsisLink(linkInformation, lsPdu));
            } else {
                controller.addLinkDetails(createIsisLink(linkInformation, lsPdu));
            }
        }
    }

    /**
     * Removes the link information from topology provider.
     *
     * @param lsPdu ls pdu instance
     */
    private void removeLinkInfo(LsPdu lsPdu) {
        Map<String, LinkInformation> linkInformationList = deviceAndLink.removeLinkInfo(lsPdu);
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
        isisLink.setLinkTed(createLinkTedInfo(lsPdu));
        return isisLink;
    }

    /**
     * Creates the link TED information.
     *
     * @param lsPdu link state PDU
     * @return isisLinkTed
     */
    private IsisLinkTed createLinkTedInfo(LsPdu lsPdu) {
        IsisLinkTed isisLinkTed = deviceAndLink.createIsisLinkTedInfo(lsPdu);
        return isisLinkTed;
    }
}