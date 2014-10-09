package org.onlab.onos.store.device.impl;

import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.device.DeviceDescription;
import org.onlab.onos.net.provider.ProviderId;
import org.onlab.onos.store.common.impl.Timestamped;

/**
 * Information published by GossipDeviceStore to notify peers of a device
 * change event.
 */
public class InternalDeviceEvent {

    private final ProviderId providerId;
    private final DeviceId deviceId;
    private final Timestamped<DeviceDescription> deviceDescription;

    protected InternalDeviceEvent(
            ProviderId providerId,
            DeviceId deviceId,
            Timestamped<DeviceDescription> deviceDescription) {
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

    public Timestamped<DeviceDescription> deviceDescription() {
        return deviceDescription;
    }

    // for serializer
    protected InternalDeviceEvent() {
        this.providerId = null;
        this.deviceId = null;
        this.deviceDescription = null;
    }
}
