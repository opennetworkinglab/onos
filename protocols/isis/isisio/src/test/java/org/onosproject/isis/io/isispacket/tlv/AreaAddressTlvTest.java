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

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit test class for AreaAddressTlv.
 */
public class AreaAddressTlvTest {

    private final String areaAddres = "490001";
    private final byte[] tlv = {1, 49};
    private AreaAddressTlv areaAddressTlv;
    private TlvHeader tlvHeader;
    private List<String> result;
    private ChannelBuffer channelBuffer;
    private byte[] result1;

    @Before
    public void setUp() throws Exception {
        tlvHeader = new TlvHeader();
        areaAddressTlv = new AreaAddressTlv(tlvHeader);
        channelBuffer = EasyMock.createMock(ChannelBuffer.class);
    }

    @After
    public void tearDown() throws Exception {
        tlvHeader = null;
        areaAddressTlv = null;
    }

    /**
     * Tests addAddress() method.
     */
    @Test
    public void testAddAddress() throws Exception {
        areaAddressTlv.addAddress(areaAddres);
        result = areaAddressTlv.areaAddress();
        assertThat(result.size(), is(1));
    }

    /**
     * Tests areaAddress() setter method.
     */
    @Test
    public void testSetAreaAddress() throws Exception {
        areaAddressTlv.addAddress(areaAddres);
        result = areaAddressTlv.areaAddress();
        assertThat(result.size(), is(1));
    }

    /**
     * Tests readFrom() method.
     */
    @Test
    public void testReadFrom() throws Exception {
        channelBuffer = ChannelBuffers.copiedBuffer(tlv);
        areaAddressTlv.readFrom(channelBuffer);
        assertThat(areaAddressTlv.areaAddress().size(), is(1));
    }

    /**
     * Tests asBytes() method.
     */
    @Test
    public void testAsBytes() throws Exception {
        channelBuffer = ChannelBuffers.copiedBuffer(tlv);
        areaAddressTlv.readFrom(channelBuffer);
        result1 = areaAddressTlv.asBytes();
        assertThat(result1, is(notNullValue()));
    }

    /**
     * Tests toString() method.
     */
    @Test
    public void testToString() throws Exception {
        assertThat(areaAddressTlv.toString(), is(notNullValue()));
    }
}