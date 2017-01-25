/*
 * Copyright 2016-present Open Networking Laboratory
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
import org.onlab.packet.EthType;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IpPrefix;
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
import org.onosproject.net.flow.criteria.IPProtocolCriterion;
import org.onosproject.net.flow.criteria.Icmpv6TypeCriterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.criteria.VlanIdCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions.OutputInstruction;
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

import static org.onlab.packet.Ethernet.TYPE_IPV4;
import static org.onlab.packet.ICMP6.ECHO_REPLY;
import static org.onlab.packet.ICMP6.ECHO_REQUEST;
import static org.onlab.packet.IPv6.PROTOCOL_ICMP6;
import static org.onlab.util.Tools.delay;
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

    private KryoNamespace appKryo = new KryoNamespace.Builder()
            .register(KryoNamespaces.API)
            .register(DummyGroup.class)
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
                        .filter(Objects::nonNull)
                        .forEach(flowOpsBuilder::add);
                break;
            case REMOVE:
                rules.stream()
                        .filter(Objects::nonNull)
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
        obj.context().ifPresent(context -> context.onSuccess(obj));
    }

    private void fail(Objective obj, ObjectiveError error) {
        obj.context().ifPresent(context -> context.onError(obj, error));
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
        PortCriterion p;
        EthCriterion e = null;
        VlanIdCriterion v = null;

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
            if (c.type() == Criterion.Type.ETH_DST ||
                    c.type() == Criterion.Type.ETH_DST_MASKED) {
                e = (EthCriterion) c;
            } else if (c.type() == Criterion.Type.VLAN_VID) {
                v = (VlanIdCriterion) c;
            } else {
                log.error("Unsupported filter {}", c);
                fail(filt, ObjectiveError.UNSUPPORTED);
                return;
            }

        }

        log.debug("Modifying Port/VLAN/MAC filtering rules in filter table: {}/{}/{}",
                  p.port(), v.vlanId(), e.mac());
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        selector.matchInPort(p.port());

        //Multicast MAC
        if (e.mask() != null) {
            selector.matchEthDstMasked(e.mac(), e.mask());
        } else {
            selector.matchEthDst(e.mac());
        }
        selector.matchVlanId(v.vlanId());
        if (!v.vlanId().equals(VlanId.NONE)) {
            treatment.popVlan();
        }
        treatment.transition(FIB_TABLE);
        FlowRule rule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(DEFAULT_PRIORITY)
                .fromApp(applicationId)
                .makePermanent()
                .forTable(FILTER_TABLE).build();

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
     * SoftRouter has a single versatile table - the filter table.
     * This table can be used to filter entries that reach the next table (FIB table).
     * It can also be used to punt packets to the controller and/or bypass
     * the FIB table to forward out of a port.
     *
     * @param fwd The forwarding objective of type versatile
     * @return  A collection of flow rules meant to be delivered to the flowrule
     *          subsystem. May return empty collection in case of failures.
     */
    private Collection<FlowRule> processVersatile(ForwardingObjective fwd) {
        log.debug("Received versatile fwd: to next:{}", fwd.nextId());
        Collection<FlowRule> flowrules = new ArrayList<>();
        if (fwd.nextId() == null && fwd.treatment() == null) {
            log.error("Forwarding objective {} from {} must contain "
                              + "nextId or Treatment", fwd.selector(), fwd.appId());
            return Collections.emptySet();
        }

        int tableId = FILTER_TABLE;

        // A punt rule for IP traffic should be directed to the FIB table
        // so that it only takes effect if the packet misses the FIB rules
        if (fwd.treatment() != null && containsPunt(fwd.treatment()) &&
                fwd.selector() != null && matchesIp(fwd.selector()) &&
                !matchesControlTraffic(fwd.selector())) {
            tableId = FIB_TABLE;
        }

        TrafficTreatment.Builder ttBuilder = DefaultTrafficTreatment.builder();
        if (fwd.treatment() != null) {
            fwd.treatment().immediate().forEach(ins -> ttBuilder.add(ins));
        }
        //convert nextId to flow actions
        if (fwd.nextId() != null) {
            // only acceptable value is output to port
            NextGroup next = flowObjectiveStore.getNextGroup(fwd.nextId());
            if (next == null) {
                log.error("next-id {} does not exist in store", fwd.nextId());
                return Collections.emptySet();
            }
            TrafficTreatment nt = appKryo.deserialize(next.data());
            if (nt == null)  {
                log.error("Error in deserializing next-id {}", fwd.nextId());
                return Collections.emptySet();
            }
            for (Instruction ins : nt.allInstructions()) {
                if (ins instanceof OutputInstruction) {
                    ttBuilder.add(ins);
                }
            }
        }

        FlowRule rule = DefaultFlowRule.builder()
                .withSelector(fwd.selector())
                .withTreatment(ttBuilder.build())
                .forTable(tableId)
                .makePermanent()
                .forDevice(deviceId)
                .fromApp(fwd.appId())
                .withPriority(fwd.priority())
                .build();

        flowrules.add(rule);

        return flowrules;
    }

    private boolean containsPunt(TrafficTreatment treatment) {
        return treatment.immediate().stream()
                .anyMatch(i -> i.type().equals(Instruction.Type.OUTPUT)
                        && ((OutputInstruction) i).port().equals(PortNumber.CONTROLLER));
    }

    private boolean matchesIp(TrafficSelector selector) {
        EthTypeCriterion c = (EthTypeCriterion) selector.getCriterion(Criterion.Type.ETH_TYPE);
        return c != null && (c.ethType().equals(EthType.EtherType.IPV4.ethType()) ||
                        c.ethType().equals(EthType.EtherType.IPV6.ethType()));
    }

    private boolean matchesControlTraffic(TrafficSelector selector) {
        EthTypeCriterion c = (EthTypeCriterion) selector.getCriterion(Criterion.Type.ETH_TYPE);
        if (c != null && c.ethType().equals(EthType.EtherType.ARP.ethType())) {
            return true;
        } else if (c != null && c.ethType().equals(EthType.EtherType.IPV6.ethType())) {
            IPProtocolCriterion i = (IPProtocolCriterion) selector.getCriterion(Criterion.Type.IP_PROTO);
            if (i != null && i.protocol() == PROTOCOL_ICMP6) {
                Icmpv6TypeCriterion ic = (Icmpv6TypeCriterion) selector.getCriterion(Criterion.Type.ICMPV6_TYPE);
                if (ic.icmpv6Type() != ECHO_REQUEST && ic.icmpv6Type() != ECHO_REPLY) {
                    return true;
                }
            }
        }
        return false;
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
        log.debug("Processing specific forwarding objective to next:{}", fwd.nextId());
        TrafficSelector selector = fwd.selector();
        EthTypeCriterion ethType =
                (EthTypeCriterion) selector.getCriterion(Criterion.Type.ETH_TYPE);
        // XXX currently supporting only the L3 unicast table
        if (ethType == null || (ethType.ethType().toShort() != TYPE_IPV4
                && ethType.ethType().toShort() != Ethernet.TYPE_IPV6)) {
            fail(fwd, ObjectiveError.UNSUPPORTED);
            return Collections.emptySet();
        }
        //We build the selector according the eth type.
        IpPrefix ipPrefix;
        TrafficSelector.Builder filteredSelector;
        if (ethType.ethType().toShort() == TYPE_IPV4) {
            ipPrefix = ((IPCriterion)
                    selector.getCriterion(Criterion.Type.IPV4_DST)).ip();

            filteredSelector = DefaultTrafficSelector.builder()
                    .matchEthType(TYPE_IPV4);
        } else {
            ipPrefix = ((IPCriterion)
                    selector.getCriterion(Criterion.Type.IPV6_DST)).ip();

            filteredSelector = DefaultTrafficSelector.builder()
                    .matchEthType(Ethernet.TYPE_IPV6);
        }
        // If the prefix is different from the default via.
        if (ipPrefix.prefixLength() != 0) {
            if (ethType.ethType().toShort() == TYPE_IPV4) {
                filteredSelector.matchIPDst(ipPrefix);
            } else {
                filteredSelector.matchIPv6Dst(ipPrefix);
            }
        }

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
                .withSelector(filteredSelector.build());

        if (tt != null) {
            ruleBuilder.withTreatment(tt);
        }

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
        log.debug("Received nextObj {}", nextObj.id());
        // delay processing to emulate group creation
        delay(50);
        TrafficTreatment treatment = nextObj.next().iterator().next();
        flowObjectiveStore.putNextGroup(nextObj.id(),
                                        new DummyGroup(treatment));
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

    @Override
    public List<String> getNextMappings(NextGroup nextGroup) {
        // nextObjectives converted to flow-actions not groups
        return Collections.emptyList();
    }

}
