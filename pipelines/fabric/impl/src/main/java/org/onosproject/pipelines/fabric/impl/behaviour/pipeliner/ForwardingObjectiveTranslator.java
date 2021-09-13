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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.onlab.packet.MacAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.EthCriterion;
import org.onosproject.net.flow.criteria.IPCriterion;
import org.onosproject.net.flow.criteria.MplsCriterion;
import org.onosproject.net.flow.criteria.PiCriterion;
import org.onosproject.net.flow.criteria.VlanIdCriterion;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.onosproject.net.group.DefaultGroupDescription;
import org.onosproject.net.group.DefaultGroupKey;
import org.onosproject.net.group.GroupBucket;
import org.onosproject.net.group.GroupBuckets;
import org.onosproject.net.group.GroupDescription;
import org.onosproject.net.group.GroupKey;
import org.onosproject.net.pi.model.PiActionId;
import org.onosproject.net.pi.model.PiTableId;
import org.onosproject.net.pi.runtime.PiAction;
import org.onosproject.net.pi.runtime.PiActionParam;
import org.onosproject.pipelines.fabric.impl.behaviour.FabricCapabilities;
import org.onosproject.pipelines.fabric.FabricConstants;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.onosproject.net.group.DefaultGroupBucket.createCloneGroupBucket;
import static org.onosproject.pipelines.fabric.impl.behaviour.Constants.PAIR_PORT;
import static org.onosproject.pipelines.fabric.impl.behaviour.FabricUtils.isSrMetadataSet;
import static org.onosproject.pipelines.fabric.impl.behaviour.FabricUtils.isValidSrMetadata;
import static org.onosproject.pipelines.fabric.impl.behaviour.FabricUtils.portType;
import static org.onosproject.pipelines.fabric.impl.behaviour.FabricUtils.criterionNotNull;
import static org.onosproject.pipelines.fabric.impl.behaviour.FabricUtils.outputPort;
import static org.onosproject.pipelines.fabric.impl.behaviour.Constants.PORT_TYPE_MASK;
import static org.onosproject.pipelines.fabric.impl.behaviour.Constants.PORT_TYPE_EDGE;
import static org.onosproject.pipelines.fabric.impl.behaviour.Constants.PORT_TYPE_INFRA;


/**
 * ObjectiveTranslator implementation ForwardingObjective.
 */
class ForwardingObjectiveTranslator
        extends AbstractObjectiveTranslator<ForwardingObjective> {

    //FIXME: Max number supported by PI
    static final int CLONE_TO_CPU_ID = 511;

    private static final Set<Criterion.Type> ACL_CRITERIA = ImmutableSet.of(
            Criterion.Type.IN_PORT,
            Criterion.Type.IN_PHY_PORT,
            Criterion.Type.ETH_DST,
            Criterion.Type.ETH_DST_MASKED,
            Criterion.Type.ETH_SRC,
            Criterion.Type.ETH_SRC_MASKED,
            Criterion.Type.ETH_TYPE,
            Criterion.Type.VLAN_VID,
            Criterion.Type.IP_PROTO,
            Criterion.Type.IPV4_SRC,
            Criterion.Type.IPV4_DST,
            Criterion.Type.TCP_SRC,
            Criterion.Type.TCP_SRC_MASKED,
            Criterion.Type.TCP_DST,
            Criterion.Type.TCP_DST_MASKED,
            Criterion.Type.UDP_SRC,
            Criterion.Type.UDP_SRC_MASKED,
            Criterion.Type.UDP_DST,
            Criterion.Type.UDP_DST_MASKED,
            Criterion.Type.ICMPV4_TYPE,
            Criterion.Type.ICMPV4_CODE,
            Criterion.Type.PROTOCOL_INDEPENDENT);

    private static final Map<PiTableId, PiActionId> NEXT_ID_ACTIONS = ImmutableMap.<PiTableId, PiActionId>builder()
            .put(FabricConstants.FABRIC_INGRESS_FORWARDING_BRIDGING,
                 FabricConstants.FABRIC_INGRESS_FORWARDING_SET_NEXT_ID_BRIDGING)
            .put(FabricConstants.FABRIC_INGRESS_FORWARDING_ROUTING_V4,
                 FabricConstants.FABRIC_INGRESS_FORWARDING_SET_NEXT_ID_ROUTING_V4)
            .put(FabricConstants.FABRIC_INGRESS_FORWARDING_ROUTING_V6,
                 FabricConstants.FABRIC_INGRESS_FORWARDING_SET_NEXT_ID_ROUTING_V6)
            .put(FabricConstants.FABRIC_INGRESS_FORWARDING_MPLS,
                 FabricConstants.FABRIC_INGRESS_FORWARDING_POP_MPLS_AND_NEXT)
            .put(FabricConstants.FABRIC_INGRESS_ACL_ACL,
                 FabricConstants.FABRIC_INGRESS_ACL_SET_NEXT_ID_ACL)
            .build();

    ForwardingObjectiveTranslator(DeviceId deviceId, FabricCapabilities capabilities) {
        super(deviceId, capabilities);
    }

    @Override
    public ObjectiveTranslation doTranslate(ForwardingObjective obj)
            throws FabricPipelinerException {

        if (!isValidSrMetadata(obj)) {
            throw new FabricPipelinerException(
                    format("Unsupported metadata configuration: metadata=%s", obj.meta()),
                    ObjectiveError.BADPARAMS);
        }

        final ObjectiveTranslation.Builder resultBuilder =
                ObjectiveTranslation.builder();
        switch (obj.flag()) {
            case SPECIFIC:
                processSpecificFwd(obj, resultBuilder);
                break;
            case VERSATILE:
                processVersatileFwd(obj, resultBuilder);
                break;
            case EGRESS:
            default:
                log.warn("Unsupported ForwardingObjective type '{}'", obj.flag());
                return ObjectiveTranslation.ofError(ObjectiveError.UNSUPPORTED);
        }
        return resultBuilder.build();
    }

    private void processVersatileFwd(ForwardingObjective obj,
                                     ObjectiveTranslation.Builder resultBuilder)
            throws FabricPipelinerException {

        final Set<Criterion.Type> unsupportedCriteria = obj.selector().criteria()
                .stream()
                .map(Criterion::type)
                .filter(t -> !ACL_CRITERIA.contains(t))
                .collect(Collectors.toSet());

        if (!unsupportedCriteria.isEmpty()) {
            throw new FabricPipelinerException(format(
                    "unsupported ACL criteria %s", unsupportedCriteria.toString()));
        }

        aclRule(obj, resultBuilder);
    }

    private void processSpecificFwd(ForwardingObjective obj,
                                    ObjectiveTranslation.Builder resultBuilder)
            throws FabricPipelinerException {

        final Set<Criterion> criteriaWithMeta = Sets.newHashSet(obj.selector().criteria());

        // FIXME: Is this really needed? Meta is such an ambiguous field...
        // Why would we match on a META field?
        if (obj.meta() != null) {
            criteriaWithMeta.addAll(obj.meta().criteria());
        }

        final ForwardingFunctionType fft = ForwardingFunctionType.getForwardingFunctionType(obj);

        switch (fft) {
            case UNKNOWN:
                throw new FabricPipelinerException(
                        "unable to detect forwarding function type");
            case L2_UNICAST:
                bridgingRule(obj, criteriaWithMeta, resultBuilder, false);
                break;
            case L2_BROADCAST:
                bridgingRule(obj, criteriaWithMeta, resultBuilder, true);
                break;
            case IPV4_ROUTING:
            case IPV4_ROUTING_MULTICAST:
                ipv4RoutingRule(obj, criteriaWithMeta, resultBuilder);
                break;
            case MPLS_SEGMENT_ROUTING:
                mplsRule(obj, criteriaWithMeta, resultBuilder);
                break;
            case IPV6_ROUTING:
            case IPV6_ROUTING_MULTICAST:
            default:
                throw new FabricPipelinerException(format(
                        "unsupported forwarding function type '%s'",
                        fft));
        }
    }

    private void bridgingRule(ForwardingObjective obj, Set<Criterion> criteriaWithMeta,
                              ObjectiveTranslation.Builder resultBuilder,
                              boolean broadcast)
            throws FabricPipelinerException {

        final VlanIdCriterion vlanIdCriterion = (VlanIdCriterion) criterionNotNull(
                criteriaWithMeta, Criterion.Type.VLAN_VID);
        final TrafficSelector.Builder selector = DefaultTrafficSelector.builder()
                .add(vlanIdCriterion);

        if (!broadcast) {
            final EthCriterion ethDstCriterion = (EthCriterion) criterionNotNull(
                    obj.selector(), Criterion.Type.ETH_DST);
            selector.matchEthDstMasked(ethDstCriterion.mac(), MacAddress.EXACT_MASK);
        }

        resultBuilder.addFlowRule(flowRule(
                obj, FabricConstants.FABRIC_INGRESS_FORWARDING_BRIDGING, selector.build()));
    }

    private void ipv4RoutingRule(ForwardingObjective obj, Set<Criterion> criteriaWithMeta,
                                 ObjectiveTranslation.Builder resultBuilder)
            throws FabricPipelinerException {
        final IPCriterion ipDstCriterion = (IPCriterion) criterionNotNull(
                criteriaWithMeta, Criterion.Type.IPV4_DST);

        if (ipDstCriterion.ip().prefixLength() == 0) {
            defaultIpv4Route(obj, resultBuilder);
            return;
        }

        final TrafficSelector selector = DefaultTrafficSelector.builder()
                .add(ipDstCriterion)
                .build();

        resultBuilder.addFlowRule(flowRule(
                obj, FabricConstants.FABRIC_INGRESS_FORWARDING_ROUTING_V4, selector));
    }

    private void defaultIpv4Route(ForwardingObjective obj,
                                  ObjectiveTranslation.Builder resultBuilder)
            throws FabricPipelinerException {
        ForwardingObjective defaultObj = obj.copy()
                .withPriority(0)
                .add();
        final TrafficSelector selector = DefaultTrafficSelector.emptySelector();
        resultBuilder.addFlowRule(flowRule(
                defaultObj, FabricConstants.FABRIC_INGRESS_FORWARDING_ROUTING_V4, selector));
    }

    private void mplsRule(ForwardingObjective obj, Set<Criterion> criteriaWithMeta,
                          ObjectiveTranslation.Builder resultBuilder)
            throws FabricPipelinerException {

        final MplsCriterion mplsCriterion = (MplsCriterion) criterionNotNull(
                criteriaWithMeta, Criterion.Type.MPLS_LABEL);
        final TrafficSelector selector = DefaultTrafficSelector.builder()
                .add(mplsCriterion)
                .build();

        resultBuilder.addFlowRule(flowRule(
                obj, FabricConstants.FABRIC_INGRESS_FORWARDING_MPLS, selector));
    }

    private void aclRule(ForwardingObjective obj,
                         ObjectiveTranslation.Builder resultBuilder)
            throws FabricPipelinerException {
        if (obj.nextId() == null && obj.treatment() != null) {
            final TrafficTreatment treatment = obj.treatment();
            final PortNumber outPort = outputPort(treatment);
            if (outPort != null
                    && outPort.equals(PortNumber.CONTROLLER)
                    && treatment.allInstructions().size() == 1) {

                final PiAction aclAction;
                if (treatment.clearedDeferred()) {
                    aclAction = PiAction.builder()
                            .withId(FabricConstants.FABRIC_INGRESS_ACL_PUNT_TO_CPU)
                            .build();
                } else {
                    // Action is SET_CLONE_SESSION_ID
                    if (obj.op() == Objective.Operation.ADD) {
                        // Action is ADD, create clone group
                        final DefaultGroupDescription cloneGroup =
                                createCloneGroup(obj.appId(),
                                                 CLONE_TO_CPU_ID,
                                                 outPort);
                        resultBuilder.addGroup(cloneGroup);
                    }
                    aclAction = PiAction.builder()
                            .withId(FabricConstants.FABRIC_INGRESS_ACL_SET_CLONE_SESSION_ID)
                            .withParameter(new PiActionParam(
                                    FabricConstants.CLONE_ID, CLONE_TO_CPU_ID))
                            .build();
                }
                final TrafficTreatment piTreatment = DefaultTrafficTreatment.builder()
                        .piTableAction(aclAction)
                        .build();
                resultBuilder.addFlowRule(flowRule(
                        obj, FabricConstants.FABRIC_INGRESS_ACL_ACL, obj.selector(), piTreatment));
                return;
            }
        }
        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder(obj.selector());
        // Meta are used to signal the port type which can be edge or infra
        Byte portType = portType(obj);
        if (portType != null && !isSrMetadataSet(obj, PAIR_PORT)) {
            if (portType == PORT_TYPE_EDGE || portType == PORT_TYPE_INFRA) {
                selectorBuilder.matchPi(PiCriterion.builder()
                        .matchTernary(FabricConstants.HDR_PORT_TYPE, (long) portType, PORT_TYPE_MASK)
                        .build());
            } else {
                throw new FabricPipelinerException(format("Port type '%s' is not allowed for table '%s'",
                        portType, FabricConstants.FABRIC_INGRESS_FILTERING_INGRESS_PORT_VLAN),
                        ObjectiveError.UNSUPPORTED);
            }
        }
        resultBuilder.addFlowRule(flowRule(
                obj, FabricConstants.FABRIC_INGRESS_ACL_ACL, selectorBuilder.build()));
    }

    private DefaultGroupDescription createCloneGroup(
            ApplicationId appId,
            int cloneSessionId,
            PortNumber outPort) {
        final GroupKey groupKey = new DefaultGroupKey(
                FabricPipeliner.KRYO.serialize(cloneSessionId));

        final List<GroupBucket> bucketList = ImmutableList.of(
                createCloneGroupBucket(DefaultTrafficTreatment.builder()
                                               .setOutput(outPort)
                                               .build()));
        final DefaultGroupDescription cloneGroup = new DefaultGroupDescription(
                deviceId, GroupDescription.Type.CLONE,
                new GroupBuckets(bucketList),
                groupKey, cloneSessionId, appId);
        return cloneGroup;
    }

    private FlowRule flowRule(
            ForwardingObjective obj, PiTableId tableId, TrafficSelector selector)
            throws FabricPipelinerException {
        return flowRule(obj, tableId, selector, nextIdOrTreatment(obj, tableId));
    }

    private static TrafficTreatment nextIdOrTreatment(
            ForwardingObjective obj, PiTableId tableId)
            throws FabricPipelinerException {
        if (obj.nextId() == null) {
            return obj.treatment();
        } else {
            if (!NEXT_ID_ACTIONS.containsKey(tableId)) {
                throw new FabricPipelinerException(format(
                        "BUG? no next_id action set for table %s", tableId));
            }
            return DefaultTrafficTreatment.builder()
                    .piTableAction(
                            setNextIdAction(obj.nextId(),
                                            NEXT_ID_ACTIONS.get(tableId)))
                    .build();
        }
    }

    private static PiAction setNextIdAction(Integer nextId, PiActionId actionId) {
        final PiActionParam nextIdParam = new PiActionParam(FabricConstants.NEXT_ID, nextId);
        return PiAction.builder()
                .withId(actionId)
                .withParameter(nextIdParam)
                .build();
    }
}
