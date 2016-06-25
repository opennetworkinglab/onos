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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Unit test class for DeviceInformationImpl.
 */
public class DeviceInformationImplTest {

    private DeviceInformationImpl deviceInformation;

    @Before
    public void setUp() throws Exception {
        deviceInformation = new DeviceInformationImpl();
    }

    @After
    public void tearDown() throws Exception {
        deviceInformation = null;
    }

    /**
     * Tests routerId() getter method.
     */
    @Test
    public void testRouterId() throws Exception {
        deviceInformation.setRouterId(Ip4Address.valueOf("1.1.1.1"));
        assertThat(deviceInformation.routerId(), is(Ip4Address.valueOf("1.1.1.1")));
    }

    /**
     * Tests routerId() setter method.
     */
    @Test
    public void testSetRouterId() throws Exception {
        deviceInformation.setRouterId(Ip4Address.valueOf("1.1.1.1"));
        assertThat(deviceInformation.routerId(), is(Ip4Address.valueOf("1.1.1.1")));
    }

    /**
     * Tests deviceId() getter method.
     */
    @Test
    public void testDeviceId() throws Exception {
        deviceInformation.setDeviceId(Ip4Address.valueOf("1.1.1.1"));
        assertThat(deviceInformation.deviceId(), is(Ip4Address.valueOf("1.1.1.1")));
    }

    /**
     * Tests deviceId() getter method.
     */
    @Test
    public void testSetDeviceId() throws Exception {
        deviceInformation.setDeviceId(Ip4Address.valueOf("1.1.1.1"));
        assertThat(deviceInformation.deviceId(), is(Ip4Address.valueOf("1.1.1.1")));
    }

    /**
     * Tests interfaceId() method.
     */
    @Test
    public void testInterfaceId() throws Exception {
        deviceInformation.addInterfaceId(Ip4Address.valueOf("1.1.1.1"));
        assertThat(deviceInformation.interfaceId().size(), is(1));
    }

    /**
     * Tests addInterfaceId() method.
     */
    @Test
    public void testAddInterfaceId() throws Exception {
        deviceInformation.addInterfaceId(Ip4Address.valueOf("1.1.1.1"));
        assertThat(deviceInformation.interfaceId().size(), is(1));
    }

    /**
     * Tests areaId() getter method.
     */
    @Test
    public void testAreaId() throws Exception {
        deviceInformation.setAreaId(Ip4Address.valueOf("1.1.1.1"));
        assertThat(deviceInformation.areaId(), is(Ip4Address.valueOf("1.1.1.1")));
    }

    /**
     * Tests areaId() setter method.
     */
    @Test
    public void testSetAreaId() throws Exception {
        deviceInformation.setAreaId(Ip4Address.valueOf("1.1.1.1"));
        assertThat(deviceInformation.areaId(), is(Ip4Address.valueOf("1.1.1.1")));
    }

    /**
     * Tests isAlreadyCreated() getter method.
     */
    @Test
    public void testIsAlreadyCreated() throws Exception {
        deviceInformation.setAlreadyCreated(true);
        assertThat(deviceInformation.isAlreadyCreated(), is(true));
    }

    /**
     * Tests isAlreadyCreated() setter method.
     */
    @Test
    public void testSetAlreadyCreated() throws Exception {
        deviceInformation.setAlreadyCreated(true);
        assertThat(deviceInformation.isAlreadyCreated(), is(true));
    }

    /**
     * Tests isDr() getter method.
     */
    @Test
    public void testIsDr() throws Exception {
        deviceInformation.setDr(true);
        assertThat(deviceInformation.isDr(), is(true));
    }

    /**
     * Tests isDr() setter method.
     */
    @Test
    public void testSetDr() throws Exception {
        deviceInformation.setDr(true);
        assertThat(deviceInformation.isDr(), is(true));
    }

    /**
     * Tests neighborId() getter method.
     */
    @Test
    public void testNeighborId() throws Exception {
        deviceInformation.setNeighborId(Ip4Address.valueOf("1.1.1.1"));
        assertThat(deviceInformation.neighborId(), is(Ip4Address.valueOf("1.1.1.1")));
    }

    /**
     * Tests neighborId() setter method.
     */
    @Test
    public void testSetNeighborId() throws Exception {
        deviceInformation.setNeighborId(Ip4Address.valueOf("1.1.1.1"));
        assertThat(deviceInformation.neighborId(), is(Ip4Address.valueOf("1.1.1.1")));
    }
}