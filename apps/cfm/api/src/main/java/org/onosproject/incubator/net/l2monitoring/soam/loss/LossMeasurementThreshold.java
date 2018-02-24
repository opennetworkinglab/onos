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

import java.util.Collection;

import org.onosproject.incubator.net.l2monitoring.soam.MilliPct;
import org.onosproject.incubator.net.l2monitoring.soam.SoamId;

/**
 * Object to support the setting of Loss Measurement Thresholds.
 *
 * The main purpose of the threshold configuration is to configure threshold
 * alarm notifications indicating that a specific performance metric is not being met
 */
public interface LossMeasurementThreshold {
    /**
     * The index of the threshold number for the specific LM threshold entry.
     * An index value of '1' needs to be supported. Other index values can also be supported
     * @return The threshold Id
     */
    SoamId thresholdId();

    /**
     * A vector of bits that indicates the type of SOAM LM thresholds notifications that are enabled.
     * A present but enables the specific SOAM LM threshold notification and
     * when the specific counter is enabled and the threshold is crossed a
     * notification is generated.
     * A not present bit disables the specific SOAM LM threshold notification.
     * If a particular SOAM LM threshold is not supported the BIT value is not present
     * @return A collection of bit options
     */
    Collection<ThresholdOption> thresholds();

    /**
     * The measured forward frame loss ratio threshold value.
     * that will be used to determine if a threshold notification is generated
     * @return Units are in milli-percent, where 1 indicates 0.001 percent
     */
    MilliPct measuredFlrForward();

    /**
     * The maximum forward frame loss ratio threshold value.
     * that will be used to determine if a threshold notification is generated
     * @return Units are in milli-percent, where 1 indicates 0.001 percent
     */
    MilliPct maxFlrForward();

    /**
     * The average forward frame loss ratio threshold value.
     * that will be used to determine if a threshold notification is generated
     * @return Units are in milli-percent, where 1 indicates 0.001 percent
     */
    MilliPct averageFlrForward();

    /**
     * The measured backward frame loss ratio threshold value.
     * that will be used to determine if a threshold notification is generated
     * @return Units are in milli-percent, where 1 indicates 0.001 percent
     */
    MilliPct measuredFlrBackward();

    /**
     * The maximum backward frame loss ratio threshold value.
     * that will be used to determine if a threshold notification is generated
     * @return Units are in milli-percent, where 1 indicates 0.001 percent
     */
    MilliPct maxFlrBackward();

    /**
     * The average backward frame loss ratio threshold value.
     * that will be used to determine if a threshold notification is generated
     * @return Units are in milli-percent, where 1 indicates 0.001 percent
     */
    MilliPct averageFlrBackward();

    /**
     * The forward high loss threshold value.
     * that will be used to determine if a threshold notification is generated.
     * @return The threshold value
     */
    Long forwardHighLoss();

    /**
     * The consecutive forward high loss threshold value.
     * that will be used to determine if a threshold notification is generated
     * @return The threshold value
     */
    Long forwardConsecutiveHighLoss();

    /**
     * The backward high loss threshold value.
     * that will be used to determine if a threshold notification is generated.
     * @return The threshold value
     */
    Long backwardHighLoss();

    /**
     * The consecutive backward high loss threshold value.
     * that will be used to determine if a threshold notification is generated
     * @return The threshold value
     */
    Long backwardConsecutiveHighLoss();

    /**
     * The forward unavailability threshold value.
     * that will be used to determine if a threshold notification is generated
     * @return The threshold value
     */
    Long forwardUnavailableCount();

    /**
     * The forward availability/total time ratio threshold value.
     * that will be used to determine if a threshold notification is generated
     * if the ratio drops below the configured value.
     * The ratio value is expressed as a percent with a value of 0 (ratio 0.00)
     * through 100000 (ratio 1.00)
     * @return Units are in milli-percent, where 1 indicates 0.001 percent
     */
    MilliPct forwardAvailableRatio();

    /**
     * The backward unavailability threshold value.
     * that will be used to determine if a threshold notification is generated
     * @return The threshold value
     */
    Long backwardUnavailableCount();

    /**
     * The backward availability/total time ratio threshold value.
     * that will be used to determine if a threshold notification is generated
     * if the ratio drops below the configured value.
     * The ratio value is expressed as a percent with a value of 0 (ratio 0.00)
     * through 100000 (ratio 1.00)
     * @return Units are in milli-percent, where 1 indicates 0.001 percent
     */
    MilliPct backwardAvailableRatio();

    /**
     * Builder for {@link LossMeasurementThreshold}.
     */
    public interface LmThresholdBuilder {

        LmThresholdBuilder addToThreshold(ThresholdOption threshold);

        LmThresholdBuilder measuredFlrForward(MilliPct measuredFlrForward);

        LmThresholdBuilder maxFlrForward(MilliPct maxFlrForward);

        LmThresholdBuilder averageFlrForward(MilliPct averageFlrForward);

        LmThresholdBuilder measuredFlrBackward(MilliPct measuredFlrBackward);

        LmThresholdBuilder maxFlrBackward(MilliPct maxFlrBackward);

        LmThresholdBuilder averageFlrBackward(MilliPct averageFlrBackward);

        LmThresholdBuilder forwardHighLoss(Long forwardHighLoss);

        LmThresholdBuilder forwardConsecutiveHighLoss(Long forwardConsecutiveHighLoss);

        LmThresholdBuilder backwardHighLoss(Long backwardHighLoss);

        LmThresholdBuilder backwardConsecutiveHighLoss(Long backwardConsecutiveHighLoss);

        LmThresholdBuilder forwardUnavailableCount(Long forwardUnavailableCount);

        LmThresholdBuilder forwardAvailableRatio(MilliPct forwardAvailableRatio);

        LmThresholdBuilder backwardUnavailableCount(Long backwardUnavailableCount);

        LmThresholdBuilder backwardAvailableRatio(MilliPct backwardAvailableRatio);

        LossMeasurementThreshold build();
    }

    /**
     * Set of enumerated threshold options.
     */
    public enum ThresholdOption {
        MEASURED_FLR_FORWARD,
        MAX_FLR_FORWARD,
        AVERAGE_FLR_FORWARD,
        MEASURED_FLR_BACKWARD,
        MAX_FLR_BACKWARD,
        AVERAGE_FLR_BACKWARD,
        FORWARD_HIGH_LOSS,
        FORWARD_CONSECUTIVE_HIGH_LOSS,
        BACKWARD_HIGH_LOSS,
        BACKWARD_CONSECUTIVE_HIGH_LOSS,
        FORWARD_UNAVAILABLE_COUNT,
        FORWARD_AVAILABLE_RATIO,
        BACKWARD_UNAVAILABLE_COUNT,
        BACKWARD_AVAILABLE_RATIO;
    }
}
