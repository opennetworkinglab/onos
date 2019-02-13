/*
 * Copyright 2015-present Open Networking Foundation
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;
import org.onlab.packet.ChassisId;
import org.onlab.util.KryoNamespace;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.NodeId;
import org.onosproject.mastership.MastershipService;
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
import org.onosproject.store.cluster.messaging.ClusterCommunicationService;
import org.onosproject.store.impl.MastershipBasedTimestamp;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.serializers.custom.DistributedStoreSerializers;
import org.onosproject.store.service.DistributedSet;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.EventuallyConsistentMapEvent;
import org.onosproject.store.service.EventuallyConsistentMapListener;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.SetEvent;
import org.onosproject.store.service.SetEventListener;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.WallClockTimestamp;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Verify.verify;
import static org.onosproject.net.DefaultAnnotations.merge;
import static org.onosproject.net.device.DeviceEvent.Type.DEVICE_ADDED;
import static org.onosproject.net.device.DeviceEvent.Type.DEVICE_AVAILABILITY_CHANGED;
import static org.onosproject.net.device.DeviceEvent.Type.DEVICE_REMOVED;
import static org.onosproject.net.device.DeviceEvent.Type.DEVICE_UPDATED;
import static org.onosproject.net.device.DeviceEvent.Type.PORT_ADDED;
import static org.onosproject.net.device.DeviceEvent.Type.PORT_STATS_UPDATED;
import static org.onosproject.net.device.DeviceEvent.Type.PORT_UPDATED;
import static org.onosproject.store.device.impl.GossipDeviceStoreMessageSubjects.DEVICE_REMOVE_REQ;
import static org.onosproject.store.service.EventuallyConsistentMapEvent.Type.PUT;
import static org.onosproject.store.service.EventuallyConsistentMapEvent.Type.REMOVE;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Manages the inventory of devices using a {@code EventuallyConsistentMap}.
 */
//@Component(immediate = true, enabled = false)
public class ECDeviceStore
    extends AbstractStore<DeviceEvent, DeviceStoreDelegate>
    implements DeviceStore {

    private final Logger log = getLogger(getClass());

    private static final String DEVICE_NOT_FOUND = "Device with ID %s not found";

    private final Map<DeviceId, Device> devices = Maps.newConcurrentMap();
    private final Map<DeviceId, Map<PortNumber, Port>> devicePorts = Maps.newConcurrentMap();
    Set<DeviceId> pendingAvailableChangeUpdates = Sets.newConcurrentHashSet();

    private EventuallyConsistentMap<DeviceKey, DeviceDescription> deviceDescriptions;
    private EventuallyConsistentMap<PortKey, PortDescription> portDescriptions;
    private EventuallyConsistentMap<DeviceId, Map<PortNumber, PortStatistics>> devicePortStats;
    private EventuallyConsistentMap<DeviceId, Map<PortNumber, PortStatistics>> devicePortDeltaStats;

    private DistributedSet<DeviceId> availableDevices;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected MastershipTermService mastershipTermService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceClockService deviceClockService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterCommunicationService clusterCommunicator;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

    private NodeId localNodeId;
    private EventuallyConsistentMapListener<DeviceKey, DeviceDescription> deviceUpdateListener =
            new InternalDeviceChangeEventListener();
    private EventuallyConsistentMapListener<PortKey, PortDescription> portUpdateListener =
            new InternalPortChangeEventListener();
    private final EventuallyConsistentMapListener<DeviceId, Map<PortNumber, PortStatistics>> portStatsListener =
            new InternalPortStatsListener();
    private final SetEventListener<DeviceId> deviceStatusTracker =
            new InternalDeviceStatusTracker();

    protected static final Serializer SERIALIZER = Serializer.using(
                  KryoNamespace.newBuilder()
                    .register(DistributedStoreSerializers.STORE_COMMON)
                    .nextId(DistributedStoreSerializers.STORE_CUSTOM_BEGIN)
                    .build("ECDevice"));

    protected static final KryoNamespace.Builder SERIALIZER_BUILDER = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API)
            .register(DeviceKey.class)
            .register(PortKey.class)
            .register(DeviceKey.class)
            .register(PortKey.class)
            .register(MastershipBasedTimestamp.class);

    @Activate
    public void activate() {
        localNodeId = clusterService.getLocalNode().id();

        deviceDescriptions = storageService.<DeviceKey, DeviceDescription>eventuallyConsistentMapBuilder()
                .withName("onos-device-descriptions")
                .withSerializer(SERIALIZER_BUILDER)
                .withTimestampProvider((k, v) -> {
                    try {
                        return deviceClockService.getTimestamp(k.deviceId());
                    } catch (IllegalStateException e) {
                        return null;
                    }
                }).build();

        portDescriptions = storageService.<PortKey, PortDescription>eventuallyConsistentMapBuilder()
                .withName("onos-port-descriptions")
                .withSerializer(SERIALIZER_BUILDER)
                .withTimestampProvider((k, v) -> {
                    try {
                        return deviceClockService.getTimestamp(k.deviceId());
                    } catch (IllegalStateException e) {
                        return null;
                    }
                }).build();

        devicePortStats = storageService.<DeviceId, Map<PortNumber, PortStatistics>>eventuallyConsistentMapBuilder()
                .withName("onos-port-stats")
                .withSerializer(SERIALIZER_BUILDER)
                .withAntiEntropyPeriod(5, TimeUnit.SECONDS)
                .withTimestampProvider((k, v) -> new WallClockTimestamp())
                .withTombstonesDisabled()
                .build();

        devicePortDeltaStats = storageService.<DeviceId, Map<PortNumber, PortStatistics>>
                eventuallyConsistentMapBuilder()
                .withName("onos-port-stats-delta")
                .withSerializer(SERIALIZER_BUILDER)
                .withAntiEntropyPeriod(5, TimeUnit.SECONDS)
                .withTimestampProvider((k, v) -> new WallClockTimestamp())
                .withTombstonesDisabled()
                .build();

        availableDevices = storageService.<DeviceId>setBuilder()
                .withName("onos-online-devices")
                .withSerializer(Serializer.using(KryoNamespaces.API))
                .withRelaxedReadConsistency()
                .build()
                .asDistributedSet();

        deviceDescriptions.addListener(deviceUpdateListener);
        portDescriptions.addListener(portUpdateListener);
        devicePortStats.addListener(portStatsListener);
        availableDevices.addListener(deviceStatusTracker);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        devicePortStats.removeListener(portStatsListener);
        deviceDescriptions.removeListener(deviceUpdateListener);
        portDescriptions.removeListener(portUpdateListener);
        availableDevices.removeListener(deviceStatusTracker);
        devicePortStats.destroy();
        devicePortDeltaStats.destroy();
        deviceDescriptions.destroy();
        portDescriptions.destroy();
        devices.clear();
        devicePorts.clear();
        log.info("Stopped");
    }

    @Override
    public Iterable<Device> getDevices() {
        return devices.values();
    }

    @Override
    public int getDeviceCount() {
        return devices.size();
    }

    @Override
    public int getAvailableDeviceCount() {
        return availableDevices.size();
    }

    @Override
    public Device getDevice(DeviceId deviceId) {
        return devices.get(deviceId);
    }

    // FIXME handle deviceDescription.isDefaultAvailable()=false case properly.
    @Override
    public DeviceEvent createOrUpdateDevice(ProviderId providerId,
            DeviceId deviceId,
            DeviceDescription deviceDescription) {
        NodeId master = mastershipService.getMasterFor(deviceId);
        if (localNodeId.equals(master)) {
            deviceDescriptions.put(new DeviceKey(providerId, deviceId), deviceDescription);
            return refreshDeviceCache(providerId, deviceId);
        } else {
                return null;
        }
    }

    private DeviceEvent refreshDeviceCache(ProviderId providerId, DeviceId deviceId) {
        AtomicReference<DeviceEvent.Type> eventType = new AtomicReference<>();
        Device device = devices.compute(deviceId, (k, existingDevice) -> {
            Device newDevice = composeDevice(deviceId);
            if (existingDevice == null) {
                eventType.set(DEVICE_ADDED);
            } else {
                // We allow only certain attributes to trigger update
                boolean propertiesChanged =
                        !Objects.equals(existingDevice.hwVersion(), newDevice.hwVersion()) ||
                                !Objects.equals(existingDevice.swVersion(), newDevice.swVersion()) ||
                                !Objects.equals(existingDevice.providerId(), newDevice.providerId());
                boolean annotationsChanged =
                        !AnnotationsUtil.isEqual(existingDevice.annotations(), newDevice.annotations());

                // Primary providers can respond to all changes, but ancillary ones
                // should respond only to annotation changes.
                if ((providerId.isAncillary() && annotationsChanged) ||
                        (!providerId.isAncillary() && (propertiesChanged || annotationsChanged))) {
                    boolean replaced = devices.replace(deviceId, existingDevice, newDevice);
                    verify(replaced, "Replacing devices cache failed. PID:%s [expected:%s, found:%s, new=%s]",
                            providerId, existingDevice, devices.get(deviceId), newDevice);
                    eventType.set(DEVICE_UPDATED);
                }
            }
            return newDevice;
        });
        if (eventType.get() != null && !providerId.isAncillary()) {
            markOnline(deviceId);
        }
        return eventType.get() != null ? new DeviceEvent(eventType.get(), device) : null;
    }

    /**
     * Returns the primary providerId for a device.
     * @param deviceId device identifier
     * @return primary providerId
     */
    private Set<ProviderId> getAllProviders(DeviceId deviceId) {
        return deviceDescriptions.keySet()
                                 .stream()
                                 .filter(deviceKey -> deviceKey.deviceId().equals(deviceId))
                                 .map(deviceKey -> deviceKey.providerId())
                                 .collect(Collectors.toSet());
    }

    /**
     * Returns the identifier for all providers for a device.
     * @param deviceId device identifier
     * @return set of provider identifiers
     */
    private ProviderId getPrimaryProviderId(DeviceId deviceId) {
        Set<ProviderId> allProviderIds = getAllProviders(deviceId);
        return allProviderIds.stream()
                             .filter(p -> !p.isAncillary())
                             .findFirst()
                             .orElse(Iterables.getFirst(allProviderIds, null));
    }

    /**
     * Returns a Device, merging descriptions from multiple Providers.
     *
     * @param deviceId      device identifier
     * @return Device instance
     */
    private Device composeDevice(DeviceId deviceId) {

        ProviderId primaryProviderId = getPrimaryProviderId(deviceId);
        DeviceDescription primaryDeviceDescription =
                deviceDescriptions.get(new DeviceKey(primaryProviderId, deviceId));

        Type type = primaryDeviceDescription.type();
        String manufacturer = primaryDeviceDescription.manufacturer();
        String hwVersion = primaryDeviceDescription.hwVersion();
        String swVersion = primaryDeviceDescription.swVersion();
        String serialNumber = primaryDeviceDescription.serialNumber();
        ChassisId chassisId = primaryDeviceDescription.chassisId();
        DefaultAnnotations annotations = mergeAnnotations(deviceId);

        return new DefaultDevice(primaryProviderId, deviceId, type, manufacturer,
                                 hwVersion, swVersion, serialNumber,
                                 chassisId, annotations);
    }

    private DeviceEvent purgeDeviceCache(DeviceId deviceId) {
        Device removedDevice = devices.remove(deviceId);
        if (removedDevice != null) {
            getAllProviders(deviceId).forEach(p -> deviceDescriptions.remove(new DeviceKey(p, deviceId)));
            return new DeviceEvent(DEVICE_REMOVED, removedDevice);
        }
        return null;
    }

    // FIXME publicization of markOnline -- trigger some action independently?
    public DeviceEvent markOnline(DeviceId deviceId) {
        if (devices.containsKey(deviceId)) {
            if (availableDevices.add(deviceId)) {
                return new DeviceEvent(DEVICE_AVAILABILITY_CHANGED, devices.get(deviceId), null);
            }
        }
        log.warn("Device {} does not exist in store", deviceId);
        return null;
    }

    @Override
    public DeviceEvent markOffline(DeviceId deviceId) {
        availableDevices.remove(deviceId);
        // set update listener will raise the event.
        return null;
    }

    @Override
    public List<DeviceEvent> updatePorts(ProviderId providerId,
            DeviceId deviceId,
            List<PortDescription> descriptions) {
        NodeId master = mastershipService.getMasterFor(deviceId);
        List<DeviceEvent> deviceEvents = null;
        if (localNodeId.equals(master)) {
            descriptions.forEach(description -> {
                PortKey portKey = new PortKey(providerId, deviceId, description.portNumber());
                portDescriptions.put(portKey, description);
            });
            deviceEvents = refreshDevicePortCache(providerId, deviceId, Optional.empty());
        } else {
            return Collections.emptyList();
        }
        return deviceEvents == null ? Collections.emptyList() : deviceEvents;
    }

    private List<DeviceEvent> refreshDevicePortCache(ProviderId providerId,
            DeviceId deviceId,
            Optional<PortNumber> portNumber) {
        Device device = devices.get(deviceId);
        checkArgument(device != null, DEVICE_NOT_FOUND, deviceId);
        List<DeviceEvent> events = Lists.newArrayList();

        Map<PortNumber, Port> ports = devicePorts.computeIfAbsent(deviceId, key -> Maps.newConcurrentMap());
        List<PortDescription> descriptions = Lists.newArrayList();
        portDescriptions.entrySet().forEach(e -> {
            PortKey key = e.getKey();
            PortDescription value = e.getValue();
            if (key.deviceId().equals(deviceId) && key.providerId().equals(providerId)) {
                if (portNumber.isPresent()) {
                    if (portNumber.get().equals(key.portNumber())) {
                        descriptions.add(value);
                    }
                } else {
                    descriptions.add(value);
                }
            }
        });

        for (PortDescription description : descriptions) {
            final PortNumber number = description.portNumber();
            ports.compute(number, (k, existingPort) -> {
                Port newPort = composePort(device, number);
                if (existingPort == null) {
                    events.add(new DeviceEvent(PORT_ADDED, device, newPort));
                } else {
                    if (existingPort.isEnabled() != newPort.isEnabled() ||
                            existingPort.type() != newPort.type() ||
                            existingPort.portSpeed() != newPort.portSpeed() ||
                            !AnnotationsUtil.isEqual(existingPort.annotations(), newPort.annotations())) {
                        events.add(new DeviceEvent(PORT_UPDATED, device, newPort));
                    }
                }
                return newPort;
            });
        }

        return events;
    }

    /**
     * Returns a Port, merging descriptions from multiple Providers.
     *
     * @param device   device the port is on
     * @param number   port number
     * @return Port instance
     */
    private Port composePort(Device device, PortNumber number) {

        Map<ProviderId, PortDescription> descriptions = Maps.newHashMap();
        portDescriptions.entrySet().forEach(entry -> {
            PortKey portKey = entry.getKey();
            if (portKey.deviceId().equals(device.id()) && portKey.portNumber().equals(number)) {
                descriptions.put(portKey.providerId(), entry.getValue());
            }
        });
        ProviderId primary = getPrimaryProviderId(device.id());
        PortDescription primaryDescription = descriptions.get(primary);

        // if no primary, assume not enabled
        boolean isEnabled = false;
        DefaultAnnotations annotations = DefaultAnnotations.builder().build();
        if (primaryDescription != null) {
            isEnabled = primaryDescription.isEnabled();
            annotations = merge(annotations, primaryDescription.annotations());
        }
        Port updated = null;
        for (Entry<ProviderId, PortDescription> e : descriptions.entrySet()) {
            if (e.getKey().equals(primary)) {
                continue;
            }
            annotations = merge(annotations, e.getValue().annotations());
            updated = buildTypedPort(device, number, isEnabled, e.getValue(), annotations);
        }
        if (primaryDescription == null) {
            return updated == null ? new DefaultPort(device, number, false, annotations) : updated;
        }
        return updated == null
                ? buildTypedPort(device, number, isEnabled, primaryDescription, annotations)
                : updated;
    }

    private Port buildTypedPort(Device device, PortNumber number, boolean isEnabled,
            PortDescription description, Annotations annotations) {
            return new DefaultPort(device, number, isEnabled, description.type(),
                    description.portSpeed(), annotations);
    }

    @Override
    public DeviceEvent updatePortStatus(ProviderId providerId,
            DeviceId deviceId,
            PortDescription portDescription) {
        portDescriptions.put(new PortKey(providerId, deviceId, portDescription.portNumber()), portDescription);
        List<DeviceEvent> events =
                refreshDevicePortCache(providerId, deviceId, Optional.of(portDescription.portNumber()));
        return Iterables.getFirst(events, null);
    }

    @Override
    public List<Port> getPorts(DeviceId deviceId) {
        return ImmutableList.copyOf(devicePorts.getOrDefault(deviceId, Maps.newHashMap()).values());
    }

    @Override
    public Stream<PortDescription> getPortDescriptions(ProviderId pid,
                                                       DeviceId deviceId) {

        return portDescriptions.entrySet().stream()
                .filter(e -> e.getKey().providerId().equals(pid))
                .map(Map.Entry::getValue);
    }

    @Override
    public Port getPort(DeviceId deviceId, PortNumber portNumber) {
        return devicePorts.getOrDefault(deviceId, Maps.newHashMap()).get(portNumber);
    }

    @Override
    public PortDescription getPortDescription(ProviderId pid,
                                              DeviceId deviceId,
                                              PortNumber portNumber) {
        return portDescriptions.get(new PortKey(pid, deviceId, portNumber));
    }

    @Override
    public DeviceEvent updatePortStatistics(ProviderId providerId,
            DeviceId deviceId,
            Collection<PortStatistics> newStatsCollection) {

        Map<PortNumber, PortStatistics> prvStatsMap = devicePortStats.get(deviceId);
        Map<PortNumber, PortStatistics> newStatsMap = Maps.newHashMap();
        Map<PortNumber, PortStatistics> deltaStatsMap = Maps.newHashMap();

        if (prvStatsMap != null) {
            for (PortStatistics newStats : newStatsCollection) {
                PortNumber port = newStats.portNumber();
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
                PortNumber port = newStats.portNumber();
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
     * @param deviceId device indentifier
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
                .setPort(newStats.portNumber())
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
    public boolean isAvailable(DeviceId deviceId) {
        return availableDevices.contains(deviceId);
    }

    @Override
    public Iterable<Device> getAvailableDevices() {
        return Iterables.filter(Iterables.transform(availableDevices, devices::get), d -> d != null);
    }

    @Override
    public DeviceEvent removeDevice(DeviceId deviceId) {
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
            MastershipRole role = Futures.getUnchecked(mastershipService.requestRoleFor(deviceId));
            if (role == MastershipRole.MASTER) {
                master = localNodeId;
            }
        }

        if (!localNodeId.equals(master)) {
            log.debug("{} has control of {}, forwarding remove request",
                      master, deviceId);

            clusterCommunicator.unicast(deviceId, DEVICE_REMOVE_REQ, SERIALIZER::encode, master)
                               .whenComplete((r, e) -> {
                                   if (e != null) {
                                       log.error("Failed to forward {} remove request to its master", deviceId, e);
                                   }
                               });
            return null;
        }

        // I have control..
        DeviceEvent event = null;
        final DeviceKey deviceKey = new DeviceKey(getPrimaryProviderId(deviceId), deviceId);
        DeviceDescription removedDeviceDescription =
                deviceDescriptions.remove(deviceKey);
        if (removedDeviceDescription != null) {
            event = purgeDeviceCache(deviceId);
        }

        if (relinquishAtEnd) {
            log.debug("Relinquishing temporary role acquired for {}", deviceId);
            mastershipService.relinquishMastership(deviceId);
        }
        return event;
    }

    private DefaultAnnotations mergeAnnotations(DeviceId deviceId) {
        ProviderId primaryProviderId = getPrimaryProviderId(deviceId);
        DeviceDescription primaryDeviceDescription =
                deviceDescriptions.get(new DeviceKey(primaryProviderId, deviceId));
        DefaultAnnotations annotations = DefaultAnnotations.builder().build();
        annotations = merge(annotations, primaryDeviceDescription.annotations());
        for (ProviderId providerId : getAllProviders(deviceId)) {
            if (!providerId.equals(primaryProviderId)) {
                annotations = merge(annotations,
                                    deviceDescriptions.get(new DeviceKey(providerId, deviceId)).annotations());
            }
        }
        return annotations;
    }

    private class InternalDeviceStatusTracker implements SetEventListener<DeviceId> {
        @Override
        public void event(SetEvent<DeviceId> event) {
            final DeviceId deviceId = event.entry();
            final Device device = devices.get(deviceId);
            if (device != null) {
                notifyDelegate(new DeviceEvent(DEVICE_AVAILABILITY_CHANGED, device));
            } else {
                pendingAvailableChangeUpdates.add(deviceId);
            }
        }
    }

    private class InternalDeviceChangeEventListener
        implements EventuallyConsistentMapListener<DeviceKey, DeviceDescription> {
        @Override
        public void event(EventuallyConsistentMapEvent<DeviceKey, DeviceDescription> event) {
            DeviceId deviceId = event.key().deviceId();
            ProviderId providerId = event.key().providerId();
            if (event.type() == PUT) {
                notifyDelegate(refreshDeviceCache(providerId, deviceId));
                if (pendingAvailableChangeUpdates.remove(deviceId)) {
                    notifyDelegate(new DeviceEvent(DEVICE_AVAILABILITY_CHANGED, devices.get(deviceId)));
                }
            } else if (event.type() == REMOVE) {
                notifyDelegate(purgeDeviceCache(deviceId));
            }
        }
    }

    private class InternalPortChangeEventListener
        implements EventuallyConsistentMapListener<PortKey, PortDescription> {
        @Override
        public void event(EventuallyConsistentMapEvent<PortKey, PortDescription> event) {
            DeviceId deviceId = event.key().deviceId();
            ProviderId providerId = event.key().providerId();
            PortNumber portNumber = event.key().portNumber();
            if (event.type() == PUT) {
                if (devices.containsKey(deviceId)) {
                    List<DeviceEvent> events = refreshDevicePortCache(providerId, deviceId, Optional.of(portNumber));
                    for (DeviceEvent deviceEvent : events) {
                        notifyDelegate(deviceEvent);
                    }
                }
            } else if (event.type() == REMOVE) {
                log.warn("Unexpected port removed event");
            }
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
