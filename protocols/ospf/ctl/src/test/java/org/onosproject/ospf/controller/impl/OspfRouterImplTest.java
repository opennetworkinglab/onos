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
import org.onlab.packet.IpAddress;
import org.onosproject.ospf.controller.OspfDeviceTed;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit test class for OspfRouterImpl.
 */
public class OspfRouterImplTest {
    private OspfRouterImpl ospfRouter;
    private OspfDeviceTed ospfDeviceTed;
    private List<OspfDeviceTed> list;

    @Before
    public void setUp() throws Exception {
        ospfRouter = new OspfRouterImpl();
    }

    @After
    public void tearDown() throws Exception {
        ospfRouter = null;
    }

    /**
     * Tests routerIp() getter method.
     */
    @Test
    public void testRouterIp() throws Exception {
        ospfRouter.setRouterIp(Ip4Address.valueOf("1.1.1.1"));
        assertThat(ospfRouter.routerIp(), is(IpAddress.valueOf("1.1.1.1")));
    }

    /**
     * Tests routerIp() setter method.
     */
    @Test
    public void testSetRouterIp() throws Exception {
        ospfRouter.setRouterIp(Ip4Address.valueOf("1.1.1.1"));
        assertThat(ospfRouter.routerIp(), is(IpAddress.valueOf("1.1.1.1")));
    }

    /**
     * Tests areaIdOfInterface() getter method.
     */
    @Test
    public void testAreaIdOfInterface() throws Exception {
        ospfRouter.setAreaIdOfInterface(Ip4Address.valueOf("1.1.1.1"));
        assertThat(ospfRouter.areaIdOfInterface(), is(IpAddress.valueOf("1.1.1.1")));
    }

    /**
     * Tests areaIdOfInterface() setter method.
     */
    @Test
    public void testSetAreaIdOfInterface() throws Exception {
        ospfRouter.setAreaIdOfInterface(Ip4Address.valueOf("1.1.1.1"));
        assertThat(ospfRouter.areaIdOfInterface(), is(IpAddress.valueOf("1.1.1.1")));
    }

    /**
     * Tests interfaceId() getter method.
     */
    @Test
    public void testInterfaceId() throws Exception {
        ospfRouter.setInterfaceId(Ip4Address.valueOf("1.1.1.1"));
        assertThat(ospfRouter.interfaceId(), is(IpAddress.valueOf("1.1.1.1")));
    }

    /**
     * Tests interfaceId() setter method.
     */
    @Test
    public void testSetInterfaceId() throws Exception {
        ospfRouter.setInterfaceId(Ip4Address.valueOf("1.1.1.1"));
        assertThat(ospfRouter.interfaceId(), is(IpAddress.valueOf("1.1.1.1")));
    }

    /**
     * Tests isDr() setter method.
     */
    @Test
    public void testSetDr() throws Exception {
        ospfRouter.setDr(true);
        assertThat(ospfRouter.isDr(), is(true));
    }

    /**
     * Tests isDr() getter method.
     */
    @Test
    public void testIsDr() throws Exception {
        ospfRouter.setDr(true);
        assertThat(ospfRouter.isDr(), is(true));
    }

    /**
     * Tests isOpaque() setter method.
     */
    @Test
    public void testSetOpaque() throws Exception {
        ospfRouter.setOpaque(true);
        assertThat(ospfRouter.isOpaque(), is(true));
    }

    /**
     * Tests isOpaque() getter method.
     */
    @Test
    public void testisOpaque() throws Exception {
        ospfRouter.setOpaque(true);
        assertThat(ospfRouter.isOpaque(), is(true));
    }

    /**
     * Tests deviceTed() getter method.
     */
    @Test
    public void testDeviceTed() throws Exception {
        ospfRouter.setDeviceTed(new OspfDeviceTedImpl());
        assertThat(ospfRouter.deviceTed(), is(notNullValue()));
    }

    /**
     * Tests deviceTed() Setter method.
     */
    @Test
    public void testSetDeviceTed() throws Exception {
        ospfRouter.setDeviceTed(new OspfDeviceTedImpl());
        assertThat(ospfRouter.deviceTed(), is(notNullValue()));
    }

    /**
     * Tests neighborRouterId() getter method.
     */
    @Test
    public void testNeighborRouterId() throws Exception {
        ospfRouter.setNeighborRouterId(Ip4Address.valueOf("1.1.1.1"));
        assertThat(ospfRouter.neighborRouterId(), is(Ip4Address.valueOf("1.1.1.1")));
    }

    /**
     * Tests neighborRouterId() Setter method.
     */
    @Test
    public void testSetNeighborRouterId() throws Exception {
        ospfRouter.setNeighborRouterId(Ip4Address.valueOf("1.1.1.1"));
        assertThat(ospfRouter.neighborRouterId(), is(Ip4Address.valueOf("1.1.1.1")));
    }
}