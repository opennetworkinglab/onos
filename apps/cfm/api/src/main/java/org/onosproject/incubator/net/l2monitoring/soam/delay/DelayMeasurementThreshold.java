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
import java.util.Collection;

import org.onosproject.incubator.net.l2monitoring.soam.SoamId;

/**
 * Object to represent a Delay Measurement Threshold.
 */
public interface DelayMeasurementThreshold {
    /**
     * The identifier or the scheduled measurement.
     * @return The id
     */
    SoamId threshId();

    /**
     * A vector of bits that indicates the type of SOAM LM thresholds notifications that are enabled.
     * A present bit enables the specific SOAM LM threshold notification and
     * when the specific counter is enabled and the threshold is crossed a
     * notification is generated.
     * A not present bit disables the specific SOAM LM threshold notification. If
     * a particular SOAM LM threshold is not supported the BIT value is not present.
     * @return A collection of bit options
     */
    Collection<ThresholdOption> thresholdsEnabled();

    /**
     * The measurement two-way delay threshold used to determine if a threshold notification is generated.
     * @return A java duration
     */
    Duration measuredFrameDelayTwoWay();

    /**
     * The maximum two-way delay threshold used to determine if a threshold notification is generated.
     * @return A java duration
     */
    Duration maxFrameDelayTwoWay();

    /**
     * The average two-way delay threshold used to determine if a threshold notification is generated.
     * @return A java duration
     */
    Duration averageFrameDelayTwoWay();

    /**
     * The measurement two-way IFDV threshold used to determine if a threshold notification is generated.
     * @return A java duration
     */
    Duration measuredInterFrameDelayVariationTwoWay();

    /**
     * The maximum two-way IFDV threshold used to determine if a threshold notification is generated.
     * @return A java duration
     */
    Duration maxInterFrameDelayVariationTwoWay();

    /**
     * The average two-way IFDV threshold used to determine if a threshold notification is generated.
     * @return A java duration
     */
    Duration averageInterFrameDelayVariationTwoWay();

    /**
     * The maximum two-way Frame Delay Range threshold used to determine if a threshold notification is generated.
     * @return A java duration
     */
    Duration maxFrameDelayRangeTwoWay();

    /**
     * The average two-way Frame Delay Range threshold used to determine if a threshold notification is generated.
     * @return A java duration
     */
    Duration averageFrameDelayRangeTwoWay();

    /**
     * The measurement forward delay threshold used to determine if a threshold notification is generated.
     * @return A java duration
     */
    Duration measuredFrameDelayForward();

    /**
     * The maximum forward delay threshold used to determine if a threshold notification is generated.
     * @return A java duration
     */
    Duration maxFrameDelayForward();

    /**
     * The average forward delay threshold used to determine if a threshold notification is generated.
     * @return A java duration
     */
    Duration averageFrameDelayForward();

    /**
     * The measurement IFDV threshold used to determine if a threshold notification is generated.
     * @return A java duration
     */
    Duration measuredInterFrameDelayVariationForward();

    /**
     * The maximum IFDV threshold  used to determine if a threshold notification is generated.
     * @return A java duration
     */
    Duration maxInterFrameDelayVariationForward();

    /**
     * The average IFDV threshold used to determine if a threshold notification is generated.
     * @return A java duration
     */
    Duration averageInterFrameDelayVariationForward();

    /**
     * The maximum Frame Delay Range threshold used to determine if a threshold notification is generated.
     * @return A java duration
     */
    Duration maxFrameDelayRangeForward();

    /**
     * The average Frame Delay Range threshold used to determine if a threshold notification is generated.
     * @return A java duration
     */
    Duration averageFrameDelayRangeForward();

    /**
     * The measurement backward delay threshold used to determine if a threshold notification is generated.
     * @return A java duration
     */
    Duration measuredFrameDelayBackward();

    /**
     * The maximum backward delay threshold used to determine if a threshold notification is generated.
     * @return A java duration
     */
    Duration maxFrameDelayBackward();

    /**
     * The average backward delay threshold used to determine if a threshold notification is generated.
     * @return A java duration
     */
    Duration averageFrameDelayBackward();

    /**
     * The measurement backward IFDV threshold used to determine if a threshold notification is generated.
     * @return A java duration
     */
    Duration measuredInterFrameDelayVariationBackward();

    /**
     * The maximum backward IFDV threshold used to determine if a threshold notification is generated.
     * @return A java duration
     */
    Duration maxInterFrameDelayVariationBackward();

    /**
     * The average backward IFDV threshold used to determine if a threshold notification is generated.
     * @return A java duration
     */
    Duration averageInterFrameDelayVariationBackward();

    /**
     * The maximum backward Frame Delay Range threshold used to determine if a threshold notification is generated.
     * @return A java duration
     */
    Duration maxFrameDelayRangeBackward();

    /**
     * The average backward Frame Delay Range threshold used to determine if a threshold notification is generated.
     * @return A java duration
     */
    Duration averageFrameDelayRangeBackward();

    /**
     * Builder for {@link DelayMeasurementThreshold}.
     */
    public interface DmThresholdBuilder {
        DmThresholdBuilder addToThresholdsEnabled(
                ThresholdOption thresholdEnabled);

        DmThresholdBuilder measuredFrameDelayTwoWay(
                Duration measuredFrameDelayTwoWay);

        DmThresholdBuilder maxFrameDelayTwoWay(Duration maxFrameDelayTwoWay);

        DmThresholdBuilder averageFrameDelayTwoWay(Duration averageFrameDelayTwoWay);

        DmThresholdBuilder measuredInterFrameDelayVariationTwoWay(
                Duration measuredInterFrameDelayVariationTwoWay);

        DmThresholdBuilder maxInterFrameDelayVariationTwoWay(
                Duration maxInterFrameDelayVariationTwoWay);

        DmThresholdBuilder averageInterFrameDelayVariationTwoWay(
                Duration averageInterFrameDelayVariationTwoWay);

        DmThresholdBuilder maxFrameDelayRangeTwoWay(
                Duration maxFrameDelayRangeTwoWay);

        DmThresholdBuilder averageFrameDelayRangeTwoWay(
                Duration averageFrameDelayRangeTwoWay);

        DmThresholdBuilder measuredFrameDelayForward(
                Duration averageFrameDelayRangeTwoWay);

        DmThresholdBuilder maxFrameDelayForward(
                Duration maxFrameDelayForward);

        DmThresholdBuilder averageFrameDelayForward(
                Duration averageFrameDelayForward);

        DmThresholdBuilder measuredInterFrameDelayVariationForward(
                Duration measuredInterFrameDelayVariationForward);

        DmThresholdBuilder maxInterFrameDelayVariationForward(
                Duration maxInterFrameDelayVariationForward);

        DmThresholdBuilder averageInterFrameDelayVariationForward(
                Duration averageInterFrameDelayVariationForward);

        DmThresholdBuilder maxFrameDelayRangeForward(
                Duration maxFrameDelayRangeForward);

        DmThresholdBuilder averageFrameDelayRangeForward(
                Duration averageFrameDelayRangeForward);

        DmThresholdBuilder measuredFrameDelayBackward(
                Duration measuredFrameDelayBackward);

        DmThresholdBuilder maxFrameDelayBackward(
                Duration maxFrameDelayBackward);

        DmThresholdBuilder averageFrameDelayBackward(
                Duration averageFrameDelayBackward);

        DmThresholdBuilder measuredInterFrameDelayVariationBackward(
                Duration measuredInterFrameDelayVariationBackward);

        DmThresholdBuilder maxInterFrameDelayVariationBackward(
                Duration maxInterFrameDelayVariationBackward);

        DmThresholdBuilder averageInterFrameDelayVariationBackward(
                Duration averageInterFrameDelayVariationBackward);

        DmThresholdBuilder maxFrameDelayRangeBackward(
                Duration maxFrameDelayRangeBackward);

        DmThresholdBuilder averageFrameDelayRangeBackward(
                Duration averageFrameDelayRangeBackward);

        public DelayMeasurementThreshold build();

    }

    /**
     * Selection of Threshold choices.
     */
    public enum ThresholdOption {
        MEASURED_FRAME_DELAY_TWO_WAY,
        MAX_FRAME_DELAY_TWO_WAY,
        AVERAGE_FRAME_DELAY_TWO_WAY,
        MEASURED_INTER_FRAME_DELAY_VARIATION_TWO_WAY,
        MAX_INTER_FRAME_DELAY_VARIATION_TWO_WAY,
        AVERAGE_INTER_FRAME_DELAY_VARIATION_TWO_WAY,
        MAX_FRAME_DELAY_RANGE_TWO_WAY,
        AVERAGE_FRAME_DELAY_RANGE_TWO_WAY,
        MEASURED_FRAME_DELAY_FORWARD,
        MAX_FRAME_DELAY_FORWARD,
        AVERAGE_FRAME_DELAY_FORWARD,
        MEASURED_INTER_FRAME_DELAY_VARIATION_FORWARD,
        MAX_INTER_FRAME_DELAY_VARIATION_FORWARD,
        AVERAGE_INTER_FRAME_DELAY_VARIATION_FORWARD,
        MAX_FRAME_DELAY_RANGE_FORWARD,
        AVERAGE_FRAME_DELAY_RANGE_FORWARD,
        MEASURED_FRAME_DELAY_BACKWARD,
        MAX_FRAME_DELAY_BACKWARD,
        AVERAGE_FRAME_DELAY_BACKWARD,
        MEASURED_INTER_FRAME_DELAY_VARIATION_BACKWARD,
        MAX_INTER_FRAME_DELAY_VARIATION_BACKWARD,
        AVERAGE_INTER_FRAME_DELAY_VARIATION_BACKWARD,
        MAX_FRAME_DELAY_RANGE_BACKWARD,
        AVERAGE_FRAME_DELAY_RANGE_BACKWARD;
    }
}
