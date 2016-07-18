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
package org.onosproject.ospf.protocol.util;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.Ip4Address;
import org.onosproject.ospf.protocol.lsa.LsaHeader;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Unit test class for OspfUtil.
 */
public class OspfUtilTest {

    private final int ospfChecksumPos1 = 12;
    private final int ospfChecksumPos2 = 13;
    private final int lsaChecksumPos1 = 16;
    private final int lsaChecksumPos2 = 17;
    private final int ospfLengthPos1 = 2;
    private final int ospfLengthPos2 = 3;
    private final int lsaLengthPos1 = 18;
    private final int lsaLengthPos2 = 19;
    private final byte[] input = {0, 2};
    private final byte[] packet = {2, 1, 0, 52, -64, -88, 56, 1, -64, -88, 56, 1, 0, 100, 0, 100, 0, 0, 0, 0, 0, 0,
            0, 0, -64, -88, 56, 1, 0, 10, 1, 1, 0, 0, 0, 40, -64, -88, 56, 1, -64, -88, 56, 1, -64, -88, 56, 1, -64,
            -88, 56, 1};
    private final byte[] rLsa = {14, 16, 2, 1, -64, -88, -86, 2, -64, -88, -86, 2, -128, 0, 0, 1, 74, -114, 0, 48, 2,
            0, 0, 2, -64, -88, -86, 0, -1, -1, -1, 0, 3, 0, 0, 10, -64, -88, -86, 0, -1, -1, -1, 0, 3, 0, 0, 10};
    private final byte[] opaqueheader = {14, 16, 2, 1, -64, -88, -86, 2, -64, -88, -86, 2, -128, 0, 0, 1, 74, -114,
            0, 48};
    private int num;
    private int result;
    private int input2;
    private long input4;
    private ChannelBuffer channelBuffer;
    private byte[] result1;
    private LsaHeader lsaHeader;
    private boolean result2;
    private long result3;


    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {
        channelBuffer = null;
        result1 = null;
        lsaHeader = null;
    }


    /**
     * Tests byteToInteger() method.
     */
    @Test
    public void testByteToInteger() throws Exception {
        result = OspfUtil.byteToInteger(input);
        assertThat(result, is(notNullValue()));
        assertThat(result, is(2));

    }

    /**
     * Tests byteToLong() method.
     */
    @Test
    public void testByteToLong() throws Exception {
        result3 = OspfUtil.byteToLong(input);
        assertThat(result3, is(notNullValue()));
        assertThat(result3, is(2L));
    }

    /**
     * Tests byteToInteger() method.
     */
    @Test
    public void testByteToLong1() throws Exception {
        result3 = OspfUtil.byteToLong(input);
        assertThat(result3, is(notNullValue()));
        assertThat(result3, is(2L));
    }

    /**
     * Tests byteToInteger() method.
     */
    @Test
    public void testByteToInteger1() throws Exception {
        result = OspfUtil.byteToInteger(input);
        assertThat(result, is(notNullValue()));
        assertThat(result, is(2));
    }

    /**
     * Tests to createRandomNumber() method.
     */
    @Test
    public void testCreateRandomNumber() throws Exception {
        num = OspfUtil.createRandomNumber();
        assertThat(num, is(notNullValue()));
    }

    /**
     * Tests readLsaHeader() method.
     */
    @Test
    public void testReadLsaHeader2() throws Exception {
        channelBuffer = ChannelBuffers.copiedBuffer(packet);
        lsaHeader = OspfUtil.readLsaHeader(channelBuffer);
        assertThat(lsaHeader, is(notNullValue()));
    }

    /**
     * Tests to readLsaHeader method.
     */
    @Test
    public void testReadLsaHeader1() throws Exception {
        channelBuffer = ChannelBuffers.copiedBuffer(opaqueheader);
        lsaHeader = OspfUtil.readLsaHeader(channelBuffer);
        assertThat(lsaHeader, is(notNullValue()));
    }

    /**
     * Tests convertToTwoBytes() method.
     */
    @Test
    public void testConvertToTwoBytes() throws Exception {
        input2 = 4;
        result1 = OspfUtil.convertToTwoBytes(input2);
        assertThat(result1.length, is(2));
        input2 = 1000;
        result1 = OspfUtil.convertToTwoBytes(input2);
        assertThat(result1.length, is(2));
    }

    /**
     * Tests convertToThreeBytes() method.
     */
    @Test
    public void testConvertToThreeBytes() throws Exception {
        input2 = 1000000;
        result1 = OspfUtil.convertToThreeBytes(input2);
        assertThat(result1.length, is(3));
        input2 = 1000;
        result1 = OspfUtil.convertToThreeBytes(input2);
        assertThat(result1.length, is(3));
        input2 = 1;
        result1 = OspfUtil.convertToThreeBytes(input2);
        assertThat(result1.length, is(3));
    }

    /**
     * Tests convertToFourBytes() method.
     */
    @Test
    public void testConvertToFourBytes() throws Exception {
        input4 = 214748364110L;
        result1 = OspfUtil.convertToFourBytes(input4);
        assertThat(result1.length, is(4));
        input4 = 1000000;
        result1 = OspfUtil.convertToFourBytes(input4);
        assertThat(result1.length, is(4));
        input4 = 10000;
        result1 = OspfUtil.convertToFourBytes(input4);
        assertThat(result1.length, is(4));
        input4 = 1;
        result1 = OspfUtil.convertToFourBytes(input4);
        assertThat(result1.length, is(4));

    }

    /**
     * Tests convertToFourBytes() method.
     */
    @Test
    public void testConvertToFourBytes1() throws Exception {
        input4 = 2147483635;
        result1 = OspfUtil.convertToFourBytes(this.input4);
        assertThat(result1.length, is(4));
        this.input4 = 1000000;
        result1 = OspfUtil.convertToFourBytes(this.input4);
        assertThat(result1.length, is(4));
        this.input4 = 10000;
        result1 = OspfUtil.convertToFourBytes(this.input4);
        assertThat(result1.length, is(4));
        this.input4 = 1;
        result1 = OspfUtil.convertToFourBytes(this.input4);
        assertThat(result1.length, is(4));

    }

    /**
     * Tests addLengthAndCheckSum() method.
     */
    @Test
    public void testAddLengthAndCheckSum() throws Exception {
        result1 = OspfUtil.addLengthAndCheckSum(packet, ospfLengthPos1, ospfLengthPos2,
                                                ospfChecksumPos1, ospfChecksumPos2);
        assertThat(result1[ospfChecksumPos1], is(packet[ospfChecksumPos1]));
        assertThat(result1[ospfChecksumPos2], is(packet[ospfChecksumPos2]));
        assertThat(result1[ospfLengthPos1], is(packet[ospfLengthPos1]));
        assertThat(result1[ospfLengthPos2], is(packet[ospfLengthPos2]));
    }

    /**
     * Tests addMetadata() method.
     */
    @Test
    public void testAddMetadata() throws Exception {
        result1 = OspfUtil.addMetadata(2, packet, 1, Ip4Address.valueOf("1.1.1.1"));
        assertThat(result1, is(notNullValue()));
    }

    /**
     * Tests addLengthAndCheckSum() method.
     */
    @Test
    public void testAddLsaLengthAndCheckSum() throws Exception {
        result1 = OspfUtil.addLengthAndCheckSum(rLsa, lsaLengthPos1, lsaLengthPos2,
                                                lsaChecksumPos1, lsaChecksumPos2);
        assertThat(result1[lsaLengthPos1], is(rLsa[lsaLengthPos1]));
        assertThat(result1[lsaLengthPos2], is(rLsa[lsaLengthPos2]));
        assertThat(result1[lsaChecksumPos1], is(rLsa[lsaChecksumPos1]));
        assertThat(result1[lsaChecksumPos2], is(rLsa[lsaChecksumPos2]));
    }

    /**
     * Tests addMetadata() method.
     */
    @Test
    public void testAddMetaData() throws Exception {
        result1 = OspfUtil.addMetadata(2, packet, 1, Ip4Address.valueOf("2.2.2.2"));
        assertThat(result1, is(notNullValue()));
    }

    /**
     * Tests sameNetwork() method.
     */
    @Test
    public void testSameNetwork() throws Exception {
        result2 = OspfUtil.sameNetwork(Ip4Address.valueOf("10.10.10.10"), Ip4Address.valueOf("10.10.10.11"),
                                       Ip4Address.valueOf("255.255.255.255"));
        assertThat(result2, is(false));
        result2 = OspfUtil.sameNetwork(Ip4Address.valueOf("10.10.10.10"), Ip4Address.valueOf("10.10.10.10"),
                                       Ip4Address.valueOf("255.255.255.255"));
        assertThat(result2, is(true));
    }

    /**
     * Tests isOpaqueEnabled() method.
     */
    @Test
    public void testIsOpaqueEnabled() throws Exception {
        result2 = OspfUtil.isOpaqueEnabled(2);
        assertThat(result2, is(false));
    }

    /**
     * Tests sameNetwork() method.
     */
    @Test
    public void testisIsOpaqueEnabled() throws Exception {
        result2 = OspfUtil.isOpaqueEnabled(2);
        assertThat(result2, is(false));
    }

    /**
     * Tests readLsaHeader() method.
     */
    @Test
    public void testReadLsaHeader() throws Exception {
        byte[] header = {0, 10, 2, 1, 7, 7, 7, 7, 7, 7, 7, 7, -128, 0, 0, 2, 46, -126, 0,
                48, 0, 0, 0, 2, 1, 1, 1, 1, 10, 10, 10, 7, 1, 0, 0, 10, 10, 10, 10, 0, -1, -1, -1,
                0, 3, 0, 0, 10, 0, 10, 66, 10, 1, 0, 0, 1, 7, 7, 7, 7, -128, 0, 0, 1, -64, 79, 0,
                116, 0, 1, 0, 4, 0, 0, 0, 0, 0, 2, 0, 84, 0, 1, 0, 1, 1, 0, 0, 0, 0, 2, 0, 4, 10,
                10, 10, 0, 0, 5, 0, 4, 0, 0, 0, 0, 0, 6, 0, 4, 73, -104, -106, -128, 0, 7, 0, 4, 73,
                -104, -106, -128, 0, 8, 0, 32, 73, -104, -106, -128, 73, -104, -106, -128, 73, -104, -106,
                -128, 73, -104, -106, -128, 73, -104, -106, -128, 73, -104, -106, -128, 73, -104, -106, -128,
                73, -104, -106, -128, 0, 9, 0, 4, 0, 0, 0, 0};
        channelBuffer = ChannelBuffers.copiedBuffer(header);
        lsaHeader = OspfUtil.readLsaHeader(channelBuffer);
        assertThat(lsaHeader, is(notNullValue()));
    }

    /**
     * Tests readLsaHeader() method.
     */
    @Test
    public void testReadreadLsaHeader() throws Exception {
        byte[] header = {0, 2, 2, 1, -64, -88, -86, 3, -64, -88, -86, 3, -128, 0, 0, 1, 58, -100, 0, 48};
        channelBuffer = ChannelBuffers.copiedBuffer(header);
        lsaHeader = OspfUtil.readLsaHeader(channelBuffer);
        assertThat(lsaHeader, is(notNullValue()));
    }
}