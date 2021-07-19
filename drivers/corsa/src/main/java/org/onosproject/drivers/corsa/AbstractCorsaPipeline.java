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

package org.onosproject.drivers.corsa;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalNotification;
import com.google.common.collect.ImmutableSet;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.packet.Ethernet;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
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
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.EthCriterion;
import org.onosproject.net.flow.criteria.EthTypeCriterion;
import org.onosproject.net.flow.criteria.IPCriterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.criteria.VlanIdCriterion;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
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
import org.onosproject.net.meter.MeterService;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.net.flow.FlowRule.Builder;
import static org.onosproject.net.flowobjective.Objective.Operation.ADD;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Abstraction of the Corsa pipeline handler.
 */
public abstract class AbstractCorsaPipeline extends AbstractHandlerBehaviour implements Pipeliner {


    private final Logger log = getLogger(getClass());

    private ServiceDirectory serviceDirectory;
    protected FlowRuleService flowRuleService;
    private CoreService coreService;
    protected GroupService groupService;
    protected MeterService meterService;
    protected FlowObjectiveStore flowObjectiveStore;
    protected DeviceId deviceId;
    protected ApplicationId appId;
    protected DeviceService deviceService;

    protected KryoNamespace appKryo = new KryoNamespace.Builder()
            .register(GroupKey.class)
            .register(DefaultGroupKey.class)
            .register(CorsaGroup.class)
            .register(byte[].class)
            .build("AbstractCorsaPipeline");

    private Cache<GroupKey, NextObjective> pendingGroups;
    protected Cache<Integer, NextObjective> pendingNext;


    private ScheduledExecutorService groupChecker =
            Executors.newScheduledThreadPool(2, groupedThreads("onos/pipeliner",
                    "ovs-corsa-%d", log));

    protected static final int CONTROLLER_PRIORITY = 255;
    protected static final int DROP_PRIORITY = 0;
    protected static final int HIGHEST_PRIORITY = 0xffff;
    protected static final String APPID = "org.onosproject.drivers.corsa.CorsaPipeline";

    @Override
    public void init(DeviceId deviceId, PipelinerContext context) {
        this.serviceDirectory = context.directory();
        this.deviceId = deviceId;

        pendingGroups = CacheBuilder.newBuilder()
                .expireAfterWrite(20, TimeUnit.SECONDS)
                .removalListener((RemovalNotification<GroupKey, NextObjective> notification) -> {
                    if (notification.getCause() == RemovalCause.EXPIRED) {
                        fail(notification.getValue(), ObjectiveError.GROUPINSTALLATIONFAILED);
                    }
                }).build();

        pendingNext = CacheBuilder.newBuilder()
                .expireAfterWrite(20, TimeUnit.SECONDS)
                .removalListener((RemovalNotification<Integer, NextObjective> notification) -> {
                    if (notification.getCause() == RemovalCause.EXPIRED) {
                        notification.getValue().context()
                                .ifPresent(c -> c.onError(notification.getValue(),
                                                          ObjectiveError.FLOWINSTALLATIONFAILED));
                    }
                }).build();

        groupChecker.scheduleAtFixedRate(new GroupChecker(), 0, 500, TimeUnit.MILLISECONDS);

        coreService = serviceDirectory.get(CoreService.class);
        flowRuleService = serviceDirectory.get(FlowRuleService.class);
        groupService = serviceDirectory.get(GroupService.class);
        meterService = serviceDirectory.get(MeterService.class);
        deviceService = serviceDirectory.get(DeviceService.class);
        flowObjectiveStore = context.store();

        groupService.addListener(new InnerGroupListener());

        appId = coreService.registerApplication(APPID);

        initializePipeline();
    }

    protected abstract void initializePipeline();

    protected void pass(Objective obj) {
        obj.context().ifPresent(context -> context.onSuccess(obj));
    }

    protected void fail(Objective obj, ObjectiveError error) {
        obj.context().ifPresent(context -> context.onError(obj, error));
    }

    private class GroupChecker implements Runnable {

        @Override
        public void run() {
            Set<GroupKey> keys = pendingGroups.asMap().keySet().stream()
                    .filter(key -> groupService.getGroup(deviceId, key) != null)
                    .collect(Collectors.toSet());

            keys.forEach(key -> {
                NextObjective obj = pendingGroups.getIfPresent(key);
                if (obj == null) {
                    return;
                }
                pass(obj);
                pendingGroups.invalidate(key);
                log.info("Heard back from group service for group {}. "
                        + "Applying pending forwarding objectives", obj.id());
                flowObjectiveStore.putNextGroup(obj.id(), new CorsaGroup(key));
            });
        }
    }

    private class CorsaGroup implements NextGroup {

        private final GroupKey key;

        public CorsaGroup(GroupKey key) {
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

    @Override
    public List<String> getNextMappings(NextGroup nextGroup) {
        //TODO: to be implemented
        return Collections.emptyList();
    }

    private class InnerGroupListener implements GroupListener {
        @Override
        public void event(GroupEvent event) {
            if (event.type() == GroupEvent.Type.GROUP_ADDED) {
                GroupKey key = event.subject().appCookie();

                NextObjective obj = pendingGroups.getIfPresent(key);
                if (obj != null) {
                    flowObjectiveStore.putNextGroup(obj.id(), new CorsaGroup(key));
                    pass(obj);
                    pendingGroups.invalidate(key);
                }
            }
        }
    }


    @Override
    public void filter(FilteringObjective filteringObjective) {
        if (filteringObjective.type() == FilteringObjective.Type.PERMIT) {
            processFilter(filteringObjective,
                    filteringObjective.op() == ADD,
                    filteringObjective.appId());
        } else {
            fail(filteringObjective, ObjectiveError.UNSUPPORTED);
        }
    }

    private void processFilter(FilteringObjective filt, boolean install,
                               ApplicationId applicationId) {
        // This driver only processes filtering criteria defined with switch
        // ports as the key
        PortCriterion port;
        if (!filt.key().equals(Criteria.dummy()) &&
                filt.key().type() == Criterion.Type.IN_PORT) {
            port = (PortCriterion) filt.key();
        } else {
            log.warn("No key defined in filtering objective from app: {}. Not"
                    + "processing filtering objective", applicationId);
            fail(filt, ObjectiveError.UNKNOWN);
            return;
        }
        // convert filtering conditions for switch-intfs into flowrules
        FlowRuleOperations.Builder ops = FlowRuleOperations.builder();
        for (Criterion c : filt.conditions()) {
            if (c.type() == Criterion.Type.ETH_DST) {
                EthCriterion eth = (EthCriterion) c;
                FlowRule.Builder rule = processEthFiler(filt, eth, port);
                rule.forDevice(deviceId)
                        .fromApp(applicationId);
                ops = install ? ops.add(rule.build()) : ops.remove(rule.build());

            } else if (c.type() == Criterion.Type.VLAN_VID) {
                VlanIdCriterion vlan = (VlanIdCriterion) c;
                FlowRule.Builder rule = processVlanFiler(filt, vlan, port);
                rule.forDevice(deviceId)
                        .fromApp(applicationId);
                ops = install ? ops.add(rule.build()) : ops.remove(rule.build());

            } else if (c.type() == Criterion.Type.IPV4_DST) {
                IPCriterion ip = (IPCriterion) c;
                FlowRule.Builder rule = processIpFilter(filt, ip, port);
                rule.forDevice(deviceId)
                        .fromApp(applicationId);
                ops = install ? ops.add(rule.build()) : ops.remove(rule.build());

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
                log.info("Applied filtering rules");
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                fail(filt, ObjectiveError.FLOWINSTALLATIONFAILED);
                log.info("Failed to apply filtering rules");
            }
        }));
    }

    protected abstract Builder processEthFiler(FilteringObjective filt,
                                               EthCriterion eth, PortCriterion port);

    protected abstract Builder processVlanFiler(FilteringObjective filt,
                                                VlanIdCriterion vlan, PortCriterion port);

    protected abstract Builder processIpFilter(FilteringObjective filt,
                                               IPCriterion ip, PortCriterion port);


    @Override
    public void forward(ForwardingObjective fwd) {

        Collection<FlowRule> rules;
        FlowRuleOperations.Builder flowBuilder = FlowRuleOperations.builder();

        rules = processForward(fwd);
        switch (fwd.op()) {
            case ADD:
                rules.stream()
                        .filter(Objects::nonNull)
                        .forEach(flowBuilder::add);
                break;
            case REMOVE:
                rules.stream()
                        .filter(Objects::nonNull)
                        .forEach(flowBuilder::remove);
                break;
            default:
                fail(fwd, ObjectiveError.UNKNOWN);
                log.warn("Unknown forwarding type {}", fwd.op());
        }

        flowRuleService.apply(flowBuilder.build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                pass(fwd);
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                fail(fwd, ObjectiveError.FLOWINSTALLATIONFAILED);
            }
        }));

    }

    private Collection<FlowRule> processForward(ForwardingObjective fwd) {
        switch (fwd.flag()) {
            case SPECIFIC:
                return processSpecific(fwd);
            case VERSATILE:
                fwd = preProcessVersatile(fwd);
                return processVersatile(fwd);
            default:
                fail(fwd, ObjectiveError.UNKNOWN);
                log.warn("Unknown forwarding flag {}", fwd.flag());
        }
        return ImmutableSet.of();
    }

    private Collection<FlowRule> processSpecific(ForwardingObjective fwd) {
        log.debug("Processing specific forwarding objective");
        TrafficSelector selector = fwd.selector();
        EthTypeCriterion ethTypeCriterion =
                (EthTypeCriterion) selector.getCriterion(Criterion.Type.ETH_TYPE);
        VlanIdCriterion vlanIdCriterion =
                (VlanIdCriterion) selector.getCriterion(Criterion.Type.VLAN_VID);
        if (ethTypeCriterion != null) {
            short et = ethTypeCriterion.ethType().toShort();
            if (et == Ethernet.TYPE_IPV4) {
                return processSpecificRoute(fwd);
            } else if (et == Ethernet.TYPE_VLAN) {
                /* The ForwardingObjective must specify VLAN ethtype in order to use the Transit Circuit */
                return processSpecificSwitch(fwd);
            }
        } else if (vlanIdCriterion != null) {
            return processSpecificSwitch(fwd);
        }

        fail(fwd, ObjectiveError.UNSUPPORTED);
        return ImmutableSet.of();
    }

    protected Collection<FlowRule> processSpecificSwitch(ForwardingObjective fwd) {
        /* Not supported by until CorsaPipelineV3 */
        log.warn("Vlan switching not supported in ovs-corsa driver");
        fail(fwd, ObjectiveError.UNSUPPORTED);
        return ImmutableSet.of();
    }

    private ForwardingObjective preProcessVersatile(ForwardingObjective fwd) {
        // The Corsa devices don't support the clear deferred actions
        // so for now we have to filter this instruction for the fwd
        // objectives sent by the Packet Manager before to create the
        // flow rule
        if (fwd.treatment().clearedDeferred()) {
            // First we create a new treatment without the unsupported action
            TrafficTreatment.Builder noClearTreatment = DefaultTrafficTreatment.builder();
            fwd.treatment().allInstructions().forEach(noClearTreatment::add);
            // Then we create a new forwarding objective without the unsupported action
            ForwardingObjective.Builder noClearFwd = DefaultForwardingObjective.builder(fwd);
            noClearFwd.withTreatment(noClearTreatment.build());
            // According to the operation we substitute fwd with the correct objective
            switch (fwd.op()) {
                case ADD:
                    fwd = noClearFwd.add(fwd.context().orElse(null));
                    break;
                case REMOVE:
                    fwd = noClearFwd.remove(fwd.context().orElse(null));
                    break;
                default:
                    log.warn("Unknown operation {}", fwd.op());
            }
        }
        return fwd;
    }

    private Collection<FlowRule> processVersatile(ForwardingObjective fwd) {
        log.debug("Processing vesatile forwarding objective");
        TrafficSelector selector = fwd.selector();

        EthTypeCriterion ethType =
                (EthTypeCriterion) selector.getCriterion(Criterion.Type.ETH_TYPE);
        if (ethType == null) {
            log.error("Versatile forwarding objective must include ethType");
            fail(fwd, ObjectiveError.UNKNOWN);
            return ImmutableSet.of();
        }
        Builder rule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(fwd.selector())
                .withTreatment(fwd.treatment())
                .withPriority(fwd.priority())
                .fromApp(fwd.appId())
                .makePermanent();
        if (ethType.ethType().toShort() == Ethernet.TYPE_ARP) {
            return processArpTraffic(fwd, rule);
        } else if (ethType.ethType().toShort() == Ethernet.TYPE_LLDP ||
                ethType.ethType().toShort() == Ethernet.TYPE_BSN) {
            return processLinkDiscovery(fwd, rule);
        } else if (ethType.ethType().toShort() == Ethernet.TYPE_IPV4) {
            return processIpTraffic(fwd, rule);
        }
        log.warn("Driver does not support given versatile forwarding objective");
        fail(fwd, ObjectiveError.UNSUPPORTED);
        return ImmutableSet.of();
    }

    protected abstract Collection<FlowRule> processArpTraffic(ForwardingObjective fwd, Builder rule);

    protected abstract Collection<FlowRule> processLinkDiscovery(ForwardingObjective fwd, Builder rule);

    protected abstract Collection<FlowRule> processIpTraffic(ForwardingObjective fwd, Builder rule);

    private Collection<FlowRule> processSpecificRoute(ForwardingObjective fwd) {
        TrafficSelector filteredSelector =
                DefaultTrafficSelector.builder()
                        .matchEthType(Ethernet.TYPE_IPV4)
                        .matchIPDst(
                                ((IPCriterion) fwd.selector().getCriterion(Criterion.Type.IPV4_DST)).ip())
                        .build();

        TrafficTreatment.Builder tb = processSpecificRoutingTreatment();

        if (fwd.nextId() != null) {
            NextGroup next = flowObjectiveStore.getNextGroup(fwd.nextId());
            GroupKey key = appKryo.deserialize(next.data());
            Group group = groupService.getGroup(deviceId, key);
            if (group == null) {
                log.warn("The group left!");
                fail(fwd, ObjectiveError.GROUPMISSING);
                return ImmutableSet.of();
            }
            tb.group(group.id());
        } else {
            log.error("Missing NextObjective ID for ForwardingObjective {}", fwd.id());
            fail(fwd, ObjectiveError.BADPARAMS);
            return ImmutableSet.of();
        }
        Builder ruleBuilder = DefaultFlowRule.builder()
                .fromApp(fwd.appId())
                .withPriority(fwd.priority())
                .forDevice(deviceId)
                .withSelector(filteredSelector)
                .withTreatment(tb.build());

        ruleBuilder = processSpecificRoutingRule(ruleBuilder);

        if (fwd.permanent()) {
            ruleBuilder.makePermanent();
        } else {
            ruleBuilder.makeTemporary(fwd.timeout());
        }
        return Collections.singletonList(ruleBuilder.build());
    }

    //Hook for modifying Route traffic treatment
    protected TrafficTreatment.Builder processSpecificRoutingTreatment() {
        return DefaultTrafficTreatment.builder();
    }

    //Hook for modifying Route flow rule
    protected abstract Builder processSpecificRoutingRule(Builder rb);

    protected enum CorsaTrafficTreatmentType {
        /**
         * If the treatment has to be handled as group.
         */
        GROUP,
        /**
         * If the treatment has to be handled as simple set of actions.
         */
        ACTIONS
    }

    /**
     * Helper class to encapsulate both traffic treatment and
     * type of treatment.
     */
    protected class CorsaTrafficTreatment {

        private CorsaTrafficTreatmentType type;
        private TrafficTreatment trafficTreatment;

        public CorsaTrafficTreatment(CorsaTrafficTreatmentType treatmentType, TrafficTreatment trafficTreatment) {
            this.type = treatmentType;
            this.trafficTreatment = trafficTreatment;
        }

        public CorsaTrafficTreatmentType type() {
            return type;
        }

        public TrafficTreatment treatment() {
            return trafficTreatment;
        }

    }

    @Override
    public void next(NextObjective nextObjective) {
        switch (nextObjective.type()) {
            case SIMPLE:
                Collection<TrafficTreatment> treatments = nextObjective.next();
                if (treatments.size() == 1) {
                    TrafficTreatment treatment = treatments.iterator().next();
                    CorsaTrafficTreatment corsaTreatment = processNextTreatment(treatment);
                    final GroupKey key = new DefaultGroupKey(appKryo.serialize(nextObjective.id()));
                    if (corsaTreatment.type() == CorsaTrafficTreatmentType.GROUP) {
                        GroupBucket bucket = DefaultGroupBucket.createIndirectGroupBucket(corsaTreatment.treatment());
                        GroupBuckets buckets = new GroupBuckets(Collections.singletonList(bucket));
                        // group id == null, let group service determine group id
                        GroupDescription groupDescription = new DefaultGroupDescription(deviceId,
                                                                                        GroupDescription.Type.INDIRECT,
                                                                                        buckets,
                                                                                        key,
                                                                                        null,
                                                                                        nextObjective.appId());
                        groupService.addGroup(groupDescription);
                        pendingGroups.put(key, nextObjective);
                    } else if (corsaTreatment.type() == CorsaTrafficTreatmentType.ACTIONS) {
                        pendingNext.put(nextObjective.id(), nextObjective);
                        flowObjectiveStore.putNextGroup(nextObjective.id(), new CorsaGroup(key));
                        nextObjective.context().ifPresent(context -> context.onSuccess(nextObjective));
                    }
                }
                break;
            case HASHED:
            case BROADCAST:
            case FAILOVER:
                fail(nextObjective, ObjectiveError.UNSUPPORTED);
                log.warn("Unsupported next objective type {}", nextObjective.type());
                break;
            default:
                fail(nextObjective, ObjectiveError.UNKNOWN);
                log.warn("Unknown next objective type {}", nextObjective.type());
        }

    }

    //Hook for altering the NextObjective treatment
    protected CorsaTrafficTreatment processNextTreatment(TrafficTreatment treatment) {
        return new CorsaTrafficTreatment(CorsaTrafficTreatmentType.GROUP, treatment);
    }

    //Init helper: Table Miss = Drop
    protected void processTableMissDrop(boolean install, int table, String description) {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();

        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.drop();

        FlowRule rule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(DROP_PRIORITY)
                .fromApp(appId)
                .makePermanent()
                .forTable(table).build();

        processFlowRule(install, rule, description);
    }

    //Init helper: Table Miss = GoTo
    protected void processTableMissGoTo(boolean install, int table, int goTo, String description) {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();

        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.transition(goTo);

        FlowRule rule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(DROP_PRIORITY)
                .fromApp(appId)
                .makePermanent()
                .forTable(table).build();

        processFlowRule(install, rule, description);
    }

    //Init helper: Apply flow rule
    protected void processFlowRule(boolean install, FlowRule rule, String description) {
        FlowRuleOperations.Builder ops = FlowRuleOperations.builder();
        ops = install ? ops.add(rule) : ops.remove(rule);

        flowRuleService.apply(ops.build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                log.info(description + " success: " + ops.toString() + ", " + rule.toString());
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                log.info(description + " error: " + ops.toString() + ", " + rule.toString());
            }
        }));
    }

    @Override
    public void purgeAll(ApplicationId appId) {
        flowRuleService.purgeFlowRules(deviceId, appId);
        groupService.purgeGroupEntries(deviceId, appId);
    }
}
