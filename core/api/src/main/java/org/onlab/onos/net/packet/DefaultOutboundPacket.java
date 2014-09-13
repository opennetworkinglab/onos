package org.onlab.onos.net.packet;

import com.google.common.base.MoreObjects;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.flow.TrafficTreatment;

import java.nio.ByteBuffer;

/**
 * Default implementation of an immutable outbound packet.
 */
public class DefaultOutboundPacket implements OutboundPacket {
    private final DeviceId sendThrough;
    private final TrafficTreatment treatment;
    private final ByteBuffer data;

    /**
     * Creates an immutable outbound packet.
     *
     * @param sendThrough identifier through which to send the packet
     * @param treatment   list of packet treatments
     * @param data        raw packet data
     */
    public DefaultOutboundPacket(DeviceId sendThrough,
                                 TrafficTreatment treatment, ByteBuffer data) {
        this.sendThrough = sendThrough;
        this.treatment = treatment;
        this.data = data;
    }

    @Override
    public DeviceId sendThrough() {
        return sendThrough;
    }

    @Override
    public TrafficTreatment treatment() {
        return treatment;
    }

    @Override
    public ByteBuffer data() {
        // FIXME: figure out immutability here
        return data;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("sendThrough", sendThrough)
                .add("treatment", treatment)
                .toString();
    }
}
