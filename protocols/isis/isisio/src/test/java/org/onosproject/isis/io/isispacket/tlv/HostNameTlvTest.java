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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit test class for HostNameTlv.
 */
public class HostNameTlvTest {
    private final String hostName = "TEST";
    private final byte[] tlv = hostName.getBytes();
    private HostNameTlv hostNameTlv;
    private TlvHeader tlvHeader;
    private String result;
    private ChannelBuffer channelBuffer;
    private byte[] result1;

    @Before
    public void setUp() throws Exception {
        tlvHeader = new TlvHeader();
        tlvHeader.setTlvLength(tlv.length);
        hostNameTlv = new HostNameTlv(tlvHeader);
        channelBuffer = EasyMock.createMock(ChannelBuffer.class);
    }

    @After
    public void tearDown() throws Exception {
        tlvHeader = null;
        hostNameTlv = null;
    }

    /**
     * Tests hostName() getter method.
     */
    @Test
    public void testHostName() throws Exception {
        hostNameTlv.setHostName(hostName);
        result = hostNameTlv.hostName();
        assertThat(result, is(hostName));
    }

    /**
     * Tests hostName() setter method.
     */
    @Test
    public void testSetHostName() throws Exception {
        hostNameTlv.setHostName(hostName);
        result = hostNameTlv.hostName();
        assertThat(result, is(hostName));
    }

    /**
     * Tests readFrom() method.
     */
    @Test
    public void testReadFrom() throws Exception {
        channelBuffer = ChannelBuffers.copiedBuffer(tlv);
        hostNameTlv.readFrom(channelBuffer);
        assertThat(hostNameTlv.hostName(), is(hostName));
    }

    /**
     * Tests asBytes() method.
     */
    @Test
    public void testAsBytes() throws Exception {
        channelBuffer = ChannelBuffers.copiedBuffer(tlv);
        hostNameTlv.readFrom(channelBuffer);
        result1 = hostNameTlv.asBytes();
        assertThat(result1, is(notNullValue()));
    }

    /**
     * Tests toString() method.
     */
    @Test
    public void testToString() throws Exception {
        assertThat(hostNameTlv.toString(), is(notNullValue()));
    }
}