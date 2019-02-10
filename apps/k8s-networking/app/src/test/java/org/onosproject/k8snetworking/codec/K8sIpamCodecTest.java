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
import org.onlab.packet.IpAddress;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.codec.impl.CodecManager;
import org.onosproject.core.CoreService;
import org.onosproject.k8snetworking.api.DefaultK8sIpam;
import org.onosproject.k8snetworking.api.K8sIpam;

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
import static org.onosproject.k8snetworking.codec.K8sIpamJsonMatcher.matchesK8sIpam;
import static org.onosproject.net.NetTestTools.APP_ID;

/**
 * Unit tests for kubernetes IPAM codec.
 */
public class K8sIpamCodecTest {

    MockCodecContext context;

    JsonCodec<K8sIpam> k8sIpamCodec;

    final CoreService mockCoreService = createMock(CoreService.class);
    private static final String REST_APP_ID = "org.onosproject.rest";

    /**
     * Initial setup for this unit test.
     */
    @Before
    public void setUp() {
        context = new MockCodecContext();
        k8sIpamCodec = new K8sIpamCodec();

        assertThat(k8sIpamCodec, notNullValue());

        expect(mockCoreService.registerApplication(REST_APP_ID))
                .andReturn(APP_ID).anyTimes();
        replay(mockCoreService);
        context.registerService(CoreService.class, mockCoreService);
    }

    /**
     * Tests the kubernetes IPAM encoding.
     */
    @Test
    public void testK8sIpamEncode() {
        K8sIpam ipam = new DefaultK8sIpam("network-1-10.10.10.10",
                IpAddress.valueOf("10.10.10.10"), "network-1");

        ObjectNode nodeJson = k8sIpamCodec.encode(ipam, context);
        assertThat(nodeJson, matchesK8sIpam(ipam));
    }

    /**
     * Tests the kubernetes IPAM decoding.
     */
    @Test
    public void testK8sIpamDecode() throws IOException {
        K8sIpam ipam = getK8sIpam("K8sIpam.json");

        assertEquals("10.10.10.2", ipam.ipAddress().toString());
        assertEquals("network-1", ipam.networkId());
    }

    /**
     * Reads in an kubernetes IPAM from the given resource and decodes it.
     *
     * @param resourceName resource to use to read the JSON for the rule
     * @return decoded kubernetes node
     * @throws IOException if processing the resource fails
     */
    private K8sIpam getK8sIpam(String resourceName) throws IOException {
        InputStream jsonStream = K8sIpamCodecTest.class.getResourceAsStream(resourceName);
        JsonNode json = context.mapper().readTree(jsonStream);
        MatcherAssert.assertThat(json, notNullValue());
        K8sIpam node = k8sIpamCodec.decode((ObjectNode) json, context);
        assertThat(node, notNullValue());
        return node;
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

        @Override
        @SuppressWarnings("unchecked")
        public <T> JsonCodec<T> codec(Class<T> entityClass) {
            if (entityClass == K8sIpam.class) {
                return (JsonCodec<T>) k8sIpamCodec;
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
