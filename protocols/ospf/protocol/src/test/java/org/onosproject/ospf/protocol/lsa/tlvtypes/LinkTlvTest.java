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
package org.onosproject.ospf.protocol.lsa.tlvtypes;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.ospf.protocol.lsa.TlvHeader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Unit test class for LinkTlv.
 */
public class LinkTlvTest {

    private final byte[] packet1 = {0, 9, 0, 4, 0, 0, 0, 1};
    private final byte[] packet2 = {0, 1, 0, 4, 0, 0, 0, 1};
    private final byte[] packet3 = {0, 2, 0, 4, 0, 0, 0, 1};
    private final byte[] packet4 = {0, 3, 0, 4, 0, 0, 0, 1};
    private final byte[] packet5 = {0, 4, 0, 4, 0, 0, 0, 1};
    private final byte[] packet6 = {0, 6, 0, 4, 0, 0, 0, 1};
    private final byte[] packet7 = {0, 7, 0, 4, 0, 0, 0, 1};
    private final byte[] packet8 = {0, 8, 0, 4, 0, 0, 0, 1};
    private final byte[] packet9 = {0, 9, 0, 4, 0, 0, 0, 1,
            0, 9, 0, 4, 0, 0, 0, 1,
            0, 1, 0, 4, 0, 0, 0, 1,
            0, 2, 0, 4, 0, 0, 0, 1,
            0, 3, 0, 4, 0, 0, 0, 1,
            0, 4, 0, 4, 0, 0, 0, 1,
            0, 6, 0, 4, 0, 0, 0, 1,
            0, 7, 0, 4, 0, 0, 0, 1,
            0, 8, 0, 4, 0, 0, 0, 1,
    };
    private LinkTlv linkTlv;
    private TlvHeader header;
    private ChannelBuffer channelBuffer;
    private byte[] result;

    @Before
    public void setUp() throws Exception {
        linkTlv = new LinkTlv(new TlvHeader());

    }

    @After
    public void tearDown() throws Exception {
        linkTlv = null;
        header = null;
        channelBuffer = null;
        result = null;
    }

    /**
     * Tests to string method.
     */
    @Test
    public void testToString() throws Exception {
        assertThat(linkTlv.toString(), is(notNullValue()));
    }

    /**
     * Tests readFrom() method.
     */
    @Test
    public void testReadFrom() throws Exception {

        header = new TlvHeader();
        header.setTlvLength(8);
        header.setTlvType(9);
        linkTlv = new LinkTlv(header);
        channelBuffer = ChannelBuffers.copiedBuffer(packet1);
        linkTlv.readFrom(channelBuffer);
        assertThat(linkTlv, is(notNullValue()));

        header = new TlvHeader();
        header.setTlvLength(8);
        header.setTlvType(1);
        linkTlv = new LinkTlv(header);
        channelBuffer = ChannelBuffers.copiedBuffer(packet2);
        linkTlv.readFrom(channelBuffer);
        assertThat(linkTlv, is(notNullValue()));

        header = new TlvHeader();
        header.setTlvLength(8);
        header.setTlvType(2);
        linkTlv = new LinkTlv(header);
        channelBuffer = ChannelBuffers.copiedBuffer(packet3);
        linkTlv.readFrom(channelBuffer);
        assertThat(linkTlv, is(notNullValue()));

        header = new TlvHeader();
        header.setTlvLength(8);
        header.setTlvType(3);
        linkTlv = new LinkTlv(header);
        channelBuffer = ChannelBuffers.copiedBuffer(packet4);
        linkTlv.readFrom(channelBuffer);
        assertThat(linkTlv, is(notNullValue()));

        header = new TlvHeader();
        header.setTlvLength(8);
        header.setTlvType(4);
        linkTlv = new LinkTlv(header);
        channelBuffer = ChannelBuffers.copiedBuffer(packet5);
        linkTlv.readFrom(channelBuffer);
        assertThat(linkTlv, is(notNullValue()));

        header = new TlvHeader();
        header.setTlvLength(8);
        header.setTlvType(5);
        linkTlv = new LinkTlv(header);
        channelBuffer = ChannelBuffers.copiedBuffer(packet1);
        linkTlv.readFrom(channelBuffer);
        assertThat(linkTlv, is(notNullValue()));

        header = new TlvHeader();
        header.setTlvLength(8);
        header.setTlvType(6);
        linkTlv = new LinkTlv(header);
        channelBuffer = ChannelBuffers.copiedBuffer(packet6);
        linkTlv.readFrom(channelBuffer);
        assertThat(linkTlv, is(notNullValue()));

        header = new TlvHeader();
        header.setTlvLength(8);
        header.setTlvType(7);
        linkTlv = new LinkTlv(header);
        channelBuffer = ChannelBuffers.copiedBuffer(packet7);
        linkTlv.readFrom(channelBuffer);
        assertThat(linkTlv, is(notNullValue()));

        header = new TlvHeader();
        header.setTlvLength(8);
        header.setTlvType(8);
        linkTlv = new LinkTlv(header);
        channelBuffer = ChannelBuffers.copiedBuffer(packet8);
        linkTlv.readFrom(channelBuffer);
        assertThat(linkTlv, is(notNullValue()));
        assertThat(linkTlv, instanceOf(LinkTlv.class));
    }

    /**
     * Tests asBytes() method.
     */
    @Test
    public void testAsBytes() throws Exception {
        result = linkTlv.asBytes();
        assertThat(result, is(notNullValue()));
    }


    /**
     * Tests getTlvBodyAsByteArray() method.
     */
    @Test
    public void testGetTlvBodyAsByteArray() throws Exception {

        channelBuffer = ChannelBuffers.copiedBuffer(packet9);
        linkTlv.readFrom(channelBuffer);

        result = linkTlv.getTlvBodyAsByteArray();
        assertThat(result, is(notNullValue()));
    }
}