package org.onlab.onos.net.device;

import org.onlab.onos.net.Device;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.Port;
import org.onlab.onos.net.PortNumber;
import org.onlab.onos.net.provider.ProviderId;
import org.onlab.onos.store.Store;

import java.util.List;

/**
 * Manages inventory of infrastructure devices; not intended for direct use.
 */
public interface DeviceStore extends Store<DeviceEvent, DeviceStoreDelegate> {

    /**
     * Returns the number of devices known to the system.
     *
     * @return number of devices
     */
    int getDeviceCount();

    /**
     * Returns an iterable collection of all devices known to the system.
     *
     * @return device collection
     */
    Iterable<Device> getDevices();

    /**
     * Returns the device with the specified identifier.
     *
     * @param deviceId device identifier
     * @return device
     */
    Device getDevice(DeviceId deviceId);

    /**
     * Creates a new infrastructure device, or updates an existing one using
     * the supplied device description.
     *
     * @param providerId        provider identifier
     * @param deviceId          device identifier
     * @param deviceDescription device description
     * @return ready to send event describing what occurred; null if no change
     */
    DeviceEvent createOrUpdateDevice(ProviderId providerId, DeviceId deviceId,
                                     DeviceDescription deviceDescription);

    /**
     * Removes the specified infrastructure device.
     *
     * @param deviceId device identifier
     * @return ready to send event describing what occurred; null if no change
     */
    DeviceEvent markOffline(DeviceId deviceId);

    /**
     * Updates the ports of the specified infrastructure device using the given
     * list of port descriptions. The list is assumed to be comprehensive.
     *
     * @param deviceId         device identifier
     * @param portDescriptions list of port descriptions
     * @return ready to send events describing what occurred; empty list if no change
     */
    List<DeviceEvent> updatePorts(DeviceId deviceId,
                                  List<PortDescription> portDescriptions);

    /**
     * Updates the port status of the specified infrastructure device using the
     * given port description.
     *
     * @param deviceId        device identifier
     * @param portDescription port description
     * @return ready to send event describing what occurred; null if no change
     */
    DeviceEvent updatePortStatus(DeviceId deviceId,
                                 PortDescription portDescription);

    /**
     * Returns the list of ports that belong to the specified device.
     *
     * @param deviceId device identifier
     * @return list of device ports
     */
    List<Port> getPorts(DeviceId deviceId);

    /**
     * Returns the specified device port.
     *
     * @param deviceId   device identifier
     * @param portNumber port number
     * @return device port
     */
    Port getPort(DeviceId deviceId, PortNumber portNumber);

    /**
     * Indicates whether the specified device is available/online.
     *
     * @param deviceId device identifier
     * @return true if device is available
     */
    boolean isAvailable(DeviceId deviceId);

    /**
     * Administratively removes the specified device from the store.
     *
     * @param deviceId device to be removed
     */
    DeviceEvent removeDevice(DeviceId deviceId);
}
