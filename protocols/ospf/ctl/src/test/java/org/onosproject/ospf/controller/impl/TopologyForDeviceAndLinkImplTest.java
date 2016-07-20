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

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.Ip4Address;
import org.onosproject.ospf.controller.DeviceInformation;
import org.onosproject.ospf.controller.OspfLinkTed;
import org.onosproject.ospf.controller.OspfLsa;
import org.onosproject.ospf.controller.area.OspfAreaImpl;
import org.onosproject.ospf.controller.area.OspfInterfaceImpl;
import org.onosproject.ospf.protocol.lsa.OpaqueLsaHeader;
import org.onosproject.ospf.protocol.lsa.TlvHeader;
import org.onosproject.ospf.protocol.lsa.subtypes.OspfLsaLink;
import org.onosproject.ospf.protocol.lsa.tlvtypes.LinkTlv;
import org.onosproject.ospf.protocol.lsa.types.OpaqueLsa10;
import org.onosproject.ospf.protocol.lsa.types.RouterLsa;

import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit test class for TopologyForDeviceAndLinkImpl.
 */
public class TopologyForDeviceAndLinkImplTest {
    private final byte[] packet = {0, 9, 0, 4, 0, 0, 0, 1,
            0, 9, 0, 4, 0, 0, 0, 1,
            0, 1, 0, 4, 0, 0, 0, 1,
            0, 2, 0, 4, 0, 0, 0, 1,
            0, 3, 0, 4, 0, 0, 0, 1,
            0, 4, 0, 4, 0, 0, 0, 1,
            0, 6, 0, 4, 0, 0, 0, 1,
            0, 7, 0, 4, 0, 0, 0, 1,
            0, 8, 0, 4, 0, 0, 0, 1,
    };
    private TopologyForDeviceAndLinkImpl topologyForDeviceAndLink;
    private Map result;
    private LinkTlv linkTlv;
    private TlvHeader header;
    private ChannelBuffer channelBuffer;

    @Before
    public void setUp() throws Exception {
        topologyForDeviceAndLink = new TopologyForDeviceAndLinkImpl();
    }

    @After
    public void tearDown() throws Exception {
        topologyForDeviceAndLink = null;
    }

    /**
     * Tests deviceInformationMap() method.
     */
    @Test
    public void testDeviceInformationMap() throws Exception {
        result = topologyForDeviceAndLink.deviceInformationMap();
        assertThat(result.size(), is(0));
    }

    /**
     * Tests setDeviceInformationMap() method.
     */
    @Test
    public void testSetDeviceInformationMap() throws Exception {
        topologyForDeviceAndLink.setDeviceInformationMap("1.1.1.1", new DeviceInformationImpl());
        result = topologyForDeviceAndLink.deviceInformationMap();
        assertThat(result.size(), is(1));
    }

    /**
     * Tests deviceInformation() method.
     */
    @Test
    public void testDeviceInformation() throws Exception {
        topologyForDeviceAndLink.setDeviceInformationMap("1.1.1.1", new DeviceInformationImpl());
        DeviceInformation deviceInformation = topologyForDeviceAndLink.deviceInformation("1.1.1.1");
        assertThat(deviceInformation, is(notNullValue()));
    }

    /**
     * Tests removeDeviceInformationMap() method.
     */
    @Test
    public void testRemoveDeviceInformationMap() throws Exception {
        topologyForDeviceAndLink.setDeviceInformationMap("1.1.1.1", new DeviceInformationImpl());
        topologyForDeviceAndLink.deviceInformation("1.1.1.1");
        result = topologyForDeviceAndLink.deviceInformationMap();
        topologyForDeviceAndLink.removeDeviceInformationMap("1.1.1.1");
        assertThat(result.size(), is(0));
    }

    /**
     * Tests linkInformationMap() method.
     */
    @Test
    public void testLinkInformationMap() throws Exception {
        result = topologyForDeviceAndLink.linkInformationMap();
        assertThat(result.size(), is(0));
    }

    /**
     * Tests setLinkInformationMap() method.
     */
    @Test
    public void testSetLinkInformationMap() throws Exception {
        topologyForDeviceAndLink.setLinkInformationMap("1.1.1.1", new LinkInformationImpl());
        result = topologyForDeviceAndLink.linkInformationMap();
        assertThat(result.size(), is(1));
    }

    /**
     * Tests removeLinkInformationMap() method.
     */
    @Test
    public void testRemoveLinkInformationMap() throws Exception {
        topologyForDeviceAndLink.setLinkInformationMap("1.1.1.1", new LinkInformationImpl());
        topologyForDeviceAndLink.removeLinkInformationMap("1.1.1.1");
        result = topologyForDeviceAndLink.linkInformationMap();
        assertThat(result.size(), is(0));
    }

    /**
     * Tests getOspfLinkTedHashMap() method.
     */
    @Test
    public void testGetOspfLinkTedHashMap() throws Exception {
        OspfLinkTed ospfLinkTed = topologyForDeviceAndLink.getOspfLinkTedHashMap("1.1.1.1");
        assertThat(ospfLinkTed, is(nullValue()));
    }

    /**
     * Tests addLocalDevice() method.
     */
    @Test
    public void testAddLocalDevice() throws Exception {
        OspfAreaImpl ospfArea = new OspfAreaImpl();
        ospfArea.setRouterId(Ip4Address.valueOf("5.5.5.5"));
        topologyForDeviceAndLink.addLocalDevice(createOspfLsa(), new OspfInterfaceImpl(), ospfArea);
        topologyForDeviceAndLink.addLocalDevice(createOspfLsa1(), new OspfInterfaceImpl(), ospfArea);
        assertThat(topologyForDeviceAndLink, is(notNullValue()));
    }

    /**
     * Tests addLocalLink() method.
     */
    @Test
    public void testAddLocalLink() throws Exception {
        Ip4Address linkData = Ip4Address.valueOf("1.1.1.1");
        Ip4Address linkSrc = Ip4Address.valueOf("2.2.2.2");
        Ip4Address linkDest = Ip4Address.valueOf("3.3.3.3");
        boolean opaqueEnabled = true;
        boolean linkSrcIdNotRouterId = true;
        topologyForDeviceAndLink.addLocalLink("10.10.10.10", linkData, linkSrc,
                                              linkDest, opaqueEnabled, linkSrcIdNotRouterId);
        assertThat(topologyForDeviceAndLink, is(notNullValue()));
    }

    /**
     * Tests removeLinks() method.
     */
    @Test
    public void testRemoveLinks() throws Exception {
        Ip4Address linkData = Ip4Address.valueOf("1.1.1.1");
        Ip4Address linkSrc = Ip4Address.valueOf("2.2.2.2");
        Ip4Address linkDest = Ip4Address.valueOf("3.3.3.3");
        boolean opaqueEnabled = true;
        boolean linkSrcIdNotRouterId = true;
        topologyForDeviceAndLink.addLocalLink("10.10.10.10", linkData, linkSrc,
                                              linkDest, opaqueEnabled, linkSrcIdNotRouterId);
        topologyForDeviceAndLink.removeLinks(Ip4Address.valueOf("10.10.10.10"));
        assertThat(topologyForDeviceAndLink, is(notNullValue()));
    }

    /**
     * Tests updateLinkInformation() method.
     */
    @Test
    public void testUpdateLinkInformation() throws Exception {
        OspfAreaImpl ospfArea = new OspfAreaImpl();
        ospfArea.setRouterId(Ip4Address.valueOf("5.5.5.5"));
        topologyForDeviceAndLink.updateLinkInformation(createOspfLsa(), ospfArea);
        assertThat(topologyForDeviceAndLink, is(notNullValue()));
    }

    /**
     * Tests getDeleteRouterInformation() method.
     */
    @Test
    public void testGetDeleteRouterInformation() throws Exception {
        OspfAreaImpl ospfArea = new OspfAreaImpl();
        ospfArea.setRouterId(Ip4Address.valueOf("5.5.5.5"));
        topologyForDeviceAndLink.updateLinkInformation(createOspfLsa(), ospfArea);
        List list = topologyForDeviceAndLink.getDeleteRouterInformation(createOspfLsa(), ospfArea);
        assertThat(list, is(notNullValue()));
    }

    /**
     * Utility for test methods.
     */
    private OspfLsa createOspfLsa() {
        RouterLsa routerLsa = new RouterLsa();
        routerLsa.setLsType(1);
        routerLsa.setAdvertisingRouter(Ip4Address.valueOf("6.6.6.6"));
        OspfLsaLink ospfLsaLink = new OspfLsaLink();
        ospfLsaLink.setLinkData("192.168.7.77");
        ospfLsaLink.setLinkId("9.9.9.9");
        ospfLsaLink.setLinkType(1);
        OspfLsaLink ospfLsaLink0 = new OspfLsaLink();
        ospfLsaLink0.setLinkData("7.7.7.7");
        ospfLsaLink0.setLinkId("7.7.7.7");
        ospfLsaLink0.setLinkType(2);
        OspfLsaLink ospfLsaLink120 = new OspfLsaLink();
        ospfLsaLink120.setLinkData("192.168.7.77");
        ospfLsaLink120.setLinkId("1.1.1.1");
        ospfLsaLink120.setLinkType(1);
        OspfLsaLink lsaLink = new OspfLsaLink();
        lsaLink.setLinkData("192.168.7.77");
        lsaLink.setLinkId("14.14.14.14");
        lsaLink.setLinkType(2);
        routerLsa.addRouterLink(lsaLink);
        routerLsa.addRouterLink(ospfLsaLink);
        routerLsa.addRouterLink(ospfLsaLink0);
        routerLsa.addRouterLink(ospfLsaLink120);
        return routerLsa;
    }

    /**
     * Utility for test methods.
     */
    private OspfLsa createOspfLsa1() throws Exception {
        OpaqueLsa10 opaqueLsa10 = new OpaqueLsa10(new OpaqueLsaHeader());
        opaqueLsa10.setLsType(10);
        header = new TlvHeader();
        header.setTlvLength(8);
        header.setTlvType(9);
        linkTlv = new LinkTlv(header);
        channelBuffer = ChannelBuffers.copiedBuffer(packet);
        linkTlv.readFrom(channelBuffer);
        opaqueLsa10.addValue(linkTlv);
        return opaqueLsa10;
    }
}
