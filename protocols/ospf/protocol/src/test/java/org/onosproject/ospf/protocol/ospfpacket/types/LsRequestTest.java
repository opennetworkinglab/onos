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
package org.onosproject.ospf.protocol.ospfpacket.types;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.Ip4Address;
import org.onosproject.ospf.controller.OspfPacketType;
import org.onosproject.ospf.protocol.ospfpacket.OspfPacketHeader;
import org.onosproject.ospf.protocol.ospfpacket.subtype.LsRequestPacket;

import java.net.UnknownHostException;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Unit test class for LsRequest.
 */
public class LsRequestTest {

    private LsRequest lsRequest;
    private List<LsRequestPacket> result;
    private OspfPacketType ospfMessageType;
    private OspfPacketHeader ospfPacketHeader;
    private byte[] result1;
    private String result2;
    private ChannelBuffer channelBuffer;
    private LsRequestPacket lsRequestPacket;

    @Before
    public void setUp() throws Exception {
        lsRequest = new LsRequest();
        lsRequest.setAuthType(1);
        lsRequest.setOspftype(3);
        lsRequest.setRouterId(Ip4Address.valueOf("10.226.165.164"));
        lsRequest.setAreaId(Ip4Address.valueOf("10.226.165.163"));
        lsRequest.setChecksum(201);
        lsRequest.setAuthentication(2);
        lsRequest.setOspfPacLength(48);
        lsRequest.setOspfVer(2);
    }

    @After
    public void tearDown() throws Exception {
        lsRequest = null;
        result = null;
        ospfMessageType = null;
        ospfPacketHeader = null;
        result1 = null;
        channelBuffer = null;
        lsRequestPacket = null;
    }

    /**
     * Tests addLinkStateRequests() method.
     */
    @Test
    public void testAddLinkStateRequests() throws Exception {
        lsRequest.addLinkStateRequests(createLsRequestPacket());
        result = lsRequest.getLinkStateRequests();
        assertThat(result, is(notNullValue()));
        assertThat(result.size(), is(1));
    }

    /**
     * Tests getLinkStateRequests() method.
     */
    @Test
    public void testGetLinkStateRequests() throws Exception {
        lsRequest.addLinkStateRequests(createLsRequestPacket());
        lsRequest.addLinkStateRequests(new LsRequestPacket());
        result = lsRequest.getLinkStateRequests();
        assertThat(result, is(notNullValue()));
        assertThat(result.size(), is(2));
    }

    /**
     * Tests ospfMessageType()getter  method.
     */
    @Test
    public void testGetOspfMessageType() throws Exception {
        ospfMessageType = lsRequest.ospfMessageType();
        assertThat(ospfMessageType, is(notNullValue()));
        assertThat(ospfMessageType, is(OspfPacketType.LSREQUEST));
    }

    /**
     * Tests readFrom() method.
     */
    @Test
    public void testReadFrom() throws Exception {
        ospfPacketHeader = new OspfPacketHeader();
        ospfPacketHeader.setAreaId(Ip4Address.valueOf("1.1.1.1"));
        ospfPacketHeader.setAuthentication(0);
        ospfPacketHeader.setAuthType(0);
        ospfPacketHeader.setChecksum(12345);
        ospfPacketHeader.setDestinationIp(Ip4Address.valueOf("10.10.10.10"));
        ospfPacketHeader.setOspfPacLength(56);
        ospfPacketHeader.setOspftype(3);
        ospfPacketHeader.setOspfVer(2);
        ospfPacketHeader.setRouterId(Ip4Address.valueOf("2.2.2.2"));
        ospfPacketHeader.setSourceIp(Ip4Address.valueOf("3.3.3.3"));
        lsRequest = new LsRequest(ospfPacketHeader);
        result1 = createByteLsReqestPacket();
        channelBuffer = ChannelBuffers.copiedBuffer(result1);
        lsRequest.readFrom(channelBuffer);
        assertThat(lsRequest, is(notNullValue()));
        assertThat(lsRequest.ospfMessageType(), is(OspfPacketType.LSREQUEST));
    }

    /**
     * Tests asBytes() method.
     */
    @Test
    public void testAsBytes() throws Exception {
        result1 = lsRequest.asBytes();
        assertThat(result1, is(notNullValue()));
    }

    /**
     * Tests getLsrHeaderAsByteArray() method.
     */
    @Test
    public void testGetLsrHeaderAsByteArray() throws Exception {
        result1 = lsRequest.getLsrHeaderAsByteArray();
        assertThat(result1, is(notNullValue()));
    }

    /**
     * Tests getLsrBodyAsByteArray() method.
     */
    @Test
    public void testGetLsrBodyAsByteArray() throws Exception {
        lsRequest.addLinkStateRequests(createLsRequestPacket());
        lsRequest.addLinkStateRequests(new LsRequestPacket());
        result1 = lsRequest.getLsrBodyAsByteArray();
        assertThat(result1, is(notNullValue()));
    }

    /**
     * Tests to string method.
     */
    @Test
    public void testToString() throws Exception {
        result2 = lsRequest.toString();
        assertThat(result2, is(notNullValue()));
    }

    private LsRequestPacket createLsRequestPacket() throws UnknownHostException {
        lsRequestPacket = new LsRequestPacket();
        lsRequestPacket.setOwnRouterId("165");
        lsRequestPacket.setLinkStateId("10.226.165.164");
        lsRequestPacket.setLsType(2);
        return lsRequestPacket;
    }

    private byte[] createByteLsReqestPacket() {
        byte[] lsRequestPacket = {2, 3, 0, 36, -64, -88, -86, 3, 0, 0, 0, 1, -67,
                -57, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, -64, -88, -86, 8,
                -64, -88, -86, 8};
        return lsRequestPacket;
    }
}