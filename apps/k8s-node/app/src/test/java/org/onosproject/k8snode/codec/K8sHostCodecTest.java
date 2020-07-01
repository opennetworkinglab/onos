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
package org.onosproject.k8snode.codec;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.codec.impl.CodecManager;
import org.onosproject.core.CoreService;
import org.onosproject.k8snode.api.DefaultK8sHost;
import org.onosproject.k8snode.api.K8sHost;

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
import static org.onosproject.k8snode.api.K8sHostState.INIT;
import static org.onosproject.k8snode.codec.K8sHostJsonMatcher.matchesK8sHost;
import static org.onosproject.net.NetTestTools.APP_ID;

public class K8sHostCodecTest {

    MockCodecContext context;

    JsonCodec<K8sHost> k8sHostCodec;

    final CoreService mockCoreService = createMock(CoreService.class);
    private static final String REST_APP_ID = "org.onosproject.rest";

    /**
     * Initial setup for this unit test.
     */
    @Before
    public void setUp() {
        context = new MockCodecContext();
        k8sHostCodec = new K8sHostCodec();

        assertThat(k8sHostCodec, notNullValue());

        expect(mockCoreService.registerApplication(REST_APP_ID))
                .andReturn(APP_ID).anyTimes();
        replay(mockCoreService);
        context.registerService(CoreService.class, mockCoreService);
    }

    /**
     * Tests the kubernetes host encoding.
     */
    @Test
    public void testK8sHostEncode() {
        K8sHost host = DefaultK8sHost.builder()
                .hostIp(IpAddress.valueOf("192.168.200.10"))
                .state(INIT)
                .nodeNames(ImmutableSet.of("1", "2"))
                .build();

        ObjectNode hostJson = k8sHostCodec.encode(host, context);
        assertThat(hostJson, matchesK8sHost(host));
    }

    /**
     * Tests the kubernetes host decoding.
     */
    @Test
    public void testK8sHostDecode() throws IOException {
        K8sHost host = getK8sHost("K8sHost.json");

        assertEquals("192.168.200.10", host.hostIp().toString());
        assertEquals("INIT", host.state().name());
    }

    private K8sHost getK8sHost(String resourceName) throws IOException {
        InputStream jsonStream = K8sHostCodecTest.class.getResourceAsStream(resourceName);
        JsonNode json = context.mapper().readTree(jsonStream);
        assertThat(json, notNullValue());
        K8sHost host = k8sHostCodec.decode((ObjectNode) json, context);
        assertThat(host, notNullValue());
        return host;
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
            if (entityClass == K8sHost.class) {
                return (JsonCodec<T>) k8sHostCodec;
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
