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

import com.google.common.testing.EqualsTester;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;

import static junit.framework.TestCase.assertEquals;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * Unit tests for the default kubevirt floating IP class.
 */
public class DefaultKubevirtFloatingIpTest {

    private static final String ID_1 = "fip_id_1";
    private static final String ID_2 = "fip_id_2";
    private static final String ROUTER_NAME_1 = "router-1";
    private static final String ROUTER_NAME_2 = "router-2";
    private static final String NETWORK_NAME_1 = "flat-1";
    private static final String NETWORK_NAME_2 = "flat-2";
    private static final IpAddress FLOATING_IP_1 = IpAddress.valueOf("10.10.10.10");
    private static final IpAddress FLOATING_IP_2 = IpAddress.valueOf("20.20.20.20");
    private static final String POD_NAME_1 = "pod-1";
    private static final String POD_NAME_2 = "pod-2";
    private static final String VM_NAME_1 = "vm-1";
    private static final String VM_NAME_2 = "vm-2";
    private static final IpAddress FIXED_IP_1 = IpAddress.valueOf("30.30.30.30");
    private static final IpAddress FIXED_IP_2 = IpAddress.valueOf("40.40.40.40");

    private KubevirtFloatingIp fip1;
    private KubevirtFloatingIp sameAsFip1;
    private KubevirtFloatingIp fip2;

    /**
     * Tests class immutability.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(DefaultKubevirtFloatingIp.class);
    }

    /**
     * Initial setup for this unit test.
     */
    @Before
    public void setUp() {
        fip1 = DefaultKubevirtFloatingIp.builder()
                .id(ID_1)
                .routerName(ROUTER_NAME_1)
                .networkName(NETWORK_NAME_1)
                .floatingIp(FLOATING_IP_1)
                .podName(POD_NAME_1)
                .vmName(VM_NAME_1)
                .fixedIp(FIXED_IP_1)
                .build();

        sameAsFip1 = DefaultKubevirtFloatingIp.builder()
                .id(ID_1)
                .routerName(ROUTER_NAME_1)
                .networkName(NETWORK_NAME_1)
                .floatingIp(FLOATING_IP_1)
                .podName(POD_NAME_1)
                .vmName(VM_NAME_1)
                .fixedIp(FIXED_IP_1)
                .build();

        fip2 = DefaultKubevirtFloatingIp.builder()
                .id(ID_2)
                .routerName(ROUTER_NAME_2)
                .networkName(NETWORK_NAME_2)
                .floatingIp(FLOATING_IP_2)
                .podName(POD_NAME_2)
                .vmName(VM_NAME_2)
                .fixedIp(FIXED_IP_2)
                .build();
    }

    /**
     * Tests object equality.
     */
    @Test
    public void testEquality() {
        new EqualsTester().addEqualityGroup(fip1, sameAsFip1)
                .addEqualityGroup(fip2)
                .testEquals();
    }

    /**
     * Test object construction.
     */
    @Test
    public void testConstruction() {
        KubevirtFloatingIp fip = fip1;

        assertEquals(ID_1, fip.id());
        assertEquals(ROUTER_NAME_1, fip.routerName());
        assertEquals(NETWORK_NAME_1, fip.networkName());
        assertEquals(FLOATING_IP_1, fip.floatingIp());
        assertEquals(POD_NAME_1, fip.podName());
        assertEquals(VM_NAME_1, fip.vmName());
        assertEquals(FIXED_IP_1, fip.fixedIp());
    }
}
