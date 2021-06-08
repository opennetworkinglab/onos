/*
 * Copyright 2021-present Open Networking Foundation
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
package org.onosproject.kubevirtnetworking.codec;

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
import org.onosproject.kubevirtnetworking.api.DefaultKubevirtPort;
import org.onosproject.kubevirtnetworking.api.KubevirtPort;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

import java.util.HashMap;
import java.util.Map;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.onosproject.kubevirtnetworking.codec.KubevirtPortJsonMatcher.matchesKubevirtPort;
import static org.onosproject.net.NetTestTools.APP_ID;

/**
 * Unit tests for KubevirtPort codec.
 */
public final class KubevirtPortCodecTest {

    MockCodecContext context;

    JsonCodec<KubevirtPort> kubevirtPortCodec;

    final CoreService mockCoreService = createMock(CoreService.class);
    private static final String REST_APP_ID = "org.onosproject.rest";

    @Before
    public void setUp() {
        context = new MockCodecContext();
        kubevirtPortCodec = new KubevirtPortCodec();

        assertThat(kubevirtPortCodec, notNullValue());

        expect(mockCoreService.registerApplication(REST_APP_ID))
                .andReturn(APP_ID).anyTimes();
        replay(mockCoreService);
        context.registerService(CoreService.class, mockCoreService);
    }

    /**
     * Tests the kubevirt port encoding.
     */
    @Test
    public void testKubevirtPortEncode() {
        KubevirtPort port = DefaultKubevirtPort.builder()
                .vmName("test-vm-1")
                .networkId("net-1")
                .macAddress(MacAddress.valueOf("11:22:33:44:55:66"))
                .ipAddress(IpAddress.valueOf("10.10.10.100"))
                .deviceId(DeviceId.deviceId("of:foo"))
                .portNumber(PortNumber.portNumber("1"))
                .build();
        ObjectNode portJson = kubevirtPortCodec.encode(port, context);
        assertThat(portJson, matchesKubevirtPort(port));
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
        public <T> JsonCodec<T> codec(Class<T> entityClass) {
            return manager.getCodec(entityClass);
        }

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


