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
import org.onosproject.isis.controller.IsisPduType;
import org.onosproject.isis.io.isispacket.IsisHeader;
import org.onosproject.isis.io.isispacket.tlv.HostNameTlv;
import org.onosproject.isis.io.isispacket.tlv.TlvHeader;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit test class for LsPdu.
 */
public class LsPduTest {
    private final String lspId = "1111.1111.1111";
    private final byte[] l1Lsp = {
            0, 86, 4, -81, 34, 34, 34, 34, 34, 34, 0, 0, 0, 0, 0, 9, 99, 11, 1, 1, 4, 3, 73,
            0, 10, -127, 1, -52, -119, 2, 82, 50, -124, 4, -64, -88, 10, 1, -128, 24, 10,
            -128, -128, -128, 10, 0, 10, 0, -1, -1, -1, -4, 10, -128, -128, -128, -64, -88,
            10, 0, -1, -1, -1, 0, 2, 12, 0, 10, -128, -128, -128, 51, 51, 51, 51, 51, 51, 2
    };
    private LsPdu lsPdu;
    private IsisHeader isisHeader;
    private TlvHeader tlvHeader;
    private int resultInt;
    private boolean resultBool;
    private byte resultByte;
    private AttachedToOtherAreas resultObj;
    private String resultStr;
    private ChannelBuffer channelBuffer;
    private byte[] result;

    @Before
    public void setUp() throws Exception {
        isisHeader = new IsisHeader();
        tlvHeader = new TlvHeader();
        isisHeader.setIsisPduType(IsisPduType.L1LSPDU.value());
        lsPdu = new LsPdu(isisHeader);
        channelBuffer = EasyMock.createMock(ChannelBuffer.class);
    }

    @After
    public void tearDown() throws Exception {
        isisHeader = null;
        lsPdu = null;
        tlvHeader = null;
        channelBuffer = null;
    }

    /**
     * Tests addTlv()  method.
     */
    @Test
    public void testAddTlv() throws Exception {
        lsPdu.addTlv(new HostNameTlv(tlvHeader));
        assertThat(lsPdu, is(notNullValue()));
    }

    /**
     * Tests remainingLifeTime() getter method.
     */
    @Test
    public void testRemainingLifeTime() throws Exception {
        lsPdu.setRemainingLifeTime(10);
        resultInt = lsPdu.remainingLifeTime();
        assertThat(resultInt, is(10));
    }

    /**
     * Tests remainingLifeTime() setter method.
     */
    @Test
    public void testSetRemainingLifeTime() throws Exception {
        lsPdu.setRemainingLifeTime(10);
        resultInt = lsPdu.remainingLifeTime();
        assertThat(resultInt, is(10));
    }

    /**
     * Tests lspDbol() getter method.
     */
    @Test
    public void testLspDbol() throws Exception {
        lsPdu.setLspDbol(true);
        resultBool = lsPdu.lspDbol();
        assertThat(resultBool, is(true));
    }

    /**
     * Tests lspDbol() setter method.
     */
    @Test
    public void testSetLspDbol() throws Exception {
        lsPdu.setLspDbol(true);
        resultBool = lsPdu.lspDbol();
        assertThat(resultBool, is(true));
    }

    /**
     * Tests typeBlock() getter method.
     */
    @Test
    public void testTypeBlock() throws Exception {
        lsPdu.setTypeBlock((byte) 1);
        resultByte = lsPdu.typeBlock();
        assertThat(resultByte, is((byte) 1));
    }

    /**
     * Tests typeBlock() setter method.
     */
    @Test
    public void testSetTypeBlock() throws Exception {
        lsPdu.setTypeBlock((byte) 1);
        resultByte = lsPdu.typeBlock();
        assertThat(resultByte, is((byte) 1));
    }

    /**
     * Tests sequenceNumber() getter method.
     */
    @Test
    public void testSequenceNumber() throws Exception {
        lsPdu.setSequenceNumber(1);
        resultInt = lsPdu.sequenceNumber();
        assertThat(resultInt, is(1));
    }

    /**
     * Tests sequenceNumber() setter method.
     */
    @Test
    public void testSetSequenceNumber() throws Exception {
        lsPdu.setSequenceNumber(1);
        resultInt = lsPdu.sequenceNumber();
        assertThat(resultInt, is(1));
    }

    /**
     * Tests checkSum() getter method.
     */
    @Test
    public void testCheckSum() throws Exception {
        lsPdu.setCheckSum(1);
        resultInt = lsPdu.checkSum();
        assertThat(resultInt, is(1));
    }

    /**
     * Tests checkSum() setter method.
     */
    @Test
    public void testSetCheckSum() throws Exception {
        lsPdu.setCheckSum(1);
        resultInt = lsPdu.checkSum();
        assertThat(resultInt, is(1));
    }

    /**
     * Tests partitionRepair() getter method.
     */
    @Test
    public void testPartitionRepair() throws Exception {
        lsPdu.setPartitionRepair(true);
        resultBool = lsPdu.partitionRepair();
        assertThat(resultBool, is(true));
    }

    /**
     * Tests partitionRepair() setter method.
     */
    @Test
    public void testSetPartitionRepair() throws Exception {
        lsPdu.setPartitionRepair(true);
        resultBool = lsPdu.partitionRepair();
        assertThat(resultBool, is(true));
    }

    /**
     * Tests attachedToOtherAreas() getter method.
     */
    @Test
    public void testAttachedToOtherAreas() throws Exception {
        lsPdu.setAttachedToOtherAreas(AttachedToOtherAreas.DEFAULTMETRIC);
        resultObj = lsPdu.attachedToOtherAreas();
        assertThat(resultObj, is(AttachedToOtherAreas.DEFAULTMETRIC));
    }

    /**
     * Tests attachedToOtherAreas() setter method.
     */
    @Test
    public void testSetAttachedToOtherAreas() throws Exception {
        lsPdu.setAttachedToOtherAreas(AttachedToOtherAreas.DEFAULTMETRIC);
        resultObj = lsPdu.attachedToOtherAreas();
        assertThat(resultObj, is(AttachedToOtherAreas.DEFAULTMETRIC));
    }

    /**
     * Tests intermediateSystemType() getter method.
     */
    @Test
    public void testIntermediateSystemType() throws Exception {
        lsPdu.setIntermediateSystemType((byte) 1);
        resultByte = lsPdu.intermediateSystemType();
        assertThat(resultByte, is((byte) 1));
    }

    /**
     * Tests intermediateSystemType() setter method.
     */
    @Test
    public void testSetIntermediateSystemType() throws Exception {
        lsPdu.setIntermediateSystemType((byte) 1);
        resultByte = lsPdu.intermediateSystemType();
        assertThat(resultByte, is((byte) 1));
    }

    /**
     * Tests lspId() getter method.
     */
    @Test
    public void testLspId() throws Exception {
        lsPdu.setLspId(lspId);
        resultStr = lsPdu.lspId();
        assertThat(resultStr, is(lspId));
    }

    /**
     * Tests lspId() setter method.
     */
    @Test
    public void testSetLspId() throws Exception {
        lsPdu.setLspId(lspId);
        resultStr = lsPdu.lspId();
        assertThat(resultStr, is(lspId));
    }

    /**
     * Tests pduLength() getter method.
     */
    @Test
    public void testPduLength() throws Exception {
        lsPdu.setPduLength(10);
        resultInt = lsPdu.pduLength();
        assertThat(resultInt, is(10));
    }

    /**
     * Tests pduLength() setter method.
     */
    @Test
    public void testSetPduLength() throws Exception {
        lsPdu.setPduLength(10);
        resultInt = lsPdu.pduLength();
        assertThat(resultInt, is(10));
    }

    /**
     * Tests readFrom() method.
     */
    @Test
    public void testReadFrom() throws Exception {
        channelBuffer = ChannelBuffers.copiedBuffer(l1Lsp);
        lsPdu.readFrom(channelBuffer);
        assertThat(lsPdu, is(notNullValue()));
    }

    /**
     * Tests asBytes()  method.
     */
    @Test
    public void testAsBytes() throws Exception {
        channelBuffer = ChannelBuffers.copiedBuffer(l1Lsp);
        lsPdu.readFrom(channelBuffer);
        result = lsPdu.asBytes();
        assertThat(result, is(notNullValue()));
    }

    /**
     * Tests l1l2IsisPduHeader()  method.
     */
    @Test
    public void testL1l2IsisPduHeader() throws Exception {
        result = lsPdu.l1l2IsisPduHeader();
        assertThat(result, is(notNullValue()));
    }

    /**
     * Tests l1l2LsPduBody()  method.
     */
    @Test
    public void testL1l2LsPduBody() throws Exception {
        channelBuffer = ChannelBuffers.copiedBuffer(l1Lsp);
        lsPdu.readFrom(channelBuffer);
        result = lsPdu.l1l2LsPduBody();
        assertThat(result, is(notNullValue()));
    }

    /**
     * Tests toString()  method.
     */
    @Test
    public void testToString() throws Exception {
        assertThat(lsPdu.toString(), is(notNullValue()));
    }

    /**
     * Tests equals()  method.
     */
    @Test
    public void testEquals() throws Exception {
        assertThat(lsPdu.equals(new LsPdu(new IsisHeader())), is(true));
    }

    /**
     * Tests hashCode()  method.
     */
    @Test
    public void testHashCode() throws Exception {
        resultInt = lsPdu.hashCode();
        assertThat(resultInt, is(notNullValue()));
    }
}
