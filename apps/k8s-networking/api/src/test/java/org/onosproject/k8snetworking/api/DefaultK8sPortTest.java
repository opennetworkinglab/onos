/*
 * Copyright 2019-present Open Networking Foundation
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
package org.onosproject.k8snetworking.api;

import com.google.common.testing.EqualsTester;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.k8snetworking.api.K8sPort.State;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertSame;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;
import static org.onosproject.k8snetworking.api.K8sPort.State.ACTIVE;
import static org.onosproject.k8snetworking.api.K8sPort.State.INACTIVE;

/**
 * Unit tests for the default kubernetes port class.
 */
public class DefaultK8sPortTest {

    private static final String NETWORK_ID_1 = "network-1";
    private static final String NETWORK_ID_2 = "network-2";
    private static final String PORT_ID_1 = "port-1";
    private static final String PORT_ID_2 = "port-2";
    private static final MacAddress MAC_ADDRESS_1 = MacAddress.valueOf("00:11:22:33:44:55");
    private static final MacAddress MAC_ADDRESS_2 = MacAddress.valueOf("11:22:33:44:55:66");
    private static final IpAddress IP_ADDRESS_1 = IpAddress.valueOf("10.10.10.10");
    private static final IpAddress IP_ADDRESS_2 = IpAddress.valueOf("20.20.20.20");
    private static final DeviceId DEVICE_ID_1 = DeviceId.deviceId("of:000000000000001");
    private static final DeviceId DEVICE_ID_2 = DeviceId.deviceId("of:000000000000002");
    private static final PortNumber PORT_NUMBER_1 = PortNumber.portNumber(1);
    private static final PortNumber PORT_NUMBER_2 = PortNumber.portNumber(2);
    private static final State STATE_1 = ACTIVE;
    private static final State STATE_2 = INACTIVE;

    private K8sPort port1;
    private K8sPort sameAsPort1;
    private K8sPort port2;

    /**
     * Tests class immutability.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(DefaultK8sPort.class);
    }

    /**
     * Initial setup for this unit test.
     */
    @Before
    public void setUp() {
        port1 = DefaultK8sPort.builder()
                .networkId(NETWORK_ID_1)
                .portId(PORT_ID_1)
                .macAddress(MAC_ADDRESS_1)
                .ipAddress(IP_ADDRESS_1)
                .deviceId(DEVICE_ID_1)
                .portNumber(PORT_NUMBER_1)
                .state(STATE_1)
                .build();

        sameAsPort1 = DefaultK8sPort.builder()
                .networkId(NETWORK_ID_1)
                .portId(PORT_ID_1)
                .macAddress(MAC_ADDRESS_1)
                .ipAddress(IP_ADDRESS_1)
                .deviceId(DEVICE_ID_1)
                .portNumber(PORT_NUMBER_1)
                .state(STATE_1)
                .build();

        port2 = DefaultK8sPort.builder()
                .networkId(NETWORK_ID_2)
                .portId(PORT_ID_2)
                .macAddress(MAC_ADDRESS_2)
                .ipAddress(IP_ADDRESS_2)
                .deviceId(DEVICE_ID_2)
                .portNumber(PORT_NUMBER_2)
                .state(STATE_2)
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
        K8sPort port = port1;

        assertEquals(NETWORK_ID_1, port.networkId());
        assertEquals(PORT_ID_1, port.portId());
        assertEquals(MAC_ADDRESS_1, port.macAddress());
        assertEquals(IP_ADDRESS_1, port.ipAddress());
        assertEquals(DEVICE_ID_1, port.deviceId());
        assertEquals(PORT_NUMBER_1, port.portNumber());
        assertSame(STATE_1, port.state());
    }
}
