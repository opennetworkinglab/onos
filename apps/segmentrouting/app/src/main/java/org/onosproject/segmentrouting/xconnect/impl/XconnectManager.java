/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.segmentrouting.xconnect.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.Ethernet;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onlab.util.KryoNamespace;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.codec.CodecService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostLocation;
import org.onosproject.net.PortNumber;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flowobjective.DefaultFilteringObjective;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.DefaultNextObjective;
import org.onosproject.net.flowobjective.DefaultObjectiveContext;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.NextObjective;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.flowobjective.ObjectiveContext;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.onosproject.net.intf.InterfaceService;
import org.onosproject.segmentrouting.SegmentRoutingService;
import org.onosproject.segmentrouting.storekey.VlanNextObjectiveStoreKey;
import org.onosproject.segmentrouting.xconnect.api.XconnectCodec;
import org.onosproject.segmentrouting.xconnect.api.XconnectDesc;
import org.onosproject.segmentrouting.xconnect.api.XconnectKey;
import org.onosproject.segmentrouting.xconnect.api.XconnectService;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.Versioned;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.onlab.util.Tools.groupedThreads;

@Service
@Component(immediate = true)
public class XconnectManager implements XconnectService {
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private CodecService codecService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    public NetworkConfigService netCfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    public DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    public FlowObjectiveService flowObjectiveService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    public MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    public SegmentRoutingService srService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    public InterfaceService interfaceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    HostService hostService;

    private static final String APP_NAME = "org.onosproject.xconnect";
    private static final String ERROR_NOT_LEADER = "Not leader controller";

    private static Logger log = LoggerFactory.getLogger(XconnectManager.class);

    private ApplicationId appId;
    private ConsistentMap<XconnectKey, Set<PortNumber>> xconnectStore;
    private ConsistentMap<XconnectKey, Integer> xconnectNextObjStore;

    private ConsistentMap<VlanNextObjectiveStoreKey, Integer> xconnectMulticastNextStore;
    private ConsistentMap<VlanNextObjectiveStoreKey, List<PortNumber>> xconnectMulticastPortsStore;

    private final MapEventListener<XconnectKey, Set<PortNumber>> xconnectListener = new XconnectMapListener();
    private ExecutorService xConnectExecutor;

    private final DeviceListener deviceListener = new InternalDeviceListener();
    private ExecutorService deviceEventExecutor;

    private final HostListener hostListener = new InternalHostListener();
    private ExecutorService hostEventExecutor;

    @Activate
    void activate() {
        appId = coreService.registerApplication(APP_NAME);
        codecService.registerCodec(XconnectDesc.class, new XconnectCodec());

        KryoNamespace.Builder serializer = KryoNamespace.newBuilder()
                .register(KryoNamespaces.API)
                .register(XconnectManager.class)
                .register(XconnectKey.class)
                .register(VlanNextObjectiveStoreKey.class);

        xconnectStore = storageService.<XconnectKey, Set<PortNumber>>consistentMapBuilder()
                .withName("onos-sr-xconnect")
                .withRelaxedReadConsistency()
                .withSerializer(Serializer.using(serializer.build()))
                .build();
        xConnectExecutor = Executors.newSingleThreadScheduledExecutor(
                groupedThreads("sr-xconnect-event", "%d", log));
        xconnectStore.addListener(xconnectListener, xConnectExecutor);

        xconnectNextObjStore = storageService.<XconnectKey, Integer>consistentMapBuilder()
                .withName("onos-sr-xconnect-next")
                .withRelaxedReadConsistency()
                .withSerializer(Serializer.using(serializer.build()))
                .build();

        xconnectMulticastNextStore = storageService.<VlanNextObjectiveStoreKey, Integer>consistentMapBuilder()
                .withName("onos-sr-xconnect-l2multicast-next")
                .withSerializer(Serializer.using(serializer.build()))
                .build();
        xconnectMulticastPortsStore = storageService.<VlanNextObjectiveStoreKey, List<PortNumber>>consistentMapBuilder()
                .withName("onos-sr-xconnect-l2multicast-ports")
                .withSerializer(Serializer.using(serializer.build()))
                .build();

        deviceEventExecutor = Executors.newSingleThreadScheduledExecutor(
                groupedThreads("sr-xconnect-device-event", "%d", log));
        deviceService.addListener(deviceListener);

        hostEventExecutor = Executors.newSingleThreadExecutor(
                groupedThreads("sr-xconnect-host-event", "%d", log));
        hostService.addListener(hostListener);

        log.info("Started");
    }

    @Deactivate
    void deactivate() {
        xconnectStore.removeListener(xconnectListener);
        deviceService.removeListener(deviceListener);
        hostService.removeListener(hostListener);
        codecService.unregisterCodec(XconnectDesc.class);

        deviceEventExecutor.shutdown();
        hostEventExecutor.shutdown();
        xConnectExecutor.shutdown();

        log.info("Stopped");
    }

    @Override
    public void addOrUpdateXconnect(DeviceId deviceId, VlanId vlanId, Set<PortNumber> ports) {
        log.info("Adding or updating xconnect. deviceId={}, vlanId={}, ports={}",
                 deviceId, vlanId, ports);
        final XconnectKey key = new XconnectKey(deviceId, vlanId);
        xconnectStore.put(key, ports);
    }

    @Override
    public void removeXonnect(DeviceId deviceId, VlanId vlanId) {
        log.info("Removing xconnect. deviceId={}, vlanId={}",
                 deviceId, vlanId);
        final XconnectKey key = new XconnectKey(deviceId, vlanId);
        xconnectStore.remove(key);

        // Cleanup multicasting support, if any.
        srService.getPairDeviceId(deviceId).ifPresent(pairDeviceId -> {
            cleanupL2MulticastRule(pairDeviceId, srService.getPairLocalPort(pairDeviceId).get(), vlanId, true);
        });

    }

    @Override
    public Set<XconnectDesc> getXconnects() {
        return xconnectStore.asJavaMap().entrySet().stream()
                .map(e -> new XconnectDesc(e.getKey(), e.getValue()))
                .collect(Collectors.toSet());
    }

    @Override
    public boolean hasXconnect(ConnectPoint cp) {
        return getXconnects().stream().anyMatch(desc -> desc.key().deviceId().equals(cp.deviceId())
                && desc.ports().contains(cp.port())
        );
    }

    @Override
    public List<VlanId> getXconnectVlans(DeviceId deviceId, PortNumber port) {
        return getXconnects().stream()
                .filter(desc -> desc.key().deviceId().equals(deviceId) && desc.ports().contains(port))
                .map(desc -> desc.key().vlanId())
                .collect(Collectors.toList());
    }

    @Override
    public boolean isXconnectVlan(DeviceId deviceId, VlanId vlanId) {
        XconnectKey key = new XconnectKey(deviceId, vlanId);
        return Versioned.valueOrNull(xconnectStore.get(key)) != null;
    }

    @Override
    public ImmutableMap<XconnectKey, Integer> getNext() {
        if (xconnectNextObjStore != null) {
            return ImmutableMap.copyOf(xconnectNextObjStore.asJavaMap());
        } else {
            return ImmutableMap.of();
        }
    }

    @Override
    public int getNextId(DeviceId deviceId, VlanId vlanId) {
        return Versioned.valueOrElse(xconnectNextObjStore.get(new XconnectKey(deviceId, vlanId)), -1);
    }

    @Override
    public void removeNextId(int nextId) {
        xconnectNextObjStore.entrySet().forEach(e -> {
            if (e.getValue().value() == nextId) {
                xconnectNextObjStore.remove(e.getKey());
            }
        });
    }

    private class XconnectMapListener implements MapEventListener<XconnectKey, Set<PortNumber>> {
        @Override
        public void event(MapEvent<XconnectKey, Set<PortNumber>> event) {
            XconnectKey key = event.key();
            Versioned<Set<PortNumber>> ports = event.newValue();
            Versioned<Set<PortNumber>> oldPorts = event.oldValue();

            switch (event.type()) {
                case INSERT:
                    populateXConnect(key, ports.value());
                    break;
                case UPDATE:
                    updateXConnect(key, oldPorts.value(), ports.value());
                    break;
                case REMOVE:
                    revokeXConnect(key, oldPorts.value());
                    break;
                default:
                    break;
            }
        }
    }

    private class InternalDeviceListener implements DeviceListener {
        // Offload the execution to an executor and then process the event
        // if this instance is the leader of the device
        @Override
        public void event(DeviceEvent event) {
            deviceEventExecutor.execute(() -> {
                DeviceId deviceId = event.subject().id();
                // Just skip if we are not the leader
                if (!isLocalLeader(deviceId)) {
                    log.debug("Not the leader of {}. Skip event {}", deviceId, event);
                    return;
                }
                // Populate or revoke according to the device availability
                if (deviceService.isAvailable(deviceId)) {
                    init(deviceId);
                } else {
                    cleanup(deviceId);
                }
            });
        }
        // We want to manage only a subset of events and if we are the leader
        @Override
        public boolean isRelevant(DeviceEvent event) {
            return event.type() == DeviceEvent.Type.DEVICE_ADDED ||
                    event.type() == DeviceEvent.Type.DEVICE_AVAILABILITY_CHANGED ||
                    event.type() == DeviceEvent.Type.DEVICE_UPDATED;
        }
    }

    private class InternalHostListener implements HostListener {
        @Override
        public void event(HostEvent event) {
            hostEventExecutor.execute(() -> {

                switch (event.type()) {
                    case HOST_MOVED:
                        log.trace("Processing host event {}", event);

                        Host host = event.subject();
                        Set<HostLocation> prevLocations = event.prevSubject().locations();
                        Set<HostLocation> newLocations = host.locations();

                        // Dual-home host port failure
                        // For each old location, in failed and paired devices update L2 vlan groups
                        Sets.difference(prevLocations, newLocations).forEach(prevLocation -> {

                            Optional<DeviceId> pairDeviceId = srService.getPairDeviceId(prevLocation.deviceId());
                            Optional<PortNumber> pairLocalPort = srService.getPairLocalPort(prevLocation.deviceId());

                            if (pairDeviceId.isPresent() && pairLocalPort.isPresent() && newLocations.stream()
                                    .anyMatch(location -> location.deviceId().equals(pairDeviceId.get())) &&
                                    hasXconnect(new ConnectPoint(prevLocation.deviceId(), prevLocation.port()))) {

                                List<VlanId> xconnectVlans = getXconnectVlans(prevLocation.deviceId(),
                                                                              prevLocation.port());
                                xconnectVlans.forEach(xconnectVlan -> {
                                    // Add single-home host into L2 multicast group at paired device side.
                                    // Also append ACL rule to forward traffic from paired port to L2 multicast group.
                                    newLocations.stream()
                                            .filter(location -> location.deviceId().equals(pairDeviceId.get()))
                                            .forEach(location -> populateL2Multicast(location.deviceId(),
                                                                                     srService.getPairLocalPort(
                                                                                             location.deviceId()).get(),
                                                                                     xconnectVlan,
                                                                                     Collections.singletonList(
                                                                                             location.port())));
                                    // Ensure pair-port attached to xconnect vlan flooding group
                                    // at dual home failed device.
                                    updateL2Flooding(prevLocation.deviceId(), pairLocalPort.get(), xconnectVlan, true);
                                });
                            }
                        });

                        // Dual-home host port restoration
                        // For each new location, reverse xconnect loop prevention groups.
                        Sets.difference(newLocations, prevLocations).forEach(newLocation -> {
                            final Optional<DeviceId> pairDeviceId = srService.getPairDeviceId(newLocation.deviceId());
                            Optional<PortNumber> pairLocalPort = srService.getPairLocalPort(newLocation.deviceId());
                            if (pairDeviceId.isPresent() && pairLocalPort.isPresent() &&
                                    hasXconnect((new ConnectPoint(newLocation.deviceId(), newLocation.port())))) {

                                List<VlanId> xconnectVlans = getXconnectVlans(newLocation.deviceId(),
                                                                              newLocation.port());
                                xconnectVlans.forEach(xconnectVlan -> {
                                    // Remove recovered dual homed port from vlan L2 multicast group
                                    prevLocations.stream()
                                            .filter(prevLocation -> prevLocation.deviceId().equals(pairDeviceId.get()))
                                            .forEach(prevLocation -> revokeL2Multicast(
                                                    prevLocation.deviceId(),
                                                    xconnectVlan,
                                                    Collections.singletonList(newLocation.port()))
                                            );

                                    // Remove pair-port from vlan's flooding group at dual home
                                    // restored device, if needed.
                                    if (!hasAccessPortInMulticastGroup(new VlanNextObjectiveStoreKey(
                                            newLocation.deviceId(), xconnectVlan), pairLocalPort.get())) {
                                        updateL2Flooding(newLocation.deviceId(),
                                                         pairLocalPort.get(),
                                                         xconnectVlan,
                                                         false);

                                        // Clean L2 multicast group at pair-device; also update store.
                                        cleanupL2MulticastRule(pairDeviceId.get(),
                                                               srService.getPairLocalPort(pairDeviceId.get()).get(),
                                                               xconnectVlan,
                                                               false);
                                    }
                                });
                            }
                        });
                        break;

                    default:
                        log.warn("Unsupported host event type: {} received. Ignoring.", event.type());
                        break;
                }
            });
        }
    }

    private void init(DeviceId deviceId) {
        getXconnects().stream()
                .filter(desc -> desc.key().deviceId().equals(deviceId))
                .forEach(desc -> populateXConnect(desc.key(), desc.ports()));
    }

    private void cleanup(DeviceId deviceId) {
        xconnectNextObjStore.entrySet().stream()
                .filter(entry -> entry.getKey().deviceId().equals(deviceId))
                .forEach(entry -> xconnectNextObjStore.remove(entry.getKey()));
        log.debug("{} is removed from xConnectNextObjStore", deviceId);
    }

    /**
     * Populates XConnect groups and flows for given key.
     *
     * @param key   XConnect key
     * @param ports a set of ports to be cross-connected
     */
    private void populateXConnect(XconnectKey key, Set<PortNumber> ports) {
        if (!isLocalLeader(key.deviceId())) {
            log.debug("Abort populating XConnect {}: {}", key, ERROR_NOT_LEADER);
            return;
        }

        populateFilter(key, ports);
        populateFwd(key, populateNext(key, ports));
        populateAcl(key);
    }

    /**
     * Populates filtering objectives for given XConnect.
     *
     * @param key   XConnect store key
     * @param ports XConnect ports
     */
    private void populateFilter(XconnectKey key, Set<PortNumber> ports) {
        ports.forEach(port -> {
            FilteringObjective.Builder filtObjBuilder = filterObjBuilder(key, port);
            ObjectiveContext context = new DefaultObjectiveContext(
                    (objective) -> log.debug("XConnect FilterObj for {} on port {} populated",
                                             key, port),
                    (objective, error) ->
                            log.warn("Failed to populate XConnect FilterObj for {} on port {}: {}",
                                     key, port, error));
            flowObjectiveService.filter(key.deviceId(), filtObjBuilder.add(context));
        });
    }

    /**
     * Populates next objectives for given XConnect.
     *
     * @param key   XConnect store key
     * @param ports XConnect ports
     */
    private int populateNext(XconnectKey key, Set<PortNumber> ports) {
        int nextId = Versioned.valueOrElse(xconnectNextObjStore.get(key), -1);
        if (nextId != -1) {
            log.debug("NextObj for {} found, id={}", key, nextId);
            return nextId;
        } else {
            NextObjective.Builder nextObjBuilder = nextObjBuilder(key, ports);
            ObjectiveContext nextContext = new DefaultObjectiveContext(
                    // To serialize this with kryo
                    (Serializable & Consumer<Objective>) (objective) ->
                            log.debug("XConnect NextObj for {} added", key),
                    (Serializable & BiConsumer<Objective, ObjectiveError>) (objective, error) -> {
                        log.warn("Failed to add XConnect NextObj for {}: {}", key, error);
                        srService.invalidateNextObj(objective.id());
                    });
            NextObjective nextObj = nextObjBuilder.add(nextContext);
            flowObjectiveService.next(key.deviceId(), nextObj);
            xconnectNextObjStore.put(key, nextObj.id());
            log.debug("NextObj for {} not found. Creating new NextObj with id={}", key, nextObj.id());
            return nextObj.id();
        }
    }

    /**
     * Populates bridging forwarding objectives for given XConnect.
     *
     * @param key    XConnect store key
     * @param nextId next objective id
     */
    private void populateFwd(XconnectKey key, int nextId) {
        ForwardingObjective.Builder fwdObjBuilder = fwdObjBuilder(key, nextId);
        ObjectiveContext fwdContext = new DefaultObjectiveContext(
                (objective) -> log.debug("XConnect FwdObj for {} populated", key),
                (objective, error) ->
                        log.warn("Failed to populate XConnect FwdObj for {}: {}", key, error));
        flowObjectiveService.forward(key.deviceId(), fwdObjBuilder.add(fwdContext));
    }

    /**
     * Populates ACL forwarding objectives for given XConnect.
     *
     * @param key XConnect store key
     */
    private void populateAcl(XconnectKey key) {
        ForwardingObjective.Builder aclObjBuilder = aclObjBuilder(key.vlanId());
        ObjectiveContext aclContext = new DefaultObjectiveContext(
                (objective) -> log.debug("XConnect AclObj for {} populated", key),
                (objective, error) ->
                        log.warn("Failed to populate XConnect AclObj for {}: {}", key, error));
        flowObjectiveService.forward(key.deviceId(), aclObjBuilder.add(aclContext));
    }

    /**
     * Revokes XConnect groups and flows for given key.
     *
     * @param key   XConnect key
     * @param ports XConnect ports
     */
    private void revokeXConnect(XconnectKey key, Set<PortNumber> ports) {
        if (!isLocalLeader(key.deviceId())) {
            log.debug("Abort revoking XConnect {}: {}", key, ERROR_NOT_LEADER);
            return;
        }

        revokeFilter(key, ports);
        int nextId = Versioned.valueOrElse(xconnectNextObjStore.get(key), -1);
        if (nextId != -1) {
            revokeFwd(key, nextId, null);
            revokeNext(key, ports, nextId, null);
        } else {
            log.warn("NextObj for {} does not exist in the store.", key);
        }
        revokeAcl(key);
    }

    /**
     * Revokes filtering objectives for given XConnect.
     *
     * @param key   XConnect store key
     * @param ports XConnect ports
     */
    private void revokeFilter(XconnectKey key, Set<PortNumber> ports) {
        ports.forEach(port -> {
            FilteringObjective.Builder filtObjBuilder = filterObjBuilder(key, port);
            ObjectiveContext context = new DefaultObjectiveContext(
                    (objective) -> log.debug("XConnect FilterObj for {} on port {} revoked",
                                             key, port),
                    (objective, error) ->
                            log.warn("Failed to revoke XConnect FilterObj for {} on port {}: {}",
                                     key, port, error));
            flowObjectiveService.filter(key.deviceId(), filtObjBuilder.remove(context));
        });
    }

    /**
     * Revokes next objectives for given XConnect.
     *
     * @param key        XConnect store key
     * @param ports      ports in the XConnect
     * @param nextId     next objective id
     * @param nextFuture completable future for this next objective operation
     */
    private void revokeNext(XconnectKey key, Set<PortNumber> ports, int nextId,
                            CompletableFuture<ObjectiveError> nextFuture) {
        ObjectiveContext context = new ObjectiveContext() {
            @Override
            public void onSuccess(Objective objective) {
                log.debug("Previous NextObj for {} removed", key);
                if (nextFuture != null) {
                    nextFuture.complete(null);
                }
            }

            @Override
            public void onError(Objective objective, ObjectiveError error) {
                log.warn("Failed to remove previous NextObj for {}: {}", key, error);
                if (nextFuture != null) {
                    nextFuture.complete(error);
                }
                srService.invalidateNextObj(objective.id());
            }
        };
        flowObjectiveService.next(key.deviceId(), nextObjBuilder(key, ports, nextId).remove(context));
        xconnectNextObjStore.remove(key);
    }

    /**
     * Revokes bridging forwarding objectives for given XConnect.
     *
     * @param key       XConnect store key
     * @param nextId    next objective id
     * @param fwdFuture completable future for this forwarding objective operation
     */
    private void revokeFwd(XconnectKey key, int nextId, CompletableFuture<ObjectiveError> fwdFuture) {
        ForwardingObjective.Builder fwdObjBuilder = fwdObjBuilder(key, nextId);
        ObjectiveContext context = new ObjectiveContext() {
            @Override
            public void onSuccess(Objective objective) {
                log.debug("Previous FwdObj for {} removed", key);
                if (fwdFuture != null) {
                    fwdFuture.complete(null);
                }
            }

            @Override
            public void onError(Objective objective, ObjectiveError error) {
                log.warn("Failed to remove previous FwdObj for {}: {}", key, error);
                if (fwdFuture != null) {
                    fwdFuture.complete(error);
                }
            }
        };
        flowObjectiveService.forward(key.deviceId(), fwdObjBuilder.remove(context));
    }

    /**
     * Revokes ACL forwarding objectives for given XConnect.
     *
     * @param key XConnect store key
     */
    private void revokeAcl(XconnectKey key) {
        ForwardingObjective.Builder aclObjBuilder = aclObjBuilder(key.vlanId());
        ObjectiveContext aclContext = new DefaultObjectiveContext(
                (objective) -> log.debug("XConnect AclObj for {} populated", key),
                (objective, error) ->
                        log.warn("Failed to populate XConnect AclObj for {}: {}", key, error));
        flowObjectiveService.forward(key.deviceId(), aclObjBuilder.remove(aclContext));
    }

    /**
     * Updates XConnect groups and flows for given key.
     *
     * @param key       XConnect key
     * @param prevPorts previous XConnect ports
     * @param ports     new XConnect ports
     */
    private void updateXConnect(XconnectKey key, Set<PortNumber> prevPorts,
                                Set<PortNumber> ports) {
        if (!isLocalLeader(key.deviceId())) {
            log.debug("Abort updating XConnect {}: {}", key, ERROR_NOT_LEADER);
            return;
        }
        // NOTE: ACL flow doesn't include port information. No need to update it.
        //       Pair port is built-in and thus not going to change. No need to update it.

        // remove old filter
        prevPorts.stream().filter(port -> !ports.contains(port)).forEach(port ->
                                                                                 revokeFilter(key,
                                                                                              ImmutableSet.of(port)));
        // install new filter
        ports.stream().filter(port -> !prevPorts.contains(port)).forEach(port ->
                                                                                 populateFilter(key,
                                                                                                ImmutableSet.of(port)));

        CompletableFuture<ObjectiveError> fwdFuture = new CompletableFuture<>();
        CompletableFuture<ObjectiveError> nextFuture = new CompletableFuture<>();

        int nextId = Versioned.valueOrElse(xconnectNextObjStore.get(key), -1);
        if (nextId != -1) {
            revokeFwd(key, nextId, fwdFuture);

            fwdFuture.thenAcceptAsync(fwdStatus -> {
                if (fwdStatus == null) {
                    log.debug("Fwd removed. Now remove group {}", key);
                    revokeNext(key, prevPorts, nextId, nextFuture);
                }
            });

            nextFuture.thenAcceptAsync(nextStatus -> {
                if (nextStatus == null) {
                    log.debug("Installing new group and flow for {}", key);
                    populateFwd(key, populateNext(key, ports));
                }
            });
        } else {
            log.warn("NextObj for {} does not exist in the store.", key);
        }
    }

    /**
     * Creates a next objective builder for XConnect with given nextId.
     *
     * @param key    XConnect key
     * @param ports  set of XConnect ports
     * @param nextId next objective id
     * @return next objective builder
     */
    private NextObjective.Builder nextObjBuilder(XconnectKey key, Set<PortNumber> ports, int nextId) {
        TrafficSelector metadata =
                DefaultTrafficSelector.builder().matchVlanId(key.vlanId()).build();
        NextObjective.Builder nextObjBuilder = DefaultNextObjective
                .builder().withId(nextId)
                .withType(NextObjective.Type.BROADCAST).fromApp(appId)
                .withMeta(metadata);
        ports.forEach(port -> {
            TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();
            tBuilder.setOutput(port);
            nextObjBuilder.addTreatment(tBuilder.build());
        });
        return nextObjBuilder;
    }

    /**
     * Creates a next objective builder for XConnect.
     *
     * @param key   XConnect key
     * @param ports set of XConnect ports
     * @return next objective builder
     */
    private NextObjective.Builder nextObjBuilder(XconnectKey key, Set<PortNumber> ports) {
        int nextId = flowObjectiveService.allocateNextId();
        return nextObjBuilder(key, ports, nextId);
    }


    /**
     * Creates a bridging forwarding objective builder for XConnect.
     *
     * @param key    XConnect key
     * @param nextId next ID of the broadcast group for this XConnect key
     * @return forwarding objective builder
     */
    private ForwardingObjective.Builder fwdObjBuilder(XconnectKey key, int nextId) {
        /*
         * Driver should treat objectives with MacAddress.NONE and !VlanId.NONE
         * as the VLAN cross-connect broadcast rules
         */
        TrafficSelector.Builder sbuilder = DefaultTrafficSelector.builder();
        sbuilder.matchVlanId(key.vlanId());
        sbuilder.matchEthDst(MacAddress.NONE);

        ForwardingObjective.Builder fob = DefaultForwardingObjective.builder();
        fob.withFlag(ForwardingObjective.Flag.SPECIFIC)
                .withSelector(sbuilder.build())
                .nextStep(nextId)
                .withPriority(XCONNECT_PRIORITY)
                .fromApp(appId)
                .makePermanent();
        return fob;
    }

    /**
     * Creates an ACL forwarding objective builder for XConnect.
     *
     * @param vlanId cross connect VLAN id
     * @return forwarding objective builder
     */
    private ForwardingObjective.Builder aclObjBuilder(VlanId vlanId) {
        TrafficSelector.Builder sbuilder = DefaultTrafficSelector.builder();
        sbuilder.matchVlanId(vlanId);

        TrafficTreatment.Builder tbuilder = DefaultTrafficTreatment.builder();

        ForwardingObjective.Builder fob = DefaultForwardingObjective.builder();
        fob.withFlag(ForwardingObjective.Flag.VERSATILE)
                .withSelector(sbuilder.build())
                .withTreatment(tbuilder.build())
                .withPriority(XCONNECT_ACL_PRIORITY)
                .fromApp(appId)
                .makePermanent();
        return fob;
    }

    /**
     * Creates a filtering objective builder for XConnect.
     *
     * @param key  XConnect key
     * @param port XConnect ports
     * @return next objective builder
     */
    private FilteringObjective.Builder filterObjBuilder(XconnectKey key, PortNumber port) {
        FilteringObjective.Builder fob = DefaultFilteringObjective.builder();
        fob.withKey(Criteria.matchInPort(port))
                .addCondition(Criteria.matchVlanId(key.vlanId()))
                .addCondition(Criteria.matchEthDst(MacAddress.NONE))
                .withPriority(XCONNECT_PRIORITY);
        return fob.permit().fromApp(appId);
    }

    /**
     * Updates L2 flooding groups; add pair link into L2 flooding group of given xconnect vlan.
     *
     * @param deviceId Device ID
     * @param port     Port details
     * @param vlanId   VLAN ID
     * @param install  Whether to add or revoke pair link addition to flooding group
     */
    private void updateL2Flooding(DeviceId deviceId, PortNumber port, VlanId vlanId, boolean install) {
        XconnectKey key = new XconnectKey(deviceId, vlanId);
        // Ensure leadership on device
        if (!isLocalLeader(deviceId)) {
            log.debug("Abort updating L2Flood {}: {}", key, ERROR_NOT_LEADER);
            return;
        }

        // Locate L2 flooding group details for given xconnect vlan
        int nextId = Versioned.valueOrElse(xconnectNextObjStore.get(key), -1);
        if (nextId == -1) {
            log.debug("XConnect vlan {} broadcast group for device {} doesn't exists. " +
                              "Aborting pair group linking.", vlanId, deviceId);
            return;
        }

        // Add pairing-port group to flooding group
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        // treatment.popVlan();
        treatment.setOutput(port);
        ObjectiveContext context = new DefaultObjectiveContext(
                (objective) ->
                        log.debug("Pair port added/removed to vlan {} next objective {} on {}",
                                  vlanId, nextId, deviceId),
                (objective, error) ->
                        log.warn("Failed adding/removing pair port to vlan {} next objective {} on {}." +
                                         "Error : {}", vlanId, nextId, deviceId, error)
        );
        NextObjective.Builder vlanNextObjectiveBuilder = DefaultNextObjective.builder()
                .withId(nextId)
                .withType(NextObjective.Type.BROADCAST)
                .fromApp(srService.appId())
                .withMeta(DefaultTrafficSelector.builder().matchVlanId(vlanId).build())
                .addTreatment(treatment.build());
        if (install) {
            flowObjectiveService.next(deviceId, vlanNextObjectiveBuilder.addToExisting(context));
        } else {
            flowObjectiveService.next(deviceId, vlanNextObjectiveBuilder.removeFromExisting(context));
        }
        log.debug("Submitted next objective {} for vlan: {} in device {}",
                  nextId, vlanId, deviceId);
    }

    /**
     * Populate L2 multicast rule on given deviceId that matches given mac, given vlan and
     * output to given port's L2 mulitcast group.
     *
     * @param deviceId    Device ID
     * @param pairPort    Pair port number
     * @param vlanId      VLAN ID
     * @param accessPorts List of access ports to be added into L2 multicast group
     */
    private void populateL2Multicast(DeviceId deviceId, PortNumber pairPort,
                                     VlanId vlanId, List<PortNumber> accessPorts) {
        // Ensure enough rights to program pair device
        if (!srService.shouldProgram(deviceId)) {
            log.debug("Abort populate L2Multicast {}-{}: {}", deviceId, vlanId, ERROR_NOT_LEADER);
            return;
        }

        boolean multicastGroupExists = true;
        int vlanMulticastNextId;
        VlanNextObjectiveStoreKey key = new VlanNextObjectiveStoreKey(deviceId, vlanId);

        // Step 1 : Populate single homed access ports into vlan's L2 multicast group
        NextObjective.Builder vlanMulticastNextObjBuilder = DefaultNextObjective
                .builder()
                .withType(NextObjective.Type.BROADCAST)
                .fromApp(srService.appId())
            .withMeta(DefaultTrafficSelector.builder().matchVlanId(vlanId)
                          .matchEthDst(MacAddress.IPV4_MULTICAST).build());
        vlanMulticastNextId = getMulticastGroupNextObjectiveId(key);
        if (vlanMulticastNextId == -1) {
            // Vlan's L2 multicast group doesn't exist; create it, update store and add pair port as sub-group
            multicastGroupExists = false;
            vlanMulticastNextId = flowObjectiveService.allocateNextId();
            addMulticastGroupNextObjectiveId(key, vlanMulticastNextId);
            vlanMulticastNextObjBuilder.addTreatment(
                    DefaultTrafficTreatment.builder().popVlan().setOutput(pairPort).build()
            );
        }
        vlanMulticastNextObjBuilder.withId(vlanMulticastNextId);
        int nextId = vlanMulticastNextId;
        accessPorts.forEach(p -> {
            TrafficTreatment.Builder egressAction = DefaultTrafficTreatment.builder();
            // Do vlan popup action based on interface configuration
            if (interfaceService.getInterfacesByPort(new ConnectPoint(deviceId, p))
                    .stream().noneMatch(i -> i.vlanTagged().contains(vlanId))) {
                egressAction.popVlan();
            }
            egressAction.setOutput(p);
            vlanMulticastNextObjBuilder.addTreatment(egressAction.build());
            addMulticastGroupPort(key, p);
        });
        ObjectiveContext context = new DefaultObjectiveContext(
                (objective) ->
                        log.debug("L2 multicast group installed/updated. "
                                          + "NextObject Id {} on {} for subnet {} ",
                                  nextId, deviceId, vlanId),
                (objective, error) ->
                        log.warn("L2 multicast group failed to install/update. "
                                         + " NextObject Id {} on {} for subnet {} : {}",
                                 nextId, deviceId, vlanId, error)
        );
        if (!multicastGroupExists) {
            flowObjectiveService.next(deviceId, vlanMulticastNextObjBuilder.add(context));

            // Step 2 : Populate ACL rule; selector = vlan + pair-port, output = vlan L2 multicast group
            TrafficSelector.Builder multicastSelector = DefaultTrafficSelector.builder();
            multicastSelector.matchEthType(Ethernet.TYPE_VLAN);
            multicastSelector.matchInPort(pairPort);
            multicastSelector.matchVlanId(vlanId);
            ForwardingObjective.Builder vlanMulticastForwardingObj = DefaultForwardingObjective.builder()
                    .withFlag(ForwardingObjective.Flag.VERSATILE)
                    .nextStep(vlanMulticastNextId)
                    .withSelector(multicastSelector.build())
                    .withPriority(100)
                    .fromApp(srService.appId())
                    .makePermanent();
            context = new DefaultObjectiveContext(
                    (objective) -> log.debug("L2 multicasting versatile rule for device {}, port/vlan {}/{} populated",
                                             deviceId,
                                             pairPort,
                                             vlanId),
                    (objective, error) -> log.warn("Failed to populate L2 multicasting versatile rule for device {}, " +
                                                           "ports/vlan {}/{}: {}", deviceId, pairPort, vlanId, error));
            flowObjectiveService.forward(deviceId, vlanMulticastForwardingObj.add(context));
        } else {
            // L2_MULTICAST & BROADCAST are similar structure in subgroups; so going with BROADCAST type.
            vlanMulticastNextObjBuilder.withType(NextObjective.Type.BROADCAST);
            flowObjectiveService.next(deviceId, vlanMulticastNextObjBuilder.addToExisting(context));
        }
    }

    /**
     * Removes access ports from VLAN L2 multicast group on given deviceId.
     *
     * @param deviceId    Device ID
     * @param vlanId      VLAN ID
     * @param accessPorts List of access ports to be added into L2 multicast group
     */
    private void revokeL2Multicast(DeviceId deviceId, VlanId vlanId, List<PortNumber> accessPorts) {
        // Ensure enough rights to program pair device
        if (!srService.shouldProgram(deviceId)) {
            log.debug("Abort revoke L2Multicast {}-{}: {}", deviceId, vlanId, ERROR_NOT_LEADER);
            return;
        }

        VlanNextObjectiveStoreKey key = new VlanNextObjectiveStoreKey(deviceId, vlanId);

        int vlanMulticastNextId = getMulticastGroupNextObjectiveId(key);
        if (vlanMulticastNextId == -1) {
            return;
        }
        NextObjective.Builder vlanMulticastNextObjBuilder = DefaultNextObjective
                .builder()
                .withType(NextObjective.Type.BROADCAST)
                .fromApp(srService.appId())
                .withMeta(DefaultTrafficSelector.builder().matchVlanId(vlanId).build())
                .withId(vlanMulticastNextId);
        accessPorts.forEach(p -> {
            TrafficTreatment.Builder egressAction = DefaultTrafficTreatment.builder();
            // Do vlan popup action based on interface configuration
            if (interfaceService.getInterfacesByPort(new ConnectPoint(deviceId, p))
                    .stream().noneMatch(i -> i.vlanTagged().contains(vlanId))) {
                egressAction.popVlan();
            }
            egressAction.setOutput(p);
            vlanMulticastNextObjBuilder.addTreatment(egressAction.build());
            removeMulticastGroupPort(key, p);
        });
        ObjectiveContext context = new DefaultObjectiveContext(
                (objective) ->
                        log.debug("L2 multicast group installed/updated. "
                                          + "NextObject Id {} on {} for subnet {} ",
                                  vlanMulticastNextId, deviceId, vlanId),
                (objective, error) ->
                        log.warn("L2 multicast group failed to install/update. "
                                         + " NextObject Id {} on {} for subnet {} : {}",
                                 vlanMulticastNextId, deviceId, vlanId, error)
        );
        flowObjectiveService.next(deviceId, vlanMulticastNextObjBuilder.removeFromExisting(context));
    }

    /**
     * Cleans up VLAN L2 multicast group on given deviceId. ACL rules for the group will also be deleted.
     * Normally multicast group is not removed if it contains access ports; which can be forced
     * by "force" flag
     *
     * @param deviceId Device ID
     * @param pairPort Pair port number
     * @param vlanId   VLAN ID
     * @param force    Forceful removal
     */
    private void cleanupL2MulticastRule(DeviceId deviceId, PortNumber pairPort, VlanId vlanId, boolean force) {

        // Ensure enough rights to program pair device
        if (!srService.shouldProgram(deviceId)) {
            log.debug("Abort cleanup L2Multicast {}-{}: {}", deviceId, vlanId, ERROR_NOT_LEADER);
            return;
        }

        VlanNextObjectiveStoreKey key = new VlanNextObjectiveStoreKey(deviceId, vlanId);

        // Ensure L2 multicast group doesn't contain access ports
        if (hasAccessPortInMulticastGroup(key, pairPort) && !force) {
            return;
        }

        // Load L2 multicast group details
        int vlanMulticastNextId = getMulticastGroupNextObjectiveId(key);
        if (vlanMulticastNextId == -1) {
            return;
        }

        // Step 1 : Clear ACL rule; selector = vlan + pair-port, output = vlan L2 multicast group
        TrafficSelector.Builder l2MulticastSelector = DefaultTrafficSelector.builder();
        l2MulticastSelector.matchEthType(Ethernet.TYPE_VLAN);
        l2MulticastSelector.matchInPort(pairPort);
        l2MulticastSelector.matchVlanId(vlanId);
        ForwardingObjective.Builder vlanMulticastForwardingObj = DefaultForwardingObjective.builder()
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .nextStep(vlanMulticastNextId)
                .withSelector(l2MulticastSelector.build())
                .withPriority(100)
                .fromApp(srService.appId())
                .makePermanent();
        ObjectiveContext context = new DefaultObjectiveContext(
                (objective) -> log.debug("L2 multicasting rule for device {}, port/vlan {}/{} deleted", deviceId,
                                         pairPort, vlanId),
                (objective, error) -> log.warn("Failed to delete L2 multicasting rule for device {}, " +
                                                       "ports/vlan {}/{}: {}", deviceId, pairPort, vlanId, error));
        flowObjectiveService.forward(deviceId, vlanMulticastForwardingObj.remove(context));

        // Step 2 : Clear L2 multicast group associated with vlan
        NextObjective.Builder l2MulticastGroupBuilder = DefaultNextObjective
                .builder()
                .withId(vlanMulticastNextId)
                .withType(NextObjective.Type.BROADCAST)
                .fromApp(srService.appId())
            .withMeta(DefaultTrafficSelector.builder()
                          .matchVlanId(vlanId)
                          .matchEthDst(MacAddress.IPV4_MULTICAST).build())
                .addTreatment(DefaultTrafficTreatment.builder().popVlan().setOutput(pairPort).build());
        context = new DefaultObjectiveContext(
                (objective) ->
                        log.debug("L2 multicast group with NextObject Id {} deleted on {} for subnet {} ",
                                  vlanMulticastNextId, deviceId, vlanId),
                (objective, error) ->
                        log.warn("L2 multicast group with NextObject Id {} failed to delete on {} for subnet {} : {}",
                                 vlanMulticastNextId, deviceId, vlanId, error)
        );
        flowObjectiveService.next(deviceId, l2MulticastGroupBuilder.remove(context));

        // Finally clear store.
        removeMulticastGroup(key);
    }

    private int getMulticastGroupNextObjectiveId(VlanNextObjectiveStoreKey key) {
        return Versioned.valueOrElse(xconnectMulticastNextStore.get(key), -1);
    }

    private void addMulticastGroupNextObjectiveId(VlanNextObjectiveStoreKey key, int nextId) {
        if (nextId == -1) {
            return;
        }
        xconnectMulticastNextStore.put(key, nextId);
    }

    private void addMulticastGroupPort(VlanNextObjectiveStoreKey groupKey, PortNumber port) {
        xconnectMulticastPortsStore.compute(groupKey, (key, ports) -> {
            if (ports == null) {
                ports = Lists.newArrayList();
            }
            ports.add(port);
            return ports;
        });
    }

    private void removeMulticastGroupPort(VlanNextObjectiveStoreKey groupKey, PortNumber port) {
        xconnectMulticastPortsStore.compute(groupKey, (key, ports) -> {
            if (ports != null && !ports.isEmpty()) {
                ports.remove(port);
            }
            return ports;
        });
    }

    private void removeMulticastGroup(VlanNextObjectiveStoreKey groupKey) {
        xconnectMulticastPortsStore.remove(groupKey);
        xconnectMulticastNextStore.remove(groupKey);
    }

    private boolean hasAccessPortInMulticastGroup(VlanNextObjectiveStoreKey groupKey, PortNumber pairPort) {
        List<PortNumber> ports = Versioned.valueOrElse(xconnectMulticastPortsStore.get(groupKey), ImmutableList.of());
        return ports.stream().anyMatch(p -> !p.equals(pairPort));
    }

    // Custom-built function, when the device is not available we need a fallback mechanism
    private boolean isLocalLeader(DeviceId deviceId) {
        if (!mastershipService.isLocalMaster(deviceId)) {
            // When the device is available we just check the mastership
            if (deviceService.isAvailable(deviceId)) {
                return false;
            }
            // Fallback with Leadership service - device id is used as topic
            NodeId leader = leadershipService.runForLeadership(
                    deviceId.toString()).leaderNodeId();
            // Verify if this node is the leader
            return clusterService.getLocalNode().id().equals(leader);
        }
        return true;
    }

}
