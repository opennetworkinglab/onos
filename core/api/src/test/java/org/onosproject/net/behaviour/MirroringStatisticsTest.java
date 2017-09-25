/*
 * Copyright 2017-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onosproject.net.behaviour;

import java.util.Map;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.testing.EqualsTester;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

public class MirroringStatisticsTest {

    private static final long BYTES_1 = 100L;
    private static final long PACKETS_1 = 2L;
    private static final String NAME_1 = "mirror1";
    private Map<String, Integer> statistics1 =
            ImmutableMap.of("tx_bytes", (int) BYTES_1, "tx_packets", (int) PACKETS_1);
    private MirroringStatistics mirrorStatisticStats1 = MirroringStatistics.mirroringStatistics(NAME_1, statistics1);

    private Map<String, Integer> sameAsStatistics1 =
            ImmutableMap.of("tx_bytes", (int) BYTES_1, "tx_packets", (int) PACKETS_1);
    private MirroringStatistics sameAsMirrorStatisticStats1 =
            MirroringStatistics.mirroringStatistics(NAME_1, sameAsStatistics1);

    private static final long BYTES_2 = 100L;
    private static final long PACKETS_2 = 2L;
    private static final String NAME_2 = "mirror2";
    private Map<String, Integer> statistics2 =
            ImmutableMap.of("tx_bytes", (int) BYTES_2, "tx_packets", (int) PACKETS_2);
    private MirroringStatistics mirrorStatisticStats2 = MirroringStatistics.mirroringStatistics(NAME_2, statistics2);

    private static final long BYTES_3 = 100L;
    private static final long PACKETS_3 = 2L;
    private static final String NAME_3 = "mirror3";
    private Map<String, Integer> statistics3 =
            ImmutableMap.of("tx_bytes", (int) BYTES_3, "tx_packets", (int) PACKETS_3);
    private MirroringStatistics mirrorStatisticStats3 = MirroringStatistics.mirroringStatistics(NAME_3, statistics3);

    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(MirroringStatistics.class);
    }

    @Test
    public void testConstruction() {
        assertThat(mirrorStatisticStats1.bytes(), is(BYTES_1));
        assertThat(mirrorStatisticStats1.name().name(), is(NAME_1));
        assertThat(mirrorStatisticStats1.packets(), is(PACKETS_1));
    }

    @Test
    public void testEquals() {
        new EqualsTester()
                .addEqualityGroup(mirrorStatisticStats1, sameAsMirrorStatisticStats1)
                .addEqualityGroup(mirrorStatisticStats2)
                .addEqualityGroup(mirrorStatisticStats3)
                .testEquals();
    }
}
