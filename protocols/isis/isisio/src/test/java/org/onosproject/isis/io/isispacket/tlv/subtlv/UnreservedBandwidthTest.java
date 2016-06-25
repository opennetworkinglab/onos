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
package org.onosproject.isis.io.isispacket.tlv.subtlv;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.isis.io.isispacket.tlv.TlvHeader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit test class for UnreservedBandwidth.
 */
public class UnreservedBandwidthTest {

    private final byte[] packet = {0, 0, 0, 1};
    private UnreservedBandwidth unreservedBandwidth;
    private TlvHeader header;
    private byte[] result;
    private ChannelBuffer channelBuffer;

    @Before
    public void setUp() throws Exception {
        unreservedBandwidth = new UnreservedBandwidth(new TlvHeader());
    }

    @After
    public void tearDown() throws Exception {
        unreservedBandwidth = null;
        header = null;
        result = null;
        channelBuffer = null;
    }

    /**
     * Tests to string method.
     */
    @Test
    public void testToString() throws Exception {
        assertThat(unreservedBandwidth.toString(), is(notNullValue()));
    }

    /**
     * Tests addUnReservedBandwidth() method.
     */
    @Test
    public void testAddUnReservedBandwidth() throws Exception {
        unreservedBandwidth.addUnReservedBandwidth(123456.78f);
        assertThat(unreservedBandwidth, is(notNullValue()));
    }

    /**
     * Tests readFrom() method.
     */
    @Test
    public void testReadFrom() throws Exception {
        header = new TlvHeader();
        header.setTlvLength(4);
        header.setTlvType(8);
        channelBuffer = ChannelBuffers.copiedBuffer(packet);
        unreservedBandwidth = new UnreservedBandwidth(header);
        unreservedBandwidth.readFrom(channelBuffer);
        unreservedBandwidth.readFrom(channelBuffer);
        assertThat(unreservedBandwidth, is(notNullValue()));
    }

    /**
     * Tests asBytes() method.
     */
    @Test
    public void testAsBytes() throws Exception {
        result = unreservedBandwidth.asBytes();
        assertThat(result, is(notNullValue()));
    }

    /**
     * Tests getLinkSubTypeTlvBodyAsByteArray() method.
     */
    @Test
    public void testGetLinkSubTypeTlvBodyAsByteArray() throws Exception {
        result = unreservedBandwidth.asBytes();
        assertThat(result, is(notNullValue()));
    }
}