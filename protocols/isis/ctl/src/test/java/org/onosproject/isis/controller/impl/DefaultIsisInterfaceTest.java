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
package org.onosproject.isis.controller.impl;

import org.easymock.EasyMock;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.MacAddress;
import org.onosproject.isis.controller.IsisInterface;
import org.onosproject.isis.controller.IsisInterfaceState;
import org.onosproject.isis.controller.IsisLsdb;
import org.onosproject.isis.controller.IsisMessage;
import org.onosproject.isis.controller.IsisNeighbor;
import org.onosproject.isis.controller.IsisNetworkType;
import org.onosproject.isis.controller.IsisPduType;
import org.onosproject.isis.controller.IsisRouterType;
import org.onosproject.isis.controller.impl.lsdb.DefaultIsisLsdb;
import org.onosproject.isis.io.isispacket.IsisHeader;
import org.onosproject.isis.io.isispacket.pdu.Csnp;
import org.onosproject.isis.io.isispacket.pdu.L1L2HelloPdu;
import org.onosproject.isis.io.isispacket.pdu.LsPdu;
import org.onosproject.isis.io.isispacket.pdu.P2PHelloPdu;
import org.onosproject.isis.io.isispacket.pdu.Psnp;
import org.onosproject.isis.io.isispacket.tlv.AdjacencyStateTlv;
import org.onosproject.isis.io.isispacket.tlv.AreaAddressTlv;
import org.onosproject.isis.io.isispacket.tlv.LspEntriesTlv;
import org.onosproject.isis.io.isispacket.tlv.LspEntry;
import org.onosproject.isis.io.isispacket.tlv.TlvHeader;
import org.onosproject.isis.io.isispacket.tlv.TlvType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

/**
 * Unit test case for DefaultIsisInterface.
 */
public class DefaultIsisInterfaceTest {
    private final MacAddress macAddress = MacAddress.valueOf("AA:BB:CC:DD:EE:FF");
    private final MacAddress macAddress1 = MacAddress.valueOf("AA:CC:CC:DD:EE:FF");
    private final Ip4Address ip4Address = Ip4Address.valueOf("10.10.0.0");
    private final byte[] mask = {
            (byte) 255, (byte) 255, (byte) 255, (byte) 224
    };
    private final byte[] mask1 = {
            (byte) 0, (byte) 0, (byte) 0, (byte) 0
    };
    private final String intSysName = "ROUTER";
    private final String sysId = "1111.1111.1111";
    private final String areaAddr = "49.002";
    private final byte[] csnpBytes = {
            0, 67, 18, 52, 18, 52, 0,
            18, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, -1, -1, -1, -1,
            -1, -1, 9, 32, 4, -81, 18, 52, 18, 52, 0, 18, 0, 0, 0,
            0, 0, 41, -92, -30, 4, -81, 41, 41, 41, 41, 41, 41, 0,
            0, 0, 0, 0, 1, 91, 126
    };
    private IsisInterfaceState resultIfState;
    private DefaultIsisInterface defaultIsisInterface;
    private IsisHeader isisHeader;
    private IsisInterface isisInterface;
    private Set<MacAddress> resultSet;
    private int resultInt;
    private IsisLsdb resultLsdb;
    private IsisNeighbor resultNeighborList;
    private Ip4Address resultIPv4Addr;
    private MacAddress resultMacAddr;
    private byte[] resultByteArr;
    private String resultStr;
    private IsisNetworkType resultNwType;
    private List<Ip4Address> ip4Addresses = new ArrayList<>();
    private DefaultIsisNeighbor defaultIsisNeighbor;
    private IsisNeighbor result;
    private IsisLsdb result1;
    private Set<MacAddress> result2;
    private Channel result3;
    private IsisMessage isisMessage;
    private IsisLsdb isisLsdb;
    private Channel channel;
    private L1L2HelloPdu helloPdu;
    private LsPdu lsPdu;
    private Csnp csnp;
    private Psnp psnp;
    private P2PHelloPdu p2PHelloPdu;
    private boolean result4;
    private String result5;

    @Before
    public void setUp() throws Exception {
        channel = EasyMock.createNiceMock(Channel.class);
        defaultIsisInterface = new DefaultIsisInterface();
        defaultIsisInterface.setInterfaceMacAddress(macAddress);
        isisHeader = new IsisHeader();
        isisHeader.setIrpDiscriminator((byte) 1);
        helloPdu = new L1L2HelloPdu(isisHeader);
        isisInterface = new DefaultIsisInterface();
        defaultIsisNeighbor = new DefaultIsisNeighbor(helloPdu, isisInterface);
        defaultIsisNeighbor.setNeighborMacAddress(macAddress);
        isisLsdb = new DefaultIsisLsdb();
    }

    @After
    public void tearDown() throws Exception {
        defaultIsisInterface = null;
        helloPdu = null;
        isisInterface = null;
        resultNeighborList = null;
    }

    /**
     * Tests interfaceIndex() getter method.
     */
    @Test
    public void testInterfaceIndex() throws Exception {
        defaultIsisInterface.setInterfaceIndex(2);
        resultInt = defaultIsisInterface.interfaceIndex();
        assertThat(resultInt, is(2));
    }

    /**
     * Tests interfaceIndex() setter method.
     */
    @Test
    public void testSetInterfaceIndex() throws Exception {
        defaultIsisInterface.setInterfaceIndex(2);
        resultInt = defaultIsisInterface.interfaceIndex();
        assertThat(resultInt, is(2));

    }

    /**
     * Tests interfaceIpAddress() getter method.
     */
    @Test
    public void testInterfaceIpAddress() throws Exception {
        defaultIsisInterface.setInterfaceIpAddress(ip4Address);
        resultIPv4Addr = defaultIsisInterface.interfaceIpAddress();
        assertThat(resultIPv4Addr, is(ip4Address));
    }

    /**
     * Tests interfaceIpAddress() setter method.
     */
    @Test
    public void testSetInterfaceIpAddress() throws Exception {
        defaultIsisInterface.setInterfaceIpAddress(ip4Address);
        resultIPv4Addr = defaultIsisInterface.interfaceIpAddress();
        assertThat(resultIPv4Addr, is(ip4Address));
    }

    /**
     * Tests networkMask() getter method.
     */
    @Test
    public void testNetworkMask() throws Exception {
        defaultIsisInterface.setNetworkMask(mask);
        resultByteArr = defaultIsisInterface.networkMask();
        assertThat(resultByteArr, is(mask));
    }

    /**
     * Tests networkMask() setter method.
     */
    @Test
    public void testSetNetworkMask() throws Exception {
        defaultIsisInterface.setNetworkMask(mask);
        resultByteArr = defaultIsisInterface.networkMask();
        assertThat(resultByteArr, is(mask));
    }

    /**
     * Tests getInterfaceMacAddress() getter method.
     */
    @Test
    public void testGetInterfaceMacAddress() throws Exception {
        defaultIsisInterface.setInterfaceMacAddress(macAddress);
        resultMacAddr = defaultIsisInterface.getInterfaceMacAddress();
        assertThat(resultMacAddr, is(macAddress));
    }

    /**
     * Tests getInterfaceMacAddress() setter method.
     */
    @Test
    public void testSetInterfaceMacAddress() throws Exception {
        defaultIsisInterface.setInterfaceMacAddress(macAddress);
        resultMacAddr = defaultIsisInterface.getInterfaceMacAddress();
        assertThat(resultMacAddr, is(macAddress));
    }

    /**
     * Tests intermediateSystemName() getter method.
     */
    @Test
    public void testIntermediateSystemName() throws Exception {
        defaultIsisInterface.setIntermediateSystemName(intSysName);
        resultStr = defaultIsisInterface.intermediateSystemName();
        assertThat(resultStr, is(intSysName));
    }

    /**
     * Tests intermediateSystemName() setter method.
     */
    @Test
    public void testSetIntermediateSystemName() throws Exception {
        defaultIsisInterface.setIntermediateSystemName(intSysName);
        resultStr = defaultIsisInterface.intermediateSystemName();
        assertThat(resultStr, is(intSysName));
    }

    /**
     * Tests systemId() getter method.
     */
    @Test
    public void testSystemId() throws Exception {
        defaultIsisInterface.setSystemId(sysId);
        resultStr = defaultIsisInterface.systemId();
        assertThat(resultStr, is(sysId));
    }

    /**
     * Tests systemId() setter method.
     */
    @Test
    public void testSetSystemId() throws Exception {
        defaultIsisInterface.setSystemId(sysId);
        resultStr = defaultIsisInterface.systemId();
        assertThat(resultStr, is(sysId));
    }

    /**
     * Tests l1LanId() getter method.
     */
    @Test
    public void testL1LanId() throws Exception {
        defaultIsisInterface.setL1LanId(sysId);
        resultStr = defaultIsisInterface.l1LanId();
        assertThat(resultStr, is(sysId));
    }

    /**
     * Tests l1LanId() setter method.
     */
    @Test
    public void testSetL1LanId() throws Exception {
        defaultIsisInterface.setL1LanId(sysId);
        resultStr = defaultIsisInterface.l1LanId();
        assertThat(resultStr, is(sysId));
    }

    /**
     * Tests l2LanId() getter method.
     */
    @Test
    public void testL2LanId() throws Exception {
        defaultIsisInterface.setL2LanId(sysId);
        resultStr = defaultIsisInterface.l2LanId();
        assertThat(resultStr, is(sysId));
    }

    /**
     * Tests l2LanId() setter method.
     */
    @Test
    public void testSetL2LanId() throws Exception {
        defaultIsisInterface.setL2LanId(sysId);
        resultStr = defaultIsisInterface.l2LanId();
        assertThat(resultStr, is(sysId));
    }

    /**
     * Tests getIdLength() getter method.
     */
    @Test
    public void testGetIdLength() throws Exception {
        defaultIsisInterface.setIdLength(8);
        resultInt = defaultIsisInterface.getIdLength();
        assertThat(resultInt, is(8));
    }

    /**
     * Tests getIdLength() setter method.
     */
    @Test
    public void testSetIdLength() throws Exception {
        defaultIsisInterface.setIdLength(8);
        resultInt = defaultIsisInterface.getIdLength();
        assertThat(resultInt, is(8));
    }

    /**
     * Tests getMaxAreaAddresses() getter method.
     */
    @Test
    public void testGetMaxAreaAddresses() throws Exception {
        defaultIsisInterface.setMaxAreaAddresses(3);
        resultInt = defaultIsisInterface.getMaxAreaAddresses();
        assertThat(resultInt, is(3));
    }

    /**
     * Tests getMaxAreaAddresses() setter method.
     */
    @Test
    public void testSetMaxAreaAddresses() throws Exception {
        defaultIsisInterface.setMaxAreaAddresses(3);
        resultInt = defaultIsisInterface.getMaxAreaAddresses();
        assertThat(resultInt, is(3));
    }

    /**
     * Tests setReservedPacketCircuitType() getter method.
     */
    @Test
    public void testReservedPacketCircuitType() throws Exception {
        defaultIsisInterface.setReservedPacketCircuitType(1);
        resultInt = defaultIsisInterface.reservedPacketCircuitType();
        assertThat(resultInt, is(1));
    }

    /**
     * Tests setReservedPacketCircuitType() setter method.
     */
    @Test
    public void testSetReservedPacketCircuitType() throws Exception {
        defaultIsisInterface.setReservedPacketCircuitType(1);
        resultInt = defaultIsisInterface.reservedPacketCircuitType();
        assertThat(resultInt, is(1));
    }

    /**
     * Tests networkType() getter method.
     */
    @Test
    public void testNetworkType() throws Exception {
        defaultIsisInterface.setNetworkType(IsisNetworkType.BROADCAST);
        resultNwType = defaultIsisInterface.networkType();
        assertThat(resultNwType, is(IsisNetworkType.BROADCAST));
    }

    /**
     * Tests networkType() setter method.
     */
    @Test
    public void testSetNetworkType() throws Exception {
        defaultIsisInterface.setNetworkType(IsisNetworkType.BROADCAST);
        resultNwType = defaultIsisInterface.networkType();
        assertThat(resultNwType, is(IsisNetworkType.BROADCAST));
    }

    /**
     * Tests areaAddress() getter method.
     */
    @Test
    public void testAreaAddress() throws Exception {
        defaultIsisInterface.setAreaAddress(areaAddr);
        resultStr = defaultIsisInterface.areaAddress();
        assertThat(resultStr, is(areaAddr));
    }

    /**
     * Tests areaAddress() setter method.
     */
    @Test
    public void testSetAreaAddress() throws Exception {
        defaultIsisInterface.setAreaAddress(areaAddr);
        resultStr = defaultIsisInterface.areaAddress();
        assertThat(resultStr, is(areaAddr));
    }

    /**
     * Tests getAreaLength() getter method.
     */

    @Test
    public void testGetAreaLength() throws Exception {
        defaultIsisInterface.setAreaLength(3);
        resultInt = defaultIsisInterface.getAreaLength();
        assertThat(resultInt, is(3));
    }

    /**
     * Tests getAreaLength() setter method.
     */
    @Test
    public void testSetAreaLength() throws Exception {
        defaultIsisInterface.setAreaLength(3);
        resultInt = defaultIsisInterface.getAreaLength();
        assertThat(resultInt, is(3));
    }

    /**
     * Tests holdingTime() getter method.
     */
    @Test
    public void testHoldingTime() throws Exception {
        defaultIsisInterface.setHoldingTime(10);
        resultInt = defaultIsisInterface.holdingTime();
        assertThat(resultInt, is(10));
    }

    /**
     * Tests holdingTime() setter method.
     */
    @Test
    public void testSetHoldingTime() throws Exception {
        defaultIsisInterface.setHoldingTime(10);
        resultInt = defaultIsisInterface.holdingTime();
        assertThat(resultInt, is(10));
    }

    /**
     * Tests priority() getter method.
     */
    @Test
    public void testPriority() throws Exception {
        defaultIsisInterface.setPriority(1);
        resultInt = defaultIsisInterface.priority();
        assertThat(resultInt, is(1));
    }

    /**
     * Tests priority() setter method.
     */
    @Test
    public void testSetPriority() throws Exception {
        defaultIsisInterface.setPriority(1);
        resultInt = defaultIsisInterface.priority();
        assertThat(resultInt, is(1));
    }

    /**
     * Tests helloInterval() getter method.
     */
    @Test
    public void testHelloInterval() throws Exception {
        defaultIsisInterface.setHelloInterval(10);
        resultInt = defaultIsisInterface.helloInterval();
        assertThat(resultInt, is(10));
    }

    /**
     * Tests helloInterval() setter method.
     */
    @Test
    public void testSetHelloInterval() throws Exception {
        defaultIsisInterface.setHelloInterval(10);
        resultInt = defaultIsisInterface.helloInterval();
        assertThat(resultInt, is(10));
    }

    /**
     * Tests interfaceState() getter method.
     */
    @Test
    public void testInterfaceState() throws Exception {
        defaultIsisInterface.setInterfaceState(IsisInterfaceState.UP);
        resultIfState = defaultIsisInterface.interfaceState();
        assertThat(resultIfState, is(IsisInterfaceState.UP));
    }

    /**
     * Tests interfaceState() setter method.
     */
    @Test
    public void testSetInterfaceState() throws Exception {
        defaultIsisInterface.setInterfaceState(IsisInterfaceState.UP);
        resultIfState = defaultIsisInterface.interfaceState();
        assertThat(resultIfState, is(IsisInterfaceState.UP));
    }

    /**
     * Tests setCircuitId() getter method.
     */
    @Test
    public void testCircuitId() throws Exception {
        defaultIsisInterface.setCircuitId(sysId);
        resultStr = defaultIsisInterface.circuitId();
        assertThat(resultStr, is(sysId));
    }

    /**
     * Tests setCircuitId() setter method.
     */
    @Test
    public void testSetCircuitId() throws Exception {
        defaultIsisInterface.setCircuitId(sysId);
        resultStr = defaultIsisInterface.circuitId();
        assertThat(resultStr, is(sysId));
    }

    /**
     * Tests setAllConfiguredInterfaceIps() setter method.
     */
    @Test
    public void testSetAllConfiguredInterfaceIps() throws Exception {
        ip4Addresses.add(ip4Address);
        defaultIsisInterface.setAllConfiguredInterfaceIps(ip4Addresses);
        assertThat(defaultIsisInterface, is(notNullValue()));
    }

    /**
     * Tests setAllConfiguredInterfaceIps() method.
     */
    @Test
    public void testRemoveNeighbor() throws Exception {
        defaultIsisInterface.removeNeighbor(defaultIsisNeighbor);
        assertThat(defaultIsisInterface, is(notNullValue()));
    }

    /**
     * Tests lookup() method.
     */
    @Test
    public void testLookup() throws Exception {
        result = defaultIsisInterface.lookup(defaultIsisNeighbor.neighborMacAddress());
        assertThat(result, is(nullValue()));
    }

    /**
     * Tests isisLsdb() method.
     */
    @Test
    public void testIsisLsdb() throws Exception {
        result1 = defaultIsisInterface.isisLsdb();
        assertThat(result1, is(nullValue()));
    }

    /**
     * Tests neighbors() method.
     */
    @Test
    public void testNeighbors() throws Exception {
        result2 = defaultIsisInterface.neighbors();
        assertThat(result2, is(notNullValue()));
    }

    /**
     * Tests channel() method.
     */
    @Test
    public void testChannel() throws Exception {
        result3 = defaultIsisInterface.channel();
        assertThat(result3, is(nullValue()));
    }

    /**
     * Tests processIsisMessage() method.
     */
    @Test
    public void testProcessIsisMessage() throws Exception {
        helloPdu = new L1L2HelloPdu(isisHeader);
        helloPdu.setSourceMac(macAddress1);
        helloPdu.setIsisPduType(IsisPduType.L2HELLOPDU.value());
        defaultIsisInterface.setNetworkType(IsisNetworkType.BROADCAST);
        isisMessage = helloPdu;
        defaultIsisInterface.processIsisMessage(isisMessage, isisLsdb, channel);
        assertThat(defaultIsisInterface, is(notNullValue()));
    }

    /**
     * Tests processIsisMessage() method.
     */
    @Test(expected = Exception.class)
    public void testProcessIsisMessage1() throws Exception {
        lsPdu = new LsPdu(isisHeader);
        lsPdu.setSourceMac(macAddress1);
        lsPdu.setIsisPduType(IsisPduType.L2LSPDU.value());
        isisMessage = lsPdu;
        defaultIsisInterface.processIsisMessage(isisMessage, isisLsdb, channel);
        assertThat(defaultIsisInterface, is(notNullValue()));
    }

    /**
     * Tests processIsisMessage() method.
     */
    @Test
    public void testProcessIsisMessage2() throws Exception {
        csnp = new Csnp(isisHeader);
        csnp.setSourceMac(macAddress1);
        csnp.setIsisPduType(IsisPduType.L2CSNP.value());
        isisMessage = csnp;
        defaultIsisInterface.processIsisMessage(isisMessage, isisLsdb, channel);
        assertThat(defaultIsisInterface, is(notNullValue()));
    }

    /**
     * Tests processIsisMessage() method.
     */
    @Test
    public void testProcessIsisMessage3() throws Exception {
        psnp = new Psnp(isisHeader);
        psnp.setSourceMac(macAddress1);
        psnp.setIsisPduType(IsisPduType.L2PSNP.value());
        isisMessage = psnp;
        defaultIsisInterface.processIsisMessage(isisMessage, isisLsdb, channel);
        assertThat(defaultIsisInterface, is(notNullValue()));
    }

    /**
     * Tests processIsisMessage() method.
     */
    @Test
    public void testProcessIsisMessage4() throws Exception {
        p2PHelloPdu = new P2PHelloPdu(isisHeader);
        p2PHelloPdu.setSourceMac(macAddress1);
        p2PHelloPdu.setIsisPduType(IsisPduType.P2PHELLOPDU.value());
        defaultIsisInterface.setNetworkType(IsisNetworkType.P2P);
        isisMessage = p2PHelloPdu;
        defaultIsisInterface.processIsisMessage(isisMessage, isisLsdb, channel);
        assertThat(defaultIsisInterface, is(notNullValue()));
    }

    /**
     * Tests validateHelloMessage() method.
     */
    @Test
    public void testValidateHelloMessage() throws Exception {
        helloPdu = new L1L2HelloPdu(isisHeader);
        result4 = defaultIsisInterface.validateHelloMessage(helloPdu);
        assertThat(result4, is(false));
    }

    /**
     * Tests processL1L2HelloPduMessage() method.
     */
    @Test(expected = Exception.class)
    public void testProcessL1L2HelloPduMessage() throws Exception {
        helloPdu = new L1L2HelloPdu(isisHeader);
        helloPdu.setSourceMac(macAddress1);
        helloPdu.setCircuitType((byte) IsisRouterType.L2.value());
        defaultIsisInterface.processL1L2HelloPduMessage(helloPdu, channel);
        assertThat(defaultIsisInterface, is(notNullValue()));
    }

    /**
     * Tests processP2pHelloPduMessage() method.
     */
    @Test(expected = Exception.class)
    public void testProcessP2pHelloPduMessagee() throws Exception {
        defaultIsisInterface.setSystemId(sysId);
        p2PHelloPdu = new P2PHelloPdu(isisHeader);
        p2PHelloPdu.setIsisPduType(IsisPduType.P2PHELLOPDU.value());
        p2PHelloPdu.setSourceMac(macAddress1);
        p2PHelloPdu.setCircuitType((byte) IsisRouterType.L2.value());
        defaultIsisInterface.setNetworkType(IsisNetworkType.P2P);
        defaultIsisInterface.processIsisMessage(p2PHelloPdu, isisLsdb, channel);
        assertThat(defaultIsisInterface, is(notNullValue()));
    }

    /**
     * Tests processP2pHelloPduMessage() method.
     */
    @Test(expected = Exception.class)
    public void testProcessP2pHelloPduMessagee1() throws Exception {
        defaultIsisInterface.setSystemId(sysId);
        p2PHelloPdu = new P2PHelloPdu(isisHeader);
        p2PHelloPdu.setIsisPduType(IsisPduType.P2PHELLOPDU.value());
        p2PHelloPdu.setSourceMac(macAddress1);
        p2PHelloPdu.setCircuitType((byte) IsisRouterType.L2.value());
        defaultIsisInterface.setNetworkType(IsisNetworkType.P2P);
        defaultIsisInterface.setReservedPacketCircuitType(IsisRouterType.L2.value());
        defaultIsisInterface.setAllConfiguredInterfaceIps(ip4Addresses);
        defaultIsisInterface.setInterfaceIpAddress(ip4Address);
        defaultIsisInterface.setNetworkMask(mask1);
        defaultIsisInterface.processIsisMessage(p2PHelloPdu, isisLsdb, channel);
        assertThat(defaultIsisInterface, is(notNullValue()));
    }

    /**
     * Tests processP2pHelloPduMessage() method.
     */
    @Test(expected = Exception.class)
    public void testProcessP2pHelloPduMessagee2() throws Exception {
        defaultIsisInterface.setSystemId(sysId);
        p2PHelloPdu = new P2PHelloPdu(isisHeader);
        TlvHeader tlvHeader = new TlvHeader();
        tlvHeader.setTlvType(TlvType.AREAADDRESS.value());
        AreaAddressTlv areaAddressTlv = new AreaAddressTlv(tlvHeader);
        areaAddressTlv.addAddress(areaAddr);
        p2PHelloPdu.addTlv(areaAddressTlv);
        p2PHelloPdu.setIsisPduType(IsisPduType.P2PHELLOPDU.value());
        p2PHelloPdu.setSourceMac(macAddress1);
        p2PHelloPdu.setCircuitType((byte) IsisRouterType.L1.value());
        defaultIsisInterface.setNetworkType(IsisNetworkType.P2P);
        defaultIsisInterface.setReservedPacketCircuitType(IsisRouterType.L1.value());
        defaultIsisInterface.setAreaAddress(areaAddr);
        defaultIsisInterface.setAllConfiguredInterfaceIps(ip4Addresses);
        defaultIsisInterface.setInterfaceIpAddress(ip4Address);
        defaultIsisInterface.setNetworkMask(mask1);
        defaultIsisInterface.processIsisMessage(p2PHelloPdu, isisLsdb, channel);
        assertThat(defaultIsisInterface, is(notNullValue()));
    }

    /**
     * Tests processP2pHelloPduMessage() method.
     */
    @Test(expected = Exception.class)
    public void testProcessP2pHelloPduMessagee3() throws Exception {
        defaultIsisInterface.setSystemId(sysId);
        p2PHelloPdu = new P2PHelloPdu(isisHeader);
        TlvHeader tlvHeader = new TlvHeader();
        tlvHeader.setTlvType(TlvType.ADJACENCYSTATE.value());
        AdjacencyStateTlv adjacencyStateTlv = new AdjacencyStateTlv(tlvHeader);
        adjacencyStateTlv.setNeighborSystemId(sysId);
        adjacencyStateTlv.setAdjacencyType((byte) IsisInterfaceState.DOWN.value());
        p2PHelloPdu.addTlv(adjacencyStateTlv);
        tlvHeader = new TlvHeader();
        tlvHeader.setTlvType(TlvType.AREAADDRESS.value());
        AreaAddressTlv areaAddressTlv = new AreaAddressTlv(tlvHeader);
        areaAddressTlv.addAddress(areaAddr);
        p2PHelloPdu.addTlv(areaAddressTlv);
        p2PHelloPdu.setIsisPduType(IsisPduType.P2PHELLOPDU.value());
        p2PHelloPdu.setSourceMac(macAddress1);
        p2PHelloPdu.setCircuitType((byte) IsisRouterType.L1.value());
        defaultIsisInterface.setNetworkType(IsisNetworkType.P2P);
        defaultIsisInterface.setReservedPacketCircuitType(IsisRouterType.L1.value());
        defaultIsisInterface.setAreaAddress(areaAddr);
        defaultIsisInterface.setAllConfiguredInterfaceIps(ip4Addresses);
        defaultIsisInterface.setInterfaceIpAddress(ip4Address);
        defaultIsisInterface.setNetworkMask(mask1);
        defaultIsisInterface.processIsisMessage(p2PHelloPdu, isisLsdb, channel);
        assertThat(defaultIsisInterface, is(notNullValue()));
    }

    /**
     * Tests processP2pHelloPduMessage() method.
     */
    @Test(expected = Exception.class)
    public void testProcessP2pHelloPduMessagee4() throws Exception {
        defaultIsisInterface.setSystemId(sysId);
        p2PHelloPdu = new P2PHelloPdu(isisHeader);
        TlvHeader tlvHeader = new TlvHeader();
        tlvHeader.setTlvType(TlvType.ADJACENCYSTATE.value());
        AdjacencyStateTlv adjacencyStateTlv = new AdjacencyStateTlv(tlvHeader);
        adjacencyStateTlv.setNeighborSystemId(sysId);
        adjacencyStateTlv.setAdjacencyType((byte) IsisInterfaceState.INITIAL.value());
        p2PHelloPdu.addTlv(adjacencyStateTlv);
        tlvHeader = new TlvHeader();
        tlvHeader.setTlvType(TlvType.AREAADDRESS.value());
        AreaAddressTlv areaAddressTlv = new AreaAddressTlv(tlvHeader);
        areaAddressTlv.addAddress(areaAddr);
        p2PHelloPdu.addTlv(areaAddressTlv);
        p2PHelloPdu.setIsisPduType(IsisPduType.P2PHELLOPDU.value());
        p2PHelloPdu.setSourceMac(macAddress1);
        p2PHelloPdu.setCircuitType((byte) IsisRouterType.L1.value());
        defaultIsisInterface.setNetworkType(IsisNetworkType.P2P);
        defaultIsisInterface.setReservedPacketCircuitType(IsisRouterType.L1L2.value());
        defaultIsisInterface.setAreaAddress(areaAddr);
        defaultIsisInterface.setAllConfiguredInterfaceIps(ip4Addresses);
        defaultIsisInterface.setInterfaceIpAddress(ip4Address);
        defaultIsisInterface.setNetworkMask(mask1);
        defaultIsisInterface.processIsisMessage(p2PHelloPdu, isisLsdb, channel);
        assertThat(defaultIsisInterface, is(notNullValue()));
    }

    @Test(expected = Exception.class)
    public void testProcessP2pHelloPduMessagee5() throws Exception {
        defaultIsisInterface.setSystemId(sysId);
        p2PHelloPdu = new P2PHelloPdu(isisHeader);
        TlvHeader tlvHeader = new TlvHeader();
        tlvHeader.setTlvType(TlvType.ADJACENCYSTATE.value());
        AdjacencyStateTlv adjacencyStateTlv = new AdjacencyStateTlv(tlvHeader);
        adjacencyStateTlv.setNeighborSystemId(sysId);
        adjacencyStateTlv.setAdjacencyType((byte) IsisInterfaceState.UP.value());
        p2PHelloPdu.addTlv(adjacencyStateTlv);
        tlvHeader = new TlvHeader();
        tlvHeader.setTlvType(TlvType.AREAADDRESS.value());
        AreaAddressTlv areaAddressTlv = new AreaAddressTlv(tlvHeader);
        areaAddressTlv.addAddress(areaAddr);
        p2PHelloPdu.addTlv(areaAddressTlv);
        p2PHelloPdu.setIsisPduType(IsisPduType.P2PHELLOPDU.value());
        p2PHelloPdu.setSourceMac(macAddress1);
        p2PHelloPdu.setCircuitType((byte) IsisRouterType.L2.value());
        defaultIsisInterface.setNetworkType(IsisNetworkType.P2P);
        defaultIsisInterface.setReservedPacketCircuitType(IsisRouterType.L1L2.value());
        defaultIsisInterface.setAreaAddress(areaAddr);
        defaultIsisInterface.setAllConfiguredInterfaceIps(ip4Addresses);
        defaultIsisInterface.setInterfaceIpAddress(ip4Address);
        defaultIsisInterface.setNetworkMask(mask1);
        defaultIsisInterface.processIsisMessage(p2PHelloPdu, isisLsdb, channel);
        assertThat(defaultIsisInterface, is(notNullValue()));
    }

    /**
     * Tests startHelloSender() method.
     */
    @Test(expected = Exception.class)
    public void testStartHelloSender() throws Exception {
        defaultIsisInterface.startHelloSender(channel);
        assertThat(defaultIsisInterface, is(notNullValue()));
    }

    /**
     * Tests lspKeyP2P() method.
     */
    @Test
    public void testLspKeyP2P() throws Exception {
        result5 = defaultIsisInterface.lspKeyP2P(sysId);
        assertThat(result5, is(notNullValue()));
    }

    /**
     * Tests processLsPduMessage() method.
     */
    @Test
    public void testProcessLsPduMessage() throws Exception {
        lsPdu = new LsPdu(isisHeader);
        lsPdu.setSourceMac(macAddress1);
        lsPdu.setIsisPduType(IsisPduType.L2LSPDU.value());
        lsPdu.setLspId(sysId);
        isisMessage = lsPdu;
        defaultIsisInterface.setNetworkType(IsisNetworkType.P2P);
        defaultIsisInterface.setSystemId(sysId);
        defaultIsisInterface.processIsisMessage(isisMessage, isisLsdb, channel);
        assertThat(defaultIsisInterface, is(notNullValue()));
    }

    /**
     * Tests processPsnPduMessage() method.
     */
    @Test
    public void testProcessPsnPduMessage() throws Exception {
        psnp = new Psnp(isisHeader);
        psnp.setSourceMac(macAddress1);
        psnp.setIsisPduType(IsisPduType.L2PSNP.value());
        TlvHeader tlvHeader = new TlvHeader();
        tlvHeader.setTlvType(TlvType.LSPENTRY.value());
        tlvHeader.setTlvLength(0);
        LspEntriesTlv lspEntriesTlv = new LspEntriesTlv(tlvHeader);
        LspEntry lspEntry = new LspEntry();
        lspEntry.setLspChecksum(0);
        lspEntry.setLspSequenceNumber(0);
        lspEntry.setRemainingTime(0);
        lspEntriesTlv.addLspEntry(lspEntry);
        psnp.addTlv(lspEntriesTlv);
        isisMessage = psnp;
        defaultIsisInterface.processIsisMessage(isisMessage, isisLsdb, channel);
        assertThat(defaultIsisInterface, is(notNullValue()));
    }

    /**
     * Tests processCsnPduMessage() method.
     */
    @Test(expected = Exception.class)
    public void testProcessCsnPduMessage() throws Exception {
        ChannelBuffer channelBuffer = ChannelBuffers.copiedBuffer(csnpBytes);
        csnp = new Csnp(isisHeader);
        csnp.readFrom(channelBuffer);
        csnp.setSourceMac(macAddress1);
        csnp.setIsisPduType(IsisPduType.L2CSNP.value());
        isisMessage = csnp;
        defaultIsisInterface.processIsisMessage(isisMessage, isisLsdb, channel);
        assertThat(defaultIsisInterface, is(notNullValue()));
    }
}
