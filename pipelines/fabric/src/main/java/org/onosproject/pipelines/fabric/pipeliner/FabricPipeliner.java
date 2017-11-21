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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.onlab.util.KryoNamespace;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.NextGroup;
import org.onosproject.net.behaviour.Pipeliner;
import org.onosproject.net.behaviour.PipelinerContext;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
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
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupDescription;
import org.onosproject.net.group.GroupEvent;
import org.onosproject.net.group.GroupListener;
import org.onosproject.net.group.GroupService;
import org.onosproject.store.serializers.KryoNamespaces;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
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

    // TODO: make this configurable
    private static final long DEFAULT_INSTALLATION_TIME_OUT = 40;
    private static final Map<Objective.Operation, GroupEvent.Type> OBJ_OP_TO_GRP_EVENT_TYPE =
            ImmutableMap.<Objective.Operation, GroupEvent.Type>builder()
                    .put(Objective.Operation.ADD, GroupEvent.Type.GROUP_ADDED)
                    .put(Objective.Operation.ADD_TO_EXISTING, GroupEvent.Type.GROUP_UPDATED)
                    .put(Objective.Operation.REMOVE, GroupEvent.Type.GROUP_REMOVED)
                    .put(Objective.Operation.REMOVE_FROM_EXISTING, GroupEvent.Type.GROUP_UPDATED)
            .build();

    protected DeviceId deviceId;
    protected FlowRuleService flowRuleService;
    protected GroupService groupService;
    protected FlowObjectiveStore flowObjectiveStore;
    protected FabricFilteringPipeliner pipelinerFilter;
    protected FabricForwardingPipeliner pipelinerForward;
    protected FabricNextPipeliner pipelinerNext;


    @Override
    public void init(DeviceId deviceId, PipelinerContext context) {
        this.deviceId = deviceId;
        this.flowRuleService = context.directory().get(FlowRuleService.class);
        this.groupService = context.directory().get(GroupService.class);
        this.flowObjectiveStore = context.directory().get(FlowObjectiveStore.class);
        this.pipelinerFilter = new FabricFilteringPipeliner(deviceId);
        this.pipelinerForward = new FabricForwardingPipeliner(deviceId);
        this.pipelinerNext = new FabricNextPipeliner(deviceId);
    }

    @Override
    public void filter(FilteringObjective filterObjective) {
        PipelinerTranslationResult result = pipelinerFilter.filter(filterObjective);
        if (result.error().isPresent()) {
            fail(filterObjective, result.error().get());
            return;
        }

        applyTranslationResult(filterObjective, result, success -> {
            if (success) {
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

        applyTranslationResult(forwardObjective, result, success -> {
            if (success) {
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

        applyTranslationResult(nextObjective, result, success -> {
            if (!success) {
                fail(nextObjective, ObjectiveError.GROUPINSTALLATIONFAILED);
                return;
            }

            // Success, put next group to objective store
            List<PortNumber> portNumbers = Lists.newArrayList();
            nextObjective.next().forEach(treatment -> {
                Instructions.OutputInstruction outputInst = treatment.allInstructions()
                        .stream()
                        .filter(inst -> inst.type() == Instruction.Type.OUTPUT)
                        .map(inst -> (Instructions.OutputInstruction) inst)
                        .findFirst()
                        .orElse(null);

                if (outputInst != null) {
                    portNumbers.add(outputInst.port());
                }
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
                                        Consumer<Boolean> callback) {
        Collection<GroupDescription> groups = result.groups();
        Collection<FlowRule> flowRules = result.flowRules();
        CompletableFuture.supplyAsync(() -> installGroups(objective, groups))
                .thenApplyAsync(groupSuccess -> groupSuccess && installFlows(objective, flowRules))
                .thenAcceptAsync(callback)
                .exceptionally((ex) -> {
                    log.warn("Got unexpected exception while applying translation result {}",
                             result);
                    fail(objective, ObjectiveError.UNKNOWN);
                    return null;
                });
    }

    private boolean installFlows(Objective objective, Collection<FlowRule> flowRules) {
        if (flowRules.isEmpty()) {
            return true;
        }
        CompletableFuture<Boolean> flowInstallFuture = new CompletableFuture<>();
        FlowRuleOperationsContext ctx = new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                flowInstallFuture.complete(true);
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                log.warn("Failed to install flow rules: {}", flowRules);
                flowInstallFuture.complete(false);
            }
        };

        FlowRuleOperations ops = buildFlowRuleOps(objective, flowRules, ctx);
        flowRuleService.apply(ops);

        try {
            return flowInstallFuture.get(DEFAULT_INSTALLATION_TIME_OUT, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.warn("Got exception while installing flows:{}", e.getMessage());
            return false;
        }
    }

    private boolean installGroups(Objective objective, Collection<GroupDescription> groups) {
        if (groups.isEmpty()) {
            return true;
        }
        Collection<Integer> groupIds = groups.stream()
                .map(GroupDescription::givenGroupId)
                .collect(Collectors.toSet());

        int numGroupsToBeInstalled = groups.size();
        CompletableFuture<Boolean> groupInstallFuture = new CompletableFuture<>();
        AtomicInteger numGroupsInstalled = new AtomicInteger(0);

        GroupListener listener = new GroupListener() {
            @Override
            public void event(GroupEvent event) {
                log.debug("Receive group event for group {}", event.subject());
                int currentNumGroupInstalled = numGroupsInstalled.incrementAndGet();
                if (currentNumGroupInstalled == numGroupsToBeInstalled) {
                    // install completed
                    groupService.removeListener(this);
                    groupInstallFuture.complete(true);
                }
            }
            @Override
            public boolean isRelevant(GroupEvent event) {
                Group group = event.subject();
                return groupIds.contains(group.givenGroupId());
            }
        };

        groupService.addListener(listener);

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
                groupService.removeListener(listener);
        }
        try {
            return groupInstallFuture.get(DEFAULT_INSTALLATION_TIME_OUT, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            groupService.removeListener(listener);
            log.warn("Got exception while installing groups: {}", e.getMessage());
            return false;
        }
    }

    static void fail(Objective objective, ObjectiveError error) {
        objective.context().ifPresent(ctx -> ctx.onError(objective, error));
    }

    static void success(Objective objective) {
        objective.context().ifPresent(ctx -> ctx.onSuccess(objective));
    }

    static FlowRuleOperations buildFlowRuleOps(Objective objective, Collection<FlowRule> flowRules,
                                               FlowRuleOperationsContext ctx) {
        FlowRuleOperations.Builder ops = FlowRuleOperations.builder();
        switch (objective.op()) {
            case ADD:
                flowRules.forEach(ops::add);
                break;
            case REMOVE:
                flowRules.forEach(ops::remove);
                break;
            default:
                log.warn("Unsupported op {} for {}", objective);
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

}
