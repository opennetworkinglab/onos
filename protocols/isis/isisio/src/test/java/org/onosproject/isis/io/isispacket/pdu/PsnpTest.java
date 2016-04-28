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
 * Unit test class for Psnp.
 */
public class PsnpTest {
    private final String srcId = "1111.1111.1111";
    private final byte[] psnpPkt = {
            0, 35, 41, 41, 41, 41, 41, 41, 0, 9, 16, 4, -81, 18, 52, 18,
            52, 0, 18, 0, 0, 0, 0, 0, 42, -94, -29
    };
    private Psnp psnp;
    private IsisHeader isisHeader;
    private ChannelBuffer channelBuffer;
    private IsisTlv isisTlv;
    private TlvHeader tlvHeader;
    private List<IsisTlv> resultList;
    private String resultStr;
    private int resultInt;
    private byte[] result;

    @Before
    public void setUp() throws Exception {
        isisHeader = new IsisHeader();
        isisHeader.setIsisPduType(IsisPduType.L1PSNP.value());
        psnp = new Psnp(isisHeader);
        tlvHeader = new TlvHeader();
        channelBuffer = EasyMock.createMock(ChannelBuffer.class);
        isisTlv = new AdjacencyStateTlv(tlvHeader);

    }

    @After
    public void tearDown() throws Exception {
        isisHeader = null;
        psnp = null;
        channelBuffer = null;
        tlvHeader = null;
        isisTlv = null;
        tlvHeader = null;
    }

    /**
     * Tests addTlv() method.
     */
    @Test
    public void testAddTlv() throws Exception {
        psnp.addTlv(isisTlv);
        resultList = psnp.getAllTlv();
        assertThat(resultList.size(), is(1));

    }

    /**
     * Tests getAllTlv() method.
     */
    @Test
    public void testGetAllTlv() throws Exception {
        psnp.addTlv(isisTlv);
        resultList = psnp.getAllTlv();
        assertThat(resultList.size(), is(1));

    }

    /**
     * Tests sourceId() getter method.
     */
    @Test
    public void testSourceId() throws Exception {
        psnp.setSourceId(srcId);
        resultStr = psnp.sourceId();
        assertThat(resultStr, is(srcId));
    }

    /**
     * Tests sourceId() setter method.
     */
    @Test
    public void testSetSourceId() throws Exception {
        psnp.setSourceId(srcId);
        resultStr = psnp.sourceId();
        assertThat(resultStr, is(srcId));
    }

    /**
     * Tests pduLength() getter method.
     */
    @Test
    public void testPduLength() throws Exception {
        psnp.setPduLength(10);
        resultInt = psnp.pduLength();
        assertThat(resultInt, is(10));
    }

    /**
     * Tests pduLength() setter method.
     */
    @Test
    public void testSetPduLength() throws Exception {
        psnp.setPduLength(10);
        resultInt = psnp.pduLength();
        assertThat(resultInt, is(10));
    }

    /**
     * Tests readFrom() method.
     */
    @Test
    public void testReadFrom() throws Exception {
        channelBuffer = ChannelBuffers.copiedBuffer(psnpPkt);
        psnp.readFrom(channelBuffer);
        assertThat(psnp, is(notNullValue()));
    }

    /**
     * Tests lasBytes() method.
     */
    @Test
    public void testAsBytes() throws Exception {
        channelBuffer = ChannelBuffers.copiedBuffer(psnpPkt);
        psnp.readFrom(channelBuffer);
        result = psnp.asBytes();
        assertThat(result, is(notNullValue()));
    }

    /**
     * Tests isisPduHeader() method.
     */
    @Test
    public void testIsisPduHeader() throws Exception {
        result = psnp.isisPduHeader();
        assertThat(result, is(notNullValue()));
    }

    /**
     * Tests partialSequenceNumberPduBody() method.
     */
    @Test
    public void testPartialSequenceNumberPduBody() throws Exception {
        channelBuffer = ChannelBuffers.copiedBuffer(psnpPkt);
        psnp.readFrom(channelBuffer);
        result = psnp.partialSequenceNumberPduBody();
        assertThat(result, is(notNullValue()));
    }

    /**
     * Tests toString() method.
     */
    @Test
    public void testToString() throws Exception {
        assertThat((psnp.toString()), is(notNullValue()));
    }

    /**
     * Tests equals() method.
     */
    @Test
    public void testEquals() throws Exception {
        assertThat(psnp.equals(new Psnp(new IsisHeader())), is(true));
    }

    /**
     * Tests hashCode() method.
     */
    @Test
    public void testHashCode() throws Exception {
        int hashCode = psnp.hashCode();
        assertThat(hashCode, is(notNullValue()));
    }
}
