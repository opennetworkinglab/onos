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
package org.onosproject.openstacktelemetry.codec.bytebuffer;

import com.google.common.testing.EqualsTester;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.TpPort;
import org.onlab.packet.VlanId;
import org.onosproject.net.DeviceId;
import org.onosproject.openstacktelemetry.api.DefaultFlowInfo;
import org.onosproject.openstacktelemetry.api.DefaultStatsInfo;
import org.onosproject.openstacktelemetry.api.FlowInfo;
import org.onosproject.openstacktelemetry.api.StatsInfo;

import java.nio.ByteBuffer;

/**
 * Unit tests for TinaFlowInfoByteBufferCodec.
 */
public final class TinaFlowInfoByteBufferCodecTest {

    private static final byte FLOW_TYPE = 1;
    private static final DeviceId DEVICE_ID = DeviceId.deviceId("of:00000000000000a1");
    private static final int INPUT_INTERFACE_ID = 10;
    private static final int OUTPUT_INTERFACE_ID = 10;
    private static final VlanId VLAN_ID = VlanId.vlanId("100");
    private static final IpAddress SRC_IP_ADDRESS = IpAddress.valueOf("10.10.10.1");
    private static final IpPrefix SRC_IP_PREFIX = IpPrefix.valueOf(SRC_IP_ADDRESS, 24);
    private static final IpAddress DST_IP_ADDRESS = IpAddress.valueOf("20.20.20.1");
    private static final IpPrefix DST_IP_PREFIX = IpPrefix.valueOf(DST_IP_ADDRESS, 24);
    private static final TpPort SRC_PORT_NUMBER = TpPort.tpPort(1000);
    private static final TpPort DST_PORT_NUMBER = TpPort.tpPort(2000);
    private static final byte PROTOCOL = 10;
    private static final MacAddress SRC_MAC_ADDRESS = MacAddress.valueOf("AA:BB:CC:DD:EE:FF");
    private static final MacAddress DST_MAC_ADDRESS = MacAddress.valueOf("FF:EE:DD:CC:BB:AA");

    private FlowInfo info;
    private final TinaFlowInfoByteBufferCodec codec = new TinaFlowInfoByteBufferCodec();

    /**
     * Initial setup for this unit test.
     */
    @Before
    public void setup() {
        StatsInfo statsInfo = new DefaultStatsInfo.DefaultBuilder().build();
        FlowInfo.Builder builder = new DefaultFlowInfo.DefaultBuilder();

        info = builder
                .withFlowType(FLOW_TYPE)
                .withDeviceId(DEVICE_ID)
                .withInputInterfaceId(INPUT_INTERFACE_ID)
                .withOutputInterfaceId(OUTPUT_INTERFACE_ID)
                .withVlanId(VLAN_ID)
                .withSrcIp(SRC_IP_PREFIX)
                .withDstIp(DST_IP_PREFIX)
                .withSrcPort(SRC_PORT_NUMBER)
                .withDstPort(DST_PORT_NUMBER)
                .withProtocol(PROTOCOL)
                .withSrcMac(SRC_MAC_ADDRESS)
                .withDstMac(DST_MAC_ADDRESS)
                .withStatsInfo(statsInfo)
                .build();
    }

    @Test
    public void testEncodeDecode() {
        ByteBuffer buffer = codec.encode(info);
        FlowInfo decoded = codec.decode(ByteBuffer.wrap(buffer.array()));
        new EqualsTester().addEqualityGroup(info, decoded).testEquals();
    }
}
