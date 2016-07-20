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
import org.onlab.packet.Ip4Address;
import org.onosproject.isis.io.isispacket.tlv.TlvHeader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit test class for InterfaceIpAddress.
 */
public class InterfaceIpAddressTest {
    private final byte[] packet = {1, 1, 1, 1};
    private final byte[] packet1 = {};
    private NeighborIpAddress interfaceIpAddress;
    private TlvHeader tlvHeader;
    private Ip4Address ip4Address = Ip4Address.valueOf("1.1.1.1");
    private byte[] result;
    private ChannelBuffer channelBuffer;

    @Before
    public void setUp() throws Exception {
        interfaceIpAddress = new NeighborIpAddress(new TlvHeader());
    }

    @After
    public void tearDown() throws Exception {
        interfaceIpAddress = null;
        tlvHeader = null;
        result = null;
        channelBuffer = null;
    }

    /**
     * Tests to string method.
     */
    @Test
    public void testToString() throws Exception {
        assertThat(interfaceIpAddress.toString(), is(notNullValue()));
    }

    /**
     * Tests readFrom() method.
     */
    @Test
    public void testReadFrom() {
        tlvHeader = new TlvHeader();
        tlvHeader.setTlvType(3);
        tlvHeader.setTlvLength(4);
        interfaceIpAddress = new NeighborIpAddress(tlvHeader);
        channelBuffer = ChannelBuffers.copiedBuffer(packet);
        interfaceIpAddress.readFrom(channelBuffer);
        assertThat(interfaceIpAddress, is(notNullValue()));
    }

    /**
     * Tests readFrom() method.
     */
    @Test
    public void testReadFrom1() throws Exception {
        tlvHeader = new TlvHeader();
        tlvHeader.setTlvType(3);
        tlvHeader.setTlvLength(4);
        interfaceIpAddress = new NeighborIpAddress(tlvHeader);
        channelBuffer = ChannelBuffers.copiedBuffer(packet1);
        interfaceIpAddress.readFrom(channelBuffer);
        assertThat(interfaceIpAddress, is(notNullValue()));
    }


}