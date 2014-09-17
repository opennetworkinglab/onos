package org.onlab.onos.provider.of.packet.impl;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;

import org.onlab.onos.net.PortNumber;
import org.onlab.onos.net.flow.instructions.Instruction;
import org.onlab.onos.net.flow.instructions.Instruction.Type;
import org.onlab.onos.net.flow.instructions.Instructions.OutputInstruction;
import org.onlab.onos.net.packet.DefaultPacketContext;
import org.onlab.onos.net.packet.InboundPacket;
import org.onlab.onos.net.packet.OutboundPacket;
import org.onlab.onos.of.controller.OpenFlowPacketContext;
import org.onlab.packet.Ethernet;
import org.projectfloodlight.openflow.types.OFPort;
import org.slf4j.Logger;

public class OpenFlowCorePacketContext extends DefaultPacketContext {

    private final Logger log = getLogger(getClass());

    private final OpenFlowPacketContext ofPktCtx;

    protected OpenFlowCorePacketContext(long time, InboundPacket inPkt,
            OutboundPacket outPkt, boolean block, OpenFlowPacketContext ofPktCtx) {
        super(time, inPkt, outPkt, block);
        this.ofPktCtx = ofPktCtx;
    }

    @Override
    public void send() {
        if (!this.block()) {
            if (outPacket() == null) {
                sendBufferedPacket();
            } else {
                Ethernet eth = new Ethernet();
                eth.deserialize(outPacket().data().array(), 0,
                        outPacket().data().array().length);
                ofPktCtx.build(eth, OFPort.FLOOD);
            }

        }
    }

    private void sendBufferedPacket() {
        List<Instruction> ins = treatmentBuilder().build().instructions();
        OFPort p = null;
        //TODO: support arbitrary list of treatments must be supported in ofPacketContext
        for (Instruction i : ins) {
            if (i.type() == Type.OUTPUT) {
                p = buildPort(((OutputInstruction) i).port());
                break; //for now...
            }
        }
        ofPktCtx.build(p);
        ofPktCtx.send();
    }

    private OFPort buildPort(PortNumber port) {
        return OFPort.of((int) port.toLong());
    }

}
