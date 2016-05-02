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
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.codec.JsonCodec;
import org.onosproject.net.MastershipRole;

import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.onosproject.net.MastershipRole.MASTER;

/**
 * Unit tests for mastership role codec.
 */
public final class MastershipRoleCodecTest {

    MockCodecContext context;
    JsonCodec<MastershipRole> mastershipRoleCodec;

    /**
     * Sets up for each test. Creates a context and fetches the mastership role
     * codec.
     */
    @Before
    public void setUp() {
        context = new MockCodecContext();
        mastershipRoleCodec = context.codec(MastershipRole.class);
        assertThat(mastershipRoleCodec, notNullValue());
    }

    /**
     * Tests encoding of a mastership role object.
     */
    @Test
    public void testMastershipRoleEncode() {
        MastershipRole mastershipRole = MASTER;
        ObjectNode mastershipRoleJson = mastershipRoleCodec.encode(mastershipRole, context);
        assertThat(mastershipRoleJson, MastershipRoleJsonMatcher.matchesMastershipRole(mastershipRole));
    }

    /**
     * Tests decoding of mastership role JSON object.
     */
    @Test
    public void testMastershipRoleDecode() throws IOException {
        MastershipRole mastershipRole = getMastershipRole("MastershipRole.json");

        assertThat(mastershipRole, is(MASTER));
    }

    /**
     * Hamcrest matcher for mastership role.
     */
    private static final class MastershipRoleJsonMatcher extends TypeSafeDiagnosingMatcher<JsonNode> {

        private final MastershipRole mastershipRole;

        private MastershipRoleJsonMatcher(MastershipRole mastershipRole) {
            this.mastershipRole = mastershipRole;
        }

        @Override
        protected boolean matchesSafely(JsonNode jsonNode, Description description) {

            // check mastership role
            String jsonRole = jsonNode.get("role").asText();
            String role = mastershipRole.name();
            if (!jsonRole.equals(role)) {
                description.appendText("mastership role was " + jsonRole);
                return false;
            }

            return true;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(mastershipRole.toString());
        }

        static MastershipRoleJsonMatcher matchesMastershipRole(MastershipRole mastershipRole) {
            return new MastershipRoleJsonMatcher(mastershipRole);
        }
    }

    /**
     * Reads in a mastership role from the given resource and decodes it.
     *
     * @param resourceName resource to use to read the JSON for the rule
     * @return decoded mastership term object
     * @throws IOException if processing the resource fails
     */
    private MastershipRole getMastershipRole(String resourceName) throws IOException {
        InputStream jsonStream = MastershipRoleCodecTest.class.getResourceAsStream(resourceName);
        JsonNode json = context.mapper().readTree(jsonStream);
        assertThat(json, notNullValue());
        MastershipRole mastershipRole = mastershipRoleCodec.decode((ObjectNode) json, context);
        assertThat(mastershipRole, notNullValue());
        return mastershipRole;
    }
}
