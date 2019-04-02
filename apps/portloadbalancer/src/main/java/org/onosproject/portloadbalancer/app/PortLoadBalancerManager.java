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

package org.onosproject.portloadbalancer.app;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.onlab.util.KryoNamespace;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.portloadbalancer.api.PortLoadBalancer;
import org.onosproject.portloadbalancer.api.PortLoadBalancerData;
import org.onosproject.portloadbalancer.api.PortLoadBalancerEvent;
import org.onosproject.portloadbalancer.api.PortLoadBalancerAdminService;
import org.onosproject.portloadbalancer.api.PortLoadBalancerId;
import org.onosproject.portloadbalancer.api.PortLoadBalancerListener;
import org.onosproject.portloadbalancer.api.PortLoadBalancerMode;
import org.onosproject.portloadbalancer.api.PortLoadBalancerService;
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
import org.onosproject.net.flowobjective.DefaultNextTreatment;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.NextObjective;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.flowobjective.ObjectiveContext;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.Versioned;
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
        service = { PortLoadBalancerService.class, PortLoadBalancerAdminService.class }
)
public class PortLoadBalancerManager implements PortLoadBalancerService, PortLoadBalancerAdminService {

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private CoreService coreService;

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

    private static final Logger log = getLogger(PortLoadBalancerManager.class);
    private static final String APP_NAME = "org.onosproject.portloadbalancer";

    private ApplicationId appId;
    private ConsistentMap<PortLoadBalancerId, PortLoadBalancer> portLoadBalancerStore;
    private ConsistentMap<PortLoadBalancerId, Integer> portLoadBalancerNextStore;
    // TODO Evaluate if ResourceService is a better option
    private ConsistentMap<PortLoadBalancerId, ApplicationId> portLoadBalancerResStore;
    private Set<PortLoadBalancerListener> listeners = Sets.newConcurrentHashSet();

    private ExecutorService portLoadBalancerEventExecutor;
    private ExecutorService portLoadBalancerProvExecutor;
    private ExecutorService deviceEventExecutor;

    private MapEventListener<PortLoadBalancerId, PortLoadBalancer> portLoadBalancerStoreListener;
    // TODO build CLI to view and clear the next store
    private MapEventListener<PortLoadBalancerId, Integer> portLoadBalancerNextStoreListener;
    private MapEventListener<PortLoadBalancerId, ApplicationId> portLoadBalancerResStoreListener;
    private final DeviceListener deviceListener = new InternalDeviceListener();

    @Activate
    public void activate() {
        appId = coreService.registerApplication(APP_NAME);

        portLoadBalancerEventExecutor = Executors.newSingleThreadExecutor(
                groupedThreads("portloadbalancer-event", "%d", log));
        portLoadBalancerProvExecutor = Executors.newSingleThreadExecutor(
                groupedThreads("portloadbalancer-prov", "%d", log));
        deviceEventExecutor = Executors.newSingleThreadScheduledExecutor(
                groupedThreads("portloadbalancer-dev-event", "%d", log));
        portLoadBalancerStoreListener = new PortLoadBalancerStoreListener();
        portLoadBalancerNextStoreListener = new PortLoadBalancerNextStoreListener();
        portLoadBalancerResStoreListener = new PortLoadBalancerResStoreListener();

        KryoNamespace serializer = KryoNamespace.newBuilder()
                .register(KryoNamespaces.API)
                .register(PortLoadBalancer.class)
                .register(PortLoadBalancerId.class)
                .register(PortLoadBalancerMode.class)
                .build();
        portLoadBalancerStore = storageService.<PortLoadBalancerId, PortLoadBalancer>consistentMapBuilder()
                .withName("onos-portloadbalancer-store")
                .withRelaxedReadConsistency()
                .withSerializer(Serializer.using(serializer))
                .build();
        portLoadBalancerStore.addListener(portLoadBalancerStoreListener);
        portLoadBalancerNextStore = storageService.<PortLoadBalancerId, Integer>consistentMapBuilder()
                .withName("onos-portloadbalancer-next-store")
                .withRelaxedReadConsistency()
                .withSerializer(Serializer.using(serializer))
                .build();
        portLoadBalancerNextStore.addListener(portLoadBalancerNextStoreListener);
        portLoadBalancerResStore = storageService.<PortLoadBalancerId, ApplicationId>consistentMapBuilder()
                .withName("onos-portloadbalancer-res-store")
                .withRelaxedReadConsistency()
                .withSerializer(Serializer.using(serializer))
                .build();
        portLoadBalancerResStore.addListener(portLoadBalancerResStoreListener);

        deviceService.addListener(deviceListener);

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        portLoadBalancerStore.removeListener(portLoadBalancerStoreListener);
        portLoadBalancerNextStore.removeListener(portLoadBalancerNextStoreListener);

        deviceService.removeListener(deviceListener);

        portLoadBalancerEventExecutor.shutdown();
        portLoadBalancerProvExecutor.shutdown();
        deviceEventExecutor.shutdown();

        log.info("Stopped");
    }

    @Override
    public void addListener(PortLoadBalancerListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(PortLoadBalancerListener listener) {
        listeners.remove(listener);
    }

    @Override
    public PortLoadBalancer createOrUpdate(PortLoadBalancerId portLoadBalancerId, Set<PortNumber> ports,
                                           PortLoadBalancerMode mode) {
        log.debug("Putting {} -> {} {} into port load balancer store", portLoadBalancerId, mode, ports);
        return Versioned.valueOrNull(portLoadBalancerStore.put(portLoadBalancerId,
                new PortLoadBalancer(portLoadBalancerId, ports, mode)));
    }

    @Override
    public PortLoadBalancer remove(PortLoadBalancerId portLoadBalancerId) {
        ApplicationId reservation = Versioned.valueOrNull(portLoadBalancerResStore.get(portLoadBalancerId));
        // Remove only if it is not used - otherwise it is necessary to release first
        if (reservation == null) {
            log.debug("Removing {} from port load balancer store", portLoadBalancerId);
            return Versioned.valueOrNull(portLoadBalancerStore.remove(portLoadBalancerId));
        }
        log.warn("Removal {} from port load balancer store was not possible " +
                          "due to a previous reservation", portLoadBalancerId);
        return null;
    }

    @Override
    public Map<PortLoadBalancerId, PortLoadBalancer> getPortLoadBalancers() {
        return ImmutableMap.copyOf(portLoadBalancerStore.asJavaMap());
    }

    @Override
    public PortLoadBalancer getPortLoadBalancer(PortLoadBalancerId portLoadBalancerId) {
        return Versioned.valueOrNull(portLoadBalancerStore.get(portLoadBalancerId));
    }

    @Override
    public Map<PortLoadBalancerId, Integer> getPortLoadBalancerNexts() {
        return ImmutableMap.copyOf(portLoadBalancerNextStore.asJavaMap());
    }

    @Override
    public int getPortLoadBalancerNext(PortLoadBalancerId portLoadBalancerId) {
        return Versioned.valueOrNull(portLoadBalancerNextStore.get(portLoadBalancerId));
    }

    @Override
    public boolean reserve(PortLoadBalancerId portLoadBalancerId, ApplicationId appId) {
        // Check if the resource is available
        ApplicationId reservation = Versioned.valueOrNull(portLoadBalancerResStore.get(portLoadBalancerId));
        PortLoadBalancer portLoadBalancer = Versioned.valueOrNull(portLoadBalancerStore.get(portLoadBalancerId));
        if (reservation == null && portLoadBalancer != null) {
            log.debug("Reserving {} -> {} into port load balancer reservation store", portLoadBalancerId, appId);
            return portLoadBalancerResStore.put(portLoadBalancerId, appId) == null;
        } else if (reservation != null && reservation.equals(appId)) {
            // App try to reserve the resource a second time
            log.debug("Already reserved {} -> {} skip reservation", portLoadBalancerId, appId);
            return true;
        }
        log.warn("Reservation failed {} -> {}", portLoadBalancerId, appId);
        return false;
    }

    @Override
    public boolean release(PortLoadBalancerId portLoadBalancerId, ApplicationId appId) {
        // Check if the resource is reserved
        ApplicationId reservation = Versioned.valueOrNull(portLoadBalancerResStore.get(portLoadBalancerId));
        if (reservation != null && reservation.equals(appId)) {
            log.debug("Removing {} -> {} from port load balancer reservation store", portLoadBalancerId, appId);
            return portLoadBalancerResStore.remove(portLoadBalancerId) != null;
        }
        log.warn("Release failed {} -> {}", portLoadBalancerId, appId);
        return false;
    }

    @Override
    public ApplicationId getReservation(PortLoadBalancerId portLoadBalancerId) {
        return Versioned.valueOrNull(portLoadBalancerResStore.get(portLoadBalancerId));
    }

    @Override
    public Map<PortLoadBalancerId, ApplicationId> getReservations() {
        return portLoadBalancerResStore.asJavaMap();
    }

    private class PortLoadBalancerStoreListener implements MapEventListener<PortLoadBalancerId, PortLoadBalancer> {
        public void event(MapEvent<PortLoadBalancerId, PortLoadBalancer> event) {
            switch (event.type()) {
                case INSERT:
                    log.debug("PortLoadBalancer {} insert new={}, old={}", event.key(), event.newValue(),
                            event.oldValue());
                    post(new PortLoadBalancerEvent(PortLoadBalancerEvent.Type.ADDED, event.newValue().value().data(),
                            null));
                    populatePortLoadBalancer(event.newValue().value());
                    break;
                case REMOVE:
                    log.debug("PortLoadBalancer {} remove new={}, old={}", event.key(), event.newValue(),
                            event.oldValue());
                    post(new PortLoadBalancerEvent(PortLoadBalancerEvent.Type.REMOVED, null,
                            event.oldValue().value().data()));
                    revokePortLoadBalancer(event.oldValue().value());
                    break;
                case UPDATE:
                    log.debug("PortLoadBalancer {} update new={}, old={}", event.key(), event.newValue(),
                            event.oldValue());
                    post(new PortLoadBalancerEvent(PortLoadBalancerEvent.Type.UPDATED, event.newValue().value().data(),
                            event.oldValue().value().data()));
                    updatePortLoadBalancer(event.newValue().value(), event.oldValue().value());
                    break;
                default:
                    break;
            }
        }
    }

    private class PortLoadBalancerNextStoreListener implements MapEventListener<PortLoadBalancerId, Integer> {
        public void event(MapEvent<PortLoadBalancerId, Integer> event) {
            switch (event.type()) {
                case INSERT:
                    log.debug("PortLoadBalancer next {} insert new={}, old={}", event.key(), event.newValue(),
                            event.oldValue());
                    break;
                case REMOVE:
                    log.debug("PortLoadBalancer next {} remove new={}, old={}", event.key(), event.newValue(),
                            event.oldValue());
                    break;
                case UPDATE:
                    log.debug("PortLoadBalancer next {} update new={}, old={}", event.key(), event.newValue(),
                            event.oldValue());
                    break;
                default:
                    break;
            }
        }
    }

    private class PortLoadBalancerResStoreListener implements MapEventListener<PortLoadBalancerId, ApplicationId> {
        public void event(MapEvent<PortLoadBalancerId, ApplicationId> event) {
            switch (event.type()) {
                case INSERT:
                    log.debug("PortLoadBalancer reservation {} insert new={}, old={}", event.key(), event.newValue(),
                              event.oldValue());
                    break;
                case REMOVE:
                    log.debug("PortLoadBalancer reservation {} remove new={}, old={}", event.key(), event.newValue(),
                              event.oldValue());
                    break;
                case UPDATE:
                    log.debug("PortLoadBalancer reservation {} update new={}, old={}", event.key(), event.newValue(),
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

    private void post(PortLoadBalancerEvent portLoadBalancerEvent) {
        portLoadBalancerEventExecutor.execute(() -> {
            for (PortLoadBalancerListener l : listeners) {
                if (l.isRelevant(portLoadBalancerEvent)) {
                    l.event(portLoadBalancerEvent);
                }
            }
        });
    }

    private void init(DeviceId deviceId) {
        portLoadBalancerStore.entrySet().stream()
                .filter(portLoadBalancerEntry -> portLoadBalancerEntry.getKey().deviceId().equals(deviceId))
                .forEach(portLoadBalancerEntry -> populatePortLoadBalancer(portLoadBalancerEntry.getValue().value()));
    }

    private void cleanup(DeviceId deviceId) {
        portLoadBalancerStore.entrySet().stream()
                .filter(entry -> entry.getKey().deviceId().equals(deviceId))
                .forEach(entry -> portLoadBalancerNextStore.remove(entry.getKey()));
        log.debug("{} is removed from portLoadBalancerNextStore", deviceId);
    }

    private void populatePortLoadBalancer(PortLoadBalancer portLoadBalancer) {
        DeviceId deviceId = portLoadBalancer.portLoadBalancerId().deviceId();
        if (!isLocalLeader(deviceId)) {
            log.debug("Not the leader of {}. Skip populatePortLoadBalancer {}", deviceId,
                    portLoadBalancer.portLoadBalancerId());
            return;
        }

        portLoadBalancerProvExecutor.execute(() -> {
            Integer nextid = Versioned.valueOrNull(portLoadBalancerNextStore
                    .get(portLoadBalancer.portLoadBalancerId()));
            if (nextid == null) {
                // Build a new context and new next objective
                PortLoadBalancerObjectiveContext context =
                        new PortLoadBalancerObjectiveContext(portLoadBalancer.portLoadBalancerId());
                NextObjective nextObj = nextObjBuilder(portLoadBalancer.portLoadBalancerId(),
                        portLoadBalancer.ports(), nextid).add(context);
                // Finally submit, store, and register the resource
                flowObjService.next(deviceId, nextObj);
                portLoadBalancerNextStore.put(portLoadBalancer.portLoadBalancerId(), nextObj.id());
            } else {
                log.info("NextObj for {} already exists. Skip populatePortLoadBalancer",
                        portLoadBalancer.portLoadBalancerId());
            }
        });
    }

    private void revokePortLoadBalancer(PortLoadBalancer portLoadBalancer) {
        DeviceId deviceId = portLoadBalancer.portLoadBalancerId().deviceId();
        if (!isLocalLeader(deviceId)) {
            log.debug("Not the leader of {}. Skip revokePortLoadBalancer {}", deviceId,
                    portLoadBalancer.portLoadBalancerId());
            return;
        }

        portLoadBalancerProvExecutor.execute(() -> {
            Integer nextid = Versioned.valueOrNull(portLoadBalancerNextStore.get(portLoadBalancer
                    .portLoadBalancerId()));
            if (nextid != null) {
                // Build a new context and remove old next objective
                PortLoadBalancerObjectiveContext context =
                        new PortLoadBalancerObjectiveContext(portLoadBalancer.portLoadBalancerId());
                NextObjective nextObj = nextObjBuilder(portLoadBalancer.portLoadBalancerId(), portLoadBalancer.ports(),
                        nextid).remove(context);
                // Finally submit and invalidate the store
                flowObjService.next(deviceId, nextObj);
                portLoadBalancerNextStore.remove(portLoadBalancer.portLoadBalancerId());
            } else {
                log.info("NextObj for {} does not exist. Skip revokePortLoadBalancer",
                        portLoadBalancer.portLoadBalancerId());
            }
        });
    }

    private void updatePortLoadBalancer(PortLoadBalancer newPortLoadBalancer, PortLoadBalancer oldPortLoadBalancer) {
        DeviceId deviceId = newPortLoadBalancer.portLoadBalancerId().deviceId();
        if (!isLocalLeader(deviceId)) {
            log.debug("Not the leader of {}. Skip updatePortLoadBalancer {}", deviceId,
                    newPortLoadBalancer.portLoadBalancerId());
            return;
        }

        portLoadBalancerProvExecutor.execute(() -> {
            Integer nextid = Versioned.valueOrNull(portLoadBalancerNextStore
                    .get(newPortLoadBalancer.portLoadBalancerId()));
            if (nextid != null) {
                // Compute modifications and context
                PortLoadBalancerObjectiveContext context =
                        new PortLoadBalancerObjectiveContext(newPortLoadBalancer.portLoadBalancerId());
                Set<PortNumber> portsToBeAdded =
                        Sets.difference(newPortLoadBalancer.ports(), oldPortLoadBalancer.ports());
                Set<PortNumber> portsToBeRemoved =
                        Sets.difference(oldPortLoadBalancer.ports(), newPortLoadBalancer.ports());
                // and send them to the flowobj subsystem
                if (!portsToBeAdded.isEmpty()) {
                    flowObjService.next(deviceId, nextObjBuilder(newPortLoadBalancer.portLoadBalancerId(),
                            portsToBeAdded, nextid)
                            .addToExisting(context));
                } else {
                    log.debug("NextObj for {} nothing to add", newPortLoadBalancer.portLoadBalancerId());

                }
                if (!portsToBeRemoved.isEmpty()) {
                    flowObjService.next(deviceId, nextObjBuilder(newPortLoadBalancer.portLoadBalancerId(),
                            portsToBeRemoved, nextid)
                            .removeFromExisting(context));
                } else {
                    log.debug("NextObj for {} nothing to remove", newPortLoadBalancer.portLoadBalancerId());
                }
            } else {
                log.info("NextObj for {} does not exist. Skip updatePortLoadBalancer",
                        newPortLoadBalancer.portLoadBalancerId());
            }
        });
    }

    private NextObjective.Builder nextObjBuilder(PortLoadBalancerId portLoadBalancerId, Set<PortNumber> ports,
                                                 Integer nextId) {
        if (nextId == null) {
            nextId = flowObjService.allocateNextId();
        }
        // This metadata is used to pass the key to the driver.
        // Some driver, e.g. OF-DPA, will use that information while creating load balancing group.
        // TODO This is not an actual LAG port. In the future, we should extend metadata structure to carry
        //      generic information. We should avoid using in_port in the metadata once generic metadata is available.
        TrafficSelector meta = DefaultTrafficSelector.builder()
                .matchInPort(PortNumber.portNumber(portLoadBalancerId.key())).build();
        NextObjective.Builder nextObjBuilder = DefaultNextObjective.builder()
                .withId(nextId)
                .withMeta(meta)
                .withType(NextObjective.Type.HASHED)
                .fromApp(appId);
        ports.forEach(port -> {
            TrafficTreatment treatment = DefaultTrafficTreatment.builder().setOutput(port).build();
            nextObjBuilder.addTreatment(DefaultNextTreatment.of(treatment));
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

    private final class PortLoadBalancerObjectiveContext implements ObjectiveContext {
        private final PortLoadBalancerId portLoadBalancerId;

        private PortLoadBalancerObjectiveContext(PortLoadBalancerId portLoadBalancerId) {
            this.portLoadBalancerId = portLoadBalancerId;
        }

        @Override
        public void onSuccess(Objective objective) {
            NextObjective nextObj = (NextObjective) objective;
            log.debug("Successfully {} nextobj {} for port load balancer {}. NextObj={}",
                    nextObj.op(), nextObj.id(), portLoadBalancerId, nextObj);
            portLoadBalancerProvExecutor.execute(() -> onSuccessHandler(nextObj, portLoadBalancerId));
        }

        @Override
        public void onError(Objective objective, ObjectiveError error) {
            NextObjective nextObj = (NextObjective) objective;
            log.warn("Failed to {} nextobj {} for port load balancer {} due to {}. NextObj={}",
                    nextObj.op(), nextObj.id(), portLoadBalancerId, error, nextObj);
            portLoadBalancerProvExecutor.execute(() -> onErrorHandler(nextObj, portLoadBalancerId));
        }
    }

    private void onSuccessHandler(NextObjective nextObjective, PortLoadBalancerId portLoadBalancerId) {
        // Operation done
        PortLoadBalancerData oldPortLoadBalancerData = new PortLoadBalancerData(portLoadBalancerId);
        PortLoadBalancerData newPortLoadBalancerData = new PortLoadBalancerData(portLoadBalancerId);
        // Other operations will not lead to a generation of an event
        switch (nextObjective.op()) {
            case ADD:
                newPortLoadBalancerData.setNextId(nextObjective.id());
                post(new PortLoadBalancerEvent(PortLoadBalancerEvent.Type.INSTALLED, newPortLoadBalancerData,
                        oldPortLoadBalancerData));
                break;
            case REMOVE:
                oldPortLoadBalancerData.setNextId(nextObjective.id());
                post(new PortLoadBalancerEvent(PortLoadBalancerEvent.Type.UNINSTALLED, newPortLoadBalancerData,
                        oldPortLoadBalancerData));
                break;
            default:
                break;
        }
    }

    private void onErrorHandler(NextObjective nextObjective, PortLoadBalancerId portLoadBalancerId) {
        // There was a failure
        PortLoadBalancerData portLoadBalancerData = new PortLoadBalancerData(portLoadBalancerId);
        // send FAILED event;
        switch (nextObjective.op()) {
            case ADD:
                // If ADD is failing apps do not know the next id; let's update the store
                portLoadBalancerNextStore.remove(portLoadBalancerId);
                portLoadBalancerResStore.remove(portLoadBalancerId);
                portLoadBalancerStore.remove(portLoadBalancerId);
                post(new PortLoadBalancerEvent(PortLoadBalancerEvent.Type.FAILED, portLoadBalancerData,
                        portLoadBalancerData));
                break;
            case ADD_TO_EXISTING:
                // If ADD_TO_EXISTING is failing let's remove the failed ports
                Collection<PortNumber> addedPorts = nextObjective.next().stream()
                        .flatMap(t -> t.allInstructions().stream())
                        .filter(i -> i.type() == Instruction.Type.OUTPUT)
                        .map(i -> ((Instructions.OutputInstruction) i).port())
                        .collect(Collectors.toList());
                portLoadBalancerStore.compute(portLoadBalancerId, (key, value) -> {
                    if (value != null && value.ports() != null && !value.ports().isEmpty()) {
                        value.ports().removeAll(addedPorts);
                    }
                    return value;
                });
                portLoadBalancerData.setNextId(nextObjective.id());
                post(new PortLoadBalancerEvent(PortLoadBalancerEvent.Type.FAILED, portLoadBalancerData,
                        portLoadBalancerData));
                break;
            case REMOVE_FROM_EXISTING:
                // If REMOVE_TO_EXISTING is failing let's re-add the failed ports
                Collection<PortNumber> removedPorts = nextObjective.next().stream()
                        .flatMap(t -> t.allInstructions().stream())
                        .filter(i -> i.type() == Instruction.Type.OUTPUT)
                        .map(i -> ((Instructions.OutputInstruction) i).port())
                        .collect(Collectors.toList());
                portLoadBalancerStore.compute(portLoadBalancerId, (key, value) -> {
                    if (value != null && value.ports() != null) {
                        value.ports().addAll(removedPorts);
                    }
                    return value;
                });
                portLoadBalancerData.setNextId(nextObjective.id());
                post(new PortLoadBalancerEvent(PortLoadBalancerEvent.Type.FAILED, portLoadBalancerData,
                        portLoadBalancerData));
                break;
            case VERIFY:
            case REMOVE:
                // If ADD/REMOVE_TO_EXISTING, REMOVE and VERIFY are failing let's send
                // also the info about the next id
                portLoadBalancerData.setNextId(nextObjective.id());
                post(new PortLoadBalancerEvent(PortLoadBalancerEvent.Type.FAILED, portLoadBalancerData,
                        portLoadBalancerData));
                break;
            default:
                break;
        }

    }
}