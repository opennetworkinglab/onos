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
package org.onosproject.k8snetworking.codec;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.codec.impl.CodecManager;
import org.onosproject.core.CoreService;
import org.onosproject.k8snetworking.api.DefaultK8sNetwork;
import org.onosproject.k8snetworking.api.K8sNetwork;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.TestCase.assertEquals;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.onosproject.k8snetworking.codec.K8sNetworkJsonMatcher.matchesK8sNetwork;
import static org.onosproject.net.NetTestTools.APP_ID;

/**
 * Unit tests for kubernetes network codec.
 */
public class K8sNetworkCodecTest {

    MockCodecContext context;

    JsonCodec<K8sNetwork> k8sNetworkCodec;

    final CoreService mockCoreService = createMock(CoreService.class);
    private static final String REST_APP_ID = "org.onosproject.rest";

    /**
     * Initial setup for this unit test.
     */
    @Before
    public void setUp() {
        context = new MockCodecContext();
        k8sNetworkCodec = new K8sNetworkCodec();

        assertThat(k8sNetworkCodec, notNullValue());

        expect(mockCoreService.registerApplication(REST_APP_ID))
                .andReturn(APP_ID).anyTimes();
        replay(mockCoreService);
        context.registerService(CoreService.class, mockCoreService);
    }

    /**
     * Tests the kubernetes network encoding.
     */
    @Test
    public void testK8sNetworkEncode() {
        K8sNetwork network = DefaultK8sNetwork.builder()
                .networkId("network-1")
                .name("network-1")
                .segmentId("1")
                .type(K8sNetwork.Type.VXLAN)
                .cidr("10.10.0.0/24")
                .mtu(1500)
                .build();

        ObjectNode nodeJson = k8sNetworkCodec.encode(network, context);
        assertThat(nodeJson, matchesK8sNetwork(network));
    }

    /**
     * Tests the kubernetes network decoding.
     */
    @Test
    public void testK8sNetworkDecode() throws IOException {
        K8sNetwork network = getK8sNetwork("K8sNetwork.json");

        assertEquals("network-1", network.networkId());
        assertEquals("network-1", network.name());
        assertEquals("1", network.segmentId());
        assertEquals("VXLAN", network.type().name());
        assertEquals("10.10.0.1", network.gatewayIp().toString());
        assertEquals("10.10.0.0/24", network.cidr());
        assertThat(network.mtu(), is(1500));
    }

    /**
     * Reads in an kubernetes network from the given resource and decodes it.
     *
     * @param resourceName resource to use to read the JSON for the rule
     * @return decoded kubernetes network
     * @throws IOException if processing the resource fails
     */
    private K8sNetwork getK8sNetwork(String resourceName) throws IOException {
        InputStream jsonStream = K8sNetworkCodecTest.class.getResourceAsStream(resourceName);
        JsonNode json = context.mapper().readTree(jsonStream);
        MatcherAssert.assertThat(json, notNullValue());
        K8sNetwork network = k8sNetworkCodec.decode((ObjectNode) json, context);
        assertThat(network, notNullValue());
        return network;
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
            if (entityClass == K8sNetwork.class) {
                return (JsonCodec<T>) k8sNetworkCodec;
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
