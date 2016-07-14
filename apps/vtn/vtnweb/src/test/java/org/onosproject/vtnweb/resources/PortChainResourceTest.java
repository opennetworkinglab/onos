/*
 * Copyright 2015-present Open Networking Laboratory
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

import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.osgi.TestServiceDirectory;
import org.onlab.rest.BaseResource;
import org.onosproject.codec.CodecService;
import org.onosproject.net.DeviceId;
import org.onosproject.vtnrsc.FiveTuple;
import org.onosproject.vtnrsc.FlowClassifierId;
import org.onosproject.vtnrsc.LoadBalanceId;
import org.onosproject.vtnrsc.PortChain;
import org.onosproject.vtnrsc.PortChainId;
import org.onosproject.vtnrsc.PortPairGroupId;
import org.onosproject.vtnrsc.PortPairId;
import org.onosproject.vtnrsc.TenantId;
import org.onosproject.vtnrsc.portchain.PortChainService;
import org.onosproject.vtnweb.web.SfcCodecContext;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * Unit tests for port chain REST APIs.
 */
public class PortChainResourceTest extends VtnResourceTest {

    final PortChainService portChainService = createMock(PortChainService.class);

    PortChainId portChainId1 = PortChainId.of("78dcd363-fc23-aeb6-f44b-56dc5e2fb3ae");
    TenantId tenantId1 = TenantId.tenantId("d382007aa9904763a801f68ecf065cf5");
    private final List<PortPairGroupId> portPairGroupList1 = Lists.newArrayList();
    private final List<FlowClassifierId> flowClassifierList1 = Lists.newArrayList();


    final MockPortChain portChain1 = new MockPortChain(portChainId1, tenantId1, "portChain1",
                                                       "Mock port chain", portPairGroupList1,
                                                       flowClassifierList1);

    /**
     * Mock class for a port chain.
     */
    private static class MockPortChain implements PortChain {

        private final PortChainId portChainId;
        private final TenantId tenantId;
        private final String name;
        private final String description;
        private final List<PortPairGroupId> portPairGroupList;
        private final List<FlowClassifierId> flowClassifierList;

        public MockPortChain(PortChainId portChainId, TenantId tenantId,
                             String name, String description,
                             List<PortPairGroupId> portPairGroupList,
                             List<FlowClassifierId> flowClassifierList) {

            this.portChainId = portChainId;
            this.tenantId = tenantId;
            this.name = name;
            this.description = description;
            this.portPairGroupList = portPairGroupList;
            this.flowClassifierList = flowClassifierList;
        }

        @Override
        public PortChainId portChainId() {
            return portChainId;
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
        public List<PortPairGroupId> portPairGroups() {
            return  ImmutableList.copyOf(portPairGroupList);
        }

        @Override
        public List<FlowClassifierId> flowClassifiers() {
            return ImmutableList.copyOf(flowClassifierList);
        }

        @Override
        public boolean exactMatch(PortChain portChain) {
            return this.equals(portChain) &&
                    Objects.equals(this.portChainId, portChain.portChainId()) &&
                    Objects.equals(this.tenantId, portChain.tenantId());
        }

        @Override
        public void addLoadBalancePath(FiveTuple fiveTuple, LoadBalanceId id, List<PortPairId> path) {
        }

        @Override
        public LoadBalanceId getLoadBalanceId(FiveTuple fiveTuple) {
            return null;
        }

        @Override
        public Set<FiveTuple> getLoadBalanceIdMapKeys() {
            return null;
        }

        @Override
        public List<PortPairId> getLoadBalancePath(LoadBalanceId id) {
            return null;
        }

        @Override
        public List<PortPairId> getLoadBalancePath(FiveTuple fiveTuple) {
            return null;
        }

        @Override
        public LoadBalanceId matchPath(List<PortPairId> path) {
            return null;
        }

        @Override
        public int getLoadBalancePathSize() {
            return 0;
        }

        @Override
        public void addSfcClassifiers(LoadBalanceId id, List<DeviceId> classifierList) {
        }

        @Override
        public void addSfcForwarders(LoadBalanceId id, List<DeviceId> forwarderList) {
        }

        @Override
        public void removeSfcClassifiers(LoadBalanceId id, List<DeviceId> classifierList) {
        }

        @Override
        public void removeSfcForwarders(LoadBalanceId id, List<DeviceId> forwarderList) {
        }

        @Override
        public List<DeviceId> getSfcClassifiers(LoadBalanceId id) {
            return null;
        }

        @Override
        public List<DeviceId> getSfcForwarders(LoadBalanceId id) {
            return null;
        }

        @Override
        public Set<LoadBalanceId> getLoadBalancePathMapKeys() {
            return null;
        }

        @Override
        public PortChain oldPortChain() {
            return null;
        }
    }

    /**
     * Sets up the global values for all the tests.
     */
    @Before
    public void setUpTest() {
        SfcCodecContext context = new SfcCodecContext();
        ServiceDirectory testDirectory = new TestServiceDirectory()
        .add(PortChainService.class, portChainService)
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
     * Tests the result of the rest api GET when there are no port chains.
     */
    @Test
    public void testPortChainsEmpty() {

        expect(portChainService.getPortChains()).andReturn(null).anyTimes();
        replay(portChainService);
        final WebTarget wt = target();
        final String response = wt.path("port_chains").request().get(String.class);
        assertThat(response, is("{\"port_chains\":[]}"));
    }

    /**
     * Tests the result of a rest api GET for port chain id.
     */
    @Test
    public void testGetPortChainId() {

        final Set<PortChain> portChains = new HashSet<>();
        portChains.add(portChain1);

        expect(portChainService.exists(anyObject())).andReturn(true).anyTimes();
        expect(portChainService.getPortChain(anyObject())).andReturn(portChain1).anyTimes();
        replay(portChainService);

        final WebTarget wt = target();
        final String response = wt.path("port_chains/1278dcd4-459f-62ed-754b-87fc5e4a6751")
                .request().get(String.class);
        final JsonObject result = Json.parse(response).asObject();
        assertThat(result, notNullValue());
    }

    /**
     * Tests that a fetch of a non-existent port chain object throws an exception.
     */
    @Test
    public void testBadGet() {
        expect(portChainService.getPortChain(anyObject()))
        .andReturn(null).anyTimes();
        replay(portChainService);
        WebTarget wt = target();
        try {
            wt.path("port_chains/78dcd363-fc23-aeb6-f44b-56dc5aafb3ae")
                    .request().get(String.class);
            fail("Fetch of non-existent port chain did not throw an exception");
        } catch (NotFoundException ex) {
            assertThat(ex.getMessage(),
                       containsString("HTTP 404 Not Found"));
        }
    }

    /**
     * Tests creating a port chain with POST.
     */
    @Test
    public void testPost() {

        expect(portChainService.createPortChain(anyObject()))
        .andReturn(true).anyTimes();
        replay(portChainService);

        WebTarget wt = target();
        InputStream jsonStream = PortChainResourceTest.class.getResourceAsStream("post-PortChain.json");

        Response response = wt.path("port_chains")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(jsonStream));
        assertThat(response.getStatus(), is(HttpURLConnection.HTTP_OK));
    }

    /**
     * Tests deleting a port chain.
     */
    @Test
    public void testDelete() {
        expect(portChainService.removePortChain(anyObject()))
        .andReturn(true).anyTimes();
        replay(portChainService);

        WebTarget wt = target();

        String location = "port_chains/1278dcd4-459f-62ed-754b-87fc5e4a6751";

        Response deleteResponse = wt.path(location)
                .request(MediaType.APPLICATION_JSON_TYPE, MediaType.TEXT_PLAIN_TYPE)
                .delete();
        assertThat(deleteResponse.getStatus(),
                   is(HttpURLConnection.HTTP_NO_CONTENT));
    }
}
