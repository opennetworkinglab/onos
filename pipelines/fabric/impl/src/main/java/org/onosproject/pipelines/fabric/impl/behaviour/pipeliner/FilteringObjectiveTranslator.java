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
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.flow.instructions.L2ModificationInstruction;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.onosproject.net.pi.runtime.PiAction;
import org.onosproject.net.pi.runtime.PiActionParam;
import org.onosproject.pipelines.fabric.impl.behaviour.FabricCapabilities;
import org.onosproject.pipelines.fabric.impl.behaviour.FabricConstants;
import org.onosproject.pipelines.fabric.impl.behaviour.FabricUtils;

import java.util.Collection;
import java.util.List;

import static java.lang.String.format;
import static org.onosproject.net.flow.criteria.Criterion.Type.INNER_VLAN_VID;
import static org.onosproject.net.flow.criteria.Criterion.Type.VLAN_VID;
import static org.onosproject.pipelines.fabric.impl.behaviour.FabricUtils.criterion;

/**
 * ObjectiveTranslator implementation for FilteringObjective.
 */
class FilteringObjectiveTranslator
        extends AbstractObjectiveTranslator<FilteringObjective> {

    // Forwarding types from fabric.p4.
    static final byte FWD_MPLS = 1;
    static final byte FWD_IPV4_ROUTING = 2;
    static final byte FWD_IPV6_ROUTING = 3;

    private static final byte[] ONE = new byte[]{1};
    private static final byte[] ZERO = new byte[]{0};

    private static final short ETH_TYPE_EXACT_MASK = (short) 0xFFFF;

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
        if (shouldAddFwdClassifierRule(obj)) {
            fwdClassifierRules(obj, inPort, ethDst, ethDstMasked, resultBuilder);
        } else {
            log.debug("Skipping fwd classifier rules for device {}.", deviceId);
        }
        return resultBuilder.build();
    }

    private boolean shouldAddFwdClassifierRule(FilteringObjective obj) {
        // NOTE: in fabric pipeline the forwarding classifier acts similarly
        // to the TMAC table of OFDPA that matches on input port.

        // Forwarding classifier rules should be added to translation when:
        // - the operation is ADD OR
        // - it doesn't refer to double tagged traffic OR
        // - it refers to double tagged traffic
        //     and SR is triggering the removal of forwarding classifier rules.
        return obj.op() == Objective.Operation.ADD ||
                !isDoubleTagged(obj) ||
                (isDoubleTagged(obj) && isLastDoubleTaggedForPort(obj));
    }

    /**
     * Check if the given filtering objective is the last filtering objective
     * for a double-tagged host for a specific port.
     * <p>
     * {@see org.onosproject.segmentrouting.RoutingRulePopulator#buildDoubleTaggedFilteringObj()}
     * {@see org.onosproject.segmentrouting.RoutingRulePopulator#processDoubleTaggedFilter()}
     *
     * @param obj Filtering objective to check.
     * @return True if SR is signaling to remove the forwarding classifier rule,
     * false otherwise.
     */
    private boolean isLastDoubleTaggedForPort(FilteringObjective obj) {
        Instructions.MetadataInstruction meta = obj.meta().writeMetadata();
        // SR is setting this metadata when a double tagged filtering objective
        // is removed and no other hosts is sharing the same input port.
        return (meta != null && (meta.metadata() & meta.metadataMask()) == 1);
    }

    private boolean isDoubleTagged(FilteringObjective obj) {
        return obj.meta() != null &&
                FabricUtils.l2Instruction(obj.meta(), L2ModificationInstruction.L2SubType.VLAN_POP) != null &&
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

        final TrafficTreatment treatment;
        if (obj.type().equals(FilteringObjective.Type.DENY)) {
            treatment = DefaultTrafficTreatment.builder()
                    .piTableAction(DENY)
                    .build();
        } else {
            treatment = obj.meta() == null
                    ? DefaultTrafficTreatment.emptyTreatment() : obj.meta();
        }
        resultBuilder.addFlowRule(flowRule(
                obj, FabricConstants.FABRIC_INGRESS_FILTERING_INGRESS_PORT_VLAN,
                selector.build(), treatment));
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
