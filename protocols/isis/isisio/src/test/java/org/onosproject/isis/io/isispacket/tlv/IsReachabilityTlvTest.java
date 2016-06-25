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
 * Unit test class for IS ReachabilityTlv.
 */
public class IsReachabilityTlvTest {
    private final byte[] tlv = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    private TlvHeader tlvHeader;
    private IsReachabilityTlv isReachabilityTlv;
    private MetricsOfReachability metricsOfReachability;
    private int resultInt;
    private ChannelBuffer channelBuffer;
    private byte[] result;

    @Before
    public void setUp() throws Exception {
        tlvHeader = new TlvHeader();
        isReachabilityTlv = new IsReachabilityTlv(tlvHeader);
        metricsOfReachability = new MetricsOfReachability();
        channelBuffer = EasyMock.createMock(ChannelBuffer.class);
    }

    @After
    public void tearDown() throws Exception {
        tlvHeader = null;
        isReachabilityTlv = null;
    }

    /**
     * Tests reserved() getter method.
     */
    @Test
    public void testReserved() throws Exception {
        isReachabilityTlv.setReserved(10);
        resultInt = isReachabilityTlv.reserved();
        assertThat(resultInt, is(10));
    }

    /**
     * Tests reserved() setter method.
     */
    @Test
    public void testSetReserved() throws Exception {
        isReachabilityTlv.setReserved(10);
        resultInt = isReachabilityTlv.reserved();
        assertThat(resultInt, is(10));
    }

    /**
     * Tests addMeticsOfReachability() getter method.
     */
    @Test
    public void testAddMeticsOfReachability() throws Exception {
        isReachabilityTlv.addMeticsOfReachability(metricsOfReachability);
        assertThat(isReachabilityTlv, is(notNullValue()));
    }

    /**
     * Tests readFrom() method.
     */
    @Test
    public void testReadFrom() throws Exception {
        channelBuffer = ChannelBuffers.copiedBuffer(tlv);
        isReachabilityTlv.readFrom(channelBuffer);
        assertThat(isReachabilityTlv, is(notNullValue()));
    }

    /**
     * Tests asBytes() method.
     */
    @Test
    public void testAsBytes() throws Exception {
        channelBuffer = ChannelBuffers.copiedBuffer(tlv);
        isReachabilityTlv.readFrom(channelBuffer);
        result = isReachabilityTlv.asBytes();
        assertThat(result, is(notNullValue()));
    }

    /**
     * Tests toString() method.
     */
    @Test
    public void testToString() throws Exception {
        assertThat(isReachabilityTlv.toString(), is(notNullValue()));
    }
}