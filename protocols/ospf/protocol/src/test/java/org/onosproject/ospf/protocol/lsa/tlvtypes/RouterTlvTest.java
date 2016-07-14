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
package org.onosproject.ospf.protocol.lsa.tlvtypes;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.Ip4Address;
import org.onosproject.ospf.protocol.lsa.TlvHeader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Unit test class for RouterTlv.
 */
public class RouterTlvTest {

    private final byte[] packet = {1, 1, 1, 1};
    private final byte[] packet1 = {1, 1, 1};
    private RouterTlv rtlv;
    private TlvHeader header;
    private ChannelBuffer channelBuffer;
    private byte[] result;

    @Before
    public void setUp() throws Exception {
        rtlv = new RouterTlv(new TlvHeader());
    }

    @After
    public void tearDown() throws Exception {
        rtlv = null;
        header = null;
        channelBuffer = null;
        result = null;
    }

    /**
     * Tests routerAddress() getter method.
     */
    @Test
    public void testGetRouterAddress() throws Exception {
        rtlv.setRouterAddress(Ip4Address.valueOf("1.1.1.1"));
        assertThat(rtlv.routerAddress(), is(Ip4Address.valueOf("1.1.1.1")));
    }

    /**
     * Tests routerAddress() setter method.
     */
    @Test
    public void testSetRouterAddress() throws Exception {
        rtlv.setRouterAddress(Ip4Address.valueOf("1.1.1.1"));
        assertThat(rtlv.routerAddress(), is(Ip4Address.valueOf("1.1.1.1")));
    }

    /**
     * Tests readFrom() method.
     */
    @Test
    public void testReadFrom() throws Exception {
        header = new TlvHeader();
        header.setTlvType(1);
        header.setTlvLength(4);
        rtlv = new RouterTlv(header);
        channelBuffer = ChannelBuffers.copiedBuffer(packet);
        rtlv.readFrom(channelBuffer);
        assertThat(rtlv, is(notNullValue()));
        assertThat(rtlv, instanceOf(RouterTlv.class));
    }

    /**
     * Tests readFrom() method.
     */
    @Test(expected = Exception.class)
    public void testReadFrom1() throws Exception {
        header = new TlvHeader();
        header.setTlvType(1);
        header.setTlvLength(4);
        rtlv = new RouterTlv(header);
        channelBuffer = ChannelBuffers.copiedBuffer(packet1);
        rtlv.readFrom(channelBuffer);
        assertThat(rtlv, is(notNullValue()));
        assertThat(rtlv, instanceOf(RouterTlv.class));
    }

    /**
     * Tests asBytes() method.
     */
    @Test(expected = Exception.class)
    public void testAsBytes() throws Exception {
        result = rtlv.asBytes();
        assertThat(result, is(notNullValue()));
    }

    /**
     * Tests getTlvBodyAsByteArray() method.
     */
    @Test(expected = Exception.class)
    public void testGetTlvBodyAsByteArray() throws Exception {
        result = rtlv.getTlvBodyAsByteArray();
        assertThat(result, is(notNullValue()));
    }

    /**
     * Tests to string method.
     */
    @Test
    public void testToString() throws Exception {
        assertThat(rtlv.toString(), is(notNullValue()));
    }
}