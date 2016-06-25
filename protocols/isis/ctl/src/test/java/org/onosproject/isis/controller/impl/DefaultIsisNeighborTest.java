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
import org.onosproject.isis.controller.IsisMessage;
import org.onosproject.isis.controller.IsisRouterType;
import org.onosproject.isis.io.isispacket.IsisHeader;
import org.onosproject.isis.io.isispacket.pdu.HelloPdu;
import org.onosproject.isis.io.isispacket.pdu.L1L2HelloPdu;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Unit test class for DefaultIsisNeighbor.
 */
public class DefaultIsisNeighborTest {

    private final String areaId = "490001";
    private final String systemId = "2929.2929.2929";
    private final String lanId = "0000.0000.0000.00";
    private DefaultIsisNeighbor isisNeighbor;
    private IsisInterface isisInterface;
    private IsisMessage isisMessage;
    private IsisHeader isisHeader;
    private int result;
    private String result1;
    private Ip4Address interfaceIp = Ip4Address.valueOf("10.10.10.10");
    private Ip4Address result2;
    private MacAddress macAddress = MacAddress.valueOf("a4:23:05:00:00:00");
    private MacAddress result3;
    private IsisRouterType isisRouterType;
    private IsisInterfaceState isisInterfaceState;
    private byte result4;

    @Before
    public void setUp() throws Exception {
        isisHeader = new IsisHeader();
        isisMessage = new L1L2HelloPdu(isisHeader);
        isisInterface = new DefaultIsisInterface();
        isisNeighbor = new DefaultIsisNeighbor((HelloPdu) isisMessage, isisInterface);
    }

    @After
    public void tearDown() throws Exception {
        isisHeader = null;
        isisMessage = null;
        isisInterface = null;
        isisNeighbor = null;
    }

    /**
     * Tests localExtendedCircuitId() getter method.
     */
    @Test
    public void testLocalExtendedCircuitId() throws Exception {
        isisNeighbor.setLocalExtendedCircuitId(1);
        result = isisNeighbor.localExtendedCircuitId();
        assertThat(result, is(1));
    }

    /**
     * Tests localExtendedCircuitId() setter method.
     */
    @Test
    public void testSetLocalExtendedCircuitId() throws Exception {
        isisNeighbor.setLocalExtendedCircuitId(1);
        result = isisNeighbor.localExtendedCircuitId();
        assertThat(result, is(1));
    }

    /**
     * Tests neighborAreaId() getter method.
     */
    @Test
    public void testNeighborAreaId() throws Exception {
        isisNeighbor.setNeighborAreaId(areaId);
        result1 = isisNeighbor.neighborAreaId();
        assertThat(result1, is(areaId));
    }

    /**
     * Tests neighborAreaId() setter method.
     */
    @Test
    public void testSetNeighborAreaId() throws Exception {
        isisNeighbor.setNeighborAreaId(areaId);
        result1 = isisNeighbor.neighborAreaId();
        assertThat(result1, is(areaId));
    }

    /**
     * Tests neighborSystemId() getter method.
     */
    @Test
    public void testNeighborSystemId() throws Exception {
        isisNeighbor.setNeighborSystemId(systemId);
        result1 = isisNeighbor.neighborSystemId();
        assertThat(result1, is(systemId));
    }

    /**
     * Tests neighborSystemId() setter method.
     */
    @Test
    public void testSetNeighborSystemId() throws Exception {
        isisNeighbor.setNeighborSystemId(systemId);
        result1 = isisNeighbor.neighborSystemId();
        assertThat(result1, is(systemId));
    }

    /**
     * Tests interfaceIp() getter method.
     */
    @Test
    public void testInterfaceIp() throws Exception {
        isisNeighbor.setInterfaceIp(interfaceIp);
        result2 = isisNeighbor.interfaceIp();
        assertThat(result2, is(interfaceIp));
    }

    /**
     * Tests interfaceIp() setter method.
     */
    @Test
    public void testSetInterfaceIp() throws Exception {
        isisNeighbor.setInterfaceIp(interfaceIp);
        result2 = isisNeighbor.interfaceIp();
        assertThat(result2, is(interfaceIp));
    }

    /**
     * Tests neighborMacAddress() getter method.
     */
    @Test
    public void testNeighborMacAddress() throws Exception {
        isisNeighbor.setNeighborMacAddress(macAddress);
        result3 = isisNeighbor.neighborMacAddress();
        assertThat(result3, is(macAddress));
    }

    /**
     * Tests neighborMacAddress() setter method.
     */
    @Test
    public void testSetNeighborMacAddress() throws Exception {
        isisNeighbor.setNeighborMacAddress(macAddress);
        result3 = isisNeighbor.neighborMacAddress();
        assertThat(result3, is(macAddress));
    }

    /**
     * Tests holdingTime() getter method.
     */
    @Test
    public void testHoldingTime() throws Exception {
        isisNeighbor.setHoldingTime(1);
        result = isisNeighbor.holdingTime();
        assertThat(result, is(1));
    }

    /**
     * Tests holdingTime() setter method.
     */
    @Test
    public void testSetHoldingTime() throws Exception {
        isisNeighbor.setHoldingTime(1);
        result = isisNeighbor.holdingTime();
        assertThat(result, is(1));
    }

    /**
     * Tests routerType() getter method.
     */
    @Test
    public void testRouterType() throws Exception {
        isisNeighbor.setRouterType(IsisRouterType.L1);
        isisRouterType = isisNeighbor.routerType();
        assertThat(isisRouterType, is(IsisRouterType.L1));
    }

    /**
     * Tests routerType() setter method.
     */
    @Test
    public void testSetRouterType() throws Exception {
        isisNeighbor.setRouterType(IsisRouterType.L1);
        isisRouterType = isisNeighbor.routerType();
        assertThat(isisRouterType, is(IsisRouterType.L1));
    }

    /**
     * Tests l1LanId() getter method.
     */
    @Test
    public void testL1LanId() throws Exception {
        isisNeighbor.setL1LanId(systemId);
        result1 = isisNeighbor.l1LanId();
        assertThat(result1, is(systemId));
    }

    /**
     * Tests l1LanId() setter method.
     */
    @Test
    public void testSetL1LanId() throws Exception {
        isisNeighbor.setL1LanId(lanId);
        result1 = isisNeighbor.l1LanId();
        assertThat(result1, is(lanId));
    }

    /**
     * Tests l2LanId() getter method.
     */
    @Test
    public void testL2LanId() throws Exception {
        isisNeighbor.setL2LanId(lanId);
        result1 = isisNeighbor.l2LanId();
        assertThat(result1, is(lanId));
    }

    /**
     * Tests l2LanId() setter method.
     */
    @Test
    public void testSetL2LanId() throws Exception {
        isisNeighbor.setL2LanId(lanId);
        result1 = isisNeighbor.l2LanId();
        assertThat(result1, is(lanId));
    }

    /**
     * Tests neighborState() getter method.
     */
    @Test
    public void testInterfaceState() throws Exception {
        isisNeighbor.setNeighborState(IsisInterfaceState.DOWN);
        isisInterfaceState = isisNeighbor.neighborState();
        assertThat(isisInterfaceState, is(IsisInterfaceState.DOWN));
    }

    /**
     * Tests neighborState() setter method.
     */
    @Test
    public void testSetNeighborState() throws Exception {
        isisNeighbor.setNeighborState(IsisInterfaceState.DOWN);
        isisInterfaceState = isisNeighbor.neighborState();
        assertThat(isisInterfaceState, is(IsisInterfaceState.DOWN));
    }

    /**
     * Tests localCircuitId() getter method.
     */
    @Test
    public void testLocalCircuitId() throws Exception {
        isisNeighbor.setLocalCircuitId((byte) 1);
        result4 = isisNeighbor.localCircuitId();
        assertThat(result4, is((byte) 1));
    }

    /**
     * Tests localCircuitId() setter method.
     */
    @Test
    public void testSetLocalCircuitId() throws Exception {
        isisNeighbor.setLocalCircuitId((byte) 1);
        result4 = isisNeighbor.localCircuitId();
        assertThat(result4, is((byte) 1));
    }

    /**
     * Tests neighborState() getter method.
     */
    @Test
    public void testNeighborState() throws Exception {
        isisNeighbor.setNeighborState(IsisInterfaceState.DOWN);
        isisInterfaceState = isisNeighbor.neighborState();
        assertThat(isisInterfaceState, is(IsisInterfaceState.DOWN));
    }

    /**
     * Tests startHoldingTimeCheck() method.
     */
    @Test
    public void testStartHoldingTimeCheck() throws Exception {
        isisNeighbor.startHoldingTimeCheck();
        assertThat(isisNeighbor, is(notNullValue()));
    }

    /**
     * Tests stopHoldingTimeCheck() method.
     */
    @Test
    public void testStopHoldingTimeCheck() throws Exception {
        isisNeighbor.stopHoldingTimeCheck();
        assertThat(isisNeighbor, is(notNullValue()));
    }

    /**
     * Tests startInactivityTimeCheck() method.
     */
    @Test(expected = Exception.class)
    public void testStartInactivityTimeCheck() throws Exception {
        isisNeighbor.startInactivityTimeCheck();
        assertThat(isisNeighbor, is(notNullValue()));
    }

    /**
     * Tests startInactivityTimeCheck() method.
     */
    @Test(expected = Exception.class)
    public void testStopInactivityTimeCheck() throws Exception {
        isisNeighbor.startInactivityTimeCheck();
        assertThat(isisNeighbor, is(notNullValue()));
    }

    /**
     * Tests neighborDown() method.
     */
    @Test(expected = Exception.class)
    public void testNeighborDown() throws Exception {
        isisNeighbor.neighborDown();
        assertThat(isisNeighbor, is(notNullValue()));
    }
}