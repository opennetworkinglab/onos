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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.glassfish.jersey.client.ClientProperties;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Test;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.osgi.TestServiceDirectory;
import org.onosproject.cluster.NodeId;
import org.onosproject.codec.CodecService;
import org.onosproject.codec.impl.CodecManager;
import org.onosproject.net.Annotations;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DeviceId;
import org.onosproject.net.region.Region;
import org.onosproject.net.region.RegionAdminService;
import org.onosproject.net.region.RegionId;
import org.onosproject.net.region.RegionService;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Set;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Unit tests for region REST APIs.
 */
public class RegionsResourceTest extends ResourceTest {

    final RegionService mockRegionService = createMock(RegionService.class);
    final RegionAdminService mockRegionAdminService = createMock(RegionAdminService.class);

    final RegionId regionId1 = RegionId.regionId("1");
    final RegionId regionId2 = RegionId.regionId("2");
    final RegionId regionId3 = RegionId.regionId("3");

    final MockRegion region1 = new MockRegion(regionId1, "r1", Region.Type.RACK);
    final MockRegion region2 = new MockRegion(regionId2, "r2", Region.Type.ROOM);
    final MockRegion region3 = new MockRegion(regionId3, "r3", Region.Type.CAMPUS);

    /**
     * Mock class for a region.
     */
    private static class MockRegion implements Region {

        private final RegionId id;
        private final String name;
        private final Type type;
        private final List<Set<NodeId>> masters;

        public MockRegion(RegionId id, String name, Type type) {
            this.id = id;
            this.name = name;
            this.type = type;

            final NodeId nodeId1 = NodeId.nodeId("1");
            final NodeId nodeId2 = NodeId.nodeId("2");
            final NodeId nodeId3 = NodeId.nodeId("3");
            final NodeId nodeId4 = NodeId.nodeId("4");

            Set<NodeId> nodeIds1 = ImmutableSet.of(nodeId1);
            Set<NodeId> nodeIds2 = ImmutableSet.of(nodeId1, nodeId2);
            Set<NodeId> nodeIds3 = ImmutableSet.of(nodeId1, nodeId2, nodeId3);
            Set<NodeId> nodeIds4 = ImmutableSet.of(nodeId1, nodeId2, nodeId3, nodeId4);

            this.masters = ImmutableList.of(nodeIds1, nodeIds2, nodeIds3, nodeIds4);
        }

        @Override
        public RegionId id() {
            return this.id;
        }

        @Override
        public String name() {
            return this.name;
        }

        @Override
        public Type type() {
            return this.type;
        }

        @Override
        public List<Set<NodeId>> masters() {
            return this.masters;
        }

        @Override
        public Annotations annotations() {
            return DefaultAnnotations.EMPTY;
        }
    }

    /**
     * Sets up the global values for all the tests.
     */
    @Before
    public void setupTest() {
        final CodecManager codecService = new CodecManager();
        codecService.activate();
        ServiceDirectory testDirectory =
                new TestServiceDirectory()
                .add(RegionService.class, mockRegionService)
                .add(RegionAdminService.class, mockRegionAdminService)
                .add(CodecService.class, codecService);
        setServiceDirectory(testDirectory);
    }

    /**
     * Hamcrest matcher to check that a region representation in JSON matches
     * the actual region.
     */
    public static class RegionJsonMatcher extends TypeSafeMatcher<JsonObject> {
        private final Region region;
        private String reason = "";

        public RegionJsonMatcher(Region regionValue) {
            this.region = regionValue;
        }

        @Override
        protected boolean matchesSafely(JsonObject jsonRegion) {

            // check id
            String jsonRegionId = jsonRegion.get("id").asString();
            String regionId = region.id().toString();
            if (!jsonRegionId.equals(regionId)) {
                reason = "region id was " + jsonRegionId;
                return false;
            }

            // check type
            String jsonType = jsonRegion.get("type").asString();
            String type = region.type().toString();
            if (!jsonType.equals(type)) {
                reason = "type was " + jsonType;
                return false;
            }

            // check name
            String jsonName = jsonRegion.get("name").asString();
            String name = region.name();
            if (!jsonName.equals(name)) {
                reason = "name was " + jsonName;
                return false;
            }

            // check size of master array
            JsonArray jsonMasters = jsonRegion.get("masters").asArray();
            if (jsonMasters.size() != region.masters().size()) {
                reason = "masters size was " + jsonMasters.size();
                return false;
            }

            // check master
            for (Set<NodeId> set : region.masters()) {
                boolean masterFound = false;
                for (int masterIndex = 0; masterIndex < jsonMasters.size(); masterIndex++) {
                    masterFound = checkEquality(jsonMasters.get(masterIndex).asArray(), set);
                }

                if (!masterFound) {
                    reason = "master not found " + set.toString();
                    return false;
                }
            }

            return true;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(reason);
        }

        private Set<NodeId> jsonToSet(JsonArray nodes) {
            final Set<NodeId> nodeIds = Sets.newHashSet();
            nodes.forEach(node -> nodeIds.add(NodeId.nodeId(node.asString())));
            return nodeIds;
        }

        private boolean checkEquality(JsonArray nodes, Set<NodeId> nodeIds) {
            Set<NodeId> jsonSet = jsonToSet(nodes);
            if (jsonSet.size() == nodes.size()) {
                return jsonSet.containsAll(nodeIds);
            }
            return false;
        }
    }

    private static RegionJsonMatcher matchesRegion(Region region) {
        return new RegionJsonMatcher(region);
    }

    /**
     * Hamcrest matcher to check that a region is represented properly in a JSON
     * array of regions.
     */
    public static class RegionJsonArrayMatcher extends TypeSafeMatcher<JsonArray> {
        private final Region region;
        private String reason = "";

        public RegionJsonArrayMatcher(Region regionValue) {
            this.region = regionValue;
        }

        @Override
        protected boolean matchesSafely(JsonArray json) {
            boolean regionFound = false;
            for (int jsonRegionIndex = 0; jsonRegionIndex < json.size(); jsonRegionIndex++) {
                final JsonObject jsonRegion = json.get(jsonRegionIndex).asObject();

                final String regionId = region.id().toString();
                final String jsonRegionId = jsonRegion.get("id").asString();
                if (jsonRegionId.equals(regionId)) {
                    regionFound = true;
                    assertThat(jsonRegion, matchesRegion(region));
                }
            }

            if (!regionFound) {
                reason = "Region with id " + region.id().toString() + " not found";
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
     * Factory to allocate a region array matcher.
     *
     * @param region region object we are looking for
     * @return matcher
     */
    private static RegionJsonArrayMatcher hasRegion(Region region) {
        return new RegionJsonArrayMatcher(region);
    }

    @Test
    public void testRegionEmptyArray() {
        expect(mockRegionService.getRegions()).andReturn(ImmutableSet.of()).anyTimes();
        replay((mockRegionService));
        final WebTarget wt = target();
        final String response = wt.path("regions").request().get(String.class);
        assertThat(response, is("{\"regions\":[]}"));

        verify(mockRegionService);
    }

    /**
     * Tests the results of the REST API GET when there are active regions.
     */
    @Test
    public void testRegionsPopulatedArray() {
        final Set<Region> regions = ImmutableSet.of(region1, region2, region3);
        expect(mockRegionService.getRegions()).andReturn(regions).anyTimes();
        replay(mockRegionService);

        final WebTarget wt = target();
        final String response = wt.path("regions").request().get(String.class);
        final JsonObject result = Json.parse(response).asObject();
        assertThat(result, notNullValue());

        assertThat(result.names(), hasSize(1));
        assertThat(result.names().get(0), is("regions"));
        final JsonArray jsonRegions = result.get("regions").asArray();
        assertThat(jsonRegions, notNullValue());
        assertThat(jsonRegions, hasRegion(region1));
        assertThat(jsonRegions, hasRegion(region2));
        assertThat(jsonRegions, hasRegion(region3));

        verify(mockRegionService);

    }

    /**
     * Tests the result of a REST API GET for a region with region id.
     */
    @Test
    public void testGetRegionById() {
        expect(mockRegionService.getRegion(anyObject())).andReturn(region1).anyTimes();
        replay(mockRegionService);

        final WebTarget wt = target();
        final String response = wt.path("regions/" + regionId1.toString()).request().get(String.class);
        final JsonObject result = Json.parse(response).asObject();
        assertThat(result, notNullValue());
        assertThat(result, matchesRegion(region1));

        verify(mockRegionService);
    }

    /**
     * Tests creating a region with POST.
     */
    @Test
    public void testRegionPost() {
        mockRegionAdminService.createRegion(anyObject(), anyObject(),
                anyObject(), anyObject());
        expectLastCall().andReturn(region2).anyTimes();
        replay(mockRegionAdminService);

        WebTarget wt = target();
        InputStream jsonStream = RegionsResourceTest.class
                .getResourceAsStream("post-region.json");

        Response response = wt.path("regions")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(jsonStream));
        assertThat(response.getStatus(), is(HttpURLConnection.HTTP_CREATED));

        verify(mockRegionAdminService);
    }

    /**
     * Tests updating a region with PUT.
     */
    @Test
    public void testRegionPut() {
        mockRegionAdminService.updateRegion(anyObject(), anyObject(),
                anyObject(), anyObject());
        expectLastCall().andReturn(region1).anyTimes();
        replay(mockRegionAdminService);

        WebTarget wt = target();
        InputStream jsonStream = RegionsResourceTest.class
                .getResourceAsStream("post-region.json");

        Response response = wt.path("regions/" + region1.id().toString())
                .request(MediaType.APPLICATION_JSON_TYPE)
                .put(Entity.json(jsonStream));
        assertThat(response.getStatus(), is(HttpURLConnection.HTTP_OK));

        verify(mockRegionAdminService);
    }

    /**
     * Tests deleting a region with DELETE.
     */
    @Test
    public void testRegionDelete() {
        mockRegionAdminService.removeRegion(anyObject());
        expectLastCall();
        replay(mockRegionAdminService);

        WebTarget wt = target();
        Response response = wt.path("regions/" + region1.id().toString())
                .request().delete();
        assertThat(response.getStatus(), is(HttpURLConnection.HTTP_NO_CONTENT));

        verify(mockRegionAdminService);
    }

    /**
     * Tests retrieving device ids that are associated with the given region.
     */
    @Test
    public void testGetRegionDevices() {
        final DeviceId deviceId1 = DeviceId.deviceId("1");
        final DeviceId deviceId2 = DeviceId.deviceId("2");
        final DeviceId deviceId3 = DeviceId.deviceId("3");

        final Set<DeviceId> deviceIds = ImmutableSet.of(deviceId1, deviceId2, deviceId3);

        expect(mockRegionService.getRegionDevices(anyObject()))
                .andReturn(deviceIds).anyTimes();
        replay(mockRegionService);

        final WebTarget wt = target();
        final String response = wt.path("regions/" +
                region1.id().toString() + "/devices").request().get(String.class);
        final JsonObject result = Json.parse(response).asObject();
        assertThat(result, notNullValue());

        assertThat(result.names(), hasSize(1));
        assertThat(result.names().get(0), is("deviceIds"));
        final JsonArray jsonDeviceIds = result.get("deviceIds").asArray();
        assertThat(jsonDeviceIds.size(), is(3));
        assertThat(jsonDeviceIds.get(0).asString(), is("1"));
        assertThat(jsonDeviceIds.get(1).asString(), is("2"));
        assertThat(jsonDeviceIds.get(2).asString(), is("3"));

        verify(mockRegionService);
    }

    /**
     * Tests creating a flow with POST.
     */
    @Test
    public void testAddDevicesPostWithoutRegion() {
        expect(mockRegionService.getRegion(anyObject())).andReturn(null).anyTimes();
        replay(mockRegionService);

        WebTarget wt = target();
        InputStream jsonStream = RegionsResourceTest.class
                .getResourceAsStream("region-deviceIds.json");

        Response response = wt.path("regions/" + region1.id() + "/devices")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(jsonStream));
        assertThat(response.getStatus(), is(HttpURLConnection.HTTP_NOT_FOUND));

        verify(mockRegionService);
    }

    /**
     * Tests adding a set of devices in region with POST.
     */
    @Test
    public void testAddDevicesPost() {
        mockRegionAdminService.addDevices(anyObject(), anyObject());
        expectLastCall();
        replay(mockRegionAdminService);

        expect(mockRegionService.getRegion(anyObject())).andReturn(region1).anyTimes();
        replay(mockRegionService);

        WebTarget wt = target();
        InputStream jsonStream = RegionsResourceTest.class
                .getResourceAsStream("region-deviceIds.json");

        Response response = wt.path("regions/" + region1.id().toString() + "/devices")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(jsonStream));
        assertThat(response.getStatus(), is(HttpURLConnection.HTTP_CREATED));

        verify(mockRegionAdminService);
    }

    /**
     * Tests deleting a set of devices contained in the given region with DELETE.
     */
    @Test
    public void testRemoveDevicesDelete() {
        mockRegionAdminService.removeDevices(anyObject(), anyObject());
        expectLastCall();
        replay(mockRegionAdminService);

        expect(mockRegionService.getRegion(anyObject())).andReturn(region1).anyTimes();
        replay(mockRegionService);

        WebTarget wt = target()
                .property(ClientProperties.SUPPRESS_HTTP_COMPLIANCE_VALIDATION, true);
        InputStream jsonStream = RegionsResourceTest.class
                .getResourceAsStream("region-deviceIds.json");

        // FIXME: need to consider whether to use jsonStream for entry deletion
        Response response = wt.path("regions/" + region1.id().toString() + "/devices")
                .request().method("DELETE", Entity.json(jsonStream));
        assertThat(response.getStatus(), is(HttpURLConnection.HTTP_NO_CONTENT));
        verify(mockRegionAdminService);
    }
}
