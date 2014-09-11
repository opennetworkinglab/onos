package org.onlab.onos.net.packet;

import org.onlab.onos.net.DeviceId;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * Represents an outbound data packet that is to be emitted to network via
 * an infrastructure device.
 */
public interface OutboundPacket {

    /**
     * Returns the identity of a device through which this packet should be
     * sent.
     *
     * @return device identity
     */
    DeviceId sendThrough();

    /**
     * Returns list of treatments for the outbound packet.
     *
     * @return output treatment
     */
    List<Treatment> treatments();

    /**
     * Returns the raw data to be sent.
     *
     * @return data to emit
     */
    ByteBuffer data();

}
