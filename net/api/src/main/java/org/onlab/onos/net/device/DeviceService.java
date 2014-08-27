package org.onlab.onos.net.device;

import org.onlab.onos.net.Device;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.MastershipRole;
import org.onlab.onos.net.Port;
import org.onlab.onos.net.PortNumber;

import java.util.List;

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
     * Returns a collection of the currently known infrastructure
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


    /**
     * Returns the list of ports associated with the device.
     *
     * @param deviceId device identifier
     * @return list of ports
     */
    List<Port> getPorts(DeviceId deviceId);

    /**
     * Returns the port with the specified number and hosted by the given device.
     * @param deviceId device identifier
     * @param portNumber port number
     * @return device port
     */
    Port getPort(DeviceId deviceId, PortNumber portNumber);

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
