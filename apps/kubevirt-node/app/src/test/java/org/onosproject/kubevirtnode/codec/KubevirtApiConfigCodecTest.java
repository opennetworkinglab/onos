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
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.codec.impl.CodecManager;
import org.onosproject.core.CoreService;
import org.onosproject.kubevirtnode.api.DefaultKubevirtApiConfig;
import org.onosproject.kubevirtnode.api.KubevirtApiConfig;

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
import static org.onosproject.kubevirtnode.api.KubevirtApiConfig.Scheme.HTTPS;
import static org.onosproject.kubevirtnode.api.KubevirtApiConfig.State.CONNECTED;
import static org.onosproject.kubevirtnode.codec.KubevirtApiConfigJsonMatcher.matchesKubevirtApiConfig;
import static org.onosproject.net.NetTestTools.APP_ID;

/**
 * Unit tests for KubeVirt API config codec.
 */
public class KubevirtApiConfigCodecTest {

    MockCodecContext context;

    JsonCodec<KubevirtApiConfig> kubevirtApiConfigCodec;

    final CoreService mockCoreService = createMock(CoreService.class);
    private static final String REST_APP_ID = "org.onosproject.rest";

    /**
     * Initial setup for this unit test.
     */
    @Before
    public void setUp() {
        context = new MockCodecContext();
        kubevirtApiConfigCodec = new KubevirtApiConfigCodec();

        assertThat(kubevirtApiConfigCodec, notNullValue());

        expect(mockCoreService.registerApplication(REST_APP_ID))
                .andReturn(APP_ID).anyTimes();
        replay(mockCoreService);
        context.registerService(CoreService.class, mockCoreService);
    }

    /**
     * Tests the KubeVirt API config encoding.
     */
    @Test
    public void testKubevirtApiConfigEncode() {
        KubevirtApiConfig config = DefaultKubevirtApiConfig.builder()
                .scheme(HTTPS)
                .ipAddress(IpAddress.valueOf("10.10.10.23"))
                .port(6443)
                .state(CONNECTED)
                .token("token")
                .caCertData("caCertData")
                .clientCertData("clientCertData")
                .clientKeyData("clientKeyData")
                .serviceFqdn("kubevirt.edgestack.svc.cluster.local")
                .apiServerFqdn("kubernetes.default.svc.cluster.local")
                .controllerIp(IpAddress.valueOf("127.0.0.1"))
                .datacenterId("BD")
                .clusterId("BD-MEH-CT01")
                .build();

        ObjectNode configJson = kubevirtApiConfigCodec.encode(config, context);
        assertThat(configJson, matchesKubevirtApiConfig(config));
    }

    /**
     * Tests the KubeVirt API config decoding.
     *
     * @throws IOException IO exception
     */
    @Test
    public void testKubevirtApiConfigDecode() throws IOException {
        KubevirtApiConfig config = getKubevirtApiConfig("KubevirtApiConfig.json");

        assertEquals("HTTPS", config.scheme().name());
        assertEquals("10.134.34.223", config.ipAddress().toString());
        assertEquals(6443, config.port());
        assertEquals("token", config.token());
        assertEquals("caCertData", config.caCertData());
        assertEquals("clientCertData", config.clientCertData());
        assertEquals("clientKeyData", config.clientKeyData());
        assertEquals("kubevirt.edgestack.svc.cluster.local", config.serviceFqdn());
        assertEquals("kubernetes.default.svc.cluster.local", config.apiServerFqdn());
        assertEquals("127.0.0.1", config.controllerIp().toString());
        assertEquals("BD", config.datacenterId());
        assertEquals("BD-MEH-CT01", config.clusterId());

    }

    private KubevirtApiConfig getKubevirtApiConfig(String resourceName) throws IOException {
        InputStream jsonStream = KubevirtNodeCodecTest.class.getResourceAsStream(resourceName);
        JsonNode json = context.mapper().readTree(jsonStream);
        assertThat(json, notNullValue());
        KubevirtApiConfig config = kubevirtApiConfigCodec.decode((ObjectNode) json, context);
        assertThat(config, notNullValue());
        return config;
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
            if (entityClass == KubevirtApiConfig.class) {
                return (JsonCodec<T>) kubevirtApiConfigCodec;
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
