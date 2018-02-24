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
package org.onosproject.incubator.net.l2monitoring.soam.delay;

import java.time.Duration;

import org.junit.Before;
import org.junit.Test;
import org.onosproject.incubator.net.l2monitoring.soam.SoamConfigException;
import org.onosproject.incubator.net.l2monitoring.soam.SoamId;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementThreshold.ThresholdOption;

import static org.junit.Assert.assertEquals;

public class DelayMeasurementThresholdOptionTest {

    DelayMeasurementThreshold dmT1;

    @Before
    public void setUp() throws Exception, SoamConfigException {
        dmT1 = DefaultDelayMeasurementThreshold.builder(SoamId.valueOf(1))
                .averageFrameDelayBackward(Duration.ofMillis(101))
                .averageFrameDelayForward(Duration.ofMillis(102))
                .averageFrameDelayTwoWay(Duration.ofMillis(103))
                .averageFrameDelayRangeBackward(Duration.ofMillis(201))
                .averageFrameDelayRangeForward(Duration.ofMillis(202))
                .averageFrameDelayRangeTwoWay(Duration.ofMillis(203))
                .averageInterFrameDelayVariationBackward(Duration.ofMillis(301))
                .averageInterFrameDelayVariationForward(Duration.ofMillis(302))
                .averageInterFrameDelayVariationTwoWay(Duration.ofMillis(303))
                .maxFrameDelayBackward(Duration.ofMillis(401))
                .maxFrameDelayForward(Duration.ofMillis(402))
                .maxFrameDelayTwoWay(Duration.ofMillis(403))
                .maxFrameDelayRangeBackward(Duration.ofMillis(501))
                .maxFrameDelayRangeForward(Duration.ofMillis(502))
                .maxFrameDelayRangeTwoWay(Duration.ofMillis(503))
                .maxInterFrameDelayVariationBackward(Duration.ofMillis(601))
                .maxInterFrameDelayVariationForward(Duration.ofMillis(602))
                .maxInterFrameDelayVariationTwoWay(Duration.ofMillis(603))
                .measuredFrameDelayBackward(Duration.ofMillis(701))
                .measuredFrameDelayForward(Duration.ofMillis(702))
                .measuredFrameDelayTwoWay(Duration.ofMillis(703))
                .measuredInterFrameDelayVariationBackward(Duration.ofMillis(801))
                .measuredInterFrameDelayVariationForward(Duration.ofMillis(802))
                .measuredInterFrameDelayVariationTwoWay(Duration.ofMillis(803))
                .addToThresholdsEnabled(ThresholdOption.MEASURED_FRAME_DELAY_TWO_WAY)
                .addToThresholdsEnabled(ThresholdOption.MAX_FRAME_DELAY_TWO_WAY)
                .addToThresholdsEnabled(ThresholdOption.AVERAGE_FRAME_DELAY_TWO_WAY)
                .addToThresholdsEnabled(ThresholdOption.MEASURED_INTER_FRAME_DELAY_VARIATION_TWO_WAY)
                .addToThresholdsEnabled(ThresholdOption.MAX_INTER_FRAME_DELAY_VARIATION_TWO_WAY)
                .addToThresholdsEnabled(ThresholdOption.AVERAGE_INTER_FRAME_DELAY_VARIATION_TWO_WAY)
                .addToThresholdsEnabled(ThresholdOption.MAX_FRAME_DELAY_RANGE_TWO_WAY)
                .addToThresholdsEnabled(ThresholdOption.AVERAGE_FRAME_DELAY_RANGE_TWO_WAY)
                .addToThresholdsEnabled(ThresholdOption.MEASURED_FRAME_DELAY_FORWARD)
                .addToThresholdsEnabled(ThresholdOption.MAX_FRAME_DELAY_FORWARD)
                .addToThresholdsEnabled(ThresholdOption.AVERAGE_FRAME_DELAY_FORWARD)
                .addToThresholdsEnabled(ThresholdOption.MEASURED_INTER_FRAME_DELAY_VARIATION_FORWARD)
                .addToThresholdsEnabled(ThresholdOption.MAX_INTER_FRAME_DELAY_VARIATION_FORWARD)
                .addToThresholdsEnabled(ThresholdOption.AVERAGE_INTER_FRAME_DELAY_VARIATION_FORWARD)
                .addToThresholdsEnabled(ThresholdOption.MAX_FRAME_DELAY_RANGE_FORWARD)
                .addToThresholdsEnabled(ThresholdOption.AVERAGE_FRAME_DELAY_RANGE_FORWARD)
                .addToThresholdsEnabled(ThresholdOption.MEASURED_FRAME_DELAY_BACKWARD)
                .addToThresholdsEnabled(ThresholdOption.MAX_FRAME_DELAY_BACKWARD)
                .addToThresholdsEnabled(ThresholdOption.AVERAGE_FRAME_DELAY_BACKWARD)
                .addToThresholdsEnabled(ThresholdOption.MEASURED_INTER_FRAME_DELAY_VARIATION_BACKWARD)
                .addToThresholdsEnabled(ThresholdOption.MAX_INTER_FRAME_DELAY_VARIATION_BACKWARD)
                .addToThresholdsEnabled(ThresholdOption.AVERAGE_INTER_FRAME_DELAY_VARIATION_BACKWARD)
                .addToThresholdsEnabled(ThresholdOption.MAX_FRAME_DELAY_RANGE_BACKWARD)
                .addToThresholdsEnabled(ThresholdOption.AVERAGE_FRAME_DELAY_RANGE_BACKWARD)
                .build();
    }

    @Test
    public void testThreshId() {
        assertEquals(1, dmT1.threshId().id().intValue());
    }

    @Test
    public void testThresholdsEnabled() {
        assertEquals(24, dmT1.thresholdsEnabled().size());
    }

    @Test
    public void testMeasuredFrameDelayTwoWay() {
        assertEquals(703, dmT1.measuredFrameDelayTwoWay().toMillis());
    }

    @Test
    public void testMaxFrameDelayTwoWay() {
        assertEquals(403, dmT1.maxFrameDelayTwoWay().toMillis());
    }

    @Test
    public void testAverageFrameDelayTwoWay() {
        assertEquals(103, dmT1.averageFrameDelayTwoWay().toMillis());
    }

    @Test
    public void testMeasuredInterFrameDelayVariationTwoWay() {
        assertEquals(803, dmT1.measuredInterFrameDelayVariationTwoWay().toMillis());
    }

    @Test
    public void testMaxInterFrameDelayVariationTwoWay() {
        assertEquals(603, dmT1.maxInterFrameDelayVariationTwoWay().toMillis());
    }

    @Test
    public void testAverageInterFrameDelayVariationTwoWay() {
        assertEquals(303, dmT1.averageInterFrameDelayVariationTwoWay().toMillis());
    }

    @Test
    public void testMaxFrameDelayRangeTwoWay() {
        assertEquals(503, dmT1.maxFrameDelayRangeTwoWay().toMillis());
    }

    @Test
    public void testAverageFrameDelayRangeTwoWay() {
        assertEquals(203, dmT1.averageFrameDelayRangeTwoWay().toMillis());
    }

    @Test
    public void testMeasuredFrameDelayForward() {
        assertEquals(702, dmT1.measuredFrameDelayForward().toMillis());
    }

    @Test
    public void testMaxFrameDelayForward() {
        assertEquals(402, dmT1.maxFrameDelayForward().toMillis());
    }

    @Test
    public void testAverageFrameDelayForward() {
        assertEquals(102, dmT1.averageFrameDelayForward().toMillis());
    }

    @Test
    public void testMeasuredInterFrameDelayVariationForward() {
        assertEquals(802, dmT1.measuredInterFrameDelayVariationForward().toMillis());
    }

    @Test
    public void testMaxInterFrameDelayVariationForward() {
        assertEquals(602, dmT1.maxInterFrameDelayVariationForward().toMillis());
    }

    @Test
    public void testAverageInterFrameDelayVariationForward() {
        assertEquals(302, dmT1.averageInterFrameDelayVariationForward().toMillis());
    }

    @Test
    public void testMaxFrameDelayRangeForward() {
        assertEquals(502, dmT1.maxFrameDelayRangeForward().toMillis());
    }

    @Test
    public void testAverageFrameDelayRangeForward() {
        assertEquals(202, dmT1.averageFrameDelayRangeForward().toMillis());
    }

    @Test
    public void testMeasuredFrameDelayBackward() {
        assertEquals(701, dmT1.measuredFrameDelayBackward().toMillis());
    }

    @Test
    public void testMaxFrameDelayBackward() {
        assertEquals(401, dmT1.maxFrameDelayBackward().toMillis());
    }

    @Test
    public void testAverageFrameDelayBackward() {
        assertEquals(101, dmT1.averageFrameDelayBackward().toMillis());
    }

    @Test
    public void testMeasuredInterFrameDelayVariationBackward() {
        assertEquals(801, dmT1.measuredInterFrameDelayVariationBackward().toMillis());
    }

    @Test
    public void testMaxInterFrameDelayVariationBackward() {
        assertEquals(601, dmT1.maxInterFrameDelayVariationBackward().toMillis());
    }

    @Test
    public void testAverageInterFrameDelayVariationBackward() {
        assertEquals(301, dmT1.averageInterFrameDelayVariationBackward().toMillis());
    }

    @Test
    public void testMaxFrameDelayRangeBackward() {
        assertEquals(501, dmT1.maxFrameDelayRangeBackward().toMillis());
    }

    @Test
    public void testAverageFrameDelayRangeBackward() {
        assertEquals(201, dmT1.averageFrameDelayRangeBackward().toMillis());
    }

}
