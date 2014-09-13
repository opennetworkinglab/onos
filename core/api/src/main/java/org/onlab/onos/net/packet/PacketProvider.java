package org.onlab.onos.net.packet;

import org.onlab.onos.net.provider.Provider;

/**
 * Abstraction of a packet provider capable of emitting packets.
 */
public interface PacketProvider extends Provider{

    /**
     * Emits the specified outbound packet onto the network.
     *
     * @param packet outbound packet
     */
    void emit(OutboundPacket packet);

}
