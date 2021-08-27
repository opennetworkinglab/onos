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

package org.onosproject.pipelines.fabric.impl.behaviour.pipeliner;

import com.google.common.collect.Lists;
import org.onlab.packet.Ethernet;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.EthCriterion;
import org.onosproject.net.flow.criteria.PiCriterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.criteria.VlanIdCriterion;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.L2SubType;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.ModVlanIdInstruction;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.onosproject.net.pi.model.PiTableId;
import org.onosproject.net.pi.runtime.PiAction;
import org.onosproject.net.pi.runtime.PiActionParam;
import org.onosproject.pipelines.fabric.impl.behaviour.FabricCapabilities;
import org.onosproject.pipelines.fabric.FabricConstants;
import org.onosproject.pipelines.fabric.impl.behaviour.FabricUtils;

import java.util.Collection;
import java.util.List;

import static java.lang.String.format;
import static org.onosproject.net.flow.criteria.Criterion.Type.INNER_VLAN_VID;
import static org.onosproject.net.flow.criteria.Criterion.Type.VLAN_VID;
import static org.onosproject.net.flow.instructions.L2ModificationInstruction.L2SubType.VLAN_ID;
import static org.onosproject.net.flow.instructions.L2ModificationInstruction.L2SubType.VLAN_POP;
import static org.onosproject.net.pi.model.PiPipelineInterpreter.PiInterpreterException;
import static org.onosproject.pipelines.fabric.impl.behaviour.Constants.CLEANUP_DOUBLE_TAGGED_HOST_ENTRIES;
import static org.onosproject.pipelines.fabric.impl.behaviour.Constants.ETH_TYPE_EXACT_MASK;
import static org.onosproject.pipelines.fabric.impl.behaviour.Constants.FWD_MPLS;
import static org.onosproject.pipelines.fabric.impl.behaviour.Constants.FWD_IPV4_ROUTING;
import static org.onosproject.pipelines.fabric.impl.behaviour.Constants.FWD_IPV6_ROUTING;
import static org.onosproject.pipelines.fabric.impl.behaviour.Constants.INTERFACE_CONFIG_UPDATE;
import static org.onosproject.pipelines.fabric.impl.behaviour.Constants.ONE;
import static org.onosproject.pipelines.fabric.impl.behaviour.Constants.ZERO;
import static org.onosproject.pipelines.fabric.impl.behaviour.FabricUtils.isSrMetadataSet;
import static org.onosproject.pipelines.fabric.impl.behaviour.FabricUtils.isValidSrMetadata;
import static org.onosproject.pipelines.fabric.impl.behaviour.FabricUtils.portType;
import static org.onosproject.pipelines.fabric.impl.behaviour.FabricUtils.l2InstructionOrFail;
import static org.onosproject.pipelines.fabric.impl.behaviour.FabricUtils.criterion;
import static org.onosproject.pipelines.fabric.impl.behaviour.FabricUtils.l2Instruction;

/**
 * ObjectiveTranslator implementation for FilteringObjective.
 */
class FilteringObjectiveTranslator
        extends AbstractObjectiveTranslator<FilteringObjective> {

    private static final PiAction DENY = PiAction.builder()
            .withId(FabricConstants.FABRIC_INGRESS_FILTERING_DENY)
            .build();

    FilteringObjectiveTranslator(DeviceId deviceId, FabricCapabilities capabilities) {
        super(deviceId, capabilities);
    }

    @Override
    public ObjectiveTranslation doTranslate(FilteringObjective obj)
            throws FabricPipelinerException {

        final ObjectiveTranslation.Builder resultBuilder =
                ObjectiveTranslation.builder();

        if (obj.key() == null || obj.key().type() != Criterion.Type.IN_PORT) {
            throw new FabricPipelinerException(
                    format("Unsupported or missing filtering key: key=%s", obj.key()),
                    ObjectiveError.BADPARAMS);
        }

        if (!isValidSrMetadata(obj)) {
            throw new FabricPipelinerException(
                    format("Unsupported metadata configuration: metadata=%s", obj.meta()),
                    ObjectiveError.BADPARAMS);
        }

        final PortCriterion inPort = (PortCriterion) obj.key();

        final VlanIdCriterion outerVlan = (VlanIdCriterion) criterion(
                obj.conditions(), Criterion.Type.VLAN_VID);
        final VlanIdCriterion innerVlan = (VlanIdCriterion) criterion(
                obj.conditions(), Criterion.Type.INNER_VLAN_VID);
        final EthCriterion ethDst = (EthCriterion) criterion(
                obj.conditions(), Criterion.Type.ETH_DST);
        final EthCriterion ethDstMasked = (EthCriterion) criterion(
                obj.conditions(), Criterion.Type.ETH_DST_MASKED);

        ingressPortVlanRule(obj, inPort, outerVlan, innerVlan, resultBuilder);
        if (shouldModifyFwdClassifierTable(obj)) {
            fwdClassifierRules(obj, inPort, ethDst, ethDstMasked, resultBuilder);
        } else {
            log.debug("Skipping fwd classifier rules for device {}.", deviceId);
        }
        return resultBuilder.build();
    }

    private boolean shouldModifyFwdClassifierTable(FilteringObjective obj) {
        // NOTE: in fabric pipeline the forwarding classifier acts similarly
        // to the TMAC table of OFDPA that matches on input port.
        // NOTE: that SR signals when it is a port update event by not setting
        // the INTERFACE_CONFIG_UPDATE metadata. During the INTERFACE_CONFIG_UPDATE
        // there is no need to add/remove rules in the fwd_classifier table.
        // NOTE: that in scenarios like (T, N) -> T where we remove only the native
        // VLAN there is not an ADD following the remove.

        // Forwarding classifier rules should be added/removed to translation when:
        // - the operation is ADD
        //     AND it is a port update event (ADD or UPDATE) OR
        // - it doesn't refer to double tagged traffic
        //     AND it is a port REMOVE event OR
        // - it refers to double tagged traffic
        //     and SR is triggering the removal of forwarding classifier rules.
        return (obj.op() == Objective.Operation.ADD && !isSrMetadataSet(obj, INTERFACE_CONFIG_UPDATE)) ||
                (!isDoubleTagged(obj) && !isSrMetadataSet(obj, INTERFACE_CONFIG_UPDATE)) ||
                (isDoubleTagged(obj) && isSrMetadataSet(obj, CLEANUP_DOUBLE_TAGGED_HOST_ENTRIES));
    }

    private boolean isDoubleTagged(FilteringObjective obj) {
        return obj.meta() != null &&
                FabricUtils.l2Instruction(obj.meta(), L2SubType.VLAN_POP) != null &&
                FabricUtils.criterion(obj.conditions(), VLAN_VID) != null &&
                FabricUtils.criterion(obj.conditions(), INNER_VLAN_VID) != null;
    }

    private void ingressPortVlanRule(
            FilteringObjective obj,
            Criterion inPortCriterion,
            VlanIdCriterion outerVlanCriterion,
            VlanIdCriterion innerVlanCriterion,
            ObjectiveTranslation.Builder resultBuilder)
            throws FabricPipelinerException {

        final boolean outerVlanValid = outerVlanCriterion != null
                && !outerVlanCriterion.vlanId().equals(VlanId.NONE);
        final boolean innerVlanValid = innerVlanCriterion != null
                && !innerVlanCriterion.vlanId().equals(VlanId.NONE);

        if (innerVlanValid && !capabilities.supportDoubleVlanTerm()) {
            throw new FabricPipelinerException(
                    "Found 2 VLAN IDs, but the pipeline does not support double VLAN termination",
                    ObjectiveError.UNSUPPORTED);
        }

        final PiCriterion piCriterion = PiCriterion.builder()
                .matchExact(FabricConstants.HDR_VLAN_IS_VALID, outerVlanValid ? ONE : ZERO)
                .build();

        final TrafficSelector.Builder selector = DefaultTrafficSelector.builder()
                .add(inPortCriterion)
                .add(piCriterion);
        if (outerVlanValid) {
            selector.add(outerVlanCriterion);
        }
        if (innerVlanValid) {
            selector.add(innerVlanCriterion);
        }

        final TrafficTreatment.Builder treatmentBuilder = DefaultTrafficTreatment.builder();
        if (obj.type().equals(FilteringObjective.Type.DENY)) {
            treatmentBuilder.piTableAction(DENY);
        } else {
            // FIXME SDFAB-52 to complete the work on metadata
            Byte portType = portType(obj);
            if (portType == null) {
                throw new FabricPipelinerException(
                        format("Unsupported port_type configuration: metadata=%s", obj.meta()),
                        ObjectiveError.BADPARAMS);
            }
            try {
                treatmentBuilder.piTableAction(mapFilteringTreatment(obj.meta(),
                        FabricConstants.FABRIC_INGRESS_FILTERING_INGRESS_PORT_VLAN, portType));
            } catch (PiInterpreterException ex) {
                throw new FabricPipelinerException(format("Unable to map treatment for table '%s': %s",
                        FabricConstants.FABRIC_INGRESS_FILTERING_INGRESS_PORT_VLAN,
                        ex.getMessage()), ObjectiveError.UNSUPPORTED);
            }
        }
        resultBuilder.addFlowRule(flowRule(
                obj, FabricConstants.FABRIC_INGRESS_FILTERING_INGRESS_PORT_VLAN,
                selector.build(), treatmentBuilder.build()));
    }

    private PiAction mapFilteringTreatment(TrafficTreatment treatment, PiTableId tableId, byte portType)
            throws PiInterpreterException {
        if (treatment == null) {
            treatment = DefaultTrafficTreatment.emptyTreatment();
        }
        // VLAN_POP action is equivalent to the permit action (VLANs pop is done anyway)
        if (isFilteringNoAction(treatment) || isFilteringPopAction(treatment)) {
            // Permit action if table is ingress_port_vlan;
            return PiAction.builder()
                    .withId(FabricConstants.FABRIC_INGRESS_FILTERING_PERMIT)
                    .withParameter(new PiActionParam(FabricConstants.PORT_TYPE, portType))
                    .build();
        }

        final ModVlanIdInstruction setVlanInst = (ModVlanIdInstruction) l2InstructionOrFail(
                treatment, VLAN_ID, tableId);
        return PiAction.builder()
                .withId(FabricConstants.FABRIC_INGRESS_FILTERING_PERMIT_WITH_INTERNAL_VLAN)
                .withParameter(new PiActionParam(FabricConstants.VLAN_ID, setVlanInst.vlanId().toShort()))
                .withParameter(new PiActionParam(FabricConstants.PORT_TYPE, portType))
                .build();
    }

    // NOTE: we use clearDeferred to signal when there are no more ports associated to a given vlan
    private static boolean isFilteringNoAction(TrafficTreatment treatment) {
        return treatment.equals(DefaultTrafficTreatment.emptyTreatment()) ||
                (treatment.allInstructions().isEmpty()) ||
                (treatment.allInstructions().size() == 1 && treatment.writeMetadata() != null);
    }

    private boolean isFilteringPopAction(TrafficTreatment treatment) {
        return l2Instruction(treatment, VLAN_POP) != null;
    }

    private void fwdClassifierRules(
            FilteringObjective obj,
            PortCriterion inPortCriterion,
            EthCriterion ethDstCriterion,
            EthCriterion ethDstMaskedCriterion,
            ObjectiveTranslation.Builder resultBuilder)
            throws FabricPipelinerException {

        final List<FlowRule> flowRules = Lists.newArrayList();

        final PortNumber inPort = inPortCriterion.port();
        if (ethDstCriterion == null) {
            if (ethDstMaskedCriterion == null) {
                // No match. Do bridging (default action).
                return;
            }
            // Masked fwd classifier rule
            final MacAddress dstMac = ethDstMaskedCriterion.mac();
            final MacAddress dstMacMask = ethDstMaskedCriterion.mask();
            flowRules.add(maskedFwdClassifierRule(inPort, dstMac, dstMacMask, obj));
        } else {
            final MacAddress dstMac = ethDstCriterion.mac();
            flowRules.addAll(ipFwdClassifierRules(inPort, dstMac, obj));
            flowRules.addAll(mplsFwdClassifierRules(inPort, dstMac, obj));
        }

        for (FlowRule f : flowRules) {
            resultBuilder.addFlowRule(f);
        }
    }

    private FlowRule maskedFwdClassifierRule(
            PortNumber inPort, MacAddress dstMac, MacAddress dstMacMask,
            FilteringObjective obj)
            throws FabricPipelinerException {
        final TrafficTreatment treatment;
        final short ethType;
        if (dstMac.equals(MacAddress.IPV4_MULTICAST)
                && dstMacMask.equals(MacAddress.IPV4_MULTICAST_MASK)) {
            treatment = fwdClassifierTreatment(FWD_IPV4_ROUTING);
            ethType = Ethernet.TYPE_IPV4;
        } else if (dstMac.equals(MacAddress.IPV6_MULTICAST)
                && dstMacMask.equals(MacAddress.IPV6_MULTICAST_MASK)) {
            treatment = fwdClassifierTreatment(FWD_IPV6_ROUTING);
            ethType = Ethernet.TYPE_IPV6;
        } else {
            throw new FabricPipelinerException(format(
                    "Unsupported masked Ethernet address for fwd " +
                            "classifier rule (mac=%s, mask=%s)",
                    dstMac, dstMacMask));
        }
        return fwdClassifierRule(inPort, ethType, dstMac, dstMacMask, treatment, obj);
    }

    private Collection<FlowRule> ipFwdClassifierRules(
            PortNumber inPort, MacAddress dstMac, FilteringObjective obj)
            throws FabricPipelinerException {
        final Collection<FlowRule> flowRules = Lists.newArrayList();
        flowRules.add(fwdClassifierRule(
                inPort, Ethernet.TYPE_IPV4, dstMac, null,
                fwdClassifierTreatment(FWD_IPV4_ROUTING), obj));
        flowRules.add(fwdClassifierRule(
                inPort, Ethernet.TYPE_IPV6, dstMac, null,
                fwdClassifierTreatment(FWD_IPV6_ROUTING), obj));
        return flowRules;
    }

    private Collection<FlowRule> mplsFwdClassifierRules(
            PortNumber inPort, MacAddress dstMac, FilteringObjective obj)
            throws FabricPipelinerException {
        // Forwarding classifier for MPLS is composed of 2 rules
        // with higher priority wrt standard forwarding classifier rules,
        // this is due to overlap on ternary matching.
        TrafficTreatment treatment = fwdClassifierTreatment(FWD_MPLS);
        final PiCriterion ethTypeMplsIpv4 = PiCriterion.builder()
                .matchTernary(FabricConstants.HDR_ETH_TYPE,
                              Ethernet.MPLS_UNICAST, ETH_TYPE_EXACT_MASK)
                .matchExact(FabricConstants.HDR_IP_ETH_TYPE,
                            Ethernet.TYPE_IPV4)
                .build();
        final TrafficSelector selectorMplsIpv4 = DefaultTrafficSelector.builder()
                .matchInPort(inPort)
                .matchPi(ethTypeMplsIpv4)
                .matchEthDstMasked(dstMac, MacAddress.EXACT_MASK)
                .build();

        final PiCriterion ethTypeMplsIpv6 = PiCriterion.builder()
                .matchTernary(FabricConstants.HDR_ETH_TYPE,
                              Ethernet.MPLS_UNICAST, ETH_TYPE_EXACT_MASK)
                .matchExact(FabricConstants.HDR_IP_ETH_TYPE,
                            Ethernet.TYPE_IPV6)
                .build();
        final TrafficSelector selectorMplsIpv6 = DefaultTrafficSelector.builder()
                .matchInPort(inPort)
                .matchPi(ethTypeMplsIpv6)
                .matchEthDstMasked(dstMac, MacAddress.EXACT_MASK)
                .build();

        return List.of(
                flowRule(obj, FabricConstants.FABRIC_INGRESS_FILTERING_FWD_CLASSIFIER,
                         selectorMplsIpv4, treatment, obj.priority() + 1),
                flowRule(obj, FabricConstants.FABRIC_INGRESS_FILTERING_FWD_CLASSIFIER,
                         selectorMplsIpv6, treatment, obj.priority() + 1)
        );
    }

    private FlowRule fwdClassifierRule(
            PortNumber inPort, short ethType, MacAddress dstMac, MacAddress dstMacMask,
            TrafficTreatment treatment, FilteringObjective obj)
            throws FabricPipelinerException {
        // Match on ip_eth_type that is the eth_type of the L3 protocol.
        // i.e., if the packet has an IP header, ip_eth_type should
        // contain the corresponding eth_type (for IPv4 or IPv6)
        final PiCriterion ethTypeCriterion = PiCriterion.builder()
                .matchExact(FabricConstants.HDR_IP_ETH_TYPE, ethType)
                .build();
        final TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchInPort(inPort)
                .matchPi(ethTypeCriterion)
                .matchEthDstMasked(dstMac, dstMacMask == null
                        ? MacAddress.EXACT_MASK : dstMacMask)
                .build();
        return flowRule(
                obj, FabricConstants.FABRIC_INGRESS_FILTERING_FWD_CLASSIFIER,
                selector, treatment);
    }

    private TrafficTreatment fwdClassifierTreatment(byte fwdType) {
        final PiActionParam param = new PiActionParam(FabricConstants.FWD_TYPE, fwdType);
        final PiAction action = PiAction.builder()
                .withId(FabricConstants.FABRIC_INGRESS_FILTERING_SET_FORWARDING_TYPE)
                .withParameter(param)
                .build();
        return DefaultTrafficTreatment.builder()
                .piTableAction(action)
                .build();

    }
}
