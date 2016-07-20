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
package org.onosproject.isis.io.util;

import com.google.common.primitives.Bytes;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.Ip4Address;
import org.onosproject.isis.controller.IsisPduType;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Unit test class for IsisUtil.
 */
public class IsisUtilTest {

    private final String systemId = "2929.2929.2929";
    private final String lanId = "2929.2929.2929.01";
    private final String areaAddres = "490001";
    private final byte[] l1Lsp = {
            -125, 27, 1, 0, 18, 1, 0, 0, 0, 86, 4, -81, 34, 34, 34,
            34, 34, 34, 0, 0, 0, 0, 0, 9, 99, 11, 1, 1, 4, 3, 73,
            0, 10, -127, 1, -52, -119, 2, 82, 50, -124, 4, -64, -88, 10, 1, -128,
            24, 10, -128, -128, -128, 10, 0, 10, 0, -1, -1, -1, -4, 10, -128, -128,
            -128, -64, -88, 10, 0, -1, -1, -1, 0, 2, 12, 0, 10, -128, -128, -128,
            51, 51, 51, 51, 51, 51, 2
    };
    private final byte[] intger = {0, 0, 0, 1};
    private Ip4Address ip4Address1 = Ip4Address.valueOf("10.10.10.10");
    private Ip4Address ip4Address2 = Ip4Address.valueOf("10.10.10.11");
    private Ip4Address mask = Ip4Address.valueOf("255.255.255.0");
    private boolean result;
    private String result1;
    private byte[] result2;
    private int result3;
    private long result4;
    private byte[] prefixBytes = {0, 0, 0, 1};
    private String prefix = "192.16.17";

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    /**
     * Tests sameNetwork() method.
     */
    @Test
    public void testSameNetwork() throws Exception {
        result = IsisUtil.sameNetwork(ip4Address1, ip4Address2, mask.toOctets());
        assertThat(result, is(true));
    }

    /**
     * Tests systemId() method.
     */
    @Test
    public void testSystemId() throws Exception {
        result1 = IsisUtil.systemId(Bytes.toArray(
                IsisUtil.sourceAndLanIdToBytes(systemId)));
        assertThat(result1, is(systemId));
    }

    /**
     * Tests systemIdPlus() method.
     */
    @Test
    public void testSystemIdPlus() throws Exception {
        result1 = IsisUtil.systemIdPlus(Bytes.toArray(
                IsisUtil.sourceAndLanIdToBytes(lanId)));
        assertThat(result1, is(lanId));
    }

    /**
     * Tests areaAddres() method.
     */
    @Test
    public void testAreaAddres() throws Exception {
        result1 = IsisUtil.areaAddres(Bytes.toArray(
                IsisUtil.areaAddressToBytes(areaAddres)));
        assertThat(result1, is(areaAddres));
    }

    /**
     * Tests areaAddressToBytes() method.
     */
    @Test
    public void testAreaAddressToBytes() throws Exception {
        result2 = Bytes.toArray(IsisUtil.areaAddressToBytes(areaAddres));
        assertThat(result2, is(notNullValue()));
    }

    /**
     * Tests getPduHeaderLength() method.
     */
    @Test
    public void testGetPduHeaderLength() throws Exception {
        result3 = IsisUtil.getPduHeaderLength(IsisPduType.L1CSNP.value());
        assertThat(result3, is(33));
        result3 = IsisUtil.getPduHeaderLength(IsisPduType.L1PSNP.value());
        assertThat(result3, is(17));
        result3 = IsisUtil.getPduHeaderLength(IsisPduType.L1HELLOPDU.value());
        assertThat(result3, is(27));
        result3 = IsisUtil.getPduHeaderLength(IsisPduType.P2PHELLOPDU.value());
        assertThat(result3, is(20));
    }

    /**
     * Tests addLengthAndMarkItInReserved() method.
     */
    @Test
    public void testAddLengthAndMarkItInReserved() throws Exception {
        result2 = IsisUtil.addLengthAndMarkItInReserved(l1Lsp,
                                                        IsisConstants.LENGTHPOSITION, IsisConstants.LENGTHPOSITION + 1,
                                                        IsisConstants.RESERVEDPOSITION);
        assertThat(result2, is(notNullValue()));
    }

    /**
     * Tests addChecksum() method.
     */
    @Test
    public void testAddChecksum() throws Exception {
        result2 = IsisUtil.addChecksum(l1Lsp,
                                       IsisConstants.CHECKSUMPOSITION, IsisConstants.CHECKSUMPOSITION + 1);
        assertThat(result2, is(notNullValue()));
    }

    /**
     * Tests framePacket() method.
     */
    @Test
    public void testFramePacket() throws Exception {
        result2 = IsisUtil.framePacket(l1Lsp, 2);
        assertThat(result2, is(notNullValue()));
    }

    /**
     * Tests sourceAndLanIdToBytes() method.
     */
    @Test
    public void testSourceAndLanIdToBytes() throws Exception {
        result2 = Bytes.toArray(IsisUtil.sourceAndLanIdToBytes(lanId));
        assertThat(result2, is(notNullValue()));
    }

    /**
     * Tests getPaddingTlvs() method.
     */
    @Test
    public void testGetPaddingTlvs() throws Exception {
        result2 = IsisUtil.getPaddingTlvs(250);
        assertThat(result2, is(notNullValue()));
    }

    /**
     * Tests convertToTwoBytes() method.
     */
    @Test
    public void testConvertToTwoBytes() throws Exception {
        result2 = IsisUtil.convertToTwoBytes(250);
        assertThat(result2.length, is(2));
    }

    /**
     * Tests convertToFourBytes() method.
     */
    @Test
    public void testConvertToFourBytes() throws Exception {
        result2 = IsisUtil.convertToFourBytes(250);
        assertThat(result2.length, is(4));
    }

    /**
     * Tests byteToInteger() method.
     */
    @Test
    public void testByteToInteger() throws Exception {
        result3 = IsisUtil.byteToInteger(intger);
        assertThat(result3, is(1));
    }

    /**
     * Tests byteToInteger() method.
     */
    @Test
    public void testByteToLong() throws Exception {
        result4 = IsisUtil.byteToLong(intger);
        assertThat(result4, is(1L));
    }

    /**
     * Tests convertToFourBytes() method.
     */
    @Test
    public void testConvertToFourBytes1() throws Exception {
        result2 = IsisUtil.convertToFourBytes(250L);
        assertThat(result2.length, is(4));
    }

    /**
     * Tests toFourBitBinary() method.
     */
    @Test
    public void testToEightBitBinary() throws Exception {
        result1 = IsisUtil.toEightBitBinary("01");
        assertThat(result1.length(), is(8));
    }

    /**
     * Tests toFourBitBinary() method.
     */
    @Test
    public void testToFourBitBinary() throws Exception {
        result1 = IsisUtil.toFourBitBinary("01");
        assertThat(result1.length(), is(4));
    }

    /**
     * Tests convertToThreeBytes() method.
     */
    @Test
    public void testConvertToThreeBytes() throws Exception {
        result2 = IsisUtil.convertToThreeBytes(30);
        assertThat(result2.length, is(3));
    }

    /**
     * Tests prefixConversion() method.
     */
    @Test
    public void testPrefixConversion() throws Exception {
        result1 = IsisUtil.prefixConversion(prefixBytes);
        assertThat(result1, is(notNullValue()));
    }

    /**
     * Tests prefixToBytes() method.
     */
    @Test
    public void testPrefixToBytes() throws Exception {
        result2 = IsisUtil.prefixToBytes(prefix);
        assertThat(result2, is(notNullValue()));
    }
}