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

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Unit test class for LspEntriesTlv.
 */
public class LspEntriesTlvTest {

    private final byte[] entry = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    private LspEntriesTlv lspEntriesTlv;
    private TlvHeader tlvHeader;
    private List<LspEntry> lspEntries = new ArrayList();
    private ChannelBuffer channelBuffer;

    @Before
    public void setUp() throws Exception {
        tlvHeader = new TlvHeader();
        lspEntriesTlv = new LspEntriesTlv(tlvHeader);
        channelBuffer = EasyMock.createMock(ChannelBuffer.class);
    }

    @After
    public void tearDown() throws Exception {
        tlvHeader = null;
        lspEntriesTlv = null;
        lspEntries.clear();
    }

    /**
     * Tests lspEntry() getter method.
     */
    @Test
    public void testLspEntry() throws Exception {
        lspEntriesTlv.addLspEntry(new LspEntry());
        assertThat(lspEntriesTlv.lspEntry().size(), is(1));
    }

    /**
     * Tests lspEntry() add method.
     */
    @Test
    public void testAddLspEntry() throws Exception {
        lspEntriesTlv.addLspEntry(new LspEntry());
        assertThat(lspEntriesTlv.lspEntry().size(), is(1));
    }

    /**
     * Tests readFrom() method.
     */
    @Test
    public void testReadFrom() throws Exception {
        channelBuffer = ChannelBuffers.copiedBuffer(entry);
        lspEntriesTlv.readFrom(channelBuffer);
        lspEntries = lspEntriesTlv.lspEntry();
        assertThat(lspEntriesTlv.lspEntry().size(), is(1));
    }

    /**
     * Tests asBytes()  method.
     */
    @Test
    public void testAsBytes() throws Exception {
        channelBuffer = ChannelBuffers.copiedBuffer(entry);
        lspEntriesTlv.readFrom(channelBuffer);
        lspEntriesTlv.asBytes();
    }

    /**
     * Tests toString()  method.
     */
    @Test
    public void testToString() throws Exception {
        assertThat(lspEntriesTlv.toString(), is(notNullValue()));
    }

}