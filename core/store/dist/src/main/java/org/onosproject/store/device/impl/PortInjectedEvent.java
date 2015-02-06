package org.onosproject.store.device.impl;

import com.google.common.base.MoreObjects;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.provider.ProviderId;

import java.util.List;

public class PortInjectedEvent {

    private ProviderId providerId;
    private DeviceId deviceId;
    private List<PortDescription> portDescriptions;

    protected PortInjectedEvent(ProviderId providerId, DeviceId deviceId, List<PortDescription> portDescriptions) {
        this.providerId = providerId;
        this.deviceId = deviceId;
        this.portDescriptions = portDescriptions;
    }

    public DeviceId deviceId() {
        return deviceId;
    }

    public ProviderId providerId() {
        return providerId;
    }

    public List<PortDescription> portDescriptions() {
        return portDescriptions;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("providerId", providerId)
                .add("deviceId", deviceId)
                .add("portDescriptions", portDescriptions)
                .toString();
    }

    // for serializer
    protected PortInjectedEvent() {
        this.providerId = null;
        this.deviceId = null;
        this.portDescriptions = null;
    }

}
