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

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.Ip4Address;
import org.onosproject.ospf.controller.OspfArea;
import org.onosproject.ospf.controller.OspfAreaAddressRange;
import org.onosproject.ospf.controller.OspfInterface;
import org.onosproject.ospf.controller.OspfNbr;
import org.onosproject.ospf.controller.OspfNeighborState;
import org.onosproject.ospf.controller.TopologyForDeviceAndLink;
import org.onosproject.ospf.controller.impl.Controller;
import org.onosproject.ospf.controller.impl.OspfInterfaceChannelHandler;
import org.onosproject.ospf.controller.impl.OspfNbrImpl;
import org.onosproject.ospf.controller.impl.TopologyForDeviceAndLinkImpl;
import org.onosproject.ospf.controller.lsdb.LsaWrapperImpl;
import org.onosproject.ospf.protocol.lsa.LsaHeader;
import org.onosproject.ospf.protocol.lsa.types.NetworkLsa;
import org.onosproject.ospf.protocol.lsa.types.RouterLsa;
import org.onosproject.ospf.protocol.util.OspfInterfaceState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Unit test class for OspfAreaImpl.
 */
public class OspfAreaImplTest {

    private OspfAreaImpl ospfArea;
    private int result;
    private OspfInterfaceImpl ospfInterface;
    private HashMap<String, OspfNbr> ospfNbrList;
    private List<OspfInterface> ospfInterfaces;
    private OspfInterfaceImpl ospfInterface1;
    private OspfInterfaceImpl ospfInterface2;
    private OspfInterfaceImpl ospfInterface3;
    private OspfInterfaceImpl ospfInterface4;
    private OspfInterfaceImpl ospfInterface5;
    private OspfInterfaceImpl ospfInterface6;
    private NetworkLsa networkLsa;
    private OspfNbrImpl ospfNbr;
    private RouterLsa routerLsa;
    private List<OspfAreaAddressRange> ospfAreaAddressRanges;
    private LsaHeader lsaHeader;
    private TopologyForDeviceAndLink topologyForDeviceAndLink;

    @Before
    public void setUp() throws Exception {
        ospfArea = new OspfAreaImpl();
        topologyForDeviceAndLink = new TopologyForDeviceAndLinkImpl();
    }

    @After
    public void tearDown() throws Exception {
        ospfArea = null;
        ospfInterface = null;
        ospfNbrList = null;
        ospfInterfaces = null;
        ospfAreaAddressRanges = null;
        lsaHeader = null;
        routerLsa = null;
        ospfNbr = null;
        networkLsa = null;
        ospfInterface1 = null;
        ospfInterface2 = null;
        ospfInterface3 = null;
        ospfInterface4 = null;
        ospfInterface5 = null;
        ospfInterface6 = null;

    }

    /**
     * Tests equals() method.
     */
    @Test
    public void testEquals() throws Exception {
        ospfArea = new OspfAreaImpl();
        ospfInterface = new OspfInterfaceImpl();
        ospfArea.setTransitCapability(true);
        ospfArea.setExternalRoutingCapability(true);
        ospfArea.setStubCost(100);
        ospfArea.initializeDb();
        ospfArea.setAddressRanges(ospfAreaAddressRanges);
        assertThat(ospfArea.equals(ospfArea), is(true));
        ospfArea = EasyMock.createMock(OspfAreaImpl.class);
        assertThat(ospfArea.equals(ospfArea), is(true));
        OspfArea ospfArea = new OspfAreaImpl();
        assertThat(ospfArea.equals(ospfArea), is(true));
    }

    /**
     * Tests hashCode() method.
     */
    @Test
    public void testHashCode() throws Exception {
        result = ospfArea.hashCode();
        assertThat(result, is(notNullValue()));
    }

    /**
     * Tests routerId() getter method.
     */
    @Test
    public void testGetRouterId() throws Exception {
        ospfArea.setRouterId(Ip4Address.valueOf("1.1.1.1"));
        assertThat(ospfArea.routerId(), is(Ip4Address.valueOf("1.1.1.1")));
    }

    /**
     * Tests routerId() setter method.
     */
    @Test
    public void testSetRouterId() throws Exception {
        ospfArea.setRouterId(Ip4Address.valueOf("1.1.1.1"));
        assertThat(ospfArea.routerId(), is(Ip4Address.valueOf("1.1.1.1")));
    }

    /**
     * Tests isOpaqueEnabled() getter method.
     */
    @Test
    public void testSetisOpaqueEnabled() throws Exception {
        ospfArea.setIsOpaqueEnabled(true);
        assertThat(ospfArea.isOpaqueEnabled(), is(true));
    }

    /**
     * Tests isOpaqueEnabled() setter method.
     */
    @Test
    public void testIsOpaqueEnabled() throws Exception {
        ospfArea.setIsOpaqueEnabled(true);
        assertThat(ospfArea.isOpaqueEnabled(), is(true));
    }

    /**
     * Tests initializeDb() method.
     */
    @Test
    public void testInitializeDb() throws Exception {
        ospfArea.initializeDb();
        assertThat(ospfArea, is(notNullValue()));
    }

    /**
     * Tests refreshArea() method.
     */
    @Test
    public void testRefreshArea() throws Exception {

        ospfInterface = new OspfInterfaceImpl();
        ospfInterface.setState(OspfInterfaceState.DR);
        ospfNbrList = new HashMap();
        ospfNbrList.put("1.1.1.1", new OspfNbrImpl(new OspfAreaImpl(), new OspfInterfaceImpl(),
                                                   Ip4Address.valueOf("1.1.1.1"),
                                                   Ip4Address.valueOf("2.2.2.2"), 2,
                                                   new OspfInterfaceChannelHandler(new Controller(),
                                                                                   new OspfAreaImpl(),
                                                                                   new OspfInterfaceImpl()),
                                                   topologyForDeviceAndLink));
        ospfNbrList.put("2.2.2.2", new OspfNbrImpl(new OspfAreaImpl(), new OspfInterfaceImpl(),
                                                   Ip4Address.valueOf("1.1.1.1"),
                                                   Ip4Address.valueOf("2.2.2.2"), 2,
                                                   new OspfInterfaceChannelHandler(new Controller(),
                                                                                   new OspfAreaImpl(),
                                                                                   new OspfInterfaceImpl()),
                                                   topologyForDeviceAndLink));
        ospfNbrList.put("3.3.3.3", new OspfNbrImpl(new OspfAreaImpl(), new OspfInterfaceImpl(),
                                                   Ip4Address.valueOf("1.1.1.1"),
                                                   Ip4Address.valueOf("2.2.2.2"), 2,
                                                   new OspfInterfaceChannelHandler(new Controller(),
                                                                                   new OspfAreaImpl(),
                                                                                   new OspfInterfaceImpl()),
                                                   topologyForDeviceAndLink));
        ospfNbrList.put("4.4.4.4", new OspfNbrImpl(new OspfAreaImpl(), new OspfInterfaceImpl(),
                                                   Ip4Address.valueOf("1.1.1.1"),
                                                   Ip4Address.valueOf("2.2.2.2"), 2,
                                                   new OspfInterfaceChannelHandler(new Controller(),
                                                                                   new OspfAreaImpl(),
                                                                                   new OspfInterfaceImpl()),
                                                   topologyForDeviceAndLink));

        ospfInterface.setListOfNeighbors(ospfNbrList);
        ospfInterface.setIpAddress(Ip4Address.valueOf("10.10.10.10"));
        ospfInterface.setIpNetworkMask(Ip4Address.valueOf("255.255.255.255"));
        ospfInterfaces = new ArrayList();
        ospfInterface1 = new OspfInterfaceImpl();
        ospfInterface1.setIpAddress(Ip4Address.valueOf("1.1.1.1"));
        ospfInterfaces.add(ospfInterface1);
        ospfInterface2 = new OspfInterfaceImpl();
        ospfInterface2.setIpAddress(Ip4Address.valueOf("2.2.2.2"));
        ospfInterfaces.add(ospfInterface2);
        ospfInterface3 = new OspfInterfaceImpl();
        ospfInterface3.setIpAddress(Ip4Address.valueOf("3.3.3.3"));
        ospfInterfaces.add(ospfInterface3);
        ospfArea.setInterfacesLst(ospfInterfaces);
        ospfArea.setRouterId(Ip4Address.valueOf("111.111.111.111"));
        networkLsa = ospfArea.buildNetworkLsa(Ip4Address.valueOf("1.1.1.1"),
                                              Ip4Address.valueOf("255.255.255.255"));
        ospfArea.refreshArea(ospfInterface);
        assertThat(ospfNbrList.size(), is(4));
        assertThat(networkLsa, is(notNullValue()));
        assertThat(ospfArea, is(notNullValue()));
    }

    /**
     * Tests buildNetworkLsa() method.
     */
    @Test
    public void testBuildNetworkLsa() throws Exception {
        ospfInterfaces = new ArrayList();
        ospfInterface1 = new OspfInterfaceImpl();
        ospfInterface1.setIpAddress(Ip4Address.valueOf("1.1.1.1"));
        ospfInterfaces.add(ospfInterface1);
        ospfInterface2 = new OspfInterfaceImpl();
        ospfInterface2.setIpAddress(Ip4Address.valueOf("2.2.2.2"));
        ospfInterfaces.add(ospfInterface2);
        ospfInterface3 = new OspfInterfaceImpl();
        ospfInterface3.setIpAddress(Ip4Address.valueOf("3.3.3.3"));
        ospfInterfaces.add(ospfInterface3);
        ospfArea.setInterfacesLst(ospfInterfaces);
        ospfArea.setRouterId(Ip4Address.valueOf("111.111.111.111"));
        networkLsa = ospfArea.buildNetworkLsa(Ip4Address.valueOf("1.1.1.1"),
                                              Ip4Address.valueOf("255.255.255.255"));
        assertThat(ospfInterfaces.size(), is(3));
        assertThat(networkLsa, is(notNullValue()));
        assertThat(ospfArea, is(notNullValue()));
    }

    /**
     * Tests buildNetworkLsa() method.
     */
    @Test
    public void testBuildNetworkLsa1() throws Exception {
        ospfInterfaces = new ArrayList();
        ospfInterface1 = new OspfInterfaceImpl();
        ospfInterface1.setIpAddress(Ip4Address.valueOf("1.1.1.1"));
        ospfInterface1.setIpNetworkMask(Ip4Address.valueOf("255.255.255.255"));
        ospfInterface1.setState(OspfInterfaceState.POINT2POINT);
        ospfNbr = new OspfNbrImpl(new OspfAreaImpl(), new OspfInterfaceImpl(),
                                  Ip4Address.valueOf("1.1.1.1"),
                                  Ip4Address.valueOf("2.2.2.2"), 2,
                                  new OspfInterfaceChannelHandler(new Controller(),
                                                                  new OspfAreaImpl(),
                                                                  new OspfInterfaceImpl()),
                                  topologyForDeviceAndLink);
        ospfNbr.setState(OspfNeighborState.FULL);
        ospfInterface1.addNeighbouringRouter(ospfNbr);
        ospfInterfaces.add(ospfInterface1);
        ospfArea.setInterfacesLst(ospfInterfaces);
        ospfArea.setRouterId(Ip4Address.valueOf("111.111.111.111"));
        networkLsa = ospfArea.buildNetworkLsa(Ip4Address.valueOf("1.1.1.1"),
                                              Ip4Address.valueOf("255.255.255.255"));
        assertThat(ospfInterfaces.size(), is(1));
        assertThat(networkLsa, is(notNullValue()));
        assertThat(ospfArea, is(notNullValue()));
    }

    /**
     * Tests buildRouterLsa() method.
     */
    @Test
    public void testBuildRouterLsa() throws Exception {
        ospfNbrList = new HashMap();
        ospfNbr = new OspfNbrImpl(new OspfAreaImpl(), new OspfInterfaceImpl(),
                                  Ip4Address.valueOf("1.1.1.1"),
                                  Ip4Address.valueOf("2.2.2.2"), 2,
                                  new OspfInterfaceChannelHandler(new Controller(),
                                                                  new OspfAreaImpl(),
                                                                  new OspfInterfaceImpl()),
                                  topologyForDeviceAndLink);
        ospfNbr.setState(OspfNeighborState.FULL);
        ospfInterfaces = new ArrayList();
        ospfInterface1 = new OspfInterfaceImpl();
        ospfInterface1.setIpAddress(Ip4Address.valueOf("1.1.1.1"));
        ospfInterface1.setState(OspfInterfaceState.DOWN);
        ospfInterface1.addNeighbouringRouter(ospfNbr);
        ospfInterfaces.add(ospfInterface1);
        ospfInterface2 = new OspfInterfaceImpl();
        ospfInterface2.setState(OspfInterfaceState.LOOPBACK);
        ospfInterface2.setIpAddress(Ip4Address.valueOf("2.2.2.2"));
        ospfInterface2.addNeighbouringRouter(ospfNbr);
        ospfInterfaces.add(ospfInterface2);
        ospfInterface3 = new OspfInterfaceImpl();
        ospfInterface3.setIpAddress(Ip4Address.valueOf("3.3.3.3"));
        ospfInterface3.setState(OspfInterfaceState.POINT2POINT);
        ospfInterface3.setIpNetworkMask(Ip4Address.valueOf("255.255.255.255"));
        ospfInterface3.addNeighbouringRouter(ospfNbr);
        ospfInterface3.setListOfNeighbors(ospfNbrList);
        ospfInterfaces.add(ospfInterface3);
        ospfInterface4 = new OspfInterfaceImpl();
        ospfInterface4.setState(OspfInterfaceState.WAITING);
        ospfInterface4.setIpAddress(Ip4Address.valueOf("3.3.3.3"));
        ospfInterface4.setIpNetworkMask(Ip4Address.valueOf("255.255.255.255"));
        ospfInterfaces.add(ospfInterface4);
        ospfInterface5 = new OspfInterfaceImpl();
        ospfInterface5.setState(OspfInterfaceState.DR);
        ospfInterface5.setIpAddress(Ip4Address.valueOf("3.3.3.3"));
        ospfInterfaces.add(ospfInterface5);
        ospfInterface6 = new OspfInterfaceImpl();
        ospfInterface6.setState(OspfInterfaceState.BDR);
        ospfInterface6.setIpAddress(Ip4Address.valueOf("3.3.3.3"));
        ospfInterface6.setDr(Ip4Address.valueOf("3.3.3.3"));
        ospfInterfaces.add(ospfInterface6);
        ospfArea.setInterfacesLst(ospfInterfaces);
        ospfArea.setRouterId(Ip4Address.valueOf("111.111.111.111"));
        assertThat(ospfInterfaces.size(), is(6));
        routerLsa = ospfArea.buildRouterLsa(ospfInterface1);
        assertThat(routerLsa, is(notNullValue()));
        routerLsa = ospfArea.buildRouterLsa(ospfInterface2);
        assertThat(routerLsa, is(notNullValue()));
        routerLsa = ospfArea.buildRouterLsa(ospfInterface3);
        assertThat(routerLsa, is(notNullValue()));
        assertThat(ospfArea, is(notNullValue()));
    }

    /**
     * Tests buildRouterLsa() method.
     */
    @Test
    public void testBuildRouterLsa1() throws Exception {
        ospfInterfaces = new ArrayList();
        ospfInterface1 = new OspfInterfaceImpl();
        ospfInterface1.setIpAddress(Ip4Address.valueOf("1.1.1.1"));
        ospfInterface1.setIpNetworkMask(Ip4Address.valueOf("255.255.255.255"));
        ospfInterface1.setState(OspfInterfaceState.POINT2POINT);
        ospfNbr = new OspfNbrImpl(new OspfAreaImpl(), new OspfInterfaceImpl(),
                                  Ip4Address.valueOf("1.1.1.1"),
                                  Ip4Address.valueOf("2.2.2.2"), 2,
                                  new OspfInterfaceChannelHandler(new Controller(),
                                                                  new OspfAreaImpl(),
                                                                  new OspfInterfaceImpl()),
                                  topologyForDeviceAndLink);
        ospfNbr.setState(OspfNeighborState.FULL);
        ospfInterface1.addNeighbouringRouter(ospfNbr);
        ospfInterfaces.add(ospfInterface1);
        ospfArea.setInterfacesLst(ospfInterfaces);
        ospfArea.setRouterId(Ip4Address.valueOf("111.111.111.111"));
        routerLsa = ospfArea.buildRouterLsa(ospfInterface1);
        assertThat(routerLsa, is(notNullValue()));
    }

    /**
     * Tests areaId() getter method.
     */
    @Test
    public void testGetAreaId() throws Exception {
        ospfArea.setAreaId(Ip4Address.valueOf("1.1.1.1"));
        assertThat(ospfArea.areaId(), is(Ip4Address.valueOf("1.1.1.1")));
    }

    /**
     * Tests areaId() setter method.
     */
    @Test
    public void testSetAreaId() throws Exception {
        ospfArea.setAreaId(Ip4Address.valueOf("1.1.1.1"));
        assertThat(ospfArea.areaId(), is(Ip4Address.valueOf("1.1.1.1")));
    }

    /**
     * Tests addressRanges() getter method.
     */
    @Test
    public void testGetAddressRanges() throws Exception {
        ospfAreaAddressRanges = new ArrayList();
        ospfAreaAddressRanges.add(new OspfAreaAddressRangeImpl());
        ospfAreaAddressRanges.add(new OspfAreaAddressRangeImpl());
        ospfAreaAddressRanges.add(new OspfAreaAddressRangeImpl());
        ospfAreaAddressRanges.add(new OspfAreaAddressRangeImpl());
        ospfArea.setAddressRanges(ospfAreaAddressRanges);
        assertThat(ospfArea.addressRanges().size(), is(4));
    }

    /**
     * Tests addressRanges() setter method.
     */
    @Test
    public void testSetAddressRanges() throws Exception {
        ospfAreaAddressRanges = new ArrayList();
        ospfAreaAddressRanges.add(new OspfAreaAddressRangeImpl());
        ospfAreaAddressRanges.add(new OspfAreaAddressRangeImpl());
        ospfAreaAddressRanges.add(new OspfAreaAddressRangeImpl());
        ospfAreaAddressRanges.add(new OspfAreaAddressRangeImpl());
        ospfArea.setAddressRanges(ospfAreaAddressRanges);
        assertThat(ospfArea.addressRanges().size(), is(4));
    }

    /**
     * Tests isTransitCapability() getter method.
     */
    @Test
    public void testIsTransitCapability() throws Exception {
        ospfArea.setTransitCapability(true);
        assertThat(ospfArea.isTransitCapability(), is(true));
    }

    /**
     * Tests isTransitCapability() setter method.
     */
    @Test
    public void testSetTransitCapability() throws Exception {
        ospfArea.setTransitCapability(true);
        assertThat(ospfArea.isTransitCapability(), is(true));
    }

    /**
     * Tests isExternalRoutingCapability() getter method.
     */
    @Test
    public void testIsExternalRoutingCapability() throws Exception {
        ospfArea.setExternalRoutingCapability(true);
        assertThat(ospfArea.isExternalRoutingCapability(), is(true));
    }

    /**
     * Tests isExternalRoutingCapability() setter method.
     */
    @Test
    public void testSetExternalRoutingCapability() throws Exception {
        ospfArea.setExternalRoutingCapability(true);
        assertThat(ospfArea.isExternalRoutingCapability(), is(true));
    }

    /**
     * Tests stubCost() getter method.
     */
    @Test
    public void testGetStubCost() throws Exception {
        ospfArea.setStubCost(100);
        assertThat(ospfArea.stubCost(), is(100));
    }

    /**
     * Tests stubCost() setter method.
     */
    @Test
    public void testSetStubCost() throws Exception {
        ospfArea.setStubCost(100);
        assertThat(ospfArea.stubCost(), is(100));
    }

    /**
     * Tests getInterfacesLst() getter method.
     */
    @Test
    public void testGetInterfacesLst() throws Exception {
        ospfInterfaces = new ArrayList();
        ospfInterface1 = new OspfInterfaceImpl();
        ospfInterface1.setIpAddress(Ip4Address.valueOf("1.1.1.1"));
        ospfInterfaces.add(ospfInterface1);
        ospfInterface2 = new OspfInterfaceImpl();
        ospfInterface2.setIpAddress(Ip4Address.valueOf("2.2.2.2"));
        ospfInterfaces.add(ospfInterface2);
        ospfInterface3 = new OspfInterfaceImpl();
        ospfInterface3.setIpAddress(Ip4Address.valueOf("3.3.3.3"));
        ospfInterfaces.add(ospfInterface3);
        ospfArea.setInterfacesLst(ospfInterfaces);
        assertThat(ospfInterfaces.size(), is(3));
        assertThat(ospfArea.getInterfacesLst(), is(notNullValue()));
    }

    /**
     * Tests setInterfacesLst() setter method.
     */
    @Test
    public void testSetInterfacesLst() throws Exception {
        ospfInterfaces = new ArrayList();
        ospfInterface1 = new OspfInterfaceImpl();
        ospfInterface1.setIpAddress(Ip4Address.valueOf("1.1.1.1"));
        ospfInterfaces.add(ospfInterface1);
        ospfInterface2 = new OspfInterfaceImpl();
        ospfInterface2.setIpAddress(Ip4Address.valueOf("2.2.2.2"));
        ospfInterfaces.add(ospfInterface2);
        ospfInterface3 = new OspfInterfaceImpl();
        ospfInterface3.setIpAddress(Ip4Address.valueOf("3.3.3.3"));
        ospfInterfaces.add(ospfInterface3);
        ospfArea.setInterfacesLst(ospfInterfaces);
        assertThat(ospfInterfaces.size(), is(3));
        assertThat(ospfArea.getInterfacesLst(), is(notNullValue()));
    }

    /**
     * Tests noNeighborInLsaExchangeProcess() method.
     */
    @Test
    public void testNoNeighborInLsaExchangeProcess() throws Exception {
        ospfInterfaces = new ArrayList();
        ospfInterface1 = new OspfInterfaceImpl();
        ospfInterface1.setIpAddress(Ip4Address.valueOf("1.1.1.1"));
        ospfNbr = new OspfNbrImpl(new OspfAreaImpl(), new OspfInterfaceImpl(),
                                  Ip4Address.valueOf("1.1.1.1"),
                                  Ip4Address.valueOf("2.2.2.2"), 2,
                                  new OspfInterfaceChannelHandler(new Controller(),
                                                                  new OspfAreaImpl(),
                                                                  new OspfInterfaceImpl()),
                                  topologyForDeviceAndLink);
        ospfNbr.setState(OspfNeighborState.EXCHANGE.EXCHANGE);
        ospfInterface1.addNeighbouringRouter(ospfNbr);
        ospfInterfaces.add(ospfInterface1);
        ospfArea.setInterfacesLst(ospfInterfaces);
        assertThat(ospfArea.noNeighborInLsaExchangeProcess(), is(false));
    }

    /**
     * Tests getLsaHeaders() method.
     */
    @Test
    public void testGetLsaHeaders() throws Exception {
        assertThat(ospfArea.getLsaHeaders(true, true).size(), is(0));
    }

    /**
     * Tests getLsa() method.
     */
    @Test
    public void testGetLsa() throws Exception {
        assertThat(ospfArea.getLsa(1, "1.1.1.1", "1.1.1.1"), is(nullValue()));
        assertThat(ospfArea.getLsa(10, "1.1.1.1", "1.1.1.1"), is(nullValue()));
    }

    /**
     * Tests lsaLookup() method.
     */
    @Test
    public void testLsaLookup() throws Exception {
        assertThat(ospfArea.lsaLookup(new RouterLsa()), is(nullValue()));
    }

    /**
     * Tests isNewerOrSameLsa() method.
     */
    @Test
    public void testIsNewerOrSameLsa() throws Exception {
        assertThat(ospfArea.isNewerOrSameLsa(new RouterLsa(), new RouterLsa()), is("same"));
    }

    /**
     * Tests addLsa() method.
     */
    @Test
    public void testAddLsa() throws Exception {
        ospfArea.addLsa(new RouterLsa(), new OspfInterfaceImpl());
        assertThat(ospfArea, is(notNullValue()));
    }

    /**
     * Tests addLsa() method.
     */
    @Test
    public void testAddLsa1() throws Exception {
        ospfArea.addLsa(new RouterLsa(), false, new OspfInterfaceImpl());
        assertThat(ospfArea, is(notNullValue()));
    }

    /**
     * Tests addLsaToMaxAgeBin() method.
     */
    @Test
    public void testAddLsaToMaxAgeBin() throws Exception {
        ospfArea.addLsaToMaxAgeBin("111", new LsaWrapperImpl());
        assertThat(ospfArea, is(notNullValue()));
    }

    /**
     * Tests setDbRouterSequenceNumber() method.
     */
    @Test
    public void testSetDbRouterSequenceNumber() throws Exception {
        ospfArea.setDbRouterSequenceNumber(123456);
        assertThat(ospfArea, is(notNullValue()));
    }

    /**
     * Tests deleteLsa() method.
     */
    @Test
    public void testDeleteLsa() throws Exception {
        ospfArea.deleteLsa(new LsaHeader());
        assertThat(ospfArea, is(notNullValue()));
    }

    /**
     * Tests removeLsaFromBin() method.
     */
    @Test
    public void testRemoveLsaFromBin() throws Exception {
        ospfArea.removeLsaFromBin(new LsaWrapperImpl());
        assertThat(ospfArea, is(notNullValue()));
    }

    /**
     * Tests to string method.
     */
    @Test
    public void testToString() throws Exception {
        assertThat(ospfArea.toString(), is(notNullValue()));
    }

    /**
     * Tests getNeighborsInFullState() method.
     */
    @Test
    public void testGetNeighborsinFullState() throws Exception {
        ospfInterfaces = new ArrayList();
        ospfInterface1 = new OspfInterfaceImpl();
        ospfInterface1.setIpAddress(Ip4Address.valueOf("1.1.1.1"));
        ospfNbr = new OspfNbrImpl(new OspfAreaImpl(), new OspfInterfaceImpl(),
                                  Ip4Address.valueOf("1.1.1.1"),
                                  Ip4Address.valueOf("2.2.2.2"), 2,
                                  new OspfInterfaceChannelHandler(new Controller(),
                                                                  new OspfAreaImpl(),
                                                                  new OspfInterfaceImpl()),
                                  topologyForDeviceAndLink);
        ospfNbr.setState(OspfNeighborState.FULL);
        ospfInterface1.addNeighbouringRouter(ospfNbr);
        ospfInterfaces.add(ospfInterface1);
        ospfArea.setInterfacesLst(ospfInterfaces);
        assertThat(ospfArea.getNeighborsInFullState(ospfInterface1).size(), is(1));
    }

    /**
     * Tests getLsaKey() method.
     */
    @Test
    public void testGetLsaKey() throws Exception {
        lsaHeader = new LsaHeader();
        lsaHeader.setAdvertisingRouter(Ip4Address.valueOf("1.1.1.1"));
        assertThat(ospfArea.getLsaKey(lsaHeader), is(notNullValue()));
    }

    /**
     * Tests addToOtherNeighborLsaTxList() method.
     */
    @Test
    public void testAddToOtherNeighborLsaTxList() throws Exception {
        ospfInterfaces = new ArrayList();
        ospfInterface1 = new OspfInterfaceImpl();
        ospfInterface1.setIpAddress(Ip4Address.valueOf("1.1.1.1"));
        ospfNbr = new OspfNbrImpl(new OspfAreaImpl(), new OspfInterfaceImpl(),
                                  Ip4Address.valueOf("1.1.1.1"),
                                  Ip4Address.valueOf("2.2.2.2"), 2,
                                  new OspfInterfaceChannelHandler(new Controller(),
                                                                  new OspfAreaImpl(),
                                                                  new OspfInterfaceImpl()),
                                  topologyForDeviceAndLink);
        ospfNbr.setState(OspfNeighborState.FULL);
        ospfInterface1.addNeighbouringRouter(ospfNbr);
        ospfInterfaces.add(ospfInterface1);
        ospfArea.setInterfacesLst(ospfInterfaces);
        lsaHeader = new LsaHeader();
        lsaHeader.setAdvertisingRouter(Ip4Address.valueOf("1.1.1.1"));
        ospfArea.addToOtherNeighborLsaTxList(lsaHeader);
        assertThat(ospfArea, is(notNullValue()));
    }

    /**
     * Tests options() getter method.
     */
    @Test
    public void testGetOptions() throws Exception {
        ospfArea.setOptions(2);
        assertThat(ospfArea.options(), is(2));
    }

    /**
     * Tests options() setter method.
     */
    @Test
    public void testSetOptions() throws Exception {
        ospfArea.setOptions(2);
        assertThat(ospfArea.options(), is(2));
    }

    /**
     * Tests isOpaqueEnabled() method.
     */
    @Test
    public void testGetOpaqueEnabledOptions() throws Exception {
        ospfArea.setIsOpaqueEnabled(true);
        assertThat(ospfArea.isOpaqueEnabled(), is(true));
    }

    /**
     * Tests database()  method.
     */
    @Test
    public void testGetDatabase() throws Exception {
        assertThat(ospfArea.database(), is(notNullValue()));
    }

    /**
     * Tests opaqueEnabledOptions()  method.
     */
    @Test
    public void testOpaqueEnabledOptionsa() throws Exception {
        assertThat(ospfArea.opaqueEnabledOptions(), is(66));
    }
}