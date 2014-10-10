package org.onlab.onos.store.device.impl.peermsg;

import java.util.Objects;

import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.provider.ProviderId;

import com.google.common.base.MoreObjects;

/**
 * Identifier for DeviceDesctiption from a Provider.
 */
public final class DeviceFragmentId {
    public final ProviderId providerId;
    public final DeviceId deviceId;

    public DeviceFragmentId(DeviceId deviceId, ProviderId providerId) {
        this.providerId = providerId;
        this.deviceId = deviceId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(providerId, deviceId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof DeviceFragmentId)) {
            return false;
        }
        DeviceFragmentId that = (DeviceFragmentId) obj;
        return Objects.equals(this.deviceId, that.deviceId) &&
               Objects.equals(this.providerId, that.providerId);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("providerId", providerId)
                .add("deviceId", deviceId)
                .toString();
    }

    // for serializer
    @SuppressWarnings("unused")
    private DeviceFragmentId() {
        this.providerId = null;
        this.deviceId = null;
    }
}