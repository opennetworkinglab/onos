/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.driver.pipeline;

import static java.util.concurrent.Executors.newScheduledThreadPool;
import static org.onlab.util.Tools.groupedThreads;
import static org.slf4j.LoggerFactory.getLogger;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalNotification;

import com.google.common.collect.ImmutableList;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.packet.Ethernet;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.NextGroup;
import org.onosproject.net.behaviour.Pipeliner;
import org.onosproject.net.behaviour.PipelinerContext;
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
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.Criterion.Type;
import org.onosproject.net.flow.criteria.EthCriterion;
import org.onosproject.net.flow.criteria.EthTypeCriterion;
import org.onosproject.net.flow.criteria.IPCriterion;
import org.onosproject.net.flow.criteria.MplsBosCriterion;
import org.onosproject.net.flow.criteria.MplsCriterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.criteria.VlanIdCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions.OutputInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.ModVlanIdInstruction;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.net.flowobjective.FlowObjectiveStore;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.NextObjective;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.onosproject.net.group.DefaultGroupBucket;
import org.onosproject.net.group.DefaultGroupDescription;
import org.onosproject.net.group.DefaultGroupKey;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupBucket;
import org.onosproject.net.group.GroupBuckets;
import org.onosproject.net.group.GroupDescription;
import org.onosproject.net.group.GroupEvent;
import org.onosproject.net.group.GroupKey;
import org.onosproject.net.group.GroupListener;
import org.onosproject.net.group.GroupService;
import org.onosproject.store.serializers.KryoNamespaces;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Driver for SPRING-OPEN pipeline.
 */
public class SpringOpenTTP extends AbstractHandlerBehaviour
        implements Pipeliner {

    /**
     * GroupCheck delay.
     */
    private static final int CHECK_DELAY = 500;

    // Default table ID - compatible with CpqD switch
    private static final int TABLE_VLAN = 0;
    private static final int TABLE_TMAC = 1;
    private static final int TABLE_IPV4_UNICAST = 2;
    private static final int TABLE_MPLS = 3;
    private static final int TABLE_DMAC = 4;
    private static final int TABLE_ACL = 5;
    private static final int TABLE_SMAC = 6;

    /**
     * Set the default values. These variables will get overwritten based on the
     * switch vendor type
     */
    protected int vlanTableId = TABLE_VLAN;
    protected int tmacTableId = TABLE_TMAC;
    protected int ipv4UnicastTableId = TABLE_IPV4_UNICAST;
    protected int mplsTableId = TABLE_MPLS;
    protected int dstMacTableId = TABLE_DMAC;
    protected int aclTableId = TABLE_ACL;
    protected int srcMacTableId = TABLE_SMAC;

    private static final Logger log = getLogger(SpringOpenTTP.class);

    private ServiceDirectory serviceDirectory;
    private FlowRuleService flowRuleService;
    private CoreService coreService;
    protected GroupService groupService;
    protected FlowObjectiveStore flowObjectiveStore;
    protected DeviceId deviceId;
    private ApplicationId appId;

    private Cache<GroupKey, NextObjective> pendingGroups;

    private static final ScheduledExecutorService GROUP_CHECKER
        = newScheduledThreadPool(2,
                                 groupedThreads("onos/pipeliner",
                                                "spring-open-%d", log));
    static {
        // ONOS-3579 workaround, let core threads die out on idle
        if (GROUP_CHECKER instanceof ScheduledThreadPoolExecutor) {
            ScheduledThreadPoolExecutor executor = (ScheduledThreadPoolExecutor) GROUP_CHECKER;
            executor.setKeepAliveTime(CHECK_DELAY * 2L, TimeUnit.MILLISECONDS);
            executor.allowCoreThreadTimeOut(true);
        }
    }

    protected KryoNamespace appKryo = new KryoNamespace.Builder()
            .register(KryoNamespaces.API)
            .register(GroupKey.class)
            .register(DefaultGroupKey.class)
            .register(TrafficTreatment.class)
            .register(SpringOpenGroup.class)
            .build("SpringOpenTTP");

    @Override
    public void init(DeviceId deviceId, PipelinerContext context) {
        this.serviceDirectory = context.directory();
        this.deviceId = deviceId;

        pendingGroups = CacheBuilder
                .newBuilder()
                .expireAfterWrite(20, TimeUnit.SECONDS)
                .removalListener((RemovalNotification<GroupKey, NextObjective> notification) -> {
                                     if (notification.getCause() == RemovalCause.EXPIRED) {
                                         fail(notification.getValue(),
                                              ObjectiveError.GROUPINSTALLATIONFAILED);
                                     }
                                 }).build();

        coreService = serviceDirectory.get(CoreService.class);
        flowRuleService = serviceDirectory.get(FlowRuleService.class);
        groupService = serviceDirectory.get(GroupService.class);
        flowObjectiveStore = context.store();

        groupService.addListener(new InnerGroupListener());

        appId = coreService
                .registerApplication("org.onosproject.driver.SpringOpenTTP");

        setTableMissEntries();
        log.info("Spring Open TTP driver initialized");
    }

    @Override
    public void filter(FilteringObjective filteringObjective) {
        if (filteringObjective.type() == FilteringObjective.Type.PERMIT) {
            log.debug("processing PERMIT filter objective");
            processFilter(filteringObjective,
                          filteringObjective.op() == Objective.Operation.ADD,
                          filteringObjective.appId());
        } else {
            log.debug("filter objective other than PERMIT not supported");
            fail(filteringObjective, ObjectiveError.UNSUPPORTED);
        }
    }

    @Override
    public void forward(ForwardingObjective fwd) {
        Collection<FlowRule> rules;
        FlowRuleOperations.Builder flowBuilder = FlowRuleOperations.builder();

        rules = processForward(fwd);
        switch (fwd.op()) {
        case ADD:
            rules.stream().filter(Objects::nonNull)
                    .forEach(flowBuilder::add);
            break;
        case REMOVE:
            rules.stream().filter(Objects::nonNull)
                    .forEach(flowBuilder::remove);
            break;
        default:
            fail(fwd, ObjectiveError.UNKNOWN);
            log.warn("Unknown forwarding type {}", fwd.op());
        }

        flowRuleService.apply(flowBuilder
                .build(new FlowRuleOperationsContext() {
                    @Override
                    public void onSuccess(FlowRuleOperations ops) {
                        pass(fwd);
                        log.debug("Provisioned tables in {} successfully with "
                                + "forwarding rules", deviceId);
                    }

                    @Override
                    public void onError(FlowRuleOperations ops) {
                        fail(fwd, ObjectiveError.FLOWINSTALLATIONFAILED);
                        log.warn("Failed to provision tables in {} with "
                                + "forwarding rules", deviceId);
                    }
                }));

    }

    @Override
    public void next(NextObjective nextObjective) {
        NextGroup nextGroup = flowObjectiveStore.getNextGroup(nextObjective.id());
        switch (nextObjective.op()) {
        case ADD:
            if (nextGroup != null) {
                log.warn("Cannot add next {} that already exists in device {}",
                         nextObjective.id(), deviceId);
                return;
            }
            log.debug("Processing NextObjective id{} in dev{} - add group",
                      nextObjective.id(), deviceId);
            addGroup(nextObjective);
            break;
        case ADD_TO_EXISTING:
            if (nextGroup != null) {
                log.debug("Processing NextObjective id{} in dev{} - add bucket",
                          nextObjective.id(), deviceId);
                addBucketToGroup(nextObjective);
            } else {
                log.warn("Cannot add to group that does not exist");
            }
            break;
        case REMOVE:
            if (nextGroup == null) {
                log.warn("Cannot remove next {} that does not exist in device {}",
                         nextObjective.id(), deviceId);
                return;
            }
            log.debug("Processing NextObjective id{}  in dev{} - remove group",
                      nextObjective.id(), deviceId);
            removeGroup(nextObjective);
            break;
        case REMOVE_FROM_EXISTING:
            if (nextGroup == null) {
                log.warn("Cannot remove from next {} that does not exist in device {}",
                         nextObjective.id(), deviceId);
                return;
            }
            log.debug("Processing NextObjective id{} in dev{} - remove bucket",
                      nextObjective.id(), deviceId);
            removeBucketFromGroup(nextObjective);
            break;
        default:
            log.warn("Unsupported operation {}", nextObjective.op());
        }
    }

    @Override
    public void purgeAll(ApplicationId appId) {
        flowRuleService.purgeFlowRules(deviceId, appId);
        groupService.purgeGroupEntries(deviceId, appId);
    }

    private void removeGroup(NextObjective nextObjective) {
        log.debug("removeGroup in {}: for next objective id {}",
                  deviceId, nextObjective.id());
        final GroupKey key = new DefaultGroupKey(
                appKryo.serialize(nextObjective.id()));
        groupService.removeGroup(deviceId, key, appId);
    }

    private void addGroup(NextObjective nextObjective) {
        log.debug("addGroup with type{} for nextObjective id {}",
                  nextObjective.type(), nextObjective.id());
        List<GroupBucket> buckets;
        switch (nextObjective.type()) {
            case SIMPLE:
                Collection<TrafficTreatment> treatments = nextObjective.next();
                if (treatments.size() == 1) {
                    // Spring Open TTP converts simple nextObjective to flow-actions
                    // in a dummy group
                    TrafficTreatment treatment = nextObjective.next().iterator().next();
                    log.debug("Converting SIMPLE group for next objective id {} " +
                            "to {} flow-actions in device:{}", nextObjective.id(),
                            treatment.allInstructions().size(), deviceId);
                    flowObjectiveStore.putNextGroup(nextObjective.id(),
                                                    new SpringOpenGroup(null, treatment));
                }
                break;
            case HASHED:
                // we convert MPLS ECMP groups to flow-actions for a single
                // bucket(output port).
                boolean mplsEcmp = false;
                if (nextObjective.meta() != null) {
                    for (Criterion c : nextObjective.meta().criteria()) {
                        if (c.type() == Type.MPLS_LABEL) {
                            mplsEcmp = true;
                        }
                    }
                }
                if (mplsEcmp) {
                    // covert to flow-actions in a dummy group by choosing the first bucket
                    log.debug("Converting HASHED group for next objective id {} " +
                              "to flow-actions in device:{}", nextObjective.id(),
                              deviceId);
                    TrafficTreatment treatment = nextObjective.next().iterator().next();
                    flowObjectiveStore.putNextGroup(nextObjective.id(),
                                                    new SpringOpenGroup(null, treatment));
                } else {
                    // process as ECMP group
                    buckets = nextObjective
                            .next()
                            .stream()
                            .map(DefaultGroupBucket::createSelectGroupBucket)
                            .collect(Collectors.toList());
                    if (!buckets.isEmpty()) {
                        final GroupKey key = new DefaultGroupKey(
                                                     appKryo.serialize(nextObjective.id()));
                        GroupDescription groupDescription = new DefaultGroupDescription(
                                                  deviceId,
                                                  GroupDescription.Type.SELECT,
                                                  new GroupBuckets(buckets),
                                                  key,
                                                  null,
                                                  nextObjective.appId());
                        log.debug("Creating HASHED group for next objective id {}"
                                + " in dev:{}", nextObjective.id(), deviceId);
                        pendingGroups.put(key, nextObjective);
                        groupService.addGroup(groupDescription);
                        verifyPendingGroupLater();
                    }
                }
                break;
            case BROADCAST:
                buckets = nextObjective
                        .next()
                        .stream()
                        .map(DefaultGroupBucket::createAllGroupBucket)
                        .collect(Collectors.toList());
                if (!buckets.isEmpty()) {
                    final GroupKey key = new DefaultGroupKey(
                            appKryo.serialize(nextObjective
                                                      .id()));
                    GroupDescription groupDescription = new DefaultGroupDescription(
                            deviceId,
                            GroupDescription.Type.ALL,
                            new GroupBuckets(buckets),
                            key,
                            null,
                            nextObjective.appId());
                    log.debug("Creating BROADCAST group for next objective id {} "
                            + "in device {}", nextObjective.id(), deviceId);
                    pendingGroups.put(key, nextObjective);
                    groupService.addGroup(groupDescription);
                    verifyPendingGroupLater();
                }
                break;
            case FAILOVER:
                log.debug("FAILOVER next objectives not supported");
                fail(nextObjective, ObjectiveError.UNSUPPORTED);
                log.warn("Unsupported next objective type {}", nextObjective.type());
                break;
            default:
                fail(nextObjective, ObjectiveError.UNKNOWN);
                log.warn("Unknown next objective type {}", nextObjective.type());
        }
    }

    private void addBucketToGroup(NextObjective nextObjective) {
        log.debug("addBucketToGroup in {}: for next objective id {}",
                  deviceId, nextObjective.id());
        Collection<TrafficTreatment> treatments = nextObjective.next();
        TrafficTreatment treatment = treatments.iterator().next();
        final GroupKey key = new DefaultGroupKey(
                appKryo.serialize(nextObjective
                        .id()));
        Group group = groupService.getGroup(deviceId, key);
        if (group == null) {
            log.warn("Group is not found in {} for {}", deviceId, key);
            return;
        }
        GroupBucket bucket;
        if (group.type() == GroupDescription.Type.INDIRECT) {
            bucket = DefaultGroupBucket.createIndirectGroupBucket(treatment);
        } else if (group.type() == GroupDescription.Type.SELECT) {
            bucket = DefaultGroupBucket.createSelectGroupBucket(treatment);
        } else if (group.type() == GroupDescription.Type.ALL) {
            bucket = DefaultGroupBucket.createAllGroupBucket(treatment);
        } else {
            log.warn("Unsupported Group type {}", group.type());
            return;
        }
        GroupBuckets bucketsToAdd = new GroupBuckets(Collections.singletonList(bucket));
        log.debug("Adding buckets to group id {} of next objective id {} in device {}",
                  group.id(), nextObjective.id(), deviceId);
        groupService.addBucketsToGroup(deviceId, key, bucketsToAdd, key, appId);
    }

    private void removeBucketFromGroup(NextObjective nextObjective) {
        log.debug("removeBucketFromGroup in {}: for next objective id {}",
                  deviceId, nextObjective.id());
        NextGroup nextGroup = flowObjectiveStore.getNextGroup(nextObjective.id());
        if (nextGroup != null) {
            Collection<TrafficTreatment> treatments = nextObjective.next();
            TrafficTreatment treatment = treatments.iterator().next();
            final GroupKey key = new DefaultGroupKey(
                    appKryo.serialize(nextObjective
                            .id()));
            Group group = groupService.getGroup(deviceId, key);
            if (group == null) {
                log.warn("Group is not found in {} for {}", deviceId, key);
                return;
            }
            GroupBucket bucket;
            if (group.type() == GroupDescription.Type.INDIRECT) {
                bucket = DefaultGroupBucket.createIndirectGroupBucket(treatment);
            } else if (group.type() == GroupDescription.Type.SELECT) {
                bucket = DefaultGroupBucket.createSelectGroupBucket(treatment);
            } else if (group.type() == GroupDescription.Type.ALL) {
                bucket = DefaultGroupBucket.createAllGroupBucket(treatment);
            } else {
                log.warn("Unsupported Group type {}", group.type());
                return;
            }
            GroupBuckets removeBuckets = new GroupBuckets(Collections.singletonList(bucket));
            log.debug("Removing buckets from group id {} of next objective id {} in device {}",
                      group.id(), nextObjective.id(), deviceId);
            groupService.removeBucketsFromGroup(deviceId, key, removeBuckets, key, appId);
        }
    }

    private Collection<FlowRule> processForward(ForwardingObjective fwd) {
        switch (fwd.flag()) {
        case SPECIFIC:
            return processSpecific(fwd);
        case VERSATILE:
            return processVersatile(fwd);
        default:
            fail(fwd, ObjectiveError.UNKNOWN);
            log.warn("Unknown forwarding flag {}", fwd.flag());
        }
        return Collections.emptySet();
    }

    private Collection<FlowRule> processVersatile(ForwardingObjective fwd) {
        log.debug("Processing versatile forwarding objective in dev:{}", deviceId);
        TrafficSelector selector = fwd.selector();
        EthTypeCriterion ethType =
                (EthTypeCriterion) selector.getCriterion(Criterion.Type.ETH_TYPE);
        if (ethType == null) {
            log.error("Versatile forwarding objective must include ethType");
            fail(fwd, ObjectiveError.UNKNOWN);
            return Collections.emptySet();
        }

        if (fwd.treatment() == null && fwd.nextId() == null) {
            log.error("VERSATILE forwarding objective needs next objective ID "
                    + "or treatment.");
            return Collections.emptySet();
        }
        // emulation of ACL table (for versatile fwd objective) requires
        // overriding any previous instructions
        TrafficTreatment.Builder treatmentBuilder = DefaultTrafficTreatment
                .builder();
        treatmentBuilder.wipeDeferred();

        if (fwd.nextId() != null) {
            NextGroup next = flowObjectiveStore.getNextGroup(fwd.nextId());
            if (next != null) {
                SpringOpenGroup soGroup = appKryo.deserialize(next.data());
                if (soGroup.dummy) {
                    // need to convert to flow-actions
                    for (Instruction ins : soGroup.treatment.allInstructions()) {
                        treatmentBuilder.add(ins);
                    }
                } else {
                    GroupKey key = soGroup.key;
                    Group group = groupService.getGroup(deviceId, key);
                    if (group == null) {
                        log.warn("The group left!");
                        fail(fwd, ObjectiveError.GROUPMISSING);
                        return Collections.emptySet();
                    }
                    treatmentBuilder.deferred().group(group.id());
                    log.debug("Adding OUTGROUP action");
                }
            }
        }

        if (fwd.treatment() != null) {
            if (fwd.treatment().allInstructions().size() == 1 &&
                    fwd.treatment().allInstructions().get(0).type() == Instruction.Type.OUTPUT) {
                OutputInstruction o = (OutputInstruction) fwd.treatment().allInstructions().get(0);
                if (o.port() == PortNumber.CONTROLLER) {
                    treatmentBuilder.popVlan();
                    treatmentBuilder.punt();
                } else {
                    treatmentBuilder.add(o);
                }
            } else {
                for (Instruction ins : fwd.treatment().allInstructions()) {
                    treatmentBuilder.add(ins);
                }
            }
        }

        FlowRule.Builder ruleBuilder = DefaultFlowRule.builder()
                .fromApp(fwd.appId()).withPriority(fwd.priority())
                .forDevice(deviceId).withSelector(fwd.selector())
                .withTreatment(treatmentBuilder.build());

        if (fwd.permanent()) {
            ruleBuilder.makePermanent();
        } else {
            ruleBuilder.makeTemporary(fwd.timeout());
        }

        ruleBuilder.forTable(aclTableId);
        return Collections.singletonList(ruleBuilder.build());
    }

    private boolean isSupportedEthTypeObjective(ForwardingObjective fwd) {
        TrafficSelector selector = fwd.selector();
        EthTypeCriterion ethType = (EthTypeCriterion) selector
                .getCriterion(Criterion.Type.ETH_TYPE);
        if ((ethType == null) ||
                ((ethType.ethType().toShort() != Ethernet.TYPE_IPV4) &&
                        (ethType.ethType().toShort() != Ethernet.MPLS_UNICAST))) {
            return false;
        }
        return true;
    }

    private boolean isSupportedEthDstObjective(ForwardingObjective fwd) {
        TrafficSelector selector = fwd.selector();
        EthCriterion ethDst = (EthCriterion) selector
                .getCriterion(Criterion.Type.ETH_DST);
        VlanIdCriterion vlanId = (VlanIdCriterion) selector
                .getCriterion(Criterion.Type.VLAN_VID);
        if (ethDst == null && vlanId == null) {
            return false;
        }
        return true;
    }

    protected Collection<FlowRule> processSpecific(ForwardingObjective fwd) {
        log.debug("Processing specific fwd objective:{} in dev:{} with next:{}",
                  fwd.id(), deviceId, fwd.nextId());
        boolean isEthTypeObj = isSupportedEthTypeObjective(fwd);
        boolean isEthDstObj = isSupportedEthDstObjective(fwd);

        if (isEthTypeObj) {
            return processEthTypeSpecificObjective(fwd);
        } else if (isEthDstObj) {
            return processEthDstSpecificObjective(fwd);
        } else {
            log.warn("processSpecific: Unsupported "
                    + "forwarding objective criteria");
            fail(fwd, ObjectiveError.UNSUPPORTED);
            return Collections.emptySet();
        }
    }

    protected Collection<FlowRule>
    processEthTypeSpecificObjective(ForwardingObjective fwd) {
        TrafficSelector selector = fwd.selector();
        EthTypeCriterion ethType = (EthTypeCriterion) selector
                .getCriterion(Criterion.Type.ETH_TYPE);

        TrafficSelector.Builder filteredSelectorBuilder =
                DefaultTrafficSelector.builder();
        int forTableId = -1;
        if (ethType.ethType().toShort() == Ethernet.TYPE_IPV4) {
            filteredSelectorBuilder = filteredSelectorBuilder
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPDst(((IPCriterion) selector
                        .getCriterion(Criterion.Type.IPV4_DST))
                        .ip());
            forTableId = ipv4UnicastTableId;
            log.debug("processing IPv4 specific forwarding objective:{} in dev:{}",
                      fwd.id(), deviceId);
        } else {
            filteredSelectorBuilder = filteredSelectorBuilder
                .matchEthType(Ethernet.MPLS_UNICAST)
                .matchMplsLabel(((MplsCriterion)
                   selector.getCriterion(Criterion.Type.MPLS_LABEL)).label());
            if (selector.getCriterion(Criterion.Type.MPLS_BOS) != null) {
                filteredSelectorBuilder.matchMplsBos(((MplsBosCriterion)
                        selector.getCriterion(Type.MPLS_BOS)).mplsBos());
            }
            forTableId = mplsTableId;
            log.debug("processing MPLS specific forwarding objective:{} in dev:{}",
                    fwd.id(), deviceId);
        }

        TrafficTreatment.Builder treatmentBuilder = DefaultTrafficTreatment
                .builder();
        if (fwd.treatment() != null) {
            for (Instruction i : fwd.treatment().allInstructions()) {
                treatmentBuilder.add(i);
            }
        }

        if (fwd.nextId() != null) {
            NextGroup next = flowObjectiveStore.getNextGroup(fwd.nextId());
            if (next != null) {
                SpringOpenGroup soGroup = appKryo.deserialize(next.data());
                if (soGroup.dummy) {
                    log.debug("Adding {} flow-actions for fwd. obj. {} -> next:{} "
                            + "in dev: {}", soGroup.treatment.allInstructions().size(),
                            fwd.id(), fwd.nextId(), deviceId);
                    for (Instruction ins : soGroup.treatment.allInstructions()) {
                        treatmentBuilder.add(ins);
                    }
                } else {
                    GroupKey key = soGroup.key;
                    Group group = groupService.getGroup(deviceId, key);
                    if (group == null) {
                        log.warn("The group left!");
                        fail(fwd, ObjectiveError.GROUPMISSING);
                        return Collections.emptySet();
                    }
                    treatmentBuilder.deferred().group(group.id());
                    log.debug("Adding OUTGROUP action to group:{} for fwd. obj. {} "
                            + "for next:{} in dev: {}", group.id(), fwd.id(),
                            fwd.nextId(), deviceId);
                }
            } else {
                log.warn("processSpecific: No associated next objective object");
                fail(fwd, ObjectiveError.GROUPMISSING);
                return Collections.emptySet();
            }
        }

        TrafficSelector filteredSelector = filteredSelectorBuilder.build();
        TrafficTreatment treatment = treatmentBuilder.transition(aclTableId)
                .build();

        FlowRule.Builder ruleBuilder = DefaultFlowRule.builder()
                .fromApp(fwd.appId()).withPriority(fwd.priority())
                .forDevice(deviceId).withSelector(filteredSelector)
                .withTreatment(treatment);

        if (fwd.permanent()) {
            ruleBuilder.makePermanent();
        } else {
            ruleBuilder.makeTemporary(fwd.timeout());
        }

        ruleBuilder.forTable(forTableId);
        return Collections.singletonList(ruleBuilder.build());

    }

    protected Collection<FlowRule>
    processEthDstSpecificObjective(ForwardingObjective fwd) {
        List<FlowRule> rules = new ArrayList<>();

        // Build filtered selector
        TrafficSelector selector = fwd.selector();
        EthCriterion ethCriterion = (EthCriterion) selector
                .getCriterion(Criterion.Type.ETH_DST);
        VlanIdCriterion vlanIdCriterion = (VlanIdCriterion) selector
                .getCriterion(Criterion.Type.VLAN_VID);
        TrafficSelector.Builder filteredSelectorBuilder =
                DefaultTrafficSelector.builder();
        // Do not match MacAddress for subnet broadcast entry
        if (!ethCriterion.mac().equals(MacAddress.NONE)) {
            filteredSelectorBuilder.matchEthDst(ethCriterion.mac());
            log.debug("processing L2 forwarding objective:{} in dev:{}",
                      fwd.id(), deviceId);
        } else {
            log.debug("processing L2 Broadcast forwarding objective:{} "
                    + "in dev:{} for vlan:{}",
                      fwd.id(), deviceId, vlanIdCriterion.vlanId());
        }
        filteredSelectorBuilder.matchVlanId(vlanIdCriterion.vlanId());
        TrafficSelector filteredSelector = filteredSelectorBuilder.build();

        // Build filtered treatment
        TrafficTreatment.Builder treatmentBuilder =
                DefaultTrafficTreatment.builder();
        if (fwd.treatment() != null) {
            treatmentBuilder.deferred();
            fwd.treatment().allInstructions().forEach(treatmentBuilder::add);
        }
        if (fwd.nextId() != null) {
            NextGroup next = flowObjectiveStore.getNextGroup(fwd.nextId());
            if (next != null) {
                SpringOpenGroup soGrp = appKryo.deserialize(next.data());
                if (soGrp.dummy) {
                    log.debug("Adding {} flow-actions for fwd. obj. {} "
                            + "in dev: {}", soGrp.treatment.allInstructions().size(),
                            fwd.id(), deviceId);
                    for (Instruction ins : soGrp.treatment.allInstructions()) {
                        treatmentBuilder.deferred().add(ins);
                    }
                } else {
                    GroupKey key = soGrp.key;
                    Group group = groupService.getGroup(deviceId, key);
                    if (group == null) {
                        log.warn("The group left!");
                        fail(fwd, ObjectiveError.GROUPMISSING);
                        return Collections.emptySet();
                    }
                    treatmentBuilder.deferred().group(group.id());
                    log.debug("Adding OUTGROUP action to group:{} for fwd. obj. {} "
                            + "in dev: {}", group.id(), fwd.id(), deviceId);
                }
            }
        }
        treatmentBuilder.immediate().transition(aclTableId);
        TrafficTreatment filteredTreatment = treatmentBuilder.build();

        // Build bridging table entries
        FlowRule.Builder flowRuleBuilder = DefaultFlowRule.builder();
        flowRuleBuilder.fromApp(fwd.appId())
                .withPriority(fwd.priority())
                .forDevice(deviceId)
                .withSelector(filteredSelector)
                .withTreatment(filteredTreatment)
                .forTable(dstMacTableId);
        if (fwd.permanent()) {
            flowRuleBuilder.makePermanent();
        } else {
            flowRuleBuilder.makeTemporary(fwd.timeout());
        }
        rules.add(flowRuleBuilder.build());

        /*
        // TODO Emulate source MAC table behavior
        // Do not install source MAC table entry for subnet broadcast
        if (!ethCriterion.mac().equals(MacAddress.NONE)) {
            // Build filtered selector
            selector = fwd.selector();
            ethCriterion = (EthCriterion) selector.getCriterion(Criterion.Type.ETH_DST);
            filteredSelectorBuilder = DefaultTrafficSelector.builder();
            filteredSelectorBuilder.matchEthSrc(ethCriterion.mac());
            filteredSelector = filteredSelectorBuilder.build();

            // Build empty treatment. Apply existing instruction if match.
            treatmentBuilder = DefaultTrafficTreatment.builder();
            filteredTreatment = treatmentBuilder.build();

            // Build bridging table entries
            flowRuleBuilder = DefaultFlowRule.builder();
            flowRuleBuilder.fromApp(fwd.appId())
                    .withPriority(fwd.priority())
                    .forDevice(deviceId)
                    .withSelector(filteredSelector)
                    .withTreatment(filteredTreatment)
                    .forTable(srcMacTableId)
                    .makePermanent();
            rules.add(flowRuleBuilder.build());
        }
        */

        return rules;
    }

    /*
     * Note: CpqD switches do not handle MPLS-related operation properly
     * for a packet with VLAN tag. We pop VLAN here as a workaround.
     * Side effect: HostService learns redundant hosts with same MAC but
     * different VLAN. No known side effect on the network reachability.
     */
    protected List<FlowRule> processEthDstFilter(EthCriterion ethCriterion,
                                       VlanIdCriterion vlanIdCriterion,
                                       FilteringObjective filt,
                                       VlanId assignedVlan,
                                       ApplicationId applicationId) {
        if (vlanIdCriterion == null) {
            return processEthDstOnlyFilter(ethCriterion, applicationId, filt.priority());
        }

        //handling untagged packets via assigned VLAN
        if (vlanIdCriterion.vlanId() == VlanId.NONE) {
            vlanIdCriterion = (VlanIdCriterion) Criteria.matchVlanId(assignedVlan);
        }

        List<FlowRule> rules = new ArrayList<>();
        TrafficSelector.Builder selectorIp = DefaultTrafficSelector
                .builder();
        TrafficTreatment.Builder treatmentIp = DefaultTrafficTreatment
                .builder();
        selectorIp.matchEthDst(ethCriterion.mac());
        selectorIp.matchEthType(Ethernet.TYPE_IPV4);
        selectorIp.matchVlanId(vlanIdCriterion.vlanId());
        treatmentIp.popVlan();
        treatmentIp.transition(ipv4UnicastTableId);
        FlowRule ruleIp = DefaultFlowRule.builder().forDevice(deviceId)
                .withSelector(selectorIp.build())
                .withTreatment(treatmentIp.build())
                .withPriority(filt.priority()).fromApp(applicationId)
                .makePermanent().forTable(tmacTableId).build();
        log.debug("adding IP ETH rule for MAC: {}", ethCriterion.mac());
        rules.add(ruleIp);

        TrafficSelector.Builder selectorMpls = DefaultTrafficSelector
                .builder();
        TrafficTreatment.Builder treatmentMpls = DefaultTrafficTreatment
                .builder();
        selectorMpls.matchEthDst(ethCriterion.mac());
        selectorMpls.matchEthType(Ethernet.MPLS_UNICAST);
        selectorMpls.matchVlanId(vlanIdCriterion.vlanId());
        treatmentMpls.popVlan();
        treatmentMpls.transition(mplsTableId);
        FlowRule ruleMpls = DefaultFlowRule.builder()
                .forDevice(deviceId).withSelector(selectorMpls.build())
                .withTreatment(treatmentMpls.build())
                .withPriority(filt.priority()).fromApp(applicationId)
                .makePermanent().forTable(tmacTableId).build();
        log.debug("adding MPLS ETH rule for MAC: {}", ethCriterion.mac());
        rules.add(ruleMpls);

        return rules;
    }

    protected List<FlowRule> processEthDstOnlyFilter(EthCriterion ethCriterion,
            ApplicationId applicationId, int priority) {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        selector.matchEthType(Ethernet.TYPE_IPV4);
        selector.matchEthDst(ethCriterion.mac());
        treatment.transition(TABLE_IPV4_UNICAST);
        FlowRule rule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(priority)
                .fromApp(applicationId)
                .makePermanent()
                .forTable(TABLE_TMAC).build();
        return ImmutableList.<FlowRule>builder().add(rule).build();
    }

    protected List<FlowRule> processVlanIdFilter(VlanIdCriterion vlanIdCriterion,
                                                 FilteringObjective filt,
                                                 VlanId assignedVlan, VlanId explicitlyAssignedVlan, VlanId pushedVlan,
                                                 boolean pushVlan, boolean popVlan,
                                                 ApplicationId applicationId) {
        List<FlowRule> rules = new ArrayList<>();
        log.debug("adding rule for VLAN: {}", vlanIdCriterion.vlanId());
        TrafficSelector.Builder selector = DefaultTrafficSelector
                .builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment
                .builder();
        PortCriterion p = (PortCriterion) filt.key();
        if (vlanIdCriterion.vlanId() != VlanId.NONE) {
            selector.matchVlanId(vlanIdCriterion.vlanId());
            selector.matchInPort(p.port());
            if (popVlan) {
                // Pop outer tag
                treatment.immediate().popVlan();
            }
            if (explicitlyAssignedVlan != null && (!popVlan || !vlanIdCriterion.vlanId().equals(assignedVlan))) {
                // Modify VLAN ID on single tagged packet or modify remaining tag after popping
                // In the first case, do not set VLAN ID again to the already existing value
                treatment.immediate().setVlanId(explicitlyAssignedVlan);
            }
            if (pushVlan) {
                // Push new tag
                treatment.immediate().pushVlan().setVlanId(pushedVlan);
            }
        } else {
            selector.matchInPort(p.port());
            treatment.immediate().pushVlan().setVlanId(assignedVlan);
        }
        treatment.transition(tmacTableId);
        FlowRule rule = DefaultFlowRule.builder().forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(filt.priority()).fromApp(applicationId)
                .makePermanent().forTable(vlanTableId).build();
        rules.add(rule);

        return rules;
    }

    private void processFilter(FilteringObjective filt, boolean install,
                               ApplicationId applicationId) {
        // This driver only processes filtering criteria defined with switch
        // ports as the key
        if (filt.key().equals(Criteria.dummy())
                || filt.key().type() != Criterion.Type.IN_PORT) {
            log.warn("No key defined in filtering objective from app: {}. Not"
                    + "processing filtering objective", applicationId);
            fail(filt, ObjectiveError.UNKNOWN);
            return;
        }

        EthCriterion ethCriterion = null;
        VlanIdCriterion vlanIdCriterion = null;

        // convert filtering conditions for switch-intfs into flowrules
        FlowRuleOperations.Builder ops = FlowRuleOperations.builder();

        for (Criterion criterion : filt.conditions()) {
            if (criterion.type() == Criterion.Type.ETH_DST) {
                ethCriterion = (EthCriterion) criterion;
            } else if (criterion.type() == Criterion.Type.VLAN_VID) {
                vlanIdCriterion = (VlanIdCriterion) criterion;
            } else if (criterion.type() == Criterion.Type.IPV4_DST) {
                log.debug("driver does not process IP filtering rules as it " +
                        "sends all misses in the IP table to the controller");
            } else {
                log.warn("Driver does not currently process filtering condition"
                                 + " of type: {}", criterion.type());
                fail(filt, ObjectiveError.UNSUPPORTED);
            }
        }

        VlanId assignedVlan = null;
        VlanId modifiedVlan = null;
        VlanId pushedVlan = null;
        boolean pushVlan = false;
        boolean popVlan = false;
        if (vlanIdCriterion != null) {
            if (filt.meta() != null) {
                for (Instruction i : filt.meta().allInstructions()) {
                    if (i instanceof L2ModificationInstruction) {
                        if (((L2ModificationInstruction) i).subtype()
                                .equals(L2ModificationInstruction.L2SubType.VLAN_PUSH)) {
                            pushVlan = true;
                        } else if (((L2ModificationInstruction) i).subtype()
                                .equals(L2ModificationInstruction.L2SubType.VLAN_POP)) {
                            if (modifiedVlan != null) {
                                log.error("Pop tag is not allowed after modify VLAN operation " +
                                        "in filtering objective", deviceId);
                                fail(filt, ObjectiveError.BADPARAMS);
                                return;
                            }
                            popVlan = true;
                        }
                    }
                    if (i instanceof ModVlanIdInstruction) {
                        if (pushVlan && vlanIdCriterion.vlanId() != VlanId.NONE) {
                            // Modify VLAN should not appear after pushing a new tag
                            if (pushedVlan != null) {
                                log.error("Modify VLAN not allowed after push tag operation " +
                                        "in filtering objective", deviceId);
                                fail(filt, ObjectiveError.BADPARAMS);
                                return;
                            }
                            pushedVlan = ((ModVlanIdInstruction) i).vlanId();
                        } else if (vlanIdCriterion.vlanId() == VlanId.NONE) {
                            // For untagged packets the pushed VLAN ID will be saved in assignedVlan
                            // just to ensure the driver works as designed for the fabric use case
                            assignedVlan = ((ModVlanIdInstruction) i).vlanId();
                        } else {
                            // For tagged packets modifiedVlan will contain the modified value of existing tag
                            if (modifiedVlan != null) {
                                log.error("Driver does not allow multiple modify VLAN operations " +
                                        "in the same filtering objective", deviceId);
                                fail(filt, ObjectiveError.BADPARAMS);
                                return;
                            }
                            modifiedVlan = ((ModVlanIdInstruction) i).vlanId();
                        }
                    }
                }
            }

            // For VLAN cross-connect packets, use the configured VLAN unless there is an explicitly provided VLAN ID
            if (vlanIdCriterion.vlanId() != VlanId.NONE) {
                if (assignedVlan == null) {
                    assignedVlan = vlanIdCriterion.vlanId();
                }
            // For untagged packets, assign a VLAN ID
            } else {
                if (filt.meta() == null) {
                    log.error("Missing metadata in filtering objective required " +
                            "for vlan assignment in dev {}", deviceId);
                    fail(filt, ObjectiveError.BADPARAMS);
                    return;
                }
                if (assignedVlan == null) {
                    log.error("Driver requires an assigned vlan-id to tag incoming "
                            + "untagged packets. Not processing vlan filters on "
                            + "device {}", deviceId);
                    fail(filt, ObjectiveError.BADPARAMS);
                    return;
                }
            }
            if (pushVlan && popVlan) {
                log.error("Cannot push and pop vlan in the same filtering objective");
                fail(filt, ObjectiveError.BADPARAMS);
                return;
            }
            if (popVlan && vlanIdCriterion.vlanId() == VlanId.NONE) {
                log.error("Cannot pop vlan for untagged packets");
                fail(filt, ObjectiveError.BADPARAMS);
                return;
            }
            if ((pushVlan && pushedVlan == null) && vlanIdCriterion.vlanId() != VlanId.NONE) {
                log.error("No VLAN ID provided for push tag operation");
                fail(filt, ObjectiveError.BADPARAMS);
                return;
            }
        }

        if (ethCriterion == null) {
            log.debug("filtering objective missing dstMac, cannot program TMAC table");
        } else {
            for (FlowRule tmacRule : processEthDstFilter(ethCriterion,
                                                         vlanIdCriterion,
                                                         filt,
                                                         assignedVlan,
                                                         applicationId)) {
                log.debug("adding MAC filtering rules in TMAC table: {} for dev: {}",
                          tmacRule, deviceId);
                ops = install ? ops.add(tmacRule) : ops.remove(tmacRule);
            }
        }

        if (vlanIdCriterion == null) {
            log.debug("filtering objective missing VLAN ID criterion, "
                    + "cannot program VLAN Table");
        } else {
            for (FlowRule vlanRule : processVlanIdFilter(vlanIdCriterion,
                                                         filt,
                                                         assignedVlan, modifiedVlan, pushedVlan,
                                                         pushVlan, popVlan,
                                                         applicationId)) {
                log.debug("adding VLAN filtering rule in VLAN table: {} for dev: {}",
                          vlanRule, deviceId);
                ops = install ? ops.add(vlanRule) : ops.remove(vlanRule);
            }
        }

        // apply filtering flow rules
        flowRuleService.apply(ops.build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                pass(filt);
                log.debug("Provisioned tables in {} with fitering "
                        + "rules", deviceId);
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                fail(filt, ObjectiveError.FLOWINSTALLATIONFAILED);
                log.warn("Failed to provision tables in {} with "
                        + "fitering rules", deviceId);
            }
        }));
    }

    protected void setTableMissEntries() {
        // set all table-miss-entries
        populateTableMissEntry(vlanTableId, true, false, false, -1);
        populateTableMissEntry(tmacTableId, false, false, true, dstMacTableId);
        populateTableMissEntry(ipv4UnicastTableId, false, true, true, aclTableId);
        populateTableMissEntry(mplsTableId, false, true, true, aclTableId);
        populateTableMissEntry(dstMacTableId, false, false, true, aclTableId);
        populateTableMissEntry(aclTableId, false, false, false, -1);
    }

    protected void populateTableMissEntry(int tableToAdd,
                                          boolean toControllerNow,
                                          boolean toControllerWrite,
                                          boolean toTable, int tableToSend) {
        TrafficSelector selector = DefaultTrafficSelector.builder().build();
        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();

        if (toControllerNow) {
            tBuilder.setOutput(PortNumber.CONTROLLER);
        }

        if (toControllerWrite) {
            tBuilder.deferred().setOutput(PortNumber.CONTROLLER);
        }

        if (toTable) {
            tBuilder.transition(tableToSend);
        }

        FlowRule flow = DefaultFlowRule.builder().forDevice(deviceId)
                .withSelector(selector).withTreatment(tBuilder.build())
                .withPriority(0).fromApp(appId).makePermanent()
                .forTable(tableToAdd).build();

        flowRuleService.applyFlowRules(flow);
    }

    private void pass(Objective obj) {
        obj.context().ifPresent(context -> context.onSuccess(obj));
    }

    protected void fail(Objective obj, ObjectiveError error) {
        obj.context().ifPresent(context -> context.onError(obj, error));
    }

    private class InnerGroupListener implements GroupListener {
        @Override
        public void event(GroupEvent event) {
            if (event.type() == GroupEvent.Type.GROUP_ADDED) {
                log.trace("InnerGroupListener: Group ADDED "
                        + "event received in device {}", deviceId);
                GroupKey key = event.subject().appCookie();

                NextObjective obj = pendingGroups.getIfPresent(key);
                if (obj != null) {
                    log.debug("Group verified: dev:{} gid:{} <<->> nextId:{}",
                              deviceId, event.subject().id(), obj.id());
                    flowObjectiveStore
                            .putNextGroup(obj.id(),
                                          new SpringOpenGroup(key, null));
                    pass(obj);
                    pendingGroups.invalidate(key);
                }
            } else if (event.type() == GroupEvent.Type.GROUP_ADD_FAILED) {
                log.warn("InnerGroupListener: Group ADD "
                        + "failed event received in device {}", deviceId);
            }
        }
    }

    private void verifyPendingGroupLater() {
        GROUP_CHECKER.schedule(new GroupChecker(), CHECK_DELAY, TimeUnit.MILLISECONDS);
    }

    private class GroupChecker implements Runnable {

        @Override
        public void run() {
            Set<GroupKey> keys = pendingGroups
                    .asMap()
                    .keySet()
                    .stream()
                    .filter(key -> groupService.getGroup(deviceId, key) != null)
                    .collect(Collectors.toSet());

            keys.forEach(key -> {
                NextObjective obj = pendingGroups
                        .getIfPresent(key);
                if (obj == null) {
                    return;
                }
                log.debug("Group verified: dev:{} gid:{} <<->> nextId:{}",
                        deviceId,
                        groupService.getGroup(deviceId, key).id(),
                        obj.id());
                pass(obj);
                pendingGroups.invalidate(key);
                flowObjectiveStore.putNextGroup(
                        obj.id(),
                        new SpringOpenGroup(key, null));
            });

            if (!pendingGroups.asMap().isEmpty()) {
                // Periodically execute only if entry remains in pendingGroups.
                // Iterating pendingGroups trigger cleanUp and expiration,
                // which will eventually empty the pendingGroups.
                verifyPendingGroupLater();
            }
        }
    }

    /**
     * SpringOpenGroup can either serve as storage for a GroupKey which can be
     * used to fetch the group from the Group Service, or it can be serve as storage
     * for Traffic Treatments which can be used as flow actions. In the latter
     * case, we refer to this as a dummy group.
     *
     */
    protected class SpringOpenGroup implements NextGroup {
        private final boolean dummy;
        private final GroupKey key;
        private final TrafficTreatment treatment;

        /**
         * Storage for a GroupKey or a TrafficTreatment. One of the params
         * to this constructor must be null.
         * @param key represents a GroupKey
         * @param treatment represents flow actions in a dummy group
         */
        public SpringOpenGroup(GroupKey key, TrafficTreatment treatment) {
            if (key == null) {
                this.key = new DefaultGroupKey(new byte[]{0});
                this.treatment = treatment;
                this.dummy = true;
            } else {
                this.key = key;
                this.treatment = DefaultTrafficTreatment.builder().build();
                this.dummy = false;
            }
        }

        public GroupKey key() {
            return key;
        }

        public boolean dummy() {
            return dummy;
        }

        public TrafficTreatment treatment() {
            return treatment;
        }

        @Override
        public byte[] data() {
            return appKryo.serialize(this);
        }

    }

    @Override
    public List<String> getNextMappings(NextGroup nextGroup) {
        // TODO Implementation deferred to vendor
        return null;
    }
}
