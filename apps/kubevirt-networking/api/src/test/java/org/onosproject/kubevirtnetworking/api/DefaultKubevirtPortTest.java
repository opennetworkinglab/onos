/*
 * Copyright 2021-present Open Networking Foundation
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
package org.onosproject.kubevirtnetworking.api;

import com.google.common.collect.ImmutableSet;
import com.google.common.testing.EqualsTester;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

import java.util.Set;

import static junit.framework.TestCase.assertEquals;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * Unit tests for the default kubevirt port class.
 */
public class DefaultKubevirtPortTest {
    private static final String VM_NAME_1 = "test-vm-1";
    private static final String VM_NAME_2 = "test-vm-2";
    private static final String NETWORK_ID_1 = "net-1";
    private static final String NETWORK_ID_2 = "net-2";
    private static final MacAddress MAC_ADDRESS_1 = MacAddress.valueOf("00:11:22:33:44:55");
    private static final MacAddress MAC_ADDRESS_2 = MacAddress.valueOf("11:22:33:44:55:66");
    private static final IpAddress IP_ADDRESS_1 = IpAddress.valueOf("10.10.10.10");
    private static final IpAddress IP_ADDRESS_2 = IpAddress.valueOf("20.20.20.20");
    private static final DeviceId DEVICE_ID_1 = DeviceId.deviceId("of:000000000000001");
    private static final DeviceId DEVICE_ID_2 = DeviceId.deviceId("of:000000000000002");
    private static final PortNumber PORT_NUMBER_1 = PortNumber.portNumber(1);
    private static final PortNumber PORT_NUMBER_2 = PortNumber.portNumber(2);
    private static final Set<String> SGS_1 = ImmutableSet.of("1");
    private static final Set<String> SGS_2 = ImmutableSet.of("2");

    private KubevirtPort port1;
    private KubevirtPort sameAsPort1;
    private KubevirtPort port2;

    /**
     * Tests class immutability.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(DefaultKubevirtPort.class);
    }

    /**
     * Initial setup for this unit test.
     */
    @Before
    public void setUp() {
        port1 = DefaultKubevirtPort.builder()
                .vmName(VM_NAME_1)
                .networkId(NETWORK_ID_1)
                .macAddress(MAC_ADDRESS_1)
                .ipAddress(IP_ADDRESS_1)
                .deviceId(DEVICE_ID_1)
                .portNumber(PORT_NUMBER_1)
                .securityGroups(SGS_1)
                .build();

        sameAsPort1 = DefaultKubevirtPort.builder()
                .vmName(VM_NAME_1)
                .networkId(NETWORK_ID_1)
                .macAddress(MAC_ADDRESS_1)
                .ipAddress(IP_ADDRESS_1)
                .deviceId(DEVICE_ID_1)
                .portNumber(PORT_NUMBER_1)
                .securityGroups(SGS_1)
                .build();

        port2 = DefaultKubevirtPort.builder()
                .vmName(VM_NAME_2)
                .networkId(NETWORK_ID_2)
                .macAddress(MAC_ADDRESS_2)
                .ipAddress(IP_ADDRESS_2)
                .deviceId(DEVICE_ID_2)
                .portNumber(PORT_NUMBER_2)
                .securityGroups(SGS_2)
                .build();
    }

    /**
     * Tests object equality.
     */
    @Test
    public void testEquality() {
        new EqualsTester().addEqualityGroup(port1, sameAsPort1)
                .addEqualityGroup(port2)
                .testEquals();
    }

    /**
     * Test object construction.
     */
    @Test
    public void testConstruction() {
        KubevirtPort port = port1;

        assertEquals(VM_NAME_1, port.vmName());
        assertEquals(NETWORK_ID_1, port.networkId());
        assertEquals(MAC_ADDRESS_1, port.macAddress());
        assertEquals(IP_ADDRESS_1, port.ipAddress());
        assertEquals(DEVICE_ID_1, port.deviceId());
        assertEquals(PORT_NUMBER_1, port.portNumber());
        assertEquals(SGS_1, port.securityGroups());
    }
}
