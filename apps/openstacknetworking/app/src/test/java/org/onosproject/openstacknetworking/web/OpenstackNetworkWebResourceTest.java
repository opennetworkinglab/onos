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
package org.onosproject.openstacknetworking.web;

import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Before;
import org.junit.Test;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.osgi.TestServiceDirectory;
import org.onosproject.openstacknetworking.api.OpenstackHaService;
import org.onosproject.openstacknetworking.api.OpenstackNetworkAdminService;
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
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Unit test for openstack network REST API.
 */
public class OpenstackNetworkWebResourceTest extends ResourceTest {

    final OpenstackNetworkAdminService mockOpenstackNetworkAdminService =
            createMock(OpenstackNetworkAdminService.class);
    final OpenstackHaService mockOpenstackHaService = createMock(OpenstackHaService.class);
    private static final String PATH = "networks";

    /**
     * Constructs an openstack network test instance.
     */
    public OpenstackNetworkWebResourceTest() {
        super(ResourceConfig.forApplicationClass(OpenstackNetworkingWebApplication.class));
    }

    /**
     * Sets up the global values for all tests.
     */
    @Before
    public void setUpTest() {
        ServiceDirectory testDirectory =
                new TestServiceDirectory()
                        .add(OpenstackNetworkAdminService.class,
                                mockOpenstackNetworkAdminService)
                        .add(OpenstackHaService.class, mockOpenstackHaService);
        setServiceDirectory(testDirectory);
    }

    /**
     * Tests the results of the REST API POST with creation operation.
     */
    @Test
    public void testCreateNetworkWithCreationOperation() {
        mockOpenstackNetworkAdminService.createNetwork(anyObject());
        replay(mockOpenstackNetworkAdminService);
        expect(mockOpenstackHaService.isActive()).andReturn(true).anyTimes();
        replay(mockOpenstackHaService);

        final WebTarget wt = target();
        InputStream jsonStream = OpenstackNetworkWebResourceTest.class
                .getResourceAsStream("openstack-network.json");

        Response response = wt.path(PATH).request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(jsonStream));
        final int status = response.getStatus();

        assertThat(status, is(201));

        verify(mockOpenstackNetworkAdminService);
    }

    /**
     * Tests the results of the REST API POST with incorrect input.
     */
    @Test
    public void testCreateNetworkWithIncorrectInput() {
        expect(mockOpenstackHaService.isActive()).andReturn(true).anyTimes();
        replay(mockOpenstackHaService);

        final WebTarget wt = target();
        InputStream jsonStream = OpenstackNetworkWebResourceTest.class
                .getResourceAsStream("dummy.json");

        Response response = wt.path(PATH).request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(jsonStream));
        final int status = response.getStatus();

        assertThat(status, is(400));
    }

    /**
     * Tests the results of the REST API POST with duplicated network ID.
     */
    @Test
    public void testCreateNetworkWithDuplicatedId() {
        mockOpenstackNetworkAdminService.createNetwork(anyObject());
        expectLastCall().andThrow(new IllegalArgumentException());
        replay(mockOpenstackNetworkAdminService);
        expect(mockOpenstackHaService.isActive()).andReturn(true).anyTimes();
        replay(mockOpenstackHaService);

        final WebTarget wt = target();
        InputStream jsonStream = OpenstackNetworkWebResourceTest.class
                .getResourceAsStream("openstack-network.json");

        Response response = wt.path(PATH).request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(jsonStream));
        final int status = response.getStatus();

        assertThat(status, is(400));

        verify(mockOpenstackNetworkAdminService);
    }

    /**
     * Tests the results of the REST API PUT with updating operation.
     */
    @Test
    public void testUpdateNetworkWithUpdatingOperation() {
        mockOpenstackNetworkAdminService.updateNetwork(anyObject());
        replay(mockOpenstackNetworkAdminService);
        expect(mockOpenstackHaService.isActive()).andReturn(true).anyTimes();
        replay(mockOpenstackHaService);

        final WebTarget wt = target();
        InputStream jsonStream = OpenstackNetworkWebResourceTest.class
                .getResourceAsStream("openstack-network.json");

        Response response = wt.path(PATH + "/396f12f8-521e-4b91-8e21-2e003500433a")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .put(Entity.json(jsonStream));
        final int status = response.getStatus();

        assertThat(status, is(200));

        verify(mockOpenstackNetworkAdminService);
    }

    /**
     * Tests the results of the REST API PUT with incorrect input.
     */
    @Test
    public void testUpdateNetworkWithIncorrectInput() {
        expect(mockOpenstackHaService.isActive()).andReturn(true).anyTimes();
        replay(mockOpenstackHaService);

        final WebTarget wt = target();
        InputStream jsonStream = OpenstackNetworkWebResourceTest.class
                .getResourceAsStream("dummy.json");

        Response response = wt.path(PATH + "/396f12f8-521e-4b91-8e21-2e003500433a")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .put(Entity.json(jsonStream));
        final int status = response.getStatus();

        assertThat(status, is(400));
    }

    /**
     * Tests the results of the REST API PUT with non-existing network ID.
     */
    @Test
    public void testUpdateNetworkWithNonexistId() {
        expect(mockOpenstackHaService.isActive()).andReturn(true).anyTimes();
        replay(mockOpenstackHaService);
        mockOpenstackNetworkAdminService.updateNetwork(anyObject());
        expectLastCall().andThrow(new IllegalArgumentException());
        replay(mockOpenstackNetworkAdminService);

        final WebTarget wt = target();
        InputStream jsonStream = OpenstackNetworkWebResourceTest.class
                .getResourceAsStream("openstack-network.json");

        Response response = wt.path(PATH + "/396f12f8-521e-4b91-8e21-2e003500433a")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .put(Entity.json(jsonStream));
        final int status = response.getStatus();

        assertThat(status, is(400));

        verify(mockOpenstackNetworkAdminService);
    }

    /**
     * Tests the results of the REST API DELETE with deletion operation.
     */
    @Test
    public void testDeleteNetworkWithDeletionOperation() {
        expect(mockOpenstackHaService.isActive()).andReturn(true).anyTimes();
        replay(mockOpenstackHaService);
        mockOpenstackNetworkAdminService.removeNetwork(anyString());
        replay(mockOpenstackNetworkAdminService);

        final WebTarget wt = target();

        Response response = wt.path(PATH + "/396f12f8-521e-4b91-8e21-2e003500433a")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .delete();
        final int status = response.getStatus();

        assertThat(status, is(204));

        verify(mockOpenstackNetworkAdminService);
    }

    /**
     * Tests the results of the REST API DELETE with non-existing network ID.
     */
    @Test
    public void testDeleteNetworkWithNonexistId() {
        expect(mockOpenstackHaService.isActive()).andReturn(true).anyTimes();
        replay(mockOpenstackHaService);
        mockOpenstackNetworkAdminService.removeNetwork(anyString());
        expectLastCall().andThrow(new IllegalArgumentException());
        replay(mockOpenstackNetworkAdminService);

        final WebTarget wt = target();

        Response response = wt.path(PATH + "/non-exist-id")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .delete();
        final int status = response.getStatus();

        assertThat(status, is(400));

        verify(mockOpenstackNetworkAdminService);
    }
}
