/*
 * Copyright 2016-present Open Networking Foundation
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
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Unit test class for NeighborForExtendedIs.
 *
 * Here we have passed a byte array containing data for 2 neighbors along with
 * their Sub TLVs. The test case checks whether the code is able to parse the Sub TLVs
 * for each neighbor or not. Along with this it also checks for neighbor id and metric
 * assigned to each neighbor.
 */
public class NeighborForExtendedIsTest {
    private final byte[] tlv = {
            (byte) 0x10, (byte) 0x00, (byte) 0x10, (byte) 0x00, (byte) 0x10, (byte) 0x02, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x0a, (byte) 0x3f, (byte) 0x06, (byte) 0x04, (byte) 0x14, (byte) 0x14, (byte) 0x14,
            (byte) 0xbe, (byte) 0x08, (byte) 0x04, (byte) 0x14, (byte) 0x14, (byte) 0x14, (byte) 0xd1, (byte) 0x09,
            (byte) 0x04, (byte) 0x49, (byte) 0x98, (byte) 0x96, (byte) 0x80, (byte) 0x0a, (byte) 0x04, (byte) 0x49,
            (byte) 0x98, (byte) 0x96, (byte) 0x80, (byte) 0x0b, (byte) 0x20, (byte) 0x49, (byte) 0x98, (byte) 0x96,
            (byte) 0x80, (byte) 0x49, (byte) 0x98, (byte) 0x96, (byte) 0x80, (byte) 0x49, (byte) 0x98, (byte) 0x96,
            (byte) 0x80, (byte) 0x49, (byte) 0x98, (byte) 0x96, (byte) 0x80, (byte) 0x49, (byte) 0x98, (byte) 0x96,
            (byte) 0x80, (byte) 0x49, (byte) 0x98, (byte) 0x96, (byte) 0x80, (byte) 0x49, (byte) 0x98, (byte) 0x96,
            (byte) 0x80, (byte) 0x49, (byte) 0x98, (byte) 0x96, (byte) 0x80, (byte) 0x12, (byte) 0x03, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x10, (byte) 0x00, (byte) 0x10, (byte) 0x00, (byte) 0x10, (byte) 0x01,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0a, (byte) 0x3f, (byte) 0x06, (byte) 0x04, (byte) 0x1e,
            (byte) 0x1e, (byte) 0x1e, (byte) 0xce, (byte) 0x08, (byte) 0x04, (byte) 0x1e, (byte) 0x1e, (byte) 0x1e,
            (byte) 0xa9, (byte) 0x09, (byte) 0x04, (byte) 0x49, (byte) 0x98, (byte) 0x96, (byte) 0x80, (byte) 0x0a,
            (byte) 0x04, (byte) 0x49, (byte) 0x98, (byte) 0x96, (byte) 0x80, (byte) 0x0b, (byte) 0x20, (byte) 0x49,
            (byte) 0x98, (byte) 0x96, (byte) 0x80, (byte) 0x49, (byte) 0x98, (byte) 0x96, (byte) 0x80, (byte) 0x49,
            (byte) 0x98, (byte) 0x96, (byte) 0x80, (byte) 0x49, (byte) 0x98, (byte) 0x96, (byte) 0x80, (byte) 0x49,
            (byte) 0x98, (byte) 0x96, (byte) 0x80, (byte) 0x49, (byte) 0x98, (byte) 0x96, (byte) 0x80, (byte) 0x49,
            (byte) 0x98, (byte) 0x96, (byte) 0x80, (byte) 0x49, (byte) 0x98, (byte) 0x96, (byte) 0x80, (byte) 0x12,
            (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x00
    };

    //tlv2 bytes are for testing the else part of readFrom() method
    private final byte[] tlv2 = {
            (byte) 0x10, (byte) 0x00, (byte) 0x10, (byte) 0x00, (byte) 0x10, (byte) 0x02, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x0a, (byte) 0x06, (byte) 0x2D, (byte) 0x04, (byte) 0x14, (byte) 0x14, (byte) 0x14,
            (byte) 0xbe, (byte) 0x2D, (byte) 0xd1, (byte) 0x14, (byte) 0x14, (byte) 0x14, (byte) 0xd1, (byte) 0x09
    };
    private final String neighborId1 = "1000.1000.1002.00";
    private final String neighborId2 = "1000.1000.1001.00";
    private final int metric = 10;
    private final int subTlvLength = 6;
    private NeighborForExtendedIs neighborForExtendedIs;
    private ChannelBuffer channelBuffer;

    @Before
    public void setUp() throws Exception {
        neighborForExtendedIs = new NeighborForExtendedIs();
        channelBuffer = EasyMock.createMock(ChannelBuffer.class);
    }

    @After
    public void tearDown() throws Exception {
        neighborForExtendedIs = null;
        channelBuffer = null;
    }

    /**
     * Tests readFrom() method.
     */
    @Test
    public void testReadFrom() throws Exception {
        channelBuffer = ChannelBuffers.copiedBuffer(tlv);
        neighborForExtendedIs.readFrom(channelBuffer);
        assertThat(neighborForExtendedIs.teSubTlv().size(), is(subTlvLength));
        assertThat(neighborForExtendedIs.neighborId(), is(neighborId1));
        assertThat(neighborForExtendedIs.metric(), is(metric));

        neighborForExtendedIs = new NeighborForExtendedIs();
        neighborForExtendedIs.readFrom(channelBuffer);
        assertThat(neighborForExtendedIs.teSubTlv().size(), is(subTlvLength));
        assertThat(neighborForExtendedIs.neighborId(), is(neighborId2));
        assertThat(neighborForExtendedIs.metric(), is(metric));
    }

    /**
     * Tests else condition of readFrom() method.
     */
    @Test
    public void testElsePartOfReadFrom() throws Exception {
        channelBuffer = ChannelBuffers.copiedBuffer(tlv2);
        neighborForExtendedIs = new NeighborForExtendedIs();
        neighborForExtendedIs.readFrom(channelBuffer);
        assertThat(neighborForExtendedIs.neighborId(), is(neighborId1));
        assertThat(neighborForExtendedIs.metric(), is(metric));
    }
}
