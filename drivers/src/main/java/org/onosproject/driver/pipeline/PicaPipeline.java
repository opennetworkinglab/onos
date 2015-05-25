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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalNotification;

import org.onlab.osgi.ServiceDirectory;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.DeviceId;
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
import org.onosproject.net.flow.criteria.IPProtocolCriterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.criteria.VlanIdCriterion;
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

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.onlab.util.Tools.groupedThreads;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Pica pipeline handler.
 */
public class PicaPipeline extends AbstractHandlerBehaviour implements Pipeliner {

    protected static final int VLAN_TABLE = 252;
    protected static final int ETHTYPE_TABLE = 252;
    protected static final int IP_UNICAST_TABLE = 251;
    protected static final int ACL_TABLE = 251;

    private static final int CONTROLLER_PRIORITY = 255;
    private static final int DROP_PRIORITY = 0;
    private static final int HIGHEST_PRIORITY = 0xffff;

    private final Logger log = getLogger(getClass());

    private ServiceDirectory serviceDirectory;
    private FlowRuleService flowRuleService;
    private CoreService coreService;
    private GroupService groupService;
    private FlowObjectiveStore flowObjectiveStore;
    private DeviceId deviceId;
    private ApplicationId appId;

    private KryoNamespace appKryo = new KryoNamespace.Builder()
            .register(GroupKey.class)
            .register(DefaultGroupKey.class)
            .register(PicaGroup.class)
            .register(byte[].class)
            .build();

    private Cache<GroupKey, NextObjective> pendingGroups;

    private ScheduledExecutorService groupChecker =
            Executors.newScheduledThreadPool(2, groupedThreads("onos/pipeliner",
                                                               "ovs-pica-%d"));

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

        groupChecker.scheduleAtFixedRate(new GroupChecker(), 0, 500, TimeUnit.MILLISECONDS);

        coreService = serviceDirectory.get(CoreService.class);
        flowRuleService = serviceDirectory.get(FlowRuleService.class);
        groupService = serviceDirectory.get(GroupService.class);
        flowObjectiveStore = context.store();

        groupService.addListener(new InnerGroupListener());

        appId = coreService.registerApplication(
                "org.onosproject.driver.OVSPicaPipeline");

        initializePipeline();
    }

    @Override
    public void filter(FilteringObjective filteringObjective) {
        if (filteringObjective.type() == FilteringObjective.Type.PERMIT) {
            processFilter(filteringObjective,
                          filteringObjective.op() == Objective.Operation.ADD,
                          filteringObjective.appId());
        } else {
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
                rules.stream()
                        .filter(rule -> rule != null)
                        .forEach(flowBuilder::add);
                break;
            case REMOVE:
                rules.stream()
                        .filter(rule -> rule != null)
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

    @Override
    public void next(NextObjective nextObjective) {
        switch (nextObjective.type()) {
            case SIMPLE:
                Collection<TrafficTreatment> treatments = nextObjective.next();
                if (treatments.size() == 1) {
                    TrafficTreatment treatment = treatments.iterator().next();
                    GroupBucket bucket =
                            DefaultGroupBucket.createIndirectGroupBucket(treatment);
                    final GroupKey key = new DefaultGroupKey(appKryo.serialize(nextObjective.id()));
                    GroupDescription groupDescription
                            = new DefaultGroupDescription(deviceId,
                                    GroupDescription.Type.INDIRECT,
                                    new GroupBuckets(Collections
                                                             .singletonList(bucket)),
                                    key,
                                    null, // let group service determine group id
                                    nextObjective.appId());
                    groupService.addGroup(groupDescription);
                    pendingGroups.put(key, nextObjective);
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
        if (ethType.ethType() == Ethernet.TYPE_ARP) {
            log.warn("Driver automatically handles ARP packets by punting to controller "
                    + " from ETHER table");
            pass(fwd);
            return Collections.emptySet();
        } else if (ethType.ethType() == Ethernet.TYPE_LLDP ||
                ethType.ethType() == Ethernet.TYPE_BSN) {
            log.warn("Driver currently does not currently handle LLDP packets");
            fail(fwd, ObjectiveError.UNSUPPORTED);
            return Collections.emptySet();
        } else if (ethType.ethType() == Ethernet.TYPE_IPV4) {
            IPCriterion ipSrc = (IPCriterion) selector
                    .getCriterion(Criterion.Type.IPV4_SRC);
            IPCriterion ipDst = (IPCriterion) selector
                    .getCriterion(Criterion.Type.IPV4_DST);
            IPProtocolCriterion ipProto = (IPProtocolCriterion) selector
                    .getCriterion(Criterion.Type.IP_PROTO);
            if (ipSrc != null) {
                log.warn("Driver does not currently handle matching Src IP");
                fail(fwd, ObjectiveError.UNSUPPORTED);
                return Collections.emptySet();
            }
            if (ipDst != null) {
                log.error("Driver handles Dst IP matching as specific forwarding "
                        + "objective, not versatile");
                fail(fwd, ObjectiveError.UNSUPPORTED);
                return Collections.emptySet();
            }
            if (ipProto != null && ipProto.protocol() == IPv4.PROTOCOL_TCP) {
                log.warn("Driver automatically punts all packets reaching the "
                        + "LOCAL table to the controller");
                pass(fwd);
                return Collections.emptySet();
            }
        }

        log.warn("Driver does not support given versatile forwarding objective");
        fail(fwd, ObjectiveError.UNSUPPORTED);
        return Collections.emptySet();
    }

    private Collection<FlowRule> processSpecific(ForwardingObjective fwd) {
        log.debug("Processing specific forwarding objective");
        TrafficSelector selector = fwd.selector();
        EthTypeCriterion ethType =
                (EthTypeCriterion) selector.getCriterion(Criterion.Type.ETH_TYPE);
        if (ethType == null || ethType.ethType() != Ethernet.TYPE_IPV4) {
            fail(fwd, ObjectiveError.UNSUPPORTED);
            return Collections.emptySet();
        }

        TrafficSelector filteredSelector =
                DefaultTrafficSelector.builder()
                        .matchEthType(Ethernet.TYPE_IPV4)
                        .matchIPDst(
                                ((IPCriterion)
                                        selector.getCriterion(Criterion.Type.IPV4_DST)).ip())
                        .build();

        TrafficTreatment.Builder tb = DefaultTrafficTreatment.builder();

        if (fwd.nextId() != null) {
            NextGroup next = flowObjectiveStore.getNextGroup(fwd.nextId());
            GroupKey key = appKryo.deserialize(next.data());
            Group group = groupService.getGroup(deviceId, key);
            if (group == null) {
                log.warn("The group left!");
                fail(fwd, ObjectiveError.GROUPMISSING);
                return Collections.emptySet();
            }
            tb.group(group.id());
        }

        FlowRule.Builder ruleBuilder = DefaultFlowRule.builder()
                .fromApp(fwd.appId())
                .withPriority(fwd.priority())
                .forDevice(deviceId)
                .withSelector(filteredSelector)
                .withTreatment(tb.build());

        if (fwd.permanent()) {
            ruleBuilder.makePermanent();
        } else {
            ruleBuilder.makeTemporary(fwd.timeout());
        }

        ruleBuilder.forTable(IP_UNICAST_TABLE);


        return Collections.singletonList(ruleBuilder.build());

    }

    private void processFilter(FilteringObjective filt, boolean install,
                                             ApplicationId applicationId) {
        // This driver only processes filtering criteria defined with switch
        // ports as the key
        PortCriterion p;
        if (!filt.key().equals(Criteria.dummy()) &&
                filt.key().type() == Criterion.Type.IN_PORT) {
            p = (PortCriterion) filt.key();
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
                EthCriterion e = (EthCriterion) c;
                log.debug("adding rule for MAC: {}", e.mac());
                TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
                TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
                selector.matchEthDst(e.mac());
                treatment.transition(IP_UNICAST_TABLE);
                FlowRule rule = DefaultFlowRule.builder()
                        .forDevice(deviceId)
                        .withSelector(selector.build())
                        .withTreatment(treatment.build())
                        .withPriority(CONTROLLER_PRIORITY)
                        .fromApp(applicationId)
                        .makePermanent()
                        .forTable(ETHTYPE_TABLE).build();
                ops =  install ? ops.add(rule) : ops.remove(rule);
            } else if (c.type() == Criterion.Type.VLAN_VID) {
                VlanIdCriterion v = (VlanIdCriterion) c;
                log.debug("adding rule for VLAN: {}", v.vlanId());
                TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
                TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
                selector.matchVlanId(v.vlanId());
                selector.matchInPort(p.port());
                treatment.transition(ETHTYPE_TABLE);
                treatment.deferred().popVlan();
                FlowRule rule = DefaultFlowRule.builder()
                        .forDevice(deviceId)
                        .withSelector(selector.build())
                        .withTreatment(treatment.build())
                        .withPriority(CONTROLLER_PRIORITY)
                        .fromApp(applicationId)
                        .makePermanent()
                        .forTable(VLAN_TABLE).build();
                ops = install ? ops.add(rule) : ops.remove(rule);
            } else if (c.type() == Criterion.Type.IPV4_DST) {
                IPCriterion ip = (IPCriterion) c;
                log.debug("adding rule for IP: {}", ip.ip());
                TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
                TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
                selector.matchEthType(Ethernet.TYPE_IPV4);
                selector.matchIPDst(ip.ip());
                treatment.transition(ACL_TABLE);
                FlowRule rule = DefaultFlowRule.builder()
                        .forDevice(deviceId)
                        .withSelector(selector.build())
                        .withTreatment(treatment.build())
                        .withPriority(HIGHEST_PRIORITY)
                        .fromApp(applicationId)
                        .makePermanent()
                        .forTable(IP_UNICAST_TABLE).build();

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
                log.info("Applied filtering rules");
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                fail(filt, ObjectiveError.FLOWINSTALLATIONFAILED);
                log.info("Failed to apply filtering rules");
            }
        }));
    }

    private void pass(Objective obj) {
        if (obj.context().isPresent()) {
            obj.context().get().onSuccess(obj);
        }
    }

    private void fail(Objective obj, ObjectiveError error) {
        if (obj.context().isPresent()) {
            obj.context().get().onError(obj, error);
        }
    }

    private void initializePipeline() {
        processVlanTable(true);
        processEtherTable(true);
        processIpUnicastTable(true);
        //processACLTable(true);
    }

    private void processVlanTable(boolean install) {
        TrafficSelector.Builder selector;
        TrafficTreatment.Builder treatment;
        FlowRuleOperations.Builder ops = FlowRuleOperations.builder();
        FlowRule rule;


        //Drop rule
        selector = DefaultTrafficSelector.builder();
        treatment = DefaultTrafficTreatment.builder();

        treatment.drop();

        rule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(DROP_PRIORITY)
                .fromApp(appId)
                .makePermanent()
                .forTable(VLAN_TABLE).build();

        ops = install ? ops.add(rule) : ops.remove(rule);

        flowRuleService.apply(ops.build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                log.info("Provisioned vlan table");
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                log.info("Failed to provision vlan table");
            }
        }));
    }

    private void processEtherTable(boolean install) {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment
                .builder();
        FlowRuleOperations.Builder ops = FlowRuleOperations.builder();
        FlowRule rule;

        selector.matchEthType(Ethernet.TYPE_ARP);
        treatment.punt();

        rule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(CONTROLLER_PRIORITY)
                .fromApp(appId)
                .makePermanent()
                .forTable(ETHTYPE_TABLE).build();

        ops = install ? ops.add(rule) : ops.remove(rule);

        selector = DefaultTrafficSelector.builder();
        treatment = DefaultTrafficTreatment.builder();

        selector.matchEthType(Ethernet.TYPE_IPV4);
        treatment.transition(IP_UNICAST_TABLE);

        rule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withPriority(CONTROLLER_PRIORITY)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .fromApp(appId)
                .makePermanent()
                .forTable(ETHTYPE_TABLE).build();

        ops = install ? ops.add(rule) : ops.remove(rule);

        //Drop rule
        selector = DefaultTrafficSelector.builder();
        treatment = DefaultTrafficTreatment.builder();

        treatment.drop();

        rule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(DROP_PRIORITY)
                .fromApp(appId)
                .makePermanent()
                .forTable(ETHTYPE_TABLE).build();


        ops = install ? ops.add(rule) : ops.remove(rule);

        flowRuleService.apply(ops.build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                log.info("Provisioned ether table");
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                log.info("Failed to provision ether table");
            }
        }));

    }


    private void processIpUnicastTable(boolean install) {
        TrafficSelector.Builder selector;
        TrafficTreatment.Builder treatment;
        FlowRuleOperations.Builder ops = FlowRuleOperations.builder();
        FlowRule rule;

        //Drop rule
        selector = DefaultTrafficSelector.builder();
        treatment = DefaultTrafficTreatment.builder();

        treatment.drop();

        rule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(DROP_PRIORITY)
                .fromApp(appId)
                .makePermanent()
                .forTable(IP_UNICAST_TABLE).build();

        ops = install ? ops.add(rule) : ops.remove(rule);

        flowRuleService.apply(ops.build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                log.info("Provisioned FIB table");
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                log.info("Failed to provision FIB table");
            }
        }));
    }


    private class InnerGroupListener implements GroupListener {
        @Override
        public void event(GroupEvent event) {
            if (event.type() == GroupEvent.Type.GROUP_ADDED) {
                GroupKey key = event.subject().appCookie();

                NextObjective obj = pendingGroups.getIfPresent(key);
                if (obj != null) {
                    flowObjectiveStore.putNextGroup(obj.id(), new PicaGroup(key));
                    pass(obj);
                    pendingGroups.invalidate(key);
                }
            }
        }
    }


    private class GroupChecker implements Runnable {

        @Override
        public void run() {
            Set<GroupKey> keys = pendingGroups.asMap().keySet().stream()
                    .filter(key -> groupService.getGroup(deviceId, key) != null)
                    .collect(Collectors.toSet());

            keys.stream().forEach(key -> {
                NextObjective obj = pendingGroups.getIfPresent(key);
                if (obj == null) {
                    return;
                }
                pass(obj);
                pendingGroups.invalidate(key);
                log.info("Heard back from group service for group {}. "
                        + "Applying pending forwarding objectives", obj.id());
                flowObjectiveStore.putNextGroup(obj.id(), new PicaGroup(key));
            });
        }
    }

    private class PicaGroup implements NextGroup {

        private final GroupKey key;

        public PicaGroup(GroupKey key) {
            this.key = key;
        }

        @SuppressWarnings("unused")
        public GroupKey key() {
            return key;
        }

        @Override
        public byte[] data() {
            return appKryo.serialize(key);
        }

    }
}
