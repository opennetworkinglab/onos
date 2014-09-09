package org.onlab.onos.of.controller;

import java.util.Collections;

import org.onlab.packet.Ethernet;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFPacketOut;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;

public final class DefaultPacketContext implements PacketContext {

    private boolean free = true;
    private boolean isBuilt = false;
    private final OpenFlowSwitch sw;
    private final OFPacketIn pktin;
    private OFPacketOut pktout = null;

    private DefaultPacketContext(OpenFlowSwitch s, OFPacketIn pkt) {
        this.sw = s;
        this.pktin = pkt;
    }

    @Override
    public void block() {
        free = false;
    }

    @Override
    public void send() {
        if (free && isBuilt) {
            sw.sendMsg(pktout);
        }

    }

    @Override
    public void build(OFPort outPort) {
        isBuilt = true;

    }

    @Override
    public void build(Ethernet ethFrame, OFPort outPort) {
        if (isBuilt) {
            return;
        }
        OFPacketOut.Builder builder = sw.factory().buildPacketOut();
        OFAction act = sw.factory().actions()
                .buildOutput()
                .setPort(OFPort.of(outPort.getPortNumber()))
                .build();
        pktout = builder.setXid(pktin.getXid())
                .setBufferId(OFBufferId.NO_BUFFER)
                .setActions(Collections.singletonList(act))
                .setData(ethFrame.serialize())
                .build();
        isBuilt = true;
    }

    @Override
    public Ethernet parsed() {
        Ethernet eth = new Ethernet();
        eth.deserialize(pktin.getData(), 0, pktin.getTotalLen());
        return eth;
    }

    @Override
    public Dpid dpid() {
        return new Dpid(sw.getId());
    }

    public static PacketContext packetContextFromPacketIn(OpenFlowSwitch s, OFPacketIn pkt) {
        return new DefaultPacketContext(s, pkt);
    }

    @Override
    public Integer inPort() {
        return pktin.getInPort().getPortNumber();
    }

}
