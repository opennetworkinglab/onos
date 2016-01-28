/*
 * Copyright 2015 Open Networking Laboratory
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
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.EncapsulationType;
import org.onosproject.net.Link;
import org.onosproject.net.LinkKey;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.VlanIdCriterion;
import org.onosproject.net.intent.FlowRuleIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentCompiler;
import org.onosproject.net.intent.IntentExtensionService;
import org.onosproject.net.intent.PathIntent;
import org.onosproject.net.intent.constraint.EncapsulationConstraint;
import org.onosproject.net.intent.impl.IntentCompilationException;
import org.onosproject.net.newresource.Resource;
import org.onosproject.net.newresource.ResourceService;
import org.onosproject.net.newresource.Resources;
import org.onosproject.net.resource.link.LinkResourceAllocations;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.onosproject.net.LinkKey.linkKey;
import static org.slf4j.LoggerFactory.getLogger;

@Component(immediate = true)
public class PathIntentCompiler implements IntentCompiler<PathIntent> {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentExtensionService intentManager;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ResourceService resourceService;

    private ApplicationId appId;

    @Activate
    public void activate() {
        appId = coreService.registerApplication("org.onosproject.net.intent");
        intentManager.registerCompiler(PathIntent.class, this);
    }

    @Deactivate
    public void deactivate() {
        intentManager.unregisterCompiler(PathIntent.class);
    }

    @Override
    public List<Intent> compile(PathIntent intent, List<Intent> installable,
                                Set<LinkResourceAllocations> resources) {
        // Note: right now recompile is not considered
        // TODO: implement recompile behavior

        List<Link> links = intent.path().links();
        List<FlowRule> rules = new LinkedList<>();

        Optional<EncapsulationConstraint> enacpConstraint = intent.constraints().stream()
                .filter(constraint -> constraint instanceof EncapsulationConstraint)
                .map(x -> (EncapsulationConstraint) x).findAny();
        //if no encapsulation or is involved only a single switch use the default behaviour
        if (!enacpConstraint.isPresent() || links.size() == 1) {

            for (int i = 0; i < links.size() - 1; i++) {
                ConnectPoint ingress = links.get(i).dst();
                ConnectPoint egress = links.get(i + 1).src();
                FlowRule rule = createFlowRule(intent.selector(), intent.treatment(),
                                               ingress, egress, intent.priority(),
                                               isLast(links, i));
                rules.add(rule);
            }

            return ImmutableList.of(new FlowRuleIntent(appId, null, rules, intent.resources()));

        } else {
            if (EncapsulationType.VLAN == enacpConstraint.get().encapType()) {
                rules = manageVlanEncap(intent);
            }
            if (EncapsulationType.MPLS == enacpConstraint.get().encapType()) {
                //TODO: to be implemented
                rules = Collections.emptyList();
            }

            return ImmutableList.of(new FlowRuleIntent(appId, null, rules, intent.resources()));
        }
    }

    private FlowRule createFlowRule(TrafficSelector originalSelector, TrafficTreatment originalTreatment,
                                    ConnectPoint ingress, ConnectPoint egress,
                                    int priority, boolean applyTreatment) {
        TrafficSelector selector = DefaultTrafficSelector.builder(originalSelector)
                .matchInPort(ingress.port())
                .build();

        TrafficTreatment.Builder treatmentBuilder;
        if (applyTreatment) {
            treatmentBuilder = DefaultTrafficTreatment.builder(originalTreatment);
        } else {
            treatmentBuilder = DefaultTrafficTreatment.builder();
        }
        TrafficTreatment treatment = treatmentBuilder.setOutput(egress.port()).build();

        return DefaultFlowRule.builder()
                .forDevice(ingress.deviceId())
                .withSelector(selector)
                .withTreatment(treatment)
                .withPriority(priority)
                .fromApp(appId)
                .makePermanent()
                .build();
    }

    private List<FlowRule> manageVlanEncap(PathIntent intent) {
        Map<LinkKey, VlanId> vlanIds = assignVlanId(intent);

        Iterator<Link> links = intent.path().links().iterator();
        Link srcLink = links.next();

        Link link = links.next();
        // List of flow rules to be installed
        List<FlowRule> rules = new LinkedList<>();

        // Ingress traffic
        VlanId vlanId = vlanIds.get(linkKey(link));
        if (vlanId == null) {
            throw new IntentCompilationException("No available VLAN ID for " + link);
        }
        VlanId prevVlanId = vlanId;

        //Tag the traffic with the new VLAN
        TrafficTreatment treat = DefaultTrafficTreatment.builder()
                .setVlanId(vlanId)
                .build();

        rules.add(createFlowRule(intent.selector(), treat, srcLink.dst(), link.src(), intent.priority(), true));

        ConnectPoint prev = link.dst();

        while (links.hasNext()) {

            link = links.next();

            if (links.hasNext()) {
                // Transit traffic
                VlanId egressVlanId = vlanIds.get(linkKey(link));
                if (egressVlanId == null) {
                    throw new IntentCompilationException("No available VLAN ID for " + link);
                }
                prevVlanId = egressVlanId;

                TrafficSelector transitSelector = DefaultTrafficSelector.builder()
                        .matchInPort(prev.port())
                        .matchVlanId(prevVlanId).build();

                TrafficTreatment.Builder transitTreat = DefaultTrafficTreatment.builder();

                // Set the new vlanId only if the previous one is different
                if (!prevVlanId.equals(egressVlanId)) {
                    transitTreat.setVlanId(egressVlanId);
                }
                rules.add(createFlowRule(transitSelector,
                                         transitTreat.build(), prev, link.src(), intent.priority(), true));
                prev = link.dst();
            } else {
                // Egress traffic
                TrafficSelector egressSelector = DefaultTrafficSelector.builder()
                        .matchInPort(prev.port())
                        .matchVlanId(prevVlanId).build();

                //TODO: think to other cases for egress packet restoration
                Optional<VlanIdCriterion> vlanCriteria = intent.selector().criteria()
                        .stream().filter(criteria -> criteria.type() == Criterion.Type.VLAN_VID)
                        .map(criteria -> (VlanIdCriterion) criteria)
                        .findAny();
                TrafficTreatment.Builder egressTreat = DefaultTrafficTreatment.builder(intent.treatment());
                if (vlanCriteria.isPresent()) {
                    egressTreat.setVlanId(vlanCriteria.get().vlanId());
                } else {
                    egressTreat.popVlan();
                }

                rules.add(createFlowRule(egressSelector,
                                         egressTreat.build(), prev, link.src(), intent.priority(), true));
            }

        }
        return rules;

    }

    private Map<LinkKey, VlanId> assignVlanId(PathIntent intent) {
        Set<LinkKey> linkRequest = Sets.newHashSetWithExpectedSize(intent.path()
                                                                           .links().size() - 2);
        for (int i = 1; i <= intent.path().links().size() - 2; i++) {
            LinkKey link = linkKey(intent.path().links().get(i));
            linkRequest.add(link);
            // add the inverse link. I want that the VLANID is reserved both for
            // the direct and inverse link
            linkRequest.add(linkKey(link.dst(), link.src()));
        }

        Map<LinkKey, VlanId> vlanIds = findVlanIds(linkRequest);
        if (vlanIds.isEmpty()) {
            log.warn("No VLAN IDs available");
            return Collections.emptyMap();
        }

        //same VLANID is used for both directions
        Set<Resource> resources = vlanIds.entrySet().stream()
                .flatMap(x -> Stream.of(
                        Resources.discrete(x.getKey().src().deviceId(), x.getKey().src().port(), x.getValue())
                                .resource(),
                        Resources.discrete(x.getKey().dst().deviceId(), x.getKey().dst().port(), x.getValue())
                                .resource()
                ))
                .collect(Collectors.toSet());
        List<org.onosproject.net.newresource.ResourceAllocation> allocations =
                resourceService.allocate(intent.id(), ImmutableList.copyOf(resources));
        if (allocations.isEmpty()) {
            Collections.emptyMap();
        }

        return vlanIds;
    }

    private Map<LinkKey, VlanId> findVlanIds(Set<LinkKey> links) {
        Map<LinkKey, VlanId> vlanIds = new HashMap<>();
        for (LinkKey link : links) {
            Set<VlanId> forward = findVlanId(link.src());
            Set<VlanId> backward = findVlanId(link.dst());
            Set<VlanId> common = Sets.intersection(forward, backward);
            if (common.isEmpty()) {
                continue;
            }
            vlanIds.put(link, common.iterator().next());
        }
        return vlanIds;
    }

    private Set<VlanId> findVlanId(ConnectPoint cp) {
        return resourceService.getAvailableResources(Resources.discrete(cp.deviceId(), cp.port()).resource()).stream()
                .filter(x -> x.last() instanceof VlanId)
                .map(x -> (VlanId) x.last())
                .collect(Collectors.toSet());
    }

    private boolean isLast(List<Link> links, int i) {
        return i == links.size() - 2;
    }
}
