package org.onlab.onos.store.device.impl;

import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.device.PortDescription;
import org.onlab.onos.net.provider.ProviderId;
import org.onlab.onos.store.impl.Timestamped;

import com.google.common.base.MoreObjects;

/**
 * Information published by GossipDeviceStore to notify peers of a port
 * status change event.
 */
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

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("providerId", providerId)
                .add("deviceId", deviceId)
                .add("portDescription", portDescription)
                .toString();
    }

    // for serializer
    protected InternalPortStatusEvent() {
        this.providerId = null;
        this.deviceId = null;
        this.portDescription = null;
    }
}
