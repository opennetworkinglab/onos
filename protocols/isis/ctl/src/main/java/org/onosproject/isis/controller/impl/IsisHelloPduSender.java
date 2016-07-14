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

import com.google.common.primitives.Bytes;
import org.jboss.netty.channel.Channel;
import org.onosproject.isis.controller.IsisInterface;
import org.onosproject.isis.controller.IsisNetworkType;
import org.onosproject.isis.controller.IsisRouterType;
import org.onosproject.isis.io.util.IsisUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Representation of an ISIS hello pdu sender task.
 */
public class IsisHelloPduSender implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(IsisHelloPduSender.class);
    private Channel channel = null;
    private IsisInterface isisInterface = null;

    /**
     * Creates an instance of Hello PDU Sender task.
     *
     * @param channel       netty channel instance
     * @param isisInterface ISIS interface instance
     */
    public IsisHelloPduSender(Channel channel, IsisInterface isisInterface) {
        this.channel = channel;
        this.isisInterface = isisInterface;
    }

    @Override
    public void run() {
        if (channel != null && channel.isConnected() && channel.isOpen()) {
            try {
                byte[] helloPdu = null;
                byte[] interfaceIndex = {(byte) isisInterface.interfaceIndex()};

                if (isisInterface.networkType() == IsisNetworkType.P2P) {
                    helloPdu = IsisUtil.getP2pHelloPdu(isisInterface, true);
                    helloPdu = Bytes.concat(helloPdu, interfaceIndex);
                    channel.write(helloPdu);
                } else if (isisInterface.networkType() == IsisNetworkType.BROADCAST) {
                    switch (IsisRouterType.get(isisInterface.reservedPacketCircuitType())) {
                        case L1:
                            helloPdu = IsisUtil.getL1HelloPdu(isisInterface, true);
                            helloPdu = Bytes.concat(helloPdu, interfaceIndex);
                            channel.write(helloPdu);
                            break;
                        case L2:
                            helloPdu = IsisUtil.getL2HelloPdu(isisInterface, true);
                            helloPdu = Bytes.concat(helloPdu, interfaceIndex);
                            channel.write(helloPdu);
                            break;
                        case L1L2:
                            helloPdu = IsisUtil.getL1HelloPdu(isisInterface, true);
                            helloPdu = Bytes.concat(helloPdu, interfaceIndex);
                            channel.write(helloPdu);

                            helloPdu = IsisUtil.getL2HelloPdu(isisInterface, true);
                            helloPdu = Bytes.concat(helloPdu, interfaceIndex);
                            channel.write(helloPdu);
                            break;
                        default:
                            log.debug("IsisHelloPduSender::Unknown circuit type...!!!");
                            break;
                    }
                }
            } catch (Exception e) {
                log.debug("Exception @IsisHelloPduSender:: {}", e.getMessage());
            }
        }
    }
}