/*
 * Copyright 2015 Open Networking Laboratory
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

import static org.onlab.util.Tools.groupedThreads;
import static org.slf4j.LoggerFactory.getLogger;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalNotification;

import org.onlab.osgi.ServiceDirectory;
import org.onlab.packet.Ethernet;
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
import org.onosproject.net.flow.criteria.EthCriterion;
import org.onosproject.net.flow.criteria.EthTypeCriterion;
import org.onosproject.net.flow.criteria.IPCriterion;
import org.onosproject.net.flow.criteria.MplsCriterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.criteria.VlanIdCriterion;
import org.onosproject.net.flow.instructions.Instruction;
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
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Driver for OpenVSwitch pipeline with group table supported.
 */
public class OVSAdvanced extends AbstractHandlerBehaviour
        implements Pipeliner {

    // Default table ID - compatible with CpqD switch
    private static final int TABLE_VLAN = 0;
    private static final int TABLE_TMAC = 1;
    private static final int TABLE_IPV4_UNICAST = 2;
    private static final int TABLE_MPLS = 3;
    private static final int TABLE_ACL = 5;

    /**
     * Set the default values. These variables will get overwritten based on the
     * switch vendor type
     */
    protected int vlanTableId = TABLE_VLAN;
    protected int tmacTableId = TABLE_TMAC;
    protected int ipv4UnicastTableId = TABLE_IPV4_UNICAST;
    protected int mplsTableId = TABLE_MPLS;
    protected int aclTableId = TABLE_ACL;

    protected final Logger log = getLogger(getClass());

    private ServiceDirectory serviceDirectory;
    private FlowRuleService flowRuleService;
    private CoreService coreService;
    protected GroupService groupService;
    protected FlowObjectiveStore flowObjectiveStore;
    protected DeviceId deviceId;
    private ApplicationId appId;

    private Cache<GroupKey, NextObjective> pendingGroups;

    private ScheduledExecutorService groupChecker = Executors
            .newScheduledThreadPool(2,
                                    groupedThreads("onos/pipeliner",
                                                   "ovs-advanced-%d"));
    protected KryoNamespace appKryo = new KryoNamespace.Builder()
            .register(GroupKey.class).register(DefaultGroupKey.class)
            .register(SegmentRoutingGroup.class).register(byte[].class).build();

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

        groupChecker.scheduleAtFixedRate(new GroupChecker(), 0, 500,
                                         TimeUnit.MILLISECONDS);

        coreService = serviceDirectory.get(CoreService.class);
        flowRuleService = serviceDirectory.get(FlowRuleService.class);
        groupService = serviceDirectory.get(GroupService.class);
        flowObjectiveStore = context.store();

        groupService.addListener(new InnerGroupListener());

        appId = coreService
                .registerApplication("org.onosproject.driver.OVSAdvanced");

        setTableMissEntries();
        log.info("OVS Advanced driver initialized");
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
            rules.stream().filter(rule -> rule != null)
                    .forEach(flowBuilder::add);
            break;
        case REMOVE:
            rules.stream().filter(rule -> rule != null)
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
                        log.debug("Provisioned tables in {} with "
                                + "forwarding rules for segment "
                                + "router", deviceId);
                    }

                    @Override
                    public void onError(FlowRuleOperations ops) {
                        fail(fwd, ObjectiveError.FLOWINSTALLATIONFAILED);
                        log.warn("Failed to provision tables in {} with "
                                + "forwarding rules for segment router",
                                deviceId);
                    }
                }));

    }

    @Override
    public void next(NextObjective nextObjective) {

        log.debug("Processing NextObjective id{} op{}", nextObjective.id(),
                  nextObjective.op());
        if (nextObjective.op() == Objective.Operation.REMOVE) {
            if (nextObjective.next().isEmpty()) {
                removeGroup(nextObjective);
            } else {
                removeBucketFromGroup(nextObjective);
            }
        } else if (nextObjective.op() == Objective.Operation.ADD) {
            NextGroup nextGroup = flowObjectiveStore.getNextGroup(nextObjective.id());
            if (nextGroup != null) {
                addBucketToGroup(nextObjective);
            } else {
                addGroup(nextObjective);
            }
        } else {
            log.warn("Unsupported operation {}", nextObjective.op());
        }

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
        switch (nextObjective.type()) {
            case SIMPLE:
                log.debug("processing SIMPLE next objective");
                Collection<TrafficTreatment> treatments = nextObjective.next();
                if (treatments.size() == 1) {
                    TrafficTreatment treatment = treatments.iterator().next();
                    GroupBucket bucket = DefaultGroupBucket
                            .createIndirectGroupBucket(treatment);
                    final GroupKey key = new DefaultGroupKey(
                            appKryo.serialize(nextObjective
                                    .id()));
                    GroupDescription groupDescription = new DefaultGroupDescription(
                            deviceId,
                            GroupDescription.Type.INDIRECT,
                            new GroupBuckets(
                                    Collections.singletonList(bucket)),
                            key,
                            null,
                            nextObjective.appId());
                    log.debug("Creating SIMPLE group for next objective id {}",
                              nextObjective.id());
                    groupService.addGroup(groupDescription);
                    pendingGroups.put(key, nextObjective);
                }
                break;
            case HASHED:
                log.debug("processing HASHED next objective");
                List<GroupBucket> buckets = nextObjective
                        .next()
                        .stream()
                        .map((treatment) -> DefaultGroupBucket
                                .createSelectGroupBucket(treatment))
                        .collect(Collectors.toList());
                if (!buckets.isEmpty()) {
                    final GroupKey key = new DefaultGroupKey(
                            appKryo.serialize(nextObjective
                                    .id()));
                    GroupDescription groupDescription = new DefaultGroupDescription(
                            deviceId,
                            GroupDescription.Type.SELECT,
                            new GroupBuckets(buckets),
                            key,
                            null,
                            nextObjective.appId());
                    log.debug("Creating HASHED group for next objective id {}",
                              nextObjective.id());
                    groupService.addGroup(groupDescription);
                    pendingGroups.put(key, nextObjective);
                }
                break;
            case BROADCAST:
            case FAILOVER:
                log.debug("BROADCAST and FAILOVER next objectives not supported");
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
        log.debug("Processing versatile forwarding objective");
        TrafficSelector selector = fwd.selector();

        EthTypeCriterion ethType =
                (EthTypeCriterion) selector.getCriterion(Criterion.Type.ETH_TYPE);
        if (ethType == null) {
            log.error("Versatile forwarding objective must include ethType");
            fail(fwd, ObjectiveError.UNKNOWN);
            return Collections.emptySet();
        }

        TrafficTreatment.Builder treatmentBuilder = DefaultTrafficTreatment
                .builder();
        treatmentBuilder.wipeDeferred();

        if (fwd.nextId() != null) {
            NextGroup next = flowObjectiveStore.getNextGroup(fwd.nextId());

            if (next != null) {
                GroupKey key = appKryo.deserialize(next.data());

                Group group = groupService.getGroup(deviceId, key);

                if (group == null) {
                    log.warn("The group left!");
                    fail(fwd, ObjectiveError.GROUPMISSING);
                    return Collections.emptySet();
                }
                treatmentBuilder.deferred().group(group.id());
                log.debug("Adding OUTGROUP action");
            }
        } else {
            log.warn("VERSATILE forwarding objective need next objective ID.");
            return Collections.emptySet();
        }

        TrafficTreatment treatment = treatmentBuilder.build();

        FlowRule.Builder ruleBuilder = DefaultFlowRule.builder()
                .fromApp(fwd.appId()).withPriority(fwd.priority())
                .forDevice(deviceId).withSelector(fwd.selector())
                .withTreatment(treatment);

        if (fwd.permanent()) {
            ruleBuilder.makePermanent();
        } else {
            ruleBuilder.makeTemporary(fwd.timeout());
        }

        return Collections.singletonList(ruleBuilder.build());
    }

    protected Collection<FlowRule> processSpecific(ForwardingObjective fwd) {
        log.debug("Processing specific");
        TrafficSelector selector = fwd.selector();
        EthTypeCriterion ethType = (EthTypeCriterion) selector
                .getCriterion(Criterion.Type.ETH_TYPE);
        if ((ethType == null) ||
                (ethType.ethType().toShort() != Ethernet.TYPE_IPV4) &&
                (ethType.ethType().toShort() != Ethernet.MPLS_UNICAST)) {
            log.warn("processSpecific: Unsupported "
                    + "forwarding objective criteraia");
            fail(fwd, ObjectiveError.UNSUPPORTED);
            return Collections.emptySet();
        }

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
            log.debug("processing IPv4 specific forwarding objective");
        } else {
            filteredSelectorBuilder = filteredSelectorBuilder
                .matchEthType(Ethernet.MPLS_UNICAST)
                .matchMplsLabel(((MplsCriterion)
                   selector.getCriterion(Criterion.Type.MPLS_LABEL)).label());
            forTableId = mplsTableId;
            log.debug("processing MPLS specific forwarding objective");
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
                GroupKey key = appKryo.deserialize(next.data());

                Group group = groupService.getGroup(deviceId, key);

                if (group == null) {
                    log.warn("The group left!");
                    fail(fwd, ObjectiveError.GROUPMISSING);
                    return Collections.emptySet();
                }
                treatmentBuilder.deferred().group(group.id());
                log.debug("Adding OUTGROUP action");
            } else {
                log.warn("processSpecific: No associated next objective object");
                fail(fwd, ObjectiveError.GROUPMISSING);
                return Collections.emptySet();
            }
        }

        TrafficSelector filteredSelector = filteredSelectorBuilder.build();
        TrafficTreatment treatment = treatmentBuilder
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

    protected List<FlowRule> processEthDstFilter(Criterion c,
                                       FilteringObjective filt,
                                       ApplicationId applicationId) {
        List<FlowRule> rules = new ArrayList<FlowRule>();
        EthCriterion e = (EthCriterion) c;
        TrafficSelector.Builder selectorIp = DefaultTrafficSelector
                .builder();
        TrafficTreatment.Builder treatmentIp = DefaultTrafficTreatment
                .builder();
        selectorIp.matchEthDst(e.mac());
        selectorIp.matchEthType(Ethernet.TYPE_IPV4);
        treatmentIp.transition(ipv4UnicastTableId);
        FlowRule ruleIp = DefaultFlowRule.builder().forDevice(deviceId)
                .withSelector(selectorIp.build())
                .withTreatment(treatmentIp.build())
                .withPriority(filt.priority()).fromApp(applicationId)
                .makePermanent().forTable(tmacTableId).build();
        log.debug("adding IP ETH rule for MAC: {}", e.mac());
        rules.add(ruleIp);

        TrafficSelector.Builder selectorMpls = DefaultTrafficSelector
                .builder();
        TrafficTreatment.Builder treatmentMpls = DefaultTrafficTreatment
                .builder();
        selectorMpls.matchEthDst(e.mac());
        selectorMpls.matchEthType(Ethernet.MPLS_UNICAST);
        treatmentMpls.transition(mplsTableId);
        FlowRule ruleMpls = DefaultFlowRule.builder()
                .forDevice(deviceId).withSelector(selectorMpls.build())
                .withTreatment(treatmentMpls.build())
                .withPriority(filt.priority()).fromApp(applicationId)
                .makePermanent().forTable(tmacTableId).build();
        log.debug("adding MPLS ETH rule for MAC: {}", e.mac());
        rules.add(ruleMpls);

        return rules;
    }

    protected List<FlowRule> processVlanIdFilter(Criterion c,
                                                 FilteringObjective filt,
                                                 ApplicationId applicationId) {
        List<FlowRule> rules = new ArrayList<FlowRule>();
        VlanIdCriterion v = (VlanIdCriterion) c;
        log.debug("adding rule for VLAN: {}", v.vlanId());
        TrafficSelector.Builder selector = DefaultTrafficSelector
                .builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment
                .builder();
        PortCriterion p = (PortCriterion) filt.key();
        if (v.vlanId() != VlanId.NONE) {
            selector.matchVlanId(v.vlanId());
            selector.matchInPort(p.port());
            treatment.deferred().popVlan();
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
        // convert filtering conditions for switch-intfs into flowrules
        FlowRuleOperations.Builder ops = FlowRuleOperations.builder();
        for (Criterion c : filt.conditions()) {
            if (c.type() == Criterion.Type.ETH_DST) {
                for (FlowRule rule : processEthDstFilter(c,
                                                         filt,
                                                         applicationId)) {
                    ops = install ? ops.add(rule) : ops.remove(rule);
                }
            } else if (c.type() == Criterion.Type.VLAN_VID) {
                for (FlowRule rule : processVlanIdFilter(c,
                                                         filt,
                                                         applicationId)) {
                    ops = install ? ops.add(rule) : ops.remove(rule);
                }
            } else if (c.type() == Criterion.Type.IPV4_DST) {
                IPCriterion ip = (IPCriterion) c;
                log.debug("adding rule for IP: {}", ip.ip());
                TrafficSelector.Builder selector = DefaultTrafficSelector
                        .builder();
                TrafficTreatment.Builder treatment = DefaultTrafficTreatment
                        .builder();
                selector.matchEthType(Ethernet.TYPE_IPV4);
                selector.matchIPDst(ip.ip());
                FlowRule rule = DefaultFlowRule.builder().forDevice(deviceId)
                        .withSelector(selector.build())
                        .withTreatment(treatment.build())
                        .withPriority(filt.priority()).fromApp(applicationId)
                        .makePermanent().forTable(ipv4UnicastTableId).build();
                ops = install ? ops.add(rule) : ops.remove(rule);
            } else {
                log.warn("Driver does not currently process filtering condition"
                                 + " of type: {}", c.type());
                fail(filt, ObjectiveError.UNSUPPORTED);
            }
        }
        // apply filtering flow rules
        flowRuleService.apply(ops.build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                pass(filt);
                log.debug("Provisioned tables in {} with fitering "
                        + "rules for segment router", deviceId);
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                fail(filt, ObjectiveError.FLOWINSTALLATIONFAILED);
                log.warn("Failed to provision tables in {} with "
                        + "fitering rules for segment router", deviceId);
            }
        }));
    }

    protected void setTableMissEntries() {
        // set all table-miss-entries
        populateTableMissEntry(vlanTableId, true, false, false, -1);
        populateTableMissEntry(tmacTableId, true, false, false, -1);
        populateTableMissEntry(ipv4UnicastTableId, true, false, false, -1);
        populateTableMissEntry(mplsTableId, true, false, false, -1);
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
        if (obj.context().isPresent()) {
            obj.context().get().onSuccess(obj);
        }
    }

    protected void fail(Objective obj, ObjectiveError error) {
        if (obj.context().isPresent()) {
            obj.context().get().onError(obj, error);
        }
    }

    private class InnerGroupListener implements GroupListener {
        @Override
        public void event(GroupEvent event) {
            if (event.type() == GroupEvent.Type.GROUP_ADDED) {
                log.debug("InnerGroupListener: Group ADDED "
                        + "event received in device {}", deviceId);
                GroupKey key = event.subject().appCookie();

                NextObjective obj = pendingGroups.getIfPresent(key);
                if (obj != null) {
                    flowObjectiveStore
                            .putNextGroup(obj.id(),
                                          new SegmentRoutingGroup(key));
                    pass(obj);
                    pendingGroups.invalidate(key);
                }
            } else if (event.type() == GroupEvent.Type.GROUP_ADD_FAILED) {
                log.warn("InnerGroupListener: Group ADD "
                        + "failed event received in device {}", deviceId);
            }
        }
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

            keys.stream()
                    .forEach(key -> {
                                 NextObjective obj = pendingGroups
                                         .getIfPresent(key);
                                 if (obj == null) {
                                     return;
                                 }
                                 pass(obj);
                                 pendingGroups.invalidate(key);
                                 flowObjectiveStore.putNextGroup(obj.id(),
                                                                 new SegmentRoutingGroup(
                                                                                         key));
                             });
        }
    }

    private class SegmentRoutingGroup implements NextGroup {

        private final GroupKey key;

        public SegmentRoutingGroup(GroupKey key) {
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
