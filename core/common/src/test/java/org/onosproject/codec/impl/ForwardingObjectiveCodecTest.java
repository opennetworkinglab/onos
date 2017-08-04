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
package org.onosproject.codec.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.VlanId;
import org.onosproject.codec.JsonCodec;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.ForwardingObjective;

import java.io.IOException;
import java.io.InputStream;

import static org.easymock.EasyMock.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.onosproject.codec.impl.ForwardingObjectiveJsonMatcher.matchesForwardingObjective;
import static org.onosproject.net.NetTestTools.APP_ID;

/**
 * Unit tests for ForwardingObjective Codec.
 */
public class ForwardingObjectiveCodecTest {

    MockCodecContext context;
    JsonCodec<ForwardingObjective> forwardingObjectiveCodec;
    final CoreService mockCoreService = createMock(CoreService.class);
    static final String SAMPLE_APP_ID = "org.onosproject.sample";

    /**
     * Sets up for each test.
     * Creates a context and fetches the ForwardingObjective codec.
     */
    @Before
    public void setUp() {
        context = new MockCodecContext();
        forwardingObjectiveCodec = context.codec(ForwardingObjective.class);
        assertThat(forwardingObjectiveCodec, notNullValue());

        context.registerService(CoreService.class, mockCoreService);
    }

    /**
     * Tests encoding of a ForwardingObjective object.
     */
    @Test
    public void testForwardingObjectiveEncode() {

        Criterion criterion1 = Criteria.matchVlanId(VlanId.ANY);
        Criterion criterion2 = Criteria.matchEthType((short) 0x8844);
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .add(criterion1)
                .add(criterion2)
                .build();

        ForwardingObjective forwardingObj = DefaultForwardingObjective.builder()
                .makePermanent()
                .fromApp(APP_ID)
                .withPriority(60)
                .withFlag(ForwardingObjective.Flag.SPECIFIC)
                .nextStep(1)
                .withSelector(selector)
                .add();

        ObjectNode forwardingObjJson = forwardingObjectiveCodec.encode(forwardingObj, context);
        assertThat(forwardingObjJson, matchesForwardingObjective(forwardingObj));
    }

    /**
     * Test decoding of a ForwardingObjective object.
     */
    @Test
    public void testForwardingObjectiveDecode() throws IOException {

        ApplicationId appId = new DefaultApplicationId(0, SAMPLE_APP_ID);

        expect(mockCoreService.registerApplication(SAMPLE_APP_ID)).andReturn(appId).anyTimes();
        replay(mockCoreService);

        ForwardingObjective forwardingObjective = getForwardingObjective("ForwardingObjective.json");

        assertThat(forwardingObjective.flag(), is(ForwardingObjective.Flag.SPECIFIC));
        assertThat(forwardingObjective.priority(), is(60));
        assertThat(forwardingObjective.timeout(), is(1));
        assertThat(forwardingObjective.op(), is(ForwardingObjective.Operation.ADD));
        assertThat(forwardingObjective.permanent(), is(false));
        assertThat(forwardingObjective.appId().name(), is(SAMPLE_APP_ID));
    }

    /**
     * Reads in a forwardingObjectiveJsonCodec from the given resource and decodes it.
     *
     * @param resourceName resource to use to read the JSON for the rule
     * @return decoded forwardingObjectiveJsonCodec
     * @throws IOException if processing the resource fails
     */
    private ForwardingObjective getForwardingObjective(String resourceName) throws IOException {
        InputStream jsonStream = ForwardingObjectiveCodecTest.class.getResourceAsStream(resourceName);
        JsonNode json = context.mapper().readTree(jsonStream);
        assertThat(json, notNullValue());
        ForwardingObjective forwardingObjective = forwardingObjectiveCodec.decode((ObjectNode) json, context);
        assertThat(forwardingObjective, notNullValue());
        return forwardingObjective;
    }
}
