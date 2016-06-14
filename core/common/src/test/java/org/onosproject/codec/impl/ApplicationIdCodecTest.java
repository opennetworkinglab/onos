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


import static org.hamcrest.MatcherAssert.assertThat;
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

    private static final class ApplicationIdJsonMatcher extends TypeSafeDiagnosingMatcher<JsonNode> {

        private final ApplicationId applicationId;

        private ApplicationIdJsonMatcher(ApplicationId applicationId) {
            this.applicationId = applicationId;
        }

        @Override
        protected boolean matchesSafely(JsonNode jsonNode, Description description) {

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
}
