/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.ospf.controller.impl;


import org.easymock.EasyMock;
import org.jboss.netty.channel.Channel;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.Ip4Address;
import org.onosproject.ospf.controller.OspfInterface;
import org.onosproject.ospf.controller.OspfLsa;
import org.onosproject.ospf.controller.OspfLsaType;
import org.onosproject.ospf.controller.OspfNeighborState;
import org.onosproject.ospf.controller.TopologyForDeviceAndLink;
import org.onosproject.ospf.controller.area.OspfAreaImpl;
import org.onosproject.ospf.controller.area.OspfInterfaceImpl;
import org.onosproject.ospf.controller.lsdb.LsaWrapperImpl;
import org.onosproject.ospf.controller.lsdb.LsdbAgeImpl;
import org.onosproject.ospf.protocol.lsa.LsaHeader;
import org.onosproject.ospf.protocol.lsa.types.NetworkLsa;
import org.onosproject.ospf.protocol.lsa.types.RouterLsa;
import org.onosproject.ospf.protocol.ospfpacket.OspfMessage;
import org.onosproject.ospf.protocol.ospfpacket.types.DdPacket;
import org.onosproject.ospf.protocol.ospfpacket.types.HelloPacket;
import org.onosproject.ospf.protocol.ospfpacket.types.LsRequest;
import org.onosproject.ospf.protocol.ospfpacket.types.LsUpdate;
import org.onosproject.ospf.protocol.util.ChecksumCalculator;
import org.onosproject.ospf.protocol.util.OspfUtil;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit test class for OspfNbrImpl.
 */
public class OspfNbrImplTest {

    private OspfNbrImpl ospfNbr;
    private OspfInterfaceImpl ospfInterface;
    private OspfAreaImpl ospfArea;
    private OspfInterfaceImpl ospfInterface1;
    private OspfInterfaceImpl ospfInterface2;
    private List<OspfInterface> ospfInterfaces;
    private List<OspfLsa> ospfLsaList;
    private Channel channel;
    private Channel channel1;
    private Channel channel2;
    private OspfMessage ospfMessage;
    private TopologyForDeviceAndLink topologyForDeviceAndLink;

    @Before
    public void setUp() throws Exception {
        ospfInterface = new OspfInterfaceImpl();
        ospfInterface.setInterfaceType(2);
        ospfInterface.setRouterDeadIntervalTime(30);
        ospfInterface.setReTransmitInterval(30);
        ospfInterface.setDr(Ip4Address.valueOf("1.1.1.1"));
        ospfInterface.setIpAddress(Ip4Address.valueOf("1.1.1.1"));
        ospfArea = new OspfAreaImpl();
        ospfInterface1 = new OspfInterfaceImpl();
        ospfInterface1.setInterfaceType(2);
        ospfInterface1.setRouterDeadIntervalTime(30);
        ospfInterface1.setReTransmitInterval(30);
        ospfInterface1.setDr(Ip4Address.valueOf("7.7.7.7"));
        ospfInterface1.setIpAddress(Ip4Address.valueOf("7.7.7.7"));
        ospfInterface2 = new OspfInterfaceImpl();
        ospfInterface2.setInterfaceType(2);
        ospfInterface2.setRouterDeadIntervalTime(30);
        ospfInterface2.setReTransmitInterval(30);
        ospfInterface2.setDr(Ip4Address.valueOf("6.6.6.6"));
        ospfInterface2.setIpAddress(Ip4Address.valueOf("6.6.6.6"));
        ospfInterfaces = new ArrayList();
        ospfInterfaces.add(ospfInterface);
        ospfInterfaces.add(ospfInterface1);
        ospfInterfaces.add(ospfInterface2);
        ospfArea.setInterfacesLst(ospfInterfaces);
        ospfArea.setRouterId(Ip4Address.valueOf("111.111.111.111"));
        topologyForDeviceAndLink = new TopologyForDeviceAndLinkImpl();
        ospfNbr = new OspfNbrImpl(ospfArea, ospfInterface, Ip4Address.valueOf("1.1.1.1"),
                                  Ip4Address.valueOf("2.2.2.2"), 2,
                                  new OspfInterfaceChannelHandler(new Controller(),
                                                                  ospfArea, ospfInterface),
                                  topologyForDeviceAndLink);

    }

    @After
    public void tearDown() throws Exception {
        ospfNbr = null;
        ospfArea = null;
        ospfInterface = null;
        ospfLsaList = null;
        channel = null;
        channel1 = null;
        channel2 = null;
        ospfMessage = null;

    }

    /**
     * Tests neighborIpAddr() method.
     */
    @Test
    public void testNeighborIpAddr() throws Exception {
        assertThat(ospfNbr.neighborIpAddr(), is(notNullValue()));
    }

    /**
     * Tests isOpaqueCapable() getter method.
     */
    @Test
    public void testIsOpaqueCapable() throws Exception {
        assertThat(ospfNbr.isOpaqueCapable(), is(false));
    }

    /**
     * Tests isOpaqueCapable() setter method.
     */
    @Test
    public void testSetIsOpaqueCapable() throws Exception {
        ospfNbr.setIsOpaqueCapable(true);
        assertThat(ospfNbr.isOpaqueCapable(), is(true));
    }

    /**
     * Tests oneWayReceived() method.
     */
    @Test
    public void testOneWayReceived() throws Exception {
        ospfMessage = new HelloPacket();
        ospfNbr.setState(OspfNeighborState.ATTEMPT);
        channel = EasyMock.createMock(Channel.class);
        ospfNbr.oneWayReceived(ospfMessage, channel);
        channel1 = EasyMock.createMock(Channel.class);
        ospfNbr.setState(OspfNeighborState.DOWN);
        ospfNbr.oneWayReceived(ospfMessage, channel1);
        channel2 = EasyMock.createMock(Channel.class);
        ospfNbr.setState(OspfNeighborState.TWOWAY);
        ospfNbr.oneWayReceived(ospfMessage, channel2);
        assertThat(ospfNbr, is(notNullValue()));
    }

    /**
     * Tests twoWayReceived() method.
     */
    @Test(expected = Exception.class)
    public void testTwoWayReceived() throws Exception {
        ospfNbr.setNeighborDr(Ip4Address.valueOf("1.1.1.1"));
        ospfMessage = new HelloPacket();
        ospfNbr.setState(OspfNeighborState.ATTEMPT);
        ospfNbr.setNeighborDr(Ip4Address.valueOf("2.2.2.2"));
        ospfInterface.setIpAddress(Ip4Address.valueOf("2.2.2.2"));
        channel = EasyMock.createMock(Channel.class);
        SocketAddress socketAddress = EasyMock.createMock(SocketAddress.class);
        channel.bind(socketAddress);
        ospfNbr.twoWayReceived(ospfMessage, channel);
        ospfInterface.setIpAddress(Ip4Address.valueOf("3.3.3.3"));
        channel1 = EasyMock.createMock(Channel.class);
        ospfNbr.twoWayReceived(ospfMessage, channel1);
        assertThat(ospfNbr, is(notNullValue()));
    }

    /**
     * Tests negotiationDone() method.
     */
    @Test(expected = Exception.class)
    public void testNegotiationDone() throws Exception {
        ospfLsaList = new ArrayList();
        ospfLsaList.add(new RouterLsa());
        ospfMessage = new HelloPacket();
        ospfNbr.setState(OspfNeighborState.EXSTART);
        channel = EasyMock.createMock(Channel.class);
        ospfNbr.negotiationDone(ospfMessage, true, ospfLsaList, channel);
        channel1 = EasyMock.createMock(Channel.class);
        ospfNbr.negotiationDone(ospfMessage, false, ospfLsaList, channel1);
        assertThat(ospfNbr, is(notNullValue()));
    }

    /**
     * Tests processLsas() method.
     */
    @Test
    public void testProcessLsas() throws Exception {
        ospfLsaList = new ArrayList();
        RouterLsa routerLsa = new RouterLsa();
        routerLsa.setLsType(1);
        ospfLsaList.add(routerLsa);
        NetworkLsa networkLsa = new NetworkLsa();
        routerLsa.setLsType(2);
        ospfLsaList.add(networkLsa);
        routerLsa.setLsType(3);
        ospfLsaList.add(routerLsa);
        ospfNbr.processLsas(ospfLsaList);
        assertThat(ospfNbr, is(notNullValue()));
    }

    /**
     * Tests seqNumMismatch() method.
     */
    @Test
    public void testSeqNumMismatch() throws Exception {
        ospfNbr.setState(OspfNeighborState.FULL);
        assertThat(ospfNbr.seqNumMismatch("samelsa"), is(notNullValue()));
    }

    /**
     * Tests badLSReq() method.
     */
    @Test
    public void testBadLSReq() throws Exception {
        channel = EasyMock.createMock(Channel.class);
        ospfNbr.setState(OspfNeighborState.FULL);
        ospfNbr.badLSReq(channel);
        assertThat(ospfNbr, is(notNullValue()));
    }

    /**
     * Tests processDdPacket() method.
     */
    @Test
    public void testProcessDdPacket() throws Exception {
        ospfArea.addLsa(new RouterLsa(), false, ospfInterface);
        ospfArea.addLsa(new RouterLsa(), ospfInterface);
        ospfArea.addLsaToMaxAgeBin("lsa", new LsaWrapperImpl());
        channel = EasyMock.createMock(Channel.class);
        DdPacket ddPacket = new DdPacket();
        ddPacket.addLsaHeader(new LsaHeader());
        ospfNbr.processDdPacket(true, ddPacket, channel);
        channel1 = EasyMock.createMock(Channel.class);
        ddPacket.setIsMore(1);
        ospfNbr.processDdPacket(false, ddPacket, channel1);
        assertThat(ospfNbr, is(notNullValue()));
    }

    /**
     * Tests exchangeDone() method.
     */
    @Test
    public void testExchangeDone() throws Exception {
        ospfMessage = new HelloPacket();
        channel = EasyMock.createMock(Channel.class);
        ospfNbr.setState(OspfNeighborState.EXCHANGE);
        ospfNbr.exchangeDone(ospfMessage, channel);
        assertThat(ospfNbr, is(notNullValue()));
    }

    /**
     * Tests exchangeDone() method.
     */
    @Test
    public void testExchangeDone1() throws Exception {
        ospfMessage = new HelloPacket();
        channel = EasyMock.createMock(Channel.class);
        ospfNbr.setState(OspfNeighborState.EXCHANGE);
        ospfLsaList = new ArrayList();
        RouterLsa routerLsa = new RouterLsa();
        routerLsa.setLsType(1);
        ospfLsaList.add(routerLsa);
        NetworkLsa networkLsa = new NetworkLsa();
        routerLsa.setLsType(2);
        ospfLsaList.add(networkLsa);
        routerLsa.setLsType(3);
        ospfLsaList.add(routerLsa);
        ospfNbr.processLsas(ospfLsaList);
        ospfNbr.setState(OspfNeighborState.EXCHANGE);
        ospfNbr.exchangeDone(ospfMessage, channel);
        assertThat(ospfNbr, is(notNullValue()));
    }

    /**
     * Tests adjOk() method.
     */
    @Test
    public void testAdjOk() throws Exception {
        channel = EasyMock.createMock(Channel.class);
        ospfInterface.setIpAddress(Ip4Address.valueOf("2.2.2.2"));
        ospfNbr.setState(OspfNeighborState.TWOWAY);
        ospfNbr.setNeighborDr(Ip4Address.valueOf("2.2.2.2"));
        ospfNbr.adjOk(channel);
        Assert.assertNotNull(ospfNbr);
    }

    /**
     * Tests processLsUpdate() method.
     */
    @Test
    public void testProcessLsUpdate() throws Exception {
        LsUpdate ospfMessage = new LsUpdate();
        ospfMessage.setSourceIp(Ip4Address.valueOf("10.10.10.10"));
        ospfMessage.addLsa(new RouterLsa());
        ospfMessage.addLsa(new NetworkLsa());
        channel = EasyMock.createMock(Channel.class);
        ospfNbr.setState(OspfNeighborState.EXCHANGE);
        ospfNbr.processLsUpdate(ospfMessage, channel);
        assertThat(ospfNbr, is(notNullValue()));
    }

    /**
     * Tests loadingDone() method.
     */
    @Test
    public void testLoadingDone() throws Exception {
        ospfArea.addLsa(new RouterLsa(), false, ospfInterface);
        ospfArea.addLsa(new RouterLsa(), ospfInterface);
        ospfArea.addLsaToMaxAgeBin("lsa", new LsaWrapperImpl());
        ospfNbr.loadingDone();
        assertThat(ospfNbr, is(notNullValue()));
    }

    /**
     * Tests processReceivedLsa() method.
     */
    @Test
    public void testProcessReceivedLsa() throws Exception {
        LsaWrapperImpl lsaWrapper = new LsaWrapperImpl();
        LsdbAgeImpl lsdbAge = new LsdbAgeImpl(new OspfAreaImpl());
        lsdbAge.ageLsaAndFlood();
        lsaWrapper.setLsdbAge(lsdbAge);
        lsaWrapper.setLsaHeader(new NetworkLsa());
        RouterLsa routerlsa = new RouterLsa();
        routerlsa.setLsType(1);
        routerlsa.setOptions(2);
        routerlsa.setAdvertisingRouter(Ip4Address.valueOf("1.1.1.1"));
        routerlsa.setAge(100);
        routerlsa.setLinkStateId("2.2.2.2");
        routerlsa.setLsSequenceNo(1010101);
        lsaWrapper.setLsaHeader(new RouterLsa());
        ospfArea.addLsa(routerlsa, false, ospfInterface);

        lsaWrapper.addLsa(OspfLsaType.ROUTER, routerlsa);
        ospfArea.addLsa(routerlsa, ospfInterface);
        ospfArea.addLsaToMaxAgeBin("lsa", new LsaWrapperImpl());
        byte[] res = routerlsa.asBytes();
        routerlsa.setLsPacketLen(res.length);
        res = new ChecksumCalculator().calculateLsaChecksum(routerlsa.asBytes(), 16, 17);
        routerlsa.setLsCheckSum(OspfUtil.byteToInteger(res));
        channel = EasyMock.createMock(Channel.class);
        lsdbAge.ageLsaAndFlood();
        assertThat(ospfNbr.processReceivedLsa(lsaWrapper.lsaHeader(), true, channel,
                                              Ip4Address.valueOf("10.10.10.10")), is(true));
        channel1 = EasyMock.createMock(Channel.class);
        assertThat(ospfNbr.processReceivedLsa(routerlsa, true, channel1,
                                              Ip4Address.valueOf("10.10.10.10")), is(true));
    }

    /**
     * Tests isNullorLatest() method.
     */
    @Test
    public void testIsNullorLatest() throws Exception {

        LsaWrapperImpl lsaWrapper = new LsaWrapperImpl();
        LsdbAgeImpl lsdbAge = new LsdbAgeImpl(new OspfAreaImpl());
        lsdbAge.ageLsaAndFlood();
        lsaWrapper.setLsdbAge(lsdbAge);
        lsaWrapper.setLsaHeader(new LsaHeader());
        lsaWrapper.addLsa(OspfLsaType.ROUTER, new RouterLsa());
        assertThat(ospfNbr.isNullorLatest(lsaWrapper, new LsaHeader()), is(notNullValue()));
    }

    /**
     * Tests processSelfOriginatedLsa() method.
     */
    @Test
    public void testProcessSelfOriginatedLsa() throws Exception {
        ospfNbr.processSelfOriginatedLsa();
        assertThat(ospfNbr, is(notNullValue()));
    }

    /**
     * Tests sendLsa() method.
     */
    @Test
    public void testSendLsa() throws Exception {
        channel = EasyMock.createMock(Channel.class);
        ospfNbr.sendLsa(new LsaHeader(), Ip4Address.valueOf("1.1.1.1"), channel);
        assertThat(ospfNbr, is(notNullValue()));
    }

    /**
     * Tests directAcknowledge() method.
     */
    @Test
    public void testDirectAcknowledge() throws Exception {
        channel = EasyMock.createMock(Channel.class);
        ospfNbr.directAcknowledge(new LsaHeader(), channel, Ip4Address.valueOf("1.1.1.1"));
        assertThat(ospfNbr, is(notNullValue()));
    }

    /**
     * Tests neighborDown() method.
     */
    @Test(expected = Exception.class)
    public void testNeighborDown() throws Exception {
        ospfNbr.neighborDown();
        assertThat(ospfNbr, is(notNullValue()));
    }

    /**
     * Tests startFloodingTimer() method.
     */
    @Test
    public void testStartFloodingTimer() throws Exception {
        channel = EasyMock.createMock(Channel.class);
        ospfNbr.startFloodingTimer(channel);
        assertThat(ospfNbr, is(notNullValue()));
    }

    /**
     * Tests lastDdPacket() getter method.
     */
    @Test
    public void testGetLastDdPacket() throws Exception {
        ospfNbr.setLastDdPacket(new DdPacket());
        assertThat(ospfNbr.lastDdPacket(), is(notNullValue()));
    }

    /**
     * Tests lastDdPacket() setter method.
     */
    @Test
    public void testSetLastDdPacket() throws Exception {
        ospfNbr.setLastDdPacket(new DdPacket());
        assertThat(ospfNbr.lastDdPacket(), is(notNullValue()));
    }

    /**
     * Tests neighborId() getter method.
     */
    @Test
    public void testNeighborId() throws Exception {
        ospfNbr.setNeighborId(Ip4Address.valueOf("1.1.1.1"));
        assertThat(ospfNbr.neighborId(), is(Ip4Address.valueOf("1.1.1.1")));
    }

    /**
     * Tests neighborId() setter method.
     */
    @Test
    public void testSetNeighborId() throws Exception {
        ospfNbr.setNeighborId(Ip4Address.valueOf("1.1.1.1"));
        assertThat(ospfNbr.neighborId(), is(Ip4Address.valueOf("1.1.1.1")));
    }

    /**
     * Tests neighborDr() getter method.
     */
    @Test
    public void testNeighborDr() throws Exception {
        ospfNbr.setNeighborDr(Ip4Address.valueOf("1.1.1.1"));
        assertThat(ospfNbr.neighborDr(), is(Ip4Address.valueOf("1.1.1.1")));
    }

    /**
     * Tests neighborDr() setter method.
     */
    @Test
    public void testSetNeighborDr() throws Exception {
        ospfNbr.setNeighborDr(Ip4Address.valueOf("1.1.1.1"));
        assertThat(ospfNbr.neighborDr(), is(Ip4Address.valueOf("1.1.1.1")));
    }

    /**
     * Tests neighborBdr() getter method.
     */
    @Test
    public void testNeighborBdr() throws Exception {
        ospfNbr.setNeighborBdr(Ip4Address.valueOf("1.1.1.1"));
        assertThat(ospfNbr.neighborBdr(), is(Ip4Address.valueOf("1.1.1.1")));
    }

    /**
     * Tests neighborBdr() setter method.
     */
    @Test
    public void testSetNeighborBdr() throws Exception {
        ospfNbr.setNeighborBdr(Ip4Address.valueOf("1.1.1.1"));
        assertThat(ospfNbr.neighborBdr(), is(Ip4Address.valueOf("1.1.1.1")));
    }

    /**
     * Tests routerPriority() getter method.
     */
    @Test
    public void testRouterPriority() throws Exception {
        ospfNbr.setRouterPriority(1);
        assertThat(ospfNbr.routerPriority(), is(1));
    }

    /**
     * Tests routerPriority() setter method.
     */
    @Test
    public void testSetRouterPriority() throws Exception {
        ospfNbr.setRouterPriority(1);
        assertThat(ospfNbr.routerPriority(), is(1));
    }

    /**
     * Tests options() getter method.
     */
    @Test
    public void testGetOptions() throws Exception {
        ospfNbr.setOptions(1);
        assertThat(ospfNbr.options(), is(1));
    }

    /**
     * Tests options() setter method.
     */
    @Test
    public void testSetOptions() throws Exception {
        ospfNbr.setOptions(1);
        assertThat(ospfNbr.options(), is(1));
    }

    /**
     * Tests ddSeqNum() getter method.
     */
    @Test
    public void testGetDdSeqNum() throws Exception {
        ospfNbr.setDdSeqNum(1);
        assertThat(ospfNbr.ddSeqNum(), is(1L));
    }

    /**
     * Tests ddSeqNum() setter method.
     */
    @Test
    public void testSetDdSeqNum() throws Exception {
        ospfNbr.setDdSeqNum(1);
        assertThat(ospfNbr.ddSeqNum(), is(1L));
    }

    /**
     * Tests isMaster() getter method.
     */
    @Test
    public void testIsMaster() throws Exception {
        ospfNbr.setIsMaster(1);
        assertThat(ospfNbr.isMaster(), is(1));
    }

    /**
     * Tests lastDdPacket() getter method.
     */
    @Test
    public void testGetLastSentDdPacket() throws Exception {
        ospfNbr.setLastDdPacket(new DdPacket());
        assertThat(ospfNbr.lastDdPacket(), is(notNullValue()));
    }

    /**
     * Tests lastDdPacket() setter method.
     */
    @Test
    public void testSetLastSentDdPacket() throws Exception {
        ospfNbr.setLastDdPacket(new DdPacket());
        assertThat(ospfNbr.lastDdPacket(), is(notNullValue()));
    }

    /**
     * Tests getLastSentLsrPacket() getter method.
     */
    @Test
    public void testGetLastSentLsrPacket() throws Exception {
        ospfNbr.setLastSentLsrPacket(new LsRequest());
        assertThat(ospfNbr.getLastSentLsrPacket(), is(notNullValue()));
    }

    /**
     * Tests getLastSentLsrPacket() setter method.
     */
    @Test
    public void testSetLastSentLsrPacket() throws Exception {
        ospfNbr.setLastSentLsrPacket(new LsRequest());
        assertThat(ospfNbr.getLastSentLsrPacket(), is(notNullValue()));
    }

    /**
     * Tests getState() getter method.
     */
    @Test
    public void testGetState() throws Exception {
        ospfNbr.setState(OspfNeighborState.EXCHANGE);
        assertThat(ospfNbr.getState(), is(OspfNeighborState.EXCHANGE));
    }

    /**
     * Tests getState() setter method.
     */
    @Test
    public void testSetState() throws Exception {
        ospfNbr.setState(OspfNeighborState.EXCHANGE);
        assertThat(ospfNbr.getState(), is(OspfNeighborState.EXCHANGE));
    }

    /**
     * Tests isMaster() setter method.
     */
    @Test
    public void testSetIsMaster() throws Exception {
        ospfNbr.setIsMaster(1);
        assertThat(ospfNbr.isMaster(), is(1));
    }

    /**
     * Tests getLsReqList() method.
     */
    @Test
    public void testGetLsReqList() throws Exception {
        assertThat(ospfNbr.getLsReqList(), is(notNullValue()));
    }

    /**
     * Tests getReTxList() method.
     */
    @Test
    public void testGetReTxList() throws Exception {
        assertThat(ospfNbr.getReTxList(), is(notNullValue()));
    }

    /**
     * Tests getPendingReTxList() method.
     */
    @Test
    public void testGetPendingReTxList() throws Exception {
        assertThat(ospfNbr.getPendingReTxList(), is(notNullValue()));
    }
}