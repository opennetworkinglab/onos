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

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.Ip4Address;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Unit test class for OpaqueLsaHeader.
 */
public class OpaqueLsaHeaderTest {

    private OpaqueLsaHeader opaqueHeader;
    private OpaqueLsaHeader opaqueLsaHeader1;
    private int num;
    private byte[] result;
    private int result1;

    @Before
    public void setUp() throws Exception {
        opaqueHeader = new OpaqueLsaHeader();
    }

    @After
    public void tearDown() throws Exception {
        opaqueHeader = null;
        opaqueLsaHeader1 = null;
        result = null;
    }

    /**
     * Tests populateHeader() method.
     */
    @Test
    public void testPopulateHeader() throws Exception {
        opaqueLsaHeader1 = new OpaqueLsaHeader();
        opaqueLsaHeader1.setLsPacketLen(10);
        opaqueLsaHeader1.setAdvertisingRouter(Ip4Address.valueOf("1.1.1.1"));
        opaqueLsaHeader1.setOptions(2);
        opaqueLsaHeader1.setAge(20);
        opaqueLsaHeader1.setLsType(3);
        opaqueLsaHeader1.setOpaqueId(1);
        opaqueLsaHeader1.setOpaqueType(3);
        opaqueLsaHeader1.setLsCheckSum(1234);
        opaqueLsaHeader1.setLsSequenceNo(456789);
        opaqueLsaHeader1.populateHeader(opaqueLsaHeader1);
        assertThat(opaqueLsaHeader1, is(notNullValue()));
    }

    /**
     * Tests opaqueId() getter method.
     */
    @Test
    public void testGetOpaqueId() throws Exception {
        opaqueHeader.setOpaqueId(1);
        num = opaqueHeader.opaqueId();
        assertThat(num, is(1));
    }

    /**
     * Tests opaqueId() setter method.
     */
    @Test
    public void testSetOpaqueId() throws Exception {
        opaqueHeader.setOpaqueId(1);
        num = opaqueHeader.opaqueId();
        assertThat(num, is(1));
    }

    /**
     * Tests opaqueType() getter method.
     */
    @Test
    public void testGetOpaqueType() throws Exception {
        opaqueHeader.setOpaqueType(1);
        num = opaqueHeader.opaqueType();
        assertThat(num, is(1));
    }

    /**
     * Tests opaqueType() setter method.
     */
    @Test
    public void testSetOpaqueType() throws Exception {
        opaqueHeader.setOpaqueType(1);
        num = opaqueHeader.opaqueType();
        assertThat(num, is(1));
    }

    /**
     * Tests getOpaqueLsaHeaderAsByteArray() method.
     */
    @Test
    public void testGetOpaqueLsaHeaderAsByteArray() throws Exception {
        result = opaqueHeader.getOpaqueLsaHeaderAsByteArray();
        assertThat(result, is(notNullValue()));
    }

    /**
     * Tests to string method.
     */
    @Test
    public void testToString() throws Exception {
        assertThat(opaqueHeader.toString(), is(notNullValue()));
    }

    /**
     * Tests hashCode() method.
     */
    @Test
    public void testHashcode() throws Exception {

        result1 = opaqueHeader.hashCode();
        assertThat(result1, is(Matchers.notNullValue()));

    }
}