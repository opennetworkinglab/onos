package org.onlab.onos.store.device.impl;

import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.device.PortDescription;
import org.onlab.onos.net.provider.ProviderId;
import org.onlab.onos.store.common.impl.Timestamped;

public class InternalPortStatusEvent {

    private final ProviderId providerId;
    private final DeviceId deviceId;
    private final Timestamped<PortDescription> portDescription;

    protected InternalPortStatusEvent(
            ProviderId providerId,
            DeviceId deviceId,
            Timestamped<PortDescription> portDescription) {
        this.providerId = providerId;
        this.deviceId = deviceId;
        this.portDescription = portDescription;
    }

    public DeviceId deviceId() {
        return deviceId;
    }

    public ProviderId providerId() {
        return providerId;
    }

    public Timestamped<PortDescription> portDescription() {
        return portDescription;
    }
}
