/*
 * Copyright 2014-present Open Networking Foundation
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
package org.onosproject.provider.of.packet.impl;

import org.onlab.packet.DeserializationException;
import org.onlab.packet.Ethernet;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instruction.Type;
import org.onosproject.net.flow.instructions.Instructions.OutputInstruction;
import org.onosproject.net.packet.DefaultPacketContext;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.openflow.controller.OpenFlowPacketContext;
import org.projectfloodlight.openflow.types.OFPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Packet context used with the OpenFlow providers.
 */
public class OpenFlowCorePacketContext extends DefaultPacketContext {

    private static final Logger log = LoggerFactory.getLogger(OpenFlowCorePacketContext.class);

    private final OpenFlowPacketContext ofPktCtx;

    /**
     * Creates a new OpenFlow core packet context.
     *
     * @param time creation time
     * @param inPkt inbound packet
     * @param outPkt outbound packet
     * @param block whether the context is blocked or not
     * @param ofPktCtx OpenFlow packet context
     */
    protected OpenFlowCorePacketContext(long time, InboundPacket inPkt,
                                        OutboundPacket outPkt, boolean block,
                                        OpenFlowPacketContext ofPktCtx) {
        super(time, inPkt, outPkt, block);
        this.ofPktCtx = ofPktCtx;
    }

    @Override
    public void send() {
        if (!this.block()) {
            if (outPacket() == null) {
                sendPacket(null);
            } else {
                try {
                    Ethernet eth = Ethernet.deserializer()
                            .deserialize(outPacket().data().array(), 0,
                                         outPacket().data().array().length);
                    sendPacket(eth);
                } catch (DeserializationException e) {
                    log.warn("Unable to deserialize packet");
                }
            }
        }
    }

    private void sendPacket(Ethernet eth) {
        List<Instruction> ins = treatmentBuilder().build().allInstructions();
        OFPort p = null;
        //TODO: support arbitrary list of treatments must be supported in ofPacketContext
        for (Instruction i : ins) {
            if (i.type() == Type.OUTPUT) {
                p = buildPort(((OutputInstruction) i).port());
                break; //for now...
            }
        }
        if (eth == null) {
            ofPktCtx.build(p);
        } else {
            ofPktCtx.build(eth, p);
        }
        ofPktCtx.send();
    }

    private OFPort buildPort(PortNumber port) {
        return OFPort.of((int) port.toLong());
    }

}
