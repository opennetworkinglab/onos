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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.Ip4Address;
import org.onosproject.ospf.controller.OspfNbr;
import org.onosproject.ospf.controller.TopologyForDeviceAndLink;
import org.onosproject.ospf.controller.impl.Controller;
import org.onosproject.ospf.controller.impl.OspfInterfaceChannelHandler;
import org.onosproject.ospf.controller.impl.OspfNbrImpl;
import org.onosproject.ospf.controller.impl.TopologyForDeviceAndLinkImpl;
import org.onosproject.ospf.protocol.lsa.OpaqueLsaHeader;
import org.onosproject.ospf.protocol.util.OspfInterfaceState;

import java.util.HashMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit test class for OspfInterfaceImpl.
 */
public class OspfInterfaceImplTest {

    private OspfInterfaceImpl ospfInterface;
    private OspfNbrImpl ospfNbr;
    private OpaqueLsaHeader opaqueLsaHeader;
    private int result;
    private HashMap<String, OspfNbr> ospfNbrHashMap;
    private TopologyForDeviceAndLink topologyForDeviceAndLink;

    @Before
    public void setUp() throws Exception {
        ospfInterface = new OspfInterfaceImpl();
        topologyForDeviceAndLink = new TopologyForDeviceAndLinkImpl();
    }

    @After
    public void tearDown() throws Exception {
        ospfInterface = null;
        ospfNbr = null;
        opaqueLsaHeader = null;
        ospfNbrHashMap = null;
    }

    /**
     * Tests state() getter method.
     */
    @Test
    public void testGetState() throws Exception {
        ospfInterface.setState(OspfInterfaceState.DROTHER);
        assertThat(ospfInterface.state(), is(OspfInterfaceState.DROTHER));
    }

    /**
     * Tests state() setter method.
     */
    @Test
    public void testSetState() throws Exception {
        ospfInterface.setState(OspfInterfaceState.DROTHER);
        assertThat(ospfInterface.state(), is(OspfInterfaceState.DROTHER));
    }

    /**
     * Tests linkStateHeaders() method.
     */
    @Test
    public void testGetLinkStateHeaders() throws Exception {

        assertThat(ospfInterface.linkStateHeaders().size(), is(0));
    }

    /**
     * Tests ipNetworkMask() getter method.
     */
    @Test
    public void testGetIpNetworkMask() throws Exception {
        ospfInterface.setIpNetworkMask(Ip4Address.valueOf("1.1.1.1"));
        assertThat(ospfInterface.ipNetworkMask(), is(Ip4Address.valueOf("1.1.1.1")));
    }

    /**
     * Tests ipNetworkMask() setter method.
     */
    @Test
    public void testSetIpNetworkMask() throws Exception {
        ospfInterface.setIpNetworkMask(Ip4Address.valueOf("1.1.1.1"));
        assertThat(ospfInterface.ipNetworkMask(), is(Ip4Address.valueOf("1.1.1.1")));
    }

    /**
     * Tests addNeighbouringRouter() method.
     */
    @Test
    public void testAddNeighbouringRouter() throws Exception {
        ospfNbr = new OspfNbrImpl(new OspfAreaImpl(), new OspfInterfaceImpl(),
                                  Ip4Address.valueOf("1.1.1.1"), Ip4Address.valueOf("2.2.2.2"), 2,
                                  new OspfInterfaceChannelHandler(new Controller(), new OspfAreaImpl(),
                                                                  new OspfInterfaceImpl()),
                                                                    topologyForDeviceAndLink);
        ospfNbr.setNeighborId(Ip4Address.valueOf("111.111.111.111"));
        ospfInterface.addNeighbouringRouter(ospfNbr);
        assertThat(ospfInterface, is(notNullValue()));

    }

    /**
     * Tests neighbouringRouter() method.
     */
    @Test
    public void testGetNeighbouringRouter() throws Exception {
        ospfNbr = new OspfNbrImpl(new OspfAreaImpl(), new OspfInterfaceImpl(),
                                  Ip4Address.valueOf("1.1.1.1"), Ip4Address.valueOf("2.2.2.2"), 2,
                                  new OspfInterfaceChannelHandler(new Controller(), new OspfAreaImpl(),
                                                                  new OspfInterfaceImpl()),
                                                                    topologyForDeviceAndLink);
        ospfNbr.setNeighborId(Ip4Address.valueOf("111.111.111.111"));
        ospfInterface.addNeighbouringRouter(ospfNbr);
        assertThat(ospfInterface.neighbouringRouter("111.111.111.111"), is(notNullValue()));
    }

    /**
     * Tests addLsaHeaderForDelayAck() method.
     */
    @Test
    public void testAddLsaHeaderForDelayAck() throws Exception {
        opaqueLsaHeader = new OpaqueLsaHeader();
        opaqueLsaHeader.setLsType(10);
        ospfInterface.addLsaHeaderForDelayAck(opaqueLsaHeader);
        assertThat(ospfInterface, is(notNullValue()));
    }

    /**
     * Tests removeLsaFromNeighborMap() method.
     */
    @Test
    public void testRemoveLsaFromNeighborMap() throws Exception {
        ospfInterface.removeLsaFromNeighborMap("lsa10");
        assertThat(ospfInterface, is(notNullValue()));
    }

    /**
     * Tests isNeighborInList() method.
     */
    @Test
    public void testIsNeighborinList() throws Exception {
        ospfNbr = new OspfNbrImpl(new OspfAreaImpl(), new OspfInterfaceImpl(),
                                  Ip4Address.valueOf("1.1.1.1"), Ip4Address.valueOf("2.2.2.2"), 2,
                                  new OspfInterfaceChannelHandler(new Controller(), new OspfAreaImpl(),
                                                                  new OspfInterfaceImpl()),
                                                                    topologyForDeviceAndLink);
        ospfNbr.setNeighborId(Ip4Address.valueOf("111.111.111.111"));
        ospfInterface.addNeighbouringRouter(ospfNbr);
        assertThat(ospfInterface.isNeighborInList("111.111.111.111"), is(notNullValue()));
    }

    /**
     * Tests listOfNeighbors() getter method.
     */
    @Test
    public void testGetListOfNeighbors() throws Exception {
        ospfNbrHashMap = new HashMap();
        ospfNbr = new OspfNbrImpl(new OspfAreaImpl(), new OspfInterfaceImpl(),
                                  Ip4Address.valueOf("1.1.1.1"), Ip4Address.valueOf("2.2.2.2"), 2,
                                  new OspfInterfaceChannelHandler(new Controller(),
                                                                  new OspfAreaImpl(),
                                                                  new OspfInterfaceImpl()),
                                                                    topologyForDeviceAndLink);
        ospfNbr.setNeighborId(Ip4Address.valueOf("111.111.111.111"));
        ospfNbrHashMap.put("111.111.111.111", ospfNbr);
        ospfInterface.setListOfNeighbors(ospfNbrHashMap);
        assertThat(ospfInterface.listOfNeighbors().size(), is(1));
    }

    /**
     * Tests listOfNeighbors() setter method.
     */
    @Test
    public void testSetListOfNeighbors() throws Exception {
        ospfNbrHashMap = new HashMap();
        ospfNbr = new OspfNbrImpl(new OspfAreaImpl(), new OspfInterfaceImpl(),
                                  Ip4Address.valueOf("1.1.1.1"), Ip4Address.valueOf("2.2.2.2"), 2,
                                  new OspfInterfaceChannelHandler(new Controller(), new OspfAreaImpl(),
                                                                  new OspfInterfaceImpl()),
                                                                    topologyForDeviceAndLink);
        ospfNbr.setNeighborId(Ip4Address.valueOf("111.111.111.111"));
        ospfNbrHashMap.put("111.111.111.111", ospfNbr);
        ospfInterface.setListOfNeighbors(ospfNbrHashMap);
        assertThat(ospfInterface.listOfNeighbors().size(), is(1));
    }

    /**
     * Tests ipAddress() getter method.
     */
    @Test
    public void testGetIpAddress() throws Exception {
        ospfInterface.setIpAddress(Ip4Address.valueOf("1.1.1.1"));
        assertThat(ospfInterface.ipAddress(), is(Ip4Address.valueOf("1.1.1.1")));
    }

    /**
     * Tests ipAddress() getter method.
     */
    @Test
    public void testSetIpAddress() throws Exception {
        ospfInterface.setIpAddress(Ip4Address.valueOf("1.1.1.1"));
        assertThat(ospfInterface.ipAddress(), is(Ip4Address.valueOf("1.1.1.1")));
    }

    /**
     * Tests routerPriority() getter method.
     */
    @Test
    public void testGetRouterPriority() throws Exception {
        ospfInterface.setRouterPriority(1);
        Assert.assertEquals(1, ospfInterface.routerPriority());
    }

    /**
     * Tests routerPriority() setter method.
     */
    @Test
    public void testSetRouterPriority() throws Exception {
        ospfInterface.setRouterPriority(1);
        assertThat(ospfInterface.routerPriority(), is(1));
    }

    /**
     * Tests areaId() getter method.
     */
    @Test
    public void testGetAreaId() throws Exception {
        ospfInterface.setAreaId(1);
        assertThat(ospfInterface.areaId(), is(1));
    }

    /**
     * Tests areaId() setter method.
     */
    @Test
    public void testSetAreaId() throws Exception {
        ospfInterface.setAreaId(1);
        assertThat(ospfInterface.areaId(), is(1));
    }

    /**
     * Tests helloIntervalTime() getter method.
     */
    @Test
    public void testGetHelloIntervalTime() throws Exception {
        ospfInterface.setHelloIntervalTime(10);
        assertThat(ospfInterface.helloIntervalTime(), is(10));
    }

    /**
     * Tests helloIntervalTime() setter method.
     */
    @Test
    public void testSetHelloIntervalTime() throws Exception {
        ospfInterface.setHelloIntervalTime(10);
        assertThat(ospfInterface.helloIntervalTime(), is(10));
    }

    /**
     * Tests routerDeadIntervalTime() getter method.
     */
    @Test
    public void testGetRouterDeadIntervalTime() throws Exception {
        ospfInterface.setRouterDeadIntervalTime(10);
        assertThat(ospfInterface.routerDeadIntervalTime(), is(10));
    }

    /**
     * Tests routerDeadIntervalTime() setter method.
     */
    @Test
    public void testSetRouterDeadIntervalTime() throws Exception {
        ospfInterface.setRouterDeadIntervalTime(10);
        assertThat(ospfInterface.routerDeadIntervalTime(), is(10));
    }

    /**
     * Tests interfaceType() getter method.
     */
    @Test
    public void testGetInterfaceType() throws Exception {
        ospfInterface.setInterfaceType(1);
        assertThat(ospfInterface.interfaceType(), is(1));
    }

    /**
     * Tests interfaceType() setter method.
     */
    @Test
    public void testSetInterfaceType() throws Exception {
        ospfInterface.setInterfaceType(1);
        assertThat(ospfInterface.interfaceType(), is(1));
    }

    /**
     * Tests interfaceCost() getter method.
     */
    @Test
    public void testGetInterfaceCost() throws Exception {
        ospfInterface.setInterfaceCost(100);
        assertThat(ospfInterface.interfaceCost(), is(100));
    }

    /**
     * Tests interfaceCost() setter method.
     */
    @Test
    public void testSetInterfaceCost() throws Exception {
        ospfInterface.setInterfaceCost(100);
        assertThat(ospfInterface.interfaceCost(), is(100));
    }

    /**
     * Tests authType() getter method.
     */
    @Test
    public void testGetAuthType() throws Exception {
        ospfInterface.setAuthType("00");
        assertThat(ospfInterface.authType(), is("00"));
    }

    /**
     * Tests authType() setter method.
     */
    @Test
    public void testSetAuthType() throws Exception {
        ospfInterface.setAuthType("00");
        assertThat(ospfInterface.authType(), is("00"));
    }

    /**
     * Tests authKey() getter method.
     */
    @Test
    public void testGetAuthKey() throws Exception {
        ospfInterface.setAuthKey("00");
        assertThat(ospfInterface.authKey(), is("00"));
    }

    /**
     * Tests authKey() setter method.
     */
    @Test
    public void testSetAuthKey() throws Exception {
        ospfInterface.setAuthKey("00");
        assertThat(ospfInterface.authKey(), is("00"));
    }

    /**
     * Tests pollInterval() getter method.
     */
    @Test
    public void testGetPollInterval() throws Exception {
        ospfInterface.setPollInterval(100);
        assertThat(ospfInterface.pollInterval(), is(100));
    }

    /**
     * Tests pollInterval() setter method.
     */
    @Test
    public void testSetPollInterval() throws Exception {
        ospfInterface.setPollInterval(100);
        assertThat(ospfInterface.pollInterval(), is(100));
    }

    /**
     * Tests mtu() getter method.
     */
    @Test
    public void testGetMtu() throws Exception {
        ospfInterface.setMtu(100);
        assertThat(ospfInterface.mtu(), is(100));
    }

    /**
     * Tests mtu() setter method.
     */
    @Test
    public void testSetMtu() throws Exception {
        ospfInterface.setMtu(100);
        assertThat(ospfInterface.mtu(), is(100));
    }

    /**
     * Tests reTransmitInterval() getter method.
     */
    @Test
    public void testGetReTransmitInterval() throws Exception {
        ospfInterface.setReTransmitInterval(100);
        assertThat(ospfInterface.reTransmitInterval(), is(100));
    }

    /**
     * Tests reTransmitInterval() setter method.
     */
    @Test
    public void testSetReTransmitInterval() throws Exception {
        ospfInterface.setReTransmitInterval(100);
        assertThat(ospfInterface.reTransmitInterval(), is(100));
    }

    /**
     * Tests dr() getter method.
     */
    @Test
    public void testGetDr() throws Exception {
        ospfInterface.setDr(Ip4Address.valueOf("1.1.1.1"));
        assertThat(ospfInterface.dr(), is(Ip4Address.valueOf("1.1.1.1")));
    }

    /**
     * Tests dr() setter method.
     */
    @Test
    public void testSetDr() throws Exception {
        ospfInterface.setDr(Ip4Address.valueOf("1.1.1.1"));
        assertThat(ospfInterface.dr(), is(Ip4Address.valueOf("1.1.1.1")));
    }

    /**
     * Tests bdr() getter method.
     */
    @Test
    public void testGetBdr() throws Exception {
        ospfInterface.setBdr(Ip4Address.valueOf("1.1.1.1"));
        assertThat(ospfInterface.bdr(), is(Ip4Address.valueOf("1.1.1.1")));
    }

    /**
     * Tests bdr() setter method.
     */
    @Test
    public void testSetBdr() throws Exception {
        ospfInterface.setBdr(Ip4Address.valueOf("1.1.1.1"));
        assertThat(ospfInterface.bdr(), is(Ip4Address.valueOf("1.1.1.1")));
    }

    /**
     * Tests transmitDelay() getter method.
     */
    @Test
    public void testGetTransmitDelay() throws Exception {
        ospfInterface.setTransmitDelay(100);
        assertThat(ospfInterface.transmitDelay(), is(100));
    }

    /**
     * Tests transmitDelay() setter method.
     */
    @Test
    public void testSetTransmitDelay() throws Exception {
        ospfInterface.setTransmitDelay(100);
        assertThat(ospfInterface.transmitDelay(), is(100));
    }

    /**
     * Tests equals() method.
     */
    @Test
    public void testEquals() throws Exception {
        assertThat(ospfInterface.equals(new OspfInterfaceImpl()), is(true));
    }

    /**
     * Tests hashCode() method.
     */
    @Test
    public void testHashCode() throws Exception {
        result = ospfInterface.hashCode();
        assertThat(result, is(notNullValue()));
    }

    /**
     * Tests to string method.
     */
    @Test
    public void testToString() throws Exception {
        assertThat(ospfInterface.toString(), is(notNullValue()));
    }
}