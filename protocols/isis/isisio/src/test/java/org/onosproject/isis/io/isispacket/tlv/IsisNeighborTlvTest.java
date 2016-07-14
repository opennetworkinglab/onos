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
package org.onosproject.isis.io.isispacket.tlv;

import org.easymock.EasyMock;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.MacAddress;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Unit test class for ISIS NeighborTLV.
 */
public class IsisNeighborTlvTest {
    private final MacAddress macAddress = MacAddress.valueOf("a4:23:05:00:00:00");
    private final byte[] tlv = {0, 0, 0, 0, 0, 0};
    private TlvHeader tlvHeader;
    private IsisNeighborTlv isisNeighborTlv;
    private List<MacAddress> resultList;
    private ChannelBuffer channelBuffer;
    private byte[] result;

    @Before
    public void setUp() throws Exception {
        tlvHeader = new TlvHeader();
        isisNeighborTlv = new IsisNeighborTlv(tlvHeader);
        channelBuffer = EasyMock.createMock(ChannelBuffer.class);
    }

    @After
    public void tearDown() throws Exception {
        tlvHeader = null;
        isisNeighborTlv = null;
    }

    /**
     * Tests addNeighbor() getter method.
     */
    @Test
    public void testAddNeighbor() throws Exception {
        isisNeighborTlv.addNeighbor(macAddress);
        resultList = isisNeighborTlv.neighbor();
        assertThat(resultList.size(), is(1));

    }

    /**
     * Tests neighbor() setter method.
     */
    @Test
    public void testNeighbor() throws Exception {
        isisNeighborTlv.addNeighbor(macAddress);
        resultList = isisNeighborTlv.neighbor();
        assertThat(resultList.size(), is(1));
    }

    /**
     * Tests readFrom() method.
     */
    @Test
    public void testReadFrom() throws Exception {
        channelBuffer = ChannelBuffers.copiedBuffer(tlv);
        isisNeighborTlv.readFrom(channelBuffer);
        assertThat(isisNeighborTlv.neighbor().size(), is(1));
    }

    /**
     * Tests asBytes() getter method.
     */
    @Test
    public void testAsBytes() throws Exception {
        channelBuffer = ChannelBuffers.copiedBuffer(tlv);
        isisNeighborTlv.readFrom(channelBuffer);
        result = isisNeighborTlv.asBytes();
        assertThat(result, is(notNullValue()));
    }

    /**
     * Tests toString() method.
     */
    @Test
    public void testToString() throws Exception {
        assertThat(isisNeighborTlv.toString(), is(notNullValue()));
    }
}