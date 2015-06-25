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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.TestApplicationId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.IdGenerator;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.DefaultPath;
import org.onosproject.net.Link;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.intent.FlowRuleIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentExtensionService;
import org.onosproject.net.intent.MockIdGenerator;
import org.onosproject.net.intent.PathIntent;
import org.onosproject.net.provider.ProviderId;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.onosproject.net.DefaultEdgeLink.createEdgeLink;
import static org.onosproject.net.Link.Type.DIRECT;
import static org.onosproject.net.NetTestTools.APP_ID;
import static org.onosproject.net.NetTestTools.PID;
import static org.onosproject.net.NetTestTools.connectPoint;

/**
 * Unit tests for PathIntentCompiler.
 */
public class PathIntentCompilerTest {

    private CoreService coreService;
    private IntentExtensionService intentExtensionService;
    private IdGenerator idGenerator = new MockIdGenerator();
    private PathIntentCompiler sut;

    private final TrafficSelector selector = DefaultTrafficSelector.builder().build();
    private final TrafficTreatment treatment = DefaultTrafficTreatment.builder().build();
    private final ApplicationId appId = new TestApplicationId("test");
    private final ProviderId pid = new ProviderId("of", "test");
    private final ConnectPoint d1p1 = connectPoint("s1", 0);
    private final ConnectPoint d2p0 = connectPoint("s2", 0);
    private final ConnectPoint d2p1 = connectPoint("s2", 1);
    private final ConnectPoint d3p1 = connectPoint("s3", 1);
    private final ConnectPoint d3p0 = connectPoint("s3", 10);
    private final ConnectPoint d1p0 = connectPoint("s1", 10);
    private static final int PRIORITY = 555;

    private final List<Link> links = Arrays.asList(
            createEdgeLink(d1p0, true),
            new DefaultLink(PID, d1p1, d2p0, DIRECT),
            new DefaultLink(PID, d2p1, d3p1, DIRECT),
            createEdgeLink(d3p0, false)
    );
    private final int hops = links.size() - 1;
    private PathIntent intent;

    /**
     * Configures objects used in all the test cases.
     */
    @Before
    public void setUp() {
        sut = new PathIntentCompiler();
        coreService = createMock(CoreService.class);
        expect(coreService.registerApplication("org.onosproject.net.intent"))
                .andReturn(appId);
        sut.coreService = coreService;

        Intent.bindIdGenerator(idGenerator);

        intent = PathIntent.builder()
                .appId(APP_ID)
                .selector(selector)
                .treatment(treatment)
                .priority(PRIORITY)
                .path(new DefaultPath(pid, links, hops))
                .build();
        intentExtensionService = createMock(IntentExtensionService.class);
        intentExtensionService.registerCompiler(PathIntent.class, sut);
        intentExtensionService.unregisterCompiler(PathIntent.class);
        sut.intentManager = intentExtensionService;

        replay(coreService, intentExtensionService);
    }

    /**
     * Tears down objects used in all the test cases.
     */
    @After
    public void tearDown() {
        Intent.unbindIdGenerator(idGenerator);
    }

    /**
     * Tests the compilation behavior of the path intent compiler.
     */
    @Test
    public void testCompile() {
        sut.activate();

        List<Intent> compiled = sut.compile(intent, Collections.emptyList(), Collections.emptySet());
        assertThat(compiled, hasSize(1));

        Collection<FlowRule> rules = ((FlowRuleIntent) compiled.get(0)).flowRules();

        FlowRule rule1 = rules.stream()
                .filter(x -> x.deviceId().equals(d1p0.deviceId()))
                .findFirst()
                .get();
        assertThat(rule1.deviceId(), is(d1p0.deviceId()));
        assertThat(rule1.selector(),
                is(DefaultTrafficSelector.builder(selector).matchInPort(d1p0.port()).build()));
        assertThat(rule1.treatment(),
                is(DefaultTrafficTreatment.builder().setOutput(d1p1.port()).build()));
        assertThat(rule1.priority(), is(intent.priority()));

        FlowRule rule2 = rules.stream()
                .filter(x -> x.deviceId().equals(d2p0.deviceId()))
                .findFirst()
                .get();
        assertThat(rule2.deviceId(), is(d2p0.deviceId()));
        assertThat(rule2.selector(),
                is(DefaultTrafficSelector.builder(selector).matchInPort(d2p0.port()).build()));
        assertThat(rule2.treatment(),
                is(DefaultTrafficTreatment.builder().setOutput(d2p1.port()).build()));
        assertThat(rule2.priority(), is(intent.priority()));

        FlowRule rule3 = rules.stream()
                .filter(x -> x.deviceId().equals(d3p0.deviceId()))
                .findFirst()
                .get();
        assertThat(rule3.deviceId(), is(d3p1.deviceId()));
        assertThat(rule3.selector(),
                is(DefaultTrafficSelector.builder(selector).matchInPort(d3p1.port()).build()));
        assertThat(rule3.treatment(),
                is(DefaultTrafficTreatment.builder(treatment).setOutput(d3p0.port()).build()));
        assertThat(rule3.priority(), is(intent.priority()));

        sut.deactivate();
    }
}
