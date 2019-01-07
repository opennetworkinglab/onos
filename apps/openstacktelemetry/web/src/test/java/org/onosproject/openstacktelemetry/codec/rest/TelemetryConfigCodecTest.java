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
package org.onosproject.openstacktelemetry.codec.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.codec.impl.CodecManager;
import org.onosproject.core.CoreService;
import org.onosproject.openstacktelemetry.api.DefaultTelemetryConfig;
import org.onosproject.openstacktelemetry.api.config.TelemetryConfig;

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
import static org.onosproject.net.NetTestTools.APP_ID;
import static org.onosproject.openstacktelemetry.api.config.TelemetryConfig.Status.ENABLED;
import static org.onosproject.openstacktelemetry.codec.rest.TelemetryConfigJsonMatcher.matchesTelemetryConfig;

/**
 * Unit tests for TelemetryConfig codec.
 */
public class TelemetryConfigCodecTest {

    MockCodecContext context;

    JsonCodec<TelemetryConfig> telemetryConfigCodec;

    final CoreService mockCoreService = createMock(CoreService.class);
    private static final String REST_APP_ID = "org.onosproject.rest";

    @Before
    public void setUp() {
        context = new MockCodecContext();
        telemetryConfigCodec = new TelemetryConfigJsonCodec();

        assertThat(telemetryConfigCodec, notNullValue());

        expect(mockCoreService.registerApplication(REST_APP_ID))
                .andReturn(APP_ID).anyTimes();
        replay(mockCoreService);
        context.registerService(CoreService.class, mockCoreService);
    }

    /**
     * Tests the telemetry config encoding.
     */
    @Test
    public void testTelemetryConfigEncode() {

        String name = "grpc";
        TelemetryConfig.ConfigType type = TelemetryConfig.ConfigType.GRPC;
        String manufacturer = "grpc.io";
        String swVersion = "1.0";
        TelemetryConfig.Status status = ENABLED;

        Map<String, String> properties = Maps.newConcurrentMap();
        properties.put("key1", "value1");
        properties.put("key2", "value2");

        TelemetryConfig config = new DefaultTelemetryConfig(name, type,
                ImmutableList.of(), manufacturer, swVersion, status, properties);

        ObjectNode configJson = telemetryConfigCodec.encode(config, context);
        assertThat(configJson, matchesTelemetryConfig(config));
    }

    /**
     * Tests the telemetry config decoding.
     */
    @Test
    public void testTelemetryConfigDecode() throws IOException {
        TelemetryConfig config = getTelemetryConfig("TelemetryConfig.json");

        assertEquals(config.name(), "grpc-config");
        assertEquals(config.type().name(), "GRPC");
        assertEquals(config.manufacturer(), "grpc.io");
        assertEquals(config.swVersion(), "1.0");
        assertEquals(config.status().name(), "ENABLED");

        config.properties().forEach((k, v) -> {
            if (k.equals("address")) {
                assertEquals(v, "127.0.0.1");
            }
            if (k.equals("port")) {
                assertEquals(v, "9092");
            }
        });
    }

    /**
     * Reads in a telemetry config from the given resource and decodes it.
     *
     * @param resourceName resource to use to read the JSON for the rule
     * @return decoded telemetry config
     * @throws IOException if processing the resource fails
     */
    private TelemetryConfig getTelemetryConfig(String resourceName) throws IOException {
        InputStream jsonStream = TelemetryConfigCodecTest.class.getResourceAsStream(resourceName);
        JsonNode json = context.mapper().readTree(jsonStream);
        MatcherAssert.assertThat(json, notNullValue());
        TelemetryConfig config = telemetryConfigCodec.decode((ObjectNode) json, context);
        assertThat(config, notNullValue());
        return config;
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
            if (entityClass == TelemetryConfig.class) {
                return (JsonCodec<T>) telemetryConfigCodec;
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
