package org.onlab.onos.net.trivial.impl;

import org.onlab.onos.net.DefaultDevice;
import org.onlab.onos.net.Device;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.MastershipRole;
import org.onlab.onos.net.Port;
import org.onlab.onos.net.PortNumber;
import org.onlab.onos.net.device.DeviceDescription;
import org.onlab.onos.net.device.DeviceEvent;
import org.onlab.onos.net.device.PortDescription;
import org.onlab.onos.net.provider.ProviderId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkArgument;
import static org.onlab.onos.net.device.DeviceEvent.Type.DEVICE_AVAILABILITY_CHANGED;
import static org.onlab.onos.net.device.DeviceEvent.Type.DEVICE_MASTERSHIP_CHANGED;
import static org.onlab.onos.net.device.DeviceEvent.Type.DEVICE_REMOVED;

/**
 * Manages inventory of infrastructure devices.
 */
class SimpleDeviceStore {

    private final Map<DeviceId, DefaultDevice> devices = new ConcurrentHashMap<>();
    private final Set<DeviceId> available = new HashSet<>();
    private final Map<DeviceId, MastershipRole> roles = new HashMap<>();

    /**
     * Returns an iterable collection of all devices known to the system.
     *
     * @return device collection
     */
    Iterable<Device> getDevices() {
        return Collections.unmodifiableSet(new HashSet<Device>(devices.values()));
    }

    /**
     * Returns the device with the specified identifier.
     *
     * @param deviceId device identifier
     * @return device
     */
    Device getDevice(DeviceId deviceId) {
        return devices.get(deviceId);
    }

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
                                     DeviceDescription deviceDescription) {
        DefaultDevice device = devices.get(deviceId);
        if (device == null) {
            return createDevice(providerId, deviceId, deviceDescription);
        }
        return updateDevice(providerId, device, deviceDescription);
    }

    // Creates the device and returns the appropriate event if necessary.
    private DeviceEvent createDevice(ProviderId providerId, DeviceId deviceId,
                                     DeviceDescription desc) {
        DefaultDevice device = new DefaultDevice(providerId, deviceId, desc.type(),
                                                 desc.manufacturer(),
                                                 desc.hwVersion(), desc.swVersion(),
                                                 desc.serialNumber());
        synchronized (this) {
            devices.put(deviceId, device);
            available.add(deviceId);
        }
        return new DeviceEvent(DeviceEvent.Type.DEVICE_ADDED, device);
    }

    // Updates the device and returns the appropriate event if necessary.
    private DeviceEvent updateDevice(ProviderId providerId, DefaultDevice device,
                                     DeviceDescription desc) {
        // We allow only certain attributes to trigger update
        if (!Objects.equals(device.hwVersion(), desc.hwVersion()) ||
                !Objects.equals(device.swVersion(), desc.swVersion())) {
            DefaultDevice updated = new DefaultDevice(providerId, device.id(),
                                                      desc.type(),
                                                      desc.manufacturer(),
                                                      desc.hwVersion(),
                                                      desc.swVersion(),
                                                      desc.serialNumber());
            synchronized (this) {
                devices.put(device.id(), updated);
                available.add(device.id());
            }
            return new DeviceEvent(DeviceEvent.Type.DEVICE_UPDATED, device);
        }

        // Otherwise merely attempt to change availability
        synchronized (this) {
            boolean added = available.add(device.id());
            return added ? new DeviceEvent(DEVICE_AVAILABILITY_CHANGED, device) : null;
        }
    }

    /**
     * Removes the specified infrastructure device.
     *
     * @param deviceId device identifier
     * @return ready to send event describing what occurred; null if no change
     */
    DeviceEvent markOffline(DeviceId deviceId) {
        synchronized (this) {
            Device device = devices.get(deviceId);
            checkArgument(device != null, "Device with ID %s is not found", deviceId);
            boolean removed = available.remove(deviceId);
            return removed ? new DeviceEvent(DEVICE_AVAILABILITY_CHANGED, device) : null;
        }
    }

    /**
     * Updates the ports of the specified infrastructure device using the given
     * list of port descriptions. The list is assumed to be comprehensive.
     *
     * @param deviceId         device identifier
     * @param portDescriptions list of port descriptions
     * @return ready to send events describing what occurred; empty list if no change
     */
    List<DeviceEvent> updatePorts(DeviceId deviceId,
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
    DeviceEvent updatePortStatus(DeviceId deviceId,
                                 PortDescription portDescription) {
        return null;
    }

    /**
     * Returns the list of ports that belong to the specified device.
     *
     * @param deviceId device identifier
     * @return list of device ports
     */
    List<Port> getPorts(DeviceId deviceId) {
        return null;
    }

    /**
     * Returns the specified device port.
     *
     * @param deviceId   device identifier
     * @param portNumber port number
     * @return device port
     */
    Port getPort(DeviceId deviceId, PortNumber portNumber) {
        return null;
    }

    /**
     * Returns the mastership role determined for this device.
     *
     * @param deviceId device identifier
     * @return mastership role
     */
    MastershipRole getRole(DeviceId deviceId) {
        MastershipRole role = roles.get(deviceId);
        return role != null ? role : MastershipRole.NONE;
    }

    /**
     * Administratively sets the role of the specified device.
     *
     * @param deviceId device identifier
     * @param role     mastership role to apply
     * @return mastership role change event or null if no change
     */
    DeviceEvent setRole(DeviceId deviceId, MastershipRole role) {
        Device device = getDevice(deviceId);
        checkArgument(device != null, "Device with ID %s not found");
        MastershipRole oldRole = roles.put(deviceId, role);
        return oldRole == role ? null : new DeviceEvent(DEVICE_MASTERSHIP_CHANGED, device);
    }

    /**
     * Administratively removes the specified device from the store.
     *
     * @param deviceId device to be removed
     */
    DeviceEvent removeDevice(DeviceId deviceId) {
        synchronized (this) {
            roles.remove(deviceId);
            Device device = devices.remove(deviceId);
            return device != null ? new DeviceEvent(DEVICE_REMOVED, device) : null;
        }
    }
}
