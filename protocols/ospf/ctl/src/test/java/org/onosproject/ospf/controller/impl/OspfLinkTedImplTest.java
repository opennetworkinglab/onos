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
import org.onlab.util.Bandwidth;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Unit test class for OspfDeviceTedImpl.
 */
public class OspfLinkTedImplTest {
    private OspfLinkTedImpl ospfLinkTed;

    @Before
    public void setUp() throws Exception {
        ospfLinkTed = new OspfLinkTedImpl();
    }

    @After
    public void tearDown() throws Exception {
        ospfLinkTed = null;
    }

    /**
     * Tests maximumLink() getter method.
     */
    @Test
    public void testMaximumLink() throws Exception {

        ospfLinkTed.setMaximumLink(Bandwidth.bps(1234));
        assertThat(ospfLinkTed.maximumLink(), is(Bandwidth.bps(1234)));
    }

    /**
     * Tests maximumLink() setter method.
     */
    @Test
    public void testSetMaximumLink() throws Exception {
        ospfLinkTed.setMaximumLink(Bandwidth.bps(1234));
        assertThat(ospfLinkTed.maximumLink(), is(Bandwidth.bps(1234)));
    }

    /**
     * Tests ipv6RemRouterId() getter method.
     */
    @Test
    public void testIpv6RemRouterId() throws Exception {
        List list = new ArrayList();
        ospfLinkTed.setIpv6RemRouterId(list);
        assertThat(ospfLinkTed.ipv6RemRouterId().size(), is(0));
    }

    /**
     * Tests ipv6RemRouterId() setter method.
     */
    @Test
    public void testSetIpv6RemRouterId() throws Exception {
        List list = new ArrayList();
        ospfLinkTed.setIpv6RemRouterId(list);
        assertThat(ospfLinkTed.ipv6RemRouterId().size(), is(0));
    }

    /**
     * Tests ipv4RemRouterId() getter method.
     */
    @Test
    public void testIpv4RemRouterId() throws Exception {
        List list = new ArrayList();
        list.add(Ip4Address.valueOf(1));
        ospfLinkTed.setIpv4RemRouterId(list);
        assertThat(ospfLinkTed.ipv4RemRouterId().size(), is(1));
    }

    /**
     * Tests ipv4RemRouterId() setter method.
     */
    @Test
    public void testSetIpv4RemRouterId() throws Exception {
        List list = new ArrayList();
        list.add(Ip4Address.valueOf(1));
        ospfLinkTed.setIpv4RemRouterId(list);
        assertThat(ospfLinkTed.ipv4RemRouterId().size(), is(1));
    }

    /**
     * Tests ipv6LocRouterId() getter method.
     */
    @Test
    public void testIpv6LocRouterId() throws Exception {
        List list = new ArrayList();
        ospfLinkTed.setIpv4LocRouterId(list);
        assertThat(ospfLinkTed.ipv6LocRouterId().size(), is(0));
    }

    /**
     * Tests ipv6LocRouterId() setter method.
     */
    @Test
    public void testSetIpv6LocRouterId() throws Exception {
        List list = new ArrayList();
        ospfLinkTed.setIpv4LocRouterId(list);
        assertThat(ospfLinkTed.ipv6LocRouterId().size(), is(0));
    }

    /**
     * Tests ipv4LocRouterId() getter method.
     */
    @Test
    public void testIpv4LocRouterId() throws Exception {
        List list = new ArrayList();
        list.add(Ip4Address.valueOf(1));
        ospfLinkTed.setIpv4LocRouterId(list);
        assertThat(ospfLinkTed.ipv4LocRouterId().size(), is(1));
    }

    /**
     * Tests ipv4LocRouterId() setter method.
     */
    @Test
    public void testSetIpv4LocRouterId() throws Exception {
        List list = new ArrayList();
        list.add(Ip4Address.valueOf(1));
        ospfLinkTed.setIpv4LocRouterId(list);
        assertThat(ospfLinkTed.ipv4LocRouterId().size(), is(1));
    }

    /**
     * Tests teMetric() getter method.
     */
    @Test
    public void testTeMetric() throws Exception {
        ospfLinkTed.setTeMetric(1234);
        assertThat(ospfLinkTed.teMetric(), is(1234));
    }

    /**
     * Tests teMetric() setter method.
     */
    @Test
    public void testSetTeMetric() throws Exception {
        ospfLinkTed.setTeMetric(1234);
        assertThat(ospfLinkTed.teMetric(), is(1234));
    }

    /**
     * Tests maxReserved() getter method.
     */
    @Test
    public void testMaxReserved() throws Exception {
        ospfLinkTed.setMaxReserved(Bandwidth.bps(1234));
        assertThat(ospfLinkTed.maxReserved(), is(Bandwidth.bps(1234)));
    }

    /**
     * Tests maxReserved() setter method.
     */
    @Test
    public void testSetMaxReserved() throws Exception {
        ospfLinkTed.setMaxReserved(Bandwidth.bps(1234));
        assertThat(ospfLinkTed.maxReserved(), is(Bandwidth.bps(1234)));
    }

    /**
     * Tests maxUnResBandwidth() getter method.
     */
    @Test
    public void testMaxUnResBandwidth() throws Exception {
        ospfLinkTed.setMaxUnResBandwidth(Bandwidth.bps(1234));
        assertThat(ospfLinkTed.maxUnResBandwidth(), is(notNullValue()));
    }

    /**
     * Tests maxUnResBandwidth() setter method.
     */
    @Test
    public void testSetMaxUnResBandwidth() throws Exception {
        ospfLinkTed.setMaxUnResBandwidth(Bandwidth.bps(1234.0));
        assertThat(ospfLinkTed.maxUnResBandwidth(), is(notNullValue()));
    }
}
