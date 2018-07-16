/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.openstacknetworking.codec;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.codec.impl.CodecManager;
import org.onosproject.openstacknetworking.api.ExternalPeerRouter;
import org.onosproject.openstacknetworking.api.InstancePort;
import org.onosproject.openstacknetworking.impl.DefaultExternalPeerRouter;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.onosproject.openstacknetworking.codec.ExternalPeerRouterJsonMatcher.matchesExternalPeerRouter;

/**
 * Unit tests for ExternalPeerRouter codec.
 */
public class ExternalPeerRouterCodecTest {

    MockCodecContext context;
    JsonCodec<ExternalPeerRouter> externalPeerRouterCodec;

    @Before
    public void setUp() {
        context = new MockCodecContext();
        externalPeerRouterCodec = new ExternalPeerRouterCodec();

        assertThat(externalPeerRouterCodec, notNullValue());
    }

    /**
     * Tests the external peer router encoding.
     */
    @Test
    public void testExternalPeerRouterEncode() {
        ExternalPeerRouter router = DefaultExternalPeerRouter.builder()
                .ipAddress(IpAddress.valueOf("10.10.10.1"))
                .macAddress(MacAddress.valueOf("11:22:33:44:55:66"))
                .vlanId(VlanId.vlanId("1"))
                .build();

        ObjectNode routerJson = externalPeerRouterCodec.encode(router, context);
        assertThat(routerJson, matchesExternalPeerRouter(router));
    }

    /**
     * Tests the external peer router decoding.
     * @throws IOException
     */
    @Test
    public void testExternalPeerRouterDecode() throws IOException {
        ExternalPeerRouter router = getPeerRouter("ExternalPeerRouter.json");

        assertThat(router.ipAddress(), is(IpAddress.valueOf("10.10.10.1")));
        assertThat(router.macAddress(), is(MacAddress.valueOf("11:22:33:44:55:66")));
        assertThat(router.vlanId(), is(VlanId.vlanId("1")));
    }

    /**
     * Reads in an external peer router from the given resource and decodes it.
     *
     * @param resourceName resource to use to read the JSON for the rule
     * @return decoded external peer router
     * @throws IOException if processing the resource fails
     */
    private ExternalPeerRouter getPeerRouter(String resourceName) throws IOException {
        InputStream jsonStream = InstancePortCodecTest.class.getResourceAsStream(resourceName);
        JsonNode json = context.mapper().readTree(jsonStream);
        MatcherAssert.assertThat(json, notNullValue());
        ExternalPeerRouter router = externalPeerRouterCodec.decode((ObjectNode) json, context);
        assertThat(router, notNullValue());
        return router;
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
            if (entityClass == InstancePort.class) {
                return (JsonCodec<T>) externalPeerRouterCodec;
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
