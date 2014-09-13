package org.onlab.onos.net.packet;

import org.onlab.onos.net.provider.ProviderService;

/**
 * Entity capable of processing inbound packets.
 */
public interface PacketProviderService extends ProviderService<PacketProvider>{

    /**
     * Submits inbound packet context for processing. This processing will be
     * done synchronously, i.e. run-to-completion.
     *
     * @param context inbound packet context
     */
    void processPacket(PacketContext context);

}
