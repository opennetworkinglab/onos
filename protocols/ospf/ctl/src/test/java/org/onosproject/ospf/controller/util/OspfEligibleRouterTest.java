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
package org.onosproject.ospf.controller.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.Ip4Address;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit test class for OspfEligibleRouter.
 */
public class OspfEligibleRouterTest {

    private OspfEligibleRouter ospfEligibleRouter;

    @Before
    public void setUp() throws Exception {
        ospfEligibleRouter = new OspfEligibleRouter();
    }

    @After
    public void tearDown() throws Exception {
        ospfEligibleRouter = null;
    }

    /**
     * Tests getIpAddress() getter method.
     */
    @Test
    public void testGetIpAddress() throws Exception {
        ospfEligibleRouter.setIpAddress(Ip4Address.valueOf("1.1.1.1"));
        assertThat(ospfEligibleRouter.getIpAddress(), is(Ip4Address.valueOf("1.1.1.1")));
    }

    /**
     * Tests setIpAddress() setter method.
     */
    @Test
    public void testSetIpAddress() throws Exception {
        ospfEligibleRouter.setIpAddress(Ip4Address.valueOf("1.1.1.1"));
        assertThat(ospfEligibleRouter.getIpAddress(), is(Ip4Address.valueOf("1.1.1.1")));
    }

    /**
     * Tests getRouterId() getter method.
     */
    @Test
    public void testGetRouterId() throws Exception {
        ospfEligibleRouter.setRouterId(Ip4Address.valueOf("1.1.1.1"));
        assertThat(ospfEligibleRouter.getRouterId(), is(Ip4Address.valueOf("1.1.1.1")));
    }

    /**
     * Tests setRouterId() setter method.
     */
    @Test
    public void testSetRouterId() throws Exception {
        ospfEligibleRouter.setRouterId(Ip4Address.valueOf("1.1.1.1"));
        assertThat(ospfEligibleRouter.getRouterId(), is(Ip4Address.valueOf("1.1.1.1")));
    }

    /**
     * Tests getRouterPriority() getter method.
     */
    @Test
    public void testGetRouterPriority() throws Exception {
        ospfEligibleRouter.setRouterPriority(1);
        assertThat(ospfEligibleRouter.getRouterPriority(), is(1));
    }

    /**
     * Tests getRouterPriority() setter method.
     */
    @Test
    public void testSetRouterPriority() throws Exception {
        ospfEligibleRouter.setRouterPriority(1);
        assertThat(ospfEligibleRouter.getRouterPriority(), is(1));
    }

    /**
     * Tests isDr() getter method.
     */
    @Test
    public void testIsDr() throws Exception {
        ospfEligibleRouter.setIsDr(true);
        assertThat(ospfEligibleRouter.isDr(), is(true));
    }

    /**
     * Tests isDr() setter method.
     */
    @Test
    public void testSetIsDr() throws Exception {
        ospfEligibleRouter.setIsDr(true);
        assertThat(ospfEligibleRouter.isDr(), is(true));
    }

    /**
     * Tests isBdr() getter method.
     */
    @Test
    public void testIsBdr() throws Exception {
        ospfEligibleRouter.setIsBdr(true);
        assertThat(ospfEligibleRouter.isBdr(), is(true));
    }

    /**
     * Tests isBdr() setter method.
     */
    @Test
    public void testSetIsBdr() throws Exception {
        ospfEligibleRouter.setIsBdr(true);
        assertThat(ospfEligibleRouter.isBdr(), is(true));
    }
}
