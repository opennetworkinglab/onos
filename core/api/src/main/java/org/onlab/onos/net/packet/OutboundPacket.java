package org.onlab.onos.net.packet;

import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.flow.TrafficTreatment;

import java.nio.ByteBuffer;

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
     * Returns how the outbound packet should be treated.
     *
     * @return output treatment
     */
    TrafficTreatment treatment();

    /**
     * Returns immutable view of the raw data to be sent.
     *
     * @return data to emit
     */
    ByteBuffer data();

}
