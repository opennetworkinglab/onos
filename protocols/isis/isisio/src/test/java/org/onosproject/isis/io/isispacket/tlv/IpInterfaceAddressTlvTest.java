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
import org.onlab.packet.Ip4Address;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Unit test class for IP InterfaceAddressTlv.
 */
public class IpInterfaceAddressTlvTest {
    private final Ip4Address ip4Address = Ip4Address.valueOf("10.10.10.10");
    private final byte[] tlv = {0, 0, 0, 0};
    private TlvHeader tlvHeader;
    private IpInterfaceAddressTlv ipInterfaceAddressTlv;
    private List<Ip4Address> resultList;
    private byte[] result;
    private ChannelBuffer channelBuffer;

    @Before
    public void setUp() throws Exception {
        tlvHeader = new TlvHeader();
        ipInterfaceAddressTlv = new IpInterfaceAddressTlv(tlvHeader);
        channelBuffer = EasyMock.createMock(ChannelBuffer.class);
    }

    @After
    public void tearDown() throws Exception {
        tlvHeader = null;
        ipInterfaceAddressTlv = null;
    }

    /**
     * Tests addInterfaceAddress() method.
     */
    @Test
    public void testAddInterfaceAddres() throws Exception {
        ipInterfaceAddressTlv.addInterfaceAddres(ip4Address);
        resultList = ipInterfaceAddressTlv.interfaceAddress();
        assertThat(resultList.size(), is(1));

    }

    /**
     * Tests interfaceAddress() getter method.
     */
    @Test
    public void testInterfaceAddress() throws Exception {
        ipInterfaceAddressTlv.addInterfaceAddres(ip4Address);
        resultList = ipInterfaceAddressTlv.interfaceAddress();
        assertThat(resultList.size(), is(1));
    }

    /**
     * Tests readFrom() method.
     */
    @Test
    public void testReadFrom() throws Exception {
        channelBuffer = ChannelBuffers.copiedBuffer(tlv);
        ipInterfaceAddressTlv.readFrom(channelBuffer);
        assertThat(ipInterfaceAddressTlv.interfaceAddress().size(), is(1));
    }

    /**
     * Tests asBytes() method.
     */
    @Test
    public void testAsBytes() throws Exception {
        channelBuffer = ChannelBuffers.copiedBuffer(tlv);
        ipInterfaceAddressTlv.readFrom(channelBuffer);
        result = ipInterfaceAddressTlv.asBytes();
        assertThat(result, is(notNullValue()));
    }

    /**
     * Tests toString() method.
     */
    @Test
    public void testToString() throws Exception {
        assertThat(ipInterfaceAddressTlv.toString(), is(notNullValue()));
    }
}