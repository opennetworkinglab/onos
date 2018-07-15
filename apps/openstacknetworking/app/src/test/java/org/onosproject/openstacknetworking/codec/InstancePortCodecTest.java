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
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.codec.impl.CodecManager;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.openstacknetworking.api.InstancePort;
import org.onosproject.openstacknetworking.impl.DefaultInstancePort;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.onosproject.openstacknetworking.codec.InstancePortJsonMatcher.matchesInstancePort;

/**
 * Unit tests for InstancePort codec.
 */
public class InstancePortCodecTest {

    MockCodecContext context;
    JsonCodec<InstancePort> instancePortCodec;


    @Before
    public void setUp() {
        context = new MockCodecContext();
        instancePortCodec = new InstancePortCodec();

        assertThat(instancePortCodec, notNullValue());
    }

    /**
     * Tests the instance port encoding.
     */
    @Test
    public void testInstancePortEncode() {
        InstancePort port = DefaultInstancePort.builder()
                .networkId("net-id-1")
                .portId("port-id-1")
                .deviceId(DeviceId.deviceId("of:000000000000000a"))
                .portNumber(PortNumber.portNumber(1, "tap-1"))
                .ipAddress(IpAddress.valueOf("10.10.10.1"))
                .macAddress(MacAddress.valueOf("11:22:33:44:55:66"))
                .state(InstancePort.State.valueOf("ACTIVE"))
                .build();

        ObjectNode portJson = instancePortCodec.encode(port, context);
        assertThat(portJson, matchesInstancePort(port));
    }

    /**
     * Tests the instance port decoding.
     */
    @Test
    public void testInstancePortDecode() throws IOException {
        InstancePort port = getInstancePort("InstancePort.json");

        assertThat(port.networkId(), is("net-id-1"));
        assertThat(port.portId(), is("port-id-1"));
        assertThat(port.deviceId(), is(DeviceId.deviceId("of:000000000000000a")));
        assertThat(port.portNumber(), is(PortNumber.portNumber(1, "tap-1")));
        assertThat(port.ipAddress(), is(IpAddress.valueOf("10.10.10.1")));
        assertThat(port.macAddress(), is(MacAddress.valueOf("11:22:33:44:55:66")));
        assertThat(port.state().name(), is("ACTIVE"));
    }

    /**
     * Reads in an instance port from the given resource and decodes it.
     *
     * @param resourceName resource to use to read the JSON for the rule
     * @return decoded instance port
     * @throws IOException if processing the resource fails
     */
    private InstancePort getInstancePort(String resourceName) throws IOException {
        InputStream jsonStream = InstancePortCodecTest.class.getResourceAsStream(resourceName);
        JsonNode json = context.mapper().readTree(jsonStream);
        MatcherAssert.assertThat(json, notNullValue());
        InstancePort port = instancePortCodec.decode((ObjectNode) json, context);
        assertThat(port, notNullValue());
        return port;
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
                return (JsonCodec<T>) instancePortCodec;
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
