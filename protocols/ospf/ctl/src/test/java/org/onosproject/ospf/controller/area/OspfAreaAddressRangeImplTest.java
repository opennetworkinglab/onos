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
package org.onosproject.ospf.controller.area;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.Ip4Address;
import org.onosproject.ospf.controller.OspfAreaAddressRange;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit test class for OspfAreaAddressRangeImpl.
 */
public class OspfAreaAddressRangeImplTest {

    private OspfAreaAddressRange ospfAreaAddressRange;
    private int result;
    private String result1;

    @Before
    public void setUp() throws Exception {
        ospfAreaAddressRange = new OspfAreaAddressRangeImpl();
    }

    @After
    public void tearDown() throws Exception {
        ospfAreaAddressRange = null;
    }

    /**
     * Tests ipAddress() getter method.
     */
    @Test
    public void testGetIpAddress() throws Exception {
        ospfAreaAddressRange.setIpAddress(Ip4Address.valueOf("1.1.1.1"));
        assertThat(ospfAreaAddressRange.ipAddress(), is(Ip4Address.valueOf("1.1.1.1")));
    }

    /**
     * Tests ipAddress() setter method.
     */
    @Test
    public void testSetIpAddress() throws Exception {
        ospfAreaAddressRange.setIpAddress(Ip4Address.valueOf("1.1.1.1"));
        assertThat(ospfAreaAddressRange.ipAddress(), is(Ip4Address.valueOf("1.1.1.1")));
    }

    /**
     * Tests mask() getter method.
     */
    @Test
    public void testGetMask() throws Exception {
        ospfAreaAddressRange.setMask("1.1.1.1");
        assertThat(ospfAreaAddressRange.mask(), is("1.1.1.1"));
    }

    /**
     * Tests mask() setter method.
     */
    @Test
    public void testSetMask() throws Exception {
        ospfAreaAddressRange.setMask("1.1.1.1");
        assertThat(ospfAreaAddressRange.mask(), is("1.1.1.1"));
    }

    /**
     * Tests isAdvertise() getter method.
     */
    @Test
    public void testIsAdvertise() throws Exception {
        ospfAreaAddressRange.setAdvertise(true);
        assertThat(ospfAreaAddressRange.isAdvertise(), is(true));
    }

    /**
     * Tests isAdvertise() setter method.
     */
    @Test
    public void testSetAdvertise() throws Exception {
        ospfAreaAddressRange.setAdvertise(true);
        assertThat(ospfAreaAddressRange.isAdvertise(), is(true));
    }

    /**
     * Tests equals() method.
     */
    @Test
    public void testEquals() throws Exception {
        assertThat(ospfAreaAddressRange.equals(new OspfAreaAddressRangeImpl()), is(true));
    }

    /**
     * Tests hashCode() method.
     */
    @Test
    public void testHashCode() throws Exception {
        result = ospfAreaAddressRange.hashCode();
        assertThat(result, is(notNullValue()));
    }

    /**
     * Tests to string method.
     */
    @Test
    public void testToString() throws Exception {
        result1 = ospfAreaAddressRange.toString();
        assertThat(result1, is(notNullValue()));
    }
}