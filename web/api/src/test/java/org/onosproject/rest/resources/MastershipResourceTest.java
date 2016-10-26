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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Description;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Test;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.osgi.TestServiceDirectory;
import org.onlab.rest.BaseResource;
import org.onosproject.cluster.NodeId;
import org.onosproject.cluster.RoleInfo;
import org.onosproject.codec.CodecService;
import org.onosproject.codec.impl.CodecManager;
import org.onosproject.mastership.MastershipAdminService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.device.DeviceService;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
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
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.onosproject.net.MastershipRole.MASTER;

/**
 * Unit tests for Mastership REST APIs.
 */
public final class MastershipResourceTest extends ResourceTest {

    private final MastershipService mockService = createMock(MastershipService.class);
    private final DeviceService mockDeviceService = createMock(DeviceService.class);
    private final MastershipAdminService mockAdminService =
                  createMock(MastershipAdminService.class);

    private final DeviceId deviceId1 = DeviceId.deviceId("dev:1");
    private final DeviceId deviceId2 = DeviceId.deviceId("dev:2");
    private final DeviceId deviceId3 = DeviceId.deviceId("dev:3");

    final Device device1 = new DefaultDevice(null, deviceId1, Device.Type.OTHER,
            "", "", "", "", null);
    private final NodeId nodeId1 = NodeId.nodeId("node:1");
    private final NodeId nodeId2 = NodeId.nodeId("node:2");
    private final NodeId nodeId3 = NodeId.nodeId("node:3");
    private final MastershipRole role1 = MASTER;

    /**
     * Creates a mock role info which is comprised of one master and three backups.
     *
     * @return a mock role info instance
     */
    private RoleInfo createMockRoleInfo() {
        NodeId master = NodeId.nodeId("master");
        List<NodeId> backups = ImmutableList.of(nodeId1, nodeId2, nodeId3);

        return new RoleInfo(master, backups);
    }

    private static final class RoleInfoJsonMatcher extends TypeSafeMatcher<JsonObject> {
        private final RoleInfo roleInfo;
        private String reason = "";

        private RoleInfoJsonMatcher(RoleInfo roleInfo) {
            this.roleInfo = roleInfo;
        }

        @Override
        protected boolean matchesSafely(JsonObject jsonNode) {

            // check master node identifier
            String jsonNodeId = jsonNode.get("master") != null ?
                                jsonNode.get("master").asString() : null;
            String nodeId = roleInfo.master().id();
            if (!StringUtils.equals(jsonNodeId, nodeId)) {
                reason = "master's node id was " + jsonNodeId;
                return false;
            }

            // check backup nodes size
            final JsonArray jsonBackupNodeIds = jsonNode.get("backups").asArray();
            if (jsonBackupNodeIds.size() != roleInfo.backups().size()) {
                reason = "backup nodes size was " + jsonBackupNodeIds.size();
                return false;
            }

            // check backup nodes' identifier
            for (NodeId backupNodeId : roleInfo.backups()) {
                boolean backupFound = false;
                for (int idx = 0; idx < jsonBackupNodeIds.size(); idx++) {
                    if (backupNodeId.id().equals(jsonBackupNodeIds.get(idx).asString())) {
                        backupFound = true;
                        break;
                    }
                }
                if (!backupFound) {
                    reason = "backup not found " + backupNodeId.id();
                    return false;
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
     * Factory to allocate a role info json matcher.
     *
     * @param roleInfo role info object we are looking for
     * @return matcher
     */
    private static RoleInfoJsonMatcher matchesRoleInfo(RoleInfo roleInfo) {
        return new RoleInfoJsonMatcher(roleInfo);
    }

    /**
     * Sets up the global values for all the tests.
     */
    @Before
    public void setUpTest() {

        final CodecManager codecService = new CodecManager();
        codecService.activate();
        ServiceDirectory testDirectory =
                new TestServiceDirectory()
                        .add(MastershipService.class, mockService)
                        .add(MastershipAdminService.class, mockAdminService)
                        .add(DeviceService.class, mockDeviceService)
                        .add(CodecService.class, codecService);

        BaseResource.setServiceDirectory(testDirectory);
    }

    /**
     * Tests the result of the REST API GET when there are active master roles.
     */
    @Test
    public void testGetLocalRole() {
        expect(mockService.getLocalRole(anyObject())).andReturn(role1).anyTimes();
        replay(mockService);

        final WebTarget wt = target();
        final String response = wt.path("mastership/" + deviceId1.toString() +
                                "/local").request().get(String.class);
        final JsonObject result = Json.parse(response).asObject();
        assertThat(result, notNullValue());

        assertThat(result.names(), hasSize(1));
        assertThat(result.names().get(0), is("role"));

        final String role = result.get("role").asString();
        assertThat(role, notNullValue());
        assertThat(role, is("MASTER"));
    }

    /**
     * Tests the result of the REST API GET when there is no active master.
     */
    @Test
    public void testGetMasterForNull() {
        expect(mockService.getMasterFor(anyObject())).andReturn(null).anyTimes();
        replay(mockService);

        final WebTarget wt = target();
        final Response response = wt.path("mastership/" + deviceId1.toString() +
                "/master").request().get();
        assertEquals(404, response.getStatus());
    }

    /**
     * Tests the result of the REST API GET when there is active master.
     */
    @Test
    public void testGetMasterFor() {
        expect(mockService.getMasterFor(anyObject())).andReturn(nodeId1).anyTimes();
        replay(mockService);

        final WebTarget wt = target();
        final String response = wt.path("mastership/" + deviceId1.toString() +
                                "/master").request().get(String.class);
        final JsonObject result = Json.parse(response).asObject();
        assertThat(result, notNullValue());

        assertThat(result.names(), hasSize(1));
        assertThat(result.names().get(0), is("nodeId"));

        final String node = result.get("nodeId").asString();
        assertThat(node, notNullValue());
        assertThat(node, is("node:1"));
    }

    /**
     * Tests the result of the REST API GET when there are no active nodes.
     */
    @Test
    public void testGetNodesForNull() {
        expect(mockService.getNodesFor(anyObject())).andReturn(null).anyTimes();
        replay(mockService);

        final WebTarget wt = target();
        final Response response = wt.path("mastership/" + deviceId1.toString() +
                "/role").request().get();
        assertEquals(404, response.getStatus());
    }

    /**
     * Tests the result of the REST API GET when there are active nodes.
     */
    @Test
    public void testGetNodesFor() {
        RoleInfo mockRoleInfo = createMockRoleInfo();
        expect(mockService.getNodesFor(anyObject())).andReturn(mockRoleInfo).anyTimes();
        replay(mockService);

        final WebTarget wt = target();
        final String response = wt.path("mastership/" + deviceId1.toString() +
                                "/role").request().get(String.class);
        final JsonObject result = Json.parse(response).asObject();
        assertThat(result, notNullValue());

        assertThat(result, matchesRoleInfo(mockRoleInfo));
    }

    /**
     * Tests the result of the REST API GET when there are active devices.
     */
    @Test
    public void testGetDevicesOf() {
        Set<DeviceId> deviceIds = ImmutableSet.of(deviceId1, deviceId2, deviceId3);
        expect(mockService.getDevicesOf(anyObject())).andReturn(deviceIds).anyTimes();
        replay(mockService);

        final WebTarget wt = target();
        final String response = wt.path("mastership/" + deviceId1.toString() +
                                "/device").request().get(String.class);
        final JsonObject result = Json.parse(response).asObject();
        assertThat(result, notNullValue());

        assertThat(result.names(), hasSize(1));
        assertThat(result.names().get(0), is("deviceIds"));

        final JsonArray jsonDevices = result.get("deviceIds").asArray();
        assertThat(jsonDevices, notNullValue());
        assertThat(jsonDevices.size(), is(3));
    }

    /**
     * Tests the result of the REST API GET for requesting mastership role.
     */
    @Test
    public void testRequestRoleFor() {
        expect(mockService.requestRoleForSync(anyObject())).andReturn(role1).anyTimes();
        replay(mockService);

        expect(mockDeviceService.getDevice(deviceId1)).andReturn(device1);
        replay(mockDeviceService);

        final WebTarget wt = target();
        final String response = wt.path("mastership/" + deviceId1.toString() +
                "/request").request().get(String.class);
        final JsonObject result = Json.parse(response).asObject();
        assertThat(result, notNullValue());

        assertThat(result.names(), hasSize(1));
        assertThat(result.names().get(0), is("role"));

        final String role = result.get("role").asString();
        assertThat(role, notNullValue());
        assertThat(role, is("MASTER"));
    }

    /**
     * Tests the result of the REST API GET for relinquishing mastership role.
     */
    @Test
    public void testRelinquishMastership() {
        mockService.relinquishMastershipSync(anyObject());
        expectLastCall();
        replay(mockService);

        final WebTarget wt = target();
        final Response response = wt.path("mastership/" + deviceId1.toString() +
                "/relinquish").request().get();
        assertThat(response.getStatus(), is(HttpURLConnection.HTTP_CREATED));
        String location = response.getLocation().toString();
        assertThat(location, Matchers.startsWith(deviceId1.toString()));
    }

    /**
     * Tests the result of the REST API PUT for setting role.
     */
    @Test
    public void testSetRole() {
        mockAdminService.setRoleSync(anyObject(), anyObject(), anyObject());
        expectLastCall();
        replay(mockAdminService);

        final WebTarget wt = target();
        final InputStream jsonStream = MetersResourceTest.class
                .getResourceAsStream("put-set-roles.json");
        final Response response = wt.path("mastership")
                                    .request().put(Entity.json(jsonStream));
        assertThat(response.getStatus(), is(HttpURLConnection.HTTP_OK));
    }

    /**
     * Tests the result of the REST API GET for balancing roles.
     */
    @Test
    public void testBalanceRoles() {
        mockAdminService.balanceRoles();
        expectLastCall();
        replay(mockAdminService);

        final WebTarget wt = target();
        final Response response = wt.path("mastership").request().get();
        assertThat(response.getStatus(), is(HttpURLConnection.HTTP_OK));
    }
}
