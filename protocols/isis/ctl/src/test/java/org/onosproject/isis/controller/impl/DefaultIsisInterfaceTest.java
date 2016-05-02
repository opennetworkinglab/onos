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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.MacAddress;
import org.onosproject.isis.controller.IsisInterface;
import org.onosproject.isis.controller.IsisInterfaceState;
import org.onosproject.isis.controller.IsisLsdb;
import org.onosproject.isis.controller.IsisNeighbor;
import org.onosproject.isis.controller.IsisNetworkType;
import org.onosproject.isis.io.isispacket.IsisHeader;
import org.onosproject.isis.io.isispacket.pdu.HelloPdu;
import org.onosproject.isis.io.isispacket.pdu.L1L2HelloPdu;

import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Unit test case for DefaultIsisInterface.
 */
public class DefaultIsisInterfaceTest {
    private final MacAddress macAddress = MacAddress.valueOf("AA:BB:CC:DD:EE:FF");
    private final Ip4Address ip4Address = Ip4Address.valueOf("10.10.10.10");
    private final byte[] mask = {
            (byte) 255, (byte) 255, (byte) 255, (byte) 224
    };
    private final String intSysName = "ROUTER";
    private final String sysId = "1111.1111.1111";
    private final String areaAddr = "49.002";
    private IsisInterfaceState resultIfState;
    private DefaultIsisInterface defaultIsisInterface;
    private HelloPdu helloPdu;
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


    @Before
    public void setUp() throws Exception {
        defaultIsisInterface = new DefaultIsisInterface();
        isisHeader = new IsisHeader();
        isisHeader.setIrpDiscriminator((byte) 1);
        helloPdu = new L1L2HelloPdu(isisHeader);
        isisInterface = new DefaultIsisInterface();
        resultNeighborList = new DefaultIsisNeighbor(helloPdu, isisInterface);


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
     * Tests getLspId() getter method.
     */
    @Test
    public void testGetLspId() throws Exception {
        defaultIsisInterface.setLspId(sysId);
        resultStr = defaultIsisInterface.getLspId();
        assertThat(resultStr, is(sysId));
    }

    /**
     * Tests getLspId() setter method.
     */
    @Test
    public void testSetLspId() throws Exception {
        defaultIsisInterface.setLspId(sysId);
        resultStr = defaultIsisInterface.getLspId();
        assertThat(resultStr, is(sysId));
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
}
