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
 * Unit test class for UnknownLinkSubType.
 */
public class UnknownLinkSubTypeTest {
    private final byte[] packet = {0, 114, 0, 4, 0, 0, 0, 1};
    private UnknownLinkSubType unknownLinkSubType;
    private TlvHeader header;
    private ChannelBuffer channelBuffer;
    private byte[] result;

    @Before
    public void setUp() throws Exception {
        unknownLinkSubType = new UnknownLinkSubType(new TlvHeader());
    }

    @After
    public void tearDown() throws Exception {
        unknownLinkSubType = null;
        header = null;
    }

    /**
     * Tests value() getter method.
     */
    @Test
    public void testValue() throws Exception {
        unknownLinkSubType.setValue(packet);
        assertThat(unknownLinkSubType.value(), is(notNullValue()));
    }

    /**
     * Tests value() setter method.
     */
    @Test
    public void testSetValue() throws Exception {
        unknownLinkSubType.setValue(packet);
        assertThat(unknownLinkSubType.value(), is(notNullValue()));
    }

    /**
     * Tests readFrom() method.
     */
    @Test
    public void testReadFrom() throws Exception {
        header = new TlvHeader();
        header.setTlvLength(8);
        header.setTlvType(114);
        unknownLinkSubType = new UnknownLinkSubType(header);
        channelBuffer = ChannelBuffers.copiedBuffer(packet);
        unknownLinkSubType.readFrom(channelBuffer);
        assertThat(unknownLinkSubType, is(notNullValue()));
    }

    /**
     * Tests asBytes() method.
     */
    @Test(expected = Exception.class)
    public void testAsBytes() throws Exception {
        result = unknownLinkSubType.asBytes();
        assertThat(result, is(notNullValue()));
    }


    /**
     * Tests getLinkSubTypeTlvBodyAsByteArray() method.
     */
    @Test
    public void testGetLinkSubTypeTlvBodyAsByteArray() throws Exception {
        channelBuffer = ChannelBuffers.copiedBuffer(packet);
        unknownLinkSubType.readFrom(channelBuffer);
        result = unknownLinkSubType.getLinkSubTypeTlvBodyAsByteArray();
        assertThat(result, is(notNullValue()));
    }

    /**
     * Tests to string method.
     */
    @Test
    public void testToString() throws Exception {
        assertThat(unknownLinkSubType.toString(), is(notNullValue()));
    }
}