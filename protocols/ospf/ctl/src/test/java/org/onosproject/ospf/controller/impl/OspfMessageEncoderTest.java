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
import org.jboss.netty.channel.ChannelHandlerContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.Ip4Address;
import org.onosproject.ospf.controller.area.OspfInterfaceImpl;
import org.onosproject.ospf.protocol.ospfpacket.types.DdPacket;
import org.onosproject.ospf.protocol.ospfpacket.types.HelloPacket;
import org.onosproject.ospf.protocol.ospfpacket.types.LsAcknowledge;
import org.onosproject.ospf.protocol.ospfpacket.types.LsRequest;
import org.onosproject.ospf.protocol.ospfpacket.types.LsUpdate;
import org.onosproject.ospf.protocol.util.OspfInterfaceState;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by sdn on 13/1/16.
 */
public class OspfMessageEncoderTest {
    private final byte[] hpacket = {2, 1, 0, 44, -64, -88, -86, 8, 0, 0, 0, 1, 39, 59,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, -1, 0, 0, 10, 2, 1, 0, 0, 0,
            40, -64, -88, -86, 8, 0, 0, 0, 0};
    private final byte[] dpacket = {2, 2, 0, 32, -64, -88, -86, 8, 0, 0, 0, 1, -96, 82,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 5, -36, 2, 7, 65, 119, -87, 126};
    private final byte[] lrpacket = {2, 3, 0, 36, -64, -88, -86, 3, 0, 0, 0, 1, -67, -57,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, -64, -88, -86, 8, -64, -88, -86, 8};
    private byte[] lAckpacket = {2, 5, 0, 44, -64, -88, -86, 8, 0, 0, 0, 1, -30, -12,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 14, 16, 2, 1, -64, -88, -86, 2, -64,
            -88, -86, 2, -128, 0, 0, 1, 74, -114, 0, 48};
    private HelloPacket helloPacket;
    private DdPacket ddPacket;
    private LsAcknowledge lsAcknowledge;
    private LsRequest lsRequest;
    private LsUpdate lsUpdate;
    private ChannelHandlerContext ctx;
    private OspfMessageEncoder ospfMessageEncoder;
    private ChannelBuffer buf;
    private SocketAddress socketAddress;
    private Channel channel;

    @Before
    public void setUp() throws Exception {
        ospfMessageEncoder = new OspfMessageEncoder();
        helloPacket = new HelloPacket();
        ddPacket = new DdPacket();
        lsAcknowledge = new LsAcknowledge();
        lsRequest = new LsRequest();
        lsUpdate = new LsUpdate();
        helloPacket.setOspftype(1);
        ddPacket.setOspftype(2);
        lsAcknowledge.setOspftype(5);
        lsRequest.setOspftype(3);
        lsUpdate.setOspftype(4);
        OspfInterfaceImpl ospfInterface = new OspfInterfaceImpl();
        ospfInterface.setState(OspfInterfaceState.DROTHER);
        ospfMessageEncoder = new OspfMessageEncoder(ospfInterface);

    }

    @After
    public void tearDown() throws Exception {
        helloPacket = null;
        ddPacket = null;
        lsAcknowledge = null;
        lsRequest = null;
        lsUpdate = null;
        ospfMessageEncoder = null;
        buf = null;
    }

    /**
     * Tests encode() method.
     */
    @Test
    public void testEncode() throws Exception {
        socketAddress = InetSocketAddress.createUnresolved("127.0.0.1", 8600);
        channel = EasyMock.createMock(Channel.class);
        helloPacket = new HelloPacket();
        helloPacket.setDestinationIp(Ip4Address.valueOf("15.15.15.15"));
        buf = ChannelBuffers.buffer(hpacket.length);
        buf.writeBytes(hpacket);
        helloPacket.readFrom(buf);
        ospfMessageEncoder.encode(ctx, channel, helloPacket);
        ddPacket = new DdPacket();
        ddPacket.setDestinationIp(Ip4Address.valueOf("15.15.15.15"));
        buf = ChannelBuffers.buffer(dpacket.length);
        buf.writeBytes(dpacket);
        ddPacket.readFrom(buf);
        ospfMessageEncoder.encode(ctx, channel, ddPacket);
        lsRequest = new LsRequest();
        lsRequest.setDestinationIp(Ip4Address.valueOf("15.15.15.15"));
        buf = ChannelBuffers.buffer(lrpacket.length);
        buf.writeBytes(lrpacket);
        lsRequest.readFrom(buf);
        ospfMessageEncoder.encode(ctx, channel, lsRequest);

        lsAcknowledge = new LsAcknowledge();
        lsAcknowledge.setDestinationIp(Ip4Address.valueOf("15.15.15.15"));
        buf = ChannelBuffers.buffer(lAckpacket.length);
        buf.writeBytes(lAckpacket);
        lsAcknowledge.readFrom(buf);
        ospfMessageEncoder.encode(ctx, channel, lsAcknowledge);
        assertThat(ospfMessageEncoder, is(notNullValue()));
    }
}