package org.onlab.onos.net.device;

import org.onlab.onos.net.DeviceId;

/**
 * Service for administering the inventory of infrastructure devices.
 */
public interface DeviceAdminService {

    /**
     * Removes the device with the specified identifier.
     *
     * @param deviceId device identifier
     */
    void removeDevice(DeviceId deviceId);

    // TODO: add ability to administratively suspend/resume device

}
