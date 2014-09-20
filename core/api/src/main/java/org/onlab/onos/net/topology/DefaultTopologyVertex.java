package org.onlab.onos.net.topology;

import org.onlab.onos.net.DeviceId;

import java.util.Objects;

/**
 * Implementation of the topology vertex backed by a device id.
 */
public class DefaultTopologyVertex implements TopologyVertex {

    private final DeviceId deviceId;

    /**
     * Creates a new topology vertex.
     *
     * @param deviceId backing infrastructure device identifier
     */
    public DefaultTopologyVertex(DeviceId deviceId) {
        this.deviceId = deviceId;
    }

    @Override
    public DeviceId deviceId() {
        return deviceId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(deviceId);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DefaultTopologyVertex) {
            final DefaultTopologyVertex other = (DefaultTopologyVertex) obj;
            return Objects.equals(this.deviceId, other.deviceId);
        }
        return false;
    }

    @Override
    public String toString() {
        return deviceId.toString();
    }

}

