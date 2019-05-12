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
import org.onosproject.openstacknetworking.api.OpenstackRouterAdminService;
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
 * Unit test for openstack router REST API.
 */
public class OpenstackRouterWebResourceTest extends ResourceTest {

    final OpenstackRouterAdminService mockOpenstackRouterAdminService =
            createMock(OpenstackRouterAdminService.class);
    final OpenstackHaService mockOpenstackHaService = createMock(OpenstackHaService.class);
    private static final String PATH = "routers";

    /**
     * Constructs an openstack router test instance.
     */
    public OpenstackRouterWebResourceTest() {
        super(ResourceConfig.forApplicationClass(OpenstackNetworkingWebApplication.class));
    }

    /**
     * Sets up the global values for all tests.
     */
    @Before
    public void setUpTest() {
        ServiceDirectory testDirectory =
                new TestServiceDirectory()
                        .add(OpenstackRouterAdminService.class,
                                mockOpenstackRouterAdminService)
                        .add(OpenstackHaService.class, mockOpenstackHaService);
        setServiceDirectory(testDirectory);
    }

    /**
     * Tests the results of the REST API POST with creation operation.
     */
    @Test
    public void testCreateRouterWithCreationOperation() {
        mockOpenstackRouterAdminService.createRouter(anyObject());
        replay(mockOpenstackRouterAdminService);
        expect(mockOpenstackHaService.isActive()).andReturn(true).anyTimes();
        replay(mockOpenstackHaService);

        final WebTarget wt = target();
        InputStream jsonStream = OpenstackRouterWebResourceTest.class
                .getResourceAsStream("openstack-router.json");

        Response response = wt.path(PATH).request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(jsonStream));
        final int status = response.getStatus();

        assertThat(status, is(201));

        verify(mockOpenstackRouterAdminService);
    }

    /**
     * Tests the results of the REST API POST with incorrect input.
     */
    @Test
    public void testCreateRouterWithIncorrectInput() {
        expect(mockOpenstackHaService.isActive()).andReturn(true).anyTimes();
        replay(mockOpenstackHaService);

        final WebTarget wt = target();
        InputStream jsonStream = OpenstackRouterWebResourceTest.class
                .getResourceAsStream("dummy.json");

        Response response = wt.path(PATH).request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(jsonStream));
        final int status = response.getStatus();

        assertThat(status, is(400));
    }

    /**
     * Tests the results of the REST API POST with duplicated router ID.
     */
    @Test
    public void testCreateRouterWithDuplicatedId() {
        expect(mockOpenstackHaService.isActive()).andReturn(true).anyTimes();
        replay(mockOpenstackHaService);
        mockOpenstackRouterAdminService.createRouter(anyObject());
        expectLastCall().andThrow(new IllegalArgumentException());
        replay(mockOpenstackRouterAdminService);

        final WebTarget wt = target();
        InputStream jsonStream = OpenstackRouterWebResourceTest.class
                .getResourceAsStream("openstack-router.json");

        Response response = wt.path(PATH).request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(jsonStream));
        final int status = response.getStatus();

        assertThat(status, is(400));

        verify(mockOpenstackRouterAdminService);
    }

    /**
     * Tests the results of the REST API PUT with updating operation.
     */
    @Test
    public void testUpdateRouterWithUpdatingOperation() {
        expect(mockOpenstackHaService.isActive()).andReturn(true).anyTimes();
        replay(mockOpenstackHaService);
        mockOpenstackRouterAdminService.updateRouter(anyObject());
        replay(mockOpenstackRouterAdminService);

        final WebTarget wt = target();
        InputStream jsonStream = OpenstackRouterWebResourceTest.class
                .getResourceAsStream("openstack-router.json");

        Response response = wt.path(PATH + "/f49a1319-423a-4ee6-ba54-1d95a4f6cc68")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .put(Entity.json(jsonStream));
        final int status = response.getStatus();

        assertThat(status, is(200));

        verify(mockOpenstackRouterAdminService);
    }

    /**
     * Tests the results of the REST API PUT with incorrect input.
     */
    @Test
    public void testUpdateRouterWithIncorrectInput() {
        expect(mockOpenstackHaService.isActive()).andReturn(true).anyTimes();
        replay(mockOpenstackHaService);

        final WebTarget wt = target();
        InputStream jsonStream = OpenstackRouterWebResourceTest.class
                .getResourceAsStream("dummy.json");

        Response response = wt.path(PATH + "/f49a1319-423a-4ee6-ba54-1d95a4f6cc68")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .put(Entity.json(jsonStream));
        final int status = response.getStatus();

        assertThat(status, is(400));
    }

    /**
     * Tests the results of the REST API PUT with non-existing router ID.
     */
    @Test
    public void testUpdateRouterWithNonexistId() {
        expect(mockOpenstackHaService.isActive()).andReturn(true).anyTimes();
        replay(mockOpenstackHaService);
        mockOpenstackRouterAdminService.updateRouter(anyObject());
        expectLastCall().andThrow(new IllegalArgumentException());
        replay(mockOpenstackRouterAdminService);

        final WebTarget wt = target();
        InputStream jsonStream = OpenstackRouterWebResourceTest.class
                .getResourceAsStream("openstack-router.json");

        Response response = wt.path(PATH + "/f49a1319-423a-4ee6-ba54-1d95a4f6cc68")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .put(Entity.json(jsonStream));
        final int status = response.getStatus();

        assertThat(status, is(400));

        verify(mockOpenstackRouterAdminService);
    }

    /**
     * Tests the results of the REST API DELETE with deletion operation.
     */
    @Test
    public void testDeleteRouterWithDeletionOperation() {
        expect(mockOpenstackHaService.isActive()).andReturn(true).anyTimes();
        replay(mockOpenstackHaService);
        mockOpenstackRouterAdminService.removeRouter(anyString());
        replay(mockOpenstackRouterAdminService);

        final WebTarget wt = target();

        Response response = wt.path(PATH + "/f49a1319-423a-4ee6-ba54-1d95a4f6cc68")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .delete();
        final int status = response.getStatus();

        assertThat(status, is(204));

        verify(mockOpenstackRouterAdminService);
    }

    /**
     * Tests the results of the REST API DELETE with non-existing router ID.
     */
    @Test
    public void testDeleteRouterWithNonexistId() {
        expect(mockOpenstackHaService.isActive()).andReturn(true).anyTimes();
        replay(mockOpenstackHaService);
        mockOpenstackRouterAdminService.removeRouter(anyString());
        expectLastCall().andThrow(new IllegalArgumentException());
        replay(mockOpenstackRouterAdminService);

        final WebTarget wt = target();

        Response response = wt.path(PATH + "/non-exist-id")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .delete();
        final int status = response.getStatus();

        assertThat(status, is(400));

        verify(mockOpenstackRouterAdminService);
    }

    /**
     * Tests the results of the REST API PUT with adding a new router interface.
     */
    @Test
    public void testAddRouterInterfaceWithAdditionOperation() {
        expect(mockOpenstackHaService.isActive()).andReturn(true).anyTimes();
        replay(mockOpenstackHaService);
        mockOpenstackRouterAdminService.addRouterInterface(anyObject());
        replay(mockOpenstackRouterAdminService);

        final WebTarget wt = target();
        InputStream jsonStream = OpenstackRouterWebResourceTest.class
                .getResourceAsStream("openstack-router-interface.json");

        Response response = wt.path(PATH + "/f49a1319-423a-4ee6-ba54-1d95a4f6cc68/add_router_interface")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .put(Entity.json(jsonStream));
        final int status = response.getStatus();

        assertThat(status, is(200));

        verify(mockOpenstackRouterAdminService);
    }

    /**
     * Tests the results of the REST API PUT with non-existing router interface ID.
     */
    @Test
    public void testAddRouterInterfaceWithNonexistId() {
        expect(mockOpenstackHaService.isActive()).andReturn(true).anyTimes();
        replay(mockOpenstackHaService);
        mockOpenstackRouterAdminService.addRouterInterface(anyObject());
        expectLastCall().andThrow(new IllegalArgumentException());
        replay(mockOpenstackRouterAdminService);

        final WebTarget wt = target();
        InputStream jsonStream = OpenstackRouterWebResourceTest.class
                .getResourceAsStream("openstack-router-interface.json");

        Response response = wt.path(PATH + "/f49a1319-423a-4ee6-ba54-1d95a4f6cc68/add_router_interface")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .put(Entity.json(jsonStream));
        final int status = response.getStatus();

        assertThat(status, is(400));

        verify(mockOpenstackRouterAdminService);
    }

    /**
     * Tests the results of the REST API DELETE with deletion operation.
     */
    @Test
    public void testDeleteRouterInterfaceWithDeletionOperation() {
        expect(mockOpenstackHaService.isActive()).andReturn(true).anyTimes();
        replay(mockOpenstackHaService);
        mockOpenstackRouterAdminService.removeRouterInterface(anyString());
        replay(mockOpenstackRouterAdminService);

        final WebTarget wt = target();
        InputStream jsonStream = OpenstackRouterWebResourceTest.class
                .getResourceAsStream("openstack-router-interface.json");

        Response response = wt.path(PATH + "/f49a1319-423a-4ee6-ba54-1d95a4f6cc68/remove_router_interface")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .put(Entity.json(jsonStream));
        final int status = response.getStatus();

        assertThat(status, is(200));

        verify(mockOpenstackRouterAdminService);
    }

    /**
     * Tests the results of the REST API DELETE with non-existing router interface ID.
     */
    @Test
    public void testDeleteRouterInterfaceWithNonexistId() {
        expect(mockOpenstackHaService.isActive()).andReturn(true).anyTimes();
        replay(mockOpenstackHaService);
        mockOpenstackRouterAdminService.removeRouterInterface(anyString());
        expectLastCall().andThrow(new IllegalArgumentException());
        replay(mockOpenstackRouterAdminService);

        final WebTarget wt = target();
        InputStream jsonStream = OpenstackRouterWebResourceTest.class
                .getResourceAsStream("openstack-router-interface.json");

        Response response = wt.path(PATH + "/f49a1319-423a-4ee6-ba54-1d95a4f6cc68/remove_router_interface")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .put(Entity.json(jsonStream));
        final int status = response.getStatus();

        assertThat(status, is(400));

        verify(mockOpenstackRouterAdminService);
    }
}
