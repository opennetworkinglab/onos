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
import org.onosproject.incubator.net.l2monitoring.soam.loss.LossMeasurementStatCurrent.LmStatCurrentBuilder;

public class LossMeasurementStatCurrentTest {
    LossMeasurementStatCurrent lmsc1;

    @Before
    public void setUp() {
        LmStatCurrentBuilder builder = DefaultLmStatCurrent
                .builder(Duration.ofMinutes(14),
                        true, Instant.ofEpochSecond(12345678L));
        builder = (LmStatCurrentBuilder) builder
                .backwardAverageFrameLossRatio(MilliPct.ofMilliPct(301))
                .backwardMaxFrameLossRatio(MilliPct.ofMilliPct(302))
                .backwardMinFrameLossRatio(MilliPct.ofMilliPct(303))
                .backwardReceivedFrames(123456780L)
                .backwardTransmittedFrames(123456781L)
                .forwardAverageFrameLossRatio(MilliPct.ofMilliPct(304))
                .forwardMaxFrameLossRatio(MilliPct.ofMilliPct(305))
                .forwardMinFrameLossRatio(MilliPct.ofMilliPct(306))
                .forwardReceivedFrames(123456782L)
                .forwardTransmittedFrames(123456783L)
                .soamPdusReceived(123456784L)
                .soamPdusSent(123456785L);

        lmsc1 = builder.build();
    }

    @Test
    public void testStartTime() {
        assertEquals(12345678L, lmsc1.startTime().getEpochSecond());
    }

    @Test
    public void testElapsedTime() {
        assertEquals(14, lmsc1.elapsedTime().toMinutes());
    }

    @Test
    public void testSuspectStatus() {
        assertTrue(lmsc1.suspectStatus());
    }

    @Test
    public void testForwardTransmittedFrames() {
        assertEquals(123456783L, lmsc1.forwardTransmittedFrames().longValue());
    }

    @Test
    public void testForwardReceivedFrames() {
        assertEquals(123456782L, lmsc1.forwardReceivedFrames().longValue());
    }

    @Test
    public void testForwardMinFrameLossRatio() {
        assertEquals(306, lmsc1.forwardMinFrameLossRatio().intValue());
    }

    @Test
    public void testForwardMaxFrameLossRatio() {
        assertEquals(305, lmsc1.forwardMaxFrameLossRatio().intValue());
    }

    @Test
    public void testForwardAverageFrameLossRatio() {
        assertEquals(304, lmsc1.forwardAverageFrameLossRatio().intValue());
    }

    @Test
    public void testBackwardTransmittedFrames() {
        assertEquals(123456781L, lmsc1.backwardTransmittedFrames().longValue());
    }

    @Test
    public void testBackwardReceivedFrames() {
        assertEquals(123456780L, lmsc1.backwardReceivedFrames().longValue());
    }

    @Test
    public void testBackwardMinFrameLossRatio() {
        assertEquals(303, lmsc1.backwardMinFrameLossRatio().intValue());
    }

    @Test
    public void testBackwardMaxFrameLossRatio() {
        assertEquals(302, lmsc1.backwardMaxFrameLossRatio().intValue());
    }

    @Test
    public void testBackwardAverageFrameLossRatio() {
        assertEquals(301, lmsc1.backwardAverageFrameLossRatio().intValue());
    }

    @Test
    public void testSoamPdusSent() {
        assertEquals(123456785L, lmsc1.soamPdusSent().longValue());
    }

    @Test
    public void testSoamPdusReceived() {
        assertEquals(123456784L, lmsc1.soamPdusReceived().longValue());
    }

}
