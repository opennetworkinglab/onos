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
import org.onosproject.isis.io.isispacket.tlv.subtlv.AdministrativeGroup;
import org.onosproject.isis.io.isispacket.tlv.subtlv.TrafficEngineeringSubTlv;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Unit test class for IP ExtendedReachabilityTlv.
 */
public class IpExtendedReachabilityTlvTest {
    private final String prefix = "00";
    private final byte[] tlv = {0, 0, 0, 0, 8, 0, 0, 0, 0, 0, 0, 0, 0};
    private final byte[] tlv1 = {0, 0, 0, 0, 16, 0, 0, 0, 0, 0, 0, 0, 0};
    private final byte[] tlv2 = {0, 0, 0, 0, 24, 0, 0, 0, 0, 0, 0, 0, 0};
    private final byte[] tlv3 = {0, 0, 0, 0, 32, 0, 0, 0, 0, 0, 0, 0, 0};
    private TlvHeader tlvHeader;
    private IpExtendedReachabilityTlv ipExtendedReachabilityTlv;
    private String result;
    private boolean result1;
    private int result2;
    private TrafficEngineeringSubTlv trafficEngineeringSubTlv;
    private byte result4;
    private ChannelBuffer channelBuffer;
    private byte[] result3;

    @Before
    public void setUp() throws Exception {
        tlvHeader = new TlvHeader();
        ipExtendedReachabilityTlv = new IpExtendedReachabilityTlv(tlvHeader);
        trafficEngineeringSubTlv = new AdministrativeGroup(tlvHeader);
        channelBuffer = EasyMock.createMock(ChannelBuffer.class);
    }

    @After
    public void tearDown() throws Exception {
        tlvHeader = null;
        ipExtendedReachabilityTlv = null;

    }

    /**
     * Tests prefix() getter method.
     */
    @Test
    public void testPrefix() throws Exception {
        ipExtendedReachabilityTlv.setPrefix(prefix);
        result = ipExtendedReachabilityTlv.prefix();
        assertThat(result, is("00"));
    }

    /**
     * Tests prefix() setter method.
     */
    @Test
    public void testSetPrefix() throws Exception {
        ipExtendedReachabilityTlv.setPrefix(prefix);
        result = ipExtendedReachabilityTlv.prefix();
        assertThat(result, is("00"));
    }

    /**
     * Tests isDown() getter method.
     */

    @Test
    public void testIsDown() throws Exception {
        ipExtendedReachabilityTlv.setDown(true);
        result1 = ipExtendedReachabilityTlv.isDown();
        assertThat(result1, is(true));
    }

    /**
     * Tests isDown() setter method.
     */
    @Test
    public void testSetDown() throws Exception {
        ipExtendedReachabilityTlv.setDown(true);
        result1 = ipExtendedReachabilityTlv.isDown();
        assertThat(result1, is(true));
    }

    /**
     * Tests isSubTlvPresence() getter method.
     */
    @Test
    public void testIsSubTlvPresence() throws Exception {
        ipExtendedReachabilityTlv.setSubTlvPresence(true);
        result1 = ipExtendedReachabilityTlv.isSubTlvPresence();
        assertThat(result1, is(true));
    }

    /**
     * Tests isSubTlvPresence() setter method.
     */
    @Test
    public void testSetSubTlvPresence() throws Exception {
        ipExtendedReachabilityTlv.setSubTlvPresence(true);
        result1 = ipExtendedReachabilityTlv.isSubTlvPresence();
        assertThat(result1, is(true));
    }

    /**
     * Tests prefixLength() getter method.
     */
    @Test
    public void testPrefixLength() throws Exception {
        ipExtendedReachabilityTlv.setPrefixLength(10);
        result2 = ipExtendedReachabilityTlv.prefixLength();
        assertThat(result2, is(10));

    }

    /**
     * Tests prefixLength() setter method.
     */
    @Test
    public void testSetPrefixLength() throws Exception {
        ipExtendedReachabilityTlv.setPrefixLength(10);
        result2 = ipExtendedReachabilityTlv.prefixLength();
        assertThat(result2, is(10));
    }

    /**
     * Tests addNeighbor() method.
     */
    @Test
    public void testAddSubTlv() throws Exception {
        ipExtendedReachabilityTlv.addSubTlv(trafficEngineeringSubTlv);
        assertThat(ipExtendedReachabilityTlv, is(notNullValue()));

    }

    /**
     * Tests subTlvLength() getter method.
     */
    @Test
    public void testSubTlvLength() throws Exception {
        ipExtendedReachabilityTlv.setSubTlvLength((byte) 10);
        result4 = ipExtendedReachabilityTlv.subTlvLength();
        assertThat(result4, is((byte) 10));
    }

    /**
     * Tests subTlvLength() setter method.
     */
    @Test
    public void testSetSubTlvLength() throws Exception {
        ipExtendedReachabilityTlv.setSubTlvLength((byte) 10);
        result4 = ipExtendedReachabilityTlv.subTlvLength();
        assertThat(result4, is((byte) 10));
    }

    /**
     * Tests metric() getter method.
     */
    @Test
    public void testMetric() throws Exception {
        ipExtendedReachabilityTlv.setMetric(10);
        result2 = ipExtendedReachabilityTlv.metric();
        assertThat(result2, is(10));
    }

    /**
     * Tests metric() setter method.
     */
    @Test
    public void testSetMetric() throws Exception {
        ipExtendedReachabilityTlv.setMetric(10);
        result2 = ipExtendedReachabilityTlv.metric();
        assertThat(result2, is(10));
    }

    /**
     * Tests readFrom() method.
     */
    @Test
    public void testReadFrom() throws Exception {
        channelBuffer = ChannelBuffers.copiedBuffer(tlv);
        ipExtendedReachabilityTlv.readFrom(channelBuffer);
        assertThat(ipExtendedReachabilityTlv.metric(), is(0));
        channelBuffer = ChannelBuffers.copiedBuffer(tlv1);
        ipExtendedReachabilityTlv.readFrom(channelBuffer);
        assertThat(ipExtendedReachabilityTlv.metric(), is(0));
        channelBuffer = ChannelBuffers.copiedBuffer(tlv2);
        ipExtendedReachabilityTlv.readFrom(channelBuffer);
        assertThat(ipExtendedReachabilityTlv.metric(), is(0));
        channelBuffer = ChannelBuffers.copiedBuffer(tlv3);
        ipExtendedReachabilityTlv.readFrom(channelBuffer);
        assertThat(ipExtendedReachabilityTlv.metric(), is(0));
    }

    /**
     * Tests asBytes() method.
     */
    @Test
    public void testAsBytes() throws Exception {
        channelBuffer = ChannelBuffers.copiedBuffer(tlv);
        ipExtendedReachabilityTlv.readFrom(channelBuffer);
        ipExtendedReachabilityTlv.setPrefix(prefix);
        result3 = ipExtendedReachabilityTlv.asBytes();
        assertThat(result3, is(notNullValue()));
    }

    /**
     * Tests toString() method.
     */
    @Test
    public void testToString() throws Exception {
        assertThat(ipExtendedReachabilityTlv.toString(), is(notNullValue()));
    }

}