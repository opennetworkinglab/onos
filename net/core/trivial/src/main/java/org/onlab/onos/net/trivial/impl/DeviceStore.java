package org.onlab.onos.net.trivial.impl;

import org.onlab.onos.net.Device;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.Port;
import org.onlab.onos.net.PortNumber;
import org.onlab.onos.net.device.DeviceDescription;
import org.onlab.onos.net.device.DeviceEvent;
import org.onlab.onos.net.device.PortDescription;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages inventory of infrastructure devices.
 */
public class DeviceStore {

    private final Map<DeviceId, Device> devices = new ConcurrentHashMap<>();

    /**
     * Creates a new infrastructure device, or updates an existing one using
     * the supplied device description.
     *
     * @param deviceId          device identifier
     * @param deviceDescription device description
     * @return ready to send event describing what occurred; null if no change
     */
    public DeviceEvent createOrUpdateDevice(DeviceId deviceId,
                                            DeviceDescription deviceDescription) {
        return null;
    }

    /**
     * Removes the specified infrastructure device.
     *
     * @param deviceId device identifier
     * @return ready to send event describing what occurred; null if no change
     */
    public DeviceEvent removeDevice(DeviceId deviceId) {
        return null;
    }

    /**
     * Updates the ports of the specified infrastructure device using the given
     * list of port descriptions. The list is assumed to be comprehensive.
     *
     * @param deviceId         device identifier
     * @param portDescriptions list of port descriptions
     * @return ready to send events describing what occurred; empty list if no change
     */
    public List<DeviceEvent> updatePorts(DeviceId deviceId,
                                         List<PortDescription> portDescriptions) {
        return new ArrayList<>();
    }

    /**
     * Updates the port status of the specified infrastructure device using the
     * given port description.
     *
     * @param deviceId        device identifier
     * @param portDescription port description
     * @return ready to send event describing what occurred; null if no change
     */
    public DeviceEvent updatePortStatus(DeviceId deviceId,
                                        PortDescription portDescription) {
        return null;
    }

    /**
     * Returns the device with the specified identifier.
     *
     * @param deviceId device identifier
     * @return device
     */
    public Device getDevice(DeviceId deviceId) {
        return null;
    }

    /**
     * Returns the list of ports that belong to the specified device.
     *
     * @param deviceId device identifier
     * @return list of device ports
     */
    public List<Port> getPorts(DeviceId deviceId) {
        return null;
    }

    /**
     * Returns the specified device port.
     *
     * @param deviceId   device identifier
     * @param portNumber port number
     * @return device port
     */
    public Port getPort(DeviceId deviceId, PortNumber portNumber) {
        return null;
    }
}
