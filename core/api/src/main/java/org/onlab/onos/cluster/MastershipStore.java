package org.onlab.onos.cluster;

import java.util.Set;

import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.MastershipRole;

/**
 * Manages inventory of mastership roles for devices, across controller instances.
 */
public interface MastershipStore {

    // three things to map: InstanceId, DeviceId, MastershipRole

    /**
     * Sets a device's role for a specified controller instance.
     *
     * @param instance controller instance identifier
     * @param deviceId device identifier
     * @param role     new role
     * @return a mastership event
     */
    MastershipEvent setRole(InstanceId instance, DeviceId deviceId,
                            MastershipRole role);

    /**
     * Adds or updates the mastership information for a device.
     *
     * @param instance controller instance identifier
     * @param deviceId device identifier
     * @param role     new role
     * @return a mastership event
     */
    MastershipEvent addOrUpdateDevice(InstanceId instance, DeviceId deviceId,
                                      MastershipRole role);

    /**
     * Returns the master for a device.
     *
     * @param deviceId the device identifier
     * @return the instance identifier of the master
     */
    InstanceId getMaster(DeviceId deviceId);

    /**
     * Returns the devices that a controller instance is master of.
     *
     * @param instanceId the instance identifier
     * @return a set of device identifiers
     */
    Set<DeviceId> getDevices(InstanceId instanceId);

    /**
     * Returns the role of a device for a specific controller instance.
     *
     * @param instanceId the instance identifier
     * @param deviceId   the device identifiers
     * @return the role
     */
    MastershipRole getRole(InstanceId instanceId, DeviceId deviceId);
}
