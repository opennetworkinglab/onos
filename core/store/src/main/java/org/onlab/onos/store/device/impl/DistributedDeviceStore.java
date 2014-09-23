package org.onlab.onos.store.device.impl;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.onos.net.device.DeviceEvent.Type.DEVICE_AVAILABILITY_CHANGED;
import static org.onlab.onos.net.device.DeviceEvent.Type.DEVICE_MASTERSHIP_CHANGED;
import static org.onlab.onos.net.device.DeviceEvent.Type.DEVICE_REMOVED;
import static org.onlab.onos.net.device.DeviceEvent.Type.PORT_ADDED;
import static org.onlab.onos.net.device.DeviceEvent.Type.PORT_REMOVED;
import static org.onlab.onos.net.device.DeviceEvent.Type.PORT_UPDATED;
import static org.slf4j.LoggerFactory.getLogger;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
import org.onlab.onos.net.Element;
import org.onlab.onos.net.MastershipRole;
import org.onlab.onos.net.Port;
import org.onlab.onos.net.PortNumber;
import org.onlab.onos.net.device.DeviceDescription;
import org.onlab.onos.net.device.DeviceEvent;
import org.onlab.onos.net.device.DeviceStore;
import org.onlab.onos.net.device.PortDescription;
import org.onlab.onos.net.provider.ProviderId;
import org.onlab.onos.store.StoreService;
import org.onlab.util.KryoPool;
import org.slf4j.Logger;

import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.hazelcast.core.EntryAdapter;
import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.ISet;
import com.hazelcast.core.MapEvent;

import de.javakaffee.kryoserializers.URISerializer;


/**
 * Manages inventory of infrastructure devices using Hazelcast-backed map.
 */
@Component(immediate = true)
@Service
public class DistributedDeviceStore implements DeviceStore {

    /**
     * An IMap EntryListener, which reflects each remote event to cache.
     *
     * @param <K> IMap key type after deserialization
     * @param <V> IMap value type after deserialization
     */
    public static final class RemoteEventHandler<K, V> extends
            EntryAdapter<byte[], byte[]> {

        private LoadingCache<K, Optional<V>> cache;

        /**
         * Constructor.
         *
         * @param cache cache to update
         */
        public RemoteEventHandler(
                LoadingCache<K, Optional<V>> cache) {
            this.cache = checkNotNull(cache);
        }

        @Override
        public void mapCleared(MapEvent event) {
            cache.invalidateAll();
        }

        @Override
        public void entryUpdated(EntryEvent<byte[], byte[]> event) {
            cache.put(POOL.<K>deserialize(event.getKey()),
                        Optional.of(POOL.<V>deserialize(
                                        event.getValue())));
        }

        @Override
        public void entryRemoved(EntryEvent<byte[], byte[]> event) {
            cache.invalidate(POOL.<DeviceId>deserialize(event.getKey()));
        }

        @Override
        public void entryAdded(EntryEvent<byte[], byte[]> event) {
            entryUpdated(event);
        }
    }

    /**
     * CacheLoader to wrap Map value with Optional,
     * to handle negative hit on underlying IMap.
     *
     * @param <K> IMap key type after deserialization
     * @param <V> IMap value type after deserialization
     */
    public static final class OptionalCacheLoader<K, V> extends
            CacheLoader<K, Optional<V>> {

        private IMap<byte[], byte[]> rawMap;

        /**
         * Constructor.
         *
         * @param rawMap underlying IMap
         */
        public OptionalCacheLoader(IMap<byte[], byte[]> rawMap) {
            this.rawMap = checkNotNull(rawMap);
        }

        @Override
        public Optional<V> load(K key) throws Exception {
            byte[] keyBytes = serialize(key);
            byte[] valBytes = rawMap.get(keyBytes);
            if (valBytes == null) {
                return Optional.absent();
            }
            V dev = deserialize(valBytes);
            return Optional.of(dev);
        }
    }

    private final Logger log = getLogger(getClass());

    public static final String DEVICE_NOT_FOUND = "Device with ID %s not found";

    // FIXME Slice out types used in common to separate pool/namespace.
    private static final KryoPool POOL = KryoPool.newBuilder()
            .register(URI.class, new URISerializer())
            .register(
                    ArrayList.class,

                    ProviderId.class,
                    Device.Type.class,

                    DeviceId.class,
                    DefaultDevice.class,
                    MastershipRole.class,
                    HashMap.class,
                    Port.class,
                    Element.class
                    )
            .register(PortNumber.class, new PortNumberSerializer())
            .register(DefaultPort.class, new DefaultPortSerializer())
            .build()
            .populate(10);

    // private IMap<DeviceId, DefaultDevice> cache;
    private IMap<byte[], byte[]> rawDevices;
    private LoadingCache<DeviceId, Optional<DefaultDevice>> devices;

    // private IMap<DeviceId, MastershipRole> roles;
    private IMap<byte[], byte[]> rawRoles;
    private LoadingCache<DeviceId, Optional<MastershipRole>> roles;

    // private ISet<DeviceId> availableDevices;
    private ISet<byte[]> availableDevices;

    // TODO DevicePorts is very inefficient consider restructuring.
    // private IMap<DeviceId, Map<PortNumber, Port>> devicePorts;
    private IMap<byte[], byte[]> rawDevicePorts;
    private LoadingCache<DeviceId, Optional<Map<PortNumber, Port>>> devicePorts;

    // FIXME change to protected once we remove DistributedDeviceManagerTest.
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StoreService storeService;

    /*protected*/public HazelcastInstance theInstance;


    @Activate
    public void activate() {
        log.info("Started");
        theInstance = storeService.getHazelcastInstance();

        // IMap event handler needs value
        final boolean includeValue = true;

        // TODO decide on Map name scheme to avoid collision
        rawDevices = theInstance.getMap("devices");
        devices = new AbsentInvalidatingLoadingCache<DeviceId, DefaultDevice>(
                CacheBuilder.newBuilder()
                .build(new OptionalCacheLoader<DeviceId, DefaultDevice>(rawDevices)));
        // refresh/populate cache based on notification from other instance
        rawDevices.addEntryListener(
                new RemoteEventHandler<DeviceId, DefaultDevice>(devices),
                includeValue);

        rawRoles = theInstance.getMap("roles");
        roles = new AbsentInvalidatingLoadingCache<DeviceId, MastershipRole>(
                CacheBuilder.newBuilder()
                .build(new OptionalCacheLoader<DeviceId, MastershipRole>(rawRoles)));
        // refresh/populate cache based on notification from other instance
        rawRoles.addEntryListener(
                new RemoteEventHandler<DeviceId, MastershipRole>(roles),
                includeValue);

        // TODO cache avai
        availableDevices = theInstance.getSet("availableDevices");

        rawDevicePorts = theInstance.getMap("devicePorts");
        devicePorts = new AbsentInvalidatingLoadingCache<DeviceId, Map<PortNumber, Port>>(
                CacheBuilder.newBuilder()
                .build(new OptionalCacheLoader<DeviceId, Map<PortNumber, Port>>(rawDevicePorts)));
        // refresh/populate cache based on notification from other instance
        rawDevicePorts.addEntryListener(
                new RemoteEventHandler<DeviceId, Map<PortNumber, Port>>(devicePorts),
                includeValue);

    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public int getDeviceCount() {
        // TODO IMap size or cache size?
        return rawDevices.size();
    }

    @Override
    public Iterable<Device> getDevices() {
// TODO Revisit if we ever need to do this.
//        log.info("{}:{}", rawMap.size(), cache.size());
//        if (rawMap.size() != cache.size()) {
//            for (Entry<byte[], byte[]> e : rawMap.entrySet()) {
//                final DeviceId key = deserialize(e.getKey());
//                final DefaultDevice val = deserialize(e.getValue());
//                cache.put(key, val);
//            }
//        }

        // TODO builder v.s. copyOf. Guava semms to be using copyOf?
        Builder<Device> builder = ImmutableSet.<Device>builder();
        for (Optional<DefaultDevice> e : devices.asMap().values()) {
            if (e.isPresent()) {
                builder.add(e.get());
            }
        }
        return builder.build();
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

            // For now claim the device as a master automatically.
            rawRoles.put(deviceIdBytes, serialize(MastershipRole.MASTER));
            roles.put(deviceId,  Optional.of(MastershipRole.MASTER));
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
                devices.put(device.id(), Optional.of(updated));
                availableDevices.add(serialize(device.id()));
            }
            return new DeviceEvent(DeviceEvent.Type.DEVICE_UPDATED, device, null);
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
        return events;
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
            return new DeviceEvent(PORT_UPDATED, device, port);
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
    public MastershipRole getRole(DeviceId deviceId) {
        MastershipRole role = roles.getUnchecked(deviceId).orNull();
        return role != null ? role : MastershipRole.NONE;
    }

    @Override
    public DeviceEvent setRole(DeviceId deviceId, MastershipRole role) {
        synchronized (this) {
            Device device = getDevice(deviceId);
            checkArgument(device != null, DEVICE_NOT_FOUND, deviceId);
            MastershipRole oldRole = deserialize(
                    rawRoles.put(serialize(deviceId), serialize(role)));
            roles.put(deviceId, Optional.of(role));
            return oldRole == role ? null :
                    new DeviceEvent(DEVICE_MASTERSHIP_CHANGED, device, null);
        }
    }

    @Override
    public DeviceEvent removeDevice(DeviceId deviceId) {
        synchronized (this) {
            byte[] deviceIdBytes = serialize(deviceId);
            rawRoles.remove(deviceIdBytes);
            roles.invalidate(deviceId);

            // TODO conditional remove?
            Device device = deserialize(rawDevices.remove(deviceIdBytes));
            devices.invalidate(deviceId);
            return device == null ? null :
                    new DeviceEvent(DEVICE_REMOVED, device, null);
        }
    }

    // TODO cache serialized DeviceID if we suffer from serialization cost

    private static byte[] serialize(final Object obj) {
        return POOL.serialize(obj);
    }

    private static <T> T deserialize(final byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        return POOL.deserialize(bytes);
    }

}
