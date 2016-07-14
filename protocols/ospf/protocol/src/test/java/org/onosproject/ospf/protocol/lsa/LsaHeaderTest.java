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
package org.onosproject.ospf.protocol.lsa;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.Ip4Address;
import org.onosproject.ospf.controller.OspfLsaType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Unit test class for LsaHeader.
 */
public class LsaHeaderTest {

    private LsaHeader lsaHeader;
    private int result;
    private Ip4Address result1;
    private long result2;
    private OspfLsaType ospflsaType;
    private LsaHeader header;
    private byte[] result3;
    private LsaHeader lsaHeader1;
    private String result4;

    @Before
    public void setUp() throws Exception {
        lsaHeader = new LsaHeader();
    }

    @After
    public void tearDown() throws Exception {
        lsaHeader = null;
        result1 = null;
        ospflsaType = null;
        header = null;
        result3 = null;
        lsaHeader1 = null;
    }

    /**
     * Tests equals() method.
     */
    @Test
    public void testEquals() throws Exception {
        assertThat(lsaHeader.equals(new LsaHeader()), is(true));
    }

    /**
     * Tests hashCode() method.
     */
    @Test
    public void testHashCode() throws Exception {
        result = lsaHeader.hashCode();
        assertThat(result, is(notNullValue()));
    }

    /**
     * Tests age() getter method.
     */
    @Test
    public void testGetAge() throws Exception {
        lsaHeader.setAge(10);
        result = lsaHeader.age();
        assertThat(result, is(10));
    }

    /**
     * Tests age() setter method.
     */
    @Test
    public void testSetAge() throws Exception {
        lsaHeader.setAge(10);
        result = lsaHeader.age();
        assertThat(result, is(10));
    }

    /**
     * Tests options() getter method.
     */
    @Test
    public void testGetOptions() throws Exception {
        lsaHeader.setOptions(2);
        result = lsaHeader.options();
        assertThat(result, is(2));
    }

    /**
     * Tests options() setter method.
     */
    @Test
    public void testSetOptions() throws Exception {
        lsaHeader.setOptions(2);
        result = lsaHeader.options();
        assertThat(result, is(2));
    }

    /**
     * Tests lsType() getter method.
     */
    @Test
    public void testGetLsType() throws Exception {
        lsaHeader.setLsType(1);
        result = lsaHeader.lsType();
        assertThat(result, is(1));
    }

    /**
     * Tests lsType() setter method.
     */
    @Test
    public void testSetLsType() throws Exception {
        lsaHeader.setLsType(1);
        result = lsaHeader.lsType();
        assertThat(result, is(1));
    }

    /**
     * Tests linkStateId() getter method.
     */
    @Test
    public void testGetLinkStateId() throws Exception {
        lsaHeader.setLinkStateId("10.226.165.164");
        result4 = lsaHeader.linkStateId();
        assertThat(result4, is("10.226.165.164"));
    }

    /**
     * Tests linkStateId() setter method.
     */
    @Test
    public void testSetLinkStateId() throws Exception {
        lsaHeader.setLinkStateId("10.226.165.164");
        result4 = lsaHeader.linkStateId();
        assertThat(result4, is("10.226.165.164"));
    }

    /**
     * Tests advertisingRouter() setter method.
     */
    @Test
    public void testGetAdvertisingRouter() throws Exception {
        lsaHeader.setAdvertisingRouter(Ip4Address.valueOf("10.226.165.164"));
        result1 = lsaHeader.advertisingRouter();
        assertThat(result1, is(Ip4Address.valueOf("10.226.165.164")));
    }

    /**
     * Tests advertisingRouter() setter method.
     */
    @Test
    public void testSetAdvertisingRouter() throws Exception {
        lsaHeader.setAdvertisingRouter(Ip4Address.valueOf("10.226.165.164"));
        result1 = lsaHeader.advertisingRouter();
        assertThat(result1, is(Ip4Address.valueOf("10.226.165.164")));
    }

    /**
     * Tests lsSequenceNo() getter method.
     */
    @Test
    public void testGetLsSequenceNo() throws Exception {
        lsaHeader.setLsSequenceNo(222);
        result2 = lsaHeader.lsSequenceNo();
        assertThat(result2, is(222L));
    }

    /**
     * Tests lsSequenceNo() setter method.
     */
    @Test
    public void testSetLsSequenceNo() throws Exception {
        lsaHeader.setLsSequenceNo(222);
        result2 = lsaHeader.lsSequenceNo();
        assertThat(result2, is(222L));
    }

    /**
     * Tests lsCheckSum() getter method.
     */
    @Test
    public void testGetLsChecksum() throws Exception {
        lsaHeader.setLsCheckSum(2);
        result = lsaHeader.lsCheckSum();
        assertThat(result, is(2));
    }

    /**
     * Tests lsCheckSum() setter method.
     */
    @Test
    public void testSetLsChecksum() throws Exception {
        lsaHeader.setLsCheckSum(2);
        result = lsaHeader.lsCheckSum();
        assertThat(result, is(2));
    }

    /**
     * Tests lsPacketLen() getter method.
     */
    @Test
    public void testGetLsPacketLen() throws Exception {
        lsaHeader.setLsPacketLen(48);
        result = lsaHeader.lsPacketLen();
        assertThat(result, is(48));
    }

    /**
     * Tests lsPacketLen() getter method.
     */
    @Test
    public void testSetLsPacketLen() throws Exception {
        lsaHeader.setLsPacketLen(48);
        result = lsaHeader.lsPacketLen();
        assertThat(result, is(48));
    }

    /**
     * Tests getOspfLsaType() getter method.
     */
    @Test
    public void testGetOspfLsaType() throws Exception {
        lsaHeader.setLsType(1);
        ospflsaType = lsaHeader.getOspfLsaType();
        assertThat(ospflsaType, is(notNullValue()));
        assertThat(ospflsaType, is(OspfLsaType.ROUTER));
        lsaHeader.setLsType(2);
        ospflsaType = lsaHeader.getOspfLsaType();
        assertThat(ospflsaType, is(notNullValue()));
        assertThat(ospflsaType, is(OspfLsaType.NETWORK));
        lsaHeader.setLsType(3);
        ospflsaType = lsaHeader.getOspfLsaType();
        assertThat(ospflsaType, is(notNullValue()));
        assertThat(ospflsaType, is(OspfLsaType.SUMMARY));
        lsaHeader.setLsType(4);
        ospflsaType = lsaHeader.getOspfLsaType();
        assertThat(ospflsaType, is(notNullValue()));
        assertThat(ospflsaType, is(OspfLsaType.ASBR_SUMMARY));
        lsaHeader.setLsType(5);
        ospflsaType = lsaHeader.getOspfLsaType();
        assertThat(ospflsaType, is(notNullValue()));
        assertThat(ospflsaType, is(OspfLsaType.EXTERNAL_LSA));
        lsaHeader.setLsType(6);
        ospflsaType = lsaHeader.getOspfLsaType();
        assertThat(ospflsaType, is(notNullValue()));
        assertThat(ospflsaType, is(OspfLsaType.UNDEFINED));
    }

    /**
     * Tests lsaHeader() getter method.
     */
    @Test
    public void testGetLsaHeader() throws Exception {
        header = (LsaHeader) lsaHeader.lsaHeader();
        assertThat(header, instanceOf(LsaHeader.class));
    }

    /**
     * Tests getLsaHeaderAsByteArray() method.
     */
    @Test
    public void testGetLsaHeaderAsByteArray() throws Exception {
        result3 = lsaHeader.getLsaHeaderAsByteArray();
        assertThat(result3, is(notNullValue()));
    }

    /**
     * Tests to string method.
     */
    @Test
    public void testToString() throws Exception {
        assertThat(lsaHeader.toString(), is(notNullValue()));
    }

    /**
     * Tests populateHeader() method.
     */
    @Test
    public void testPopulateHeader() throws Exception {
        lsaHeader1 = new LsaHeader();
        lsaHeader1.setLsPacketLen(10);
        lsaHeader1.setAdvertisingRouter(Ip4Address.valueOf("1.1.1.1"));
        lsaHeader1.setOptions(2);
        lsaHeader1.setAge(20);
        lsaHeader1.setLsType(3);
        lsaHeader1.setLinkStateId("2.2.2.2");
        lsaHeader1.setLsCheckSum(1234);
        lsaHeader1.setLsSequenceNo(456789);
        lsaHeader.populateHeader(lsaHeader1);
        assertThat(lsaHeader1, is(notNullValue()));
    }
}