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
        Criterion criterion = Criteria.matchEthType((short) 3);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result.get("type").textValue(), is(criterion.type().toString()));
        assertThat(result.get("ethType").asInt(), is(3));
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
        Criterion criterion = Criteria.matchVlanPcp((byte) 4);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result.get("type").textValue(), is(criterion.type().toString()));
        assertThat(result.get("priority").asInt(), is(4));
    }

    /**
     * Tests IP DSCP criterion.
     */
    @Test
    public void matchIPDscpTest() {
        Criterion criterion = Criteria.matchIPDscp((byte) 5);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result.get("type").textValue(), is(criterion.type().toString()));
        assertThat(result.get("ipDscp").asInt(), is(5));
    }

    /**
     * Tests IP ECN criterion.
     */
    @Test
    public void matchIPEcnTest() {
        Criterion criterion = Criteria.matchIPEcn((byte) 2);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result.get("type").textValue(), is(criterion.type().toString()));
        assertThat(result.get("ipEcn").asInt(), is(2));
    }

    /**
     * Tests IP protocol criterion.
     */
    @Test
    public void matchIPProtocolTest() {
        Criterion criterion = Criteria.matchIPProtocol((byte) 7);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result.get("type").textValue(), is(criterion.type().toString()));
        assertThat(result.get("protocol").asInt(), is(7));
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
        Criterion criterion = Criteria.matchTcpSrc((short) 22);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result.get("type").textValue(), is(criterion.type().toString()));
        assertThat(result.get("tcpPort").asInt(), is(22));
    }

    /**
     * Tests destination TCP port criterion.
     */
    @Test
    public void matchTcpDstTest() {
        Criterion criterion = Criteria.matchTcpDst((short) 22);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result.get("type").textValue(), is(criterion.type().toString()));
        assertThat(result.get("tcpPort").asInt(), is(22));
    }

    /**
     * Tests source UDP port criterion.
     */
    @Test
    public void matchUdpSrcTest() {
        Criterion criterion = Criteria.matchUdpSrc((short) 22);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result.get("type").textValue(), is(criterion.type().toString()));
        assertThat(result.get("udpPort").asInt(), is(22));
    }

    /**
     * Tests destination UDP criterion.
     */
    @Test
    public void matchUdpDstTest() {
        Criterion criterion = Criteria.matchUdpDst((short) 22);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result.get("type").textValue(), is(criterion.type().toString()));
        assertThat(result.get("udpPort").asInt(), is(22));
    }

    /**
     * Tests source SCTP criterion.
     */
    @Test
    public void matchSctpSrcTest() {
        Criterion criterion = Criteria.matchSctpSrc((short) 22);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result.get("type").textValue(), is(criterion.type().toString()));
        assertThat(result.get("sctpPort").asInt(), is(22));
    }

    /**
     * Tests destination SCTP criterion.
     */
    @Test
    public void matchSctpDstTest() {
        Criterion criterion = Criteria.matchSctpDst((short) 22);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result.get("type").textValue(), is(criterion.type().toString()));
        assertThat(result.get("sctpPort").asInt(), is(22));
    }

    /**
     * Tests ICMP type criterion.
     */
    @Test
    public void matchIcmpTypeTest() {
        Criterion criterion = Criteria.matchIcmpType((byte) 6);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result.get("type").textValue(), is(criterion.type().toString()));
        assertThat(result.get("icmpType").asInt(), is(6));
    }

    /**
     * Tests ICMP code criterion.
     */
    @Test
    public void matchIcmpCodeTest() {
        Criterion criterion = Criteria.matchIcmpCode((byte) 6);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result.get("type").textValue(), is(criterion.type().toString()));
        assertThat(result.get("icmpCode").asInt(), is(6));
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
        Criterion criterion = Criteria.matchIPv6FlowLabel(7);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result.get("type").textValue(), is(criterion.type().toString()));
        assertThat(result.get("flowLabel").asInt(), is(7));
    }

    /**
     * Tests ICMP v6 type criterion.
     */
    @Test
    public void matchIcmpv6TypeTest() {
        Criterion criterion = Criteria.matchIcmpv6Type((byte) 15);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result.get("type").textValue(), is(criterion.type().toString()));
        assertThat(result.get("icmpv6Type").asInt(), is(15));
    }

    /**
     * Tests ICMP v6 code criterion.
     */
    @Test
    public void matchIcmpv6CodeTest() {
        Criterion criterion = Criteria.matchIcmpv6Code((byte) 17);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result.get("type").textValue(), is(criterion.type().toString()));
        assertThat(result.get("icmpv6Code").asInt(), is(17));
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
        Criterion criterion = Criteria.matchMplsLabel(88);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result.get("type").textValue(), is(criterion.type().toString()));
        assertThat(result.get("label").asInt(), is(88));
    }

    /**
     * Tests lambda criterion.
     */
    @Test
    public void matchLambdaTest() {
        Criterion criterion = Criteria.matchLambda((short) 9);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result.get("type").textValue(), is(criterion.type().toString()));
        assertThat(result.get("lambda").asInt(), is(9));
    }

    /**
     * Tests optical signal type criterion.
     */
    @Test
    public void matchOpticalSignalTypeTest() {
        Criterion criterion = Criteria.matchOpticalSignalType((short) 11);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result.get("type").textValue(), is(criterion.type().toString()));
        assertThat(result.get("signalType").asInt(), is(11));
    }

}
