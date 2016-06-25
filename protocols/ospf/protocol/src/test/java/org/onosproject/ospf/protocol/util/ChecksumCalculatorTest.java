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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.Ip4Address;
import org.onosproject.ospf.protocol.lsa.LsaHeader;
import org.onosproject.ospf.protocol.lsa.OpaqueLsaHeader;
import org.onosproject.ospf.protocol.lsa.types.AsbrSummaryLsa;
import org.onosproject.ospf.protocol.lsa.types.ExternalLsa;
import org.onosproject.ospf.protocol.lsa.types.NetworkLsa;
import org.onosproject.ospf.protocol.lsa.types.OpaqueLsa10;
import org.onosproject.ospf.protocol.lsa.types.OpaqueLsa11;
import org.onosproject.ospf.protocol.lsa.types.OpaqueLsa9;
import org.onosproject.ospf.protocol.lsa.types.RouterLsa;
import org.onosproject.ospf.protocol.lsa.types.SummaryLsa;
import org.onosproject.ospf.protocol.ospfpacket.types.DdPacket;
import org.onosproject.ospf.protocol.ospfpacket.types.HelloPacket;
import org.onosproject.ospf.protocol.ospfpacket.types.LsAcknowledge;
import org.onosproject.ospf.protocol.ospfpacket.types.LsRequest;
import org.onosproject.ospf.protocol.ospfpacket.types.LsUpdate;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit test class for ChecksumCalculator.
 */
public class ChecksumCalculatorTest {

    private final int ospfChecksumPos1 = 12;
    private final int ospfChecksumPos2 = 13;
    private final int lsaChecksumPos1 = 16;
    private final int lsaChecksumPos2 = 17;
    private final byte[] updatePacket = {1, 1, 1, 1, 2, 4, 0, -96, 9, 9, 9, 9, 5, 5, 5, 5, 62, 125,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 100, 2, 10, 1, 0, 0, 1, 9, 9, 9, 9, -128,
            0, 0, 1, -7, 62, 0, -124, 0, 2, 0, 108, 0, 1, 0, 1, 2, 0, 0, 0, 0, 2, 0, 4, -64, -88,
            7, -91, 0, 3, 0, 4, -64, -88, 7, -91, 0, 4, 0, 4, 0, 0, 0, 0, 0, 5, 0, 4, 0, 0, 0, 1,
            0, 6, 0, 4, 0, 0, 0, 0, 0, 7, 0, 4, 0, 0, 0, 0, 0, 8, 0, 32, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 9,
            0, 4, 0, 0, 0, 0, -128, 2, 0, 4, 0, 0, 0, 1};

    private final byte[] helloPacket = {2, 1, 0, 44, -64, -88, -86, 8, 0, 0, 0, 1, 39, 59,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, -1, 0, 0, 10, 2, 1, 0, 0, 0, 40, -64, -88,
            -86, 8, 0, 0, 0, 0};
    private final byte[] rlsa = {14, 16, 2, 1, -64, -88, -86, 2, -64, -88, -86, 2, -128, 0,
            0, 1, 74, -114, 0, 48, 2, 0, 0, 2, -64, -88, -86, 0, -1, -1, -1, 0, 3, 0, 0, 10,
            -64, -88, -86, 0, -1, -1, -1, 0, 3, 0, 0, 10};
    private ChecksumCalculator checksumCalculator;
    private boolean validate;
    private HelloPacket hello;
    private LsUpdate message;
    private DdPacket message1;
    private LsRequest message2;
    private RouterLsa router;
    private LsAcknowledge lsack;
    private ExternalLsa external;
    private NetworkLsa external1;
    private SummaryLsa external2;
    private AsbrSummaryLsa external3;
    private OpaqueLsa9 external4;
    private OpaqueLsa10 external5;
    private OpaqueLsa11 external6;
    private byte[] result;

    @Before
    public void setUp() throws Exception {
        checksumCalculator = new ChecksumCalculator();
    }

    @After
    public void tearDown() throws Exception {
        checksumCalculator = null;
        hello = null;
        message = null;
        message1 = null;
        message2 = null;
        router = null;
        lsack = null;
        external = null;
        external1 = null;
        external2 = null;
        external3 = null;
        result = null;
    }

    /**
     * Tests convertToSixteenBits() method.
     */
    @Test
    public void testConvertToSixteenBits() throws Exception {
        int num = checksumCalculator.convertToSixteenBits("16cdd");
        assertThat(num, is(27870));
    }

    /**
     * Tests isValidOspfCheckSum() method.
     */
    @Test
    public void testIsValidOspfCheckSum() throws Exception {
        hello = new HelloPacket();
        hello.setOspfVer(2);
        hello.setOspftype(1);
        hello.setOspfPacLength(172);
        hello.setRouterId(Ip4Address.valueOf("192.168.170.3"));
        hello.setAreaId(Ip4Address.valueOf("0.0.0.1"));
        hello.setChecksum(5537);
        hello.setAuthType(0);
        hello.setAuthentication(0);
        validate = checksumCalculator.isValidOspfCheckSum(hello, ospfChecksumPos1, ospfChecksumPos2);
        assertThat(validate, is(false));
        lsack = new LsAcknowledge();
        lsack.setOspfVer(2);
        lsack.setOspftype(5);
        lsack.setOspfPacLength(172);
        lsack.setRouterId(Ip4Address.valueOf("192.168.170.3"));
        lsack.setAreaId(Ip4Address.valueOf("0.0.0.1"));
        lsack.setChecksum(37537);
        lsack.setAuthType(0);
        lsack.setAuthentication(0);
        validate = checksumCalculator.isValidOspfCheckSum(lsack, ospfChecksumPos1, ospfChecksumPos2);
        assertThat(validate, is(true));
        message = new LsUpdate();
        message.setOspfVer(2);
        message.setOspftype(5);
        message.setOspfPacLength(172);
        message.setRouterId(Ip4Address.valueOf("192.168.170.3"));
        message.setAreaId(Ip4Address.valueOf("0.0.0.1"));
        message.setChecksum(37537);
        message.setAuthType(0);
        message.setAuthentication(0);
        validate = checksumCalculator.isValidOspfCheckSum(message, ospfChecksumPos1, ospfChecksumPos2);
        assertThat(validate, is(true));
        message1 = new DdPacket();
        message1.setOspfVer(2);
        message1.setOspftype(5);
        message1.setOspfPacLength(172);
        message1.setRouterId(Ip4Address.valueOf("192.168.170.3"));
        message1.setAreaId(Ip4Address.valueOf("0.0.0.1"));
        message1.setChecksum(37537);
        message1.setAuthType(0);
        message1.setAuthentication(0);
        validate = checksumCalculator.isValidOspfCheckSum(message1, ospfChecksumPos1, ospfChecksumPos2);
        assertThat(validate, is(true));
        message2 = new LsRequest();
        message2.setOspfVer(2);
        message2.setOspftype(5);
        message2.setOspfPacLength(172);
        message2.setRouterId(Ip4Address.valueOf("192.168.170.3"));
        message2.setAreaId(Ip4Address.valueOf("0.0.0.1"));
        message2.setChecksum(37537);
        message2.setAuthType(0);
        message2.setAuthentication(0);
        validate = checksumCalculator.isValidOspfCheckSum(message2, ospfChecksumPos1, ospfChecksumPos2);
        assertThat(validate, is(true));
    }

    /**
     * Tests isValidLsaCheckSum() method.
     */
    @Test
    public void testIsValidLsaCheckSum() throws Exception {
        router = new RouterLsa();
        router.setAge(1);
        router.setOptions(2);
        router.setLsType(1);
        router.setLinkStateId("192.168.170.3");
        router.setAdvertisingRouter(Ip4Address.valueOf("192.168.170.3"));
        router.setLsSequenceNo(2147483649L);
        router.setLsCheckSum(49499);
        router.setLsPacketLen(48);
        validate = checksumCalculator.isValidLsaCheckSum(router, router.lsType(), lsaChecksumPos1, lsaChecksumPos2);
        assertThat(validate, is(true));

    }

    /**
     * Tests isValidLsaCheckSum() method.
     */
    @Test
    public void testIsValidLsaCheckSum4() throws Exception {
        external = new ExternalLsa(new LsaHeader());
        external.setAge(2);
        external.setOptions(2);
        external.setLsType(5);
        external.setLinkStateId("80.212.16.0");
        external.setAdvertisingRouter(Ip4Address.valueOf("192.168.170.2"));
        external.setLsSequenceNo(2147483649L);
        external.setLsCheckSum(25125);
        external.setLsPacketLen(36);
        validate = checksumCalculator.isValidLsaCheckSum(external, external.lsType(), lsaChecksumPos1, lsaChecksumPos2);
        assertThat(validate, is(false));
    }

    /**
     * Tests isValidLsaCheckSum() method.
     */
    @Test(expected = Exception.class)
    public void testIsValidLsaCheckSum5() throws Exception {
        external1 = new NetworkLsa();
        external1.setAge(2);
        external1.setOptions(2);
        external1.setLsType(2);
        external1.setLinkStateId("80.212.16.0");
        external1.setAdvertisingRouter(Ip4Address.valueOf("192.168.170.2"));
        external1.setLsSequenceNo(2147483649L);
        external1.setLsCheckSum(25125);
        external1.setLsPacketLen(36);
        validate = checksumCalculator.isValidLsaCheckSum(external1, external1.lsType(),
                                                         lsaChecksumPos1, lsaChecksumPos2);
        assertThat(validate, is(false));
    }

    /**
     * Tests isValidLsaCheckSum() method.
     */
    @Test
    public void testIsValidLsaCheckSum6() throws Exception {

        external2 = new SummaryLsa(new LsaHeader());
        external2.setAge(2);
        external2.setOptions(2);
        external2.setLsType(3);
        external2.setLinkStateId("80.212.16.0");
        external2.setAdvertisingRouter(Ip4Address.valueOf("192.168.170.2"));
        external2.setLsSequenceNo(2147483649L);
        external2.setLsCheckSum(25125);
        external2.setLsPacketLen(36);
        validate = checksumCalculator.isValidLsaCheckSum(external2, external2.lsType(),
                                                         lsaChecksumPos1, lsaChecksumPos2);
        assertThat(validate, is(false));
    }

    /**
     * Tests isValidLsaCheckSum() method.
     */
    @Test(expected = Exception.class)
    public void testIsValidLsaCheckSum7() throws Exception {
        external3 = new AsbrSummaryLsa(new LsaHeader());
        external3.setAge(2);
        external3.setOptions(2);
        external3.setLsType(4);
        external3.setLinkStateId("80.212.16.0");
        external3.setAdvertisingRouter(Ip4Address.valueOf("192.168.170.2"));
        external3.setLsSequenceNo(2147483649L);
        external3.setLsCheckSum(25125);
        external3.setLsPacketLen(36);
        validate = checksumCalculator.isValidLsaCheckSum(external3, external3.lsType(),
                                                         lsaChecksumPos1, lsaChecksumPos2);
        assertThat(validate, is(false));
    }

    /**
     * Tests isValidLsaCheckSum() method.
     */
    @Test(expected = Exception.class)
    public void testIsValidLsaCheckSum1() throws Exception {
        external4 = new OpaqueLsa9(new OpaqueLsaHeader());
        external4.setAge(2);
        external4.setOptions(2);
        external4.setLsType(9);
        external4.setLinkStateId("80.212.16.0");
        external4.setAdvertisingRouter(Ip4Address.valueOf("192.168.170.2"));
        external4.setLsSequenceNo(2147483649L);
        external4.setLsCheckSum(25125);
        external4.setLsPacketLen(36);
        validate = checksumCalculator.isValidLsaCheckSum(external4, external4.lsType(),
                                                         lsaChecksumPos1, lsaChecksumPos2);
        assertThat(validate, is(false));
    }

    /**
     * Tests isValidLsaCheckSum() method.
     */
    @Test(expected = Exception.class)
    public void testIsValidLsaCheckSum2() throws Exception {
        external5 = new OpaqueLsa10(new OpaqueLsaHeader());
        external5.setAge(2);
        external5.setOptions(2);
        external5.setLsType(10);
        external5.setLinkStateId("80.212.16.0");
        external5.setAdvertisingRouter(Ip4Address.valueOf("192.168.170.2"));
        external5.setLsSequenceNo(2147483649L);
        external5.setLsCheckSum(25125);
        external5.setLsPacketLen(36);
        validate = checksumCalculator.isValidLsaCheckSum(external5, external5.lsType(),
                                                         lsaChecksumPos1, lsaChecksumPos2);

        assertThat(validate, is(false));
    }

    /**
     * Tests isValidLsaCheckSum() method.
     */
    @Test(expected = Exception.class)
    public void testIsValidLsaCheckSum3() throws Exception {
        external6 = new OpaqueLsa11(new OpaqueLsaHeader());
        external6.setAge(2);
        external6.setOptions(2);
        external6.setLsType(10);
        external6.setLinkStateId("80.212.16.0");
        external6.setAdvertisingRouter(Ip4Address.valueOf("192.168.170.2"));
        external6.setLsSequenceNo(2147483649L);
        external6.setLsCheckSum(25125);
        external6.setLsPacketLen(36);
        validate = checksumCalculator.isValidLsaCheckSum(external6, external6.lsType(),
                                                         lsaChecksumPos1, lsaChecksumPos2);
        assertThat(validate, is(false));
    }

    /**
     * Tests validateLsaCheckSum() method.
     */
    @Test
    public void testValidateLsaCheckSum() throws Exception {
        assertThat(checksumCalculator.validateLsaCheckSum(rlsa, lsaChecksumPos1,
                                                          lsaChecksumPos2), is(true));

    }

    /**
     * Tests validateOspfCheckSum() method.
     */
    @Test
    public void testValidateOspfCheckSum() throws Exception {
        assertThat(checksumCalculator.validateOspfCheckSum(helloPacket, ospfChecksumPos1,
                                                           ospfChecksumPos2), is(true));
    }

    /**
     * Tests calculateLsaChecksum() method.
     */
    @Test
    public void testCalculateLsaChecksum() throws Exception {
        result = checksumCalculator.calculateLsaChecksum(rlsa, lsaChecksumPos1, lsaChecksumPos2);
        assertThat(result, is(notNullValue()));
    }

    /**
     * Tests calculateOspfCheckSum() method.
     */
    @Test
    public void testCalculateOspfCheckSum() throws Exception {
        result = checksumCalculator.calculateOspfCheckSum(helloPacket, ospfChecksumPos1,
                                                          ospfChecksumPos2);
        assertThat(result, is(notNullValue()));
        result = checksumCalculator.calculateOspfCheckSum(updatePacket, ospfChecksumPos1,
                                                          ospfChecksumPos2);
        assertThat(result, is(notNullValue()));
    }
}