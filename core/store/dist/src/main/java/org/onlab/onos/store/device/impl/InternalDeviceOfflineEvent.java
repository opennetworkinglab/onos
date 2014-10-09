package org.onlab.onos.store.device.impl;

import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.device.Timestamp;

/**
 * Information published by GossipDeviceStore to notify peers of a device
 * going offline.
 */
public class InternalDeviceOfflineEvent {

    private final DeviceId deviceId;
    private final Timestamp timestamp;

    /**
     * Creates a InternalDeviceOfflineEvent.
     * @param deviceId identifier of device going offline.
     * @param timestamp timestamp of when the device went offline.
     */
    public InternalDeviceOfflineEvent(DeviceId deviceId, Timestamp timestamp) {
        this.deviceId = deviceId;
        this.timestamp = timestamp;
    }

    public DeviceId deviceId() {
        return deviceId;
    }

    public Timestamp timestamp() {
        return timestamp;
    }

    // for serializer
    @SuppressWarnings("unused")
    private InternalDeviceOfflineEvent() {
        deviceId = null;
        timestamp = null;
    }
}
