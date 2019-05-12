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
import org.onosproject.openstacknetworking.api.OpenstackSecurityGroupAdminService;
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
 * Unit test for openstack security group REST API.
 */
public class OpenstackSecurityGroupWebResourceTest extends ResourceTest {

    final OpenstackSecurityGroupAdminService mockOpenstackSecurityGroupAdminService =
            createMock(OpenstackSecurityGroupAdminService.class);
    final OpenstackHaService mockOpenstackHaService = createMock(OpenstackHaService.class);
    private static final String PATH = "security-groups";

    /**
     * Constructs an openstack security group test instance.
     */
    public OpenstackSecurityGroupWebResourceTest() {
        super(ResourceConfig.forApplicationClass(OpenstackNetworkingWebApplication.class));
    }

    /**
     * Sets up the global values for all tests.
     */
    @Before
    public void setUpTest() {
        ServiceDirectory testDirectory =
                new TestServiceDirectory()
                        .add(OpenstackSecurityGroupAdminService.class,
                                mockOpenstackSecurityGroupAdminService)
                        .add(OpenstackHaService.class, mockOpenstackHaService);
        setServiceDirectory(testDirectory);
    }

    /**
     * Tests the results of the REST API POST with creation operation.
     */
    @Test
    public void testCreateSecurityGroupWithCreationOperation() {
        mockOpenstackSecurityGroupAdminService.createSecurityGroup(anyObject());
        replay(mockOpenstackSecurityGroupAdminService);
        expect(mockOpenstackHaService.isActive()).andReturn(true).anyTimes();
        replay(mockOpenstackHaService);

        final WebTarget wt = target();
        InputStream jsonStream = OpenstackSecurityGroupWebResourceTest.class
                .getResourceAsStream("openstack-security-group.json");

        Response response = wt.path(PATH).request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(jsonStream));
        final int status = response.getStatus();

        assertThat(status, is(201));

        verify(mockOpenstackSecurityGroupAdminService);
    }

    /**
     * Tests the results of the REST API POST with incorrect input.
     */
    @Test
    public void testCreateSecurityGroupWithIncorrectInput() {
        expect(mockOpenstackHaService.isActive()).andReturn(true).anyTimes();
        replay(mockOpenstackHaService);

        final WebTarget wt = target();
        InputStream jsonStream = OpenstackSecurityGroupWebResourceTest.class
                .getResourceAsStream("dummy.json");

        Response response = wt.path(PATH).request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(jsonStream));
        final int status = response.getStatus();

        assertThat(status, is(400));
    }

    /**
     * Tests the results of the REST API POST with duplicated security group ID.
     */
    @Test
    public void testCreateSecurityGroupWithDuplicatedId() {
        expect(mockOpenstackHaService.isActive()).andReturn(true).anyTimes();
        replay(mockOpenstackHaService);
        mockOpenstackSecurityGroupAdminService.createSecurityGroup(anyObject());
        expectLastCall().andThrow(new IllegalArgumentException());
        replay(mockOpenstackSecurityGroupAdminService);

        final WebTarget wt = target();
        InputStream jsonStream = OpenstackSecurityGroupWebResourceTest.class
                .getResourceAsStream("openstack-security-group.json");

        Response response = wt.path(PATH).request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(jsonStream));
        final int status = response.getStatus();

        assertThat(status, is(400));

        verify(mockOpenstackSecurityGroupAdminService);
    }

    /**
     * Tests the results of the REST API PUT with updating operation.
     */
    @Test
    public void testUpdateSecurityGroupWithUpdatingOperation() {
        expect(mockOpenstackHaService.isActive()).andReturn(true).anyTimes();
        replay(mockOpenstackHaService);

        final WebTarget wt = target();
        InputStream jsonStream = OpenstackSecurityGroupWebResourceTest.class
                .getResourceAsStream("openstack-security-group.json");

        Response response = wt.path(PATH)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .put(Entity.json(jsonStream));
        final int status = response.getStatus();

        assertThat(status, is(200));
    }

    /**
     * Tests the results of the REST API DELETE with deletion operation.
     */
    @Test
    public void testDeleteSecurityGroupWithDeletionOperation() {
        expect(mockOpenstackHaService.isActive()).andReturn(true).anyTimes();
        replay(mockOpenstackHaService);
        mockOpenstackSecurityGroupAdminService.removeSecurityGroup(anyString());
        replay(mockOpenstackSecurityGroupAdminService);

        final WebTarget wt = target();

        Response response = wt.path(PATH + "/2076db17-a522-4506-91de-c6dd8e837028")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .delete();
        final int status = response.getStatus();

        assertThat(status, is(204));

        verify(mockOpenstackSecurityGroupAdminService);
    }

    /**
     * Tests the results of the REST API DELETE with non-existing security group ID.
     */
    @Test
    public void testDeleteSecurityGroupWithNonexistId() {
        expect(mockOpenstackHaService.isActive()).andReturn(true).anyTimes();
        replay(mockOpenstackHaService);
        mockOpenstackSecurityGroupAdminService.removeSecurityGroup(anyString());
        expectLastCall().andThrow(new IllegalArgumentException());
        replay(mockOpenstackSecurityGroupAdminService);

        final WebTarget wt = target();

        Response response = wt.path(PATH + "/non-exist-id")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .delete();
        final int status = response.getStatus();

        assertThat(status, is(400));

        verify(mockOpenstackSecurityGroupAdminService);
    }
}