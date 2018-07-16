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
package org.onosproject.openstacknetworking.impl;

import com.google.common.testing.EqualsTester;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.openstacknetworking.api.ExternalPeerRouter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * Unit tests for DefaultExternalPeerRouter.
 */
public class DefaultExternalPeerRouterTest {

    private static final IpAddress ROUTER_IP_1 = IpAddress.valueOf("1.2.3.4");
    private static final IpAddress ROUTER_IP_2 = IpAddress.valueOf("5.6.7.8");
    private static final MacAddress ROUTER_MAC_1 = MacAddress.valueOf("11:22:33:44:55:66");
    private static final MacAddress ROUTER_MAC_2 = MacAddress.valueOf("77:88:99:AA:BB:CC");
    private static final VlanId VLAN_ID_1 = VlanId.vlanId("1");
    private static final VlanId VLAN_ID_2 = VlanId.vlanId("2");

    private ExternalPeerRouter router1;
    private ExternalPeerRouter sameAsRouter1;
    private ExternalPeerRouter router2;

    /**
     * Checks that the DefaultExternalPeerRouter class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(DefaultExternalPeerRouter.class);
    }

    /**
     * Initial setup for this unit test.
     */
    @Before
    public void setUp() {
        ExternalPeerRouter.Builder builder1 = DefaultExternalPeerRouter.builder();

        router1 = builder1
                    .ipAddress(ROUTER_IP_1)
                    .macAddress(ROUTER_MAC_1)
                    .vlanId(VLAN_ID_1)
                    .build();

        ExternalPeerRouter.Builder builder2 = DefaultExternalPeerRouter.builder();

        sameAsRouter1 = builder2
                    .ipAddress(ROUTER_IP_1)
                    .macAddress(ROUTER_MAC_1)
                    .vlanId(VLAN_ID_1)
                    .build();

        ExternalPeerRouter.Builder builder3 = DefaultExternalPeerRouter.builder();

        router2 = builder3
                    .ipAddress(ROUTER_IP_2)
                    .macAddress(ROUTER_MAC_2)
                    .vlanId(VLAN_ID_2)
                    .build();
    }

    /**
     * Tests object equality.
     */
    @Test
    public void testEquality() {
        new EqualsTester().addEqualityGroup(router1, sameAsRouter1)
                .addEqualityGroup(router2)
                .testEquals();
    }

    /**
     * Test object construction.
     */
    @Test
    public void testConstruction() {
        ExternalPeerRouter router = router1;

        assertThat(router.ipAddress(), is(router1.ipAddress()));
        assertThat(router.macAddress(), is(router1.macAddress()));
        assertThat(router.vlanId(), is(router1.vlanId()));
    }
}
