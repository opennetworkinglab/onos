/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.net.flowobjective.impl.composition;

import org.onlab.packet.IpPrefix;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.OchSignalCriterion;
import org.onosproject.net.flow.criteria.EthCriterion;
import org.onosproject.net.flow.criteria.VlanIdCriterion;
import org.onosproject.net.flow.criteria.VlanPcpCriterion;
import org.onosproject.net.flow.criteria.MplsCriterion;
import org.onosproject.net.flow.criteria.IPCriterion;
import org.onosproject.net.flow.criteria.IPv6FlowLabelCriterion;
import org.onosproject.net.flow.criteria.OduSignalIdCriterion;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.L0ModificationInstruction;
import org.onosproject.net.flow.instructions.L1ModificationInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction;
import org.onosproject.net.flow.instructions.L3ModificationInstruction;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.ForwardingObjective;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * Provide util functions for FlowObjectiveComposition.
 */
public final class FlowObjectiveCompositionUtil {

    private FlowObjectiveCompositionUtil() {}

    // only work with VERSATILE
    public static ForwardingObjective composeParallel(ForwardingObjective fo1, ForwardingObjective fo2) {

        TrafficSelector trafficSelector = intersectTrafficSelector(fo1.selector(), fo2.selector());
        if (trafficSelector == null) {
            return null;
        }

        TrafficTreatment trafficTreatment = unionTrafficTreatment(fo1.treatment(), fo2.treatment());

        return DefaultForwardingObjective.builder()
                .fromApp(fo1.appId())
                .makePermanent()
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .withPriority(fo1.priority() + fo2.priority())
                .withSelector(trafficSelector)
                .withTreatment(trafficTreatment)
                .add();
    }

    public static ForwardingObjective composeSequential(ForwardingObjective fo1,
                                                        ForwardingObjective fo2,
                                                        int priorityMultiplier) {

        TrafficSelector revertTrafficSelector = revertTreatmentSelector(fo1.treatment(), fo2.selector());
        if (revertTrafficSelector == null) {
            return null;
        }

        TrafficSelector trafficSelector = intersectTrafficSelector(fo1.selector(), revertTrafficSelector);
        if (trafficSelector == null) {
            return null;
        }

        TrafficTreatment trafficTreatment = unionTrafficTreatment(fo1.treatment(), fo2.treatment());

        return DefaultForwardingObjective.builder()
                .fromApp(fo1.appId())
                .makePermanent()
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .withPriority(fo1.priority() * priorityMultiplier + fo2.priority())
                .withSelector(trafficSelector)
                .withTreatment(trafficTreatment)
                .add();
    }

    public static ForwardingObjective composeOverride(ForwardingObjective fo, int priorityAddend) {
        return DefaultForwardingObjective.builder()
                .fromApp(fo.appId())
                .makePermanent()
                .withFlag(fo.flag())
                .withPriority(fo.priority() + priorityAddend)
                .withSelector(fo.selector())
                .withTreatment(fo.treatment())
                .add();
    }

    public static TrafficSelector intersectTrafficSelector(TrafficSelector ts1, TrafficSelector ts2) {

        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();

        Set<Criterion.Type> ts1IntersectTs2 = getTypeSet(ts1);
        ts1IntersectTs2.retainAll(getTypeSet(ts2));
        for (Criterion.Type type : ts1IntersectTs2) {
            Criterion criterion = intersectCriterion(ts1.getCriterion(type), ts2.getCriterion(type));
            if (criterion == null) {
                return null;
            } else {
                selectorBuilder.add(criterion);
            }
        }

        Set<Criterion.Type> ts1MinusTs2 = getTypeSet(ts1);
        ts1MinusTs2.removeAll(getTypeSet(ts2));
        for (Criterion.Type type : ts1MinusTs2) {
            selectorBuilder.add(ts1.getCriterion(type));
        }

        Set<Criterion.Type> ts2MinusTs1 = getTypeSet(ts2);
        ts2MinusTs1.removeAll(getTypeSet(ts1));
        for (Criterion.Type type : ts2MinusTs1) {
            selectorBuilder.add(ts2.getCriterion(type));
        }

        return selectorBuilder.build();
    }

    public static TrafficTreatment unionTrafficTreatment(TrafficTreatment tt1, TrafficTreatment tt2) {

        TrafficTreatment.Builder treatmentBuilder = DefaultTrafficTreatment.builder();

        for (Instruction instruction : tt1.allInstructions()) {
            treatmentBuilder.add(instruction);
        }

        for (Instruction instruction : tt2.allInstructions()) {
            treatmentBuilder.add(instruction);
        }

        return treatmentBuilder.build();
    }

    public static TrafficSelector revertTreatmentSelector(TrafficTreatment trafficTreatment,
                                                          TrafficSelector trafficSelector) {

        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();

        Map<Criterion.Type, Criterion> criterionMap = new HashMap<>();
        for (Criterion criterion : trafficSelector.criteria()) {
            criterionMap.put(criterion.type(), criterion);
        }

        for (Instruction instruction : trafficTreatment.allInstructions()) {
            switch (instruction.type()) {
                case OUTPUT:
                    break;
                case GROUP:
                    break;
                case L0MODIFICATION: {
                    L0ModificationInstruction l0 = (L0ModificationInstruction) instruction;
                    switch (l0.subtype()) {
                        case OCH:
                            if (criterionMap.containsKey(Criterion.Type.OCH_SIGID)) {
                                if (((OchSignalCriterion) criterionMap.get((Criterion.Type.OCH_SIGID))).lambda()
                                        .equals(((L0ModificationInstruction.ModOchSignalInstruction) l0).lambda())) {
                                    criterionMap.remove(Criterion.Type.OCH_SIGID);
                                } else {
                                    return null;
                                }
                            }
                        default:
                            break;
                    }
                    break;
                }
                case L1MODIFICATION: {
                    L1ModificationInstruction l1 = (L1ModificationInstruction) instruction;
                    switch (l1.subtype()) {
                        case ODU_SIGID:
                            if (criterionMap.containsKey(Criterion.Type.ODU_SIGID)) {
                                if (((OduSignalIdCriterion) criterionMap.get((Criterion.Type.ODU_SIGID))).oduSignalId()
                                        .equals(((L1ModificationInstruction.ModOduSignalIdInstruction) l1)
                                                .oduSignalId())) {
                                    criterionMap.remove(Criterion.Type.ODU_SIGID);
                                } else {
                                    return null;
                                }
                            }
                        default:
                            break;
                    }
                    break;
                }
                case L2MODIFICATION: {
                    L2ModificationInstruction l2 = (L2ModificationInstruction) instruction;
                    switch (l2.subtype()) {
                        case ETH_SRC:
                            if (criterionMap.containsKey(Criterion.Type.ETH_SRC)) {
                                if (((EthCriterion) criterionMap.get((Criterion.Type.ETH_SRC))).mac()
                                        .equals(((L2ModificationInstruction.ModEtherInstruction) l2).mac())) {
                                    criterionMap.remove(Criterion.Type.ETH_SRC);
                                } else {
                                    return null;
                                }
                            } else {
                                break;
                            }
                        case ETH_DST:
                            if (criterionMap.containsKey(Criterion.Type.ETH_DST)) {
                                if (((EthCriterion) criterionMap.get((Criterion.Type.ETH_DST))).mac()
                                        .equals(((L2ModificationInstruction.ModEtherInstruction) l2).mac())) {
                                    criterionMap.remove(Criterion.Type.ETH_DST);
                                } else {
                                    return null;
                                }
                            } else {
                                break;
                            }
                        case VLAN_ID:
                            if (criterionMap.containsKey(Criterion.Type.VLAN_VID)) {
                                if (((VlanIdCriterion) criterionMap.get((Criterion.Type.VLAN_VID))).vlanId()
                                        .equals(((L2ModificationInstruction.ModVlanIdInstruction) l2).vlanId())) {
                                    criterionMap.remove(Criterion.Type.VLAN_VID);
                                } else {
                                    return null;
                                }
                            } else {
                                break;
                            }
                        case VLAN_PCP:
                            if (criterionMap.containsKey(Criterion.Type.VLAN_PCP)) {
                                if (((VlanPcpCriterion) criterionMap.get((Criterion.Type.VLAN_PCP))).priority()
                                        == ((L2ModificationInstruction.ModVlanPcpInstruction) l2).vlanPcp()) {
                                    criterionMap.remove(Criterion.Type.VLAN_PCP);
                                } else {
                                    return null;
                                }
                            } else {
                                break;
                            }
                        case MPLS_LABEL:
                            if (criterionMap.containsKey(Criterion.Type.MPLS_LABEL)) {
                                if (((MplsCriterion) criterionMap.get((Criterion.Type.MPLS_LABEL))).label()
                                        .equals(((L2ModificationInstruction.ModMplsLabelInstruction) l2).label())) {
                                    criterionMap.remove(Criterion.Type.ETH_DST);
                                } else {
                                    return null;
                                }
                            } else {
                                break;
                            }
                        default:
                            break;
                    }
                    break;
                }
                case TABLE:
                    break;
                case L3MODIFICATION: {
                    L3ModificationInstruction l3 = (L3ModificationInstruction) instruction;
                    switch (l3.subtype()) {
                        case IPV4_SRC:
                            if (criterionMap.containsKey(Criterion.Type.IPV4_SRC)) {
                                if (((IPCriterion) criterionMap.get(Criterion.Type.IPV4_SRC)).ip()
                                        .contains(((L3ModificationInstruction.ModIPInstruction) l3).ip())) {
                                    criterionMap.remove(Criterion.Type.IPV4_SRC);
                                } else {
                                    return null;
                                }
                            } else {
                                break;
                            }
                        case IPV4_DST:
                            if (criterionMap.containsKey(Criterion.Type.IPV4_DST)) {
                                if (((IPCriterion) criterionMap.get(Criterion.Type.IPV4_DST)).ip()
                                        .contains(((L3ModificationInstruction.ModIPInstruction) l3).ip())) {
                                    criterionMap.remove(Criterion.Type.IPV4_DST);
                                } else {
                                    return null;
                                }
                            } else {
                                break;
                            }
                        case IPV6_SRC:
                            if (criterionMap.containsKey(Criterion.Type.IPV6_SRC)) {
                                if (((IPCriterion) criterionMap.get(Criterion.Type.IPV6_SRC)).ip()
                                        .contains(((L3ModificationInstruction.ModIPInstruction) l3).ip())) {
                                    criterionMap.remove(Criterion.Type.IPV6_SRC);
                                } else {
                                    return null;
                                }
                            } else {
                                break;
                            }
                        case IPV6_DST:
                            if (criterionMap.containsKey(Criterion.Type.IPV6_DST)) {
                                if (((IPCriterion) criterionMap.get(Criterion.Type.IPV6_DST)).ip()
                                        .contains(((L3ModificationInstruction.ModIPInstruction) l3).ip())) {
                                    criterionMap.remove(Criterion.Type.IPV6_DST);
                                } else {
                                    return null;
                                }
                            } else {
                                break;
                            }
                        case IPV6_FLABEL:
                            if (criterionMap.containsKey(Criterion.Type.IPV6_FLABEL)) {
                                if (((IPv6FlowLabelCriterion) criterionMap.get(Criterion.Type.IPV6_FLABEL)).flowLabel()
                                        == (((L3ModificationInstruction.ModIPv6FlowLabelInstruction) l3).flowLabel())) {
                                    criterionMap.remove(Criterion.Type.IPV4_SRC);
                                } else {
                                    return null;
                                }
                            } else {
                                break;
                            }
                        default:
                            break;
                    }
                    break;
                }
                case METADATA:
                    break;
                default:
                    break;
            }
        }

        for (Criterion criterion : criterionMap.values()) {
            selectorBuilder.add(criterion);
        }

        return selectorBuilder.build();
    }

    public static Set<Criterion.Type> getTypeSet(TrafficSelector trafficSelector) {
        Set<Criterion.Type> typeSet = new HashSet<>();
        for (Criterion criterion : trafficSelector.criteria()) {
            typeSet.add(criterion.type());
        }
        return typeSet;
    }

    public static Criterion intersectCriterion(Criterion c1, Criterion c2) {
        switch (c1.type()) {
            case IPV4_SRC: {
                IpPrefix ipPrefix = intersectIpPrefix(((IPCriterion) c1).ip(), ((IPCriterion) c2).ip());
                if (ipPrefix == null) {
                    return null;
                } else {
                    return Criteria.matchIPSrc(ipPrefix);
                }
            }
            case IPV4_DST: {
                IpPrefix ipPrefix = intersectIpPrefix(((IPCriterion) c1).ip(), ((IPCriterion) c2).ip());
                if (ipPrefix == null) {
                    return null;
                } else {
                    return Criteria.matchIPDst(ipPrefix);
                }
            }
            case IPV6_SRC: {
                IpPrefix ipPrefix = intersectIpPrefix(((IPCriterion) c1).ip(), ((IPCriterion) c2).ip());
                if (ipPrefix == null) {
                    return null;
                } else {
                    return Criteria.matchIPv6Src(ipPrefix);
                }
            }
            case IPV6_DST: {
                IpPrefix ipPrefix = intersectIpPrefix(((IPCriterion) c1).ip(), ((IPCriterion) c2).ip());
                if (ipPrefix == null) {
                    return null;
                } else {
                    return Criteria.matchIPv6Dst(ipPrefix);
                }
            }
            default:
                if (!c1.equals(c2)) {
                    return null;
                } else {
                    return c1;
                }
        }
    }

    public static IpPrefix intersectIpPrefix(IpPrefix ip1, IpPrefix ip2) {
        if (ip1.contains(ip2)) {
            return ip1;
        } else if (ip2.contains(ip1)) {
            return ip2;
        } else {
            return null;
        }
    }

    public static FlowObjectiveCompositionTree parsePolicyString(String policy) {
        List<FlowObjectiveCompositionTree> postfix = transformToPostfixForm(policy);
        return buildPolicyTree(postfix);
    }

    private static List<FlowObjectiveCompositionTree> transformToPostfixForm(String policy) {
        Stack<Character> stack = new Stack<>();
        List<FlowObjectiveCompositionTree> postfix = new ArrayList<>();

        for (int i = 0; i < policy.length(); i++) {
            Character ch = policy.charAt(i);
            if (Character.isDigit(ch)) {

                int applicationId = ch - '0';
                while (i + 1 < policy.length() && Character.isDigit(policy.charAt(i + 1))) {
                    i++;
                    applicationId = applicationId * 10 + policy.charAt(i) - '0';
                }

                postfix.add(new FlowObjectiveCompositionTree((short) applicationId));
            } else if (ch == '(') {
                stack.push(ch);
            } else if (ch == ')') {
                while (stack.peek() != '(') {
                    postfix.add(new FlowObjectiveCompositionTree(stack.pop()));
                }
                stack.pop();
            } else {
                while (!stack.isEmpty() && compareOperatorPriority(stack.peek(), ch)) {
                    postfix.add(new FlowObjectiveCompositionTree(stack.pop()));
                }
                stack.push(ch);
            }
        }
        while (!stack.isEmpty()) {
            postfix.add(new FlowObjectiveCompositionTree(stack.pop()));
        }

        return postfix;
    }

    private static boolean compareOperatorPriority(char peek, char cur) {
        if (peek == '/' && (cur == '+' || cur == '>' || cur == '/')) {
            return true;
        } else if (peek == '>' && (cur == '+' || cur == '>')) {
            return true;
        } else if (peek == '+' && cur == '+') {
            return true;
        }
        return false;
    }

    private static FlowObjectiveCompositionTree buildPolicyTree(List<FlowObjectiveCompositionTree> postfix) {
        Stack<FlowObjectiveCompositionTree> stack = new Stack<>();
        for (FlowObjectiveCompositionTree node : postfix) {
            if (node.operator == FlowObjectiveCompositionManager.PolicyOperator.Application) {
                stack.push(node);
            } else {
                node.rightChild = stack.pop();
                node.leftChild = stack.pop();
                stack.push(node);
            }
        }
        return stack.pop();
    }

    public static Collection<ForwardingObjective> minusForwardingObjectives(Collection<ForwardingObjective> fo1,
                                                                            Collection<ForwardingObjective> fo2) {
        Map<Integer, ForwardingObjective> map = new HashMap<>();
        for (ForwardingObjective fo : fo1) {
            map.put(fo.id(), fo);
        }
        for (ForwardingObjective fo : fo2) {
            map.remove(fo.id());
        }
        return map.values();
    }


}
