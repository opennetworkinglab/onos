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

import com.google.common.collect.ImmutableSet;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.EthCriterion;
import org.onosproject.net.flow.criteria.IPCriterion;
import org.onosproject.net.flow.criteria.MplsCriterion;
import org.onosproject.net.flow.criteria.VlanIdCriterion;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.onosproject.net.pi.runtime.PiAction;
import org.onosproject.net.pi.runtime.PiActionParam;
import org.onosproject.pipelines.fabric.FabricConstants;
import org.slf4j.Logger;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Handling forwarding objective for fabric pipeliner.
 */
public class FabricForwardingPipeliner {
    private static final Logger log = getLogger(FabricForwardingPipeliner.class);

    protected DeviceId deviceId;

    public FabricForwardingPipeliner(DeviceId deviceId) {
        this.deviceId = deviceId;
    }

    public PipelinerTranslationResult forward(ForwardingObjective forwardObjective) {
        PipelinerTranslationResult.Builder resultBuilder = PipelinerTranslationResult.builder();
        if (forwardObjective.flag() == ForwardingObjective.Flag.VERSATILE) {
            processVersatileFwd(forwardObjective, resultBuilder);
        } else {
            processSpecificFwd(forwardObjective, resultBuilder);
        }
        return resultBuilder.build();
    }

    private void processVersatileFwd(ForwardingObjective fwd,
                                     PipelinerTranslationResult.Builder resultBuilder) {
        // TODO: Move IPv6 match to different ACL table

        boolean unsupported = fwd.selector().criteria().stream()
                .anyMatch(criterion -> criterion.type() == Criterion.Type.IPV6_DST);
        unsupported |= fwd.selector().criteria().stream()
                .anyMatch(criterion -> criterion.type() == Criterion.Type.IPV6_SRC);

        if (unsupported) {
            resultBuilder.setError(ObjectiveError.UNSUPPORTED);
            return;
        }

        // program ACL table only
        FlowRule flowRule = DefaultFlowRule.builder()
                .withSelector(fwd.selector())
                .withTreatment(fwd.treatment())
                .forTable(FabricConstants.FABRIC_INGRESS_FORWARDING_ACL)
                .withPriority(fwd.priority())
                .forDevice(deviceId)
                .makePermanent()
                .fromApp(fwd.appId())
                .build();
        resultBuilder.addFlowRule(flowRule);
    }

    private void processSpecificFwd(ForwardingObjective fwd,
                                    PipelinerTranslationResult.Builder resultBuilder) {
        TrafficSelector selector = fwd.selector();
        TrafficSelector meta = fwd.meta();

        ImmutableSet.Builder<Criterion> criterionSetBuilder = ImmutableSet.builder();
        criterionSetBuilder.addAll(selector.criteria());

        if (meta != null) {
            criterionSetBuilder.addAll(meta.criteria());
        }

        Set<Criterion> criteria = criterionSetBuilder.build();

        VlanIdCriterion vlanIdCriterion = null;
        EthCriterion ethDstCriterion = null;
        IPCriterion ipDstCriterion = null;
        MplsCriterion mplsCriterion = null;

        for (Criterion criterion : criteria) {
            switch (criterion.type()) {
                case ETH_DST:
                    ethDstCriterion = (EthCriterion) criterion;
                    break;
                case VLAN_VID:
                    vlanIdCriterion = (VlanIdCriterion) criterion;
                    break;
                case IPV4_DST:
                    ipDstCriterion = (IPCriterion) criterion;
                    break;
                case MPLS_LABEL:
                    mplsCriterion = (MplsCriterion) criterion;
                    break;
                case ETH_TYPE:
                case MPLS_BOS:
                    // do nothing
                    break;
                default:
                    log.warn("Unsupported criterion {}", criterion);
                    break;
            }
        }

        ForwardingFunctionType forwardingFunctionType =
                ForwardingFunctionType.getForwardingFunctionType(fwd);
        switch (forwardingFunctionType) {
            case L2_UNICAST:
                processL2UnicastRule(vlanIdCriterion, ethDstCriterion, fwd, resultBuilder);
                break;
            case L2_BROADCAST:
                processL2BroadcastRule(vlanIdCriterion, fwd, resultBuilder);
                break;
            case IPV4_UNICAST:
                processIpv4UnicastRule(ipDstCriterion, fwd, resultBuilder);
                break;
            case MPLS:
                processMplsRule(mplsCriterion, fwd, resultBuilder);
                break;
            case IPV4_MULTICAST:
            case IPV6_UNICAST:
            case IPV6_MULTICAST:
            default:
                log.warn("Unsupported forwarding function type {}", criteria);
                resultBuilder.setError(ObjectiveError.UNSUPPORTED);
                break;
        }
    }

    // L2 Unicast: learnt mac address + vlan
    private void processL2UnicastRule(VlanIdCriterion vlanIdCriterion,
                                      EthCriterion ethDstCriterion,
                                      ForwardingObjective fwd,
                                      PipelinerTranslationResult.Builder resultBuilder) {
        checkNotNull(vlanIdCriterion, "VlanId criterion should not be null");
        checkNotNull(ethDstCriterion, "EthDst criterion should not be null");

        VlanId vlanId = vlanIdCriterion.vlanId();
        MacAddress ethDst = ethDstCriterion.mac();

        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchVlanId(vlanId)
                .matchEthDst(ethDst)
                .build();
        TrafficTreatment treatment = fwd.treatment();
        if (fwd.nextId() != null) {
            treatment = buildSetNextIdTreatment(fwd.nextId());
        }

        FlowRule flowRule = DefaultFlowRule.builder()
                .withSelector(selector)
                .withTreatment(treatment)
                .fromApp(fwd.appId())
                .withPriority(fwd.priority())
                .makePermanent()
                .forDevice(deviceId)
                .forTable(FabricConstants.FABRIC_INGRESS_FORWARDING_BRIDGING)
                .build();

        resultBuilder.addFlowRule(flowRule);
    }

    private void processL2BroadcastRule(VlanIdCriterion vlanIdCriterion,
                                        ForwardingObjective fwd,
                                        PipelinerTranslationResult.Builder resultBuilder) {
        checkNotNull(vlanIdCriterion, "VlanId criterion should not be null");

        VlanId vlanId = vlanIdCriterion.vlanId();

        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchVlanId(vlanId)
                .build();
        TrafficTreatment treatment = fwd.treatment();
        if (fwd.nextId() != null) {
            treatment = buildSetNextIdTreatment(fwd.nextId());
        }
        FlowRule flowRule = DefaultFlowRule.builder()
                .withSelector(selector)
                .withTreatment(treatment)
                .fromApp(fwd.appId())
                .withPriority(fwd.priority())
                .makePermanent()
                .forDevice(deviceId)
                .forTable(FabricConstants.FABRIC_INGRESS_FORWARDING_BRIDGING)
                .build();

        resultBuilder.addFlowRule(flowRule);
    }

    private void processIpv4UnicastRule(IPCriterion ipDstCriterion, ForwardingObjective fwd,
                                        PipelinerTranslationResult.Builder resultBuilder) {
        checkNotNull(ipDstCriterion, "IP dst criterion should not be null");
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchIPDst(ipDstCriterion.ip())
                .build();
        TrafficTreatment treatment = fwd.treatment();
        if (fwd.nextId() != null) {
            treatment = buildSetNextIdTreatment(fwd.nextId());
        }
        FlowRule flowRule = DefaultFlowRule.builder()
                .withSelector(selector)
                .withTreatment(treatment)
                .fromApp(fwd.appId())
                .withPriority(fwd.priority())
                .makePermanent()
                .forDevice(deviceId)
                .forTable(FabricConstants.FABRIC_INGRESS_FORWARDING_UNICAST_V4)
                .build();

        resultBuilder.addFlowRule(flowRule);
    }

    private void processMplsRule(MplsCriterion mplsCriterion, ForwardingObjective fwd,
                                 PipelinerTranslationResult.Builder resultBuilder) {
        checkNotNull(mplsCriterion, "Mpls criterion should not be null");
        TrafficTreatment treatment;

        treatment = fwd.treatment();
        if (fwd.nextId() != null) {
            PiActionParam nextIdParam = new PiActionParam(FabricConstants.NEXT_ID,
                                                          ImmutableByteSequence.copyFrom(fwd.nextId().byteValue()));
            PiAction nextIdAction = PiAction.builder()
                    .withId(FabricConstants.FABRIC_INGRESS_FORWARDING_POP_MPLS_AND_NEXT)
                    .withParameter(nextIdParam)
                    .build();
            treatment = DefaultTrafficTreatment.builder()
                    .piTableAction(nextIdAction)
                    .build();
        }

        TrafficSelector selector = DefaultTrafficSelector.builder()
                .add(mplsCriterion)
                .build();

        FlowRule flowRule = DefaultFlowRule.builder()
                .withSelector(selector)
                .withTreatment(treatment)
                .fromApp(fwd.appId())
                .withPriority(fwd.priority())
                .makePermanent()
                .forDevice(deviceId)
                .forTable(FabricConstants.FABRIC_INGRESS_FORWARDING_MPLS)
                .build();

        resultBuilder.addFlowRule(flowRule);
    }

    /**
     * Builds treatment with set_next_id action, returns empty treatment
     * if next id is null.
     *
     * @param nextId the next id for action
     * @return treatment with set_next_id action; empty treatment if next id is null
     */
    private static TrafficTreatment buildSetNextIdTreatment(Integer nextId) {
        PiActionParam nextIdParam = new PiActionParam(FabricConstants.NEXT_ID,
                                                      ImmutableByteSequence.copyFrom(nextId.byteValue()));
        PiAction nextIdAction = PiAction.builder()
                .withId(FabricConstants.FABRIC_INGRESS_FORWARDING_SET_NEXT_ID)
                .withParameter(nextIdParam)
                .build();

        return DefaultTrafficTreatment.builder()
                .piTableAction(nextIdAction)
                .build();
    }
}
