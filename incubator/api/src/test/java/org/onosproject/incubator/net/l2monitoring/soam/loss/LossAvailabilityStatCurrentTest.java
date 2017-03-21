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
import org.onosproject.incubator.net.l2monitoring.soam.MilliPct;
import org.onosproject.incubator.net.l2monitoring.soam.loss.LossAvailabilityStatCurrent.LaStatCurrentBuilder;

public class LossAvailabilityStatCurrentTest {

    LossAvailabilityStatCurrent lasc1;

    @Before
    public void setUp() {
        LaStatCurrentBuilder builder = DefaultLaStatCurrent.builder(
                Duration.ofMinutes(13), true, Instant.ofEpochSecond(123456789L));
        builder = (LaStatCurrentBuilder) builder
                .backwardAvailable(123456780L)
                .backwardAverageFrameLossRatio(MilliPct.ofMilliPct(12345))
                .backwardConsecutiveHighLoss(123456781L)
                .backwardHighLoss(123456782L)
                .backwardMaxFrameLossRatio(MilliPct.ofMilliPct(12346))
                .backwardMinFrameLossRatio(MilliPct.ofMilliPct(12347))
                .backwardUnavailable(123456783L)
                .forwardAvailable(123456784L)
                .forwardAverageFrameLossRatio(MilliPct.ofMilliPct(12348))
                .forwardConsecutiveHighLoss(123456785L)
                .forwardHighLoss(123456786L)
                .forwardMaxFrameLossRatio(MilliPct.ofMilliPct(12349))
                .forwardMinFrameLossRatio(MilliPct.ofMilliPct(12350))
                .forwardUnavailable(123456787L);

        lasc1 = builder.build();
    }

    @Test
    public void testStartTime() {
        assertEquals(123456789L, lasc1.startTime().getEpochSecond());
    }

    @Test
    public void testElapsedTime() {
        assertEquals(13, lasc1.elapsedTime().toMinutes());
    }

    @Test
    public void testSuspectStatus() {
        assertTrue(lasc1.suspectStatus());
    }

    @Test
    public void testForwardHighLoss() {
        assertEquals(123456786L, lasc1.forwardHighLoss().longValue());
    }

    @Test
    public void testBackwardHighLoss() {
        assertEquals(123456782L, lasc1.backwardHighLoss().longValue());
    }

    @Test
    public void testForwardConsecutiveHighLoss() {
        assertEquals(123456785L, lasc1.forwardConsecutiveHighLoss().longValue());
    }

    @Test
    public void testBackwardConsecutiveHighLoss() {
        assertEquals(123456781L, lasc1.backwardConsecutiveHighLoss().longValue());
    }

    @Test
    public void testForwardAvailable() {
        assertEquals(123456784L, lasc1.forwardAvailable().longValue());
    }

    @Test
    public void testBackwardAvailable() {
        assertEquals(123456780L, lasc1.backwardAvailable().longValue());
    }

    @Test
    public void testForwardUnavailable() {
        assertEquals(123456787L, lasc1.forwardUnavailable().longValue());
    }

    @Test
    public void testBackwardUnavailable() {
        assertEquals(123456783L, lasc1.backwardUnavailable().longValue());
    }

    @Test
    public void testForwardMinFrameLossRatio() {
        assertEquals(12350, lasc1.forwardMinFrameLossRatio().intValue());
    }

    @Test
    public void testForwardMaxFrameLossRatio() {
        assertEquals(12349, lasc1.forwardMaxFrameLossRatio().intValue());
    }

    @Test
    public void testForwardAverageFrameLossRatio() {
        assertEquals(12348, lasc1.forwardAverageFrameLossRatio().intValue());
    }

    @Test
    public void testBackwardMinFrameLossRatio() {
        assertEquals(12347, lasc1.backwardMinFrameLossRatio().intValue());
    }

    @Test
    public void testBackwardMaxFrameLossRatio() {
        assertEquals(12346, lasc1.backwardMaxFrameLossRatio().intValue());
    }

    @Test
    public void testBackwardAverageFrameLossRatio() {
        assertEquals(12345, lasc1.backwardAverageFrameLossRatio().intValue());
    }

}
