/*
 * Copyright 2015-present Open Networking Foundation
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

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.EnumMap;

import com.fasterxml.jackson.databind.JsonNode;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.Ip6Address;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.MplsLabel;
import org.onlab.packet.TpPort;
import org.onlab.packet.VlanId;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.GridType;
import org.onosproject.net.Lambda;
import org.onosproject.net.OchSignalType;
import org.onosproject.net.OduSignalId;
import org.onosproject.net.OduSignalType;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.criteria.Criterion;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.net.flow.criteria.PiCriterion;
import org.onosproject.net.pi.model.PiMatchFieldId;
import org.onosproject.net.pi.runtime.PiExactFieldMatch;
import org.onosproject.net.pi.runtime.PiFieldMatch;
import org.onosproject.net.pi.runtime.PiLpmFieldMatch;
import org.onosproject.net.pi.runtime.PiOptionalFieldMatch;
import org.onosproject.net.pi.runtime.PiRangeFieldMatch;
import org.onosproject.net.pi.runtime.PiTernaryFieldMatch;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.onlab.junit.TestUtils.getField;
import static org.onlab.util.ImmutableByteSequence.copyFrom;
import static org.onosproject.codec.impl.CriterionJsonMatcher.matchesCriterion;

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
    final MacAddress mcastMac = MacAddress.valueOf("01:00:5E:00:00:01");
    final MacAddress mcastMacMask = MacAddress.valueOf("FF:FF:FF:80:00:00");
    final TpPort tpPort = TpPort.tpPort(40000);
    final TpPort tpPortMask = TpPort.tpPort(65472);
    final int tributaryPortNumber = 11;
    final int tributarySlotLen = 80;
    final byte[] tributarySlotBitmap = new byte[] {1, 2, 3, 4, 2, 3, 4, 2, 3, 4};


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
        EncodeCriterionCodecHelper encoder = new EncodeCriterionCodecHelper(
                Criteria.dummy(), context);
        EnumMap<Criterion.Type, Object> formatMap =
                getField(encoder, "formatMap");
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
        assertThat(result, matchesCriterion(criterion));
    }

    /**
     * Tests in physical port criterion.
     */
    @Test
    public void matchInPhyPortTest() {
        Criterion criterion = Criteria.matchInPhyPort(port);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result, matchesCriterion(criterion));
    }

    /**
     * Tests metadata criterion.
     */
    @Test
    public void matchMetadataTest() {
        Criterion criterion = Criteria.matchMetadata(0xabcdL);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result, matchesCriterion(criterion));
    }

    /**
     * Tests ethernet destination criterion.
     */
    @Test
    public void matchEthDstTest() {
        Criterion criterion = Criteria.matchEthDst(mac1);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result, matchesCriterion(criterion));
    }

    /**
     * Tests masked ethernet destination criterion (Criterion.Type.ETH_DST_MASKED).
     */
    @Test
    public void matchEthDstMaskTest() {
        Criterion criterion = Criteria.matchEthDstMasked(mcastMac, mcastMacMask);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result, matchesCriterion(criterion));
    }

    /**
     * Tests ethernet source criterion.
     */
    @Test
    public void matchEthSrcTest() {
        Criterion criterion = Criteria.matchEthSrc(mac1);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result, matchesCriterion(criterion));
    }

    /**
     * Tests masked ethernet source criterion (Criterion.Type.ETH_SRC_MASKED).
     */
    @Test
    public void matchEthSrcMaskTest() {
        Criterion criterion = Criteria.matchEthSrcMasked(mcastMac, mcastMacMask);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result, matchesCriterion(criterion));
    }

    /**
     * Tests ethernet type criterion.
     */
    @Test
    public void matchEthTypeTest() {
        Criterion criterion = Criteria.matchEthType((short) 0x8844);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result, matchesCriterion(criterion));
    }

    /**
     * Tests VLAN Id criterion.
     */
    @Test
    public void matchVlanIdTest() {
        Criterion criterion = Criteria.matchVlanId(VlanId.ANY);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result, matchesCriterion(criterion));
    }

    /**
     * Tests VLAN PCP criterion.
     */
    @Test
    public void matchVlanPcpTest() {
        Criterion criterion = Criteria.matchVlanPcp((byte) 7);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result, matchesCriterion(criterion));
    }

    /**
     * Tests IP DSCP criterion.
     */
    @Test
    public void matchIPDscpTest() {
        Criterion criterion = Criteria.matchIPDscp((byte) 63);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result, matchesCriterion(criterion));
    }

    /**
     * Tests IP ECN criterion.
     */
    @Test
    public void matchIPEcnTest() {
        Criterion criterion = Criteria.matchIPEcn((byte) 3);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result, matchesCriterion(criterion));
    }

    /**
     * Tests IP protocol criterion.
     */
    @Test
    public void matchIPProtocolTest() {
        Criterion criterion = Criteria.matchIPProtocol((byte) 250);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result, matchesCriterion(criterion));
    }

    /**
     * Tests IP source criterion.
     */
    @Test
    public void matchIPSrcTest() {
        Criterion criterion = Criteria.matchIPSrc(ipPrefix4);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result, matchesCriterion(criterion));
    }

    /**
     * Tests IP destination criterion.
     */
    @Test
    public void matchIPDstTest() {
        Criterion criterion = Criteria.matchIPDst(ipPrefix4);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result, matchesCriterion(criterion));
    }

    /**
     * Tests source TCP port criterion.
     */
    @Test
    public void matchTcpSrcTest() {
        Criterion criterion = Criteria.matchTcpSrc(tpPort);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result, matchesCriterion(criterion));
    }

    /**
     * Tests source TCP port masked criterion.
     */
    @Test
    public void matchTcpSrcMaskedTest() {
        Criterion criterion = Criteria.matchTcpSrcMasked(tpPort, tpPortMask);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result, matchesCriterion(criterion));
    }

    /**
     * Tests destination TCP port criterion.
     */
    @Test
    public void matchTcpDstTest() {
        Criterion criterion = Criteria.matchTcpDst(tpPort);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result, matchesCriterion(criterion));
    }

    /**
     * Tests destination TCP port masked criterion.
     */
    @Test
    public void matchTcpDstMaskedTest() {
        Criterion criterion = Criteria.matchTcpDstMasked(tpPort, tpPortMask);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result, matchesCriterion(criterion));
    }

    /**
     * Tests source UDP port criterion.
     */
    @Test
    public void matchUdpSrcTest() {
        Criterion criterion = Criteria.matchUdpSrc(tpPort);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result, matchesCriterion(criterion));
    }

    /**
     * Tests source UDP port masked criterion.
     */
    @Test
    public void matchUdpSrcMaskedTest() {
        Criterion criterion = Criteria.matchUdpSrcMasked(tpPort, tpPortMask);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result, matchesCriterion(criterion));
    }

    /**
     * Tests destination UDP criterion.
     */
    @Test
    public void matchUdpDstTest() {
        Criterion criterion = Criteria.matchUdpDst(tpPort);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result, matchesCriterion(criterion));
    }

    /**
     * Tests destination UDP port masked criterion.
     */
    @Test
    public void matchUdpDstMaskedTest() {
        Criterion criterion = Criteria.matchUdpDstMasked(tpPort, tpPortMask);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result, matchesCriterion(criterion));
    }

    /**
     * Tests source SCTP criterion.
     */
    @Test
    public void matchSctpSrcTest() {
        Criterion criterion = Criteria.matchSctpSrc(tpPort);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result, matchesCriterion(criterion));
    }

    /**
     * Tests source SCTP port masked criterion.
     */
    @Test
    public void matchSctpSrcMaskedTest() {
        Criterion criterion = Criteria.matchSctpSrcMasked(tpPort, tpPortMask);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result, matchesCriterion(criterion));
    }

    /**
     * Tests destination SCTP criterion.
     */
    @Test
    public void matchSctpDstTest() {
        Criterion criterion = Criteria.matchSctpDst(tpPort);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result, matchesCriterion(criterion));
    }

    /**
     * Tests destination SCTP port masked criterion.
     */
    @Test
    public void matchSctpDstMaskedTest() {
        Criterion criterion = Criteria.matchSctpDstMasked(tpPort, tpPortMask);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result, matchesCriterion(criterion));
    }

    /**
     * Tests ICMP type criterion.
     */
    @Test
    public void matchIcmpTypeTest() {
        Criterion criterion = Criteria.matchIcmpType((byte) 250);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result, matchesCriterion(criterion));
    }

    /**
     * Tests ICMP code criterion.
     */
    @Test
    public void matchIcmpCodeTest() {
        Criterion criterion = Criteria.matchIcmpCode((byte) 250);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result, matchesCriterion(criterion));
    }

    /**
     * Tests IPv6 source criterion.
     */
    @Test
    public void matchIPv6SrcTest() {
        Criterion criterion = Criteria.matchIPv6Src(ipPrefix6);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result, matchesCriterion(criterion));
    }

    /**
     * Tests IPv6 destination criterion.
     */
    @Test
    public void matchIPv6DstTest() {
        Criterion criterion = Criteria.matchIPv6Dst(ipPrefix6);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result, matchesCriterion(criterion));
    }

    /**
     * Tests IPv6 flow label criterion.
     */
    @Test
    public void matchIPv6FlowLabelTest() {
        Criterion criterion = Criteria.matchIPv6FlowLabel(0xffffe);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result, matchesCriterion(criterion));
    }

    /**
     * Tests ICMP v6 type criterion.
     */
    @Test
    public void matchIcmpv6TypeTest() {
        Criterion criterion = Criteria.matchIcmpv6Type((byte) 250);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result, matchesCriterion(criterion));
    }

    /**
     * Tests ICMP v6 code criterion.
     */
    @Test
    public void matchIcmpv6CodeTest() {
        Criterion criterion = Criteria.matchIcmpv6Code((byte) 250);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result, matchesCriterion(criterion));
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
        assertThat(result, matchesCriterion(criterion));
    }

    /**
     * Tests IPV6 SLL criterion.
     */
    @Test
    public void matchIPv6NDSourceLinkLayerAddressTest() {
        Criterion criterion = Criteria.matchIPv6NDSourceLinkLayerAddress(mac1);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result, matchesCriterion(criterion));
    }

    /**
     * Tests IPV6 TLL criterion.
     */
    @Test
    public void matchIPv6NDTargetLinkLayerAddressTest() {
        Criterion criterion = Criteria.matchIPv6NDTargetLinkLayerAddress(mac1);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result, matchesCriterion(criterion));
    }

    /**
     * Tests MPLS label criterion.
     */
    @Test
    public void matchMplsLabelTest() {
        Criterion criterion = Criteria.matchMplsLabel(MplsLabel.mplsLabel(0xffffe));
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result, matchesCriterion(criterion));
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

        assertThat(result, matchesCriterion(criterion));
    }

    /**
     * Tests lambda criterion.
     */
    @Test
    public void matchOchSignal() {
        Lambda ochSignal = Lambda.ochSignal(GridType.DWDM, ChannelSpacing.CHL_100GHZ, 4, 8);
        Criterion criterion = Criteria.matchLambda(ochSignal);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result, matchesCriterion(criterion));
    }

    /**
     * Tests Och signal type criterion.
     */
    @Test
    public void matchOchSignalTypeTest() {
        Criterion criterion = Criteria.matchOchSignalType(OchSignalType.FIXED_GRID);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result, matchesCriterion(criterion));
    }

   /**
     * Tests Odu Signal ID criterion.
     */
    @Test
    public void matchOduSignalIdTest() {

        OduSignalId oduSignalId = OduSignalId.oduSignalId(tributaryPortNumber, tributarySlotLen, tributarySlotBitmap);

        Criterion criterion = Criteria.matchOduSignalId(oduSignalId);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result, matchesCriterion(criterion));
    }

    /**
     * Tests Odu Signal Type criterion.
     */
    @Test
    public void matchOduSignalTypeTest() {

        OduSignalType signalType = OduSignalType.ODU2;

        Criterion criterion = Criteria.matchOduSignalType(signalType);
        ObjectNode result = criterionCodec.encode(criterion, context);
        assertThat(result, matchesCriterion(criterion));
    }

    /**
     * Tests protocol-independent type criterion encoding.
     */
    @Test
    public void matchPiTypeEncodingTest() {

        PiMatchFieldId ethMatchFieldId = PiMatchFieldId.of("ethernet_t.etherType");
        byte[] matchExactBytes1 = {0x08, 0x00};
        Criterion exactBytesCriterion = PiCriterion.builder().matchExact(ethMatchFieldId, matchExactBytes1).build();
        ObjectNode exactResult = criterionCodec.encode(exactBytesCriterion, context);
        assertThat(exactResult, matchesCriterion(exactBytesCriterion));

        PiMatchFieldId ipv4MatchFieldId = PiMatchFieldId.of("ipv4_t.dstAddr");
        int mask = 0x00ffffff;
        byte[] matchLpmBytes1 = {0x0a, 0x01, 0x01, 0x01};
        Criterion lpmBytesCriterion = PiCriterion.builder().matchLpm(ipv4MatchFieldId, matchLpmBytes1, mask).build();
        ObjectNode lpmResult = criterionCodec.encode(lpmBytesCriterion, context);
        assertThat(lpmResult, matchesCriterion(lpmBytesCriterion));

        byte[] matchTernaryBytes1 = {0x0a, 0x01, 0x01, 0x01};
        byte[] matchTernaryMaskBytes = {0x7f, 0x7f, 0x7f, 0x00};
        Criterion ternaryBytesCriterion = PiCriterion.builder().matchTernary(ipv4MatchFieldId, matchTernaryBytes1,
                                                                    matchTernaryMaskBytes).build();
        ObjectNode ternaryResult = criterionCodec.encode(ternaryBytesCriterion, context);
        assertThat(ternaryResult, matchesCriterion(ternaryBytesCriterion));

        byte[] matchRangeBytes1 = {0x10};
        byte[] matchRangeHighBytes = {0x30};
        Criterion rangeBytesCriterion = PiCriterion.builder()
                .matchRange(ipv4MatchFieldId, matchRangeBytes1, matchRangeHighBytes).build();
        ObjectNode rangeResult = criterionCodec.encode(rangeBytesCriterion, context);
        assertThat(rangeResult, matchesCriterion(rangeBytesCriterion));

        byte[] matchOptionalBytes = {0x00, 0x11, 0x22, 0x33, 0x44, 0x55};
        Criterion optionalBytesCriterion = PiCriterion.builder().matchOptional(ethMatchFieldId,
                                                                               matchOptionalBytes).build();
        ObjectNode optionalResult = criterionCodec.encode(optionalBytesCriterion, context);
        assertThat(optionalResult, matchesCriterion(optionalBytesCriterion));
    }

    /**
     * Tests protocol-independent type criterion decoding.
     */
    @Test
    public void matchPiTypeDecodingTest() throws IOException {
        Criterion criterion = getCriterion("PiCriterion.json");
        Assert.assertThat(criterion.type(), is(Criterion.Type.PROTOCOL_INDEPENDENT));
        Collection<PiFieldMatch> piFieldMatches = ((PiCriterion) criterion).fieldMatches();
        for (PiFieldMatch piFieldMatch : piFieldMatches) {
            switch (piFieldMatch.type()) {
                case EXACT:
                    Assert.assertThat(piFieldMatch.fieldId().id(), is("ingress_port"));
                    Assert.assertThat(((PiExactFieldMatch) piFieldMatch).value(), is(
                            copyFrom((byte) 0x10)));
                    break;
                case LPM:
                    Assert.assertThat(piFieldMatch.fieldId().id(), is("src_addr"));
                    Assert.assertThat(((PiLpmFieldMatch) piFieldMatch).value(),
                                      is(copyFrom(0xa010101)));
                    Assert.assertThat(((PiLpmFieldMatch) piFieldMatch).prefixLength(), is(24));
                    break;
                case TERNARY:
                    Assert.assertThat(piFieldMatch.fieldId().id(), is("dst_addr"));
                    Assert.assertThat(((PiTernaryFieldMatch) piFieldMatch).value(),
                                      is(copyFrom(0xa010101)));
                    Assert.assertThat(((PiTernaryFieldMatch) piFieldMatch).mask(),
                                      is(copyFrom(0xfffffff)));
                    break;
                case RANGE:
                    Assert.assertThat(piFieldMatch.fieldId().id(), is("egress_port"));
                    Assert.assertThat(((PiRangeFieldMatch) piFieldMatch).highValue(),
                                      is(copyFrom((byte) 0x20)));
                    Assert.assertThat(((PiRangeFieldMatch) piFieldMatch).lowValue(),
                                      is(copyFrom((byte) 0x10)));
                    break;
                case OPTIONAL:
                    Assert.assertThat(piFieldMatch.fieldId().id(), is("eth_dst"));
                    Assert.assertThat(((PiOptionalFieldMatch) piFieldMatch).value(),
                                      is(copyFrom(new byte[]{0x00, 0x11, 0x22, 0x33, 0x44, 0x55})));
                    break;
                default:
                    Assert.fail();
            }
        }
    }

    /**
     * Reads in a criterion from the given resource and decodes it.
     *
     * @param resourceName resource to use to read the JSON for the rule
     * @return decoded criterion
     * @throws IOException if processing the resource fails
     */
    private Criterion getCriterion(String resourceName) throws IOException {
        InputStream jsonStream = CriterionCodecTest.class.getResourceAsStream(resourceName);
        JsonNode json = context.mapper().readTree(jsonStream);
        MatcherAssert.assertThat(json, notNullValue());
        Criterion criterion = criterionCodec.decode((ObjectNode) json, context);
        Assert.assertThat(criterion, notNullValue());
        return criterion;
    }
}
