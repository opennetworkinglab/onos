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
package org.onosproject.openstacktelemetry.api;

import com.google.common.testing.EqualsTester;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.TpPort;
import org.onlab.packet.VlanId;
import org.onosproject.net.DeviceId;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * Unit tests for DefaultFlowInfo class.
 */
public final class DefaultFlowInfoTest {

    private static final String IP_ADDRESS_1 = "10.10.10.1";
    private static final String IP_ADDRESS_2 = "20.20.20.1";

    private static final String MAC_ADDRESS_1 = "AA:BB:CC:DD:EE:FF";
    private static final String MAC_ADDRESS_2 = "FF:EE:DD:CC:BB:AA";

    private static final int IP_PREFIX_LENGTH_1 = 10;
    private static final int IP_PREFIX_LENGTH_2 = 20;

    private static final int PORT_1 = 1000;
    private static final int PORT_2 = 2000;

    private static final int STATIC_INTEGER_1 = 1;
    private static final int STATIC_INTEGER_2 = 2;

    private static final String STATIC_STRING_1 = "1";
    private static final String STATIC_STRING_2 = "2";


    private FlowInfo info1;
    private FlowInfo sameAsInfo1;
    private FlowInfo info2;

    /**
     * Initial setup for this unit test.
     */
    @Before
    public void setUp() {

        FlowInfo.Builder builder1 = new DefaultFlowInfo.DefaultBuilder();
        FlowInfo.Builder builder2 = new DefaultFlowInfo.DefaultBuilder();
        FlowInfo.Builder builder3 = new DefaultFlowInfo.DefaultBuilder();

        StatsInfo statsInfo = new DefaultStatsInfo.DefaultBuilder().build();

        info1 = builder1
                .withFlowType((byte) STATIC_INTEGER_1)
                .withInputInterfaceId(STATIC_INTEGER_1)
                .withOutputInterfaceId(STATIC_INTEGER_1)
                .withDeviceId(DeviceId.deviceId(STATIC_STRING_1))
                .withSrcIp(IpPrefix.valueOf(
                        IpAddress.valueOf(IP_ADDRESS_1), IP_PREFIX_LENGTH_1))
                .withDstIp(IpPrefix.valueOf(
                        IpAddress.valueOf(IP_ADDRESS_1), IP_PREFIX_LENGTH_1))
                .withSrcPort(TpPort.tpPort(PORT_1))
                .withDstPort(TpPort.tpPort(PORT_1))
                .withProtocol((byte) STATIC_INTEGER_1)
                .withVlanId(VlanId.vlanId(STATIC_STRING_1))
                .withSrcMac(MacAddress.valueOf(MAC_ADDRESS_1))
                .withDstMac(MacAddress.valueOf(MAC_ADDRESS_1))
                .withStatsInfo(statsInfo)
                .build();

        sameAsInfo1 = builder2
                .withFlowType((byte) STATIC_INTEGER_1)
                .withInputInterfaceId(STATIC_INTEGER_1)
                .withOutputInterfaceId(STATIC_INTEGER_1)
                .withDeviceId(DeviceId.deviceId(STATIC_STRING_1))
                .withSrcIp(IpPrefix.valueOf(
                        IpAddress.valueOf(IP_ADDRESS_1), IP_PREFIX_LENGTH_1))
                .withDstIp(IpPrefix.valueOf(
                        IpAddress.valueOf(IP_ADDRESS_1), IP_PREFIX_LENGTH_1))
                .withSrcPort(TpPort.tpPort(PORT_1))
                .withDstPort(TpPort.tpPort(PORT_1))
                .withProtocol((byte) STATIC_INTEGER_1)
                .withVlanId(VlanId.vlanId(STATIC_STRING_1))
                .withSrcMac(MacAddress.valueOf(MAC_ADDRESS_1))
                .withDstMac(MacAddress.valueOf(MAC_ADDRESS_1))
                .withStatsInfo(statsInfo)
                .build();

        info2 = builder3
                .withFlowType((byte) STATIC_INTEGER_2)
                .withInputInterfaceId(STATIC_INTEGER_2)
                .withOutputInterfaceId(STATIC_INTEGER_2)
                .withDeviceId(DeviceId.deviceId(STATIC_STRING_2))
                .withSrcIp(IpPrefix.valueOf(
                        IpAddress.valueOf(IP_ADDRESS_2), IP_PREFIX_LENGTH_2))
                .withDstIp(IpPrefix.valueOf(
                        IpAddress.valueOf(IP_ADDRESS_2), IP_PREFIX_LENGTH_2))
                .withSrcPort(TpPort.tpPort(PORT_2))
                .withDstPort(TpPort.tpPort(PORT_2))
                .withProtocol((byte) STATIC_INTEGER_2)
                .withVlanId(VlanId.vlanId(STATIC_STRING_2))
                .withSrcMac(MacAddress.valueOf(MAC_ADDRESS_2))
                .withDstMac(MacAddress.valueOf(MAC_ADDRESS_2))
                .withStatsInfo(statsInfo)
                .build();
    }

    /**
     * Tests class immutability.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(DefaultFlowInfo.class);
    }

    /**
     * Tests object equality.
     */
    @Test
    public void testEquality() {
        new EqualsTester()
                .addEqualityGroup(info1, sameAsInfo1)
                .addEqualityGroup(info2).testEquals();
    }

    /**
     * Tests object construction.
     */
    @Test
    public void testConstruction() {
        FlowInfo info = info1;

        assertThat(info.flowType(), is((byte) STATIC_INTEGER_1));
        assertThat(info.inputInterfaceId(), is(STATIC_INTEGER_1));
        assertThat(info.outputInterfaceId(), is(STATIC_INTEGER_1));
        assertThat(info.deviceId(), is(DeviceId.deviceId(STATIC_STRING_1)));
        assertThat(info.srcIp(), is(IpPrefix.valueOf(
                IpAddress.valueOf(IP_ADDRESS_1), IP_PREFIX_LENGTH_1)));
        assertThat(info.dstIp(), is(IpPrefix.valueOf(
                IpAddress.valueOf(IP_ADDRESS_1), IP_PREFIX_LENGTH_1)));
        assertThat(info.srcPort(), is(TpPort.tpPort(PORT_1)));
        assertThat(info.dstPort(), is(TpPort.tpPort(PORT_1)));
        assertThat(info.protocol(), is((byte) STATIC_INTEGER_1));
        assertThat(info.vlanId(), is(VlanId.vlanId(STATIC_STRING_1)));
        assertThat(info.srcMac(), is(MacAddress.valueOf(MAC_ADDRESS_1)));
        assertThat(info.dstMac(), is(MacAddress.valueOf(MAC_ADDRESS_1)));
    }
}
