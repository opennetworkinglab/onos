package org.onlab.onos.net.device;

import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.MastershipRole;

/**
 * Service for administering the inventory of infrastructure devices.
 */
public interface DeviceAdminService {

    /**
     * Applies the current mastership role for the specified device.
     *
     * @param deviceId device identifier
     * @param role     requested role
     */
    void setRole(DeviceId deviceId, MastershipRole role);

    /**
     * Removes the device with the specified identifier.
     *
     * @param deviceId device identifier
     */
    void removeDevice(DeviceId deviceId);

    // TODO: add ability to administratively suspend/resume device

}
