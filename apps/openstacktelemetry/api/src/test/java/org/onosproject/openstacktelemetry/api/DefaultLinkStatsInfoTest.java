/*
 * Copyright 2019-present Open Networking Foundation
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

import static junit.framework.TestCase.assertEquals;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * Unit tests for DefaultLinkStatsInfo class.
 */
public final class DefaultLinkStatsInfoTest {

    private static final long TX_PACKET_1 = 1;
    private static final long TX_PACKET_2 = 2;

    private static final long RX_PACKET_1 = 1;
    private static final long RX_PACKET_2 = 2;

    private static final long TX_BYTE_1 = 1;
    private static final long TX_BYTE_2 = 2;

    private static final long RX_BYTE_1 = 1;
    private static final long RX_BYTE_2 = 2;

    private static final long TX_DROP_1 = 1;
    private static final long TX_DROP_2 = 2;

    private static final long RX_DROP_1 = 1;
    private static final long RX_DROP_2 = 2;

    private static final long TIMESTAMP_1 = 10;
    private static final long TIMESTAMP_2 = 20;

    private LinkStatsInfo info1;
    private LinkStatsInfo sameAsInfo1;
    private LinkStatsInfo info2;

    public static LinkStatsInfo createLinkStatsInfo1() {
        return DefaultLinkStatsInfo.builder()
                .withTxPacket(TX_PACKET_1)
                .withRxPacket(RX_PACKET_1)
                .withTxByte(TX_BYTE_1)
                .withRxByte(RX_BYTE_1)
                .withTxDrop(TX_DROP_1)
                .withRxDrop(RX_DROP_1)
                .withTimestamp(TIMESTAMP_1)
                .build();
    }

    public static LinkStatsInfo createLinkStatsInfo2() {
        return DefaultLinkStatsInfo.builder()
                .withTxPacket(TX_PACKET_2)
                .withRxPacket(RX_PACKET_2)
                .withTxByte(TX_BYTE_2)
                .withRxByte(RX_BYTE_2)
                .withTxDrop(TX_DROP_2)
                .withRxDrop(RX_DROP_2)
                .withTimestamp(TIMESTAMP_2)
                .build();
    }

    /**
     * Initial setup for this unit test.
     */
    @Before
    public void setUp() {
        info1 = createLinkStatsInfo1();
        sameAsInfo1 = createLinkStatsInfo1();
        info2 = createLinkStatsInfo2();
    }

    /**
     * Tests class immutability.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(DefaultLinkStatsInfo.class);
    }

    /**
     * Tests object equality.
     */
    @Test
    public void testEquality() {
        new EqualsTester()
                .addEqualityGroup(info1, sameAsInfo1)
                .addEqualityGroup(info2).testEquals();
    }

    /**
     * Tests object construction.
     */
    @Test
    public void testConstruction() {
        LinkStatsInfo info = info1;

        assertEquals(TX_PACKET_1, info.getTxPacket());
        assertEquals(RX_PACKET_1, info.getRxPacket());
        assertEquals(TX_BYTE_1, info.getTxByte());
        assertEquals(RX_BYTE_1, info.getRxByte());
        assertEquals(TX_DROP_1, info.getTxDrop());
        assertEquals(RX_DROP_1, info.getRxDrop());
        assertEquals(TIMESTAMP_1, info.getTimestamp());
    }
}
