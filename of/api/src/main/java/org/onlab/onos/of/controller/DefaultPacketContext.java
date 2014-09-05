package org.onlab.onos.of.controller;

import org.onlab.packet.Ethernet;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFPacketOut;
import org.projectfloodlight.openflow.types.OFPort;

public class DefaultPacketContext implements PacketContext {

    private boolean free = true;
    private boolean isBuilt = false;
    private final OpenFlowSwitch sw;
    private final OFPacketIn pktin;
    private final OFPacketOut pktout = null;

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
        // TODO Auto-generated method stub

    }

    @Override
    public Ethernet parsed() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Dpid dpid() {
        // TODO Auto-generated method stub
        return null;
    }

    public static PacketContext PacketContextFromPacketIn(OpenFlowSwitch s, OFPacketIn pkt) {
        return new DefaultPacketContext(s, pkt);
    }

}
