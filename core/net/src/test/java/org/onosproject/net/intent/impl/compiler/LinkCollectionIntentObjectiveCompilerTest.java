/*
 * Copyright 2017-present Open Networking Laboratory
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
import com.google.common.collect.ImmutableSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.cfg.ComponentConfigAdapter;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.DeviceId;
import org.onosproject.net.EncapsulationType;
import org.onosproject.net.FilteredConnectPoint;
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;
import org.onosproject.net.domain.DomainService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.net.flowobjective.FlowObjectiveServiceAdapter;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.NextObjective;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.intent.FlowObjectiveIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentExtensionService;
import org.onosproject.net.intent.LinkCollectionIntent;
import org.onosproject.net.intent.constraint.EncapsulationConstraint;
import org.onosproject.net.resource.MockResourceService;
import org.onosproject.net.resource.ResourceService;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.easymock.EasyMock.*;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.onosproject.net.Link.Type.DIRECT;
import static org.onosproject.net.NetTestTools.PID;
import static org.onosproject.net.domain.DomainId.LOCAL;
import static org.onosproject.net.flowobjective.ForwardingObjective.Flag.SPECIFIC;
import static org.onosproject.net.flowobjective.NextObjective.Type.BROADCAST;
import static org.onosproject.net.flowobjective.NextObjective.Type.SIMPLE;
import static org.onosproject.net.flowobjective.Objective.Operation.ADD;

public class LinkCollectionIntentObjectiveCompilerTest extends AbstractLinkCollectionTest {
    private static final VlanId VLAN_1 = VlanId.vlanId("1");
    private static final VlanId VLAN_100 = VlanId.vlanId("100");

    private LinkCollectionIntentObjectiveCompiler compiler;
    private FlowObjectiveServiceAdapter flowObjectiveService;

    private NextObjective nextObjective;
    private ForwardingObjective forwardingObjective;
    private FilteringObjective filteringObjective;

    private ResourceService resourceService;

    @Before
    public void setUp() {
        compiler = new LinkCollectionIntentObjectiveCompiler();
        coreService = createMock(CoreService.class);
        expect(coreService.registerApplication("org.onosproject.net.intent"))
                .andReturn(appId);
        flowObjectiveService = new FlowObjectiveServiceAdapter();
        resourceService = new MockResourceService();
        compiler.coreService = coreService;
        compiler.flowObjectiveService = flowObjectiveService;


        domainService = createMock(DomainService.class);
        expect(domainService.getDomain(anyObject(DeviceId.class))).andReturn(LOCAL).anyTimes();
        compiler.domainService = domainService;

        super.setUp();

        intentExtensionService = createMock(IntentExtensionService.class);
        intentExtensionService.registerCompiler(LinkCollectionIntent.class, compiler);
        intentExtensionService.unregisterCompiler(LinkCollectionIntent.class);

        registrator = new IntentConfigurableRegistrator();
        registrator.extensionService = intentExtensionService;
        registrator.cfgService = new ComponentConfigAdapter();
        registrator.activate();

        compiler.registrator = registrator;
        compiler.resourceService = resourceService;

        LinkCollectionCompiler.optimizeInstructions = false;
        LinkCollectionCompiler.copyTtl = false;

        replay(coreService, domainService, intentExtensionService);
        compiler.activate();
    }

    @After
    public void tearDown() {
        super.tearDown();
        compiler.deactivate();
    }

    /**
     * We test the proper compilation of a simple link collection intent
     * with connect points, empty trivial treatment and empty trivial selector.
     */
    @Test
    public void testCompile() {
        LinkCollectionIntent intent = LinkCollectionIntent.builder()
                .appId(appId)
                .selector(selector)
                .treatment(treatment)
                .links(links)
                .filteredIngressPoints(ImmutableSet.of(new FilteredConnectPoint(d1p1)))
                .filteredEgressPoints(ImmutableSet.of(new FilteredConnectPoint(d3p1)))
                .build();

        List<Intent> result = compiler.compile(intent, Collections.emptyList());
        assertThat(result, hasSize(1));
        assertThat(result.get(0), instanceOf(FlowObjectiveIntent.class));

        FlowObjectiveIntent foIntent = (FlowObjectiveIntent) result.get(0);
        List<Objective> objectives = foIntent.objectives();
        assertThat(objectives, hasSize(6));

        /*
         * First set of objective
         */
        forwardingObjective = (ForwardingObjective) objectives.get(0);
        nextObjective = (NextObjective) objectives.get(1);

        // expect selector and treatment
        TrafficSelector expectSelector = DefaultTrafficSelector.builder()
                .matchInPort(PortNumber.portNumber(1))
                .build();
        TrafficTreatment expectTreatment = DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.portNumber(1))
                .build();

        // test case for first next objective
        checkNext(nextObjective, SIMPLE, expectTreatment, expectSelector, ADD);

        // test case for first forwarding objective
        checkForward(forwardingObjective, ADD, expectSelector, nextObjective.id(), SPECIFIC);

        /*
         * Second set of objective
         */
        forwardingObjective = (ForwardingObjective) objectives.get(2);
        nextObjective = (NextObjective) objectives.get(3);


        expectSelector = DefaultTrafficSelector.builder()
                .matchInPort(PortNumber.portNumber(0))
                .build();

        // test case for second next objective
        checkNext(nextObjective, SIMPLE, expectTreatment, expectSelector, ADD);

        // test case for second forwarding objective
        checkForward(forwardingObjective, ADD, expectSelector, nextObjective.id(), SPECIFIC);

        /*
         * 3rd set of objective
         */
        forwardingObjective = (ForwardingObjective) objectives.get(4);
        nextObjective = (NextObjective) objectives.get(5);
        expectSelector = DefaultTrafficSelector.builder()
                .matchInPort(PortNumber.portNumber(1))
                .build();

        // test case for 3rd next objective
        checkNext(nextObjective, SIMPLE, expectTreatment, expectSelector, ADD);

        // test case for 3rd forwarding objective
        checkForward(forwardingObjective, ADD, expectSelector, nextObjective.id(), SPECIFIC);
    }

    /**
     * Single point to multi point case. Scenario is the follow:
     *
     * -1 of1 2-1 of2 2--1 of3 2-
     *             3
     *             `-1 of4 2-
     *
     * We test the proper compilation of sp2mp with trivial selector,
     * empty treatment and points.
     *
     */
    @Test
    public void testFilteredConnectPointForSp() {
        Set<Link> testLinks = ImmutableSet.of(
                DefaultLink.builder().providerId(PID).src(of1p2).dst(of2p1).type(DIRECT).build(),
                DefaultLink.builder().providerId(PID).src(of2p2).dst(of3p1).type(DIRECT).build(),
                DefaultLink.builder().providerId(PID).src(of2p3).dst(of4p1).type(DIRECT).build()
        );

        Set<FilteredConnectPoint> ingress = ImmutableSet.of(
                new FilteredConnectPoint(of1p1, vlan100Selector)
        );

        Set<FilteredConnectPoint> egress = ImmutableSet.of(
                new FilteredConnectPoint(of3p2, vlan100Selector),
                new FilteredConnectPoint(of4p2, vlan100Selector)
        );

        TrafficSelector broadcastSelector = DefaultTrafficSelector.builder()
                .matchEthDst(MacAddress.BROADCAST)
                .build();

        LinkCollectionIntent intent = LinkCollectionIntent.builder()
                .appId(appId)
                .selector(broadcastSelector)
                .treatment(treatment)
                .links(testLinks)
                .filteredIngressPoints(ingress)
                .filteredEgressPoints(egress)
                .applyTreatmentOnEgress(true)
                .resourceGroup(resourceGroup1)
                .build();

        List<Intent> result = compiler.compile(intent, Collections.emptyList());
        assertThat(result, hasSize(1));
        assertThat(result.get(0), instanceOf(FlowObjectiveIntent.class));

        FlowObjectiveIntent foIntent = (FlowObjectiveIntent) result.get(0);
        List<Objective> objectives = foIntent.objectives();
        assertThat(objectives, hasSize(12));

        /*
         * First set of objective
         */
        filteringObjective = (FilteringObjective) objectives.get(0);
        forwardingObjective = (ForwardingObjective) objectives.get(1);
        nextObjective = (NextObjective) objectives.get(2);

        // expect selector and treatment
        TrafficSelector expectSelector = DefaultTrafficSelector.builder()
                .matchInPort(PortNumber.portNumber(1))
                .matchVlanId(VLAN_100)
                .matchEthDst(MacAddress.BROADCAST)
                .build();
        List<TrafficTreatment> expectTreatments = ImmutableList.of(
                DefaultTrafficTreatment.builder()
                        .setOutput(PortNumber.portNumber(2))
                        .build()
        );

        PortCriterion inPortCriterion =
                (PortCriterion) expectSelector.getCriterion(Criterion.Type.IN_PORT);

        // test case for first filtering objective
        checkFiltering(filteringObjective, inPortCriterion, intent.priority(),
                       null, appId, true, vlan100Selector.criteria());

        // test case for first next objective
        checkNext(nextObjective, BROADCAST, expectTreatments, expectSelector, ADD);

        // test case for first forwarding objective
        checkForward(forwardingObjective, ADD, expectSelector, nextObjective.id(), SPECIFIC);

        /*
         * Second set of objective
         */
        filteringObjective = (FilteringObjective) objectives.get(3);
        forwardingObjective = (ForwardingObjective) objectives.get(4);
        nextObjective = (NextObjective) objectives.get(5);

        inPortCriterion =
                (PortCriterion) expectSelector.getCriterion(Criterion.Type.IN_PORT);

        // test case for first filtering objective
        checkFiltering(filteringObjective, inPortCriterion, intent.priority(),
                       null, appId, true, vlan100Selector.criteria());

        // test case for second next objective
        checkNext(nextObjective, BROADCAST, expectTreatments, expectSelector, ADD);

        // test case for second forwarding objective
        checkForward(forwardingObjective, ADD, expectSelector, nextObjective.id(), SPECIFIC);

        /*
         * 3rd set of objective
         */
        filteringObjective = (FilteringObjective) objectives.get(6);
        forwardingObjective = (ForwardingObjective) objectives.get(7);
        nextObjective = (NextObjective) objectives.get(8);

        inPortCriterion =
                (PortCriterion) expectSelector.getCriterion(Criterion.Type.IN_PORT);

        // test case for first filtering objective
        checkFiltering(filteringObjective, inPortCriterion, intent.priority(),
                       null, appId, true, vlan100Selector.criteria());

        // test case for 3rd next objective
        checkNext(nextObjective, BROADCAST, expectTreatments, expectSelector, ADD);

        // test case for 3rd forwarding objective
        checkForward(forwardingObjective, ADD, expectSelector, nextObjective.id(), SPECIFIC);

        /*
         * 4th set of objective
         */
        filteringObjective = (FilteringObjective) objectives.get(9);
        forwardingObjective = (ForwardingObjective) objectives.get(10);
        nextObjective = (NextObjective) objectives.get(11);

        inPortCriterion =
                (PortCriterion) expectSelector.getCriterion(Criterion.Type.IN_PORT);

        // test case for first filtering objective
        checkFiltering(filteringObjective, inPortCriterion, intent.priority(),
                       null, appId, true, vlan100Selector.criteria());

        // test case for 3rd next objective
        expectTreatments = ImmutableList.of(
                DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.portNumber(2))
                .build(),
        DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.portNumber(3))
                .build()
        );
        checkNext(nextObjective, BROADCAST, expectTreatments, expectSelector, ADD);

        // test case for 3rd forwarding objective
        checkForward(forwardingObjective, ADD, expectSelector, nextObjective.id(), SPECIFIC);
    }

    /**
     * Multi point to single point intent with filtered connect point.
     * Scenario is the follow:
     *
     * -1 of1 2-1 of2 2-1 of4 2-
     *             3
     * -1 of3 2---/
     *
     */
    @Test
    public void testFilteredConnectPointForMp() {
        Set<Link> testLinks = ImmutableSet.of(
                DefaultLink.builder().providerId(PID).src(of1p2).dst(of2p1).type(DIRECT).build(),
                DefaultLink.builder().providerId(PID).src(of3p2).dst(of2p3).type(DIRECT).build(),
                DefaultLink.builder().providerId(PID).src(of2p2).dst(of4p1).type(DIRECT).build()
        );

        Set<FilteredConnectPoint> ingress = ImmutableSet.of(
                new FilteredConnectPoint(of3p1, vlan100Selector),
                new FilteredConnectPoint(of1p1, vlan100Selector)
        );

        Set<FilteredConnectPoint> egress = ImmutableSet.of(
                new FilteredConnectPoint(of4p2, vlan100Selector)
        );

        LinkCollectionIntent intent = LinkCollectionIntent.builder()
                .appId(appId)
                .selector(ethDstSelector)
                .treatment(treatment)
                .links(testLinks)
                .filteredIngressPoints(ingress)
                .filteredEgressPoints(egress)
                .build();

        List<Intent> result = compiler.compile(intent, Collections.emptyList());
        assertThat(result, hasSize(1));
        assertThat(result.get(0), instanceOf(FlowObjectiveIntent.class));

        FlowObjectiveIntent foIntent = (FlowObjectiveIntent) result.get(0);
        List<Objective> objectives = foIntent.objectives();
        assertThat(objectives, hasSize(15));

        TrafficSelector expectSelector = DefaultTrafficSelector
                .builder(ethDstSelector)
                .matchInPort(PortNumber.portNumber(1))
                .matchVlanId(VLAN_100)
                .build();

        TrafficTreatment expectTreatment = DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.portNumber(2))
                .build();

        /*
         * First set of objective
         */
        filteringObjective = (FilteringObjective) objectives.get(0);
        forwardingObjective = (ForwardingObjective) objectives.get(1);
        nextObjective = (NextObjective) objectives.get(2);

        PortCriterion inPortCriterion =
                (PortCriterion) expectSelector.getCriterion(Criterion.Type.IN_PORT);

        // test case for first filtering objective
        checkFiltering(filteringObjective, inPortCriterion, intent.priority(),
                       null, appId, true, vlan100Selector.criteria());

        // test case for first next objective
        checkNext(nextObjective, SIMPLE, expectTreatment, expectSelector, ADD);

        // test case for first forwarding objective
        checkForward(forwardingObjective, ADD, expectSelector, nextObjective.id(), SPECIFIC);

        /*
         * Second set of objective
         */
        filteringObjective = (FilteringObjective) objectives.get(3);
        forwardingObjective = (ForwardingObjective) objectives.get(4);
        nextObjective = (NextObjective) objectives.get(5);

        // test case for first filtering objective
        checkFiltering(filteringObjective, inPortCriterion, intent.priority(),
                       null, appId, true, vlan100Selector.criteria());

        // test case for first next objective
        checkNext(nextObjective, SIMPLE, expectTreatment, expectSelector, ADD);

        // test case for first forwarding objective
        checkForward(forwardingObjective, ADD, expectSelector, nextObjective.id(), SPECIFIC);

        /*
         * 3rd set of objective
         */
        filteringObjective = (FilteringObjective) objectives.get(6);
        forwardingObjective = (ForwardingObjective) objectives.get(7);
        nextObjective = (NextObjective) objectives.get(8);

        // test case for first filtering objective
        checkFiltering(filteringObjective, inPortCriterion, intent.priority(),
                       null, appId, true, vlan100Selector.criteria());

        // test case for first next objective
        checkNext(nextObjective, SIMPLE, expectTreatment, expectSelector, ADD);

        // test case for first forwarding objective
        checkForward(forwardingObjective, ADD, expectSelector, nextObjective.id(), SPECIFIC);

        /*
         * 4th set of objective
         */
        filteringObjective = (FilteringObjective) objectives.get(9);
        forwardingObjective = (ForwardingObjective) objectives.get(10);
        nextObjective = (NextObjective) objectives.get(11);

        // test case for first filtering objective
        checkFiltering(filteringObjective, inPortCriterion, intent.priority(),
                       null, appId, true, vlan100Selector.criteria());

        // test case for first next objective
        checkNext(nextObjective, SIMPLE, expectTreatment, expectSelector, ADD);

        // test case for first forwarding objective
        checkForward(forwardingObjective, ADD, expectSelector, nextObjective.id(), SPECIFIC);

        /*
         * 5th set of objective
         */
        filteringObjective = (FilteringObjective) objectives.get(12);
        forwardingObjective = (ForwardingObjective) objectives.get(13);
        nextObjective = (NextObjective) objectives.get(14);

        expectSelector = DefaultTrafficSelector.builder(ethDstSelector)
                .matchVlanId(VLAN_100)
                .matchInPort(PortNumber.portNumber(3))
                .build();

        inPortCriterion =
                (PortCriterion) expectSelector.getCriterion(Criterion.Type.IN_PORT);

        // test case for first filtering objective
        checkFiltering(filteringObjective, inPortCriterion, intent.priority(),
                       null, appId, true, vlan100Selector.criteria());

        // test case for first next objective
        checkNext(nextObjective, SIMPLE, expectTreatment, expectSelector, ADD);

        // test case for first forwarding objective
        checkForward(forwardingObjective, ADD, expectSelector, nextObjective.id(), SPECIFIC);
    }

    /**
     * Multiple point to single point intent with only one switch.
     * We test the proper compilation of mp2sp with
     * trivial selector, trivial treatment and 1 hop.
     */
    @Test
    public void singleHopTestForMp() {
        Set<Link> testLinks = ImmutableSet.of();

        Set<FilteredConnectPoint> ingress = ImmutableSet.of(
                new FilteredConnectPoint(of1p1, vlan100Selector),
                new FilteredConnectPoint(of1p2, vlan100Selector)
        );

        Set<FilteredConnectPoint> egress = ImmutableSet.of(
                new FilteredConnectPoint(of1p3, vlan100Selector)
        );


        LinkCollectionIntent intent = LinkCollectionIntent.builder()
                .appId(appId)
                .selector(ethDstSelector)
                .treatment(treatment)
                .links(testLinks)
                .filteredIngressPoints(ingress)
                .filteredEgressPoints(egress)
                .build();

        List<Intent> result = compiler.compile(intent, Collections.emptyList());
        assertThat(result, hasSize(1));
        assertThat(result.get(0), instanceOf(FlowObjectiveIntent.class));

        FlowObjectiveIntent foIntent = (FlowObjectiveIntent) result.get(0);
        List<Objective> objectives = foIntent.objectives();
        assertThat(objectives, hasSize(6));

        TrafficSelector expectSelector = DefaultTrafficSelector
                .builder(ethDstSelector)
                .matchInPort(PortNumber.portNumber(1))
                .matchVlanId(VLAN_100)
                .build();

        TrafficTreatment expectTreatment = DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.portNumber(3))
                .build();

        /*
         * First set of objective
         */
        filteringObjective = (FilteringObjective) objectives.get(0);
        forwardingObjective = (ForwardingObjective) objectives.get(1);
        nextObjective = (NextObjective) objectives.get(2);

        PortCriterion inPortCriterion =
                (PortCriterion) expectSelector.getCriterion(Criterion.Type.IN_PORT);

        // test case for first filtering objective
        checkFiltering(filteringObjective, inPortCriterion, intent.priority(),
                       null, appId, true, vlan100Selector.criteria());

        // test case for first next objective
        checkNext(nextObjective, SIMPLE, expectTreatment, expectSelector, ADD);

        // test case for first forwarding objective
        checkForward(forwardingObjective, ADD, expectSelector, nextObjective.id(), SPECIFIC);

        /*
         * Second set of objective
         */
        filteringObjective = (FilteringObjective) objectives.get(3);
        forwardingObjective = (ForwardingObjective) objectives.get(4);
        nextObjective = (NextObjective) objectives.get(5);

        expectSelector = DefaultTrafficSelector.builder(ethDstSelector)
                .matchInPort(PortNumber.portNumber(2))
                .matchVlanId(VLAN_100)
                .build();

        inPortCriterion =
                (PortCriterion) expectSelector.getCriterion(Criterion.Type.IN_PORT);

        // test case for first filtering objective
        checkFiltering(filteringObjective, inPortCriterion, intent.priority(),
                       null, appId, true, vlan100Selector.criteria());

        // test case for first next objective
        checkNext(nextObjective, SIMPLE, expectTreatment, expectSelector, ADD);

        // test case for first forwarding objective
        checkForward(forwardingObjective, ADD, expectSelector, nextObjective.id(), SPECIFIC);
    }

    /**
     * Single point to multiple point intent with only one switch.
     * We test the proper compilation of sp2mp with
     * trivial selector, trivial treatment and 1 hop.
     */
    @Test
    public void singleHopTestForSp() {
        Set<Link> testLinks = ImmutableSet.of();

        Set<FilteredConnectPoint> ingress = ImmutableSet.of(
                new FilteredConnectPoint(of1p1, vlan100Selector)
        );

        Set<FilteredConnectPoint> egress = ImmutableSet.of(
                new FilteredConnectPoint(of1p2, vlan100Selector),
                new FilteredConnectPoint(of1p3, vlan100Selector)
        );


        LinkCollectionIntent intent = LinkCollectionIntent.builder()
                .appId(appId)
                .selector(ethDstSelector)
                .treatment(treatment)
                .links(testLinks)
                .filteredIngressPoints(ingress)
                .filteredEgressPoints(egress)
                .applyTreatmentOnEgress(true)
                .resourceGroup(resourceGroup2)
                .build();

        List<Intent> result = compiler.compile(intent, Collections.emptyList());
        assertThat(result, hasSize(1));
        assertThat(result.get(0), instanceOf(FlowObjectiveIntent.class));

        FlowObjectiveIntent foIntent = (FlowObjectiveIntent) result.get(0);
        List<Objective> objectives = foIntent.objectives();
        assertThat(objectives, hasSize(3));

        TrafficSelector expectSelector = DefaultTrafficSelector
                .builder(ethDstSelector)
                .matchInPort(PortNumber.portNumber(1))
                .matchVlanId(VLAN_100)
                .build();

        List<TrafficTreatment> expectTreatments = ImmutableList.of(
                DefaultTrafficTreatment.builder()
                        .setOutput(PortNumber.portNumber(2))
                        .build(),
                DefaultTrafficTreatment.builder()
                        .setOutput(PortNumber.portNumber(3))
                        .build()
        );

        /*
         * First set of objective
         */
        filteringObjective = (FilteringObjective) objectives.get(0);
        forwardingObjective = (ForwardingObjective) objectives.get(1);
        nextObjective = (NextObjective) objectives.get(2);

        PortCriterion inPortCriterion =
                (PortCriterion) expectSelector.getCriterion(Criterion.Type.IN_PORT);

        // test case for first filtering objective
        checkFiltering(filteringObjective, inPortCriterion, intent.priority(),
                       null, appId, true, vlan100Selector.criteria());

        // test case for first next objective
        checkNext(nextObjective, BROADCAST, expectTreatments, expectSelector, ADD);

        // test case for first forwarding objective
        checkForward(forwardingObjective, ADD, expectSelector, nextObjective.id(), SPECIFIC);
    }

    /**
     * Single point to multi point case. Scenario is the follow:
     *
     * -1 of1 2-1 of2 2--1 of3 2-
     *             3
     *             `-1 of4 2-
     *
     * We test the proper compilation constraint of sp2mp
     * with encapsulation, trivial selector, empty treatment and points.
     */
    @Test
    public void testFilteredConnectPointForSpWithEncap() throws Exception {
        LinkCollectionCompiler.labelAllocator.setLabelSelection(LABEL_SELECTION);
        Set<Link> testLinks = ImmutableSet.of(
                DefaultLink.builder().providerId(PID).src(of1p2).dst(of2p1).type(DIRECT).build(),
                DefaultLink.builder().providerId(PID).src(of2p2).dst(of3p1).type(DIRECT).build(),
                DefaultLink.builder().providerId(PID).src(of2p3).dst(of4p1).type(DIRECT).build()
        );

        Set<FilteredConnectPoint> ingress = ImmutableSet.of(
                new FilteredConnectPoint(of1p1, vlan100Selector)
        );

        Set<FilteredConnectPoint> egress = ImmutableSet.of(
                new FilteredConnectPoint(of3p2, vlan100Selector),
                new FilteredConnectPoint(of4p2, vlan100Selector)
        );

        TrafficSelector broadcastSelector = DefaultTrafficSelector.builder()
                .matchEthDst(MacAddress.BROADCAST)
                .build();

        EncapsulationConstraint constraint = new EncapsulationConstraint(EncapsulationType.VLAN);
        LinkCollectionIntent intent = LinkCollectionIntent.builder()
                .appId(appId)
                .selector(broadcastSelector)
                .treatment(treatment)
                .links(testLinks)
                .filteredIngressPoints(ingress)
                .filteredEgressPoints(egress)
                .applyTreatmentOnEgress(true)
                .resourceGroup(resourceGroup1)
                .constraints(ImmutableList.of(constraint))
                .build();

        List<Intent> result = compiler.compile(intent, Collections.emptyList());
        assertThat(result, hasSize(1));
        assertThat(result.get(0), instanceOf(FlowObjectiveIntent.class));

        FlowObjectiveIntent foIntent = (FlowObjectiveIntent) result.get(0);
        List<Objective> objectives = foIntent.objectives();
        assertThat(objectives, hasSize(12));

        /*
         * First set of objective
         */
        filteringObjective = (FilteringObjective) objectives.get(0);
        forwardingObjective = (ForwardingObjective) objectives.get(1);
        nextObjective = (NextObjective) objectives.get(2);

        // expect selector and treatment
        TrafficSelector expectSelector = DefaultTrafficSelector.builder()
                .matchInPort(PortNumber.portNumber(1))
                .matchVlanId(VLAN_100)
                .matchEthDst(MacAddress.BROADCAST)
                .build();
        List<TrafficTreatment> expectTreatments = ImmutableList.of(
                DefaultTrafficTreatment.builder()
                        .setVlanId(VLAN_1)
                        .setOutput(PortNumber.portNumber(2))
                        .build()
        );
        TrafficSelector filteringSelector = vlan100Selector;
        PortCriterion inPortCriterion =
                (PortCriterion) expectSelector.getCriterion(Criterion.Type.IN_PORT);

        // test case for first filtering objective
        checkFiltering(filteringObjective, inPortCriterion, intent.priority(),
                       null, appId, true, filteringSelector.criteria());

        // test case for first next objective
        checkNext(nextObjective, BROADCAST, expectTreatments, expectSelector, ADD);

        // test case for first forwarding objective
        checkForward(forwardingObjective, ADD, expectSelector, nextObjective.id(), SPECIFIC);

        /*
         * Second set of objective
         */
        filteringObjective = (FilteringObjective) objectives.get(3);
        forwardingObjective = (ForwardingObjective) objectives.get(4);
        nextObjective = (NextObjective) objectives.get(5);
        expectSelector = DefaultTrafficSelector.builder()
                .matchInPort(PortNumber.portNumber(1))
                .matchVlanId(VLAN_1)
                .build();
        expectTreatments = ImmutableList.of(
                DefaultTrafficTreatment.builder()
                        .setVlanId(VLAN_100)
                        .setOutput(PortNumber.portNumber(2))
                        .build()
        );
        filteringSelector = vlan1Selector;
        inPortCriterion =
                (PortCriterion) expectSelector.getCriterion(Criterion.Type.IN_PORT);

        // test case for first filtering objective
        checkFiltering(filteringObjective, inPortCriterion, intent.priority(),
                       null, appId, true, filteringSelector.criteria());

        // test case for second next objective
        checkNext(nextObjective, BROADCAST, expectTreatments, expectSelector, ADD);

        // test case for second forwarding objective
        checkForward(forwardingObjective, ADD, expectSelector, nextObjective.id(), SPECIFIC);

        /*
         * 3rd set of objective
         */
        filteringObjective = (FilteringObjective) objectives.get(6);
        forwardingObjective = (ForwardingObjective) objectives.get(7);
        nextObjective = (NextObjective) objectives.get(8);
        inPortCriterion =
                (PortCriterion) expectSelector.getCriterion(Criterion.Type.IN_PORT);

        // test case for first filtering objective
        checkFiltering(filteringObjective, inPortCriterion, intent.priority(),
                       null, appId, true, filteringSelector.criteria());

        // test case for 3rd next objective
        checkNext(nextObjective, BROADCAST, expectTreatments, expectSelector, ADD);

        // test case for 3rd forwarding objective
        checkForward(forwardingObjective, ADD, expectSelector, nextObjective.id(), SPECIFIC);

        /*
         * 4th set of objective
         */
        filteringObjective = (FilteringObjective) objectives.get(9);
        forwardingObjective = (ForwardingObjective) objectives.get(10);
        nextObjective = (NextObjective) objectives.get(11);

        inPortCriterion =
                (PortCriterion) expectSelector.getCriterion(Criterion.Type.IN_PORT);

        // test case for first filtering objective
        checkFiltering(filteringObjective, inPortCriterion, intent.priority(),
                       null, appId, true, filteringSelector.criteria());

        // test case for 3rd next objective
        expectTreatments = ImmutableList.of(
                DefaultTrafficTreatment.builder()
                        .setVlanId(VLAN_1)
                        .setOutput(PortNumber.portNumber(2))
                        .build(),
                DefaultTrafficTreatment.builder()
                        .setVlanId(VLAN_1)
                        .setOutput(PortNumber.portNumber(3))
                        .build()
        );
        checkNext(nextObjective, BROADCAST, expectTreatments, expectSelector, ADD);

        // test case for 3rd forwarding objective
        checkForward(forwardingObjective, ADD, expectSelector, nextObjective.id(), SPECIFIC);
    }

    /**
     * Multi point to single point intent with filtered connect point.
     * Scenario is the follow:
     *
     * -1 of1 2-1 of2 2-1 of4 2-
     *             3
     * -1 of3 2---/
     * We test the proper compilation constraint of mp2sp
     * with encapsulation, trivial selector, empty treatment and points.
     */
    @Test
    public void testFilteredConnectPointForMpWithEncap() throws Exception {
        LinkCollectionCompiler.labelAllocator.setLabelSelection(LABEL_SELECTION);
        Set<Link> testLinks = ImmutableSet.of(
                DefaultLink.builder().providerId(PID).src(of1p2).dst(of2p1).type(DIRECT).build(),
                DefaultLink.builder().providerId(PID).src(of3p2).dst(of2p3).type(DIRECT).build(),
                DefaultLink.builder().providerId(PID).src(of2p2).dst(of4p1).type(DIRECT).build()
        );
        Set<FilteredConnectPoint> ingress = ImmutableSet.of(
                new FilteredConnectPoint(of3p1, vlan100Selector),
                new FilteredConnectPoint(of1p1, vlan100Selector)
        );
        Set<FilteredConnectPoint> egress = ImmutableSet.of(
                new FilteredConnectPoint(of4p2, vlan100Selector)
        );
        EncapsulationConstraint constraint = new EncapsulationConstraint(EncapsulationType.VLAN);
        LinkCollectionIntent intent = LinkCollectionIntent.builder()
                .appId(appId)
                .selector(ethDstSelector)
                .treatment(treatment)
                .links(testLinks)
                .filteredIngressPoints(ingress)
                .filteredEgressPoints(egress)
                .constraints(ImmutableList.of(constraint))
                .build();
        List<Intent> result = compiler.compile(intent, Collections.emptyList());
        assertThat(result, hasSize(1));
        assertThat(result.get(0), instanceOf(FlowObjectiveIntent.class));
        FlowObjectiveIntent foIntent = (FlowObjectiveIntent) result.get(0);
        List<Objective> objectives = foIntent.objectives();
        assertThat(objectives, hasSize(15));
        TrafficSelector expectSelector = DefaultTrafficSelector
                .builder(ethDstSelector)
                .matchInPort(PortNumber.portNumber(1))
                .matchVlanId(VLAN_100)
                .build();
        TrafficSelector filteringSelector = vlan100Selector;
        TrafficTreatment expectTreatment = DefaultTrafficTreatment.builder()
                .setVlanId(VLAN_1)
                .setOutput(PortNumber.portNumber(2))
                .build();
        /*
         * First set of objective
         */
        filteringObjective = (FilteringObjective) objectives.get(0);
        forwardingObjective = (ForwardingObjective) objectives.get(1);
        nextObjective = (NextObjective) objectives.get(2);
        PortCriterion inPortCriterion =
                (PortCriterion) expectSelector.getCriterion(Criterion.Type.IN_PORT);
        // test case for first filtering objective
        checkFiltering(filteringObjective, inPortCriterion, intent.priority(),
                       null, appId, true, filteringSelector.criteria());
        // test case for first next objective
        checkNext(nextObjective, SIMPLE, expectTreatment, expectSelector, ADD);
        // test case for first forwarding objective
        checkForward(forwardingObjective, ADD, expectSelector, nextObjective.id(), SPECIFIC);
        /*
         * Second set of objective
         */
        filteringObjective = (FilteringObjective) objectives.get(3);
        forwardingObjective = (ForwardingObjective) objectives.get(4);
        nextObjective = (NextObjective) objectives.get(5);
        expectSelector = DefaultTrafficSelector
                .builder()
                .matchInPort(PortNumber.portNumber(1))
                .matchVlanId(VLAN_1)
                .build();
        filteringSelector = vlan1Selector;
        expectTreatment = DefaultTrafficTreatment
                .builder()
                .setVlanId(VLAN_100)
                .setOutput(PortNumber.portNumber(2))
                .build();
        // test case for second filtering objective
        checkFiltering(filteringObjective, inPortCriterion, intent.priority(),
                       null, appId, true, filteringSelector.criteria());
        // test case for second next objective
        checkNext(nextObjective, SIMPLE, expectTreatment, expectSelector, ADD);
        // test case for second forwarding objective
        checkForward(forwardingObjective, ADD, expectSelector, nextObjective.id(), SPECIFIC);
        /*
         * 3rd set of objective
         */
        filteringObjective = (FilteringObjective) objectives.get(6);
        forwardingObjective = (ForwardingObjective) objectives.get(7);
        nextObjective = (NextObjective) objectives.get(8);
        filteringSelector = vlan100Selector;
        expectTreatment = DefaultTrafficTreatment
                .builder()
                .setVlanId(VLAN_1)
                .setOutput(PortNumber.portNumber(2))
                .build();
        expectSelector = DefaultTrafficSelector
                .builder(ethDstSelector)
                .matchInPort(PortNumber.portNumber(1))
                .matchVlanId(VLAN_100)
                .build();
        // test case for 3rd filtering objective
        checkFiltering(filteringObjective, inPortCriterion, intent.priority(),
                       null, appId, true, filteringSelector.criteria());
        // test case for 3rd next objective
        checkNext(nextObjective, SIMPLE, expectTreatment, expectSelector, ADD);
        // test case for 3rd forwarding objective
        checkForward(forwardingObjective, ADD, expectSelector, nextObjective.id(), SPECIFIC);
        /*
         * 4th set of objective
         */
        filteringObjective = (FilteringObjective) objectives.get(9);
        forwardingObjective = (ForwardingObjective) objectives.get(10);
        nextObjective = (NextObjective) objectives.get(11);
        filteringSelector = vlan1Selector;
        expectSelector = DefaultTrafficSelector
                .builder()
                .matchInPort(PortNumber.portNumber(1))
                .matchVlanId(VLAN_1)
                .build();
        // test case for 4th filtering objective
        checkFiltering(filteringObjective, inPortCriterion, intent.priority(),
                       null, appId, true, filteringSelector.criteria());
        // test case for 4th next objective
        checkNext(nextObjective, SIMPLE, expectTreatment, expectSelector, ADD);
        // test case for 4th forwarding objective
        checkForward(forwardingObjective, ADD, expectSelector, nextObjective.id(), SPECIFIC);

        /*
         * 5th set of objective
         */
        filteringObjective = (FilteringObjective) objectives.get(12);
        forwardingObjective = (ForwardingObjective) objectives.get(13);
        nextObjective = (NextObjective) objectives.get(14);
        expectSelector = DefaultTrafficSelector.builder()
                .matchVlanId(VlanId.vlanId("1"))
                .matchInPort(PortNumber.portNumber(3))
                .build();
        inPortCriterion =
                (PortCriterion) expectSelector.getCriterion(Criterion.Type.IN_PORT);
        // test case for 5th filtering objective
        checkFiltering(filteringObjective, inPortCriterion, intent.priority(),
                       null, appId, true, filteringSelector.criteria());
        // test case for 5th next objective
        checkNext(nextObjective, SIMPLE, expectTreatment, expectSelector, ADD);
        // test case for 5th forwarding objective
        checkForward(forwardingObjective, ADD, expectSelector,
                     nextObjective.id(), SPECIFIC);
    }

    private void checkFiltering(FilteringObjective filteringObjective,
                           Criterion key,
                           int priority,
                           TrafficSelector meta,
                           ApplicationId appId,
                           boolean permanent,
                           Collection<Criterion> conditions) {
        conditions = ImmutableList.copyOf(conditions);
        assertThat(filteringObjective.key(), is(key));
        assertThat(filteringObjective.priority(), is(priority));
        assertThat(filteringObjective.meta(), is(meta));
        assertThat(filteringObjective.appId(), is(appId));
        assertThat(filteringObjective.permanent(), is(permanent));
        assertThat(filteringObjective.conditions(), is(conditions));
    }

    private void checkNext(NextObjective nextObjective,
                           NextObjective.Type type,
                           TrafficTreatment next,
                           TrafficSelector meta,
                           Objective.Operation op) {
        checkNext(nextObjective, type, ImmutableList.of(next), meta, op);
    }

    private void checkNext(NextObjective nextObjective,
                           NextObjective.Type type,
                           Collection<TrafficTreatment> next,
                           TrafficSelector meta,
                           Objective.Operation op) {
        assertThat(nextObjective.type(), is(type));
        assertThat(nextObjective.next().size(), is(next.size()));
        assertThat(nextObjective.next().containsAll(next), is(true));
        assertThat(nextObjective.meta(), is(meta));
        assertThat(nextObjective.op(), is(op));
    }

    private void checkForward(ForwardingObjective forwardingObjective,
                              Objective.Operation op,
                              TrafficSelector selector,
                              int nextId,
                              ForwardingObjective.Flag flag) {
        assertThat(forwardingObjective.op(), is(op));
        assertThat(forwardingObjective.selector(), is(selector));
        assertThat(forwardingObjective.nextId(), is(nextId));
        assertThat(forwardingObjective.flag(), is(flag));
    }
}
