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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalListener;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.onlab.util.KryoNamespace;
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
import java.util.concurrent.ConcurrentHashMap;
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

    protected DeviceId deviceId;
    protected FlowRuleService flowRuleService;
    protected GroupService groupService;
    protected GroupListener groupListener = new InternalGroupListener();
    protected FlowObjectiveStore flowObjectiveStore;
    protected FabricFilteringPipeliner pipelinerFilter;
    protected FabricForwardingPipeliner pipelinerForward;
    protected FabricNextPipeliner pipelinerNext;

    private Map<FlowId, PendingInstallObjective> pendingInstallObjectiveFlows = new ConcurrentHashMap<>();
    private Map<GroupId, PendingInstallObjective> pendingInstallObjectiveGroups = new ConcurrentHashMap<>();
    private Cache<Objective, PendingInstallObjective> pendingInstallObjectives = CacheBuilder.newBuilder()
            .expireAfterWrite(DEFAULT_INSTALLATION_TIME_OUT, TimeUnit.SECONDS)
            .removalListener((RemovalListener<Objective, PendingInstallObjective>) removalNotification -> {
                RemovalCause cause = removalNotification.getCause();
                PendingInstallObjective pio = removalNotification.getValue();
                if (cause == RemovalCause.EXPIRED && pio != null) {
                    pio.failed(ObjectiveError.INSTALLATIONTIMEOUT);
                }
            })
            .build();


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

        Set<FlowId> flowIds = flowRules.stream().map(FlowRule::id).collect(Collectors.toSet());
        Set<GroupId> groupIds = groups.stream().map(GroupDescription::givenGroupId)
                .map(GroupId::new).collect(Collectors.toSet());

        PendingInstallObjective pio =
                new PendingInstallObjective(objective, flowIds, groupIds, callback);

        flowIds.forEach(flowId -> {
            pendingInstallObjectiveFlows.put(flowId, pio);
        });

        groupIds.forEach(groupId -> {
            pendingInstallObjectiveGroups.put(groupId, pio);
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
                        PendingInstallObjective pio = pendingInstallObjectiveFlows.remove(flowId);

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
                    pio.failed(ObjectiveError.FLOWINSTALLATIONFAILED);
                }
            }
        };

        FlowRuleOperations ops = buildFlowRuleOps(objective, flowRules, ctx);
        if (ops != null) {
            flowRuleService.apply(ops);
        } else {
            // remove pendings
            flowRules.forEach(flowRule -> pendingInstallObjectiveFlows.remove(flowRule.id()));
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
            case ADD_TO_EXISTING:
            case REMOVE_FROM_EXISTING:
                // Next objective may use ADD_TO_EXIST or REMOVE_FROM_EXIST op
                // No need to update FlowRuls for vlan_meta table.
                return null;
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
            PendingInstallObjective pio = pendingInstallObjectiveGroups.remove(groupId);
            if (pio == null) {
                return;
            }
            if (GROUP_FAILED_TYPES.contains(event.type())) {
                pio.failed(ObjectiveError.GROUPINSTALLATIONFAILED);
            }
            pio.groupInstalled(groupId);
        }

        @Override
        public boolean isRelevant(GroupEvent event) {
            return pendingInstallObjectiveGroups.containsKey(event.subject().id());
        }
    }

    class PendingInstallObjective {
        Objective objective;
        Collection<FlowId> flowIds;
        Collection<GroupId> groupIds;
        Consumer<Boolean> callback;

        public PendingInstallObjective(Objective objective, Collection<FlowId> flowIds,
                                       Collection<GroupId> groupIds, Consumer<Boolean> callback) {
            this.objective = objective;
            this.flowIds = flowIds;
            this.groupIds = groupIds;
            this.callback = callback;
        }

        void flowInstalled(FlowId flowId) {
            flowIds.remove(flowId);
            checkIfFinished();
        }

        void groupInstalled(GroupId groupId) {
            groupIds.remove(groupId);
            checkIfFinished();
        }

        private void checkIfFinished() {
            if (flowIds.isEmpty() && groupIds.isEmpty()) {
                pendingInstallObjectives.invalidate(objective);
                callback.accept(true);
            }
        }

        void failed(ObjectiveError error) {
            flowIds.forEach(pendingInstallObjectiveFlows::remove);
            groupIds.forEach(pendingInstallObjectiveGroups::remove);
            pendingInstallObjectives.invalidate(objective);
            fail(objective, error);
        }
    }
}
