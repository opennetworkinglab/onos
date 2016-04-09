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
package org.onosproject.ospf.controller.lsdb;

import org.jboss.netty.channel.Channel;
import org.onosproject.ospf.controller.LsaWrapper;
import org.onosproject.ospf.controller.OspfArea;
import org.onosproject.ospf.controller.OspfInterface;
import org.onosproject.ospf.controller.OspfLsaType;
import org.onosproject.ospf.controller.area.OspfAreaImpl;
import org.onosproject.ospf.controller.area.OspfInterfaceImpl;
import org.onosproject.ospf.protocol.lsa.LsaHeader;
import org.onosproject.ospf.protocol.lsa.types.NetworkLsa;
import org.onosproject.ospf.protocol.lsa.types.RouterLsa;
import org.onosproject.ospf.protocol.util.ChecksumCalculator;
import org.onosproject.ospf.protocol.util.OspfInterfaceState;
import org.onosproject.ospf.protocol.util.OspfParameters;
import org.onosproject.ospf.protocol.util.OspfUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;

/**
 * Consumes LSA from the Queue and processes it.
 * Its a producer consumer implementation using Blocking queue.
 */
public class LsaQueueConsumer implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(LsaQueueConsumer.class);
    private BlockingQueue queue = null;
    private Channel channel;
    private OspfArea ospfArea;

    /**
     * Creates an instance of LSA queue consumer.
     *
     * @param queue    queue instance
     * @param channel  netty channel instance
     * @param ospfArea OSPF area instance
     */
    public LsaQueueConsumer(BlockingQueue queue, Channel channel, OspfArea ospfArea) {
        this.queue = queue;
        this.channel = channel;
        this.ospfArea = ospfArea;
    }

    /**
     * Threads run method.
     */
    public void run() {
        log.debug("LSAQueueConsumer:run...!!!");
        try {
            while (true) {
                if (!queue.isEmpty()) {
                    LsaWrapper wrapper = (LsaWrapper) queue.take();
                    String lsaProcessing = wrapper.lsaProcessing();
                    switch (lsaProcessing) {
                        case OspfParameters.VERIFYCHECKSUM:
                            log.debug("LSAQueueConsumer: Message - " + OspfParameters.VERIFYCHECKSUM + " consumed.");
                            processVerifyChecksum(wrapper);
                            break;
                        case OspfParameters.REFRESHLSA:
                            log.debug("LSAQueueConsumer: Message - " + OspfParameters.REFRESHLSA + " consumed.");
                            processRefreshLsa(wrapper);
                            break;
                        case OspfParameters.MAXAGELSA:
                            log.debug("LSAQueueConsumer: Message - " + OspfParameters.MAXAGELSA + " consumed.");
                            processMaxAgeLsa(wrapper);
                            break;
                        default:
                            log.debug("Unknown command to process the LSA in queue ...!!!");
                            break;
                    }
                }
            }

        } catch (Exception e) {
            log.debug("Error::LSAQueueConsumer::{}", e.getMessage());
        }
    }

    /**
     * Processes verify checksum - checkAges.
     *
     * @param wrapper LSA wrapper instance
     */
    private void processVerifyChecksum(LsaWrapper wrapper) throws Exception {
        ChecksumCalculator checkSum = new ChecksumCalculator();
        if (!checkSum.isValidLsaCheckSum(wrapper.ospfLsa(), ((LsaWrapperImpl) wrapper).lsaHeader().lsType(),
                                         OspfUtil.LSAPACKET_CHECKSUM_POS1,
                                         OspfUtil.LSAPACKET_CHECKSUM_POS2)) {
            log.debug("LSAQueueConsumer::Checksum mismatch. Received LSA packet type {} ",
                      ((LsaWrapperImpl) wrapper).lsaHeader().lsType());

            //Checksum Invalid
            //RFC 2328 Restart the Router.
            //Currently we are not restarting. We are not handling this case.
        }
    }

    /**
     * Process refresh LSA.
     *
     * @param wrapper LSA wrapper instance
     */
    private void processRefreshLsa(LsaWrapper wrapper) throws Exception {
        if (wrapper.isSelfOriginated()) { //self originated
            //set the destination
            OspfInterface ospfInterface = wrapper.ospfInterface();
            if (ospfInterface != null) {
                LsaHeader header = ((LsaWrapperImpl) wrapper).lsaHeader();
                header.setAge(wrapper.currentAge());
                if (((OspfInterfaceImpl) ospfInterface).state() == OspfInterfaceState.DR) {
                    if (header.lsType() == OspfLsaType.ROUTER.value()) {
                        RouterLsa routerLsa = ((OspfAreaImpl) ospfArea).buildRouterLsa(ospfInterface);
                        ((OspfAreaImpl) ospfArea).addLsa(routerLsa, true, ospfInterface);
                        ((OspfAreaImpl) ospfArea).addToOtherNeighborLsaTxList(routerLsa);
                    } else if (header.lsType() == OspfLsaType.NETWORK.value()) {
                        if (ospfInterface.listOfNeighbors().size() > 0) {
                            NetworkLsa networkLsa = ((OspfAreaImpl) ospfArea).buildNetworkLsa(
                                    ospfInterface.ipAddress(), ospfInterface.ipNetworkMask());
                            ospfArea.addLsa(networkLsa, true, ospfInterface);
                            ((OspfAreaImpl) ospfArea).addToOtherNeighborLsaTxList(networkLsa);
                        }
                    }
                }

                if (((OspfInterfaceImpl) ospfInterface).state() == OspfInterfaceState.BDR ||
                        ((OspfInterfaceImpl) ospfInterface).state() == OspfInterfaceState.POINT2POINT ||
                        ((OspfInterfaceImpl) ospfInterface).state() == OspfInterfaceState.DROTHER) {
                    ospfArea.refreshArea(ospfInterface);
                }
                log.debug("LSAQueueConsumer: processRefreshLsa - Flooded SelfOriginated LSA {}",
                          ((LsaWrapperImpl) wrapper).lsaHeader());
            }
        }
    }

    /**
     * Process max age LSA.
     *
     * @param wrapper LSA wrapper instance
     */
    private void processMaxAgeLsa(LsaWrapper wrapper) {
        //set the destination
        OspfInterface ospfInterface = wrapper.ospfInterface();
        if (ospfInterface != null) {
            LsaHeader header = (LsaHeader) wrapper.ospfLsa().lsaHeader();
            header.setAge(OspfParameters.MAXAGE);
            ((LsaWrapperImpl) wrapper).lsaHeader().setAge(OspfParameters.MAXAGE);
            if (((OspfInterfaceImpl) ospfInterface).state() == OspfInterfaceState.DR ||
                    ((OspfInterfaceImpl) ospfInterface).state() == OspfInterfaceState.POINT2POINT) {
                //remove from db
                ((OspfAreaImpl) ospfArea).addToOtherNeighborLsaTxList(((LsaWrapperImpl) wrapper).lsaHeader());
                ((OspfAreaImpl) ospfArea).deleteLsa(((LsaWrapperImpl) wrapper).lsaHeader());
            } else {
                ((OspfAreaImpl) ospfArea).deleteLsa(((LsaWrapperImpl) wrapper).lsaHeader());
            }
            log.debug("LSAQueueConsumer: processMaxAgeLsa - Flooded SelfOriginated-Max Age LSA {}",
                      ((LsaWrapperImpl) wrapper).lsaHeader());
        }
    }

    /**
     * Sets the channel.
     *
     * @param channel channel instance
     */
    public void setChannel(Channel channel) {
        this.channel = channel;
    }
}