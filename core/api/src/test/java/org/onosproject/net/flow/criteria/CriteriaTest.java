/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.net.flow.criteria;

import org.junit.Test;
import org.onosproject.net.PortNumber;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.Ip6Address;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;

import com.google.common.testing.EqualsTester;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;
import static org.onlab.junit.UtilityClassChecker.assertThatClassIsUtility;
import static org.onosproject.net.PortNumber.portNumber;

/**
 * Unit tests for the Criteria class and its subclasses.
 */
public class CriteriaTest {

    final PortNumber port1 = portNumber(1);
    final PortNumber port2 = portNumber(2);

    Criterion matchInPort1 = Criteria.matchInPort(port1);
    Criterion sameAsMatchInPort1 = Criteria.matchInPort(port1);
    Criterion matchInPort2 = Criteria.matchInPort(port2);

    Criterion matchInPhyPort1 = Criteria.matchInPhyPort(port1);
    Criterion sameAsMatchInPhyPort1 = Criteria.matchInPhyPort(port1);
    Criterion matchInPhyPort2 = Criteria.matchInPhyPort(port2);

    long metadata1 = 1;
    long metadata2 = 2;
    Criterion matchMetadata1 = Criteria.matchMetadata(metadata1);
    Criterion sameAsMatchMetadata1 = Criteria.matchMetadata(metadata1);
    Criterion matchMetadata2 = Criteria.matchMetadata(metadata2);

    private static final String MAC1 = "00:00:00:00:00:01";
    private static final String MAC2 = "00:00:00:00:00:02";
    private MacAddress mac1 = MacAddress.valueOf(MAC1);
    private MacAddress mac2 = MacAddress.valueOf(MAC2);
    Criterion matchEth1 = Criteria.matchEthSrc(mac1);
    Criterion sameAsMatchEth1 = Criteria.matchEthSrc(mac1);
    Criterion matchEth2 = Criteria.matchEthDst(mac2);

    short ethType1 = 1;
    short ethType2 = 2;
    Criterion matchEthType1 = Criteria.matchEthType(ethType1);
    Criterion sameAsMatchEthType1 = Criteria.matchEthType(ethType1);
    Criterion matchEthType2 = Criteria.matchEthType(ethType2);

    short vlan1 = 1;
    short vlan2 = 2;
    VlanId vlanId1 = VlanId.vlanId(vlan1);
    VlanId vlanId2 = VlanId.vlanId(vlan2);
    Criterion matchVlanId1 = Criteria.matchVlanId(vlanId1);
    Criterion sameAsMatchVlanId1 = Criteria.matchVlanId(vlanId1);
    Criterion matchVlanId2 = Criteria.matchVlanId(vlanId2);

    byte vlanPcp1 = 1;
    byte vlanPcp2 = 2;
    Criterion matchVlanPcp1 = Criteria.matchVlanPcp(vlanPcp1);
    Criterion sameAsMatchVlanPcp1 = Criteria.matchVlanPcp(vlanPcp1);
    Criterion matchVlanPcp2 = Criteria.matchVlanPcp(vlanPcp2);

    byte ipDscp1 = 1;
    byte ipDscp2 = 2;
    Criterion matchIpDscp1 = Criteria.matchIPDscp(ipDscp1);
    Criterion sameAsMatchIpDscp1 = Criteria.matchIPDscp(ipDscp1);
    Criterion matchIpDscp2 = Criteria.matchIPDscp(ipDscp2);

    byte ipEcn1 = 1;
    byte ipEcn2 = 2;
    Criterion matchIpEcn1 = Criteria.matchIPEcn(ipEcn1);
    Criterion sameAsMatchIpEcn1 = Criteria.matchIPEcn(ipEcn1);
    Criterion matchIpEcn2 = Criteria.matchIPEcn(ipEcn2);

    byte protocol1 = 1;
    byte protocol2 = 2;
    Criterion matchIpProtocol1 = Criteria.matchIPProtocol(protocol1);
    Criterion sameAsMatchIpProtocol1 = Criteria.matchIPProtocol(protocol1);
    Criterion matchIpProtocol2 = Criteria.matchIPProtocol(protocol2);

    private static final String IP1 = "1.2.3.4/24";
    private static final String IP2 = "5.6.7.8/24";
    private static final String IPV61 = "fe80::1/64";
    private static final String IPV62 = "fc80::2/64";
    private IpPrefix ip1 = IpPrefix.valueOf(IP1);
    private IpPrefix ip2 = IpPrefix.valueOf(IP2);
    private IpPrefix ipv61 = IpPrefix.valueOf(IPV61);
    private IpPrefix ipv62 = IpPrefix.valueOf(IPV62);
    Criterion matchIp1 = Criteria.matchIPSrc(ip1);
    Criterion sameAsMatchIp1 = Criteria.matchIPSrc(ip1);
    Criterion matchIp2 = Criteria.matchIPSrc(ip2);
    Criterion matchIpv61 = Criteria.matchIPSrc(ipv61);
    Criterion sameAsMatchIpv61 = Criteria.matchIPSrc(ipv61);
    Criterion matchIpv62 = Criteria.matchIPSrc(ipv62);

    Criterion matchTcpPort1 = Criteria.matchTcpSrc((short) 1);
    Criterion sameAsMatchTcpPort1 = Criteria.matchTcpSrc((short) 1);
    Criterion matchTcpPort2 = Criteria.matchTcpDst((short) 2);

    Criterion matchUdpPort1 = Criteria.matchUdpSrc((short) 1);
    Criterion sameAsMatchUdpPort1 = Criteria.matchUdpSrc((short) 1);
    Criterion matchUdpPort2 = Criteria.matchUdpDst((short) 2);

    Criterion matchSctpPort1 = Criteria.matchSctpSrc((short) 1);
    Criterion sameAsMatchSctpPort1 = Criteria.matchSctpSrc((short) 1);
    Criterion matchSctpPort2 = Criteria.matchSctpDst((short) 2);

    byte icmpType1 = 1;
    byte icmpType2 = 2;
    Criterion matchIcmpType1 = Criteria.matchIcmpType(icmpType1);
    Criterion sameAsMatchIcmpType1 = Criteria.matchIcmpType(icmpType1);
    Criterion matchIcmpType2 = Criteria.matchIcmpType(icmpType2);

    byte icmpCode1 = 1;
    byte icmpCode2 = 2;
    Criterion matchIcmpCode1 = Criteria.matchIcmpCode(icmpCode1);
    Criterion sameAsMatchIcmpCode1 = Criteria.matchIcmpCode(icmpCode1);
    Criterion matchIcmpCode2 = Criteria.matchIcmpCode(icmpCode2);

    int flowLabel1 = 1;
    int flowLabel2 = 2;
    Criterion matchFlowLabel1 = Criteria.matchIPv6FlowLabel(flowLabel1);
    Criterion sameAsMatchFlowLabel1 = Criteria.matchIPv6FlowLabel(flowLabel1);
    Criterion matchFlowLabel2 = Criteria.matchIPv6FlowLabel(flowLabel2);

    byte icmpv6Type1 = 1;
    byte icmpv6Type2 = 2;
    Criterion matchIcmpv6Type1 = Criteria.matchIcmpv6Type(icmpv6Type1);
    Criterion sameAsMatchIcmpv6Type1 = Criteria.matchIcmpv6Type(icmpv6Type1);
    Criterion matchIcmpv6Type2 = Criteria.matchIcmpv6Type(icmpv6Type2);

    byte icmpv6Code1 = 1;
    byte icmpv6Code2 = 2;
    Criterion matchIcmpv6Code1 = Criteria.matchIcmpv6Code(icmpv6Code1);
    Criterion sameAsMatchIcmpv6Code1 = Criteria.matchIcmpv6Code(icmpv6Code1);
    Criterion matchIcmpv6Code2 = Criteria.matchIcmpv6Code(icmpv6Code2);

    private static final String IPV6_ADDR1 = "fe80::1";
    private static final String IPV6_ADDR2 = "fe80::2";
    private Ip6Address ip6TargetAddress1 = Ip6Address.valueOf(IPV6_ADDR1);
    private Ip6Address ip6TargetAddress2 = Ip6Address.valueOf(IPV6_ADDR2);
    Criterion matchIpv6TargetAddr1 =
        Criteria.matchIPv6NDTargetAddress(ip6TargetAddress1);
    Criterion sameAsMatchIpv6TargetAddr1 =
        Criteria.matchIPv6NDTargetAddress(ip6TargetAddress1);
    Criterion matchIpv6TargetAddr2 =
        Criteria.matchIPv6NDTargetAddress(ip6TargetAddress2);

    private static final String LL_MAC1 = "00:00:00:00:00:01";
    private static final String LL_MAC2 = "00:00:00:00:00:02";
    private MacAddress llMac1 = MacAddress.valueOf(LL_MAC1);
    private MacAddress llMac2 = MacAddress.valueOf(LL_MAC2);
    Criterion matchSrcLlAddr1 =
        Criteria.matchIPv6NDSourceLinkLayerAddress(llMac1);
    Criterion sameAsMatchSrcLlAddr1 =
        Criteria.matchIPv6NDSourceLinkLayerAddress(llMac1);
    Criterion matchSrcLlAddr2 =
        Criteria.matchIPv6NDSourceLinkLayerAddress(llMac2);
    Criterion matchTargetLlAddr1 =
        Criteria.matchIPv6NDTargetLinkLayerAddress(llMac1);
    Criterion sameAsMatchTargetLlAddr1 =
        Criteria.matchIPv6NDTargetLinkLayerAddress(llMac1);
    Criterion matchTargetLlAddr2 =
        Criteria.matchIPv6NDTargetLinkLayerAddress(llMac2);

    int mpls1 = 1;
    int mpls2 = 2;
    Criterion matchMpls1 = Criteria.matchMplsLabel(mpls1);
    Criterion sameAsMatchMpls1 = Criteria.matchMplsLabel(mpls1);
    Criterion matchMpls2 = Criteria.matchMplsLabel(mpls2);

    short lambda1 = 1;
    short lambda2 = 2;
    Criterion matchLambda1 = Criteria.matchLambda(lambda1);
    Criterion sameAsMatchLambda1 = Criteria.matchLambda(lambda1);
    Criterion matchLambda2 = Criteria.matchLambda(lambda2);

    short signalLambda1 = 1;
    short signalLambda2 = 2;
    Criterion matchSignalLambda1 = Criteria.matchOpticalSignalType(signalLambda1);
    Criterion sameAsMatchSignalLambda1 = Criteria.matchOpticalSignalType(signalLambda1);
    Criterion matchSignalLambda2 = Criteria.matchOpticalSignalType(signalLambda2);

    /**
     * Checks that a Criterion object has the proper type, and then converts
     * it to the proper type.
     *
     * @param criterion Criterion object to convert
     * @param type Enumerated type value for the Criterion class
     * @param clazz Desired Criterion class
     * @param <T> The type the caller wants returned
     * @return converted object
     */
    @SuppressWarnings("unchecked")
    private <T> T checkAndConvert(Criterion criterion, Criterion.Type type, Class clazz) {
        assertThat(criterion, is(notNullValue()));
        assertThat(criterion.type(), is(equalTo(type)));
        assertThat(criterion, instanceOf(clazz));
        return (T) criterion;
    }

    /**
     * Check that the Criteria class is a valid utility class.
     */
    @Test
    public void testCriteriaUtility() {
        assertThatClassIsUtility(Criteria.class);
    }

    /**
     * Check that the Criteria implementations are immutable.
     */
    @Test
    public void testCriteriaImmutability() {
        assertThatClassIsImmutable(Criteria.PortCriterion.class);
        assertThatClassIsImmutable(Criteria.MetadataCriterion.class);
        assertThatClassIsImmutable(Criteria.EthCriterion.class);
        assertThatClassIsImmutable(Criteria.EthTypeCriterion.class);
        assertThatClassIsImmutable(Criteria.VlanIdCriterion.class);
        assertThatClassIsImmutable(Criteria.VlanPcpCriterion.class);
        assertThatClassIsImmutable(Criteria.IPDscpCriterion.class);
        assertThatClassIsImmutable(Criteria.IPEcnCriterion.class);
        assertThatClassIsImmutable(Criteria.IPProtocolCriterion.class);
        assertThatClassIsImmutable(Criteria.IPCriterion.class);
        assertThatClassIsImmutable(Criteria.TcpPortCriterion.class);
        assertThatClassIsImmutable(Criteria.UdpPortCriterion.class);
        assertThatClassIsImmutable(Criteria.SctpPortCriterion.class);
        assertThatClassIsImmutable(Criteria.IcmpTypeCriterion.class);
        assertThatClassIsImmutable(Criteria.IcmpCodeCriterion.class);
        assertThatClassIsImmutable(Criteria.IPv6FlowLabelCriterion.class);
        assertThatClassIsImmutable(Criteria.Icmpv6TypeCriterion.class);
        assertThatClassIsImmutable(Criteria.Icmpv6CodeCriterion.class);
        assertThatClassIsImmutable(Criteria.IPv6NDTargetAddressCriterion.class);
        assertThatClassIsImmutable(Criteria.IPv6NDLinkLayerAddressCriterion.class);
        assertThatClassIsImmutable(Criteria.MplsCriterion.class);
        assertThatClassIsImmutable(Criteria.LambdaCriterion.class);
        assertThatClassIsImmutable(Criteria.OpticalSignalTypeCriterion.class);
    }

    // PortCriterion class

    /**
     * Test the matchInPort method.
     */
    @Test
    public void testMatchInPortMethod() {
        PortNumber p1 = portNumber(1);
        Criterion matchInPort = Criteria.matchInPort(p1);
        Criteria.PortCriterion portCriterion =
                checkAndConvert(matchInPort,
                                Criterion.Type.IN_PORT,
                                Criteria.PortCriterion.class);
        assertThat(portCriterion.port(), is(equalTo(p1)));
    }

    /**
     * Test the matchInPhyPort method.
     */
    @Test
    public void testMatchInPhyPortMethod() {
        PortNumber p1 = portNumber(1);
        Criterion matchInPhyPort = Criteria.matchInPhyPort(p1);
        Criteria.PortCriterion portCriterion =
                checkAndConvert(matchInPhyPort,
                                Criterion.Type.IN_PHY_PORT,
                                Criteria.PortCriterion.class);
        assertThat(portCriterion.port(), is(equalTo(p1)));
    }

    /**
     * Test the equals() method of the PortCriterion class.
     */
    @Test
    public void testPortCriterionEquals() {
        new EqualsTester()
                .addEqualityGroup(matchInPort1, sameAsMatchInPort1)
                .addEqualityGroup(matchInPort2)
                .testEquals();

        new EqualsTester()
                .addEqualityGroup(matchInPhyPort1, sameAsMatchInPhyPort1)
                .addEqualityGroup(matchInPhyPort2)
                .testEquals();
    }

    // MetadataCriterion class

    /**
     * Test the matchMetadata method.
     */
    @Test
    public void testMatchMetadataMethod() {
        Long metadata = 12L;
        Criterion matchMetadata = Criteria.matchMetadata(metadata);
        Criteria.MetadataCriterion metadataCriterion =
                checkAndConvert(matchMetadata,
                                Criterion.Type.METADATA,
                                Criteria.MetadataCriterion.class);
        assertThat(metadataCriterion.metadata(), is(equalTo(metadata)));
    }

    /**
     * Test the equals() method of the MetadataCriterion class.
     */
    @Test
    public void testMetadataCriterionEquals() {
        new EqualsTester()
                .addEqualityGroup(matchMetadata1, sameAsMatchMetadata1)
                .addEqualityGroup(matchMetadata2)
                .testEquals();
    }

    // EthCriterion class

    /**
     * Test the matchEthDst method.
     */
    @Test
    public void testMatchEthDstMethod() {
        Criterion matchEthDst = Criteria.matchEthDst(mac1);
        Criteria.EthCriterion ethCriterion =
                checkAndConvert(matchEthDst,
                        Criterion.Type.ETH_DST,
                        Criteria.EthCriterion.class);
        assertThat(ethCriterion.mac(), is(equalTo(mac1)));
    }

    /**
     * Test the matchEthSrc method.
     */
    @Test
    public void testMatchEthSrcMethod() {
        Criterion matchEthSrc = Criteria.matchEthSrc(mac1);
        Criteria.EthCriterion ethCriterion =
                checkAndConvert(matchEthSrc,
                                Criterion.Type.ETH_SRC,
                                Criteria.EthCriterion.class);
        assertThat(ethCriterion.mac(), is(mac1));
    }

    /**
     * Test the equals() method of the EthCriterion class.
     */
    @Test
    public void testEthCriterionEquals() {
        new EqualsTester()
                .addEqualityGroup(matchEth1, sameAsMatchEth1)
                .addEqualityGroup(matchEth2)
                .testEquals();
    }

    // EthTypeCriterion class

    /**
     * Test the matchEthType method.
     */
    @Test
    public void testMatchEthTypeMethod() {
        Short ethType = 12;
        Criterion matchEthType = Criteria.matchEthType(ethType);
        Criteria.EthTypeCriterion ethTypeCriterion =
                checkAndConvert(matchEthType,
                                Criterion.Type.ETH_TYPE,
                                Criteria.EthTypeCriterion.class);
        assertThat(ethTypeCriterion.ethType(), is(equalTo(ethType)));
    }

    /**
     * Test the equals() method of the EthTypeCriterion class.
     */
    @Test
    public void testEthTypeCriterionEquals() {
        new EqualsTester()
                .addEqualityGroup(matchEthType1, sameAsMatchEthType1)
                .addEqualityGroup(matchEthType2)
                .testEquals();
    }

    // VlanIdCriterion class

    /**
     * Test the matchVlanId method.
     */
    @Test
    public void testMatchVlanIdMethod() {
        Criterion matchVlanId = Criteria.matchVlanId(vlanId1);
        Criteria.VlanIdCriterion vlanIdCriterion =
                checkAndConvert(matchVlanId,
                        Criterion.Type.VLAN_VID,
                        Criteria.VlanIdCriterion.class);
        assertThat(vlanIdCriterion.vlanId(), is(equalTo(vlanId1)));
    }

    /**
     * Test the equals() method of the VlanIdCriterion class.
     */
    @Test
    public void testVlanIdCriterionEquals() {
        new EqualsTester()
                .addEqualityGroup(matchVlanId1, sameAsMatchVlanId1)
                .addEqualityGroup(matchVlanId2)
                .testEquals();
    }

    // VlanPcpCriterion class

    /**
     * Test the matchVlanPcp method.
     */
    @Test
    public void testMatchVlanPcpMethod() {
        Criterion matchVlanPcp = Criteria.matchVlanPcp(vlanPcp1);
        Criteria.VlanPcpCriterion vlanPcpCriterion =
                checkAndConvert(matchVlanPcp,
                        Criterion.Type.VLAN_PCP,
                        Criteria.VlanPcpCriterion.class);
        assertThat(vlanPcpCriterion.priority(), is(equalTo(vlanPcp1)));
    }

    /**
     * Test the equals() method of the VlanPcpCriterion class.
     */
    @Test
    public void testVlanPcpCriterionEquals() {
        new EqualsTester()
                .addEqualityGroup(matchVlanPcp1, sameAsMatchVlanPcp1)
                .addEqualityGroup(matchVlanPcp2)
                .testEquals();
    }

    // IPDscpCriterion class

    /**
     * Test the matchIPDscp method.
     */
    @Test
    public void testMatchIPDscpMethod() {
        Criterion matchIPDscp = Criteria.matchIPDscp(ipDscp1);
        Criteria.IPDscpCriterion ipDscpCriterion =
                checkAndConvert(matchIPDscp,
                        Criterion.Type.IP_DSCP,
                        Criteria.IPDscpCriterion.class);
        assertThat(ipDscpCriterion.ipDscp(), is(equalTo(ipDscp1)));
    }

    /**
     * Test the equals() method of the IPDscpCriterion class.
     */
    @Test
    public void testIPDscpCriterionEquals() {
        new EqualsTester()
                .addEqualityGroup(matchIpDscp1, sameAsMatchIpDscp1)
                .addEqualityGroup(matchIpDscp2)
                .testEquals();
    }

    // IPEcnCriterion class

    /**
     * Test the matchIPEcn method.
     */
    @Test
    public void testMatchIPEcnMethod() {
        Criterion matchIPEcn = Criteria.matchIPEcn(ipEcn1);
        Criteria.IPEcnCriterion ipEcnCriterion =
                checkAndConvert(matchIPEcn,
                        Criterion.Type.IP_ECN,
                        Criteria.IPEcnCriterion.class);
        assertThat(ipEcnCriterion.ipEcn(), is(equalTo(ipEcn1)));
    }

    /**
     * Test the equals() method of the IPEcnCriterion class.
     */
    @Test
    public void testIPEcnCriterionEquals() {
        new EqualsTester()
                .addEqualityGroup(matchIpEcn1, sameAsMatchIpEcn1)
                .addEqualityGroup(matchIpEcn2)
                .testEquals();
    }

    // IpProtocolCriterion class

    /**
     * Test the matchIpProtocol method.
     */
    @Test
    public void testMatchIpProtocolMethod() {
        Criterion matchIPProtocol = Criteria.matchIPProtocol(protocol1);
        Criteria.IPProtocolCriterion ipProtocolCriterion =
                checkAndConvert(matchIPProtocol,
                        Criterion.Type.IP_PROTO,
                        Criteria.IPProtocolCriterion.class);
        assertThat(ipProtocolCriterion.protocol(), is(equalTo(protocol1)));
    }

    /**
     * Test the equals() method of the IpProtocolCriterion class.
     */
    @Test
    public void testIpProtocolCriterionEquals() {
        new EqualsTester()
                .addEqualityGroup(matchIpProtocol1, sameAsMatchIpProtocol1)
                .addEqualityGroup(matchIpProtocol2)
                .testEquals();
    }

    // IPCriterion class

    /**
     * Test the matchIPSrc method: IPv4.
     */
    @Test
    public void testMatchIPSrcMethod() {
        Criterion matchIpSrc = Criteria.matchIPSrc(ip1);
        Criteria.IPCriterion ipCriterion =
                checkAndConvert(matchIpSrc,
                                Criterion.Type.IPV4_SRC,
                                Criteria.IPCriterion.class);
        assertThat(ipCriterion.ip(), is(ip1));
    }

    /**
     * Test the matchIPDst method: IPv4.
     */
    @Test
    public void testMatchIPDstMethod() {
        Criterion matchIPDst = Criteria.matchIPDst(ip1);
        Criteria.IPCriterion ipCriterion =
                checkAndConvert(matchIPDst,
                        Criterion.Type.IPV4_DST,
                        Criteria.IPCriterion.class);
        assertThat(ipCriterion.ip(), is(equalTo(ip1)));
    }

    /**
     * Test the matchIPSrc method: IPv6.
     */
    @Test
    public void testMatchIPv6SrcMethod() {
        Criterion matchIpv6Src = Criteria.matchIPv6Src(ipv61);
        Criteria.IPCriterion ipCriterion =
                checkAndConvert(matchIpv6Src,
                        Criterion.Type.IPV6_SRC,
                        Criteria.IPCriterion.class);
        assertThat(ipCriterion.ip(), is(ipv61));
    }

    /**
     * Test the matchIPDst method: IPv6.
     */
    @Test
    public void testMatchIPv6DstMethod() {
        Criterion matchIPv6Dst = Criteria.matchIPv6Dst(ipv61);
        Criteria.IPCriterion ipCriterion =
                checkAndConvert(matchIPv6Dst,
                        Criterion.Type.IPV6_DST,
                        Criteria.IPCriterion.class);
        assertThat(ipCriterion.ip(), is(equalTo(ipv61)));
    }

    /**
     * Test the equals() method of the IpCriterion class.
     */
    @Test
    public void testIPCriterionEquals() {
        new EqualsTester()
                .addEqualityGroup(matchIp1, sameAsMatchIp1)
                .addEqualityGroup(matchIp2)
                .testEquals();

        new EqualsTester()
                .addEqualityGroup(matchIpv61, sameAsMatchIpv61)
                .addEqualityGroup(matchIpv62)
                .testEquals();
    }

    // TcpPortCriterion class

    /**
     * Test the matchTcpSrc method.
     */
    @Test
    public void testMatchTcpSrcMethod() {
        Criterion matchTcpSrc = Criteria.matchTcpSrc((short) 1);
        Criteria.TcpPortCriterion tcpPortCriterion =
                checkAndConvert(matchTcpSrc,
                                Criterion.Type.TCP_SRC,
                                Criteria.TcpPortCriterion.class);
        assertThat(tcpPortCriterion.tcpPort(), is(equalTo((short) 1)));
    }

    /**
     * Test the matchTcpDst method.
     */
    @Test
    public void testMatchTcpDstMethod() {
        Criterion matchTcpDst = Criteria.matchTcpDst((short) 1);
        Criteria.TcpPortCriterion tcpPortCriterion =
                checkAndConvert(matchTcpDst,
                        Criterion.Type.TCP_DST,
                        Criteria.TcpPortCriterion.class);
        assertThat(tcpPortCriterion.tcpPort(), is(equalTo((short) 1)));
    }

    /**
     * Test the equals() method of the TcpPortCriterion class.
     */
    @Test
    public void testTcpPortCriterionEquals() {
        new EqualsTester()
                .addEqualityGroup(matchTcpPort1, sameAsMatchTcpPort1)
                .addEqualityGroup(matchTcpPort2)
                .testEquals();
    }

    // UdpPortCriterion class

    /**
     * Test the matchUdpSrc method.
     */
    @Test
    public void testMatchUdpSrcMethod() {
        Criterion matchUdpSrc = Criteria.matchUdpSrc((short) 1);
        Criteria.UdpPortCriterion udpPortCriterion =
                checkAndConvert(matchUdpSrc,
                                Criterion.Type.UDP_SRC,
                                Criteria.UdpPortCriterion.class);
        assertThat(udpPortCriterion.udpPort(), is(equalTo((short) 1)));
    }

    /**
     * Test the matchUdpDst method.
     */
    @Test
    public void testMatchUdpDstMethod() {
        Criterion matchUdpDst = Criteria.matchUdpDst((short) 1);
        Criteria.UdpPortCriterion udpPortCriterion =
                checkAndConvert(matchUdpDst,
                        Criterion.Type.UDP_DST,
                        Criteria.UdpPortCriterion.class);
        assertThat(udpPortCriterion.udpPort(), is(equalTo((short) 1)));
    }

    /**
     * Test the equals() method of the UdpPortCriterion class.
     */
    @Test
    public void testUdpPortCriterionEquals() {
        new EqualsTester()
                .addEqualityGroup(matchUdpPort1, sameAsMatchUdpPort1)
                .addEqualityGroup(matchUdpPort2)
                .testEquals();
    }

    // SctpPortCriterion class

    /**
     * Test the matchSctpSrc method.
     */
    @Test
    public void testMatchSctpSrcMethod() {
        Criterion matchSctpSrc = Criteria.matchSctpSrc((short) 1);
        Criteria.SctpPortCriterion sctpPortCriterion =
                checkAndConvert(matchSctpSrc,
                                Criterion.Type.SCTP_SRC,
                                Criteria.SctpPortCriterion.class);
        assertThat(sctpPortCriterion.sctpPort(), is(equalTo((short) 1)));
    }

    /**
     * Test the matchSctpDst method.
     */
    @Test
    public void testMatchSctpDstMethod() {
        Criterion matchSctpDst = Criteria.matchSctpDst((short) 1);
        Criteria.SctpPortCriterion sctpPortCriterion =
                checkAndConvert(matchSctpDst,
                        Criterion.Type.SCTP_DST,
                        Criteria.SctpPortCriterion.class);
        assertThat(sctpPortCriterion.sctpPort(), is(equalTo((short) 1)));
    }

    /**
     * Test the equals() method of the SctpPortCriterion class.
     */
    @Test
    public void testSctpPortCriterionEquals() {
        new EqualsTester()
                .addEqualityGroup(matchSctpPort1, sameAsMatchSctpPort1)
                .addEqualityGroup(matchSctpPort2)
                .testEquals();
    }

    // IcmpTypeCriterion class

    /**
     * Test the matchIcmpType method.
     */
    @Test
    public void testMatchIcmpTypeMethod() {
        Byte icmpType = 12;
        Criterion matchIcmpType = Criteria.matchIcmpType(icmpType);
        Criteria.IcmpTypeCriterion icmpTypeCriterion =
                checkAndConvert(matchIcmpType,
                                Criterion.Type.ICMPV4_TYPE,
                                Criteria.IcmpTypeCriterion.class);
        assertThat(icmpTypeCriterion.icmpType(), is(equalTo(icmpType)));
    }

    /**
     * Test the equals() method of the IcmpTypeCriterion class.
     */
    @Test
    public void testIcmpTypeCriterionEquals() {
        new EqualsTester()
                .addEqualityGroup(matchIcmpType1, sameAsMatchIcmpType1)
                .addEqualityGroup(matchIcmpType2)
                .testEquals();
    }

    // IcmpCodeCriterion class

    /**
     * Test the matchIcmpCode method.
     */
    @Test
    public void testMatchIcmpCodeMethod() {
        Byte icmpCode = 12;
        Criterion matchIcmpCode = Criteria.matchIcmpCode(icmpCode);
        Criteria.IcmpCodeCriterion icmpCodeCriterion =
                checkAndConvert(matchIcmpCode,
                                Criterion.Type.ICMPV4_CODE,
                                Criteria.IcmpCodeCriterion.class);
        assertThat(icmpCodeCriterion.icmpCode(), is(equalTo(icmpCode)));
    }

    /**
     * Test the equals() method of the IcmpCodeCriterion class.
     */
    @Test
    public void testIcmpCodeCriterionEquals() {
        new EqualsTester()
                .addEqualityGroup(matchIcmpCode1, sameAsMatchIcmpCode1)
                .addEqualityGroup(matchIcmpCode2)
                .testEquals();
    }

    // IPv6FlowLabelCriterion class

    /**
     * Test the matchIPv6FlowLabel method.
     */
    @Test
    public void testMatchIPv6FlowLabelMethod() {
        Integer flowLabel = 12;
        Criterion matchFlowLabel = Criteria.matchIPv6FlowLabel(flowLabel);
        Criteria.IPv6FlowLabelCriterion flowLabelCriterion =
                checkAndConvert(matchFlowLabel,
                                Criterion.Type.IPV6_FLABEL,
                                Criteria.IPv6FlowLabelCriterion.class);
        assertThat(flowLabelCriterion.flowLabel(), is(equalTo(flowLabel)));
    }

    /**
     * Test the equals() method of the IPv6FlowLabelCriterion class.
     */
    @Test
    public void testIPv6FlowLabelCriterionEquals() {
        new EqualsTester()
                .addEqualityGroup(matchFlowLabel1, sameAsMatchFlowLabel1)
                .addEqualityGroup(matchFlowLabel2)
                .testEquals();
    }

    // Icmpv6TypeCriterion class

    /**
     * Test the matchIcmpv6Type method.
     */
    @Test
    public void testMatchIcmpv6TypeMethod() {
        Byte icmpv6Type = 12;
        Criterion matchIcmpv6Type = Criteria.matchIcmpv6Type(icmpv6Type);
        Criteria.Icmpv6TypeCriterion icmpv6TypeCriterion =
                checkAndConvert(matchIcmpv6Type,
                                Criterion.Type.ICMPV6_TYPE,
                                Criteria.Icmpv6TypeCriterion.class);
        assertThat(icmpv6TypeCriterion.icmpv6Type(), is(equalTo(icmpv6Type)));
    }

    /**
     * Test the equals() method of the Icmpv6TypeCriterion class.
     */
    @Test
    public void testIcmpv6TypeCriterionEquals() {
        new EqualsTester()
                .addEqualityGroup(matchIcmpv6Type1, sameAsMatchIcmpv6Type1)
                .addEqualityGroup(matchIcmpv6Type2)
                .testEquals();
    }

    // Icmpv6CodeCriterion class

    /**
     * Test the matchIcmpv6Code method.
     */
    @Test
    public void testMatchIcmpv6CodeMethod() {
        Byte icmpv6Code = 12;
        Criterion matchIcmpv6Code = Criteria.matchIcmpv6Code(icmpv6Code);
        Criteria.Icmpv6CodeCriterion icmpv6CodeCriterion =
                checkAndConvert(matchIcmpv6Code,
                                Criterion.Type.ICMPV6_CODE,
                                Criteria.Icmpv6CodeCriterion.class);
        assertThat(icmpv6CodeCriterion.icmpv6Code(), is(equalTo(icmpv6Code)));
    }

    /**
     * Test the equals() method of the Icmpv6CodeCriterion class.
     */
    @Test
    public void testIcmpv6CodeCriterionEquals() {
        new EqualsTester()
                .addEqualityGroup(matchIcmpv6Code1, sameAsMatchIcmpv6Code1)
                .addEqualityGroup(matchIcmpv6Code2)
                .testEquals();
    }

    // IPv6NDTargetAddressCriterion class

    /**
     * Test the matchIPv6NDTargetAddress method.
     */
    @Test
    public void testMatchIPv6NDTargetAddressMethod() {
        Criterion matchTargetAddress =
            Criteria.matchIPv6NDTargetAddress(ip6TargetAddress1);
        Criteria.IPv6NDTargetAddressCriterion targetAddressCriterion =
                checkAndConvert(matchTargetAddress,
                                Criterion.Type.IPV6_ND_TARGET,
                                Criteria.IPv6NDTargetAddressCriterion.class);
        assertThat(targetAddressCriterion.targetAddress(),
                   is(ip6TargetAddress1));
    }

    /**
     * Test the equals() method of the IPv6NDTargetAddressCriterion class.
     */
    @Test
    public void testIPv6NDTargetAddressCriterionEquals() {
        new EqualsTester()
                .addEqualityGroup(matchIpv6TargetAddr1,
                                  sameAsMatchIpv6TargetAddr1)
                .addEqualityGroup(matchIpv6TargetAddr2)
                .testEquals();
    }

    // IPv6NDLinkLayerAddressCriterion class

    /**
     * Test the matchIPv6NDSourceLinkLayerAddress method.
     */
    @Test
    public void testMatchIPv6NDSourceLinkLayerAddressMethod() {
        Criterion matchSrcLlAddr =
            Criteria.matchIPv6NDSourceLinkLayerAddress(llMac1);
        Criteria.IPv6NDLinkLayerAddressCriterion srcLlCriterion =
                checkAndConvert(matchSrcLlAddr,
                        Criterion.Type.IPV6_ND_SLL,
                        Criteria.IPv6NDLinkLayerAddressCriterion.class);
        assertThat(srcLlCriterion.mac(), is(equalTo(llMac1)));
    }

    /**
     * Test the matchIPv6NDTargetLinkLayerAddress method.
     */
    @Test
    public void testMatchIPv6NDTargetLinkLayerAddressMethod() {
        Criterion matchTargetLlAddr =
            Criteria.matchIPv6NDTargetLinkLayerAddress(llMac1);
        Criteria.IPv6NDLinkLayerAddressCriterion targetLlCriterion =
                checkAndConvert(matchTargetLlAddr,
                        Criterion.Type.IPV6_ND_TLL,
                        Criteria.IPv6NDLinkLayerAddressCriterion.class);
        assertThat(targetLlCriterion.mac(), is(equalTo(llMac1)));
    }

    /**
     * Test the equals() method of the IPv6NDLinkLayerAddressCriterion class.
     */
    @Test
    public void testIPv6NDLinkLayerAddressCriterionEquals() {
        new EqualsTester()
                .addEqualityGroup(matchSrcLlAddr1, sameAsMatchSrcLlAddr1)
                .addEqualityGroup(matchSrcLlAddr2)
                .testEquals();

        new EqualsTester()
                .addEqualityGroup(matchTargetLlAddr1, sameAsMatchTargetLlAddr1)
                .addEqualityGroup(matchTargetLlAddr2)
                .testEquals();
    }

    // MplsCriterion class

    /**
     * Test the matchMplsLabel method.
     */
    @Test
    public void testMatchMplsLabelMethod() {
        Criterion matchMplsLabel = Criteria.matchMplsLabel(mpls1);
        Criteria.MplsCriterion mplsCriterion =
                checkAndConvert(matchMplsLabel,
                        Criterion.Type.MPLS_LABEL,
                        Criteria.MplsCriterion.class);
        assertThat(mplsCriterion.label(), is(equalTo(mpls1)));
    }

    /**
     * Test the equals() method of the MplsCriterion class.
     */
    @Test
    public void testMplsCriterionEquals() {
        new EqualsTester()
                .addEqualityGroup(matchMpls1, sameAsMatchMpls1)
                .addEqualityGroup(matchMpls2)
                .testEquals();
    }

    // LambdaCriterion class

    /**
     * Test the matchLambda method.
     */
    @Test
    public void testMatchLambdaMethod() {
        Criterion matchLambda = Criteria.matchLambda(lambda1);
        Criteria.LambdaCriterion lambdaCriterion =
                checkAndConvert(matchLambda,
                        Criterion.Type.OCH_SIGID,
                        Criteria.LambdaCriterion.class);
        assertThat(lambdaCriterion.lambda(), is(equalTo(lambda1)));
    }

    /**
     * Test the equals() method of the LambdaCriterion class.
     */
    @Test
    public void testLambdaCriterionEquals() {
        new EqualsTester()
                .addEqualityGroup(matchLambda1, sameAsMatchLambda1)
                .addEqualityGroup(matchLambda2)
                .testEquals();
    }

    // OpticalSignalTypeCriterion class

    /**
     * Test the matchOpticalSignalType method.
     */
    @Test
    public void testMatchOpticalSignalTypeMethod() {
        Criterion matchLambda = Criteria.matchOpticalSignalType(signalLambda1);
        Criteria.OpticalSignalTypeCriterion opticalSignalTypeCriterion =
                checkAndConvert(matchLambda,
                        Criterion.Type.OCH_SIGTYPE,
                        Criteria.OpticalSignalTypeCriterion.class);
        assertThat(opticalSignalTypeCriterion.signalType(), is(equalTo(signalLambda1)));
    }

    /**
     * Test the equals() method of the OpticalSignalTypeCriterion class.
     */
    @Test
    public void testOpticalSignalTypeCriterionEquals() {
        new EqualsTester()
                .addEqualityGroup(matchSignalLambda1, sameAsMatchSignalLambda1)
                .addEqualityGroup(matchSignalLambda2)
                .testEquals();
    }
}
