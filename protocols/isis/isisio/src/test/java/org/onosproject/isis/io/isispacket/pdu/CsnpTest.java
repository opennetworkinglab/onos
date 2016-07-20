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
import org.onosproject.isis.io.isispacket.tlv.AdjacencyStateTlv;
import org.onosproject.isis.io.isispacket.tlv.IsisTlv;
import org.onosproject.isis.io.isispacket.tlv.TlvHeader;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit test class for Csnp.
 */
public class CsnpTest {

    private final String srcId = "1111.1111.1111";
    private final byte[] csnpBytes = {
            0, 67, 18, 52, 18, 52, 0, 0, 67, 18, 52, 18, 52, 0,
            18, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, -1, -1, -1, -1,
            -1, -1, 9, 32, 4, -81, 18, 52, 18, 52, 0, 18, 0, 0, 0,
            0, 0, 41, -92, -30, 4, -81, 41, 41, 41, 41, 41, 41, 0,
            0, 0, 0, 0, 1, 91, 126, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0
    };
    private Csnp csnp;
    private IsisHeader isisHeader;
    private IsisTlv isisTlv;
    private TlvHeader tlvHeader;
    private List<IsisTlv> resultList;
    private String resultStr;
    private int resultInt;
    private ChannelBuffer channelBuffer;
    private byte[] result;

    @Before
    public void setUp() throws Exception {
        isisHeader = new IsisHeader();
        isisHeader.setIsisPduType(IsisPduType.L1CSNP.value());
        csnp = new Csnp(isisHeader);
        tlvHeader = new TlvHeader();
        isisTlv = new AdjacencyStateTlv(tlvHeader);
        channelBuffer = EasyMock.createMock(ChannelBuffer.class);
    }

    @After
    public void tearDown() throws Exception {
        isisHeader = null;
        csnp = null;
        tlvHeader = null;
        isisTlv = null;
    }

    /**
     * Tests getAllTlv()  method.
     */
    @Test
    public void testGetAllTlv() throws Exception {
        channelBuffer = ChannelBuffers.copiedBuffer(csnpBytes);
        csnp.readFrom(channelBuffer);
        resultList = csnp.getAllTlv();
        assertThat(resultList.size(), is(0));
    }

    /**
     * Tests sourceId() getter method.
     */
    @Test
    public void testSourceId() throws Exception {
        csnp.setSourceId(srcId);
        resultStr = csnp.sourceId();
        assertThat(resultStr, is(srcId));
    }

    /**
     * Tests sourceId() setter method.
     */
    @Test
    public void testSetSourceId() throws Exception {
        csnp.setSourceId(srcId);
        resultStr = csnp.sourceId();
        assertThat(resultStr, is(srcId));
    }


    /**
     * Tests startLspId() getter method.
     */
    @Test
    public void testStartLspId() throws Exception {
        csnp.setStartLspId(srcId);
        resultStr = csnp.startLspId();
        assertThat(resultStr, is(srcId));
    }

    /**
     * Tests startLspId() setter method.
     */
    @Test
    public void testSetStartLspId() throws Exception {
        csnp.setStartLspId(srcId);
        resultStr = csnp.startLspId();
        assertThat(resultStr, is(srcId));
    }

    /**
     * Tests endLspId()  getter method.
     */
    @Test
    public void testEndLspId() throws Exception {
        csnp.setEndLspId(srcId);
        resultStr = csnp.endLspId();
        assertThat(resultStr, is(srcId));
    }

    /**
     * Tests endLspId() setter method.
     */
    @Test
    public void testSetEndLspId() throws Exception {
        csnp.setEndLspId(srcId);
        resultStr = csnp.endLspId();
        assertThat(resultStr, is(srcId));
    }

    /**
     * Tests pduLength() getter method.
     */
    @Test
    public void testPduLength() throws Exception {
        csnp.setPduLength(10);
        resultInt = csnp.pduLength();
        assertThat(resultInt, is(10));
    }

    /**
     * Tests pduLength() setter method.
     */
    @Test
    public void testSetPduLength() throws Exception {
        csnp.setPduLength(10);
        resultInt = csnp.pduLength();
        assertThat(resultInt, is(10));
    }

    /**
     * Tests readFrom() method.
     */
    @Test
    public void testReadFrom() throws Exception {
        channelBuffer = ChannelBuffers.copiedBuffer(csnpBytes);
        csnp.readFrom(channelBuffer);
        assertThat(csnp, is(notNullValue()));
    }

    /**
     * Tests asBytes() method.
     */
    @Test
    public void testAsBytes() throws Exception {
        channelBuffer = ChannelBuffers.copiedBuffer(csnpBytes);
        csnp.readFrom(channelBuffer);
        result = csnp.asBytes();
        assertThat(csnp, is(notNullValue()));
    }

    /**
     * Tests isisPduHeader() method.
     */
    @Test
    public void testIsisPduHeader() throws Exception {
        result = csnp.isisPduHeader();
        assertThat(result, is(notNullValue()));
    }

    /**
     * Tests completeSequenceNumberPduBody() method.
     */
    @Test
    public void testCompleteSequenceNumberPduBody() throws Exception {
        channelBuffer = ChannelBuffers.copiedBuffer(csnpBytes);
        csnp.readFrom(channelBuffer);
        result = csnp.completeSequenceNumberPduBody();
        assertThat(result, is(notNullValue()));
    }

    /**
     * Tests toString() method.
     */
    @Test
    public void testToString() throws Exception {
        assertThat((csnp.toString()), is(notNullValue()));
    }

    /**
     * Tests equals() method.
     */
    @Test
    public void testEquals() throws Exception {
        assertThat(csnp.equals(new Csnp(new IsisHeader())), is(true));
    }

    /**
     * Tests hashCode() method.
     */
    @Test
    public void testHashCode() throws Exception {
        int hashCode = csnp.hashCode();
        assertThat(hashCode, is(notNullValue()));
    }
}



