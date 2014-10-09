package org.onlab.onos.store.device.impl.peermsg;

import java.util.Objects;

import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.PortNumber;
import org.onlab.onos.net.provider.ProviderId;

import com.google.common.base.MoreObjects;

/**
 * Identifier for PortDescription from a Provider.
 */
public final class PortFragmentId {
    public final ProviderId providerId;
    public final DeviceId deviceId;
    public final PortNumber portNumber;

    public PortFragmentId(DeviceId deviceId, ProviderId providerId,
                          PortNumber portNumber) {
        this.providerId = providerId;
        this.deviceId = deviceId;
        this.portNumber = portNumber;
    }

    @Override
    public int hashCode() {
        return Objects.hash(providerId, deviceId, portNumber);
    };

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PortFragmentId)) {
            return false;
        }
        PortFragmentId that = (PortFragmentId) obj;
        return Objects.equals(this.deviceId, that.deviceId) &&
               Objects.equals(this.portNumber, that.portNumber) &&
               Objects.equals(this.providerId, that.providerId);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("providerId", providerId)
                .add("deviceId", deviceId)
                .add("portNumber", portNumber)
                .toString();
    }

    // for serializer
    @SuppressWarnings("unused")
    private PortFragmentId() {
        this.providerId = null;
        this.deviceId = null;
        this.portNumber = null;
    }
}