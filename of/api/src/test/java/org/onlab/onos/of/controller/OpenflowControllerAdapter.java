package org.onlab.onos.of.controller;

import org.projectfloodlight.openflow.protocol.OFMessage;

/**
 * Test adapter for the OpenFlow controller interface.
 */
public class OpenflowControllerAdapter implements OpenFlowController {
    @Override
    public Iterable<OpenFlowSwitch> getSwitches() {
        return null;
    }

    @Override
    public Iterable<OpenFlowSwitch> getMasterSwitches() {
        return null;
    }

    @Override
    public Iterable<OpenFlowSwitch> getEqualSwitches() {
        return null;
    }

    @Override
    public OpenFlowSwitch getSwitch(Dpid dpid) {
        return null;
    }

    @Override
    public OpenFlowSwitch getMasterSwitch(Dpid dpid) {
        return null;
    }

    @Override
    public OpenFlowSwitch getEqualSwitch(Dpid dpid) {
        return null;
    }

    @Override
    public void addListener(OpenFlowSwitchListener listener) {
    }

    @Override
    public void removeListener(OpenFlowSwitchListener listener) {
    }

    @Override
    public void addPacketListener(int priority, PacketListener listener) {
    }

    @Override
    public void removePacketListener(PacketListener listener) {
    }

    @Override
    public void write(Dpid dpid, OFMessage msg) {
    }

    @Override
    public void processPacket(Dpid dpid, OFMessage msg) {
    }

    @Override
    public void setRole(Dpid dpid, RoleState role) {
    }
}
