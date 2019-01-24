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
import org.onlab.packet.MacAddress;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.codec.impl.CodecManager;
import org.onosproject.core.CoreService;
import org.onosproject.k8snetworking.api.DefaultK8sPort;
import org.onosproject.k8snetworking.api.K8sPort;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

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
import static org.onosproject.k8snetworking.api.K8sPort.State.ACTIVE;
import static org.onosproject.k8snetworking.codec.K8sPortJsonMatcher.matchesK8sPort;
import static org.onosproject.net.NetTestTools.APP_ID;

/**
 * Unit tests for kubernetes port codec.
 */
public class K8sPortCodecTest {

    MockCodecContext context;

    JsonCodec<K8sPort> k8sPortCodec;

    final CoreService mockCoreService = createMock(CoreService.class);
    private static final String REST_APP_ID = "org.onosproject.rest";

    /**
     * Initial setup for this unit test.
     */
    @Before
    public void setUp() {
        context = new MockCodecContext();
        k8sPortCodec = new K8sPortCodec();

        assertThat(k8sPortCodec, notNullValue());

        expect(mockCoreService.registerApplication(REST_APP_ID))
                .andReturn(APP_ID).anyTimes();
        replay(mockCoreService);
        context.registerService(CoreService.class, mockCoreService);
    }

    /**
     * Tests the kubernetes port encoding.
     */
    @Test
    public void testK8sPortEncode() {
        K8sPort port = DefaultK8sPort.builder()
                .networkId("network-1")
                .portId("port-1")
                .deviceId(DeviceId.deviceId("of:0000000000000001"))
                .ipAddress(IpAddress.valueOf("10.10.10.2"))
                .macAddress(MacAddress.valueOf("00:11:22:33:44:55"))
                .portNumber(PortNumber.portNumber(1))
                .state(ACTIVE)
                .build();

        ObjectNode nodeJson = k8sPortCodec.encode(port, context);
        assertThat(nodeJson, matchesK8sPort(port));
    }

    /**
     * Tests the kubernetes port decoding.
     */
    @Test
    public void testK8sPortDecode() throws IOException {
        K8sPort port = getK8sPort("K8sPort.json");

        assertEquals("network-1", port.networkId());
        assertEquals("port-1", port.portId());
        assertEquals("00:11:22:33:44:55", port.macAddress().toString());
        assertEquals("10.10.10.10", port.ipAddress().toString());
        assertEquals("of:0000000000000001", port.deviceId().toString());
        assertEquals("1", port.portNumber().toString());
        assertEquals("ACTIVE", port.state().name());
    }

    /**
     * Reads in an kubernetes port from the given resource and decodes it.
     *
     * @param resourceName resource to use to read the JSON for the rule
     * @return decoded kubernetes port
     * @throws IOException if processing the resource fails
     */
    private K8sPort getK8sPort(String resourceName) throws IOException {
        InputStream jsonStream = K8sPortCodecTest.class.getResourceAsStream(resourceName);
        JsonNode json = context.mapper().readTree(jsonStream);
        MatcherAssert.assertThat(json, notNullValue());
        K8sPort port = k8sPortCodec.decode((ObjectNode) json, context);
        assertThat(port, notNullValue());
        return port;
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
            if (entityClass == K8sPort.class) {
                return (JsonCodec<T>) k8sPortCodec;
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
