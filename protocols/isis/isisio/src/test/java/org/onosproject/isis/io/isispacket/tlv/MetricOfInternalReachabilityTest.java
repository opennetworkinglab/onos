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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Unit test class for MetricOfInternalReachability.
 */
public class MetricOfInternalReachabilityTest {

    private final Ip4Address ip4Address = Ip4Address.valueOf("10.10.10.10");
    private final byte[] internalReachability = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    private MetricOfInternalReachability reachability;
    private Ip4Address result;
    private boolean result1;
    private byte result2;
    private ChannelBuffer channelBuffer;
    private byte[] result3;

    @Before
    public void setUp() throws Exception {
        reachability = new MetricOfInternalReachability();
        channelBuffer = EasyMock.createMock(ChannelBuffer.class);
    }

    @After
    public void tearDown() throws Exception {
        reachability = null;
        result = null;
        result1 = false;
    }

    /**
     * Tests getIpAddress() getter method.
     */
    @Test
    public void testGetIpAddress() throws Exception {
        reachability.setIpAddress(ip4Address);
        result = reachability.getIpAddress();
        assertThat(result, is(ip4Address));
    }

    /**
     * Tests setIpAddress() setter method.
     */
    @Test
    public void testSetIpAddress() throws Exception {
        reachability.setIpAddress(ip4Address);
        result = reachability.getIpAddress();
        assertThat(result, is(ip4Address));
    }

    /**
     * Tests getSubnetAddres() getter method.
     */
    @Test
    public void testGetSubnetAddres() throws Exception {
        reachability.setSubnetAddres(ip4Address);
        result = reachability.getSubnetAddres();
        assertThat(result, is(ip4Address));
    }

    /**
     * Tests setSubnetAddres() setter method.
     */
    @Test
    public void testSetSubnetAddres() throws Exception {
        reachability.setSubnetAddres(ip4Address);
        result = reachability.getSubnetAddres();
        assertThat(result, is(ip4Address));
    }

    /**
     * Tests isErrorIsInternal() getter method.
     */
    @Test
    public void testIsErrorIsInternal() throws Exception {
        reachability.setErrorIsInternal(true);
        result1 = reachability.isErrorIsInternal();
        assertThat(result1, is(true));
    }

    /**
     * Tests isErrorIsInternal() setter method.
     */
    @Test
    public void testSetErrorIsInternal() throws Exception {
        reachability.setErrorIsInternal(true);
        result1 = reachability.isErrorIsInternal();
        assertThat(result1, is(true));
    }

    /**
     * Tests isExpenseIsInternal() getter method.
     */
    @Test
    public void testIsExpenseIsInternal() throws Exception {
        reachability.setExpenseIsInternal(true);
        result1 = reachability.isExpenseIsInternal();
        assertThat(result1, is(true));
    }

    /**
     * Tests isExpenseIsInternal() setter method.
     */
    @Test
    public void testSetExpenseIsInternal() throws Exception {
        reachability.setExpenseIsInternal(true);
        result1 = reachability.isExpenseIsInternal();
        assertThat(result1, is(true));
    }

    /**
     * Tests isDelayIsInternal() getter method.
     */
    @Test
    public void testIsDelayIsInternal() throws Exception {
        reachability.setDelayIsInternal(true);
        result1 = reachability.isDelayIsInternal();
        assertThat(result1, is(true));
    }

    /**
     * Tests isDelayIsInternal() setter method.
     */
    @Test
    public void testSetDelayIsInternal() throws Exception {
        reachability.setDelayIsInternal(true);
        result1 = reachability.isDelayIsInternal();
        assertThat(result1, is(true));
    }

    /**
     * Tests isDefaultDistributionDown() getter method.
     */
    @Test
    public void testIsDefaultDistributionDown() throws Exception {
        reachability.setDefaultDistributionDown(true);
        result1 = reachability.isDefaultDistributionDown();
        assertThat(result1, is(true));
    }

    /**
     * Tests isDefaultDistributionDown() setter method.
     */
    @Test
    public void testSetDefaultDistributionDown() throws Exception {
        reachability.setDefaultDistributionDown(true);
        result1 = reachability.isDefaultDistributionDown();
        assertThat(result1, is(true));
    }

    /**
     * Tests isDefaultIsInternal() getter method.
     */
    @Test
    public void testIsDefaultIsInternal() throws Exception {
        reachability.setDefaultIsInternal(true);
        result1 = reachability.isDefaultIsInternal();
        assertThat(result1, is(true));
    }

    /**
     * Tests isDefaultIsInternal() setter method.
     */
    @Test
    public void testSetDefaultIsInternal() throws Exception {
        reachability.setDefaultIsInternal(true);
        result1 = reachability.isDefaultIsInternal();
        assertThat(result1, is(true));
    }

    /**
     * Tests isErrorMetricSupported() getter method.
     */
    @Test
    public void testIsErrorMetricSupported() throws Exception {
        reachability.setErrorMetricSupported(true);
        result1 = reachability.isErrorMetricSupported();
        assertThat(result1, is(true));
    }

    /**
     * Tests isErrorMetricSupported() setter method.
     */
    @Test
    public void testSetErrorMetricSupported() throws Exception {
        reachability.setErrorMetricSupported(true);
        result1 = reachability.isErrorMetricSupported();
        assertThat(result1, is(true));
    }

    /**
     * Tests isExpenseMetricSupported() setter method.
     */
    @Test
    public void testIsExpenseMetricSupported() throws Exception {
        reachability.setExpenseMetricSupported(true);
        result1 = reachability.isExpenseMetricSupported();
        assertThat(result1, is(true));
    }

    /**
     * Tests isExpenseMetricSupported() setter method.
     */
    @Test
    public void testSetExpenseMetricSupported() throws Exception {
        reachability.setExpenseMetricSupported(true);
        result1 = reachability.isExpenseMetricSupported();
        assertThat(result1, is(true));
    }

    /**
     * Tests isDelayMetricSupported() getter method.
     */
    @Test
    public void testIsDelayMetricSupported() throws Exception {
        reachability.setDelayMetricSupported(true);
        result1 = reachability.isDelayMetricSupported();
        assertThat(result1, is(true));
    }

    /**
     * Tests isDelayMetricSupported() setter method.
     */
    @Test
    public void testSetDelayMetricSupported() throws Exception {
        reachability.setDelayMetricSupported(true);
        result1 = reachability.isDelayMetricSupported();
        assertThat(result1, is(true));
    }

    /**
     * Tests errorMetric() getter method.
     */
    @Test
    public void testErrorMetric() throws Exception {
        reachability.setErrorMetric((byte) 0);
        result2 = reachability.errorMetric();
        assertThat(result2, is((byte) 0));
    }

    /**
     * Tests errorMetric() setter method.
     */
    @Test
    public void testSetErrorMetric() throws Exception {
        reachability.setErrorMetric((byte) 0);
        result2 = reachability.errorMetric();
        assertThat(result2, is((byte) 0));
    }

    /**
     * Tests delayMetric() getter method.
     */
    @Test
    public void testDelayMetric() throws Exception {
        reachability.setDelayMetric((byte) 0);
        result2 = reachability.delayMetric();
        assertThat(result2, is((byte) 0));
    }

    /**
     * Tests delayMetric() setter method.
     */
    @Test
    public void testSetDelayMetric() throws Exception {
        reachability.setDelayMetric((byte) 0);
        result2 = reachability.delayMetric();
        assertThat(result2, is((byte) 0));
    }

    /**
     * Tests defaultMetric() getter method.
     */
    @Test
    public void testDefaultMetric() throws Exception {
        reachability.setDefaultMetric((byte) 0);
        result2 = reachability.defaultMetric();
        assertThat(result2, is((byte) 0));
    }

    /**
     * Tests defaultMetric() setter method.
     */
    @Test
    public void testSetDefaultMetric() throws Exception {
        reachability.setDefaultMetric((byte) 0);
        result2 = reachability.defaultMetric();
        assertThat(result2, is((byte) 0));
    }

    /**
     * Tests readFrom() method.
     */
    @Test
    public void testReadFrom() throws Exception {
        channelBuffer = ChannelBuffers.copiedBuffer(internalReachability);
        reachability.readFrom(channelBuffer);
        assertThat(reachability, is(notNullValue()));
    }

    /**
     * Tests asBytes() method.
     */
    @Test
    public void testAsBytes() throws Exception {
        channelBuffer = ChannelBuffers.copiedBuffer(internalReachability);
        reachability.readFrom(channelBuffer);
        result3 = reachability.asBytes();
        assertThat(result3, is(notNullValue()));
    }

    /**
     * Tests toString() method.
     */
    @Test
    public void testToString() throws Exception {
        assertThat(reachability.toString(), is(notNullValue()));
    }
}