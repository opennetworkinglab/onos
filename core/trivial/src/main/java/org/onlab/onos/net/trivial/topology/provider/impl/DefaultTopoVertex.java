package org.onlab.onos.net.trivial.topology.provider.impl;

import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.topology.TopoVertex;

import java.util.Objects;

/**
 * Implementation of the topology vertex backed by a device id.
 */
class DefaultTopoVertex implements TopoVertex {

    private final DeviceId deviceId;

    /**
     * Creates a new topology vertex.
     *
     * @param deviceId backing infrastructure device identifier
     */
    DefaultTopoVertex(DeviceId deviceId) {
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
        if (obj instanceof DefaultTopoVertex) {
            final DefaultTopoVertex other = (DefaultTopoVertex) obj;
            return Objects.equals(this.deviceId, other.deviceId);
        }
        return false;
    }

    @Override
    public String toString() {
        return deviceId.toString();
    }

}

