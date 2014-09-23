package org.onlab.onos.cluster;

import java.util.Set;

import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.MastershipRole;

/**
 * Service responsible for determining the controller instance mastership of
 * a device in a clustered environment. This is the central authority for
 * determining mastership, but is not responsible for actually applying it
 * to the devices; this falls on the device service.
 */
public interface MastershipService {

    /**
     * Returns the current master for a given device.
     *
     * @param deviceId the identifier of the device
     * @return the ID of the master controller for the device
     */
    InstanceId getMasterFor(DeviceId deviceId);

    /**
     * Returns the devices for which a controller is master.
     *
     * @param instanceId the ID of the controller
     * @return a set of device IDs
     */
    Set<DeviceId> getDevicesOf(InstanceId instanceId);

    /**
     * Returns the mastership status of this controller for a given device.
     *
     * @param deviceId the the identifier of the device
     * @return the role of this controller instance
     */
    MastershipRole requestRoleFor(DeviceId deviceId);

    /**
     * Adds the specified mastership listener.
     *
     * @param listener the mastership listener
     */
    void addListener(MastershipListener listener);

    /**
     * Removes the specified device listener.
     *
     * @param listener the mastership listener
     */
    void removeListener(MastershipListener listener);

}
