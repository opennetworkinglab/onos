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

import org.hamcrest.MatcherAssert;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.Ip4Address;
import org.onosproject.ospf.controller.OspfPacketType;
import org.onosproject.ospf.protocol.lsa.LsaHeader;
import org.onosproject.ospf.protocol.lsa.OpaqueLsaHeader;
import org.onosproject.ospf.protocol.ospfpacket.OspfPacketHeader;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Unit test class for LsAck.
 */
public class LsAcknowledgeTest {

    private LsaHeader lsaHeader;
    private LsAcknowledge lsAck;
    private OspfPacketType ospfPacketType;
    private OspfPacketHeader ospfPacketHeader;
    private byte[] result;
    private ChannelBuffer channelBuffer;
    private OpaqueLsaHeader opaqueLsaHeader;

    @Before
    public void setUp() throws Exception {
        lsaHeader = new LsaHeader();
        lsAck = new LsAcknowledge();
        lsAck.setAuthType(1);
        lsAck.setOspftype(5);
        lsAck.setRouterId(Ip4Address.valueOf("10.226.165.164"));
        lsAck.setAreaId(Ip4Address.valueOf("10.226.165.100"));
        lsAck.setChecksum(201);
        lsAck.setAuthentication(2);
        lsAck.setOspfPacLength(48);
        lsAck.setOspfVer(2);
    }

    @After
    public void tearDown() throws Exception {
        lsaHeader = null;
        lsAck = null;
        ospfPacketType = null;
        ospfPacketHeader = null;
        result = null;
        channelBuffer = null;
        opaqueLsaHeader = null;
    }

    /**
     * Tests getLinkStateHeaders() getter method.
     */
    @Test
    public void testGetLinkStateHeaders() throws Exception {
        lsaHeader = createLsaHeader();
        lsAck.addLinkStateHeader(lsaHeader);
        lsAck.addLinkStateHeader(lsaHeader);
        List headers = lsAck.getLinkStateHeaders();
        assertThat(headers.size(), is(1));

    }

    /**
     * Tests addLinkStateHeader() method.
     */
    @Test
    public void testAddLinkStateHeader() throws Exception {
        lsaHeader = createLsaHeader();
        lsAck.addLinkStateHeader(lsaHeader);
        lsAck.addLinkStateHeader(lsaHeader);
        assertThat(lsAck, is(notNullValue()));
    }

    /**
     * Tests ospfMessageType() getter method.
     */
    @Test
    public void testGetOspfMessageType() throws Exception {
        ospfPacketType = lsAck.ospfMessageType();
        assertThat(ospfPacketType, is(notNullValue()));
        assertThat(ospfPacketType, is(OspfPacketType.LSAACK));
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
        ospfPacketHeader.setOspftype(5);
        ospfPacketHeader.setOspfVer(2);
        ospfPacketHeader.setRouterId(Ip4Address.valueOf("2.2.2.2"));
        ospfPacketHeader.setSourceIp(Ip4Address.valueOf("3.3.3.3"));
        result = createByteForLSAck();
        lsAck = new LsAcknowledge(ospfPacketHeader);
        channelBuffer = ChannelBuffers.copiedBuffer(result);
        lsAck.readFrom(channelBuffer);
        assertThat(lsAck, is(notNullValue()));
        assertThat(lsAck.ospfMessageType(), is(OspfPacketType.LSAACK));
    }

    /**
     * Tests asBytes() method.
     */
    @Test
    public void testAsBytes() throws Exception {
        result = lsAck.asBytes();
        assertThat(result, is(notNullValue()));
    }

    /**
     * Tests getLsAckAsByteArray() method.
     */
    @Test
    public void testGetLsAckAsByteArray() throws Exception {
        result = lsAck.getLsAckAsByteArray();
        assertThat(result, is(notNullValue()));
    }

    /**
     * Tests getLsAckBodyAsByteArray() method.
     */
    @Test
    public void testGetLsAckBodyAsByteArray() throws Exception {
        lsaHeader = createLsaHeader();
        opaqueLsaHeader = new OpaqueLsaHeader();
        lsAck.addLinkStateHeader(lsaHeader);
        lsAck.addLinkStateHeader(opaqueLsaHeader);
        result = lsAck.getLsAckBodyAsByteArray();
        assertThat(result, is(notNullValue()));
    }

    /**
     * Tests to string method.
     */
    @Test
    public void testToString() throws Exception {
        MatcherAssert.assertThat(lsAck.toString(), is(notNullValue()));
    }

    /**
     * Utility method used by junit methods.
     */
    private LsaHeader createLsaHeader() {
        lsaHeader = new LsaHeader();
        lsaHeader.setAge(10);
        lsaHeader.setLinkStateId("10.226.165.164");
        lsaHeader.setLsCheckSum(222);
        lsaHeader.setLsPacketLen(48);
        lsaHeader.setLsSequenceNo(2020);
        lsaHeader.setLsType(5);
        lsaHeader.setOptions(2);
        lsaHeader.setAdvertisingRouter(Ip4Address.valueOf("10.226.165.165"));
        return lsaHeader;
    }

    /**
     * Utility method used by junit methods.
     */
    private byte[] createByteForLSAck() {
        byte[] lsAckPacket = {2, 5, 0, 44, -64, -88, -86, 8, 0, 0, 0, 1, -30,
                -12, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 14, 16, 2, 1, -64, -88, -86,
                2, -64, -88, -86, 2, -128, 0, 0, 1, 74, -114, 0, 48};

        return lsAckPacket;
    }
}