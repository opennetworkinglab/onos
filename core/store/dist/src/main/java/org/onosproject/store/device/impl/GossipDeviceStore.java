/*
 * Copyright 2014-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.store.device.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.commons.lang3.RandomUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.ChassisId;
import org.onlab.util.KryoNamespace;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.mastership.MastershipService;
import org.onosproject.mastership.MastershipTerm;
import org.onosproject.mastership.MastershipTermService;
import org.onosproject.net.Annotations;
import org.onosproject.net.AnnotationsUtil;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.DefaultPort;
import org.onosproject.net.Device;
import org.onosproject.net.Device.Type;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DefaultPortStatistics;
import org.onosproject.net.device.DeviceClockService;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceStore;
import org.onosproject.net.device.DeviceStoreDelegate;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.device.PortStatistics;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.Timestamp;
import org.onosproject.store.cluster.messaging.ClusterCommunicationService;
import org.onosproject.store.cluster.messaging.MessageSubject;
import org.onosproject.store.impl.Timestamped;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.serializers.StoreSerializer;
import org.onosproject.store.serializers.custom.DistributedStoreSerializers;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.EventuallyConsistentMapEvent;
import org.onosproject.store.service.EventuallyConsistentMapListener;
import org.onosproject.store.service.MultiValuedTimestamp;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.WallClockTimestamp;
import org.slf4j.Logger;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Predicates.notNull;
import static com.google.common.base.Verify.verify;
import static java.util.concurrent.Executors.newCachedThreadPool;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onlab.util.Tools.minPriority;
import static org.onosproject.cluster.ControllerNodeToNodeId.toNodeId;
import static org.onosproject.net.device.DeviceEvent.Type.DEVICE_AVAILABILITY_CHANGED;
import static org.onosproject.net.device.DeviceEvent.Type.PORT_ADDED;
import static org.onosproject.net.device.DeviceEvent.Type.PORT_REMOVED;
import static org.onosproject.net.device.DeviceEvent.Type.PORT_STATS_UPDATED;
import static org.onosproject.net.device.DeviceEvent.Type.PORT_UPDATED;
import static org.onosproject.store.device.impl.GossipDeviceStoreMessageSubjects.DEVICE_ADVERTISE;
import static org.onosproject.store.device.impl.GossipDeviceStoreMessageSubjects.DEVICE_OFFLINE;
import static org.onosproject.store.device.impl.GossipDeviceStoreMessageSubjects.DEVICE_REMOVED;
import static org.onosproject.store.device.impl.GossipDeviceStoreMessageSubjects.DEVICE_REMOVE_REQ;
import static org.onosproject.store.device.impl.GossipDeviceStoreMessageSubjects.DEVICE_UPDATE;
import static org.onosproject.store.device.impl.GossipDeviceStoreMessageSubjects.PORT_STATUS_UPDATE;
import static org.onosproject.store.device.impl.GossipDeviceStoreMessageSubjects.PORT_UPDATE;
import static org.onosproject.store.service.EventuallyConsistentMapEvent.Type.PUT;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Manages inventory of infrastructure devices using gossip protocol to distribute
 * information.
 */
@Component(immediate = true)
@Service
public class GossipDeviceStore
        extends AbstractStore<DeviceEvent, DeviceStoreDelegate>
        implements DeviceStore {

    private final Logger log = getLogger(getClass());

    private static final String DEVICE_NOT_FOUND = "Device with ID %s not found";
    // Timeout in milliseconds to process device or ports on remote master node
    private static final int REMOTE_MASTER_TIMEOUT = 1000;

    // innerMap is used to lock a Device, thus instance should never be replaced.
    // collection of Description given from various providers
    private final ConcurrentMap<DeviceId, Map<ProviderId, DeviceDescriptions>>
            deviceDescs = Maps.newConcurrentMap();

    // cache of Device and Ports generated by compositing descriptions from providers
    private final ConcurrentMap<DeviceId, Device> devices = Maps.newConcurrentMap();
    private final ConcurrentMap<DeviceId, ConcurrentMap<PortNumber, Port>> devicePorts = Maps.newConcurrentMap();

    private EventuallyConsistentMap<DeviceId, Map<PortNumber, PortStatistics>> devicePortStats;
    private EventuallyConsistentMap<DeviceId, Map<PortNumber, PortStatistics>> devicePortDeltaStats;
    private final EventuallyConsistentMapListener<DeviceId, Map<PortNumber, PortStatistics>>
            portStatsListener = new InternalPortStatsListener();

    // to be updated under Device lock
    private final Map<DeviceId, Timestamp> offline = Maps.newHashMap();
    private final Map<DeviceId, Timestamp> removalRequest = Maps.newHashMap();

    // available(=UP) devices
    private final Set<DeviceId> availableDevices = Sets.newConcurrentHashSet();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceClockService deviceClockService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterCommunicationService clusterCommunicator;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipTermService termService;


    protected static final StoreSerializer SERIALIZER = StoreSerializer.using(KryoNamespace.newBuilder()
                    .register(DistributedStoreSerializers.STORE_COMMON)
                    .nextId(DistributedStoreSerializers.STORE_CUSTOM_BEGIN)
                    .register(new InternalDeviceEventSerializer(), InternalDeviceEvent.class)
                    .register(new InternalDeviceOfflineEventSerializer(), InternalDeviceOfflineEvent.class)
                    .register(InternalDeviceRemovedEvent.class)
                    .register(new InternalPortEventSerializer(), InternalPortEvent.class)
                    .register(new InternalPortStatusEventSerializer(), InternalPortStatusEvent.class)
                    .register(DeviceAntiEntropyAdvertisement.class)
                    .register(DeviceFragmentId.class)
                    .register(PortFragmentId.class)
                    .build("GossipDevice"));

    private ExecutorService executor;

    private ScheduledExecutorService backgroundExecutor;

    // TODO make these anti-entropy parameters configurable
    private long initialDelaySec = 5;
    private long periodSec = 5;

    @Activate
    public void activate() {
        executor = newCachedThreadPool(groupedThreads("onos/device", "fg-%d", log));

        backgroundExecutor =
                newSingleThreadScheduledExecutor(minPriority(groupedThreads("onos/device", "bg-%d", log)));

        addSubscriber(DEVICE_UPDATE, this::handleDeviceEvent);
        addSubscriber(DEVICE_OFFLINE, this::handleDeviceOfflineEvent);
        addSubscriber(DEVICE_REMOVE_REQ, this::handleRemoveRequest);
        addSubscriber(DEVICE_REMOVED, this::handleDeviceRemovedEvent);
        addSubscriber(PORT_UPDATE, this::handlePortEvent);
        addSubscriber(PORT_STATUS_UPDATE, this::handlePortStatusEvent);
        addSubscriber(DEVICE_ADVERTISE, this::handleDeviceAdvertisement);

        // start anti-entropy thread
        backgroundExecutor.scheduleAtFixedRate(new SendAdvertisementTask(),
                                               initialDelaySec, periodSec, TimeUnit.SECONDS);

        // Create a distributed map for port stats.
        KryoNamespace.Builder deviceDataSerializer = KryoNamespace.newBuilder()
                .register(KryoNamespaces.API)
                .nextId(KryoNamespaces.BEGIN_USER_CUSTOM_ID)
                .register(MultiValuedTimestamp.class);

        devicePortStats = storageService.<DeviceId, Map<PortNumber, PortStatistics>>eventuallyConsistentMapBuilder()
                .withName("port-stats")
                .withSerializer(deviceDataSerializer)
                .withAntiEntropyPeriod(5, TimeUnit.SECONDS)
                .withTimestampProvider((k, v) -> new WallClockTimestamp())
                .withTombstonesDisabled()
                .build();
        devicePortDeltaStats = storageService.<DeviceId, Map<PortNumber, PortStatistics>>
                eventuallyConsistentMapBuilder()
                .withName("port-stats-delta")
                .withSerializer(deviceDataSerializer)
                .withAntiEntropyPeriod(5, TimeUnit.SECONDS)
                .withTimestampProvider((k, v) -> new WallClockTimestamp())
                .withTombstonesDisabled()
                .build();
        devicePortStats.addListener(portStatsListener);
        log.info("Started");
    }

    private <M> void addSubscriber(MessageSubject subject, Consumer<M> handler) {
        clusterCommunicator.addSubscriber(subject, SERIALIZER::decode, handler, executor);
    }

    @Deactivate
    public void deactivate() {
        devicePortStats.removeListener(portStatsListener);
        devicePortStats.destroy();
        devicePortDeltaStats.destroy();
        executor.shutdownNow();

        backgroundExecutor.shutdownNow();
        try {
            if (!backgroundExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                log.error("Timeout during executor shutdown");
            }
        } catch (InterruptedException e) {
            log.error("Error during executor shutdown", e);
        }

        deviceDescs.clear();
        devices.clear();
        devicePorts.clear();
        availableDevices.clear();
        clusterCommunicator.removeSubscriber(DEVICE_UPDATE);
        clusterCommunicator.removeSubscriber(DEVICE_OFFLINE);
        clusterCommunicator.removeSubscriber(DEVICE_REMOVE_REQ);
        clusterCommunicator.removeSubscriber(DEVICE_REMOVED);
        clusterCommunicator.removeSubscriber(PORT_UPDATE);
        clusterCommunicator.removeSubscriber(PORT_STATUS_UPDATE);
        clusterCommunicator.removeSubscriber(DEVICE_ADVERTISE);
        log.info("Stopped");
    }

    @Override
    public int getDeviceCount() {
        return devices.size();
    }

    @Override
    public Iterable<Device> getDevices() {
        return Collections.unmodifiableCollection(devices.values());
    }

    @Override
    public Iterable<Device> getAvailableDevices() {
        return FluentIterable.from(getDevices())
                .filter(input -> isAvailable(input.id()));
    }

    @Override
    public Device getDevice(DeviceId deviceId) {
        return devices.get(deviceId);
    }

    @Override
    public synchronized DeviceEvent createOrUpdateDevice(ProviderId providerId,
                                                         DeviceId deviceId,
                                                         DeviceDescription deviceDescription) {
        NodeId localNode = clusterService.getLocalNode().id();
        NodeId deviceNode = mastershipService.getMasterFor(deviceId);

        // Process device update only if we're the master,
        // otherwise signal the actual master.
        DeviceEvent deviceEvent = null;
        if (localNode.equals(deviceNode)) {

            final Timestamp newTimestamp = deviceClockService.getTimestamp(deviceId);
            final Timestamped<DeviceDescription> deltaDesc = new Timestamped<>(deviceDescription, newTimestamp);
            final Timestamped<DeviceDescription> mergedDesc;
            final Map<ProviderId, DeviceDescriptions> device = getOrCreateDeviceDescriptionsMap(deviceId);

            synchronized (device) {
                deviceEvent = createOrUpdateDeviceInternal(providerId, deviceId, deltaDesc);
                mergedDesc = device.get(providerId).getDeviceDesc();
            }

            if (deviceEvent != null) {
                log.debug("Notifying peers of a device update topology event for providerId: {} and deviceId: {}",
                          providerId, deviceId);
                notifyPeers(new InternalDeviceEvent(providerId, deviceId, mergedDesc));
            }

        } else {
            return null;
        }

        return deviceEvent;
    }

    private DeviceEvent createOrUpdateDeviceInternal(ProviderId providerId,
                                                     DeviceId deviceId,
                                                     Timestamped<DeviceDescription> deltaDesc) {

        // Collection of DeviceDescriptions for a Device
        Map<ProviderId, DeviceDescriptions> device
                = getOrCreateDeviceDescriptionsMap(deviceId);

        synchronized (device) {
            // locking per device

            if (isDeviceRemoved(deviceId, deltaDesc.timestamp())) {
                log.debug("Ignoring outdated event: {}", deltaDesc);
                return null;
            }

            DeviceDescriptions descs = getOrCreateProviderDeviceDescriptions(device, providerId, deltaDesc);

            final Device oldDevice = devices.get(deviceId);
            final Device newDevice;

            if (deltaDesc == descs.getDeviceDesc() ||
                    deltaDesc.isNewer(descs.getDeviceDesc())) {
                // on new device or valid update
                descs.putDeviceDesc(deltaDesc);
                newDevice = composeDevice(deviceId, device);
            } else {
                // outdated event, ignored.
                return null;
            }
            if (oldDevice == null) {
                // REGISTER
                if (!deltaDesc.value().isDefaultAvailable()) {
                    return registerDevice(providerId, newDevice, deltaDesc.timestamp());
                }
                // ADD
                return createDevice(providerId, newDevice, deltaDesc.timestamp());
            } else {
                // UPDATE or ignore (no change or stale)
                return updateDevice(providerId, oldDevice, newDevice, deltaDesc.timestamp(),
                                    deltaDesc.value().isDefaultAvailable());
            }
        }
    }

    // Creates the device and returns the appropriate event if necessary.
    // Guarded by deviceDescs value (=Device lock)
    private DeviceEvent createDevice(ProviderId providerId,
                                     Device newDevice, Timestamp timestamp) {

        // update composed device cache
        Device oldDevice = devices.putIfAbsent(newDevice.id(), newDevice);
        verify(oldDevice == null,
               "Unexpected Device in cache. PID:%s [old=%s, new=%s]",
               providerId, oldDevice, newDevice);

        if (!providerId.isAncillary()) {
            markOnline(newDevice.id(), timestamp);
        }

        return new DeviceEvent(DeviceEvent.Type.DEVICE_ADDED, newDevice, null);
    }

    // Updates the device and returns the appropriate event if necessary.
    // Guarded by deviceDescs value (=Device lock)
    private DeviceEvent updateDevice(ProviderId providerId,
                                     Device oldDevice,
                                     Device newDevice, Timestamp newTimestamp,
                                     boolean forceAvailable) {
        // We allow only certain attributes to trigger update
        boolean propertiesChanged =
                !Objects.equals(oldDevice.hwVersion(), newDevice.hwVersion()) ||
                        !Objects.equals(oldDevice.swVersion(), newDevice.swVersion()) ||
                        !Objects.equals(oldDevice.providerId(), newDevice.providerId()) ||
                        !Objects.equals(oldDevice.chassisId(), newDevice.chassisId());
        boolean annotationsChanged =
                !AnnotationsUtil.isEqual(oldDevice.annotations(), newDevice.annotations());

        // Primary providers can respond to all changes, but ancillary ones
        // should respond only to annotation changes.
        DeviceEvent event = null;
        if ((providerId.isAncillary() && annotationsChanged) ||
                (!providerId.isAncillary() && (propertiesChanged || annotationsChanged))) {
            boolean replaced = devices.replace(newDevice.id(), oldDevice, newDevice);
            if (!replaced) {
                verify(replaced,
                       "Replacing devices cache failed. PID:%s [expected:%s, found:%s, new=%s]",
                       providerId, oldDevice, devices.get(newDevice.id()), newDevice);
            }

            event = new DeviceEvent(DeviceEvent.Type.DEVICE_UPDATED, newDevice, null);
        }

        if (!providerId.isAncillary() && forceAvailable) {
            boolean wasOnline = availableDevices.contains(newDevice.id());
            markOnline(newDevice.id(), newTimestamp);
            if (!wasOnline) {
                notifyDelegateIfNotNull(new DeviceEvent(DEVICE_AVAILABILITY_CHANGED, newDevice, null));
            }
        }
        return event;
    }

    private DeviceEvent registerDevice(ProviderId providerId, Device newDevice, Timestamp newTimestamp) {
        // update composed device cache
        Device oldDevice = devices.putIfAbsent(newDevice.id(), newDevice);
        verify(oldDevice == null,
               "Unexpected Device in cache. PID:%s [old=%s, new=%s]",
               providerId, oldDevice, newDevice);

        if (!providerId.isAncillary()) {
            markOffline(newDevice.id(), newTimestamp);
        }

        return new DeviceEvent(DeviceEvent.Type.DEVICE_ADDED, newDevice, null);
    }

    @Override
    public DeviceEvent markOffline(DeviceId deviceId) {
        return markOffline(deviceId, deviceClockService.getTimestamp(deviceId));
    }

    private DeviceEvent markOffline(DeviceId deviceId, Timestamp timestamp) {
        final DeviceEvent event = markOfflineInternal(deviceId, timestamp);
        if (event != null) {
            log.debug("Notifying peers of a device offline topology event for deviceId: {} {}",
                     deviceId, timestamp);
            notifyPeers(new InternalDeviceOfflineEvent(deviceId, timestamp));
        }
        return event;
    }

    private DeviceEvent markOfflineInternal(DeviceId deviceId, Timestamp timestamp) {
        Map<ProviderId, DeviceDescriptions> providerDescs
                = getOrCreateDeviceDescriptionsMap(deviceId);

        // locking device
        synchronized (providerDescs) {
            // accept off-line if given timestamp is newer than
            // the latest Timestamp from Primary provider
            DeviceDescriptions primDescs = getPrimaryDescriptions(providerDescs);
            if (primDescs == null) {
                return null;
            }

            Timestamp lastTimestamp = primDescs.getLatestTimestamp();
            if (timestamp.compareTo(lastTimestamp) <= 0) {
                // outdated event ignore
                return null;
            }

            offline.put(deviceId, timestamp);

            Device device = devices.get(deviceId);
            if (device == null) {
                return null;
            }
            boolean removed = availableDevices.remove(deviceId);
            if (removed) {
                return new DeviceEvent(DEVICE_AVAILABILITY_CHANGED, device, null);
            }
            return null;
        }
    }

    @Override
    public boolean markOnline(DeviceId deviceId) {
        if (devices.containsKey(deviceId)) {
            final Timestamp timestamp = deviceClockService.getTimestamp(deviceId);
            Map<?, ?> deviceLock = getOrCreateDeviceDescriptionsMap(deviceId);
            synchronized (deviceLock) {
                if (markOnline(deviceId, timestamp)) {
                    notifyDelegate(new DeviceEvent(DEVICE_AVAILABILITY_CHANGED, getDevice(deviceId), null));
                    return true;
                } else {
                    return false;
                }
            }
        }
        log.warn("Device {} does not exist in store", deviceId);
        return false;

    }

    /**
     * Marks the device as available if the given timestamp is not outdated,
     * compared to the time the device has been marked offline.
     *
     * @param deviceId  identifier of the device
     * @param timestamp of the event triggering this change.
     * @return true if availability change request was accepted and changed the state
     */
    // Guarded by deviceDescs value (=Device lock)
    private boolean markOnline(DeviceId deviceId, Timestamp timestamp) {
        // accept on-line if given timestamp is newer than
        // the latest offline request Timestamp
        Timestamp offlineTimestamp = offline.get(deviceId);
        if (offlineTimestamp == null ||
                offlineTimestamp.compareTo(timestamp) < 0) {

            offline.remove(deviceId);
            return availableDevices.add(deviceId);
        }
        return false;
    }

    @Override
    public synchronized List<DeviceEvent> updatePorts(ProviderId providerId,
                                                      DeviceId deviceId,
                                                      List<PortDescription> portDescriptions) {

        NodeId localNode = clusterService.getLocalNode().id();
        // TODO: It might be negligible, but this will have negative impact to topology discovery performance,
        // since it will trigger distributed store read.
        // Also, it'll probably be better if side-way communication happened on ConfigurationProvider, etc.
        // outside Device subsystem. so that we don't have to modify both Device and Link stores.
        // If we don't care much about topology performance, then it might be OK.
        NodeId deviceNode = mastershipService.getMasterFor(deviceId);

        // Process port update only if we're the master of the device,
        // otherwise signal the actual master.
        List<DeviceEvent> deviceEvents = null;
        if (localNode.equals(deviceNode)) {

            final Timestamp newTimestamp;
            try {
                newTimestamp = deviceClockService.getTimestamp(deviceId);
            } catch (IllegalStateException e) {
                log.info("Timestamp was not available for device {}", deviceId);
                log.debug("  discarding {}", portDescriptions);
                // Failed to generate timestamp.

                // Possible situation:
                //  Device connected and became master for short period of time,
                // but lost mastership before this instance had the chance to
                // retrieve term information.

                // Information dropped here is expected to be recoverable by
                // device probing after mastership change

                return Collections.emptyList();
            }
            log.debug("timestamp for {} {}", deviceId, newTimestamp);

            final Timestamped<List<PortDescription>> timestampedInput
                    = new Timestamped<>(portDescriptions, newTimestamp);
            final Timestamped<List<PortDescription>> merged;

            final Map<ProviderId, DeviceDescriptions> device = getOrCreateDeviceDescriptionsMap(deviceId);

            synchronized (device) {
                deviceEvents = updatePortsInternal(providerId, deviceId, timestampedInput);
                final DeviceDescriptions descs = device.get(providerId);
                List<PortDescription> mergedList =
                        FluentIterable.from(portDescriptions)
                                .transform(input ->
                                    // lookup merged port description
                                    descs.getPortDesc(input.portNumber()).value()
                                ).toList();
                merged = new Timestamped<>(mergedList, newTimestamp);
            }

            if (!deviceEvents.isEmpty()) {
                log.debug("Notifying peers of a ports update topology event for providerId: {} and deviceId: {}",
                         providerId, deviceId);
                notifyPeers(new InternalPortEvent(providerId, deviceId, merged));
            }

        } else {
            return Collections.emptyList();
        }

        return deviceEvents == null ? Collections.emptyList() : deviceEvents;
    }

    private List<DeviceEvent> updatePortsInternal(ProviderId providerId,
                                                  DeviceId deviceId,
                                                  Timestamped<List<PortDescription>> portDescriptions) {

        Device device = devices.get(deviceId);
        if (device == null) {
            log.debug("Device is no longer valid: {}", deviceId);
            return Collections.emptyList();
        }

        Map<ProviderId, DeviceDescriptions> descsMap = deviceDescs.get(deviceId);
        checkArgument(descsMap != null, DEVICE_NOT_FOUND, deviceId);

        List<DeviceEvent> events = new ArrayList<>();
        synchronized (descsMap) {

            if (isDeviceRemoved(deviceId, portDescriptions.timestamp())) {
                log.debug("Ignoring outdated events: {}", portDescriptions);
                return Collections.emptyList();
            }

            DeviceDescriptions descs = descsMap.get(providerId);
            // every provider must provide DeviceDescription.
            checkArgument(descs != null,
                          "Device description for Device ID %s from Provider %s was not found",
                          deviceId, providerId);

            Map<PortNumber, Port> ports = getPortMap(deviceId);

            final Timestamp newTimestamp = portDescriptions.timestamp();

            // Add new ports
            Set<PortNumber> processed = new HashSet<>();
            for (PortDescription portDescription : portDescriptions.value()) {
                final PortNumber number = portDescription.portNumber();
                processed.add(number);

                final Port oldPort = ports.get(number);
                final Port newPort;


                final Timestamped<PortDescription> existingPortDesc = descs.getPortDesc(number);
                if (existingPortDesc == null ||
                        newTimestamp.compareTo(existingPortDesc.timestamp()) >= 0) {
                    // on new port or valid update
                    // update description
                    descs.putPortDesc(new Timestamped<>(portDescription,
                                                        portDescriptions.timestamp()));
                    newPort = composePort(device, number, descsMap);
                } else {
                    // outdated event, ignored.
                    continue;
                }

                events.add(oldPort == null ?
                                   createPort(device, newPort, ports) :
                                   updatePort(device, oldPort, newPort, ports));
            }

            events.addAll(pruneOldPorts(device, ports, processed));
        }
        return FluentIterable.from(events).filter(notNull()).toList();
    }

    // Creates a new port based on the port description adds it to the map and
    // Returns corresponding event.
    // Guarded by deviceDescs value (=Device lock)
    private DeviceEvent createPort(Device device, Port newPort,
                                   Map<PortNumber, Port> ports) {
        ports.put(newPort.number(), newPort);
        return new DeviceEvent(PORT_ADDED, device, newPort);
    }

    // Checks if the specified port requires update and if so, it replaces the
    // existing entry in the map and returns corresponding event.
    // Guarded by deviceDescs value (=Device lock)
    private DeviceEvent updatePort(Device device, Port oldPort,
                                   Port newPort,
                                   Map<PortNumber, Port> ports) {

        if (oldPort.isEnabled() != newPort.isEnabled() ||
                oldPort.type() != newPort.type() ||
                oldPort.portSpeed() != newPort.portSpeed() ||
                !AnnotationsUtil.isEqual(oldPort.annotations(), newPort.annotations())) {
            ports.put(oldPort.number(), newPort);
            return new DeviceEvent(PORT_UPDATED, device, newPort);
        }
        return null;
    }

    private DeviceEvent removePort(DeviceId deviceId, PortNumber portNumber) {

        log.info("Deleted port: " + deviceId.toString() + "/" + portNumber.toString());
        Port deletedPort = devicePorts.get(deviceId).remove(portNumber);

        return new DeviceEvent(PORT_REMOVED, getDevice(deviceId), deletedPort);
    }

    // Prunes the specified list of ports based on which ports are in the
    // processed list and returns list of corresponding events.
    // Guarded by deviceDescs value (=Device lock)
    private List<DeviceEvent> pruneOldPorts(Device device,
                                            Map<PortNumber, Port> ports,
                                            Set<PortNumber> processed) {
        List<DeviceEvent> events = new ArrayList<>();
        Iterator<Entry<PortNumber, Port>> iterator = ports.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<PortNumber, Port> e = iterator.next();
            PortNumber portNumber = e.getKey();
            if (!processed.contains(portNumber)) {
                events.add(new DeviceEvent(PORT_REMOVED, device, e.getValue()));
                iterator.remove();
            }
        }
        return events;
    }

    // Gets the map of ports for the specified device; if one does not already
    // exist, it creates and registers a new one.
    private ConcurrentMap<PortNumber, Port> getPortMap(DeviceId deviceId) {
        return devicePorts.computeIfAbsent(deviceId, k -> new ConcurrentHashMap<>());
    }

    private Map<ProviderId, DeviceDescriptions> getOrCreateDeviceDescriptionsMap(
            DeviceId deviceId) {
        Map<ProviderId, DeviceDescriptions> r;
        r = deviceDescs.get(deviceId);
        if (r == null) {
            r = new HashMap<>();
            final Map<ProviderId, DeviceDescriptions> concurrentlyAdded;
            concurrentlyAdded = deviceDescs.putIfAbsent(deviceId, r);
            if (concurrentlyAdded != null) {
                r = concurrentlyAdded;
            }
        }
        return r;
    }

    // Guarded by deviceDescs value (=Device lock)
    private DeviceDescriptions getOrCreateProviderDeviceDescriptions(
            Map<ProviderId, DeviceDescriptions> device,
            ProviderId providerId, Timestamped<DeviceDescription> deltaDesc) {
        synchronized (device) {
            DeviceDescriptions r = device.get(providerId);
            if (r == null) {
                r = new DeviceDescriptions(deltaDesc);
                device.put(providerId, r);
            }
            return r;
        }
    }

    @Override
    public synchronized DeviceEvent updatePortStatus(ProviderId providerId,
                                                     DeviceId deviceId,
                                                     PortDescription portDescription) {
        final Timestamp newTimestamp;
        try {
            newTimestamp = deviceClockService.getTimestamp(deviceId);
        } catch (IllegalStateException e) {
            log.info("Timestamp was not available for device {}", deviceId);
            log.debug("  discarding {}", portDescription);
            // Failed to generate timestamp. Ignoring.
            // See updatePorts comment
            return null;
        }
        final Timestamped<PortDescription> deltaDesc
                = new Timestamped<>(portDescription, newTimestamp);
        final DeviceEvent event;
        final Timestamped<PortDescription> mergedDesc;
        final Map<ProviderId, DeviceDescriptions> device = getOrCreateDeviceDescriptionsMap(deviceId);
        synchronized (device) {
            event = updatePortStatusInternal(providerId, deviceId, deltaDesc);
            mergedDesc = device.get(providerId)
                    .getPortDesc(portDescription.portNumber());
        }
        if (event != null) {
            log.debug("Notifying peers of a port status update topology event for providerId: {} and deviceId: {}",
                     providerId, deviceId);
            notifyPeers(new InternalPortStatusEvent(providerId, deviceId, mergedDesc));
        }
        return event;
    }

    private DeviceEvent updatePortStatusInternal(ProviderId providerId, DeviceId deviceId,
                                                 Timestamped<PortDescription> deltaDesc) {
        Device device = devices.get(deviceId);
        checkArgument(device != null, DEVICE_NOT_FOUND, deviceId);

        Map<ProviderId, DeviceDescriptions> descsMap = deviceDescs.get(deviceId);
        checkArgument(descsMap != null, DEVICE_NOT_FOUND, deviceId);

        synchronized (descsMap) {

            if (isDeviceRemoved(deviceId, deltaDesc.timestamp())) {
                log.debug("Ignoring outdated event: {}", deltaDesc);
                return null;
            }

            DeviceDescriptions descs = descsMap.get(providerId);
            // assuming all providers must to give DeviceDescription
            verify(descs != null,
                   "Device description for Device ID %s from Provider %s was not found",
                   deviceId, providerId);

            ConcurrentMap<PortNumber, Port> ports = getPortMap(deviceId);
            final PortNumber number = deltaDesc.value().portNumber();
            final Port oldPort = ports.get(number);
            final Port newPort;
            final Timestamped<PortDescription> existingPortDesc = descs.getPortDesc(number);
            boolean toDelete = false;

            if (existingPortDesc == null ||
                    deltaDesc.isNewer(existingPortDesc)) {
                // on new port or valid update
                // update description
                descs.putPortDesc(deltaDesc);
                newPort = composePort(device, number, descsMap);
                toDelete = deltaDesc.value().isRemoved();
            } else {
                // same or outdated event, ignored.
                log.trace("ignore same or outdated {} >= {}", existingPortDesc, deltaDesc);
                return null;
            }

            if (oldPort == null) {
                return createPort(device, newPort, ports);
            } else {
                return toDelete ? removePort(deviceId, number) : updatePort(device, oldPort, newPort, ports);
            }
        }
    }

    @Override
    public List<Port> getPorts(DeviceId deviceId) {
        Map<PortNumber, Port> ports = devicePorts.get(deviceId);
        if (ports == null) {
            return Collections.emptyList();
        }
        return ImmutableList.copyOf(ports.values());
    }

    @Override
    public Stream<PortDescription> getPortDescriptions(ProviderId pid,
                                                       DeviceId deviceId) {
        Map<ProviderId, DeviceDescriptions> descs = this.deviceDescs.get(deviceId);
        if (descs == null) {
            return null;
        }
        // inner-Map(=descs) is HashMap, thus requires synchronization even for reads
        final Optional<DeviceDescriptions> devDescs;
        synchronized (descs) {
            devDescs = Optional.ofNullable(descs.get(pid));
        }
        // DeviceDescriptions is concurrent access-safe
        return devDescs
            .map(dd -> dd.getPortDescs().values().stream()
                                             .map(Timestamped::value))
            .orElse(Stream.empty());
    }

    @Override
    public DeviceEvent updatePortStatistics(ProviderId providerId, DeviceId deviceId,
                                            Collection<PortStatistics> newStatsCollection) {

        Map<PortNumber, PortStatistics> prvStatsMap = devicePortStats.get(deviceId);
        Map<PortNumber, PortStatistics> newStatsMap = Maps.newHashMap();
        Map<PortNumber, PortStatistics> deltaStatsMap = Maps.newHashMap();

        if (prvStatsMap != null) {
            for (PortStatistics newStats : newStatsCollection) {
                PortNumber port = PortNumber.portNumber(newStats.port());
                PortStatistics prvStats = prvStatsMap.get(port);
                DefaultPortStatistics.Builder builder = DefaultPortStatistics.builder();
                PortStatistics deltaStats = builder.build();
                if (prvStats != null) {
                    deltaStats = calcDeltaStats(deviceId, prvStats, newStats);
                }
                deltaStatsMap.put(port, deltaStats);
                newStatsMap.put(port, newStats);
            }
        } else {
            for (PortStatistics newStats : newStatsCollection) {
                PortNumber port = PortNumber.portNumber(newStats.port());
                newStatsMap.put(port, newStats);
            }
        }
        devicePortDeltaStats.put(deviceId, deltaStatsMap);
        devicePortStats.put(deviceId, newStatsMap);
        // DeviceEvent returns null because of InternalPortStatsListener usage
        return null;
    }

    /**
     * Calculate delta statistics by subtracting previous from new statistics.
     *
     * @param deviceId device identifier
     * @param prvStats previous port statistics
     * @param newStats new port statistics
     * @return PortStatistics
     */
    public PortStatistics calcDeltaStats(DeviceId deviceId, PortStatistics prvStats, PortStatistics newStats) {
        // calculate time difference
        long deltaStatsSec, deltaStatsNano;
        if (newStats.durationNano() < prvStats.durationNano()) {
            deltaStatsNano = newStats.durationNano() - prvStats.durationNano() + TimeUnit.SECONDS.toNanos(1);
            deltaStatsSec = newStats.durationSec() - prvStats.durationSec() - 1L;
        } else {
            deltaStatsNano = newStats.durationNano() - prvStats.durationNano();
            deltaStatsSec = newStats.durationSec() - prvStats.durationSec();
        }
        DefaultPortStatistics.Builder builder = DefaultPortStatistics.builder();
        DefaultPortStatistics deltaStats = builder.setDeviceId(deviceId)
                .setPort(newStats.port())
                .setPacketsReceived(newStats.packetsReceived() - prvStats.packetsReceived())
                .setPacketsSent(newStats.packetsSent() - prvStats.packetsSent())
                .setBytesReceived(newStats.bytesReceived() - prvStats.bytesReceived())
                .setBytesSent(newStats.bytesSent() - prvStats.bytesSent())
                .setPacketsRxDropped(newStats.packetsRxDropped() - prvStats.packetsRxDropped())
                .setPacketsTxDropped(newStats.packetsTxDropped() - prvStats.packetsTxDropped())
                .setPacketsRxErrors(newStats.packetsRxErrors() - prvStats.packetsRxErrors())
                .setPacketsTxErrors(newStats.packetsTxErrors() - prvStats.packetsTxErrors())
                .setDurationSec(deltaStatsSec)
                .setDurationNano(deltaStatsNano)
                .build();
        return deltaStats;
    }

    @Override
    public List<PortStatistics> getPortStatistics(DeviceId deviceId) {
        Map<PortNumber, PortStatistics> portStats = devicePortStats.get(deviceId);
        if (portStats == null) {
            return Collections.emptyList();
        }
        return ImmutableList.copyOf(portStats.values());
    }

    @Override
    public PortStatistics getStatisticsForPort(DeviceId deviceId, PortNumber portNumber) {
        Map<PortNumber, PortStatistics> portStatsMap = devicePortStats.get(deviceId);
        if (portStatsMap == null) {
            return null;
        }
        PortStatistics portStats = portStatsMap.get(portNumber);
        return portStats;
    }

    @Override
    public List<PortStatistics> getPortDeltaStatistics(DeviceId deviceId) {
        Map<PortNumber, PortStatistics> portStats = devicePortDeltaStats.get(deviceId);
        if (portStats == null) {
            return Collections.emptyList();
        }
        return ImmutableList.copyOf(portStats.values());
    }

    @Override
    public PortStatistics getDeltaStatisticsForPort(DeviceId deviceId, PortNumber portNumber) {
        Map<PortNumber, PortStatistics> portStatsMap = devicePortDeltaStats.get(deviceId);
        if (portStatsMap == null) {
            return null;
        }
        PortStatistics portStats = portStatsMap.get(portNumber);
        return portStats;
    }

    @Override
    public Port getPort(DeviceId deviceId, PortNumber portNumber) {
        Map<PortNumber, Port> ports = devicePorts.get(deviceId);
        return ports == null ? null : ports.get(portNumber);
    }

    @Override
    public PortDescription getPortDescription(ProviderId pid,
                                              DeviceId deviceId,
                                              PortNumber portNumber) {
        Map<ProviderId, DeviceDescriptions> descs = this.deviceDescs.get(deviceId);
        if (descs == null) {
            return null;
        }
        // inner-Map(=descs) is HashMap, thus requires synchronization even for reads
        final Optional<DeviceDescriptions> devDescs;
        synchronized (descs) {
            devDescs = Optional.ofNullable(descs.get(pid));
        }
        // DeviceDescriptions is concurrent access-safe
        return devDescs
                .map(deviceDescriptions -> deviceDescriptions.getPortDesc(portNumber))
                .map(Timestamped::value)
                .orElse(null);
    }

    @Override
    public boolean isAvailable(DeviceId deviceId) {
        return availableDevices.contains(deviceId);
    }

    @Override
    public synchronized DeviceEvent removeDevice(DeviceId deviceId) {
        final NodeId myId = clusterService.getLocalNode().id();
        NodeId master = mastershipService.getMasterFor(deviceId);

        // if there exist a master, forward
        // if there is no master, try to become one and process

        boolean relinquishAtEnd = false;
        if (master == null) {
            final MastershipRole myRole = mastershipService.getLocalRole(deviceId);
            if (myRole != MastershipRole.NONE) {
                relinquishAtEnd = true;
            }
            log.debug("Temporarily requesting role for {} to remove", deviceId);
            mastershipService.requestRoleFor(deviceId);
            MastershipTerm term = termService.getMastershipTerm(deviceId);
            if (term != null && myId.equals(term.master())) {
                master = myId;
            }
        }

        if (!myId.equals(master)) {
            log.debug("{} has control of {}, forwarding remove request",
                      master, deviceId);

            // TODO check unicast return value
            clusterCommunicator.unicast(deviceId, DEVICE_REMOVE_REQ, SERIALIZER::encode, master);
             /* error log:
             log.error("Failed to forward {} remove request to {}", deviceId, master, e);
             */

            // event will be triggered after master processes it.
            return null;
        }

        // I have control..

        Timestamp timestamp = deviceClockService.getTimestamp(deviceId);
        DeviceEvent event = removeDeviceInternal(deviceId, timestamp);
        if (event != null) {
            log.debug("Notifying peers of a device removed topology event for deviceId: {}",
                      deviceId);
            notifyPeers(new InternalDeviceRemovedEvent(deviceId, timestamp));
        }
        if (relinquishAtEnd) {
            log.debug("Relinquishing temporary role acquired for {}", deviceId);
            mastershipService.relinquishMastership(deviceId);
        }
        return event;
    }

    private DeviceEvent removeDeviceInternal(DeviceId deviceId,
                                             Timestamp timestamp) {

        Map<ProviderId, DeviceDescriptions> descs = getOrCreateDeviceDescriptionsMap(deviceId);
        synchronized (descs) {
            // accept removal request if given timestamp is newer than
            // the latest Timestamp from Primary provider
            DeviceDescriptions primDescs = getPrimaryDescriptions(descs);
            if (primDescs == null) {
                return null;
            }

            Timestamp lastTimestamp = primDescs.getLatestTimestamp();
            if (timestamp.compareTo(lastTimestamp) <= 0) {
                // outdated event ignore
                return null;
            }
            removalRequest.put(deviceId, timestamp);

            Device device = devices.remove(deviceId);
            // should DEVICE_REMOVED carry removed ports?
            Map<PortNumber, Port> ports = devicePorts.get(deviceId);
            if (ports != null) {
                ports.clear();
            }
            markOfflineInternal(deviceId, timestamp);
            descs.clear();
            return device == null ? null :
                    new DeviceEvent(DeviceEvent.Type.DEVICE_REMOVED, device, null);
        }
    }

    /**
     * Checks if given timestamp is superseded by removal request
     * with more recent timestamp.
     *
     * @param deviceId         identifier of a device
     * @param timestampToCheck timestamp of an event to check
     * @return true if device is already removed
     */
    private boolean isDeviceRemoved(DeviceId deviceId, Timestamp timestampToCheck) {
        Timestamp removalTimestamp = removalRequest.get(deviceId);
        if (removalTimestamp != null &&
                removalTimestamp.compareTo(timestampToCheck) >= 0) {
            // removalRequest is more recent
            return true;
        }
        return false;
    }

    /**
     * Returns a Device, merging description given from multiple Providers.
     *
     * @param deviceId      device identifier
     * @param providerDescs Collection of Descriptions from multiple providers
     * @return Device instance
     */
    private Device composeDevice(DeviceId deviceId,
                                 Map<ProviderId, DeviceDescriptions> providerDescs) {

        checkArgument(!providerDescs.isEmpty(), "No device descriptions supplied");

        ProviderId primary = pickPrimaryPid(providerDescs);

        DeviceDescriptions desc = providerDescs.get(primary);

        final DeviceDescription base = desc.getDeviceDesc().value();
        Type type = base.type();
        String manufacturer = base.manufacturer();
        String hwVersion = base.hwVersion();
        String swVersion = base.swVersion();
        String serialNumber = base.serialNumber();
        ChassisId chassisId = base.chassisId();
        DefaultAnnotations.Builder annotations = DefaultAnnotations.builder();
        annotations.putAll(base.annotations());

        for (Entry<ProviderId, DeviceDescriptions> e : providerDescs.entrySet()) {
            if (e.getKey().equals(primary)) {
                continue;
            }
            // Note: should keep track of Description timestamp in the future
            // and only merge conflicting keys when timestamp is newer.
            // Currently assuming there will never be a key conflict between
            // providers

            // annotation merging. not so efficient, should revisit later
            annotations.putAll(e.getValue().getDeviceDesc().value().annotations());
        }

        return new DefaultDevice(primary, deviceId, type, manufacturer,
                                 hwVersion, swVersion, serialNumber,
                                 chassisId, annotations.build());
    }

    private Port buildTypedPort(Device device, PortNumber number, boolean isEnabled,
                                 PortDescription description, Annotations annotations) {
                return new DefaultPort(device, number, isEnabled, description.type(),
                        description.portSpeed(), annotations);
    }

    /**
     * Returns a Port, merging description given from multiple Providers.
     *
     * @param device   device the port is on
     * @param number   port number
     * @param descsMap Collection of Descriptions from multiple providers
     * @return Port instance
     */
    private Port composePort(Device device, PortNumber number,
                             Map<ProviderId, DeviceDescriptions> descsMap) {

        ProviderId primary = pickPrimaryPid(descsMap);
        DeviceDescriptions primDescs = descsMap.get(primary);
        // if no primary, assume not enabled
        boolean isEnabled = false;
        DefaultAnnotations.Builder annotations = DefaultAnnotations.builder();
        Timestamp newest = null;
        final Timestamped<PortDescription> portDesc = primDescs.getPortDesc(number);
        if (portDesc != null) {
            isEnabled = portDesc.value().isEnabled();
            annotations.putAll(portDesc.value().annotations());
            newest = portDesc.timestamp();
        }
        Port updated = null;
        for (Entry<ProviderId, DeviceDescriptions> e : descsMap.entrySet()) {
            if (e.getKey().equals(primary)) {
                continue;
            }
            // Note: should keep track of Description timestamp in the future
            // and only merge conflicting keys when timestamp is newer.
            // Currently assuming there will never be a key conflict between
            // providers

            // annotation merging. not so efficient, should revisit later
            final Timestamped<PortDescription> otherPortDesc = e.getValue().getPortDesc(number);
            if (otherPortDesc != null) {
                if (newest != null && newest.isNewerThan(otherPortDesc.timestamp())) {
                    continue;
                }
                annotations.putAll(otherPortDesc.value().annotations());
                PortDescription other = otherPortDesc.value();
                updated = buildTypedPort(device, number, isEnabled, other, annotations.build());
                newest = otherPortDesc.timestamp();
            }
        }
        if (portDesc == null) {
            return updated == null ? new DefaultPort(device, number, false, annotations.build()) : updated;
        }
        PortDescription current = portDesc.value();
        return updated == null
                ? buildTypedPort(device, number, isEnabled, current, annotations.build())
                : updated;
    }

    /**
     * @return primary ProviderID, or randomly chosen one if none exists
     */
    private ProviderId pickPrimaryPid(
            Map<ProviderId, DeviceDescriptions> providerDescs) {
        ProviderId fallBackPrimary = null;
        for (Entry<ProviderId, DeviceDescriptions> e : providerDescs.entrySet()) {
            if (!e.getKey().isAncillary()) {
                return e.getKey();
            } else if (fallBackPrimary == null) {
                // pick randomly as a fallback in case there is no primary
                fallBackPrimary = e.getKey();
            }
        }
        return fallBackPrimary;
    }

    private DeviceDescriptions getPrimaryDescriptions(
            Map<ProviderId, DeviceDescriptions> providerDescs) {
        ProviderId pid = pickPrimaryPid(providerDescs);
        return providerDescs.get(pid);
    }

    private void unicastMessage(NodeId recipient, MessageSubject subject, Object event) throws IOException {
        clusterCommunicator.unicast(event, subject, SERIALIZER::encode, recipient);
    }

    private void broadcastMessage(MessageSubject subject, Object event) {
        clusterCommunicator.broadcast(event, subject, SERIALIZER::encode);
    }

    private void notifyPeers(InternalDeviceEvent event) {
        broadcastMessage(DEVICE_UPDATE, event);
    }

    private void notifyPeers(InternalDeviceOfflineEvent event) {
        broadcastMessage(GossipDeviceStoreMessageSubjects.DEVICE_OFFLINE, event);
    }

    private void notifyPeers(InternalDeviceRemovedEvent event) {
        broadcastMessage(GossipDeviceStoreMessageSubjects.DEVICE_REMOVED, event);
    }

    private void notifyPeers(InternalPortEvent event) {
        broadcastMessage(GossipDeviceStoreMessageSubjects.PORT_UPDATE, event);
    }

    private void notifyPeers(InternalPortStatusEvent event) {
        broadcastMessage(GossipDeviceStoreMessageSubjects.PORT_STATUS_UPDATE, event);
    }

    private void notifyPeer(NodeId recipient, InternalDeviceEvent event) {
        try {
            unicastMessage(recipient, DEVICE_UPDATE, event);
        } catch (IOException e) {
            log.error("Failed to send" + event + " to " + recipient, e);
        }
    }

    private void notifyPeer(NodeId recipient, InternalDeviceOfflineEvent event) {
        try {
            unicastMessage(recipient, GossipDeviceStoreMessageSubjects.DEVICE_OFFLINE, event);
        } catch (IOException e) {
            log.error("Failed to send" + event + " to " + recipient, e);
        }
    }

    private void notifyPeer(NodeId recipient, InternalDeviceRemovedEvent event) {
        try {
            unicastMessage(recipient, GossipDeviceStoreMessageSubjects.DEVICE_REMOVED, event);
        } catch (IOException e) {
            log.error("Failed to send" + event + " to " + recipient, e);
        }
    }

    private void notifyPeer(NodeId recipient, InternalPortEvent event) {
        try {
            unicastMessage(recipient, GossipDeviceStoreMessageSubjects.PORT_UPDATE, event);
        } catch (IOException e) {
            log.error("Failed to send" + event + " to " + recipient, e);
        }
    }

    private void notifyPeer(NodeId recipient, InternalPortStatusEvent event) {
        try {
            unicastMessage(recipient, GossipDeviceStoreMessageSubjects.PORT_STATUS_UPDATE, event);
        } catch (IOException e) {
            log.error("Failed to send" + event + " to " + recipient, e);
        }
    }

    private DeviceAntiEntropyAdvertisement createAdvertisement() {
        final NodeId self = clusterService.getLocalNode().id();

        final int numDevices = deviceDescs.size();
        Map<DeviceFragmentId, Timestamp> adDevices = new HashMap<>(numDevices);
        final int portsPerDevice = 8; // random factor to minimize reallocation
        Map<PortFragmentId, Timestamp> adPorts = new HashMap<>(numDevices * portsPerDevice);
        Map<DeviceId, Timestamp> adOffline = new HashMap<>(numDevices);

        deviceDescs.forEach((deviceId, devDescs) -> {

            // for each Device...
            synchronized (devDescs) {

                // send device offline timestamp
                Timestamp lOffline = this.offline.get(deviceId);
                if (lOffline != null) {
                    adOffline.put(deviceId, lOffline);
                }

                for (Entry<ProviderId, DeviceDescriptions>
                        prov : devDescs.entrySet()) {

                    // for each Provider Descriptions...
                    final ProviderId provId = prov.getKey();
                    final DeviceDescriptions descs = prov.getValue();

                    adDevices.put(new DeviceFragmentId(deviceId, provId),
                                  descs.getDeviceDesc().timestamp());

                    for (Entry<PortNumber, Timestamped<PortDescription>>
                            portDesc : descs.getPortDescs().entrySet()) {

                        final PortNumber number = portDesc.getKey();
                        adPorts.put(new PortFragmentId(deviceId, provId, number),
                                    portDesc.getValue().timestamp());
                    }
                }
            }
        });

        return new DeviceAntiEntropyAdvertisement(self, adDevices, adPorts, adOffline);
    }

    /**
     * Responds to anti-entropy advertisement message.
     * <p>
     * Notify sender about out-dated information using regular replication message.
     * Send back advertisement to sender if not in sync.
     *
     * @param advertisement to respond to
     */
    private void handleAdvertisement(DeviceAntiEntropyAdvertisement advertisement) {

        final NodeId sender = advertisement.sender();

        Map<DeviceFragmentId, Timestamp> devAds = new HashMap<>(advertisement.deviceFingerPrints());
        Map<PortFragmentId, Timestamp> portAds = new HashMap<>(advertisement.ports());
        Map<DeviceId, Timestamp> offlineAds = new HashMap<>(advertisement.offline());

        // Fragments to request
        Collection<DeviceFragmentId> reqDevices = new ArrayList<>();
        Collection<PortFragmentId> reqPorts = new ArrayList<>();

        for (Entry<DeviceId, Map<ProviderId, DeviceDescriptions>> de : deviceDescs.entrySet()) {
            final DeviceId deviceId = de.getKey();
            final Map<ProviderId, DeviceDescriptions> lDevice = de.getValue();

            synchronized (lDevice) {
                // latestTimestamp across provider
                // Note: can be null initially
                Timestamp localLatest = offline.get(deviceId);

                // handle device Ads
                for (Entry<ProviderId, DeviceDescriptions> prov : lDevice.entrySet()) {
                    final ProviderId provId = prov.getKey();
                    final DeviceDescriptions lDeviceDescs = prov.getValue();

                    final DeviceFragmentId devFragId = new DeviceFragmentId(deviceId, provId);


                    Timestamped<DeviceDescription> lProvDevice = lDeviceDescs.getDeviceDesc();
                    Timestamp advDevTimestamp = devAds.get(devFragId);

                    if (advDevTimestamp == null || lProvDevice.isNewerThan(
                            advDevTimestamp)) {
                        // remote does not have it or outdated, suggest
                        notifyPeer(sender, new InternalDeviceEvent(provId, deviceId, lProvDevice));
                    } else if (!lProvDevice.timestamp().equals(advDevTimestamp)) {
                        // local is outdated, request
                        reqDevices.add(devFragId);
                    }

                    // handle port Ads
                    for (Entry<PortNumber, Timestamped<PortDescription>>
                            pe : lDeviceDescs.getPortDescs().entrySet()) {

                        final PortNumber num = pe.getKey();
                        final Timestamped<PortDescription> lPort = pe.getValue();

                        final PortFragmentId portFragId = new PortFragmentId(deviceId, provId, num);

                        Timestamp advPortTimestamp = portAds.get(portFragId);
                        if (advPortTimestamp == null || lPort.isNewerThan(
                                advPortTimestamp)) {
                            // remote does not have it or outdated, suggest
                            notifyPeer(sender, new InternalPortStatusEvent(provId, deviceId, lPort));
                        } else if (!lPort.timestamp().equals(advPortTimestamp)) {
                            // local is outdated, request
                            log.trace("need update {} < {}", lPort.timestamp(), advPortTimestamp);
                            reqPorts.add(portFragId);
                        }

                        // remove port Ad already processed
                        portAds.remove(portFragId);
                    } // end local port loop

                    // remove device Ad already processed
                    devAds.remove(devFragId);

                    // find latest and update
                    final Timestamp providerLatest = lDeviceDescs.getLatestTimestamp();
                    if (localLatest == null ||
                            providerLatest.compareTo(localLatest) > 0) {
                        localLatest = providerLatest;
                    }
                } // end local provider loop

                // checking if remote timestamp is more recent.
                Timestamp rOffline = offlineAds.get(deviceId);
                if (rOffline != null &&
                        rOffline.compareTo(localLatest) > 0) {
                    // remote offline timestamp suggests that the
                    // device is off-line
                    markOfflineInternal(deviceId, rOffline);
                }

                Timestamp lOffline = offline.get(deviceId);
                if (lOffline != null && rOffline == null) {
                    // locally offline, but remote is online, suggest offline
                    notifyPeer(sender, new InternalDeviceOfflineEvent(deviceId, lOffline));
                }

                // remove device offline Ad already processed
                offlineAds.remove(deviceId);
            } // end local device loop
        } // device lock

        // If there is any Ads left, request them
        log.trace("Ads left {}, {}", devAds, portAds);
        reqDevices.addAll(devAds.keySet());
        reqPorts.addAll(portAds.keySet());

        if (reqDevices.isEmpty() && reqPorts.isEmpty()) {
            log.trace("Nothing to request to remote peer {}", sender);
            return;
        }

        log.debug("Need to sync {} {}", reqDevices, reqPorts);

        // 2-way Anti-Entropy for now
        try {
            unicastMessage(sender, DEVICE_ADVERTISE, createAdvertisement());
        } catch (IOException e) {
            log.error("Failed to send response advertisement to " + sender, e);
        }

// Sketch of 3-way Anti-Entropy
//        DeviceAntiEntropyRequest request = new DeviceAntiEntropyRequest(self, reqDevices, reqPorts);
//        ClusterMessage message = new ClusterMessage(
//                clusterService.getLocalNode().id(),
//                GossipDeviceStoreMessageSubjects.DEVICE_REQUEST,
//                SERIALIZER.encode(request));
//
//        try {
//            clusterCommunicator.unicast(message, advertisement.sender());
//        } catch (IOException e) {
//            log.error("Failed to send advertisement reply to "
//                      + advertisement.sender(), e);
//        }
    }

    private void notifyDelegateIfNotNull(DeviceEvent event) {
        if (event != null) {
            notifyDelegate(event);
        }
    }

    private final class SendAdvertisementTask implements Runnable {

        @Override
        public void run() {
            if (Thread.currentThread().isInterrupted()) {
                log.debug("Interrupted, quitting");
                return;
            }

            try {
                final NodeId self = clusterService.getLocalNode().id();
                Set<ControllerNode> nodes = clusterService.getNodes();

                ImmutableList<NodeId> nodeIds = FluentIterable.from(nodes)
                        .transform(toNodeId())
                        .toList();

                if (nodeIds.size() == 1 && nodeIds.get(0).equals(self)) {
                    log.trace("No other peers in the cluster.");
                    return;
                }

                NodeId peer;
                do {
                    int idx = RandomUtils.nextInt(0, nodeIds.size());
                    peer = nodeIds.get(idx);
                } while (peer.equals(self));

                DeviceAntiEntropyAdvertisement ad = createAdvertisement();

                if (Thread.currentThread().isInterrupted()) {
                    log.debug("Interrupted, quitting");
                    return;
                }

                try {
                    unicastMessage(peer, DEVICE_ADVERTISE, ad);
                } catch (IOException e) {
                    log.debug("Failed to send anti-entropy advertisement to {}", peer);
                    return;
                }
            } catch (Exception e) {
                // catch all Exception to avoid Scheduled task being suppressed.
                log.error("Exception thrown while sending advertisement", e);
            }
        }
    }

    private void handleDeviceEvent(InternalDeviceEvent event) {
        ProviderId providerId = event.providerId();
        DeviceId deviceId = event.deviceId();
        Timestamped<DeviceDescription> deviceDescription = event.deviceDescription();

        try {
            notifyDelegateIfNotNull(createOrUpdateDeviceInternal(providerId, deviceId,
                    deviceDescription));
        } catch (Exception e) {
            log.warn("Exception thrown handling device update", e);
        }
    }

    private void handleDeviceOfflineEvent(InternalDeviceOfflineEvent event) {
        DeviceId deviceId = event.deviceId();
        Timestamp timestamp = event.timestamp();

        try {
            notifyDelegateIfNotNull(markOfflineInternal(deviceId, timestamp));
        } catch (Exception e) {
            log.warn("Exception thrown handling device offline", e);
        }
    }

    private void handleRemoveRequest(DeviceId did) {
        try {
            removeDevice(did);
        } catch (Exception e) {
            log.warn("Exception thrown handling device remove", e);
        }
    }

    private void handleDeviceRemovedEvent(InternalDeviceRemovedEvent event) {
        DeviceId deviceId = event.deviceId();
        Timestamp timestamp = event.timestamp();

        try {
            notifyDelegateIfNotNull(removeDeviceInternal(deviceId, timestamp));
        } catch (Exception e) {
            log.warn("Exception thrown handling device removed", e);
        }
    }

    private void handlePortEvent(InternalPortEvent event) {
        ProviderId providerId = event.providerId();
        DeviceId deviceId = event.deviceId();
        Timestamped<List<PortDescription>> portDescriptions = event.portDescriptions();

        if (getDevice(deviceId) == null) {
            log.debug("{} not found on this node yet, ignoring.", deviceId);
            // Note: dropped information will be recovered by anti-entropy
            return;
        }

        try {
            notifyDelegate(updatePortsInternal(providerId, deviceId, portDescriptions));
        } catch (Exception e) {
            log.warn("Exception thrown handling port update", e);
        }
    }

    private void handlePortStatusEvent(InternalPortStatusEvent event) {
        ProviderId providerId = event.providerId();
        DeviceId deviceId = event.deviceId();
        Timestamped<PortDescription> portDescription = event.portDescription();

        if (getDevice(deviceId) == null) {
            log.debug("{} not found on this node yet, ignoring.", deviceId);
            // Note: dropped information will be recovered by anti-entropy
            return;
        }

        try {
            notifyDelegateIfNotNull(updatePortStatusInternal(providerId, deviceId, portDescription));
        } catch (Exception e) {
            log.warn("Exception thrown handling port update", e);
        }
    }

    private void handleDeviceAdvertisement(DeviceAntiEntropyAdvertisement advertisement) {
        try {
            handleAdvertisement(advertisement);
        } catch (Exception e) {
            log.warn("Exception thrown handling Device advertisements.", e);
        }
    }

    private class InternalPortStatsListener
            implements EventuallyConsistentMapListener<DeviceId, Map<PortNumber, PortStatistics>> {
        @Override
        public void event(EventuallyConsistentMapEvent<DeviceId, Map<PortNumber, PortStatistics>> event) {
            if (event.type() == PUT) {
                Device device = devices.get(event.key());
                if (device != null) {
                    notifyDelegate(new DeviceEvent(PORT_STATS_UPDATED, device));
                }
            }
        }
    }
}
