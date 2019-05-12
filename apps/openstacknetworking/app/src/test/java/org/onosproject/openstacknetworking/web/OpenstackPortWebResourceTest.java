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
import org.onosproject.openstacknode.api.OpenstackNodeService;
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
 * Unit test for openstack port REST API.
 */
public class OpenstackPortWebResourceTest extends ResourceTest {

    final OpenstackNetworkAdminService mockOpenstackNetworkAdminService =
            createMock(OpenstackNetworkAdminService.class);
    final OpenstackNodeService mockOpenstackNodeService =
            createMock(OpenstackNodeService.class);
    final OpenstackHaService mockOpenstackHaService = createMock(OpenstackHaService.class);

    private static final String PATH = "ports";

    /**
     * Constructs an openstack port test instance.
     */
    public OpenstackPortWebResourceTest() {
        super(ResourceConfig.forApplicationClass(OpenstackNetworkingWebApplication.class));
    }

    /**
     * Sets up the global values for all tests.
     */
    @Before
    public void setUpTest() {
        ServiceDirectory testDirectory = new TestServiceDirectory()
                .add(OpenstackNetworkAdminService.class, mockOpenstackNetworkAdminService)
                .add(OpenstackNodeService.class, mockOpenstackNodeService)
                .add(OpenstackHaService.class, mockOpenstackHaService);
        setServiceDirectory(testDirectory);

    }

    /**
     * Tests the results of the REST API POST with creation operation.
     */
    @Test
    public void testCreatePortWithCreationOperation() {
        mockOpenstackNetworkAdminService.createPort(anyObject());
        replay(mockOpenstackNetworkAdminService);
        expect(mockOpenstackHaService.isActive()).andReturn(true).anyTimes();
        replay(mockOpenstackHaService);

        final WebTarget wt = target();
        InputStream jsonStream = OpenstackNetworkWebResourceTest.class
                .getResourceAsStream("openstack-port.json");

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
    public void testCreatePortWithIncorrectInput() {
        expect(mockOpenstackHaService.isActive()).andReturn(true).anyTimes();
        replay(mockOpenstackHaService);

        final WebTarget wt = target();
        InputStream jsonStream = OpenstackPortWebResourceTest.class
                .getResourceAsStream("dummy.json");

        Response response = wt.path(PATH).request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(jsonStream));
        final int status = response.getStatus();

        assertThat(status, is(400));
    }

    /**
     * Tests the results of the REST API POST with duplicated port ID.
     */
    @Test
    public void testCreatePortWithDuplicatedId() {
        expect(mockOpenstackHaService.isActive()).andReturn(true).anyTimes();
        replay(mockOpenstackHaService);
        mockOpenstackNetworkAdminService.createPort(anyObject());
        expectLastCall().andThrow(new IllegalArgumentException());
        replay(mockOpenstackNetworkAdminService);

        final WebTarget wt = target();
        InputStream jsonStream = OpenstackNetworkWebResourceTest.class
                .getResourceAsStream("openstack-port.json");

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
    public void testUpdatePortWithUpdatingOperation() {
        expect(mockOpenstackHaService.isActive()).andReturn(true).anyTimes();
        replay(mockOpenstackHaService);
        mockOpenstackNetworkAdminService.updatePort(anyObject());
        replay(mockOpenstackNetworkAdminService);

        final WebTarget wt = target();
        InputStream jsonStream = OpenstackNetworkWebResourceTest.class
                .getResourceAsStream("openstack-port.json");

        Response response = wt.path(PATH + "/65c0ee9f-d634-4522-8954-51021b570b0d")
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
    public void testUpdatePortWithIncorrectInput() {
        expect(mockOpenstackHaService.isActive()).andReturn(true).anyTimes();
        replay(mockOpenstackHaService);

        final WebTarget wt = target();
        InputStream jsonStream = OpenstackNetworkWebResourceTest.class
                .getResourceAsStream("dummy.json");

        Response response = wt.path(PATH + "/65c0ee9f-d634-4522-8954-51021b570b0d")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .put(Entity.json(jsonStream));
        final int status = response.getStatus();

        assertThat(status, is(400));
    }

    /**
     * Tests the results of the REST API PUT with non-existing port ID.
     */
    @Test
    public void testUpdatePortWithNonexistId() {
        expect(mockOpenstackHaService.isActive()).andReturn(true).anyTimes();
        replay(mockOpenstackHaService);

        mockOpenstackNetworkAdminService.updatePort(anyObject());
        expectLastCall().andThrow(new IllegalArgumentException());
        replay(mockOpenstackNetworkAdminService);

        final WebTarget wt = target();
        InputStream jsonStream = OpenstackNetworkWebResourceTest.class
                .getResourceAsStream("openstack-port.json");

        Response response = wt.path(PATH + "/65c0ee9f-d634-4522-8954-51021b570b0d")
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
    public void testDeletePortWithDeletionOperation() {
        expect(mockOpenstackHaService.isActive()).andReturn(true).anyTimes();
        replay(mockOpenstackHaService);

        mockOpenstackNetworkAdminService.removePort(anyString());
        replay(mockOpenstackNetworkAdminService);

        final WebTarget wt = target();

        Response response = wt.path(PATH + "/65c0ee9f-d634-4522-8954-51021b570b0d")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .delete();
        final int status = response.getStatus();

        assertThat(status, is(204));

        verify(mockOpenstackNetworkAdminService);
    }

    /**
     * Tests the results of the REST API DELETE with non-existing port ID.
     */
    @Test
    public void testDeletePortWithNonexistId() {
        expect(mockOpenstackHaService.isActive()).andReturn(true).anyTimes();
        replay(mockOpenstackHaService);

        mockOpenstackNetworkAdminService.removePort(anyString());
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
