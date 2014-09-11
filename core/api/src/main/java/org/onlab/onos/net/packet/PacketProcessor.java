package org.onlab.onos.net.packet;

/**
 * Abstraction of an inbound packet processor.
 */
public interface PacketProcessor {

    /**
     * Processes the inbound packet as specified in the given context.
     *
     * @param context packet processing context
     */
    void process(PacketContext context);

}
