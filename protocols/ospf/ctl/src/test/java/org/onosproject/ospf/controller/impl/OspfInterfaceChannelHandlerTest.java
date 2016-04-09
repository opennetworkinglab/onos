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
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.buffer.HeapChannelBufferFactory;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelConfig;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.Ip4Address;
import org.onosproject.ospf.controller.OspfAreaAddressRange;
import org.onosproject.ospf.controller.OspfInterface;
import org.onosproject.ospf.controller.OspfNbr;
import org.onosproject.ospf.controller.OspfNeighborState;
import org.onosproject.ospf.controller.TopologyForDeviceAndLink;
import org.onosproject.ospf.controller.area.OspfAreaAddressRangeImpl;
import org.onosproject.ospf.controller.area.OspfAreaImpl;
import org.onosproject.ospf.controller.area.OspfInterfaceImpl;
import org.onosproject.ospf.controller.util.OspfEligibleRouter;
import org.onosproject.ospf.exceptions.OspfParseException;
import org.onosproject.ospf.protocol.lsa.LsaHeader;
import org.onosproject.ospf.protocol.lsa.TlvHeader;
import org.onosproject.ospf.protocol.lsa.tlvtypes.RouterTlv;
import org.onosproject.ospf.protocol.lsa.types.RouterLsa;
import org.onosproject.ospf.protocol.ospfpacket.OspfMessage;
import org.onosproject.ospf.protocol.ospfpacket.subtype.LsRequestPacket;
import org.onosproject.ospf.protocol.ospfpacket.types.DdPacket;
import org.onosproject.ospf.protocol.ospfpacket.types.HelloPacket;
import org.onosproject.ospf.protocol.ospfpacket.types.LsAcknowledge;
import org.onosproject.ospf.protocol.ospfpacket.types.LsRequest;
import org.onosproject.ospf.protocol.ospfpacket.types.LsUpdate;
import org.onosproject.ospf.protocol.util.ChecksumCalculator;
import org.onosproject.ospf.protocol.util.OspfInterfaceState;

import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;


/**
 * Unit test class for OspfInterfaceChannelHandler.
 */
public class OspfInterfaceChannelHandlerTest {

    private List<OspfAreaAddressRange> addressRanges;
    private List<OspfInterface> ospfInterfaces;
    private Controller controller;
    private OspfAreaImpl ospfArea;
    private OspfInterfaceImpl ospfInterface;
    private OspfInterfaceChannelHandler ospfInterfaceChannelHandler;
    private HashMap<String, OspfNbr> ospfNbrHashMap;
    private OspfNbrImpl ospfNbr;
    private Channel channel;
    private ChannelHandlerContext channelHandlerContext;
    private ChannelStateEvent channelStateEvent;
    private HelloPacket helloPacket;
    private DdPacket ddPacket;
    private ChecksumCalculator checksumCalculator;
    private byte[] byteArray;
    private byte[] checkArray;
    private ChannelBuffer buf;
    private OspfEligibleRouter ospfEligibleRouter;
    private LsUpdate lsUpdate;
    private LsAcknowledge lsAck;
    private LsRequest lsRequest;
    private TopologyForDeviceAndLink topologyForDeviceAndLink;

    @Before
    public void setUp() throws Exception {
        addressRanges = new ArrayList();
        ospfInterfaces = new ArrayList<>();
        ospfArea = createOspfArea();
        ospfInterface = createOspfInterface();
        ospfNbrHashMap = new HashMap();
        topologyForDeviceAndLink = new TopologyForDeviceAndLinkImpl();
        ospfNbr = new OspfNbrImpl(ospfArea, ospfInterface, Ip4Address.valueOf("10.10.10.10"),
                                  Ip4Address.valueOf("2.2.2.2"), 2,
                                  new OspfInterfaceChannelHandler(new Controller(), ospfArea,
                                                                  ospfInterface),
                                  topologyForDeviceAndLink);
        ospfNbr.setNeighborId(Ip4Address.valueOf("10.10.10.10"));
        ospfNbr.setRouterPriority(0);
        ospfNbr.setNeighborDr(Ip4Address.valueOf("13.13.13.13"));
        ospfInterface.addNeighbouringRouter(ospfNbr);
        controller = new Controller();
        ospfInterfaceChannelHandler = new OspfInterfaceChannelHandler();
        ospfInterfaceChannelHandler = new OspfInterfaceChannelHandler(controller, ospfArea,
                                                                      ospfInterface);
    }

    @After
    public void tearDown() throws Exception {
        ospfInterfaceChannelHandler = null;
        addressRanges = null;
        ospfInterfaces = null;
        controller = null;
        ospfArea = null;
        ospfInterfaceChannelHandler = null;
        ospfInterface = null;
        ospfNbrHashMap = null;
        channel = null;
        channelHandlerContext = null;
        channelStateEvent = null;
        helloPacket = null;
        ddPacket = null;
        checksumCalculator = null;
        byteArray = null;
        checkArray = null;
        ospfEligibleRouter = null;
        lsUpdate = null;
        lsAck = null;
        lsRequest = null;
    }

    /**
     * Tests interfaceUp() method.
     */
    @Test
    public void testInterfaceUp() throws Exception {
        ospfInterface.setInterfaceType(2);
        ospfInterface.setRouterPriority(0);
        ospfInterfaceChannelHandler.interfaceUp();
        assertThat(ospfInterfaceChannelHandler, is(notNullValue()));
    }

    /**
     * Tests interfaceUp() method.
     */
    @Test
    public void testInterfaceUp1() throws Exception {

        ospfInterface.setInterfaceType(2);
        ospfInterface.setRouterPriority(0);
        ospfInterfaceChannelHandler.interfaceUp();
        assertThat(ospfInterfaceChannelHandler, is(notNullValue()));
    }

    /**
     * Tests interfaceUp() method.
     */
    @Test
    public void testInterfaceUp2() throws Exception {
        ospfInterface.setInterfaceType(1);
        ospfInterface.setRouterPriority(1);
        ospfInterfaceChannelHandler.interfaceUp();
        assertThat(ospfInterfaceChannelHandler, is(notNullValue()));
    }

    /**
     * Tests interfaceUp() method.
     */
    @Test
    public void testInterfaceUp3() throws Exception {
        ospfInterface.setInterfaceType(2);
        ospfInterface.setRouterPriority(1);
        ospfInterfaceChannelHandler.interfaceUp();
        assertThat(ospfInterfaceChannelHandler, is(notNullValue()));
    }

    /**
     * Tests backupSeen() method.
     */
    @Test
    public void testBackupSeen() throws Exception {
        channel = EasyMock.createMock(Channel.class);
        ospfInterface.setState(OspfInterfaceState.WAITING);
        ospfInterfaceChannelHandler.backupSeen(channel);
        assertThat(ospfInterface.dr(), is(notNullValue()));
    }

    /**
     * Tests waitTimer() method.
     */
    @Test
    public void testWaitTimer() throws Exception {
        channel = EasyMock.createMock(Channel.class);
        ospfInterface.setState(OspfInterfaceState.WAITING);
        ospfInterface.setIpAddress(Ip4Address.valueOf("10.10.10.10"));
        ospfInterface.setIpNetworkMask(Ip4Address.valueOf("255.255.255.255"));
        ospfInterfaceChannelHandler.waitTimer(channel);
        assertThat(ospfInterface.dr(), is(notNullValue()));
    }

    /**
     * Tests neighborChange() method.
     */
    @Test
    public void testNeighborChange() throws Exception {
        ospfNbrHashMap = new HashMap();
        ospfNbr = new OspfNbrImpl(new OspfAreaImpl(), new OspfInterfaceImpl(),
                                  Ip4Address.valueOf("1.1.1.1"), Ip4Address.valueOf("2.2.2.2"), 2,
                                  new OspfInterfaceChannelHandler(new Controller(),
                                                                  new OspfAreaImpl(),
                                                                  new OspfInterfaceImpl()),
                                  topologyForDeviceAndLink);
        ospfNbr.setNeighborId(Ip4Address.valueOf("111.111.111.111"));
        ospfNbrHashMap.put("111.111.111.111", ospfNbr);
        ospfNbr.setState(OspfNeighborState.EXCHANGE);
        ospfInterface.setListOfNeighbors(ospfNbrHashMap);
        ospfInterface.setIpAddress(Ip4Address.valueOf("10.10.10.10"));
        ospfInterface.setIpNetworkMask(Ip4Address.valueOf("255.255.255.255"));
        ospfInterface.setIpAddress(Ip4Address.valueOf("10.10.10.10"));
        ospfInterface.setIpNetworkMask(Ip4Address.valueOf("255.255.255.255"));
        channel = EasyMock.createMock(Channel.class);
        ospfInterface.setState(OspfInterfaceState.DR);
        ospfInterfaceChannelHandler.waitTimer(channel);
        assertThat(ospfInterface.dr(), is(Ip4Address.valueOf("0.0.0.0")));
    }

    /**
     * Tests interfaceDown() method.
     */
    @Test(expected = Exception.class)
    public void testInterfaceDown() throws Exception {
        ospfInterfaceChannelHandler.interfaceDown();
        assertThat(ospfInterface.state(), is(OspfInterfaceState.DOWN));
    }

    /**
     * Tests channelConnected() method.
     */
    @Test(expected = Exception.class)
    public void testChannelConnected() throws Exception {
        channelHandlerContext = EasyMock.createMock(ChannelHandlerContext.class);
        channelStateEvent = EasyMock.createMock(ChannelStateEvent.class);
        ospfInterfaceChannelHandler.channelConnected(channelHandlerContext, channelStateEvent);
        assertThat(ospfInterface.state(), is(notNullValue()));
    }

    /**
     * Tests exceptionCaught() method.
     */
    @Test(expected = Exception.class)
    public void testExceptionCaught() throws Exception {
        channelHandlerContext = EasyMock.createMock(ChannelHandlerContext.class);
        ExceptionEvent exception = EasyMock.createMock(ExceptionEvent.class);
        ospfInterfaceChannelHandler.exceptionCaught(channelHandlerContext, exception);
        assertThat(ospfInterfaceChannelHandler, is(notNullValue()));
    }

    /**
     * Tests channelDisconnected() method.
     */
    @Test(expected = Exception.class)
    public void testChannelDisconnected() throws Exception {
        channelHandlerContext = EasyMock.createMock(ChannelHandlerContext.class);
        channelStateEvent = EasyMock.createMock(ChannelStateEvent.class);

        ospfInterfaceChannelHandler.channelDisconnected(channelHandlerContext, channelStateEvent);

        assertThat(ospfInterface.state(), is(notNullValue()));
    }

    /**
     * Tests messageReceived() method.
     */
    @Test
    public void testMessageReceived() throws Exception {
        ospfInterface.setIpAddress(Ip4Address.valueOf("11.11.11.11"));
        ospfInterface.setInterfaceType(2);
        ospfArea.setAreaId(Ip4Address.valueOf("13.13.13.13"));
        channelHandlerContext = EasyMock.createMock(ChannelHandlerContext.class);
        MessageEvent messageEvent = new MessageEvent() {
            @Override
            public Object getMessage() {
                helloPacket = new HelloPacket();
                helloPacket.setSourceIp(Ip4Address.valueOf("1.1.1.1"));
                helloPacket.setRouterId(Ip4Address.valueOf("10.10.10.10"));
                helloPacket.setOspfVer(2);
                helloPacket.setAreaId(Ip4Address.valueOf("12.12.12.12"));
                helloPacket.setOptions(2);
                helloPacket.setAreaId(Ip4Address.valueOf("5.5.5.5"));
                helloPacket.setNetworkMask(Ip4Address.valueOf("3.3.3.3"));
                helloPacket.setOspftype(1);
                helloPacket.setAuthType(0);
                helloPacket.setAuthentication(0);
                checksumCalculator = new ChecksumCalculator();
                byteArray = helloPacket.asBytes();
                helloPacket.setOspfPacLength(byteArray.length);
                checkArray = checksumCalculator.calculateOspfCheckSum(byteArray, 12, 13);
                buf = ChannelBuffers.copiedBuffer(checkArray);
                helloPacket.setChecksum(buf.readUnsignedShort());
                List<HelloPacket> messPackets = new ArrayList<>();
                messPackets.add(helloPacket);
                return messPackets;
            }

            @Override
            public SocketAddress getRemoteAddress() {
                return null;
            }

            @Override
            public Channel getChannel() {
                return null;
            }

            @Override
            public ChannelFuture getFuture() {
                return null;
            }
        };
        ospfInterfaceChannelHandler.messageReceived(channelHandlerContext, messageEvent);
        assertThat(ospfInterfaceChannelHandler, is(notNullValue()));
    }

    /**
     * Tests processOspfMessage() method.
     */
    @Test
    public void testProcessOspfMessage() throws Exception {
        ospfInterface.setIpAddress(Ip4Address.valueOf("11.11.11.11"));
        ospfInterface.setIpNetworkMask(Ip4Address.valueOf("225.225.225.225"));
        ospfInterface.setInterfaceType(2);
        ospfArea.setAreaId(Ip4Address.valueOf("12.12.12.12"));
        channelHandlerContext = EasyMock.createMock(ChannelHandlerContext.class);
        OspfMessage message;
        helloPacket = new HelloPacket();
        helloPacket.setSourceIp(Ip4Address.valueOf("1.1.1.1"));
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
        checksumCalculator = new ChecksumCalculator();
        byteArray = helloPacket.asBytes();
        helloPacket.setOspfPacLength(byteArray.length);
        checkArray = checksumCalculator.calculateOspfCheckSum(byteArray, 12, 13);
        checkArray[0] = -53;
        checkArray[1] = 37;
        buf = ChannelBuffers.copiedBuffer(checkArray);
        helloPacket.setChecksum(buf.readUnsignedShort());
        message = helloPacket;
        ospfInterfaceChannelHandler.processOspfMessage(message, channelHandlerContext);
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
        message = ddPacket;
        ospfInterfaceChannelHandler.processOspfMessage(message, channelHandlerContext);
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
        checkArray[0] = -33;
        checkArray[1] = -58;
        buf = ChannelBuffers.copiedBuffer(checkArray);
        lsRequest.setChecksum(buf.readUnsignedShort());
        message = lsRequest;
        ospfInterfaceChannelHandler.processOspfMessage(message, channelHandlerContext);
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
        ospfInterfaceChannelHandler.processOspfMessage(message, channelHandlerContext);
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
        ospfInterfaceChannelHandler.processOspfMessage(message, channelHandlerContext);
        assertThat(ospfInterfaceChannelHandler, is(notNullValue()));

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
        ospfInterfaceChannelHandler.processHelloMessage(message, channelHandlerContext);
        assertThat(ospfInterfaceChannelHandler, is(notNullValue()));
    }

    /**
     * Tests processHelloMessage() method.
     */
    @Test
    public void testProcessHelloMessage1() throws Exception {
        ospfInterface.setInterfaceType(2);
        ospfInterface.setRouterPriority(1);
        ospfInterfaceChannelHandler.interfaceUp();
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
        ospfInterfaceChannelHandler.processHelloMessage(message, channelHandlerContext);
        assertThat(ospfInterfaceChannelHandler, is(notNullValue()));
        ospfNbrHashMap = new HashMap();
        ospfNbr = new OspfNbrImpl(ospfArea, ospfInterface, Ip4Address.valueOf("10.10.10.10"),
                                  Ip4Address.valueOf("2.2.2.2"), 2,
                                  new OspfInterfaceChannelHandler(new Controller(), ospfArea,
                                                                  ospfInterface),
                                  topologyForDeviceAndLink);
        ospfNbr.setNeighborId(Ip4Address.valueOf("2.2.2.2"));
        ospfNbr.setRouterPriority(0);
        ospfNbr.setNeighborDr(Ip4Address.valueOf("13.13.13.13"));
        ospfInterface.addNeighbouringRouter(ospfNbr);
        ospfInterfaceChannelHandler.processHelloMessage(message, channelHandlerContext);
        assertThat(ospfInterfaceChannelHandler, is(notNullValue()));
    }

    /**
     * Tests processDdMessage() method.
     */
    @Test
    public void testProcessDdMessage() throws Exception {
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
                                  new OspfInterfaceChannelHandler(new Controller(), ospfArea,
                                                                  ospfInterface),
                                  topologyForDeviceAndLink);
        ospfNbr.setLastDdPacket(createDdPacket());
        ospfNbr.setNeighborId(Ip4Address.valueOf("2.2.2.2"));
        ospfNbr.setState(OspfNeighborState.EXSTART);
        ospfNbr.setRouterPriority(0);
        ospfNbr.setNeighborDr(Ip4Address.valueOf("13.13.13.13"));
        ospfNbr.setDdSeqNum(123);
        ospfInterface.addNeighbouringRouter(ospfNbr);
        ospfInterfaceChannelHandler.processDdMessage(message, channelHandlerContext);
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
                                  new OspfInterfaceChannelHandler(new Controller(), ospfArea,
                                                                  ospfInterface),
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
        ospfInterfaceChannelHandler.processDdMessage(message, channelHandlerContext);
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
                                  new OspfInterfaceChannelHandler(new Controller(), ospfArea,
                                                                  ospfInterface),
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
        ospfInterfaceChannelHandler.processDdMessage(message, channelHandlerContext);
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
                                  new OspfInterfaceChannelHandler(new Controller(), ospfArea,
                                                                  ospfInterface),
                                  topologyForDeviceAndLink);
        ospfNbr.setLastDdPacket(createDdPacket());
        ospfNbr.setNeighborId(Ip4Address.valueOf("2.2.2.2"));
        ospfNbr.setState(OspfNeighborState.LOADING);
        ospfNbr.setRouterPriority(0);
        ospfNbr.setNeighborDr(Ip4Address.valueOf("13.13.13.13"));
        ospfNbr.setDdSeqNum(123);
        ospfInterface.addNeighbouringRouter(ospfNbr);
        ospfInterfaceChannelHandler.processDdMessage(message, channelHandlerContext);
        ddPacket.setIsMore(1);
        ddPacket.setIsInitialize(0);
        ddPacket.setIsMaster(0);
        ddPacket.setSequenceNo(123);
        ospfInterface.addNeighbouringRouter(ospfNbr);
        ospfNbr.setState(OspfNeighborState.LOADING);
        ospfInterface.addNeighbouringRouter(ospfNbr);

        ospfInterfaceChannelHandler.processDdMessage(message, channelHandlerContext);

        assertThat(ospfInterfaceChannelHandler, is(notNullValue()));

    }

    /**
     * Tests processLsRequestMessage() method.
     */
    @Test
    public void testProcessLSRequestMessage() throws Exception {
        ospfInterface.setIpAddress(Ip4Address.valueOf("11.11.11.11"));
        ospfInterface.setInterfaceType(2);
        ospfInterface.setIpNetworkMask(Ip4Address.valueOf("255.255.255.255"));
        ospfInterface.setHelloIntervalTime(10);
        ospfInterface.setRouterDeadIntervalTime(10);
        ospfArea.setAreaId(Ip4Address.valueOf("12.12.12.12"));
        channelHandlerContext = EasyMock.createMock(ChannelHandlerContext.class);
        OspfMessage message;
        lsRequest = new LsRequest();
        lsRequest.setSourceIp(Ip4Address.valueOf("1.1.1.1"));
        lsRequest.setOspfVer(2);
        lsRequest.setAreaId(Ip4Address.valueOf("12.12.12.12"));
        lsRequest.setRouterId(Ip4Address.valueOf("10.226.165.100"));
        List<LsRequestPacket> lsRequests = new ArrayList();
        LsRequestPacket lsRequestPacket = new LsRequestPacket();
        lsRequestPacket.setLsType(3);
        lsRequestPacket.setLinkStateId("2.2.2.2");
        lsRequestPacket.setOwnRouterId("2.2.2.2");
        lsRequests.add(lsRequestPacket);
        lsRequests.add(lsRequestPacket);
        lsRequest.addLinkStateRequests(new LsRequestPacket());
        lsRequest.addLinkStateRequests(new LsRequestPacket());
        message = lsRequest;
        ospfNbrHashMap = new HashMap();
        ospfNbr.setState(OspfNeighborState.EXCHANGE);
        ospfNbr = new OspfNbrImpl(ospfArea, createOspfInterface(), Ip4Address.valueOf("10.10.10.10"),
                                  Ip4Address.valueOf("10.226.165.100"), 2,
                                  new OspfInterfaceChannelHandler(new Controller(), ospfArea,
                                                                  createOspfInterface1()),
                                  topologyForDeviceAndLink);
        ospfNbr.setLastDdPacket(createDdPacket());
        ospfNbr.setNeighborId(Ip4Address.valueOf("2.2.2.2"));
        ospfNbr.setState(OspfNeighborState.FULL);
        ospfNbr.setRouterPriority(0);
        ospfNbr.setNeighborDr(Ip4Address.valueOf("13.13.13.13"));
        ospfNbr.setDdSeqNum(123);
        ospfInterface.addNeighbouringRouter(ospfNbr);
        ospfInterface.setListOfNeighbors(ospfNbrHashMap);
        ospfInterfaceChannelHandler.processLsRequestMessage(message, channelHandlerContext);
        assertThat(ospfInterfaceChannelHandler, is(notNullValue()));

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
                                  new OspfInterfaceChannelHandler(new Controller(), ospfArea,
                                                                  createOspfInterface1()),
                                  topologyForDeviceAndLink);
        ospfNbr.setLastDdPacket(createDdPacket());
        ospfNbr.setNeighborId(Ip4Address.valueOf("2.2.2.2"));
        ospfNbr.setState(OspfNeighborState.FULL);
        ospfNbr.setRouterPriority(0);
        ospfNbr.setNeighborDr(Ip4Address.valueOf("13.13.13.13"));
        ospfNbr.setDdSeqNum(123);
        ospfInterface.addNeighbouringRouter(ospfNbr);
        ospfInterfaceChannelHandler.processLsUpdateMessage(message, channelHandlerContext);
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
                                  new OspfInterfaceChannelHandler(new Controller(), ospfArea,
                                                                  createOspfInterface()),
                                  topologyForDeviceAndLink);
        ospfNbr.setLastDdPacket(createDdPacket());
        ospfNbr.setNeighborId(Ip4Address.valueOf("2.2.2.2"));
        ospfNbr.setState(OspfNeighborState.FULL);
        ospfNbr.setRouterPriority(0);
        ospfNbr.setNeighborDr(Ip4Address.valueOf("13.13.13.13"));
        ospfNbr.setDdSeqNum(123);
        ospfInterfaceChannelHandler.processLsAckMessage(message, channelHandlerContext);
        assertThat(ospfInterfaceChannelHandler, is(notNullValue()));

    }

    /**
     * Tests compareDdPackets() method.
     */
    @Test
    public void testCompareDDPackets() throws Exception {
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
        ddPacket.setIsInitialize(1);
        ddPacket.setIsMaster(1);
        ddPacket.setIsMore(1);
        ddPacket.setOptions(2);
        ddPacket.setAreaId(Ip4Address.valueOf("12.12.12.12"));
        assertThat(ospfInterfaceChannelHandler.compareDdPackets(ddPacket, ddPacket), is(true));
    }

    @Test(expected = Exception.class)
    public void testCloseChannel() throws Exception {
        channelHandlerContext = EasyMock.createMock(ChannelHandlerContext.class);

        ospfInterfaceChannelHandler.closeChannel(channelHandlerContext);

        assertThat(ospfInterface.dr(), is(notNullValue()));
    }

    /**
     * Tests electRouter() method.
     */
    @Test
    public void testElectRouter() throws Exception {
        ospfInterface.setDr(Ip4Address.valueOf("3.3.3.3"));
        ospfInterface.setBdr(Ip4Address.valueOf("3.3.3.3"));
        ospfInterface.setIpNetworkMask(Ip4Address.valueOf("255.255.255.255"));
        ChannelConfig channelConfig = EasyMock.createMock(ChannelConfig.class);
        EasyMock.expect(channelConfig.getBufferFactory()).andReturn(new HeapChannelBufferFactory());
        Channel channel = EasyMock.createMock(Channel.class);
        ospfInterfaceChannelHandler.electRouter(channel);
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
        OspfEligibleRouter eligibleRouter = ospfInterfaceChannelHandler.electBdr(ospfEligibleRouters);
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
        OspfEligibleRouter eligibleRouter = ospfInterfaceChannelHandler.electDr(ospfEligibleRouters,
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
        OspfEligibleRouter eligibleRouter = ospfInterfaceChannelHandler.selectRouterBasedOnPriority(
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
                                  new OspfInterfaceChannelHandler(new Controller(), ospfArea,
                                                                  createOspfInterface()),
                                  topologyForDeviceAndLink);

        ospfInterfaceChannelHandler.addDeviceInformation(new OspfRouterImpl());
        assertThat(ospfInterfaceChannelHandler, is(notNullValue()));
    }

    /**
     * Tests removeDeviceInformation() method.
     */
    @Test(expected = Exception.class)
    public void testRemoveDeviceInformation() throws Exception {
        ospfNbr = new OspfNbrImpl(ospfArea, createOspfInterface(), Ip4Address.valueOf("10.10.10.10"),
                                  Ip4Address.valueOf("10.226.165.100"), 2,
                                  new OspfInterfaceChannelHandler(new Controller(), ospfArea,
                                                                  createOspfInterface()),
                                  topologyForDeviceAndLink);

        ospfInterfaceChannelHandler.removeDeviceInformation(new OspfRouterImpl());
        assertThat(ospfInterfaceChannelHandler, is(notNullValue()));
    }

    /**
     * Tests addLinkInformation() method.
     */
    @Test(expected = Exception.class)
    public void testaddLinkInformation() throws Exception {
        ospfNbr = new OspfNbrImpl(ospfArea, createOspfInterface(), Ip4Address.valueOf("10.10.10.10"),
                                  Ip4Address.valueOf("10.226.165.100"), 2,
                                  new OspfInterfaceChannelHandler(new Controller(), ospfArea,
                                                                  createOspfInterface()),
                                  topologyForDeviceAndLink);

        List topTlv = new ArrayList();
        topTlv.add(new RouterTlv(new TlvHeader()));
        ospfInterfaceChannelHandler.addLinkInformation(new OspfRouterImpl(), new OspfLinkTedImpl());
        assertThat(ospfInterfaceChannelHandler, is(notNullValue()));
    }

    /**
     * Tests removeLinkInformation() method.
     */
    @Test(expected = Exception.class)
    public void testRemoveLinkInformation() throws Exception {
        ospfNbr = new OspfNbrImpl(ospfArea, createOspfInterface(), Ip4Address.valueOf("10.10.10.10"),
                                  Ip4Address.valueOf("10.226.165.100"), 2,
                                  new OspfInterfaceChannelHandler(new Controller(), ospfArea,
                                                                  createOspfInterface()),
                                  topologyForDeviceAndLink);

        ospfInterfaceChannelHandler.removeLinkInformation(ospfNbr);
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
                                  Ip4Address.valueOf("1.1.1.1"), 2, ospfInterfaceChannelHandler,
                                  topologyForDeviceAndLink);
        ospfNbr.setState(OspfNeighborState.EXSTART);
        ospfNbr.setNeighborId(Ip4Address.valueOf("10.226.165.100"));
        this.ospfInterface = new OspfInterfaceImpl();
        this.ospfInterface.setIpAddress(Ip4Address.valueOf("10.226.165.164"));
        this.ospfInterface.setIpNetworkMask(Ip4Address.valueOf("255.255.255.255"));
        this.ospfInterface.setAreaId(2);
        this.ospfInterface.setAuthKey("authKey");
        this.ospfInterface.setAuthType("AuthReq");
        this.ospfInterface.setBdr(Ip4Address.valueOf("111.111.111.111"));
        this.ospfInterface.setDr(Ip4Address.valueOf("111.111.111.111"));
        this.ospfInterface.setHelloIntervalTime(20);
        this.ospfInterface.setInterfaceCost(10);
        this.ospfInterface.setInterfaceType(2);
        this.ospfInterface.setReTransmitInterval(2000);
        this.ospfInterface.setMtu(6500);
        this.ospfInterface.setPollInterval(1000);
        this.ospfInterface.setRouterDeadIntervalTime(1000);
        this.ospfInterface.setRouterPriority(1);
        this.ospfInterface.setTransmitDelay(500);
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
                                  Ip4Address.valueOf("1.1.1.1"), 2, ospfInterfaceChannelHandler,
                                  topologyForDeviceAndLink);
        ospfNbr.setState(OspfNeighborState.FULL);
        ospfNbr.setNeighborId(Ip4Address.valueOf("10.226.165.100"));
        ospfInterface = new OspfInterfaceImpl();
        ospfInterface.setIpAddress(Ip4Address.valueOf("10.226.165.164"));
        ospfInterface.setIpNetworkMask(Ip4Address.valueOf("255.255.255.255"));
        ospfInterface.setAreaId(2);
        ospfInterface.setAuthKey("authKey");
        ospfInterface.setAuthType("AuthReq");
        ospfInterface.setBdr(Ip4Address.valueOf("111.111.111.111"));
        ospfInterface.setDr(Ip4Address.valueOf("111.111.111.111"));
        ospfInterface.setHelloIntervalTime(20);
        ospfInterface.setInterfaceCost(10);
        ospfInterface.setInterfaceType(2);
        ospfInterface.setReTransmitInterval(2000);
        ospfInterface.setMtu(6500);
        ospfInterface.setPollInterval(1000);
        ospfInterface.setRouterDeadIntervalTime(1000);
        ospfInterface.setRouterPriority(1);
        ospfInterface.setTransmitDelay(500);
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
        ospfArea.setStubCost(10);
        ospfArea.setAreaId(Ip4Address.valueOf("10.226.165.164"));
        ospfArea.setExternalRoutingCapability(true);
        ospfArea.setTransitCapability(true);
        ospfArea.setAddressRanges(addressRanges);
        OspfInterfaceImpl ospfInterface = createOspfInterface();
        ospfInterfaces.add(ospfInterface);
        ospfArea.setInterfacesLst(ospfInterfaces);
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