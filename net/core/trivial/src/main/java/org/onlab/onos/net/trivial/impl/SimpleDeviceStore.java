package org.onlab.onos.net.trivial.impl;

import com.google.common.collect.ImmutableList;
import org.onlab.onos.net.DefaultDevice;
import org.onlab.onos.net.DefaultPort;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkArgument;
import static org.onlab.onos.net.device.DeviceEvent.Type.*;

/**
 * Manages inventory of infrastructure devices using trivial in-memory
 * implementation.
 */
class SimpleDeviceStore {

    public static final String DEVICE_NOT_FOUND = "Device with ID %s not found";

    private final Map<DeviceId, DefaultDevice> devices = new ConcurrentHashMap<>();
    private final Map<DeviceId, MastershipRole> roles = new ConcurrentHashMap<>();
    private final Set<DeviceId> availableDevices = new HashSet<>();
    private final Map<DeviceId, Map<PortNumber, Port>> devicePorts = new HashMap<>();

    /**
     * Returns the number of devices known to the system.
     *
     * @return number of devices
     */
    int getDeviceCount() {
        return devices.size();
    }

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
            availableDevices.add(deviceId);

            // For now claim the device as a master automatically.
            roles.put(deviceId, MastershipRole.MASTER);
        }
        return new DeviceEvent(DeviceEvent.Type.DEVICE_ADDED, device, null);
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
                availableDevices.add(device.id());
            }
            return new DeviceEvent(DeviceEvent.Type.DEVICE_UPDATED, device, null);
        }

        // Otherwise merely attempt to change availability
        synchronized (this) {
            boolean added = availableDevices.add(device.id());
            return !added ? null :
                    new DeviceEvent(DEVICE_AVAILABILITY_CHANGED, device, null);
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
            checkArgument(device != null, DEVICE_NOT_FOUND, deviceId);
            boolean removed = availableDevices.remove(deviceId);
            return !removed ? null :
                    new DeviceEvent(DEVICE_AVAILABILITY_CHANGED, device, null);
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
        List<DeviceEvent> events = new ArrayList<>();
        synchronized (this) {
            Device device = devices.get(deviceId);
            checkArgument(device != null, DEVICE_NOT_FOUND, deviceId);
            Map<PortNumber, Port> ports = getPortMap(deviceId);

            // Add new ports
            Set<PortNumber> processed = new HashSet<>();
            for (PortDescription portDescription : portDescriptions) {
                Port port = ports.get(portDescription.portNumber());
                events.add(port == null ?
                                   createPort(device, portDescription, ports) :
                                   updatePort(device, port, portDescription, ports));
                processed.add(portDescription.portNumber());
            }

            events.addAll(pruneOldPorts(device, ports, processed));
        }
        return events;
    }

    // Creates a new port based on the port description adds it to the map and
    // Returns corresponding event.
    private DeviceEvent createPort(Device device, PortDescription portDescription,
                                   Map<PortNumber, Port> ports) {
        DefaultPort port = new DefaultPort(device, portDescription.portNumber(),
                                           portDescription.isEnabled());
        ports.put(port.number(), port);
        return new DeviceEvent(PORT_ADDED, device, port);
    }

    // CHecks if the specified port requires update and if so, it replaces the
    // existing entry in the map and returns corresponding event.
    private DeviceEvent updatePort(Device device, Port port,
                                   PortDescription portDescription,
                                   Map<PortNumber, Port> ports) {
        if (port.isEnabled() != portDescription.isEnabled()) {
            DefaultPort updatedPort =
                    new DefaultPort(device, portDescription.portNumber(),
                                    portDescription.isEnabled());
            ports.put(port.number(), updatedPort);
            return new DeviceEvent(PORT_UPDATED, device, port);
        }
        return null;
    }

    // Prunes the specified list of ports based on which ports are in the
    // processed list and returns list of corresponding events.
    private List<DeviceEvent> pruneOldPorts(Device device,
                                            Map<PortNumber, Port> ports,
                                            Set<PortNumber> processed) {
        List<DeviceEvent> events = new ArrayList<>();
        Iterator<PortNumber> iterator = ports.keySet().iterator();
        while (iterator.hasNext()) {
            PortNumber portNumber = iterator.next();
            if (!processed.contains(portNumber)) {
                events.add(new DeviceEvent(PORT_REMOVED, device,
                                           ports.get(portNumber)));
                iterator.remove();
            }
        }
        return events;
    }

    // Gets the map of ports for the specified device; if one does not already
    // exist, it creates and registers a new one.
    private Map<PortNumber, Port> getPortMap(DeviceId deviceId) {
        Map<PortNumber, Port> ports = devicePorts.get(deviceId);
        if (ports == null) {
            ports = new HashMap<>();
            devicePorts.put(deviceId, ports);
        }
        return ports;
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
        synchronized (this) {
            Device device = devices.get(deviceId);
            checkArgument(device != null, DEVICE_NOT_FOUND, deviceId);
            Map<PortNumber, Port> ports = getPortMap(deviceId);
            Port port = ports.get(portDescription.portNumber());
            return updatePort(device, port, portDescription, ports);
        }
    }

    /**
     * Returns the list of ports that belong to the specified device.
     *
     * @param deviceId device identifier
     * @return list of device ports
     */
    List<Port> getPorts(DeviceId deviceId) {
        Map<PortNumber, Port> ports = devicePorts.get(deviceId);
        return ports == null ? new ArrayList<Port>() : ImmutableList.copyOf(ports.values());
    }

    /**
     * Returns the specified device port.
     *
     * @param deviceId   device identifier
     * @param portNumber port number
     * @return device port
     */
    Port getPort(DeviceId deviceId, PortNumber portNumber) {
        Map<PortNumber, Port> ports = devicePorts.get(deviceId);
        return ports == null ? null : ports.get(portNumber);
    }

    /**
     * Indicates whether the specified device is available/online.
     *
     * @param deviceId device identifier
     * @return true if device is available
     */
    boolean isAvailable(DeviceId deviceId) {
        return availableDevices.contains(deviceId);
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
        synchronized (this) {
            Device device = getDevice(deviceId);
            checkArgument(device != null, DEVICE_NOT_FOUND, deviceId);
            MastershipRole oldRole = roles.put(deviceId, role);
            return oldRole == role ? null :
                    new DeviceEvent(DEVICE_MASTERSHIP_CHANGED, device, null);
        }
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
            return device == null ? null :
                    new DeviceEvent(DEVICE_REMOVED, device, null);
        }
    }
}
