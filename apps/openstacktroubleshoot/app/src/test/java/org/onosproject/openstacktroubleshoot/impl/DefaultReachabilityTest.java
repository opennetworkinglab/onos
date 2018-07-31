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
package org.onosproject.openstacktroubleshoot.impl;

import com.google.common.testing.EqualsTester;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onosproject.openstacktroubleshoot.api.Reachability;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * Unit tests for DefaultReachability class.
 */
public final class DefaultReachabilityTest {

    private static final IpAddress IP_ADDRESS_1_1 = IpAddress.valueOf("1.2.3.4");
    private static final IpAddress IP_ADDRESS_1_2 = IpAddress.valueOf("2.3.4.5");
    private static final IpAddress IP_ADDRESS_2_1 = IpAddress.valueOf("5.6.7.8");
    private static final IpAddress IP_ADDRESS_2_2 = IpAddress.valueOf("6.7.8.9");

    private Reachability reachability1;
    private Reachability sameAsReachability1;
    private Reachability reachability2;

    /**
     * Initial setup for this unit test.
     */
    @Before
    public void setUp() {
        reachability1 = DefaultReachability.builder()
                                .srcIp(IP_ADDRESS_1_1)
                                .dstIp(IP_ADDRESS_1_2)
                                .isReachable(true)
                                .build();

        sameAsReachability1 = DefaultReachability.builder()
                                .srcIp(IP_ADDRESS_1_1)
                                .dstIp(IP_ADDRESS_1_2)
                                .isReachable(true)
                                .build();

        reachability2 = DefaultReachability.builder()
                                .srcIp(IP_ADDRESS_2_1)
                                .dstIp(IP_ADDRESS_2_2)
                                .isReachable(false)
                                .build();
    }

    /**
     * Checks the class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(DefaultReachability.class);
    }

    /**
     * Tests object equality.
     */
    @Test
    public void testEquality() {
        new EqualsTester().addEqualityGroup(reachability1, sameAsReachability1)
                .addEqualityGroup(reachability2)
                .testEquals();
    }

    /**
     * Test object construction.
     */
    @Test
    public void testConstruction() {
        Reachability reachability = reachability1;

        assertEquals(reachability.srcIp(), IP_ADDRESS_1_1);
        assertEquals(reachability.dstIp(), IP_ADDRESS_1_2);
        assertTrue(reachability.isReachable());
    }
}
