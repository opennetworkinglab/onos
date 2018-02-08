/*
 * Copyright 2015-present Open Networking Foundation
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
import org.junit.Before;
import org.junit.Test;
import org.onlab.graph.ScalarWeight;
import org.onlab.packet.Ethernet;
import org.onlab.packet.MplsLabel;
import org.onlab.packet.VlanId;
import org.onosproject.TestApplicationId;
import org.onosproject.cfg.ComponentConfigAdapter;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.DefaultPath;
import org.onosproject.net.DeviceId;
import org.onosproject.net.EncapsulationType;
import org.onosproject.net.Link;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.flow.instructions.L2ModificationInstruction;
import org.onosproject.net.intent.AbstractIntentTest;
import org.onosproject.net.intent.FlowRuleIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentExtensionService;
import org.onosproject.net.intent.PathIntent;
import org.onosproject.net.intent.constraint.EncapsulationConstraint;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.net.resource.MockResourceService;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.easymock.EasyMock.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.junit.Assert.assertTrue;
import static org.onosproject.net.DefaultEdgeLink.createEdgeLink;
import static org.onosproject.net.Link.Type.DIRECT;
import static org.onosproject.net.Link.Type.INDIRECT;
import static org.onosproject.net.NetTestTools.*;

/**
 * Unit tests for PathIntentCompiler.
 */
public class PathIntentCompilerTest extends AbstractIntentTest {

    private CoreService coreService;
    private IntentExtensionService intentExtensionService;
    private IntentConfigurableRegistrator registrator;
    private PathIntentCompiler sut;

    private final TrafficSelector selector = DefaultTrafficSelector.builder().build();
    private final TrafficTreatment treatment = DefaultTrafficTreatment.builder().build();
    private final VlanId ingressVlan = VlanId.vlanId(((short) 101));
    private final TrafficSelector vlanSelector = DefaultTrafficSelector.builder()
            .matchVlanId(ingressVlan).build();
    private final VlanId egressVlan = VlanId.vlanId((short) 100);
    private final TrafficTreatment vlanTreatment = DefaultTrafficTreatment.builder()
            .setVlanId(egressVlan).build();

    private final ApplicationId appId = new TestApplicationId("test");
    private final ProviderId pid = new ProviderId("of", "test");

    // Edge scenario
    private final ConnectPoint d1p2 = connectPoint("s1", 2);
    private final ConnectPoint d1p3 = connectPoint("s1", 3);
    private final List<Link> edgeNet = Arrays.asList(
            createEdgeLink(d1p2, true),
            createEdgeLink(d1p3, false)
    );
    private final int edgeHops = edgeNet.size() - 1;
    private PathIntent edgeIntentNoVlan;
    private PathIntent edgeIntentIngressVlan;
    private PathIntent edgeIntentEgressVlan;
    private PathIntent edgeIntentVlan;

    // Single-hop scenario - indirect
    private final ConnectPoint d1p4 = connectPoint("s1", 4);
    private final ConnectPoint d2p2 = connectPoint("s2", 2);
    private final ConnectPoint d2p3 = connectPoint("s2", 3);
    private final ConnectPoint d3p2 = connectPoint("s3", 2);
    private final List<Link> singleHopIndirect = Arrays.asList(
            DefaultLink.builder().providerId(PID).src(d1p4).dst(d2p2).type(DIRECT).build(),
            DefaultLink.builder().providerId(PID).src(d2p3).dst(d3p2).type(INDIRECT).build()
    );
    private final int singleHopIndirectHops = singleHopIndirect.size() - 1;
    private PathIntent singleHopIndirectIntentNoVlan;
    private PathIntent singleHopIndirectIntentIngressVlan;
    private PathIntent singleHopIndirectIntentEgressVlan;
    private PathIntent singleHopIndirectIntentVlan;


    // Single-hop scenario- direct
    private final ConnectPoint d1p5 = connectPoint("s1", 5);
    private final ConnectPoint d2p4 = connectPoint("s2", 4);
    private final ConnectPoint d2p5 = connectPoint("s2", 5);
    private final ConnectPoint d3p3 = connectPoint("s3", 3);
    private final List<Link> singleHopDirect = Arrays.asList(
            DefaultLink.builder().providerId(PID).src(d1p5).dst(d2p4).type(DIRECT).build(),
            DefaultLink.builder().providerId(PID).src(d2p5).dst(d3p3).type(DIRECT).build()
    );
    private final int singleHopDirectHops = singleHopDirect.size() - 1;
    private PathIntent singleHopDirectIntentNoVlan;
    private PathIntent singleHopDirectIntentIngressVlan;
    private PathIntent singleHopDirectIntentEgressVlan;
    private PathIntent singleHopDirectIntentVlan;


    // Multi-hop scenario
    private final ConnectPoint d1p1 = connectPoint("s1", 0);
    private final ConnectPoint d2p0 = connectPoint("s2", 0);
    private final ConnectPoint d2p1 = connectPoint("s2", 1);
    private final ConnectPoint d3p1 = connectPoint("s3", 1);
    private final ConnectPoint d3p0 = connectPoint("s3", 10);
    private final ConnectPoint d1p0 = connectPoint("s1", 10);
    private static final int PRIORITY = 555;

    private final List<Link> links = Arrays.asList(
            createEdgeLink(d1p0, true),
            DefaultLink.builder().providerId(PID).src(d1p1).dst(d2p0).type(DIRECT).build(),
            DefaultLink.builder().providerId(PID).src(d2p1).dst(d3p1).type(DIRECT).build(),
            createEdgeLink(d3p0, false)
    );
    private final int hops = links.size() - 1;
    private PathIntent intent;
    private PathIntent constraintVlanIntent;
    private PathIntent constrainIngressEgressVlanIntent;
    private PathIntent constraintMplsIntent;

    /**
     * Configures mock objects used in all the test cases.
     * Creates the intents to test as well.
     */
    @Before
    public void setUp() {
        sut = new PathIntentCompiler();
        coreService = createMock(CoreService.class);
        expect(coreService.registerApplication("org.onosproject.net.intent"))
                .andReturn(appId);
        sut.coreService = coreService;
        sut.resourceService = new MockResourceService();

        super.setUp();

        intent = PathIntent.builder()
                .appId(APP_ID)
                .selector(selector)
                .treatment(treatment)
                .priority(PRIORITY)
                .path(new DefaultPath(pid, links, ScalarWeight.toWeight(hops)))
                .build();

        //Intent with VLAN encap without egress VLAN
        constraintVlanIntent = PathIntent.builder()
                .appId(APP_ID)
                .selector(selector)
                .treatment(treatment)
                .priority(PRIORITY)
                .constraints(ImmutableList.of(new EncapsulationConstraint(EncapsulationType.VLAN)))
                .path(new DefaultPath(pid, links, ScalarWeight.toWeight(hops)))
                .build();

        //Intent with VLAN encap with ingress and egress VLAN
        constrainIngressEgressVlanIntent = PathIntent.builder()
                .appId(APP_ID)
                .selector(vlanSelector)
                .treatment(vlanTreatment)
                .priority(PRIORITY)
                .constraints(ImmutableList.of(new EncapsulationConstraint(EncapsulationType.VLAN)))
                .path(new DefaultPath(pid, links, ScalarWeight.toWeight(hops)))
                .build();

        constraintMplsIntent = PathIntent.builder()
                .appId(APP_ID)
                .selector(selector)
                .treatment(treatment)
                .priority(PRIORITY)
                .constraints(ImmutableList.of(new EncapsulationConstraint(EncapsulationType.MPLS)))
                .path(new DefaultPath(pid, links, ScalarWeight.toWeight(hops)))
                .build();

        edgeIntentNoVlan = PathIntent.builder()
                .appId(APP_ID)
                .selector(selector)
                .treatment(treatment)
                .priority(PRIORITY)
                .constraints(ImmutableList.of(new EncapsulationConstraint(EncapsulationType.VLAN)))
                .path(new DefaultPath(pid, edgeNet, ScalarWeight.toWeight(edgeHops)))
                .build();

        edgeIntentIngressVlan = PathIntent.builder()
                .appId(APP_ID)
                .selector(vlanSelector)
                .treatment(treatment)
                .priority(PRIORITY)
                .constraints(ImmutableList.of(new EncapsulationConstraint(EncapsulationType.VLAN)))
                .path(new DefaultPath(pid, edgeNet, ScalarWeight.toWeight(edgeHops)))
                .build();

        edgeIntentEgressVlan = PathIntent.builder()
                .appId(APP_ID)
                .selector(selector)
                .treatment(vlanTreatment)
                .priority(PRIORITY)
                .constraints(ImmutableList.of(new EncapsulationConstraint(EncapsulationType.VLAN)))
                .path(new DefaultPath(pid, edgeNet, ScalarWeight.toWeight(edgeHops)))
                .build();

        edgeIntentVlan = PathIntent.builder()
                .appId(APP_ID)
                .selector(vlanSelector)
                .treatment(vlanTreatment)
                .priority(PRIORITY)
                .constraints(ImmutableList.of(new EncapsulationConstraint(EncapsulationType.VLAN)))
                .path(new DefaultPath(pid, edgeNet, ScalarWeight.toWeight(edgeHops)))
                .build();

        singleHopIndirectIntentNoVlan = PathIntent.builder()
                .appId(APP_ID)
                .selector(selector)
                .treatment(treatment)
                .priority(PRIORITY)
                .constraints(ImmutableList.of(new EncapsulationConstraint(EncapsulationType.VLAN)))
                .path(new DefaultPath(pid, singleHopIndirect, ScalarWeight.toWeight(singleHopIndirectHops)))
                .build();

        singleHopIndirectIntentIngressVlan = PathIntent.builder()
                .appId(APP_ID)
                .selector(vlanSelector)
                .treatment(treatment)
                .priority(PRIORITY)
                .constraints(ImmutableList.of(new EncapsulationConstraint(EncapsulationType.VLAN)))
                .path(new DefaultPath(pid, singleHopIndirect, ScalarWeight.toWeight(singleHopIndirectHops)))
                .build();

        singleHopIndirectIntentEgressVlan = PathIntent.builder()
                .appId(APP_ID)
                .selector(selector)
                .treatment(vlanTreatment)
                .priority(PRIORITY)
                .constraints(ImmutableList.of(new EncapsulationConstraint(EncapsulationType.VLAN)))
                .path(new DefaultPath(pid, singleHopIndirect, ScalarWeight.toWeight(singleHopIndirectHops)))
                .build();

        singleHopIndirectIntentVlan = PathIntent.builder()
                .appId(APP_ID)
                .selector(vlanSelector)
                .treatment(vlanTreatment)
                .priority(PRIORITY)
                .constraints(ImmutableList.of(new EncapsulationConstraint(EncapsulationType.VLAN)))
                .path(new DefaultPath(pid, singleHopIndirect, ScalarWeight.toWeight(singleHopIndirectHops)))
                .build();

        singleHopDirectIntentNoVlan = PathIntent.builder()
                .appId(APP_ID)
                .selector(selector)
                .treatment(treatment)
                .priority(PRIORITY)
                .constraints(ImmutableList.of(new EncapsulationConstraint(EncapsulationType.VLAN)))
                .path(new DefaultPath(pid, singleHopDirect, ScalarWeight.toWeight(singleHopDirectHops)))
                .build();

        singleHopDirectIntentIngressVlan = PathIntent.builder()
                .appId(APP_ID)
                .selector(vlanSelector)
                .treatment(treatment)
                .priority(PRIORITY)
                .constraints(ImmutableList.of(new EncapsulationConstraint(EncapsulationType.VLAN)))
                .path(new DefaultPath(pid, singleHopDirect, ScalarWeight.toWeight(singleHopDirectHops)))
                .build();

        singleHopDirectIntentEgressVlan = PathIntent.builder()
                .appId(APP_ID)
                .selector(selector)
                .treatment(vlanTreatment)
                .priority(PRIORITY)
                .constraints(ImmutableList.of(new EncapsulationConstraint(EncapsulationType.VLAN)))
                .path(new DefaultPath(pid, singleHopDirect, ScalarWeight.toWeight(singleHopDirectHops)))
                .build();

        singleHopDirectIntentVlan = PathIntent.builder()
                .appId(APP_ID)
                .selector(vlanSelector)
                .treatment(vlanTreatment)
                .priority(PRIORITY)
                .constraints(ImmutableList.of(new EncapsulationConstraint(EncapsulationType.VLAN)))
                .path(new DefaultPath(pid, singleHopDirect, ScalarWeight.toWeight(singleHopDirectHops)))
                .build();

        intentExtensionService = createMock(IntentExtensionService.class);
        intentExtensionService.registerCompiler(PathIntent.class, sut);
        intentExtensionService.unregisterCompiler(PathIntent.class);

        registrator = new IntentConfigurableRegistrator();
        registrator.extensionService = intentExtensionService;
        registrator.cfgService = new ComponentConfigAdapter();
        registrator.activate();

        sut.registrator = registrator;

        replay(coreService, intentExtensionService);
    }

    /**
     * Tests the compilation behavior of the path intent compiler in case of
     * VLAN {@link EncapsulationType} encapsulation constraint {@link EncapsulationConstraint}
     * and edge communication. No ingress VLAN. No egress VLAN.
     */
    @Test
    public void testVlanEncapCompileEdgeNoVlan() {
        sut.activate();

        List<Intent> compiled = sut.compile(edgeIntentNoVlan, Collections.emptyList());
        assertThat(compiled, hasSize(1));

        Collection<FlowRule> rules = ((FlowRuleIntent) compiled.get(0)).flowRules();
        assertThat(rules, hasSize(1));

        FlowRule rule = rules.stream()
                .filter(x -> x.deviceId().equals(d1p2.deviceId()))
                .findFirst()
                .get();
        verifyIdAndPriority(rule, d1p2.deviceId());

        assertThat(rule.selector(), is(DefaultTrafficSelector.builder().matchInPort(d1p2.port())
                                               .build()));
        assertThat(rule.treatment(),
                   is(DefaultTrafficTreatment.builder().setOutput(d1p3.port()).build()));

        sut.deactivate();
    }

    /**
     * Tests the compilation behavior of the path intent compiler in case of
     * VLAN {@link EncapsulationType} encapsulation constraint {@link EncapsulationConstraint}
     * and edge communication. Ingress VLAN. No egress VLAN.
     */
    @Test
    public void testVlanEncapCompileEdgeIngressVlan() {
        sut.activate();

        List<Intent> compiled = sut.compile(edgeIntentIngressVlan, Collections.emptyList());
        assertThat(compiled, hasSize(1));

        Collection<FlowRule> rules = ((FlowRuleIntent) compiled.get(0)).flowRules();
        assertThat(rules, hasSize(1));

        FlowRule rule = rules.stream()
                .filter(x -> x.deviceId().equals(d1p2.deviceId()))
                .findFirst()
                .get();
        verifyIdAndPriority(rule, d1p2.deviceId());

        assertThat(rule.selector(), is(DefaultTrafficSelector.builder().matchInPort(d1p2.port())
                                               .matchVlanId(ingressVlan).build()));
        assertThat(rule.treatment(),
                   is(DefaultTrafficTreatment.builder().setOutput(d1p3.port()).build()));

        sut.deactivate();
    }

    /**
     * Tests the compilation behavior of the path intent compiler in case of
     * VLAN {@link EncapsulationType} encapsulation constraint {@link EncapsulationConstraint}
     * and edge communication. No ingress VLAN. Egress VLAN.
     */
    @Test
    public void testVlanEncapCompileEdgeEgressVlan() {
        sut.activate();

        List<Intent> compiled = sut.compile(edgeIntentEgressVlan, Collections.emptyList());
        assertThat(compiled, hasSize(1));

        Collection<FlowRule> rules = ((FlowRuleIntent) compiled.get(0)).flowRules();
        assertThat(rules, hasSize(1));

        FlowRule rule = rules.stream()
                .filter(x -> x.deviceId().equals(d1p2.deviceId()))
                .findFirst()
                .get();
        verifyIdAndPriority(rule, d1p2.deviceId());

        assertThat(rule.selector(), is(DefaultTrafficSelector.builder().matchInPort(d1p2.port())
                                               .build()));
        assertThat(rule.treatment(), is(DefaultTrafficTreatment.builder().setVlanId(egressVlan)
                                                .setOutput(d1p3.port()).build()));

        Set<L2ModificationInstruction.ModVlanIdInstruction> vlanMod = rule.treatment().allInstructions().stream()
                .filter(treat -> treat instanceof L2ModificationInstruction.ModVlanIdInstruction)
                .map(x -> (L2ModificationInstruction.ModVlanIdInstruction) x)
                .collect(Collectors.toSet());
        assertThat(rule.treatment().allInstructions().stream()
                           .filter(treat -> treat instanceof L2ModificationInstruction.ModVlanIdInstruction)
                           .collect(Collectors.toSet()), hasSize(1));
        assertThat(vlanMod.iterator().next().vlanId(), is(egressVlan));
        assertThat(rule.treatment().allInstructions().stream()
                           .filter(treat -> treat instanceof L2ModificationInstruction.ModVlanHeaderInstruction)
                           .collect(Collectors.toSet()), hasSize(0));

        sut.deactivate();
    }

    /**
     * Tests the compilation behavior of the path intent compiler in case of
     * VLAN {@link EncapsulationType} encapsulation constraint {@link EncapsulationConstraint}
     * and edge communication. Ingress VLAN. Egress VLAN.
     */
    @Test
    public void testVlanEncapCompileEdgeVlan() {
        sut.activate();

        List<Intent> compiled = sut.compile(edgeIntentVlan, Collections.emptyList());
        assertThat(compiled, hasSize(1));

        Collection<FlowRule> rules = ((FlowRuleIntent) compiled.get(0)).flowRules();
        assertThat(rules, hasSize(1));

        FlowRule rule = rules.stream()
                .filter(x -> x.deviceId().equals(d1p2.deviceId()))
                .findFirst()
                .get();
        verifyIdAndPriority(rule, d1p2.deviceId());

        assertThat(rule.selector(), is(DefaultTrafficSelector.builder().matchInPort(d1p2.port())
                                               .matchVlanId(ingressVlan).build()));
        assertThat(rule.treatment(), is(DefaultTrafficTreatment.builder().setVlanId(egressVlan)
                                                .setOutput(d1p3.port()).build()));

        Set<L2ModificationInstruction.ModVlanIdInstruction> vlanMod = rule.treatment().allInstructions().stream()
                .filter(treat -> treat instanceof L2ModificationInstruction.ModVlanIdInstruction)
                .map(x -> (L2ModificationInstruction.ModVlanIdInstruction) x)
                .collect(Collectors.toSet());
        assertThat(rule.treatment().allInstructions().stream()
                           .filter(treat -> treat instanceof L2ModificationInstruction.ModVlanIdInstruction)
                           .collect(Collectors.toSet()), hasSize(1));
        assertThat(vlanMod.iterator().next().vlanId(), is(egressVlan));
        assertThat(rule.treatment().allInstructions().stream()
                           .filter(treat -> treat instanceof L2ModificationInstruction.ModVlanHeaderInstruction)
                           .collect(Collectors.toSet()), hasSize(0));

        sut.deactivate();
    }

    /**
     * Tests the compilation behavior of the path intent compiler in case of
     * VLAN {@link EncapsulationType} encapsulation constraint {@link EncapsulationConstraint}
     * and single-hop-indirect-link scenario. No ingress VLAN. No egress VLAN.
     */
    @Test
    public void testVlanEncapCompileSingleHopIndirectNoVlan() {
        sut.activate();

        List<Intent> compiled = sut.compile(singleHopIndirectIntentNoVlan, Collections.emptyList());
        assertThat(compiled, hasSize(1));

        Collection<FlowRule> rules = ((FlowRuleIntent) compiled.get(0)).flowRules();
        assertThat(rules, hasSize(1));


        FlowRule rule = rules.stream()
                .filter(x -> x.deviceId().equals(d2p2.deviceId()))
                .findFirst()
                .get();
        verifyIdAndPriority(rule, d2p2.deviceId());

        assertThat(rule.selector(), is(DefaultTrafficSelector.builder().matchInPort(d2p2.port())
                                               .build()));
        assertThat(rule.treatment(),
                   is(DefaultTrafficTreatment.builder().setOutput(d2p3.port()).build()));

        sut.deactivate();
    }

    /**
     * Tests the compilation behavior of the path intent compiler in case of
     * VLAN {@link EncapsulationType} encapsulation constraint {@link EncapsulationConstraint}
     * and single-hop-indirect-link scenario. Ingress VLAN. No egress VLAN.
     */
    @Test
    public void testVlanEncapCompileSingleHopIndirectIngressVlan() {
        sut.activate();

        List<Intent> compiled = sut.compile(singleHopIndirectIntentIngressVlan, Collections.emptyList());
        assertThat(compiled, hasSize(1));

        Collection<FlowRule> rules = ((FlowRuleIntent) compiled.get(0)).flowRules();
        assertThat(rules, hasSize(1));


        FlowRule rule = rules.stream()
                .filter(x -> x.deviceId().equals(d2p2.deviceId()))
                .findFirst()
                .get();
        verifyIdAndPriority(rule, d2p2.deviceId());

        assertThat(rule.selector(), is(DefaultTrafficSelector.builder().matchInPort(d2p2.port())
                                               .matchVlanId(ingressVlan).build()));
        assertThat(rule.treatment(),
                   is(DefaultTrafficTreatment.builder().setOutput(d2p3.port()).build()));

        sut.deactivate();
    }

    /**
     * Tests the compilation behavior of the path intent compiler in case of
     * VLAN {@link EncapsulationType} encapsulation constraint {@link EncapsulationConstraint}
     * and single-hop-indirect-link scenario. No ingress VLAN. Egress VLAN.
     */
    @Test
    public void testVlanEncapCompileSingleHopIndirectEgressVlan() {
        sut.activate();

        List<Intent> compiled = sut.compile(singleHopIndirectIntentEgressVlan, Collections.emptyList());
        assertThat(compiled, hasSize(1));

        Collection<FlowRule> rules = ((FlowRuleIntent) compiled.get(0)).flowRules();
        assertThat(rules, hasSize(1));


        FlowRule rule = rules.stream()
                .filter(x -> x.deviceId().equals(d2p2.deviceId()))
                .findFirst()
                .get();
        verifyIdAndPriority(rule, d2p2.deviceId());

        assertThat(rule.selector(), is(DefaultTrafficSelector.builder().matchInPort(d2p2.port())
                                               .build()));
        assertThat(rule.treatment(), is(DefaultTrafficTreatment.builder().setVlanId(egressVlan)
                              .setOutput(d2p3.port()).build()));

        Set<L2ModificationInstruction.ModVlanIdInstruction> vlanMod = rule.treatment().allInstructions().stream()
                .filter(treat -> treat instanceof L2ModificationInstruction.ModVlanIdInstruction)
                .map(x -> (L2ModificationInstruction.ModVlanIdInstruction) x)
                .collect(Collectors.toSet());
        assertThat(rule.treatment().allInstructions().stream()
                           .filter(treat -> treat instanceof L2ModificationInstruction.ModVlanIdInstruction)
                           .collect(Collectors.toSet()), hasSize(1));
        assertThat(vlanMod.iterator().next().vlanId(), is(egressVlan));
        assertThat(rule.treatment().allInstructions().stream()
                           .filter(treat -> treat instanceof L2ModificationInstruction.ModVlanHeaderInstruction)
                           .collect(Collectors.toSet()), hasSize(0));

        sut.deactivate();
    }

    /**
     * Tests the compilation behavior of the path intent compiler in case of
     * VLAN {@link EncapsulationType} encapsulation constraint {@link EncapsulationConstraint}
     * and single-hop-indirect-link scenario. Ingress VLAN. Egress VLAN.
     */
    @Test
    public void testVlanEncapCompileSingleHopIndirectVlan() {
        sut.activate();

        List<Intent> compiled = sut.compile(singleHopIndirectIntentVlan, Collections.emptyList());
        assertThat(compiled, hasSize(1));

        Collection<FlowRule> rules = ((FlowRuleIntent) compiled.get(0)).flowRules();
        assertThat(rules, hasSize(1));


        FlowRule rule = rules.stream()
                .filter(x -> x.deviceId().equals(d2p2.deviceId()))
                .findFirst()
                .get();
        verifyIdAndPriority(rule, d2p2.deviceId());

        assertThat(rule.selector(), is(DefaultTrafficSelector.builder().matchInPort(d2p2.port())
                                               .matchVlanId(ingressVlan).build()));
        assertThat(rule.treatment(), is(DefaultTrafficTreatment.builder().setVlanId(egressVlan)
                                                .setOutput(d2p3.port()).build()));

        Set<L2ModificationInstruction.ModVlanIdInstruction> vlanMod = rule.treatment().allInstructions().stream()
                .filter(treat -> treat instanceof L2ModificationInstruction.ModVlanIdInstruction)
                .map(x -> (L2ModificationInstruction.ModVlanIdInstruction) x)
                .collect(Collectors.toSet());
        assertThat(rule.treatment().allInstructions().stream()
                           .filter(treat -> treat instanceof L2ModificationInstruction.ModVlanIdInstruction)
                           .collect(Collectors.toSet()), hasSize(1));
        assertThat(vlanMod.iterator().next().vlanId(), is(egressVlan));
        assertThat(rule.treatment().allInstructions().stream()
                           .filter(treat -> treat instanceof L2ModificationInstruction.ModVlanHeaderInstruction)
                           .collect(Collectors.toSet()), hasSize(0));

        sut.deactivate();
    }

    /**
     * Tests the compilation behavior of the path intent compiler in case of
     * VLAN {@link EncapsulationType} encapsulation constraint {@link EncapsulationConstraint}
     * and single-hop-direct-link scenario. No ingress VLAN. No egress VLAN.
     */
    @Test
    public void testVlanEncapCompileSingleHopDirectNoVlan() {
        sut.activate();

        List<Intent> compiled = sut.compile(singleHopDirectIntentNoVlan, Collections.emptyList());
        assertThat(compiled, hasSize(1));

        Collection<FlowRule> rules = ((FlowRuleIntent) compiled.get(0)).flowRules();
        assertThat(rules, hasSize(1));


        FlowRule rule = rules.stream()
                .filter(x -> x.deviceId().equals(d2p4.deviceId()))
                .findFirst()
                .get();
        verifyIdAndPriority(rule, d2p4.deviceId());

        assertThat(rule.selector(), is(DefaultTrafficSelector.builder().matchInPort(d2p4.port())
                                               .build()));
        assertThat(rule.treatment(),
                   is(DefaultTrafficTreatment.builder().setOutput(d2p5.port()).build()));

        sut.deactivate();
    }

    /**
     * Tests the compilation behavior of the path intent compiler in case of
     * VLAN {@link EncapsulationType} encapsulation constraint {@link EncapsulationConstraint}
     * and single-hop-direct-link scenario. Ingress VLAN. No egress VLAN.
     */
    @Test
    public void testVlanEncapCompileSingleHopDirectIngressVlan() {
        sut.activate();

        List<Intent> compiled = sut.compile(singleHopDirectIntentIngressVlan, Collections.emptyList());
        assertThat(compiled, hasSize(1));

        Collection<FlowRule> rules = ((FlowRuleIntent) compiled.get(0)).flowRules();
        assertThat(rules, hasSize(1));


        FlowRule rule = rules.stream()
                .filter(x -> x.deviceId().equals(d2p4.deviceId()))
                .findFirst()
                .get();
        verifyIdAndPriority(rule, d2p4.deviceId());

        assertThat(rule.selector(), is(DefaultTrafficSelector.builder().matchInPort(d2p4.port())
                                               .matchVlanId(ingressVlan).build()));
        assertThat(rule.treatment(),
                   is(DefaultTrafficTreatment.builder().setOutput(d2p5.port()).build()));

        sut.deactivate();
    }

    /**
     * Tests the compilation behavior of the path intent compiler in case of
     * VLAN {@link EncapsulationType} encapsulation constraint {@link EncapsulationConstraint}
     * and single-hop-direct-link scenario. No ingress VLAN. Egress VLAN.
     */
    @Test
    public void testVlanEncapCompileSingleHopDirectEgressVlan() {
        sut.activate();

        List<Intent> compiled = sut.compile(singleHopDirectIntentEgressVlan, Collections.emptyList());
        assertThat(compiled, hasSize(1));

        Collection<FlowRule> rules = ((FlowRuleIntent) compiled.get(0)).flowRules();
        assertThat(rules, hasSize(1));


        FlowRule rule = rules.stream()
                .filter(x -> x.deviceId().equals(d2p4.deviceId()))
                .findFirst()
                .get();
        verifyIdAndPriority(rule, d2p4.deviceId());

        assertThat(rule.selector(), is(DefaultTrafficSelector.builder().matchInPort(d2p4.port())
                                               .build()));
        assertThat(rule.treatment(), is(DefaultTrafficTreatment.builder().setVlanId(egressVlan)
                                                .setOutput(d2p5.port()).build()));

        Set<L2ModificationInstruction.ModVlanIdInstruction> vlanMod = rule.treatment().allInstructions().stream()
                .filter(treat -> treat instanceof L2ModificationInstruction.ModVlanIdInstruction)
                .map(x -> (L2ModificationInstruction.ModVlanIdInstruction) x)
                .collect(Collectors.toSet());
        assertThat(rule.treatment().allInstructions().stream()
                           .filter(treat -> treat instanceof L2ModificationInstruction.ModVlanIdInstruction)
                           .collect(Collectors.toSet()), hasSize(1));
        assertThat(vlanMod.iterator().next().vlanId(), is(egressVlan));
        assertThat(rule.treatment().allInstructions().stream()
                           .filter(treat -> treat instanceof L2ModificationInstruction.ModVlanHeaderInstruction)
                           .collect(Collectors.toSet()), hasSize(0));

        sut.deactivate();
    }

    /**
     * Tests the compilation behavior of the path intent compiler in case of
     * VLAN {@link EncapsulationType} encapsulation constraint {@link EncapsulationConstraint}
     * and single-hop-direct-link scenario. Ingress VLAN. Egress VLAN.
     */
    @Test
    public void testVlanEncapCompileSingleHopDirectVlan() {
        sut.activate();

        List<Intent> compiled = sut.compile(singleHopDirectIntentVlan, Collections.emptyList());
        assertThat(compiled, hasSize(1));

        Collection<FlowRule> rules = ((FlowRuleIntent) compiled.get(0)).flowRules();
        assertThat(rules, hasSize(1));


        FlowRule rule = rules.stream()
                .filter(x -> x.deviceId().equals(d2p4.deviceId()))
                .findFirst()
                .get();
        verifyIdAndPriority(rule, d2p4.deviceId());

        assertThat(rule.selector(), is(DefaultTrafficSelector.builder().matchInPort(d2p4.port())
                                               .matchVlanId(ingressVlan).build()));
        assertThat(rule.treatment(), is(DefaultTrafficTreatment.builder().setVlanId(egressVlan)
                                                .setOutput(d2p5.port()).build()));

        Set<L2ModificationInstruction.ModVlanIdInstruction> vlanMod = rule.treatment().allInstructions().stream()
                .filter(treat -> treat instanceof L2ModificationInstruction.ModVlanIdInstruction)
                .map(x -> (L2ModificationInstruction.ModVlanIdInstruction) x)
                .collect(Collectors.toSet());
        assertThat(rule.treatment().allInstructions().stream()
                           .filter(treat -> treat instanceof L2ModificationInstruction.ModVlanIdInstruction)
                           .collect(Collectors.toSet()), hasSize(1));
        assertThat(vlanMod.iterator().next().vlanId(), is(egressVlan));
        assertThat(rule.treatment().allInstructions().stream()
                           .filter(treat -> treat instanceof L2ModificationInstruction.ModVlanHeaderInstruction)
                           .collect(Collectors.toSet()), hasSize(0));

        sut.deactivate();
    }

    /**
     * Tests the compilation behavior of the path intent compiler.
     */
    @Test
    public void testCompile() {
        sut.activate();

        List<Intent> compiled = sut.compile(intent, Collections.emptyList());
        assertThat(compiled, hasSize(1));

        assertThat("key is inherited",
                   compiled.stream().map(Intent::key).collect(Collectors.toList()),
                   everyItem(is(intent.key())));

        Collection<FlowRule> rules = ((FlowRuleIntent) compiled.get(0)).flowRules();

        FlowRule rule1 = rules.stream()
                .filter(x -> x.deviceId().equals(d1p0.deviceId()))
                .findFirst()
                .get();
        verifyIdAndPriority(rule1, d1p0.deviceId());
        assertThat(rule1.selector(),
                is(DefaultTrafficSelector.builder(selector).matchInPort(d1p0.port()).build()));
        assertThat(rule1.treatment(),
                is(DefaultTrafficTreatment.builder().setOutput(d1p1.port()).build()));


        FlowRule rule2 = rules.stream()
                .filter(x -> x.deviceId().equals(d2p0.deviceId()))
                .findFirst()
                .get();
        verifyIdAndPriority(rule2, d2p0.deviceId());
        assertThat(rule2.selector(),
                is(DefaultTrafficSelector.builder(selector).matchInPort(d2p0.port()).build()));
        assertThat(rule2.treatment(),
                is(DefaultTrafficTreatment.builder().setOutput(d2p1.port()).build()));

        FlowRule rule3 = rules.stream()
                .filter(x -> x.deviceId().equals(d3p0.deviceId()))
                .findFirst()
                .get();
        verifyIdAndPriority(rule3, d3p1.deviceId());
        assertThat(rule3.selector(),
                is(DefaultTrafficSelector.builder(selector).matchInPort(d3p1.port()).build()));
        assertThat(rule3.treatment(),
                is(DefaultTrafficTreatment.builder(treatment).setOutput(d3p0.port()).build()));

        sut.deactivate();
    }

    /**
     * Tests the compilation behavior of the path intent compiler in case of
     * VLAN {@link EncapsulationType} encapsulation constraint {@link EncapsulationConstraint}.
     */
    @Test
    public void testVlanEncapCompile() {
        sut.activate();

        List<Intent> compiled = sut.compile(constraintVlanIntent, Collections.emptyList());
        assertThat(compiled, hasSize(1));

        Collection<FlowRule> rules = ((FlowRuleIntent) compiled.get(0)).flowRules();
        assertThat(rules, hasSize(3));

        FlowRule rule1 = rules.stream()
                .filter(x -> x.deviceId().equals(d1p0.deviceId()))
                .findFirst()
                .get();
        verifyIdAndPriority(rule1, d1p0.deviceId());
        assertThat(rule1.selector(), is(DefaultTrafficSelector.builder(selector)
                                        .matchInPort(d1p0.port()).build()));
        VlanId vlanToEncap = verifyVlanEncapTreatment(rule1.treatment(), d1p1, true, false);

        FlowRule rule2 = rules.stream()
                .filter(x -> x.deviceId().equals(d2p0.deviceId()))
                .findFirst()
                .get();
        verifyIdAndPriority(rule2, d2p0.deviceId());
        verifyVlanEncapSelector(rule2.selector(), d2p0, vlanToEncap);
        vlanToEncap = verifyVlanEncapTreatment(rule2.treatment(), d2p1, false, false);

        FlowRule rule3 = rules.stream()
                .filter(x -> x.deviceId().equals(d3p0.deviceId()))
                .findFirst()
                .get();
        verifyIdAndPriority(rule3, d3p1.deviceId());
        verifyVlanEncapSelector(rule3.selector(), d3p1, vlanToEncap);
        verifyVlanEncapTreatment(rule3.treatment(), d3p0, false, true);

        sut.deactivate();
    }

    /**
     * Tests the compilation behavior of the path intent compiler in case of
     * VLAN {@link EncapsulationType} encapsulation constraint {@link EncapsulationConstraint}.
     * This test includes a selector to match a VLAN at the ingress and a treatment to set VLAN at the egress.
     */
    @Test
    public void testEncapIngressEgressVlansCompile() {
        sut.activate();

        List<Intent> compiled = sut.compile(constrainIngressEgressVlanIntent,
                Collections.emptyList());
        assertThat(compiled, hasSize(1));

        Collection<FlowRule> rules = ((FlowRuleIntent) compiled.get(0)).flowRules();
        assertThat(rules, hasSize(3));

        FlowRule rule1 = rules.stream()
                .filter(x -> x.deviceId().equals(d1p0.deviceId()))
                .findFirst()
                .get();
        verifyIdAndPriority(rule1, d1p0.deviceId());
        verifyVlanEncapSelector(rule1.selector(), d1p0, ingressVlan);
        VlanId vlanToEncap = verifyVlanEncapTreatment(rule1.treatment(), d1p1, true, false);

        FlowRule rule2 = rules.stream()
                .filter(x -> x.deviceId().equals(d2p0.deviceId()))
                .findFirst()
                .get();
        verifyIdAndPriority(rule2, d2p0.deviceId());
        verifyVlanEncapSelector(rule2.selector(), d2p0, vlanToEncap);
        vlanToEncap = verifyVlanEncapTreatment(rule2.treatment(), d2p1, false, false);

        FlowRule rule3 = rules.stream()
                .filter(x -> x.deviceId().equals(d3p0.deviceId()))
                .findFirst()
                .get();
        verifyIdAndPriority(rule3, d3p1.deviceId());
        verifyVlanEncapSelector(rule3.selector(), d3p1, vlanToEncap);
        Set<L2ModificationInstruction.ModVlanIdInstruction> vlanMod = rule3.treatment().allInstructions().stream()
                .filter(treat -> treat instanceof L2ModificationInstruction.ModVlanIdInstruction)
                .map(x -> (L2ModificationInstruction.ModVlanIdInstruction) x)
                .collect(Collectors.toSet());
        assertThat(rule3.treatment().allInstructions().stream()
                .filter(treat -> treat instanceof L2ModificationInstruction.ModVlanIdInstruction)
                .collect(Collectors.toSet()), hasSize(1));
        assertThat(vlanMod.iterator().next().vlanId(), is(egressVlan));
        assertThat(rule3.treatment().allInstructions().stream()
                .filter(treat -> treat instanceof L2ModificationInstruction.ModVlanHeaderInstruction)
                .collect(Collectors.toSet()), hasSize(0));

        sut.deactivate();
    }

    /**
     * Tests the random selection of VLAN Ids in the PathCompiler.
     * It can fail randomly (it is unlikely)
     */
    @Test
    public void testRandomVlanSelection() {

            sut.activate();

            List<Intent> compiled = sut.compile(constraintVlanIntent, Collections.emptyList());
            assertThat(compiled, hasSize(1));

            Collection<FlowRule> rules = ((FlowRuleIntent) compiled.get(0)).flowRules();
            assertThat(rules, hasSize(3));

            FlowRule rule1 = rules.stream()
                    .filter(x -> x.deviceId().equals(d1p0.deviceId()))
                    .findFirst()
                    .get();
            verifyIdAndPriority(rule1, d1p0.deviceId());
            assertThat(rule1.selector(), is(DefaultTrafficSelector.builder(selector)
                    .matchInPort(d1p0.port()).build()));

            VlanId vlanToEncap = verifyVlanEncapTreatment(rule1.treatment(), d1p1, true, false);

            assertTrue(VlanId.NO_VID < vlanToEncap.toShort() && vlanToEncap.toShort() < VlanId.MAX_VLAN);

            /*
             * This second part is meant to test if the random selection is working properly.
             * We are compiling the same intent in order to verify if the VLAN ID is different
             * from the previous one.
             */

            List<Intent> compiled2 = sut.compile(constraintVlanIntent, Collections.emptyList());
            assertThat(compiled2, hasSize(1));

            Collection<FlowRule> rules2 = ((FlowRuleIntent) compiled2.get(0)).flowRules();
            assertThat(rules2, hasSize(3));

            FlowRule rule2 = rules2.stream()
                    .filter(x -> x.deviceId().equals(d1p0.deviceId()))
                    .findFirst()
                    .get();
            verifyIdAndPriority(rule2, d1p0.deviceId());
            assertThat(rule2.selector(), is(DefaultTrafficSelector.builder(selector)
                    .matchInPort(d1p0.port()).build()));

            VlanId vlanToEncap2 = verifyVlanEncapTreatment(rule2.treatment(), d1p1, true, false);

            assertTrue(VlanId.NO_VID < vlanToEncap2.toShort() && vlanToEncap2.toShort() < VlanId.MAX_VLAN);

            sut.deactivate();


    }

    private VlanId verifyVlanEncapTreatment(TrafficTreatment trafficTreatment,
                                        ConnectPoint egress, boolean isIngress, boolean isEgress) {
        Set<Instructions.OutputInstruction> ruleOutput = trafficTreatment.allInstructions().stream()
                .filter(treat -> treat instanceof Instructions.OutputInstruction)
                .map(treat -> (Instructions.OutputInstruction) treat)
                .collect(Collectors.toSet());
        assertThat(ruleOutput, hasSize(1));
        assertThat((ruleOutput.iterator().next()).port(), is(egress.port()));
        VlanId vlanToEncap = VlanId.NONE;
        if (isIngress && !isEgress) {
            Set<L2ModificationInstruction.ModVlanIdInstruction> vlanRules = trafficTreatment.allInstructions().stream()
                    .filter(treat -> treat instanceof L2ModificationInstruction.ModVlanIdInstruction)
                    .map(x -> (L2ModificationInstruction.ModVlanIdInstruction) x)
                    .collect(Collectors.toSet());
            assertThat(vlanRules, hasSize(1));
            L2ModificationInstruction.ModVlanIdInstruction vlanRule = vlanRules.iterator().next();
            assertThat(vlanRule.vlanId().toShort(), greaterThan(VlanId.NO_VID));
            assertThat(vlanRule.vlanId().toShort(), lessThan(VlanId.MAX_VLAN));
            vlanToEncap = vlanRule.vlanId();
        } else if (!isIngress && !isEgress) {

            Set<L2ModificationInstruction.ModVlanIdInstruction> vlanRules = trafficTreatment.allInstructions().stream()
                    .filter(treat -> treat instanceof L2ModificationInstruction.ModVlanIdInstruction)
                    .map(x -> (L2ModificationInstruction.ModVlanIdInstruction) x)
                    .collect(Collectors.toSet());
            assertThat(vlanRules, hasSize(1));
            L2ModificationInstruction.ModVlanIdInstruction vlanRule = vlanRules.iterator().next();
            assertThat(vlanRule.vlanId().toShort(), greaterThan(VlanId.NO_VID));
            assertThat(vlanRule.vlanId().toShort(), lessThan(VlanId.MAX_VLAN));
            vlanToEncap = vlanRule.vlanId();

        } else {
            assertThat(trafficTreatment.allInstructions().stream()
                               .filter(treat -> treat instanceof L2ModificationInstruction.ModVlanIdInstruction)
                               .collect(Collectors.toSet()), hasSize(0));
            assertThat(trafficTreatment.allInstructions().stream()
                               .filter(treat -> treat instanceof L2ModificationInstruction.ModVlanHeaderInstruction)
                               .collect(Collectors.toSet()), hasSize(1));

        }

        return vlanToEncap;

    }

    private void verifyVlanEncapSelector(TrafficSelector trafficSelector, ConnectPoint ingress, VlanId vlanToMatch) {

        assertThat(trafficSelector, is(DefaultTrafficSelector.builder().matchInPort(ingress.port())
                   .matchVlanId(vlanToMatch).build()));
    }

    /**
     * Tests the compilation behavior of the path intent compiler in case of
     * encasulation costraint {@link EncapsulationConstraint}.
     */
    @Test
    public void testMplsEncapCompile() {
        sut.activate();

        List<Intent> compiled = sut.compile(constraintMplsIntent, Collections.emptyList());
        assertThat(compiled, hasSize(1));

        Collection<FlowRule> rules = ((FlowRuleIntent) compiled.get(0)).flowRules();
        assertThat(rules, hasSize(3));

        FlowRule rule1 = rules.stream()
                .filter(x -> x.deviceId().equals(d1p0.deviceId()))
                .findFirst()
                .get();
        verifyIdAndPriority(rule1, d1p0.deviceId());
        assertThat(rule1.selector(), is(DefaultTrafficSelector
                                                .builder(selector)
                                                .matchInPort(d1p0.port())
                                                .build()));
        MplsLabel mplsLabelToEncap = verifyMplsEncapTreatment(rule1.treatment(), d1p1, true, false);

        FlowRule rule2 = rules.stream()
                .filter(x -> x.deviceId().equals(d2p0.deviceId()))
                .findFirst()
                .get();
        verifyIdAndPriority(rule2, d2p0.deviceId());
        verifyMplsEncapSelector(rule2.selector(), d2p0, mplsLabelToEncap);
        mplsLabelToEncap = verifyMplsEncapTreatment(rule2.treatment(), d2p1, false, false);

        FlowRule rule3 = rules.stream()
                .filter(x -> x.deviceId().equals(d3p0.deviceId()))
                .findFirst()
                .get();
        verifyIdAndPriority(rule3, d3p1.deviceId());
        verifyMplsEncapSelector(rule3.selector(), d3p1, mplsLabelToEncap);
        verifyMplsEncapTreatment(rule3.treatment(), d3p0, false, true);

        sut.deactivate();
    }


    private MplsLabel verifyMplsEncapTreatment(TrafficTreatment trafficTreatment,
                                               ConnectPoint egress, boolean isIngress, boolean isEgress) {
        Set<Instructions.OutputInstruction> ruleOutput = trafficTreatment.allInstructions().stream()
                .filter(treat -> treat instanceof Instructions.OutputInstruction)
                .map(treat -> (Instructions.OutputInstruction) treat)
                .collect(Collectors.toSet());
        assertThat(ruleOutput, hasSize(1));
        assertThat((ruleOutput.iterator().next()).port(), is(egress.port()));
        MplsLabel mplsToEncap = MplsLabel.mplsLabel(0);
        if (isIngress && !isEgress) {
            Set<L2ModificationInstruction.ModMplsLabelInstruction> mplsRules = trafficTreatment
                    .allInstructions()
                    .stream()
                    .filter(treat -> treat instanceof L2ModificationInstruction.ModMplsLabelInstruction)
                    .map(x -> (L2ModificationInstruction.ModMplsLabelInstruction) x)
                    .collect(Collectors.toSet());
            assertThat(mplsRules, hasSize(1));
            L2ModificationInstruction.ModMplsLabelInstruction mplsRule = mplsRules.iterator().next();
            assertThat(mplsRule.label().toInt(), greaterThan(0));
            assertThat(mplsRule.label().toInt(), lessThan(MplsLabel.MAX_MPLS));
            mplsToEncap = mplsRule.label();
        } else if (!isIngress && !isEgress) {
            Set<L2ModificationInstruction.ModMplsLabelInstruction> mplsRules = trafficTreatment
                    .allInstructions()
                    .stream()
                    .filter(treat -> treat instanceof L2ModificationInstruction.ModMplsLabelInstruction)
                    .map(x -> (L2ModificationInstruction.ModMplsLabelInstruction) x)
                    .collect(Collectors.toSet());
            assertThat(mplsRules, hasSize(1));
            L2ModificationInstruction.ModMplsLabelInstruction mplsRule = mplsRules.iterator().next();
            assertThat(mplsRule.label().toInt(), greaterThan(0));
            assertThat(mplsRule.label().toInt(), lessThan(MplsLabel.MAX_MPLS));
            mplsToEncap = mplsRule.label();
        } else {
            assertThat(trafficTreatment.allInstructions().stream()
                               .filter(treat -> treat instanceof L2ModificationInstruction.ModMplsLabelInstruction)
                               .collect(Collectors.toSet()), hasSize(0));
            assertThat(trafficTreatment.allInstructions().stream()
                               .filter(treat -> treat instanceof L2ModificationInstruction.ModMplsHeaderInstruction)
                               .collect(Collectors.toSet()), hasSize(1));

        }
        return mplsToEncap;

    }

    private void verifyMplsEncapSelector(TrafficSelector trafficSelector, ConnectPoint ingress, MplsLabel mplsLabel) {

        assertThat(trafficSelector, is(DefaultTrafficSelector.builder()
                                               .matchInPort(ingress.port()).matchEthType(Ethernet.MPLS_UNICAST)
                                               .matchMplsLabel(mplsLabel).build()));
    }

    private void verifyIdAndPriority(FlowRule rule, DeviceId deviceId) {
        assertThat(rule.deviceId(), is(deviceId));
        assertThat(rule.priority(), is(PRIORITY));
    }
}
