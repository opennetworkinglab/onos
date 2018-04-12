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
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultFlowRule;
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
import org.onosproject.pipelines.fabric.FabricConstants;
import org.slf4j.Logger;

import java.util.Collection;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Handling filtering objective for fabric pipeliner.
 */
public class FabricFilteringPipeliner {
    private static final Logger log = getLogger(FabricFilteringPipeliner.class);
    // Forwarding types
    private static final byte FWD_BRIDGING = 0;
    private static final byte FWD_MPLS = 1;
    private static final byte FWD_IPV4_UNICAST = 2;
    private static final byte FWD_IPV4_MULTICAST = 3;
    private static final byte FWD_IPV6_UNICAST = 4;
    private static final byte FWD_IPV6_MULTICAST = 5;
    private static final PiCriterion VLAN_VALID = PiCriterion.builder()
            .matchExact(FabricConstants.HDR_VLAN_TAG_IS_VALID, new byte[]{1})
            .build();
    private static final PiCriterion VLAN_INVALID = PiCriterion.builder()
            .matchExact(FabricConstants.HDR_VLAN_TAG_IS_VALID, new byte[]{0})
            .build();

    protected DeviceId deviceId;

    public FabricFilteringPipeliner(DeviceId deviceId) {
        this.deviceId = deviceId;
    }

    /**
     * Translates filtering objective to flows and groups.
     *
     * @param filterObjective the filtering objective
     * @return translation result, contains flows, groups or error it generated
     */
    public PipelinerTranslationResult filter(FilteringObjective filterObjective) {
        PipelinerTranslationResult.Builder resultBuilder = PipelinerTranslationResult.builder();
        // maps selector and treatment from filtering objective to filtering
        // control block.

        if (filterObjective.type() == FilteringObjective.Type.DENY) {
            log.warn("Unsupported filtering objective type {}", filterObjective.type());
            resultBuilder.setError(ObjectiveError.UNSUPPORTED);
            return resultBuilder.build();
        }

        if (filterObjective.key() == null ||
                filterObjective.key().type() != Criterion.Type.IN_PORT) {
            log.warn("Unsupported filter key {}", filterObjective.key());
            resultBuilder.setError(ObjectiveError.BADPARAMS);
            return resultBuilder.build();
        }
        PortCriterion inPortCriterion = (PortCriterion) filterObjective.key();
        VlanIdCriterion vlanCriterion = filterObjective.conditions().stream()
                .filter(criterion -> criterion.type() == Criterion.Type.VLAN_VID)
                .map(criterion -> (VlanIdCriterion) criterion)
                .findFirst()
                .orElse(null);
        EthCriterion ethDstCriterion = filterObjective.conditions().stream()
                .filter(criterion -> criterion.type() == Criterion.Type.ETH_DST)
                .map(criterion -> (EthCriterion) criterion)
                .findFirst()
                .orElse(null);

        FlowRule inPortVlanTableRule = createInPortVlanTable(inPortCriterion, vlanCriterion,
                                                             filterObjective);
        Collection<FlowRule> fwdClassifierRules = createFwdClassifierRules(inPortCriterion, ethDstCriterion,
                                                                           filterObjective);

        resultBuilder.addFlowRule(inPortVlanTableRule);
        fwdClassifierRules.forEach(resultBuilder::addFlowRule);
        return resultBuilder.build();
    }

    private FlowRule createInPortVlanTable(Criterion inPortCriterion,
                                           VlanIdCriterion vlanCriterion,
                                           FilteringObjective filterObjective) {
        Criterion vlanIsVlalidCriterion;
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder()
                .add(inPortCriterion);

        VlanId vlanId = null;
        if (vlanCriterion != null) {
            vlanId = vlanCriterion.vlanId();
        }

        vlanIsVlalidCriterion = VLAN_VALID;
        if (vlanId == null || vlanId.equals(VlanId.NONE)) {
            // untag vlan, match in port only
            vlanIsVlalidCriterion = VLAN_INVALID;
        }

        selector.add(vlanIsVlalidCriterion);

        // TODO: check if this treatment is valid or not
        TrafficTreatment treatment = filterObjective.meta();
        if (treatment == null) {
            treatment = DefaultTrafficTreatment.emptyTreatment();
        }

        return DefaultFlowRule.builder()
                .fromApp(filterObjective.appId())
                .withPriority(filterObjective.priority())
                .withSelector(selector.build())
                .withTreatment(treatment)
                .withPriority(filterObjective.priority())
                .forTable(FabricConstants.FABRIC_INGRESS_FILTERING_INGRESS_PORT_VLAN)
                .forDevice(deviceId)
                .makePermanent()
                .build();
    }

    private Collection<FlowRule> createFwdClassifierRules(PortCriterion inPortCriterion,
                                                          EthCriterion ethDstCriterion,
                                                          FilteringObjective filterObjective) {
        Collection<FlowRule> flowRules = Lists.newArrayList();
        if (ethDstCriterion == null) {
            // Bridging table, do nothing
            return flowRules;
        }
        PortNumber port = inPortCriterion.port();
        MacAddress dstMac = ethDstCriterion.mac();
        if (dstMac.isMulticast()) {
            flowRules.add(createMulticastFwdClassifierRule(port, dstMac, filterObjective));
            return flowRules;
        }

        flowRules.addAll(createIpFwdClassifierRules(port, dstMac, filterObjective));
        flowRules.add(createMplsFwdClassifierRule(port, dstMac, filterObjective));
        return flowRules;
    }

    private FlowRule createMulticastFwdClassifierRule(PortNumber inPort, MacAddress dstMac,
                                                      FilteringObjective filterObjective) {
        TrafficTreatment treatment;
        short ethType;
        if (dstMac.equals(MacAddress.IPV4_MULTICAST)) {
            // Ipv4 multicast
            treatment = createFwdClassifierTreatment(FWD_IPV4_MULTICAST);
            ethType = Ethernet.TYPE_IPV4;
        } else {
            // IPv6 multicast
            treatment = createFwdClassifierTreatment(FWD_IPV6_MULTICAST);
            ethType = Ethernet.TYPE_IPV6;
        }
        return createFwdClassifierRule(inPort, ethType, dstMac, treatment, filterObjective);
    }

    private Collection<FlowRule> createIpFwdClassifierRules(PortNumber inPort,
                                                            MacAddress dstMac,
                                                            FilteringObjective filterObjective) {
        Collection<FlowRule> flowRules = Lists.newArrayList();
        TrafficTreatment treatment;
        treatment = createFwdClassifierTreatment(FWD_IPV4_UNICAST);
        flowRules.add(createFwdClassifierRule(inPort, Ethernet.TYPE_IPV4, dstMac, treatment, filterObjective));
        treatment = createFwdClassifierTreatment(FWD_IPV6_UNICAST);
        flowRules.add(createFwdClassifierRule(inPort, Ethernet.TYPE_IPV6, dstMac, treatment, filterObjective));
        return flowRules;
    }

    private FlowRule createMplsFwdClassifierRule(PortNumber inPort,
                                                 MacAddress dstMac,
                                                 FilteringObjective filterObjective) {
        TrafficTreatment treatment = createFwdClassifierTreatment(FWD_MPLS);
        return createFwdClassifierRule(inPort, Ethernet.MPLS_UNICAST, dstMac, treatment, filterObjective);
    }

    private FlowRule createFwdClassifierRule(PortNumber inPort,
                                             short ethType,
                                             MacAddress dstMac,
                                             TrafficTreatment treatment,
                                             FilteringObjective filterObjective) {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder()
                .matchInPort(inPort)
                .matchEthDst(dstMac)
                .matchEthType(ethType);

        return DefaultFlowRule.builder()
                .withSelector(selector.build())
                .withTreatment(treatment)
                .fromApp(filterObjective.appId())
                .withPriority(filterObjective.priority())
                .forDevice(deviceId)
                .makePermanent()
                .forTable(FabricConstants.FABRIC_INGRESS_FILTERING_FWD_CLASSIFIER)
                .build();
    }

    private TrafficTreatment createFwdClassifierTreatment(byte fwdType) {
        PiActionParam param = new PiActionParam(FabricConstants.FWD_TYPE,
                                                ImmutableByteSequence.copyFrom(fwdType));
        PiAction action = PiAction.builder()
                .withId(FabricConstants.FABRIC_INGRESS_FILTERING_SET_FORWARDING_TYPE)
                .withParameter(param)
                .build();
        return DefaultTrafficTreatment.builder()
                .piTableAction(action)
                .build();

    }
}
