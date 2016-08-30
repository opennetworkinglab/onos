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

package org.onosproject.rest.resources;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.google.common.collect.ImmutableSet;
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
import org.onosproject.codec.impl.GroupCodec;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.core.DefaultGroupId;
import org.onosproject.core.GroupId;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.NetTestTools;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.group.DefaultGroupKey;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupBucket;
import org.onosproject.net.group.GroupBuckets;
import org.onosproject.net.group.GroupDescription;
import org.onosproject.net.group.GroupKey;
import org.onosproject.net.group.GroupService;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyShort;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.onosproject.net.NetTestTools.APP_ID;

/**
 * Unit tests for Groups REST APIs.
 */
public class GroupsResourceTest extends ResourceTest {
    final GroupService mockGroupService = createMock(GroupService.class);
    CoreService mockCoreService = createMock(CoreService.class);
    final DeviceService mockDeviceService = createMock(DeviceService.class);

    final HashMap<DeviceId, Set<Group>> groups = new HashMap<>();


    final DeviceId deviceId1 = DeviceId.deviceId("1");
    final DeviceId deviceId2 = DeviceId.deviceId("2");
    final DeviceId deviceId3 = DeviceId.deviceId("3");
    final Device device1 = new DefaultDevice(null, deviceId1, Device.Type.OTHER,
            "", "", "", "", null);
    final Device device2 = new DefaultDevice(null, deviceId2, Device.Type.OTHER,
            "", "", "", "", null);

    final MockGroup group1 = new MockGroup(deviceId1, 1, "0x111", 1);
    final MockGroup group2 = new MockGroup(deviceId1, 2, "0x222", 2);

    final MockGroup group3 = new MockGroup(deviceId2, 3, "0x333", 3);
    final MockGroup group4 = new MockGroup(deviceId2, 4, "0x444", 4);

    final MockGroup group5 = new MockGroup(deviceId3, 5, "0x555", 5);
    final MockGroup group6 = new MockGroup(deviceId3, 6, "0x666", 6);

    /**
     * Mock class for a group.
     */
    private static class MockGroup implements Group {

        final DeviceId deviceId;
        final ApplicationId appId;
        final GroupKey appCookie;
        final long baseValue;
        final List<GroupBucket> bucketList;
        GroupBuckets buckets;

        public MockGroup(DeviceId deviceId, int appId, String appCookie, int id) {
            this.deviceId = deviceId;
            this.appId = new DefaultApplicationId(appId, String.valueOf(appId));
            this.appCookie = new DefaultGroupKey(appCookie.getBytes());
            this.baseValue = id * 100;
            this.bucketList = new ArrayList<>();
            this.buckets = new GroupBuckets(bucketList);
        }

        @Override
        public GroupId id() {
            return new DefaultGroupId((int) baseValue + 55);
        }

        @Override
        public GroupState state() {
            return GroupState.ADDED;
        }

        @Override
        public long life() {
            return baseValue + 11;
        }

        @Override
        public long packets() {
            return baseValue + 22;
        }

        @Override
        public long bytes() {
            return baseValue + 33;
        }

        @Override
        public long referenceCount() {
            return baseValue + 44;
        }

        @Override
        public int age() {
            return 0;
        }

        @Override
        public Type type() {
            return GroupDescription.Type.ALL;
        }

        @Override
        public DeviceId deviceId() {
            return this.deviceId;
        }

        @Override
        public ApplicationId appId() {
            return this.appId;
        }

        @Override
        public GroupKey appCookie() {
            return this.appCookie;
        }

        @Override
        public Integer givenGroupId() {
            return (int) baseValue + 55;
        }

        @Override
        public GroupBuckets buckets() {
            return this.buckets;
        }
    }

    /**
     * Populates some groups used as testing data.
     */
    private void setupMockGroups() {
        final Set<Group> groups1 = new HashSet<>();
        groups1.add(group1);
        groups1.add(group2);

        final Set<Group> groups2 = new HashSet<>();
        groups2.add(group3);
        groups2.add(group4);

        groups.put(deviceId1, groups1);
        groups.put(deviceId2, groups2);

        expect(mockGroupService.getGroups(deviceId1))
                .andReturn(groups.get(deviceId1)).anyTimes();
        expect(mockGroupService.getGroups(deviceId2))
                .andReturn(groups.get(deviceId2)).anyTimes();
    }

    /**
     * Sets up the global values for all the tests.
     */
    @Before
    public void setUpTest() {
        // Mock device service
        expect(mockDeviceService.getDevice(deviceId1))
                .andReturn(device1);
        expect(mockDeviceService.getDevice(deviceId2))
                .andReturn(device2);
        expect(mockDeviceService.getDevices())
                .andReturn(ImmutableSet.of(device1, device2));

        // Mock Core Service
        expect(mockCoreService.getAppId(anyShort()))
                .andReturn(NetTestTools.APP_ID).anyTimes();
        expect(mockCoreService.registerApplication(GroupCodec.REST_APP_ID))
                .andReturn(APP_ID).anyTimes();
        replay(mockCoreService);

        // Register the services needed for the test
        final CodecManager codecService = new CodecManager();
        codecService.activate();
        ServiceDirectory testDirectory =
                new TestServiceDirectory()
                        .add(GroupService.class, mockGroupService)
                        .add(DeviceService.class, mockDeviceService)
                        .add(CodecService.class, codecService)
                        .add(CoreService.class, mockCoreService);

        BaseResource.setServiceDirectory(testDirectory);
    }

    /**
     * Cleans up and verifies the mocks.
     */
    @After
    public void tearDownTest() {
        verify(mockGroupService);
        verify(mockCoreService);
    }

    /**
     * Hamcrest matcher to check that a group representation in JSON matches
     * the actual group.
     */
    public static class GroupJsonMatcher extends TypeSafeMatcher<JsonObject> {
        private final Group group;
        private final String expectedAppId;
        private String reason = "";

        public GroupJsonMatcher(Group groupValue, String expectedAppIdValue) {
            group = groupValue;
            expectedAppId = expectedAppIdValue;
        }

        @Override
        public boolean matchesSafely(JsonObject jsonGroup) {
            // check id
            final String jsonId = jsonGroup.get("id").asString();
            final String groupId = group.id().id().toString();
            if (!jsonId.equals(groupId)) {
                reason = "id " + group.id().id().toString();
                return false;
            }

            // check application id
            final String jsonAppId = jsonGroup.get("appId").asString();
            final String appId = group.appId().name();
            if (!jsonAppId.equals(appId)) {
                reason = "appId " + group.appId().name();
                return false;
            }

            // check device id
            final String jsonDeviceId = jsonGroup.get("deviceId").asString();
            if (!jsonDeviceId.equals(group.deviceId().toString())) {
                reason = "deviceId " + group.deviceId();
                return false;
            }

            // check bucket array
            if (group.buckets().buckets() != null) {
                final JsonArray jsonBuckets = jsonGroup.get("buckets").asArray();
                if (group.buckets().buckets().size() != jsonBuckets.size()) {
                    reason = "buckets array size of " +
                            Integer.toString(group.buckets().buckets().size());
                    return false;
                }
                for (final GroupBucket groupBucket : group.buckets().buckets()) {
                    boolean groupBucketFound = false;
                    for (int groupBucketIndex = 0; groupBucketIndex < jsonBuckets.size(); groupBucketIndex++) {
                        final String jsonType = jsonBuckets.get(groupBucketIndex).asObject().get("type").asString();
                        final String bucketType = groupBucket.type().name();
                        if (jsonType.equals(bucketType)) {
                            groupBucketFound = true;
                        }
                    }
                    if (!groupBucketFound) {
                        reason = "group bucket " + groupBucket.toString();
                        return false;
                    }
                }
            }

            return true;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(reason);
        }
    }

    /**
     * Factory to allocate a group matcher.
     *
     * @param group group object we are looking for
     * @return matcher
     */
    private static GroupJsonMatcher matchesGroup(Group group, String expectedAppName) {
        return new GroupJsonMatcher(group, expectedAppName);
    }

    /**
     * Hamcrest matcher to check that a group is represented properly in a JSON
     * array of flows.
     */
    public static class GroupJsonArrayMatcher extends TypeSafeMatcher<JsonArray> {
        private final Group group;
        private String reason = "";

        public GroupJsonArrayMatcher(Group groupValue) {
            group = groupValue;
        }

        @Override
        public boolean matchesSafely(JsonArray json) {
            boolean groupFound = false;
            for (int jsonGroupIndex = 0; jsonGroupIndex < json.size();
                 jsonGroupIndex++) {

                final JsonObject jsonGroup = json.get(jsonGroupIndex).asObject();

                final String groupId = group.id().id().toString();
                final String jsonGroupId = jsonGroup.get("id").asString();
                if (jsonGroupId.equals(groupId)) {
                    groupFound = true;

                    //  We found the correct group, check attribute values
                    assertThat(jsonGroup, matchesGroup(group, APP_ID.name()));
                }
            }
            if (!groupFound) {
                reason = "Group with id " + group.id().id().toString() + " not found";
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
     * Factory to allocate a group array matcher.
     *
     * @param group group object we are looking for
     * @return matcher
     */
    private static GroupJsonArrayMatcher hasGroup(Group group) {
        return new GroupJsonArrayMatcher(group);
    }

    /**
     * Tests the result of the rest api GET when there are no groups.
     */
    @Test
    public void testGroupsEmptyArray() {
        expect(mockGroupService.getGroups(deviceId1)).andReturn(null).anyTimes();
        expect(mockGroupService.getGroups(deviceId2)).andReturn(null).anyTimes();
        replay(mockGroupService);
        replay(mockDeviceService);
        final WebTarget wt = target();
        final String response = wt.path("groups").request().get(String.class);
        assertThat(response, is("{\"groups\":[]}"));
    }

    /**
     * Tests the result of the rest api GET when there are active groups.
     */
    @Test
    public void testGroupsPopulatedArray() {
        setupMockGroups();
        replay(mockGroupService);
        replay(mockDeviceService);
        final WebTarget wt = target();
        final String response = wt.path("groups").request().get(String.class);
        final JsonObject result = Json.parse(response).asObject();
        assertThat(result, notNullValue());

        assertThat(result.names(), hasSize(1));
        assertThat(result.names().get(0), is("groups"));
        final JsonArray jsonGroups = result.get("groups").asArray();
        assertThat(jsonGroups, notNullValue());
        assertThat(jsonGroups, hasGroup(group1));
        assertThat(jsonGroups, hasGroup(group2));
        assertThat(jsonGroups, hasGroup(group3));
        assertThat(jsonGroups, hasGroup(group4));
    }

    /**
     * Tests the result of a rest api GET for a device.
     */
    @Test
    public void testGroupsSingleDevice() {
        setupMockGroups();
        final Set<Group> groups = new HashSet<>();
        groups.add(group5);
        groups.add(group6);
        expect(mockGroupService.getGroups(anyObject()))
                .andReturn(groups).anyTimes();
        replay(mockGroupService);
        replay(mockDeviceService);
        final WebTarget wt = target();
        final String response = wt.path("groups/" + deviceId3).request().get(String.class);
        final JsonObject result = Json.parse(response).asObject();
        assertThat(result, notNullValue());

        assertThat(result.names(), hasSize(1));
        assertThat(result.names().get(0), is("groups"));
        final JsonArray jsonGroups = result.get("groups").asArray();
        assertThat(jsonGroups, notNullValue());
        assertThat(jsonGroups, hasGroup(group5));
        assertThat(jsonGroups, hasGroup(group6));
    }

    /**
     * Test the result of a rest api GET with specifying device id and appcookie.
     */
    @Test
    public void testGroupByDeviceIdAndAppCookie() {
        setupMockGroups();
        expect(mockGroupService.getGroup(anyObject(), anyObject()))
                .andReturn(group5).anyTimes();
        replay(mockGroupService);
        final WebTarget wt = target();
        final String response = wt.path("groups/" + deviceId3 + "/" + "0x111")
                .request().get(String.class);
        final JsonObject result = Json.parse(response).asObject();
        assertThat(result, notNullValue());

        assertThat(result.names(), hasSize(1));
        assertThat(result.names().get(0), is("groups"));
        final JsonArray jsonFlows = result.get("groups").asArray();
        assertThat(jsonFlows, notNullValue());
        assertThat(jsonFlows, hasGroup(group5));
    }

    /**
     * Test whether the REST API returns 404 if no entry has been found.
     */
    @Test
    public void testGroupByDeviceIdAndAppCookieNull() {
        setupMockGroups();
        expect(mockGroupService.getGroup(anyObject(), anyObject()))
                .andReturn(null).anyTimes();
        replay(mockGroupService);
        final WebTarget wt = target();
        final Response response = wt.path("groups/" + deviceId3 + "/" + "0x222").request().get();

        assertEquals(404, response.getStatus());
    }

    /**
     * Tests creating a group with POST.
     */
    @Test
    public void testPost() {
        mockGroupService.addGroup(anyObject());
        expectLastCall();
        replay(mockGroupService);

        WebTarget wt = target();
        InputStream jsonStream = GroupsResourceTest.class
                .getResourceAsStream("post-group.json");

        Response response = wt.path("groups/of:0000000000000001")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(jsonStream));
        assertThat(response.getStatus(), is(HttpURLConnection.HTTP_CREATED));
        String location = response.getLocation().getPath();
        assertThat(location, Matchers.startsWith("/groups/of:0000000000000001/"));
    }

    /**
     * Tests deleting a group.
     */
    @Test
    public void testDelete() {
        setupMockGroups();
        mockGroupService.removeGroup(anyObject(), anyObject(), anyObject());
        expectLastCall();
        replay(mockGroupService);

        WebTarget wt = target();

        String location = "/groups/1/0x111";

        Response deleteResponse = wt.path(location)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .delete();
        assertThat(deleteResponse.getStatus(),
                is(HttpURLConnection.HTTP_NO_CONTENT));
    }
}
