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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.testing.EqualsTester;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.TpPort;
import org.onlab.packet.VlanId;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.codec.impl.CodecManager;
import org.onosproject.core.CoreService;
import org.onosproject.net.DeviceId;
import org.onosproject.openstacktelemetry.api.FlowInfo;
import org.onosproject.openstacktelemetry.api.StatsInfo;
import org.onosproject.openstacktelemetry.api.DefaultFlowInfo;
import org.onosproject.openstacktelemetry.api.DefaultStatsInfo;

import java.util.HashMap;
import java.util.Map;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.onosproject.net.NetTestTools.APP_ID;
import static org.onosproject.openstacktelemetry.codec.rest.FlowInfoJsonMatcher.matchesFlowInfo;

/**
 * Unit tests for FlowInfo codec.
 */
public class FlowInfoJsonCodecTest {
    MockCodecContext context;
    JsonCodec<FlowInfo> flowInfoCodec;
    JsonCodec<StatsInfo> statsInfoCodec;
    final CoreService mockCoreService = createMock(CoreService.class);
    private static final String REST_APP_ID = "org.onosproject.rest";

    private static final int INPUT_INTERFACE_ID = 1;
    private static final int OUTPUT_INTERFACE_ID = 2;

    private static final int VLAN_ID = 1;
    private static final short VXLAN_ID = 10;
    private static final int PROTOCOL = 1;
    private static final int FLOW_TYPE = 1;
    private static final String DEVICE_ID = "of:00000000000000a1";

    private static final String SRC_IP_ADDRESS = "10.10.10.1";
    private static final int SRC_IP_PREFIX = 24;
    private static final String DST_IP_ADDRESS = "20.20.20.1";
    private static final int DST_IP_PREFIX = 24;
    private static final int SRC_PORT = 1000;
    private static final int DST_PORT = 2000;
    private static final String SRC_MAC_ADDRESS = "AA:BB:CC:DD:EE:FF";
    private static final String DST_MAC_ADDRESS = "FF:EE:DD:CC:BB:AA";

    private static final long LONG_VALUE = 1L;
    private static final int INTEGER_VALUE = 1;
    private static final short SHORT_VALUE = (short) 1;

    /**
     * Initial setup for this unit test.
     */
    @Before
    public void setUp() {
        context = new MockCodecContext();
        flowInfoCodec = new FlowInfoJsonCodec();
        statsInfoCodec = new StatsInfoJsonCodec();

        assertThat(flowInfoCodec, notNullValue());
        assertThat(statsInfoCodec, notNullValue());

        expect(mockCoreService.registerApplication(REST_APP_ID))
                .andReturn(APP_ID).anyTimes();
        replay(mockCoreService);
        context.registerService(CoreService.class, mockCoreService);
    }

    /**
     * Tests the flow info encoding.
     */
    @Test
    public void testEncode() {
        StatsInfo statsInfo = new DefaultStatsInfo.DefaultBuilder()
                                    .withStartupTime(LONG_VALUE)
                                    .withFstPktArrTime(LONG_VALUE)
                                    .withLstPktOffset(INTEGER_VALUE)
                                    .withPrevAccBytes(LONG_VALUE)
                                    .withPrevAccPkts(INTEGER_VALUE)
                                    .withCurrAccBytes(LONG_VALUE)
                                    .withCurrAccPkts(INTEGER_VALUE)
                                    .withErrorPkts(SHORT_VALUE)
                                    .withDropPkts(SHORT_VALUE)
                                    .build();
        FlowInfo flowInfo = new DefaultFlowInfo.DefaultBuilder()
                                    .withFlowType((byte) FLOW_TYPE)
                                    .withDeviceId(DeviceId.deviceId(DEVICE_ID))
                                    .withInputInterfaceId(INPUT_INTERFACE_ID)
                                    .withOutputInterfaceId(OUTPUT_INTERFACE_ID)
                                    .withVlanId(VlanId.vlanId((short) VLAN_ID))
                                    .withSrcIp(IpPrefix.valueOf(
                                            IpAddress.valueOf(SRC_IP_ADDRESS), SRC_IP_PREFIX))
                                    .withDstIp(IpPrefix.valueOf(
                                            IpAddress.valueOf(DST_IP_ADDRESS), DST_IP_PREFIX))
                                    .withSrcPort(TpPort.tpPort(SRC_PORT))
                                    .withDstPort(TpPort.tpPort(DST_PORT))
                                    .withProtocol((byte) PROTOCOL)
                                    .withSrcMac(MacAddress.valueOf(SRC_MAC_ADDRESS))
                                    .withDstMac(MacAddress.valueOf(DST_MAC_ADDRESS))
                                    .withStatsInfo(statsInfo)
                                    .build();

        ObjectNode nodeJson = flowInfoCodec.encode(flowInfo, context);
        assertThat(nodeJson, matchesFlowInfo(flowInfo));

        FlowInfo flowInfoDecoded = flowInfoCodec.decode(nodeJson, context);
        new EqualsTester().addEqualityGroup(flowInfo, flowInfoDecoded).testEquals();
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
            if (entityClass == FlowInfo.class) {
                return (JsonCodec<T>) flowInfoCodec;
            }
            if (entityClass == StatsInfo.class) {
                return (JsonCodec<T>) statsInfoCodec;
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
