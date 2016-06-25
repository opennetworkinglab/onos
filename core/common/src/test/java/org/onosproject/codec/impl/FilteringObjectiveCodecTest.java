/*
 * Copyright 2016-present Open Networking Laboratory
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
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flowobjective.DefaultFilteringObjective;
import org.onosproject.net.flowobjective.FilteringObjective;

import java.io.IOException;
import java.io.InputStream;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.onosproject.codec.impl.FilteringObjectiveJsonMatcher.matchesFilteringObjective;
import static org.onosproject.net.NetTestTools.APP_ID;

/**
 * Unit tests for FilteringObjective Codec.
 */
public class FilteringObjectiveCodecTest {

    MockCodecContext context;
    JsonCodec<FilteringObjective> filteringObjectiveCodec;
    final CoreService mockCoreService = createMock(CoreService.class);
    static final String SAMPLE_APP_ID = "org.onosproject.sample";

    /**
     * Sets up for each test.
     * Creates a context and fetches the FilteringObjective codec.
     */
    @Before
    public void setUp() {
        context = new MockCodecContext();
        filteringObjectiveCodec = context.codec(FilteringObjective.class);
        assertThat(filteringObjectiveCodec, notNullValue());

        context.registerService(CoreService.class, mockCoreService);
    }

    /**
     * Tests encoding of a FilteringObjective object.
     */
    @Test
    public void testFilteringObjectiveEncode() {

        Criterion condition1 = Criteria.matchVlanId(VlanId.ANY);
        Criterion condition2 = Criteria.matchEthType((short) 0x8844);

        FilteringObjective filteringObj = DefaultFilteringObjective.builder()
                .makePermanent()
                .permit()
                .fromApp(APP_ID)
                .withPriority(60)
                .addCondition(condition1)
                .addCondition(condition2)
                .add();

        // TODO: need to add test case for TrafficTreatment (META in filteringObj)

        ObjectNode filteringObjJson = filteringObjectiveCodec.encode(filteringObj, context);
        assertThat(filteringObjJson, matchesFilteringObjective(filteringObj));
    }

    /**
     * Test decoding of a FilteringObjective object.
     */
    @Test
    public void testFilteringObjectiveDecode() throws IOException {

        ApplicationId appId = new DefaultApplicationId(0, SAMPLE_APP_ID);

        expect(mockCoreService.registerApplication(SAMPLE_APP_ID)).andReturn(appId).anyTimes();
        replay(mockCoreService);

        FilteringObjective filteringObjective = getFilteringObjective("FilteringObjective.json");

        assertThat(filteringObjective.type(), is(FilteringObjective.Type.PERMIT));
        assertThat(filteringObjective.priority(), is(60));
        assertThat(filteringObjective.timeout(), is(1));
        assertThat(filteringObjective.op(), is(FilteringObjective.Operation.ADD));
        assertThat(filteringObjective.permanent(), is(false));
    }

    /**
     * Reads in a filteringObjective from the given resource and decodes it.
     *
     * @param resourceName resource to use to read the JSON for the rule
     * @return decoded filteringObjective
     * @throws IOException if processing the resource fails
     */
    private FilteringObjective getFilteringObjective(String resourceName) throws IOException {
        InputStream jsonStream = FilteringObjectiveCodecTest.class.getResourceAsStream(resourceName);
        JsonNode json = context.mapper().readTree(jsonStream);
        assertThat(json, notNullValue());
        FilteringObjective filteringObjective = filteringObjectiveCodec.decode((ObjectNode) json, context);
        assertThat(filteringObjective, notNullValue());
        return filteringObjective;
    }
}
