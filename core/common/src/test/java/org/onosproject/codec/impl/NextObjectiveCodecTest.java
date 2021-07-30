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
import org.onosproject.codec.JsonCodec;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flowobjective.DefaultNextObjective;
import org.onosproject.net.flowobjective.DefaultNextTreatment;
import org.onosproject.net.flowobjective.NextObjective;
import org.onosproject.net.flowobjective.NextTreatment;

import java.io.IOException;
import java.io.InputStream;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.onosproject.codec.impl.NextObjectiveJsonMatcher.matchesNextObjective;
import static org.onosproject.net.NetTestTools.APP_ID;

/**
 * Unit tests for NextObjective Codec.
 */
public class NextObjectiveCodecTest {

    MockCodecContext context;
    JsonCodec<NextObjective> nextObjectiveCodec;
    final CoreService mockCoreService = createMock(CoreService.class);
    static final String SAMPLE_APP_ID = "org.onosproject.sample";

    /**
     * Sets up for each test.
     * Creates a context and fetches the NextObjective codec.
     */
    @Before
    public void setUp() {
        context = new MockCodecContext();
        nextObjectiveCodec = context.codec(NextObjective.class);
        assertThat(nextObjectiveCodec, notNullValue());

        context.registerService(CoreService.class, mockCoreService);
    }

    /**
     * Tests encoding of a NextObjective object.
     */
    @Test
    public void testNextObjectiveEncode() {

        TrafficTreatment treatment = DefaultTrafficTreatment.builder().build();
        NextTreatment nextTreatment = DefaultNextTreatment.of(treatment, 5);

        NextObjective nextObj = DefaultNextObjective.builder()
                .makePermanent()
                .withType(NextObjective.Type.HASHED)
                .fromApp(APP_ID)
                .withPriority(60)
                .withId(5)
                .addTreatment(nextTreatment)
                .add();

        ObjectNode nextObjJson = nextObjectiveCodec.encode(nextObj, context);
        assertThat(nextObjJson, matchesNextObjective(nextObj));
    }

    /**
     * Test decoding of a NextObjective object.
     */
    @Test
    public void testNextObjectiveDecode() throws IOException {

        ApplicationId appId = new DefaultApplicationId(0, SAMPLE_APP_ID);

        expect(mockCoreService.registerApplication(SAMPLE_APP_ID)).andReturn(appId).anyTimes();
        replay(mockCoreService);

        NextObjective nextObjective = getNextObjective("NextObjective.json");

        assertThat(nextObjective.type(), is(NextObjective.Type.FAILOVER));
        assertThat(nextObjective.op(), is(NextObjective.Operation.ADD));
    }

    /**
     * Reads in a nextObjective from the given resource and decodes it.
     *
     * @param resourceName resource to use to read the JSON for the rule
     * @return decoded nextObjective
     * @throws IOException if processing the resource fails
     */
    private NextObjective getNextObjective(String resourceName) throws IOException {
        InputStream jsonStream = NextObjectiveCodecTest.class.getResourceAsStream(resourceName);
        JsonNode json = context.mapper().readTree(jsonStream);
        assertThat(json, notNullValue());
        NextObjective nextObjective = nextObjectiveCodec.decode((ObjectNode) json, context);
        assertThat(nextObjective, notNullValue());
        return nextObjective;
    }
}
