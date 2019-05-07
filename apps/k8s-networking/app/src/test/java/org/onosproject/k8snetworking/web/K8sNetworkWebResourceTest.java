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
package org.onosproject.k8snetworking.web;

import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Before;
import org.junit.Test;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.osgi.TestServiceDirectory;
import org.onosproject.codec.CodecService;
import org.onosproject.codec.impl.CodecManager;
import org.onosproject.k8snetworking.api.DefaultK8sNetwork;
import org.onosproject.k8snetworking.api.K8sNetwork;
import org.onosproject.k8snetworking.api.K8sNetworkAdminService;
import org.onosproject.k8snetworking.codec.K8sNetworkCodec;
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
 * Unit tests for kubernetes network REST API.
 */
public class K8sNetworkWebResourceTest extends ResourceTest {

    final K8sNetworkAdminService mockAdminService = createMock(K8sNetworkAdminService.class);
    private static final String PATH = "network";

    private K8sNetwork k8sNetwork;

    /**
     * Constructs a kubernetes networking resource test instance.
     */
    public K8sNetworkWebResourceTest() {
        super(ResourceConfig.forApplicationClass(K8sNetworkingWebApplication.class));
    }

    /**
     * Sets up the global values for all the tests.
     */
    @Before
    public void setUpTest() {
        final CodecManager codecService = new CodecManager();
        codecService.activate();
        codecService.registerCodec(K8sNetwork.class, new K8sNetworkCodec());
        ServiceDirectory testDirectory =
                new TestServiceDirectory()
                        .add(K8sNetworkAdminService.class, mockAdminService)
                        .add(CodecService.class, codecService);
        setServiceDirectory(testDirectory);

        k8sNetwork = DefaultK8sNetwork.builder()
                .networkId("sona-network")
                .name("sona-network")
                .type(K8sNetwork.Type.VXLAN)
                .segmentId("1")
                .cidr("10.10.0.0/24")
                .mtu(1500)
                .build();
    }

    /**
     * Tests the results of the REST API POST method with creating new network operation.
     */
    @Test
    public void testCreateNetworkWithCreateOperation() {
        mockAdminService.createNetwork(anyObject());
        replay(mockAdminService);

        final WebTarget wt = target();
        InputStream jsonStream = K8sNetworkWebResourceTest.class
                .getResourceAsStream("k8s-network.json");
        Response response = wt.path(PATH).request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(jsonStream));
        final int status = response.getStatus();

        assertThat(status, is(201));

        verify(mockAdminService);
    }

    /**
     * Tests the results of the REST API PUT method with modifying the network.
     */
    @Test
    public void testUpdateNetworkWithModifyOperation() {
        mockAdminService.updateNetwork(anyObject());
        replay(mockAdminService);

        String location = PATH + "/network-1";

        final WebTarget wt = target();
        InputStream jsonStream = K8sNetworkWebResourceTest.class
                .getResourceAsStream("k8s-network.json");
        Response response = wt.path(location)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .put(Entity.json(jsonStream));
        final int status = response.getStatus();

        assertThat(status, is(200));

        verify(mockAdminService);
    }

    /**
     * Tests the results of the REST API DELETE method with deleting the network.
     */
    @Test
    public void testDeleteNetworkWithDeletionOperation() {
        mockAdminService.removeNetwork(anyString());
        replay(mockAdminService);

        String location = PATH + "/network-1";

        final WebTarget wt = target();
        Response response = wt.path(location).request(
                MediaType.APPLICATION_JSON_TYPE).delete();

        final int status = response.getStatus();

        assertThat(status, is(204));

        verify(mockAdminService);
    }

    /**
     * Tests the results of checking network existence.
     */
    @Test
    public void testHasNetworkWithValidNetwork() {
        expect(mockAdminService.network(anyString())).andReturn(k8sNetwork);
        replay(mockAdminService);

        String location = PATH + "/exist/network-1";

        final WebTarget wt = target();
        String response = wt.path(location).request(
                MediaType.APPLICATION_JSON_TYPE).get(String.class);

        assertThat(response, is("{\"result\":true}"));

        verify((mockAdminService));
    }

    /**
     * Tests the results of checking network existence.
     */
    @Test
    public void testHasNetworkWithNullNetwork() {
        expect(mockAdminService.network(anyString())).andReturn(null);
        replay(mockAdminService);

        String location = PATH + "/exist/network-1";

        final WebTarget wt = target();
        String response = wt.path(location).request(
                MediaType.APPLICATION_JSON_TYPE).get(String.class);

        assertThat(response, is("{\"result\":false}"));

        verify((mockAdminService));
    }
}
