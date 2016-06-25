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
package org.onosproject.isis.io.isispacket.pdu;

import org.easymock.EasyMock;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.MacAddress;
import org.onosproject.isis.controller.IsisInterfaceState;
import org.onosproject.isis.io.isispacket.IsisHeader;
import org.onosproject.isis.io.isispacket.tlv.AdjacencyStateTlv;
import org.onosproject.isis.io.isispacket.tlv.IsisTlv;
import org.onosproject.isis.io.isispacket.tlv.TlvHeader;

import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit test class for HelloPdu.
 */
public class HelloPduTest {

    private final byte[] macAddr = new byte[]{
            (byte) 0xa4, (byte) 0x22, (byte) 0xc2, 0x00, 0x00, 0x00
    };
    private final String srcId = "1111.1111.1111";
    private final byte[] helloL1L2 = {
            1, 34, 34, 34, 34, 34, 34, 0,
            30, 5, -39, 64, 34, 34, 34, 34, 34, 34, 1, -127, 1, -52, 1,
            4, 3, 73, 0, 10, -124, 4, 10, 0, 10, 2, -45, 3, 0, 0, 0,
            8, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 8, -1, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 8,
            -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 8, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 8, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 8, -93, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
    };

    private HelloPdu helloPdu;
    private IsisHeader isisHeader;
    private IsisTlv isisTlv;
    private TlvHeader tlvHeader;
    private MacAddress macAddress;
    private List<IsisTlv> resultList;
    private List<String> resultListStr;
    private List<Ip4Address> resultListIPv4;
    private List<MacAddress> resultListMac;
    private IsisInterfaceState resultAdjState;
    private String resultStr;
    private int resultInt;
    private byte resultByte;
    private ChannelBuffer channelBuffer;
    private HelloPdu pdu;

    @Before
    public void setUp() throws Exception {
        isisHeader = new IsisHeader();
        helloPdu = new L1L2HelloPdu(isisHeader);
        tlvHeader = new TlvHeader();
        isisTlv = new AdjacencyStateTlv(tlvHeader);
        macAddress = new MacAddress(macAddr);
        channelBuffer = EasyMock.createMock(ChannelBuffer.class);
        pdu = helloPdu;
    }

    @After
    public void tearDown() throws Exception {
        isisHeader = null;
        helloPdu = null;
        isisTlv = null;
        tlvHeader = null;
        macAddress = null;
    }

    /**
     * Tests addTlv() getter method.
     */
    @Test
    public void testAddTlv() throws Exception {
        helloPdu.addTlv(isisTlv);
        resultList = helloPdu.tlvs();
        assertThat(resultList.size(), is(1));
    }

    /**
     * Tests tlvs()  method.
     */
    @Test
    public void testTlvs() throws Exception {
        helloPdu.addTlv(isisTlv);
        resultList = helloPdu.tlvs();
        assertThat(resultList.size(), is(1));
    }

    /**
     * Tests areaAddres()  method.
     */
    @Test
    public void testAreaAddress() throws Exception {
        channelBuffer = ChannelBuffers.copiedBuffer(helloL1L2);
        helloPdu.readFrom(channelBuffer);
        resultListStr = helloPdu.areaAddress();
        assertThat(resultListStr.size(), is(1));
    }

    /**
     * Tests interfaceIpAddresse() method.
     */
    @Test
    public void testInterfaceIpAddresses() throws Exception {
        channelBuffer = ChannelBuffers.copiedBuffer(helloL1L2);
        helloPdu.readFrom(channelBuffer);
        resultListIPv4 = helloPdu.interfaceIpAddresses();
        assertThat(resultListIPv4.size(), is(1));
    }

    /**
     * Tests neighborList() method.
     */
    @Test
    public void testNeighborList() throws Exception {
        channelBuffer = ChannelBuffers.copiedBuffer(helloL1L2);
        helloPdu.readFrom(channelBuffer);
        resultListMac = helloPdu.neighborList();
        assertThat(resultListMac, is(nullValue()));
    }

    /**
     * Tests adjacencyState() method.
     */
    @Test
    public void testAdjacencyState() throws Exception {
        channelBuffer = ChannelBuffers.copiedBuffer(helloL1L2);
        helloPdu.readFrom(channelBuffer);
        resultAdjState = helloPdu.adjacencyState();
        assertThat(resultAdjState, is(nullValue()));
    }

    /**
     * Tests sourceId() getter method.
     */
    @Test
    public void testSourceId() throws Exception {
        helloPdu.setSourceId(srcId);
        resultStr = helloPdu.sourceId();
        assertThat(resultStr, is(srcId));
    }

    /**
     * Tests sourceId() setter method.
     */
    @Test
    public void testSetSourceId() throws Exception {
        helloPdu.setSourceId(srcId);
        resultStr = helloPdu.sourceId();
        assertThat(resultStr, is(srcId));
    }

    /**
     * Tests pduLength() getter method.
     */
    @Test
    public void testPduLength() throws Exception {
        helloPdu.setPduLength(10);
        resultInt = helloPdu.pduLength();
        assertThat(resultInt, is(10));

    }

    /**
     * Tests pduLength() setter method.
     */
    @Test
    public void testSetPduLength() throws Exception {
        helloPdu.setPduLength(10);
        resultInt = helloPdu.pduLength();
        assertThat(resultInt, is(10));
    }

    /**
     * Tests holdingTime() getter method.
     */
    @Test
    public void testHoldingTime() throws Exception {
        helloPdu.setHoldingTime(10);
        resultInt = helloPdu.holdingTime();
        assertThat(resultInt, is(10));
    }

    /**
     * Tests holdingTime() setter method.
     */
    @Test
    public void testSetHoldingTime() throws Exception {
        helloPdu.setHoldingTime(10);
        resultInt = helloPdu.holdingTime();
        assertThat(resultInt, is(10));
    }

    /**
     * Tests circuitType() getter method.
     */
    @Test
    public void testCircuitType() throws Exception {
        helloPdu.setCircuitType((byte) 1);
        resultByte = helloPdu.circuitType();
        assertThat(resultByte, is((byte) 1));
    }

    /**
     * Tests circuitType() setter method.
     */
    @Test
    public void testSetCircuitType() throws Exception {
        helloPdu.setCircuitType((byte) 1);
        resultByte = helloPdu.circuitType();
        assertThat(resultByte, is((byte) 1));
    }

    /**
     * Tests toString() getter method.
     */
    @Test
    public void testToString() throws Exception {
        pdu = helloPdu;
        assertThat(pdu.toString(), is(notNullValue()));
    }

    /**
     * Tests equals() method.
     */
    @Test
    public void testEquals() throws Exception {
        pdu = helloPdu;
        assertThat(pdu.equals(new L1L2HelloPdu(new IsisHeader())), is(true));
    }

    /**
     * Tests hashCode()  method.
     */
    @Test
    public void testHashCode() throws Exception {
        pdu = helloPdu;
        resultInt = pdu.hashCode();
        assertThat(resultInt, is(notNullValue()));
    }
}