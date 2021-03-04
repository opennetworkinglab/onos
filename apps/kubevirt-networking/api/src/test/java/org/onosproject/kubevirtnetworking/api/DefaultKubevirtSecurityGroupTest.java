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
import org.onlab.packet.IpPrefix;

import java.util.Set;

import static junit.framework.TestCase.assertEquals;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * Unit tests for the default kubevirt security group class.
 */
public class DefaultKubevirtSecurityGroupTest {
    private static final String ID_1 = "id-1";
    private static final String ID_2 = "id-1";
    private static final String NAME_1 = "sg-1";
    private static final String NAME_2 = "sg-2";
    private static final String DESCRIPTION_1 = "sg-1";
    private static final String DESCRIPTION_2 = "sg-2";

    private static final String RULE_ID_1 = "rule-1";
    private static final String RULE_ID_2 = "rule-2";
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

    private static final Set<KubevirtSecurityGroupRule> RULES_1 = ImmutableSet.of(
            createRule(
                    RULE_ID_1,
                    ID_1,
                    DIRECTION_1,
                    ETHER_TYPE_1,
                    PORT_RANGE_MAX_1,
                    PORT_RANGE_MIN_1,
                    PROTOCOL_1,
                    REMOTE_IP_PREFIX_1,
                    REMOTE_GROUP_ID_1
            )
    );
    private static final Set<KubevirtSecurityGroupRule> RULES_2 = ImmutableSet.of(
            createRule(
                    RULE_ID_2,
                    ID_2,
                    DIRECTION_2,
                    ETHER_TYPE_2,
                    PORT_RANGE_MAX_2,
                    PORT_RANGE_MIN_2,
                    PROTOCOL_2,
                    REMOTE_IP_PREFIX_2,
                    REMOTE_GROUP_ID_2
            )
    );

    private KubevirtSecurityGroup sg1;
    private KubevirtSecurityGroup sameAsSg1;
    private KubevirtSecurityGroup sg2;

    /**
     * Tests class immutability.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(DefaultKubevirtSecurityGroup.class);
    }

    /**
     * Initial setup for this unit test.
     */
    @Before
    public void setUp() {
        sg1 = DefaultKubevirtSecurityGroup.builder()
                .id(ID_1)
                .name(NAME_1)
                .description(DESCRIPTION_1)
                .rules(RULES_1)
                .build();

        sameAsSg1 = DefaultKubevirtSecurityGroup.builder()
                .id(ID_1)
                .name(NAME_1)
                .description(DESCRIPTION_1)
                .rules(RULES_1)
                .build();

        sg2 = DefaultKubevirtSecurityGroup.builder()
                .id(ID_2)
                .name(NAME_2)
                .description(DESCRIPTION_2)
                .rules(RULES_2)
                .build();
    }

    /**
     * Tests object equality.
     */
    @Test
    public void testEquality() {
        new EqualsTester().addEqualityGroup(sg1, sameAsSg1)
                .addEqualityGroup(sg2)
                .testEquals();
    }

    /**
     * Test object construction.
     */
    @Test
    public void testConstruction() {
        KubevirtSecurityGroup sg = sg1;

        assertEquals(ID_1, sg.id());
        assertEquals(NAME_1, sg.name());
        assertEquals(DESCRIPTION_1, sg.description());
        assertEquals(RULES_1, sg.rules());
    }

    private static KubevirtSecurityGroupRule createRule(String id, String sgId,
                                                        String direction, String etherType,
                                                        Integer portRangeMax,
                                                        Integer portRangeMin,
                                                        String protocol,
                                                        IpPrefix remoteIpPrefix,
                                                        String remoteGroupId) {
        return DefaultKubevirtSecurityGroupRule.builder()
                .id(id)
                .securityGroupId(sgId)
                .direction(direction)
                .etherType(etherType)
                .portRangeMax(portRangeMax)
                .portRangeMin(portRangeMin)
                .protocol(protocol)
                .remoteIpPrefix(remoteIpPrefix)
                .remoteGroupId(remoteGroupId)
                .build();
    }
}
