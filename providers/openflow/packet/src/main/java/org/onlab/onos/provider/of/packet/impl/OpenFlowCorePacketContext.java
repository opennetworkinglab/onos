/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.onlab.onos.provider.of.packet.impl;

import org.onlab.onos.net.PortNumber;
import org.onlab.onos.net.flow.instructions.Instruction;
import org.onlab.onos.net.flow.instructions.Instruction.Type;
import org.onlab.onos.net.flow.instructions.Instructions.OutputInstruction;
import org.onlab.onos.net.packet.DefaultPacketContext;
import org.onlab.onos.net.packet.InboundPacket;
import org.onlab.onos.net.packet.OutboundPacket;
import org.onlab.onos.openflow.controller.OpenFlowPacketContext;
import org.onlab.packet.Ethernet;
import org.projectfloodlight.openflow.types.OFPort;

import java.util.List;

public class OpenFlowCorePacketContext extends DefaultPacketContext {

    private final OpenFlowPacketContext ofPktCtx;

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
                Ethernet eth = new Ethernet();
                eth.deserialize(outPacket().data().array(), 0,
                                outPacket().data().array().length);
                sendPacket(eth);
            }

        }
    }

    private void sendPacket(Ethernet eth) {
        List<Instruction> ins = treatmentBuilder().build().instructions();
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
