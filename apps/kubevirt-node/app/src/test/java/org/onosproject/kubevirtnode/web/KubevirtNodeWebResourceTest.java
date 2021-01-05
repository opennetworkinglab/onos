/*
 * Copyright 2020-present Open Networking Foundation
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
package org.onosproject.kubevirtnode.web;

import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Before;
import org.junit.Test;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.osgi.TestServiceDirectory;
import org.onlab.packet.IpAddress;
import org.onosproject.codec.CodecService;
import org.onosproject.codec.impl.CodecManager;
import org.onosproject.kubevirtnode.api.DefaultKubevirtNode;
import org.onosproject.kubevirtnode.api.KubevirtNode;
import org.onosproject.kubevirtnode.api.KubevirtNodeAdminService;
import org.onosproject.kubevirtnode.api.KubevirtNodeState;
import org.onosproject.kubevirtnode.codec.KubevirtNodeCodec;
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
 * Unit test for KubeVirt node REST API.
 */
public class KubevirtNodeWebResourceTest extends ResourceTest {

    final KubevirtNodeAdminService mockKubevirtNodeAdminService = createMock(KubevirtNodeAdminService.class);
    private static final String PATH = "node";

    private KubevirtNode kubevirtNode;

    /**
     * Constructs a KubeVirt node resource test instance.
     */
    public KubevirtNodeWebResourceTest() {
        super(ResourceConfig.forApplicationClass(KubevirtNodeWebApplication.class));
    }

    /**
     * Sets up the global values for all the tests.
     */
    @Before
    public void setUpTest() {
        final CodecManager codecService = new CodecManager();
        codecService.activate();
        codecService.registerCodec(KubevirtNode.class, new KubevirtNodeCodec());
        ServiceDirectory testDirectory =
                new TestServiceDirectory()
                .add(KubevirtNodeAdminService.class, mockKubevirtNodeAdminService)
                .add(CodecService.class, codecService);
        setServiceDirectory(testDirectory);

        kubevirtNode = DefaultKubevirtNode.builder()
                .hostname("worker-node")
                .type(KubevirtNode.Type.WORKER)
                .dataIp(IpAddress.valueOf("10.134.34.222"))
                .managementIp(IpAddress.valueOf("10.134.231.30"))
                .intgBridge(DeviceId.deviceId("of:00000000000000a1"))
                .tunBridge(DeviceId.deviceId("of:00000000000000a2"))
                .state(KubevirtNodeState.INIT)
                .build();
    }

    /**
     * Tests the results of the REST API POST method with creating new nodes operation.
     */
    @Test
    public void testCreateNodesWithCreateOperation() {
        expect(mockKubevirtNodeAdminService.node(anyString())).andReturn(null).once();
        mockKubevirtNodeAdminService.createNode(anyObject());
        replay(mockKubevirtNodeAdminService);

        final WebTarget wt = target();
        InputStream jsonStream = KubevirtNodeWebResourceTest.class
                .getResourceAsStream("kubevirt-worker-node.json");

        Response response = wt.path(PATH).request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(jsonStream));
        final int status = response.getStatus();

        assertThat(status, is(201));

        verify(mockKubevirtNodeAdminService);
    }

    /**
     * Tests the results of the REST API PUT method with modifying the nodes.
     */
    @Test
    public void testUpdateNodesWithModifyOperation() {
        expect(mockKubevirtNodeAdminService.node(anyString())).andReturn(kubevirtNode).once();
        mockKubevirtNodeAdminService.updateNode(anyObject());
        replay(mockKubevirtNodeAdminService);

        final WebTarget wt = target();
        InputStream jsonStream = KubevirtNodeWebResourceTest.class
                .getResourceAsStream("kubevirt-worker-node.json");

        Response response = wt.path(PATH).request(MediaType.APPLICATION_JSON_TYPE)
                .put(Entity.json(jsonStream));
        final int status = response.getStatus();

        assertThat(status, is(200));

        verify(mockKubevirtNodeAdminService);
    }

    /**
     * Tests the results of the REST API PUT method without modifying the nodes.
     */
    @Test
    public void testUpdateNodesWithoutModifyOperation() {
        expect(mockKubevirtNodeAdminService.node(anyString())).andReturn(null).once();
        replay(mockKubevirtNodeAdminService);

        final WebTarget wt = target();
        InputStream jsonStream = KubevirtNodeWebResourceTest.class
                .getResourceAsStream("kubevirt-worker-node.json");

        Response response = wt.path(PATH).request(MediaType.APPLICATION_JSON_TYPE)
                .put(Entity.json(jsonStream));
        final int status = response.getStatus();

        assertThat(status, is(304));

        verify(mockKubevirtNodeAdminService);
    }

    /**
     * Tests the results of the REST API DELETE method with deleting the nodes.
     */
    @Test
    public void testDeleteNodesWithDeletionOperation() {
        expect(mockKubevirtNodeAdminService.node(anyString())).andReturn(kubevirtNode).once();
        expect(mockKubevirtNodeAdminService.removeNode(anyString())).andReturn(kubevirtNode).once();
        replay(mockKubevirtNodeAdminService);

        String location = PATH + "/worker-node";

        final WebTarget wt = target();
        Response response = wt.path(location).request(
                MediaType.APPLICATION_JSON_TYPE).delete();

        final int status = response.getStatus();

        assertThat(status, is(204));

        verify(mockKubevirtNodeAdminService);
    }

    /**
     * Tests the results of the REST API DELETE method without deleting the nodes.
     */
    @Test
    public void testDeleteNodesWithoutDeletionOperation() {
        expect(mockKubevirtNodeAdminService.node(anyString())).andReturn(null).once();
        replay(mockKubevirtNodeAdminService);

        String location = PATH + "/worker-node";

        final WebTarget wt = target();
        Response response = wt.path(location).request(
                MediaType.APPLICATION_JSON_TYPE).delete();

        final int status = response.getStatus();

        assertThat(status, is(304));

        verify(mockKubevirtNodeAdminService);
    }
}
