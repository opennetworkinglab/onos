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

package org.onosproject.l2lb.app;

import com.google.common.collect.Sets;
import org.onlab.util.KryoNamespace;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.l2lb.api.L2Lb;
import org.onosproject.l2lb.api.L2LbData;
import org.onosproject.l2lb.api.L2LbEvent;
import org.onosproject.l2lb.api.L2LbAdminService;
import org.onosproject.l2lb.api.L2LbId;
import org.onosproject.l2lb.api.L2LbListener;
import org.onosproject.l2lb.api.L2LbMode;
import org.onosproject.l2lb.api.L2LbService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.flowobjective.DefaultNextObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.NextObjective;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.flowobjective.ObjectiveContext;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.onosproject.net.intf.InterfaceService;
import org.onosproject.net.packet.PacketService;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.Versioned;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static org.onlab.util.Tools.groupedThreads;
import static org.slf4j.LoggerFactory.getLogger;

@Component(
    immediate = true,
    service = {
        L2LbService.class,
        L2LbAdminService.class
    }
)
public class L2LbManager implements L2LbService, L2LbAdminService {

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private InterfaceService interfaceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private FlowObjectiveService flowObjService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private DeviceService deviceService;

    private static final Logger log = getLogger(L2LbManager.class);
    private static final String APP_NAME = "org.onosproject.l2lb";

    private ApplicationId appId;
    private ConsistentMap<L2LbId, L2Lb> l2LbStore;
    private ConsistentMap<L2LbId, Integer> l2LbNextStore;
    // TODO Evaluate if ResourceService is a better option
    private ConsistentMap<L2LbId, ApplicationId> l2LbResStore;
    private Set<L2LbListener> listeners = Sets.newConcurrentHashSet();

    private ExecutorService l2LbEventExecutor;
    private ExecutorService l2LbProvExecutor;
    private ExecutorService deviceEventExecutor;

    private MapEventListener<L2LbId, L2Lb> l2LbStoreListener;
    // TODO build CLI to view and clear the next store
    private MapEventListener<L2LbId, Integer> l2LbNextStoreListener;
    private MapEventListener<L2LbId, ApplicationId> l2LbResStoreListener;
    private final DeviceListener deviceListener = new InternalDeviceListener();

    @Activate
    public void activate() {
        appId = coreService.registerApplication(APP_NAME);

        l2LbEventExecutor = Executors.newSingleThreadExecutor(
                groupedThreads("l2lb-event", "%d", log));
        l2LbProvExecutor = Executors.newSingleThreadExecutor(
                groupedThreads("l2lb-prov", "%d", log));
        deviceEventExecutor = Executors.newSingleThreadScheduledExecutor(
                groupedThreads("l2lb-dev-event", "%d", log));
        l2LbStoreListener = new L2LbStoreListener();
        l2LbNextStoreListener = new L2LbNextStoreListener();
        l2LbResStoreListener = new L2LbResStoreListener();

        KryoNamespace serializer = KryoNamespace.newBuilder()
                .register(KryoNamespaces.API)
                .register(L2Lb.class)
                .register(L2LbId.class)
                .register(L2LbMode.class)
                .build();
        l2LbStore = storageService.<L2LbId, L2Lb>consistentMapBuilder()
                .withName("onos-l2lb-store")
                .withRelaxedReadConsistency()
                .withSerializer(Serializer.using(serializer))
                .build();
        l2LbStore.addListener(l2LbStoreListener);
        l2LbNextStore = storageService.<L2LbId, Integer>consistentMapBuilder()
                .withName("onos-l2lb-next-store")
                .withRelaxedReadConsistency()
                .withSerializer(Serializer.using(serializer))
                .build();
        l2LbNextStore.addListener(l2LbNextStoreListener);
        l2LbResStore = storageService.<L2LbId, ApplicationId>consistentMapBuilder()
                .withName("onos-l2lb-res-store")
                .withRelaxedReadConsistency()
                .withSerializer(Serializer.using(serializer))
                .build();
        l2LbResStore.addListener(l2LbResStoreListener);

        deviceService.addListener(deviceListener);

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        l2LbStore.removeListener(l2LbStoreListener);
        l2LbNextStore.removeListener(l2LbNextStoreListener);

        l2LbEventExecutor.shutdown();
        l2LbProvExecutor.shutdown();
        deviceEventExecutor.shutdown();

        log.info("Stopped");
    }

    @Override
    public void addListener(L2LbListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(L2LbListener listener) {
        listeners.remove(listener);
    }

    @Override
    public L2Lb createOrUpdate(DeviceId deviceId, int key, Set<PortNumber> ports, L2LbMode mode) {
        L2LbId l2LbId = new L2LbId(deviceId, key);
        log.debug("Putting {} -> {} {} into L2 load balancer store", l2LbId, mode, ports);
        return Versioned.valueOrNull(l2LbStore.put(l2LbId, new L2Lb(l2LbId, ports, mode)));
    }

    @Override
    public L2Lb remove(DeviceId deviceId, int key) {
        L2LbId l2LbId = new L2LbId(deviceId, key);
        ApplicationId reservation = Versioned.valueOrNull(l2LbResStore.get(l2LbId));
        // Remove only if it is not used - otherwise it is necessary to release first
        if (reservation == null) {
            log.debug("Removing {} from L2 load balancer store", l2LbId);
            return Versioned.valueOrNull(l2LbStore.remove(l2LbId));
        }
        log.warn("Removal {} from L2 load balancer store was not possible " +
                          "due to a previous reservation", l2LbId);
        return null;
    }

    @Override
    public Map<L2LbId, L2Lb> getL2Lbs() {
        return l2LbStore.asJavaMap();
    }

    @Override
    public L2Lb getL2Lb(DeviceId deviceId, int key) {
        return Versioned.valueOrNull(l2LbStore.get(new L2LbId(deviceId, key)));
    }

    @Override
    public Map<L2LbId, Integer> getL2LbNexts() {
        return l2LbNextStore.asJavaMap();
    }

    @Override
    public int getL2LbNext(DeviceId deviceId, int key) {
        return Versioned.valueOrNull(l2LbNextStore.get(new L2LbId(deviceId, key)));
    }

    @Override
    public boolean reserve(L2LbId l2LbId, ApplicationId appId) {
        // Check if the resource is available
        ApplicationId reservation = Versioned.valueOrNull(l2LbResStore.get(l2LbId));
        L2Lb l2Lb = Versioned.valueOrNull(l2LbStore.get(l2LbId));
        if (reservation == null && l2Lb != null) {
            log.debug("Reserving {} -> {} into L2 load balancer reservation store", l2LbId, appId);
            return l2LbResStore.put(l2LbId, appId) == null;
        } else if (reservation != null && reservation.equals(appId)) {
            // App try to reserve the resource a second time
            log.debug("Already reserved {} -> {} skip reservation", l2LbId, appId);
            return true;
        }
        log.warn("Reservation failed {} -> {}", l2LbId, appId);
        return false;
    }

    @Override
    public boolean release(L2LbId l2LbId, ApplicationId appId) {
        // Check if the resource is reserved
        ApplicationId reservation = Versioned.valueOrNull(l2LbResStore.get(l2LbId));
        if (reservation != null && reservation.equals(appId)) {
            log.debug("Removing {} -> {} from L2 load balancer reservation store", l2LbId, appId);
            return l2LbResStore.remove(l2LbId) != null;
        }
        log.warn("Release failed {} -> {}", l2LbId, appId);
        return false;
    }

    @Override
    public ApplicationId getReservation(L2LbId l2LbId) {
        return Versioned.valueOrNull(l2LbResStore.get(l2LbId));
    }

    @Override
    public Map<L2LbId, ApplicationId> getReservations() {
        return l2LbResStore.asJavaMap();
    }

    private class L2LbStoreListener implements MapEventListener<L2LbId, L2Lb> {
        public void event(MapEvent<L2LbId, L2Lb> event) {
            switch (event.type()) {
                case INSERT:
                    log.debug("L2Lb {} insert new={}, old={}", event.key(), event.newValue(), event.oldValue());
                    post(new L2LbEvent(L2LbEvent.Type.ADDED, event.newValue().value().data(), null));
                    populateL2Lb(event.newValue().value());
                    break;
                case REMOVE:
                    log.debug("L2Lb {} remove new={}, old={}", event.key(), event.newValue(), event.oldValue());
                    post(new L2LbEvent(L2LbEvent.Type.REMOVED, null, event.oldValue().value().data()));
                    revokeL2Lb(event.oldValue().value());
                    break;
                case UPDATE:
                    log.debug("L2Lb {} update new={}, old={}", event.key(), event.newValue(), event.oldValue());
                    post(new L2LbEvent(L2LbEvent.Type.UPDATED, event.newValue().value().data(),
                            event.oldValue().value().data()));
                    updateL2Lb(event.newValue().value(), event.oldValue().value());
                    break;
                default:
                    break;
            }
        }
    }

    private class L2LbNextStoreListener implements MapEventListener<L2LbId, Integer> {
        public void event(MapEvent<L2LbId, Integer> event) {
            switch (event.type()) {
                case INSERT:
                    log.debug("L2Lb next {} insert new={}, old={}", event.key(), event.newValue(), event.oldValue());
                    break;
                case REMOVE:
                    log.debug("L2Lb next {} remove new={}, old={}", event.key(), event.newValue(), event.oldValue());
                    break;
                case UPDATE:
                    log.debug("L2Lb next {} update new={}, old={}", event.key(), event.newValue(), event.oldValue());
                    break;
                default:
                    break;
            }
        }
    }

    private class L2LbResStoreListener implements MapEventListener<L2LbId, ApplicationId> {
        public void event(MapEvent<L2LbId, ApplicationId> event) {
            switch (event.type()) {
                case INSERT:
                    log.debug("L2Lb reservation {} insert new={}, old={}", event.key(), event.newValue(),
                              event.oldValue());
                    break;
                case REMOVE:
                    log.debug("L2Lb reservation {} remove new={}, old={}", event.key(), event.newValue(),
                              event.oldValue());
                    break;
                case UPDATE:
                    log.debug("L2Lb reservation {} update new={}, old={}", event.key(), event.newValue(),
                              event.oldValue());
                    break;
                default:
                    break;
            }
        }
    }

    private class InternalDeviceListener implements DeviceListener {
        // We want to manage only a subset of events and if we are the leader
        @Override
        public void event(DeviceEvent event) {
            deviceEventExecutor.execute(() -> {
                DeviceId deviceId = event.subject().id();
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
        // Some events related to the devices are skipped
        @Override
        public boolean isRelevant(DeviceEvent event) {
            return event.type() == DeviceEvent.Type.DEVICE_ADDED ||
                    event.type() == DeviceEvent.Type.DEVICE_AVAILABILITY_CHANGED ||
                    event.type() == DeviceEvent.Type.DEVICE_UPDATED;
        }
    }

    private void post(L2LbEvent l2LbEvent) {
        l2LbEventExecutor.execute(() -> {
            for (L2LbListener l : listeners) {
                if (l.isRelevant(l2LbEvent)) {
                    l.event(l2LbEvent);
                }
            }
        });
    }

    private void init(DeviceId deviceId) {
        l2LbStore.entrySet().stream()
                .filter(l2lbentry -> l2lbentry.getKey().deviceId().equals(deviceId))
                .forEach(l2lbentry -> populateL2Lb(l2lbentry.getValue().value()));
    }

    private void cleanup(DeviceId deviceId) {
        l2LbStore.entrySet().stream()
                .filter(entry -> entry.getKey().deviceId().equals(deviceId))
                .forEach(entry -> l2LbNextStore.remove(entry.getKey()));
        log.debug("{} is removed from l2LbNextObjStore", deviceId);
    }

    private void populateL2Lb(L2Lb l2Lb) {
        DeviceId deviceId = l2Lb.l2LbId().deviceId();
        if (!isLocalLeader(deviceId)) {
            log.debug("Not the leader of {}. Skip populateL2Lb {}", deviceId, l2Lb.l2LbId());
            return;
        }

        l2LbProvExecutor.execute(() -> {
            Integer nextid = Versioned.valueOrNull(l2LbNextStore.get(l2Lb.l2LbId()));
            if (nextid == null) {
                // Build a new context and new next objective
                L2LbObjectiveContext context = new L2LbObjectiveContext(l2Lb.l2LbId());
                NextObjective nextObj = nextObjBuilder(l2Lb.l2LbId(), l2Lb.ports(), nextid).add(context);
                // Finally submit, store, and register the resource
                flowObjService.next(deviceId, nextObj);
                l2LbNextStore.put(l2Lb.l2LbId(), nextObj.id());
            } else {
                log.info("NextObj for {} already exists. Skip populateL2Lb", l2Lb.l2LbId());
            }
        });
    }

    private void revokeL2Lb(L2Lb l2Lb) {
        DeviceId deviceId = l2Lb.l2LbId().deviceId();
        if (!isLocalLeader(deviceId)) {
            log.debug("Not the leader of {}. Skip revokeL2Lb {}", deviceId, l2Lb.l2LbId());
            return;
        }

        l2LbProvExecutor.execute(() -> {
            Integer nextid = Versioned.valueOrNull(l2LbNextStore.get(l2Lb.l2LbId()));
            if (nextid != null) {
                // Build a new context and remove old next objective
                L2LbObjectiveContext context = new L2LbObjectiveContext(l2Lb.l2LbId());
                NextObjective nextObj = nextObjBuilder(l2Lb.l2LbId(), l2Lb.ports(), nextid).remove(context);
                // Finally submit and invalidate the store
                flowObjService.next(deviceId, nextObj);
                l2LbNextStore.remove(l2Lb.l2LbId());
            } else {
                log.info("NextObj for {} does not exist. Skip revokeL2Lb", l2Lb.l2LbId());
            }
        });
    }

    private void updateL2Lb(L2Lb newL2Lb, L2Lb oldL2Lb) {
        DeviceId deviceId = newL2Lb.l2LbId().deviceId();
        if (!isLocalLeader(deviceId)) {
            log.debug("Not the leader of {}. Skip updateL2Lb {}", deviceId, newL2Lb.l2LbId());
            return;
        }

        l2LbProvExecutor.execute(() -> {
            Integer nextid = Versioned.valueOrNull(l2LbNextStore.get(newL2Lb.l2LbId()));
            if (nextid != null) {
                // Compute modifications and context
                L2LbObjectiveContext context = new L2LbObjectiveContext(newL2Lb.l2LbId());
                Set<PortNumber> portsToBeAdded = Sets.difference(newL2Lb.ports(), oldL2Lb.ports());
                Set<PortNumber> portsToBeRemoved = Sets.difference(oldL2Lb.ports(), newL2Lb.ports());
                // and send them to the flowobj subsystem
                if (!portsToBeAdded.isEmpty()) {
                    flowObjService.next(deviceId, nextObjBuilder(newL2Lb.l2LbId(), portsToBeAdded, nextid)
                            .addToExisting(context));
                } else {
                    log.debug("NextObj for {} nothing to add", newL2Lb.l2LbId());

                }
                if (!portsToBeRemoved.isEmpty()) {
                    flowObjService.next(deviceId, nextObjBuilder(newL2Lb.l2LbId(), portsToBeRemoved, nextid)
                            .removeFromExisting(context));
                } else {
                    log.debug("NextObj for {} nothing to remove", newL2Lb.l2LbId());
                }
            } else {
                log.info("NextObj for {} does not exist. Skip updateL2Lb", newL2Lb.l2LbId());
            }
        });
    }

    private NextObjective.Builder nextObjBuilder(L2LbId l2LbId, Set<PortNumber> ports, Integer nextId) {
        if (nextId == null) {
            nextId = flowObjService.allocateNextId();
        }
        // TODO replace logical l2lb port
        TrafficSelector meta = DefaultTrafficSelector.builder()
                .matchInPort(PortNumber.portNumber(l2LbId.key())).build();
        NextObjective.Builder nextObjBuilder = DefaultNextObjective.builder()
                .withId(nextId)
                .withMeta(meta)
                .withType(NextObjective.Type.HASHED)
                .fromApp(appId);
        ports.forEach(port -> {
            TrafficTreatment treatment = DefaultTrafficTreatment.builder().setOutput(port).build();
            nextObjBuilder.addTreatment(treatment);
        });
        return nextObjBuilder;
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

    private final class L2LbObjectiveContext implements ObjectiveContext {
        private final L2LbId l2LbId;

        private L2LbObjectiveContext(L2LbId l2LbId) {
            this.l2LbId = l2LbId;
        }

        @Override
        public void onSuccess(Objective objective) {
            NextObjective nextObj = (NextObjective) objective;
            log.debug("Success {} nextobj {} for L2 load balancer {}", nextObj.op(), nextObj, l2LbId);
            l2LbProvExecutor.execute(() -> onSuccessHandler(nextObj, l2LbId));
        }

        @Override
        public void onError(Objective objective, ObjectiveError error) {
            NextObjective nextObj = (NextObjective) objective;
            log.debug("Failed {} nextobj {} for L2 load balancer {} due to {}", nextObj.op(), nextObj,
                      l2LbId, error);
            l2LbProvExecutor.execute(() -> onErrorHandler(nextObj, l2LbId));
        }
    }

    private void onSuccessHandler(NextObjective nextObjective, L2LbId l2LbId) {
        // Operation done
        L2LbData oldl2LbData = new L2LbData(l2LbId);
        L2LbData newl2LbData = new L2LbData(l2LbId);
        // Other operations will not lead to a generation of an event
        switch (nextObjective.op()) {
            case ADD:
                newl2LbData.setNextId(nextObjective.id());
                post(new L2LbEvent(L2LbEvent.Type.INSTALLED, newl2LbData, oldl2LbData));
                break;
            case REMOVE:
                oldl2LbData.setNextId(nextObjective.id());
                post(new L2LbEvent(L2LbEvent.Type.UNINSTALLED, newl2LbData, oldl2LbData));
                break;
            default:
                break;
        }
    }

    private void onErrorHandler(NextObjective nextObjective, L2LbId l2LbId) {
        // There was a failure
        L2LbData l2LbData = new L2LbData(l2LbId);
        // send FAILED event;
        switch (nextObjective.op()) {
            case ADD:
                // If ADD is failing apps do not know the next id; let's update the store
                l2LbNextStore.remove(l2LbId);
                l2LbResStore.remove(l2LbId);
                l2LbStore.remove(l2LbId);
                post(new L2LbEvent(L2LbEvent.Type.FAILED, l2LbData, l2LbData));
                break;
            case ADD_TO_EXISTING:
                // If ADD_TO_EXISTING is failing let's remove the failed ports
                Collection<PortNumber> addedPorts = nextObjective.next().stream()
                        .flatMap(t -> t.allInstructions().stream())
                        .filter(i -> i.type() == Instruction.Type.OUTPUT)
                        .map(i -> ((Instructions.OutputInstruction) i).port())
                        .collect(Collectors.toList());
                l2LbStore.compute(l2LbId, (key, value) -> {
                    if (value != null && value.ports() != null && !value.ports().isEmpty()) {
                        value.ports().removeAll(addedPorts);
                    }
                    return value;
                });
                l2LbData.setNextId(nextObjective.id());
                post(new L2LbEvent(L2LbEvent.Type.FAILED, l2LbData, l2LbData));
                break;
            case REMOVE_FROM_EXISTING:
                // If REMOVE_TO_EXISTING is failing let's re-add the failed ports
                Collection<PortNumber> removedPorts = nextObjective.next().stream()
                        .flatMap(t -> t.allInstructions().stream())
                        .filter(i -> i.type() == Instruction.Type.OUTPUT)
                        .map(i -> ((Instructions.OutputInstruction) i).port())
                        .collect(Collectors.toList());
                l2LbStore.compute(l2LbId, (key, value) -> {
                    if (value != null && value.ports() != null) {
                        value.ports().addAll(removedPorts);
                    }
                    return value;
                });
                l2LbData.setNextId(nextObjective.id());
                post(new L2LbEvent(L2LbEvent.Type.FAILED, l2LbData, l2LbData));
                break;
            case VERIFY:
            case REMOVE:
                // If ADD/REMOVE_TO_EXISTING, REMOVE and VERIFY are failing let's send
                // also the info about the next id
                l2LbData.setNextId(nextObjective.id());
                post(new L2LbEvent(L2LbEvent.Type.FAILED, l2LbData, l2LbData));
                break;
            default:
                break;
        }

    }
}