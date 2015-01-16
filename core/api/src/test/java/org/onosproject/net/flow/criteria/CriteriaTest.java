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

    Criterion matchTcpPort1 = Criteria.matchTcpSrc((short) 1);
    Criterion sameAsMatchTcpPort1 = Criteria.matchTcpSrc((short) 1);
    Criterion matchTcpPort2 = Criteria.matchTcpDst((short) 2);

    private static final String MAC1 = "00:00:00:00:00:01";
    private static final String MAC2 = "00:00:00:00:00:02";
    private MacAddress mac1 = new MacAddress(MAC1.getBytes());
    private MacAddress mac2 = new MacAddress(MAC2.getBytes());
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
        assertThatClassIsImmutable(Criteria.EthCriterion.class);
        assertThatClassIsImmutable(Criteria.EthTypeCriterion.class);
        assertThatClassIsImmutable(Criteria.IPCriterion.class);
        assertThatClassIsImmutable(Criteria.IPProtocolCriterion.class);
        assertThatClassIsImmutable(Criteria.VlanPcpCriterion.class);
        assertThatClassIsImmutable(Criteria.VlanIdCriterion.class);
        assertThatClassIsImmutable(Criteria.TcpPortCriterion.class);
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
     * Test the equals() method of the PortCriterion class.
     */
    @Test
    public void testPortCriterionEquals() {
        new EqualsTester()
                .addEqualityGroup(matchInPort1, sameAsMatchInPort1)
                .addEqualityGroup(matchInPort2)
                .testEquals();
    }

    // EthCriterion class

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
     * Test the matchEthDst method.
     */
    @Test
    public void testMatchEthDstMethod() {
        Criterion matchTcpDst = Criteria.matchEthDst(mac1);
        Criteria.EthCriterion ethCriterion =
                checkAndConvert(matchTcpDst,
                        Criterion.Type.ETH_DST,
                        Criteria.EthCriterion.class);
        assertThat(ethCriterion.mac(), is(equalTo(mac1)));
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
     * Test the matchIPSrc method.
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
     * Test the matchIPDst method.
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
     * Test the matchIPSrc method.
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
     * Test the matchIPDst method.
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
