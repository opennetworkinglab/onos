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

import java.util.Set;

import static junit.framework.TestCase.assertEquals;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * Unit tests for the default kubevirt load balancer class.
 */
public class DefaultKubevirtLoadBalancerTest {

    private static final String NAME_1 = "lb-1";
    private static final String NAME_2 = "lb-2";
    private static final String DESCRIPTION_1 = "dummy lb 1";
    private static final String DESCRIPTION_2 = "dummy lb 2";
    private static final String NETWORK_ID_1 = "net-1";
    private static final String NETWORK_ID_2 = "net-2";
    private static final IpAddress VIP_1 = IpAddress.valueOf("10.10.10.10");
    private static final IpAddress VIP_2 = IpAddress.valueOf("20.20.20.20");
    private static final Integer PORT_RANGE_MAX_1 = 80;
    private static final Integer PORT_RANGE_MAX_2 = 8080;
    private static final Integer PORT_RANGE_MIN_1 = 80;
    private static final Integer PORT_RANGE_MIN_2 = 8080;
    private static final String PROTOCOL_1 = "tcp";
    private static final String PROTOCOL_2 = "udp";
    private static final Set<IpAddress> MEMBERS_1 =
            ImmutableSet.of(IpAddress.valueOf("10.10.10.11"),
                    IpAddress.valueOf("10.10.10.12"));
    private static final Set<IpAddress> MEMBERS_2 =
            ImmutableSet.of(IpAddress.valueOf("20.20.20.21"),
                    IpAddress.valueOf("20.20.20.22"));
    private static final KubevirtLoadBalancerRule RULE_1 =
            DefaultKubevirtLoadBalancerRule.builder()
                    .protocol(PROTOCOL_1)
                    .portRangeMax(PORT_RANGE_MAX_1)
                    .portRangeMin(PORT_RANGE_MIN_1)
                    .build();
    private static final KubevirtLoadBalancerRule RULE_2 =
            DefaultKubevirtLoadBalancerRule.builder()
                    .protocol(PROTOCOL_2)
                    .portRangeMax(PORT_RANGE_MAX_2)
                    .portRangeMin(PORT_RANGE_MIN_2)
                    .build();
    private static final Set<KubevirtLoadBalancerRule> RULES_1 = ImmutableSet.of(RULE_1);
    private static final Set<KubevirtLoadBalancerRule> RULES_2 = ImmutableSet.of(RULE_2);

    private KubevirtLoadBalancer lb1;
    private KubevirtLoadBalancer sameAsLb1;
    private KubevirtLoadBalancer lb2;

    /**
     * Tests class immutability.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(DefaultKubevirtLoadBalancer.class);
    }

    /**
     * Initial setup for this unit test.
     */
    @Before
    public void setUp() {
        lb1 = DefaultKubevirtLoadBalancer.builder()
                .name(NAME_1)
                .description(DESCRIPTION_1)
                .networkId(NETWORK_ID_1)
                .vip(VIP_1)
                .members(MEMBERS_1)
                .rules(RULES_1)
                .build();

        sameAsLb1 = DefaultKubevirtLoadBalancer.builder()
                .name(NAME_1)
                .description(DESCRIPTION_1)
                .networkId(NETWORK_ID_1)
                .vip(VIP_1)
                .members(MEMBERS_1)
                .rules(RULES_1)
                .build();

        lb2 = DefaultKubevirtLoadBalancer.builder()
                .name(NAME_2)
                .description(DESCRIPTION_2)
                .networkId(NETWORK_ID_2)
                .vip(VIP_2)
                .members(MEMBERS_2)
                .rules(RULES_2)
                .build();
    }

    /**
     * Tests object equality.
     */
    @Test
    public void testEquality() {
        new EqualsTester().addEqualityGroup(lb1, sameAsLb1)
                .addEqualityGroup(lb2)
                .testEquals();
    }

    /**
     * Test object construction.
     */
    @Test
    public void testConstruction() {
        KubevirtLoadBalancer lb = lb1;

        assertEquals(NAME_1, lb.name());
        assertEquals(DESCRIPTION_1, lb.description());
        assertEquals(NETWORK_ID_1, lb.networkId());
        assertEquals(VIP_1, lb.vip());
        assertEquals(MEMBERS_1, lb.members());
        assertEquals(RULES_1, lb.rules());
    }
}
