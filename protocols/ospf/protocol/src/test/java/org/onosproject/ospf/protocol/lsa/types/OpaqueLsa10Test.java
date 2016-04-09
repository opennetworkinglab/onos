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
package org.onosproject.ospf.protocol.lsa.types;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.Ip4Address;
import org.onosproject.ospf.controller.OspfLsaType;
import org.onosproject.ospf.protocol.lsa.OpaqueLsaHeader;
import org.onosproject.ospf.protocol.lsa.TlvHeader;
import org.onosproject.ospf.protocol.lsa.tlvtypes.LinkTlv;
import org.onosproject.ospf.protocol.lsa.tlvtypes.RouterTlv;


import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit test class for OpaqueLsa10.
 */
public class OpaqueLsa10Test {

    private final byte[] packet = {0, 1, 0, 4, 1, 1, 1, 1, 0, 2, 0, 84, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    private OpaqueLsa10 opaqueLsa10;
    private TopLevelTlv tlv;
    private OpaqueLsaHeader opqueHeader;
    private ChannelBuffer channelBuffer;
    private byte[] result;
    private RouterTlv routerTlv;
    private RouterTlv routerTlv1;
    private LinkTlv linkTlv;
    private LinkTlv linkTlv1;
    private OspfLsaType ospflsaType;
    private int result1;
    private List result2;

    @Before
    public void setUp() throws Exception {
        opaqueLsa10 = new OpaqueLsa10(new OpaqueLsaHeader());
    }

    @After
    public void tearDown() throws Exception {
        opaqueLsa10 = null;
        tlv = null;
        opqueHeader = null;
        channelBuffer = null;
        result = null;
        routerTlv = null;
        routerTlv1 = null;
        linkTlv1 = null;
        ospflsaType = null;
    }

    /**
     * Tests to string method.
     */
    @Test
    public void testToString() throws Exception {
        assertThat(opaqueLsa10.toString(), is(notNullValue()));
    }

    /**
     * Tests addValue() method.
     */
    @Test
    public void testAddValue() throws Exception {
        tlv = new RouterTlv(new TlvHeader());
        opaqueLsa10.addValue(tlv);
        assertThat(opaqueLsa10, is(notNullValue()));
    }

    /**
     * Tests readFrom() method.
     */
    @Test(expected = Exception.class)
    public void testReadFrom() throws Exception {
        opqueHeader = new OpaqueLsaHeader();
        opqueHeader.setLsType(10);
        opqueHeader.setLsPacketLen(48);
        opqueHeader.setLsCheckSum(10);
        opqueHeader.setAge(4);
        opqueHeader.setOpaqueId(1);
        opqueHeader.setOpaqueType(10);
        opqueHeader.setLsSequenceNo(250);
        opqueHeader.setAdvertisingRouter(Ip4Address.valueOf("100.226.165.165"));
        opqueHeader.setOptions(66);
        opaqueLsa10 = new OpaqueLsa10(opqueHeader);
        channelBuffer = ChannelBuffers.copiedBuffer(packet);
        opaqueLsa10.readFrom(channelBuffer);
        result = opaqueLsa10.asBytes();
        assertThat(result, is(notNullValue()));
    }

    /**
     * Tests asBytes() method.
     */
    @Test(expected = Exception.class)
    public void testAsBytes() throws Exception {
        opqueHeader = new OpaqueLsaHeader();
        opqueHeader.setLsType(10);
        opqueHeader.setLsPacketLen(48);
        opqueHeader.setLsCheckSum(10);
        opqueHeader.setAge(4);
        opqueHeader.setOpaqueId(1);
        opqueHeader.setOpaqueType(10);
        opqueHeader.setLsSequenceNo(250);
        opqueHeader.setAdvertisingRouter(Ip4Address.valueOf("100.226.165.165"));
        opqueHeader.setOptions(66);
        opaqueLsa10 = new OpaqueLsa10(opqueHeader);
        channelBuffer = ChannelBuffers.copiedBuffer(packet);
        opaqueLsa10.readFrom(channelBuffer);
        result = opaqueLsa10.getLsaBodyAsByteArray();
        result = opaqueLsa10.asBytes();
        assertThat(result, is(notNullValue()));
    }

    /**
     * Tests getLsaBodyAsByteArray() method.
     */
    @Test(expected = Exception.class)
    public void testGetLsaBodyAsByteArray() throws Exception {
        opqueHeader = new OpaqueLsaHeader();
        opqueHeader.setLsType(10);
        opqueHeader.setLsPacketLen(48);
        opqueHeader.setLsCheckSum(10);
        opqueHeader.setAge(4);
        opqueHeader.setOpaqueId(1);
        opqueHeader.setOpaqueType(10);
        opqueHeader.setLsSequenceNo(250);
        opqueHeader.setAdvertisingRouter(Ip4Address.valueOf("100.226.165.165"));
        opqueHeader.setOptions(2);
        opaqueLsa10 = new OpaqueLsa10(opqueHeader);
        routerTlv = new RouterTlv(new TlvHeader());
        linkTlv = new LinkTlv(new TlvHeader());
        opaqueLsa10.addValue(routerTlv);
        opaqueLsa10.addValue(linkTlv);
        routerTlv1 = new RouterTlv(new TlvHeader());
        linkTlv1 = new LinkTlv(new TlvHeader());
        opaqueLsa10.addValue(routerTlv1);
        opaqueLsa10.addValue(linkTlv1);
        channelBuffer = ChannelBuffers.copiedBuffer(packet);
        opaqueLsa10.readFrom(channelBuffer);
        result = opaqueLsa10.getLsaBodyAsByteArray();
        assertThat(result, is(notNullValue()));
    }

    /**
     * Tests getLsaBodyAsByteArray() method.
     */
    @Test(expected = Exception.class)
    public void testGetLsaBodyAsByteArray1() throws Exception {
        opqueHeader = new OpaqueLsaHeader();
        opqueHeader.setLsType(10);
        opqueHeader.setLsPacketLen(48);
        opqueHeader.setLsCheckSum(10);
        opqueHeader.setAge(4);
        opqueHeader.setOpaqueId(1);
        opqueHeader.setOpaqueType(10);
        opqueHeader.setLsSequenceNo(250);
        opqueHeader.setAdvertisingRouter(Ip4Address.valueOf("100.226.165.165"));
        opqueHeader.setOptions(2);
        opaqueLsa10 = new OpaqueLsa10(opqueHeader);
        routerTlv = new RouterTlv(new TlvHeader());
        opaqueLsa10.addValue(routerTlv);
        channelBuffer = ChannelBuffers.copiedBuffer(packet);
        result = opaqueLsa10.getLsaBodyAsByteArray();
        assertThat(result, is(notNullValue()));
    }

    /**
     * Tests getLsaBodyAsByteArray() method.
     */
    @Test(expected = Exception.class)
    public void testGetLsaBodyAsByteArray2() throws Exception {
        opqueHeader = new OpaqueLsaHeader();
        opqueHeader.setLsType(10);
        opqueHeader.setLsPacketLen(48);
        opqueHeader.setLsCheckSum(10);
        opqueHeader.setAge(4);
        opqueHeader.setOpaqueId(1);
        opqueHeader.setOpaqueType(10);
        opqueHeader.setLsSequenceNo(250);
        opqueHeader.setAdvertisingRouter(Ip4Address.valueOf("100.226.165.165"));
        opqueHeader.setOptions(2);
        opaqueLsa10 = new OpaqueLsa10(opqueHeader);
        linkTlv = new LinkTlv(new TlvHeader());
        opaqueLsa10.addValue(linkTlv);
        channelBuffer = ChannelBuffers.copiedBuffer(packet);
        opaqueLsa10.readFrom(channelBuffer);
        result = opaqueLsa10.getLsaBodyAsByteArray();
        assertThat(result, is(notNullValue()));
    }

    /**
     * Tests getOspfLsaType() getter method.
     */
    @Test
    public void testGetOspfLsaType() throws Exception {
        opaqueLsa10.setLsType(10);
        ospflsaType = opaqueLsa10.getOspfLsaType();
        assertThat(ospflsaType, is(notNullValue()));
        assertThat(ospflsaType, is(OspfLsaType.AREA_LOCAL_OPAQUE_LSA));
    }

    /**
     * Tests hashCode() method.
     */
    @Test
    public void testHashcode() throws Exception {

        result1 = opaqueLsa10.hashCode();
        assertThat(result1, is(notNullValue()));

    }

    /**
     * Tests topLevelValues() method.
     */
    @Test
    public void testTopLevelValues() throws Exception {

        result2 = opaqueLsa10.topLevelValues();
        assertThat(result2, is(notNullValue()));

    }

}