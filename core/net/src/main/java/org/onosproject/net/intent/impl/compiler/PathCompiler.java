/*
 * Copyright 2016 Open Networking Laboratory
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

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.onlab.packet.VlanId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.LinkKey;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.VlanIdCriterion;
import org.onosproject.net.flow.instructions.L2ModificationInstruction;
import org.onosproject.net.intent.PathIntent;
import org.onosproject.net.intent.constraint.EncapsulationConstraint;
import org.onosproject.net.intent.impl.IntentCompilationException;
import org.onosproject.net.newresource.Resource;
import org.onosproject.net.newresource.ResourceService;
import org.onosproject.net.newresource.Resources;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;

import static org.onosproject.net.LinkKey.linkKey;

/**
 * Shared APIs and implementations for path compilers.
 */

public class PathCompiler<T> {

    /**
     * Defines methods used to create objects representing flows.
     */
    public interface PathCompilerCreateFlow<T> {

        void createFlow(TrafficSelector originalSelector,
                        TrafficTreatment originalTreatment,
                        ConnectPoint ingress, ConnectPoint egress,
                        int priority,
                        boolean applyTreatment,
                        List<T> flows,
                        List<DeviceId> devices);

        Logger log();

        ResourceService resourceService();
    }

    private boolean isLast(List<Link> links, int i) {
        return i == links.size() - 2;
    }

    private Map<LinkKey, VlanId> assignVlanId(PathCompilerCreateFlow creator, PathIntent intent) {
        Set<LinkKey> linkRequest =
                Sets.newHashSetWithExpectedSize(intent.path()
                        .links().size() - 2);
        for (int i = 1; i <= intent.path().links().size() - 2; i++) {
            LinkKey link = linkKey(intent.path().links().get(i));
            linkRequest.add(link);
            // add the inverse link. I want that the VLANID is reserved both for
            // the direct and inverse link
            linkRequest.add(linkKey(link.dst(), link.src()));
        }

        Map<LinkKey, VlanId> vlanIds = findVlanIds(creator, linkRequest);
        if (vlanIds.isEmpty()) {
            creator.log().warn("No VLAN IDs available");
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
                creator.resourceService().allocate(intent.id(), ImmutableList.copyOf(resources));
        if (allocations.isEmpty()) {
            Collections.emptyMap();
        }

        return vlanIds;
    }

    private Map<LinkKey, VlanId> findVlanIds(PathCompilerCreateFlow creator, Set<LinkKey> links) {
        Map<LinkKey, VlanId> vlanIds = new HashMap<>();
        for (LinkKey link : links) {
            Set<VlanId> forward = findVlanId(creator, link.src());
            Set<VlanId> backward = findVlanId(creator, link.dst());
            Set<VlanId> common = Sets.intersection(forward, backward);
            if (common.isEmpty()) {
                continue;
            }
            vlanIds.put(link, common.iterator().next());
        }
        return vlanIds;
    }

    private Set<VlanId> findVlanId(PathCompilerCreateFlow creator, ConnectPoint cp) {
        return creator.resourceService().getAvailableResourceValues(
                Resources.discrete(cp.deviceId(), cp.port()).id(),
                VlanId.class);
    }

    private void manageVlanEncap(PathCompilerCreateFlow<T> creator, List<T> flows,
                                 List<DeviceId> devices,
                                 PathIntent intent) {
        Map<LinkKey, VlanId> vlanIds = assignVlanId(creator, intent);

        Iterator<Link> links = intent.path().links().iterator();
        Link srcLink = links.next();

        Link link = links.next();

        // Ingress traffic
        VlanId vlanId = vlanIds.get(linkKey(link));
        if (vlanId == null) {
            throw new IntentCompilationException("No available VLAN ID for " + link);
        }
        VlanId prevVlanId = vlanId;

        Optional<VlanIdCriterion> vlanCriterion = intent.selector().criteria()
                .stream().filter(criterion -> criterion.type() == Criterion.Type.VLAN_VID)
                .map(criterion -> (VlanIdCriterion) criterion)
                .findAny();

        //Push VLAN if selector does not include VLAN
        TrafficTreatment.Builder treatBuilder = DefaultTrafficTreatment.builder();
        if (!vlanCriterion.isPresent()) {
            treatBuilder.pushVlan();
        }
        //Tag the traffic with the new encapsulation VLAN
        treatBuilder.setVlanId(vlanId);
        creator.createFlow(intent.selector(), treatBuilder.build(),
                           srcLink.dst(), link.src(), intent.priority(), true,
                           flows, devices);

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
                creator.createFlow(transitSelector,
                                   transitTreat.build(), prev, link.src(),
                                   intent.priority(), true, flows, devices);
                prev = link.dst();
            } else {
                // Egress traffic
                TrafficSelector egressSelector = DefaultTrafficSelector.builder()
                        .matchInPort(prev.port())
                        .matchVlanId(prevVlanId).build();
                TrafficTreatment.Builder egressTreat = DefaultTrafficTreatment.builder(intent.treatment());

                Optional<L2ModificationInstruction.ModVlanIdInstruction> modVlanIdInstruction = intent.treatment()
                        .allInstructions().stream().filter(
                                instruction -> instruction instanceof L2ModificationInstruction.ModVlanIdInstruction)
                        .map(x -> (L2ModificationInstruction.ModVlanIdInstruction) x).findAny();

                Optional<L2ModificationInstruction.PopVlanInstruction> popVlanInstruction = intent.treatment()
                        .allInstructions().stream().filter(
                                instruction -> instruction instanceof L2ModificationInstruction.PopVlanInstruction)
                        .map(x -> (L2ModificationInstruction.PopVlanInstruction) x).findAny();

                if (!modVlanIdInstruction.isPresent() && !popVlanInstruction.isPresent()) {
                    if (vlanCriterion.isPresent()) {
                        egressTreat.setVlanId(vlanCriterion.get().vlanId());
                    } else {
                        egressTreat.popVlan();
                    }
                }

                creator.createFlow(egressSelector,
                                   egressTreat.build(), prev, link.src(),
                                   intent.priority(), true, flows, devices);
            }
        }
    }

    /**
     * Compiles an intent down to flows.
     *
     * @param creator how to create the flows
     * @param intent intent to process
     * @param flows list of generated flows
     * @param devices list of devices that correspond to the flows
     */
    public void compile(PathCompilerCreateFlow<T> creator,
                        PathIntent intent,
                        List<T> flows,
                        List<DeviceId> devices) {
        // Note: right now recompile is not considered
        // TODO: implement recompile behavior

        List<Link> links = intent.path().links();

        Optional<EncapsulationConstraint> encapConstraint = intent.constraints().stream()
                .filter(constraint -> constraint instanceof EncapsulationConstraint)
                .map(x -> (EncapsulationConstraint) x).findAny();
        //if no encapsulation or is involved only a single switch use the default behaviour
        if (!encapConstraint.isPresent() || links.size() == 1) {
            for (int i = 0; i < links.size() - 1; i++) {
                ConnectPoint ingress = links.get(i).dst();
                ConnectPoint egress = links.get(i + 1).src();
                creator.createFlow(intent.selector(), intent.treatment(),
                                   ingress, egress, intent.priority(),
                                   isLast(links, i), flows, devices);
            }
        }

        encapConstraint.map(EncapsulationConstraint::encapType)
                .map(type -> {
                    switch (type) {
                        case VLAN:
                            manageVlanEncap(creator, flows, devices, intent);
                            // TODO: implement MPLS case here
                        default:
                            // Nothing to do
                    }
                    return 0;
                });
    }

}
