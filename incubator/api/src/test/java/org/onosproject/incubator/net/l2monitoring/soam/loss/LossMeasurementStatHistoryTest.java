/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.incubator.net.l2monitoring.soam.loss;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.time.Instant;

import org.junit.Before;
import org.junit.Test;
import org.onosproject.incubator.net.l2monitoring.soam.SoamId;
import org.onosproject.incubator.net.l2monitoring.soam.loss.LossMeasurementStatHistory.LmStatHistoryBuilder;

public class LossMeasurementStatHistoryTest {
    LossMeasurementStatHistory lmsh1;

    @Before
    public void setUp() {
        LmStatHistoryBuilder builder = DefaultLmStatHistory.builder(
                Duration.ofMinutes(11), true, SoamId.valueOf(6),
                Instant.ofEpochSecond(123456789L));

        lmsh1 = builder.build();
    }

    @Test
    public void testHistoryStatsId() {
        assertEquals(6, lmsh1.historyStatsId().id().intValue());
    }

    @Test
    public void testEndTime() {
        assertEquals(123456789L, lmsh1.endTime().getEpochSecond());
    }

    @Test
    public void testElapsedTime() {
        assertEquals(11, lmsh1.elapsedTime().toMinutes());
    }

    @Test
    public void testSuspectStatus() {
        assertTrue(lmsh1.suspectStatus());
    }

}
