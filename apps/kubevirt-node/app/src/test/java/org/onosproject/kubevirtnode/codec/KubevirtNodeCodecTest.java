/*
 * Copyright 2020-present Open Networking Foundation
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
package org.onosproject.kubevirtnode.codec;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.codec.impl.CodecManager;
import org.onosproject.core.CoreService;
import org.onosproject.kubevirtnode.api.DefaultKubevirtNode;
import org.onosproject.kubevirtnode.api.DefaultKubevirtPhyInterface;
import org.onosproject.kubevirtnode.api.KubevirtNode;
import org.onosproject.kubevirtnode.api.KubevirtNodeState;
import org.onosproject.kubevirtnode.api.KubevirtPhyInterface;
import org.onosproject.net.DeviceId;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.onosproject.kubevirtnode.codec.KubevirtNodeJsonMatcher.matchesKubevirtNode;
import static org.onosproject.net.NetTestTools.APP_ID;

/**
 * Unit tests for KubevirtNode codec.
 */
public class KubevirtNodeCodecTest {
    MockCodecContext context;

    JsonCodec<KubevirtNode> kubevirtNodeCodec;
    JsonCodec<KubevirtPhyInterface> kubevirtPhyInterfaceJsonCodec;

    final CoreService mockCoreService = createMock(CoreService.class);
    private static final String REST_APP_ID = "org.onosproject.rest";
    private static final DeviceId DEVICE_ID_1 = DeviceId.deviceId(String.format("of:%016d", 1));
    private static final DeviceId DEVICE_ID_2 = DeviceId.deviceId(String.format("of:%016d", 2));
    private static final DeviceId DEVICE_ID_3 = DeviceId.deviceId(String.format("of:%016d", 3));


    @Before
    public void setUp() {
        context = new MockCodecContext();
        kubevirtNodeCodec = new KubevirtNodeCodec();
        kubevirtPhyInterfaceJsonCodec = new KubevirtPhyInterfaceCodec();

        assertThat(kubevirtNodeCodec, notNullValue());
        assertThat(kubevirtPhyInterfaceJsonCodec, notNullValue());

        expect(mockCoreService.registerApplication(REST_APP_ID))
                .andReturn(APP_ID).anyTimes();
        replay(mockCoreService);
        context.registerService(CoreService.class, mockCoreService);
    }

    /**
     * Tests the kubevirt compute node encoding.
     */
    @Test
    public void testKubevirtComputeNodeEncode() {
        KubevirtPhyInterface phyIntf1 = DefaultKubevirtPhyInterface.builder()
                .network("mgmtnetwork")
                .intf("eth3")
                .physBridge(DEVICE_ID_1)
                .build();

        KubevirtPhyInterface phyIntf2 = DefaultKubevirtPhyInterface.builder()
                .network("oamnetwork")
                .intf("eth4")
                .physBridge(DEVICE_ID_2)
                .build();

        KubevirtNode node = DefaultKubevirtNode.builder()
                .hostname("worker")
                .type(KubevirtNode.Type.WORKER)
                .state(KubevirtNodeState.INIT)
                .managementIp(IpAddress.valueOf("10.10.10.1"))
                .intgBridge(DeviceId.deviceId("br-int"))
                .tunBridge(DeviceId.deviceId("br-tun"))
                .dataIp(IpAddress.valueOf("20.20.20.2"))
                .phyIntfs(ImmutableList.of(phyIntf1, phyIntf2))
                .build();

        ObjectNode nodeJson = kubevirtNodeCodec.encode(node, context);
        assertThat(nodeJson, matchesKubevirtNode(node));
    }

    /**
     * Tests the kubevirt compute node decoding.
     *
     * @throws IOException io exception
     */
    @Test
    public void testKubevirtComputeNodeDecode() throws IOException {
        KubevirtNode node = getKubevirtNode("KubevirtWorkerNode.json");

        assertThat(node.hostname(), is("worker-01"));
        assertThat(node.type().name(), is("WORKER"));
        assertThat(node.managementIp().toString(), is("172.16.130.4"));
        assertThat(node.dataIp().toString(), is("172.16.130.4"));
        assertThat(node.intgBridge().toString(), is("of:00000000000000a1"));
        assertThat(node.tunBridge().toString(), is("of:00000000000000a2"));
        assertThat(node.phyIntfs().size(), is(2));

        node.phyIntfs().forEach(intf -> {
            if (intf.network().equals("mgmtnetwork")) {
                assertThat(intf.intf(), is("eth3"));
                assertThat(intf.physBridge().toString(), is("of:00000000000000a3"));
            }
            if (intf.network().equals("oamnetwork")) {
                assertThat(intf.intf(), is("eth4"));
                assertThat(intf.physBridge().toString(), is("of:00000000000000a4"));
            }
        });
    }

    private KubevirtNode getKubevirtNode(String resourceName) throws IOException {
        InputStream jsonStream = KubevirtNodeCodecTest.class.getResourceAsStream(resourceName);
        JsonNode json = context.mapper().readTree(jsonStream);
        MatcherAssert.assertThat(json, notNullValue());
        KubevirtNode node = kubevirtNodeCodec.decode((ObjectNode) json, context);
        assertThat(node, notNullValue());
        return node;
    }

    /**
     * Tests the kubevirt gateway node encoding.
     */
    @Test
    public void testKubevirtGatweayNodeEncode() {
        KubevirtNode node = DefaultKubevirtNode.builder()
                .hostname("gateway")
                .type(KubevirtNode.Type.GATEWAY)
                .state(KubevirtNodeState.INIT)
                .managementIp(IpAddress.valueOf("10.10.10.1"))
                .intgBridge(DeviceId.deviceId("br-int"))
                .tunBridge(DeviceId.deviceId("br-tun"))
                .dataIp(IpAddress.valueOf("20.20.20.2"))
                .gatewayBridgeName("gateway")
                .build();

        ObjectNode nodeJson = kubevirtNodeCodec.encode(node, context);
        assertThat(nodeJson, matchesKubevirtNode(node));
    }

    /**
     * Tests the kubevirt gateway node decoding.
     *
     * @throws IOException io exception
     */
    @Test
    public void testKubevirtGatewayNodeDecode() throws IOException {
        KubevirtNode node = getKubevirtNode("KubevirtGatewayNode.json");

        assertThat(node.hostname(), is("gateway-01"));
        assertThat(node.type().name(), is("GATEWAY"));
        assertThat(node.managementIp().toString(), is("172.16.130.4"));
        assertThat(node.dataIp().toString(), is("172.16.130.4"));
        assertThat(node.intgBridge().toString(), is("of:00000000000000a1"));
        assertThat(node.tunBridge().toString(), is("of:00000000000000a2"));
        assertThat(node.gatewayBridgeName(), is("gateway"));
    }

    /**
     * Mock codec context for use in codec unit tests.
     */
    private class MockCodecContext implements CodecContext {

        private final ObjectMapper mapper = new ObjectMapper();
        private final CodecManager manager = new CodecManager();
        private final Map<Class<?>, Object> services = new HashMap<>();

        /**
         * Constructs a new mock codec context.
         */
        public MockCodecContext() {
            manager.activate();
        }

        @Override
        public ObjectMapper mapper() {
            return mapper;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> JsonCodec<T> codec(Class<T> entityClass) {
            if (entityClass == KubevirtPhyInterface.class) {
                return (JsonCodec<T>) kubevirtPhyInterfaceJsonCodec;
            }

            return manager.getCodec(entityClass);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T getService(Class<T> serviceClass) {
            return (T) services.get(serviceClass);
        }

        // for registering mock services
        public <T> void registerService(Class<T> serviceClass, T impl) {
            services.put(serviceClass, impl);
        }
    }
}
