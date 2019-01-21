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
import org.onosproject.k8snode.api.DefaultK8sNode;
import org.onosproject.k8snode.api.K8sNode;
import org.onosproject.k8snode.api.K8sNodeAdminService;
import org.onosproject.k8snode.api.K8sNodeState;
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

/**
 * Unit test for Kubernetes node REST API.
 */
public class K8sNodeWebResourceTest extends ResourceTest {

    final K8sNodeAdminService mockK8sNodeAdminService = createMock(K8sNodeAdminService.class);
    private static final String PATH = "configure";

    private K8sNode k8sNode;

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
        ServiceDirectory testDirectory =
                new TestServiceDirectory()
                .add(K8sNodeAdminService.class, mockK8sNodeAdminService)
                .add(CodecService.class, codecService);
        setServiceDirectory(testDirectory);

        k8sNode = DefaultK8sNode.builder()
                .hostname("minion-node")
                .type(K8sNode.Type.MINION)
                .dataIp(IpAddress.valueOf("10.134.34.222"))
                .managementIp(IpAddress.valueOf("10.134.231.30"))
                .intgBridge(DeviceId.deviceId("of:00000000000000a1"))
                .state(K8sNodeState.INIT)
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
        Response response = wt.path(PATH).request(MediaType.APPLICATION_JSON_TYPE)
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
        Response response = wt.path(PATH).request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(jsonStream));
        final int status = response.getStatus();

        assertThat(status, is(201));

        verify(mockK8sNodeAdminService);
    }

    /**
     * Tests the results of the REST API PUT method with modifying the nodes.
     */
    @Test
    public void testUpdateNodesWithoutModifyOperation() {
        expect(mockK8sNodeAdminService.node(anyString())).andReturn(k8sNode).once();
        mockK8sNodeAdminService.updateNode(anyObject());
        replay(mockK8sNodeAdminService);

        final WebTarget wt = target();
        InputStream jsonStream = K8sNodeWebResourceTest.class
                .getResourceAsStream("k8s-node-minion-config.json");
        Response response = wt.path(PATH).request(MediaType.APPLICATION_JSON_TYPE)
                .put(Entity.json(jsonStream));
        final int status = response.getStatus();

        assertThat(status, is(200));

        verify(mockK8sNodeAdminService);
    }

    /**
     * Tests the results of the REST API PUT method without modifying the nodes.
     */
    @Test
    public void testUpdateNodesWithModifyOperation() {
        expect(mockK8sNodeAdminService.node(anyString())).andReturn(null).once();
        replay(mockK8sNodeAdminService);

        final WebTarget wt = target();
        InputStream jsonStream = K8sNodeWebResourceTest.class
                .getResourceAsStream("k8s-node-minion-config.json");
        Response response = wt.path(PATH).request(MediaType.APPLICATION_JSON_TYPE)
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

        String location = PATH + "/minion-node";

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

        String location = PATH + "/minion-node";

        final WebTarget wt = target();
        Response response = wt.path(location).request(
                MediaType.APPLICATION_JSON_TYPE).delete();

        final int status = response.getStatus();

        assertThat(status, is(304));

        verify(mockK8sNodeAdminService);
    }
}
