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
package org.onosproject.isis.controller.impl;

import org.easymock.EasyMock;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.Ip4Address;
import org.onosproject.isis.controller.IsisInterface;
import org.onosproject.isis.controller.IsisMessage;
import org.onosproject.isis.controller.IsisNetworkType;
import org.onosproject.isis.controller.IsisProcess;
import org.onosproject.isis.io.isispacket.pdu.L1L2HelloPdu;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Unit test class for IsisChannelHandler.
 */
public class IsisChannelHandlerTest {

    private final String processId = "1";
    private final byte[] config = {0, 0, 0, 0, 0, 0, 0};
    private IsisChannelHandler isisChannelHandler;
    private Controller controller;
    private IsisProcess isisProcess;
    private List<IsisProcess> isisProcessList = new ArrayList();
    private ChannelHandlerContext channelHandlerContext;
    private ChannelStateEvent channelStateEvent;
    private ExceptionEvent exceptionEvent;
    private MessageEvent messageEvent;
    private IsisMessage isisMessage;
    private List<IsisInterface> isisInterfaceList = new ArrayList<>();
    private Ip4Address ip4Address = Ip4Address.valueOf("10.10.10.10");

    @Before
    public void setUp() throws Exception {
        controller = EasyMock.createNiceMock(Controller.class);
        isisProcess = EasyMock.createNiceMock(IsisProcess.class);
        channelHandlerContext = EasyMock.createNiceMock(ChannelHandlerContext.class);
        channelStateEvent = EasyMock.createNiceMock(ChannelStateEvent.class);
        exceptionEvent = EasyMock.createNiceMock(ExceptionEvent.class);
        messageEvent = EasyMock.createNiceMock(MessageEvent.class);
        isisMessage = EasyMock.createNiceMock(L1L2HelloPdu.class);
        isisMessage.setInterfaceIndex(2);
        isisChannelHandler = new IsisChannelHandler(controller, isisProcessList);
    }

    @After
    public void tearDown() throws Exception {
        isisChannelHandler = null;
    }

    /**
     * Tests initializeInterfaceMap() method.
     */
    @Test
    public void testInitializeInterfaceMap() throws Exception {
        isisChannelHandler.initializeInterfaceMap();
        assertThat(isisChannelHandler, is(notNullValue()));
    }

    /**
     * Tests updateInterfaceMap() method.
     */
    @Test(expected = Exception.class)
    public void testUpdateInterfaceMap() throws Exception {
        IsisInterface isisInterface = new DefaultIsisInterface();
        IsisInterface isisInterface1 = new DefaultIsisInterface();
        isisInterface.setInterfaceIpAddress(ip4Address);
        isisInterface.setInterfaceIndex(1);
        isisInterfaceList.add(isisInterface);
        IsisProcess isisProcess = new DefaultIsisProcess();
        isisProcess.setIsisInterfaceList(isisInterfaceList);
        isisProcessList.add(isisProcess);
        isisChannelHandler.updateInterfaceMap(isisProcessList);
        assertThat(isisChannelHandler, is(notNullValue()));
        isisProcessList = new ArrayList<>();
        isisInterface1.setInterfaceIpAddress(ip4Address);
        isisInterface1.setInterfaceIndex(1);
        isisInterface1.setInterfaceIpAddress(ip4Address);
        isisInterface1.setInterfaceIndex(1);
        isisInterface1.setSystemId("9999.9999.9999");
        isisInterface1.setIntermediateSystemName("router");
        isisInterface1.setReservedPacketCircuitType(3);
        isisInterface1.setCircuitId("10");
        isisInterface1.setNetworkType(IsisNetworkType.BROADCAST);
        isisInterface1.setAreaAddress("490001");
        isisInterface1.setHoldingTime(50);
        isisInterface1.setHelloInterval(10);
        isisInterfaceList.add(isisInterface1);
        isisProcess.setIsisInterfaceList(isisInterfaceList);
        isisProcessList.add(isisProcess);
        isisChannelHandler.updateInterfaceMap(isisProcessList);
        assertThat(isisChannelHandler, is(notNullValue()));
    }

    /**
     * Tests initializeInterfaceIpList() method.
     */
    @Test
    public void testInitializeInterfaceIpList() throws Exception {
        isisChannelHandler.initializeInterfaceIpList();
        assertThat(isisChannelHandler, is(notNullValue()));
    }

    /**
     * Tests channelConnected() method.
     */
    @Test(expected = Exception.class)
    public void testChannelConnected() throws Exception {
        isisChannelHandler.channelConnected(channelHandlerContext, channelStateEvent);
        assertThat(isisChannelHandler, is(notNullValue()));
    }

    /**
     * Tests channelDisconnected() method.
     */
    @Test
    public void testChannelDisconnected() throws Exception {
        isisChannelHandler.channelDisconnected(channelHandlerContext, channelStateEvent);
        assertThat(isisChannelHandler, is(notNullValue()));
    }

    /**
     * Tests exceptionCaught() method.
     */
    @Test(expected = Exception.class)
    public void testExceptionCaught() throws Exception {
        isisChannelHandler.exceptionCaught(channelHandlerContext, exceptionEvent);
        assertThat(isisChannelHandler, is(notNullValue()));
    }

    /**
     * Tests messageReceived() method.
     */
    @Test
    public void testMessageReceived() throws Exception {
        isisChannelHandler.messageReceived(channelHandlerContext, messageEvent);
        assertThat(isisChannelHandler, is(notNullValue()));
    }

    /**
     * Tests processIsisMessage() method.
     */
    @Test(expected = Exception.class)
    public void testProcessIsisMessage() throws Exception {
        isisChannelHandler.processIsisMessage(isisMessage, channelHandlerContext);
        assertThat(isisChannelHandler, is(notNullValue()));
    }

    /**
     * Tests startHelloSender() method.
     */
    @Test
    public void testStartHelloSender() throws Exception {
        isisChannelHandler.startHelloSender();
        assertThat(isisChannelHandler, is(notNullValue()));
    }

    /**
     * Tests stopHelloSender() method.
     */
    @Test
    public void testStopHelloSender() throws Exception {
        isisChannelHandler.stopHelloSender();
        assertThat(isisChannelHandler, is(notNullValue()));
    }

    /**
     * Tests sentConfigPacket() method.
     */
    @Test
    public void testSentConfigPacket() throws Exception {
        isisChannelHandler.sentConfigPacket(config);
        assertThat(isisChannelHandler, is(notNullValue()));
    }
}