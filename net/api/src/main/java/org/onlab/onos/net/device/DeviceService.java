package org.onlab.onos.net.device;

import org.onlab.onos.net.Device;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.MastershipRole;

/**
 * Service for interacting with the inventory of infrastructure devices.
 */
public interface DeviceService {

    /**
     * Returns the current mastership role for the specified device.
     *
     * @param deviceId device identifier
     * @return designated mastership role
     */
    MastershipRole getRole(DeviceId deviceId);

    /**
     * Returns an iterable collection of the currently known infrastructure
     * devices.
     *
     * @return collection of devices
     */
    Iterable<Device> getDevices();

    /**
     * Returns the device with the specified identifier.
     *
     * @param deviceId device identifier
     * @return device or null if one with the given identifier is not known
     */
    Device getDevice(DeviceId deviceId);


//    List<Port> getPorts(DeviceId deviceId);

    /**
     * Adds the specified device listener.
     *
     * @param listener device listener
     */
    void addListener(DeviceListener listener);

    /**
     * Removes the specified device listener.
     *
     * @param listener device listener
     */
    void removeListener(DeviceListener listener);
}
