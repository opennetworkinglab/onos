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

import com.google.common.collect.Lists;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.packet.EthType;
import org.onlab.packet.IPv4;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.Pipeliner;
import org.onosproject.net.behaviour.PipelinerContext;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceProvider;
import org.onosproject.net.device.DeviceProviderRegistry;
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
import org.onosproject.net.flow.criteria.EthTypeCriterion;
import org.onosproject.net.flow.criteria.IPProtocolCriterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.criteria.VlanIdCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.flow.instructions.L2ModificationInstruction;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.NextObjective;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Pipeliner for OLT device.
 */

public class OltPipeline extends AbstractHandlerBehaviour implements Pipeliner {

    private static final Integer QQ_TABLE = 1;
    private final Logger log = getLogger(getClass());

    static final ProviderId PID = new ProviderId("olt", "org.onosproject.olt", true);

    static final String DEVICE = "isAccess";
    static final String OLT = "true";

    private ServiceDirectory serviceDirectory;
    private FlowRuleService flowRuleService;
    private DeviceId deviceId;
    private CoreService coreService;

    private ApplicationId appId;

    private DeviceProvider provider = new AnnotationProvider();

    @Override
    public void init(DeviceId deviceId, PipelinerContext context) {
        log.debug("Initiate OLT pipeline");
        this.serviceDirectory = context.directory();
        this.deviceId = deviceId;
        DeviceProviderRegistry registry =
                serviceDirectory.get(DeviceProviderRegistry.class);
        flowRuleService = serviceDirectory.get(FlowRuleService.class);
        coreService = serviceDirectory.get(CoreService.class);

        appId = coreService.registerApplication(
                "org.onosproject.driver.OLTPipeline");

    }

    @Override
    public void filter(FilteringObjective filter) {
        Instructions.OutputInstruction output;

        if (filter.meta() != null && !filter.meta().immediate().isEmpty()) {
            output = (Instructions.OutputInstruction) filter.meta().immediate().stream()
                    .filter(t -> t.type().equals(Instruction.Type.OUTPUT))
                    .limit(1)
                    .findFirst().get();

            if (output == null || !output.port().equals(PortNumber.CONTROLLER)) {
                log.error("OLT can only filter packet to controller");
                fail(filter, ObjectiveError.UNSUPPORTED);
                return;
            }
        } else {
            fail(filter, ObjectiveError.BADPARAMS);
            return;
        }

        if (filter.key().type() != Criterion.Type.IN_PORT) {
            fail(filter, ObjectiveError.BADPARAMS);
            return;
        }

        EthTypeCriterion ethType = (EthTypeCriterion)
                filterForCriterion(filter.conditions(), Criterion.Type.ETH_TYPE);

        if (ethType == null) {
            fail(filter, ObjectiveError.BADPARAMS);
            return;
        }

        if (ethType.ethType().equals(EthType.EtherType.EAPOL.ethType())) {
            provisionEapol(filter, ethType, output);
        } else if (ethType.ethType().equals(EthType.EtherType.IPV4.ethType())) {
            IPProtocolCriterion ipProto = (IPProtocolCriterion)
                    filterForCriterion(filter.conditions(), Criterion.Type.IP_PROTO);
            if (ipProto.protocol() == IPv4.PROTOCOL_IGMP) {
                provisionIgmp(filter, ethType, ipProto, output);
            } else {
                log.error("OLT can only filter igmp");
                fail(filter, ObjectiveError.UNSUPPORTED);
            }
        } else {
            log.error("OLT can only filter eapol and igmp");
            fail(filter, ObjectiveError.UNSUPPORTED);
        }

    }


    @Override
    public void forward(ForwardingObjective fwd) {
        TrafficTreatment treatment = fwd.treatment();

        List<Instruction> instructions = treatment.allInstructions();

        Optional<Instruction> vlanIntruction = instructions.stream()
                .filter(i -> i.type() == Instruction.Type.L2MODIFICATION)
                .filter(i -> ((L2ModificationInstruction) i).subtype() ==
                        L2ModificationInstruction.L2SubType.VLAN_PUSH ||
                        ((L2ModificationInstruction) i).subtype() ==
                                L2ModificationInstruction.L2SubType.VLAN_POP)
                .findAny();

        if (!vlanIntruction.isPresent()) {
            fail(fwd, ObjectiveError.BADPARAMS);
            return;
        }

        L2ModificationInstruction vlanIns =
                (L2ModificationInstruction) vlanIntruction.get();

        if (vlanIns.subtype() == L2ModificationInstruction.L2SubType.VLAN_PUSH) {
            installUpstreamRules(fwd);
        } else if (vlanIns.subtype() == L2ModificationInstruction.L2SubType.VLAN_POP) {
            installDownstreamRules(fwd);
        } else {
            log.error("Unknown OLT operation: {}", fwd);
            fail(fwd, ObjectiveError.UNSUPPORTED);
            return;
        }

        pass(fwd);

    }

    @Override
    public void next(NextObjective nextObjective) {
        throw new UnsupportedOperationException("OLT does not next hop.");
    }

    private void installDownstreamRules(ForwardingObjective fwd) {
        List<Pair<Instruction, Instruction>> vlanOps =
                vlanOps(fwd,
                        L2ModificationInstruction.L2SubType.VLAN_POP);

        if (vlanOps == null) {
            return;
        }

        Instruction output = fetchOutput(fwd, "downstream");

        if (output == null) {
            return;
        }

        Pair<Instruction, Instruction> popAndRewrite = vlanOps.remove(0);

        TrafficSelector selector = fwd.selector();

        Criterion outerVlan = selector.getCriterion(Criterion.Type.VLAN_VID);
        Criterion innerVlan = selector.getCriterion(Criterion.Type.INNER_VLAN_VID);
        Criterion inport = selector.getCriterion(Criterion.Type.IN_PORT);

        if (outerVlan == null || innerVlan == null || inport == null) {
            log.error("Forwarding objective is underspecified: {}", fwd);
            fail(fwd, ObjectiveError.BADPARAMS);
            return;
        }

        Criterion innerVid = Criteria.matchVlanId(((VlanIdCriterion) innerVlan).vlanId());

        FlowRule.Builder outer = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .fromApp(appId)
                .makePermanent()
                .withPriority(fwd.priority())
                .withSelector(buildSelector(inport, outerVlan))
                .withTreatment(buildTreatment(popAndRewrite.getLeft(),
                                              Instructions.transition(QQ_TABLE)));

        FlowRule.Builder inner = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .fromApp(appId)
                .forTable(QQ_TABLE)
                .makePermanent()
                .withPriority(fwd.priority())
                .withSelector(buildSelector(inport, innerVid))
                .withTreatment(buildTreatment(popAndRewrite.getRight(),
                                              output));

        applyRules(fwd, inner, outer);

    }

    private void installUpstreamRules(ForwardingObjective fwd) {
        List<Pair<Instruction, Instruction>> vlanOps =
                vlanOps(fwd,
                        L2ModificationInstruction.L2SubType.VLAN_PUSH);

        if (vlanOps == null) {
            return;
        }

        Instruction output = fetchOutput(fwd, "upstream");

        if (output == null) {
            return;
        }

        Pair<Instruction, Instruction> innerPair = vlanOps.remove(0);

        Pair<Instruction, Instruction> outerPair = vlanOps.remove(0);

        FlowRule.Builder inner = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .fromApp(appId)
                .makePermanent()
                .withPriority(fwd.priority())
                .withSelector(fwd.selector())
                .withTreatment(buildTreatment(innerPair.getRight(),
                                              Instructions.transition(QQ_TABLE)));

        PortCriterion inPort = (PortCriterion)
                fwd.selector().getCriterion(Criterion.Type.IN_PORT);

        VlanId cVlanId = ((L2ModificationInstruction.ModVlanIdInstruction)
                innerPair.getRight()).vlanId();

        FlowRule.Builder outer = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .fromApp(appId)
                .forTable(QQ_TABLE)
                .makePermanent()
                .withPriority(fwd.priority())
                .withSelector(buildSelector(inPort,
                                            Criteria.matchVlanId(cVlanId)))
                .withTreatment(buildTreatment(outerPair.getLeft(),
                                              outerPair.getRight(),
                                              output));

        applyRules(fwd, inner, outer);

    }

    private Instruction fetchOutput(ForwardingObjective fwd, String direction) {
        Instruction output = fwd.treatment().allInstructions().stream()
                .filter(i -> i.type() == Instruction.Type.OUTPUT)
                .findFirst().orElse(null);

        if (output == null) {
            log.error("OLT {} rule has no output", direction);
            fail(fwd, ObjectiveError.BADPARAMS);
            return null;
        }
        return output;
    }

    private List<Pair<Instruction, Instruction>> vlanOps(ForwardingObjective fwd,
                                                         L2ModificationInstruction.L2SubType type) {

        List<Pair<Instruction, Instruction>> vlanOps = findVlanOps(
                fwd.treatment().allInstructions(), type);

        if (vlanOps == null) {
            String direction = type == L2ModificationInstruction.L2SubType.VLAN_POP
                    ? "downstream" : "upstream";
            log.error("Missing vlan operations in {} forwarding: {}", direction, fwd);
            fail(fwd, ObjectiveError.BADPARAMS);
            return null;
        }
        return vlanOps;
    }


    private List<Pair<Instruction, Instruction>> findVlanOps(List<Instruction> instructions,
             L2ModificationInstruction.L2SubType type) {

        List<Instruction> vlanPushs = findL2Instructions(
                type,
                instructions);
        List<Instruction> vlanSets = findL2Instructions(
                L2ModificationInstruction.L2SubType.VLAN_ID,
                instructions);

        if (vlanPushs.size() != vlanSets.size()) {
            return null;
        }

        List<Pair<Instruction, Instruction>> pairs = Lists.newArrayList();

        for (int i = 0; i < vlanPushs.size(); i++) {
            pairs.add(new ImmutablePair<>(vlanPushs.get(i), vlanSets.get(i)));
        }
        return pairs;
    }

    private List<Instruction> findL2Instructions(L2ModificationInstruction.L2SubType subType,
                                                 List<Instruction> actions) {
        return actions.stream()
                .filter(i -> i.type() == Instruction.Type.L2MODIFICATION)
                .filter(i -> ((L2ModificationInstruction) i).subtype() == subType)
                .collect(Collectors.toList());
    }

    private void provisionEapol(FilteringObjective filter,
                                EthTypeCriterion ethType,
                                Instructions.OutputInstruction output) {

        TrafficSelector selector = buildSelector(filter.key(), ethType);
        TrafficTreatment treatment = buildTreatment(output);
        buildAndApplyRule(filter, selector, treatment);

    }

    private void provisionIgmp(FilteringObjective filter, EthTypeCriterion ethType,
                               IPProtocolCriterion ipProto,
                               Instructions.OutputInstruction output) {
        TrafficSelector selector = buildSelector(filter.key(), ethType, ipProto);
        TrafficTreatment treatment = buildTreatment(output);
        buildAndApplyRule(filter, selector, treatment);
    }

    private void buildAndApplyRule(FilteringObjective filter, TrafficSelector selector,
                                   TrafficTreatment treatment) {
        FlowRule rule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .forTable(0)
                .fromApp(filter.appId())
                .makePermanent()
                .withSelector(selector)
                .withTreatment(treatment)
                .withPriority(filter.priority())
                .build();

        FlowRuleOperations.Builder opsBuilder = FlowRuleOperations.builder();

        switch (filter.type()) {
            case PERMIT:
                opsBuilder.add(rule);
                break;
            case DENY:
                opsBuilder.remove(rule);
                break;
            default:
                log.warn("Unknown filter type : {}", filter.type());
                fail(filter, ObjectiveError.UNSUPPORTED);
        }

        applyFlowRules(opsBuilder, filter);
    }

    private void applyRules(ForwardingObjective fwd,
                            FlowRule.Builder inner, FlowRule.Builder outer) {
        FlowRuleOperations.Builder builder = FlowRuleOperations.builder();
        switch (fwd.op()) {
            case ADD:
                builder.add(inner.build()).add(outer.build());
                break;
            case REMOVE:
                builder.remove(inner.build()).remove(outer.build());
                break;
            case ADD_TO_EXISTING:
                break;
            case REMOVE_FROM_EXISTING:
                break;
            default:
                log.warn("Unknown forwarding operation: {}", fwd.op());
        }

        applyFlowRules(builder, fwd);
    }

    private void applyFlowRules(FlowRuleOperations.Builder builder,
                                Objective objective) {
        flowRuleService.apply(builder.build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                pass(objective);
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                fail(objective, ObjectiveError.FLOWINSTALLATIONFAILED);
            }
        }));
    }

    private Criterion filterForCriterion(Collection<Criterion> criteria, Criterion.Type type) {
        return criteria.stream()
                .filter(c -> c.type().equals(type))
                .limit(1)
                .findFirst().orElse(null);
    }

    private TrafficSelector buildSelector(Criterion... criteria) {


        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();

        for (Criterion c : criteria) {
            sBuilder.add(c);
        }

        return sBuilder.build();
    }

    private TrafficTreatment buildTreatment(Instruction... instructions) {


        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();

        for (Instruction i : instructions) {
            tBuilder.add(i);
        }

        return tBuilder.build();
    }


    private void fail(Objective obj, ObjectiveError error) {
        if (obj.context().isPresent()) {
            obj.context().get().onError(obj, error);
        }
    }

    private void pass(Objective obj) {
        if (obj.context().isPresent()) {
            obj.context().get().onSuccess(obj);
        }
    }

    /**
     * Build a device description.
     *
     * @param deviceId a deviceId
     * @param key      the key of the annotation
     * @param value    the value for the annotation
     * @return a device description
     */
    private DeviceDescription description(DeviceId deviceId, String key, String value) {
        DeviceService deviceService = serviceDirectory.get(DeviceService.class);
        Device device = deviceService.getDevice(deviceId);

        checkNotNull(device, "Device not found in device service.");

        DefaultAnnotations.Builder builder = DefaultAnnotations.builder();
        if (value != null) {
            builder.set(key, value);
        } else {
            builder.remove(key);
        }
        return new DefaultDeviceDescription(device.id().uri(), device.type(),
                                            device.manufacturer(), device.hwVersion(),
                                            device.swVersion(), device.serialNumber(),
                                            device.chassisId(), builder.build());
    }

    /**
     * Simple ancillary provider used to annotate device.
     */
    private static final class AnnotationProvider
            extends AbstractProvider implements DeviceProvider {
        private AnnotationProvider() {
            super(PID);
        }

        @Override
        public void triggerProbe(DeviceId deviceId) {
        }

        @Override
        public void roleChanged(DeviceId deviceId, MastershipRole newRole) {
        }

        @Override
        public boolean isReachable(DeviceId deviceId) {
            return false;
        }
    }

}
