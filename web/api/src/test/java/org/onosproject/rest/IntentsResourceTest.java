/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.rest;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicLong;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.osgi.TestServiceDirectory;
import org.onlab.rest.BaseResource;
import org.onosproject.codec.CodecService;
import org.onosproject.codec.impl.CodecManager;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.core.IdGenerator;
import org.onosproject.net.NetworkResource;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.Key;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.google.common.base.MoreObjects;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Unit tests for Intents REST APIs.
 */
@Ignore
public class IntentsResourceTest extends ResourceTest {
    final IntentService mockIntentService = createMock(IntentService.class);
    final HashSet<Intent> intents = new HashSet<>();
    private static final ApplicationId APP_ID = new DefaultApplicationId(1, "test");
    private IdGenerator mockGenerator;

    /**
     * Mock ID generator.  This should be refactored to share the one in
     * the core/api tests.
     */
    public class MockIdGenerator implements IdGenerator {
        private AtomicLong nextId = new AtomicLong(0);

        @Override
        public long getNewId() {
            return nextId.getAndIncrement();
        }
    }

    /**
     * Mock compilable intent class.
     */
    private static class MockIntent extends Intent {

        public MockIntent(Collection<NetworkResource> resources) {
            super(APP_ID, resources);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                    .add("id", id())
                    .add("appId", appId())
                    .toString();
        }
    }

    private class MockResource implements NetworkResource {
        int id;

        MockResource(int id) {
            this.id = id;
        }

        public String toString() {
            return "Resource " + Integer.toString(id);
        }
    }

    /**
     * Hamcrest matcher to check that an intent representation in JSON matches
     * the actual intent.
     */
    public static class IntentJsonMatcher extends TypeSafeMatcher<JsonObject> {
        private final Intent intent;
        private String reason = "";

        public IntentJsonMatcher(Intent intentValue) {
            intent = intentValue;
        }

        @Override
        public boolean matchesSafely(JsonObject jsonIntent) {
            // check id
            final String jsonId = jsonIntent.get("id").asString();
            if (!jsonId.equals(intent.id().toString())) {
                reason = "id " + intent.id().toString();
                return false;
            }

            // check application id
            final String jsonAppId = jsonIntent.get("appId").asString();
            if (!jsonAppId.equals(intent.appId().toString())) {
                reason = "appId " + intent.appId().toString();
                return false;
            }

            // check intent type
            final String jsonType = jsonIntent.get("type").asString();
            if (!jsonType.equals("MockIntent")) {
                reason = "type MockIntent";
                return false;
            }

            // check details field
            final String jsonDetails = jsonIntent.get("details").asString();
            if (!jsonDetails.equals(intent.toString())) {
                reason = "details " + intent.toString();
                return false;
            }

            // check resources array
            final JsonArray jsonResources = jsonIntent.get("resources").asArray();
            if (intent.resources() != null) {
                if (intent.resources().size() != jsonResources.size()) {
                    reason = "resources array size of " + Integer.toString(intent.resources().size());
                    return false;
                }
                for (final NetworkResource resource : intent.resources()) {
                    boolean resourceFound = false;
                    final String resourceString = resource.toString();
                    for (int resourceIndex = 0; resourceIndex < jsonResources.size(); resourceIndex++) {
                        final JsonValue value = jsonResources.get(resourceIndex);
                        if (value.asString().equals(resourceString)) {
                            resourceFound = true;
                        }
                    }
                    if (!resourceFound) {
                        reason = "resource " + resourceString;
                        return false;
                    }
                }
            } else if (jsonResources.size() != 0) {
                reason = "resources array empty";
                return false;
            }
            return true;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(reason);
        }
    }

    /**
     * Factory to allocate an intent matcher.
     *
     * @param intent intent object we are looking for
     * @return matcher
     */
    private static IntentJsonMatcher matchesIntent(Intent intent) {
        return new IntentJsonMatcher(intent);
    }

    /**
     * Hamcrest matcher to check that an intent is represented properly in a JSON
     * array of intents.
     */
    public static class IntentJsonArrayMatcher extends TypeSafeMatcher<JsonArray> {
        private final Intent intent;
        private String reason = "";

        public IntentJsonArrayMatcher(Intent intentValue) {
            intent = intentValue;
        }

        @Override
        public boolean matchesSafely(JsonArray json) {
            boolean intentFound = false;
            final int expectedAttributes = 5;
            for (int jsonIntentIndex = 0; jsonIntentIndex < json.size();
                 jsonIntentIndex++) {

                final JsonObject jsonIntent = json.get(jsonIntentIndex).asObject();

                if (jsonIntent.names().size() != expectedAttributes) {
                    reason = "Found an intent with the wrong number of attributes";
                    return false;
                }

                final String jsonIntentId = jsonIntent.get("id").asString();
                if (jsonIntentId.equals(intent.id().toString())) {
                    intentFound = true;

                    //  We found the correct intent, check attribute values
                    assertThat(jsonIntent, matchesIntent(intent));
                }
            }
            if (!intentFound) {
                reason = "Intent with id " + intent.id().toString() + " not found";
                return false;
            } else {
                return true;
            }
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(reason);
        }
    }

    /**
     * Factory to allocate an intent array matcher.
     *
     * @param intent intent object we are looking for
     * @return matcher
     */
    private static IntentJsonArrayMatcher hasIntent(Intent intent) {
        return new IntentJsonArrayMatcher(intent);
    }

    /**
     * Initializes test mocks and environment.
     */
    @Before
    public void setUpTest() {
        expect(mockIntentService.getIntents()).andReturn(intents).anyTimes();

        // Register the services needed for the test
        final CodecManager codecService =  new CodecManager();
        codecService.activate();
        ServiceDirectory testDirectory =
                new TestServiceDirectory()
                        .add(IntentService.class, mockIntentService)
                        .add(CodecService.class, codecService);

        BaseResource.setServiceDirectory(testDirectory);

        mockGenerator = new MockIdGenerator();
        Intent.bindIdGenerator(mockGenerator);
    }

    /**
     * Tears down and verifies test mocks and environment.
     */
    @After
    public void tearDownTest() {
        verify(mockIntentService);
        Intent.unbindIdGenerator(mockGenerator);
    }

    /**
     * Tests the result of the rest api GET when there are no intents.
     */
    @Test
    public void testIntentsEmptyArray() {
        replay(mockIntentService);
        final WebResource rs = resource();
        final String response = rs.path("intents").get(String.class);
        assertThat(response, is("{\"intents\":[]}"));
    }

    /**
     * Tests the result of the rest api GET when intents are defined.
     */
    @Test
    public void testIntentsArray() {
        replay(mockIntentService);

        final Intent intent1 = new MockIntent(Collections.emptyList());
        final HashSet<NetworkResource> resources = new HashSet<>();
        resources.add(new MockResource(1));
        resources.add(new MockResource(2));
        resources.add(new MockResource(3));
        final Intent intent2 = new MockIntent(resources);

        intents.add(intent1);
        intents.add(intent2);
        final WebResource rs = resource();
        final String response = rs.path("intents").get(String.class);
        assertThat(response, containsString("{\"intents\":["));

        final JsonObject result = JsonObject.readFrom(response);
        assertThat(result, notNullValue());

        assertThat(result.names(), hasSize(1));
        assertThat(result.names().get(0), is("intents"));

        final JsonArray jsonIntents = result.get("intents").asArray();
        assertThat(jsonIntents, notNullValue());

        assertThat(jsonIntents, hasIntent(intent1));
        assertThat(jsonIntents, hasIntent(intent2));
    }

    /**
     * Tests the result of a rest api GET for a single intent.
     */
    @Test
    public void testIntentsSingle() {
        final HashSet<NetworkResource> resources = new HashSet<>();
        resources.add(new MockResource(1));
        resources.add(new MockResource(2));
        resources.add(new MockResource(3));
        final Intent intent = new MockIntent(resources);

        intents.add(intent);

        expect(mockIntentService.getIntent(Key.of(0, APP_ID)))
                .andReturn(intent)
                .anyTimes();
        replay(mockIntentService);

        final WebResource rs = resource();
        final String response = rs.path("intents/0").get(String.class);
        final JsonObject result = JsonObject.readFrom(response);
        assertThat(result, matchesIntent(intent));
    }

    /**
     * Tests that a fetch of a non-existent intent object throws an exception.
     */
    @Test
    public void testBadGet() {

        expect(mockIntentService.getIntent(Key.of(0, APP_ID)))
                .andReturn(null)
                .anyTimes();
        replay(mockIntentService);

        WebResource rs = resource();
        try {
            rs.path("intents/0").get(String.class);
            fail("Fetch of non-existent intent did not throw an exception");
        } catch (UniformInterfaceException ex) {
            assertThat(ex.getMessage(),
                    containsString("returned a response status of"));
        }
    }
}
