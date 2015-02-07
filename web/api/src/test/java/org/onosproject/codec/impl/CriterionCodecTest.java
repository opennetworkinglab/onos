/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.codec.impl;

import java.util.EnumMap;

import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.Ip6Address;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.criteria.Criterion;

import com.fasterxml.jackson.databind.node.ObjectNode;

import static org.onlab.junit.TestUtils.getField;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Unit tests for criterion codec.
 */
public class CriterionCodecTest {

    CodecContext context;
    JsonCodec<Criterion> criterionCodec;
    final PortNumber port = PortNumber.portNumber(1);
    final IpPrefix ipPrefix4 = IpPrefix.valueOf("10.1.1.0/24");
    final IpPrefix ipPrefix6 = IpPrefix.valueOf("fe80::/64");
    final MacAddress mac1 = MacAddress.valueOf("00:00:11:00:00:01");

    /**
     * Sets up for each test.  Creates a context and fetches the criterion
     * codec.
     */
    @Before
    public void setUp() {
        context = new MockCodecContext();
        criterionCodec = context.codec(Criterion.class);
        assertThat(criterionCodec, notNullValue());
    }


    /**
     * Checks that all criterion types are covered by the codec.
     */
    @Test
    public void checkCriterionTypes() throws Exception {
        EnumMap<Criterion.Type, Object> formatMap =
                getField(criterionCodec, "formatMap");
        assertThat(formatMap, notNullValue());

        for (Criterion.Type type : Criterion.Type.values()) {
            assertThat("Entry not found for " + type.toString(),
                    formatMap.get(type), notNullValue());
        }
    }

    /**
     * Tests in port criterion.
     */
    @Test
    public void matchInPortTest() {
        Criterion criterion = Criteria.matchInPort(port);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result.get("type").textValue(), is(criterion.type().toString()));
        assertThat(result.get("port").asLong(), is(port.toLong()));
    }

    /**
     * Tests in physical port criterion.
     */
    @Test
    public void matchInPhyPortTest() {
        Criterion criterion = Criteria.matchInPhyPort(port);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result.get("type").textValue(), is(criterion.type().toString()));
        assertThat(result.get("port").asLong(), is(port.toLong()));
    }

    /**
     * Tests metadata criterion.
     */
    @Test
    public void matchMetadataTest() {
        Criterion criterion = Criteria.matchMetadata(0xabcdL);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result.get("type").textValue(), is(criterion.type().toString()));
        assertThat(result.get("metadata").asLong(), is(0xabcdL));
    }

    /**
     * Tests ethernet destination criterion.
     */
    @Test
    public void matchEthDstTest() {
        Criterion criterion = Criteria.matchEthDst(mac1);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result.get("type").textValue(), is(criterion.type().toString()));
        assertThat(result.get("mac").asText(), is(mac1.toString()));
    }

    /**
     * Tests ethernet source criterion.
     */
    @Test
    public void matchEthSrcTest() {
        Criterion criterion = Criteria.matchEthSrc(mac1);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result.get("type").textValue(), is(criterion.type().toString()));
        assertThat(result.get("mac").asText(), is(mac1.toString()));
    }

    /**
     * Tests ethernet type criterion.
     */
    @Test
    public void matchEthTypeTest() {
        Criterion criterion = Criteria.matchEthType((short) 0x8844);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result.get("type").textValue(), is(criterion.type().toString()));
        assertThat(result.get("ethType").asInt(), is(0x8844));
    }

    /**
     * Tests VLAN Id criterion.
     */
    @Test
    public void matchVlanIdTest() {
        Criterion criterion = Criteria.matchVlanId(VlanId.ANY);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result.get("type").textValue(), is(criterion.type().toString()));
        assertThat((short) result.get("vlanId").asInt(), is(VlanId.ANY.toShort()));
    }

    /**
     * Tests VLAN PCP criterion.
     */
    @Test
    public void matchVlanPcpTest() {
        Criterion criterion = Criteria.matchVlanPcp((byte) 7);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result.get("type").textValue(), is(criterion.type().toString()));
        assertThat(result.get("priority").asInt(), is(7));
    }

    /**
     * Tests IP DSCP criterion.
     */
    @Test
    public void matchIPDscpTest() {
        Criterion criterion = Criteria.matchIPDscp((byte) 63);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result.get("type").textValue(), is(criterion.type().toString()));
        assertThat(result.get("ipDscp").asInt(), is(63));
    }

    /**
     * Tests IP ECN criterion.
     */
    @Test
    public void matchIPEcnTest() {
        Criterion criterion = Criteria.matchIPEcn((byte) 7);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result.get("type").textValue(), is(criterion.type().toString()));
        assertThat(result.get("ipEcn").asInt(), is(7));
    }

    /**
     * Tests IP protocol criterion.
     */
    @Test
    public void matchIPProtocolTest() {
        Criterion criterion = Criteria.matchIPProtocol((byte) 250);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result.get("type").textValue(), is(criterion.type().toString()));
        assertThat(result.get("protocol").asInt(), is(250));
    }

    /**
     * Tests IP source criterion.
     */
    @Test
    public void matchIPSrcTest() {
        Criterion criterion = Criteria.matchIPSrc(ipPrefix4);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result.get("type").textValue(), is(criterion.type().toString()));
        assertThat(result.get("ip").asText(), is(ipPrefix4.toString()));
    }

    /**
     * Tests IP destination criterion.
     */
    @Test
    public void matchIPDstTest() {
        Criterion criterion = Criteria.matchIPDst(ipPrefix4);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result.get("type").textValue(), is(criterion.type().toString()));
        assertThat(result.get("ip").asText(), is(ipPrefix4.toString()));
    }

    /**
     * Tests source TCP port criterion.
     */
    @Test
    public void matchTcpSrcTest() {
        Criterion criterion = Criteria.matchTcpSrc((short) 40000);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result.get("type").textValue(), is(criterion.type().toString()));
        assertThat(result.get("tcpPort").asInt(), is(40000));
    }

    /**
     * Tests destination TCP port criterion.
     */
    @Test
    public void matchTcpDstTest() {
        Criterion criterion = Criteria.matchTcpDst((short) 40000);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result.get("type").textValue(), is(criterion.type().toString()));
        assertThat(result.get("tcpPort").asInt(), is(40000));
    }

    /**
     * Tests source UDP port criterion.
     */
    @Test
    public void matchUdpSrcTest() {
        Criterion criterion = Criteria.matchUdpSrc((short) 40000);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result.get("type").textValue(), is(criterion.type().toString()));
        assertThat(result.get("udpPort").asInt(), is(40000));
    }

    /**
     * Tests destination UDP criterion.
     */
    @Test
    public void matchUdpDstTest() {
        Criterion criterion = Criteria.matchUdpDst((short) 40000);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result.get("type").textValue(), is(criterion.type().toString()));
        assertThat(result.get("udpPort").asInt(), is(40000));
    }

    /**
     * Tests source SCTP criterion.
     */
    @Test
    public void matchSctpSrcTest() {
        Criterion criterion = Criteria.matchSctpSrc((short) 40000);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result.get("type").textValue(), is(criterion.type().toString()));
        assertThat(result.get("sctpPort").asInt(), is(40000));
    }

    /**
     * Tests destination SCTP criterion.
     */
    @Test
    public void matchSctpDstTest() {
        Criterion criterion = Criteria.matchSctpDst((short) 40000);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result.get("type").textValue(), is(criterion.type().toString()));
        assertThat(result.get("sctpPort").asInt(), is(40000));
    }

    /**
     * Tests ICMP type criterion.
     */
    @Test
    public void matchIcmpTypeTest() {
        Criterion criterion = Criteria.matchIcmpType((byte) 250);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result.get("type").textValue(), is(criterion.type().toString()));
        assertThat(result.get("icmpType").asInt(), is(250));
    }

    /**
     * Tests ICMP code criterion.
     */
    @Test
    public void matchIcmpCodeTest() {
        Criterion criterion = Criteria.matchIcmpCode((byte) 250);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result.get("type").textValue(), is(criterion.type().toString()));
        assertThat(result.get("icmpCode").asInt(), is(250));
    }

    /**
     * Tests IPv6 source criterion.
     */
    @Test
    public void matchIPv6SrcTest() {
        Criterion criterion = Criteria.matchIPv6Src(ipPrefix6);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result.get("type").textValue(), is(criterion.type().toString()));
        assertThat(result.get("ip").asText(), is(ipPrefix6.toString()));
    }

    /**
     * Tests IPv6 destination criterion.
     */
    @Test
    public void matchIPv6DstTest() {
        Criterion criterion = Criteria.matchIPv6Dst(ipPrefix6);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result.get("type").textValue(), is(criterion.type().toString()));
        assertThat(result.get("ip").asText(), is(ipPrefix6.toString()));
    }

    /**
     * Tests IPv6 flow label criterion.
     */
    @Test
    public void matchIPv6FlowLabelTest() {
        Criterion criterion = Criteria.matchIPv6FlowLabel(0xffffe);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result.get("type").textValue(), is(criterion.type().toString()));
        assertThat(result.get("flowLabel").asInt(), is(0xffffe));
    }

    /**
     * Tests ICMP v6 type criterion.
     */
    @Test
    public void matchIcmpv6TypeTest() {
        Criterion criterion = Criteria.matchIcmpv6Type((byte) 250);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result.get("type").textValue(), is(criterion.type().toString()));
        assertThat(result.get("icmpv6Type").asInt(), is(250));
    }

    /**
     * Tests ICMP v6 code criterion.
     */
    @Test
    public void matchIcmpv6CodeTest() {
        Criterion criterion = Criteria.matchIcmpv6Code((byte) 250);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result.get("type").textValue(), is(criterion.type().toString()));
        assertThat(result.get("icmpv6Code").asInt(), is(250));
    }

    /**
     * Tests IPV6 target address criterion.
     */
    @Test
    public void matchIPv6NDTargetAddressTest() {
        Criterion criterion =
                Criteria.matchIPv6NDTargetAddress(
                        Ip6Address.valueOf("1111:2222::"));
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result.get("type").textValue(), is(criterion.type().toString()));
        assertThat(result.get("targetAddress").asText(), is("1111:2222::"));
    }

    /**
     * Tests IPV6 SLL criterion.
     */
    @Test
    public void matchIPv6NDSourceLinkLayerAddressTest() {
        Criterion criterion = Criteria.matchIPv6NDSourceLinkLayerAddress(mac1);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result.get("type").textValue(), is(criterion.type().toString()));
        assertThat(result.get("mac").asText(), is(mac1.toString()));
    }

    /**
     * Tests IPV6 TLL criterion.
     */
    @Test
    public void matchIPv6NDTargetLinkLayerAddressTest() {
        Criterion criterion = Criteria.matchIPv6NDTargetLinkLayerAddress(mac1);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result.get("type").textValue(), is(criterion.type().toString()));
        assertThat(result.get("mac").asText(), is(mac1.toString()));
    }

    /**
     * Tests MPLS label criterion.
     */
    @Test
    public void matchMplsLabelTest() {
        Criterion criterion = Criteria.matchMplsLabel(0xffffe);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result.get("type").textValue(), is(criterion.type().toString()));
        assertThat(result.get("label").asInt(), is(0xffffe));
    }

    /**
     * Tests IPv6 Extension Header pseudo-field flags criterion.
     */
    @Test
    public void matchIPv6ExthdrFlagsTest() {
        int exthdrFlags =
            Criterion.IPv6ExthdrFlags.NONEXT.getValue() |
            Criterion.IPv6ExthdrFlags.ESP.getValue() |
            Criterion.IPv6ExthdrFlags.AUTH.getValue() |
            Criterion.IPv6ExthdrFlags.DEST.getValue() |
            Criterion.IPv6ExthdrFlags.FRAG.getValue() |
            Criterion.IPv6ExthdrFlags.ROUTER.getValue() |
            Criterion.IPv6ExthdrFlags.HOP.getValue() |
            Criterion.IPv6ExthdrFlags.UNREP.getValue() |
            Criterion.IPv6ExthdrFlags.UNSEQ.getValue();
        Criterion criterion = Criteria.matchIPv6ExthdrFlags(exthdrFlags);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result.get("type").textValue(), is(criterion.type().toString()));
        assertThat(result.get("exthdrFlags").asInt(), is(exthdrFlags));
    }

    /**
     * Tests lambda criterion.
     */
    @Test
    public void matchLambdaTest() {
        Criterion criterion = Criteria.matchLambda((short) 40000);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result.get("type").textValue(), is(criterion.type().toString()));
        assertThat(result.get("lambda").asInt(), is(40000));
    }

    /**
     * Tests optical signal type criterion.
     */
    @Test
    public void matchOpticalSignalTypeTest() {
        Criterion criterion = Criteria.matchOpticalSignalType((byte) 250);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result.get("type").textValue(), is(criterion.type().toString()));
        assertThat(result.get("signalType").asInt(), is(250));
    }

}
