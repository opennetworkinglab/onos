package org.onosproject.store.device.impl;

import com.google.common.base.MoreObjects;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.provider.ProviderId;

public class DeviceInjectedEvent {
    private final ProviderId providerId;
    private final DeviceId deviceId;
    private final DeviceDescription deviceDescription;

    protected DeviceInjectedEvent(
            ProviderId providerId,
            DeviceId deviceId,
            DeviceDescription deviceDescription) {
        this.providerId = providerId;
        this.deviceId = deviceId;
        this.deviceDescription = deviceDescription;
    }

    public DeviceId deviceId() {
        return deviceId;
    }

    public ProviderId providerId() {
        return providerId;
    }

    public DeviceDescription deviceDescription() {
        return deviceDescription;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("providerId", providerId)
                .add("deviceId", deviceId)
                .add("deviceDescription", deviceDescription)
                .toString();
    }

    // for serializer
    protected DeviceInjectedEvent() {
        this.providerId = null;
        this.deviceId = null;
        this.deviceDescription = null;
    }
}
