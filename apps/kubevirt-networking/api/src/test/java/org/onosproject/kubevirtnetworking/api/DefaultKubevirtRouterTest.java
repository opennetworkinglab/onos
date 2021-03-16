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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.testing.EqualsTester;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;

import static junit.framework.TestCase.assertEquals;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;
import static org.onosproject.kubevirtnetworking.api.KubevirtNetwork.Type.FLAT;
import static org.onosproject.kubevirtnetworking.api.KubevirtNetwork.Type.VXLAN;

/**
 * Unit tests for the default kubevirt router class.
 */
public class DefaultKubevirtRouterTest {

    private static final String NAME_1 = "router-1";
    private static final String NAME_2 = "router-2";
    private static final String DESCRIPTION_1 = "dummy router 1";
    private static final String DESCRIPTION_2 = "dummy router 2";
    private static final boolean ENABLE_SNAT_1 = false;
    private static final boolean ENABLE_SNAT_2 = true;
    private static final MacAddress MAC_ADDRESS_1 = MacAddress.valueOf("11:22:33:44:55:66");
    private static final MacAddress MAC_ADDRESS_2 = MacAddress.valueOf("22:33:44:55:66:77");
    private static final KubevirtNetwork.Type TYPE_1 = FLAT;
    private static final KubevirtNetwork.Type TYPE_2 = VXLAN;
    private static final String NETWORK_NAME_1 = "net-1";
    private static final String NETWORK_NAME_2 = "net-2";
    private static final String GATEWAY_HOST_1 = "gateway-1";
    private static final String GATEWAY_HOST_2 = "gateway-2";

    private static final KubevirtPeerRouter PEER_ROUTER_1 =
            new KubevirtPeerRouter(IpAddress.valueOf("192.168.10.10"),
                    MacAddress.valueOf("11:22:33:44:55:66"));
    private static final KubevirtPeerRouter PEER_ROUTER_2 =
            new KubevirtPeerRouter(IpAddress.valueOf("192.168.20.20"),
                    MacAddress.valueOf("22:33:44:55:66:77"));

    private KubevirtRouter router1;
    private KubevirtRouter sameAsRouter1;
    private KubevirtRouter router2;

    /**
     * Tests class immutability.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(DefaultKubevirtRouter.class);
    }

    /**
     * Initial setup for this unit test.
     */
    @Before
    public void setUp() {
        router1 = DefaultKubevirtRouter.builder()
                .name(NAME_1)
                .description(DESCRIPTION_1)
                .enableSnat(ENABLE_SNAT_1)
                .mac(MAC_ADDRESS_1)
                .internal(ImmutableSet.of(NETWORK_NAME_1))
                .external(ImmutableMap.of("10.10.10.10", NETWORK_NAME_1))
                .peerRouter(PEER_ROUTER_1)
                .electedGateway(GATEWAY_HOST_1)
                .build();
        sameAsRouter1 = DefaultKubevirtRouter.builder()
                .name(NAME_1)
                .description(DESCRIPTION_1)
                .enableSnat(ENABLE_SNAT_1)
                .mac(MAC_ADDRESS_1)
                .internal(ImmutableSet.of(NETWORK_NAME_1))
                .external(ImmutableMap.of("10.10.10.10", NETWORK_NAME_1))
                .peerRouter(PEER_ROUTER_1)
                .electedGateway(GATEWAY_HOST_1)
                .build();
        router2 = DefaultKubevirtRouter.builder()
                .name(NAME_2)
                .description(DESCRIPTION_2)
                .enableSnat(ENABLE_SNAT_2)
                .mac(MAC_ADDRESS_2)
                .internal(ImmutableSet.of(NETWORK_NAME_2))
                .external(ImmutableMap.of("20.20.20.20", NETWORK_NAME_2))
                .peerRouter(PEER_ROUTER_2)
                .electedGateway(GATEWAY_HOST_2)
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
        KubevirtRouter router = router1;

        assertEquals(NAME_1, router.name());
        assertEquals(DESCRIPTION_1, router.description());
        assertEquals(ENABLE_SNAT_1, router.enableSnat());
        assertEquals(MAC_ADDRESS_1, router.mac());
        assertEquals(ImmutableSet.of(NETWORK_NAME_1), router.internal());
        assertEquals(ImmutableMap.of("10.10.10.10", NETWORK_NAME_1), router.external());
        assertEquals(PEER_ROUTER_1, router.peerRouter());
    }
}
