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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import org.onosproject.isis.io.isispacket.tlv.TlvHeader;

/**
 * Unit test class for TrafficEngineeringMetric.
 */
public class TrafficEngineeringMetricTest {

    private final byte[] packet = {0, 0, 1, 1};
    private TrafficEngineeringMetric trafficEngineeringMetric;
    private TlvHeader header;
    private byte[] result;
    private ChannelBuffer channelBuffer;

    @Before
    public void setUp() throws Exception {
        trafficEngineeringMetric = new TrafficEngineeringMetric(new TlvHeader());
    }

    @After
    public void tearDown() throws Exception {
        trafficEngineeringMetric = null;
        header = null;
        result = null;
        channelBuffer = null;
    }

    /**
     * Tests trafficEngineeringMetric() setter method.
     */
    @Test
    public void testSetTrafficEngineeringMetric() throws Exception {
        trafficEngineeringMetric.setTrafficEngineeringMetric(123456789L);
        assertThat(trafficEngineeringMetric, is(notNullValue()));
    }

    /**
     * Tests readFrom() method.
     */
    @Test
    public void testReadFrom() throws Exception {
        header = new TlvHeader();
        header.setTlvLength(4);
        header.setTlvType(5);
        channelBuffer = ChannelBuffers.copiedBuffer(packet);
        trafficEngineeringMetric = new TrafficEngineeringMetric(header);
        trafficEngineeringMetric.readFrom(channelBuffer);
        assertThat(trafficEngineeringMetric, is(notNullValue()));
    }

    /**
     * Tests asBytes() method.
     */
    @Test
    public void testAsBytes() throws Exception {
        result = trafficEngineeringMetric.asBytes();
        assertThat(result, is(notNullValue()));
    }

    /**
     * Tests getLinkSubTypeTlvBodyAsByteArray() method.
     */
    @Test
    public void testGetLinkSubTypeTlvBodyAsByteArray() throws Exception {
        result = trafficEngineeringMetric.tlvBodyAsBytes();
        assertThat(result, is(notNullValue()));
    }

    /**
     * Tests to string method.
     */
    @Test
    public void testToString() throws Exception {
        assertThat(trafficEngineeringMetric.toString(), is(notNullValue()));
    }
}