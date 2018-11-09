/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.ofagent.rest;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.google.common.collect.Sets;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.osgi.TestServiceDirectory;
import org.onlab.packet.IpAddress;
import org.onlab.packet.TpPort;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.net.TenantId;
import org.onosproject.ofagent.api.OFAgent;
import org.onosproject.ofagent.api.OFAgentAdminService;
import org.onosproject.ofagent.api.OFAgentService;
import org.onosproject.ofagent.api.OFController;
import org.onosproject.ofagent.impl.DefaultOFAgent;
import org.onosproject.ofagent.impl.DefaultOFController;
import org.onosproject.rest.resources.ResourceTest;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Set;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.onosproject.ofagent.api.OFAgent.State.STOPPED;

/**
 * Test class for OFAgent application REST resource.
 */
public class OFAgentWebResourceTest extends ResourceTest {

    private static final Set<OFController> CONTROLLER_SET_1 = Sets.newHashSet(
            DefaultOFController.of(
                    IpAddress.valueOf("147.91.1.4"),
                    TpPort.tpPort(6633)));

    private static final Set<OFController> CONTROLLER_SET_2 = Sets.newHashSet(
            DefaultOFController.of(
                    IpAddress.valueOf("147.91.4.25"),
                    TpPort.tpPort(6633)),
            DefaultOFController.of(
                    IpAddress.valueOf("147.91.4.27"),
                    TpPort.tpPort(6653)));

    private static final Set<OFController> CONTROLLER_SET = Sets.newHashSet(
            DefaultOFController.of(
                    IpAddress.valueOf("147.91.2.11"),
                    TpPort.tpPort(6633)),
            DefaultOFController.of(
                    IpAddress.valueOf("147.91.2.9"),
                    TpPort.tpPort(6633)),
            DefaultOFController.of(
                    IpAddress.valueOf("147.91.2.17"),
                    TpPort.tpPort(6653)));

    private static final NetworkId NETWORK_1 = NetworkId.networkId(1);
    private static final NetworkId NETWORK_2 = NetworkId.networkId(2);
    private static final NetworkId NETWORK = NetworkId.networkId(3);


    private static final TenantId TENANT_1 = TenantId.tenantId("Tenant_1");
    private static final TenantId TENANT_2 = TenantId.tenantId("Tenant_2");
    private static final TenantId TENANT = TenantId.tenantId("Tenant");

    private static final OFAgent OF_AGENT = DefaultOFAgent.builder()
            .networkId(NETWORK)
            .tenantId(TENANT)
            .controllers(CONTROLLER_SET)
            .state(STOPPED)
            .build();

    private Set<OFAgent> agents = Sets.newHashSet(DefaultOFAgent.builder()
                                                  .networkId(NETWORK_1)
                                                  .tenantId(TENANT_1)
                                                  .controllers(CONTROLLER_SET_1)
                                                  .state(STOPPED)
                                                  .build(),
                                          DefaultOFAgent.builder()
                                                  .networkId(NETWORK_2)
                                                  .tenantId(TENANT_2)
                                                  .controllers(CONTROLLER_SET_2)
                                                  .state(STOPPED)
                                                  .build(),
                                          OF_AGENT);

    private Set<OFAgent> empty = Sets.newHashSet();

    private final OFAgentAdminService mockOFAgentAdminService = createMock(OFAgentAdminService.class);
    private final OFAgentService mockOFAgentService = createMock(OFAgentService.class);

    /**
     * Constructs OFAgent Web application test instance.
     */
    public OFAgentWebResourceTest() {
        super(ResourceConfig.forApplicationClass(OFAgentWebApplication.class));
    }

    /**
     * Sets up the global values for all the tests.
     */
    @Before
    public void setUpMocks() {
        ServiceDirectory testDirectory = new TestServiceDirectory()
                .add(OFAgentAdminService.class, mockOFAgentAdminService)
                .add(OFAgentService.class, mockOFAgentService);
        setServiceDirectory(testDirectory);
    }

    /**
     * Cleans up.
     */
    @After
    public void tearDownMocks() {
    }

    /**
     * Tests the result of the rest api GET when there are OFAgents.
     *
     * @throws IOException IO exception
     */
    @Test
    public void testNonEmptyOFAgentSet() throws IOException {
        expect(mockOFAgentService.agents()).andReturn(agents).anyTimes();
        replay(mockOFAgentService);

        final WebTarget wt = target();
        assertNotNull("WebTarget is null", wt);
        assertNotNull("WebTarget request is null", wt.request());
        final String response = wt.path("service/ofagents").request().get(String.class);
        final JsonObject result = Json.parse(response).asObject();
        assertThat(result, notNullValue());
        assertThat(result.names(), hasSize(1));
        assertThat(result.names().get(0), is("ofAgents"));

        mockOFAgentService.agents().forEach(ofAgent -> {

            String expectedJsonStringNetworkId = "\"networkId\":\"" + ofAgent.networkId().id() + "\"";
            assertThat(response, containsString(expectedJsonStringNetworkId));

            String expectedJsonStringTenantId = "\"tenantId\":\"" + ofAgent.tenantId().id() + "\"";
            assertThat(response, containsString(expectedJsonStringTenantId));

            String expectedJsonStringState = "\"state\":\"" + ofAgent.state() + "\"";
            assertThat(response, containsString(expectedJsonStringState));

            ofAgent.controllers().forEach(ofController -> {
                String expectedJsonStringIP = "\"ip\":\"" + ofController.ip() + "\"";
                assertThat(response, containsString(expectedJsonStringIP));

                String expectedJsonStringPort = "\"port\":\"" + ofController.port() + "\"";
                assertThat(response, containsString(expectedJsonStringPort));
            });
        });

        verify(mockOFAgentService);
    }

    /**
     * Tests the result of the rest api GET when there are no OFAgents.
     *
     * @throws IOException IO exception
     */
    @Test
    public void testEmptyOFAgentSet() throws IOException {
        expect(mockOFAgentService.agents()).andReturn(empty).anyTimes();
        replay(mockOFAgentService);

        final WebTarget wt = target();
        assertNotNull("WebTarget is null", wt);
        assertNotNull("WebTarget request is null", wt.request());
        final String response = wt.path("service/ofagents").request().get(String.class);
        final JsonObject result = Json.parse(response).asObject();
        assertThat(result, notNullValue());
        assertThat(result.names(), hasSize(1));
        assertThat(response, is("{\"ofAgents\":[]}"));

        verify(mockOFAgentService);
    }

    /**
     * Tests the result of the rest api GET for OFAgent.
     *
     * @throws IOException IO exception
     */
    @Test
    public void testOFAgent() throws IOException {
        expect(mockOFAgentService.agent(eq(NETWORK))).andReturn(OF_AGENT).anyTimes();
        replay(mockOFAgentService);

        final WebTarget wt = target();
        assertNotNull("WebTarget is null", wt);
        assertNotNull("WebTarget request is null", wt.request());
        final Response response = wt.path("service/ofagent/" + NETWORK).request().get();
        final JsonObject result = Json.parse(response.readEntity(String.class)).asObject();
        assertThat(result, notNullValue());
        assertThat(result.names(), hasSize(4));
        assertThat(result.get("networkId").asString(), is(NETWORK.id().toString()));
        assertThat(result.get("tenantId").asString(), is(TENANT.id()));
        assertThat(result.get("state").asString(), is(STOPPED.toString()));

        verify(mockOFAgentService);
    }


    /**
     * Tests the result of the rest api GET for non-existent OFAgent.
     *
     * @throws IOException IO exception
     */
    @Test
    public void testNonExistentOFAgent() throws IOException {
        expect(mockOFAgentService.agent(anyObject())).andReturn(null).anyTimes();
        replay(mockOFAgentService);

        final WebTarget wt = target();
        assertNotNull("WebTarget is null", wt);
        assertNotNull("WebTarget request is null", wt.request());
        final Response response = wt.path("service/ofagent/" + NETWORK_1).request().get();
        assertThat(response.getStatus(), is(HttpURLConnection.HTTP_NOT_FOUND));

        verify(mockOFAgentService);
    }


    /**
     * Tests creating an OFAgent with POST.
     */
    @Test
    public void testOFAgentCreate() {
        mockOFAgentAdminService.createAgent(anyObject());
        expectLastCall().anyTimes();
        replay(mockOFAgentAdminService);


        InputStream jsonStream = OFAgentWebResourceTest.class
                .getResourceAsStream("post-ofagent-create.json");
        assertNotNull("post-ofagent-create.json is null", jsonStream);
        WebTarget wt = target();

        Response response = wt.path("service/ofagent-create")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(jsonStream));
        assertThat(response.getStatus(), is(HttpURLConnection.HTTP_CREATED));

        verify(mockOFAgentAdminService);
    }

    /**
     * Tests creating an OFAgent with bad POST request.
     */
    @Test
    public void testOFAgentCreateBadRequest() {
        InputStream jsonStream = OFAgentWebResourceTest.class
                .getResourceAsStream("post-bad-request.json");
        assertNotNull("post-bad-request.json is null", jsonStream);
        WebTarget wt = target();

        Response response = wt.path("service/ofagent-create")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(jsonStream));
        assertThat(response.getStatus(), is(HttpURLConnection.HTTP_INTERNAL_ERROR));
    }

    /**
     * Tests updating an OFAgent with PUT.
     */
    @Test
    public void testOFAgentUpdate() {
        expect(mockOFAgentService.agent(eq(NETWORK))).andReturn(OF_AGENT).anyTimes();
        replay(mockOFAgentService);

        mockOFAgentAdminService.updateAgent(anyObject());
        expectLastCall().anyTimes();
        replay(mockOFAgentAdminService);

        InputStream jsonStream = OFAgentWebResourceTest.class
                .getResourceAsStream("put-ofagent-update.json");
        assertNotNull("put-ofagent-update.json is null", jsonStream);
        WebTarget wt = target();
        Response response = wt.path("service/ofagent-update")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .put(Entity.json(jsonStream));
        assertThat(response.getStatus(), is(HttpURLConnection.HTTP_OK));
        assertThat(response.readEntity(String.class), containsString("OFAgent updated"));

        verify(mockOFAgentService);
        verify(mockOFAgentAdminService);

    }

    /**
     * Tests non-existent OFAgent updating with PUT.
     */
    @Test
    public void testNonExistentOFAgentUpdate() {
        expect(mockOFAgentService.agent(anyObject())).andReturn(null).anyTimes();
        replay(mockOFAgentService);

        InputStream jsonStream = OFAgentWebResourceTest.class
                .getResourceAsStream("put-non-existent-ofagent-update.json");
        assertNotNull("put-non-existent-ofagent-update.json is null", jsonStream);
        WebTarget wt = target();
        Response response = wt.path("service/ofagent-update")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .put(Entity.json(jsonStream));
        assertThat(response.getStatus(), is(HttpURLConnection.HTTP_NOT_FOUND));

        verify(mockOFAgentService);

    }

    /**
     * Tests OFAgent updating with bad PUT request.
     */
    @Test
    public void testOFAgentUpdateBadRequest() {
        expect(mockOFAgentService.agent(anyObject())).andReturn(null).anyTimes();
        replay(mockOFAgentService);

        InputStream jsonStream = OFAgentWebResourceTest.class
                .getResourceAsStream("put-bad-request.json");
        assertNotNull("put-bad-request.json is null", jsonStream);
        WebTarget wt = target();
        Response response = wt.path("service/ofagent-update")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .put(Entity.json(jsonStream));
        assertThat(response.getStatus(), is(HttpURLConnection.HTTP_INTERNAL_ERROR));

        verify(mockOFAgentService);
    }

    /**
     * Tests starting an OFAgent with POST.
     */
    @Test
    public void testOFAgentStart() {
        expect(mockOFAgentService.agent(eq(NETWORK))).andReturn(OF_AGENT).anyTimes();
        replay(mockOFAgentService);

        mockOFAgentAdminService.startAgent(anyObject());
        expectLastCall().anyTimes();
        replay(mockOFAgentAdminService);

        InputStream jsonStream = OFAgentWebResourceTest.class
                .getResourceAsStream("post-ofagent-start.json");
        assertNotNull("post-ofagent-create.json is null", jsonStream);
        WebTarget wt = target();

        Response response = wt.path("service/ofagent-start")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(jsonStream));
        assertThat(response.getStatus(), is(HttpURLConnection.HTTP_OK));
//        assertThat(response.readEntity(String.class), containsString("OFAgent started"));
        assertThat(response.readEntity(String.class), is("OFAgent started"));

        verify(mockOFAgentService);
        verify(mockOFAgentAdminService);
    }

    /**
     * Tests non-existent OFAgent starting with POST.
     */
    @Test
    public void testNonExistentOFAgentStart() {
        expect(mockOFAgentService.agent(eq(NETWORK))).andReturn(null).anyTimes();
        replay(mockOFAgentService);

        InputStream jsonStream = OFAgentWebResourceTest.class
                .getResourceAsStream("post-non-existent-ofagent-start.json");
        assertNotNull("post-non-existent-ofagent-start.json is null", jsonStream);
        WebTarget wt = target();

        Response response = wt.path("service/ofagent-start")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(jsonStream));
        assertThat(response.getStatus(), is(HttpURLConnection.HTTP_NOT_FOUND));

        verify(mockOFAgentService);
    }

    /**
     * Tests OFAgent starting with bad POST request.
     */
    @Test
    public void testOFAgentStartBadRequest() {

        InputStream jsonStream = OFAgentWebResourceTest.class
                .getResourceAsStream("post-bad-request.json");
        assertNotNull("post-bad-request.json is null", jsonStream);
        WebTarget wt = target();

        Response response = wt.path("service/ofagent-start")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(jsonStream));
        assertThat(response.getStatus(), is(HttpURLConnection.HTTP_BAD_REQUEST));

    }

    /**
     * Tests stopping an OFAgent with POST.
     */
    @Test
    public void testOFAgentStop() {
        expect(mockOFAgentService.agent(eq(NETWORK))).andReturn(OF_AGENT).anyTimes();
        replay(mockOFAgentService);

        mockOFAgentAdminService.stopAgent(anyObject());
        expectLastCall().anyTimes();
        replay(mockOFAgentAdminService);

        InputStream jsonStream = OFAgentWebResourceTest.class
                .getResourceAsStream("post-ofagent-stop.json");
        assertNotNull("post-ofagent-stop.json is null", jsonStream);
        WebTarget wt = target();
        Response response = wt.path("service/ofagent-stop")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(jsonStream));
        assertThat(response.getStatus(), is(HttpURLConnection.HTTP_NO_CONTENT));

        verify(mockOFAgentService);
        verify(mockOFAgentAdminService);
    }

    /**
     * Tests stopping non-existent OFAgent with POST.
     */
    @Test
    public void testNonExistentOFAgentStop() {
        expect(mockOFAgentService.agent(NETWORK)).andReturn(null).anyTimes();
        replay(mockOFAgentService);

        InputStream jsonStream = OFAgentWebResourceTest.class
                .getResourceAsStream("post-non-existent-ofagent-stop.json");
        assertNotNull("post-non-existent-ofagent-stop.json is null", jsonStream);
        WebTarget wt = target();

        Response response = wt.path("service/ofagent-stop")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(jsonStream));
        assertThat(response.getStatus(), is(HttpURLConnection.HTTP_NOT_FOUND));

        verify(mockOFAgentService);
    }

    /**
     * Tests stopping FAgent with bad POST request.
     */
    @Test
    public void testOFAgentStopBadRequest() {
        InputStream jsonStream = OFAgentWebResourceTest.class
                .getResourceAsStream("post-bad-request.json");
        assertNotNull("post-bad-request.json is null", jsonStream);
        WebTarget wt = target();

        Response response = wt.path("service/ofagent-stop")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(jsonStream));
        assertThat(response.getStatus(), is(HttpURLConnection.HTTP_INTERNAL_ERROR));
    }


    /**
     * Tests deleting an OFAgent with DELETE.
     */
    @Test
    public void testOFAgentRemove() {
        expect(mockOFAgentService.agent(eq(NETWORK))).andReturn(OF_AGENT).anyTimes();
        replay(mockOFAgentService);

        expect(mockOFAgentAdminService.removeAgent(NETWORK)).andReturn(OF_AGENT).anyTimes();
        replay(mockOFAgentAdminService);

        WebTarget wt = target();
        Response response = wt.path("service/ofagent-remove/" + NETWORK.toString())
                .request(MediaType.APPLICATION_JSON_TYPE)
                .delete();
        assertThat(response.getStatus(), is(HttpURLConnection.HTTP_OK));
        final JsonObject result = Json.parse(response.readEntity(String.class)).asObject();
        assertThat(result.get("networkId").asString(), is(NETWORK.id().toString()));
        assertThat(result.get("state").asString(), is(STOPPED.toString()));

        verify(mockOFAgentService);
        verify(mockOFAgentAdminService);
    }

    /**
     * Tests deleting a non-existent OFAgent with DELETE.
     */
    @Test
    public void testNonExistentOFAgentRemove() {
        expect(mockOFAgentService.agent(eq(NETWORK))).andReturn(null).anyTimes();
        replay(mockOFAgentService);

        expect(mockOFAgentAdminService.removeAgent(NETWORK)).andReturn(null).anyTimes();
        replay(mockOFAgentAdminService);

        WebTarget wt = target();
        Response response = wt.path("service/ofagent-remove/" + NETWORK.toString())
                .request(MediaType.APPLICATION_JSON_TYPE)
                .delete();
        assertThat(response.getStatus(), is(HttpURLConnection.HTTP_BAD_REQUEST));
        assertThat(response.readEntity(String.class), containsString("OFAgent not found"));

        verify(mockOFAgentService);
        verify(mockOFAgentAdminService);
    }
}
