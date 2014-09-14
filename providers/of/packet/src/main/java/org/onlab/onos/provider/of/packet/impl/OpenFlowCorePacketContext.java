package org.onlab.onos.provider.of.packet.impl;

import static org.slf4j.LoggerFactory.getLogger;

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
        if (!this.isHandled()) {
            block();
            if (outPacket() == null) {
                ofPktCtx.build(OFPort.FLOOD);
            } else {
                Ethernet eth = new Ethernet();
                eth.deserialize(outPacket().data().array(), 0,
                        outPacket().data().array().length);
                ofPktCtx.build(eth, OFPort.FLOOD);
            }
            ofPktCtx.send();
        }
    }

}
