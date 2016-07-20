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
package org.onosproject.ospf.protocol.lsa.types;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.Ip4Address;
import org.onosproject.ospf.controller.OspfLsaType;
import org.onosproject.ospf.protocol.lsa.LsaHeader;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit test class for SummaryLsa.
 */
public class SummaryLsaTest {

    private SummaryLsa summaryLsa;
    private Ip4Address result;
    private int result1;
    private byte[] inputByteArray;
    private LsaHeader lsaHeader;
    private ChannelBuffer channelBuffer;
    private byte[] result2;
    private OspfLsaType ospflsaType;

    @Before
    public void setUp() throws Exception {
        summaryLsa = new SummaryLsa(new LsaHeader());
    }

    @After
    public void tearDown() throws Exception {
        summaryLsa = null;
        result = null;
        inputByteArray = null;
        lsaHeader = null;
        channelBuffer = null;
        result2 = null;
        ospflsaType = null;
    }

    /**
     * Tests networkMask() getter method.
     */
    @Test
    public void testGetNetworkMask() throws Exception {
        summaryLsa.setNetworkMask(Ip4Address.valueOf("10.226.165.164"));
        result = summaryLsa.networkMask();
        assertThat(result, is(Ip4Address.valueOf("10.226.165.164")));
    }

    /**
     * Tests networkMask() setter method.
     */
    @Test
    public void testSetNetworkMask() throws Exception {
        summaryLsa.setNetworkMask(Ip4Address.valueOf("10.226.165.164"));
        result = summaryLsa.networkMask();
        assertThat(result, is(Ip4Address.valueOf("10.226.165.164")));
    }

    /**
     * Tests metric() getter method.
     */
    @Test
    public void testGetMetric() throws Exception {
        summaryLsa.setMetric(10);
        result1 = summaryLsa.metric();
        assertThat(result1, is(10));
    }

    /**
     * Tests metric() setter method.
     */
    @Test
    public void testSetMetric() throws Exception {
        summaryLsa.setMetric(20);
        result1 = summaryLsa.metric();
        assertThat(result1, is(20));
    }

    /**
     * Tests readFrom() method.
     */
    @Test
    public void testReadFrom() throws Exception {
        inputByteArray = createByteForNetworkLsa();
        lsaHeader = createLsaHeader();
        summaryLsa = new SummaryLsa(lsaHeader);
        channelBuffer = ChannelBuffers.copiedBuffer(inputByteArray);
        summaryLsa.readFrom(channelBuffer);
        assertThat(summaryLsa, is(notNullValue()));
    }

    /**
     * Tests readFrom() method.
     */
    @Test(expected = Exception.class)
    public void testReadFrom1() throws Exception {
        byte[] temp = {0, 0, 0};
        inputByteArray = temp;
        lsaHeader = createLsaHeader();
        summaryLsa = new SummaryLsa(lsaHeader);
        channelBuffer = ChannelBuffers.copiedBuffer(inputByteArray);
        summaryLsa.readFrom(channelBuffer);
        assertThat(summaryLsa, is(notNullValue()));
    }

    /**
     * Tests asBytes() method.
     */
    @Test
    public void testAsBytes() throws Exception {
        result2 = summaryLsa.asBytes();
        assertThat(result2, is(notNullValue()));
    }

    /**
     * Tests getLsaBodyAsByteArray() method.
     */
    @Test
    public void testGetLsaBodyAsByteArray() throws Exception {
        result2 = summaryLsa.getLsaBodyAsByteArray();
        assertThat(result2, is(notNullValue()));
    }

    /**
     * Tests getOspfLsaType() getter method.
     */
    @Test
    public void testGetOspfLsaType() throws Exception {
        ospflsaType = summaryLsa.getOspfLsaType();
        assertThat(ospflsaType, is(notNullValue()));
        assertThat(ospflsaType, is(OspfLsaType.SUMMARY));
    }

    /**
     * Utility method used by junit methods.
     */
    private byte[] createByteForNetworkLsa() {
        byte[] packet = {2, 1, 1, 52, -64, -88, 56, 1, -64, -88, 56, 1, 0, 100, 0, 100, 0, 0, 0, 0, 0, 0, 0, 0, -64,
                -88, 56, 1, 0, 10, 1, 1, 0, 0, 0, 40, -64, -88, 56, 1, -64, -88, 56, 1, -64, -88, 56, 1, -64, -88, 56,
                1};
        return packet;
    }

    /**
     * Utility method used by junit methods.
     */
    private LsaHeader createLsaHeader() {
        lsaHeader = new LsaHeader();
        lsaHeader.setLsType(3);
        lsaHeader.setLsPacketLen(48);
        lsaHeader.setLsCheckSum(10);
        lsaHeader.setAge(4);
        lsaHeader.setLinkStateId("10.226.165.164");
        lsaHeader.setLsSequenceNo(250);
        lsaHeader.setAdvertisingRouter(Ip4Address.valueOf("100.226.165.165"));
        lsaHeader.setOptions(2);
        return lsaHeader;
    }

    /**
     * Tests hashcode() method.
     */
    @Test
    public void testHashcode() throws Exception {

        result1 = summaryLsa.hashCode();
        assertThat(result1, is(notNullValue()));

    }
}