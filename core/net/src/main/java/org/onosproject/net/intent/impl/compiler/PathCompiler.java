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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import org.apache.commons.lang.math.RandomUtils;
import org.onlab.packet.EthType;
import org.onlab.packet.Ethernet;
import org.onlab.packet.MplsLabel;
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
import org.onosproject.net.flow.criteria.EthTypeCriterion;
import org.onosproject.net.flow.criteria.MplsCriterion;
import org.onosproject.net.flow.criteria.VlanIdCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction;
import org.onosproject.net.intent.IntentCompilationException;
import org.onosproject.net.intent.PathIntent;
import org.onosproject.net.intent.constraint.EncapsulationConstraint;
import org.onosproject.net.resource.Resource;
import org.onosproject.net.resource.ResourceAllocation;
import org.onosproject.net.resource.ResourceService;
import org.onosproject.net.resource.Resources;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.onosproject.net.LinkKey.linkKey;

/**
 * Shared APIs and implementations for path compilers.
 */

public class PathCompiler<T> {

    public static final boolean RANDOM_SELECTION = true;

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
        List<ResourceAllocation> allocations =
                creator.resourceService().allocate(intent.id(), ImmutableList.copyOf(resources));
        if (allocations.isEmpty()) {
            return Collections.emptyMap();
        }

        return vlanIds;
    }

    /**
     * Implements the first fit selection behavior.
     *
     * @param available the set of available VLAN ids.
     * @return the chosen VLAN id.
     */
    private VlanId firsFitSelection(Set<VlanId> available) {
        if (!available.isEmpty()) {
            return available.iterator().next();
        }
        return VlanId.vlanId(VlanId.NO_VID);
    }

    /**
     * Implements the random selection behavior.
     *
     * @param available the set of available VLAN ids.
     * @return the chosen VLAN id.
     */
    private VlanId randomSelection(Set<VlanId> available) {
        if (!available.isEmpty()) {
            int size = available.size();
            int index = RandomUtils.nextInt(size);
            return Iterables.get(available, index);
        }
        return VlanId.vlanId(VlanId.NO_VID);
    }

    /**
    * Select a VLAN id from the set of available VLAN ids.
    *
    * @param available the set of available VLAN ids.
    * @return the chosen VLAN id.
    */
    private VlanId selectVlanId(Set<VlanId> available) {
        return RANDOM_SELECTION ? randomSelection(available) : firsFitSelection(available);
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
            VlanId selected = selectVlanId(common);
            if (selected.toShort() == VlanId.NO_VID) {
                continue;
            }
            vlanIds.put(link, selected);
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
                /* For the next hop we have to remember
                 * the previous egress VLAN id and the egress
                 * node
                 */
                prevVlanId = egressVlanId;
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

                Optional<L2ModificationInstruction.ModVlanHeaderInstruction> popVlanInstruction = intent.treatment()
                        .allInstructions().stream().filter(
                                instruction -> instruction instanceof
                                        L2ModificationInstruction.ModVlanHeaderInstruction)
                        .map(x -> (L2ModificationInstruction.ModVlanHeaderInstruction) x).findAny();

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

    private Map<LinkKey, MplsLabel> assignMplsLabel(PathCompilerCreateFlow creator, PathIntent intent) {
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

        Map<LinkKey, MplsLabel> labels = findMplsLabels(creator, linkRequest);
        if (labels.isEmpty()) {
            throw new IntentCompilationException("No available MPLS Label");
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
                creator.resourceService().allocate(intent.id(), ImmutableList.copyOf(resources));
        if (allocations.isEmpty()) {
            return Collections.emptyMap();
        }

        return labels;
    }

    private Map<LinkKey, MplsLabel> findMplsLabels(PathCompilerCreateFlow creator, Set<LinkKey> links) {
        Map<LinkKey, MplsLabel> labels = new HashMap<>();
        for (LinkKey link : links) {
            Set<MplsLabel> forward = findMplsLabel(creator, link.src());
            Set<MplsLabel> backward = findMplsLabel(creator, link.dst());
            Set<MplsLabel> common = Sets.intersection(forward, backward);
            if (common.isEmpty()) {
                continue;
            }
            labels.put(link, common.iterator().next());
        }

        return labels;
    }

    private Set<MplsLabel> findMplsLabel(PathCompilerCreateFlow creator, ConnectPoint cp) {
        return creator.resourceService().getAvailableResourceValues(
                Resources.discrete(cp.deviceId(), cp.port()).id(),
                MplsLabel.class);
    }

    private void manageMplsEncap(PathCompilerCreateFlow<T> creator, List<T> flows,
                                           List<DeviceId> devices,
                                           PathIntent intent) {
        Map<LinkKey, MplsLabel> mplsLabels = assignMplsLabel(creator, intent);

        Iterator<Link> links = intent.path().links().iterator();
        Link srcLink = links.next();

        Link link = links.next();
        // List of flow rules to be installed

        // Ingress traffic
        MplsLabel mplsLabel = mplsLabels.get(linkKey(link));
        if (mplsLabel == null) {
            throw new IntentCompilationException("No available MPLS Label for " + link);
        }
        MplsLabel prevMplsLabel = mplsLabel;

        Optional<MplsCriterion> mplsCriterion = intent.selector().criteria()
                .stream().filter(criterion -> criterion.type() == Criterion.Type.MPLS_LABEL)
                .map(criterion -> (MplsCriterion) criterion)
                .findAny();

        //Push MPLS if selector does not include MPLS
        TrafficTreatment.Builder treatBuilder = DefaultTrafficTreatment.builder();
        if (!mplsCriterion.isPresent()) {
            treatBuilder.pushMpls();
        }
        //Tag the traffic with the new encapsulation MPLS label
        treatBuilder.setMpls(mplsLabel);
        creator.createFlow(intent.selector(), treatBuilder.build(),
                srcLink.dst(), link.src(), intent.priority(), true, flows, devices);

        ConnectPoint prev = link.dst();

        while (links.hasNext()) {

            link = links.next();

            if (links.hasNext()) {
                // Transit traffic
                MplsLabel transitMplsLabel = mplsLabels.get(linkKey(link));
                if (transitMplsLabel == null) {
                    throw new IntentCompilationException("No available MPLS label for " + link);
                }
                prevMplsLabel = transitMplsLabel;

                TrafficSelector transitSelector = DefaultTrafficSelector.builder()
                        .matchInPort(prev.port())
                        .matchEthType(Ethernet.MPLS_UNICAST)
                        .matchMplsLabel(prevMplsLabel).build();

                TrafficTreatment.Builder transitTreat = DefaultTrafficTreatment.builder();

                // Set the new MPLS Label only if the previous one is different
                if (!prevMplsLabel.equals(transitMplsLabel)) {
                    transitTreat.setMpls(transitMplsLabel);
                }
                creator.createFlow(transitSelector,
                        transitTreat.build(), prev, link.src(), intent.priority(), true, flows, devices);
                prev = link.dst();
            } else {
                TrafficSelector.Builder egressSelector = DefaultTrafficSelector.builder()
                        .matchInPort(prev.port())
                        .matchEthType(Ethernet.MPLS_UNICAST)
                        .matchMplsLabel(prevMplsLabel);
                TrafficTreatment.Builder egressTreat = DefaultTrafficTreatment.builder(intent.treatment());

                // Egress traffic
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
                            egressSelector.matchVlanId(VlanId.ANY);
                        }
                    }
                }

                if (mplsCriterion.isPresent()) {
                    egressTreat.setMpls(mplsCriterion.get().label());
                } else {
                    egressTreat.popMpls(outputEthType(intent.selector()));
                }


                if (mplsCriterion.isPresent()) {
                    egressTreat.setMpls(mplsCriterion.get().label());
                } else {
                    egressTreat.popVlan();
                }

                creator.createFlow(egressSelector.build(),
                        egressTreat.build(), prev, link.src(), intent.priority(), true, flows, devices);
            }

        }

    }

    private MplsLabel getMplsLabel(Map<LinkKey, MplsLabel> labels, LinkKey link) {
        return labels.get(link);
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
        if (!encapConstraint.isPresent() || links.size() == 2) {
            for (int i = 0; i < links.size() - 1; i++) {
                ConnectPoint ingress = links.get(i).dst();
                ConnectPoint egress = links.get(i + 1).src();
                creator.createFlow(intent.selector(), intent.treatment(),
                                   ingress, egress, intent.priority(),
                                   isLast(links, i), flows, devices);
            }
            return;
        }

        encapConstraint.map(EncapsulationConstraint::encapType)
                .map(type -> {
                    switch (type) {
                        case VLAN:
                            manageVlanEncap(creator, flows, devices, intent);
                            break;
                        case MPLS:
                             manageMplsEncap(creator, flows, devices, intent);
                            break;
                        default:
                            // Nothing to do
                    }
                    return 0;
                });
    }

}
