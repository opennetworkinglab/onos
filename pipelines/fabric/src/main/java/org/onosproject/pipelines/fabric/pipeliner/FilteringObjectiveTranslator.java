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

package org.onosproject.pipelines.fabric.pipeliner;

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
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.onosproject.net.pi.runtime.PiAction;
import org.onosproject.net.pi.runtime.PiActionParam;
import org.onosproject.pipelines.fabric.FabricCapabilities;
import org.onosproject.pipelines.fabric.FabricConstants;

import java.util.Collection;
import java.util.List;

import static java.lang.String.format;
import static org.onosproject.pipelines.fabric.FabricUtils.criterion;

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

    private static final PiAction DENY = PiAction.builder()
            .withId(FabricConstants.FABRIC_INGRESS_FILTERING_DENY)
            .build();

    private static final PiCriterion VLAN_VALID = PiCriterion.builder()
            .matchExact(FabricConstants.HDR_VLAN_IS_VALID, ONE)
            .build();
    private static final PiCriterion VLAN_INVALID = PiCriterion.builder()
            .matchExact(FabricConstants.HDR_VLAN_IS_VALID, ZERO)
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
        final VlanIdCriterion vlan = (VlanIdCriterion) criterion(
                obj.conditions(), Criterion.Type.VLAN_VID);
        final EthCriterion ethDst = (EthCriterion) criterion(
                obj.conditions(), Criterion.Type.ETH_DST);
        final EthCriterion ethDstMasked = (EthCriterion) criterion(
                obj.conditions(), Criterion.Type.ETH_DST_MASKED);

        ingressPortVlanRule(obj, inPort, vlan, resultBuilder);
        fwdClassifierRules(obj, inPort, ethDst, ethDstMasked, resultBuilder);

        return resultBuilder.build();
    }

    private void ingressPortVlanRule(
            FilteringObjective obj,
            Criterion inPortCriterion,
            VlanIdCriterion vlanCriterion,
            ObjectiveTranslation.Builder resultBuilder)
            throws FabricPipelinerException {

        final boolean vlanValid = vlanCriterion != null
                && !vlanCriterion.vlanId().equals(VlanId.NONE);

        final TrafficSelector.Builder selector = DefaultTrafficSelector.builder()
                .add(inPortCriterion)
                .add(vlanValid ? VLAN_VALID : VLAN_INVALID);
        if (vlanValid) {
            selector.add(vlanCriterion);
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
            flowRules.add(mplsFwdClassifierRule(inPort, dstMac, obj));
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

    private FlowRule mplsFwdClassifierRule(
            PortNumber inPort, MacAddress dstMac, FilteringObjective obj)
            throws FabricPipelinerException {
        return fwdClassifierRule(
                inPort, Ethernet.MPLS_UNICAST, dstMac, null,
                fwdClassifierTreatment(FWD_MPLS), obj);
    }

    private FlowRule fwdClassifierRule(
            PortNumber inPort, short ethType, MacAddress dstMac, MacAddress dstMacMask,
            TrafficTreatment treatment, FilteringObjective obj)
            throws FabricPipelinerException {
        final TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchInPort(inPort)
                .matchEthType(ethType)
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
