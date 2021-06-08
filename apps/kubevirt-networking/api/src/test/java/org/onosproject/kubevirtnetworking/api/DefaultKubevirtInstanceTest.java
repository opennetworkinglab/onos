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

import static junit.framework.TestCase.assertEquals;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * Unit tests for the default kubevirt instance class.
 */
public class DefaultKubevirtInstanceTest {

    private static final String UID_1 = "1";
    private static final String UID_2 = "2";
    private static final String NAME_1 = "instance-1";
    private static final String NAME_2 = "instance-2";
    private static final String NETWORK_ID_1 = "net-1";
    private static final String NETWORK_ID_2 = "net-2";
    private static final String VM_NAME_1 = "test-vm-1";
    private static final String VM_NAME_2 = "test-vm-2";
    private static final MacAddress MAC_1 = MacAddress.valueOf("11:22:33:44:55:66");
    private static final MacAddress MAC_2 = MacAddress.valueOf("66:55:44:33:22:11");
    private static final IpAddress IP_1 = IpAddress.valueOf("10.10.10.10");
    private static final IpAddress IP_2 = IpAddress.valueOf("20.20.20.20");
    private static final DeviceId DID_1 = DeviceId.deviceId("did1");
    private static final DeviceId DID_2 = DeviceId.deviceId("did2");
    private static final PortNumber PN_1 = PortNumber.portNumber(1);
    private static final PortNumber PN_2 = PortNumber.portNumber(2);
    private static final KubevirtPort PORT_1 = createPort(VM_NAME_1, NETWORK_ID_1, MAC_1, IP_1, DID_1, PN_1);
    private static final KubevirtPort PORT_2 = createPort(VM_NAME_2, NETWORK_ID_2, MAC_2, IP_2, DID_2, PN_2);

    private KubevirtInstance instance1;
    private KubevirtInstance sameAsInstance1;
    private KubevirtInstance instance2;

    /**
     * Tests class immutability.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(DefaultKubevirtInstance.class);
    }

    /**
     * Initial setup for this unit test.
     */
    @Before
    public void setUp() {
        instance1 = DefaultKubevirtInstance.builder()
                .uid(UID_1)
                .name(NAME_1)
                .ports(ImmutableSet.of(PORT_1))
                .build();

        sameAsInstance1 = DefaultKubevirtInstance.builder()
                .uid(UID_1)
                .name(NAME_1)
                .ports(ImmutableSet.of(PORT_1))
                .build();

        instance2 = DefaultKubevirtInstance.builder()
                .uid(UID_2)
                .name(NAME_2)
                .ports(ImmutableSet.of(PORT_2))
                .build();
    }

    /**
     * Tests object equality.
     */
    @Test
    public void testEquality() {
        new EqualsTester().addEqualityGroup(instance1, sameAsInstance1)
                .addEqualityGroup(instance2)
                .testEquals();
    }

    /**
     * Test object construction.
     */
    @Test
    public void testConstruction() {
        KubevirtInstance instance = instance1;

        assertEquals(UID_1, instance1.uid());
        assertEquals(NAME_1, instance1.name());
        assertEquals(ImmutableSet.of(PORT_1), instance1.ports());
    }

    static KubevirtPort createPort(String vmName, String networkId, MacAddress mac,
                                    IpAddress ip, DeviceId did, PortNumber pn) {
        return DefaultKubevirtPort.builder()
                .vmName(vmName)
                .networkId(networkId)
                .macAddress(mac)
                .ipAddress(ip)
                .deviceId(did)
                .portNumber(pn)
                .build();
    }
}
