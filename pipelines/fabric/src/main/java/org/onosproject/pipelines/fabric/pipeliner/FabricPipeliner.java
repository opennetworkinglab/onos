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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
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

    private static final int NUM_CALLBACK_THREAD = 2;

    protected DeviceId deviceId;
    protected FlowRuleService flowRuleService;
    protected GroupService groupService;
    protected FlowObjectiveStore flowObjectiveStore;
    FabricFilteringPipeliner pipelinerFilter;
    FabricForwardingPipeliner pipelinerForward;
    FabricNextPipeliner pipelinerNext;

    private Map<PendingFlowKey, PendingInstallObjective> pendingInstallObjectiveFlows = new ConcurrentHashMap<>();
    private Map<PendingGroupKey, PendingInstallObjective> pendingInstallObjectiveGroups = new ConcurrentHashMap<>();
    private Map<Objective, PendingInstallObjective> pendingInstallObjectives = Maps.newConcurrentMap();

    private static ExecutorService flowObjCallbackExecutor =
            Executors.newFixedThreadPool(NUM_CALLBACK_THREAD, Tools.groupedThreads("fabric-pipeliner", "cb-", log));


    @Override
    public void init(DeviceId deviceId, PipelinerContext context) {
        Driver driver = handler().driver();
        this.deviceId = deviceId;
        this.flowRuleService = context.directory().get(FlowRuleService.class);
        this.groupService = context.directory().get(GroupService.class);
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
                log.info("Ignore error {}. Let flow subsystem retry", error);
                success(filterObjective);
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
                log.info("Ignore error {}. Let flow subsystem retry", error);
                success(forwardObjective);
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

        if (nextObjective.op() == Objective.Operation.MODIFY) {
            // TODO: support MODIFY operation
            log.debug("Currently we don't support MODIFY operation, return failure directly to the context");
            fail(nextObjective, ObjectiveError.UNSUPPORTED);
            return;
        }

        applyTranslationResult(nextObjective, result, error -> {
            if (error != null) {
                log.info("Ignore error {}. Let flow/group subsystem retry", error);
                success(nextObjective);
                return;
            }

            if (nextObjective.op() == Objective.Operation.REMOVE) {
                if (flowObjectiveStore.getNextGroup(nextObjective.id()) == null) {
                    log.warn("Can not find next obj {} from store", nextObjective.id());
                    return;
                }
                flowObjectiveStore.removeNextGroup(nextObjective.id());
            } else {
                // Success, put next group to objective store
                List<PortNumber> portNumbers = Lists.newArrayList();
                nextObjective.next().forEach(treatment ->
                        treatment.allInstructions()
                                .stream()
                                .filter(inst -> inst.type() == Instruction.Type.OUTPUT)
                                .map(inst -> (Instructions.OutputInstruction) inst)
                                .findFirst()
                                .map(Instructions.OutputInstruction::port)
                                .ifPresent(portNumbers::add)
                );
                FabricNextGroup nextGroup = new FabricNextGroup(nextObjective.type(),
                                                                portNumbers);
                flowObjectiveStore.putNextGroup(nextObjective.id(), nextGroup);
            }

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

        pendingGroupKeys.forEach(pendingGroupKey ->
            pendingInstallObjectiveGroups.put(pendingGroupKey, pio)
        );

        pendingInstallObjectives.put(objective, pio);
        installGroups(objective, groups);
        installFlows(objective, flowRules);
    }

    private void installFlows(Objective objective, Collection<FlowRule> flowRules) {
        if (flowRules.isEmpty()) {
            return;
        }

        FlowRuleOperations ops = buildFlowRuleOps(objective, flowRules);
        if (ops == null) {
            return;
        }
        flowRuleService.apply(ops);

        flowRules.forEach(flow -> {
            PendingFlowKey pfk = new PendingFlowKey(flow.id(), objective.id());
            PendingInstallObjective pio = pendingInstallObjectiveFlows.remove(pfk);

            if (pio != null) {
                pio.flowInstalled(flow.id());
            }
        });
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
                groups.forEach(group -> groupService.addBucketsToGroup(deviceId, group.appCookie(),
                        group.buckets(), group.appCookie(), group.appId())
                );
                break;
            case REMOVE_FROM_EXISTING:
                groups.forEach(group -> groupService.removeBucketsFromGroup(deviceId, group.appCookie(),
                        group.buckets(), group.appCookie(), group.appId())
                );
                break;
            default:
                log.warn("Unsupported objective operation {}", objective.op());
                return;
        }

        groups.forEach(group -> {
            PendingGroupKey pendingGroupKey = new PendingGroupKey(new GroupId(group.givenGroupId()), objective.op());
            PendingInstallObjective pio = pendingInstallObjectiveGroups.remove(pendingGroupKey);
            pio.groupInstalled(pendingGroupKey);
        });

    }

    private static void fail(Objective objective, ObjectiveError error) {
        CompletableFuture.runAsync(() -> objective.context().ifPresent(ctx -> ctx.onError(objective, error)),
                flowObjCallbackExecutor);

    }

    private static void success(Objective objective) {
        CompletableFuture.runAsync(() -> objective.context().ifPresent(ctx -> ctx.onSuccess(objective)),
                flowObjCallbackExecutor);
    }

    private static FlowRuleOperations buildFlowRuleOps(Objective objective, Collection<FlowRule> flowRules) {
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
        return ops.build();
    }

    class FabricNextGroup implements NextGroup {
        private NextObjective.Type type;
        private Collection<PortNumber> outputPorts;

        FabricNextGroup(NextObjective.Type type, Collection<PortNumber> outputPorts) {
            this.type = type;
            this.outputPorts = ImmutableList.copyOf(outputPorts);
        }

        NextObjective.Type type() {
            return type;
        }

        Collection<PortNumber> outputPorts() {
            return outputPorts;
        }

        @Override
        public byte[] data() {
            return KRYO.serialize(this);
        }
    }

    class PendingInstallObjective {
        Objective objective;
        Collection<FlowId> flowIds;
        Collection<PendingGroupKey> pendingGroupKeys;
        Consumer<ObjectiveError> callback;

        PendingInstallObjective(Objective objective, Collection<FlowId> flowIds,
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
                pendingInstallObjectives.remove(objective);
                callback.accept(null);
            }
        }

        void failed(Objective obj, ObjectiveError error) {
            flowIds.forEach(flowId -> {
                PendingFlowKey pfk = new PendingFlowKey(flowId, obj.id());
                pendingInstallObjectiveFlows.remove(pfk);
            });
            pendingGroupKeys.forEach(pendingInstallObjectiveGroups::remove);
            pendingInstallObjectives.remove(objective);
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
