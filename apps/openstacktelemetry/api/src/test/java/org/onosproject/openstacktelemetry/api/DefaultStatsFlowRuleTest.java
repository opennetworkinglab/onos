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
package org.onosproject.openstacktelemetry.api;

import com.google.common.testing.EqualsTester;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.TpPort;

import static org.junit.Assert.assertEquals;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * Unit tests for DefaultStatsFlowRule class.
 */
public final class DefaultStatsFlowRuleTest {

    private static final IpAddress IP_ADDRESS_1 = IpAddress.valueOf("10.10.10.1");
    private static final IpAddress IP_ADDRESS_2 = IpAddress.valueOf("20.20.20.1");

    private static final int IP_PREFIX_LENGTH_1 = 10;
    private static final int IP_PREFIX_LENGTH_2 = 20;

    private static final int PORT_1 = 1000;
    private static final int PORT_2 = 2000;

    private static final byte PROTOCOL_1 = 1;
    private static final byte PROTOCOL_2 = 2;

    private StatsFlowRule rule1;
    private StatsFlowRule sameAsRule1;
    private StatsFlowRule rule2;

    /**
     * Initial setup for this unit test.
     */
    @Before
    public void setUp() {

        rule1 = DefaultStatsFlowRule.builder()
                .srcIpPrefix(IpPrefix.valueOf(IP_ADDRESS_1, IP_PREFIX_LENGTH_1))
                .dstIpPrefix(IpPrefix.valueOf(IP_ADDRESS_2, IP_PREFIX_LENGTH_2))
                .srcTpPort(TpPort.tpPort(PORT_1))
                .dstTpPort(TpPort.tpPort(PORT_2))
                .ipProtocol(PROTOCOL_1)
                .build();

        sameAsRule1 = DefaultStatsFlowRule.builder()
                .srcIpPrefix(IpPrefix.valueOf(IP_ADDRESS_1, IP_PREFIX_LENGTH_1))
                .dstIpPrefix(IpPrefix.valueOf(IP_ADDRESS_2, IP_PREFIX_LENGTH_2))
                .srcTpPort(TpPort.tpPort(PORT_1))
                .dstTpPort(TpPort.tpPort(PORT_2))
                .ipProtocol(PROTOCOL_1)
                .build();

        rule2 = DefaultStatsFlowRule.builder()
                .srcIpPrefix(IpPrefix.valueOf(IP_ADDRESS_2, IP_PREFIX_LENGTH_2))
                .dstIpPrefix(IpPrefix.valueOf(IP_ADDRESS_1, IP_PREFIX_LENGTH_1))
                .srcTpPort(TpPort.tpPort(PORT_2))
                .dstTpPort(TpPort.tpPort(PORT_1))
                .ipProtocol(PROTOCOL_2)
                .build();
    }

    /**
     * Tests class immutability.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(DefaultStatsFlowRule.class);
    }

    /**
     * Tests object equality.
     */
    @Test
    public void testEquality() {
        new EqualsTester()
                .addEqualityGroup(rule1, sameAsRule1)
                .addEqualityGroup(rule2).testEquals();
    }

    /**
     * Tests object construction.
     */
    @Test
    public void testConstruction() {
        StatsFlowRule rule = rule1;

        assertEquals(IpPrefix.valueOf(IP_ADDRESS_1, IP_PREFIX_LENGTH_1), rule.srcIpPrefix());
        assertEquals(IpPrefix.valueOf(IP_ADDRESS_2, IP_PREFIX_LENGTH_2), rule.dstIpPrefix());
        assertEquals(TpPort.tpPort(PORT_1), rule.srcTpPort());
        assertEquals(TpPort.tpPort(PORT_2), rule.dstTpPort());
        assertEquals(PROTOCOL_1, rule.ipProtocol());
    }
}
