package org.onlab.onos.store.device.impl;

import static com.google.common.base.Predicates.notNull;

import com.google.common.base.Optional;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.hazelcast.core.IMap;
import com.hazelcast.core.ISet;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
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
import org.onlab.onos.store.common.AbsentInvalidatingLoadingCache;
import org.onlab.onos.store.common.AbstractHazelcastStore;
import org.onlab.onos.store.common.OptionalCacheLoader;
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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.cache.CacheBuilder.newBuilder;
import static org.onlab.onos.net.device.DeviceEvent.Type.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Manages inventory of infrastructure devices using Hazelcast-backed map.
 */
@Component(immediate = true)
@Service
public class DistributedDeviceStore
        extends AbstractHazelcastStore<DeviceEvent, DeviceStoreDelegate>
        implements DeviceStore {

    private final Logger log = getLogger(getClass());

    public static final String DEVICE_NOT_FOUND = "Device with ID %s not found";

    // private IMap<DeviceId, DefaultDevice> cache;
    private IMap<byte[], byte[]> rawDevices;
    private LoadingCache<DeviceId, Optional<DefaultDevice>> devices;

    // private ISet<DeviceId> availableDevices;
    private ISet<byte[]> availableDevices;

    // TODO DevicePorts is very inefficient consider restructuring.
    // private IMap<DeviceId, Map<PortNumber, Port>> devicePorts;
    private IMap<byte[], byte[]> rawDevicePorts;
    private LoadingCache<DeviceId, Optional<Map<PortNumber, Port>>> devicePorts;

    private String devicesListener;

    private String portsListener;

    @Override
    @Activate
    public void activate() {
        super.activate();

        // IMap event handler needs value
        final boolean includeValue = true;

        // TODO decide on Map name scheme to avoid collision
        rawDevices = theInstance.getMap("devices");
        final OptionalCacheLoader<DeviceId, DefaultDevice> deviceLoader
                = new OptionalCacheLoader<>(kryoSerializationService, rawDevices);
        devices = new AbsentInvalidatingLoadingCache<>(newBuilder().build(deviceLoader));
        // refresh/populate cache based on notification from other instance
        devicesListener = rawDevices.addEntryListener(new RemoteDeviceEventHandler(devices), includeValue);

        // TODO cache availableDevices
        availableDevices = theInstance.getSet("availableDevices");

        rawDevicePorts = theInstance.getMap("devicePorts");
        final OptionalCacheLoader<DeviceId, Map<PortNumber, Port>> devicePortLoader
                = new OptionalCacheLoader<>(kryoSerializationService, rawDevicePorts);
        devicePorts = new AbsentInvalidatingLoadingCache<>(newBuilder().build(devicePortLoader));
        // refresh/populate cache based on notification from other instance
        portsListener = rawDevicePorts.addEntryListener(new RemotePortEventHandler(devicePorts), includeValue);

        loadDeviceCache();
        loadDevicePortsCache();

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        rawDevicePorts.removeEntryListener(portsListener);
        rawDevices.removeEntryListener(devicesListener);
        log.info("Stopped");
    }

    @Override
    public int getDeviceCount() {
        return devices.asMap().size();
    }

    @Override
    public Iterable<Device> getDevices() {
        // TODO builder v.s. copyOf. Guava semms to be using copyOf?
        Builder<Device> builder = ImmutableSet.builder();
        for (Optional<DefaultDevice> e : devices.asMap().values()) {
            if (e.isPresent()) {
                builder.add(e.get());
            }
        }
        return builder.build();
    }

    private void loadDeviceCache() {
        for (byte[] keyBytes : rawDevices.keySet()) {
            final DeviceId id = deserialize(keyBytes);
            devices.refresh(id);
        }
    }

    private void loadDevicePortsCache() {
        for (byte[] keyBytes : rawDevicePorts.keySet()) {
            final DeviceId id = deserialize(keyBytes);
            devicePorts.refresh(id);
        }
    }

    @Override
    public Device getDevice(DeviceId deviceId) {
        // TODO revisit if ignoring exception is safe.
        return devices.getUnchecked(deviceId).orNull();
    }

    @Override
    public DeviceEvent createOrUpdateDevice(ProviderId providerId, DeviceId deviceId,
                                            DeviceDescription deviceDescription) {
        DefaultDevice device = devices.getUnchecked(deviceId).orNull();
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
            final byte[] deviceIdBytes = serialize(deviceId);
            rawDevices.put(deviceIdBytes, serialize(device));
            devices.put(deviceId, Optional.of(device));

            availableDevices.add(deviceIdBytes);
        }
        return new DeviceEvent(DEVICE_ADDED, device, null);
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
                final byte[] deviceIdBytes = serialize(device.id());
                rawDevices.put(deviceIdBytes, serialize(updated));
                devices.put(device.id(), Optional.of(updated));
                availableDevices.add(serialize(device.id()));
            }
            return new DeviceEvent(DeviceEvent.Type.DEVICE_UPDATED, updated, null);
        }

        // Otherwise merely attempt to change availability
        synchronized (this) {
            boolean added = availableDevices.add(serialize(device.id()));
            return !added ? null :
                    new DeviceEvent(DEVICE_AVAILABILITY_CHANGED, device, null);
        }
    }

    @Override
    public DeviceEvent markOffline(DeviceId deviceId) {
        synchronized (this) {
            Device device = devices.getUnchecked(deviceId).orNull();
            boolean removed = device != null && availableDevices.remove(serialize(deviceId));
            return !removed ? null :
                    new DeviceEvent(DEVICE_AVAILABILITY_CHANGED, device, null);
        }
    }

    @Override
    public List<DeviceEvent> updatePorts(DeviceId deviceId,
                                         List<PortDescription> portDescriptions) {
        List<DeviceEvent> events = new ArrayList<>();
        synchronized (this) {
            Device device = devices.getUnchecked(deviceId).orNull();
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

            updatePortMap(deviceId, ports);

            events.addAll(pruneOldPorts(device, ports, processed));
        }
        return FluentIterable.from(events).filter(notNull()).toList();
    }

    // Creates a new port based on the port description adds it to the map and
    // Returns corresponding event.
    //@GuardedBy("this")
    private DeviceEvent createPort(Device device, PortDescription portDescription,
                                   Map<PortNumber, Port> ports) {
        DefaultPort port = new DefaultPort(device, portDescription.portNumber(),
                                           portDescription.isEnabled());
        ports.put(port.number(), port);
        updatePortMap(device.id(), ports);
        return new DeviceEvent(PORT_ADDED, device, port);
    }

    // Checks if the specified port requires update and if so, it replaces the
    // existing entry in the map and returns corresponding event.
    //@GuardedBy("this")
    private DeviceEvent updatePort(Device device, Port port,
                                   PortDescription portDescription,
                                   Map<PortNumber, Port> ports) {
        if (port.isEnabled() != portDescription.isEnabled()) {
            DefaultPort updatedPort =
                    new DefaultPort(device, portDescription.portNumber(),
                                    portDescription.isEnabled());
            ports.put(port.number(), updatedPort);
            updatePortMap(device.id(), ports);
            return new DeviceEvent(PORT_UPDATED, device, updatedPort);
        }
        return null;
    }

    // Prunes the specified list of ports based on which ports are in the
    // processed list and returns list of corresponding events.
    //@GuardedBy("this")
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
    private Map<PortNumber, Port> getPortMap(DeviceId deviceId) {
        Map<PortNumber, Port> ports = devicePorts.getUnchecked(deviceId).orNull();
        if (ports == null) {
            ports = new HashMap<>();
            // this probably is waste of time in most cases.
            updatePortMap(deviceId, ports);
        }
        return ports;
    }

    //@GuardedBy("this")
    private void updatePortMap(DeviceId deviceId, Map<PortNumber, Port> ports) {
        rawDevicePorts.put(serialize(deviceId), serialize(ports));
        devicePorts.put(deviceId, Optional.of(ports));
    }

    @Override
    public DeviceEvent updatePortStatus(DeviceId deviceId,
                                        PortDescription portDescription) {
        synchronized (this) {
            Device device = devices.getUnchecked(deviceId).orNull();
            checkArgument(device != null, DEVICE_NOT_FOUND, deviceId);
            Map<PortNumber, Port> ports = getPortMap(deviceId);
            Port port = ports.get(portDescription.portNumber());
            return updatePort(device, port, portDescription, ports);
        }
    }

    @Override
    public List<Port> getPorts(DeviceId deviceId) {
        Map<PortNumber, Port> ports = devicePorts.getUnchecked(deviceId).orNull();
        return ports == null ? Collections.<Port>emptyList() : ImmutableList.copyOf(ports.values());
    }

    @Override
    public Port getPort(DeviceId deviceId, PortNumber portNumber) {
        Map<PortNumber, Port> ports = devicePorts.getUnchecked(deviceId).orNull();
        return ports == null ? null : ports.get(portNumber);
    }

    @Override
    public boolean isAvailable(DeviceId deviceId) {
        return availableDevices.contains(serialize(deviceId));
    }

    @Override
    public DeviceEvent removeDevice(DeviceId deviceId) {
        synchronized (this) {
            byte[] deviceIdBytes = serialize(deviceId);

            // TODO conditional remove?
            Device device = deserialize(rawDevices.remove(deviceIdBytes));
            devices.invalidate(deviceId);
            return device == null ? null :
                    new DeviceEvent(DEVICE_REMOVED, device, null);
        }
    }

    private class RemoteDeviceEventHandler extends RemoteCacheEventHandler<DeviceId, DefaultDevice> {
        public RemoteDeviceEventHandler(LoadingCache<DeviceId, Optional<DefaultDevice>> cache) {
            super(cache);
        }

        @Override
        protected void onAdd(DeviceId deviceId, DefaultDevice device) {
            notifyDelegate(new DeviceEvent(DEVICE_ADDED, device));
        }

        @Override
        protected void onRemove(DeviceId deviceId, DefaultDevice device) {
            notifyDelegate(new DeviceEvent(DEVICE_REMOVED, device));
        }

        @Override
        protected void onUpdate(DeviceId deviceId, DefaultDevice oldDevice, DefaultDevice device) {
            notifyDelegate(new DeviceEvent(DEVICE_UPDATED, device));
        }
    }

    private class RemotePortEventHandler extends RemoteCacheEventHandler<DeviceId, Map<PortNumber, Port>> {
        public RemotePortEventHandler(LoadingCache<DeviceId, Optional<Map<PortNumber, Port>>> cache) {
            super(cache);
        }

        @Override
        protected void onAdd(DeviceId deviceId, Map<PortNumber, Port> ports) {
//            notifyDelegate(new DeviceEvent(PORT_ADDED, getDevice(deviceId)));
        }

        @Override
        protected void onRemove(DeviceId deviceId, Map<PortNumber, Port> ports) {
//            notifyDelegate(new DeviceEvent(PORT_REMOVED, getDevice(deviceId)));
        }

        @Override
        protected void onUpdate(DeviceId deviceId, Map<PortNumber, Port> oldPorts, Map<PortNumber, Port> ports) {
//            notifyDelegate(new DeviceEvent(PORT_UPDATED, getDevice(deviceId)));
        }
    }


    // TODO cache serialized DeviceID if we suffer from serialization cost
}
