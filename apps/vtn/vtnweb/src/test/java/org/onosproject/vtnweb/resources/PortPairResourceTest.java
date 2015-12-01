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
import org.onosproject.vtnrsc.PortPair;
import org.onosproject.vtnrsc.PortPairId;
import org.onosproject.vtnrsc.TenantId;
import org.onosproject.vtnrsc.portpair.PortPairService;
import org.onosproject.vtnweb.web.SfcCodecContext;

import com.eclipsesource.json.JsonObject;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
/**
 * Unit tests for port pair REST APIs.
 */
public class PortPairResourceTest extends VtnResourceTest {

    final PortPairService portPairService = createMock(PortPairService.class);

    PortPairId portPairId1 = PortPairId.of("78dcd363-fc23-aeb6-f44b-56dc5e2fb3ae");
    TenantId tenantId1 = TenantId.tenantId("d382007aa9904763a801f68ecf065cf5");

    final MockPortPair portPair1 = new MockPortPair(portPairId1, tenantId1, "portPair1",
                                                    "Mock port pair", "dace4513-24fc-4fae-af4b-321c5e2eb3d1",
            "aef3478a-4a56-2a6e-cd3a-9dee4e2ec345");

    /**
     * Mock class for a port pair.
     */
    private static class MockPortPair implements PortPair {

        private final PortPairId portPairId;
        private final TenantId tenantId;
        private final String name;
        private final String description;
        private final String ingress;
        private final String egress;

        public MockPortPair(PortPairId portPairId, TenantId tenantId,
                            String name, String description,
                            String ingress, String egress) {

            this.portPairId = portPairId;
            this.tenantId = tenantId;
            this.name = name;
            this.description = description;
            this.ingress = ingress;
            this.egress = egress;
        }

        @Override
        public PortPairId portPairId() {
            return portPairId;
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
        public String ingress() {
            return ingress;
        }

        @Override
        public String egress() {
            return egress;
        }

        @Override
        public boolean exactMatch(PortPair portPair) {
            return this.equals(portPair) &&
                    Objects.equals(this.portPairId, portPair.portPairId()) &&
                    Objects.equals(this.tenantId, portPair.tenantId());
        }
    }

    /**
     * Sets up the global values for all the tests.
     */
    @Before
    public void setUpTest() {

        SfcCodecContext context = new SfcCodecContext();
        ServiceDirectory testDirectory = new TestServiceDirectory().add(PortPairService.class, portPairService)
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
     * Tests the result of the rest api GET when there are no port pairs.
     */
    @Test
    public void testPortPairsEmpty() {

        expect(portPairService.getPortPairs()).andReturn(null).anyTimes();
        replay(portPairService);
        final WebResource rs = resource();
        final String response = rs.path("port_pairs").get(String.class);
        assertThat(response, is("{\"port_pairs\":[]}"));
    }

    /**
     * Tests the result of a rest api GET for port pair id.
     */
    @Test
    public void testGetPortPairId() {

        final Set<PortPair> portPairs = new HashSet<>();
        portPairs.add(portPair1);

        expect(portPairService.exists(anyObject())).andReturn(true).anyTimes();
        expect(portPairService.getPortPair(anyObject())).andReturn(portPair1).anyTimes();
        replay(portPairService);

        final WebResource rs = resource();
        final String response = rs.path("port_pairs/78dcd363-fc23-aeb6-f44b-56dc5e2fb3ae").get(String.class);
        final JsonObject result = JsonObject.readFrom(response);
        assertThat(result, notNullValue());
    }

    /**
     * Tests that a fetch of a non-existent port pair object throws an exception.
     */
    @Test
    public void testBadGet() {
        expect(portPairService.getPortPair(anyObject()))
        .andReturn(null).anyTimes();
        replay(portPairService);
        WebResource rs = resource();
        try {
            rs.path("port_pairs/78dcd363-fc23-aeb6-f44b-56dc5aafb3ae").get(String.class);
            fail("Fetch of non-existent port pair did not throw an exception");
        } catch (UniformInterfaceException ex) {
            assertThat(ex.getMessage(),
                       containsString("returned a response status of"));
        }
    }

    /**
     * Tests creating a port pair with POST.
     */
    @Test
    public void testPost() {

        expect(portPairService.createPortPair(anyObject()))
        .andReturn(true).anyTimes();
        replay(portPairService);

        WebResource rs = resource();
        InputStream jsonStream = PortPairResourceTest.class.getResourceAsStream("post-PortPair.json");

        ClientResponse response = rs.path("port_pairs")
                .type(MediaType.APPLICATION_JSON_TYPE)
                .post(ClientResponse.class, jsonStream);
        assertThat(response.getStatus(), is(HttpURLConnection.HTTP_OK));
    }

    /**
     * Tests deleting a port pair.
     */
    @Test
    public void testDelete() {
        expect(portPairService.removePortPair(anyObject()))
        .andReturn(true).anyTimes();
        replay(portPairService);

        WebResource rs = resource();

        String location = "port_pairs/78dcd363-fc23-aeb6-f44b-56dc5e2fb3ae";

        ClientResponse deleteResponse = rs.path(location)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .delete(ClientResponse.class);
        assertThat(deleteResponse.getStatus(),
                   is(HttpURLConnection.HTTP_NO_CONTENT));
    }
}
