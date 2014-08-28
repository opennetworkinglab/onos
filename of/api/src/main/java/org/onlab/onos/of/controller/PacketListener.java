package org.onlab.onos.of.controller;

/**
 * Notifies providers about Packet in events.
 */
public interface PacketListener {

    /**
     * Handle the packet.
     * @param pktCtx the packet context ({@link }
     */
    public void handlePacket(PacketContext pktCtx);
}
