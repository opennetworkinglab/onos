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
package org.onosproject.net.intent.impl.compiler;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpPrefix;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
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
import org.onosproject.net.flow.instructions.L4ModificationInstruction.ModTransportPortInstruction;
import org.onosproject.net.intent.FlowRuleIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentCompilationException;
import org.onosproject.net.intent.IntentCompiler;
import org.onosproject.net.intent.LinkCollectionIntent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component(immediate = true)
public class LinkCollectionIntentCompiler implements IntentCompiler<LinkCollectionIntent> {

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentConfigurableRegistrator registrator;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    private ApplicationId appId;

    @Activate
    public void activate() {
        appId = coreService.registerApplication("org.onosproject.net.intent");
        registrator.registerCompiler(LinkCollectionIntent.class, this, false);
    }

    @Deactivate
    public void deactivate() {
        registrator.unregisterCompiler(LinkCollectionIntent.class, false);
    }

    @Override
    public List<Intent> compile(LinkCollectionIntent intent, List<Intent> installable) {
        SetMultimap<DeviceId, PortNumber> inputPorts = HashMultimap.create();
        SetMultimap<DeviceId, PortNumber> outputPorts = HashMultimap.create();

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

        List<FlowRule> rules = new ArrayList<>();
        for (DeviceId deviceId: outputPorts.keys()) {
            rules.addAll(createRules(intent, deviceId, inputPorts.get(deviceId), outputPorts.get(deviceId)));
        }
        return Collections.singletonList(new FlowRuleIntent(appId, rules, intent.resources()));
    }

    private List<FlowRule> createRules(LinkCollectionIntent intent, DeviceId deviceId,
                                       Set<PortNumber> inPorts, Set<PortNumber> outPorts) {
        TrafficTreatment.Builder defaultTreatmentBuilder = DefaultTrafficTreatment.builder();
        outPorts.stream()
                .forEach(defaultTreatmentBuilder::setOutput);
        TrafficTreatment outputOnlyTreatment = defaultTreatmentBuilder.build();
        Set<PortNumber> ingressPorts = Collections.emptySet();
        Set<PortNumber> egressPorts = Collections.emptySet();

        if (!intent.applyTreatmentOnEgress()) {
            ingressPorts = intent.ingressPoints().stream()
                    .filter(point -> point.deviceId().equals(deviceId))
                    .map(ConnectPoint::port)
                    .collect(Collectors.toSet());
        } else {
            egressPorts = intent.egressPoints().stream()
                    .filter(point -> point.deviceId().equals(deviceId))
                    .map(ConnectPoint::port)
                    .collect(Collectors.toSet());
        }

        List<FlowRule> rules = new ArrayList<>(inPorts.size());
        for (PortNumber inPort: inPorts) {
            TrafficSelector.Builder selectorBuilder;
            TrafficTreatment treatment;
            TrafficTreatment intentTreatment;

            if (!intent.applyTreatmentOnEgress()) {
                TrafficTreatment.Builder ingressTreatmentBuilder = DefaultTrafficTreatment.builder(intent.treatment());
                outPorts.stream()
                        .forEach(ingressTreatmentBuilder::setOutput);
                intentTreatment = ingressTreatmentBuilder.build();

                if (ingressPorts.contains(inPort)) {
                    selectorBuilder = DefaultTrafficSelector.builder(intent.selector());
                    treatment = intentTreatment;
                } else {
                    selectorBuilder = applyTreatmentToSelector(intent.selector(), intentTreatment);
                    treatment = outputOnlyTreatment;
                }
            } else {
                if (outPorts.stream().allMatch(egressPorts::contains)) {
                    TrafficTreatment.Builder egressTreatmentBuilder =
                            DefaultTrafficTreatment.builder(intent.treatment());
                    outPorts.stream()
                            .forEach(egressTreatmentBuilder::setOutput);

                    selectorBuilder = DefaultTrafficSelector.builder(intent.selector());
                    treatment = egressTreatmentBuilder.build();
                } else {
                    selectorBuilder = DefaultTrafficSelector.builder(intent.selector());
                    treatment = outputOnlyTreatment;
                }
            }
            TrafficSelector selector = selectorBuilder.matchInPort(inPort).build();

            FlowRule rule = DefaultFlowRule.builder()
                    .forDevice(deviceId)
                    .withSelector(selector)
                    .withTreatment(treatment)
                    .withPriority(intent.priority())
                    .fromApp(appId)
                    .makePermanent()
                    .build();
            rules.add(rule);
        }

        return rules;
    }

    private TrafficSelector.Builder applyTreatmentToSelector(TrafficSelector selector, TrafficTreatment treatment) {
        TrafficSelector.Builder defaultSelectorBuilder = DefaultTrafficSelector.builder(selector);
        treatment.allInstructions().forEach(instruction -> {
            switch (instruction.type()) {
                case L0MODIFICATION:
                case L1MODIFICATION:
                    throw new IntentCompilationException("L0 and L1 mods not supported");
                case L2MODIFICATION:
                    L2ModificationInstruction l2mod = (L2ModificationInstruction) instruction;
                    switch (l2mod.subtype()) {
                        case ETH_SRC:
                        case ETH_DST:
                            ModEtherInstruction ethInstr = (ModEtherInstruction) l2mod;
                            switch (ethInstr.subtype()) {
                                case ETH_SRC:
                                    defaultSelectorBuilder.matchEthSrc(ethInstr.mac());
                                    break;
                                case ETH_DST:
                                    defaultSelectorBuilder.matchEthDst(ethInstr.mac());
                                    break;
                                default:
                                    throw new IntentCompilationException("Bad eth subtype");
                            }
                            break;
                        case VLAN_ID:
                            ModVlanIdInstruction vlanIdInstr = (ModVlanIdInstruction) l2mod;
                            defaultSelectorBuilder.matchVlanId(vlanIdInstr.vlanId());
                            break;
                        case VLAN_PUSH:
                            //FIXME
                            break;
                        case VLAN_POP:
                            //TODO how do we handle dropped label? remove the selector?
                            throw new IntentCompilationException("Can't handle pop label");
                        case VLAN_PCP:
                            ModVlanPcpInstruction vlanPcpInstruction = (ModVlanPcpInstruction) l2mod;
                            defaultSelectorBuilder.matchVlanPcp(vlanPcpInstruction.vlanPcp());
                            break;
                        case MPLS_LABEL:
                        case MPLS_PUSH:
                            //FIXME
                            ModMplsLabelInstruction mplsInstr = (ModMplsLabelInstruction) l2mod;
                            defaultSelectorBuilder.matchMplsLabel(mplsInstr.label());
                            break;
                        case MPLS_POP:
                            //TODO how do we handle dropped label? remove the selector?
                            throw new IntentCompilationException("Can't handle pop label");
                        case DEC_MPLS_TTL:
                            // no-op
                            break;
                        case MPLS_BOS:
                            ModMplsBosInstruction mplsBosInstr = (ModMplsBosInstruction) l2mod;
                            defaultSelectorBuilder.matchMplsBos(mplsBosInstr.mplsBos());
                            break;
                        case TUNNEL_ID:
                            ModTunnelIdInstruction tunInstr = (ModTunnelIdInstruction) l2mod;
                            defaultSelectorBuilder.matchTunnelId(tunInstr.tunnelId());
                            break;
                        default:
                            throw new IntentCompilationException("Unknown L2 Modification instruction");
                    }
                    break;
                case L3MODIFICATION:
                    L3ModificationInstruction l3mod = (L3ModificationInstruction) instruction;
                    // TODO check ethernet proto
                    switch (l3mod.subtype()) {
                        case IPV4_SRC:
                        case IPV4_DST:
                        case IPV6_SRC:
                        case IPV6_DST:
                            ModIPInstruction ipInstr = (ModIPInstruction) l3mod;
                            // TODO check if ip falls in original prefix
                            IpPrefix prefix = ipInstr.ip().toIpPrefix();
                            switch (ipInstr.subtype()) {
                                case IPV4_SRC:
                                    defaultSelectorBuilder.matchIPSrc(prefix);
                                    break;
                                case IPV4_DST:
                                    defaultSelectorBuilder.matchIPSrc(prefix);
                                    break;
                                case IPV6_SRC:
                                    defaultSelectorBuilder.matchIPv6Src(prefix);
                                    break;
                                case IPV6_DST:
                                    defaultSelectorBuilder.matchIPv6Dst(prefix);
                                    break;
                                default:
                                    throw new IntentCompilationException("Bad type for IP instruction");
                            }
                            break;
                        case IPV6_FLABEL:
                            ModIPv6FlowLabelInstruction ipFlowInstr = (ModIPv6FlowLabelInstruction) l3mod;
                            defaultSelectorBuilder.matchIPv6FlowLabel(ipFlowInstr.flowLabel());
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
                            ModArpIPInstruction arpIpInstr = (ModArpIPInstruction) l3mod;
                            if (arpIpInstr.ip().isIp4()) {
                                defaultSelectorBuilder.matchArpSpa((Ip4Address) arpIpInstr.ip());
                            } else {
                                throw new IntentCompilationException("IPv6 not supported for ARP");
                            }
                            break;
                        case ARP_SHA:
                            ModArpEthInstruction arpEthInstr = (ModArpEthInstruction) l3mod;
                            defaultSelectorBuilder.matchArpSha(arpEthInstr.mac());
                            break;
                        case ARP_OP:
                            ModArpOpInstruction arpOpInstr = (ModArpOpInstruction) l3mod;
                            //FIXME is the long to int cast safe?
                            defaultSelectorBuilder.matchArpOp((int) arpOpInstr.op());
                            break;
                        default:
                            throw new IntentCompilationException("Unknown L3 Modification instruction");
                    }
                    break;
                case L4MODIFICATION:
                    if (instruction instanceof ModTransportPortInstruction) {
                        // TODO check IP proto
                        ModTransportPortInstruction l4mod = (ModTransportPortInstruction) instruction;
                        switch (l4mod.subtype()) {
                            case TCP_SRC:
                                defaultSelectorBuilder.matchTcpSrc(l4mod.port());
                                break;
                            case TCP_DST:
                                defaultSelectorBuilder.matchTcpDst(l4mod.port());
                                break;
                            case UDP_SRC:
                                defaultSelectorBuilder.matchUdpSrc(l4mod.port());
                                break;
                            case UDP_DST:
                                defaultSelectorBuilder.matchUdpDst(l4mod.port());
                                break;
                            default:
                                throw new IntentCompilationException("Unknown L4 Modification instruction");
                        }
                    } else {
                        throw new IntentCompilationException("Unknown L4 Modification instruction");
                    }
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