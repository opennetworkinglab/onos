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
import org.onosproject.net.flow.criteria.EthCriterion;
import org.onosproject.net.flow.criteria.EthTypeCriterion;
import org.onosproject.net.flow.criteria.IPCriterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.criteria.VlanIdCriterion;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.net.flowobjective.FlowObjectiveStore;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.NextObjective;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.onosproject.store.serializers.KryoNamespaces;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

import static org.slf4j.LoggerFactory.getLogger;

/**
 *  Simple 2-Table Pipeline for Software/NPU based routers. This pipeline
 *  does not forward IP traffic to next-hop groups. Instead it forwards traffic
 *  using OF FlowMod actions.
 */
public class SoftRouterPipeline extends AbstractHandlerBehaviour implements Pipeliner {

    protected static final int FILTER_TABLE = 0;
    protected static final int FIB_TABLE = 1;

    private static final int DROP_PRIORITY = 0;
    private static final int DEFAULT_PRIORITY = 0x8000;
    private static final int HIGHEST_PRIORITY = 0xffff;

    private ServiceDirectory serviceDirectory;
    protected FlowRuleService flowRuleService;
    private CoreService coreService;
    private FlowObjectiveStore flowObjectiveStore;
    protected DeviceId deviceId;
    protected ApplicationId appId;
    private ApplicationId driverId;
    private Collection<Filter> filters;
    private Collection<ForwardingObjective> pendingVersatiles;

    private KryoNamespace appKryo = new KryoNamespace.Builder()
        .register(DummyGroup.class)
        .register(KryoNamespaces.API)
        .register(byte[].class)
        .build();

    private final Logger log = getLogger(getClass());

    @Override
    public void init(DeviceId deviceId, PipelinerContext context) {
        this.serviceDirectory = context.directory();
        this.deviceId = deviceId;
        coreService = serviceDirectory.get(CoreService.class);
        flowRuleService = serviceDirectory.get(FlowRuleService.class);
        flowObjectiveStore = context.store();
        driverId = coreService.registerApplication(
                "org.onosproject.driver.SoftRouterPipeline");
        filters = Collections.newSetFromMap(new ConcurrentHashMap<Filter, Boolean>());
        pendingVersatiles = Collections.newSetFromMap(
                                new ConcurrentHashMap<ForwardingObjective, Boolean>());
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
        FlowRuleOperations.Builder flowOpsBuilder = FlowRuleOperations.builder();

        rules = processForward(fwd);
        switch (fwd.op()) {
            case ADD:
                rules.stream()
                        .filter(rule -> rule != null)
                        .forEach(flowOpsBuilder::add);
                break;
            case REMOVE:
                rules.stream()
                        .filter(rule -> rule != null)
                        .forEach(flowOpsBuilder::remove);
                break;
            default:
                fail(fwd, ObjectiveError.UNKNOWN);
                log.warn("Unknown forwarding type {}", fwd.op());
        }


        flowRuleService.apply(flowOpsBuilder.build(new FlowRuleOperationsContext() {
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
            if (treatments.size() != 1) {
                log.error("Next Objectives of type Simple should only have a "
                        + "single Traffic Treatment. Next Objective Id:{}", nextObjective.id());
               fail(nextObjective, ObjectiveError.BADPARAMS);
               return;
            }
            processSimpleNextObjective(nextObjective);
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
        //Drop rules for both tables
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        FlowRuleOperations.Builder ops = FlowRuleOperations.builder();

        treatment.drop();

        FlowRule rule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(DROP_PRIORITY)
                .fromApp(driverId)
                .makePermanent()
                .forTable(FILTER_TABLE)
                .build();
        ops = ops.add(rule);

        rule = DefaultFlowRule.builder().forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(DROP_PRIORITY)
                .fromApp(driverId)
                .makePermanent()
                .forTable(FIB_TABLE)
                .build();
        ops = ops.add(rule);

        flowRuleService.apply(ops.build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                log.info("Provisioned drop rules in both tables");
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                log.info("Failed to provision drop rules");
            }
        }));
    }

    private void processFilter(FilteringObjective filt, boolean install,
                               ApplicationId applicationId) {
        // This driver only processes filtering criteria defined with switch
        // ports as the key
        PortCriterion p; EthCriterion e = null; VlanIdCriterion v = null;
        Collection<IPCriterion> ips = new ArrayList<IPCriterion>();
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
                e = (EthCriterion) c;
            } else if (c.type() == Criterion.Type.VLAN_VID) {
                v = (VlanIdCriterion) c;
            } else if (c.type() == Criterion.Type.IPV4_DST) {
                ips.add((IPCriterion) c);
            } else {
                log.error("Unsupported filter {}", c);
                fail(filt, ObjectiveError.UNSUPPORTED);
                return;
            }
        }

        log.debug("adding Port/VLAN/MAC filtering rules in filter table: {}/{}/{}",
                  p.port(), v.vlanId(), e.mac());
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        selector.matchInPort(p.port());
        selector.matchVlanId(v.vlanId());
        selector.matchEthDst(e.mac());
        selector.matchEthType(Ethernet.TYPE_IPV4);
        treatment.popVlan();
        treatment.transition(FIB_TABLE); // all other IPs to the FIB table
        FlowRule rule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(DEFAULT_PRIORITY)
                .fromApp(applicationId)
                .makePermanent()
                .forTable(FILTER_TABLE).build();
        ops =  ops.add(rule);

        for (IPCriterion ipaddr : ips) {
            log.debug("adding IP filtering rules in FILTER table: {}", ipaddr.ip());
            selector = DefaultTrafficSelector.builder();
            treatment = DefaultTrafficTreatment.builder();
            selector.matchInPort(p.port());
            selector.matchVlanId(v.vlanId());
            selector.matchEthDst(e.mac());
            selector.matchEthType(Ethernet.TYPE_IPV4);
            selector.matchIPDst(ipaddr.ip()); // router IPs to the controller
            treatment.setOutput(PortNumber.CONTROLLER);
            rule = DefaultFlowRule.builder()
                    .forDevice(deviceId)
                    .withSelector(selector.build())
                    .withTreatment(treatment.build())
                    .withPriority(HIGHEST_PRIORITY)
                    .fromApp(applicationId)
                    .makePermanent()
                    .forTable(FILTER_TABLE).build();
            ops =  ops.add(rule);
        }

        // cache for later use
        Filter filter = new Filter(p, e, v, ips);
        filters.add(filter);
        // apply any pending versatile forwarding objectives
        for (ForwardingObjective fwd : pendingVersatiles) {
            Collection<FlowRule> ret = processVersatilesWithFilters(filter, fwd);
            for (FlowRule fr : ret) {
                ops.add(fr);
            }
        }

        ops = install ? ops.add(rule) : ops.remove(rule);
        // apply filtering flow rules
        flowRuleService.apply(ops.build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                log.info("Applied filtering rules");
                pass(filt);
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                log.info("Failed to apply filtering rules");
                fail(filt, ObjectiveError.FLOWINSTALLATIONFAILED);
            }
        }));
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

    /**
     * SoftRouter has a single versatile table - the filter table. All versatile
     * flow rules must include the filtering rules.
     *
     * @param fwd The forwarding objective of type versatile
     * @return  A collection of flow rules meant to be delivered to the flowrule
     *          subsystem. May return empty collection in case of failures.
     */
    private Collection<FlowRule> processVersatile(ForwardingObjective fwd) {
        if (filters.isEmpty()) {
            pendingVersatiles.add(fwd);
            return Collections.emptySet();
        }
        Collection<FlowRule> flowrules = new ArrayList<FlowRule>();
        for (Filter filter : filters) {
            flowrules.addAll(processVersatilesWithFilters(filter, fwd));
        }
        return flowrules;
    }

    private Collection<FlowRule> processVersatilesWithFilters(
                Filter filt, ForwardingObjective fwd) {
        log.info("Processing versatile forwarding objective");
        Collection<FlowRule> flows = new ArrayList<FlowRule>();
        TrafficSelector match = fwd.selector();
        EthTypeCriterion ethType =
                (EthTypeCriterion) match.getCriterion(Criterion.Type.ETH_TYPE);
        if (ethType == null) {
            log.error("Versatile forwarding objective must include ethType");
            fail(fwd, ObjectiveError.UNKNOWN);
            return Collections.emptySet();
        }

        if (ethType.ethType().toShort() == Ethernet.TYPE_ARP) {
            // need to install ARP request & reply flow rules for each interface filter

            // rule for ARP replies
            TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
            selector.matchInPort(filt.port());
            selector.matchVlanId(filt.vlanId());
            selector.matchEthDst(filt.mac());
            selector.matchEthType(Ethernet.TYPE_ARP);
            FlowRule.Builder ruleBuilder = DefaultFlowRule.builder()
                    .fromApp(fwd.appId())
                    .withPriority(fwd.priority())
                    .forDevice(deviceId)
                    .withSelector(selector.build())
                    .withTreatment(fwd.treatment())
                    .makePermanent()
                    .forTable(FILTER_TABLE);
            flows.add(ruleBuilder.build());

            //rule for ARP requests
            selector = DefaultTrafficSelector.builder();
            selector.matchInPort(filt.port());
            selector.matchVlanId(filt.vlanId());
            selector.matchEthDst(MacAddress.BROADCAST);
            selector.matchEthType(Ethernet.TYPE_ARP);
            ruleBuilder = DefaultFlowRule.builder()
                    .fromApp(fwd.appId())
                    .withPriority(fwd.priority())
                    .forDevice(deviceId)
                    .withSelector(selector.build())
                    .withTreatment(fwd.treatment())
                    .makePermanent()
                    .forTable(FILTER_TABLE);
            flows.add(ruleBuilder.build());

            return flows;
        }
        // not handling other versatile flows
        return Collections.emptySet();
    }

    /**
     * SoftRouter has a single specific table - the FIB Table. It emulates
     * LPM matching of dstIP by using higher priority flows for longer prefixes.
     * Flows are forwarded using flow-actions
     *
     * @param fwd The forwarding objective of type simple
     * @return A collection of flow rules meant to be delivered to the flowrule
     *         subsystem. Typically the returned collection has a single flowrule.
     *         May return empty collection in case of failures.
     *
     */
    private Collection<FlowRule> processSpecific(ForwardingObjective fwd) {
        log.debug("Processing specific forwarding objective");
        TrafficSelector selector = fwd.selector();
        EthTypeCriterion ethType =
                (EthTypeCriterion) selector.getCriterion(Criterion.Type.ETH_TYPE);
        // XXX currently supporting only the L3 unicast table
        if (ethType == null || ethType.ethType().toShort() != Ethernet.TYPE_IPV4) {
            fail(fwd, ObjectiveError.UNSUPPORTED);
            return Collections.emptySet();
        }

        TrafficSelector filteredSelector =
                DefaultTrafficSelector.builder()
                        .matchEthType(Ethernet.TYPE_IPV4)
                        .matchIPDst(((IPCriterion)
                                selector.getCriterion(Criterion.Type.IPV4_DST)).ip())
                        .build();

        TrafficTreatment tt = null;
        if (fwd.nextId() != null) {
            NextGroup next = flowObjectiveStore.getNextGroup(fwd.nextId());
            if (next == null) {
                log.error("next-id {} does not exist in store", fwd.nextId());
                return Collections.emptySet();
            }
            tt = appKryo.deserialize(next.data());
            if (tt == null)  {
                log.error("Error in deserializing next-id {}", fwd.nextId());
                return Collections.emptySet();
            }
        }

        FlowRule.Builder ruleBuilder = DefaultFlowRule.builder()
                .fromApp(fwd.appId())
                .withPriority(fwd.priority())
                .forDevice(deviceId)
                .withSelector(filteredSelector)
                .withTreatment(tt);

        if (fwd.permanent()) {
            ruleBuilder.makePermanent();
        } else {
            ruleBuilder.makeTemporary(fwd.timeout());
        }

        ruleBuilder.forTable(FIB_TABLE);
        return Collections.singletonList(ruleBuilder.build());
    }

    /**
     * Next Objectives are stored as dummy groups for retrieval later
     * when Forwarding Objectives reference the next objective id. At that point
     * the dummy group is fetched from the distributed store and the enclosed
     * treatment is applied as a flow rule action.
     *
     * @param nextObj the next objective of type simple
     */
    private void processSimpleNextObjective(NextObjective nextObj) {
        // Simple next objective has a single treatment (not a collection)
        TrafficTreatment treatment = nextObj.next().iterator().next();
        flowObjectiveStore.putNextGroup(nextObj.id(),
                                        new DummyGroup(treatment));
    }

    private class Filter {
        private PortCriterion port;
        private VlanIdCriterion vlan;
        private EthCriterion eth;

        @SuppressWarnings("unused")
        private Collection<IPCriterion> ips;

        public Filter(PortCriterion p, EthCriterion e, VlanIdCriterion v,
                      Collection<IPCriterion> ips) {
            this.eth = e;
            this.port = p;
            this.vlan = v;
            this.ips = ips;
        }

        public PortNumber port() {
            return port.port();
        }

        public VlanId vlanId() {
            return vlan.vlanId();
        }

        public MacAddress mac() {
            return eth.mac();
        }
    }

    private class DummyGroup implements NextGroup {
        TrafficTreatment nextActions;

        public DummyGroup(TrafficTreatment next) {
            this.nextActions = next;
        }

        @Override
        public byte[] data() {
            return appKryo.serialize(nextActions);
        }

    }

}
