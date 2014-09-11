package org.onlab.onos.net.packet;

/**
 * Represents context for processing an inbound packet, and (optionally)
 * emitting a corresponding outbound packet.
 */
public interface PacketContext {

    /**
     * Returns the time when the packet was received.
     *
     * @return the time in millis since start of epoch
     */
    long time();

    /**
     * Returns the inbound packet being processed.
     *
     * @return inbound packet
     */
    InboundPacket inPacket();

    /**
     * Returns the view of the outbound packet.
     *
     * @return outbound packet
     */
    OutboundPacket outPacket();

    /**
     * Appends a new treatment to be applied to the outbound packet.
     *
     * @param treatment output treatment
     */
    void appendTreatment(Treatment treatment);

    /**
     * Triggers the outbound packet to be sent.
     */
    void send();

    /**
     * Blocks the outbound packet from being sent from this point onward.
     */
    void block();

    /**
     * Indicates whether the packet has already been handled, i.e. sent or
     * blocked.
     *
     * @return true if sent or blocked
     */
    boolean isHandled();

}
