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
import org.onosproject.cluster.NodeId;
import org.onosproject.simplefabric.api.FabricRoute;
import org.onosproject.simplefabric.api.FabricRoute.Source;

import static junit.framework.TestCase.assertEquals;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * Unit tests for the default fabric router.
 */
public final class DefaultFabricRouteTest {

    private static final Source SOURCE_1 = Source.STATIC;
    private static final Source SOURCE_2 = Source.BGP;

    private static final IpPrefix IP_PREFIX_1 = IpPrefix.valueOf("10.10.10.1/32");
    private static final IpPrefix IP_PREFIX_2 = IpPrefix.valueOf("20.20.20.2/32");

    private static final IpAddress NEXT_HOP_1 = IpAddress.valueOf("10.10.10.1");
    private static final IpAddress NEXT_HOP_2 = IpAddress.valueOf("20.20.20.2");

    private static final NodeId SOURCE_NODE_1 = NodeId.nodeId("1");
    private static final NodeId SOURCE_NODE_2 = NodeId.nodeId("2");

    private FabricRoute fabricRoute1;
    private FabricRoute sameAsFabricRoute1;
    private FabricRoute fabricRoute2;

    /**
     * Tests class immutability.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(DefaultFabricRoute.class);
    }

    /**
     * Initial setup for this unit test.
     */
    @Before
    public void setUp() {
        fabricRoute1 = DefaultFabricRoute.builder()
                                .source(SOURCE_1)
                                .prefix(IP_PREFIX_1)
                                .nextHop(NEXT_HOP_1)
                                .sourceNode(SOURCE_NODE_1)
                                .build();

        sameAsFabricRoute1 = DefaultFabricRoute.builder()
                                .source(SOURCE_1)
                                .prefix(IP_PREFIX_1)
                                .nextHop(NEXT_HOP_1)
                                .sourceNode(SOURCE_NODE_1)
                                .build();

        fabricRoute2 = DefaultFabricRoute.builder()
                                .source(SOURCE_2)
                                .prefix(IP_PREFIX_2)
                                .nextHop(NEXT_HOP_2)
                                .sourceNode(SOURCE_NODE_2)
                                .build();
    }

    /**
     * Tests object equality.
     */
    @Test
    public void testEquality() {
        new EqualsTester().addEqualityGroup(fabricRoute1, sameAsFabricRoute1)
                .addEqualityGroup(fabricRoute2)
                .testEquals();
    }

    /**
     * Test object construction.
     */
    @Test
    public void testConstruction() {
        FabricRoute route = fabricRoute1;

        assertEquals(route.source(), SOURCE_1);
        assertEquals(route.prefix(), IP_PREFIX_1);
        assertEquals(route.nextHop(), NEXT_HOP_1);
        assertEquals(route.sourceNode(), SOURCE_NODE_1);
    }
}
