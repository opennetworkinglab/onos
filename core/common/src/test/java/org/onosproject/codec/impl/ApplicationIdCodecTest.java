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
import org.onosproject.core.ApplicationId;
import org.onosproject.core.DefaultApplicationId;

import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit tests for ApplicationId codec.
 */
public final class ApplicationIdCodecTest {

    MockCodecContext context;
    JsonCodec<ApplicationId> applicationIdCodec;

    /**
     * Sets up for each test. Creates a context and fetches the applicationId
     * codec.
     */
    @Before
    public void setUp() {
        context = new MockCodecContext();
        applicationIdCodec = context.codec(ApplicationId.class);
        assertThat(applicationIdCodec, notNullValue());
    }

    /**
     * Tests encoding of an application id object.
     */
    @Test
    public void testApplicationIdEncode() {

        int id = 1;
        String name = "org.onosproject.foo";
        ApplicationId appId = new DefaultApplicationId(id, name);

        ObjectNode applicationIdJson = applicationIdCodec.encode(appId, context);
        assertThat(applicationIdJson, ApplicationIdJsonMatcher.matchesApplicationId(appId));
    }

    /**
     * Tests decoding of an application id object.
     */
    @Test
    public void testApplicationIdDecode() throws IOException {
        ApplicationId appId = getApplicationId("ApplicationId.json");

        assertThat((int) appId.id(), is(1));
        assertThat(appId.name(), is("org.onosproject.foo"));
    }

    private static final class ApplicationIdJsonMatcher extends TypeSafeDiagnosingMatcher<JsonNode> {

        private final ApplicationId applicationId;

        private ApplicationIdJsonMatcher(ApplicationId applicationId) {
            this.applicationId = applicationId;
        }

        @Override
        protected boolean matchesSafely(JsonNode jsonNode, Description description) {

            // check application role
            int jsonAppId = jsonNode.get("id").asInt();
            int appId = applicationId.id();
            if (jsonAppId != appId) {
                description.appendText("application ID was " + jsonAppId);
                return false;
            }

            String jsonAppName = jsonNode.get("name").asText();
            String appName = applicationId.name();

            if (!jsonAppName.equals(appName)) {
                description.appendText("application name was " + jsonAppName);
                return false;
            }

            return true;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(applicationId.toString());
        }

        static ApplicationIdJsonMatcher matchesApplicationId(ApplicationId applicationId) {
            return new ApplicationIdJsonMatcher(applicationId);
        }
    }

    /**
     * Reads in a application id from the given resource and decodes it.
     *
     * @param resourceName resource to use to read the JSON for the rule
     * @return decoded application id
     * @throws IOException if processing the resource fails
     */
    private ApplicationId getApplicationId(String resourceName) throws IOException {
        InputStream jsonStream = ApplicationIdCodecTest.class.getResourceAsStream(resourceName);
        JsonNode json = context.mapper().readTree(jsonStream);
        assertThat(json, notNullValue());
        ApplicationId applicationId = applicationIdCodec.decode((ObjectNode) json, context);
        assertThat(applicationId, notNullValue());
        return applicationId;
    }
}
