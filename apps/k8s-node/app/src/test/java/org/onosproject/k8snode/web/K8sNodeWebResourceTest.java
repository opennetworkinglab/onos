/*
 * Copyright 2019-present Open Networking Foundation
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
package org.onosproject.k8snode.web;

import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Before;
import org.junit.Test;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.osgi.TestServiceDirectory;
import org.onlab.packet.IpAddress;
import org.onosproject.codec.CodecService;
import org.onosproject.codec.impl.CodecManager;
import org.onosproject.k8snode.api.DefaultK8sApiConfig;
import org.onosproject.k8snode.api.DefaultK8sNode;
import org.onosproject.k8snode.api.HostNodesInfo;
import org.onosproject.k8snode.api.K8sApiConfig;
import org.onosproject.k8snode.api.K8sApiConfigAdminService;
import org.onosproject.k8snode.api.K8sNode;
import org.onosproject.k8snode.api.K8sNodeAdminService;
import org.onosproject.k8snode.api.K8sNodeInfo;
import org.onosproject.k8snode.api.K8sNodeState;
import org.onosproject.k8snode.codec.HostNodesInfoCodec;
import org.onosproject.k8snode.codec.K8sApiConfigCodec;
import org.onosproject.k8snode.codec.K8sNodeCodec;
import org.onosproject.net.DeviceId;
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
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.onosproject.k8snode.api.K8sApiConfig.State.DISCONNECTED;

/**
 * Unit test for Kubernetes node REST API.
 */
public class K8sNodeWebResourceTest extends ResourceTest {

    final K8sNodeAdminService mockK8sNodeAdminService = createMock(K8sNodeAdminService.class);
    final K8sApiConfigAdminService mockK8sApiConfigAdminService =
            createMock(K8sApiConfigAdminService.class);
    private static final String NODE_PATH = "configure/node";
    private static final String API_PATH = "configure/api";

    private K8sNode k8sNode;
    private K8sApiConfig k8sApiConfig;

    /**
     * Constructs a kubernetes node resource test instance.
     */
    public K8sNodeWebResourceTest() {
        super(ResourceConfig.forApplicationClass(K8sNodeWebApplication.class));
    }

    /**
     * Sets up the global values for all the tests.
     */
    @Before
    public void setUpTest() {
        final CodecManager codecService = new CodecManager();
        codecService.activate();
        codecService.registerCodec(K8sNode.class, new K8sNodeCodec());
        codecService.registerCodec(K8sApiConfig.class, new K8sApiConfigCodec());
        codecService.registerCodec(HostNodesInfo.class, new HostNodesInfoCodec());
        ServiceDirectory testDirectory =
                new TestServiceDirectory()
                .add(K8sNodeAdminService.class, mockK8sNodeAdminService)
                .add(K8sApiConfigAdminService.class, mockK8sApiConfigAdminService)
                .add(CodecService.class, codecService);
        setServiceDirectory(testDirectory);

        k8sNode = DefaultK8sNode.builder()
                .clusterName("kubernetes")
                .hostname("minion-node")
                .type(K8sNode.Type.MINION)
                .dataIp(IpAddress.valueOf("10.134.34.222"))
                .managementIp(IpAddress.valueOf("10.134.231.30"))
                .nodeInfo(new K8sNodeInfo(IpAddress.valueOf("30.30.30.3"), null))
                .intgBridge(DeviceId.deviceId("of:00000000000000a1"))
                .extBridge(DeviceId.deviceId("of:00000000000000b1"))
                .state(K8sNodeState.INIT)
                .build();

        k8sApiConfig = DefaultK8sApiConfig.builder()
                .clusterName("kubernetes")
                .segmentId(1)
                .mode(K8sApiConfig.Mode.NORMAL)
                .scheme(K8sApiConfig.Scheme.HTTPS)
                .ipAddress(IpAddress.valueOf("10.134.34.223"))
                .port(6443)
                .state(DISCONNECTED)
                .token("tokenMod")
                .caCertData("caCertData")
                .clientCertData("clientCertData")
                .clientKeyData("clientKeyData")
                .build();
    }

    /**
     * Tests the results of the REST API POST method with creating new nodes operation.
     */
    @Test
    public void testCreateNodesWithCreateOperation() {
        expect(mockK8sNodeAdminService.node(anyString())).andReturn(null).once();
        mockK8sNodeAdminService.createNode(anyObject());
        replay(mockK8sNodeAdminService);

        final WebTarget wt = target();
        InputStream jsonStream = K8sNodeWebResourceTest.class
                .getResourceAsStream("k8s-node-minion-config.json");
        Response response = wt.path(NODE_PATH).request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(jsonStream));
        final int status = response.getStatus();

        assertThat(status, is(201));

        verify(mockK8sNodeAdminService);
    }

    /**
     * Tests the results of the REST API POST method without creating new nodes operation.
     */
    @Test
    public void testCreateNodesWithoutCreateOperation() {
        expect(mockK8sNodeAdminService.node(anyString())).andReturn(k8sNode).once();
        replay(mockK8sNodeAdminService);

        final WebTarget wt = target();
        InputStream jsonStream = K8sNodeWebResourceTest.class
                .getResourceAsStream("k8s-node-minion-config.json");
        Response response = wt.path(NODE_PATH).request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(jsonStream));
        final int status = response.getStatus();

        assertThat(status, is(201));

        verify(mockK8sNodeAdminService);
    }

    /**
     * Tests the results of the REST API PUT method with modifying the nodes.
     */
    @Test
    public void testUpdateNodesWithModifyOperation() {
        expect(mockK8sNodeAdminService.node(anyString())).andReturn(k8sNode).once();
        mockK8sNodeAdminService.updateNode(anyObject());
        replay(mockK8sNodeAdminService);

        final WebTarget wt = target();
        InputStream jsonStream = K8sNodeWebResourceTest.class
                .getResourceAsStream("k8s-node-minion-config.json");
        Response response = wt.path(NODE_PATH).request(MediaType.APPLICATION_JSON_TYPE)
                .put(Entity.json(jsonStream));
        final int status = response.getStatus();

        assertThat(status, is(200));

        verify(mockK8sNodeAdminService);
    }

    /**
     * Tests the results of the REST API PUT method without modifying the nodes.
     */
    @Test
    public void testUpdateNodesWithoutModifyOperation() {
        expect(mockK8sNodeAdminService.node(anyString())).andReturn(null).once();
        replay(mockK8sNodeAdminService);

        final WebTarget wt = target();
        InputStream jsonStream = K8sNodeWebResourceTest.class
                .getResourceAsStream("k8s-node-minion-config.json");
        Response response = wt.path(NODE_PATH).request(MediaType.APPLICATION_JSON_TYPE)
                .put(Entity.json(jsonStream));
        final int status = response.getStatus();

        assertThat(status, is(304));

        verify(mockK8sNodeAdminService);
    }

    /**
     * Tests the results of the REST API DELETE method with deleting the nodes.
     */
    @Test
    public void testDeleteNodesWithDeletionOperation() {
        expect(mockK8sNodeAdminService.node(anyString())).andReturn(k8sNode).once();
        expect(mockK8sNodeAdminService.removeNode(anyString())).andReturn(k8sNode).once();
        replay(mockK8sNodeAdminService);

        String location = NODE_PATH + "/minion-node";

        final WebTarget wt = target();
        Response response = wt.path(location).request(
                MediaType.APPLICATION_JSON_TYPE).delete();

        final int status = response.getStatus();

        assertThat(status, is(204));

        verify(mockK8sNodeAdminService);
    }

    /**
     * Tests the results of the REST API DELETE method without deleting the nodes.
     */
    @Test
    public void testDeleteNodesWithoutDeletionOperation() {
        expect(mockK8sNodeAdminService.node(anyString())).andReturn(null).once();
        replay(mockK8sNodeAdminService);

        String location = NODE_PATH + "/minion-node";

        final WebTarget wt = target();
        Response response = wt.path(location).request(
                MediaType.APPLICATION_JSON_TYPE).delete();

        final int status = response.getStatus();

        assertThat(status, is(304));

        verify(mockK8sNodeAdminService);
    }

    /**
     * Tests the results of the REST API POST method with creating new configs operation.
     */
    @Test
    public void testCreateConfigsWithCreateOperation() {
        expect(mockK8sApiConfigAdminService.apiConfig(anyString())).andReturn(null).once();
        mockK8sApiConfigAdminService.createApiConfig(anyObject());
        replay(mockK8sApiConfigAdminService);

        final WebTarget wt = target();
        InputStream jsonStream = K8sNodeWebResourceTest.class
                .getResourceAsStream("k8s-api-config.json");
        Response response = wt.path(API_PATH).request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(jsonStream));
        final int status = response.getStatus();

        assertThat(status, is(201));

        verify(mockK8sApiConfigAdminService);
    }

    /**
     * Tests the results of the REST API POST method without creating new configs operation.
     */
    @Test
    public void testCreateConfigsWithoutCreateOperation() {
        expect(mockK8sApiConfigAdminService.apiConfig(anyString())).andReturn(k8sApiConfig).once();
        replay(mockK8sApiConfigAdminService);

        final WebTarget wt = target();
        InputStream jsonStream = K8sNodeWebResourceTest.class
                .getResourceAsStream("k8s-api-config.json");
        Response response = wt.path(API_PATH).request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(jsonStream));
        final int status = response.getStatus();

        assertThat(status, is(201));

        verify(mockK8sApiConfigAdminService);
    }

    /**
     * Tests the results of the REST API PUT method with modifying the configs.
     */
    @Test
    public void testUpdateConfigsWithModifyOperation() {
        expect(mockK8sApiConfigAdminService.apiConfig(anyString()))
                .andReturn(k8sApiConfig).once();
        mockK8sApiConfigAdminService.updateApiConfig(anyObject());
        replay(mockK8sApiConfigAdminService);

        final WebTarget wt = target();
        InputStream jsonStream = K8sNodeWebResourceTest.class
                .getResourceAsStream("k8s-api-config.json");
        Response response = wt.path(API_PATH).request(MediaType.APPLICATION_JSON_TYPE)
                .put(Entity.json(jsonStream));
        final int status = response.getStatus();

        assertThat(status, is(200));

        verify(mockK8sApiConfigAdminService);
    }

    /**
     * Tests the results of the REST API PUT method without modifying the configs.
     */
    @Test
    public void testUpdateConfigsWithoutModifyOperation() {
        expect(mockK8sApiConfigAdminService.apiConfig(anyString())).andReturn(null).once();
        replay(mockK8sApiConfigAdminService);

        final WebTarget wt = target();
        InputStream jsonStream = K8sNodeWebResourceTest.class
                .getResourceAsStream("k8s-api-config.json");
        Response response = wt.path(API_PATH).request(MediaType.APPLICATION_JSON_TYPE)
                .put(Entity.json(jsonStream));
        final int status = response.getStatus();

        assertThat(status, is(304));

        verify(mockK8sApiConfigAdminService);
    }

    /**
     * Tests the results of the REST API DELETE method with deleting the configs.
     */
    @Test
    public void testDeleteConfigsWithDeletionOperation() {
        expect(mockK8sApiConfigAdminService.apiConfig(anyString()))
                .andReturn(k8sApiConfig).once();
        expect(mockK8sApiConfigAdminService.removeApiConfig(anyString()))
                .andReturn(k8sApiConfig).once();
        replay(mockK8sApiConfigAdminService);

        String location = API_PATH + "/https://test:8663";

        final WebTarget wt = target();
        Response response = wt.path(location).request(
                MediaType.APPLICATION_JSON_TYPE).delete();

        final int status = response.getStatus();

        assertThat(status, is(204));

        verify(mockK8sApiConfigAdminService);
    }

    /**
     * Tests the results of the REST API DELETE method without deleting the configs.
     */
    @Test
    public void testDeleteConfigsWithoutDeletionOperation() {
        expect(mockK8sApiConfigAdminService.apiConfig(anyString())).andReturn(null).once();
        replay(mockK8sApiConfigAdminService);

        String location = API_PATH + "/https://test:8663";

        final WebTarget wt = target();
        Response response = wt.path(location).request(
                MediaType.APPLICATION_JSON_TYPE).delete();

        final int status = response.getStatus();

        assertThat(status, is(304));

        verify(mockK8sApiConfigAdminService);
    }
}
