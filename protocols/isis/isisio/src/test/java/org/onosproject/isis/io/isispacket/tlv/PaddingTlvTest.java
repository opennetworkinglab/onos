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

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit test class for PaddingTlv.
 */
public class PaddingTlvTest {

    private final byte[] tlv = {0, 0, 0, 0, 0, 0, 0};
    private PaddingTlv paddingTlv;
    private TlvHeader tlvHeader;
    private ChannelBuffer channelBuffer;
    private byte[] result;

    @Before
    public void setUp() throws Exception {
        tlvHeader = new TlvHeader();
        paddingTlv = new PaddingTlv(tlvHeader);
    }

    @After
    public void tearDown() throws Exception {
        paddingTlv = null;
    }

    /**
     * Tests readFrom() method.
     */
    @Test
    public void testReadFrom() throws Exception {
        channelBuffer = ChannelBuffers.copiedBuffer(tlv);
        paddingTlv.readFrom(channelBuffer);
        assertThat(paddingTlv, is(notNullValue()));
    }

    /**
     * Tests asBytes() method.
     */
    @Test
    public void testAsBytes() throws Exception {
        channelBuffer = ChannelBuffers.copiedBuffer(tlv);
        paddingTlv.readFrom(channelBuffer);
        result = paddingTlv.asBytes();
        assertThat(result, is(notNullValue()));
    }

    /**
     * Tests toString() getter method.
     */
    @Test
    public void testToString() throws Exception {
        assertThat(paddingTlv.toString(), is(notNullValue()));
    }
}