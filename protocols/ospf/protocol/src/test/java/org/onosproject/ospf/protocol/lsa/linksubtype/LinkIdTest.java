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
package org.onosproject.ospf.protocol.lsa.linksubtype;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.ospf.protocol.lsa.TlvHeader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit test class for LinkId.
 */
public class LinkIdTest {

    private final byte[] packet = {1, 1, 1, 1};
    private final byte[] packet1 = {0, 0, 1};
    private LinkId linkId;
    private TlvHeader tlvHeader;
    private ChannelBuffer channelBuffer;
    private byte[] result;

    @Before
    public void setUp() throws Exception {
        linkId = new LinkId(new TlvHeader());
    }

    @After
    public void tearDown() throws Exception {
        linkId = null;
        channelBuffer = null;
        tlvHeader = null;
    }

    /**
     * Tests to string method.
     */
    @Test
    public void testToString() throws Exception {
        assertThat(linkId.toString(), is(notNullValue()));
    }

    /**
     * Tests linkId() getter method.
     */
    @Test
    public void testGetLinkId() throws Exception {
        linkId.setLinkId("1.1.1.1");
        assertThat(linkId, is(notNullValue()));
    }

    /**
     * Tests linkId() setter method.
     */
    @Test
    public void testSetLinkId() throws Exception {
        linkId.setLinkId("1.1.1.1");
        assertThat(linkId, is(notNullValue()));
    }

    /**
     * Tests readFrom() method.
     */
    @Test
    public void testReadFrom() throws Exception {
        tlvHeader = new TlvHeader();
        tlvHeader.setTlvType(2);
        tlvHeader.setTlvLength(4);
        linkId = new LinkId(tlvHeader);
        channelBuffer = ChannelBuffers.copiedBuffer(packet);
        linkId.readFrom(channelBuffer);
        assertThat(linkId, is(notNullValue()));
    }

    /**
     * Tests readFrom() method.
     */
    @Test(expected = Exception.class)
    public void testReadFrom1() throws Exception {
        tlvHeader = new TlvHeader();
        tlvHeader.setTlvType(2);
        tlvHeader.setTlvLength(4);
        linkId = new LinkId(tlvHeader);
        channelBuffer = ChannelBuffers.copiedBuffer(packet1);
        linkId.readFrom(channelBuffer);
        assertThat(linkId, is(notNullValue()));
    }

    /**
     * Tests asBytes() method.
     */
    @Test
    public void testAsBytes() throws Exception {
        result = linkId.asBytes();
        assertThat(result, is(notNullValue()));
    }

    /**
     * Tests getLinkSubTypeTlvBodyAsByteArray() method.
     */
    @Test
    public void testGetLinkSubTypeTlvBodyAsByteArray() throws Exception {
        result = linkId.getLinkSubTypeTlvBodyAsByteArray();
        assertThat(result, is(notNullValue()));
    }
}