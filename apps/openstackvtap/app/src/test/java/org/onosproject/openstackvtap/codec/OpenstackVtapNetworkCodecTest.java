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
package org.onosproject.openstackvtap.codec;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.codec.impl.CodecManager;
import org.onosproject.core.CoreService;
import org.onosproject.openstackvtap.api.OpenstackVtapNetwork;
import org.onosproject.openstackvtap.impl.DefaultOpenstackVtapNetwork;

import java.util.HashMap;
import java.util.Map;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.onosproject.net.NetTestTools.APP_ID;
import static org.onosproject.openstackvtap.codec.OpenstackVtapNetworkJsonMatcher.matchesVtapNetwork;

/**
 * Unit tests for OpenstackVtapNetwork codec.
 */
public class OpenstackVtapNetworkCodecTest {

    MockCodecContext context;
    JsonCodec<OpenstackVtapNetwork> vtapNetworkCodec;
    final CoreService mockCoreService = createMock(CoreService.class);
    private static final String REST_APP_ID = "org.onosproject.rest";

    private static final OpenstackVtapNetwork.Mode MODE = OpenstackVtapNetwork.Mode.VXLAN;
    private static final int VNI = 1;
    private static final String SERVER_IP_ADDRESS = "10.10.10.1";

    /**
     * Initial setup for this unit test.
     */
    @Before
    public void setUp() {
        context = new MockCodecContext();
        vtapNetworkCodec = new OpenstackVtapNetworkCodec();

        assertThat(vtapNetworkCodec, notNullValue());

        expect(mockCoreService.registerApplication(REST_APP_ID))
                .andReturn(APP_ID).anyTimes();
        replay(mockCoreService);
        context.registerService(CoreService.class, mockCoreService);
    }

    /**
     * Tests the openstack vtap network encoding.
     */
    @Test
    public void testEncode() {
        OpenstackVtapNetwork vtapNetwork = DefaultOpenstackVtapNetwork.builder()
                .mode(MODE)
                .networkId(VNI)
                .serverIp(IpAddress.valueOf(SERVER_IP_ADDRESS))
                .build();

        ObjectNode nodeJson = vtapNetworkCodec.encode(vtapNetwork, context);
        assertThat(nodeJson, matchesVtapNetwork(vtapNetwork));
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
            if (entityClass == OpenstackVtapNetwork.class) {
                return (JsonCodec<T>) vtapNetworkCodec;
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
