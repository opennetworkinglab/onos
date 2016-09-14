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

import com.google.common.collect.ImmutableSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.TestApplicationId;
import org.onosproject.cfg.ComponentConfigAdapter;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.IdGenerator;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.DeviceId;
import org.onosproject.net.FilteredConnectPoint;
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.intent.FlowRuleIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentExtensionService;
import org.onosproject.net.intent.LinkCollectionIntent;
import org.onosproject.net.intent.MockIdGenerator;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.onosproject.net.Link.Type.DIRECT;
import static org.onosproject.net.NetTestTools.*;

public class LinkCollectionIntentCompilerTest {

    private final ApplicationId appId = new TestApplicationId("test");

    private final ConnectPoint d1p1 = connectPoint("s1", 1);
    private final ConnectPoint d2p0 = connectPoint("s2", 0);
    private final ConnectPoint d2p1 = connectPoint("s2", 1);
    private final ConnectPoint d3p1 = connectPoint("s3", 1);
    private final ConnectPoint d3p0 = connectPoint("s3", 10);
    private final ConnectPoint d1p0 = connectPoint("s1", 10);

    private final DeviceId of1Id = DeviceId.deviceId("of:of1");
    private final DeviceId of2Id = DeviceId.deviceId("of:of2");
    private final DeviceId of3Id = DeviceId.deviceId("of:of3");
    private final DeviceId of4Id = DeviceId.deviceId("of:of4");

    private final ConnectPoint of1p1 = connectPoint("of1", 1);
    private final ConnectPoint of1p2 = connectPoint("of1", 2);
    private final ConnectPoint of2p1 = connectPoint("of2", 1);
    private final ConnectPoint of2p2 = connectPoint("of2", 2);
    private final ConnectPoint of2p3 = connectPoint("of2", 3);
    private final ConnectPoint of3p1 = connectPoint("of3", 1);
    private final ConnectPoint of3p2 = connectPoint("of3", 2);
    private final ConnectPoint of4p1 = connectPoint("of4", 1);
    private final ConnectPoint of4p2 = connectPoint("of4", 2);

    private final Set<Link> links = ImmutableSet.of(
            DefaultLink.builder().providerId(PID).src(d1p1).dst(d2p0).type(DIRECT).build(),
            DefaultLink.builder().providerId(PID).src(d2p1).dst(d3p1).type(DIRECT).build(),
            DefaultLink.builder().providerId(PID).src(d1p1).dst(d3p1).type(DIRECT).build());

    private final TrafficSelector selector = DefaultTrafficSelector.builder().build();
    private final TrafficTreatment treatment = DefaultTrafficTreatment.builder().build();

    private final TrafficSelector vlan100Selector = DefaultTrafficSelector.builder()
            .matchVlanId(VlanId.vlanId("100"))
            .build();

    private final TrafficSelector vlan200Selector = DefaultTrafficSelector.builder()
            .matchVlanId(VlanId.vlanId("200"))
            .build();

    private final TrafficSelector ipPrefixSelector = DefaultTrafficSelector.builder()
            .matchIPDst(IpPrefix.valueOf("192.168.100.0/24"))
            .build();

    private final TrafficTreatment ethDstTreatment = DefaultTrafficTreatment.builder()
            .setEthDst(MacAddress.valueOf("C0:FF:EE:C0:FF:EE"))
            .build();

    private CoreService coreService;
    private IntentExtensionService intentExtensionService;
    private IntentConfigurableRegistrator registrator;
    private IdGenerator idGenerator = new MockIdGenerator();

    private LinkCollectionIntent intent;

    private LinkCollectionIntentCompiler sut;



    private List<FlowRule> getFlowRulesByDevice(DeviceId deviceId, Collection<FlowRule> flowRules) {
        return flowRules.stream()
                .filter(fr -> fr.deviceId().equals(deviceId))
                .collect(Collectors.toList());
    }

    @Before
    public void setUp() {
        sut = new LinkCollectionIntentCompiler();
        coreService = createMock(CoreService.class);
        expect(coreService.registerApplication("org.onosproject.net.intent"))
                .andReturn(appId);
        sut.coreService = coreService;

        Intent.bindIdGenerator(idGenerator);

        intent = LinkCollectionIntent.builder()
                .appId(APP_ID)
                .selector(selector)
                .treatment(treatment)
                .links(links)
                .ingressPoints(ImmutableSet.of(d1p1))
                .egressPoints(ImmutableSet.of(d3p1))
                .build();

        intentExtensionService = createMock(IntentExtensionService.class);
        intentExtensionService.registerCompiler(LinkCollectionIntent.class, sut);
        intentExtensionService.unregisterCompiler(LinkCollectionIntent.class);

        registrator = new IntentConfigurableRegistrator();
        registrator.extensionService = intentExtensionService;
        registrator.cfgService = new ComponentConfigAdapter();
        registrator.activate();

        sut.registrator = registrator;

        replay(coreService, intentExtensionService);


    }

    @After
    public void tearDown() {

        Intent.unbindIdGenerator(idGenerator);
    }

    @Test
    public void testCompile() {
        sut.activate();

        List<Intent> compiled = sut.compile(intent, Collections.emptyList());
        assertThat(compiled, hasSize(1));

        Collection<FlowRule> rules = ((FlowRuleIntent) compiled.get(0)).flowRules();
        assertThat(rules, hasSize(links.size()));

        // if not found, get() raises an exception
        FlowRule rule1 = rules.stream()
                .filter(rule -> rule.deviceId().equals(d1p0.deviceId()))
                .findFirst()
                .get();
        assertThat(rule1.selector(), is(
                DefaultTrafficSelector.builder(intent.selector()).matchInPort(d1p1.port()).build()
        ));
        assertThat(rule1.treatment(), is(
                DefaultTrafficTreatment.builder(intent.treatment()).setOutput(d1p1.port()).build()
        ));
        assertThat(rule1.priority(), is(intent.priority()));

        FlowRule rule2 = rules.stream()
                .filter(rule -> rule.deviceId().equals(d2p0.deviceId()))
                .findFirst()
                .get();
        assertThat(rule2.selector(), is(
                DefaultTrafficSelector.builder(intent.selector()).matchInPort(d2p0.port()).build()
        ));
        assertThat(rule2.treatment(), is(
                DefaultTrafficTreatment.builder().setOutput(d2p1.port()).build()
        ));
        assertThat(rule2.priority(), is(intent.priority()));

        FlowRule rule3 = rules.stream()
                .filter(rule -> rule.deviceId().equals(d3p0.deviceId()))
                .findFirst()
                .get();
        assertThat(rule3.selector(), is(
                DefaultTrafficSelector.builder(intent.selector()).matchInPort(d3p1.port()).build()
        ));
        assertThat(rule3.treatment(), is(
                DefaultTrafficTreatment.builder().setOutput(d3p1.port()).build()
        ));
        assertThat(rule3.priority(), is(intent.priority()));

        sut.deactivate();
    }

    /**
     * Single point to multi point case.
     * -1 of1 2-1 of2 2--1 of3 2-
     *             3
     *             `-1 of4 2-
     */
    @Test
    public void testFilteredConnectPoint1() {
        sut.activate();
        Set<Link> testLinks = ImmutableSet.of(
                DefaultLink.builder().providerId(PID).src(of1p2).dst(of2p1).type(DIRECT).build(),
                DefaultLink.builder().providerId(PID).src(of2p2).dst(of3p1).type(DIRECT).build(),
                DefaultLink.builder().providerId(PID).src(of2p3).dst(of4p1).type(DIRECT).build()
        );

        TrafficSelector expectOf1Selector = DefaultTrafficSelector.builder(vlan100Selector)
                .matchInPort(PortNumber.portNumber(1))
                .build();

        TrafficTreatment expectOf1Treatment = DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.portNumber(2))
                .build();

        TrafficSelector expectOf2Selector = DefaultTrafficSelector.builder(vlan100Selector)
                .matchInPort(PortNumber.portNumber(1))
                .build();

        TrafficTreatment expectOf2Treatment = DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.portNumber(2))
                .setOutput(PortNumber.portNumber(3))
                .build();

        TrafficSelector expectOf3Selector = DefaultTrafficSelector.builder(vlan100Selector)
                .matchInPort(PortNumber.portNumber(1))
                .build();

        TrafficTreatment expectOf3Treatment = DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.portNumber(2))
                .build();

        TrafficSelector expectOf4Selector = DefaultTrafficSelector.builder(vlan100Selector)
                .matchInPort(PortNumber.portNumber(1))
                .build();

        TrafficTreatment expectOf4Treatment = DefaultTrafficTreatment.builder()
                .setVlanId(VlanId.vlanId("200"))
                .setOutput(PortNumber.portNumber(2))
                .build();



        Set<FilteredConnectPoint> ingress = ImmutableSet.of(
                new FilteredConnectPoint(of1p1, vlan100Selector)
        );

        Set<FilteredConnectPoint> egress = ImmutableSet.of(
                new FilteredConnectPoint(of3p2, vlan100Selector),
                new FilteredConnectPoint(of4p2, vlan200Selector)
        );

        intent = LinkCollectionIntent.builder()
                .appId(APP_ID)
                .filteredIngressPoints(ingress)
                .filteredEgressPoints(egress)
                .treatment(treatment)
                .applyTreatmentOnEgress(true)
                .links(testLinks)
                .build();

        assertThat(sut, is(notNullValue()));

        List<Intent> result = sut.compile(intent, Collections.emptyList());

        assertThat(result, is(notNullValue()));
        assertThat(result, hasSize(1));

        Intent resultIntent = result.get(0);
        assertThat(resultIntent, instanceOf(FlowRuleIntent.class));

        if (resultIntent instanceof FlowRuleIntent) {
            FlowRuleIntent frIntent = (FlowRuleIntent) resultIntent;

            assertThat(frIntent.flowRules(), hasSize(4));

            List<FlowRule> deviceFlowRules;
            FlowRule flowRule;

            // Of1
            deviceFlowRules = getFlowRulesByDevice(of1Id, frIntent.flowRules());
            assertThat(deviceFlowRules, hasSize(1));
            flowRule = deviceFlowRules.get(0);
            assertThat(flowRule.selector(), is(expectOf1Selector));
            assertThat(flowRule.treatment(), is(expectOf1Treatment));

            // Of2
            deviceFlowRules = getFlowRulesByDevice(of2Id, frIntent.flowRules());
            assertThat(deviceFlowRules, hasSize(1));
            flowRule = deviceFlowRules.get(0);
            assertThat(flowRule.selector(), is(expectOf2Selector));
            assertThat(flowRule.treatment(), is(expectOf2Treatment));

            // Of3
            deviceFlowRules = getFlowRulesByDevice(of3Id, frIntent.flowRules());
            assertThat(deviceFlowRules, hasSize(1));
            flowRule = deviceFlowRules.get(0);
            assertThat(flowRule.selector(), is(expectOf3Selector));
            assertThat(flowRule.treatment(), is(expectOf3Treatment));

            // Of4
            deviceFlowRules = getFlowRulesByDevice(of4Id, frIntent.flowRules());
            assertThat(deviceFlowRules, hasSize(1));
            flowRule = deviceFlowRules.get(0);
            assertThat(flowRule.selector(), is(expectOf4Selector));
            assertThat(flowRule.treatment(), is(expectOf4Treatment));

        }
        sut.deactivate();
    }

    /**
     * Multi point to single point intent with filtered connect point.
     *
     * -1 of1 2-1 of2 2-1 of4 2-
     *             3
     * -1 of3 2---/
     */
    @Test
    public void testFilteredConnectPoint2() {
        sut.activate();
        Set<Link> testlinks = ImmutableSet.of(
                DefaultLink.builder().providerId(PID).src(of1p2).dst(of2p1).type(DIRECT).build(),
                DefaultLink.builder().providerId(PID).src(of3p2).dst(of2p3).type(DIRECT).build(),
                DefaultLink.builder().providerId(PID).src(of2p2).dst(of4p1).type(DIRECT).build()
        );

        Set<FilteredConnectPoint> ingress = ImmutableSet.of(
                new FilteredConnectPoint(of1p1, vlan100Selector),
                new FilteredConnectPoint(of3p1, vlan100Selector)
        );

        Set<FilteredConnectPoint> egress = ImmutableSet.of(
                new FilteredConnectPoint(of4p2, vlan200Selector)
        );

        TrafficSelector expectOf1Selector = DefaultTrafficSelector.builder(vlan100Selector)
                .matchInPort(PortNumber.portNumber(1))
                .build();

        TrafficTreatment expectOf1Treatment = DefaultTrafficTreatment.builder()
                .setVlanId(VlanId.vlanId("200"))
                .setOutput(PortNumber.portNumber(2))
                .build();

        TrafficSelector expectOf2Selector1 = DefaultTrafficSelector.builder(vlan200Selector)
                .matchInPort(PortNumber.portNumber(1))
                .build();

        TrafficSelector expectOf2Selector2 = DefaultTrafficSelector.builder(vlan200Selector)
                .matchInPort(PortNumber.portNumber(3))
                .build();

        TrafficTreatment expectOf2Treatment = DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.portNumber(2))
                .build();

        TrafficSelector expectOf3Selector = DefaultTrafficSelector.builder(vlan100Selector)
                .matchInPort(PortNumber.portNumber(1))
                .build();

        TrafficTreatment expectOf3Treatment = DefaultTrafficTreatment.builder()
                .setVlanId(VlanId.vlanId("200"))
                .setOutput(PortNumber.portNumber(2))
                .build();

        TrafficSelector expectOf4Selector = DefaultTrafficSelector.builder(vlan100Selector)
                .matchInPort(PortNumber.portNumber(1))
                .matchVlanId(VlanId.vlanId("200"))
                .build();

        TrafficTreatment expectOf4Treatment = DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.portNumber(2))
                .build();

        intent = LinkCollectionIntent.builder()
                .appId(APP_ID)
                .filteredIngressPoints(ingress)
                .filteredEgressPoints(egress)
                .treatment(treatment)
                .links(testlinks)
                .build();

        List<Intent> result = sut.compile(intent, Collections.emptyList());

        assertThat(result, is(notNullValue()));
        assertThat(result, hasSize(1));

        Intent resultIntent = result.get(0);
        assertThat(resultIntent, instanceOf(FlowRuleIntent.class));

        if (resultIntent instanceof FlowRuleIntent) {
            FlowRuleIntent frIntent = (FlowRuleIntent) resultIntent;
            assertThat(frIntent.flowRules(), hasSize(5));

            List<FlowRule> deviceFlowRules;
            FlowRule flowRule;

            // Of1
            deviceFlowRules = getFlowRulesByDevice(of1Id, frIntent.flowRules());
            assertThat(deviceFlowRules, hasSize(1));
            flowRule = deviceFlowRules.get(0);
            assertThat(flowRule.selector(), is(expectOf1Selector));
            assertThat(flowRule.treatment(), is(expectOf1Treatment));

            // Of2 (has 2 flows)
            deviceFlowRules = getFlowRulesByDevice(of2Id, frIntent.flowRules());
            assertThat(deviceFlowRules, hasSize(2));
            flowRule = deviceFlowRules.get(0);
            assertThat(flowRule.selector(), is(expectOf2Selector1));
            assertThat(flowRule.treatment(), is(expectOf2Treatment));
            flowRule = deviceFlowRules.get(1);
            assertThat(flowRule.selector(), is(expectOf2Selector2));
            assertThat(flowRule.treatment(), is(expectOf2Treatment));

            // Of3
            deviceFlowRules = getFlowRulesByDevice(of3Id, frIntent.flowRules());
            assertThat(deviceFlowRules, hasSize(1));
            flowRule = deviceFlowRules.get(0);
            assertThat(flowRule.selector(), is(expectOf3Selector));
            assertThat(flowRule.treatment(), is(expectOf3Treatment));

            // Of4
            deviceFlowRules = getFlowRulesByDevice(of4Id, frIntent.flowRules());
            assertThat(deviceFlowRules, hasSize(1));
            flowRule = deviceFlowRules.get(0);
            assertThat(flowRule.selector(), is(expectOf4Selector));
            assertThat(flowRule.treatment(), is(expectOf4Treatment));
        }

        sut.deactivate();
    }

    /**
     * Single point to multi point without filtered connect point case.
     * -1 of1 2-1 of2 2--1 of3 2-
     *             3
     *             `-1 of4 2-
     */
    @Test
    public void nonTrivialTranslation1() {
        sut.activate();
        Set<Link> testLinks = ImmutableSet.of(
                DefaultLink.builder().providerId(PID).src(of1p2).dst(of2p1).type(DIRECT).build(),
                DefaultLink.builder().providerId(PID).src(of2p2).dst(of3p1).type(DIRECT).build(),
                DefaultLink.builder().providerId(PID).src(of2p3).dst(of4p1).type(DIRECT).build()
        );

        TrafficSelector expectOf1Selector = DefaultTrafficSelector.builder(ipPrefixSelector)
                .matchInPort(PortNumber.portNumber(1))
                .build();

        TrafficTreatment expectOf1Treatment = DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.portNumber(2))
                .build();

        TrafficSelector expectOf2Selector = DefaultTrafficSelector.builder(ipPrefixSelector)
                .matchInPort(PortNumber.portNumber(1))
                .build();

        TrafficTreatment expectOf2Treatment = DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.portNumber(2))
                .setOutput(PortNumber.portNumber(3))
                .build();

        TrafficSelector expectOf3Selector = DefaultTrafficSelector.builder(ipPrefixSelector)
                .matchInPort(PortNumber.portNumber(1))
                .build();

        TrafficTreatment expectOf3Treatment = DefaultTrafficTreatment.builder(ethDstTreatment)
                .setOutput(PortNumber.portNumber(2))
                .build();

        TrafficSelector expectOf4Selector = DefaultTrafficSelector.builder(ipPrefixSelector)
                .matchInPort(PortNumber.portNumber(1))
                .build();

        TrafficTreatment expectOf4Treatment = DefaultTrafficTreatment.builder(ethDstTreatment)
                .setOutput(PortNumber.portNumber(2))
                .build();



        Set<ConnectPoint> ingress = ImmutableSet.of(
                of1p1
        );

        Set<ConnectPoint> egress = ImmutableSet.of(
                of3p2,
                of4p2
        );

        intent = LinkCollectionIntent.builder()
                .appId(APP_ID)
                .selector(ipPrefixSelector)
                .treatment(ethDstTreatment)
                .ingressPoints(ingress)
                .egressPoints(egress)
                .applyTreatmentOnEgress(true)
                .links(testLinks)
                .build();

        assertThat(sut, is(notNullValue()));

        List<Intent> result = sut.compile(intent, Collections.emptyList());

        assertThat(result, is(notNullValue()));
        assertThat(result, hasSize(1));

        Intent resultIntent = result.get(0);
        assertThat(resultIntent, instanceOf(FlowRuleIntent.class));

        if (resultIntent instanceof FlowRuleIntent) {
            FlowRuleIntent frIntent = (FlowRuleIntent) resultIntent;

            assertThat(frIntent.flowRules(), hasSize(4));

            List<FlowRule> deviceFlowRules;
            FlowRule flowRule;

            // Of1
            deviceFlowRules = getFlowRulesByDevice(of1Id, frIntent.flowRules());
            assertThat(deviceFlowRules, hasSize(1));
            flowRule = deviceFlowRules.get(0);
            assertThat(flowRule.selector(), is(expectOf1Selector));
            assertThat(flowRule.treatment(), is(expectOf1Treatment));

            // Of2
            deviceFlowRules = getFlowRulesByDevice(of2Id, frIntent.flowRules());
            assertThat(deviceFlowRules, hasSize(1));
            flowRule = deviceFlowRules.get(0);
            assertThat(flowRule.selector(), is(expectOf2Selector));
            assertThat(flowRule.treatment(), is(expectOf2Treatment));

            // Of3
            deviceFlowRules = getFlowRulesByDevice(of3Id, frIntent.flowRules());
            assertThat(deviceFlowRules, hasSize(1));
            flowRule = deviceFlowRules.get(0);
            assertThat(flowRule.selector(), is(expectOf3Selector));
            assertThat(flowRule.treatment(), is(expectOf3Treatment));

            // Of4
            deviceFlowRules = getFlowRulesByDevice(of4Id, frIntent.flowRules());
            assertThat(deviceFlowRules, hasSize(1));
            flowRule = deviceFlowRules.get(0);
            assertThat(flowRule.selector(), is(expectOf4Selector));
            assertThat(flowRule.treatment(), is(expectOf4Treatment));

        }
        sut.deactivate();
    }

    /**
     * Multi point to single point intent without filtered connect point.
     *
     * -1 of1 2-1 of2 2-1 of4 2-
     *             3
     * -1 of3 2---/
     */
    @Test
    public void nonTrivialTranslation2() {
        sut.activate();
        Set<Link> testlinks = ImmutableSet.of(
                DefaultLink.builder().providerId(PID).src(of1p2).dst(of2p1).type(DIRECT).build(),
                DefaultLink.builder().providerId(PID).src(of3p2).dst(of2p3).type(DIRECT).build(),
                DefaultLink.builder().providerId(PID).src(of2p2).dst(of4p1).type(DIRECT).build()
        );

        Set<ConnectPoint> ingress = ImmutableSet.of(
                of1p1,
                of3p1
        );

        Set<ConnectPoint> egress = ImmutableSet.of(
                of4p2
        );

        TrafficSelector expectOf1Selector = DefaultTrafficSelector.builder(ipPrefixSelector)
                .matchInPort(PortNumber.portNumber(1))
                .build();

        TrafficTreatment expectOf1Treatment = DefaultTrafficTreatment.builder(ethDstTreatment)
                .setOutput(PortNumber.portNumber(2))
                .build();

        TrafficSelector expectOf2Selector1 = DefaultTrafficSelector.builder(ipPrefixSelector)
                .matchInPort(PortNumber.portNumber(1))
                .matchEthDst(MacAddress.valueOf("C0:FF:EE:C0:FF:EE"))
                .build();

        TrafficSelector expectOf2Selector2 = DefaultTrafficSelector.builder(ipPrefixSelector)
                .matchEthDst(MacAddress.valueOf("C0:FF:EE:C0:FF:EE"))
                .matchInPort(PortNumber.portNumber(3))
                .build();

        TrafficTreatment expectOf2Treatment = DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.portNumber(2))
                .build();

        TrafficSelector expectOf3Selector = DefaultTrafficSelector.builder(ipPrefixSelector)
                .matchInPort(PortNumber.portNumber(1))
                .build();

        TrafficTreatment expectOf3Treatment = DefaultTrafficTreatment.builder(ethDstTreatment)
                .setOutput(PortNumber.portNumber(2))
                .build();

        TrafficSelector expectOf4Selector = DefaultTrafficSelector.builder(ipPrefixSelector)
                .matchEthDst(MacAddress.valueOf("C0:FF:EE:C0:FF:EE"))
                .matchInPort(PortNumber.portNumber(1))
                .build();

        TrafficTreatment expectOf4Treatment = DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.portNumber(2))
                .build();

        intent = LinkCollectionIntent.builder()
                .appId(APP_ID)
                .selector(ipPrefixSelector)
                .ingressPoints(ingress)
                .egressPoints(egress)
                .treatment(ethDstTreatment)
                .links(testlinks)
                .build();

        List<Intent> result = sut.compile(intent, Collections.emptyList());

        assertThat(result, is(notNullValue()));
        assertThat(result, hasSize(1));

        Intent resultIntent = result.get(0);
        assertThat(resultIntent, instanceOf(FlowRuleIntent.class));

        if (resultIntent instanceof FlowRuleIntent) {
            FlowRuleIntent frIntent = (FlowRuleIntent) resultIntent;
            assertThat(frIntent.flowRules(), hasSize(5));

            List<FlowRule> deviceFlowRules;
            FlowRule flowRule;

            // Of1
            deviceFlowRules = getFlowRulesByDevice(of1Id, frIntent.flowRules());
            assertThat(deviceFlowRules, hasSize(1));
            flowRule = deviceFlowRules.get(0);
            assertThat(flowRule.selector(), is(expectOf1Selector));
            assertThat(flowRule.treatment(), is(expectOf1Treatment));

            // Of2 (has 2 flows)
            deviceFlowRules = getFlowRulesByDevice(of2Id, frIntent.flowRules());
            assertThat(deviceFlowRules, hasSize(2));
            flowRule = deviceFlowRules.get(0);
            assertThat(flowRule.selector(), is(expectOf2Selector1));
            assertThat(flowRule.treatment(), is(expectOf2Treatment));
            flowRule = deviceFlowRules.get(1);
            assertThat(flowRule.selector(), is(expectOf2Selector2));
            assertThat(flowRule.treatment(), is(expectOf2Treatment));

            // Of3
            deviceFlowRules = getFlowRulesByDevice(of3Id, frIntent.flowRules());
            assertThat(deviceFlowRules, hasSize(1));
            flowRule = deviceFlowRules.get(0);
            assertThat(flowRule.selector(), is(expectOf3Selector));
            assertThat(flowRule.treatment(), is(expectOf3Treatment));

            // Of4
            deviceFlowRules = getFlowRulesByDevice(of4Id, frIntent.flowRules());
            assertThat(deviceFlowRules, hasSize(1));
            flowRule = deviceFlowRules.get(0);
            assertThat(flowRule.selector(), is(expectOf4Selector));
            assertThat(flowRule.treatment(), is(expectOf4Treatment));
        }

        sut.deactivate();
    }
}
