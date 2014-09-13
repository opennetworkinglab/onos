package org.onlab.onos.of.controller;

/**
 * Notifies providers about Packet in events.
 */
public interface PacketListener {

    /**
     * Handles the packet.
     *
     * @param pktCtx the packet context
     */
    public void handlePacket(OpenFlowPacketContext pktCtx);
}
