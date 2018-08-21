/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.pipelines.fabric.pipeliner;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalListener;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.onlab.util.KryoNamespace;
import org.onlab.util.Tools;
import org.onosproject.core.GroupId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.NextGroup;
import org.onosproject.net.behaviour.Pipeliner;
import org.onosproject.net.behaviour.PipelinerContext;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.driver.Driver;
import org.onosproject.net.flow.FlowId;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleOperations;
import org.onosproject.net.flow.FlowRuleOperationsContext;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.net.flowobjective.FlowObjectiveStore;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.NextObjective;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.onosproject.net.group.GroupDescription;
import org.onosproject.net.group.GroupEvent;
import org.onosproject.net.group.GroupListener;
import org.onosproject.net.group.GroupService;
import org.onosproject.store.serializers.KryoNamespaces;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Pipeliner for fabric pipeline.
 */
public class FabricPipeliner  extends AbstractHandlerBehaviour implements Pipeliner {
    private static final Logger log = getLogger(FabricPipeliner.class);

    protected static final KryoNamespace KRYO = new KryoNamespace.Builder()
            .register(KryoNamespaces.API)
            .register(FabricNextGroup.class)
            .build("FabricPipeliner");

    private static final Set<GroupEvent.Type> GROUP_FAILED_TYPES =
            Sets.newHashSet(GroupEvent.Type.GROUP_ADD_FAILED,
                            GroupEvent.Type.GROUP_REMOVE_FAILED,
                            GroupEvent.Type.GROUP_UPDATE_FAILED);

    // TODO: make this configurable
    private static final long DEFAULT_INSTALLATION_TIME_OUT = 40;
    private static final int NUM_CALLBACK_THREAD = 2;

    protected DeviceId deviceId;
    protected FlowRuleService flowRuleService;
    protected GroupService groupService;
    protected GroupListener groupListener = new InternalGroupListener();
    protected FlowObjectiveStore flowObjectiveStore;
    protected FabricFilteringPipeliner pipelinerFilter;
    protected FabricForwardingPipeliner pipelinerForward;
    protected FabricNextPipeliner pipelinerNext;

    private Map<PendingFlowKey, PendingInstallObjective> pendingInstallObjectiveFlows = new ConcurrentHashMap<>();
    private Map<PendingGroupKey, PendingInstallObjective> pendingInstallObjectiveGroups = new ConcurrentHashMap<>();
    private Cache<Objective, PendingInstallObjective> pendingInstallObjectives = CacheBuilder.newBuilder()
            .expireAfterWrite(DEFAULT_INSTALLATION_TIME_OUT, TimeUnit.SECONDS)
            .removalListener((RemovalListener<Objective, PendingInstallObjective>) removalNotification -> {
                RemovalCause cause = removalNotification.getCause();
                PendingInstallObjective pio = removalNotification.getValue();
                if (cause == RemovalCause.EXPIRED && pio != null) {
                    pio.failed(pio.objective, ObjectiveError.INSTALLATIONTIMEOUT);
                }
            })
            .build();
    private static ExecutorService flowObjCallbackExecutor =
            Executors.newFixedThreadPool(NUM_CALLBACK_THREAD, Tools.groupedThreads("fabric-pipeliner", "cb-", log));


    @Override
    public void init(DeviceId deviceId, PipelinerContext context) {
        Driver driver = handler().driver();
        this.deviceId = deviceId;
        this.flowRuleService = context.directory().get(FlowRuleService.class);
        this.groupService = context.directory().get(GroupService.class);
        this.groupService.addListener(groupListener);
        this.flowObjectiveStore = context.directory().get(FlowObjectiveStore.class);
        this.pipelinerFilter = new FabricFilteringPipeliner(deviceId);
        this.pipelinerForward = new FabricForwardingPipeliner(deviceId);
        this.pipelinerNext = new FabricNextPipeliner(deviceId, driver);
    }

    @Override
    public void filter(FilteringObjective filterObjective) {
        PipelinerTranslationResult result = pipelinerFilter.filter(filterObjective);
        if (result.error().isPresent()) {
            fail(filterObjective, result.error().get());
            return;
        }

        applyTranslationResult(filterObjective, result, error -> {
            if (error == null) {
                success(filterObjective);
            } else {
                fail(filterObjective, ObjectiveError.FLOWINSTALLATIONFAILED);
            }
        });
    }

    @Override
    public void forward(ForwardingObjective forwardObjective) {
        PipelinerTranslationResult result = pipelinerForward.forward(forwardObjective);
        if (result.error().isPresent()) {
            fail(forwardObjective, result.error().get());
            return;
        }

        applyTranslationResult(forwardObjective, result, error -> {
            if (error == null) {
                success(forwardObjective);
            } else {
                fail(forwardObjective, ObjectiveError.FLOWINSTALLATIONFAILED);
            }
        });
    }

    @Override
    public void next(NextObjective nextObjective) {
        PipelinerTranslationResult result = pipelinerNext.next(nextObjective);

        if (result.error().isPresent()) {
            fail(nextObjective, result.error().get());
            return;
        }

        if (nextObjective.op() == Objective.Operation.VERIFY) {
            // TODO: support VERIFY operation
            log.debug("Currently we don't support VERIFY operation, return success directly to the context");
            success(nextObjective);
            return;
        }

        applyTranslationResult(nextObjective, result, error -> {
            if (error != null) {
                fail(nextObjective, error);
                return;
            }

            // Success, put next group to objective store
            List<PortNumber> portNumbers = Lists.newArrayList();
            nextObjective.next().forEach(treatment -> {
                treatment.allInstructions()
                        .stream()
                        .filter(inst -> inst.type() == Instruction.Type.OUTPUT)
                        .map(inst -> (Instructions.OutputInstruction) inst)
                        .findFirst()
                        .map(Instructions.OutputInstruction::port)
                        .ifPresent(portNumbers::add);
            });
            FabricNextGroup nextGroup = new FabricNextGroup(nextObjective.type(),
                                                            portNumbers);
            flowObjectiveStore.putNextGroup(nextObjective.id(), nextGroup);
            success(nextObjective);
        });
    }

    @Override
    public List<String> getNextMappings(NextGroup nextGroup) {
        FabricNextGroup fabricNextGroup = KRYO.deserialize(nextGroup.data());
        NextObjective.Type type = fabricNextGroup.type();
        Collection<PortNumber> outputPorts = fabricNextGroup.outputPorts();

        return outputPorts.stream()
                .map(port -> String.format("%s -> %s", type, port))
                .collect(Collectors.toList());
    }

    private void applyTranslationResult(Objective objective,
                                        PipelinerTranslationResult result,
                                        Consumer<ObjectiveError> callback) {
        Collection<GroupDescription> groups = result.groups();
        Collection<FlowRule> flowRules = result.flowRules();

        Set<FlowId> flowIds = flowRules.stream().map(FlowRule::id).collect(Collectors.toSet());
        Set<PendingGroupKey> pendingGroupKeys = groups.stream().map(GroupDescription::givenGroupId)
                .map(GroupId::new)
                .map(gid -> new PendingGroupKey(gid, objective.op()))
                .collect(Collectors.toSet());

        PendingInstallObjective pio =
                new PendingInstallObjective(objective, flowIds, pendingGroupKeys, callback);

        flowIds.forEach(flowId -> {
            PendingFlowKey pfk = new PendingFlowKey(flowId, objective.id());
            pendingInstallObjectiveFlows.put(pfk, pio);
        });

        pendingGroupKeys.forEach(pendingGroupKey -> {
            pendingInstallObjectiveGroups.put(pendingGroupKey, pio);
        });

        pendingInstallObjectives.put(objective, pio);
        installGroups(objective, groups);
        installFlows(objective, flowRules);
    }

    private void installFlows(Objective objective, Collection<FlowRule> flowRules) {
        if (flowRules.isEmpty()) {
            return;
        }

        FlowRuleOperationsContext ctx = new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                ops.stages().forEach(stage -> {
                    stage.forEach(op -> {
                        FlowId flowId = op.rule().id();
                        PendingFlowKey pfk = new PendingFlowKey(flowId, objective.id());
                        PendingInstallObjective pio = pendingInstallObjectiveFlows.remove(pfk);

                        if (pio != null) {
                            pio.flowInstalled(flowId);
                        }
                    });
                });
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                log.warn("Failed to install flow rules: {}", flowRules);
                PendingInstallObjective pio = pendingInstallObjectives.getIfPresent(objective);
                if (pio != null) {
                    pio.failed(objective, ObjectiveError.FLOWINSTALLATIONFAILED);
                }
            }
        };

        FlowRuleOperations ops = buildFlowRuleOps(objective, flowRules, ctx);
        if (ops != null) {
            flowRuleService.apply(ops);
        } else {
            // remove pendings
            flowRules.forEach(flowRule -> {
                PendingFlowKey pfk = new PendingFlowKey(flowRule.id(), objective.id());
                pendingInstallObjectiveFlows.remove(pfk);
            });
        }
    }

    private void installGroups(Objective objective, Collection<GroupDescription> groups) {
        if (groups.isEmpty()) {
            return;
        }

        switch (objective.op()) {
            case ADD:
                groups.forEach(groupService::addGroup);
                break;
            case REMOVE:
                groups.forEach(group -> groupService.removeGroup(deviceId, group.appCookie(), objective.appId()));
                break;
            case ADD_TO_EXISTING:
                groups.forEach(group -> {
                    groupService.addBucketsToGroup(deviceId, group.appCookie(),
                                                   group.buckets(),
                                                   group.appCookie(),
                                                   group.appId());
                });
                break;
            case REMOVE_FROM_EXISTING:
                groups.forEach(group -> {
                    groupService.removeBucketsFromGroup(deviceId, group.appCookie(),
                                                        group.buckets(),
                                                        group.appCookie(),
                                                        group.appId());
                });
                break;
            default:
                log.warn("Unsupported objective operation {}", objective.op());
        }
    }

    static void fail(Objective objective, ObjectiveError error) {
        CompletableFuture.runAsync(() -> {
            objective.context().ifPresent(ctx -> ctx.onError(objective, error));
        }, flowObjCallbackExecutor);

    }

    static void success(Objective objective) {
        CompletableFuture.runAsync(() -> {
            objective.context().ifPresent(ctx -> ctx.onSuccess(objective));
        }, flowObjCallbackExecutor);
    }

    static FlowRuleOperations buildFlowRuleOps(Objective objective, Collection<FlowRule> flowRules,
                                               FlowRuleOperationsContext ctx) {
        FlowRuleOperations.Builder ops = FlowRuleOperations.builder();
        switch (objective.op()) {
            case ADD:
            case ADD_TO_EXISTING: // For egress VLAN
                flowRules.forEach(ops::add);
                break;
            case REMOVE:
            case REMOVE_FROM_EXISTING: // For egress VLAN
                flowRules.forEach(ops::remove);
                break;
            default:
                log.warn("Unsupported op {} for {}", objective.op(), objective);
                fail(objective, ObjectiveError.BADPARAMS);
                return null;
        }
        return ops.build(ctx);
    }

    class FabricNextGroup implements NextGroup {
        private NextObjective.Type type;
        private Collection<PortNumber> outputPorts;

        public FabricNextGroup(NextObjective.Type type, Collection<PortNumber> outputPorts) {
            this.type = type;
            this.outputPorts = ImmutableList.copyOf(outputPorts);
        }

        public NextObjective.Type type() {
            return type;
        }

        public Collection<PortNumber> outputPorts() {
            return outputPorts;
        }

        @Override
        public byte[] data() {
            return KRYO.serialize(this);
        }
    }

    class InternalGroupListener implements GroupListener {
        @Override
        public void event(GroupEvent event) {
            GroupId groupId = event.subject().id();
            PendingGroupKey pendingGroupKey = new PendingGroupKey(groupId, event.type());
            PendingInstallObjective pio = pendingInstallObjectiveGroups.remove(pendingGroupKey);
            if (GROUP_FAILED_TYPES.contains(event.type())) {
                pio.failed(pio.objective, ObjectiveError.GROUPINSTALLATIONFAILED);
            }
            pio.groupInstalled(pendingGroupKey);
        }

        @Override
        public boolean isRelevant(GroupEvent event) {
            PendingGroupKey pendingGroupKey = new PendingGroupKey(event.subject().id(), event.type());
            return pendingInstallObjectiveGroups.containsKey(pendingGroupKey);
        }
    }

    class PendingInstallObjective {
        Objective objective;
        Collection<FlowId> flowIds;
        Collection<PendingGroupKey> pendingGroupKeys;
        Consumer<ObjectiveError> callback;

        public PendingInstallObjective(Objective objective, Collection<FlowId> flowIds,
                                       Collection<PendingGroupKey> pendingGroupKeys,
                                       Consumer<ObjectiveError> callback) {
            this.objective = objective;
            this.flowIds = flowIds;
            this.pendingGroupKeys = pendingGroupKeys;
            this.callback = callback;
        }

        void flowInstalled(FlowId flowId) {
            synchronized (this) {
                flowIds.remove(flowId);
                checkIfFinished();
            }
        }

        void groupInstalled(PendingGroupKey pendingGroupKey) {
            synchronized (this) {
                pendingGroupKeys.remove(pendingGroupKey);
                checkIfFinished();
            }
        }

        private void checkIfFinished() {
            if (flowIds.isEmpty() && pendingGroupKeys.isEmpty()) {
                pendingInstallObjectives.invalidate(objective);
                callback.accept(null);
            }
        }

        void failed(Objective obj, ObjectiveError error) {
            flowIds.forEach(flowId -> {
                PendingFlowKey pfk = new PendingFlowKey(flowId, obj.id());
                pendingInstallObjectiveFlows.remove(pfk);
            });
            pendingGroupKeys.forEach(pendingInstallObjectiveGroups::remove);
            pendingInstallObjectives.invalidate(objective);
            callback.accept(error);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            PendingInstallObjective pio = (PendingInstallObjective) o;
            return Objects.equal(objective, pio.objective) &&
                    Objects.equal(flowIds, pio.flowIds) &&
                    Objects.equal(pendingGroupKeys, pio.pendingGroupKeys) &&
                    Objects.equal(callback, pio.callback);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(objective, flowIds, pendingGroupKeys, callback);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("obj", objective)
                    .add("flowIds", flowIds)
                    .add("pendingGroupKeys", pendingGroupKeys)
                    .add("callback", callback)
                    .toString();
        }
    }

    class PendingFlowKey {
        private FlowId flowId;
        private int objId;

        PendingFlowKey(FlowId flowId, int objId) {
            this.flowId = flowId;
            this.objId = objId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            PendingFlowKey pendingFlowKey = (PendingFlowKey) o;
            return Objects.equal(flowId, pendingFlowKey.flowId) &&
                    objId == pendingFlowKey.objId;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(flowId, objId);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("flowId", flowId)
                    .add("objId", objId)
                    .toString();
        }
    }

    class PendingGroupKey {
        private GroupId groupId;
        private GroupEvent.Type expectedEventType;

        PendingGroupKey(GroupId groupId, GroupEvent.Type expectedEventType) {
            this.groupId = groupId;
            this.expectedEventType = expectedEventType;
        }

        PendingGroupKey(GroupId groupId, NextObjective.Operation objOp) {
            this.groupId = groupId;

            switch (objOp) {
                case ADD:
                    expectedEventType = GroupEvent.Type.GROUP_ADDED;
                    break;
                case REMOVE:
                    expectedEventType = GroupEvent.Type.GROUP_REMOVED;
                    break;
                case MODIFY:
                case ADD_TO_EXISTING:
                case REMOVE_FROM_EXISTING:
                    expectedEventType = GroupEvent.Type.GROUP_UPDATED;
                    break;
                default:
                    expectedEventType = null;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            PendingGroupKey pendingGroupKey = (PendingGroupKey) o;
            return Objects.equal(groupId, pendingGroupKey.groupId) &&
                    expectedEventType == pendingGroupKey.expectedEventType;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(groupId, expectedEventType);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("groupId", groupId)
                    .add("expectedEventType", expectedEventType)
                    .toString();
        }
    }
}
