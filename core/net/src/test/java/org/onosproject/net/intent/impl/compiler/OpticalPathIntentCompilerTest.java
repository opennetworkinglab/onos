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
import org.onosproject.net.OchSignalType;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.intent.FlowRuleIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentExtensionService;
import org.onosproject.net.intent.MockIdGenerator;
import org.onosproject.net.intent.OpticalPathIntent;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.onosproject.net.Link.Type.DIRECT;
import static org.onosproject.net.NetTestTools.PID;
import static org.onosproject.net.NetTestTools.connectPoint;
import static org.onosproject.net.NetTestTools.createLambda;

public class OpticalPathIntentCompilerTest {

    private CoreService coreService;
    private IntentExtensionService intentExtensionService;
    private final IdGenerator idGenerator = new MockIdGenerator();
    private OpticalPathIntentCompiler sut;

    private final ApplicationId appId = new TestApplicationId("test");
    private final ConnectPoint d1p1 = connectPoint("s1", 0);
    private final ConnectPoint d2p0 = connectPoint("s2", 0);
    private final ConnectPoint d2p1 = connectPoint("s2", 1);
    private final ConnectPoint d3p1 = connectPoint("s3", 1);

    private final List<Link> links = Arrays.asList(
            DefaultLink.builder().providerId(PID).src(d1p1).dst(d2p0).type(DIRECT).build(),
            DefaultLink.builder().providerId(PID).src(d2p1).dst(d3p1).type(DIRECT).build()
    );
    private final int hops = links.size() + 1;
    private OpticalPathIntent intent;

    @Before
    public void setUp() {
        sut = new OpticalPathIntentCompiler();
        coreService = createMock(CoreService.class);
        expect(coreService.registerApplication("org.onosproject.net.intent"))
                .andReturn(appId);
        sut.coreService = coreService;

        Intent.bindIdGenerator(idGenerator);

        intent = OpticalPathIntent.builder()
                .appId(appId)
                .src(d1p1)
                .dst(d3p1)
                .path(new DefaultPath(PID, links, hops))
                .lambda(createLambda())
                .signalType(OchSignalType.FIXED_GRID)
                .build();
        intentExtensionService = createMock(IntentExtensionService.class);
        intentExtensionService.registerCompiler(OpticalPathIntent.class, sut);
        intentExtensionService.unregisterCompiler(OpticalPathIntent.class);
        sut.intentManager = intentExtensionService;

        replay(coreService, intentExtensionService);
    }

    @After
    public void tearDown() {
        Intent.unbindIdGenerator(idGenerator);
    }

    @Test
    public void testCompiler() {
        sut.activate();

        List<Intent> compiled = sut.compile(intent, Collections.emptyList());
        assertThat(compiled, hasSize(1));

        Collection<FlowRule> rules = ((FlowRuleIntent) compiled.get(0)).flowRules();
        rules.stream()
                .filter(x -> x.deviceId().equals(d1p1.deviceId()))
                .findFirst()
                .get();

        rules.stream()
                .filter(x -> x.deviceId().equals(d2p1.deviceId()))
                .findFirst()
                .get();

        rules.stream()
                .filter(x -> x.deviceId().equals(d3p1.deviceId()))
                .findFirst()
                .get();

        rules.forEach(rule -> assertEquals("FlowRule priority is incorrect",
                                           intent.priority(), rule.priority()));

        sut.deactivate();
    }

    //TODO test bidirectional optical paths and verify rules

}
