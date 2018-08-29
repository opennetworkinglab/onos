/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.simplefabric.impl;

import com.google.common.testing.EqualsTester;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onosproject.net.EncapsulationType;
import org.onosproject.simplefabric.api.FabricSubnet;

import static junit.framework.TestCase.assertEquals;

/**
 * Unit tests for the default fabric IP subnet class.
 */
public final class DefaultFabricSubnetTest {

    private static final IpPrefix IP_PREFIX_1 = IpPrefix.valueOf("10.10.10.11/32");
    private static final IpPrefix IP_PREFIX_2 = IpPrefix.valueOf("20.20.20.11/32");

    private static final IpAddress GATEWAY_IP_1 = IpAddress.valueOf("10.10.10.1");
    private static final IpAddress GATEWAY_IP_2 = IpAddress.valueOf("20.20.20.1");

    private static final MacAddress GATEWAY_MAC_1 = MacAddress.valueOf("00:11:22:33:44:55");
    private static final MacAddress GATEWAY_MAC_2 = MacAddress.valueOf("11:22:33:44:55:66");

    private static final EncapsulationType ENCAP_TYPE_1 = EncapsulationType.NONE;
    private static final EncapsulationType ENCAP_TYPE_2 = EncapsulationType.NONE;

    private static final String NETWORK_NAME_1 = "sonaFabric1";
    private static final String NETWORK_NAME_2 = "sonaFabric2";

    private FabricSubnet subnet1;
    private FabricSubnet sameAsSubnet1;
    private FabricSubnet subnet2;

    /**
     * Initial setup for this unit test.
     */
    @Before
    public void setUp() {

        subnet1 = DefaultFabricSubnet.builder()
                            .prefix(IP_PREFIX_1)
                            .gatewayIp(GATEWAY_IP_1)
                            .gatewayMac(GATEWAY_MAC_1)
                            .encapsulation(ENCAP_TYPE_1)
                            .networkName(NETWORK_NAME_1)
                            .build();

        sameAsSubnet1 = DefaultFabricSubnet.builder()
                            .prefix(IP_PREFIX_1)
                            .gatewayIp(GATEWAY_IP_1)
                            .gatewayMac(GATEWAY_MAC_1)
                            .encapsulation(ENCAP_TYPE_1)
                            .networkName(NETWORK_NAME_1)
                            .build();

        subnet2 = DefaultFabricSubnet.builder()
                            .prefix(IP_PREFIX_2)
                            .gatewayIp(GATEWAY_IP_2)
                            .gatewayMac(GATEWAY_MAC_2)
                            .encapsulation(ENCAP_TYPE_2)
                            .networkName(NETWORK_NAME_2)
                            .build();
    }

    /**
     * Tests object equality.
     */
    @Test
    public void testEquality() {
        new EqualsTester().addEqualityGroup(subnet1, sameAsSubnet1)
                .addEqualityGroup(subnet2)
                .testEquals();
    }

    /**
     * Test object construction.
     */
    @Test
    public void testConstruction() {
        FabricSubnet subnet = subnet1;

        assertEquals(subnet.prefix(), IP_PREFIX_1);
        assertEquals(subnet.gatewayIp(), GATEWAY_IP_1);
        assertEquals(subnet.gatewayMac(), GATEWAY_MAC_1);
        assertEquals(subnet.encapsulation(), ENCAP_TYPE_1);
        assertEquals(subnet.networkName(), NETWORK_NAME_1);
    }
}
