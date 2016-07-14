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
 * Unit test class for LocalInterfaceIpAddress.
 */
public class LocalInterfaceIpAddressTest {

    private final byte[] packet = {1, 1, 1, 1};
    private final byte[] packet1 = {};
    private LocalInterfaceIpAddress localInterfaceIpAddress;
    private TlvHeader tlvHeader;
    private byte[] result;
    private ChannelBuffer channelBuffer;

    @Before
    public void setUp() throws Exception {
        localInterfaceIpAddress = new LocalInterfaceIpAddress(new TlvHeader());
    }

    @After
    public void tearDown() throws Exception {
        localInterfaceIpAddress = null;
        tlvHeader = null;
        result = null;
        channelBuffer = null;
    }

    /**
     * Tests to string method.
     */
    @Test
    public void testToString() throws Exception {
        assertThat(localInterfaceIpAddress.toString(), is(notNullValue()));
    }

    /**
     * Tests addLocalInterfaceIPAddress() method.
     */
    @Test
    public void testAddLocalInterfaceIPAddress() throws Exception {
        localInterfaceIpAddress.addLocalInterfaceIPAddress("1.1.1.1");
        assertThat(localInterfaceIpAddress, is(notNullValue()));
    }

    /**
     * Tests readFrom() method.
     */
    @Test
    public void testReadFrom() throws Exception {
        tlvHeader = new TlvHeader();
        tlvHeader.setTlvType(3);
        tlvHeader.setTlvLength(4);
        localInterfaceIpAddress = new LocalInterfaceIpAddress(tlvHeader);
        channelBuffer = ChannelBuffers.copiedBuffer(packet);
        localInterfaceIpAddress.readFrom(channelBuffer);
        assertThat(localInterfaceIpAddress, is(notNullValue()));
    }

    /**
     * Tests readFrom() method.
     */
    @Test
    public void testReadFrom1() throws Exception {
        tlvHeader = new TlvHeader();
        tlvHeader.setTlvType(3);
        tlvHeader.setTlvLength(4);
        localInterfaceIpAddress = new LocalInterfaceIpAddress(tlvHeader);
        channelBuffer = ChannelBuffers.copiedBuffer(packet1);
        localInterfaceIpAddress.readFrom(channelBuffer);
        assertThat(localInterfaceIpAddress, is(notNullValue()));
    }

    /**
     * Tests asBytes() method.
     */
    @Test
    public void testAsBytes() throws Exception {
        result = localInterfaceIpAddress.asBytes();
        assertThat(result, is(notNullValue()));
    }


    /**
     * Tests getLinkSubTypeTlvBodyAsByteArray() method.
     */
    @Test
    public void testGetLinkSubTypeTlvBodyAsByteArray() throws Exception {
        result = localInterfaceIpAddress.getLinkSubTypeTlvBodyAsByteArray();
        assertThat(result, is(notNullValue()));
    }
}
