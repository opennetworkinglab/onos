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

import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;
import org.onosproject.incubator.net.l2monitoring.soam.MilliPct;
import org.onosproject.incubator.net.l2monitoring.soam.SoamId;
import org.onosproject.incubator.net.l2monitoring.soam.loss.LossMeasurementThreshold.ThresholdOption;

public class LossMeasurementThresholdOptionTest {

    LossMeasurementThreshold lmt1;

    @Before
    public void setUp() throws Exception {
        lmt1 = DefaultLmThreshold
                .builder(SoamId.valueOf(4))
                .addToThreshold(ThresholdOption.BACKWARD_CONSECUTIVE_HIGH_LOSS)
                .addToThreshold(ThresholdOption.MAX_FLR_BACKWARD)
                .averageFlrBackward(MilliPct.ofMilliPct(301))
                .averageFlrForward(MilliPct.ofMilliPct(302))
                .backwardAvailableRatio(MilliPct.ofMilliPct(303))
                .backwardConsecutiveHighLoss(123451L)
                .backwardHighLoss(123452L)
                .backwardUnavailableCount(123453L)
                .forwardAvailableRatio(MilliPct.ofMilliPct(304))
                .forwardConsecutiveHighLoss(123454L)
                .forwardHighLoss(123455L)
                .forwardUnavailableCount(123456L)
                .maxFlrBackward(MilliPct.ofMilliPct(305))
                .maxFlrForward(MilliPct.ofMilliPct(306))
                .measuredFlrBackward(MilliPct.ofMilliPct(307))
                .measuredFlrForward(MilliPct.ofMilliPct(308))
                .build();
    }

    @Test
    public void testThresholdId() {
        assertEquals(4, lmt1.thresholdId().id().intValue());
    }

    @Test
    public void testThreshold() {
        assertEquals(2, lmt1.thresholds().size());
        ArrayList<ThresholdOption> list = new ArrayList<>();
        lmt1.thresholds().forEach(list::add);
        assertEquals(ThresholdOption.BACKWARD_CONSECUTIVE_HIGH_LOSS, list.get(0));
        assertEquals(ThresholdOption.MAX_FLR_BACKWARD, list.get(1));
    }

    @Test
    public void testMeasuredFlrForward() {
        assertEquals(308, lmt1.measuredFlrForward().intValue());
    }

    @Test
    public void testMaxFlrForward() {
        assertEquals(306, lmt1.maxFlrForward().intValue());
    }

    @Test
    public void testAverageFlrForward() {
        assertEquals(302, lmt1.averageFlrForward().intValue());
    }

    @Test
    public void testMeasuredFlrBackward() {
        assertEquals(307, lmt1.measuredFlrBackward().intValue());
    }

    @Test
    public void testMaxFlrBackward() {
        assertEquals(305, lmt1.maxFlrBackward().intValue());
    }

    @Test
    public void testAverageFlrBackward() {
        assertEquals(301, lmt1.averageFlrBackward().intValue());
    }

    @Test
    public void testForwardHighLoss() {
        assertEquals(123455L, lmt1.forwardHighLoss().longValue());
    }

    @Test
    public void testForwardConsecutiveHighLoss() {
        assertEquals(123454L, lmt1.forwardConsecutiveHighLoss().longValue());
    }

    @Test
    public void testBackwardHighLoss() {
        assertEquals(123452L, lmt1.backwardHighLoss().longValue());
    }

    @Test
    public void testBackwardConsecutiveHighLoss() {
        assertEquals(123451L, lmt1.backwardConsecutiveHighLoss().longValue());
    }

    @Test
    public void testForwardUnavailableCount() {
        assertEquals(123456L, lmt1.forwardUnavailableCount().longValue());
    }

    @Test
    public void testForwardAvailableRatio() {
        assertEquals(304, lmt1.forwardAvailableRatio().intValue());
    }

    @Test
    public void testBackwardUnavailableCount() {
        assertEquals(123453L, lmt1.backwardUnavailableCount().longValue());
    }

    @Test
    public void testBackwardAvailableRatio() {
        assertEquals(303, lmt1.backwardAvailableRatio().intValue());
    }
}
