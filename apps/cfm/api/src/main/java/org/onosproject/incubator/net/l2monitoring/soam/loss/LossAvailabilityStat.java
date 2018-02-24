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

import java.time.Duration;

import org.onosproject.incubator.net.l2monitoring.soam.MilliPct;

/**
 * Abstract base interface to represent Loss Availability Stats.
 */
public interface LossAvailabilityStat {

    Duration elapsedTime();

    /**
     * Indicates whether the Measurement Interval has been marked as suspect.
     * The object is set to false at the start of a measurement interval.
     * Conditions for a discontinuity include, but are not limited to the following:
     * 1 - The local time-of-day clock is adjusted by at least 10 seconds
     * 2 - The conducting of a performance measurement is halted before the
     *  current Measurement Interval is completed
     *  3 - A local test, failure, or reconfiguration that disrupts service
     * @return true when there is a discontinuity in the performance measurements during the Measurement Interval
     */
    boolean suspectStatus();

    /**
     * The number of high loss intervals (HLI) over time in the forward direction.
     * The value starts at 0 and increments for every HLI that occurs.
     * This parameter is equivalent to 'L Sub T' found in MEF 10.2.1
     * @return the number of intervals
     */
    Long forwardHighLoss();

    /**
     * The number of high loss intervals (HLI) over time in the backward direction.
     * The value starts at 0 and increments for every HLI that occurs.
     * This parameter is equivalent to 'L Sub T' found in MEF 10.2.1
     * @return the number of intervals
     */
    Long backwardHighLoss();

    /**
     * The number of consecutive high loss intervals (CHLI) over time in the forward direction.
     * The value starts at 0 and increments for every HLI that occurs.
     * This parameter is equivalent to 'B Sub T' found in MEF 10.2.1
     * @return the number of intervals
     */
    Long forwardConsecutiveHighLoss();

    /**
     * The number of consecutive high loss intervals (CHLI) over time in the backward direction.
     * The value starts at 0 and increments for every HLI that occurs.
     * This parameter is equivalent to 'B Sub T' found in MEF 10.2.1
     * @return the number of intervals
     */
    Long backwardConsecutiveHighLoss();

    /**
     * The number of availability indicators during a small time interval.
     * evaluated as available (low frame loss) in the forward direction by this
     * MEP during this measurement interval.
     * @return The number of availability indicators
     */
    Long forwardAvailable();

    /**
     * The number of availability indicators during a small time interval.
     * evaluated as available (low frame loss) in the backward direction by
     * this MEP during this measurement interval.
     * @return The number of availability indicators
     */
    Long backwardAvailable();

    /**
     * The number of availability indicators during a small time interval.
     * evaluated as unavailable (high frame loss) in the forward direction by
     * this MEP during this measurement interval
     * @return The number of availability indicators
     */
    Long forwardUnavailable();

    /**
     * The number of availability indicators during a small time interval.
     * evaluated as unavailable (high frame loss) in the backward direction by
     * this MEP during this measurement interval
     * @return The number of availability indicators
     */
    Long backwardUnavailable();

    /**
     * The minimum one-way availability flr in the forward direction,.
     * from among the set of availability flr values calculated by the MEP in this Measurement Interval.
     * There is one availability flr value for each 'delta_t' time period within
     * the Measurement Interval, as specified in MEF 10.2.1.
     * The flr value is a ratio that is expressed as a percent with a value of 0
     * (ratio 0.00) through 100000 (ratio 1.00).
     * @return The ratio as 1/1000th of a percent, where 1 indicates 0.001 percent
     */
    MilliPct forwardMinFrameLossRatio();

    /**
     * The maximum one-way availability flr in the forward direction,.
     * from among the set of availability flr values calculated by the MEP in this Measurement Interval.
     * There is one availability flr value for each 'delta_t' time period within
     * the Measurement Interval, as specified in MEF 10.2.1.
     * The flr value is a ratio that is expressed as a percent with a value of 0
     * (ratio 0.00) through 100000 (ratio 1.00).
     * @return The ratio as 1/1000th of a percent, where 1 indicates 0.001 percent
     */
    MilliPct forwardMaxFrameLossRatio();

    /**
     * The average one-way availability flr in the forward direction,.
     * from among the set of availability flr values calculated by the MEP in this Measurement Interval.
     * There is one availability flr value for each 'delta_t' time period within
     * the Measurement Interval, as specified in MEF 10.2.1.
     * The flr value is a ratio that is expressed as a percent with a value of 0
     * (ratio 0.00) through 100000 (ratio 1.00).
     * @return The ratio as 1/1000th of a percent, where 1 indicates 0.001 percent
     */
    MilliPct forwardAverageFrameLossRatio();

    /**
     * The minimum one-way availability flr in the backward direction,.
     * from among the set of availability flr values calculated by the MEP in this Measurement Interval.
     * There is one availability flr value for each 'delta_t' time period within
     * the Measurement Interval, as specified in MEF 10.2.1.
     * The flr value is a ratio that is expressed as a percent with a value of 0
     * (ratio 0.00) through 100000 (ratio 1.00).
     * @return The ratio as 1/1000th of a percent, where 1 indicates 0.001 percent
     */
    MilliPct backwardMinFrameLossRatio();

    /**
     * The maximum one-way availability flr in the backward direction,.
     * from among the set of availability flr values calculated by the MEP in this Measurement Interval.
     * There is one availability flr value for each 'delta_t' time period within
     * the Measurement Interval, as specified in MEF 10.2.1.
     * The flr value is a ratio that is expressed as a percent with a value of 0
     * (ratio 0.00) through 100000 (ratio 1.00).
     * @return The ratio as 1/1000th of a percent, where 1 indicates 0.001 percent
     */
    MilliPct backwardMaxFrameLossRatio();

    /**
     * The average one-way availability flr in the backward direction,.
     * from among the set of availability flr values calculated by the MEP in this Measurement Interval.
     * There is one availability flr value for each 'delta_t' time period within
     * the Measurement Interval, as specified in MEF 10.2.1.
     * The flr value is a ratio that is expressed as a percent with a value of 0
     * (ratio 0.00) through 100000 (ratio 1.00).
     * @return The ratio as 1/1000th of a percent, where 1 indicates 0.001 percent
     */
    MilliPct backwardAverageFrameLossRatio();

    /**
     * Abstract builder for classes derived from LossAvailabilityStat.
     * {@link LossAvailabilityStat}.
     */
    public interface LaStatBuilder {
        LaStatBuilder forwardHighLoss(Long forwardHighLoss);

        LaStatBuilder backwardHighLoss(Long backwardHighLoss);

        LaStatBuilder forwardConsecutiveHighLoss(Long forwardConsecutiveHighLoss);

        LaStatBuilder backwardConsecutiveHighLoss(Long backwardConsecutiveHighLoss);

        LaStatBuilder forwardAvailable(Long forwardAvailable);

        LaStatBuilder backwardAvailable(Long backwardAvailable);

        LaStatBuilder forwardUnavailable(Long forwardUnavailable);

        LaStatBuilder backwardUnavailable(Long backwardUnavailable);

        LaStatBuilder forwardMinFrameLossRatio(MilliPct forwardMinFrameLossRatio);

        LaStatBuilder forwardMaxFrameLossRatio(MilliPct forwardMaxFrameLossRatio);

        LaStatBuilder forwardAverageFrameLossRatio(MilliPct forwardAverageFrameLossRatio);

        LaStatBuilder backwardMinFrameLossRatio(MilliPct backwardMinFrameLossRatio);

        LaStatBuilder backwardMaxFrameLossRatio(MilliPct backwardMaxFrameLossRatio);

        LaStatBuilder backwardAverageFrameLossRatio(MilliPct backwardAverageFrameLossRatio);
    }
}
