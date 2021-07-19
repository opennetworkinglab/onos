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
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction;
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
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Pica pipeline handler.
 */
public class PicaPipeline extends AbstractHandlerBehaviour implements Pipeliner {

    protected static final int IP_UNICAST_TABLE = 252;
    protected static final int ACL_TABLE = 0;

    //private static final int CONTROLLER_PRIORITY = 255;
    private static final int DROP_PRIORITY = 0;
    private static final int HIGHEST_PRIORITY = 0xffff;

    private final Logger log = getLogger(getClass());

    private ServiceDirectory serviceDirectory;
    private FlowRuleService flowRuleService;
    private CoreService coreService;
    private FlowObjectiveStore flowObjectiveStore;
    private DeviceId deviceId;
    private ApplicationId appId;
    private Collection<Filter> filters;
    private Collection<ForwardingObjective> pendingVersatiles;

    private KryoNamespace appKryo = new KryoNamespace.Builder()
            .register(KryoNamespaces.API)
            .register(PicaGroup.class)
            .build("PicaPipeline");

    @Override
    public void init(DeviceId deviceId, PipelinerContext context) {
        this.serviceDirectory = context.directory();
        this.deviceId = deviceId;

        coreService = serviceDirectory.get(CoreService.class);
        flowRuleService = serviceDirectory.get(FlowRuleService.class);
        flowObjectiveStore = context.store();
        filters = Collections.newSetFromMap(new ConcurrentHashMap<Filter, Boolean>());
        pendingVersatiles = Collections.newSetFromMap(
            new ConcurrentHashMap<ForwardingObjective, Boolean>());
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
                TrafficTreatment treatment = treatments.iterator().next();
                TrafficTreatment.Builder filteredTreatment = DefaultTrafficTreatment.builder();
                VlanId modVlanId;
                for (Instruction ins : treatment.allInstructions()) {
                    if (ins.type() == Instruction.Type.L2MODIFICATION) {
                        L2ModificationInstruction l2ins = (L2ModificationInstruction) ins;
                        switch (l2ins.subtype()) {
                            case ETH_DST:
                                filteredTreatment.setEthDst(
                                        ((L2ModificationInstruction.ModEtherInstruction) l2ins).mac());
                                break;
                            case ETH_SRC:
                                filteredTreatment.setEthSrc(
                                        ((L2ModificationInstruction.ModEtherInstruction) l2ins).mac());
                                break;
                            case VLAN_ID:
                                modVlanId = ((L2ModificationInstruction.ModVlanIdInstruction) l2ins).vlanId();
                                filteredTreatment.setVlanId(modVlanId);
                                break;
                            default:
                                break;
                        }
                    } else if (ins.type() == Instruction.Type.OUTPUT) {
                        //long portNum = ((Instructions.OutputInstruction) ins).port().toLong();
                        filteredTreatment.add(ins);
                    } else {
                        // Ignore the vlan_pcp action since it's does matter much.
                        log.warn("Driver does not handle this type of TrafficTreatment"
                                + " instruction in nextObjectives:  {}", ins.type());
                    }
                }
                // store for future use
                flowObjectiveStore.putNextGroup(nextObjective.id(),
                                                new PicaGroup(filteredTreatment.build()));
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

    @Override
    public void purgeAll(ApplicationId appId) {
        flowRuleService.purgeFlowRules(deviceId, appId);
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
        TrafficTreatment treatment = fwd.treatment();
        Collection<FlowRule> flowrules = new ArrayList<FlowRule>();

        // first add this rule for basic single-table operation
        // or non-ARP related multi-table operation
        FlowRule rule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector)
                .withTreatment(treatment)
                .withPriority(fwd.priority())
                .fromApp(fwd.appId())
                .makePermanent()
                .forTable(ACL_TABLE).build();
        flowrules.add(rule);

        EthTypeCriterion ethType =
                (EthTypeCriterion) selector.getCriterion(Criterion.Type.ETH_TYPE);
        if (ethType == null) {
            log.warn("No ethType in versatile forwarding obj. Not processing further.");
            return flowrules;
        }

        // now deal with possible mix of ARP with filtering objectives
        // in multi-table scenarios
        if (ethType.ethType().toShort() == Ethernet.TYPE_ARP) {
            if (filters.isEmpty()) {
                pendingVersatiles.add(fwd);
                return flowrules;
            }
            for (Filter filter : filters) {
                flowrules.addAll(processVersatilesWithFilters(filter, fwd));
            }
        }
        return flowrules;
    }

    private Collection<FlowRule> processVersatilesWithFilters(
                Filter filt, ForwardingObjective fwd) {
        Collection<FlowRule> flows = new ArrayList<FlowRule>();

        // rule for ARP replies
        log.debug("adding ARP rule in ACL table");
        TrafficSelector.Builder sel = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treat = DefaultTrafficTreatment.builder();
        sel.matchInPort(filt.port());
        sel.matchVlanId(filt.vlanId());
        sel.matchEthDst(filt.mac());
        sel.matchEthType(Ethernet.TYPE_ARP);
        treat.setOutput(PortNumber.CONTROLLER);
        FlowRule rule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(sel.build())
                .withTreatment(treat.build())
                .withPriority(HIGHEST_PRIORITY)
                .fromApp(appId)
                .makePermanent()
                .forTable(ACL_TABLE).build();
        flows.add(rule);

        // rule for ARP Broadcast
        sel = DefaultTrafficSelector.builder();
        treat = DefaultTrafficTreatment.builder();
        sel.matchInPort(filt.port());
        sel.matchVlanId(filt.vlanId());
        sel.matchEthDst(MacAddress.BROADCAST);
        sel.matchEthType(Ethernet.TYPE_ARP);
        treat.setOutput(PortNumber.CONTROLLER);
        rule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(sel.build())
                .withTreatment(treat.build())
                .withPriority(HIGHEST_PRIORITY)
                .fromApp(appId)
                .makePermanent()
                .forTable(ACL_TABLE).build();
        flows.add(rule);

        return flows;
    }


    private Collection<FlowRule> processSpecific(ForwardingObjective fwd) {
        log.debug("Processing specific forwarding objective");
        TrafficSelector selector = fwd.selector();
        EthTypeCriterion ethType =
                (EthTypeCriterion) selector.getCriterion(Criterion.Type.ETH_TYPE);
        if (ethType == null || ethType.ethType().toShort() != Ethernet.TYPE_IPV4) {
            fail(fwd, ObjectiveError.UNSUPPORTED);
            return Collections.emptySet();
        }

        List<FlowRule> ipflows = new ArrayList<FlowRule>();
        for (Filter f: filters) {
            TrafficSelector filteredSelector =
                    DefaultTrafficSelector.builder()
                    .matchEthType(Ethernet.TYPE_IPV4)
                    .matchIPDst(
                                ((IPCriterion)
                                        selector.getCriterion(Criterion.Type.IPV4_DST)).ip())
                    .matchEthDst(f.mac())
                    .matchVlanId(f.vlanId())
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
            ruleBuilder.forTable(IP_UNICAST_TABLE);
            ipflows.add(ruleBuilder.build());
        }

        return ipflows;
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

        EthCriterion e = null;
        VlanIdCriterion v = null;
        Collection<IPCriterion> ips = new ArrayList<IPCriterion>();
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

        if (v == null || e == null) {
            log.warn("Pica Pipeline ETH_DST and/or VLAN_ID not specified");
            fail(filt, ObjectiveError.BADPARAMS);
            return;
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

        for (IPCriterion ipaddr : ips) {
            log.debug("adding IP filtering rules in ACL table: {}", ipaddr.ip());
            TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
            TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
            selector.matchInPort(p.port());
            selector.matchVlanId(v.vlanId());
            selector.matchEthDst(e.mac());
            selector.matchEthType(Ethernet.TYPE_IPV4);
            selector.matchIPDst(ipaddr.ip()); // router IPs to the controller
            treatment.setOutput(PortNumber.CONTROLLER);
            FlowRule rule = DefaultFlowRule.builder()
                    .forDevice(deviceId)
                    .withSelector(selector.build())
                    .withTreatment(treatment.build())
                    .withPriority(HIGHEST_PRIORITY)
                    .fromApp(applicationId)
                    .makePermanent()
                    .forTable(ACL_TABLE).build();
            ops =  ops.add(rule);
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
        obj.context().ifPresent(context -> context.onSuccess(obj));
    }

    private void fail(Objective obj, ObjectiveError error) {
        obj.context().ifPresent(context -> context.onError(obj, error));
    }

    private void initializePipeline() {
        //processIpUnicastTable(true);
        processAclTable(true);
    }

    private void processAclTable(boolean install) {
        TrafficSelector.Builder selector;
        TrafficTreatment.Builder treatment;
        FlowRuleOperations.Builder ops = FlowRuleOperations.builder();
        FlowRule rule;

        //Drop rule
        selector = DefaultTrafficSelector.builder();
        treatment = DefaultTrafficTreatment.builder();

        rule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(DROP_PRIORITY)
                .fromApp(appId)
                .makePermanent()
                .forTable(ACL_TABLE).build();

        ops = install ? ops.add(rule) : ops.remove(rule);

        flowRuleService.apply(ops.build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                log.info("Provisioned ACL table");
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                log.info("Failed to provision ACL table");
            }
        }));
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

    private class PicaGroup implements NextGroup {
        TrafficTreatment nextActions;

        public PicaGroup(TrafficTreatment next) {
            this.nextActions = next;
        }

        @Override
        public byte[] data() {
            return appKryo.serialize(nextActions);
        }
    }

    @Override
    public List<String> getNextMappings(NextGroup nextGroup) {
        // TODO Implementation deferred to vendor
        return null;
    }
}
