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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit test class for TlvHeader.
 */
public class TlvHeaderTest {

    private TlvHeader tlvHeader;
    private int result;
    private ChannelBuffer channelBuffer;

    @Before
    public void setUp() throws Exception {
        tlvHeader = new TlvHeader();
        channelBuffer = EasyMock.createMock(ChannelBuffer.class);
    }

    @After
    public void tearDown() throws Exception {
        tlvHeader = null;
        channelBuffer = null;
    }

    /**
     * Tests tlvLength() getter method.
     */
    @Test
    public void testTlvLength() throws Exception {
        tlvHeader.setTlvLength(1);
        result = tlvHeader.tlvLength();
        assertThat(result, is(1));
    }

    /**
     * Tests tlvLength() setter method.
     */
    @Test
    public void testSetTlvLength() throws Exception {
        tlvHeader.setTlvLength(1);
        result = tlvHeader.tlvLength();
        assertThat(result, is(1));
    }

    /**
     * Tests tlvType() getter method.
     */
    @Test
    public void testTlvType() throws Exception {
        tlvHeader.setTlvType(1);
        result = tlvHeader.tlvType();
        assertThat(result, is(1));
    }

    /**
     * Tests tlvType() setter method.
     */
    @Test
    public void testSetTlvType() throws Exception {
        tlvHeader.setTlvType(1);
        result = tlvHeader.tlvType();
        assertThat(result, is(1));
    }

    /**
     * Tests readFrom() method.
     */
    @Test
    public void testReadFrom() throws Exception {
        tlvHeader.readFrom(channelBuffer);
        assertThat(tlvHeader, is(notNullValue()));
    }

    /**
     * Tests asBytes() getter method.
     */
    @Test
    public void testAsBytes() throws Exception {
        assertThat(tlvHeader.asBytes(), is(nullValue()));
    }

    /**
     * Tests tlvHeaderAsByteArray() method.
     */
    @Test
    public void testTlvHeaderAsByteArray() throws Exception {
        tlvHeader.setTlvLength(1);
        tlvHeader.setTlvType(1);
        assertThat(tlvHeader.tlvHeaderAsByteArray(), is(notNullValue()));
        assertThat(tlvHeader.tlvType(), is(1));
        assertThat(tlvHeader.tlvLength(), is(1));
    }

    /**
     * Tests toString() getter method.
     */
    @Test
    public void testToString() throws Exception {
        assertThat(tlvHeader.toString(), is(notNullValue()));
    }
}