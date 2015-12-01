/*
 * Copyright 2014-2015 Open Networking Laboratory
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
package org.onosproject.vtnweb.resources;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.ws.rs.core.MediaType;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.osgi.TestServiceDirectory;
import org.onlab.rest.BaseResource;
import org.onosproject.codec.CodecService;
import org.onosproject.vtnrsc.PortPairGroup;
import org.onosproject.vtnrsc.PortPairGroupId;
import org.onosproject.vtnrsc.PortPairId;
import org.onosproject.vtnrsc.TenantId;
import org.onosproject.vtnrsc.portpairgroup.PortPairGroupService;
import org.onosproject.vtnweb.web.SfcCodecContext;

import com.eclipsesource.json.JsonObject;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
/**
 * Unit tests for port pair group REST APIs.
 */
public class PortPairGroupResourceTest extends VtnResourceTest {

    final PortPairGroupService portPairGroupService = createMock(PortPairGroupService.class);

    PortPairGroupId portPairGroupId1 = PortPairGroupId.of("4512d643-24fc-4fae-af4b-321c5e2eb3d1");
    TenantId tenantId1 = TenantId.tenantId("d382007aa9904763a801f68ecf065cf5");
    private final List<PortPairId> portPairList1 = Lists.newArrayList();

    final MockPortPairGroup portPairGroup1 = new MockPortPairGroup(portPairGroupId1, tenantId1, "portPairGroup1",
                                                                   "Mock port pair group", portPairList1);

    /**
     * Mock class for a port pair group.
     */
    private static class MockPortPairGroup implements PortPairGroup {

        private final PortPairGroupId portPairGroupId;
        private final TenantId tenantId;
        private final String name;
        private final String description;
        private final List<PortPairId> portPairList;

        public MockPortPairGroup(PortPairGroupId portPairGroupId, TenantId tenantId,
                                 String name, String description,
                                 List<PortPairId> portPairList) {

            this.portPairGroupId = portPairGroupId;
            this.tenantId = tenantId;
            this.name = name;
            this.description = description;
            this.portPairList = portPairList;
        }

        @Override
        public PortPairGroupId portPairGroupId() {
            return portPairGroupId;
        }

        @Override
        public TenantId tenantId() {
            return tenantId;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public String description() {
            return description;
        }

        @Override
        public List<PortPairId> portPairs() {
            return ImmutableList.copyOf(portPairList);
        }

        @Override
        public boolean exactMatch(PortPairGroup portPairGroup) {
            return this.equals(portPairGroup) &&
                    Objects.equals(this.portPairGroupId, portPairGroup.portPairGroupId()) &&
                    Objects.equals(this.tenantId, portPairGroup.tenantId());
        }
    }

    /**
     * Sets up the global values for all the tests.
     */
    @Before
    public void setUpTest() {
        SfcCodecContext context = new SfcCodecContext();
        ServiceDirectory testDirectory = new TestServiceDirectory()
        .add(PortPairGroupService.class, portPairGroupService)
        .add(CodecService.class, context.codecManager());
        BaseResource.setServiceDirectory(testDirectory);

    }

    /**
     * Cleans up.
     */
    @After
    public void tearDownTest() {
    }

    /**
     * Tests the result of the rest api GET when there are no port pair groups.
     */
    @Test
    public void testPortPairGroupsEmpty() {

        expect(portPairGroupService.getPortPairGroups()).andReturn(null).anyTimes();
        replay(portPairGroupService);
        final WebResource rs = resource();
        final String response = rs.path("port_pair_groups").get(String.class);
        assertThat(response, is("{\"port_pair_groups\":[]}"));
    }

    /**
     * Tests the result of a rest api GET for port pair group id.
     */
    @Test
    public void testGetPortPairGroupId() {

        final Set<PortPairGroup> portPairGroups = new HashSet<>();
        portPairGroups.add(portPairGroup1);

        expect(portPairGroupService.exists(anyObject())).andReturn(true).anyTimes();
        expect(portPairGroupService.getPortPairGroup(anyObject())).andReturn(portPairGroup1).anyTimes();
        replay(portPairGroupService);

        final WebResource rs = resource();
        final String response = rs.path("port_pair_groups/4512d643-24fc-4fae-af4b-321c5e2eb3d1").get(String.class);
        final JsonObject result = JsonObject.readFrom(response);
        assertThat(result, notNullValue());
    }

    /**
     * Tests that a fetch of a non-existent port pair group object throws an exception.
     */
    @Test
    public void testBadGet() {
        expect(portPairGroupService.getPortPairGroup(anyObject()))
        .andReturn(null).anyTimes();
        replay(portPairGroupService);
        WebResource rs = resource();
        try {
            rs.path("port_pair_groups/78dcd363-fc23-aeb6-f44b-56dc5aafb3ae").get(String.class);
            fail("Fetch of non-existent port pair group did not throw an exception");
        } catch (UniformInterfaceException ex) {
            assertThat(ex.getMessage(),
                       containsString("returned a response status of"));
        }
    }

    /**
     * Tests creating a port pair group with POST.
     */
    @Test
    public void testPost() {

        expect(portPairGroupService.createPortPairGroup(anyObject()))
        .andReturn(true).anyTimes();
        replay(portPairGroupService);

        WebResource rs = resource();
        InputStream jsonStream = PortPairGroupResourceTest.class.getResourceAsStream("post-PortPairGroup.json");

        ClientResponse response = rs.path("port_pair_groups")
                .type(MediaType.APPLICATION_JSON_TYPE)
                .post(ClientResponse.class, jsonStream);
        assertThat(response.getStatus(), is(HttpURLConnection.HTTP_OK));
    }

    /**
     * Tests deleting a port pair group.
     */
    @Test
    public void testDelete() {
        expect(portPairGroupService.removePortPairGroup(anyObject()))
        .andReturn(true).anyTimes();
        replay(portPairGroupService);

        WebResource rs = resource();

        String location = "port_pair_groups/4512d643-24fc-4fae-af4b-321c5e2eb3d1";

        ClientResponse deleteResponse = rs.path(location)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .delete(ClientResponse.class);
        assertThat(deleteResponse.getStatus(),
                   is(HttpURLConnection.HTTP_NO_CONTENT));
    }
}
