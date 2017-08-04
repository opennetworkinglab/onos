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
import com.google.common.collect.ImmutableSet;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.Ethernet;
import org.onlab.packet.MacAddress;
import org.onosproject.codec.JsonCodec;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.criteria.Criterion;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit tests for traffic selector codec.
 */
public class TrafficSelectorCodecTest {

    private MockCodecContext context;
    private JsonCodec<TrafficSelector> trafficSelectorCodec;

    @Before
    public void setUp() {
        context = new MockCodecContext();
        trafficSelectorCodec = context.codec(TrafficSelector.class);
        assertThat(trafficSelectorCodec, notNullValue());
    }

    /**
     * Tests encoding of a traffic selector object.
     */
    @Test
    public void testTrafficSelectorEncode() {

        Criterion inPort = Criteria.matchInPort(PortNumber.portNumber(0));
        Criterion ethSrc = Criteria.matchEthSrc(MacAddress.valueOf("11:22:33:44:55:66"));
        Criterion ethDst = Criteria.matchEthDst(MacAddress.valueOf("44:55:66:77:88:99"));
        Criterion ethType = Criteria.matchEthType(Ethernet.TYPE_IPV4);

        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();
        TrafficSelector selector = sBuilder
                .add(inPort)
                .add(ethSrc)
                .add(ethDst)
                .add(ethType)
                .build();

        ObjectNode selectorJson = trafficSelectorCodec.encode(selector, context);
        assertThat(selectorJson, TrafficSelectorJsonMatcher.matchesTrafficSelector(selector));
    }

    /**
     * Tests decoding of a traffic selector JSON object.
     */
    @Test
    public void testTrafficSelectorDecode() throws IOException {
        TrafficSelector selector = getSelector("TrafficSelector.json");

        Set<Criterion> criteria = selector.criteria();
        assertThat(criteria.size(), is(4));

        ImmutableSet<String> types = ImmutableSet.of("IN_PORT", "ETH_SRC", "ETH_DST", "ETH_TYPE");

        criteria.forEach(c -> assertThat(types.contains(c.type().name()), is(true)));
    }

    public static final class TrafficSelectorJsonMatcher
            extends TypeSafeDiagnosingMatcher<JsonNode> {
        private final TrafficSelector trafficSelector;

        private TrafficSelectorJsonMatcher(TrafficSelector trafficSelector) {
            this.trafficSelector = trafficSelector;
        }

        @Override
        protected boolean matchesSafely(JsonNode jsonNode, Description description) {
            // check size of criteria
            JsonNode jsonCriteria = jsonNode.get("criteria");
            if (jsonCriteria.size() != trafficSelector.criteria().size()) {
                description.appendText("criteria size was " + jsonCriteria.size());
                return false;
            }

            // check criteria
            for (Criterion criterion : trafficSelector.criteria()) {
                boolean criterionFound = false;
                for (int criterionIndex = 0; criterionIndex < jsonCriteria.size(); criterionIndex++) {
                    CriterionJsonMatcher criterionMatcher = CriterionJsonMatcher.matchesCriterion(criterion);
                    if (criterionMatcher.matches(jsonCriteria.get(criterionIndex))) {
                        criterionFound = true;
                        break;
                    }
                }

                if (!criterionFound) {
                    description.appendText("criterion not found " + criterion.toString());
                    return false;
                }
            }

            return true;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(trafficSelector.toString());
        }

        /**
         * Factory to allocate a traffic selector matcher.
         *
         * @param selector traffic selector object we are looking for
         * @return matcher
         */
        static TrafficSelectorJsonMatcher matchesTrafficSelector(TrafficSelector selector) {
            return new TrafficSelectorJsonMatcher(selector);
        }
    }

    /**
     * Reads in a traffic selector from the given resource and decodes it.
     *
     * @param resourceName resource to use to read the JSON for the rule
     * @return decoded trafficSelector
     * @throws IOException if processing the resource fails
     */
    private TrafficSelector getSelector(String resourceName) throws IOException {
        InputStream jsonStream = TrafficSelectorCodecTest.class.getResourceAsStream(resourceName);
        JsonNode json = context.mapper().readTree(jsonStream);
        assertThat(json, notNullValue());
        TrafficSelector selector = trafficSelectorCodec.decode((ObjectNode) json, context);
        assertThat(selector, notNullValue());
        return selector;
    }
}
