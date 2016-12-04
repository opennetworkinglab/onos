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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.EthType;
import org.onlab.packet.Ethernet;
import org.onlab.packet.MplsLabel;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.LinkKey;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.EthTypeCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction;
import org.onosproject.net.intent.FlowRuleIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentCompiler;
import org.onosproject.net.intent.IntentExtensionService;
import org.onosproject.net.intent.MplsPathIntent;
import org.onosproject.net.resource.Resource;
import org.onosproject.net.resource.ResourceAllocation;
import org.onosproject.net.resource.ResourceService;
import org.onosproject.net.resource.Resources;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.LinkKey.linkKey;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @deprecated in Goldeneye Release, in favour of encapsulation
 * constraint {@link org.onosproject.net.intent.constraint.EncapsulationConstraint}
 */
@Deprecated
@Component(immediate = true)
public class MplsPathIntentCompiler implements IntentCompiler<MplsPathIntent> {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentExtensionService intentExtensionService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ResourceService resourceService;

    protected ApplicationId appId;

    @Override
    public List<Intent> compile(MplsPathIntent intent, List<Intent> installable) {
        Map<LinkKey, MplsLabel> labels = assignMplsLabel(intent);
        List<FlowRule> rules = generateRules(intent, labels);

        return Collections.singletonList(new FlowRuleIntent(appId,
                                                            intent.key(),
                                                            rules,
                                                            intent.resources()));
    }

    @Activate
    public void activate() {
        appId = coreService.registerApplication("org.onosproject.net.intent");
        intentExtensionService.registerCompiler(MplsPathIntent.class, this);
    }

    @Deactivate
    public void deactivate() {
        intentExtensionService.unregisterCompiler(MplsPathIntent.class);
    }

    private Map<LinkKey, MplsLabel> assignMplsLabel(MplsPathIntent intent) {
        // TODO: do it better... Suggestions?
        Set<LinkKey> linkRequest = Sets.newHashSetWithExpectedSize(intent.path()
                .links().size() - 2);
        for (int i = 1; i <= intent.path().links().size() - 2; i++) {
            LinkKey link = linkKey(intent.path().links().get(i));
            linkRequest.add(link);
            // add the inverse link. I want that the label is reserved both for
            // the direct and inverse link
            linkRequest.add(linkKey(link.dst(), link.src()));
        }

        Map<LinkKey, MplsLabel> labels = findMplsLabels(linkRequest);
        if (labels.isEmpty()) {
            return Collections.emptyMap();
        }

        // for short term solution: same label is used for both directions
        // TODO: introduce the concept of Tx and Rx resources of a port
        Set<Resource> resources = labels.entrySet().stream()
                .flatMap(x -> Stream.of(
                        Resources.discrete(x.getKey().src().deviceId(), x.getKey().src().port(), x.getValue())
                                .resource(),
                        Resources.discrete(x.getKey().dst().deviceId(), x.getKey().dst().port(), x.getValue())
                                .resource()
                ))
                .collect(Collectors.toSet());
        List<ResourceAllocation> allocations =
                resourceService.allocate(intent.id(), ImmutableList.copyOf(resources));
        if (allocations.isEmpty()) {
            Collections.emptyMap();
        }

        return labels;
    }

    private Map<LinkKey, MplsLabel> findMplsLabels(Set<LinkKey> links) {
        Map<LinkKey, MplsLabel> labels = new HashMap<>();
        for (LinkKey link : links) {
            Set<MplsLabel> forward = findMplsLabel(link.src());
            Set<MplsLabel> backward = findMplsLabel(link.dst());
            Set<MplsLabel> common = Sets.intersection(forward, backward);
            if (common.isEmpty()) {
                continue;
            }
            labels.put(link, common.iterator().next());
        }

        return labels;
    }

    private Set<MplsLabel> findMplsLabel(ConnectPoint cp) {
        return resourceService.getAvailableResourceValues(
                Resources.discrete(cp.deviceId(), cp.port()).id(),
                MplsLabel.class);
    }

    private MplsLabel getMplsLabel(Map<LinkKey, MplsLabel> labels, LinkKey link) {
        return labels.get(link);
    }

    private List<FlowRule> generateRules(MplsPathIntent intent,
                                         Map<LinkKey, MplsLabel> labels) {

        Iterator<Link> links = intent.path().links().iterator();
        Link srcLink = links.next();
        ConnectPoint prev = srcLink.dst();

        Link link = links.next();
        // List of flow rules to be installed
        List<FlowRule> rules = new LinkedList<>();

        // Ingress traffic
        // Get the new MPLS label
        MplsLabel mpls = getMplsLabel(labels, linkKey(link));
        checkNotNull(mpls);
        MplsLabel prevLabel = mpls;
        rules.add(ingressFlow(prev.port(), link, intent, mpls));

        prev = link.dst();

        while (links.hasNext()) {

            link = links.next();

            if (links.hasNext()) {
                // Transit traffic
                // Get the new MPLS label
                mpls = getMplsLabel(labels, linkKey(link));
                checkNotNull(mpls);
                rules.add(transitFlow(prev.port(), link, intent,
                        prevLabel, mpls));
                prevLabel = mpls;

            } else {
                // Egress traffic
                rules.add(egressFlow(prev.port(), link, intent,
                        prevLabel));
            }

            prev = link.dst();
        }
        return rules;
    }

    private FlowRule ingressFlow(PortNumber inPort, Link link,
                                 MplsPathIntent intent,
                                 MplsLabel label) {

        TrafficSelector.Builder ingressSelector = DefaultTrafficSelector
                .builder(intent.selector());
        TrafficTreatment.Builder treat = DefaultTrafficTreatment.builder();
        ingressSelector.matchInPort(inPort);

        if (intent.ingressLabel().isPresent()) {
            ingressSelector.matchEthType(Ethernet.MPLS_UNICAST)
                    .matchMplsLabel(intent.ingressLabel().get());

            // Swap the MPLS label
            treat.setMpls(label);
        } else {
            // Push and set the MPLS label
            treat.pushMpls().setMpls(label);
        }
        // Add the output action
        treat.setOutput(link.src().port());

        return createFlowRule(intent, link.src().deviceId(), ingressSelector.build(), treat.build());
    }

    private FlowRule transitFlow(PortNumber inPort, Link link,
                                 MplsPathIntent intent,
                                 MplsLabel prevLabel,
                                 MplsLabel outLabel) {

        // Ignore the ingress Traffic Selector and use only the MPLS label
        // assigned in the previous link
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchInPort(inPort).matchEthType(Ethernet.MPLS_UNICAST)
                .matchMplsLabel(prevLabel);
        TrafficTreatment.Builder treat = DefaultTrafficTreatment.builder();

        // Set the new label only if the label on the packet is
        // different
        if (!prevLabel.equals(outLabel)) {
            treat.setMpls(outLabel);
        }

        treat.setOutput(link.src().port());
        return createFlowRule(intent, link.src().deviceId(), selector.build(), treat.build());
    }

    private FlowRule egressFlow(PortNumber inPort, Link link,
                                MplsPathIntent intent,
                                MplsLabel prevLabel) {
        // egress point: either set the egress MPLS label or pop the
        // MPLS label based on the intent annotations

        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchInPort(inPort).matchEthType(Ethernet.MPLS_UNICAST)
                .matchMplsLabel(prevLabel);

        // apply the intent's treatments
        TrafficTreatment.Builder treat = DefaultTrafficTreatment.builder(intent
                .treatment());

        // check if the treatement is popVlan or setVlan (rewrite),
        // than selector needs to match any VlanId
        for (Instruction instruct : intent.treatment().allInstructions()) {
            if (instruct instanceof L2ModificationInstruction) {
                L2ModificationInstruction l2Mod = (L2ModificationInstruction) instruct;
                if (l2Mod.subtype() == L2ModificationInstruction.L2SubType.VLAN_PUSH) {
                    break;
                }
                if (l2Mod.subtype() == L2ModificationInstruction.L2SubType.VLAN_POP ||
                        l2Mod.subtype() == L2ModificationInstruction.L2SubType.VLAN_ID) {
                    selector.matchVlanId(VlanId.ANY);
                }
            }
        }

        if (intent.egressLabel().isPresent()) {
            treat.setMpls(intent.egressLabel().get());
        } else {
                treat.popMpls(outputEthType(intent.selector()));
        }
        treat.setOutput(link.src().port());
        return createFlowRule(intent, link.src().deviceId(),
                selector.build(), treat.build());
    }

    protected FlowRule createFlowRule(MplsPathIntent intent, DeviceId deviceId,
                                      TrafficSelector selector, TrafficTreatment treat) {
        return DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector)
                .withTreatment(treat)
                .withPriority(intent.priority())
                .fromApp(appId)
                .makePermanent()
                .build();
    }

    // if the ingress ethertype is defined, the egress traffic
    // will be use that value, otherwise the IPv4 ethertype is used.
    private EthType outputEthType(TrafficSelector selector) {
        Criterion c = selector.getCriterion(Criterion.Type.ETH_TYPE);
        if (c != null && c instanceof EthTypeCriterion) {
            EthTypeCriterion ethertype = (EthTypeCriterion) c;
            return ethertype.ethType();
        } else {
            return EthType.EtherType.IPV4.ethType();
        }
    }
}
