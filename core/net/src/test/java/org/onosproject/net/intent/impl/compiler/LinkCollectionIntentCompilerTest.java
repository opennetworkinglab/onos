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
import com.google.common.collect.Maps;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.VlanId;
import org.onosproject.TestApplicationId;
import org.onosproject.cfg.ComponentConfigAdapter;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.IdGenerator;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.Link;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.intent.FlowRuleIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentExtensionService;
import org.onosproject.net.intent.LinkCollectionIntent;
import org.onosproject.net.intent.MockIdGenerator;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.onosproject.net.Link.Type.DIRECT;
import static org.onosproject.net.NetTestTools.APP_ID;
import static org.onosproject.net.NetTestTools.PID;
import static org.onosproject.net.NetTestTools.connectPoint;

public class LinkCollectionIntentCompilerTest {

    private final ApplicationId appId = new TestApplicationId("test");

    private final ConnectPoint d1p1 = connectPoint("s1", 0);
    private final ConnectPoint d2p0 = connectPoint("s2", 0);
    private final ConnectPoint d2p1 = connectPoint("s2", 1);
    private final ConnectPoint d3p1 = connectPoint("s3", 1);
    private final ConnectPoint d3p2 = connectPoint("s3", 9);
    private final ConnectPoint d3p0 = connectPoint("s3", 10);
    private final ConnectPoint d1p0 = connectPoint("s1", 10);

    private final Set<Link> links = ImmutableSet.of(
            DefaultLink.builder().providerId(PID).src(d1p1).dst(d2p0).type(DIRECT).build(),
            DefaultLink.builder().providerId(PID).src(d2p1).dst(d3p1).type(DIRECT).build(),
            DefaultLink.builder().providerId(PID).src(d1p1).dst(d3p1).type(DIRECT).build());

    private final Set<Link> linksMultiple = ImmutableSet.of(
            DefaultLink.builder().providerId(PID).src(d3p1).dst(d2p0).type(DIRECT).build());

    private final TrafficSelector selector = DefaultTrafficSelector.builder().build();
    private final TrafficTreatment treatment = DefaultTrafficTreatment.builder().build();

    private final VlanId ingressVlan1 = VlanId.vlanId("10");
    private final TrafficSelector selectorVlan1 = DefaultTrafficSelector
            .builder()
            .matchVlanId(ingressVlan1)
            .build();

    private final VlanId ingressVlan2 = VlanId.vlanId("20");
    private final TrafficSelector selectorVlan2 = DefaultTrafficSelector
            .builder()
            .matchVlanId(ingressVlan2)
            .build();

    private final VlanId egressVlan = VlanId.vlanId("666");
    private final TrafficTreatment vlanTreatment = DefaultTrafficTreatment
            .builder()
            .setVlanId(egressVlan)
            .build();

    private CoreService coreService;
    private IntentExtensionService intentExtensionService;
    private IntentConfigurableRegistrator registrator;
    private IdGenerator idGenerator = new MockIdGenerator();

    private LinkCollectionIntent intent;
    private LinkCollectionIntent intentMultiple;


    private LinkCollectionIntentCompiler sut;

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
        intentMultiple = LinkCollectionIntent.builder()
                .appId(APP_ID)
                .treatment(vlanTreatment)
                .links(linksMultiple)
                .ingressPoints(ImmutableSet.of(d3p0, d3p2))
                .egressPoints(ImmutableSet.of(d2p1))
                .ingressSelectors(this.createIngressSelectors())
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

    @Test
    public void testCompileMultipleSelectors() {
        sut.activate();

        List<Intent> compiled = sut.compile(intentMultiple, Collections.emptyList());
        assertThat(compiled, hasSize(1));


        Collection<FlowRule> rules = ((FlowRuleIntent) compiled.get(0)).flowRules();
        assertThat(rules, hasSize((linksMultiple.size()) + intentMultiple.ingressPoints().size()));

        Set<FlowRule> d3Rules = rules
                .parallelStream()
                .filter(rule -> rule.deviceId().equals(d3p0.deviceId()))
                .collect(Collectors.toSet());
        assertThat(d3Rules, hasSize(intentMultiple.ingressPoints().size()));

        FlowRule rule1 = rules.stream()
                .filter(rule -> rule.deviceId().equals(d3p0.deviceId())
                        &&
                        rule.selector().getCriterion(Criterion.Type.IN_PORT).equals(Criteria.matchInPort(d3p0.port())))
                .findFirst()
                .get();
        assertThat(rule1.selector(), is(
                DefaultTrafficSelector
                        .builder(intentMultiple.selector())
                        .matchInPort(d3p0.port())
                        .matchVlanId(ingressVlan1)
                        .build()
        ));
        assertThat(rule1.treatment(), is(
                DefaultTrafficTreatment
                        .builder(intentMultiple.treatment())
                        .setOutput(d3p1.port())
                        .build()
        ));
        assertThat(rule1.priority(), is(intentMultiple.priority()));

        FlowRule rule2 = rules.stream()
                .filter(rule -> rule.deviceId().equals(d3p0.deviceId())
                        &&
                        rule.selector().getCriterion(Criterion.Type.IN_PORT).equals(Criteria.matchInPort(d3p2.port())))
                .findFirst()
                .get();
        assertThat(rule2.selector(), is(
                DefaultTrafficSelector
                        .builder(intentMultiple.selector())
                        .matchInPort(d3p2.port())
                        .matchVlanId(ingressVlan2)
                        .build()
        ));
        assertThat(rule2.treatment(), is(
                DefaultTrafficTreatment
                        .builder(intentMultiple.treatment())
                        .setOutput(d3p1.port())
                        .build()
        ));
        assertThat(rule1.priority(), is(intentMultiple.priority()));

        Set<FlowRule> d2Rules = rules
                .parallelStream()
                .filter(rule -> rule.deviceId().equals(d2p0.deviceId()))
                .collect(Collectors.toSet());
        assertThat(d2Rules, hasSize(intentMultiple.egressPoints().size()));

        // We do not need in_port filter
        FlowRule rule3 = rules.stream()
                .filter(rule -> rule.deviceId().equals(d2p0.deviceId()))
                .findFirst()
                .get();
        assertThat(rule3.selector(), is(
                DefaultTrafficSelector
                        .builder(intentMultiple.selector())
                        .matchInPort(d2p0.port())
                        .matchVlanId(egressVlan)
                        .build()
        ));
        assertThat(rule3.treatment(), is(
                DefaultTrafficTreatment
                        .builder()
                        .setOutput(d2p1.port())
                        .build()
        ));
        assertThat(rule3.priority(), is(intentMultiple.priority()));

        sut.deactivate();
    }

    public Map<ConnectPoint, TrafficSelector> createIngressSelectors() {
        Map<ConnectPoint, TrafficSelector> mapToReturn = Maps.newHashMap();
        mapToReturn.put(d3p0, selectorVlan1);
        mapToReturn.put(d3p2, selectorVlan2);
        return mapToReturn;
    }
}
