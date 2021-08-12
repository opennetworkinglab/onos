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

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;
import static org.onosproject.kubevirtnetworking.api.KubevirtNetwork.Type.FLAT;
import static org.onosproject.kubevirtnetworking.api.KubevirtNetwork.Type.VXLAN;

/**
 * Unit tests for the default kubevirt network class.
 */
public class DefaultKubevirtNetworkTest {
    private static final String NETWORK_ID_1 = "net-1";
    private static final String NETWORK_ID_2 = "net-2";
    private static final KubevirtNetwork.Type TYPE_1 = FLAT;
    private static final KubevirtNetwork.Type TYPE_2 = VXLAN;
    private static final String NAME_1 = "net-1";
    private static final String NAME_2 = "net-2";
    private static final Integer MTU_1 = 1500;
    private static final Integer MTU_2 = 1400;
    private static final String SEGMENT_ID_1 = null;
    private static final String SEGMENT_ID_2 = "2";
    private static final IpAddress GATEWAY_IP_1 = IpAddress.valueOf("10.10.10.1");
    private static final IpAddress GATEWAY_IP_2 = IpAddress.valueOf("20.20.20.1");
    private static final boolean DEFAULT_ROUTE_1 = true;
    private static final boolean DEFAULT_ROUTE_2 = false;
    private static final String CIDR_1 = "10.10.10.0/24";
    private static final String CIDR_2 = "20.20.20.0/24";
    private static final IpAddress IP_POOL_START_1 = IpAddress.valueOf("10.10.10.100");
    private static final IpAddress IP_POOL_START_2 = IpAddress.valueOf("20.20.20.100");
    private static final IpAddress IP_POOL_END_1 = IpAddress.valueOf("10.10.10.200");
    private static final IpAddress IP_POOL_END_2 = IpAddress.valueOf("20.20.20.200");
    private static final IpAddress DNS_1 = IpAddress.valueOf("8.8.8.8");
    private static final IpAddress DNS_2 = IpAddress.valueOf("8.8.4.4");

    private KubevirtNetwork network1;
    private KubevirtNetwork sameAsNetwork1;
    private KubevirtNetwork network2;

    /**
     * Tests class immutability.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(DefaultKubevirtNetwork.class);
    }

    /**
     * Initial setup for this unit test.
     */
    @Before
    public void setUp() {
        network1 = DefaultKubevirtNetwork.builder()
                .networkId(NETWORK_ID_1)
                .name(NAME_1)
                .type(TYPE_1)
                .mtu(MTU_1)
                .segmentId(SEGMENT_ID_1)
                .gatewayIp(GATEWAY_IP_1)
                .defaultRoute(DEFAULT_ROUTE_1)
                .cidr(CIDR_1)
                .ipPool(new KubevirtIpPool(IP_POOL_START_1, IP_POOL_END_1))
                .hostRoutes(ImmutableSet.of())
                .dnses(ImmutableSet.of(DNS_1))
                .build();

        sameAsNetwork1 = DefaultKubevirtNetwork.builder()
                .networkId(NETWORK_ID_1)
                .name(NAME_1)
                .type(TYPE_1)
                .mtu(MTU_1)
                .segmentId(SEGMENT_ID_1)
                .gatewayIp(GATEWAY_IP_1)
                .defaultRoute(DEFAULT_ROUTE_1)
                .cidr(CIDR_1)
                .ipPool(new KubevirtIpPool(IP_POOL_START_1, IP_POOL_END_1))
                .hostRoutes(ImmutableSet.of())
                .dnses(ImmutableSet.of(DNS_1))
                .build();

        network2 = DefaultKubevirtNetwork.builder()
                .networkId(NETWORK_ID_2)
                .name(NAME_2)
                .type(TYPE_2)
                .mtu(MTU_2)
                .segmentId(SEGMENT_ID_2)
                .gatewayIp(GATEWAY_IP_2)
                .defaultRoute(DEFAULT_ROUTE_2)
                .cidr(CIDR_2)
                .ipPool(new KubevirtIpPool(IP_POOL_START_2, IP_POOL_END_2))
                .hostRoutes(ImmutableSet.of())
                .dnses(ImmutableSet.of(DNS_2))
                .build();
    }

    /**
     * Tests object equality.
     */
    @Test
    public void testEquality() {
        new EqualsTester().addEqualityGroup(network1, sameAsNetwork1)
                .addEqualityGroup(network2)
                .testEquals();
    }

    /**
     * Test object construction.
     */
    @Test
    public void testConstruction() {
        KubevirtNetwork network = network1;

        assertEquals(NETWORK_ID_1, network.networkId());
        assertEquals(TYPE_1, network.type());
        assertEquals(NAME_1, network.name());
        assertEquals(MTU_1, network.mtu());
        assertEquals(GATEWAY_IP_1, network.gatewayIp());
        assertEquals(DEFAULT_ROUTE_1, network.defaultRoute());
        assertEquals(CIDR_1, network.cidr());
        assertEquals(new KubevirtIpPool(IP_POOL_START_1, IP_POOL_END_1), network.ipPool());
        assertEquals(ImmutableSet.of(DNS_1), network.dnses());
    }

    /**
     * Test IP address initialization.
     */
    @Test
    public void testIpInitialization() {
        KubevirtIpPool ipPool1 = network1.ipPool();
        assertEquals(101, ipPool1.availableIps().size());
        assertEquals(0, ipPool1.allocatedIps().size());
    }

    /**
     * Test IP address allocation.
     */
    @Test
    public void testIpAllocationAndRelease() throws Exception {
        KubevirtIpPool ipPool1 = network1.ipPool();
        IpAddress ip = ipPool1.allocateIp();
        assertEquals(100, ipPool1.availableIps().size());
        assertEquals(1, ipPool1.allocatedIps().size());
        assertEquals(IpAddress.valueOf("10.10.10.100"), ip);

        ipPool1.releaseIp(ip);
        assertEquals(101, ipPool1.availableIps().size());
        assertEquals(0, ipPool1.allocatedIps().size());
        assertTrue(ipPool1.availableIps().contains(ip));
    }
}
