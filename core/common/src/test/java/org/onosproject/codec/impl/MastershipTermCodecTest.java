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
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.cluster.NodeId;
import org.onosproject.codec.JsonCodec;
import org.onosproject.mastership.MastershipTerm;

import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit tests for mastership term codec.
 */
public class MastershipTermCodecTest {

    MockCodecContext context;
    JsonCodec<MastershipTerm> mastershipTermCodec;

    /**
     * Sets up for each test. Creates a context and fetches the mastership term
     * codec.
     */
    @Before
    public void setUp() {
        context = new MockCodecContext();
        mastershipTermCodec = context.codec(MastershipTerm.class);
        assertThat(mastershipTermCodec, notNullValue());
    }

    /**
     * Tests encoding of a mastership term object.
     */
    @Test
    public void testMastershipTermEncode() {
        NodeId masterNodeId = NodeId.nodeId("1");
        long termNumber = 10;

        MastershipTerm mastershipTerm = MastershipTerm.of(masterNodeId, termNumber);
        ObjectNode mastershipTermJson = mastershipTermCodec.encode(mastershipTerm, context);
        assertThat(mastershipTermJson, MastershipTermJsonMatcher.matchesMastershipTerm(mastershipTerm));
    }

    /**
     * Tests decoding of mastership term JSON object.
     */
    @Test
    public void testMastershipTermDecode() throws IOException {
        MastershipTerm mastershipTerm = getMastershipTerm("MastershipTerm.json");

        assertThat(mastershipTerm.master().id(), is("1"));
        assertThat(mastershipTerm.termNumber(), is(10L));
    }

    /**
     * Hamcrest matcher for mastership term.
     */
    private static final class MastershipTermJsonMatcher extends TypeSafeDiagnosingMatcher<JsonNode> {

        private final MastershipTerm mastershipTerm;

        private MastershipTermJsonMatcher(MastershipTerm mastershipTerm) {
            this.mastershipTerm = mastershipTerm;
        }

        @Override
        protected boolean matchesSafely(JsonNode jsonNode, Description description) {

            // check node identifier of master
            String jsonNodeId = jsonNode.get("master").asText();
            String nodeId = mastershipTerm.master().id();
            if (!jsonNodeId.equals(nodeId)) {
                description.appendText("master's node id was " + jsonNodeId);
                return false;
            }

            // check term number
            long jsonTermNumber = jsonNode.get("termNumber").asLong();
            long termNumber = mastershipTerm.termNumber();
            if (jsonTermNumber != termNumber) {
                description.appendText("term number was " + jsonTermNumber);
                return false;
            }

            return true;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(mastershipTerm.toString());
        }

        static MastershipTermJsonMatcher matchesMastershipTerm(MastershipTerm mastershipTerm) {
            return new MastershipTermJsonMatcher(mastershipTerm);
        }
    }

    /**
     * Reads in a mastership term from the given resource and decodes it.
     *
     * @param resourceName resource to use to read the JSON for the rule
     * @return decoded mastership term object
     * @throws IOException if processing the resource fails
     */
    private MastershipTerm getMastershipTerm(String resourceName) throws IOException {
        InputStream jsonStream = MastershipTermCodecTest.class.getResourceAsStream(resourceName);
        JsonNode json = context.mapper().readTree(jsonStream);
        assertThat(json, notNullValue());
        MastershipTerm mastershipTerm = mastershipTermCodec.decode((ObjectNode) json, context);
        assertThat(mastershipTerm, notNullValue());
        return mastershipTerm;
    }
}
