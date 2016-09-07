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
package org.onosproject.ospf.controller.impl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip6Address;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Unit test class for OspfDeviceTedImpl.
 */
public class OspfDeviceTedImplTest {
    private OspfDeviceTedImpl ospfDeviceTed;

    @Before
    public void setUp() throws Exception {
        ospfDeviceTed = new OspfDeviceTedImpl();
    }

    @After
    public void tearDown() throws Exception {
        ospfDeviceTed = null;
    }

    /**
     * Tests ipv4RouterIds() getter method.
     */
    @Test
    public void testIpv4RouterIds() throws Exception {
        List list = new ArrayList();
        list.add(Ip4Address.valueOf("1.1.1.1"));
        ospfDeviceTed.setIpv4RouterIds(list);
        assertThat(ospfDeviceTed.ipv4RouterIds().size(), is(1));
    }

    /**
     * Tests ipv4RouterIds() setter method.
     */
    @Test
    public void testSetIpv4RouterIds() throws Exception {
        List list = new ArrayList();
        list.add(Ip4Address.valueOf("1.1.1.1"));
        ospfDeviceTed.setIpv4RouterIds(list);
        assertThat(ospfDeviceTed.ipv4RouterIds().size(), is(1));
    }

    /**
     * Tests abr() getter method.
     */
    @Test
    public void testAbr() throws Exception {
        ospfDeviceTed.setAbr(true);
        assertThat(ospfDeviceTed.abr(), is(true));
    }

    /**
     * Tests abr() setter method.
     */
    @Test
    public void testSetAbr() throws Exception {
        ospfDeviceTed.setAbr(true);
        assertThat(ospfDeviceTed.abr(), is(true));
    }

    /**
     * Tests asbr() getter method.
     */
    @Test
    public void testAsbr() throws Exception {
        ospfDeviceTed.setAsbr(true);
        assertThat(ospfDeviceTed.asbr(), is(true));
    }

    /**
     * Tests asbr() setter method.
     */
    @Test
    public void testSetAsbr() throws Exception {
        ospfDeviceTed.setAsbr(true);
        assertThat(ospfDeviceTed.asbr(), is(true));
    }

    /**
     * Tests topologyIds() getter method.
     */
    @Test
    public void testTopologyIds() throws Exception {
        List list = new ArrayList();
        list.add(Ip4Address.valueOf("1.1.1.1"));
        ospfDeviceTed.setTopologyIds(list);
        assertThat(ospfDeviceTed.topologyIds().size(), is(1));
    }

    /**
     * Tests topologyIds() setter method.
     */
    @Test
    public void testSetTopologyIds() throws Exception {
        List list = new ArrayList();
        list.add(Ip4Address.valueOf("1.1.1.1"));
        ospfDeviceTed.setTopologyIds(list);
        assertThat(ospfDeviceTed.topologyIds().size(), is(1));
    }

    /**
     * Tests ipv6RouterIds() getter method.
     */
    @Test
    public void testIpv6RouterIds() throws Exception {
        List list = new ArrayList();
        list.add(Ip6Address.valueOf(1));
        ospfDeviceTed.setIpv6RouterIds(list);
        assertThat(ospfDeviceTed.ipv6RouterIds().size(), is(1));
    }

    /**
     * Tests ipv6RouterIds() setter method.
     */
    @Test
    public void testSetIpv6RouterIds() throws Exception {
        List list = new ArrayList();
        list.add(Ip6Address.valueOf(1));
        ospfDeviceTed.setIpv6RouterIds(list);
        assertThat(ospfDeviceTed.ipv6RouterIds().size(), is(1));
    }
}
