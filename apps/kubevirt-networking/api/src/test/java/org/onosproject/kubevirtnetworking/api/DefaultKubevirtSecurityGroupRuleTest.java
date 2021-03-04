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
import org.onlab.packet.IpPrefix;

import static junit.framework.TestCase.assertEquals;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * Unit tests for the default kubevirt security group rule class.
 */
public class DefaultKubevirtSecurityGroupRuleTest {
    private static final String ID_1 = "id-1";
    private static final String ID_2 = "id-2";
    private static final String SECURITY_GROUP_ID_1 = "sg-id-1";
    private static final String SECURITY_GROUP_ID_2 = "sg-id-2";
    private static final String DIRECTION_1 = "ingress";
    private static final String DIRECTION_2 = "egress";
    private static final String ETHER_TYPE_1 = "IPv4";
    private static final String ETHER_TYPE_2 = "IPv4";
    private static final Integer PORT_RANGE_MAX_1 = 80;
    private static final Integer PORT_RANGE_MAX_2 = 8080;
    private static final Integer PORT_RANGE_MIN_1 = 80;
    private static final Integer PORT_RANGE_MIN_2 = 8080;
    private static final String PROTOCOL_1 = "tcp";
    private static final String PROTOCOL_2 = "udp";
    private static final IpPrefix REMOTE_IP_PREFIX_1 = IpPrefix.valueOf("10.10.10.0/24");
    private static final IpPrefix REMOTE_IP_PREFIX_2 = IpPrefix.valueOf("20.20.20.0/24");
    private static final String REMOTE_GROUP_ID_1 = "rid-1";
    private static final String REMOTE_GROUP_ID_2 = "rid-2";

    private KubevirtSecurityGroupRule rule1;
    private KubevirtSecurityGroupRule sameAsRule1;
    private KubevirtSecurityGroupRule rule2;

    /**
     * Tests class immutability.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(DefaultKubevirtSecurityGroupRule.class);
    }

    /**
     * Initial setup for this unit test.
     */
    @Before
    public void setUp() {
        rule1 = DefaultKubevirtSecurityGroupRule.builder()
                .id(ID_1)
                .securityGroupId(SECURITY_GROUP_ID_1)
                .direction(DIRECTION_1)
                .etherType(ETHER_TYPE_1)
                .portRangeMax(PORT_RANGE_MAX_1)
                .portRangeMin(PORT_RANGE_MIN_1)
                .protocol(PROTOCOL_1)
                .remoteIpPrefix(REMOTE_IP_PREFIX_1)
                .remoteGroupId(REMOTE_GROUP_ID_1)
                .build();

        sameAsRule1 = DefaultKubevirtSecurityGroupRule.builder()
                .id(ID_1)
                .securityGroupId(SECURITY_GROUP_ID_1)
                .direction(DIRECTION_1)
                .etherType(ETHER_TYPE_1)
                .portRangeMax(PORT_RANGE_MAX_1)
                .portRangeMin(PORT_RANGE_MIN_1)
                .protocol(PROTOCOL_1)
                .remoteIpPrefix(REMOTE_IP_PREFIX_1)
                .remoteGroupId(REMOTE_GROUP_ID_1)
                .build();

        rule2 = DefaultKubevirtSecurityGroupRule.builder()
                .id(ID_2)
                .securityGroupId(SECURITY_GROUP_ID_2)
                .direction(DIRECTION_2)
                .etherType(ETHER_TYPE_2)
                .portRangeMax(PORT_RANGE_MAX_2)
                .portRangeMin(PORT_RANGE_MIN_2)
                .protocol(PROTOCOL_2)
                .remoteIpPrefix(REMOTE_IP_PREFIX_2)
                .remoteGroupId(REMOTE_GROUP_ID_2)
                .build();
    }

    /**
     * Tests object equality.
     */
    @Test
    public void testEquality() {
        new EqualsTester().addEqualityGroup(rule1, sameAsRule1)
                .addEqualityGroup(rule2)
                .testEquals();
    }

    /**
     * Test object construction.
     */
    @Test
    public void testConstruction() {
        KubevirtSecurityGroupRule rule = rule1;

        assertEquals(ID_1, rule.id());
        assertEquals(SECURITY_GROUP_ID_1, rule.securityGroupId());
        assertEquals(DIRECTION_1, rule.direction());
        assertEquals(ETHER_TYPE_1, rule.etherType());
        assertEquals(PORT_RANGE_MAX_1, rule.portRangeMax());
        assertEquals(PORT_RANGE_MIN_1, rule.portRangeMin());
        assertEquals(PROTOCOL_1, rule.protocol());
        assertEquals(REMOTE_IP_PREFIX_1, rule.remoteIpPrefix());
        assertEquals(REMOTE_GROUP_ID_1, rule.remoteGroupId());
    }
}
