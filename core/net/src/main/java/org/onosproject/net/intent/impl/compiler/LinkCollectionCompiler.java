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

package org.onosproject.net.intent.impl.compiler;

import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpPrefix;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;
import org.onosproject.net.FilteredConnectPoint;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.MplsCriterion;
import org.onosproject.net.flow.criteria.TunnelIdCriterion;
import org.onosproject.net.flow.criteria.VlanIdCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.L0ModificationInstruction;
import org.onosproject.net.flow.instructions.L1ModificationInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.ModEtherInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.ModVlanIdInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.ModVlanPcpInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.ModMplsLabelInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.ModMplsBosInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.ModTunnelIdInstruction;
import org.onosproject.net.flow.instructions.L3ModificationInstruction;
import org.onosproject.net.flow.instructions.L3ModificationInstruction.ModIPInstruction;
import org.onosproject.net.flow.instructions.L3ModificationInstruction.ModIPv6FlowLabelInstruction;
import org.onosproject.net.flow.instructions.L3ModificationInstruction.ModArpIPInstruction;
import org.onosproject.net.flow.instructions.L3ModificationInstruction.ModArpEthInstruction;
import org.onosproject.net.flow.instructions.L3ModificationInstruction.ModArpOpInstruction;
import org.onosproject.net.flow.instructions.L4ModificationInstruction;
import org.onosproject.net.flow.instructions.L4ModificationInstruction.ModTransportPortInstruction;
import org.onosproject.net.intent.IntentCompilationException;
import org.onosproject.net.intent.LinkCollectionIntent;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.onosproject.net.flow.criteria.Criterion.Type.MPLS_LABEL;
import static org.onosproject.net.flow.criteria.Criterion.Type.TUNNEL_ID;
import static org.onosproject.net.flow.criteria.Criterion.Type.VLAN_VID;


/**
 * Shared APIs and implementations for Link Collection compilers.
 */
public class LinkCollectionCompiler<T> {

    private static final Set<Criterion.Type> TAG_CRITERION_TYPES =
            Sets.immutableEnumSet(VLAN_VID, MPLS_LABEL, TUNNEL_ID);

    /**
     * Helper class to encapsulate treatment and selector.
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
     * Helper method to compute input and output ports.
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
     * Gets ingress and egress port number of specific device.
     *
     * @param intent the related
     * @param deviceId device Id
     * @param ingressPorts the ingress ports to compute
     * @param egressPorts the egress ports to compute
     */
    protected void computePorts(LinkCollectionIntent intent,
                                DeviceId deviceId,
                                Set<PortNumber> ingressPorts,
                                Set<PortNumber> egressPorts) {

        if (!intent.applyTreatmentOnEgress()) {
            ingressPorts.addAll(intent.ingressPoints().stream()
                    .filter(point -> point.deviceId().equals(deviceId))
                    .map(ConnectPoint::port)
                    .collect(Collectors.toSet()));
        } else {
            egressPorts.addAll(intent.egressPoints().stream()
                    .filter(point -> point.deviceId().equals(deviceId))
                    .map(ConnectPoint::port)
                    .collect(Collectors.toSet()));
        }

    }

    /**
     * Creates the flows representations.
     *
     * @param intent the intent to compile
     * @param deviceId the affected device
     * @param inPorts the input ports
     * @param outPorts the output ports
     * @return the list of flows representations
     */
    protected List<T> createRules(LinkCollectionIntent intent, DeviceId deviceId,
                                       Set<PortNumber> inPorts, Set<PortNumber> outPorts) {
        return null;
    }


    /**
     * Computes treatment and selector which will be used
     * in the flow representation (Rule, Objective).
     *
     * @param intent the intent to compile
     * @param inPort the input port of this device
     * @param deviceId the current device
     * @param outPorts the output ports of this device
     * @param ingressPorts intent ingress ports of this device
     * @param egressPorts intent egress ports of this device
     * @return the forwarding instruction object which encapsulates treatment and selector
     */
    protected ForwardingInstructions createForwardingInstructions(LinkCollectionIntent intent,
                                                                  PortNumber inPort,
                                                                  DeviceId deviceId,
                                                                  Set<PortNumber> outPorts,
                                                                  Set<PortNumber> ingressPorts,
                                                                  Set<PortNumber> egressPorts) {

        TrafficTreatment.Builder treatmentBuilder = DefaultTrafficTreatment.builder();
        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder(intent.selector());
        selectorBuilder.matchInPort(inPort);

        if (!intent.applyTreatmentOnEgress()) {
            // FIXME: currently, we assume this intent is compile from mp2sp intent
            Optional<FilteredConnectPoint> filteredIngressPoint =
                    getFilteredConnectPointFromIntent(deviceId, inPort, intent);
            Optional<FilteredConnectPoint> filteredEgressPoint =
                    intent.filteredEgressPoints().stream().findFirst();

            if (filteredIngressPoint.isPresent()) {
                // Ingress device
                intent.treatment().allInstructions().stream()
                        .filter(inst -> inst.type() != Instruction.Type.NOACTION)
                        .forEach(treatmentBuilder::add);

                if (filteredEgressPoint.isPresent()) {
                    // Apply selector from ingress point
                    filteredIngressPoint.get()
                            .trafficSelector()
                            .criteria()
                            .forEach(selectorBuilder::add);

                    TrafficTreatment forwardingTreatment =
                            forwardingTreatment(filteredIngressPoint.get(),
                                                filteredEgressPoint.get());

                    forwardingTreatment.allInstructions().stream()
                            .filter(inst -> inst.type() != Instruction.Type.NOACTION)
                            .forEach(treatmentBuilder::add);
                } else {
                    throw new IntentCompilationException("Can't find filtered connection point");
                }

            } else {
                // Not ingress device, won't apply treatments.
                // Use selector by treatment from intent.
                updateBuilder(selectorBuilder, intent.treatment());

                // Selector should be overridden by selector from connect point.
                if (filteredEgressPoint.isPresent()) {
                    filteredEgressPoint.get()
                            .trafficSelector()
                            .criteria()
                            .forEach(selectorBuilder::add);
                }

            }

            outPorts.forEach(treatmentBuilder::setOutput);

        } else {
            // FIXME: currently, we assume this intent is compile from sp2mp intent
            Optional<FilteredConnectPoint> filteredIngressPoint =
                    intent.filteredIngressPoints().stream().findFirst();

            if (filteredIngressPoint.isPresent()) {
                // Apply selector from ingress point
                filteredIngressPoint.get()
                        .trafficSelector()
                        .criteria()
                        .forEach(selectorBuilder::add);
            } else {
                throw new IntentCompilationException(
                        "Filtered connection point for ingress" +
                                "point does not exist");
            }

            for (PortNumber outPort : outPorts) {
                Optional<FilteredConnectPoint> filteredEgressPoint =
                        getFilteredConnectPointFromIntent(deviceId, outPort, intent);

                if (filteredEgressPoint.isPresent()) {
                    // Egress port, apply treatment + forwarding treatment
                    intent.treatment().allInstructions().stream()
                            .filter(inst -> inst.type() != Instruction.Type.NOACTION)
                            .forEach(treatmentBuilder::add);

                    TrafficTreatment forwardingTreatment =
                            forwardingTreatment(filteredIngressPoint.get(),
                                                filteredEgressPoint.get());
                    forwardingTreatment.allInstructions().stream()
                            .filter(inst -> inst.type() != Instruction.Type.NOACTION)
                            .forEach(treatmentBuilder::add);
                }

                treatmentBuilder.setOutput(outPort);

            }

        }


        return new ForwardingInstructions(treatmentBuilder.build(), selectorBuilder.build());

    }

    /**
     * Get FilteredConnectPoint from LinkCollectionIntent.
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
     * @param ingress ingress point for the intent
     * @param egress egress point for the intent
     * @return Builder of TrafficTreatment
     */
    private TrafficTreatment forwardingTreatment(FilteredConnectPoint ingress,
                                                         FilteredConnectPoint egress) {


        if (ingress.trafficSelector().equals(egress.trafficSelector())) {
            return DefaultTrafficTreatment.emptyTreatment();
        }

        TrafficTreatment.Builder builder = DefaultTrafficTreatment.builder();

        /*
         * "null" means there is no tag for the port
         * Tag criterion will be null if port is normal connection point
         */
        Criterion ingressTagCriterion = getTagCriterion(ingress.trafficSelector());
        Criterion egressTagCriterion = getTagCriterion(egress.trafficSelector());

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
                    builder.popMpls();
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
        throw new IntentCompilationException("L0 not supported");
    }

    /**
     * Update the selector builder using a L1 instruction.
     *
     * @param builder the builder to update
     * @param l1instruction the l1 instruction to use
     */
    private void updateBuilder(TrafficSelector.Builder builder, L1ModificationInstruction l1instruction) {
        throw new IntentCompilationException("L1 not supported");
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
                        throw new IntentCompilationException("Bad eth subtype");
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
                throw new IntentCompilationException("Can't handle pop label");
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
                throw new IntentCompilationException("Can't handle pop label");
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
                throw new IntentCompilationException("Unknown L2 Modification instruction");
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
                        throw new IntentCompilationException("Bad type for IP instruction");
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
                ModArpIPInstruction arpIpInstr = (ModArpIPInstruction) l3instruction;
                if (arpIpInstr.ip().isIp4()) {
                    builder.matchArpSpa((Ip4Address) arpIpInstr.ip());
                } else {
                    throw new IntentCompilationException("IPv6 not supported for ARP");
                }
                break;
            case ARP_SHA:
                ModArpEthInstruction arpEthInstr = (ModArpEthInstruction) l3instruction;
                builder.matchArpSha(arpEthInstr.mac());
                break;
            case ARP_OP:
                ModArpOpInstruction arpOpInstr = (ModArpOpInstruction) l3instruction;
                //FIXME is the long to int cast safe?
                builder.matchArpOp((int) arpOpInstr.op());
                break;
            default:
                throw new IntentCompilationException("Unknown L3 Modification instruction");
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
                    throw new IntentCompilationException("Unknown L4 Modification instruction");
            }
        } else {
            throw new IntentCompilationException("Unknown L4 Modification instruction");
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
                    throw new IntentCompilationException("Unknown instruction type");
            }
        });

    }

}
