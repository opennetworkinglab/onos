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
 * Unit test class for LinkType.
 */
public class LinkTypeTest {

    private final byte[] packet = {0, 0, 0, 1};
    private final byte[] packet1 = {0, 0, 1};
    private LinkType linkType;
    private byte[] result;
    private ChannelBuffer channelBuffer;
    private TlvHeader tlvHeader;

    @Before
    public void setUp() throws Exception {
        linkType = new LinkType();
    }

    @After
    public void tearDown() throws Exception {
        linkType = null;
        channelBuffer = null;
        tlvHeader = null;
    }

    /**
     * Tests to string method.
     */
    @Test
    public void testToString() throws Exception {
        assertThat(linkType.toString(), is(notNullValue()));
    }

    /**
     * Tests linkType() getter method.
     */
    @Test
    public void testGetLinkType() throws Exception {
        linkType.setLinkType(1);
        assertThat(linkType, is(notNullValue()));
    }

    /**
     * Tests linkType() setter method.
     */
    @Test
    public void testSetLinkType() throws Exception {
        linkType.setLinkType(1);
        assertThat(linkType, is(notNullValue()));
    }

    /**
     * Tests readFrom() method.
     */
    @Test
    public void testReadFrom() throws Exception {
        tlvHeader = new TlvHeader();
        tlvHeader.setTlvType(1);
        tlvHeader.setTlvLength(4);
        linkType = new LinkType(tlvHeader);
        channelBuffer = ChannelBuffers.copiedBuffer(packet);
        linkType.readFrom(channelBuffer);
        assertThat(linkType, is(notNullValue()));
    }

    /**
     * Tests readFrom() method.
     */
    @Test
    public void testReadFrom1() throws Exception {
        tlvHeader = new TlvHeader();
        tlvHeader.setTlvType(1);
        tlvHeader.setTlvLength(4);
        linkType = new LinkType(tlvHeader);
        channelBuffer = ChannelBuffers.copiedBuffer(packet1);
        linkType.readFrom(channelBuffer);
        assertThat(linkType, is(notNullValue()));
    }

    /**
     * Tests asBytes() method.
     */
    @Test
    public void testAsBytes() throws Exception {
        result = linkType.asBytes();
        assertThat(result, is(notNullValue()));
    }


    /**
     * Tests getLinkSubTypeTlvBodyAsByteArray() method.
     */
    @Test
    public void testGetLinkSubTypeTlvBodyAsByteArray() throws Exception {
        result = linkType.getLinkSubTypeTlvBodyAsByteArray();
        assertThat(result, is(notNullValue()));
    }
}