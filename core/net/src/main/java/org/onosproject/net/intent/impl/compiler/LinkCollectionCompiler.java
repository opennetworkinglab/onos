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

package org.onosproject.net.intent.impl.compiler;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import org.onlab.packet.EthType;
import org.onlab.packet.Ethernet;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MplsLabel;
import org.onlab.packet.VlanId;
import org.onlab.util.Identifier;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.EncapsulationType;
import org.onosproject.net.FilteredConnectPoint;
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;
import org.onosproject.net.domain.DomainId;
import org.onosproject.net.domain.DomainPointToPointIntent;
import org.onosproject.net.domain.DomainService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.EthTypeCriterion;
import org.onosproject.net.flow.criteria.MplsCriterion;
import org.onosproject.net.flow.criteria.TunnelIdCriterion;
import org.onosproject.net.flow.criteria.VlanIdCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.L0ModificationInstruction;
import org.onosproject.net.flow.instructions.L1ModificationInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.ModEtherInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.ModMplsBosInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.ModMplsLabelInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.ModTunnelIdInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.ModVlanIdInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.ModVlanPcpInstruction;
import org.onosproject.net.flow.instructions.L3ModificationInstruction;
import org.onosproject.net.flow.instructions.L3ModificationInstruction.ModArpEthInstruction;
import org.onosproject.net.flow.instructions.L3ModificationInstruction.ModArpIPInstruction;
import org.onosproject.net.flow.instructions.L3ModificationInstruction.ModArpOpInstruction;
import org.onosproject.net.flow.instructions.L3ModificationInstruction.ModIPInstruction;
import org.onosproject.net.flow.instructions.L3ModificationInstruction.ModIPv6FlowLabelInstruction;
import org.onosproject.net.flow.instructions.L4ModificationInstruction;
import org.onosproject.net.flow.instructions.L4ModificationInstruction.ModTransportPortInstruction;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentCompilationException;
import org.onosproject.net.intent.LinkCollectionIntent;
import org.onosproject.net.intent.constraint.DomainConstraint;
import org.onosproject.net.intent.constraint.EncapsulationConstraint;
import org.onosproject.net.resource.impl.LabelAllocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.onosproject.net.domain.DomainId.LOCAL;
import static org.onosproject.net.flow.criteria.Criterion.Type.*;

/**
 * Shared APIs and implementations for Link Collection compilers.
 */
public abstract class LinkCollectionCompiler<T> {

    /**
     * Reference to the label allocator.
     */
    static LabelAllocator labelAllocator;

    /**
     * Influence compiler behavior. If true the compiler
     * try to optimize the chain of the actions.
     */
    static boolean optimizeInstructions;

    /**
     * Influence compiler behavior. If true the compiler
     * try to optimize the copy ttl actions.
     */
    static boolean copyTtl;

    /**
     * The allowed tag criterions.
     */
    private static final Set<Criterion.Type> TAG_CRITERION_TYPES =
            Sets.immutableEnumSet(VLAN_VID, MPLS_LABEL, TUNNEL_ID);

    /**
     * Error message for wrong egress scenario.
     */
    private static final String WRONG_EGRESS = "Egress points not equal to 1 " +
            "and apply treatment at ingress, " +
            "which treatments should I apply ???";

    /**
     * Error message for wrong ingress scenario.
     */
    private static final String WRONG_INGRESS = "Ingress points not equal to 1 " +
            "and apply treatment at egress, " +
            "how can I match in the core ???";

    /**
     * Error message for wrong encapsulation scenario.
     */
    private static final String WRONG_ENCAPSULATION = "Wrong scenario - 1 hop with " +
            "encapsualtion";

    /**
     * Error message for unavailable labels.
     */
    private static final String NO_LABELS = "No available label for %s";

    /**
     * Error message for wrong encapsulation.
     */
    private static final String UNKNOWN_ENCAPSULATION = "Unknown encapsulation type";

    /**
     * Error message for unsupported L0 instructions.
     */
    private static final String UNSUPPORTED_L0 = "L0 not supported";

    /**
     * Error message for unsupported L1 instructions.
     */
    private static final String UNSUPPORTED_L1 = "L1 not supported";

    /**
     * Error message for unsupported eth subtype.
     */
    private static final String UNSUPPORTED_ETH_SUBTYPE = "Bad eth subtype";

    /**
     * Error message for unsupported pop action.
     */
    private static final String UNSUPPORTED_POP_ACTION = "Can't handle pop label";

    /**
     * Error message for unsupported L2 instructions.
     */
    private static final String UNSUPPORTED_L2 = "Unknown L2 Modification instruction";

    /**
     * Error message for unsupported IP subtype.
     */
    private static final String UNSUPPORTED_IP_SUBTYPE = "Bad ip subtype";

    /**
     * Error message for unsupported ARP.
     */
    private static final String UNSUPPORTED_ARP = "IPv6 not supported for ARP";

    /**
     * Error message for unsupported L3 instructions.
     */
    private static final String UNSUPPORTED_L3 = "Unknown L3 Modification instruction";

    /**
     * Error message for unsupported L4 subtype.
     */
    private static final String UNSUPPORTED_L4_SUBTYPE = "Unknown L4 subtype";

    /**
     * Error message for unsupported L4 instructions.
     */
    private static final String UNSUPPORTED_L4 = "Unknown L4 Modification instruction";

    /**
     * Error message for unsupported instructions.
     */
    private static final String UNSUPPORTED_INSTRUCTION = "Unknown instruction type";

    private static Logger log = LoggerFactory.getLogger(LinkCollectionCompiler.class);

    /**
     * Influence compiler behavior.
     *
     * @return true if we need the compiler optimizeTreatments the chain of the actions.
     */
    abstract boolean optimizeTreatments();

    /**
     * Creates the flows representations. This default implementation does
     * nothing. Subclasses should override this method to create their
     * specific flows representations (flow rule, flow objective).
     *
     * @param intent the intent to compile
     * @param deviceId the affected device
     * @param inPorts the input ports
     * @param outPorts the output ports
     * @param labels the labels for the label switching hop by hop
     * @return the list of flows representations
     */
    protected List<T> createRules(LinkCollectionIntent intent,
                                  DeviceId deviceId,
                                  Set<PortNumber> inPorts,
                                  Set<PortNumber> outPorts,
                                  Map<ConnectPoint, Identifier<?>> labels) {
        return null;
    }

    /**
     * Helper method to handle the different scenario (not encap, single hop, encap).
     *
     * @param encapConstraint the encapsulation constraint if it is present
     * @param intent the link collection intent
     * @param inPort the in port
     * @param outPorts the out ports
     * @param deviceId the current device
     * @param labels the labels used by the encapsulation
     * @return the forwarding instruction
     */
    protected ForwardingInstructions createForwardingInstruction(Optional<EncapsulationConstraint> encapConstraint,
                                                                 LinkCollectionIntent intent,
                                                                 PortNumber inPort,
                                                                 Set<PortNumber> outPorts,
                                                                 DeviceId deviceId,
                                                                 Map<ConnectPoint, Identifier<?>> labels) {
        ForwardingInstructions instructions = null;
        /*
         * If not encapsulation or single hop.
         */
        if (!encapConstraint.isPresent() || intent.links().isEmpty()) {
            instructions = this.createForwardingInstructions(
                    intent,
                    inPort,
                    deviceId,
                    outPorts
            );
        /*
         * If encapsulation is present. We retrieve the labels
         * for this iteration;
         */
        } else {
            Identifier<?> inLabel = labels.get(new ConnectPoint(deviceId, inPort));
            Map<ConnectPoint, Identifier<?>> outLabels = Maps.newHashMap();
            outPorts.forEach(outPort -> {
                ConnectPoint key = new ConnectPoint(deviceId, outPort);
                outLabels.put(key, labels.get(key));
            });
            instructions = this.createForwardingInstructions(
                    intent,
                    inPort,
                    inLabel,
                    deviceId,
                    outPorts,
                    outLabels,
                    encapConstraint.get().encapType()
            );
        }
        return instructions;
    }

    /**
     * Helper method which handles the proper generation of the ouput actions.
     *
     * @param outPorts the output ports
     * @param deviceId the current device
     * @param intent the intent to compile
     * @param outLabels the output labels
     * @param type the encapsulation type
     * @param preCondition the previous state
     * @param treatmentBuilder the builder to update with the ouput actions
     */
    private void manageOutputPorts(Set<PortNumber> outPorts,
                                   DeviceId deviceId,
                                   LinkCollectionIntent intent,
                                   Map<ConnectPoint, Identifier<?>> outLabels,
                                   EncapsulationType type,
                                   TrafficSelector.Builder preCondition,
                                   TrafficTreatment.Builder treatmentBuilder) {
        /*
         * We need to order the actions. First the actions
         * related to the not-egress points. At the same time we collect
         * also the egress points.
         */
        List<FilteredConnectPoint> egressPoints = Lists.newArrayList();
        for (PortNumber outPort : outPorts) {
            Optional<FilteredConnectPoint> filteredEgressPoint =
                    getFilteredConnectPointFromIntent(deviceId, outPort, intent);
            if (!filteredEgressPoint.isPresent()) {
                /*
                 * We build a temporary selector for the encapsulation.
                 */
                TrafficSelector.Builder encapBuilder = DefaultTrafficSelector.builder();
                /*
                 * We retrieve the associated label to the output port.
                 */
                ConnectPoint cp = new ConnectPoint(deviceId, outPort);
                Identifier<?> outLabel = outLabels.get(cp);
                /*
                 * If there are not labels, we cannot handle.
                 */
                if (outLabel == null) {
                    throw new IntentCompilationException(String.format(NO_LABELS, cp));
                }
                /*
                 * In the core we match using encapsulation.
                 */
                updateSelectorFromEncapsulation(
                        encapBuilder,
                        type,
                        outLabel
                );
                /*
                 * We generate the transition.
                 */
                TrafficTreatment forwardingTreatment =
                        forwardingTreatment(preCondition.build(),
                                            encapBuilder.build(),
                                            getEthType(intent.selector()));
                /*
                 * We add the instruction necessary to the transition.
                 */
                forwardingTreatment.allInstructions().stream()
                        .filter(inst -> inst.type() != Instruction.Type.NOACTION)
                        .forEach(treatmentBuilder::add);
                /*
                 * Finally we set the output action.
                 */
                treatmentBuilder.setOutput(outPort);
                /*
                 * The encapsulation modifies the packet. If we are optimizing
                 * we have to update the state.
                 */
                if (optimizeTreatments()) {
                    preCondition = encapBuilder;
                }
            } else {
                egressPoints.add(filteredEgressPoint.get());
            }
        }
        /*
         * The idea is to order the egress points. Before we deal
         * with the egress points which looks like similar to the
         * selector derived from the previous state then the
         * the others.
         */
        TrafficSelector prevState = preCondition.build();
        if (optimizeTreatments()) {
            egressPoints = orderedEgressPoints(prevState, egressPoints);
        }
        /*
         * In this case, we have to transit to the final
         * state.
         */
        generateEgressActions(treatmentBuilder, egressPoints, prevState, intent);

    }

    /**
     * Helper method to generate the egress actions.
     *
     * @param treatmentBuilder the treatment builder to update
     * @param egressPoints the egress points
     * @param initialState the initial state of the transition
     */
    private void generateEgressActions(TrafficTreatment.Builder treatmentBuilder,
                                       List<FilteredConnectPoint> egressPoints,
                                       TrafficSelector initialState,
                                       LinkCollectionIntent intent) {

        TrafficSelector prevState = initialState;
        for (FilteredConnectPoint egressPoint : egressPoints) {
            /*
             * If we are at the egress, we have to transit to the final
             * state. First we add the Intent treatment.
             */
            intent.treatment().allInstructions().stream()
                    .filter(inst -> inst.type() != Instruction.Type.NOACTION)
                    .forEach(treatmentBuilder::add);
            /*
             * We generate the transition FIP->FEP.
             */
            TrafficTreatment forwardingTreatment =
                    forwardingTreatment(prevState,
                                        egressPoint.trafficSelector(),
                                        getEthType(intent.selector()));
            /*
             * We add the instruction necessary to the transition.
             * Potentially we override the intent treatment.
             */
            forwardingTreatment.allInstructions().stream()
                    .filter(inst -> inst.type() != Instruction.Type.NOACTION)
                    .forEach(treatmentBuilder::add);
            /*
             * Finally we set the output action.
             */
            treatmentBuilder.setOutput(egressPoint.connectPoint().port());
            if (optimizeTreatments()) {
                /*
                 * We update the previous state. In this way instead of
                 * transiting from FIP->FEP we do FEP->FEP and so on.
                 */
                prevState = egressPoint.trafficSelector();
            }
        }

    }

    /**
     * Helper method to order the egress ports according to a
     * specified criteria. The idea is to generate first the actions
     * for the egress ports which are similar to the specified criteria
     * then the others. In this way we can mitigate the problems related
     * to the chain of actions and we can optimize also the number of
     * actions.
     *
     * @param orderCriteria the ordering criteria
     * @param pointsToOrder the egress points to order
     * @return a list of port ordered
     */
    private List<FilteredConnectPoint> orderedEgressPoints(TrafficSelector orderCriteria,
                                        List<FilteredConnectPoint> pointsToOrder) {
        /*
         * We are interested only to the labels. The idea is to order
         * by the tags.
         *
         */
        Criterion vlanIdCriterion = orderCriteria.getCriterion(VLAN_VID);
        Criterion mplsLabelCriterion = orderCriteria.getCriterion(MPLS_LABEL);
        /*
         * We collect all the untagged points.
         *
         */
        List<FilteredConnectPoint> untaggedEgressPoints = pointsToOrder
                .stream()
                .filter(pointToOrder -> {
                    TrafficSelector selector = pointToOrder.trafficSelector();
                    return selector.getCriterion(VLAN_VID) == null &&
                            selector.getCriterion(MPLS_LABEL) == null;
                }).collect(Collectors.toList());
        /*
         * We collect all the vlan points.
         */
        List<FilteredConnectPoint> vlanEgressPoints = pointsToOrder
                .stream()
                .filter(pointToOrder -> {
                    TrafficSelector selector = pointToOrder.trafficSelector();
                    return selector.getCriterion(VLAN_VID) != null &&
                            selector.getCriterion(MPLS_LABEL) == null;
                }).collect(Collectors.toList());
        /*
         * We collect all the mpls points.
         */
        List<FilteredConnectPoint> mplsEgressPoints = pointsToOrder
                .stream()
                .filter(pointToOrder -> {
                    TrafficSelector selector = pointToOrder.trafficSelector();
                    return selector.getCriterion(VLAN_VID) == null &&
                            selector.getCriterion(MPLS_LABEL) != null;
                }).collect(Collectors.toList());
        /*
         * We create the final list of ports.
         */
        List<FilteredConnectPoint> orderedList = Lists.newArrayList();
        /*
         * The ordering criteria is vlan id. First we add the vlan
         * ports. Then the others.
         */
        if (vlanIdCriterion != null && mplsLabelCriterion == null) {
            orderedList.addAll(vlanEgressPoints);
            orderedList.addAll(untaggedEgressPoints);
            orderedList.addAll(mplsEgressPoints);
            return orderedList;
        }
        /*
         * The ordering criteria is mpls label. First we add the mpls
         * ports. Then the others.
         */
        if (vlanIdCriterion == null && mplsLabelCriterion != null) {
            orderedList.addAll(mplsEgressPoints);
            orderedList.addAll(untaggedEgressPoints);
            orderedList.addAll(vlanEgressPoints);
            return orderedList;
        }
        /*
         * The ordering criteria is untagged. First we add the untagged
         * ports. Then the others.
         */
        if (vlanIdCriterion == null) {
            orderedList.addAll(untaggedEgressPoints);
            orderedList.addAll(vlanEgressPoints);
            orderedList.addAll(mplsEgressPoints);
            return orderedList;
        }
        /*
         * Unhandled scenario.
         */
        orderedList.addAll(vlanEgressPoints);
        orderedList.addAll(mplsEgressPoints);
        orderedList.addAll(untaggedEgressPoints);
        return orderedList;
    }

    /**
     * Manages the Intents with a single ingress point (p2p, sp2mp)
     * creating properly the selector builder and the treatment builder.
     *
     * @param selectorBuilder the selector builder to update
     * @param treatmentBuilder the treatment builder to update
     * @param intent the intent to compile
     * @param deviceId the current device
     * @param outPorts the output ports of this device
     */
    private void manageSpIntent(TrafficSelector.Builder selectorBuilder,
                                TrafficTreatment.Builder treatmentBuilder,
                                LinkCollectionIntent intent,
                                DeviceId deviceId,
                                Set<PortNumber> outPorts) {
        /*
         * Sanity check.
         */
        if (intent.filteredIngressPoints().size() != 1) {
            throw new IntentCompilationException(WRONG_INGRESS);
        }
        /*
         * For the p2p and sp2mp the transition initial state
         * to final state is performed at the egress.
         */
        Optional<FilteredConnectPoint> filteredIngressPoint =
                intent.filteredIngressPoints().stream().findFirst();
        /*
         * We build the final selector, adding the selector
         * of the FIP to the Intent selector and potentially
         * overriding its matches.
         */
        filteredIngressPoint.get()
                .trafficSelector()
                .criteria()
                .forEach(selectorBuilder::add);
        /*
         * In this scenario, potentially we can have several output
         * ports. First we have to insert in the treatment the actions
         * for the core.
         */
        List<FilteredConnectPoint> egressPoints = Lists.newArrayList();
        for (PortNumber outPort : outPorts) {
            Optional<FilteredConnectPoint> filteredEgressPoint =
                    getFilteredConnectPointFromIntent(deviceId, outPort, intent);
            if (!filteredEgressPoint.isPresent()) {
                treatmentBuilder.setOutput(outPort);
            } else {
                egressPoints.add(filteredEgressPoint.get());
            }
        }
        /*
         * The idea is to order the egress points. Before we deal
         * with the egress points which looks like similar to the ingress
         * point then the others.
         */
        TrafficSelector prevState = filteredIngressPoint.get().trafficSelector();
        if (optimizeTreatments()) {
            egressPoints = orderedEgressPoints(prevState, egressPoints);
        }
        /*
         * Then we deal with the egress points.
         */
        generateEgressActions(treatmentBuilder, egressPoints, prevState, intent);
    }

    /**
     * Manages the Intents with multiple ingress points creating properly
     * the selector builder and the treatment builder.
     *
     * @param selectorBuilder the selector builder to update
     * @param treatmentBuilder the treatment builder to update
     * @param intent the intent to compile
     * @param inPort the input port of the current device
     * @param deviceId the current device
     * @param outPorts the output ports of this device
     */
    private void manageMpIntent(TrafficSelector.Builder selectorBuilder,
                                TrafficTreatment.Builder treatmentBuilder,
                                LinkCollectionIntent intent,
                                PortNumber inPort,
                                DeviceId deviceId,
                                Set<PortNumber> outPorts) {
        /*
         * Sanity check
         */
        if (intent.filteredEgressPoints().size() != 1) {
            throw new IntentCompilationException(WRONG_EGRESS);
        }
        /*
         * We try to understand if the device is one of the ingress points.
         */
        Optional<FilteredConnectPoint> filteredIngressPoint =
                getFilteredConnectPointFromIntent(deviceId, inPort, intent);
        /*
         * We retrieve from the Intent the unique egress points.
         */
        Optional<FilteredConnectPoint> filteredEgressPoint =
                intent.filteredEgressPoints().stream().findFirst();
        /*
         * We check if the device is the ingress device
         */
        if (filteredIngressPoint.isPresent()) {
            /*
             * We are at ingress, so basically what we have to do is this:
             * apply a set of operations (treatment, FEP) in order to have
             * a transition from the initial state to the final state.
             *
             * We initialize the treatment with the Intent treatment
             */
            intent.treatment().allInstructions().stream()
                    .filter(inst -> inst.type() != Instruction.Type.NOACTION)
                    .forEach(treatmentBuilder::add);
            /*
             * We build the final selector, adding the selector
             * of the FIP to the Intent selector and potentially
             * overriding its matches.
             */
            filteredIngressPoint.get()
                    .trafficSelector()
                    .criteria()
                    .forEach(selectorBuilder::add);
            /*
             * We define the transition FIP->FEP, basically
             * the set of the operations we need for reaching
             * the final state.
             */
            TrafficTreatment forwardingTreatment =
                    forwardingTreatment(filteredIngressPoint.get().trafficSelector(),
                                        filteredEgressPoint.get().trafficSelector(),
                                        getEthType(intent.selector()));
            /*
             * We add to the treatment the actions necessary for the
             * transition, potentially overriding the treatment of the
             * Intent. The Intent treatment has always a low priority
             * in respect of the FEP.
             */
            forwardingTreatment.allInstructions().stream()
                    .filter(inst -> inst.type() != Instruction.Type.NOACTION)
                    .forEach(treatmentBuilder::add);
        } else {
            /*
             * We are in the core or in the egress switch.
             * The packets are in their final state. We need
             * to match against this final state.
             *
             * we derive the final state defined by the intent
             * treatment.
             */
            updateBuilder(selectorBuilder, intent.treatment());
            /*
             * We derive the final state defined by the unique
             * FEP. We merge the two states.
             */
            filteredEgressPoint.get()
                    .trafficSelector()
                    .criteria()
                    .forEach(selectorBuilder::add);
        }
        /*
         * Finally we set the output action.
         */
        outPorts.forEach(treatmentBuilder::setOutput);
    }

    /**
     * Computes treatment and selector which will be used
     * in the flow representation (Rule, Objective).
     *
     * @param intent the intent to compile
     * @param inPort the input port of this device
     * @param deviceId the current device
     * @param outPorts the output ports of this device
     * @return the forwarding instruction object which encapsulates treatment and selector
     */
    protected ForwardingInstructions createForwardingInstructions(LinkCollectionIntent intent,
                                                                  PortNumber inPort,
                                                                  DeviceId deviceId,
                                                                  Set<PortNumber> outPorts) {

        /*
         * We build an empty treatment and we initialize the selector with
         * the intent selector.
         */
        TrafficTreatment.Builder treatmentBuilder = DefaultTrafficTreatment
                .builder();
        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector
                .builder(intent.selector())
                .matchInPort(inPort);

        if (!intent.applyTreatmentOnEgress()) {
            manageMpIntent(selectorBuilder,
                           treatmentBuilder,
                           intent,
                           inPort,
                           deviceId,
                           outPorts
            );
        } else {
            manageSpIntent(selectorBuilder,
                           treatmentBuilder,
                           intent,
                           deviceId,
                           outPorts
            );
        }
        /*
         * We return selector and treatment necessary to build the flow rule
         * or the flow objective.
         */
        return new ForwardingInstructions(treatmentBuilder.build(), selectorBuilder.build());
    }

    /**
     * Manages the ingress of the Intents (p2p, sp2mp, mp2sp) with encapsulation.
     *
     * @param selectorBuilder the selector builder to update
     * @param treatmentBuilder the treatment builder to update
     * @param intent the intent to compile
     * @param inPort the input port of this device
     * @param deviceId the current device
     * @param outPorts the output ports of this device
     * @param outLabels the labels associated to the output port
     * @param type the encapsulation type
     */
    private void manageEncapAtIngress(TrafficSelector.Builder selectorBuilder,
                                      TrafficTreatment.Builder treatmentBuilder,
                                      LinkCollectionIntent intent,
                                      PortNumber inPort,
                                      DeviceId deviceId,
                                      Set<PortNumber> outPorts,
                                      Map<ConnectPoint, Identifier<?>> outLabels,
                                      EncapsulationType type) {

        Optional<FilteredConnectPoint> filteredIngressPoint =
                getFilteredConnectPointFromIntent(deviceId, inPort, intent);
        /*
         * We fill the selector builder with the intent selector.
         */
        intent.selector().criteria().forEach(selectorBuilder::add);
        /*
         * We build the final selector, adding the selector
         * of the FIP to the Intent selector and potentially
         * overriding its matches.
         */
        filteredIngressPoint.get()
                .trafficSelector()
                .criteria()
                .forEach(selectorBuilder::add);
        /*
         * In this case the precondition is the selector of the filtered
         * ingress point.
         */
        TrafficSelector.Builder preCondition = DefaultTrafficSelector
                .builder(filteredIngressPoint.get().trafficSelector());
        /*
         * Generate the output actions.
         */
        manageOutputPorts(
                outPorts,
                deviceId,
                intent,
                outLabels,
                type,
                preCondition,
                treatmentBuilder
        );

    }

    /**
     * Manages the core and transit of the Intents (p2p, sp2mp, mp2sp)
     * with encapsulation.
     *
     * @param selectorBuilder the selector builder to update
     * @param treatmentBuilder the treatment builder to update
     * @param intent the intent to compile
     * @param inPort the input port of this device
     * @param inLabel the label associated to the input port
     * @param deviceId the current device
     * @param outPorts the output ports of this device
     * @param outLabels the labels associated to the output port
     * @param type the encapsulation type
     */
    private void manageEncapAtCoreAndEgress(TrafficSelector.Builder selectorBuilder,
                                            TrafficTreatment.Builder treatmentBuilder,
                                            LinkCollectionIntent intent,
                                            PortNumber inPort,
                                            Identifier<?> inLabel,
                                            DeviceId deviceId,
                                            Set<PortNumber> outPorts,
                                            Map<ConnectPoint, Identifier<?>> outLabels,
                                            EncapsulationType type) {

        /*
         * If there are not labels, we cannot handle.
         */
        ConnectPoint inCp = new ConnectPoint(deviceId, inPort);
        if (inLabel == null) {
            throw new IntentCompilationException(String.format(NO_LABELS, inCp));
        }
        /*
         * In the core and at egress we match using encapsulation.
         */
        updateSelectorFromEncapsulation(
                selectorBuilder,
                type,
                inLabel
        );
        /*
         * Generate the output actions.
         */
        manageOutputPorts(
                outPorts,
                deviceId,
                intent,
                outLabels,
                type,
                selectorBuilder,
                treatmentBuilder
        );

    }

    /**
     * Computes treatment and selector which will be used
     * in the flow representation (Rule, Objective).
     *
     * @param intent the intent to compile
     * @param inPort the input port of this device
     * @param inLabel the label associated to the input port
     * @param deviceId the current device
     * @param outPorts the output ports of this device
     * @param outLabels the labels associated to the output port
     * @param type the encapsulation type
     * @return the forwarding instruction object which encapsulates treatment and selector
     */
    protected ForwardingInstructions createForwardingInstructions(LinkCollectionIntent intent,
                                                                  PortNumber inPort,
                                                                  Identifier<?> inLabel,
                                                                  DeviceId deviceId,
                                                                  Set<PortNumber> outPorts,
                                                                  Map<ConnectPoint, Identifier<?>> outLabels,
                                                                  EncapsulationType type) {
        /*
         * We build an empty treatment and an empty selector.
         */
        TrafficTreatment.Builder treatmentBuilder = DefaultTrafficTreatment.builder();
        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
        selectorBuilder.matchInPort(inPort);
        Optional<FilteredConnectPoint> filteredIngressPoint =
                getFilteredConnectPointFromIntent(deviceId, inPort, intent);

        if (filteredIngressPoint.isPresent()) {
            manageEncapAtIngress(selectorBuilder,
                                 treatmentBuilder,
                                 intent,
                                 inPort,
                                 deviceId,
                                 outPorts,
                                 outLabels,
                                 type
            );
        } else {
            manageEncapAtCoreAndEgress(selectorBuilder,
                                       treatmentBuilder,
                                       intent,
                                       inPort,
                                       inLabel,
                                       deviceId,
                                       outPorts,
                                       outLabels,
                                       type);
        }
        /*
         * We return selector and treatment necessary to build the flow rule
         * or the flow objective.
         */
        return new ForwardingInstructions(treatmentBuilder.build(), selectorBuilder.build());
    }

    /**
     * Helper class to encapsulate treatment and selector
     * in an unique abstraction.
     */
    protected class ForwardingInstructions {

        private TrafficTreatment trafficTreatment;

        private TrafficSelector trafficSelector;

        public ForwardingInstructions(TrafficTreatment treatment, TrafficSelector selector) {

            this.trafficTreatment = treatment;
            this.trafficSelector = selector;

        }

        public TrafficTreatment treatment() {
            return this.trafficTreatment;
        }

        public TrafficSelector selector() {
            return this.trafficSelector;
        }

    }

    /**
     * Helper method to compute input and output ports
     * for each device crossed in the path.
     *
     * @param intent the related intents
     * @param inputPorts the input ports to compute
     * @param outputPorts the output ports to compute
     */
    protected void computePorts(LinkCollectionIntent intent,
                                SetMultimap<DeviceId, PortNumber> inputPorts,
                                SetMultimap<DeviceId, PortNumber> outputPorts) {

        for (Link link : intent.links()) {
            inputPorts.put(link.dst().deviceId(), link.dst().port());
            outputPorts.put(link.src().deviceId(), link.src().port());
        }

        for (ConnectPoint ingressPoint : intent.ingressPoints()) {
            inputPorts.put(ingressPoint.deviceId(), ingressPoint.port());
        }

        for (ConnectPoint egressPoint : intent.egressPoints()) {
            outputPorts.put(egressPoint.deviceId(), egressPoint.port());
        }

    }

    /**
     * Retrieves the encapsulation constraint from the link collection intent.
     *
     * @param intent the intent to analyze
     * @return the encapsulation constraint
     */
    protected Optional<EncapsulationConstraint> getIntentEncapConstraint(LinkCollectionIntent intent) {
        return intent.constraints().stream()
                .filter(constraint -> constraint instanceof EncapsulationConstraint)
                .map(x -> (EncapsulationConstraint) x).findAny();
    }

    /**
     * Checks if domain processing is enabled for this intent by looking for the {@link DomainConstraint}.
     *
     * @param intent the intent to be checked
     * @return is the processing of domains enabled
     */
    protected boolean isDomainProcessingEnabled(LinkCollectionIntent intent) {
        return intent.constraints().contains(DomainConstraint.domain());
    }

    /**
     * Creates the domain intents that the {@link LinkCollectionIntent} contains.
     *
     * @param intent        the link collection intent
     * @param domainService the domain service
     * @return the resulting list of domain intents
     */
    protected List<Intent> getDomainIntents(LinkCollectionIntent intent,
                                            DomainService domainService) {
        ImmutableList.Builder<Intent> intentList = ImmutableList.builder();
        // domain handling is only applied for a single entry and exit point
        // TODO: support multi point to multi point
        if (intent.filteredIngressPoints().size() != 1 || intent
                .filteredEgressPoints().size() != 1) {
            log.warn("Multiple ingress or egress ports not supported!");
            return intentList.build();
        }
        ImmutableList.Builder<Link> domainLinks = ImmutableList.builder();
        // get the initial ingress connection point
        FilteredConnectPoint ingress =
                intent.filteredIngressPoints().iterator().next();
        FilteredConnectPoint egress;
        DeviceId currentDevice = ingress.connectPoint().deviceId();
        // the current domain (or LOCAL)
        DomainId currentDomain = domainService.getDomain(currentDevice);
        // if we entered a domain store the domain ingress
        FilteredConnectPoint domainIngress =
                LOCAL.equals(currentDomain) ? null : ingress;
        // loop until (hopefully) all links have been checked once
        // this is necessary because a set is not sorted by default
        for (int i = 0; i < intent.links().size(); i++) {
            // find the next link
            List<Link> nextLinks =
                    getEgressLinks(intent.links(), currentDevice);
            // no matching link exists
            if (nextLinks.isEmpty()) {
                throw new IntentCompilationException(
                        "No matching link starting at " + ingress
                                .connectPoint().deviceId());
            }
            // get the first link
            Link nextLink = nextLinks.get(0);
            ingress = new FilteredConnectPoint(nextLink.src());
            egress = new FilteredConnectPoint(nextLink.dst());
            // query the domain for the domain of the link's destination
            DomainId dstDomain = domainService
                    .getDomain(egress.connectPoint().deviceId());
            if (!currentDomain.equals(dstDomain)) {
                // we are leaving the current domain or LOCAL
                log.debug("Domain transition from {} to {}.", currentDomain,
                          dstDomain);
                if (!LOCAL.equals(currentDomain)) {
                    // add the domain intent to the intent list
                    intentList.add(createDomainP2PIntent(intent, domainIngress,
                                                         ingress,
                                                         domainLinks.build()));
                    // TODO: might end up with an unused builder
                    // reset domain links builder
                    domainLinks = ImmutableList.builder();
                }
                // update current domain (might be LOCAL)
                currentDomain = dstDomain;
                // update the domain's ingress
                domainIngress = LOCAL.equals(currentDomain) ? null : egress;
            } else {
                if (!LOCAL.equals(currentDomain)) {
                    // we are staying in the same domain, store the traversed link
                    domainLinks.add(nextLink);
                    log.debug("{} belongs to the same domain.",
                              egress.connectPoint().deviceId());
                }
            }
            currentDevice = egress.connectPoint().deviceId();
        }
        // get the egress point
        egress = intent.filteredEgressPoints().iterator().next();
        // still inside a domain?
        if (!LOCAL.equals(currentDomain) &&
                currentDomain.equals(domainService.getDomain(
                        egress.connectPoint().deviceId()))) {
            // add intent
            intentList.add(createDomainP2PIntent(intent, domainIngress, egress,
                                                 domainLinks.build()));
        }

        return intentList.build();
    }

    /**
     * Create a domain point to point intent from the parameters.
     *
     * @param originalIntent the original intent to extract the app ID and key
     * @param ingress the ingress connection point
     * @param egress the egress connection point
     * @param domainLinks the list of traversed links
     * @return the domain point to point intent
     */
    private static DomainPointToPointIntent createDomainP2PIntent(
            Intent originalIntent, FilteredConnectPoint ingress,
            FilteredConnectPoint egress, List<Link> domainLinks) {
        return DomainPointToPointIntent.builder()
                .appId(originalIntent.appId())
                .filteredIngressPoint(ingress)
                .filteredEgressPoint(egress)
                .key(originalIntent.key())
                .links(domainLinks)
                .build();
    }

    /**
     * Get links originating from the source device ID.
     *
     * @param links  list of available links
     * @param source the device ID of the source device
     * @return the list of links with the given source
     */
    private List<Link> getEgressLinks(Set<Link> links, final DeviceId source) {
        return links.stream()
                .filter(link -> link.src().deviceId().equals(source))
                .collect(Collectors.toList());
    }


    /**
     * Get FilteredConnectPoint from LinkCollectionIntent.
     *
     * @param deviceId device Id for connect point
     * @param portNumber port number
     * @param intent source intent
     * @return filtered connetion point
     */
    private Optional<FilteredConnectPoint> getFilteredConnectPointFromIntent(DeviceId deviceId,
                                                                             PortNumber portNumber,
                                                                             LinkCollectionIntent intent) {
        Set<FilteredConnectPoint> filteredConnectPoints =
                Sets.union(intent.filteredIngressPoints(), intent.filteredEgressPoints());
        return filteredConnectPoints.stream()
                .filter(port -> port.connectPoint().deviceId().equals(deviceId))
                .filter(port -> port.connectPoint().port().equals(portNumber))
                .findFirst();
    }

    /**
     * Get tag criterion from selector.
     * The criterion should be one of type in tagCriterionTypes.
     *
     * @param selector selector
     * @return Criterion that matched, if there is no tag criterion, return null
     */
    private Criterion getTagCriterion(TrafficSelector selector) {
        return selector.criteria().stream()
                .filter(criterion -> TAG_CRITERION_TYPES.contains(criterion.type()))
                .findFirst()
                .orElse(Criteria.dummy());

    }

    /**
     * Compares tag type between ingress and egress point and generate
     * treatment for egress point of intent.
     *
     * @param ingress ingress selector for the intent
     * @param egress egress selector for the intent
     * @param ethType the ethertype to use in mpls_pop
     * @return Builder of TrafficTreatment
     */
    private TrafficTreatment forwardingTreatment(TrafficSelector ingress,
                                                 TrafficSelector egress,
                                                 EthType ethType) {


        if (ingress.equals(egress)) {
            return DefaultTrafficTreatment.emptyTreatment();
        }

        TrafficTreatment.Builder builder = DefaultTrafficTreatment.builder();

        /*
         * "null" means there is no tag for the port
         * Tag criterion will be null if port is normal connection point
         */
        Criterion ingressTagCriterion = getTagCriterion(ingress);
        Criterion egressTagCriterion = getTagCriterion(egress);

        if (ingressTagCriterion.type() != egressTagCriterion.type()) {

            /*
             * Tag type of ingress port and egress port are different.
             * Need to remove tag from ingress, then add new tag for egress.
             * Remove nothing if ingress port use VXLAN or there is no tag
             * on ingress port.
             */
            switch (ingressTagCriterion.type()) {
                case VLAN_VID:
                    builder.popVlan();
                    break;

                case MPLS_LABEL:
                    if (copyTtl) {
                        builder.copyTtlIn();
                    }
                    builder.popMpls(ethType);
                    break;

                default:
                    break;

            }

            /*
             * Push new tag for egress port.
             */
            switch (egressTagCriterion.type()) {
                case VLAN_VID:
                    builder.pushVlan();
                    break;

                case MPLS_LABEL:
                    builder.pushMpls();
                    if (copyTtl) {
                        builder.copyTtlOut();
                    }
                    break;

                default:
                    break;

            }
        }

        switch (egressTagCriterion.type()) {
            case VLAN_VID:
                VlanIdCriterion vlanIdCriterion = (VlanIdCriterion) egressTagCriterion;
                builder.setVlanId(vlanIdCriterion.vlanId());
                break;

            case MPLS_LABEL:
                MplsCriterion mplsCriterion = (MplsCriterion) egressTagCriterion;
                builder.setMpls(mplsCriterion.label());
                break;

            case TUNNEL_ID:
                TunnelIdCriterion tunnelIdCriterion = (TunnelIdCriterion) egressTagCriterion;
                builder.setTunnelId(tunnelIdCriterion.tunnelId());
                break;

            default:
                break;

        }

        return builder.build();
    }

    /**
     * Update the selector builder using a L0 instruction.
     *
     * @param builder the builder to update
     * @param l0instruction the l0 instruction to use
     */
    private void updateBuilder(TrafficSelector.Builder builder, L0ModificationInstruction l0instruction) {
        throw new IntentCompilationException(UNSUPPORTED_L0);
    }

    /**
     * Update the selector builder using a L1 instruction.
     *
     * @param builder the builder to update
     * @param l1instruction the l1 instruction to use
     */
    private void updateBuilder(TrafficSelector.Builder builder, L1ModificationInstruction l1instruction) {
        throw new IntentCompilationException(UNSUPPORTED_L1);
    }

    /**
     * Update the selector builder using a L2 instruction.
     *
     * @param builder the builder to update
     * @param l2instruction the l2 instruction to use
     */
    private void updateBuilder(TrafficSelector.Builder builder, L2ModificationInstruction l2instruction) {
        switch (l2instruction.subtype()) {
            case ETH_SRC:
            case ETH_DST:
                ModEtherInstruction ethInstr = (ModEtherInstruction) l2instruction;
                switch (ethInstr.subtype()) {
                    case ETH_SRC:
                        builder.matchEthSrc(ethInstr.mac());
                        break;

                    case ETH_DST:
                        builder.matchEthDst(ethInstr.mac());
                        break;

                    default:
                        throw new IntentCompilationException(UNSUPPORTED_ETH_SUBTYPE);
                }
                break;

            case VLAN_ID:
                ModVlanIdInstruction vlanIdInstr = (ModVlanIdInstruction) l2instruction;
                builder.matchVlanId(vlanIdInstr.vlanId());
                break;

            case VLAN_PUSH:
                //FIXME
                break;

            case VLAN_POP:
                //TODO how do we handle dropped label? remove the selector?
                throw new IntentCompilationException(UNSUPPORTED_POP_ACTION);
            case VLAN_PCP:
                ModVlanPcpInstruction vlanPcpInstruction = (ModVlanPcpInstruction) l2instruction;
                builder.matchVlanPcp(vlanPcpInstruction.vlanPcp());
                break;

            case MPLS_LABEL:
            case MPLS_PUSH:
                //FIXME
                ModMplsLabelInstruction mplsInstr = (ModMplsLabelInstruction) l2instruction;
                builder.matchMplsLabel(mplsInstr.label());
                break;

            case MPLS_POP:
                //TODO how do we handle dropped label? remove the selector?
                throw new IntentCompilationException(UNSUPPORTED_POP_ACTION);
            case DEC_MPLS_TTL:
                // no-op
                break;

            case MPLS_BOS:
                ModMplsBosInstruction mplsBosInstr = (ModMplsBosInstruction) l2instruction;
                builder.matchMplsBos(mplsBosInstr.mplsBos());
                break;

            case TUNNEL_ID:
                ModTunnelIdInstruction tunInstr = (ModTunnelIdInstruction) l2instruction;
                builder.matchTunnelId(tunInstr.tunnelId());
                break;

            default:
                throw new IntentCompilationException(UNSUPPORTED_L2);
        }

    }

    /**
     * Update the selector builder using a L3 instruction.
     *
     * @param builder the builder to update
     * @param l3instruction the l3 instruction to use
     */
    private void updateBuilder(TrafficSelector.Builder builder, L3ModificationInstruction l3instruction) {
        // TODO check ethernet proto
        switch (l3instruction.subtype()) {
            case IPV4_SRC:
            case IPV4_DST:
            case IPV6_SRC:
            case IPV6_DST:
                ModIPInstruction ipInstr = (ModIPInstruction) l3instruction;
                // TODO check if ip falls in original prefix
                IpPrefix prefix = ipInstr.ip().toIpPrefix();
                switch (ipInstr.subtype()) {
                    case IPV4_SRC:
                        builder.matchIPSrc(prefix);
                        break;

                    case IPV4_DST:
                        builder.matchIPSrc(prefix);
                        break;

                    case IPV6_SRC:
                        builder.matchIPv6Src(prefix);
                        break;

                    case IPV6_DST:
                        builder.matchIPv6Dst(prefix);
                        break;

                    default:
                        throw new IntentCompilationException(UNSUPPORTED_IP_SUBTYPE);
                }
                break;

            case IPV6_FLABEL:
                ModIPv6FlowLabelInstruction ipFlowInstr = (ModIPv6FlowLabelInstruction) l3instruction;
                builder.matchIPv6FlowLabel(ipFlowInstr.flowLabel());
                break;

            case DEC_TTL:
                // no-op
                break;

            case TTL_OUT:
                // no-op
                break;

            case TTL_IN:
                // no-op
                break;

            case ARP_SPA:
                ModArpIPInstruction srcArpIpInstr = (ModArpIPInstruction) l3instruction;
                if (srcArpIpInstr.ip().isIp4()) {
                    builder.matchArpSpa((Ip4Address) srcArpIpInstr.ip());
                } else {
                    throw new IntentCompilationException(UNSUPPORTED_ARP);
                }
                break;

            case ARP_SHA:
                ModArpEthInstruction srcArpEthInstr = (ModArpEthInstruction) l3instruction;
                builder.matchArpSha(srcArpEthInstr.mac());
                break;

            case ARP_TPA:
                ModArpIPInstruction dstArpIpInstr = (ModArpIPInstruction) l3instruction;
                if (dstArpIpInstr.ip().isIp4()) {
                    builder.matchArpTpa((Ip4Address) dstArpIpInstr.ip());
                } else {
                    throw new IntentCompilationException(UNSUPPORTED_ARP);
                }
                break;

            case ARP_THA:
                ModArpEthInstruction dstArpEthInstr = (ModArpEthInstruction) l3instruction;
                builder.matchArpTha(dstArpEthInstr.mac());
                break;

            case ARP_OP:
                ModArpOpInstruction arpOpInstr = (ModArpOpInstruction) l3instruction;
                //FIXME is the long to int cast safe?
                builder.matchArpOp((int) arpOpInstr.op());
                break;

            default:
                throw new IntentCompilationException(UNSUPPORTED_L3);
        }
    }

    /**
     * Update the selector builder using a L4 instruction.
     *
     * @param builder the builder to update
     * @param l4instruction the l4 instruction to use
     */
    private void updateBuilder(TrafficSelector.Builder builder, L4ModificationInstruction l4instruction) {
        if (l4instruction instanceof ModTransportPortInstruction) {
            // TODO check IP proto
            ModTransportPortInstruction l4mod = (ModTransportPortInstruction) l4instruction;
            switch (l4mod.subtype()) {
                case TCP_SRC:
                    builder.matchTcpSrc(l4mod.port());
                    break;

                case TCP_DST:
                    builder.matchTcpDst(l4mod.port());
                    break;

                case UDP_SRC:
                    builder.matchUdpSrc(l4mod.port());
                    break;

                case UDP_DST:
                    builder.matchUdpDst(l4mod.port());
                    break;

                default:
                    throw new IntentCompilationException(UNSUPPORTED_L4_SUBTYPE);
            }
        } else {
            throw new IntentCompilationException(UNSUPPORTED_L4);
        }
    }

    /**
     * Update selector builder by using treatment.
     *
     * @param builder builder to update
     * @param treatment traffic treatment
     */
    private void updateBuilder(TrafficSelector.Builder builder, TrafficTreatment treatment) {

        treatment.allInstructions().forEach(instruction -> {
            switch (instruction.type()) {
                case L0MODIFICATION:
                    updateBuilder(builder, (L0ModificationInstruction) instruction);
                    break;

                case L1MODIFICATION:
                    updateBuilder(builder, (L1ModificationInstruction) instruction);
                    break;

                case L2MODIFICATION:
                    updateBuilder(builder, (L2ModificationInstruction) instruction);
                    break;

                case L3MODIFICATION:
                    updateBuilder(builder, (L3ModificationInstruction) instruction);
                    break;

                case L4MODIFICATION:
                    updateBuilder(builder, (L4ModificationInstruction) instruction);
                    break;

                case NOACTION:
                case OUTPUT:
                case GROUP:
                case QUEUE:
                case TABLE:
                case METER:
                case METADATA:
                case EXTENSION: // TODO is extension no-op or unsupported?
                    // Nothing to do
                    break;

                default:
                    throw new IntentCompilationException(UNSUPPORTED_INSTRUCTION);
            }
        });

    }

    /**
     * The method generates a selector starting from
     * the encapsulation information (type and label to match).
     *
     * @param selectorBuilder the builder to update
     * @param type the type of encapsulation
     * @param identifier the label to match
     */
    private void updateSelectorFromEncapsulation(TrafficSelector.Builder selectorBuilder,
                                                 EncapsulationType type,
                                                 Identifier<?> identifier) {
        switch (type) {
            case MPLS:
                MplsLabel label = (MplsLabel) identifier;
                selectorBuilder.matchMplsLabel(label);
                selectorBuilder.matchEthType(Ethernet.MPLS_UNICAST);
                break;

            case VLAN:
                VlanId id = (VlanId) identifier;
                selectorBuilder.matchVlanId(id);
                break;

            default:
                throw new IntentCompilationException(UNKNOWN_ENCAPSULATION);
        }
    }

    /**
     * Helper function to define the match on the ethertype.
     * If the selector define an ethertype we will use it,
     * otherwise IPv4 will be used by default.
     *
     * @param selector the traffic selector
     * @return the ethertype we should match
     */
    private EthType getEthType(TrafficSelector selector) {
        Criterion c = selector.getCriterion(Criterion.Type.ETH_TYPE);
        if (c != null && c instanceof EthTypeCriterion) {
            EthTypeCriterion ethertype = (EthTypeCriterion) c;
            return ethertype.ethType();
        }
        return EthType.EtherType.IPV4.ethType();
    }

}
