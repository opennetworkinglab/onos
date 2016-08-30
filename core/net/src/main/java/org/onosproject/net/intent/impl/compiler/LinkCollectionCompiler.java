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
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpPrefix;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
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
import org.onosproject.net.intent.IntentCompilationException;
import org.onosproject.net.intent.LinkCollectionIntent;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Shared APIs and implementations for Link Collection compilers.
 */
public class LinkCollectionCompiler<T> {

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
     * Helper method to compute input and ouput ports.
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
     * Helper method to compute ingress and egress ports.
     *
     * @param intent the related intents
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
     * @param inPort the input port
     * @param deviceId the current device
     * @param outPorts the output ports
     * @param ingressPorts the ingress ports
     * @param egressPorts the egress ports
     * @return the forwarding instruction object which encapsulates treatment and selector
     */
    protected ForwardingInstructions createForwardingInstructions(LinkCollectionIntent intent,
                                                                  PortNumber inPort,
                                                                  DeviceId deviceId,
                                                                  Set<PortNumber> outPorts,
                                                                  Set<PortNumber> ingressPorts,
                                                                  Set<PortNumber> egressPorts) {

        TrafficTreatment.Builder defaultTreatmentBuilder = DefaultTrafficTreatment.builder();
        outPorts.forEach(defaultTreatmentBuilder::setOutput);
        TrafficTreatment outputOnlyTreatment = defaultTreatmentBuilder.build();
        TrafficSelector.Builder selectorBuilder;
        TrafficTreatment treatment;
        TrafficTreatment intentTreatment;

        if (!intent.applyTreatmentOnEgress()) {
            TrafficTreatment.Builder ingressTreatmentBuilder = DefaultTrafficTreatment.builder(intent.treatment());
            outPorts.forEach(ingressTreatmentBuilder::setOutput);
            intentTreatment = ingressTreatmentBuilder.build();

            if (ingressPorts.contains(inPort)) {
                if (intent.ingressSelectors() != null && !intent.ingressSelectors().isEmpty()) {
                    /**
                     * We iterate on the ingress points looking for the connect point
                     * associated to inPort.
                     */
                    Optional<ConnectPoint> connectPoint = intent.ingressPoints()
                            .stream()
                            .filter(ingressPoint -> ingressPoint.port().equals(inPort)
                                    && ingressPoint.deviceId().equals(deviceId))
                            .findFirst();
                    if (connectPoint.isPresent()) {
                        selectorBuilder = DefaultTrafficSelector
                                .builder(intent.ingressSelectors().get(connectPoint.get()));
                    } else {
                        throw new IntentCompilationException("Looking for connect point associated to the selector." +
                                                                     "inPort not in IngressPoints");
                    }
                } else {
                    selectorBuilder = DefaultTrafficSelector.builder(intent.selector());
                }
                treatment = this.updateBuilder(ingressTreatmentBuilder, selectorBuilder.build()).build();
            } else {
                selectorBuilder = this.createSelectorFromFwdInstructions(
                        new ForwardingInstructions(intentTreatment, intent.selector())
                );
                treatment = outputOnlyTreatment;
            }
        } else {
            if (outPorts.stream().allMatch(egressPorts::contains)) {
                TrafficTreatment.Builder egressTreatmentBuilder = DefaultTrafficTreatment.builder();
                if (intent.egressTreatments() != null && !intent.egressTreatments().isEmpty()) {
                    for (PortNumber outPort : outPorts) {
                        Optional<ConnectPoint> connectPoint = intent.egressPoints()
                                .stream()
                                .filter(egressPoint -> egressPoint.port().equals(outPort)
                                        && egressPoint.deviceId().equals(deviceId))
                                .findFirst();
                        if (connectPoint.isPresent()) {
                            TrafficTreatment egressTreatment = intent.egressTreatments().get(connectPoint.get());
                            this.addTreatment(egressTreatmentBuilder, egressTreatment);
                            egressTreatmentBuilder = this.updateBuilder(egressTreatmentBuilder, intent.selector());
                            egressTreatmentBuilder.setOutput(outPort);
                        } else {
                            throw new IntentCompilationException("Looking for connect point associated to " +
                                                                         "the treatment. outPort not in egressPoints");
                        }
                    }
                } else {
                    egressTreatmentBuilder = this
                            .updateBuilder(DefaultTrafficTreatment.builder(intent.treatment()), intent.selector());
                    outPorts.forEach(egressTreatmentBuilder::setOutput);
                }
                selectorBuilder = DefaultTrafficSelector.builder(intent.selector());
                treatment = egressTreatmentBuilder.build();
            } else {
                selectorBuilder = DefaultTrafficSelector.builder(intent.selector());
                treatment = outputOnlyTreatment;
            }
        }

        TrafficSelector selector = selectorBuilder.matchInPort(inPort).build();

        return new ForwardingInstructions(treatment, selector);

    }

    /**
     * Update a builder using a treatment.
     * @param builder the builder to update
     * @param treatment the treatment to add
     * @return the new builder
     */
    private TrafficTreatment.Builder addTreatment(TrafficTreatment.Builder builder, TrafficTreatment treatment) {
        builder.deferred();
        for (Instruction instruction : treatment.deferred()) {
            builder.add(instruction);
        }
        builder.immediate();
        for (Instruction instruction : treatment.immediate()) {
            builder.add(instruction);
        }
        return builder;
    }

    /**
     * Update the original builder with the necessary operations
     * to have a correct forwarding given an ingress selector.
     * TODO
     * This means that if the ingress selectors match on different vlanids and
     * the egress treatment rewrite the vlanid the forwarding works
     * but if we need to push for example an mpls label at the egress
     * we need to implement properly this method.
     *
     * @param treatmentBuilder the builder to modify
     * @param intentSelector the intent selector to use as input
     * @return the new treatment created
     */
    private TrafficTreatment.Builder updateBuilder(TrafficTreatment.Builder treatmentBuilder,
                                                   TrafficSelector intentSelector) {
        return treatmentBuilder;
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
     * Computes the new traffic selector using the
     * forwarding instructions.
     *
     * @param fwInstructions it encapsulates the instructions to compute the new selector
     * @return the traffic selector builder
     */
    private TrafficSelector.Builder createSelectorFromFwdInstructions(ForwardingInstructions fwInstructions) {
            TrafficSelector.Builder defaultSelectorBuilder = DefaultTrafficSelector.builder(fwInstructions.selector());
            fwInstructions.treatment().allInstructions().forEach(instruction -> {
                switch (instruction.type()) {
                    case L0MODIFICATION:
                        updateBuilder(defaultSelectorBuilder, (L0ModificationInstruction) instruction);
                        break;
                    case L1MODIFICATION:
                        updateBuilder(defaultSelectorBuilder, (L1ModificationInstruction) instruction);
                        break;
                    case L2MODIFICATION:
                        updateBuilder(defaultSelectorBuilder, (L2ModificationInstruction) instruction);
                        break;
                    case L3MODIFICATION:
                        updateBuilder(defaultSelectorBuilder, (L3ModificationInstruction) instruction);
                        break;
                    case L4MODIFICATION:
                        updateBuilder(defaultSelectorBuilder, (L4ModificationInstruction) instruction);
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
            return defaultSelectorBuilder;
    }

}
