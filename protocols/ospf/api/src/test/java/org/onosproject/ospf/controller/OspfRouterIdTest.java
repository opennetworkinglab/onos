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
package org.onosproject.ospf.controller;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;

import java.net.URI;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit test class for OspfRouterId.
 */
public class OspfRouterIdTest {

    private OspfRouterId ospfRouterId;

    @Before
    public void setUp() throws Exception {
        ospfRouterId = new OspfRouterId(IpAddress.valueOf("2.2.2.2"));
    }

    @After
    public void tearDown() throws Exception {
        ospfRouterId = null;
    }

    /**
     * Tests constructor.
     */
    @Test
    public void testOspfRouterId() throws Exception {
        assertThat(OspfRouterId.ospfRouterId(IpAddress.valueOf("2.2.2.2")), instanceOf(OspfRouterId.class));

    }

    /**
     * Tests ipAddress() getter method.
     */
    @Test
    public void testIpAddress() throws Exception {
        assertThat(ospfRouterId.ipAddress(), instanceOf(IpAddress.class));
    }

    /**
     * Tests to string method.
     */
    @Test
    public void testToString() throws Exception {
        assertThat(ospfRouterId.toString(), is(notNullValue()));
    }

    /**
     * Tests equals() method.
     */
    @Test
    public void testEquals() throws Exception {
        assertThat(ospfRouterId.equals(new OspfRouterId(IpAddress.valueOf("3.3.3.3"))), is(false));
    }

    /**
     * Tests hashCode() method.
     */
    @Test
    public void testHashCode() throws Exception {
        assertThat(ospfRouterId.hashCode(), is(notNullValue()));
    }

    /**
     * Tests constructor.
     */
    @Test
    public void testOspfRouterId1() throws Exception {
        assertThat(OspfRouterId.ospfRouterId(OspfRouterId.uri(ospfRouterId)), instanceOf(OspfRouterId.class));
    }

    /**
     * Tests uri() method.
     */
    @Test
    public void testUri() throws Exception {
        assertThat(OspfRouterId.uri(IpAddress.valueOf("2.2.2.2")), instanceOf(URI.class));
    }

    /**
     * Tests uri() method..
     */
    @Test
    public void testUri1() throws Exception {
        assertThat(OspfRouterId.uri(ospfRouterId), instanceOf(URI.class));
    }
}