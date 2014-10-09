package org.onlab.onos.store.device.impl;

import java.util.List;

import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.device.PortDescription;
import org.onlab.onos.net.provider.ProviderId;
import org.onlab.onos.store.common.impl.Timestamped;

/**
 * Information published by GossipDeviceStore to notify peers of a port
 * change event.
 */
public class InternalPortEvent {

    private final ProviderId providerId;
    private final DeviceId deviceId;
    private final Timestamped<List<PortDescription>> portDescriptions;

    protected InternalPortEvent(
            ProviderId providerId,
            DeviceId deviceId,
            Timestamped<List<PortDescription>> portDescriptions) {
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

    public Timestamped<List<PortDescription>> portDescriptions() {
        return portDescriptions;
    }

    // for serializer
    protected InternalPortEvent() {
        this.providerId = null;
        this.deviceId = null;
        this.portDescriptions = null;
    }
}
