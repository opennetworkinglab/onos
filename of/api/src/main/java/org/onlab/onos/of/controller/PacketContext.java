package org.onlab.onos.of.controller;

import org.onlab.packet.Ethernet;
import org.projectfloodlight.openflow.types.OFPort;

/**
 * A representation of a packet context which allows any provider
 * to view the packet in event but may block the response to the
 * event if blocked has been called.
 */
public interface PacketContext {

    //TODO: may want to support sending packet out other switches than
    // the one it came in on.
    /**
     * Blocks further responses (ie. send() calls) on this
     * packet in event.
     */
    public void block();

    /**
     * Provided build has been called send the packet
     * out the switch it came in on.
     */
    public void send();

    /**
     * Build the packet out in response to this packet in event.
     * @param outPort the out port to send to packet out of.
     */
    public void build(OFPort outPort);

    /**
     * Build the packet out in response to this packet in event.
     * @param ethFrame the actual packet to send out.
     * @param outPort the out port to send to packet out of.
     */
    public void build(Ethernet ethFrame, OFPort outPort);

    /**
     * Provided a handle onto the parsed payload.
     * @return the parsed form of the payload.
     */
    public Ethernet parsed();

    /**
     * Provide the dpid of the switch where the packet in arrived.
     * @return the dpid of the switch.
     */
    public Dpid dpid();

    /**
     * Provide the port on which the packet arrived.
     * @return the port
     */
    public Integer inPort();
}
