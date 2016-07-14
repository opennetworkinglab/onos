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
 * Unit test class for AsbrSummaryLsa.
 */
public class AsbrSummaryLsaTest {

    private final Ip4Address ipAddress = Ip4Address.valueOf("10.226.165.164");
    private AsbrSummaryLsa asbrSummaryLsa;
    private Ip4Address result;
    private int result1;
    private int num;
    private byte[] inputByteArray;
    private byte[] result2;
    private ChannelBuffer channelBuffer;
    private LsaHeader lsaHeader;
    private OspfLsaType ospflsaType;
    private String result3;
    private boolean result4;

    @Before
    public void setUp() throws Exception {
        asbrSummaryLsa = new AsbrSummaryLsa(new LsaHeader());
    }

    @After
    public void tearDown() throws Exception {
        asbrSummaryLsa = null;
    }

    /**
     * Tests networkMask() getter method.
     */
    @Test
    public void testGetNetworkMask() throws Exception {
        asbrSummaryLsa.setNetworkMask(ipAddress);
        result = asbrSummaryLsa.networkMask();
        assertThat(result, is(notNullValue()));
        assertThat(result, is(ipAddress));
    }

    /**
     * Tests networkMask() setter method.
     */
    @Test
    public void testSetNetworkMask() throws Exception {
        asbrSummaryLsa.setNetworkMask(ipAddress);
        result = asbrSummaryLsa.networkMask();
        assertThat(result, is(notNullValue()));
        assertThat(result, is(ipAddress));
    }

    /**
     * Tests metric() getter method.
     */
    @Test
    public void testGetMetric() throws Exception {
        num = 10;
        asbrSummaryLsa.setMetric(num);
        result1 = asbrSummaryLsa.metric();
        assertThat(result1, is(notNullValue()));
        assertThat(result1, is(num));
    }

    /**
     * Tests metric() setter method.
     */
    @Test
    public void testSetMetric() throws Exception {
        num = 20;
        asbrSummaryLsa.setMetric(num);
        result1 = asbrSummaryLsa.metric();
        assertThat(result1, is(notNullValue()));
        assertThat(result1, is(num));
    }

    /**
     * Tests readFrom() method.
     */
    @Test
    public void testReadFrom() throws Exception {
        inputByteArray = createByteForNetworkLsa();
        lsaHeader = createLsaHeader();
        asbrSummaryLsa = new AsbrSummaryLsa(lsaHeader);
        channelBuffer = ChannelBuffers.copiedBuffer(inputByteArray);
        asbrSummaryLsa.readFrom(channelBuffer);
        assertThat(asbrSummaryLsa, is(notNullValue()));
    }

    /**
     * Tests readFrom() method.
     */
    @Test(expected = Exception.class)
    public void testReadFrom1() throws Exception {
        byte[] temp = {0, 1, 2, 3};
        inputByteArray = temp;
        lsaHeader = createLsaHeader();
        asbrSummaryLsa = new AsbrSummaryLsa(lsaHeader);
        channelBuffer = ChannelBuffers.copiedBuffer(inputByteArray);
        asbrSummaryLsa.readFrom(channelBuffer);
        assertThat(asbrSummaryLsa, is(notNullValue()));
    }

    /**
     * Tests asBytes() method.
     */
    @Test(expected = Exception.class)
    public void testAsBytes() throws Exception {
        result2 = asbrSummaryLsa.asBytes();
        assertThat(result2, is(notNullValue()));
    }

    /**
     * Tests getLsaBodyAsByteArray() method.
     */
    @Test(expected = Exception.class)
    public void testGetLsaBodyAsByteArray() throws Exception {
        result2 = asbrSummaryLsa.getLsaBodyAsByteArray();
        assertThat(result2, is(notNullValue()));
    }

    /**
     * Tests ospfLsaType() getter method.
     */
    @Test
    public void testGetOspfLsaType() throws Exception {

        ospflsaType = asbrSummaryLsa.getOspfLsaType();
        assertThat(ospflsaType, is(notNullValue()));
        assertThat(ospflsaType, is(OspfLsaType.ASBR_SUMMARY));
    }

    /**
     * Tests to string method.
     */
    @Test
    public void testToString() throws Exception {

        result3 = asbrSummaryLsa.toString();
        assertThat(result3, is(notNullValue()));

    }

    /**
     * Tests hashcode() method.
     */
    @Test
    public void testHashcode() throws Exception {

        result1 = asbrSummaryLsa.hashCode();
        assertThat(result1, is(notNullValue()));

    }

    /**
     * Tests equals() method.
     */
    @Test
    public void testEqual() throws Exception {

        result4 = asbrSummaryLsa.equals(new AsbrSummaryLsa(new LsaHeader()));
        assertThat(result4, is(true));

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
        lsaHeader.setLsType(1);
        lsaHeader.setLsPacketLen(48);
        lsaHeader.setLsCheckSum(10);
        lsaHeader.setAge(4);
        lsaHeader.setLinkStateId("10.226.165.164");
        lsaHeader.setLsSequenceNo(250);
        lsaHeader.setAdvertisingRouter(Ip4Address.valueOf("100.226.165.165"));
        lsaHeader.setOptions(2);
        return lsaHeader;
    }
}
