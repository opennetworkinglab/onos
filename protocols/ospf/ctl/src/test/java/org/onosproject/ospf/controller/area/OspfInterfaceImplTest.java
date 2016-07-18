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
package org.onosproject.ospf.controller.area;

import org.easymock.EasyMock;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.buffer.HeapChannelBufferFactory;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelConfig;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.Ip4Address;
import org.onosproject.ospf.controller.OspfAreaAddressRange;
import org.onosproject.ospf.controller.OspfInterface;
import org.onosproject.ospf.controller.OspfLsaType;
import org.onosproject.ospf.controller.OspfMessage;
import org.onosproject.ospf.controller.OspfNbr;
import org.onosproject.ospf.controller.OspfNeighborState;
import org.onosproject.ospf.controller.OspfProcess;
import org.onosproject.ospf.controller.TopologyForDeviceAndLink;
import org.onosproject.ospf.controller.impl.Controller;
import org.onosproject.ospf.controller.impl.OspfInterfaceChannelHandler;
import org.onosproject.ospf.controller.impl.OspfLinkTedImpl;
import org.onosproject.ospf.controller.impl.OspfNbrImpl;
import org.onosproject.ospf.controller.impl.OspfRouterImpl;
import org.onosproject.ospf.controller.impl.TopologyForDeviceAndLinkImpl;
import org.onosproject.ospf.controller.util.OspfEligibleRouter;
import org.onosproject.ospf.controller.util.OspfInterfaceType;
import org.onosproject.ospf.exceptions.OspfParseException;
import org.onosproject.ospf.protocol.lsa.LsaHeader;
import org.onosproject.ospf.protocol.lsa.OpaqueLsaHeader;
import org.onosproject.ospf.protocol.lsa.TlvHeader;
import org.onosproject.ospf.protocol.lsa.tlvtypes.RouterTlv;
import org.onosproject.ospf.protocol.lsa.types.OpaqueLsa10;
import org.onosproject.ospf.protocol.lsa.types.RouterLsa;
import org.onosproject.ospf.protocol.ospfpacket.subtype.LsRequestPacket;
import org.onosproject.ospf.protocol.ospfpacket.types.DdPacket;
import org.onosproject.ospf.protocol.ospfpacket.types.HelloPacket;
import org.onosproject.ospf.protocol.ospfpacket.types.LsAcknowledge;
import org.onosproject.ospf.protocol.ospfpacket.types.LsRequest;
import org.onosproject.ospf.protocol.ospfpacket.types.LsUpdate;
import org.onosproject.ospf.protocol.util.ChecksumCalculator;
import org.onosproject.ospf.protocol.util.OspfInterfaceState;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit test class for OspfInterfaceImpl.
 */
public class OspfInterfaceImplTest {
    private List<OspfAreaAddressRange> addressRanges = new ArrayList();
    private List<OspfInterface> ospfInterfaces = new ArrayList();
    private OspfInterfaceImpl ospfInterface;
    private OspfNbrImpl ospfNbr;
    private OpaqueLsaHeader opaqueLsaHeader;
    private int result;
    private OspfAreaImpl ospfArea;
    private HashMap<String, OspfNbr> ospfNbrHashMap;
    private TopologyForDeviceAndLink topologyForDeviceAndLink;
    private Channel channel;
    private ChannelHandlerContext channelHandlerContext;
    private ChannelStateEvent channelStateEvent;
    private HelloPacket helloPacket;
    private DdPacket ddPacket;
    private ChecksumCalculator checksumCalculator;
    private byte[] byteArray;
    private byte[] checkArray;
    private OspfInterfaceChannelHandler ospfInterfaceChannelHandler;
    private LsRequest lsRequest;
    private ChannelBuffer buf;
    private LsUpdate lsUpdate;
    private LsAcknowledge lsAck;
    private Controller controller;
    private List<OspfProcess> ospfProcesses = new ArrayList();
    private OspfProcess ospfProcess;
    private OspfEligibleRouter ospfEligibleRouter;

    @Before
    public void setUp() throws Exception {
        ospfProcess = new OspfProcessImpl();
        ospfProcesses.add(ospfProcess);
        ospfInterface = new OspfInterfaceImpl();
        topologyForDeviceAndLink = new TopologyForDeviceAndLinkImpl();
        channel = EasyMock.createMock(Channel.class);
        ospfArea = createOspfArea();
        ospfInterface = createOspfInterface();
        ospfNbrHashMap = new HashMap();
        topologyForDeviceAndLink = new TopologyForDeviceAndLinkImpl();
        ospfNbr = new OspfNbrImpl(ospfArea, ospfInterface, Ip4Address.valueOf("10.10.10.10"),
                                  Ip4Address.valueOf("2.2.2.2"), 2,
                                  topologyForDeviceAndLink);
        ospfNbr.setNeighborId(Ip4Address.valueOf("10.10.10.10"));
        ospfNbr.setRouterPriority(0);
        ospfNbr.setNeighborDr(Ip4Address.valueOf("13.13.13.13"));
        ospfInterface.addNeighbouringRouter(ospfNbr);
        controller = new Controller();
        ospfInterfaceChannelHandler = new OspfInterfaceChannelHandler(controller, ospfProcesses);

    }

    @After
    public void tearDown() throws Exception {
        ospfInterface = null;
        ospfNbr = null;
        opaqueLsaHeader = null;
        ospfNbrHashMap = null;
    }

    /**
     * Tests state() getter method.
     */
    @Test
    public void testGetState() throws Exception {
        ospfInterface.setState(OspfInterfaceState.DROTHER);
        assertThat(ospfInterface.state(), is(OspfInterfaceState.DROTHER));
    }

    /**
     * Tests state() setter method.
     */
    @Test
    public void testSetState() throws Exception {
        ospfInterface.setState(OspfInterfaceState.DROTHER);
        assertThat(ospfInterface.state(), is(OspfInterfaceState.DROTHER));
    }

    /**
     * Tests linkStateHeaders() method.
     */
    @Test
    public void testGetLinkStateHeaders() throws Exception {

        assertThat(ospfInterface.linkStateHeaders().size(), is(0));
    }

    /**
     * Tests ipNetworkMask() getter method.
     */
    @Test
    public void testGetIpNetworkMask() throws Exception {
        ospfInterface.setIpNetworkMask(Ip4Address.valueOf("1.1.1.1"));
        assertThat(ospfInterface.ipNetworkMask(), is(Ip4Address.valueOf("1.1.1.1")));
    }

    /**
     * Tests ipNetworkMask() setter method.
     */
    @Test
    public void testSetIpNetworkMask() throws Exception {
        ospfInterface.setIpNetworkMask(Ip4Address.valueOf("1.1.1.1"));
        assertThat(ospfInterface.ipNetworkMask(), is(Ip4Address.valueOf("1.1.1.1")));
    }

    /**
     * Tests addNeighbouringRouter() method.
     */
    @Test
    public void testAddNeighbouringRouter() throws Exception {
        ospfNbr = new OspfNbrImpl(new OspfAreaImpl(), new OspfInterfaceImpl(),
                                  Ip4Address.valueOf("1.1.1.1"), Ip4Address.valueOf("2.2.2.2"), 2,
                                  topologyForDeviceAndLink);
        ospfNbr.setNeighborId(Ip4Address.valueOf("111.111.111.111"));
        ospfInterface.addNeighbouringRouter(ospfNbr);
        assertThat(ospfInterface, is(notNullValue()));

    }

    /**
     * Tests neighbouringRouter() method.
     */
    @Test
    public void testGetNeighbouringRouter() throws Exception {
        ospfNbr = new OspfNbrImpl(new OspfAreaImpl(), new OspfInterfaceImpl(),
                                  Ip4Address.valueOf("1.1.1.1"), Ip4Address.valueOf("2.2.2.2"), 2,
                                  topologyForDeviceAndLink);
        ospfNbr.setNeighborId(Ip4Address.valueOf("111.111.111.111"));
        ospfInterface.addNeighbouringRouter(ospfNbr);
        assertThat(ospfInterface.neighbouringRouter("111.111.111.111"), is(notNullValue()));
    }

    /**
     * Tests addLsaHeaderForDelayAck() method.
     */
    @Test
    public void testAddLsaHeaderForDelayAck() throws Exception {
        opaqueLsaHeader = new OpaqueLsaHeader();
        opaqueLsaHeader.setLsType(10);
        ospfInterface.addLsaHeaderForDelayAck(opaqueLsaHeader);
        assertThat(ospfInterface, is(notNullValue()));
    }

    /**
     * Tests removeLsaFromNeighborMap() method.
     */
    @Test
    public void testRemoveLsaFromNeighborMap() throws Exception {
        ospfInterface.removeLsaFromNeighborMap("lsa10");
        assertThat(ospfInterface, is(notNullValue()));
    }

    /**
     * Tests isNeighborInList() method.
     */
    @Test
    public void testIsNeighborinList() throws Exception {
        ospfNbr = new OspfNbrImpl(new OspfAreaImpl(), new OspfInterfaceImpl(),
                                  Ip4Address.valueOf("1.1.1.1"), Ip4Address.valueOf("2.2.2.2"), 2,
                                  topologyForDeviceAndLink);
        ospfNbr.setNeighborId(Ip4Address.valueOf("111.111.111.111"));
        ospfInterface.addNeighbouringRouter(ospfNbr);
        assertThat(ospfInterface.isNeighborInList("111.111.111.111"), is(notNullValue()));
    }

    /**
     * Tests listOfNeighbors() getter method.
     */
    @Test
    public void testGetListOfNeighbors() throws Exception {
        ospfNbrHashMap = new HashMap();
        ospfNbr = new OspfNbrImpl(new OspfAreaImpl(), new OspfInterfaceImpl(),
                                  Ip4Address.valueOf("1.1.1.1"), Ip4Address.valueOf("2.2.2.2"), 2,
                                  topologyForDeviceAndLink);
        ospfNbr.setNeighborId(Ip4Address.valueOf("111.111.111.111"));
        ospfNbrHashMap.put("111.111.111.111", ospfNbr);
        ospfInterface.setListOfNeighbors(ospfNbrHashMap);
        assertThat(ospfInterface.listOfNeighbors().size(), is(1));
    }

    /**
     * Tests listOfNeighbors() setter method.
     */
    @Test
    public void testSetListOfNeighbors() throws Exception {
        ospfNbrHashMap = new HashMap();
        ospfNbr = new OspfNbrImpl(new OspfAreaImpl(), new OspfInterfaceImpl(),
                                  Ip4Address.valueOf("1.1.1.1"), Ip4Address.valueOf("2.2.2.2"), 2,
                                  topologyForDeviceAndLink);
        ospfNbr.setNeighborId(Ip4Address.valueOf("111.111.111.111"));
        ospfNbrHashMap.put("111.111.111.111", ospfNbr);
        ospfInterface.setListOfNeighbors(ospfNbrHashMap);
        assertThat(ospfInterface.listOfNeighbors().size(), is(1));
    }

    /**
     * Tests ipAddress() getter method.
     */
    @Test
    public void testGetIpAddress() throws Exception {
        ospfInterface.setIpAddress(Ip4Address.valueOf("1.1.1.1"));
        assertThat(ospfInterface.ipAddress(), is(Ip4Address.valueOf("1.1.1.1")));
    }

    /**
     * Tests ipAddress() getter method.
     */
    @Test
    public void testSetIpAddress() throws Exception {
        ospfInterface.setIpAddress(Ip4Address.valueOf("1.1.1.1"));
        assertThat(ospfInterface.ipAddress(), is(Ip4Address.valueOf("1.1.1.1")));
    }

    /**
     * Tests routerPriority() getter method.
     */
    @Test
    public void testGetRouterPriority() throws Exception {
        ospfInterface.setRouterPriority(1);
        Assert.assertEquals(1, ospfInterface.routerPriority());
    }

    /**
     * Tests routerPriority() setter method.
     */
    @Test
    public void testSetRouterPriority() throws Exception {
        ospfInterface.setRouterPriority(1);
        assertThat(ospfInterface.routerPriority(), is(1));
    }

    /**
     * Tests helloIntervalTime() getter method.
     */
    @Test
    public void testGetHelloIntervalTime() throws Exception {
        ospfInterface.setHelloIntervalTime(10);
        assertThat(ospfInterface.helloIntervalTime(), is(10));
    }

    /**
     * Tests helloIntervalTime() setter method.
     */
    @Test
    public void testSetHelloIntervalTime() throws Exception {
        ospfInterface.setHelloIntervalTime(10);
        assertThat(ospfInterface.helloIntervalTime(), is(10));
    }

    /**
     * Tests routerDeadIntervalTime() getter method.
     */
    @Test
    public void testGetRouterDeadIntervalTime() throws Exception {
        ospfInterface.setRouterDeadIntervalTime(10);
        assertThat(ospfInterface.routerDeadIntervalTime(), is(10));
    }

    /**
     * Tests routerDeadIntervalTime() setter method.
     */
    @Test
    public void testSetRouterDeadIntervalTime() throws Exception {
        ospfInterface.setRouterDeadIntervalTime(10);
        assertThat(ospfInterface.routerDeadIntervalTime(), is(10));
    }

    /**
     * Tests interfaceType() getter method.
     */
    @Test
    public void testGetInterfaceType() throws Exception {
        ospfInterface.setInterfaceType(1);
        assertThat(ospfInterface.interfaceType(), is(1));
    }

    /**
     * Tests interfaceType() setter method.
     */
    @Test
    public void testSetInterfaceType() throws Exception {
        ospfInterface.setInterfaceType(1);
        assertThat(ospfInterface.interfaceType(), is(1));
    }

    /**
     * Tests mtu() getter method.
     */
    @Test
    public void testGetMtu() throws Exception {
        ospfInterface.setMtu(100);
        assertThat(ospfInterface.mtu(), is(100));
    }

    /**
     * Tests mtu() setter method.
     */
    @Test
    public void testSetMtu() throws Exception {
        ospfInterface.setMtu(100);
        assertThat(ospfInterface.mtu(), is(100));
    }

    /**
     * Tests reTransmitInterval() getter method.
     */
    @Test
    public void testGetReTransmitInterval() throws Exception {
        ospfInterface.setReTransmitInterval(100);
        assertThat(ospfInterface.reTransmitInterval(), is(100));
    }

    /**
     * Tests reTransmitInterval() setter method.
     */
    @Test
    public void testSetReTransmitInterval() throws Exception {
        ospfInterface.setReTransmitInterval(100);
        assertThat(ospfInterface.reTransmitInterval(), is(100));
    }

    /**
     * Tests dr() getter method.
     */
    @Test
    public void testGetDr() throws Exception {
        ospfInterface.setDr(Ip4Address.valueOf("1.1.1.1"));
        assertThat(ospfInterface.dr(), is(Ip4Address.valueOf("1.1.1.1")));
    }

    /**
     * Tests dr() setter method.
     */
    @Test
    public void testSetDr() throws Exception {
        ospfInterface.setDr(Ip4Address.valueOf("1.1.1.1"));
        assertThat(ospfInterface.dr(), is(Ip4Address.valueOf("1.1.1.1")));
    }

    /**
     * Tests bdr() getter method.
     */
    @Test
    public void testGetBdr() throws Exception {
        ospfInterface.setBdr(Ip4Address.valueOf("1.1.1.1"));
        assertThat(ospfInterface.bdr(), is(Ip4Address.valueOf("1.1.1.1")));
    }

    /**
     * Tests bdr() setter method.
     */
    @Test
    public void testSetBdr() throws Exception {
        ospfInterface.setBdr(Ip4Address.valueOf("1.1.1.1"));
        assertThat(ospfInterface.bdr(), is(Ip4Address.valueOf("1.1.1.1")));
    }

    /**
     * Tests equals() method.
     */
    @Test
    public void testEquals() throws Exception {
        assertThat(ospfInterface.equals(new OspfInterfaceImpl()), is(false));
    }

    /**
     * Tests hashCode() method.
     */
    @Test
    public void testHashCode() throws Exception {
        result = ospfInterface.hashCode();
        assertThat(result, is(notNullValue()));
    }

    /**
     * Tests to string method.
     */
    @Test
    public void testToString() throws Exception {
        assertThat(ospfInterface.toString(), is(notNullValue()));
    }

    /**
     * Tests to interfaceUp() method.
     */
    @Test(expected = Exception.class)
    public void testInterfaceUp() throws Exception {
        ospfInterface.setInterfaceType(OspfInterfaceType.POINT_TO_POINT.value());
        ospfInterface.interfaceUp();
        assertThat(ospfInterface, is(notNullValue()));
    }

    /**
     * Tests to interfaceUp() method.
     */
    @Test(expected = Exception.class)
    public void testInterfaceUp1() throws Exception {

        ospfInterface.setInterfaceType(OspfInterfaceType.BROADCAST.value());
        ospfInterface.interfaceUp();
        assertThat(ospfInterface, is(notNullValue()));
    }

    /**
     * Tests to interfaceUp() method.
     */
    @Test(expected = Exception.class)
    public void testInterfaceUp2() throws Exception {

        ospfInterface.setRouterPriority(1);
        ospfInterface.setInterfaceType(OspfInterfaceType.BROADCAST.value());
        ospfInterface.interfaceUp();
        assertThat(ospfInterface, is(notNullValue()));
    }

    /**
     * Tests to backupSeen() method.
     */
    @Test
    public void testBackupSeen() throws Exception {
        ospfInterface.setOspfArea(ospfArea);
        ospfInterface.setState(OspfInterfaceState.WAITING);
        ospfInterface.backupSeen(channel);
        assertThat(ospfInterface, is(notNullValue()));
    }

    /**
     * Tests to waitTimer() method.
     */
    @Test
    public void testWaitTimer() throws Exception {
        ospfInterface.setOspfArea(ospfArea);
        ospfInterface.setState(OspfInterfaceState.WAITING);
        ospfInterface.waitTimer(channel);
        assertThat(ospfInterface, is(notNullValue()));
    }

    /**
     * Tests to callDrElection() method.
     */
    @Test
    public void testCallDrElection() throws Exception {
        ospfInterface.setOspfArea(ospfArea);
        ospfInterface.setState(OspfInterfaceState.WAITING);
        ospfInterface.callDrElection(channel);
        assertThat(ospfInterface, is(notNullValue()));
    }

    /**
     * Tests to neighborChange() method.
     */
    @Test
    public void testNeighborChange() throws Exception {
        ospfInterface.setOspfArea(ospfArea);
        ospfInterface.setState(OspfInterfaceState.DR);
        ospfInterface.neighborChange();
        assertThat(ospfInterface, is(notNullValue()));
    }

    /**
     * Tests to interfaceDown() method.
     */
    @Test(expected = Exception.class)
    public void testInterfaceDown() throws Exception {
        ospfInterface.interfaceDown();
        assertThat(ospfInterface, is(notNullValue()));
    }

    /**
     * Tests processOspfMessage() method.
     */
    @Test
    public void testProcessOspfMessage() throws Exception {
        ospfInterface.setOspfArea(ospfArea);
        ospfInterface.setInterfaceType(OspfInterfaceType.POINT_TO_POINT.value());
        ospfInterface.setIpNetworkMask(Ip4Address.valueOf("0.0.0.0"));
        ospfInterface.setIpAddress(Ip4Address.valueOf("11.11.11.11"));
        ospfInterface.setInterfaceType(2);
        ospfArea.setAreaId(Ip4Address.valueOf("12.12.12.12"));
        channelHandlerContext = EasyMock.createMock(ChannelHandlerContext.class);
        OspfMessage message;
        helloPacket = new HelloPacket();
        helloPacket.setSourceIp(Ip4Address.valueOf("1.1.0.1"));
        helloPacket.setRouterId(Ip4Address.valueOf("10.10.10.10"));
        helloPacket.setOspfVer(2);
        helloPacket.setAreaId(Ip4Address.valueOf("12.12.12.12"));
        helloPacket.setOptions(2);
        helloPacket.setNetworkMask(Ip4Address.valueOf("3.3.3.3"));
        helloPacket.setOspftype(1);
        helloPacket.setAuthType(0);
        helloPacket.setHelloInterval(60);
        helloPacket.setRouterDeadInterval(60);
        helloPacket.setAuthentication(0);
        helloPacket.setNetworkMask(Ip4Address.valueOf("1.1.1.1"));
        checksumCalculator = new ChecksumCalculator();
        byteArray = helloPacket.asBytes();
        helloPacket.setOspfPacLength(byteArray.length);
        checkArray = checksumCalculator.calculateOspfCheckSum(byteArray, 12, 13);
        checkArray[0] = -51;
        checkArray[1] = 52;
        buf = ChannelBuffers.copiedBuffer(checkArray);
        helloPacket.setChecksum(buf.readUnsignedShort());
        message = helloPacket;
        ospfInterface.processOspfMessage(message, channelHandlerContext);
        ddPacket = new DdPacket();
        ddPacket.setSourceIp(Ip4Address.valueOf("1.1.1.1"));
        ddPacket.setRouterId(Ip4Address.valueOf("10.10.10.10"));
        ddPacket.setOspfVer(2);
        ddPacket.setAreaId(Ip4Address.valueOf("12.12.12.12"));
        ddPacket.setOptions(2);
        ddPacket.setOspftype(2);
        ddPacket.setAuthType(0);
        ddPacket.setAuthentication(0);
        checksumCalculator = new ChecksumCalculator();
        byteArray = ddPacket.asBytes();
        ddPacket.setOspfPacLength(byteArray.length);
        checkArray = checksumCalculator.calculateOspfCheckSum(byteArray, 12, 13);
        checkArray[0] = -49;
        checkArray[1] = -79;
        buf = ChannelBuffers.copiedBuffer(checkArray);
        ddPacket.setChecksum(buf.readUnsignedShort());
        channelHandlerContext = null;
        channelHandlerContext = EasyMock.createMock(ChannelHandlerContext.class);
        message = ddPacket;
        ospfInterface.processOspfMessage(message, channelHandlerContext);
        lsRequest = new LsRequest();
        lsRequest.setSourceIp(Ip4Address.valueOf("1.1.1.1"));
        lsRequest.setRouterId(Ip4Address.valueOf("10.10.10.10"));
        lsRequest.setOspfVer(2);
        lsRequest.setAreaId(Ip4Address.valueOf("12.12.12.12"));
        lsRequest.setOspftype(3);
        lsRequest.setAuthType(0);
        lsRequest.setAuthentication(0);
        checksumCalculator = new ChecksumCalculator();
        byteArray = lsRequest.asBytes();
        lsRequest.setOspfPacLength(byteArray.length);
        checkArray = checksumCalculator.calculateOspfCheckSum(byteArray, 12, 13);
        checkArray[0] = -47;
        checkArray[1] = -72;
        buf = ChannelBuffers.copiedBuffer(checkArray);
        lsRequest.setChecksum(buf.readUnsignedShort());
        message = lsRequest;
        channelHandlerContext = null;
        channelHandlerContext = EasyMock.createMock(ChannelHandlerContext.class);
        ospfInterface.processOspfMessage(message, channelHandlerContext);
        lsUpdate = new LsUpdate();
        lsUpdate.setSourceIp(Ip4Address.valueOf("1.1.1.1"));
        lsUpdate.setRouterId(Ip4Address.valueOf("10.10.10.10"));
        lsUpdate.setOspfVer(2);
        lsUpdate.setAreaId(Ip4Address.valueOf("12.12.12.12"));
        lsUpdate.setOspftype(4);
        lsUpdate.setAuthType(0);
        lsUpdate.setAuthentication(0);
        checksumCalculator = new ChecksumCalculator();
        byteArray = lsUpdate.asBytes();
        lsUpdate.setOspfPacLength(byteArray.length);
        checkArray = checksumCalculator.calculateOspfCheckSum(byteArray, 12, 13);
        checkArray[0] = -47;
        checkArray[1] = -77;
        buf = ChannelBuffers.copiedBuffer(checkArray);
        lsUpdate.setChecksum(buf.readUnsignedShort());
        message = lsUpdate;
        ospfInterface.processOspfMessage(message, channelHandlerContext);
        lsAck = new LsAcknowledge();
        lsAck.setSourceIp(Ip4Address.valueOf("1.1.1.1"));
        lsAck.setRouterId(Ip4Address.valueOf("10.10.10.10"));
        lsAck.setOspfVer(2);
        lsAck.setAreaId(Ip4Address.valueOf("12.12.12.12"));
        lsAck.setOspftype(5);
        lsAck.setAuthType(0);
        lsAck.setAuthentication(0);
        checksumCalculator = new ChecksumCalculator();
        byteArray = lsAck.asBytes();
        lsAck.setOspfPacLength(byteArray.length);
        checkArray = checksumCalculator.calculateOspfCheckSum(byteArray, 12, 13);
        checkArray[0] = -47;
        checkArray[1] = -74;
        buf = ChannelBuffers.copiedBuffer(checkArray);
        lsAck.setChecksum(buf.readUnsignedShort());
        message = lsAck;
        channelHandlerContext = null;
        channelHandlerContext = EasyMock.createMock(ChannelHandlerContext.class);
        ospfInterface.processOspfMessage(message, channelHandlerContext);
        assertThat(ospfInterface, is(notNullValue()));
    }

    /**
     * Tests processHelloMessage() method.
     */
    @Test
    public void testProcessHelloMessage() throws Exception {
        ospfInterface.setIpAddress(Ip4Address.valueOf("11.11.11.11"));
        ospfInterface.setInterfaceType(1);
        ospfInterface.setIpNetworkMask(Ip4Address.valueOf("244.244.244.244"));
        ospfInterface.setHelloIntervalTime(10);
        ospfInterface.setRouterDeadIntervalTime(10);
        ospfArea.setAreaId(Ip4Address.valueOf("12.12.12.12"));
        channelHandlerContext = EasyMock.createMock(ChannelHandlerContext.class);
        OspfMessage message;
        helloPacket = new HelloPacket();
        helloPacket.setSourceIp(Ip4Address.valueOf("1.1.1.1"));
        helloPacket.setOspfVer(2);
        helloPacket.setAreaId(Ip4Address.valueOf("12.12.12.12"));
        helloPacket.setNetworkMask(Ip4Address.valueOf("244.244.244.244"));
        helloPacket.setHelloInterval(10);
        helloPacket.setRouterDeadInterval(10);
        helloPacket.setDr(Ip4Address.valueOf("10.10.10.10"));
        helloPacket.setBdr(Ip4Address.valueOf("11.11.11.11"));
        helloPacket.setRouterId(Ip4Address.valueOf("111.111.111.111"));
        message = helloPacket;
        ospfInterface.processHelloMessage(message, channelHandlerContext);
        assertThat(ospfInterfaceChannelHandler, is(notNullValue()));
    }

    /**
     * Tests processHelloMessage() method.
     */
    @Test
    public void testProcessHelloMessage1() throws Exception {
        ospfInterface.setOspfArea(ospfArea);
        ospfInterface.setInterfaceType(2);
        ospfInterface.setRouterPriority(1);
        ospfInterface.interfaceUp();
        ospfInterface.setIpAddress(Ip4Address.valueOf("11.11.11.11"));
        ospfInterface.setState(OspfInterfaceState.WAITING);
        ospfInterface.setInterfaceType(2);
        ospfInterface.setIpNetworkMask(Ip4Address.valueOf("244.244.244.244"));
        ospfInterface.setHelloIntervalTime(10);
        ospfInterface.setRouterDeadIntervalTime(10);
        ospfArea.setAreaId(Ip4Address.valueOf("12.12.12.12"));
        channelHandlerContext = EasyMock.createMock(ChannelHandlerContext.class);
        OspfMessage message;
        helloPacket = new HelloPacket();
        helloPacket.setSourceIp(Ip4Address.valueOf("1.1.1.1"));
        helloPacket.setOspfVer(2);
        helloPacket.setAreaId(Ip4Address.valueOf("12.12.12.12"));
        helloPacket.setNetworkMask(Ip4Address.valueOf("244.244.244.244"));
        helloPacket.setHelloInterval(10);
        helloPacket.setRouterDeadInterval(10);
        helloPacket.setDr(Ip4Address.valueOf("10.10.10.10"));
        helloPacket.setBdr(Ip4Address.valueOf("11.11.11.11"));
        helloPacket.setRouterId(Ip4Address.valueOf("2.2.2.2"));
        message = helloPacket;
        channelHandlerContext = null;
        channelHandlerContext = EasyMock.createMock(ChannelHandlerContext.class);
        ospfInterface.processHelloMessage(message, channelHandlerContext);
        assertThat(ospfInterfaceChannelHandler, is(notNullValue()));
        ospfNbrHashMap = new HashMap();
        ospfNbr = new OspfNbrImpl(ospfArea, ospfInterface, Ip4Address.valueOf("10.10.10.10"),
                                  Ip4Address.valueOf("2.2.2.2"), 2,
                                  topologyForDeviceAndLink);
        ospfNbr.setNeighborId(Ip4Address.valueOf("2.2.2.2"));
        ospfNbr.setRouterPriority(0);
        ospfNbr.setNeighborDr(Ip4Address.valueOf("13.13.13.13"));
        ospfInterface.addNeighbouringRouter(ospfNbr);
        channelHandlerContext = null;
        channelHandlerContext = EasyMock.createMock(ChannelHandlerContext.class);
        ospfInterface.processHelloMessage(message, channelHandlerContext);
        assertThat(ospfInterfaceChannelHandler, is(notNullValue()));
    }

    /**
     * Tests processDdMessage() method.
     */
    @Test
    public void testProcessDdMessage() throws Exception {
        ospfInterface.setOspfArea(ospfArea);
        ospfInterface.setIpAddress(Ip4Address.valueOf("11.11.11.11"));
        ospfInterface.setInterfaceType(2);
        ospfInterface.setIpNetworkMask(Ip4Address.valueOf("255.255.255.255"));
        ospfInterface.setHelloIntervalTime(10);
        ospfInterface.setRouterDeadIntervalTime(10);
        ospfArea.setAreaId(Ip4Address.valueOf("12.12.12.12"));
        channelHandlerContext = EasyMock.createMock(ChannelHandlerContext.class);
        OspfMessage message;
        ddPacket = new DdPacket();
        ddPacket.setSourceIp(Ip4Address.valueOf("1.1.1.1"));
        ddPacket.setOspfVer(2);
        ddPacket.setAreaId(Ip4Address.valueOf("12.12.12.12"));
        ddPacket.setRouterId(Ip4Address.valueOf("2.2.2.2"));
        ddPacket.setIsOpaqueCapable(true);
        ddPacket.setIsMore(1);
        ddPacket.setIsInitialize(1);
        ddPacket.setIsMaster(1);
        ddPacket.setSequenceNo(123);
        message = ddPacket;
        ospfNbrHashMap = new HashMap();
        ospfNbr = new OspfNbrImpl(ospfArea, ospfInterface, Ip4Address.valueOf("10.10.10.10"),
                                  Ip4Address.valueOf("2.2.2.2"), 2,
                                  topologyForDeviceAndLink);
        ospfNbr.setLastDdPacket(createDdPacket());
        ospfNbr.setNeighborId(Ip4Address.valueOf("2.2.2.2"));
        ospfNbr.setState(OspfNeighborState.EXSTART);
        ospfNbr.setRouterPriority(0);
        ospfNbr.setNeighborDr(Ip4Address.valueOf("13.13.13.13"));
        ospfNbr.setDdSeqNum(123);
        ospfInterface.addNeighbouringRouter(ospfNbr);
        channelHandlerContext = null;
        channelHandlerContext = EasyMock.createMock(ChannelHandlerContext.class);
        ospfInterface.processDdMessage(message, channelHandlerContext);
        assertThat(ospfInterfaceChannelHandler, is(notNullValue()));
    }

    /**
     * Tests processDdMessage() method.
     */
    @Test(expected = Exception.class)
    public void testProcessDdMessage3() throws Exception {
        ospfInterface.setIpAddress(Ip4Address.valueOf("11.11.11.11"));
        ospfInterface.setInterfaceType(2);
        ospfInterface.setIpNetworkMask(Ip4Address.valueOf("255.255.255.255"));
        ospfInterface.setHelloIntervalTime(10);
        ospfInterface.setRouterDeadIntervalTime(10);
        ospfArea.setAreaId(Ip4Address.valueOf("12.12.12.12"));
        channelHandlerContext = EasyMock.createMock(ChannelHandlerContext.class);
        OspfMessage message;
        ddPacket = new DdPacket();
        ddPacket.setSourceIp(Ip4Address.valueOf("1.1.1.1"));
        ddPacket.setOspfVer(2);
        ddPacket.setAreaId(Ip4Address.valueOf("12.12.12.12"));
        ddPacket.setRouterId(Ip4Address.valueOf("2.2.2.2"));
        ddPacket.setIsOpaqueCapable(true);
        ddPacket.setIsMore(1);
        ddPacket.setIsInitialize(1);
        ddPacket.setIsMaster(1);
        ddPacket.setSequenceNo(123);
        message = ddPacket;
        ospfNbrHashMap = new HashMap();
        ospfNbr = new OspfNbrImpl(ospfArea, ospfInterface, Ip4Address.valueOf("10.10.10.10"),
                                  Ip4Address.valueOf("2.2.2.2"), 2,
                                  topologyForDeviceAndLink);
        ospfNbr.setLastDdPacket(createDdPacket());
        ospfNbr.setNeighborId(Ip4Address.valueOf("2.2.2.2"));
        ospfNbr.setState(OspfNeighborState.EXSTART);
        ospfNbr.setRouterPriority(0);
        ospfNbr.setNeighborDr(Ip4Address.valueOf("13.13.13.13"));
        ospfNbr.setDdSeqNum(123);
        ospfInterface.addNeighbouringRouter(ospfNbr);
        ddPacket.setIsMore(1);
        ddPacket.setIsInitialize(0);
        ddPacket.setIsMaster(0);
        ddPacket.setSequenceNo(123);
        ospfInterface.addNeighbouringRouter(ospfNbr);
        channelHandlerContext = null;
        channelHandlerContext = EasyMock.createMock(ChannelHandlerContext.class);
        ospfInterface.processDdMessage(message, channelHandlerContext);
        assertThat(ospfInterfaceChannelHandler, is(notNullValue()));
    }

    /**
     * Tests processDdMessage() method.
     */
    @Test(expected = Exception.class)
    public void testProcessDdMessage1() throws Exception {
        ospfInterface.setIpAddress(Ip4Address.valueOf("11.11.11.11"));
        ospfInterface.setInterfaceType(2);
        ospfInterface.setIpNetworkMask(Ip4Address.valueOf("255.255.255.255"));
        ospfInterface.setHelloIntervalTime(10);
        ospfInterface.setRouterDeadIntervalTime(10);
        ospfArea.setAreaId(Ip4Address.valueOf("12.12.12.12"));
        channelHandlerContext = EasyMock.createMock(ChannelHandlerContext.class);
        OspfMessage message;
        ddPacket = new DdPacket();
        ddPacket.setSourceIp(Ip4Address.valueOf("1.1.1.1"));
        ddPacket.setOspfVer(2);
        ddPacket.setAreaId(Ip4Address.valueOf("12.12.12.12"));
        ddPacket.setRouterId(Ip4Address.valueOf("2.2.2.2"));
        ddPacket.setIsOpaqueCapable(true);
        ddPacket.setIsMore(1);
        ddPacket.setIsInitialize(1);
        ddPacket.setIsMaster(1);
        ddPacket.setSequenceNo(123);
        message = ddPacket;
        ospfNbrHashMap = new HashMap();
        ospfNbr = new OspfNbrImpl(ospfArea, ospfInterface, Ip4Address.valueOf("10.10.10.10"),
                                  Ip4Address.valueOf("2.2.2.2"), 2,
                                  topologyForDeviceAndLink);
        ospfNbr.setLastDdPacket(createDdPacket());
        ospfNbr.setNeighborId(Ip4Address.valueOf("2.2.2.2"));
        ospfNbr.setState(OspfNeighborState.EXCHANGE);
        ospfNbr.setRouterPriority(0);
        ospfNbr.setNeighborDr(Ip4Address.valueOf("13.13.13.13"));
        ospfNbr.setDdSeqNum(123);
        ospfInterface.addNeighbouringRouter(ospfNbr);
        ddPacket.setIsMore(1);
        ddPacket.setIsInitialize(0);
        ddPacket.setIsMaster(0);
        ddPacket.setSequenceNo(123);
        ospfInterface.addNeighbouringRouter(ospfNbr);
        channelHandlerContext = null;
        channelHandlerContext = EasyMock.createMock(ChannelHandlerContext.class);
        ospfInterface.processDdMessage(message, channelHandlerContext);
        assertThat(ospfInterfaceChannelHandler, is(notNullValue()));

    }

    /**
     * Tests processDdMessage() method.
     */
    @Test(expected = Exception.class)
    public void testProcessDdMessage2() throws Exception {
        ospfInterface.setIpAddress(Ip4Address.valueOf("11.11.11.11"));
        ospfInterface.setInterfaceType(2);
        ospfInterface.setIpNetworkMask(Ip4Address.valueOf("255.255.255.255"));
        ospfInterface.setHelloIntervalTime(10);
        ospfInterface.setRouterDeadIntervalTime(10);
        ospfArea.setAreaId(Ip4Address.valueOf("12.12.12.12"));
        channelHandlerContext = EasyMock.createMock(ChannelHandlerContext.class);
        OspfMessage message;
        ddPacket = new DdPacket();
        ddPacket.setSourceIp(Ip4Address.valueOf("1.1.1.1"));
        ddPacket.setOspfVer(2);
        ddPacket.setAreaId(Ip4Address.valueOf("12.12.12.12"));
        ddPacket.setRouterId(Ip4Address.valueOf("2.2.2.2"));
        ddPacket.setIsOpaqueCapable(true);
        ddPacket.setIsMore(1);
        ddPacket.setIsInitialize(1);
        ddPacket.setIsMaster(1);
        ddPacket.setSequenceNo(123);
        message = ddPacket;
        ospfNbrHashMap = new HashMap();
        ospfNbr = new OspfNbrImpl(ospfArea, ospfInterface, Ip4Address.valueOf("10.10.10.10"),
                                  Ip4Address.valueOf("2.2.2.2"), 2,
                                  topologyForDeviceAndLink);
        ospfNbr.setLastDdPacket(createDdPacket());
        ospfNbr.setNeighborId(Ip4Address.valueOf("2.2.2.2"));
        ospfNbr.setState(OspfNeighborState.LOADING);
        ospfNbr.setRouterPriority(0);
        ospfNbr.setNeighborDr(Ip4Address.valueOf("13.13.13.13"));
        ospfNbr.setDdSeqNum(123);
        ospfInterface.addNeighbouringRouter(ospfNbr);
        ospfInterface.setState(OspfInterfaceState.POINT2POINT);
        channelHandlerContext = null;
        channelHandlerContext = EasyMock.createMock(ChannelHandlerContext.class);
        ospfInterface.processDdMessage(message, channelHandlerContext);
        assertThat(ospfInterfaceChannelHandler, is(notNullValue()));

    }

    /**
     * Tests processDdMessage() method.
     */
    @Test(expected = Exception.class)
    public void testProcessDdMessage4() throws Exception {
        ospfInterface.setIpAddress(Ip4Address.valueOf("11.11.11.11"));
        ospfInterface.setInterfaceType(2);
        ospfInterface.setIpNetworkMask(Ip4Address.valueOf("255.255.255.255"));
        ospfInterface.setHelloIntervalTime(10);
        ospfInterface.setRouterDeadIntervalTime(10);
        ospfArea.setAreaId(Ip4Address.valueOf("12.12.12.12"));
        channelHandlerContext = EasyMock.createMock(ChannelHandlerContext.class);
        OspfMessage message;
        ddPacket = new DdPacket();
        ddPacket.setSourceIp(Ip4Address.valueOf("1.1.1.1"));
        ddPacket.setOspfVer(2);
        ddPacket.setAreaId(Ip4Address.valueOf("12.12.12.12"));
        ddPacket.setRouterId(Ip4Address.valueOf("2.2.2.2"));
        ddPacket.setIsOpaqueCapable(true);
        ddPacket.setIsMore(1);
        ddPacket.setIsInitialize(1);
        ddPacket.setIsMaster(1);
        ddPacket.setSequenceNo(123);
        message = ddPacket;
        ospfNbrHashMap = new HashMap();
        ospfNbr = new OspfNbrImpl(ospfArea, ospfInterface, Ip4Address.valueOf("10.10.10.10"),
                                  Ip4Address.valueOf("2.2.2.2"), 2,
                                  topologyForDeviceAndLink);
        ospfNbr.setLastDdPacket(createDdPacket());
        ospfNbr.setNeighborId(Ip4Address.valueOf("2.2.2.2"));
        ospfNbr.setState(OspfNeighborState.EXCHANGE);
        ospfNbr.setRouterPriority(0);
        ospfNbr.setNeighborDr(Ip4Address.valueOf("13.13.13.13"));
        ospfNbr.setDdSeqNum(123);
        ospfInterface.addNeighbouringRouter(ospfNbr);
        ospfInterface.setState(OspfInterfaceState.POINT2POINT);
        channelHandlerContext = null;
        channelHandlerContext = EasyMock.createMock(ChannelHandlerContext.class);
        ddPacket.setIsMore(1);
        ddPacket.setIsInitialize(0);
        ddPacket.setIsMaster(1);
        ddPacket.setSequenceNo(123);
        ospfInterface.addNeighbouringRouter(ospfNbr);
        ospfNbr.setState(OspfNeighborState.EXCHANGE);
        ospfInterface.addNeighbouringRouter(ospfNbr);
        channelHandlerContext = null;
        channelHandlerContext = EasyMock.createMock(ChannelHandlerContext.class);
        ospfInterface.processDdMessage(message, channelHandlerContext);
        assertThat(ospfInterfaceChannelHandler, is(notNullValue()));

    }

    /**
     * Tests processDdMessage() method.
     */
    @Test(expected = Exception.class)
    public void testProcessDdMessage5() throws Exception {
        ospfInterface.setIpAddress(Ip4Address.valueOf("11.11.11.11"));
        ospfInterface.setInterfaceType(2);
        ospfInterface.setIpNetworkMask(Ip4Address.valueOf("255.255.255.255"));
        ospfInterface.setHelloIntervalTime(10);
        ospfInterface.setRouterDeadIntervalTime(10);
        ospfArea.setAreaId(Ip4Address.valueOf("12.12.12.12"));
        channelHandlerContext = EasyMock.createMock(ChannelHandlerContext.class);
        OspfMessage message;
        ddPacket = new DdPacket();
        ddPacket.setSourceIp(Ip4Address.valueOf("1.1.1.1"));
        ddPacket.setOspfVer(2);
        ddPacket.setAreaId(Ip4Address.valueOf("12.12.12.12"));
        ddPacket.setRouterId(Ip4Address.valueOf("2.2.2.2"));
        ddPacket.setIsOpaqueCapable(true);
        ddPacket.setIsMore(1);
        ddPacket.setIsInitialize(1);
        ddPacket.setIsMaster(1);
        ddPacket.setSequenceNo(123);
        message = ddPacket;
        ospfNbrHashMap = new HashMap();
        ospfNbr = new OspfNbrImpl(ospfArea, ospfInterface, Ip4Address.valueOf("10.10.10.10"),
                                  Ip4Address.valueOf("2.2.2.2"), 2,
                                  topologyForDeviceAndLink);
        ospfNbr.setLastDdPacket(createDdPacket());
        ospfNbr.setNeighborId(Ip4Address.valueOf("2.2.2.2"));
        ospfNbr.setState(OspfNeighborState.EXCHANGE);
        ospfNbr.setRouterPriority(0);
        ospfNbr.setNeighborDr(Ip4Address.valueOf("13.13.13.13"));
        ospfNbr.setDdSeqNum(123);
        ospfInterface.addNeighbouringRouter(ospfNbr);
        ospfInterface.setState(OspfInterfaceState.POINT2POINT);
        channelHandlerContext = null;
        channelHandlerContext = EasyMock.createMock(ChannelHandlerContext.class);
        ddPacket.setIsMore(1);
        ddPacket.setIsInitialize(1);
        ddPacket.setIsMaster(0);
        ddPacket.setSequenceNo(123);
        ospfInterface.addNeighbouringRouter(ospfNbr);
        ospfNbr.setState(OspfNeighborState.EXCHANGE);
        ospfInterface.addNeighbouringRouter(ospfNbr);
        channelHandlerContext = null;
        channelHandlerContext = EasyMock.createMock(ChannelHandlerContext.class);
        ospfInterface.processDdMessage(message, channelHandlerContext);
        assertThat(ospfInterfaceChannelHandler, is(notNullValue()));

    }

    /**
     * Tests processDdMessage() method.
     */
    @Test(expected = Exception.class)
    public void testProcessDdMessage6() throws Exception {
        ospfInterface.setIpAddress(Ip4Address.valueOf("11.11.11.11"));
        ospfInterface.setInterfaceType(2);
        ospfInterface.setIpNetworkMask(Ip4Address.valueOf("255.255.255.255"));
        ospfInterface.setHelloIntervalTime(10);
        ospfInterface.setRouterDeadIntervalTime(10);
        ospfArea.setAreaId(Ip4Address.valueOf("12.12.12.12"));
        channelHandlerContext = EasyMock.createMock(ChannelHandlerContext.class);
        OspfMessage message;
        ddPacket = new DdPacket();
        ddPacket.setSourceIp(Ip4Address.valueOf("1.1.1.1"));
        ddPacket.setOspfVer(2);
        ddPacket.setAreaId(Ip4Address.valueOf("12.12.12.12"));
        ddPacket.setRouterId(Ip4Address.valueOf("2.2.2.2"));
        ddPacket.setIsOpaqueCapable(true);
        ddPacket.setIsMore(1);
        ddPacket.setIsInitialize(1);
        ddPacket.setIsMaster(1);
        ddPacket.setSequenceNo(123);
        message = ddPacket;
        ospfNbrHashMap = new HashMap();
        ospfNbr = new OspfNbrImpl(ospfArea, ospfInterface, Ip4Address.valueOf("10.10.10.10"),
                                  Ip4Address.valueOf("2.2.2.2"), 2,
                                  topologyForDeviceAndLink);
        ospfNbr.setLastDdPacket(createDdPacket());
        ospfNbr.setNeighborId(Ip4Address.valueOf("2.2.2.2"));
        ospfNbr.setState(OspfNeighborState.FULL);
        ospfNbr.setRouterPriority(0);
        ospfNbr.setNeighborDr(Ip4Address.valueOf("13.13.13.13"));
        ospfNbr.setDdSeqNum(123);
        ospfInterface.addNeighbouringRouter(ospfNbr);
        ospfInterface.setState(OspfInterfaceState.POINT2POINT);
        channelHandlerContext = null;
        channelHandlerContext = EasyMock.createMock(ChannelHandlerContext.class);
        ddPacket.setIsMore(1);
        ddPacket.setIsInitialize(1);
        ddPacket.setIsMaster(0);
        ddPacket.setSequenceNo(123);
        ospfInterface.addNeighbouringRouter(ospfNbr);
        ospfNbr.setState(OspfNeighborState.FULL);
        ospfInterface.addNeighbouringRouter(ospfNbr);
        channelHandlerContext = null;
        channelHandlerContext = EasyMock.createMock(ChannelHandlerContext.class);
        ospfInterface.processDdMessage(message, channelHandlerContext);
        assertThat(ospfInterfaceChannelHandler, is(notNullValue()));

    }

    /**
     * Tests processLsRequestMessage() method.
     */
    @Test(expected = Exception.class)
    public void testProcessLSRequestMessage() throws Exception {
        ospfArea.setRouterId(Ip4Address.valueOf("11.11.11.11"));
        ospfArea.setAreaId(Ip4Address.valueOf("12.12.12.12"));
        ospfInterface.setOspfArea(ospfArea);
        ospfInterface.setIpAddress(Ip4Address.valueOf("11.11.11.11"));
        ospfInterface.setInterfaceType(2);
        ospfInterface.setIpNetworkMask(Ip4Address.valueOf("255.255.255.255"));
        ospfInterface.setHelloIntervalTime(10);
        ospfInterface.setRouterDeadIntervalTime(10);
        channelHandlerContext = EasyMock.createMock(ChannelHandlerContext.class);
        OspfMessage message;
        lsRequest = new LsRequest();
        lsRequest.setSourceIp(Ip4Address.valueOf("1.1.1.1"));
        lsRequest.setOspfVer(2);
        lsRequest.setAreaId(Ip4Address.valueOf("12.12.12.12"));
        lsRequest.setRouterId(Ip4Address.valueOf("10.226.165.100"));
        List<LsRequestPacket> lsRequests = new ArrayList();
        LsRequestPacket lsRequestPacket = new LsRequestPacket();
        lsRequestPacket.setLsType(OspfLsaType.AREA_LOCAL_OPAQUE_LSA.value());
        lsRequestPacket.setLinkStateId("2.2.2.2");
        lsRequestPacket.setOwnRouterId("10.226.165.100");
        lsRequests.add(lsRequestPacket);
        lsRequests.add(lsRequestPacket);
        lsRequest.addLinkStateRequests(lsRequestPacket);
        message = lsRequest;
        ospfNbrHashMap = new HashMap();
        ospfNbr.setState(OspfNeighborState.EXCHANGE);
        ospfNbr = new OspfNbrImpl(ospfArea, createOspfInterface(), Ip4Address.valueOf("10.10.10.10"),
                                  Ip4Address.valueOf("10.226.165.100"), 2,
                                  topologyForDeviceAndLink);
        ospfNbr.setLastDdPacket(createDdPacket());
        ospfNbr.setNeighborId(Ip4Address.valueOf("10.226.165.100"));
        ospfNbr.setState(OspfNeighborState.FULL);
        ospfNbr.setRouterPriority(0);
        ospfNbr.setNeighborDr(Ip4Address.valueOf("13.13.13.13"));
        ospfNbr.setDdSeqNum(123);
        ospfInterface.addNeighbouringRouter(ospfNbr);
        OpaqueLsaHeader lsaHeader = new OpaqueLsaHeader();
        lsaHeader.setLsType(OspfLsaType.AREA_LOCAL_OPAQUE_LSA.value());
        lsaHeader.setLinkStateId("2.2.2.2");
        lsaHeader.setAdvertisingRouter(Ip4Address.valueOf("10.226.165.100"));
        OpaqueLsa10 opaqueLsa10 = new OpaqueLsa10(lsaHeader);
        ospfArea.addLsa(opaqueLsa10, false, ospfInterface);
        ospfInterface.setOspfArea(ospfArea);
        ospfInterface.processLsRequestMessage(message, channelHandlerContext);
        assertThat(ospfInterface, is(notNullValue()));
    }

    /**
     * Tests processLsUpdateMessage() method.
     */
    @Test
    public void testProcessLSUpdateMessage() throws Exception {
        ospfInterface.setIpAddress(Ip4Address.valueOf("11.11.11.11"));
        ospfInterface.setInterfaceType(2);
        ospfInterface.setIpNetworkMask(Ip4Address.valueOf("255.255.255.255"));
        ospfInterface.setHelloIntervalTime(10);
        ospfInterface.setRouterDeadIntervalTime(10);
        ospfArea.setAreaId(Ip4Address.valueOf("12.12.12.12"));
        channelHandlerContext = EasyMock.createMock(ChannelHandlerContext.class);
        OspfMessage message;
        lsUpdate = new LsUpdate();
        lsUpdate.setSourceIp(Ip4Address.valueOf("1.1.1.1"));
        lsUpdate.setOspfVer(2);
        lsUpdate.setAreaId(Ip4Address.valueOf("12.12.12.12"));
        lsUpdate.setRouterId(Ip4Address.valueOf("10.226.165.100"));
        RouterLsa routerLsa = new RouterLsa();
        lsUpdate.addLsa(routerLsa);
        lsUpdate.setNumberOfLsa(1);
        message = lsUpdate;
        ospfNbrHashMap = new HashMap();
        ospfNbr.setState(OspfNeighborState.FULL);
        ospfNbr = new OspfNbrImpl(ospfArea, createOspfInterface(), Ip4Address.valueOf("10.10.10.10"),
                                  Ip4Address.valueOf("10.226.165.100"), 2,
                                  topologyForDeviceAndLink);
        ospfNbr.setLastDdPacket(createDdPacket());
        ospfNbr.setNeighborId(Ip4Address.valueOf("2.2.2.2"));
        ospfNbr.setState(OspfNeighborState.FULL);
        ospfNbr.setRouterPriority(0);
        ospfNbr.setNeighborDr(Ip4Address.valueOf("13.13.13.13"));
        ospfNbr.setDdSeqNum(123);
        ospfInterface.addNeighbouringRouter(ospfNbr);
        channelHandlerContext = null;
        channelHandlerContext = EasyMock.createMock(ChannelHandlerContext.class);
        ospfInterface.processLsUpdateMessage(message, channelHandlerContext);
        assertThat(ospfInterfaceChannelHandler, is(notNullValue()));

    }

    @Test(expected = Exception.class)
    public void testProcessLSAckMessage() throws Exception {
        ospfInterface.setIpAddress(Ip4Address.valueOf("11.11.11.11"));
        ospfInterface.setInterfaceType(2);
        ospfInterface.setIpNetworkMask(Ip4Address.valueOf("255.255.255.255"));
        ospfInterface.setHelloIntervalTime(10);
        ospfInterface.setRouterDeadIntervalTime(10);
        ospfArea.setAreaId(Ip4Address.valueOf("12.12.12.12"));
        channelHandlerContext = EasyMock.createMock(ChannelHandlerContext.class);
        OspfMessage message;
        lsAck = new LsAcknowledge();
        lsAck.setSourceIp(Ip4Address.valueOf("1.1.1.1"));
        lsAck.setOspfVer(2);
        lsAck.setAreaId(Ip4Address.valueOf("12.12.12.12"));
        LsaHeader lsaHeader = new LsaHeader();
        lsAck.addLinkStateHeader(lsaHeader);
        message = lsAck;
        ospfNbr = new OspfNbrImpl(ospfArea, createOspfInterface(), Ip4Address.valueOf("10.10.10.10"),
                                  Ip4Address.valueOf("10.226.165.100"), 2,
                                  topologyForDeviceAndLink);
        ospfNbr.setLastDdPacket(createDdPacket());
        ospfNbr.setNeighborId(Ip4Address.valueOf("2.2.2.2"));
        ospfNbr.setState(OspfNeighborState.FULL);
        ospfNbr.setRouterPriority(0);
        ospfNbr.setNeighborDr(Ip4Address.valueOf("13.13.13.13"));
        ospfNbr.setDdSeqNum(123);
        channelHandlerContext = null;
        channelHandlerContext = EasyMock.createMock(ChannelHandlerContext.class);
        ospfInterface.processLsAckMessage(message, channelHandlerContext);
        assertThat(ospfInterfaceChannelHandler, is(notNullValue()));

    }

    /**
     * Tests electRouter() method.
     */
    @Test
    public void testElectRouter() throws Exception {
        ospfInterface.setOspfArea(ospfArea);
        ospfInterface.setDr(Ip4Address.valueOf("3.3.3.3"));
        ospfInterface.setBdr(Ip4Address.valueOf("3.3.3.3"));
        ospfInterface.setIpNetworkMask(Ip4Address.valueOf("255.255.255.255"));
        ChannelConfig channelConfig = EasyMock.createMock(ChannelConfig.class);
        EasyMock.expect(channelConfig.getBufferFactory()).andReturn(new HeapChannelBufferFactory());
        Channel channel = EasyMock.createMock(Channel.class);
        ospfInterface.electRouter(channel);
        assertThat(ospfInterface.dr(), is(notNullValue()));
    }

    /**
     * Tests electBdr() method.
     */
    @Test
    public void testElectBdr() throws Exception {
        ospfEligibleRouter = new OspfEligibleRouter();
        ospfEligibleRouter.setIpAddress(Ip4Address.valueOf("1.1.1.1"));
        ospfEligibleRouter.setIsDr(true);
        ospfEligibleRouter.setRouterPriority(10);
        ospfEligibleRouter.setRouterId(Ip4Address.valueOf("1.1.1.1"));
        ospfEligibleRouter.setIsBdr(false);
        OspfEligibleRouter ospfEligibleRouter1 = new OspfEligibleRouter();
        ospfEligibleRouter.setIpAddress(Ip4Address.valueOf("1.1.1.1"));
        ospfEligibleRouter.setIsDr(true);
        ospfEligibleRouter.setRouterPriority(10);
        ospfEligibleRouter.setRouterId(Ip4Address.valueOf("1.1.1.1"));
        ospfEligibleRouter.setIsBdr(false);
        OspfEligibleRouter ospfEligibleRouter2 = new OspfEligibleRouter();
        ospfEligibleRouter.setIpAddress(Ip4Address.valueOf("1.1.1.1"));
        ospfEligibleRouter.setIsDr(true);
        ospfEligibleRouter.setRouterPriority(10);
        ospfEligibleRouter.setRouterId(Ip4Address.valueOf("1.1.1.1"));
        ospfEligibleRouter.setIsBdr(false);
        List<OspfEligibleRouter> ospfEligibleRouters = new ArrayList<>();

        ospfEligibleRouters.add(ospfEligibleRouter);
        ospfEligibleRouters.add(ospfEligibleRouter1);
        ospfEligibleRouters.add(ospfEligibleRouter2);
        OspfEligibleRouter eligibleRouter = ospfInterface.electBdr(ospfEligibleRouters);
        assertThat(ospfEligibleRouters.size(), is(3));
        assertThat(eligibleRouter, is(notNullValue()));
    }

    /**
     * Tests electDr() method.
     */
    @Test
    public void testElectDR() throws Exception {
        ospfEligibleRouter = new OspfEligibleRouter();
        ospfEligibleRouter.setIpAddress(Ip4Address.valueOf("1.1.1.1"));
        ospfEligibleRouter.setIsDr(true);
        ospfEligibleRouter.setRouterPriority(10);
        ospfEligibleRouter.setRouterId(Ip4Address.valueOf("1.1.1.1"));
        ospfEligibleRouter.setIsBdr(false);
        OspfEligibleRouter ospfEligibleRouter1 = new OspfEligibleRouter();
        ospfEligibleRouter.setIpAddress(Ip4Address.valueOf("1.1.1.1"));
        ospfEligibleRouter.setIsDr(true);
        ospfEligibleRouter.setRouterPriority(10);
        ospfEligibleRouter.setRouterId(Ip4Address.valueOf("1.1.1.1"));
        ospfEligibleRouter.setIsBdr(false);
        OspfEligibleRouter ospfEligibleRouter2 = new OspfEligibleRouter();
        ospfEligibleRouter.setIpAddress(Ip4Address.valueOf("1.1.1.1"));
        ospfEligibleRouter.setIsDr(true);
        ospfEligibleRouter.setRouterPriority(10);
        ospfEligibleRouter.setRouterId(Ip4Address.valueOf("1.1.1.1"));
        ospfEligibleRouter.setIsBdr(false);
        List<OspfEligibleRouter> ospfEligibleRouters = new ArrayList<>();
        ospfEligibleRouters.add(ospfEligibleRouter);
        ospfEligibleRouters.add(ospfEligibleRouter1);
        ospfEligibleRouters.add(ospfEligibleRouter2);
        OspfEligibleRouter eligibleRouter = ospfInterface.electDr(ospfEligibleRouters,
                                                                  ospfEligibleRouter);
        assertThat(ospfEligibleRouters.size(), is(3));
        assertThat(eligibleRouter, is(notNullValue()));
    }

    /**
     * Tests selectRouterBasedOnPriority() method.
     */
    @Test
    public void testSelectRouterBasedOnPriority() throws Exception {
        ospfEligibleRouter = new OspfEligibleRouter();
        ospfEligibleRouter.setIpAddress(Ip4Address.valueOf("1.1.1.1"));
        ospfEligibleRouter.setIsDr(true);
        ospfEligibleRouter.setRouterPriority(10);
        ospfEligibleRouter.setRouterId(Ip4Address.valueOf("1.1.1.1"));
        ospfEligibleRouter.setIsBdr(false);
        OspfEligibleRouter ospfEligibleRouter1 = new OspfEligibleRouter();
        ospfEligibleRouter.setIpAddress(Ip4Address.valueOf("1.1.1.1"));
        ospfEligibleRouter.setIsDr(true);
        ospfEligibleRouter.setRouterPriority(11);
        ospfEligibleRouter.setRouterId(Ip4Address.valueOf("1.1.1.1"));
        ospfEligibleRouter.setIsBdr(false);
        OspfEligibleRouter ospfEligibleRouter2 = new OspfEligibleRouter();
        ospfEligibleRouter.setIpAddress(Ip4Address.valueOf("1.1.1.1"));
        ospfEligibleRouter.setIsDr(true);
        ospfEligibleRouter.setRouterPriority(12);
        ospfEligibleRouter.setRouterId(Ip4Address.valueOf("1.1.1.1"));
        ospfEligibleRouter.setIsBdr(false);
        List<OspfEligibleRouter> ospfEligibleRouters = new ArrayList<>();
        ospfEligibleRouters.add(ospfEligibleRouter);
        ospfEligibleRouters.add(ospfEligibleRouter1);
        ospfEligibleRouters.add(ospfEligibleRouter2);
        OspfEligibleRouter eligibleRouter = ospfInterface.selectRouterBasedOnPriority(
                ospfEligibleRouters);
        assertThat(eligibleRouter, is(notNullValue()));
    }

    /**
     * Tests addDeviceInformation() method.
     */
    @Test(expected = Exception.class)
    public void testAddDeviceInformation() throws Exception {
        ospfNbr = new OspfNbrImpl(ospfArea, createOspfInterface(), Ip4Address.valueOf("10.10.10.10"),
                                  Ip4Address.valueOf("10.226.165.100"), 2,
                                  topologyForDeviceAndLink);

        ospfInterface.addDeviceInformation(new OspfRouterImpl());
        assertThat(ospfInterfaceChannelHandler, is(notNullValue()));
    }

    /**
     * Tests removeDeviceInformation() method.
     */
    @Test(expected = Exception.class)
    public void testRemoveDeviceInformation() throws Exception {
        ospfNbr = new OspfNbrImpl(ospfArea, createOspfInterface(), Ip4Address.valueOf("10.10.10.10"),
                                  Ip4Address.valueOf("10.226.165.100"), 2,
                                  topologyForDeviceAndLink);

        ospfInterface.removeDeviceInformation(new OspfRouterImpl());
        assertThat(ospfInterfaceChannelHandler, is(notNullValue()));
    }

    /**
     * Tests addLinkInformation() method.
     */
    @Test(expected = Exception.class)
    public void testaddLinkInformation() throws Exception {
        ospfNbr = new OspfNbrImpl(ospfArea, createOspfInterface(), Ip4Address.valueOf("10.10.10.10"),
                                  Ip4Address.valueOf("10.226.165.100"), 2,
                                  topologyForDeviceAndLink);

        List topTlv = new ArrayList();
        topTlv.add(new RouterTlv(new TlvHeader()));
        ospfInterface.addLinkInformation(new OspfRouterImpl(), new OspfLinkTedImpl());
        assertThat(ospfInterfaceChannelHandler, is(notNullValue()));
    }

    /**
     * Tests removeLinkInformation() method.
     */
    @Test(expected = Exception.class)
    public void testRemoveLinkInformation() throws Exception {
        ospfNbr = new OspfNbrImpl(ospfArea, createOspfInterface(), Ip4Address.valueOf("10.10.10.10"),
                                  Ip4Address.valueOf("10.226.165.100"), 2,
                                  topologyForDeviceAndLink);

        ospfInterface.removeLinkInformation(new OspfRouterImpl(), new OspfLinkTedImpl());
        assertThat(ospfInterfaceChannelHandler, is(notNullValue()));
    }

    /**
     * Utility for test method.
     */
    private DdPacket createDdPacket() throws OspfParseException {
        byte[] ddPacket = {2, 2, 0, 32, -64, -88, -86, 8, 0, 0, 0, 1, -96, 82,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 5, -36, 2, 7, 65, 119, -87, 126};
        DdPacket ddPacket1 = new DdPacket();
        ChannelBuffer buf = ChannelBuffers.buffer(ddPacket.length);
        buf.writeBytes(ddPacket);
        ddPacket1.readFrom(buf);
        return ddPacket1;
    }

    /**
     * Utility for test method.
     */
    private OspfInterfaceImpl createOspfInterface() throws UnknownHostException {
        ospfInterface = new OspfInterfaceImpl();
        OspfAreaImpl ospfArea = new OspfAreaImpl();
        OspfInterfaceChannelHandler ospfInterfaceChannelHandler = EasyMock.createMock(
                OspfInterfaceChannelHandler.class);
        ospfNbr = new OspfNbrImpl(ospfArea, ospfInterface, Ip4Address.valueOf("10.226.165.164"),
                                  Ip4Address.valueOf("1.1.1.1"), 2,
                                  topologyForDeviceAndLink);
        ospfNbr.setState(OspfNeighborState.EXSTART);
        ospfNbr.setNeighborId(Ip4Address.valueOf("10.226.165.100"));
        this.ospfInterface = new OspfInterfaceImpl();
        this.ospfInterface.setIpAddress(Ip4Address.valueOf("10.226.165.164"));
        this.ospfInterface.setIpNetworkMask(Ip4Address.valueOf("255.255.255.255"));
        this.ospfInterface.setBdr(Ip4Address.valueOf("111.111.111.111"));
        this.ospfInterface.setDr(Ip4Address.valueOf("111.111.111.111"));
        this.ospfInterface.setHelloIntervalTime(20);
        this.ospfInterface.setInterfaceType(2);
        this.ospfInterface.setReTransmitInterval(2000);
        this.ospfInterface.setMtu(6500);
        this.ospfInterface.setRouterDeadIntervalTime(1000);
        this.ospfInterface.setRouterPriority(1);
        this.ospfInterface.setInterfaceType(1);
        this.ospfInterface.addNeighbouringRouter(ospfNbr);
        return this.ospfInterface;
    }

    /**
     * Utility for test method.
     */
    private OspfInterfaceImpl createOspfInterface1() throws UnknownHostException {
        ospfInterface = new OspfInterfaceImpl();
        OspfAreaImpl ospfArea = new OspfAreaImpl();
        OspfInterfaceChannelHandler ospfInterfaceChannelHandler = EasyMock.createMock(
                OspfInterfaceChannelHandler.class);
        ospfNbr = new OspfNbrImpl(ospfArea, ospfInterface, Ip4Address.valueOf("10.226.165.164"),
                                  Ip4Address.valueOf("1.1.1.1"), 2,
                                  topologyForDeviceAndLink);
        ospfNbr.setState(OspfNeighborState.FULL);
        ospfNbr.setNeighborId(Ip4Address.valueOf("10.226.165.100"));
        ospfInterface = new OspfInterfaceImpl();
        ospfInterface.setIpAddress(Ip4Address.valueOf("10.226.165.164"));
        ospfInterface.setIpNetworkMask(Ip4Address.valueOf("255.255.255.255"));
        ospfInterface.setBdr(Ip4Address.valueOf("111.111.111.111"));
        ospfInterface.setDr(Ip4Address.valueOf("111.111.111.111"));
        ospfInterface.setHelloIntervalTime(20);
        ospfInterface.setInterfaceType(2);
        ospfInterface.setReTransmitInterval(2000);
        ospfInterface.setMtu(6500);
        ospfInterface.setRouterDeadIntervalTime(1000);
        ospfInterface.setRouterPriority(1);
        ospfInterface.setInterfaceType(1);
        ospfInterface.addNeighbouringRouter(ospfNbr);
        return ospfInterface;
    }

    /**
     * Utility for test method.
     */
    private OspfAreaImpl createOspfArea() throws UnknownHostException {
        OspfAreaAddressRangeImpl ospfAreaAddressRange;
        ospfAreaAddressRange = createOspfAreaAddressRange();
        addressRanges.add(ospfAreaAddressRange);
        OspfAreaImpl ospfArea = new OspfAreaImpl();
        ospfArea.setAreaId(Ip4Address.valueOf("10.226.165.164"));
        ospfArea.setExternalRoutingCapability(true);
        OspfInterfaceImpl ospfInterface = createOspfInterface();
        ospfInterfaces.add(ospfInterface);
        ospfArea.setOspfInterfaceList(ospfInterfaces);
        RouterLsa routerLsa = new RouterLsa();
        routerLsa.setLsType(1);
        routerLsa.setLinkStateId("2.2.2.2");
        routerLsa.setAdvertisingRouter(Ip4Address.valueOf("2.2.2.2"));
        try {
            ospfArea.addLsa(routerLsa, false, ospfInterface);
        } catch (Exception e) {
            System.out.println("ospfAreaImpl createOspfArea");
        }
        ospfArea.setRouterId(Ip4Address.valueOf("111.111.111.111"));

        return ospfArea;
    }

    /**
     * Utility for test method.
     */
    private OspfAreaAddressRangeImpl createOspfAreaAddressRange() {
        OspfAreaAddressRangeImpl ospfAreaAddressRange = new OspfAreaAddressRangeImpl();
        ospfAreaAddressRange.setIpAddress(Ip4Address.valueOf("10.226.165.164"));
        ospfAreaAddressRange.setAdvertise(true);
        ospfAreaAddressRange.setMask("mask");
        return ospfAreaAddressRange;
    }

}