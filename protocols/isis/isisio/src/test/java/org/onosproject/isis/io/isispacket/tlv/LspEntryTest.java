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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Unit test class for LspEntry.
 */
public class LspEntryTest {

    private final String lspId = "10.10.10.10";
    private final byte[] entry = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    private LspEntry lspEntry;
    private int result;
    private String result2;
    private ChannelBuffer channelBuffer;
    private byte[] result3;

    @Before
    public void setUp() throws Exception {
        lspEntry = new LspEntry();
        channelBuffer = EasyMock.createMock(ChannelBuffer.class);
    }

    @After
    public void tearDown() throws Exception {
        lspEntry = null;
        channelBuffer = null;
    }

    /**
     * Tests lspSequenceNumber() getter method.
     */
    @Test
    public void testLspSequenceNumber() throws Exception {
        lspEntry.setLspSequenceNumber(0);
        result = lspEntry.lspSequenceNumber();
        assertThat(result, is(0));
    }

    /**
     * Tests lspSequenceNumber() setter method.
     */
    @Test
    public void testSetLspSequenceNumber() throws Exception {
        lspEntry.setLspSequenceNumber(0);
        result = lspEntry.lspSequenceNumber();
        assertThat(result, is(0));
    }

    /**
     * Tests lspChecksum() getter method.
     */
    @Test
    public void testLspChecksum() throws Exception {
        lspEntry.setLspChecksum(0);
        result = lspEntry.lspChecksum();
        assertThat(result, is(0));
    }

    /**
     * Tests lspChecksum() setter method.
     */
    @Test
    public void testSetLspChecksum() throws Exception {
        lspEntry.setLspChecksum(0);
        result = lspEntry.lspChecksum();
        assertThat(result, is(0));
    }

    /**
     * Tests remainingTime() getter method.
     */
    @Test
    public void testRemainingTime() throws Exception {
        lspEntry.setRemainingTime(0);
        result = lspEntry.remainingTime();
        assertThat(result, is(0));
    }

    /**
     * Tests remainingTime() setter method.
     */
    @Test
    public void testSetRemainingTime() throws Exception {
        lspEntry.setRemainingTime(0);
        result = lspEntry.remainingTime();
        assertThat(result, is(0));
    }

    /**
     * Tests lspId() getter method.
     */
    @Test
    public void testLspId() throws Exception {
        lspEntry.setLspId(lspId);
        result2 = lspEntry.lspId();
        assertThat(result2, is("10.10.10.10"));
    }

    /**
     * Tests lspId() getter method.
     */
    @Test
    public void testSetLspId() throws Exception {
        lspEntry.setLspId(lspId);
        result2 = lspEntry.lspId();
        assertThat(result2, is("10.10.10.10"));
    }

    /**
     * Tests readFrom() method.
     */
    @Test
    public void testReadFrom() throws Exception {
        channelBuffer = ChannelBuffers.copiedBuffer(entry);
        lspEntry.readFrom(channelBuffer);
        assertThat(lspEntry, is(notNullValue()));
    }

    /**
     * Tests lspEntryAsBytes() method.
     */
    @Test
    public void testLspEntryAsBytes() throws Exception {
        channelBuffer = ChannelBuffers.copiedBuffer(entry);
        lspEntry.readFrom(channelBuffer);
        result3 = lspEntry.lspEntryAsBytes();
        assertThat(lspEntry, is(notNullValue()));
    }

    /**
     * Tests toString() method.
     */
    @Test
    public void testToString() throws Exception {
        assertThat(lspEntry.toString(), is(notNullValue()));
    }
}