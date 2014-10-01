package org.onlab.onos.store.device.impl;

import static com.google.common.base.Predicates.notNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.net.DefaultDevice;
import org.onlab.onos.net.DefaultPort;
import org.onlab.onos.net.Device;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.Port;
import org.onlab.onos.net.PortNumber;
import org.onlab.onos.net.device.DeviceDescription;
import org.onlab.onos.net.device.DeviceEvent;
import org.onlab.onos.net.device.DeviceStore;
import org.onlab.onos.net.device.DeviceStoreDelegate;
import org.onlab.onos.net.device.PortDescription;
import org.onlab.onos.net.provider.ProviderId;
import org.onlab.onos.store.AbstractStore;
import org.onlab.onos.store.ClockService;
import org.onlab.onos.store.Timestamp;
import org.slf4j.Logger;

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
import java.util.concurrent.ConcurrentMap;

import static com.google.common.base.Preconditions.checkArgument;
import static org.onlab.onos.net.device.DeviceEvent.Type.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Manages inventory of infrastructure devices using a protocol that takes into consideration
 * the order in which device events occur.
 */
@Component(immediate = true)
@Service
public class OnosDistributedDeviceStore
        extends AbstractStore<DeviceEvent, DeviceStoreDelegate>
        implements DeviceStore {

    private final Logger log = getLogger(getClass());

    public static final String DEVICE_NOT_FOUND = "Device with ID %s not found";

    private ConcurrentMap<DeviceId, VersionedValue<Device>> devices;
    private ConcurrentMap<DeviceId, Map<PortNumber, VersionedValue<Port>>> devicePorts;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClockService clockService;

    @Activate
    public void activate() {

        devices = new ConcurrentHashMap<>();
        devicePorts = new ConcurrentHashMap<>();

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public int getDeviceCount() {
        return devices.size();
    }

    @Override
    public Iterable<Device> getDevices() {
        Builder<Device> builder = ImmutableSet.builder();
        synchronized (this) {
            for (VersionedValue<Device> device : devices.values()) {
                builder.add(device.entity());
            }
            return builder.build();
        }
    }

    @Override
    public Device getDevice(DeviceId deviceId) {
        VersionedValue<Device> device = devices.get(deviceId);
        checkArgument(device != null, DEVICE_NOT_FOUND, deviceId);
        return device.entity();
    }

    @Override
    public DeviceEvent createOrUpdateDevice(ProviderId providerId, DeviceId deviceId,
                                            DeviceDescription deviceDescription) {
        Timestamp newTimestamp = clockService.getTimestamp(deviceId);
        VersionedValue<Device> device = devices.get(deviceId);

        if (device == null) {
            return createDevice(providerId, deviceId, deviceDescription, newTimestamp);
        }

        checkState(newTimestamp.compareTo(device.timestamp()) > 0,
                "Existing device has a timestamp in the future!");

        return updateDevice(providerId, device.entity(), deviceDescription, newTimestamp);
    }

    // Creates the device and returns the appropriate event if necessary.
    private DeviceEvent createDevice(ProviderId providerId, DeviceId deviceId,
                                     DeviceDescription desc, Timestamp timestamp) {
        Device device = new DefaultDevice(providerId, deviceId, desc.type(),
                                                 desc.manufacturer(),
                                                 desc.hwVersion(), desc.swVersion(),
                                                 desc.serialNumber());

        devices.put(deviceId, new VersionedValue<>(device, true, timestamp));
        // TODO,FIXME: broadcast a message telling peers of a device event.
        return new DeviceEvent(DEVICE_ADDED, device, null);
    }

    // Updates the device and returns the appropriate event if necessary.
    private DeviceEvent updateDevice(ProviderId providerId, Device device,
                                     DeviceDescription desc, Timestamp timestamp) {
        // We allow only certain attributes to trigger update
        if (!Objects.equals(device.hwVersion(), desc.hwVersion()) ||
                !Objects.equals(device.swVersion(), desc.swVersion())) {

            Device updated = new DefaultDevice(providerId, device.id(),
                                                      desc.type(),
                                                      desc.manufacturer(),
                                                      desc.hwVersion(),
                                                      desc.swVersion(),
                                                      desc.serialNumber());
            devices.put(device.id(), new VersionedValue<Device>(updated, true, timestamp));
            // FIXME: broadcast a message telling peers of a device event.
            return new DeviceEvent(DeviceEvent.Type.DEVICE_UPDATED, updated, null);
        }

        // Otherwise merely attempt to change availability
        Device updated = new DefaultDevice(providerId, device.id(),
                desc.type(),
                desc.manufacturer(),
                desc.hwVersion(),
                desc.swVersion(),
                desc.serialNumber());

        VersionedValue<Device> oldDevice = devices.put(device.id(),
                new VersionedValue<Device>(updated, true, timestamp));
        if (!oldDevice.isUp()) {
            return new DeviceEvent(DEVICE_AVAILABILITY_CHANGED, device, null);
        } else {
            return null;
        }
    }

    @Override
    public DeviceEvent markOffline(DeviceId deviceId) {
        VersionedValue<Device> device = devices.get(deviceId);
        boolean willRemove = device != null && device.isUp();
        if (!willRemove) {
            return null;
        }
        Timestamp timestamp = clockService.getTimestamp(deviceId);
        if (replaceIfLatest(device.entity(), false, timestamp)) {
            return new DeviceEvent(DEVICE_AVAILABILITY_CHANGED, device.entity(), null);
        }
        return null;
    }

    // Replace existing value if its timestamp is older.
    private synchronized boolean replaceIfLatest(Device device, boolean isUp, Timestamp timestamp) {
        VersionedValue<Device> existingValue = devices.get(device.id());
        if (timestamp.compareTo(existingValue.timestamp()) > 0) {
            devices.put(device.id(), new VersionedValue<Device>(device, isUp, timestamp));
            return true;
        }
        return false;
    }

    @Override
    public List<DeviceEvent> updatePorts(DeviceId deviceId,
                                         List<PortDescription> portDescriptions) {
        List<DeviceEvent> events = new ArrayList<>();
        synchronized (this) {
            VersionedValue<Device> device = devices.get(deviceId);
            checkArgument(device != null, DEVICE_NOT_FOUND, deviceId);
            Map<PortNumber, VersionedValue<Port>> ports = getPortMap(deviceId);
            Timestamp newTimestamp = clockService.getTimestamp(deviceId);

            // Add new ports
            Set<PortNumber> processed = new HashSet<>();
            for (PortDescription portDescription : portDescriptions) {
                VersionedValue<Port> port = ports.get(portDescription.portNumber());
                if (port == null) {
                    events.add(createPort(device, portDescription, ports, newTimestamp));
                }
                checkState(newTimestamp.compareTo(port.timestamp()) > 0,
                        "Existing port state has a timestamp in the future!");
                events.add(updatePort(device.entity(), port.entity(), portDescription, ports, newTimestamp));
                processed.add(portDescription.portNumber());
            }

            updatePortMap(deviceId, ports);

            events.addAll(pruneOldPorts(device.entity(), ports, processed));
        }
        return FluentIterable.from(events).filter(notNull()).toList();
    }

    // Creates a new port based on the port description adds it to the map and
    // Returns corresponding event.
    //@GuardedBy("this")
    private DeviceEvent createPort(VersionedValue<Device> device, PortDescription portDescription,
                                   Map<PortNumber, VersionedValue<Port>> ports, Timestamp timestamp) {
        Port port = new DefaultPort(device.entity(), portDescription.portNumber(),
                                           portDescription.isEnabled());
        ports.put(port.number(), new VersionedValue<Port>(port, true, timestamp));
        updatePortMap(device.entity().id(), ports);
        return new DeviceEvent(PORT_ADDED, device.entity(), port);
    }

    // Checks if the specified port requires update and if so, it replaces the
    // existing entry in the map and returns corresponding event.
    //@GuardedBy("this")
    private DeviceEvent updatePort(Device device, Port port,
                                   PortDescription portDescription,
                                   Map<PortNumber, VersionedValue<Port>> ports,
                                   Timestamp timestamp) {
        if (port.isEnabled() != portDescription.isEnabled()) {
            VersionedValue<Port> updatedPort = new VersionedValue<Port>(
                    new DefaultPort(device, portDescription.portNumber(),
                                    portDescription.isEnabled()),
                    portDescription.isEnabled(),
                    timestamp);
            ports.put(port.number(), updatedPort);
            updatePortMap(device.id(), ports);
            return new DeviceEvent(PORT_UPDATED, device, updatedPort.entity());
        }
        return null;
    }

    // Prunes the specified list of ports based on which ports are in the
    // processed list and returns list of corresponding events.
    //@GuardedBy("this")
    private List<DeviceEvent> pruneOldPorts(Device device,
                                            Map<PortNumber, VersionedValue<Port>> ports,
                                            Set<PortNumber> processed) {
        List<DeviceEvent> events = new ArrayList<>();
        Iterator<PortNumber> iterator = ports.keySet().iterator();
        while (iterator.hasNext()) {
            PortNumber portNumber = iterator.next();
            if (!processed.contains(portNumber)) {
                events.add(new DeviceEvent(PORT_REMOVED, device,
                                           ports.get(portNumber).entity()));
                iterator.remove();
            }
        }
        if (!events.isEmpty()) {
            updatePortMap(device.id(), ports);
        }
        return events;
    }

    // Gets the map of ports for the specified device; if one does not already
    // exist, it creates and registers a new one.
    // WARN: returned value is a copy, changes made to the Map
    //       needs to be written back using updatePortMap
    //@GuardedBy("this")
    private Map<PortNumber, VersionedValue<Port>> getPortMap(DeviceId deviceId) {
        Map<PortNumber, VersionedValue<Port>> ports = devicePorts.get(deviceId);
        if (ports == null) {
            ports = new HashMap<>();
            // this probably is waste of time in most cases.
            updatePortMap(deviceId, ports);
        }
        return ports;
    }

    //@GuardedBy("this")
    private void updatePortMap(DeviceId deviceId, Map<PortNumber, VersionedValue<Port>> ports) {
        devicePorts.put(deviceId, ports);
    }

    @Override
    public DeviceEvent updatePortStatus(DeviceId deviceId,
                                        PortDescription portDescription) {
        VersionedValue<Device> device = devices.get(deviceId);
        checkArgument(device != null, DEVICE_NOT_FOUND, deviceId);
        Map<PortNumber, VersionedValue<Port>> ports = getPortMap(deviceId);
        VersionedValue<Port> port = ports.get(portDescription.portNumber());
        Timestamp timestamp = clockService.getTimestamp(deviceId);
        return updatePort(device.entity(), port.entity(), portDescription, ports, timestamp);
    }

    @Override
    public List<Port> getPorts(DeviceId deviceId) {
        Map<PortNumber, VersionedValue<Port>> versionedPorts = devicePorts.get(deviceId);
        if (versionedPorts == null) {
            return Collections.emptyList();
        }
        List<Port> ports = new ArrayList<>();
        for (VersionedValue<Port> port : versionedPorts.values()) {
            ports.add(port.entity());
        }
        return ports;
    }

    @Override
    public Port getPort(DeviceId deviceId, PortNumber portNumber) {
        Map<PortNumber, VersionedValue<Port>> ports = devicePorts.get(deviceId);
        return ports == null ? null : ports.get(portNumber).entity();
    }

    @Override
    public boolean isAvailable(DeviceId deviceId) {
        return devices.get(deviceId).isUp();
    }

    @Override
    public DeviceEvent removeDevice(DeviceId deviceId) {
        VersionedValue<Device> previousDevice = devices.remove(deviceId);
        return previousDevice == null ? null :
            new DeviceEvent(DEVICE_REMOVED, previousDevice.entity(), null);
    }
}
