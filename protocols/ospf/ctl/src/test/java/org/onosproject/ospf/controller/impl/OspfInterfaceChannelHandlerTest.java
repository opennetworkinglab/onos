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
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.Ip4Address;
import org.onosproject.ospf.controller.OspfArea;
import org.onosproject.ospf.controller.OspfAreaAddressRange;
import org.onosproject.ospf.controller.OspfInterface;
import org.onosproject.ospf.controller.OspfLsaType;
import org.onosproject.ospf.controller.OspfNeighborState;
import org.onosproject.ospf.controller.OspfProcess;
import org.onosproject.ospf.controller.TopologyForDeviceAndLink;
import org.onosproject.ospf.controller.area.OspfAreaAddressRangeImpl;
import org.onosproject.ospf.controller.area.OspfAreaImpl;
import org.onosproject.ospf.controller.area.OspfInterfaceImpl;
import org.onosproject.ospf.controller.area.OspfProcessImpl;
import org.onosproject.ospf.exceptions.OspfParseException;
import org.onosproject.ospf.protocol.lsa.LsaHeader;
import org.onosproject.ospf.protocol.lsa.types.RouterLsa;
import org.onosproject.ospf.protocol.ospfpacket.types.DdPacket;
import org.onosproject.ospf.protocol.ospfpacket.types.HelloPacket;
import org.onosproject.ospf.protocol.util.ChecksumCalculator;

import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;


/**
 * Unit test class for OspfInterfaceChannelHandler.
 */
public class OspfInterfaceChannelHandlerTest {
    private final String string1 = "2.2.2.2";
    private List<OspfAreaAddressRange> addressRanges = new ArrayList();
    private List<OspfInterface> ospfInterfaces = new ArrayList<>();
    private Controller controller;
    private OspfAreaImpl ospfArea;
    private OspfInterfaceImpl ospfInterface;
    private OspfInterfaceChannelHandler ospfInterfaceChannelHandler;
    private OspfNbrImpl ospfNbr;
    private ChannelHandlerContext channelHandlerContext;
    private ChannelStateEvent channelStateEvent;
    private TopologyForDeviceAndLink topologyForDeviceAndLink;
    private List<OspfProcess> ospfProcesses = new ArrayList<>();
    private OspfProcess ospfProcess;
    private Ip4Address ip4Address1 = Ip4Address.valueOf("10.10.10.10");
    private Ip4Address ip4Address2 = Ip4Address.valueOf("2.2.2.2");
    private Ip4Address ip4Address3 = Ip4Address.valueOf("13.13.13.13");
    private Ip4Address ip4Address4 = Ip4Address.valueOf("111.111.111.111");
    private Ip4Address ip4Address5 = Ip4Address.valueOf("10.226.165.164");
    private Ip4Address ip4Address6 = Ip4Address.valueOf("1.1.1.1");
    private Ip4Address ip4Address7 = Ip4Address.valueOf("10.226.165.100");
    private Ip4Address subnetAddress = Ip4Address.valueOf("255.255.255.255");
    private byte[] byteArray;
    private byte[] checkArray;
    private HelloPacket helloPacket;
    private ChecksumCalculator checksumCalculator;
    private ChannelBuffer buf;
    private List<OspfArea> ospfAreas = new ArrayList<>();

    @Before
    public void setUp() throws Exception {
        ospfProcess = new OspfProcessImpl();
        ospfArea = createOspfArea();
        ospfAreas.add(ospfArea);
        ospfProcess.setAreas(ospfAreas);
        ospfProcesses.add(ospfProcess);
        controller = new Controller();
        topologyForDeviceAndLink = new TopologyForDeviceAndLinkImpl();
        ospfNbr = new OspfNbrImpl(ospfArea, ospfInterface, ip4Address1,
                                  ip4Address2, 2, topologyForDeviceAndLink);
        ospfNbr.setNeighborId(ip4Address1);
        ospfNbr.setRouterPriority(0);
        ospfNbr.setNeighborDr(ip4Address3);
        ospfInterface.addNeighbouringRouter(ospfNbr);
        ospfInterfaceChannelHandler = new OspfInterfaceChannelHandler(controller, ospfProcesses);
    }

    @After
    public void tearDown() throws Exception {
        ospfInterfaceChannelHandler = null;
        ospfInterfaceChannelHandler = null;
        ospfInterface = null;
        channelHandlerContext = null;
        channelStateEvent = null;
    }

    /**
     * Tests channelConnected() method.
     */
    @Test(expected = Exception.class)
    public void testChannelConnected() throws Exception {
        channelHandlerContext = EasyMock.createMock(ChannelHandlerContext.class);
        channelStateEvent = EasyMock.createMock(ChannelStateEvent.class);
        ospfInterfaceChannelHandler.channelConnected(channelHandlerContext, channelStateEvent);
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
    @Test
    public void testChannelDisconnected() throws Exception {
        channelHandlerContext = EasyMock.createMock(ChannelHandlerContext.class);
        channelStateEvent = EasyMock.createMock(ChannelStateEvent.class);
        ospfInterfaceChannelHandler.channelDisconnected(channelHandlerContext, channelStateEvent);
        assertThat(ospfInterfaceChannelHandler, is(notNullValue()));
    }

    /**
     * Tests initializeInterfaceMap() method.
     */
    @Test
    public void testInitializeInterfaceMap() throws Exception {
        ospfInterfaceChannelHandler.initializeInterfaceMap();
        assertThat(ospfInterfaceChannelHandler, is(notNullValue()));
    }

    /**
     * Tests updateInterfaceMap() method.
     */
    @Test
    public void testUpdateInterfaceMap() throws Exception {
        ospfInterfaceChannelHandler.updateInterfaceMap(ospfProcesses);
        assertThat(ospfInterfaceChannelHandler, is(notNullValue()));
    }

    /**
     * Utility for test method.
     */
    private OspfAreaImpl createOspfArea() throws Exception {
        OspfAreaAddressRangeImpl ospfAreaAddressRange;
        ospfAreaAddressRange = createOspfAreaAddressRange();
        addressRanges.add(ospfAreaAddressRange);
        OspfAreaImpl ospfArea = new OspfAreaImpl();
        ospfArea.setAreaId(ip4Address5);
        ospfArea.setExternalRoutingCapability(true);
        OspfInterfaceImpl ospfInterface = createOspfInterface();
        ospfInterfaces.add(ospfInterface);
        ospfArea.setOspfInterfaceList(ospfInterfaces);
        RouterLsa routerLsa = new RouterLsa();
        routerLsa.setLsType(1);
        routerLsa.setLinkStateId(string1);
        routerLsa.setAdvertisingRouter(ip4Address2);
        ospfArea.addLsa(routerLsa, false, ospfInterface);
        ospfArea.setRouterId(ip4Address4);

        return ospfArea;
    }

    /**
     * Utility for test method.
     */
    private OspfAreaAddressRangeImpl createOspfAreaAddressRange() {
        OspfAreaAddressRangeImpl ospfAreaAddressRange = new OspfAreaAddressRangeImpl();
        ospfAreaAddressRange.setIpAddress(ip4Address5);
        ospfAreaAddressRange.setAdvertise(true);
        ospfAreaAddressRange.setMask("mask");
        return ospfAreaAddressRange;
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
     * Utility for test method.
     */
    private OspfInterfaceImpl createOspfInterface1() throws UnknownHostException {
        ospfInterface = new OspfInterfaceImpl();
        OspfAreaImpl ospfArea = new OspfAreaImpl();
        OspfInterfaceChannelHandler ospfInterfaceChannelHandler = EasyMock.createMock(
                OspfInterfaceChannelHandler.class);
        ospfNbr = new OspfNbrImpl(ospfArea, ospfInterface, ip4Address5,
                                  ip4Address6, 2, topologyForDeviceAndLink);
        ospfNbr.setState(OspfNeighborState.FULL);
        ospfNbr.setNeighborId(ip4Address7);
        ospfInterface = new OspfInterfaceImpl();
        ospfInterface.setIpAddress(ip4Address5);
        ospfInterface.setIpNetworkMask(subnetAddress);
        ospfInterface.setBdr(ip4Address4);
        ospfInterface.setDr(ip4Address4);
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
    private OspfInterfaceImpl createOspfInterface() throws Exception {
        ospfInterface = new OspfInterfaceImpl();
        LsaHeader lsaHeader = new LsaHeader();
        lsaHeader.setLsType(OspfLsaType.ROUTER.value());
        RouterLsa routerLsa = new RouterLsa();
        OspfAreaImpl ospfArea = new OspfAreaImpl();
        ospfArea.addLsa(routerLsa, true, ospfInterface);
        ospfNbr = new OspfNbrImpl(ospfArea, ospfInterface, ip4Address5,
                                  ip4Address6, 2, topologyForDeviceAndLink);
        ospfNbr.setState(OspfNeighborState.EXSTART);
        ospfNbr.setNeighborId(ip4Address7);
        this.ospfInterface = new OspfInterfaceImpl();
        this.ospfInterface.setIpAddress(ip4Address5);
        this.ospfInterface.setIpNetworkMask(subnetAddress);
        this.ospfInterface.setBdr(ip4Address4);
        this.ospfInterface.setDr(ip4Address4);
        this.ospfInterface.setHelloIntervalTime(20);
        this.ospfInterface.setInterfaceType(2);
        this.ospfInterface.setReTransmitInterval(2000);
        this.ospfInterface.setMtu(6500);
        this.ospfInterface.setRouterDeadIntervalTime(1000);
        this.ospfInterface.setRouterPriority(1);
        this.ospfInterface.setInterfaceType(1);
        this.ospfInterface.setInterfaceIndex(1);
        this.ospfInterface.addNeighbouringRouter(ospfNbr);
        this.ospfInterface.setOspfArea(ospfArea);
        return this.ospfInterface;
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
}