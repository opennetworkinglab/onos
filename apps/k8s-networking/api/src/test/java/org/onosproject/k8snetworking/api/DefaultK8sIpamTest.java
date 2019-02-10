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

import static junit.framework.TestCase.assertEquals;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * Unit tests for the default kubernetes IPAM.
 */
public class DefaultK8sIpamTest {

    private static final IpAddress IP_ADDRESS_1 = IpAddress.valueOf("10.10.10.10");
    private static final IpAddress IP_ADDRESS_2 = IpAddress.valueOf("20.20.20.20");
    private static final String NETWORK_ID_1 = "network-1";
    private static final String NETWORK_ID_2 = "network-2";
    private static final String IPAM_ID_1 = NETWORK_ID_1 + "-" + IP_ADDRESS_1.toString();
    private static final String IPAM_ID_2 = NETWORK_ID_2 + "-" + IP_ADDRESS_2.toString();

    private K8sIpam ipam1;
    private K8sIpam sameAsIpam1;
    private K8sIpam ipam2;

    /**
     * Tests class immutability.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(DefaultK8sIpam.class);
    }

    /**
     * Initial setup for this unit test.
     */
    @Before
    public void setUp() {
        ipam1 = new DefaultK8sIpam(IPAM_ID_1, IP_ADDRESS_1, NETWORK_ID_1);
        sameAsIpam1 = new DefaultK8sIpam(IPAM_ID_1, IP_ADDRESS_1, NETWORK_ID_1);
        ipam2 = new DefaultK8sIpam(IPAM_ID_2, IP_ADDRESS_2, NETWORK_ID_2);
    }

    /**
     * Tests object equality.
     */
    @Test
    public void testEquality() {
        new EqualsTester().addEqualityGroup(ipam1, sameAsIpam1)
                .addEqualityGroup(ipam2)
                .testEquals();
    }

    /**
     * Test object construction.
     */
    @Test
    public void testConstruction() {
        K8sIpam ipam = ipam1;

        assertEquals(IPAM_ID_1, ipam.ipamId());
        assertEquals(IP_ADDRESS_1, ipam.ipAddress());
        assertEquals(NETWORK_ID_1, ipam.networkId());
    }
}
