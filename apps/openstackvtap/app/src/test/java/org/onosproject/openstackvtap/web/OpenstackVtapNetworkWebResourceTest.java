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
package org.onosproject.openstackvtap.web;

import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Before;
import org.junit.Test;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.osgi.TestServiceDirectory;
import org.onlab.packet.IpAddress;
import org.onosproject.codec.CodecService;
import org.onosproject.codec.impl.CodecManager;
import org.onosproject.openstackvtap.api.OpenstackVtapNetwork;
import org.onosproject.openstackvtap.api.OpenstackVtapService;
import org.onosproject.openstackvtap.codec.OpenstackVtapNetworkCodec;
import org.onosproject.openstackvtap.impl.DefaultOpenstackVtapNetwork;
import org.onosproject.rest.resources.ResourceTest;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Unit test for openstack vtap network REST API.
 */
public class OpenstackVtapNetworkWebResourceTest extends ResourceTest {

    final OpenstackVtapService mockVtapService = createMock(OpenstackVtapService.class);
    private static final String PATH = "vtap-network";

    /**
     * Constructs an openstack vtap network resource test instance.
     */
    public OpenstackVtapNetworkWebResourceTest() {
        super(ResourceConfig.forApplicationClass(OpenstackVtapWebApplication.class));
    }

    private OpenstackVtapNetwork vtapNetwork;

    /**
     * Sets up the global values for all the tests.
     */
    @Before
    public void setUpTest() {
        final CodecManager codecService = new CodecManager();
        codecService.activate();
        codecService.registerCodec(OpenstackVtapNetwork.class, new OpenstackVtapNetworkCodec());
        ServiceDirectory testDirectory =
                new TestServiceDirectory()
                        .add(OpenstackVtapService.class, mockVtapService)
                        .add(CodecService.class, codecService);
        setServiceDirectory(testDirectory);

        vtapNetwork = DefaultOpenstackVtapNetwork.builder()
                .mode(OpenstackVtapNetwork.Mode.VXLAN)
                .networkId(1)
                .serverIp(IpAddress.valueOf("10.10.10.1"))
                .build();
    }

    /**
     * Tests the results of the REST API POST method with creating new vtap network operation.
     */
    @Test
    public void testCreateVtapNetworkWithCreateOperation() {
        expect(mockVtapService.createVtapNetwork(anyObject(), anyObject(), anyObject()))
                .andReturn(vtapNetwork).once();
        replay(mockVtapService);

        final WebTarget wt = target();
        InputStream jsonStream = OpenstackVtapNetworkWebResourceTest.class
                .getResourceAsStream("openstack-vtap-config.json");
        Response response = wt.path(PATH).request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(jsonStream));
        final int status = response.getStatus();

        assertThat(status, is(200));

        verify(mockVtapService);
    }

    /**
     * Tests the results of the REST API POST method without creating new vtap network operation.
     */
    @Test
    public void testCreateVtapNetworkWithoutCreateOperation() {
        final WebTarget wt = target();
        InputStream jsonStream = OpenstackVtapNetworkWebResourceTest.class
                .getResourceAsStream("openstack-vtap-config.json");
        Response response = wt.path(PATH).request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(jsonStream));
        final int status = response.getStatus();

        assertThat(status, is(409));
    }

    /**
     * Tests the results of the REST API PUT method with modifying the vtap network.
     */
    @Test
    public void testUpdateVtapNetworkWithoutModifyOperation() {
        expect(mockVtapService.updateVtapNetwork(anyObject())).andReturn(vtapNetwork).once();
        replay(mockVtapService);

        final WebTarget wt = target();
        InputStream jsonStream = OpenstackVtapNetworkWebResourceTest.class
                .getResourceAsStream("openstack-vtap-config.json");
        Response response = wt.path(PATH).request(MediaType.APPLICATION_JSON_TYPE)
                .put(Entity.json(jsonStream));
        final int status = response.getStatus();

        assertThat(status, is(200));

        verify(mockVtapService);
    }

    /**
     * Tests the results of the REST API PUT method without modifying the vtap network.
     */
    @Test
    public void testUpdateVtapNetworkWithModifyOperation() {
        final WebTarget wt = target();
        InputStream jsonStream = OpenstackVtapNetworkWebResourceTest.class
                .getResourceAsStream("openstack-vtap-config.json");
        Response response = wt.path(PATH).request(MediaType.APPLICATION_JSON_TYPE)
                .put(Entity.json(jsonStream));
        final int status = response.getStatus();

        assertThat(status, is(404));
    }

    /**
     * Tests the results of the REST API DELETE method with deleting the vtap network.
     */
    @Test
    public void testDeleteVtapNetworkWithDeletionOperation() {
        expect(mockVtapService.removeVtapNetwork()).andReturn(vtapNetwork).once();
        replay(mockVtapService);

        final WebTarget wt = target();
        Response response = wt.path(PATH).request(MediaType.APPLICATION_JSON_TYPE)
                .delete();

        final int status = response.getStatus();

        assertThat(status, is(200));

        verify(mockVtapService);
    }

    /**
     * Tests the results of the REST API DELETE method without deleting the vtap network.
     */
    @Test
    public void testDeleteVtapNetworkWithoutDeletionOperation() {
        final WebTarget wt = target();
        Response response = wt.path(PATH).request(MediaType.APPLICATION_JSON_TYPE)
                .delete();

        final int status = response.getStatus();

        assertThat(status, is(404));
    }

    /**
     * Tests the results of the REST API GET method with getting the vtap network.
     */
    @Test
    public void testGetVtapNetworkWithGetOperation() {
        expect(mockVtapService.getVtapNetwork()).andReturn(vtapNetwork).once();
        replay(mockVtapService);

        final WebTarget wt = target();
        Response response = wt.path(PATH).request(MediaType.APPLICATION_JSON_TYPE)
                .get();

        final int status = response.getStatus();

        assertThat(status, is(200));

        verify(mockVtapService);
    }

    /**
     * Tests the results of the REST API GET method without getting the vtap network.
     */
    @Test
    public void testGetVtapNetworkWithoutGetOperation() {
        final WebTarget wt = target();
        Response response = wt.path(PATH).request(MediaType.APPLICATION_JSON_TYPE)
                .get();

        final int status = response.getStatus();

        assertThat(status, is(404));
    }

}
