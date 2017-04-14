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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.cfg.ComponentConfigAdapter;
import org.onosproject.core.CoreService;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.FilteredConnectPoint;
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;
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
import org.onosproject.net.resource.MockResourceService;
import org.onosproject.net.resource.ResourceService;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.easymock.EasyMock.*;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.onosproject.net.Link.Type.DIRECT;
import static org.onosproject.net.NetTestTools.PID;

public class LinkCollectionIntentObjectiveCompilerTest extends AbstractLinkCollectionTest {

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

        Intent.bindIdGenerator(idGenerator);

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

        replay(coreService, intentExtensionService);

    }

    @After
    public void tearDown() {
        Intent.unbindIdGenerator(idGenerator);
    }

    /**
     * We test the proper compilation of a simple link collection intent
     * with connect points, empty trivial treatment and empty trivial selector.
     */
    @Test
    public void testCompile() {
        compiler.activate();

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
        assertThat(objectives, hasSize(9));

        /*
         * First set of objective
         */
        filteringObjective = (FilteringObjective) objectives.get(0);
        forwardingObjective = (ForwardingObjective) objectives.get(1);
        nextObjective = (NextObjective) objectives.get(2);

        // expect selector and treatment
        TrafficSelector expectSelector = DefaultTrafficSelector.builder()
                .matchInPort(PortNumber.portNumber(1))
                .build();
        TrafficTreatment expectTreatment = DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.portNumber(1))
                .build();
        PortCriterion inPortCriterion =
                (PortCriterion) expectSelector.getCriterion(Criterion.Type.IN_PORT);

        // test case for first filtering objective
        assertThat(filteringObjective.key(), is(inPortCriterion));
        assertThat(filteringObjective.priority(), is(intent.priority()));
        assertThat(filteringObjective.meta(), nullValue());
        assertThat(filteringObjective.appId(), is(appId));
        assertThat(filteringObjective.permanent(), is(true));
        assertThat(filteringObjective.conditions(), is(Lists.newArrayList(expectSelector.criteria())));

        // test case for first next objective
        assertThat(nextObjective.type(), is(NextObjective.Type.SIMPLE));
        assertThat(nextObjective.next(), hasSize(1));
        assertThat(nextObjective.next().iterator().next(), is(expectTreatment));
        assertThat(nextObjective.meta(), is(expectSelector));
        assertThat(nextObjective.op(), is(Objective.Operation.ADD));

        // test case for first forwarding objective
        assertThat(forwardingObjective.op(), is(Objective.Operation.ADD));
        assertThat(forwardingObjective.selector(), is(expectSelector));
        assertThat(forwardingObjective.nextId(), is(nextObjective.id()));
        assertThat(forwardingObjective.flag(), is(ForwardingObjective.Flag.SPECIFIC));

        /*
         * Second set of objective
         */
        filteringObjective = (FilteringObjective) objectives.get(3);
        forwardingObjective = (ForwardingObjective) objectives.get(4);
        nextObjective = (NextObjective) objectives.get(5);


        expectSelector = DefaultTrafficSelector.builder()
                .matchInPort(PortNumber.portNumber(0))
                .build();

        inPortCriterion =
                (PortCriterion) expectSelector.getCriterion(Criterion.Type.IN_PORT);

        // test case for first filtering objective
        assertThat(filteringObjective.key(), is(inPortCriterion));
        assertThat(filteringObjective.priority(), is(intent.priority()));
        assertThat(filteringObjective.meta(), nullValue());
        assertThat(filteringObjective.appId(), is(appId));
        assertThat(filteringObjective.permanent(), is(true));
        assertThat(filteringObjective.conditions(), is(Lists.newArrayList(expectSelector.criteria())));

        // test case for second next objective
        assertThat(nextObjective.type(), is(NextObjective.Type.SIMPLE));
        assertThat(nextObjective.next(), hasSize(1));
        assertThat(nextObjective.next().iterator().next(), is(expectTreatment));
        assertThat(nextObjective.meta(), is(expectSelector));
        assertThat(nextObjective.op(), is(Objective.Operation.ADD));

        // test case for second forwarding objective
        assertThat(forwardingObjective.op(), is(Objective.Operation.ADD));
        assertThat(forwardingObjective.selector(), is(expectSelector));
        assertThat(forwardingObjective.nextId(), is(nextObjective.id()));
        assertThat(forwardingObjective.flag(), is(ForwardingObjective.Flag.SPECIFIC));

        /*
         * 3rd set of objective
         */
        filteringObjective = (FilteringObjective) objectives.get(6);
        forwardingObjective = (ForwardingObjective) objectives.get(7);
        nextObjective = (NextObjective) objectives.get(8);

        expectSelector = DefaultTrafficSelector.builder()
                .matchInPort(PortNumber.portNumber(1))
                .build();

        inPortCriterion =
                (PortCriterion) expectSelector.getCriterion(Criterion.Type.IN_PORT);

        // test case for first filtering objective
        assertThat(filteringObjective.key(), is(inPortCriterion));
        assertThat(filteringObjective.priority(), is(intent.priority()));
        assertThat(filteringObjective.meta(), nullValue());
        assertThat(filteringObjective.appId(), is(appId));
        assertThat(filteringObjective.permanent(), is(true));
        assertThat(filteringObjective.conditions(), is(Lists.newArrayList(expectSelector.criteria())));

        // test case for 3rd next objective
        assertThat(nextObjective.type(), is(NextObjective.Type.SIMPLE));
        assertThat(nextObjective.next(), hasSize(1));
        assertThat(nextObjective.next().iterator().next(), is(expectTreatment));
        assertThat(nextObjective.meta(), is(expectSelector));
        assertThat(nextObjective.op(), is(Objective.Operation.ADD));

        // test case for 3rd forwarding objective
        assertThat(forwardingObjective.op(), is(Objective.Operation.ADD));
        assertThat(forwardingObjective.selector(), is(expectSelector));
        assertThat(forwardingObjective.nextId(), is(nextObjective.id()));
        assertThat(forwardingObjective.flag(), is(ForwardingObjective.Flag.SPECIFIC));

        compiler.deactivate();
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
        compiler.activate();
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
                .matchVlanId(VlanId.vlanId("100"))
                .matchEthDst(MacAddress.BROADCAST)
                .build();
        TrafficTreatment expectTreatment = DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.portNumber(2))
                .build();

        PortCriterion inPortCriterion =
                (PortCriterion) expectSelector.getCriterion(Criterion.Type.IN_PORT);

        // test case for first filtering objective
        assertThat(filteringObjective.key(), is(inPortCriterion));
        assertThat(filteringObjective.priority(), is(intent.priority()));
        assertThat(filteringObjective.meta(), nullValue());
        assertThat(filteringObjective.appId(), is(appId));
        assertThat(filteringObjective.permanent(), is(true));
        assertThat(filteringObjective.conditions(), is(Lists.newArrayList(expectSelector.criteria())));

        // test case for first next objective
        assertThat(nextObjective.type(), is(NextObjective.Type.BROADCAST));
        assertThat(nextObjective.next(), hasSize(1));
        assertThat(nextObjective.next().iterator().next(), is(expectTreatment));
        assertThat(nextObjective.meta(), is(expectSelector));
        assertThat(nextObjective.op(), is(Objective.Operation.ADD));

        // test case for first forwarding objective
        assertThat(forwardingObjective.op(), is(Objective.Operation.ADD));
        assertThat(forwardingObjective.selector(), is(expectSelector));
        assertThat(forwardingObjective.nextId(), is(nextObjective.id()));
        assertThat(forwardingObjective.flag(), is(ForwardingObjective.Flag.SPECIFIC));

        /*
         * Second set of objective
         */
        filteringObjective = (FilteringObjective) objectives.get(3);
        forwardingObjective = (ForwardingObjective) objectives.get(4);
        nextObjective = (NextObjective) objectives.get(5);

        inPortCriterion =
                (PortCriterion) expectSelector.getCriterion(Criterion.Type.IN_PORT);

        // test case for first filtering objective
        assertThat(filteringObjective.key(), is(inPortCriterion));
        assertThat(filteringObjective.priority(), is(intent.priority()));
        assertThat(filteringObjective.meta(), nullValue());
        assertThat(filteringObjective.appId(), is(appId));
        assertThat(filteringObjective.permanent(), is(true));
        assertThat(filteringObjective.conditions(), is(Lists.newArrayList(expectSelector.criteria())));

        // test case for second next objective
        assertThat(nextObjective.type(), is(NextObjective.Type.BROADCAST));
        assertThat(nextObjective.next(), hasSize(1));
        assertThat(nextObjective.next().iterator().next(), is(expectTreatment));
        assertThat(nextObjective.meta(), is(expectSelector));
        assertThat(nextObjective.op(), is(Objective.Operation.ADD));

        // test case for second forwarding objective
        assertThat(forwardingObjective.op(), is(Objective.Operation.ADD));
        assertThat(forwardingObjective.selector(), is(expectSelector));
        assertThat(forwardingObjective.nextId(), is(nextObjective.id()));
        assertThat(forwardingObjective.flag(), is(ForwardingObjective.Flag.SPECIFIC));

        /*
         * 3rd set of objective
         */
        filteringObjective = (FilteringObjective) objectives.get(6);
        forwardingObjective = (ForwardingObjective) objectives.get(7);
        nextObjective = (NextObjective) objectives.get(8);

        inPortCriterion =
                (PortCriterion) expectSelector.getCriterion(Criterion.Type.IN_PORT);

        // test case for first filtering objective
        assertThat(filteringObjective.key(), is(inPortCriterion));
        assertThat(filteringObjective.priority(), is(intent.priority()));
        assertThat(filteringObjective.meta(), nullValue());
        assertThat(filteringObjective.appId(), is(appId));
        assertThat(filteringObjective.permanent(), is(true));
        assertThat(filteringObjective.conditions(), is(Lists.newArrayList(expectSelector.criteria())));

        // test case for 3rd next objective
        assertThat(nextObjective.type(), is(NextObjective.Type.BROADCAST));
        assertThat(nextObjective.next(), hasSize(1));
        assertThat(nextObjective.next().iterator().next(), is(expectTreatment));
        assertThat(nextObjective.meta(), is(expectSelector));
        assertThat(nextObjective.op(), is(Objective.Operation.ADD));

        // test case for 3rd forwarding objective
        assertThat(forwardingObjective.op(), is(Objective.Operation.ADD));
        assertThat(forwardingObjective.selector(), is(expectSelector));
        assertThat(forwardingObjective.nextId(), is(nextObjective.id()));
        assertThat(forwardingObjective.flag(), is(ForwardingObjective.Flag.SPECIFIC));

        /*
         * 4th set of objective
         */
        filteringObjective = (FilteringObjective) objectives.get(9);
        forwardingObjective = (ForwardingObjective) objectives.get(10);
        nextObjective = (NextObjective) objectives.get(11);

        inPortCriterion =
                (PortCriterion) expectSelector.getCriterion(Criterion.Type.IN_PORT);

        // test case for first filtering objective
        assertThat(filteringObjective.key(), is(inPortCriterion));
        assertThat(filteringObjective.priority(), is(intent.priority()));
        assertThat(filteringObjective.meta(), nullValue());
        assertThat(filteringObjective.appId(), is(appId));
        assertThat(filteringObjective.permanent(), is(true));
        assertThat(filteringObjective.conditions(), is(Lists.newArrayList(expectSelector.criteria())));

        // test case for 3rd next objective
        assertThat(nextObjective.type(), is(NextObjective.Type.BROADCAST));

        // have 2 treatments in this objective
        assertThat(nextObjective.next(), hasSize(2));
        expectTreatment = DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.portNumber(2))
                .build();
        assertThat(nextObjective.next(), hasItem(expectTreatment));

        expectTreatment = DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.portNumber(3))
                .build();
        assertThat(nextObjective.next(), hasItem(expectTreatment));

        assertThat(nextObjective.meta(), is(expectSelector));
        assertThat(nextObjective.op(), is(Objective.Operation.ADD));

        // test case for 3rd forwarding objective
        assertThat(forwardingObjective.op(), is(Objective.Operation.ADD));
        assertThat(forwardingObjective.selector(), is(expectSelector));
        assertThat(forwardingObjective.nextId(), is(nextObjective.id()));
        assertThat(forwardingObjective.flag(), is(ForwardingObjective.Flag.SPECIFIC));

        compiler.deactivate();
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
        compiler.activate();
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
                .matchVlanId(VlanId.vlanId("100"))
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
        assertThat(filteringObjective.key(), is(inPortCriterion));
        assertThat(filteringObjective.priority(), is(intent.priority()));
        assertThat(filteringObjective.meta(), nullValue());
        assertThat(filteringObjective.appId(), is(appId));
        assertThat(filteringObjective.permanent(), is(true));
        assertThat(filteringObjective.conditions(), is(Lists.newArrayList(expectSelector.criteria())));

        // test case for first next objective
        assertThat(nextObjective.type(), is(NextObjective.Type.SIMPLE));
        assertThat(nextObjective.next(), hasSize(1));
        assertThat(nextObjective.next().iterator().next(), is(expectTreatment));
        assertThat(nextObjective.meta(), is(expectSelector));
        assertThat(nextObjective.op(), is(Objective.Operation.ADD));

        // test case for first forwarding objective
        assertThat(forwardingObjective.op(), is(Objective.Operation.ADD));
        assertThat(forwardingObjective.selector(), is(expectSelector));
        assertThat(forwardingObjective.nextId(), is(nextObjective.id()));
        assertThat(forwardingObjective.flag(), is(ForwardingObjective.Flag.SPECIFIC));

        /*
         * Second set of objective
         */
        filteringObjective = (FilteringObjective) objectives.get(3);
        forwardingObjective = (ForwardingObjective) objectives.get(4);
        nextObjective = (NextObjective) objectives.get(5);

        // test case for first filtering objective
        assertThat(filteringObjective.key(), is(inPortCriterion));
        assertThat(filteringObjective.priority(), is(intent.priority()));
        assertThat(filteringObjective.meta(), nullValue());
        assertThat(filteringObjective.appId(), is(appId));
        assertThat(filteringObjective.permanent(), is(true));
        assertThat(filteringObjective.conditions(), is(Lists.newArrayList(expectSelector.criteria())));

        // test case for first next objective
        assertThat(nextObjective.type(), is(NextObjective.Type.SIMPLE));
        assertThat(nextObjective.next(), hasSize(1));
        assertThat(nextObjective.next().iterator().next(), is(expectTreatment));
        assertThat(nextObjective.meta(), is(expectSelector));
        assertThat(nextObjective.op(), is(Objective.Operation.ADD));

        // test case for first forwarding objective
        assertThat(forwardingObjective.op(), is(Objective.Operation.ADD));
        assertThat(forwardingObjective.selector(), is(expectSelector));
        assertThat(forwardingObjective.nextId(), is(nextObjective.id()));
        assertThat(forwardingObjective.flag(), is(ForwardingObjective.Flag.SPECIFIC));

        /*
         * 3rd set of objective
         */
        filteringObjective = (FilteringObjective) objectives.get(6);
        forwardingObjective = (ForwardingObjective) objectives.get(7);
        nextObjective = (NextObjective) objectives.get(8);

        // test case for first filtering objective
        assertThat(filteringObjective.key(), is(inPortCriterion));
        assertThat(filteringObjective.priority(), is(intent.priority()));
        assertThat(filteringObjective.meta(), nullValue());
        assertThat(filteringObjective.appId(), is(appId));
        assertThat(filteringObjective.permanent(), is(true));
        assertThat(filteringObjective.conditions(), is(Lists.newArrayList(expectSelector.criteria())));

        // test case for first next objective
        assertThat(nextObjective.type(), is(NextObjective.Type.SIMPLE));
        assertThat(nextObjective.next(), hasSize(1));
        assertThat(nextObjective.next().iterator().next(), is(expectTreatment));
        assertThat(nextObjective.meta(), is(expectSelector));
        assertThat(nextObjective.op(), is(Objective.Operation.ADD));

        // test case for first forwarding objective
        assertThat(forwardingObjective.op(), is(Objective.Operation.ADD));
        assertThat(forwardingObjective.selector(), is(expectSelector));
        assertThat(forwardingObjective.nextId(), is(nextObjective.id()));
        assertThat(forwardingObjective.flag(), is(ForwardingObjective.Flag.SPECIFIC));

        /*
         * 4th set of objective
         */
        filteringObjective = (FilteringObjective) objectives.get(9);
        forwardingObjective = (ForwardingObjective) objectives.get(10);
        nextObjective = (NextObjective) objectives.get(11);

        // test case for first filtering objective
        assertThat(filteringObjective.key(), is(inPortCriterion));
        assertThat(filteringObjective.priority(), is(intent.priority()));
        assertThat(filteringObjective.meta(), nullValue());
        assertThat(filteringObjective.appId(), is(appId));
        assertThat(filteringObjective.permanent(), is(true));
        assertThat(filteringObjective.conditions(), is(Lists.newArrayList(expectSelector.criteria())));

        // test case for first next objective
        assertThat(nextObjective.type(), is(NextObjective.Type.SIMPLE));
        assertThat(nextObjective.next(), hasSize(1));
        assertThat(nextObjective.next().iterator().next(), is(expectTreatment));
        assertThat(nextObjective.meta(), is(expectSelector));
        assertThat(nextObjective.op(), is(Objective.Operation.ADD));

        // test case for first forwarding objective
        assertThat(forwardingObjective.op(), is(Objective.Operation.ADD));
        assertThat(forwardingObjective.selector(), is(expectSelector));
        assertThat(forwardingObjective.nextId(), is(nextObjective.id()));
        assertThat(forwardingObjective.flag(), is(ForwardingObjective.Flag.SPECIFIC));

        /*
         * 5th set of objective
         */
        filteringObjective = (FilteringObjective) objectives.get(12);
        forwardingObjective = (ForwardingObjective) objectives.get(13);
        nextObjective = (NextObjective) objectives.get(14);

        expectSelector = DefaultTrafficSelector.builder(ethDstSelector)
                .matchVlanId(VlanId.vlanId("100"))
                .matchInPort(PortNumber.portNumber(3))
                .build();

        inPortCriterion =
                (PortCriterion) expectSelector.getCriterion(Criterion.Type.IN_PORT);

        // test case for first filtering objective
        assertThat(filteringObjective.key(), is(inPortCriterion));
        assertThat(filteringObjective.priority(), is(intent.priority()));
        assertThat(filteringObjective.meta(), nullValue());
        assertThat(filteringObjective.appId(), is(appId));
        assertThat(filteringObjective.permanent(), is(true));
        assertThat(filteringObjective.conditions(), is(Lists.newArrayList(expectSelector.criteria())));

        // test case for first next objective
        assertThat(nextObjective.type(), is(NextObjective.Type.SIMPLE));
        assertThat(nextObjective.next(), hasSize(1));
        assertThat(nextObjective.next().iterator().next(), is(expectTreatment));
        assertThat(nextObjective.meta(), is(expectSelector));
        assertThat(nextObjective.op(), is(Objective.Operation.ADD));

        // test case for first forwarding objective
        assertThat(forwardingObjective.op(), is(Objective.Operation.ADD));
        assertThat(forwardingObjective.selector(), is(expectSelector));
        assertThat(forwardingObjective.nextId(), is(nextObjective.id()));
        assertThat(forwardingObjective.flag(), is(ForwardingObjective.Flag.SPECIFIC));
        compiler.deactivate();
    }

    /**
     * Multiple point to single point intent with only one switch.
     * We test the proper compilation of mp2sp with
     * trivial selector, trivial treatment and 1 hop.
     */
    @Test
    public void singleHopTestForMp() {
        compiler.activate();
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
                .matchVlanId(VlanId.vlanId("100"))
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
        assertThat(filteringObjective.key(), is(inPortCriterion));
        assertThat(filteringObjective.priority(), is(intent.priority()));
        assertThat(filteringObjective.meta(), nullValue());
        assertThat(filteringObjective.appId(), is(appId));
        assertThat(filteringObjective.permanent(), is(true));
        assertThat(filteringObjective.conditions(), is(Lists.newArrayList(expectSelector.criteria())));

        // test case for first next objective
        assertThat(nextObjective.type(), is(NextObjective.Type.SIMPLE));
        assertThat(nextObjective.next(), hasSize(1));
        assertThat(nextObjective.next().iterator().next(), is(expectTreatment));
        assertThat(nextObjective.meta(), is(expectSelector));
        assertThat(nextObjective.op(), is(Objective.Operation.ADD));

        // test case for first forwarding objective
        assertThat(forwardingObjective.op(), is(Objective.Operation.ADD));
        assertThat(forwardingObjective.selector(), is(expectSelector));
        assertThat(forwardingObjective.nextId(), is(nextObjective.id()));
        assertThat(forwardingObjective.flag(), is(ForwardingObjective.Flag.SPECIFIC));

        /*
         * Second set of objective
         */
        filteringObjective = (FilteringObjective) objectives.get(3);
        forwardingObjective = (ForwardingObjective) objectives.get(4);
        nextObjective = (NextObjective) objectives.get(5);

        expectSelector = DefaultTrafficSelector.builder(ethDstSelector)
                .matchInPort(PortNumber.portNumber(2))
                .matchVlanId(VlanId.vlanId("100"))
                .build();

        inPortCriterion =
                (PortCriterion) expectSelector.getCriterion(Criterion.Type.IN_PORT);

        // test case for first filtering objective
        assertThat(filteringObjective.key(), is(inPortCriterion));
        assertThat(filteringObjective.priority(), is(intent.priority()));
        assertThat(filteringObjective.meta(), nullValue());
        assertThat(filteringObjective.appId(), is(appId));
        assertThat(filteringObjective.permanent(), is(true));
        assertThat(filteringObjective.conditions(), is(Lists.newArrayList(expectSelector.criteria())));

        // test case for first next objective
        assertThat(nextObjective.type(), is(NextObjective.Type.SIMPLE));
        assertThat(nextObjective.next(), hasSize(1));
        assertThat(nextObjective.next().iterator().next(), is(expectTreatment));
        assertThat(nextObjective.meta(), is(expectSelector));
        assertThat(nextObjective.op(), is(Objective.Operation.ADD));

        // test case for first forwarding objective
        assertThat(forwardingObjective.op(), is(Objective.Operation.ADD));
        assertThat(forwardingObjective.selector(), is(expectSelector));
        assertThat(forwardingObjective.nextId(), is(nextObjective.id()));
        assertThat(forwardingObjective.flag(), is(ForwardingObjective.Flag.SPECIFIC));
        compiler.deactivate();
    }

    /**
     * Single point to multiple point intent with only one switch.
     * We test the proper compilation of sp2mp with
     * trivial selector, trivial treatment and 1 hop.
     */
    @Test
    public void singleHopTestForSp() {
        compiler.activate();
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
                .matchVlanId(VlanId.vlanId("100"))
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
        assertThat(filteringObjective.key(), is(inPortCriterion));
        assertThat(filteringObjective.priority(), is(intent.priority()));
        assertThat(filteringObjective.meta(), nullValue());
        assertThat(filteringObjective.appId(), is(appId));
        assertThat(filteringObjective.permanent(), is(true));
        assertThat(filteringObjective.conditions(), is(Lists.newArrayList(expectSelector.criteria())));

        // test case for first next objective
        assertThat(nextObjective.type(), is(NextObjective.Type.BROADCAST));
        assertThat(nextObjective.next(), hasSize(2));
        assertThat(nextObjective.next(), hasItem(expectTreatment));
        expectTreatment = DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.portNumber(3))
                .build();
        assertThat(nextObjective.next(), hasItem(expectTreatment));
        assertThat(nextObjective.meta(), is(expectSelector));
        assertThat(nextObjective.op(), is(Objective.Operation.ADD));

        // test case for first forwarding objective
        assertThat(forwardingObjective.op(), is(Objective.Operation.ADD));
        assertThat(forwardingObjective.selector(), is(expectSelector));
        assertThat(forwardingObjective.nextId(), is(nextObjective.id()));
        assertThat(forwardingObjective.flag(), is(ForwardingObjective.Flag.SPECIFIC));

        compiler.deactivate();

    }


}
