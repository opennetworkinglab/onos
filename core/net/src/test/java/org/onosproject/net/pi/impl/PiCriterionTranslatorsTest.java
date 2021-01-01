/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.net.pi.impl;

import org.junit.Test;
import org.onlab.packet.EthType;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip6Address;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.MplsLabel;
import org.onlab.packet.TpPort;
import org.onlab.packet.VlanId;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.criteria.ArpHaCriterion;
import org.onosproject.net.flow.criteria.ArpOpCriterion;
import org.onosproject.net.flow.criteria.ArpPaCriterion;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.criteria.EthCriterion;
import org.onosproject.net.flow.criteria.EthTypeCriterion;
import org.onosproject.net.flow.criteria.IPCriterion;
import org.onosproject.net.flow.criteria.IPDscpCriterion;
import org.onosproject.net.flow.criteria.IPEcnCriterion;
import org.onosproject.net.flow.criteria.IPProtocolCriterion;
import org.onosproject.net.flow.criteria.IPv6ExthdrFlagsCriterion;
import org.onosproject.net.flow.criteria.IPv6FlowLabelCriterion;
import org.onosproject.net.flow.criteria.IPv6NDLinkLayerAddressCriterion;
import org.onosproject.net.flow.criteria.IPv6NDTargetAddressCriterion;
import org.onosproject.net.flow.criteria.IcmpCodeCriterion;
import org.onosproject.net.flow.criteria.IcmpTypeCriterion;
import org.onosproject.net.flow.criteria.Icmpv6CodeCriterion;
import org.onosproject.net.flow.criteria.Icmpv6TypeCriterion;
import org.onosproject.net.flow.criteria.MetadataCriterion;
import org.onosproject.net.flow.criteria.MplsBosCriterion;
import org.onosproject.net.flow.criteria.MplsCriterion;
import org.onosproject.net.flow.criteria.MplsTcCriterion;
import org.onosproject.net.flow.criteria.PbbIsidCriterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.criteria.SctpPortCriterion;
import org.onosproject.net.flow.criteria.TcpFlagsCriterion;
import org.onosproject.net.flow.criteria.TcpPortCriterion;
import org.onosproject.net.flow.criteria.TunnelIdCriterion;
import org.onosproject.net.flow.criteria.UdpPortCriterion;
import org.onosproject.net.flow.criteria.VlanIdCriterion;
import org.onosproject.net.flow.criteria.VlanPcpCriterion;
import org.onosproject.net.pi.model.PiMatchFieldId;
import org.onosproject.net.pi.runtime.PiExactFieldMatch;
import org.onosproject.net.pi.runtime.PiLpmFieldMatch;
import org.onosproject.net.pi.runtime.PiTernaryFieldMatch;

import java.util.Random;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.onosproject.net.pi.impl.CriterionTranslatorHelper.translateCriterion;
import static org.onosproject.net.pi.model.PiMatchType.EXACT;
import static org.onosproject.net.pi.model.PiMatchType.LPM;
import static org.onosproject.net.pi.model.PiMatchType.TERNARY;

/**
 * Tests for CriterionTranslators.
 */
public class PiCriterionTranslatorsTest {

    private Random random = new Random();
    private final PiMatchFieldId fieldId = PiMatchFieldId.of("foo.bar");

    @Test
    public void testEthCriterion() throws Exception {
        MacAddress value1 = MacAddress.valueOf(random.nextLong());
        MacAddress value2 = MacAddress.valueOf(random.nextLong());
        MacAddress mask = MacAddress.valueOf(random.nextLong());
        int bitWidth = value1.toBytes().length * 8;

        EthCriterion criterion = (EthCriterion) Criteria.matchEthDst(value1);
        PiExactFieldMatch exactMatch = (PiExactFieldMatch) translateCriterion(criterion, fieldId, EXACT, bitWidth);

        EthCriterion maskedCriterion = (EthCriterion) Criteria.matchEthDstMasked(value2, mask);
        PiTernaryFieldMatch ternaryMatch = (PiTernaryFieldMatch) translateCriterion(maskedCriterion, fieldId, TERNARY,
                bitWidth);

        assertThat(exactMatch.value().asArray(), is(criterion.mac().toBytes()));
        assertThat(ternaryMatch.value().asArray(), is(maskedCriterion.mac().toBytes()));
        assertThat(ternaryMatch.mask().asArray(), is(maskedCriterion.mask().toBytes()));
    }

    @Test
    public void testEthTypeCriterion() throws Exception {
        EthType ethType = new EthType(random.nextInt());
        int bitWidth = 16;

        EthTypeCriterion criterion = (EthTypeCriterion) Criteria.matchEthType(ethType);

        PiExactFieldMatch exactMatch = (PiExactFieldMatch) translateCriterion(criterion, fieldId, EXACT, bitWidth);

        assertThat(exactMatch.value().asReadOnlyBuffer().getShort(), is(criterion.ethType().toShort()));
    }

    @Test
    public void testIpCriterion() throws Exception {
        IpPrefix prefix1 = IpPrefix.valueOf(random.nextInt(), random.nextInt(32));
        int bitWidth = prefix1.address().toOctets().length * 8;

        IPCriterion criterion = (IPCriterion) Criteria.matchIPDst(prefix1);

        PiLpmFieldMatch lpmMatch = (PiLpmFieldMatch) translateCriterion(criterion, fieldId, LPM, bitWidth);

        assertThat(lpmMatch.value().asArray(), is(criterion.ip().address().toOctets()));
        assertThat(lpmMatch.prefixLength(), is(criterion.ip().prefixLength()));
    }

    @Test
    public void testPortCriterion() throws Exception {
        PortNumber portNumber = PortNumber.portNumber(random.nextLong());
        int bitWidth = 64;

        PortCriterion criterion = (PortCriterion) Criteria.matchInPort(portNumber);

        PiExactFieldMatch exactMatch = (PiExactFieldMatch) translateCriterion(criterion, fieldId, EXACT, bitWidth);

        assertThat(exactMatch.value().asReadOnlyBuffer().getLong(), is(criterion.port().toLong()));
    }

    @Test
    public void testVlanIdCriterion() throws Exception {
        VlanId vlanId = VlanId.vlanId((short) random.nextInt(255));
        int bitWidth = 16;

        VlanIdCriterion criterion = (VlanIdCriterion) Criteria.matchVlanId(vlanId);

        PiExactFieldMatch exactMatch = (PiExactFieldMatch) translateCriterion(criterion, fieldId, EXACT, bitWidth);

        assertThat(exactMatch.value().asReadOnlyBuffer().getShort(), is(criterion.vlanId().toShort()));
    }

    @Test
    public void testUdpPortCriterion() throws Exception {
        TpPort value1 = TpPort.tpPort(random.nextInt(65536));
        TpPort value2 = TpPort.tpPort(random.nextInt(65536));
        TpPort mask = TpPort.tpPort(random.nextInt(65536));
        int bitWidth = 16;

        UdpPortCriterion criterion = (UdpPortCriterion) Criteria.matchUdpDst(value1);
        PiExactFieldMatch exactMatch = (PiExactFieldMatch) translateCriterion(criterion, fieldId, EXACT, bitWidth);

        UdpPortCriterion maskedCriterion = (UdpPortCriterion) Criteria.matchUdpDstMasked(value2, mask);
        PiTernaryFieldMatch ternaryMatch = (PiTernaryFieldMatch) translateCriterion(maskedCriterion, fieldId, TERNARY,
                bitWidth);

        assertThat(exactMatch.value().asReadOnlyBuffer().getShort(), is((short) criterion.udpPort().toInt()));
        assertThat(ternaryMatch.value().asReadOnlyBuffer().getShort(), is((short) maskedCriterion.udpPort().toInt()));
        assertThat(ternaryMatch.mask().asReadOnlyBuffer().getShort(), is((short) maskedCriterion.mask().toInt()));
    }

    @Test
    public void testIPDscpCriterion() throws Exception {
        byte[] ipDscp = new byte[1];
        random.nextBytes(ipDscp);

        IPDscpCriterion criterion = (IPDscpCriterion) Criteria.matchIPDscp(ipDscp[0]);
        int bitWidth = 6;

        PiExactFieldMatch exactMatch = (PiExactFieldMatch) translateCriterion(criterion, fieldId, EXACT, bitWidth);

        assertThat(exactMatch.value().asReadOnlyBuffer().get(), is(criterion.ipDscp()));
    }

    @Test
    public void testIPProtocolCriterion() throws Exception {
        short proto = (short) random.nextInt(256);
        int bitWidth = 16;

        IPProtocolCriterion criterion = (IPProtocolCriterion) Criteria.matchIPProtocol(proto);

        PiExactFieldMatch exactMatch = (PiExactFieldMatch) translateCriterion(criterion, fieldId, EXACT, bitWidth);

        assertThat(exactMatch.value().asReadOnlyBuffer().getShort(), is(criterion.protocol()));
    }

    @Test
    public void testIPv6ExthdrFlagsCriterion() throws Exception {
        int exthdrFlags = random.nextInt();
        int bitWidth = 32;

        IPv6ExthdrFlagsCriterion criterion = (IPv6ExthdrFlagsCriterion) Criteria.matchIPv6ExthdrFlags(exthdrFlags);

        PiExactFieldMatch exactMatch = (PiExactFieldMatch) translateCriterion(criterion, fieldId, EXACT, bitWidth);

        assertThat(exactMatch.value().asReadOnlyBuffer().getInt(), is(criterion.exthdrFlags()));
    }

    @Test
    public void testIPv6FlowLabelCriterion() throws Exception {
        int flowLabel = random.nextInt();
        int bitWidth = 32;

        IPv6FlowLabelCriterion criterion = (IPv6FlowLabelCriterion) Criteria.matchIPv6FlowLabel(flowLabel);

        PiExactFieldMatch exactMatch = (PiExactFieldMatch) translateCriterion(criterion, fieldId, EXACT, bitWidth);

        assertThat(exactMatch.value().asReadOnlyBuffer().getInt(), is(criterion.flowLabel()));
    }

    @Test
    public void testIPv6NDLinkLayerAddressCriterion() throws Exception {
        MacAddress mac = MacAddress.valueOf(random.nextLong());
        int bitWidth = mac.toBytes().length * 8;

        IPv6NDLinkLayerAddressCriterion criterion = (IPv6NDLinkLayerAddressCriterion) Criteria
                .matchIPv6NDSourceLinkLayerAddress(mac);

        PiExactFieldMatch exactMatch = (PiExactFieldMatch) translateCriterion(criterion, fieldId, EXACT, bitWidth);

        assertThat(exactMatch.value().asArray(), is(criterion.mac().toBytes()));
    }

    @Test
    public void testIPv6NDTargetAddressCriterion() throws Exception {
        Ip6Address targetAddress = Ip6Address.valueOf("2001:A304:6101:1::E0:F726:4E58");
        int bitWidth = targetAddress.toOctets().length * 8;

        IPv6NDTargetAddressCriterion criterion = (IPv6NDTargetAddressCriterion) Criteria
                .matchIPv6NDTargetAddress(targetAddress);

        PiExactFieldMatch exactMatch = (PiExactFieldMatch) translateCriterion(criterion, fieldId, EXACT, bitWidth);

        assertThat(exactMatch.value().asArray(), is(criterion.targetAddress().toOctets()));
    }

    @Test
    public void testIcmpCodeCriterion() throws Exception {
        short icmpCode = (short) random.nextInt(256);
        int bitWidth = 16;

        IcmpCodeCriterion criterion = (IcmpCodeCriterion) Criteria.matchIcmpCode(icmpCode);

        PiExactFieldMatch exactMatch = (PiExactFieldMatch) translateCriterion(criterion, fieldId, EXACT, bitWidth);

        assertThat(exactMatch.value().asReadOnlyBuffer().getShort(), is(criterion.icmpCode()));
    }

    @Test
    public void testIcmpTypeCriterion() throws Exception {
        short icmpType = (short) random.nextInt(256);
        int bitWidth = 16;

        IcmpTypeCriterion criterion = (IcmpTypeCriterion) Criteria.matchIcmpType(icmpType);

        PiExactFieldMatch exactMatch = (PiExactFieldMatch) translateCriterion(criterion, fieldId, EXACT, bitWidth);

        assertThat(exactMatch.value().asReadOnlyBuffer().getShort(), is(criterion.icmpType()));
    }

    @Test
    public void testIcmpv6CodeCriterion() throws Exception {
        short icmpv6Code = (short) random.nextInt(256);
        int bitWidth = 16;

        Icmpv6CodeCriterion criterion = (Icmpv6CodeCriterion) Criteria.matchIcmpv6Code(icmpv6Code);

        PiExactFieldMatch exactMatch = (PiExactFieldMatch) translateCriterion(criterion, fieldId, EXACT, bitWidth);

        assertThat(exactMatch.value().asReadOnlyBuffer().getShort(), is(criterion.icmpv6Code()));
    }

    @Test
    public void testIcmpv6TypeCriterion() throws Exception {
        short icmpv6Type = (short) random.nextInt(256);
        int bitWidth = 16;

        Icmpv6TypeCriterion criterion = (Icmpv6TypeCriterion) Criteria.matchIcmpv6Type(icmpv6Type);

        PiExactFieldMatch exactMatch = (PiExactFieldMatch) translateCriterion(criterion, fieldId, EXACT, bitWidth);

        assertThat(exactMatch.value().asReadOnlyBuffer().getShort(), is(criterion.icmpv6Type()));
    }

    @Test
    public void testMetadataCriterion() throws Exception {
        long metadata = random.nextLong();
        int bitWidth = 64;

        MetadataCriterion criterion = (MetadataCriterion) Criteria.matchMetadata(metadata);

        PiExactFieldMatch exactMatch = (PiExactFieldMatch) translateCriterion(criterion, fieldId, EXACT, bitWidth);

        assertThat(exactMatch.value().asReadOnlyBuffer().getLong(), is(criterion.metadata()));
    }

    @Test
    public void testMplsBosCriterion() throws Exception {
        boolean mplsBos = random.nextBoolean();
        int bitWidth = 32;
        int bMplsBos = 0;

        MplsBosCriterion criterion = (MplsBosCriterion) Criteria.matchMplsBos(mplsBos);

        PiExactFieldMatch exactMatch = (PiExactFieldMatch) translateCriterion(criterion, fieldId, EXACT, bitWidth);

        bMplsBos = (criterion.mplsBos()) ? 1 : 0;

        assertThat(exactMatch.value().asReadOnlyBuffer().getInt(), is(bMplsBos));
    }

    @Test
    public void testMplsCriterion() throws Exception {
        MplsLabel mplsLabel = MplsLabel.mplsLabel(random.nextInt(1 << 20));
        int bitWidth = 32;

        MplsCriterion criterion = (MplsCriterion) Criteria.matchMplsLabel(mplsLabel);

        PiExactFieldMatch exactMatch = (PiExactFieldMatch) translateCriterion(criterion, fieldId, EXACT, bitWidth);

        assertThat(exactMatch.value().asReadOnlyBuffer().getInt(), is(criterion.label().toInt()));
    }

    @Test
    public void testMplsTcCriterion() throws Exception {
        byte[] mplsTc = new byte[1];
        random.nextBytes(mplsTc);

        int bitWidth = 16;

        MplsTcCriterion criterion = (MplsTcCriterion) Criteria.matchMplsTc(mplsTc[0]);

        PiExactFieldMatch exactMatch = (PiExactFieldMatch) translateCriterion(criterion, fieldId, EXACT, bitWidth);

        assertThat(exactMatch.value().asReadOnlyBuffer().get(1), is(criterion.tc()));
    }

    @Test
    public void testPbbIsidCriterion() throws Exception {
        int pbbIsid = random.nextInt();
        int bitWidth = 32;

        PbbIsidCriterion criterion = (PbbIsidCriterion) Criteria.matchPbbIsid(pbbIsid);

        PiExactFieldMatch exactMatch = (PiExactFieldMatch) translateCriterion(criterion, fieldId, EXACT, bitWidth);

        assertThat(exactMatch.value().asReadOnlyBuffer().getInt(), is(criterion.pbbIsid()));
    }

    @Test
    public void testSctpPortCriterion() throws Exception {
        TpPort value1 = TpPort.tpPort(random.nextInt(1 << 16));
        TpPort value2 = TpPort.tpPort(random.nextInt(1 << 16));
        TpPort mask = TpPort.tpPort(random.nextInt(1 << 16));

        int bitWidth = 16;

        SctpPortCriterion criterion = (SctpPortCriterion) Criteria.matchSctpDst(value1);
        PiExactFieldMatch exactMatch = (PiExactFieldMatch) translateCriterion(criterion, fieldId, EXACT, bitWidth);

        SctpPortCriterion maskedCriterion = (SctpPortCriterion) Criteria.matchSctpDstMasked(value2, mask);
        PiTernaryFieldMatch ternaryMatch = (PiTernaryFieldMatch) translateCriterion(maskedCriterion, fieldId, TERNARY,
                bitWidth);

        assertThat(exactMatch.value().asReadOnlyBuffer().getShort(), is((short) criterion.sctpPort().toInt()));
        assertThat(ternaryMatch.value().asReadOnlyBuffer().getShort(), is((short) maskedCriterion.sctpPort().toInt()));
        assertThat(ternaryMatch.mask().asReadOnlyBuffer().getShort(), is((short) maskedCriterion.mask().toInt()));
    }

    @Test
    public void testTcpFlagsCriterion() throws Exception {
        int pbbIsid = random.nextInt(1 << 12);
        int bitWidth = 12;

        TcpFlagsCriterion criterion = (TcpFlagsCriterion) Criteria.matchTcpFlags(pbbIsid);

        PiExactFieldMatch exactMatch = (PiExactFieldMatch) translateCriterion(criterion, fieldId, EXACT, bitWidth);

        assertThat(exactMatch.value().asReadOnlyBuffer().getShort(), is((short) criterion.flags()));
    }

    @Test
    public void testTcpPortCriterion() throws Exception {
        TpPort value1 = TpPort.tpPort(random.nextInt(1 << 16));
        TpPort value2 = TpPort.tpPort(random.nextInt(1 << 16));
        TpPort mask = TpPort.tpPort(random.nextInt(1 << 16));

        int bitWidth = 16;

        TcpPortCriterion criterion = (TcpPortCriterion) Criteria.matchTcpDst(value1);
        PiExactFieldMatch exactMatch = (PiExactFieldMatch) translateCriterion(criterion, fieldId, EXACT, bitWidth);

        TcpPortCriterion maskedCriterion = (TcpPortCriterion) Criteria.matchTcpDstMasked(value2, mask);
        PiTernaryFieldMatch ternaryMatch = (PiTernaryFieldMatch) translateCriterion(maskedCriterion, fieldId, TERNARY,
                bitWidth);

        assertThat(exactMatch.value().asReadOnlyBuffer().getShort(), is((short) criterion.tcpPort().toInt()));
        assertThat(ternaryMatch.value().asReadOnlyBuffer().getShort(), is((short) maskedCriterion.tcpPort().toInt()));
        assertThat(ternaryMatch.mask().asReadOnlyBuffer().getShort(), is((short) maskedCriterion.mask().toInt()));
    }

    @Test
    public void testTunnelIdCriterion() throws Exception {
        long tunnelId = random.nextLong();
        int bitWidth = 64;

        TunnelIdCriterion criterion = (TunnelIdCriterion) Criteria.matchTunnelId(tunnelId);

        PiExactFieldMatch exactMatch = (PiExactFieldMatch) translateCriterion(criterion, fieldId, EXACT, bitWidth);

        assertThat(exactMatch.value().asReadOnlyBuffer().getLong(), is(criterion.tunnelId()));
    }


    @Test
    public void testVlanPcpCriterion() throws Exception {
        byte[] vlanPcp = new byte[1];
        random.nextBytes(vlanPcp);

        int bitWidth = 3;

        VlanPcpCriterion criterion = (VlanPcpCriterion) Criteria.matchVlanPcp(vlanPcp[0]);

        PiExactFieldMatch exactMatch = (PiExactFieldMatch) translateCriterion(criterion, fieldId, EXACT, bitWidth);

        assertThat(exactMatch.value().asReadOnlyBuffer().get(), is(criterion.priority()));
    }

    @Test
    public void testArpHaCriterionn() throws Exception {
        MacAddress mac = MacAddress.valueOf(random.nextLong());
        int bitWidth = mac.toBytes().length * 8;

        ArpHaCriterion criterion = (ArpHaCriterion) Criteria.matchArpTha(mac);

        PiExactFieldMatch exactMatch = (PiExactFieldMatch) translateCriterion(criterion, fieldId, EXACT, bitWidth);

        assertThat(exactMatch.value().asArray(), is(criterion.mac().toBytes()));
    }

    @Test
    public void testArpOpCriterion() throws Exception {
        int arpOp = random.nextInt();
        int bitWidth = 32;

        ArpOpCriterion criterion = (ArpOpCriterion) Criteria.matchArpOp(arpOp);

        PiExactFieldMatch exactMatch = (PiExactFieldMatch) translateCriterion(criterion, fieldId, EXACT, bitWidth);

        assertThat(exactMatch.value().asReadOnlyBuffer().getInt(), is(criterion.arpOp()));
    }

    @Test
    public void testArpPaCriterion() throws Exception {
        Ip4Address ip = Ip4Address.valueOf(random.nextInt());
        int bitWidth = ip.toOctets().length * 8;

        ArpPaCriterion criterion = (ArpPaCriterion) Criteria.matchArpTpa(ip);

        PiExactFieldMatch exactMatch = (PiExactFieldMatch) translateCriterion(criterion, fieldId, EXACT, bitWidth);

        assertThat(exactMatch.value().asReadOnlyBuffer().getInt(), is(criterion.ip().toInt()));
    }

    @Test
    public void testIPEcnCriterion() throws Exception {
        byte[] ipEcn = new byte[1];
        random.nextBytes(ipEcn);

        int bitWidth = 2;

        IPEcnCriterion criterion = (IPEcnCriterion) Criteria.matchIPEcn(ipEcn[0]);

        PiExactFieldMatch exactMatch = (PiExactFieldMatch) translateCriterion(criterion, fieldId, EXACT, bitWidth);

        assertThat(exactMatch.value().asReadOnlyBuffer().get(), is(criterion.ipEcn()));
    }

    @Test
    public void testLpmToTernaryTranslation() throws Exception {
        IpPrefix ipPrefix = IpPrefix.valueOf("10.0.0.1/23");
        int bitWidth = ipPrefix.address().toOctets().length * Byte.SIZE;

        IPCriterion criterion = (IPCriterion) Criteria.matchIPDst(ipPrefix);
        PiTernaryFieldMatch ternaryMatch =
                (PiTernaryFieldMatch) translateCriterion(criterion, fieldId, TERNARY, bitWidth);

        ImmutableByteSequence expectedMask = ImmutableByteSequence.prefixOnes(Integer.BYTES, 23);
        ImmutableByteSequence expectedValue = ImmutableByteSequence.copyFrom(ipPrefix.address().toOctets());

        assertThat(ternaryMatch.mask(), is(expectedMask));
        assertThat(ternaryMatch.value(), is(expectedValue));
    }

    @Test
    public void testTernaryToLpmTranslation() throws Exception {
        EthCriterion criterion =
                (EthCriterion) Criteria.matchEthDstMasked(MacAddress.ONOS,
                                                          MacAddress.IPV4_MULTICAST_MASK);

        PiLpmFieldMatch lpmMatch =
                (PiLpmFieldMatch) translateCriterion(criterion, fieldId, LPM,
                                                     MacAddress.MAC_ADDRESS_LENGTH * Byte.SIZE);
        ImmutableByteSequence expectedValue = ImmutableByteSequence.copyFrom(MacAddress.ONOS.toBytes());

        assertThat(lpmMatch.prefixLength(), is(25));
        assertThat(lpmMatch.value(), is(expectedValue));
    }
}
