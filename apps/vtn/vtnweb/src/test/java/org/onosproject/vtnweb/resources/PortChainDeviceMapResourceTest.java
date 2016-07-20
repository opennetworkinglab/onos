/*
 * Copyright 2016-present Open Networking Laboratory
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
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.ws.rs.client.WebTarget;

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
 * Unit tests for port chain device map REST APIs.
 */
public class PortChainDeviceMapResourceTest extends VtnResourceTest {

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
            return ImmutableList.copyOf(portPairGroupList);
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
            DeviceId deviceId1 = DeviceId.deviceId("of:000000000000001");
            List<DeviceId> classifierList = Lists.newArrayList();
            classifierList.add(deviceId1);
            return classifierList;
        }

        @Override
        public List<DeviceId> getSfcForwarders(LoadBalanceId id) {
            DeviceId deviceId1 = DeviceId.deviceId("of:000000000000002");
            DeviceId deviceId2 = DeviceId.deviceId("of:000000000000003");
            List<DeviceId> forwarderList = Lists.newArrayList();
            forwarderList.add(deviceId1);
            forwarderList.add(deviceId2);
            return forwarderList;
        }

        @Override
        public Set<LoadBalanceId> getLoadBalancePathMapKeys() {
            LoadBalanceId id = LoadBalanceId.of((byte) 1);
            Set<LoadBalanceId> set = new HashSet<LoadBalanceId>();
            set.add(id);
            return set;
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
     * Tests the result of a rest api GET for port chain id.
     */
    @Test
    public void testGetPortChainDeviceMap() {

        expect(portChainService.getPortChain(anyObject())).andReturn(portChain1).anyTimes();
        replay(portChainService);

        final WebTarget wt = target();
        final String response = wt.path("portChainDeviceMap/1278dcd4-459f-62ed-754b-87fc5e4a6751").request()
                .get(String.class);
        final JsonObject result = Json.parse(response).asObject();
        assertThat(result, notNullValue());
        assertThat(result.names().get(0), is("portChainDeviceMap"));

    }
}
