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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onlab.util.KryoNamespace;
import org.onosproject.codec.CodecService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
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
import org.onosproject.segmentrouting.SegmentRoutingService;
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
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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
    public MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL_UNARY)
    public SegmentRoutingService srService;

    private static final String APP_NAME = "org.onosproject.xconnect";
    private static final String ERROR_NOT_MASTER = "Not master controller";

    private static Logger log = LoggerFactory.getLogger(XconnectManager.class);

    private ApplicationId appId;
    private ConsistentMap<XconnectKey, Set<PortNumber>> xconnectStore;
    private ConsistentMap<XconnectKey, NextObjective> xconnectNextObjStore;

    private final MapEventListener<XconnectKey, Set<PortNumber>> xconnectListener = new XconnectMapListener();
    private final DeviceListener deviceListener = new InternalDeviceListener();

    @Activate
    void activate() {
        appId = coreService.registerApplication(APP_NAME);
        codecService.registerCodec(XconnectDesc.class, new XconnectCodec());

        KryoNamespace.Builder serializer = KryoNamespace.newBuilder()
                .register(KryoNamespaces.API)
                .register(XconnectKey.class);

        xconnectStore = storageService.<XconnectKey, Set<PortNumber>>consistentMapBuilder()
                .withName("onos-sr-xconnect")
                .withRelaxedReadConsistency()
                .withSerializer(Serializer.using(serializer.build()))
                .build();
        xconnectStore.addListener(xconnectListener);

        xconnectNextObjStore = storageService.<XconnectKey, NextObjective>consistentMapBuilder()
                .withName("onos-sr-xconnect-next")
                .withRelaxedReadConsistency()
                .withSerializer(Serializer.using(serializer.build()))
                .build();

        deviceService.addListener(deviceListener);

        log.info("Started");
    }

    @Deactivate
    void deactivate() {
        xconnectStore.removeListener(xconnectListener);
        deviceService.removeListener(deviceListener);
        codecService.unregisterCodec(XconnectDesc.class);

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
    }

    @Override
    public Set<XconnectDesc> getXconnects() {
        return xconnectStore.asJavaMap().entrySet().stream()
                .map(e -> new XconnectDesc(e.getKey(), e.getValue()))
                .collect(Collectors.toSet());
    }

    @Override
    public boolean hasXconnect(ConnectPoint cp) {
        return getXconnects().stream().anyMatch(desc ->
                desc.key().deviceId().equals(cp.deviceId()) && desc.ports().contains(cp.port())
        );
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
        @Override
        public void event(DeviceEvent event) {
            DeviceId deviceId = event.subject().id();
            if (!mastershipService.isLocalMaster(deviceId)) {
                return;
            }

            switch (event.type()) {
                case DEVICE_ADDED:
                case DEVICE_AVAILABILITY_CHANGED:
                case DEVICE_UPDATED:
                    if (deviceService.isAvailable(deviceId)) {
                        init(deviceId);
                    } else {
                        cleanup(deviceId);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    void init(DeviceId deviceId) {
        getXconnects().stream()
                .filter(desc -> desc.key().deviceId().equals(deviceId))
                .forEach(desc -> populateXConnect(desc.key(), desc.ports()));
    }

    void cleanup(DeviceId deviceId) {
        xconnectNextObjStore.entrySet().stream()
                .filter(entry -> entry.getKey().deviceId().equals(deviceId))
                .forEach(entry -> xconnectNextObjStore.remove(entry.getKey()));
        log.debug("{} is removed from xConnectNextObjStore", deviceId);
    }

    /**
     * Populates XConnect groups and flows for given key.
     *
     * @param key XConnect key
     * @param ports a set of ports to be cross-connected
     */
    private void populateXConnect(XconnectKey key, Set<PortNumber> ports) {
        if (!mastershipService.isLocalMaster(key.deviceId())) {
            log.info("Abort populating XConnect {}: {}", key, ERROR_NOT_MASTER);
            return;
        }

        ports = addPairPort(key.deviceId(), ports);
        populateFilter(key, ports);
        populateFwd(key, populateNext(key, ports));
        populateAcl(key);
    }

    /**
     * Populates filtering objectives for given XConnect.
     *
     * @param key XConnect store key
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
     * @param key XConnect store key
     * @param ports XConnect ports
     */
    private NextObjective populateNext(XconnectKey key, Set<PortNumber> ports) {
        NextObjective nextObj;
        if (xconnectNextObjStore.containsKey(key)) {
            nextObj = xconnectNextObjStore.get(key).value();
            log.debug("NextObj for {} found, id={}", key, nextObj.id());
        } else {
            NextObjective.Builder nextObjBuilder = nextObjBuilder(key, ports);
            ObjectiveContext nextContext = new DefaultObjectiveContext(
                    // To serialize this with kryo
                    (Serializable & Consumer<Objective>) (objective) ->
                            log.debug("XConnect NextObj for {} added", key),
                    (Serializable & BiConsumer<Objective, ObjectiveError>) (objective, error) ->
                            log.warn("Failed to add XConnect NextObj for {}: {}", key, error)
            );
            nextObj = nextObjBuilder.add(nextContext);
            flowObjectiveService.next(key.deviceId(), nextObj);
            xconnectNextObjStore.put(key, nextObj);
            log.debug("NextObj for {} not found. Creating new NextObj with id={}", key, nextObj.id());
        }
        return nextObj;
    }

    /**
     * Populates bridging forwarding objectives for given XConnect.
     *
     * @param key XConnect store key
     * @param nextObj next objective
     */
    private void populateFwd(XconnectKey key, NextObjective nextObj) {
        ForwardingObjective.Builder fwdObjBuilder = fwdObjBuilder(key, nextObj.id());
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
     * @param key XConnect key
     * @param ports XConnect ports
     */
    private void revokeXConnect(XconnectKey key, Set<PortNumber> ports) {
        if (!mastershipService.isLocalMaster(key.deviceId())) {
            log.info("Abort populating XConnect {}: {}", key, ERROR_NOT_MASTER);
            return;
        }

        ports = addPairPort(key.deviceId(), ports);
        revokeFilter(key, ports);
        if (xconnectNextObjStore.containsKey(key)) {
            NextObjective nextObj = xconnectNextObjStore.get(key).value();
            revokeFwd(key, nextObj, null);
            revokeNext(key, nextObj, null);
        } else {
            log.warn("NextObj for {} does not exist in the store.", key);
        }
        revokeAcl(key);
    }

    /**
     * Revokes filtering objectives for given XConnect.
     *
     * @param key XConnect store key
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
     * @param key XConnect store key
     * @param nextObj next objective
     * @param nextFuture completable future for this next objective operation
     */
    private void revokeNext(XconnectKey key, NextObjective nextObj,
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
            }
        };
        flowObjectiveService.next(key.deviceId(),
                (NextObjective) nextObj.copy().remove(context));
        xconnectNextObjStore.remove(key);
    }

    /**
     * Revokes bridging forwarding objectives for given XConnect.
     *
     * @param key XConnect store key
     * @param nextObj next objective
     * @param fwdFuture completable future for this forwarding objective operation
     */
    private void revokeFwd(XconnectKey key, NextObjective nextObj,
                           CompletableFuture<ObjectiveError> fwdFuture) {
        ForwardingObjective.Builder fwdObjBuilder = fwdObjBuilder(key, nextObj.id());
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
     * @param key XConnect key
     * @param prevPorts previous XConnect ports
     * @param ports new XConnect ports
     */
    private void updateXConnect(XconnectKey key, Set<PortNumber> prevPorts,
                                Set<PortNumber> ports) {
        // NOTE: ACL flow doesn't include port information. No need to update it.
        //       Pair port is built-in and thus not going to change. No need to update it.

        // remove old filter
        prevPorts.stream().filter(port -> !ports.contains(port)).forEach(port ->
                revokeFilter(key, ImmutableSet.of(port)));
        // install new filter
        ports.stream().filter(port -> !prevPorts.contains(port)).forEach(port ->
                populateFilter(key, ImmutableSet.of(port)));

        CompletableFuture<ObjectiveError> fwdFuture = new CompletableFuture<>();
        CompletableFuture<ObjectiveError> nextFuture = new CompletableFuture<>();

        if (xconnectNextObjStore.containsKey(key)) {
            NextObjective nextObj = xconnectNextObjStore.get(key).value();
            revokeFwd(key, nextObj, fwdFuture);

            fwdFuture.thenAcceptAsync(fwdStatus -> {
                if (fwdStatus == null) {
                    log.debug("Fwd removed. Now remove group {}", key);
                    revokeNext(key, nextObj, nextFuture);
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
     * Creates a next objective builder for XConnect.
     *
     * @param key XConnect key
     * @param ports set of XConnect ports
     * @return next objective builder
     */
    private NextObjective.Builder nextObjBuilder(XconnectKey key, Set<PortNumber> ports) {
        int nextId = flowObjectiveService.allocateNextId();
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
     * Creates a bridging forwarding objective builder for XConnect.
     *
     * @param key XConnect key
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
     * @param key XConnect key
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
     * Add pair port to the given set of port.
     *
     * @param deviceId device Id
     * @param ports ports specified in the xconnect config
     * @return port specified in the xconnect config plus the pair port (if configured)
     */
    private Set<PortNumber> addPairPort(DeviceId deviceId, Set<PortNumber> ports) {
        if (srService == null) {
            return ports;
        }
        Set<PortNumber> newPorts = Sets.newHashSet(ports);
        srService.getPairLocalPort(deviceId).ifPresent(newPorts::add);
        return newPorts;
    }

    // TODO DEVICE listener
    // up : init
    // down: removeDevice


}
