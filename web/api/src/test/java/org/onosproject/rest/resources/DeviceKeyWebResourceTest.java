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

package org.onosproject.rest.resources;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import org.hamcrest.Description;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Test;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.osgi.TestServiceDirectory;
import org.onosproject.codec.CodecService;
import org.onosproject.codec.impl.CodecManager;
import org.onosproject.net.key.DeviceKey;
import org.onosproject.net.key.DeviceKeyAdminService;
import org.onosproject.net.key.DeviceKeyId;
import org.onosproject.net.key.DeviceKeyService;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.HashSet;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Unit tests for device key REST APIs.
 */
public class DeviceKeyWebResourceTest extends ResourceTest {

    final DeviceKeyService mockDeviceKeyService = createMock(DeviceKeyService.class);
    final DeviceKeyAdminService mockDeviceKeyAdminService = createMock(DeviceKeyAdminService.class);

    final HashSet<DeviceKey> deviceKeySet = new HashSet<>();

    private static final String ID = "id";
    private static final String TYPE = "type";
    private static final String LABEL = "label";
    private static final String COMMUNITY_NAME = "community_name";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";

    private final String deviceKeyId1 = "DeviceKeyId1";
    private final String deviceKeyId2 = "DeviceKeyId2";
    private final String deviceKeyId3 = "DeviceKeyId3";
    private final String deviceKeyId4 = "DeviceKeyId4";
    private final String deviceKeyLabel = "DeviceKeyLabel";
    private final String deviceKeyCommunityName = "DeviceKeyCommunityName";
    private final String deviceKeyUsername = "DeviceKeyUsername";
    private final String deviceKeyPassword = "DeviceKeyPassword";

    private final DeviceKey deviceKey1 = DeviceKey.createDeviceKeyUsingCommunityName(
            DeviceKeyId.deviceKeyId(deviceKeyId1), deviceKeyLabel, deviceKeyCommunityName);
    private final DeviceKey deviceKey2 = DeviceKey.createDeviceKeyUsingUsernamePassword(
            DeviceKeyId.deviceKeyId(deviceKeyId2), null, deviceKeyUsername, deviceKeyPassword);
    private final DeviceKey deviceKey3 = DeviceKey.createDeviceKeyUsingUsernamePassword(
            DeviceKeyId.deviceKeyId(deviceKeyId3), null, null, null);
    private final DeviceKey deviceKey4 = DeviceKey.createDeviceKeyUsingCommunityName(
            DeviceKeyId.deviceKeyId(deviceKeyId4), null, null);

    /**
     * Initializes test mocks and environment.
     */
    @Before
    public void setUpMocks() {
        expect(mockDeviceKeyService.getDeviceKeys()).andReturn(deviceKeySet).anyTimes();

        // Register the services needed for the test
        CodecManager codecService = new CodecManager();
        codecService.activate();
        ServiceDirectory testDirectory =
                new TestServiceDirectory()
                        .add(DeviceKeyService.class, mockDeviceKeyService)
                        .add(DeviceKeyAdminService.class, mockDeviceKeyAdminService)
                        .add(CodecService.class, codecService);

        setServiceDirectory(testDirectory);
    }

    /**
     * Hamcrest matcher to check that a device key representation in JSON matches
     * the actual device key.
     */
    public static class DeviceKeyJsonMatcher extends TypeSafeMatcher<JsonObject> {
        private final DeviceKey deviceKey;
        private String reason = "";

        public DeviceKeyJsonMatcher(DeviceKey deviceKeyValue) {
            deviceKey = deviceKeyValue;
        }

        @Override
        public boolean matchesSafely(JsonObject jsonHost) {
            // Check the device key id
            final String jsonId = jsonHost.get(ID).asString();
            if (!jsonId.equals(deviceKey.deviceKeyId().id().toString())) {
                reason = ID + " " + deviceKey.deviceKeyId().id().toString();
                return false;
            }

            // Check the device key label
            final String jsonLabel = (jsonHost.get(LABEL).isNull()) ? null : jsonHost.get(LABEL).asString();
            if (deviceKey.label() != null) {
                if ((jsonLabel == null) || !jsonLabel.equals(deviceKey.label())) {
                    reason = LABEL + " " + deviceKey.label();
                    return false;
                }
            }

            // Check the device key type
            final String jsonType = jsonHost.get(TYPE).asString();
            if (!jsonType.equals(deviceKey.type().toString())) {
                reason = TYPE + " " + deviceKey.type().toString();
                return false;
            }

            if (jsonType.equals(DeviceKey.Type.COMMUNITY_NAME.toString())) {
                // Check the device key community name
                final String jsonCommunityName = jsonHost.get(COMMUNITY_NAME).isNull() ?
                        null : jsonHost.get(COMMUNITY_NAME).asString();
                if (deviceKey.asCommunityName().name() != null) {
                    if (!jsonCommunityName.equals(deviceKey.asCommunityName().name().toString())) {
                        reason = COMMUNITY_NAME + " " + deviceKey.asCommunityName().name().toString();
                        return false;
                    }
                }
            } else if (jsonType.equals(DeviceKey.Type.USERNAME_PASSWORD.toString())) {
                // Check the device key username
                final String jsonUsername = jsonHost.get(USERNAME).isNull() ?
                        null : jsonHost.get(USERNAME).asString();
                if (deviceKey.asUsernamePassword().username() != null) {
                    if (!jsonUsername.equals(deviceKey.asUsernamePassword().username().toString())) {
                        reason = USERNAME + " " + deviceKey.asUsernamePassword().username().toString();
                        return false;
                    }
                }

                // Check the device key password
                final String jsonPassword = jsonHost.get(PASSWORD).isNull() ?
                        null : jsonHost.get(PASSWORD).asString();
                if (deviceKey.asUsernamePassword().password() != null) {
                    if (!jsonPassword.equals(deviceKey.asUsernamePassword().password().toString())) {
                        reason = PASSWORD + " " + deviceKey.asUsernamePassword().password().toString();
                        return false;
                    }
                }
            } else {
                reason = "Unknown " + TYPE + " " + deviceKey.type().toString();
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
     * Factory to allocate a device key array matcher.
     *
     * @param deviceKey device key object we are looking for
     * @return matcher
     */
    private static DeviceKeyJsonMatcher matchesDeviceKey(DeviceKey deviceKey) {
        return new DeviceKeyJsonMatcher(deviceKey);
    }

    /**
     * Hamcrest matcher to check that a device key is represented properly in a JSON
     * array of device keys.
     */
    public static class DeviceKeyJsonArrayMatcher extends TypeSafeMatcher<JsonArray> {
        private final DeviceKey deviceKey;
        private String reason = "";

        public DeviceKeyJsonArrayMatcher(DeviceKey deviceKeyValue) {
            deviceKey = deviceKeyValue;
        }

        @Override
        public boolean matchesSafely(JsonArray json) {
            boolean deviceKeyFound = false;
            final int expectedAttributes = 5;
            for (int jsonDeviceKeyIndex = 0; jsonDeviceKeyIndex < json.size();
                 jsonDeviceKeyIndex++) {

                final JsonObject jsonHost = json.get(jsonDeviceKeyIndex).asObject();

                // Device keys can have a variable number of attribute so we check
                // that there is a minimum number.
                if (jsonHost.names().size() < expectedAttributes) {
                    reason = "Found a device key with the wrong number of attributes";
                    return false;
                }

                final String jsonDeviceKeyId = jsonHost.get(ID).asString();
                if (jsonDeviceKeyId.equals(deviceKey.deviceKeyId().id().toString())) {
                    deviceKeyFound = true;

                    //  We found the correct device key, check the device key attribute values
                    assertThat(jsonHost, matchesDeviceKey(deviceKey));
                }
            }
            if (!deviceKeyFound) {
                reason = "Device key with id " + deviceKey.deviceKeyId().id().toString() + " was not found";
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
     * Factory to allocate a device key array matcher.
     *
     * @param deviceKey device key object we are looking for
     * @return matcher
     */
    private static DeviceKeyJsonArrayMatcher hasDeviceKey(DeviceKey deviceKey) {
        return new DeviceKeyJsonArrayMatcher(deviceKey);
    }

    /**
     * Tests the result of the REST API GET when there are no device keys.
     */
    @Test
    public void testGetDeviceKeysEmptyArray() {
        replay(mockDeviceKeyService);

        WebTarget wt = target();
        String response = wt.path("keys").request().get(String.class);
        assertThat(response, is("{\"keys\":[]}"));

        verify(mockDeviceKeyService);
    }

    /**
     * Tests the result of the REST API GET when device keys are defined.
     */
    @Test
    public void testGetDeviceKeysArray() {
        replay(mockDeviceKeyService);
        deviceKeySet.add(deviceKey1);
        deviceKeySet.add(deviceKey2);
        deviceKeySet.add(deviceKey3);
        deviceKeySet.add(deviceKey4);

        WebTarget wt = target();
        String response = wt.path("keys").request().get(String.class);
        assertThat(response, containsString("{\"keys\":["));

        final JsonObject result = Json.parse(response).asObject();
        assertThat(result, notNullValue());

        assertThat(result.names(), hasSize(1));
        assertThat(result.names().get(0), is("keys"));

        final JsonArray deviceKeys = result.get("keys").asArray();
        assertThat(deviceKeys, notNullValue());
        assertEquals("Device keys array is not the correct size.", 4, deviceKeys.size());

        assertThat(deviceKeys, hasDeviceKey(deviceKey1));
        assertThat(deviceKeys, hasDeviceKey(deviceKey2));
        assertThat(deviceKeys, hasDeviceKey(deviceKey3));
        assertThat(deviceKeys, hasDeviceKey(deviceKey4));

        verify(mockDeviceKeyService);
    }

    /**
     * Tests the result of the REST API GET using a device key identifier.
     */
    @Test
    public void testGetDeviceKeyById() {
        deviceKeySet.add(deviceKey1);

        expect(mockDeviceKeyService.getDeviceKey(DeviceKeyId.deviceKeyId(deviceKeyId1)))
                .andReturn(deviceKey1)
                .anyTimes();
        replay(mockDeviceKeyService);

        WebTarget wt = target();
        String response = wt.path("keys/" + deviceKeyId1).request().get(String.class);
        final JsonObject result = Json.parse(response).asObject();
        assertThat(result, notNullValue());

        assertThat(result, matchesDeviceKey(deviceKey1));

        verify(mockDeviceKeyService);
    }

    /**
     * Tests that a GET of a non-existent object throws an exception.
     */
    @Test
    public void testGetNonExistentDeviceKey() {

        expect(mockDeviceKeyService.getDeviceKey(DeviceKeyId.deviceKeyId(deviceKeyId1)))
                .andReturn(null)
                .anyTimes();
        replay(mockDeviceKeyService);

        WebTarget wt = target();
        try {
            wt.path("keys/" + deviceKeyId1).request().get(String.class);
            fail("GET of a non-existent device key did not throw an exception");
        } catch (NotFoundException ex) {
            assertThat(ex.getMessage(), containsString("HTTP 404 Not Found"));
        }

        verify(mockDeviceKeyService);
    }

    /**
     * Tests adding of new device key using POST via JSON stream.
     */
    @Test
    public void testPost() {

        mockDeviceKeyAdminService.addKey(anyObject());
        expectLastCall();

        replay(mockDeviceKeyAdminService);

        WebTarget wt = target();
        InputStream jsonStream = DeviceKeyWebResourceTest.class
                .getResourceAsStream("post-device-key.json");

        Response response = wt.path("keys").request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(jsonStream));
        assertThat(response.getStatus(), is(HttpURLConnection.HTTP_CREATED));

        String location = response.getLocation().getPath();
        assertThat(location, Matchers.startsWith("/keys/" + deviceKeyId3));

        verify(mockDeviceKeyAdminService);
    }

    /**
     * Tests adding of a null device key using POST via JSON stream.
     */
    @Test
    public void testPostNullDeviceKey() {

        replay(mockDeviceKeyAdminService);

        WebTarget wt = target();
        try {
            wt.path("keys").request(MediaType.APPLICATION_JSON_TYPE)
                    .post(Entity.json(null), String.class);
            fail("POST of null device key did not throw an exception");
        } catch (BadRequestException ex) {
            assertThat(ex.getMessage(), containsString("HTTP 400 Bad Request"));
        } catch (InternalServerErrorException ex) {
            assertThat(ex.getMessage(), containsString("HTTP 500 Internal Server Error"));
        }

        verify(mockDeviceKeyAdminService);
    }

    /**
     * Tests removing a device key with DELETE request.
     */
    @Test
    public void testDelete() {
        expect(mockDeviceKeyService.getDeviceKey(DeviceKeyId.deviceKeyId(deviceKeyId2)))
                .andReturn(deviceKey2)
                .anyTimes();
        mockDeviceKeyAdminService.removeKey(anyObject());
        expectLastCall();

        replay(mockDeviceKeyService);
        replay(mockDeviceKeyAdminService);

        WebTarget wt = target();

        Response response = wt.path("keys/" + deviceKeyId2)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .delete();
        assertThat(response.getStatus(), is(HttpURLConnection.HTTP_OK));

        verify(mockDeviceKeyService);
        verify(mockDeviceKeyAdminService);
    }

    /**
     * Tests that a DELETE of a non-existent device key throws an exception.
     */
    @Test
    public void testDeleteNonExistentDeviceKey() {
        expect(mockDeviceKeyService.getDeviceKey(anyObject()))
                .andReturn(null)
                .anyTimes();

        expectLastCall();

        replay(mockDeviceKeyService);
        replay(mockDeviceKeyAdminService);

        WebTarget wt = target();

        try {
            wt.path("keys/" + "NON_EXISTENT_DEVICE_KEY").request()
                    .delete(String.class);
            fail("Delete of a non-existent device key did not throw an exception");
        } catch (NotFoundException ex) {
            assertThat(ex.getMessage(), containsString("HTTP 404 Not Found"));
        }

        verify(mockDeviceKeyService);
        verify(mockDeviceKeyAdminService);
    }
}
