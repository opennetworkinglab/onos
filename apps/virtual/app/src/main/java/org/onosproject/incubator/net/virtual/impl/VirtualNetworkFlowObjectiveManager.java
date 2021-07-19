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

package org.onosproject.incubator.net.virtual.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalNotification;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.tuple.Pair;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.incubator.net.virtual.AbstractVnetService;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.VirtualNetworkFlowObjectiveStore;
import org.onosproject.incubator.net.virtual.VirtualNetworkService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.NextGroup;
import org.onosproject.net.behaviour.Pipeliner;
import org.onosproject.net.behaviour.PipelinerContext;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleOperations;
import org.onosproject.net.flow.FlowRuleOperationsContext;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.FlowObjectiveStore;
import org.onosproject.net.flowobjective.FlowObjectiveStoreDelegate;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.NextObjective;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.onosproject.net.flowobjective.ObjectiveEvent;
import org.onosproject.net.group.DefaultGroupKey;
import org.onosproject.net.group.GroupKey;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.BoundedThreadPool.newFixedThreadPool;
import static org.onlab.util.Tools.groupedThreads;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provides implementation of the flow objective programming service for virtual networks.
 */
// NOTE: This manager is designed to provide flow objective programming service
// for virtual networks. Actually, virtual networks don't need to consider
// the different implementation of data-path pipeline. But, the interfaces
// and usages of flow objective service are still valuable for virtual network.
// This manager is working as an interpreter from FlowObjective to FlowRules
// to provide symmetric interfaces with ONOS core services.
// The behaviours are based on DefaultSingleTablePipeline.

public class VirtualNetworkFlowObjectiveManager extends AbstractVnetService
        implements FlowObjectiveService {

    public static final int INSTALL_RETRY_ATTEMPTS = 5;
    public static final long INSTALL_RETRY_INTERVAL = 1000; // ms

    private final Logger log = getLogger(getClass());

    protected DeviceService deviceService;

    // Note: The following dependencies are added on behalf of the pipeline
    // driver behaviours to assure these services are available for their
    // initialization.
    protected FlowRuleService flowRuleService;

    protected VirtualNetworkFlowObjectiveStore virtualFlowObjectiveStore;
    protected FlowObjectiveStore flowObjectiveStore;
    private final FlowObjectiveStoreDelegate delegate;

    private final PipelinerContext context = new InnerPipelineContext();

    private final Map<DeviceId, Pipeliner> pipeliners = Maps.newConcurrentMap();

    // local stores for queuing fwd and next objectives that are waiting for an
    // associated next objective execution to complete. The signal for completed
    // execution comes from a pipeline driver, in this or another controller
    // instance, via the DistributedFlowObjectiveStore.
    private final Map<Integer, Set<PendingFlowObjective>> pendingForwards =
            Maps.newConcurrentMap();
    private final Map<Integer, Set<PendingFlowObjective>> pendingNexts =
            Maps.newConcurrentMap();

    // local store to track which nextObjectives were sent to which device
    // for debugging purposes
    private Map<Integer, DeviceId> nextToDevice = Maps.newConcurrentMap();

    private ExecutorService executorService;

    public VirtualNetworkFlowObjectiveManager(VirtualNetworkService manager,
                                              NetworkId networkId) {
        super(manager, networkId);

        deviceService = manager.get(networkId(), DeviceService.class);
        flowRuleService = manager.get(networkId(), FlowRuleService.class);

        executorService = newFixedThreadPool(4, groupedThreads("onos/virtual/objective-installer", "%d", log));

        virtualFlowObjectiveStore =
                serviceDirectory.get(VirtualNetworkFlowObjectiveStore.class);
        delegate = new InternalStoreDelegate();
        virtualFlowObjectiveStore.setDelegate(networkId(), delegate);
        flowObjectiveStore = new StoreConvertor();
    }

    @Override
    public void filter(DeviceId deviceId, FilteringObjective filteringObjective) {
        executorService.execute(new ObjectiveInstaller(deviceId, filteringObjective));
    }

    @Override
    public void forward(DeviceId deviceId, ForwardingObjective forwardingObjective) {
        if (forwardingObjective.nextId() == null ||
                forwardingObjective.op() == Objective.Operation.REMOVE ||
                flowObjectiveStore.getNextGroup(forwardingObjective.nextId()) != null ||
                !queueFwdObjective(deviceId, forwardingObjective)) {
            // fast path
            executorService.execute(new ObjectiveInstaller(deviceId, forwardingObjective));
        }
    }

    @Override
    public void next(DeviceId deviceId, NextObjective nextObjective) {
        nextToDevice.put(nextObjective.id(), deviceId);
        if (nextObjective.op() == Objective.Operation.ADD ||
                flowObjectiveStore.getNextGroup(nextObjective.id()) != null ||
                !queueNextObjective(deviceId, nextObjective)) {
            // either group exists or we are trying to create it - let it through
            executorService.execute(new ObjectiveInstaller(deviceId, nextObjective));
        }
    }

    @Override
    public int allocateNextId() {
        return flowObjectiveStore.allocateNextId();
    }

    @Override
    public void initPolicy(String policy) {

    }

    @Override
    public Map<Pair<Integer, DeviceId>, List<String>> getNextMappingsChain() {
        return ImmutableMap.of();
    }

    @Override
    public List<String> getNextMappings() {
        List<String> mappings = new ArrayList<>();
        Map<Integer, NextGroup> allnexts = flowObjectiveStore.getAllGroups();
        // XXX if the NextGroup after de-serialization actually stored info of the deviceId
        // then info on any nextObj could be retrieved from one controller instance.
        // Right now the drivers on one instance can only fetch for next-ids that came
        // to them.
        // Also, we still need to send the right next-id to the right driver as potentially
        // there can be different drivers for different devices. But on that account,
        // no instance should be decoding for another instance's nextIds.

        for (Map.Entry<Integer, NextGroup> e : allnexts.entrySet()) {
            // get the device this next Objective was sent to
            DeviceId deviceId = nextToDevice.get(e.getKey());
            mappings.add("NextId " + e.getKey() + ": " +
                                 ((deviceId != null) ? deviceId : "nextId not in this onos instance"));
            if (deviceId != null) {
                // this instance of the controller sent the nextObj to a driver
                Pipeliner pipeliner = getDevicePipeliner(deviceId);
                List<String> nextMappings = pipeliner.getNextMappings(e.getValue());
                if (nextMappings != null) {
                    mappings.addAll(nextMappings);
                }
            }
        }
        return mappings;
    }

    @Override
    public List<String> getPendingFlowObjectives() {
        List<String> pendingFlowObjectives = new ArrayList<>();

        for (Integer nextId : pendingForwards.keySet()) {
            Set<PendingFlowObjective> pfwd = pendingForwards.get(nextId);
            StringBuilder pend = new StringBuilder();
            pend.append("NextId: ")
                    .append(nextId);
            for (PendingFlowObjective pf : pfwd) {
                pend.append("\n    FwdId: ")
                        .append(String.format("%11s", pf.flowObjective().id()))
                        .append(", DeviceId: ")
                        .append(pf.deviceId())
                        .append(", Selector: ")
                        .append(((ForwardingObjective) pf.flowObjective())
                                        .selector().criteria());
            }
            pendingFlowObjectives.add(pend.toString());
        }

        for (Integer nextId : pendingNexts.keySet()) {
            Set<PendingFlowObjective> pnext = pendingNexts.get(nextId);
            StringBuilder pend = new StringBuilder();
            pend.append("NextId: ")
                    .append(nextId);
            for (PendingFlowObjective pn : pnext) {
                pend.append("\n    NextOp: ")
                        .append(pn.flowObjective().op())
                        .append(", DeviceId: ")
                        .append(pn.deviceId())
                        .append(", Treatments: ")
                        .append(((NextObjective) pn.flowObjective())
                                        .next());
            }
            pendingFlowObjectives.add(pend.toString());
        }

        return pendingFlowObjectives;
    }

    @Override
    public void purgeAll(DeviceId deviceId, ApplicationId appId) {
        // TODO: purge queued flow objectives?
        pipeliners.get(deviceId).purgeAll(appId);
    }

    private boolean queueFwdObjective(DeviceId deviceId, ForwardingObjective fwd) {
        boolean queued = false;
        synchronized (pendingForwards) {
            // double check the flow objective store, because this block could run
            // after a notification arrives
            if (flowObjectiveStore.getNextGroup(fwd.nextId()) == null) {
                pendingForwards.compute(fwd.nextId(), (id, pending) -> {
                    PendingFlowObjective pendfo = new PendingFlowObjective(deviceId, fwd);
                    if (pending == null) {
                        return Sets.newHashSet(pendfo);
                    } else {
                        pending.add(pendfo);
                        return pending;
                    }
                });
                queued = true;
            }
        }
        if (queued) {
            log.debug("Queued forwarding objective {} for nextId {} meant for device {}",
                      fwd.id(), fwd.nextId(), deviceId);
        }
        return queued;
    }

    private boolean queueNextObjective(DeviceId deviceId, NextObjective next) {

        // we need to hold off on other operations till we get notified that the
        // initial group creation has succeeded
        boolean queued = false;
        synchronized (pendingNexts) {
            // double check the flow objective store, because this block could run
            // after a notification arrives
            if (flowObjectiveStore.getNextGroup(next.id()) == null) {
                pendingNexts.compute(next.id(), (id, pending) -> {
                    PendingFlowObjective pendfo = new PendingFlowObjective(deviceId, next);
                    if (pending == null) {
                        return Sets.newHashSet(pendfo);
                    } else {
                        pending.add(pendfo);
                        return pending;
                    }
                });
                queued = true;
            }
        }
        if (queued) {
            log.debug("Queued next objective {} with operation {} meant for device {}",
                      next.id(), next.op(), deviceId);
        }
        return queued;
    }

    /**
     * Task that passes the flow objective down to the driver. The task will
     * make a few attempts to find the appropriate driver, then eventually give
     * up and report an error if no suitable driver could be found.
     */
    private class ObjectiveInstaller implements Runnable {
        private final DeviceId deviceId;
        private final Objective objective;

        private final int numAttempts;

        public ObjectiveInstaller(DeviceId deviceId, Objective objective) {
            this(deviceId, objective, 1);
        }

        public ObjectiveInstaller(DeviceId deviceId, Objective objective, int attemps) {
            this.deviceId = checkNotNull(deviceId);
            this.objective = checkNotNull(objective);
            this.numAttempts = attemps;
        }

        @Override
        public void run() {
            try {
                Pipeliner pipeliner = getDevicePipeliner(deviceId);

                if (pipeliner != null) {
                    if (objective instanceof NextObjective) {
                        nextToDevice.put(objective.id(), deviceId);
                        pipeliner.next((NextObjective) objective);
                    } else if (objective instanceof ForwardingObjective) {
                        pipeliner.forward((ForwardingObjective) objective);
                    } else {
                        pipeliner.filter((FilteringObjective) objective);
                    }
                    //Attempts to check if pipeliner is null for retry attempts
                } else if (numAttempts < INSTALL_RETRY_ATTEMPTS) {
                    Thread.sleep(INSTALL_RETRY_INTERVAL);
                    executorService.execute(new ObjectiveInstaller(deviceId, objective, numAttempts + 1));
                } else {
                    // Otherwise we've tried a few times and failed, report an
                    // error back to the user.
                    objective.context().ifPresent(
                            c -> c.onError(objective, ObjectiveError.NOPIPELINER));
                }
                //Exception thrown
            } catch (Exception e) {
                log.warn("Exception while installing flow objective", e);
            }
        }
    }

    private class InternalStoreDelegate implements FlowObjectiveStoreDelegate {
        @Override
        public void notify(ObjectiveEvent event) {
            if (event.type() == ObjectiveEvent.Type.ADD) {
                log.debug("Received notification of obj event {}", event);
                Set<PendingFlowObjective> pending;

                // first send all pending flows
                synchronized (pendingForwards) {
                    // needs to be synchronized for queueObjective lookup
                    pending = pendingForwards.remove(event.subject());
                }
                if (pending == null) {
                    log.debug("No forwarding objectives pending for this "
                                      + "obj event {}", event);
                } else {
                    log.debug("Processing {} pending forwarding objectives for nextId {}",
                              pending.size(), event.subject());
                    pending.forEach(p -> getDevicePipeliner(p.deviceId())
                            .forward((ForwardingObjective) p.flowObjective()));
                }

                // now check for pending next-objectives
                synchronized (pendingNexts) {
                    // needs to be synchronized for queueObjective lookup
                    pending = pendingNexts.remove(event.subject());
                }
                if (pending == null) {
                    log.debug("No next objectives pending for this "
                                      + "obj event {}", event);
                } else {
                    log.debug("Processing {} pending next objectives for nextId {}",
                              pending.size(), event.subject());
                    pending.forEach(p -> getDevicePipeliner(p.deviceId())
                            .next((NextObjective) p.flowObjective()));
                }
            }
        }
    }

    /**
     * Retrieves (if it exists) the device pipeline behaviour from the cache.
     * Otherwise it warms the caches and triggers the init method of the Pipeline.
     * For virtual network, it returns OVS pipeliner.
     *
     * @param deviceId the id of the device associated to the pipeline
     * @return the implementation of the Pipeliner behaviour
     */
    private Pipeliner getDevicePipeliner(DeviceId deviceId) {
        return pipeliners.computeIfAbsent(deviceId, this::initPipelineHandler);
    }

    /**
     * Creates and initialize {@link Pipeliner}.
     * <p>
     * Note: Expected to be called under per-Device lock.
     *      e.g., {@code pipeliners}' Map#compute family methods
     *
     * @param deviceId Device to initialize pipeliner
     * @return {@link Pipeliner} instance or null
     */
    private Pipeliner initPipelineHandler(DeviceId deviceId) {
        //FIXME: do we need a standard pipeline for virtual device?
        Pipeliner pipeliner = new DefaultVirtualDevicePipeline();
        pipeliner.init(deviceId, context);
        return pipeliner;
    }

    // Processing context for initializing pipeline driver behaviours.
    private class InnerPipelineContext implements PipelinerContext {
        @Override
        public ServiceDirectory directory() {
            return serviceDirectory;
        }

        @Override
        public FlowObjectiveStore store() {
            return flowObjectiveStore;
        }
    }

    /**
     * Data class used to hold a pending flow objective that could not
     * be processed because the associated next object was not present.
     * Note that this pending flow objective could be a forwarding objective
     * waiting for a next objective to complete execution. Or it could a
     * next objective (with a different operation - remove, addToExisting, or
     * removeFromExisting) waiting for a next objective with the same id to
     * complete execution.
     */
    private class PendingFlowObjective {
        private final DeviceId deviceId;
        private final Objective flowObj;

        public PendingFlowObjective(DeviceId deviceId, Objective flowObj) {
            this.deviceId = deviceId;
            this.flowObj = flowObj;
        }

        public DeviceId deviceId() {
            return deviceId;
        }

        public Objective flowObjective() {
            return flowObj;
        }

        @Override
        public int hashCode() {
            return Objects.hash(deviceId, flowObj);
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof PendingFlowObjective)) {
                return false;
            }
            final PendingFlowObjective other = (PendingFlowObjective) obj;
            if (this.deviceId.equals(other.deviceId) &&
                    this.flowObj.equals(other.flowObj)) {
                return true;
            }
            return false;
        }
    }

    /**
     * This class is a wrapping class from VirtualNetworkFlowObjectiveStore
     * to FlowObjectiveStore for PipelinerContext.
     */
    private class StoreConvertor implements FlowObjectiveStore {

        @Override
        public void setDelegate(FlowObjectiveStoreDelegate delegate) {
            virtualFlowObjectiveStore.setDelegate(networkId(), delegate);
        }

        @Override
        public void unsetDelegate(FlowObjectiveStoreDelegate delegate) {
            virtualFlowObjectiveStore.unsetDelegate(networkId(), delegate);
        }

        @Override
        public boolean hasDelegate() {
            return virtualFlowObjectiveStore.hasDelegate(networkId());
        }

        @Override
        public void putNextGroup(Integer nextId, NextGroup group) {
            virtualFlowObjectiveStore.putNextGroup(networkId(), nextId, group);
        }

        @Override
        public NextGroup getNextGroup(Integer nextId) {
            return virtualFlowObjectiveStore.getNextGroup(networkId(), nextId);
        }

        @Override
        public NextGroup removeNextGroup(Integer nextId) {
            return virtualFlowObjectiveStore.removeNextGroup(networkId(), nextId);
        }

        @Override
        public Map<Integer, NextGroup> getAllGroups() {
            return virtualFlowObjectiveStore.getAllGroups(networkId());
        }

        @Override
        public int allocateNextId() {
            return virtualFlowObjectiveStore.allocateNextId(networkId());
        }
    }

    /**
     * Simple single table pipeline abstraction for virtual networks.
     */
    private class DefaultVirtualDevicePipeline
            extends AbstractHandlerBehaviour implements Pipeliner {

        private final Logger log = getLogger(getClass());

        private DeviceId deviceId;

        private Cache<Integer, NextObjective> pendingNext;

        private KryoNamespace appKryo = new KryoNamespace.Builder()
                .register(GroupKey.class)
                .register(DefaultGroupKey.class)
                .register(SingleGroup.class)
                .register(byte[].class)
                .build("DefaultVirtualDevicePipeline");

        @Override
        public void init(DeviceId deviceId, PipelinerContext context) {
            this.deviceId = deviceId;

            pendingNext = CacheBuilder.newBuilder()
                    .expireAfterWrite(20, TimeUnit.SECONDS)
                    .removalListener((RemovalNotification<Integer, NextObjective> notification) -> {
                        if (notification.getCause() == RemovalCause.EXPIRED) {
                            notification.getValue().context()
                                    .ifPresent(c -> c.onError(notification.getValue(),
                                                              ObjectiveError.FLOWINSTALLATIONFAILED));
                        }
                    }).build();
        }

        @Override
        public void filter(FilteringObjective filter) {

            TrafficTreatment.Builder actions;
            switch (filter.type()) {
                case PERMIT:
                    actions = (filter.meta() == null) ?
                            DefaultTrafficTreatment.builder().punt() :
                            DefaultTrafficTreatment.builder(filter.meta());
                    break;
                case DENY:
                    actions = (filter.meta() == null) ?
                            DefaultTrafficTreatment.builder() :
                            DefaultTrafficTreatment.builder(filter.meta());
                    actions.drop();
                    break;
                default:
                    log.warn("Unknown filter type: {}", filter.type());
                    actions = DefaultTrafficTreatment.builder().drop();
            }

            TrafficSelector.Builder selector = DefaultTrafficSelector.builder();

            filter.conditions().forEach(selector::add);

            if (filter.key() != null) {
                selector.add(filter.key());
            }

            FlowRule.Builder ruleBuilder = DefaultFlowRule.builder()
                    .forDevice(deviceId)
                    .withSelector(selector.build())
                    .withTreatment(actions.build())
                    .fromApp(filter.appId())
                    .withPriority(filter.priority());

            if (filter.permanent()) {
                ruleBuilder.makePermanent();
            } else {
                ruleBuilder.makeTemporary(filter.timeout());
            }

            installObjective(ruleBuilder, filter);
        }

        @Override
        public void forward(ForwardingObjective fwd) {
            TrafficSelector selector = fwd.selector();

            if (fwd.treatment() != null) {
                // Deal with SPECIFIC and VERSATILE in the same manner.
                FlowRule.Builder ruleBuilder = DefaultFlowRule.builder()
                        .forDevice(deviceId)
                        .withSelector(selector)
                        .fromApp(fwd.appId())
                        .withPriority(fwd.priority())
                        .withTreatment(fwd.treatment());

                if (fwd.permanent()) {
                    ruleBuilder.makePermanent();
                } else {
                    ruleBuilder.makeTemporary(fwd.timeout());
                }
                installObjective(ruleBuilder, fwd);

            } else {
                NextObjective nextObjective = pendingNext.getIfPresent(fwd.nextId());
                if (nextObjective != null) {
                    pendingNext.invalidate(fwd.nextId());
                    nextObjective.next().forEach(treat -> {
                        FlowRule.Builder ruleBuilder = DefaultFlowRule.builder()
                                .forDevice(deviceId)
                                .withSelector(selector)
                                .fromApp(fwd.appId())
                                .withPriority(fwd.priority())
                                .withTreatment(treat);

                        if (fwd.permanent()) {
                            ruleBuilder.makePermanent();
                        } else {
                            ruleBuilder.makeTemporary(fwd.timeout());
                        }
                        installObjective(ruleBuilder, fwd);
                    });
                } else {
                    fwd.context().ifPresent(c -> c.onError(fwd,
                                                           ObjectiveError.GROUPMISSING));
                }
            }
        }

        private void installObjective(FlowRule.Builder ruleBuilder, Objective objective) {
            FlowRuleOperations.Builder flowBuilder = FlowRuleOperations.builder();
            switch (objective.op()) {

                case ADD:
                    flowBuilder.add(ruleBuilder.build());
                    break;
                case REMOVE:
                    flowBuilder.remove(ruleBuilder.build());
                    break;
                default:
                    log.warn("Unknown operation {}", objective.op());
            }

            flowRuleService.apply(flowBuilder.build(new FlowRuleOperationsContext() {
                @Override
                public void onSuccess(FlowRuleOperations ops) {
                    objective.context().ifPresent(context -> context.onSuccess(objective));
                }

                @Override
                public void onError(FlowRuleOperations ops) {
                    objective.context()
                            .ifPresent(context ->
                                               context.onError(objective,
                                                                  ObjectiveError.FLOWINSTALLATIONFAILED));
                }
            }));
        }

        @Override
        public void next(NextObjective nextObjective) {

            pendingNext.put(nextObjective.id(), nextObjective);
            flowObjectiveStore.putNextGroup(nextObjective.id(),
                                            new SingleGroup(
                                                    new DefaultGroupKey(
                                                            appKryo.serialize(nextObjective.id()))));
            nextObjective.context().ifPresent(context -> context.onSuccess(nextObjective));
        }

        @Override
        public void purgeAll(ApplicationId appId) {
            flowRuleService.purgeFlowRules(deviceId, appId);
        }

        @Override
        public List<String> getNextMappings(NextGroup nextGroup) {
            // Default single table pipeline does not use nextObjectives or groups
            return null;
        }

        private class SingleGroup implements NextGroup {

            private final GroupKey key;

            public SingleGroup(GroupKey key) {
                this.key = key;
            }

            public GroupKey key() {
                return key;
            }

            @Override
            public byte[] data() {
                return appKryo.serialize(key);
            }
        }
    }

}
