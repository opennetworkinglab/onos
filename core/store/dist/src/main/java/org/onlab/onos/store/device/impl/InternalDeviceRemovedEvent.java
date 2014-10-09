package org.onlab.onos.store.device.impl;

import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.device.Timestamp;

/**
 * Information published by GossipDeviceStore to notify peers of a device
 * being administratively removed.
 */
public class InternalDeviceRemovedEvent {

    private final DeviceId deviceId;
    private final Timestamp timestamp;

    /**
     * Creates a InternalDeviceRemovedEvent.
     * @param deviceId identifier of the removed device.
     * @param timestamp timestamp of when the device was administratively removed.
     */
    public InternalDeviceRemovedEvent(DeviceId deviceId, Timestamp timestamp) {
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
    private InternalDeviceRemovedEvent() {
        deviceId = null;
        timestamp = null;
    }
}
