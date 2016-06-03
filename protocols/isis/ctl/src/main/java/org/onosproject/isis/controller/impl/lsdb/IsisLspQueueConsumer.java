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
package org.onosproject.isis.controller.impl.lsdb;

import org.jboss.netty.channel.Channel;
import org.onosproject.isis.controller.IsisLsdb;
import org.onosproject.isis.controller.IsisPduType;
import org.onosproject.isis.controller.LspWrapper;
import org.onosproject.isis.controller.impl.DefaultIsisInterface;
import org.onosproject.isis.io.isispacket.pdu.LsPdu;
import org.onosproject.isis.io.util.IsisConstants;
import org.onosproject.isis.io.util.IsisUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;

/**
 * Representation of LSP queue consumer.
 */
public class IsisLspQueueConsumer implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(IsisLspQueueConsumer.class);
    private BlockingQueue queue = null;

    /**
     * Creates an instance of LSP queue consumer.
     *
     * @param queue queue instance
     */
    public IsisLspQueueConsumer(BlockingQueue queue) {
        this.queue = queue;
    }

    /**
     * Gets the LSP wrapper instance from queue and process it.
     */
    @Override
    public void run() {
        log.debug("LSPQueueConsumer:run...!!!");
        try {
            while (true) {
                if (!queue.isEmpty()) {
                    LspWrapper wrapper = (LspWrapper) queue.take();
                    String lspProcessing = wrapper.lspProcessing();
                    switch (lspProcessing) {
                        case IsisConstants.REFRESHLSP:
                            log.debug("LSPQueueConsumer: Message - " + IsisConstants.REFRESHLSP +
                                    " consumed.");
                            processRefreshLsp(wrapper);
                            break;
                        case IsisConstants.MAXAGELSP:
                            log.debug("LSPQueueConsumer: Message - " + IsisConstants.MAXAGELSP +
                                    " consumed.");
                            processMaxAgeLsa(wrapper);
                            break;
                        default:
                            log.debug("Unknown command to process the LSP in queue ...!!!");
                            break;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Error::LSPQueueConsumer::{}", e.getMessage());
        }
    }

    /**
     * Process refresh LSP.
     *
     * @param wrapper LSP wrapper instance
     */
    private void processRefreshLsp(LspWrapper wrapper) throws Exception {
        if (wrapper.isSelfOriginated()) { //self originated
            DefaultIsisInterface isisInterface = (DefaultIsisInterface) wrapper.isisInterface();
            Channel channel = isisInterface.channel();
            if (channel != null && channel.isConnected()) {
                LsPdu lsPdu = (LsPdu) wrapper.lsPdu();
                lsPdu.setSequenceNumber(isisInterface.isisLsdb().lsSequenceNumber(
                        IsisPduType.get(lsPdu.pduType())));
                lsPdu.setRemainingLifeTime(IsisConstants.LSPMAXAGE);
                byte[] lspBytes = lsPdu.asBytes();
                lspBytes = IsisUtil.addLengthAndMarkItInReserved(lspBytes, IsisConstants.LENGTHPOSITION,
                        IsisConstants.LENGTHPOSITION + 1,
                        IsisConstants.RESERVEDPOSITION);
                lspBytes = IsisUtil.addChecksum(lspBytes, IsisConstants.CHECKSUMPOSITION,
                        IsisConstants.CHECKSUMPOSITION + 1);
                //write to the channel
                channel.write(IsisUtil.framePacket(lspBytes, isisInterface.interfaceIndex()));
                // Updating the database with resetting remaining life time to default.
                IsisLsdb isisDb = isisInterface.isisLsdb();
                isisDb.addLsp(lsPdu, true, isisInterface);
                log.debug("LSPQueueConsumer: processRefreshLsp - Flooded SelfOriginated LSP {}",
                        wrapper.lsPdu());
            }
        }
    }

    /**
     * Process max age LSP.
     *
     * @param wrapper LSP wrapper instance
     */
    private void processMaxAgeLsa(LspWrapper wrapper) {
        //set the destination
        DefaultIsisInterface isisInterface = (DefaultIsisInterface) wrapper.isisInterface();
        if (isisInterface != null) {
            //delete from db
            LsPdu lsPdu = (LsPdu) wrapper.lsPdu();
            IsisLsdb isisDb = isisInterface.isisLsdb();
            isisDb.deleteLsp(lsPdu);
            log.debug("LSPQueueConsumer: processMaxAgeLsp - Removed-Max Age LSP {}",
                    wrapper.lsPdu());
        }
    }
}