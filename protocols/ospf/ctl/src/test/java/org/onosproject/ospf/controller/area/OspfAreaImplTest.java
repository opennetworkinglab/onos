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
import org.onosproject.ospf.controller.OspfInterface;
import org.onosproject.ospf.controller.OspfNeighborState;
import org.onosproject.ospf.controller.TopologyForDeviceAndLink;
import org.onosproject.ospf.controller.impl.OspfNbrImpl;
import org.onosproject.ospf.controller.impl.TopologyForDeviceAndLinkImpl;
import org.onosproject.ospf.controller.lsdb.LsaWrapperImpl;
import org.onosproject.ospf.protocol.lsa.LsaHeader;
import org.onosproject.ospf.protocol.lsa.OpaqueLsaHeader;
import org.onosproject.ospf.protocol.lsa.types.NetworkLsa;
import org.onosproject.ospf.protocol.lsa.types.OpaqueLsa10;
import org.onosproject.ospf.protocol.lsa.types.RouterLsa;
import org.onosproject.ospf.protocol.util.OspfInterfaceState;
import org.onosproject.ospf.protocol.util.OspfParameters;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Unit test class for OspfAreaImpl.
 */
public class OspfAreaImplTest {

    private OspfAreaImpl ospfArea;
    private int result;
    private List<OspfInterface> ospfInterfaces = new ArrayList<>();
    private OspfInterfaceImpl ospfInterface1;
    private OspfInterfaceImpl ospfInterface2;
    private OspfInterfaceImpl ospfInterface3;
    private OspfNbrImpl ospfNbr;
    private OspfNbrImpl ospfNbr1;
    private NetworkLsa networkLsa;
    private LsaHeader lsaHeader;
    private Ip4Address ip4Address = Ip4Address.valueOf("10.10.10.10");
    private Ip4Address ip4Address1 = Ip4Address.valueOf("11.11.11.11");
    private Ip4Address networkAddress = Ip4Address.valueOf("255.255.255.255");
    private TopologyForDeviceAndLink topologyForDeviceAndLink;
    private RouterLsa routerLsa;
    private OpaqueLsaHeader opaqueLsaHeader;
    private OpaqueLsa10 opaqueLsa10;

    @Before
    public void setUp() throws Exception {
        lsaHeader = new LsaHeader();
        opaqueLsaHeader = new OpaqueLsaHeader();
        opaqueLsaHeader.setAdvertisingRouter(ip4Address);
        lsaHeader.setAdvertisingRouter(ip4Address);
        routerLsa = new RouterLsa(lsaHeader);
        routerLsa.setAdvertisingRouter(ip4Address);
        opaqueLsa10 = new OpaqueLsa10(opaqueLsaHeader);
        ospfArea = new OspfAreaImpl();
        ospfInterface1 = new OspfInterfaceImpl();
        topologyForDeviceAndLink = new TopologyForDeviceAndLinkImpl();
        ospfNbr = new OspfNbrImpl(ospfArea, ospfInterface1, ip4Address, ip4Address1,
                                  2, topologyForDeviceAndLink);
        ospfNbr.setState(OspfNeighborState.EXCHANGE);
        ospfNbr1 = new OspfNbrImpl(ospfArea, ospfInterface1, ip4Address, ip4Address1,
                                   2, topologyForDeviceAndLink);
        ospfNbr1.setState(OspfNeighborState.FULL);
        ospfNbr1.setNeighborId(ip4Address);
        ospfNbr.setNeighborId(ip4Address);
        ospfNbr.setIsOpaqueCapable(true);
        ospfInterface1.addNeighbouringRouter(ospfNbr);
        ospfInterface1.addNeighbouringRouter(ospfNbr1);
        ospfInterface2 = new OspfInterfaceImpl();
        ospfInterface2.setIpAddress(ip4Address);
        ospfInterface2.setIpNetworkMask(networkAddress);
        ospfInterface2.setState(OspfInterfaceState.LOOPBACK);
        ospfInterface2.addNeighbouringRouter(ospfNbr);
        ospfInterface2.addNeighbouringRouter(ospfNbr1);
        ospfInterfaces.add(ospfInterface2);
    }

    @After
    public void tearDown() throws Exception {
        ospfArea = null;
        ospfInterfaces = null;
        lsaHeader = null;
        networkLsa = null;
        ospfInterface1 = null;
        ospfInterface2 = null;
        ospfInterface3 = null;

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
        ospfArea.setOspfInterfaceList(ospfInterfaces);
        ospfArea.setRouterId(Ip4Address.valueOf("111.111.111.111"));
        networkLsa = ospfArea.buildNetworkLsa(Ip4Address.valueOf("1.1.1.1"),
                                              Ip4Address.valueOf("255.255.255.255"));
        assertThat(ospfInterfaces.size(), is(3));
        assertThat(networkLsa, is(notNullValue()));
        assertThat(ospfArea, is(notNullValue()));
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
     * Tests ospfInterfaceList() getter method.
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
        ospfArea.setOspfInterfaceList(ospfInterfaces);
        assertThat(ospfInterfaces.size(), is(3));
        assertThat(ospfArea.ospfInterfaceList(), is(notNullValue()));
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
        ospfArea.setOspfInterfaceList(ospfInterfaces);
        assertThat(ospfInterfaces.size(), is(3));
        assertThat(ospfArea.ospfInterfaceList(), is(notNullValue()));
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
     * Tests getLsaKey() method.
     */
    @Test
    public void testGetLsaKey() throws Exception {
        lsaHeader = new LsaHeader();
        lsaHeader.setAdvertisingRouter(Ip4Address.valueOf("1.1.1.1"));
        assertThat(ospfArea.getLsaKey(lsaHeader), is(notNullValue()));
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

    /**
     * Tests noNeighborInLsaExchangeProcess()  method.
     */
    @Test
    public void testNoNeighborInLsaExchangeProcess() throws Exception {
        ospfArea.setOspfInterfaceList(ospfInterfaces);
        ospfArea.noNeighborInLsaExchangeProcess();
        assertThat(ospfArea, is(notNullValue()));
    }

    /**
     * Tests getNeighborsInFullState()  method.
     */
    @Test
    public void testGetNeighborsInFullState() throws Exception {
        ospfArea.getNeighborsInFullState(ospfInterface1);
        assertThat(ospfArea, is(notNullValue()));
    }

    /**
     * Tests addToOtherNeighborLsaTxList()  method.
     */
    @Test
    public void testAddToOtherNeighborLsaTxList() throws Exception {
        ospfArea.setOspfInterfaceList(ospfInterfaces);
        ospfArea.addToOtherNeighborLsaTxList(routerLsa);
        assertThat(ospfArea, is(notNullValue()));

        opaqueLsa10.setLsType(OspfParameters.LINK_LOCAL_OPAQUE_LSA);
        ospfArea.addToOtherNeighborLsaTxList(opaqueLsa10);
        assertThat(ospfArea, is(notNullValue()));
    }

    /**
     * Tests buildRouterLsa()  method.
     */
    @Test
    public void testBuildRouterLsa() throws Exception {
        ospfArea.setRouterId(ip4Address);
        ospfArea.setOspfInterfaceList(ospfInterfaces);
        ospfInterface1.setState(OspfInterfaceState.POINT2POINT);
        ospfInterface1.setIpAddress(ip4Address);
        ospfInterface1.setIpNetworkMask(networkAddress);
        ospfInterfaces.add(ospfInterface1);
        ospfArea.buildRouterLsa(ospfInterface1);
        ospfArea.setOspfInterfaceList(ospfInterfaces);
        assertThat(ospfArea, is(notNullValue()));

    }
}