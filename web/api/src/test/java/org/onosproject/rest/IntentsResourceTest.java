/*
 * Copyright 2014-2015 Open Networking Laboratory
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

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import org.hamcrest.Description;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.osgi.TestServiceDirectory;
import org.onlab.rest.BaseResource;
import org.onosproject.codec.CodecService;
import org.onosproject.codec.impl.CodecManager;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.core.IdGenerator;
import org.onosproject.net.NetworkResource;
import org.onosproject.net.intent.FakeIntentManager;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.IntentState;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.MockIdGenerator;
import org.onosproject.rest.resources.CoreWebApplication;

import javax.ws.rs.core.MediaType;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.HashSet;

import static org.easymock.EasyMock.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.onosproject.net.intent.IntentTestsMocks.MockIntent;

/**
 * Unit tests for Intents REST APIs.
 */
public class IntentsResourceTest extends ResourceTest {
    final IntentService mockIntentService = createMock(IntentService.class);
    final CoreService mockCoreService = createMock(CoreService.class);
    final HashSet<Intent> intents = new HashSet<>();
    private static final ApplicationId APP_ID = new DefaultApplicationId(1, "test");
    private IdGenerator mockGenerator;

    public IntentsResourceTest() {
        super(CoreWebApplication.class);
    }

    private class MockResource implements NetworkResource {
        int id;

        MockResource(int id) {
            this.id = id;
        }

        @Override
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
            final String appId = intent.appId().name();
            if (!jsonAppId.equals(appId)) {
                reason = "appId was " + jsonAppId;
                return false;
            }

            // check intent type
            final String jsonType = jsonIntent.get("type").asString();
            if (!jsonType.equals("MockIntent")) {
                reason = "type MockIntent";
                return false;
            }

            // check state field
            final String jsonState = jsonIntent.get("state").asString();
            if (!jsonState.equals("INSTALLED")) {
                reason = "state INSTALLED";
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
        expect(mockIntentService.getIntentState(anyObject()))
                .andReturn(IntentState.INSTALLED)
                .anyTimes();
        // Register the services needed for the test
        final CodecManager codecService =  new CodecManager();
        codecService.activate();
        ServiceDirectory testDirectory =
                new TestServiceDirectory()
                        .add(IntentService.class, mockIntentService)
                        .add(CodecService.class, codecService)
                        .add(CoreService.class, mockCoreService);

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

        final Intent intent1 = new MockIntent(1L, Collections.emptyList());
        final HashSet<NetworkResource> resources = new HashSet<>();
        resources.add(new MockResource(1));
        resources.add(new MockResource(2));
        resources.add(new MockResource(3));
        final Intent intent2 = new MockIntent(2L, resources);

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
        final Intent intent = new MockIntent(3L, resources);

        intents.add(intent);

        expect(mockIntentService.getIntent(Key.of(0, APP_ID)))
                .andReturn(intent)
                .anyTimes();
        expect(mockIntentService.getIntent(Key.of("0", APP_ID)))
                .andReturn(intent)
                .anyTimes();
        expect(mockIntentService.getIntent(Key.of(0, APP_ID)))
                .andReturn(intent)
                .anyTimes();
        expect(mockIntentService.getIntent(Key.of("0x0", APP_ID)))
                .andReturn(null)
                .anyTimes();
        replay(mockIntentService);
        expect(mockCoreService.getAppId(APP_ID.name()))
                .andReturn(APP_ID).anyTimes();
        replay(mockCoreService);
        final WebResource rs = resource();

        // Test get using key string
        final String response = rs.path("intents/" + APP_ID.name()
                + "/0").get(String.class);
        final JsonObject result = JsonObject.readFrom(response);
        assertThat(result, matchesIntent(intent));

        // Test get using numeric value
        final String responseNumeric = rs.path("intents/" + APP_ID.name()
                + "/0x0").get(String.class);
        final JsonObject resultNumeric = JsonObject.readFrom(responseNumeric);
        assertThat(resultNumeric, matchesIntent(intent));
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

    /**
     * Tests creating an intent with POST.
     */
    @Test
    public void testPost() {
        ApplicationId testId = new DefaultApplicationId(2, "myApp");
        expect(mockCoreService.getAppId("myApp"))
                .andReturn(testId);
        replay(mockCoreService);

        mockIntentService.submit(anyObject());
        expectLastCall();
        replay(mockIntentService);

        InputStream jsonStream = IntentsResourceTest.class
                .getResourceAsStream("post-intent.json");
        WebResource rs = resource();

        ClientResponse response = rs.path("intents")
                .type(MediaType.APPLICATION_JSON_TYPE)
                .post(ClientResponse.class, jsonStream);
        assertThat(response.getStatus(), is(HttpURLConnection.HTTP_CREATED));
        String location = response.getLocation().getPath();
        assertThat(location, Matchers.startsWith("/intents/myApp/"));
    }

    /**
     * Tests creating an intent with POST and illegal JSON.
     */
    @Test
    public void testBadPost() {
        replay(mockCoreService);
        replay(mockIntentService);

        String json = "this is invalid!";
        WebResource rs = resource();

        ClientResponse response = rs.path("intents")
                .type(MediaType.APPLICATION_JSON_TYPE)
                .post(ClientResponse.class, json);
        assertThat(response.getStatus(), is(HttpURLConnection.HTTP_BAD_REQUEST));
    }

    /**
     * Tests removing an intent with DELETE.
     */
    @Test
    public void testRemove() {
        final HashSet<NetworkResource> resources = new HashSet<>();
        resources.add(new MockResource(1));
        resources.add(new MockResource(2));
        resources.add(new MockResource(3));
        final Intent intent = new MockIntent(3L, resources);
        final ApplicationId appId = new DefaultApplicationId(2, "app");
        IntentService fakeManager = new FakeIntentManager();

        expect(mockCoreService.getAppId("app"))
                .andReturn(appId).once();
        replay(mockCoreService);

        mockIntentService.withdraw(anyObject());
        expectLastCall().andDelegateTo(fakeManager).once();
        expect(mockIntentService.getIntent(Key.of(2, appId)))
                .andReturn(intent)
                .once();
        expect(mockIntentService.getIntent(Key.of("0x2", appId)))
                .andReturn(null)
                .once();

        mockIntentService.addListener(anyObject());
        expectLastCall().andDelegateTo(fakeManager).once();
        mockIntentService.removeListener(anyObject());
        expectLastCall().andDelegateTo(fakeManager).once();

        replay(mockIntentService);

        WebResource rs = resource();

        ClientResponse response = rs.path("intents/app/0x2")
                .type(MediaType.APPLICATION_JSON_TYPE)
                .delete(ClientResponse.class);
        assertThat(response.getStatus(), is(HttpURLConnection.HTTP_NO_CONTENT));
    }

    /**
     * Tests removal of a non existent intent with DELETE.
     */
    @Test
    public void testBadRemove() {
        final ApplicationId appId = new DefaultApplicationId(2, "app");

        expect(mockCoreService.getAppId("app"))
                .andReturn(appId).once();
        replay(mockCoreService);

        expect(mockIntentService.getIntent(Key.of(2, appId)))
                .andReturn(null)
                .once();
        expect(mockIntentService.getIntent(Key.of("0x2", appId)))
                .andReturn(null)
                .once();

        replay(mockIntentService);

        WebResource rs = resource();

        ClientResponse response = rs.path("intents/app/0x2")
                .type(MediaType.APPLICATION_JSON_TYPE)
                .delete(ClientResponse.class);
        assertThat(response.getStatus(), is(HttpURLConnection.HTTP_NO_CONTENT));
    }

}
