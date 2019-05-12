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
 * Unit test for openstack floating IP REST API.
 */
public class OpenstackFloatingIpWebResourceTest extends ResourceTest {

    final OpenstackRouterAdminService mockOpenstackRouterAdminService =
            createMock(OpenstackRouterAdminService.class);
    final OpenstackHaService mockOpenstackHaService = createMock(OpenstackHaService.class);
    private static final String PATH = "floatingips";

    /**
     * Constructs an openstack floating IP test instance.
     */
    public OpenstackFloatingIpWebResourceTest() {
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
    public void testCreateFloatingIpWithCreationOperation() {
        mockOpenstackRouterAdminService.createFloatingIp(anyObject());
        replay(mockOpenstackRouterAdminService);
        expect(mockOpenstackHaService.isActive()).andReturn(true).anyTimes();
        replay(mockOpenstackHaService);

        final WebTarget wt = target();
        InputStream jsonStream = OpenstackFloatingIpWebResourceTest.class
                .getResourceAsStream("openstack-floatingip1.json");

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
    public void testCreateFloatingIpWithIncorrectInput() {
        expect(mockOpenstackHaService.isActive()).andReturn(true).anyTimes();
        replay(mockOpenstackHaService);
        final WebTarget wt = target();
        InputStream jsonStream = OpenstackFloatingIpWebResourceTest.class
                .getResourceAsStream("dummy.json");

        Response response = wt.path(PATH).request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(jsonStream));
        final int status = response.getStatus();

        assertThat(status, is(400));
    }

    /**
     * Tests the results of the REST API POST with duplicated floating IP.
     */
    @Test
    public void testCreateFloatingIpWithDuplicatedIp() {
        expect(mockOpenstackHaService.isActive()).andReturn(true).anyTimes();
        replay(mockOpenstackHaService);
        mockOpenstackRouterAdminService.createFloatingIp(anyObject());
        expectLastCall().andThrow(new IllegalArgumentException());
        replay(mockOpenstackRouterAdminService);

        final WebTarget wt = target();
        InputStream jsonStream = OpenstackFloatingIpWebResourceTest.class
                .getResourceAsStream("openstack-floatingip1.json");

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
    public void testUpdateFloatingIpWithUpdatingOperation() {
        expect(mockOpenstackHaService.isActive()).andReturn(true).anyTimes();
        replay(mockOpenstackHaService);
        mockOpenstackRouterAdminService.updateFloatingIp(anyObject());
        replay(mockOpenstackRouterAdminService);

        final WebTarget wt = target();
        InputStream jsonStream = OpenstackFloatingIpWebResourceTest.class
                .getResourceAsStream("openstack-floatingip1.json");

        Response response = wt.path(PATH + "/2f245a7b-796b-4f26-9cf9-9e82d248fda7")
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
    public void testUpdateFloatingIpWithIncorrectInput() {
        expect(mockOpenstackHaService.isActive()).andReturn(true).anyTimes();
        replay(mockOpenstackHaService);
        final WebTarget wt = target();
        InputStream jsonStream = OpenstackFloatingIpWebResourceTest.class
                .getResourceAsStream("dummy.json");

        Response response = wt.path(PATH + "/2f245a7b-796b-4f26-9cf9-9e82d248fda7")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .put(Entity.json(jsonStream));
        final int status = response.getStatus();

        assertThat(status, is(400));
    }

    /**
     * Tests the results of the REST API PUT with non-existing ID.
     */
    @Test
    public void testUpdateFloatingIpWithNonexistId() {
        expect(mockOpenstackHaService.isActive()).andReturn(true).anyTimes();
        replay(mockOpenstackHaService);
        mockOpenstackRouterAdminService.updateFloatingIp(anyObject());
        expectLastCall().andThrow(new IllegalArgumentException());
        replay(mockOpenstackRouterAdminService);

        final WebTarget wt = target();
        InputStream jsonStream = OpenstackFloatingIpWebResourceTest.class
                .getResourceAsStream("openstack-floatingip1.json");

        Response response = wt.path(PATH + "/2f245a7b-796b-4f26-9cf9-9e82d248fda7")
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
    public void testDeleteFloatingIpWithDeletionOperation() {
        expect(mockOpenstackHaService.isActive()).andReturn(true).anyTimes();
        replay(mockOpenstackHaService);
        mockOpenstackRouterAdminService.removeFloatingIp(anyString());
        replay(mockOpenstackRouterAdminService);

        final WebTarget wt = target();

        Response response = wt.path(PATH + "/2f245a7b-796b-4f26-9cf9-9e82d248fda7")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .delete();
        final int status = response.getStatus();

        assertThat(status, is(204));

        verify(mockOpenstackRouterAdminService);
    }

    /**
     * Tests the results of the REST API DELETE with non-existing ID.
     */
    @Test
    public void testDeleteFloatingIpWithNonexistId() {
        expect(mockOpenstackHaService.isActive()).andReturn(true).anyTimes();
        replay(mockOpenstackHaService);
        mockOpenstackRouterAdminService.removeFloatingIp(anyString());
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
}
