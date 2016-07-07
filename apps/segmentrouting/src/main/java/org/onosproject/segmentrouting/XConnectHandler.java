/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.segmentrouting;

import com.google.common.collect.ImmutableSet;
import org.onlab.packet.MacAddress;
import org.onlab.util.KryoNamespace;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.config.NetworkConfigEvent;
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
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.NextObjective;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.flowobjective.ObjectiveContext;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.onosproject.segmentrouting.config.XConnectConfig;
import org.onosproject.segmentrouting.storekey.XConnectStoreKey;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Handles cross connect related events.
 */
public class XConnectHandler {
    private static final Logger log = LoggerFactory.getLogger(XConnectHandler.class);
    private static final String CONFIG_NOT_FOUND = "XConnect config not found";
    private static final String NOT_MASTER = "Not master controller";
    private final SegmentRoutingManager srManager;
    private final StorageService storageService;
    private final ConsistentMap<XConnectStoreKey, NextObjective> xConnectNextObjStore;
    private final KryoNamespace.Builder xConnectKryo;

    protected XConnectHandler(SegmentRoutingManager srManager) {
        this.srManager = srManager;
        this.storageService = srManager.storageService;
        xConnectKryo = new KryoNamespace.Builder()
                .register(KryoNamespaces.API)
                .register(XConnectStoreKey.class)
                .register(NextObjContext.class);
        xConnectNextObjStore = storageService
                .<XConnectStoreKey, NextObjective>consistentMapBuilder()
                .withName("onos-xconnect-nextobj-store")
                .withSerializer(Serializer.using(xConnectKryo.build()))
                .build();
    }

    /**
     * Read initial XConnect for given device.
     *
     * @param deviceId ID of the device to be initialized
     */
    public void init(DeviceId deviceId) {
        // Try to read XConnect config
        XConnectConfig config =
                srManager.cfgService.getConfig(srManager.appId, XConnectConfig.class);
        if (config == null) {
            log.info("Skip XConnect initialization: {}", CONFIG_NOT_FOUND);
            return;
        }

        config.getXconnects(deviceId).forEach(key -> {
            populateXConnect(key, config.getPorts(key));
        });
    }

    /**
     * Processes Segment Routing App Config added event.
     *
     * @param event network config added event
     */
    protected void processXConnectConfigAdded(NetworkConfigEvent event) {
        log.info("Processing XConnect CONFIG_ADDED");
        XConnectConfig config = (XConnectConfig) event.config().get();
        config.getXconnects().forEach(key -> {
            populateXConnect(key, config.getPorts(key));
        });
    }

    /**
     * Processes Segment Routing App Config updated event.
     *
     * @param event network config updated event
     */
    protected void processXConnectConfigUpdated(NetworkConfigEvent event) {
        log.info("Processing XConnect CONFIG_UPDATED");
        XConnectConfig prevConfig = (XConnectConfig) event.prevConfig().get();
        XConnectConfig config = (XConnectConfig) event.config().get();
        Set<XConnectStoreKey> prevKeys = prevConfig.getXconnects();
        Set<XConnectStoreKey> keys = config.getXconnects();

        Set<XConnectStoreKey> pendingRemove = prevKeys.stream()
                .filter(key -> !keys.contains(key)).collect(Collectors.toSet());
        Set<XConnectStoreKey> pendingAdd = keys.stream()
                .filter(key -> !prevKeys.contains(key)).collect(Collectors.toSet());
        Set<XConnectStoreKey> pendingUpdate = keys.stream()
                .filter(key -> prevKeys.contains(key) &&
                        !config.getPorts(key).equals(prevConfig.getPorts(key)))
                .collect(Collectors.toSet());

        pendingRemove.forEach(key -> {
            revokeXConnect(key, prevConfig.getPorts(key));
        });
        pendingAdd.forEach(key -> {
            populateXConnect(key, config.getPorts(key));
        });
        pendingUpdate.forEach(key -> {
            updateXConnect(key, prevConfig.getPorts(key), config.getPorts(key));
        });
    }

    /**
     * Processes Segment Routing App Config removed event.
     *
     * @param event network config removed event
     */
    protected void processXConnectConfigRemoved(NetworkConfigEvent event) {
        log.info("Processing XConnect CONFIG_REMOVED");
        XConnectConfig prevConfig = (XConnectConfig) event.prevConfig().get();
        prevConfig.getXconnects().forEach(key -> {
            revokeXConnect(key, prevConfig.getPorts(key));
        });
    }

    /**
     * Checks if there is any XConnect configured on given connect point.
     *
     * @param cp connect point
     * @return true if there is XConnect configured on given connect point.
     */
    public boolean hasXConnect(ConnectPoint cp) {
        // Try to read XConnect config
        XConnectConfig config =
                srManager.cfgService.getConfig(srManager.appId, XConnectConfig.class);
        if (config == null) {
            log.warn("Failed to read XConnect config: {}", CONFIG_NOT_FOUND);
            return false;
        }
        return config.getXconnects(cp.deviceId()).stream()
                .anyMatch(key -> config.getPorts(key).contains(cp.port()));
    }

    /**
     * Populates XConnect groups and flows for given key.
     *
     * @param key XConnect key
     * @param ports a set of ports to be cross-connected
     */
    private void populateXConnect(XConnectStoreKey key, Set<PortNumber> ports) {
        if (!srManager.mastershipService.isLocalMaster(key.deviceId())) {
            log.info("Abort populating XConnect {}: {}", key, NOT_MASTER);
            return;
        }
        populateFilter(key, ports);
        populateFwd(key, populateNext(key, ports));
    }

    /**
     * Populates filtering objectives for given XConnect.
     *
     * @param key XConnect store key
     * @param ports XConnect ports
     */
    private void populateFilter(XConnectStoreKey key, Set<PortNumber> ports) {
        ports.forEach(port -> {
            FilteringObjective.Builder filtObjBuilder = filterObjBuilder(key, port);
            ObjectiveContext context = new DefaultObjectiveContext(
                    (objective) -> log.debug("XConnect FilterObj for {} on port {} populated",
                            key, port),
                    (objective, error) ->
                            log.warn("Failed to populate XConnect FilterObj for {} on port {}: {}",
                                    key, port, error));
            srManager.flowObjectiveService.filter(key.deviceId(), filtObjBuilder.add(context));
        });
    }

    /**
     * Populates next objectives for given XConnect.
     *
     * @param key XConnect store key
     * @param ports XConnect ports
     */
    private NextObjective populateNext(XConnectStoreKey key, Set<PortNumber> ports) {
        NextObjective nextObj = null;
        if (xConnectNextObjStore.containsKey(key)) {
            nextObj = xConnectNextObjStore.get(key).value();
            log.debug("NextObj for {} found, id={}", key, nextObj.id());
        } else {
            NextObjective.Builder nextObjBuilder = nextObjBuilder(key, ports);
            ObjectiveContext nextContext = new NextObjContext(Objective.Operation.ADD, key);
            nextObj = nextObjBuilder.add(nextContext);
            srManager.flowObjectiveService.next(key.deviceId(), nextObj);
            xConnectNextObjStore.put(key, nextObj);
            log.debug("NextObj for {} not found. Creating new NextObj with id={}", key, nextObj.id());
        }
        return nextObj;
    }

    /**
     * Populates forwarding objectives for given XConnect.
     *
     * @param key XConnect store key
     * @param nextObj next objective
     */
    private void populateFwd(XConnectStoreKey key, NextObjective nextObj) {
        ForwardingObjective.Builder fwdObjBuilder = fwdObjBuilder(key, nextObj.id());
        ObjectiveContext fwdContext = new DefaultObjectiveContext(
                (objective) -> log.debug("XConnect FwdObj for {} populated", key),
                (objective, error) ->
                        log.warn("Failed to populate XConnect FwdObj for {}: {}", key, error));
        srManager.flowObjectiveService.forward(key.deviceId(), fwdObjBuilder.add(fwdContext));
    }

    /**
     * Revokes XConnect groups and flows for given key.
     *
     * @param key XConnect key
     * @param ports XConnect ports
     */
    private void revokeXConnect(XConnectStoreKey key, Set<PortNumber> ports) {
        if (!srManager.mastershipService.isLocalMaster(key.deviceId())) {
            log.info("Abort populating XConnect {}: {}", key, NOT_MASTER);
            return;
        }

        revokeFilter(key, ports);
        if (xConnectNextObjStore.containsKey(key)) {
            NextObjective nextObj = xConnectNextObjStore.get(key).value();
            revokeFwd(key, nextObj, null);
            revokeNext(key, nextObj, null);
        } else {
            log.warn("NextObj for {} does not exist in the store.", key);
        }
    }

    /**
     * Revokes filtering objectives for given XConnect.
     *
     * @param key XConnect store key
     * @param ports XConnect ports
     */
    private void revokeFilter(XConnectStoreKey key, Set<PortNumber> ports) {
        ports.forEach(port -> {
            FilteringObjective.Builder filtObjBuilder = filterObjBuilder(key, port);
            ObjectiveContext context = new DefaultObjectiveContext(
                    (objective) -> log.debug("XConnect FilterObj for {} on port {} revoked",
                            key, port),
                    (objective, error) ->
                            log.warn("Failed to revoke XConnect FilterObj for {} on port {}: {}",
                                    key, port, error));
            srManager.flowObjectiveService.filter(key.deviceId(), filtObjBuilder.remove(context));
        });
    }

    /**
     * Revokes next objectives for given XConnect.
     *
     * @param key XConnect store key
     * @param nextObj next objective
     * @param nextFuture completable future for this next objective operation
     */
    private void revokeNext(XConnectStoreKey key, NextObjective nextObj,
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
        srManager.flowObjectiveService.next(key.deviceId(),
                (NextObjective) nextObj.copy().remove(context));
        xConnectNextObjStore.remove(key);
    }

    /**
     * Revokes forwarding objectives for given XConnect.
     *
     * @param key XConnect store key
     * @param nextObj next objective
     * @param fwdFuture completable future for this forwarding objective operation
     */
    private void revokeFwd(XConnectStoreKey key, NextObjective nextObj,
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
        srManager.flowObjectiveService
                .forward(key.deviceId(), fwdObjBuilder.remove(context));
    }

    /**
     * Updates XConnect groups and flows for given key.
     *
     * @param key XConnect key
     * @param prevPorts previous XConnect ports
     * @param ports new XConnect ports
     */
    private void updateXConnect(XConnectStoreKey key, Set<PortNumber> prevPorts,
            Set<PortNumber> ports) {
        // remove old filter
        prevPorts.stream().filter(port -> !ports.contains(port)).forEach(port -> {
            revokeFilter(key, ImmutableSet.of(port));
        });
        // install new filter
        ports.stream().filter(port -> !prevPorts.contains(port)).forEach(port -> {
            populateFilter(key, ImmutableSet.of(port));
        });

        CompletableFuture<ObjectiveError> fwdFuture = new CompletableFuture<>();
        CompletableFuture<ObjectiveError> nextFuture = new CompletableFuture<>();

        if (xConnectNextObjStore.containsKey(key)) {
            NextObjective nextObj = xConnectNextObjStore.get(key).value();
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
     * Remove all groups on given device.
     *
     * @param deviceId device ID
     */
    protected void removeDevice(DeviceId deviceId) {
        xConnectNextObjStore.entrySet().stream()
                .filter(entry -> entry.getKey().deviceId().equals(deviceId))
                .forEach(entry -> {
                    xConnectNextObjStore.remove(entry.getKey());
                });
        log.debug("{} is removed from xConnectNextObjStore", deviceId);
    }

    /**
     * Creates a next objective builder for XConnect.
     *
     * @param key XConnect key
     * @param ports set of XConnect ports
     * @return next objective builder
     */
    private NextObjective.Builder nextObjBuilder(XConnectStoreKey key, Set<PortNumber> ports) {
        int nextId = srManager.flowObjectiveService.allocateNextId();
        TrafficSelector metadata =
                DefaultTrafficSelector.builder().matchVlanId(key.vlanId()).build();
        NextObjective.Builder nextObjBuilder = DefaultNextObjective
                .builder().withId(nextId)
                .withType(NextObjective.Type.BROADCAST).fromApp(srManager.appId)
                .withMeta(metadata);
        ports.forEach(port -> {
            TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();
            tBuilder.setOutput(port);
            nextObjBuilder.addTreatment(tBuilder.build());
        });
        return nextObjBuilder;
    }

    /**
     * Creates a forwarding objective builder for XConnect.
     *
     * @param key XConnect key
     * @param nextId next ID of the broadcast group for this XConnect key
     * @return next objective builder
     */
    private ForwardingObjective.Builder fwdObjBuilder(XConnectStoreKey key, int nextId) {
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
                .withPriority(SegmentRoutingService.XCONNECT_PRIORITY)
                .fromApp(srManager.appId)
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
    private FilteringObjective.Builder filterObjBuilder(XConnectStoreKey key, PortNumber port) {
        FilteringObjective.Builder fob = DefaultFilteringObjective.builder();
        fob.withKey(Criteria.matchInPort(port))
                .addCondition(Criteria.matchVlanId(key.vlanId()))
                .addCondition(Criteria.matchEthDst(MacAddress.NONE))
                .withPriority(SegmentRoutingService.XCONNECT_PRIORITY);
        return fob.permit().fromApp(srManager.appId);
    }

    // TODO: Lambda closure in DefaultObjectiveContext cannot be serialized properly
    //       with Kryo 3.0.3. It will be fixed in 3.0.4. By then we can use
    //       DefaultObjectiveContext again.
    private final class NextObjContext implements ObjectiveContext {
        Objective.Operation op;
        XConnectStoreKey key;

        private NextObjContext(Objective.Operation op, XConnectStoreKey key) {
            this.op = op;
            this.key = key;
        }

        @Override
        public void onSuccess(Objective objective) {
            log.debug("XConnect NextObj for {} {}ED", key, op);
        }

        @Override
        public void onError(Objective objective, ObjectiveError error) {
            log.warn("Failed to {} XConnect NextObj for {}: {}", op, key, error);
        }
    }
}
