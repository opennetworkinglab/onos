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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.Ip4Address;
import org.onosproject.ospf.controller.OspfPacketType;
import org.onosproject.ospf.protocol.lsa.LsaHeader;
import org.onosproject.ospf.protocol.lsa.OpaqueLsaHeader;
import org.onosproject.ospf.protocol.ospfpacket.OspfPacketHeader;

import java.util.List;
import java.util.Vector;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit test class for OspfRouterId.
 */
public class DdPacketTest {

    private byte[] packet;
    private byte[] result2;
    private DdPacket ddPacket;
    private Vector<LsaHeader> lsaHeaderList = new Vector<LsaHeader>();
    private int result;
    private long result1;
    private OpaqueLsaHeader opqueHeader;
    private OpaqueLsaHeader opqueHeader1;
    private List<LsaHeader> header;
    private OspfPacketHeader ospfPacketHeader;
    private ChannelBuffer channelBuffer;
    private LsaHeader lsaHeader;
    private long result3;
    private OspfPacketType ospfPacketType;

    @Before
    public void setUp() throws Exception {
        ddPacket = new DdPacket();
        ddPacket.setAuthType(1);
        ddPacket.setOspftype(2);
        ddPacket.setRouterId(Ip4Address.valueOf("10.226.165.164"));
        ddPacket.setAreaId(Ip4Address.valueOf("10.226.165.100"));
        ddPacket.setChecksum(201);
        ddPacket.setAuthentication(2);
        ddPacket.setOspfPacLength(48);
        ddPacket.setOspfVer(2);
    }

    @After
    public void tearDown() throws Exception {
        ddPacket = null;
        lsaHeaderList.clear();
        opqueHeader = null;
        opqueHeader1 = null;
        header = null;
        ospfPacketHeader = null;
        channelBuffer = null;
        lsaHeader = null;
        ospfPacketType = null;
    }

    /**
     * Tests isOpaqueCapable() getter method.
     */
    @Test
    public void testIsOpaqueCapable() throws Exception {
        ddPacket.setIsOpaqueCapable(true);
        assertThat(ddPacket.isOpaqueCapable(), is(true));
    }

    /**
     * Tests isOpaqueCapable() setter method.
     */
    @Test
    public void testSetIsOpaqueCapable() throws Exception {
        ddPacket.setIsOpaqueCapable(true);
        assertThat(ddPacket.isOpaqueCapable(), is(true));
    }

    /**
     * Tests ims() getter method.
     */
    @Test
    public void testGetIms() throws Exception {
        ddPacket.setIms(1);
        result = ddPacket.ims();
        assertThat(result, is(notNullValue()));
        assertThat(result, is(1));
    }

    /**
     * Tests ims() setter method.
     */
    @Test
    public void testSetIms() throws Exception {
        ddPacket.setIms(1);
        result = ddPacket.ims();
        assertThat(result, is(notNullValue()));
        assertThat(result, is(1));
    }

    /**
     * Tests isMaster() getter method.
     */
    @Test
    public void testGetIsMaster() throws Exception {
        ddPacket.setIsMaster(2);
        result = ddPacket.isMaster();
        assertThat(result, is(notNullValue()));
        assertThat(result, is(2));
    }

    /**
     * Tests isMaster() setter method.
     */
    @Test
    public void testSetIsMaster() throws Exception {
        ddPacket.setIsMaster(2);
        result = ddPacket.isMaster();
        assertThat(result, is(notNullValue()));
        assertThat(result, is(2));
    }

    /**
     * Tests isInitialize() getter method.
     */
    @Test
    public void testGetIsInitialize() throws Exception {
        ddPacket.setIsInitialize(3);
        result = ddPacket.isInitialize();
        assertThat(result, is(notNullValue()));
        assertThat(result, is(3));
    }

    /**
     * Tests isInitialize() setter method.
     */
    @Test
    public void testSetIsInitialize() throws Exception {
        ddPacket.setIsInitialize(3);
        int result = ddPacket.isInitialize();
        assertThat(result, is(notNullValue()));
        assertThat(result, is(3));
    }

    /**
     * Tests isMore() getter method.
     */
    @Test
    public void testGetIsMore() throws Exception {
        ddPacket.setIsMore(4);
        result = ddPacket.isMore();
        assertThat(result, is(notNullValue()));
        assertThat(result, is(4));
    }

    /**
     * Tests isMore() setter method.
     */
    @Test
    public void testSetIsMore() throws Exception {
        ddPacket.setIsMore(4);
        int result = ddPacket.isMore();
        assertThat(result, is(notNullValue()));
        assertThat(result, is(4));
    }

    /**
     * Tests imtu() getter method.
     */
    @Test
    public void testGetImtu() throws Exception {
        ddPacket.setImtu(5);
        result = ddPacket.imtu();
        assertThat(result, is(notNullValue()));
        assertThat(result, is(5));
    }

    /**
     * Tests imtu() setter method.
     */
    @Test
    public void testSetImtu() throws Exception {
        ddPacket.setImtu(5);
        result = ddPacket.imtu();
        assertThat(result, is(notNullValue()));
        assertThat(result, is(5));
    }

    /**
     * Tests options() getter method.
     */
    @Test
    public void testGetOptions() throws Exception {
        ddPacket.setOptions(2);
        result = ddPacket.options();
        assertThat(result, is(notNullValue()));
        assertThat(result, is(2));
    }

    /**
     * Tests options() setter method.
     */
    @Test
    public void testSetOptions() throws Exception {
        ddPacket.setOptions(2);
        result = ddPacket.options();
        Assert.assertNotNull(result);
        Assert.assertEquals(2, result);
    }

    /**
     * Tests sequenceNo() getter method.
     */
    @Test
    public void testGetSequenceno() throws Exception {
        ddPacket.setSequenceNo(2020);
        result1 = ddPacket.sequenceNo();
        assertThat(result1, is(notNullValue()));
        assertThat(result1, is(2020L));
    }

    /**
     * Tests sequenceNo() setter method.
     */
    @Test
    public void testSetSequenceno() throws Exception {
        ddPacket.setSequenceNo(2020);
        result3 = ddPacket.sequenceNo();
        assertThat(result3, is(notNullValue()));
        assertThat(result3, is(2020L));
    }

    /**
     * Tests getLsaHeaderList() getter method.
     */
    @Test
    public void testGetLsaHeaderList() throws Exception {
        ddPacket.addLsaHeader(createLsaHeader());
        opqueHeader = new OpaqueLsaHeader();
        opqueHeader.setLsType(9);
        opqueHeader.setLsPacketLen(48);
        opqueHeader.setLsCheckSum(10);
        opqueHeader.setAge(4);
        opqueHeader.setOpaqueId(9);
        opqueHeader.setOpaqueType(9);
        opqueHeader.setLsSequenceNo(250);
        opqueHeader.setAdvertisingRouter(Ip4Address.valueOf("100.226.165.165"));
        opqueHeader.setOptions(2);
        ddPacket.setIsOpaqueCapable(true);
        ddPacket.addLsaHeader(opqueHeader);
        opqueHeader1 = new OpaqueLsaHeader();
        opqueHeader1.setLsType(10);
        opqueHeader1.setLsPacketLen(48);
        opqueHeader1.setLsCheckSum(10);
        opqueHeader1.setAge(4);
        opqueHeader1.setOpaqueId(9);
        opqueHeader1.setOpaqueType(9);
        opqueHeader1.setLsSequenceNo(250);
        opqueHeader1.setAdvertisingRouter(Ip4Address.valueOf("100.226.165.165"));
        opqueHeader1.setOptions(66);
        ddPacket.addLsaHeader(opqueHeader1);
        header = ddPacket.getLsaHeaderList();
        assertThat(header, is(notNullValue()));
    }

    /**
     * Tests getLsaHeaderList() setter method.
     */
    @Test
    public void testSetLsaHeaderList() throws Exception {
        ddPacket.addLsaHeader(createLsaHeader());
        opqueHeader = new OpaqueLsaHeader();
        opqueHeader.setLsType(9);
        opqueHeader.setLsPacketLen(48);
        opqueHeader.setLsCheckSum(10);
        opqueHeader.setAge(4);
        opqueHeader.setOpaqueId(9);
        opqueHeader.setOpaqueType(9);
        opqueHeader.setLsSequenceNo(250);
        opqueHeader.setAdvertisingRouter(Ip4Address.valueOf("100.226.165.165"));
        opqueHeader.setOptions(66);
        ddPacket.addLsaHeader(opqueHeader);
        opqueHeader1 = new OpaqueLsaHeader();
        opqueHeader1.setLsType(10);
        opqueHeader1.setLsPacketLen(48);
        opqueHeader1.setLsCheckSum(10);
        opqueHeader1.setAge(4);
        opqueHeader1.setOpaqueId(9);
        opqueHeader1.setOpaqueType(9);
        opqueHeader1.setLsSequenceNo(250);
        opqueHeader1.setAdvertisingRouter(Ip4Address.valueOf("100.226.165.165"));
        opqueHeader1.setOptions(2);
        ddPacket.addLsaHeader(opqueHeader1);
        header = ddPacket.getLsaHeaderList();
        assertThat(header.contains(createLsaHeader()), is(true));
    }

    /**
     * Tests addLsaHeader() method.
     */
    @Test
    public void testAddLsaHeader() throws Exception {
        ddPacket.addLsaHeader(createLsaHeader());
        assertThat(ddPacket, is(notNullValue()));
    }

    /**
     * Tests ospfMessageType() getter method.
     */
    @Test
    public void testGetOspfMessageType() throws Exception {
        ospfPacketType = ddPacket.ospfMessageType();
        assertThat(ospfPacketType, is(notNullValue()));
        assertThat(ospfPacketType, is(OspfPacketType.DD));
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
        ospfPacketHeader.setOspftype(2);
        ospfPacketHeader.setOspfVer(2);
        ospfPacketHeader.setRouterId(Ip4Address.valueOf("2.2.2.2"));
        ospfPacketHeader.setSourceIp(Ip4Address.valueOf("3.3.3.3"));
        ddPacket.setIsOpaqueCapable(true);
        ddPacket.setOptions(66);
        ddPacket = new DdPacket(ospfPacketHeader);
        packet = createByteForDdPacket();
        channelBuffer = ChannelBuffers.copiedBuffer(packet);
        ddPacket.readFrom(channelBuffer);
        assertThat(ddPacket, is(notNullValue()));
        assertThat(ddPacket.ospfMessageType(), is(OspfPacketType.DD));
    }

    /**
     * Tests asBytes() method.
     */
    @Test
    public void testAsBytes() throws Exception {
        result2 = ddPacket.asBytes();
        assertThat(result2, is(notNullValue()));
    }

    /**
     * Tests getDdHeaderAsByteArray() method.
     */
    @Test
    public void testGetDdHeaderAsByteArray() throws Exception {
        opqueHeader = new OpaqueLsaHeader();
        opqueHeader.setLsType(9);
        opqueHeader.setLsPacketLen(48);
        opqueHeader.setLsCheckSum(10);
        opqueHeader.setAge(4);
        opqueHeader.setOpaqueId(9);
        opqueHeader.setOpaqueType(9);
        opqueHeader.setLsSequenceNo(250);
        opqueHeader.setAdvertisingRouter(Ip4Address.valueOf("100.226.165.165"));
        opqueHeader.setOptions(66);
        ddPacket.addLsaHeader(opqueHeader);
        opqueHeader1 = new OpaqueLsaHeader();
        opqueHeader1.setLsType(10);
        opqueHeader1.setLsPacketLen(48);
        opqueHeader1.setLsCheckSum(10);
        opqueHeader1.setAge(4);
        opqueHeader1.setOpaqueId(9);
        opqueHeader1.setOpaqueType(9);
        opqueHeader1.setLsSequenceNo(250);
        opqueHeader1.setAdvertisingRouter(Ip4Address.valueOf("100.226.165.165"));
        opqueHeader1.setOptions(2);
        ddPacket.addLsaHeader(opqueHeader1);
        result2 = ddPacket.getDdHeaderAsByteArray();
        assertThat(result2, is(notNullValue()));
    }

    /**
     * Tests getDdBodyAsByteArray() method.
     */
    @Test
    public void testGetDdBodyAsByteArray() throws Exception {
        lsaHeader = createLsaHeader();
        ddPacket.addLsaHeader(lsaHeader);
        result2 = ddPacket.getDdBodyAsByteArray();
        assertThat(result2, is(notNullValue()));
    }

    /**
     * Tests to string method.
     */
    @Test
    public void testToString() throws Exception {
        assertThat(ddPacket.toString(), is(notNullValue()));
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
        lsaHeader.setLsType(2);
        lsaHeader.setOptions(2);
        lsaHeader.setAdvertisingRouter(Ip4Address.valueOf("10.226.165.165"));
        return lsaHeader;
    }

    /**
     * Utility method used by junit methods.
     */
    private byte[] createByteForDdPacket() {
        byte[] ddPacket = {5, -36, 66, 1, 65, 119, -87, 126, 0, 23, 2, 1, 10, 10,
                10, 10, 10, 10, 10, 10, -128, 0, 0, 6, -69, 26, 0, 36};

        return ddPacket;
    }
}