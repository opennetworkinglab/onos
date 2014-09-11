package org.onlab.onos.net.packet;

import org.onlab.onos.net.ConnectPoint;
import org.onlab.packet.Ethernet;

import java.nio.ByteBuffer;

/**
 * Represents a data packet intercepted from an infrastructure device.
 */
public interface InboundPacket {

    /**
     * Returns the device and port from where the packet was received.
     *
     * @return connection point where received
     */
    ConnectPoint receivedFrom();

    /**
     * Returns the parsed form of the packet.
     *
     * @return parsed Ethernet frame; null if the packet is not an Ethernet
     * frame or one for which there is no parser
     */
    Ethernet parsed();

    /**
     * Unparsed packet data.
     *
     * @return raw packet bytes
     */
    ByteBuffer unparsed();

}
