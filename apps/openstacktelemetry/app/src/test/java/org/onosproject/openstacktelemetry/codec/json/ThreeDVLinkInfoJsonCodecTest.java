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
package org.onosproject.openstacktelemetry.codec.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.codec.impl.CodecManager;
import org.onosproject.core.CoreService;
import org.onosproject.openstacktelemetry.api.DefaultLinkInfo;
import org.onosproject.openstacktelemetry.api.LinkInfo;
import org.onosproject.openstacktelemetry.api.LinkStatsInfo;

import java.util.HashMap;
import java.util.Map;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.onosproject.net.NetTestTools.APP_ID;
import static org.onosproject.openstacktelemetry.api.DefaultLinkStatsInfoTest.createLinkStatsInfo1;
import static org.onosproject.openstacktelemetry.codec.json.ThreeDVLinkInfoJsonMatcher.matchesLinkInfo;

/**
 * Unit tests for 3DV LinkInfo codec.
 */
public class ThreeDVLinkInfoJsonCodecTest {

    MockCodecContext context;
    JsonCodec<LinkInfo> linkInfoCodec;
    JsonCodec<LinkStatsInfo> linkStatsInfoCodec;
    final CoreService mockCoreService = createMock(CoreService.class);
    private static final String REST_APP_ID = "org.onosproject.rest";

    private static final String LINK_ID = "of:0000000000000001_1";
    private static final String SRC_IP = "10.10.10.1";
    private static final String DST_IP = "20.20.20.1";
    private static final int SRC_PORT = 100;
    private static final int DST_PORT = 200;
    private static final String PROTOCOL = "TCP";
    private static final LinkStatsInfo LINK_STATS = createLinkStatsInfo1();

    @Before
    public void setUp() {
        context = new MockCodecContext();
        linkInfoCodec = new ThreeDVLinkInfoJsonCodec();
        linkStatsInfoCodec = new ThreeDVLinkStatsInfoJsonCodec();

        assertThat(linkInfoCodec, notNullValue());
        assertThat(linkStatsInfoCodec, notNullValue());

        expect(mockCoreService.registerApplication(REST_APP_ID))
                .andReturn(APP_ID).anyTimes();
        replay(mockCoreService);
        context.registerService(CoreService.class, mockCoreService);
    }

    /**
     * Tests the Link Info encode.
     */
    @Test
    public void testLinkInfoEncode() {
        LinkInfo linkInfo = DefaultLinkInfo.builder()
                .withLinkId(LINK_ID)
                .withSrcIp(SRC_IP)
                .withDstIp(DST_IP)
                .withSrcPort(SRC_PORT)
                .withDstPort(DST_PORT)
                .withProtocol(PROTOCOL)
                .withLinkStats(LINK_STATS)
                .build();

        ObjectNode nodeJson = linkInfoCodec.encode(linkInfo, context);
        assertThat(nodeJson, matchesLinkInfo(linkInfo));
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
            if (entityClass == LinkInfo.class) {
                return (JsonCodec<T>) linkInfoCodec;
            }
            if (entityClass == LinkStatsInfo.class) {
                return (JsonCodec<T>) linkStatsInfoCodec;
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
