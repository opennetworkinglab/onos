package org.onlab.onos.net.packet;

/**
 * Abstraction of a packet provider capable of emitting packets.
 */
public interface PacketProvider {

    /**
     * Emits the specified outbound packet onto the network.
     *
     * @param packet outbound packet
     */
    void emit(OutboundPacket packet);

}
