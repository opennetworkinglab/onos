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

import com.google.common.collect.Sets;
import org.onlab.packet.EthType;
import org.onlab.packet.Ethernet;
import org.onlab.packet.MplsLabel;
import org.onlab.packet.VlanId;
import org.onlab.util.Identifier;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.EncapsulationType;
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
import org.onosproject.net.resource.ResourceService;
import org.onosproject.net.resource.impl.LabelAllocator;
import org.slf4j.Logger;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.onosproject.net.LinkKey.linkKey;

/**
 * Shared APIs and implementations for path compilers.
 */

public class PathCompiler<T> {

    private static final String ERROR_VLAN = "No VLAN Ids available for ";
    private static final String ERROR_MPLS = "No available MPLS labels for ";

    static LabelAllocator labelAllocator;

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

    /**
     * Returns the ethertype match needed. If the selector provides
     * an ethertype, it will be used. IPv4 will be used otherwise.
     *
     * @param selector the traffic selector.
     * @return the ethertype we should match against
     */
    private EthType getEthType(TrafficSelector selector) {
        Criterion c = selector.getCriterion(Criterion.Type.ETH_TYPE);
        if (c != null && c instanceof EthTypeCriterion) {
            EthTypeCriterion ethertype = (EthTypeCriterion) c;
            return ethertype.ethType();
        } else {
            return EthType.EtherType.IPV4.ethType();
        }
    }

    /**
     * Creates the flow rules for the path intent using VLAN
     * encapsulation.
     *
     * @param creator the flowrules creator
     * @param flows the list of flows to fill
     * @param devices the devices on the path
     * @param intent the PathIntent to compile
     */
    private void manageVlanEncap(PathCompilerCreateFlow<T> creator, List<T> flows,
                                 List<DeviceId> devices,
                                 PathIntent intent) {

        Set<Link> linksSet = Sets.newConcurrentHashSet();
        for (int i = 1; i <= intent.path().links().size() - 2; i++) {
            linksSet.add(intent.path().links().get(i));
        }

        Map<LinkKey, Identifier<?>> vlanIds = labelAllocator.assignLabelToLinks(
                linksSet,
                intent.key(),
                EncapsulationType.VLAN
        );

        Iterator<Link> links = intent.path().links().iterator();
        Link srcLink = links.next();

        Link link = links.next();

        // Ingress traffic
        VlanId vlanId = (VlanId) vlanIds.get(linkKey(link));
        if (vlanId == null) {
            throw new IntentCompilationException(ERROR_VLAN + link);
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
                VlanId egressVlanId = (VlanId) vlanIds.get(linkKey(link));
                if (egressVlanId == null) {
                    throw new IntentCompilationException(ERROR_VLAN + link);
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

    /**
     * Creates the flow rules for the path intent using MPLS
     * encapsulation.
     *
     * @param creator the flowrules creator
     * @param flows the list of flows to fill
     * @param devices the devices on the path
     * @param intent the PathIntent to compile
     */
    private void manageMplsEncap(PathCompilerCreateFlow<T> creator, List<T> flows,
                                           List<DeviceId> devices,
                                           PathIntent intent) {

        Set<Link> linksSet = Sets.newConcurrentHashSet();
        for (int i = 1; i <= intent.path().links().size() - 2; i++) {
            linksSet.add(intent.path().links().get(i));
        }

        Map<LinkKey, Identifier<?>> mplsLabels = labelAllocator.assignLabelToLinks(
                linksSet,
                intent.key(),
                EncapsulationType.MPLS
        );
        Iterator<Link> links = intent.path().links().iterator();
        Link srcLink = links.next();

        Link link = links.next();
        // List of flow rules to be installed

        // Ingress traffic
        MplsLabel mplsLabel = (MplsLabel) mplsLabels.get(linkKey(link));
        if (mplsLabel == null) {
            throw new IntentCompilationException(ERROR_MPLS + link);
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
                MplsLabel transitMplsLabel = (MplsLabel) mplsLabels.get(linkKey(link));
                if (transitMplsLabel == null) {
                    throw new IntentCompilationException(ERROR_MPLS + link);
                }
                TrafficSelector transitSelector = DefaultTrafficSelector.builder()
                        .matchInPort(prev.port())
                        .matchEthType(Ethernet.MPLS_UNICAST)
                        .matchMplsLabel(prevMplsLabel).build();

                TrafficTreatment.Builder transitTreat = DefaultTrafficTreatment.builder();

                // Set the new MPLS label only if the previous one is different
                if (!prevMplsLabel.equals(transitMplsLabel)) {
                    transitTreat.setMpls(transitMplsLabel);
                }
                creator.createFlow(transitSelector,
                        transitTreat.build(), prev, link.src(), intent.priority(), true, flows, devices);
                prevMplsLabel = transitMplsLabel;
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
                    egressTreat.popMpls(getEthType(intent.selector()));
                }

                creator.createFlow(egressSelector.build(),
                        egressTreat.build(), prev, link.src(), intent.priority(), true, flows, devices);
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
