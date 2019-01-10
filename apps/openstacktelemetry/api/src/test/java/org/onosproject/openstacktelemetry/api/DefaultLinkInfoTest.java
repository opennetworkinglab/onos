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
import static org.onosproject.openstacktelemetry.api.DefaultLinkStatsInfoTest.createLinkStatsInfo1;
import static org.onosproject.openstacktelemetry.api.DefaultLinkStatsInfoTest.createLinkStatsInfo2;

/**
 * Unit tests for DefaultLinkInfo class.
 */
public final class DefaultLinkInfoTest {

    private static final String LINK_ID_1 = "L100";
    private static final String LINK_ID_2 = "L200";

    private static final String SRC_IP_1 = "10.10.10.1";
    private static final String SRC_IP_2 = "10.10.10.2";

    private static final int SRC_PORT_1 = 80;
    private static final int SRC_PORT_2 = 90;

    private static final String DST_IP_1 = "20.20.20.1";
    private static final String DST_IP_2 = "20.20.20.2";

    private static final int DST_PORT_1 = 100;
    private static final int DST_PORT_2 = 110;

    private static final LinkStatsInfo LINK_STATS_1 = createLinkStatsInfo1();
    private static final LinkStatsInfo LINK_STATS_2 = createLinkStatsInfo2();

    private static final String PROTOCOL_1 = "TCP";
    private static final String PROTOCOL_2 = "UDP";

    private LinkInfo info1;
    private LinkInfo sameAsInfo1;
    private LinkInfo info2;

    /**
     * Initial setup for this unit test.
     */
    @Before
    public void setUp() {
        LinkInfo.Builder builder1 = DefaultLinkInfo.builder();
        LinkInfo.Builder builder2 = DefaultLinkInfo.builder();
        LinkInfo.Builder builder3 = DefaultLinkInfo.builder();

        info1 = builder1
                .withLinkId(LINK_ID_1)
                .withSrcIp(SRC_IP_1)
                .withSrcPort(SRC_PORT_1)
                .withDstIp(DST_IP_1)
                .withDstPort(DST_PORT_1)
                .withLinkStats(LINK_STATS_1)
                .withProtocol(PROTOCOL_1)
                .build();

        sameAsInfo1 = builder2
                .withLinkId(LINK_ID_1)
                .withSrcIp(SRC_IP_1)
                .withSrcPort(SRC_PORT_1)
                .withDstIp(DST_IP_1)
                .withDstPort(DST_PORT_1)
                .withLinkStats(LINK_STATS_1)
                .withProtocol(PROTOCOL_1)
                .build();

        info2 = builder3
                .withLinkId(LINK_ID_2)
                .withSrcIp(SRC_IP_2)
                .withSrcPort(SRC_PORT_2)
                .withDstIp(DST_IP_2)
                .withDstPort(DST_PORT_2)
                .withLinkStats(LINK_STATS_2)
                .withProtocol(PROTOCOL_2)
                .build();
    }

    /**
     * Tests class immutability.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(DefaultLinkInfo.class);
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
        LinkInfo info = info1;

        assertEquals(LINK_ID_1, info.linkId());
        assertEquals(SRC_IP_1, info.srcIp());
        assertEquals(SRC_PORT_1, info.srcPort());
        assertEquals(DST_IP_1, info.dstIp());
        assertEquals(DST_PORT_1, info.dstPort());
        assertEquals(LINK_STATS_1, info.linkStats());
        assertEquals(PROTOCOL_1, info.protocol());
    }
}
