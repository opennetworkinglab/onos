/*
 * Copyright 2019-present Open Networking Foundation
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
package org.onosproject.k8snode.codec;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.codec.impl.CodecManager;
import org.onosproject.core.CoreService;
import org.onosproject.k8snode.api.DefaultK8sNode;
import org.onosproject.k8snode.api.K8sNode;
import org.onosproject.k8snode.api.K8sNodeInfo;
import org.onosproject.k8snode.api.K8sNodeState;
import org.onosproject.net.DeviceId;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.TestCase.assertEquals;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.onosproject.k8snode.codec.K8sNodeJsonMatcher.matchesK8sNode;
import static org.onosproject.net.NetTestTools.APP_ID;

/**
 * Unit tests for Kubernetes Node codec.
 */
public class K8sNodeCodecTest {

    MockCodecContext context;

    JsonCodec<K8sNode> k8sNodeCodec;

    final CoreService mockCoreService = createMock(CoreService.class);
    private static final String REST_APP_ID = "org.onosproject.rest";

    /**
     * Initial setup for this unit test.
     */
    @Before
    public void setUp() {
        context = new MockCodecContext();
        k8sNodeCodec = new K8sNodeCodec();

        assertThat(k8sNodeCodec, notNullValue());

        expect(mockCoreService.registerApplication(REST_APP_ID))
                .andReturn(APP_ID).anyTimes();
        replay(mockCoreService);
        context.registerService(CoreService.class, mockCoreService);
    }

    /**
     * Tests the kubernetes minion node encoding.
     */
    @Test
    public void testK8sMinionNodeEncode() {
        K8sNode node = DefaultK8sNode.builder()
                .clusterName("kubernetes")
                .hostname("minion")
                .type(K8sNode.Type.MINION)
                .segmentId(100)
                .state(K8sNodeState.INIT)
                .managementIp(IpAddress.valueOf("10.10.10.1"))
                .dataIp(IpAddress.valueOf("20.20.20.2"))
                .nodeInfo(new K8sNodeInfo(IpAddress.valueOf("30.30.30.3"), null))
                .intgBridge(DeviceId.deviceId("kbr-int"))
                .extIntf("eth1")
                .extBridgeIp(IpAddress.valueOf("10.10.10.5"))
                .extGatewayIp(IpAddress.valueOf("10.10.10.1"))
                .extGatewayMac(MacAddress.valueOf("FF:FF:FF:FF:FF:FF"))
                .build();

        ObjectNode nodeJson = k8sNodeCodec.encode(node, context);
        assertThat(nodeJson, matchesK8sNode(node));
    }

    /**
     * Tests the kubernetes minion node decoding.
     *
     * @throws IOException IO exception
     */
    @Test
    public void testK8sMinionNodeDecode() throws IOException {
        K8sNode node = getK8sNode("K8sMinionNode.json");

        assertEquals("kubernetes", node.clusterName());
        assertEquals("minion", node.hostname());
        assertEquals("MINION", node.type().name());
        assertEquals(100, node.segmentId());
        assertEquals("172.16.130.4", node.managementIp().toString());
        assertEquals("172.16.130.4", node.dataIp().toString());
        assertEquals("172.16.130.5", node.nodeIp().toString());
        assertEquals("of:00000000000000a1", node.intgBridge().toString());
        assertEquals("eth1", node.extIntf());
        assertEquals("172.16.130.5", node.extBridgeIp().toString());
        assertEquals("172.16.130.1", node.extGatewayIp().toString());
        assertEquals("FF:FF:FF:FF:FF:FF", node.extGatewayMac().toString());
    }

    private K8sNode getK8sNode(String resourceName) throws IOException {
        InputStream jsonStream = K8sNodeCodecTest.class.getResourceAsStream(resourceName);
        JsonNode json = context.mapper().readTree(jsonStream);
        assertThat(json, notNullValue());
        K8sNode node = k8sNodeCodec.decode((ObjectNode) json, context);
        assertThat(node, notNullValue());
        return node;
    }

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

        @Override
        @SuppressWarnings("unchecked")
        public <T> JsonCodec<T> codec(Class<T> entityClass) {
            if (entityClass == K8sNode.class) {
                return (JsonCodec<T>) k8sNodeCodec;
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
