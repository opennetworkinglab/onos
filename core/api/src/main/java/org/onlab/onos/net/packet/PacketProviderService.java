package org.onlab.onos.net.packet;

/**
 * Entity capable of processing inbound packets.
 */
public interface PacketProviderService {

    /**
     * Submits inbound packet context for processing. This processing will be
     * done synchronously, i.e. run-to-completion.
     *
     * @param context inbound packet context
     */
    void processPacket(PacketContext context);

}
