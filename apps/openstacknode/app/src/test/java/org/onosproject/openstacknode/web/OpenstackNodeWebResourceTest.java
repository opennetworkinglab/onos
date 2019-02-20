/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.openstacknode.web;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.google.common.collect.ImmutableSet;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Before;
import org.junit.Test;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.osgi.TestServiceDirectory;
import org.onlab.packet.IpAddress;
import org.onosproject.codec.CodecService;
import org.onosproject.codec.impl.CodecManager;
import org.onosproject.net.DeviceId;
import org.onosproject.openstacknode.api.DefaultOpenstackNode;
import org.onosproject.openstacknode.api.NodeState;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.onosproject.openstacknode.api.OpenstackNodeAdminService;
import org.onosproject.openstacknode.api.OpenstackNodeService;
import org.onosproject.openstacknode.codec.OpenstackNodeCodec;
import org.onosproject.rest.resources.ResourceTest;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.onosproject.openstacknode.codec.OpenstackNodeJsonArrayMatcher.hasNode;

/**
 * Unit test for Openstack node REST API.
 */
public class OpenstackNodeWebResourceTest extends ResourceTest {

    final OpenstackNodeService mockOpenstackNodeService =
            createMock(OpenstackNodeService.class);
    final OpenstackNodeAdminService mockOpenstackNodeAdminService =
            createMock(OpenstackNodeAdminService.class);
    private static final String PATH = "configure";

    /**
     * Constructs an openstack node resource test instance.
     */
    public OpenstackNodeWebResourceTest() {
        super(ResourceConfig.forApplicationClass(OpenstackNodeWebApplication.class));
    }

    private OpenstackNode openstackNode;

    /**
     * Sets up the global values for all the tests.
     */
    @Before
    public void setUpTest() {
        final CodecManager codecService = new CodecManager();
        codecService.activate();
        codecService.registerCodec(OpenstackNode.class, new OpenstackNodeCodec());
        ServiceDirectory testDirectory =
                new TestServiceDirectory()
                        .add(OpenstackNodeService.class, mockOpenstackNodeService)
                        .add(OpenstackNodeAdminService.class, mockOpenstackNodeAdminService)
                        .add(CodecService.class, codecService);
        setServiceDirectory(testDirectory);

        openstackNode = DefaultOpenstackNode.builder()
                            .hostname("compute-node")
                            .type(OpenstackNode.NodeType.COMPUTE)
                            .dataIp(IpAddress.valueOf("10.134.34.222"))
                            .managementIp(IpAddress.valueOf("10.134.231.30"))
                            .intgBridge(DeviceId.deviceId("of:00000000000000a1"))
                            .uplinkPort("eth2")
                            .state(NodeState.INIT)
                            .build();
    }

    /**
     * Tests the results of the REST API POST method with creating new nodes operation.
     */
    @Test
    public void testCreateNodesWithCreateOperation() {
        expect(mockOpenstackNodeService.node(anyString())).andReturn(null).once();
        replay(mockOpenstackNodeService);

        mockOpenstackNodeAdminService.createNode(anyObject());
        replay(mockOpenstackNodeAdminService);

        final WebTarget wt = target();
        InputStream jsonStream = OpenstackNodeWebResourceTest.class
                .getResourceAsStream("openstack-node-gateway-config.json");
        Response response = wt.path(PATH).request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(jsonStream));
        final int status = response.getStatus();

        assertThat(status, is(201));

        verify(mockOpenstackNodeService);
        verify(mockOpenstackNodeAdminService);
    }

    /**
     * Tests the results of the REST API POST method without creating new nodes operation.
     */
    @Test
    public void testCreateNodesWithoutCreateOperation() {
        expect(mockOpenstackNodeService.node(anyString())).andReturn(openstackNode).once();
        replay(mockOpenstackNodeService);

        final WebTarget wt = target();
        InputStream jsonStream = OpenstackNodeWebResourceTest.class
                .getResourceAsStream("openstack-node-gateway-config.json");
        Response response = wt.path(PATH).request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(jsonStream));
        final int status = response.getStatus();

        assertThat(status, is(201));

        verify(mockOpenstackNodeService);
    }

    /**
     * Tests the results of the REST API PUT method with modifying the nodes.
     */
    @Test
    public void testUpdateNodesWithoutModifyOperation() {
        expect(mockOpenstackNodeService.node(anyString())).andReturn(openstackNode).once();
        replay(mockOpenstackNodeService);

        mockOpenstackNodeAdminService.updateNode(anyObject());
        replay(mockOpenstackNodeAdminService);

        final WebTarget wt = target();
        InputStream jsonStream = OpenstackNodeWebResourceTest.class
                .getResourceAsStream("openstack-node-gateway-config.json");
        Response response = wt.path(PATH).request(MediaType.APPLICATION_JSON_TYPE)
                .put(Entity.json(jsonStream));
        final int status = response.getStatus();

        assertThat(status, is(200));

        verify(mockOpenstackNodeService);
        verify(mockOpenstackNodeAdminService);
    }

    /**
     * Tests the results of the REST API PUT method without modifying the nodes.
     */
    @Test
    public void testUpdateNodesWithModifyOperation() {
        expect(mockOpenstackNodeService.node(anyString())).andReturn(null).once();
        replay(mockOpenstackNodeService);

        final WebTarget wt = target();
        InputStream jsonStream = OpenstackNodeWebResourceTest.class
                .getResourceAsStream("openstack-node-gateway-config.json");
        Response response = wt.path(PATH).request(MediaType.APPLICATION_JSON_TYPE)
                .put(Entity.json(jsonStream));
        final int status = response.getStatus();

        assertThat(status, is(304));

        verify(mockOpenstackNodeService);
    }

    /**
     * Tests the results of the REST API DELETE method with deleting the nodes.
     */
    @Test
    public void testDeleteNodesWithDeletionOperation() {
        expect(mockOpenstackNodeService.node(anyString())).andReturn(openstackNode).once();
        replay(mockOpenstackNodeService);

        expect(mockOpenstackNodeAdminService.removeNode(anyString())).andReturn(openstackNode).once();
        replay(mockOpenstackNodeAdminService);

        String location = PATH + "/gateway-node";

        final WebTarget wt = target();
        Response response = wt.path(location).request(
                            MediaType.APPLICATION_JSON_TYPE).delete();

        final int status = response.getStatus();

        assertThat(status, is(204));

        verify(mockOpenstackNodeService);
        verify(mockOpenstackNodeAdminService);
    }

    /**
     * Tests the results of the REST API DELETE method without deleting the nodes.
     */
    @Test
    public void testDeleteNodesWithoutDeletionOperation() {
        expect(mockOpenstackNodeService.node(anyString())).andReturn(null).once();
        replay(mockOpenstackNodeService);

        String location = PATH + "/gateway-node";

        final WebTarget wt = target();
        Response response = wt.path(location).request(
                MediaType.APPLICATION_JSON_TYPE).delete();

        final int status = response.getStatus();

        assertThat(status, is(304));

        verify(mockOpenstackNodeService);
    }

    /**
     * Tests the result of the REST API GET when there are valid nodes.
     */
    @Test
    public void testGetNodesPopulatedArray() {
        expect(mockOpenstackNodeService.nodes()).
                andReturn(ImmutableSet.of(openstackNode)).anyTimes();
        replay(mockOpenstackNodeService);

        final WebTarget wt = target();
        final String response = wt.path(PATH).request().get(String.class);
        final JsonObject result = Json.parse(response).asObject();

        assertThat(result, notNullValue());
        assertThat(result.names(), hasSize(1));
        assertThat(result.names().get(0), is("nodes"));
        final JsonArray jsonNodes = result.get("nodes").asArray();
        assertThat(jsonNodes, notNullValue());
        assertThat(jsonNodes.size(), is(1));
        assertThat(jsonNodes, hasNode(openstackNode));

        verify(mockOpenstackNodeService);
    }
}
