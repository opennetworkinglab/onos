package org.onlab.onos.net.packet;

/**
 * Service for intercepting data plane packets and for emitting synthetic
 * outbound packets.
 */
public interface PacketService {

    // TODO: ponder better ordering scheme that does not require absolute numbers

    /**
     * Adds the specified processor to the list of packet processors.
     * It will be added into the list in the order of priority. The higher
     * numbers will be processing the packets after the lower numbers.
     *
     * @param processor processor to be added
     * @param priority  priority in the reverse natural order
     * @throws java.lang.IllegalArgumentException if a processor with the
     *                                            given priority already exists
     */
    void addProcessor(PacketProcessor processor, int priority);

    /**
     * Removes the specified processor from the processing pipeline.
     *
     * @param processor packet processor
     */
    void removeProcessor(PacketProcessor processor);

    /**
     * Emits the specified outbound packet onto the network.
     *
     * @param packet outbound packet
     */
    void emit(OutboundPacket packet);

}
