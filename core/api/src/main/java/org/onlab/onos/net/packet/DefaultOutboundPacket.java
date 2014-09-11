package org.onlab.onos.net.packet;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import org.onlab.onos.net.DeviceId;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * Default implementation of an immutable outbound packet.
 */
public class DefaultOutboundPacket implements OutboundPacket {
    private final DeviceId sendThrough;
    private final List<Treatment> treatments;
    private final ByteBuffer data;

    /**
     * Creates an immutable outbound packet.
     *
     * @param sendThrough identifier through which to send the packet
     * @param treatments  list of packet treatments
     * @param data        raw packet data
     */
    public DefaultOutboundPacket(DeviceId sendThrough,
                                 List<Treatment> treatments, ByteBuffer data) {
        this.sendThrough = sendThrough;
        this.treatments = ImmutableList.copyOf(treatments);
        this.data = data;
    }

    @Override
    public DeviceId sendThrough() {
        return sendThrough;
    }

    @Override
    public List<Treatment> treatments() {
        return treatments;
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
                .add("treatments", treatments)
                .toString();
    }
}
