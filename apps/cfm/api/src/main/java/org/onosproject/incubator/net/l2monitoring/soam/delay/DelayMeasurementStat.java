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
import java.util.Map;

/**
 * Abstract object as a base for DelayMeasurementStatCurrent and DelayMeasurementStatHistory interfaces.
 */
public interface DelayMeasurementStat {
    /**
     * The time that the current Measurement Interval has been running.
     * @return A java duration
     */
    Duration elapsedTime();

    /**
     * The suspect flag for the current measurement interval in which the notification was generated.
     * reference MEF-SOAM-PM-MIB.mefSoamPmNotificationObjSuspect";
     * @return true if the measurement might include an error
     */
    boolean suspectStatus();

    /**
     * The minimum two-way frame delay calculated by this MEP for this Measurement Interval.
     * @return A java duration
     */
    Duration frameDelayTwoWayMin();

    /**
     * The maximum two-way frame delay calculated by this MEP for this Measurement Interval.
     * @return A java duration
     */
    Duration frameDelayTwoWayMax();

    /**
     * The average two-way frame delay calculated by this MEP for this Measurement Interval.
     * @return A java duration
     */
    Duration frameDelayTwoWayAvg();

    /**
     * The minimum foward frame delay calculated by this MEP for this Measurement Interval.
     * @return A java duration
     */
    Duration frameDelayForwardMin();

    /**
     * The maximum forward frame delay calculated by this MEP for this Measurement Interval.
     * @return A java duration
     */
    Duration frameDelayForwardMax();

    /**
     * The average forward frame delay calculated by this MEP for this Measurement Interval.
     * @return A java duration
     */
    Duration frameDelayForwardAvg();

    /**
     * The minimum backward frame delay calculated by this MEP for this Measurement Interval.
     * @return A java duration
     */
    Duration frameDelayBackwardMin();

    /**
     * The maximum backward frame delay calculated by this MEP for this Measurement Interval.
     * @return A java duration
     */
    Duration frameDelayBackwardMax();

    /**
     * The average backward frame delay calculated by this MEP for this Measurement Interval.
     * @return A java duration
     */
    Duration frameDelayBackwardAvg();

    /**
     * The minimum two-way inter-frame delay interval calculated by this MEP for this Measurement Interval.
     * @return A java duration
     */
    Duration interFrameDelayVariationTwoWayMin();

    /**
     * The maximum two-way inter-frame delay interval calculated by this MEP for this Measurement Interval.
     * @return A java duration
     */
    Duration interFrameDelayVariationTwoWayMax();

    /**
     * The average two-way inter-frame delay interval calculated by this MEP for this Measurement Interval.
     * @return A java duration
     */
    Duration interFrameDelayVariationTwoWayAvg();

    /**
     * The minimum one-way inter-frame delay interval in the forward direction.
     * calculated by this MEP for this Measurement Interval.
     * @return A java duration
     */
    Duration interFrameDelayVariationForwardMin();

    /**
     * The maximum one-way inter-frame delay interval in the forward direction.
     * calculated by this MEP for this Measurement Interval.
     * @return A java duration
     */
    Duration interFrameDelayVariationForwardMax();

    /**
     * The average one-way inter-frame delay interval in the forward direction.
     * calculated by this MEP for this Measurement Interval.
     * @return A java duration
     */
    Duration interFrameDelayVariationForwardAvg();

    /**
     * The minimum one-way inter-frame delay interval in the backward direction.
     * calculated by this MEP for this Measurement Interval.
     * @return A java duration
     */
    Duration interFrameDelayVariationBackwardMin();

    /**
     * The maximum one-way inter-frame delay interval in the backward direction.
     * calculated by this MEP for this Measurement Interval.
     * @return A java duration
     */
    Duration interFrameDelayVariationBackwardMax();

    /**
     * The average one-way inter-frame delay interval in the backward direction.
     * calculated by this MEP for this Measurement Interval.
     * @return A java duration
     */
    Duration interFrameDelayVariationBackwardAvg();

    /**
     * The maximum two-way Frame Delay Range calculated by this MEP for this Measurement Interval.
     * @return A java duration
     */
    Duration frameDelayRangeTwoWayMax();

    /**
     * The average two-way Frame Delay Range calculated by this MEP for this Measurement Interval.
     * @return A java duration
     */
    Duration frameDelayRangeTwoWayAvg();

    /**
     * The maximum one-way Frame Delay Range in the forward direction.
     * calculated by this MEP for this Measurement Interval
     * @return A java duration
     */
    Duration frameDelayRangeForwardMax();

    /**
     * The average one-way Frame Delay Range in the forward direction.
     * calculated by this MEP for this Measurement Interval
     * @return A java duration
     */
    Duration frameDelayRangeForwardAvg();

    /**
     * The maximum one-way Frame Delay Range in the backward direction.
     * calculated by this MEP for this Measurement Interval
     * @return A java duration
     */
    Duration frameDelayRangeBackwardMax();

    /**
     * The average one-way Frame Delay Range in the backward direction.
     * calculated by this MEP for this Measurement Interval
     * @return A java duration
     */
    Duration frameDelayRangeBackwardAvg();

    /**
     * The count of the number of SOAM PDUs sent during this Measurement Interval.
     * @return the count as an integer
     */
    Integer soamPdusSent();

    /**
     * The count of the number of SOAM PDUs received during this Measurement Interval.
     * @return the count as an integer
     */
    Integer soamPdusReceived();

    /**
     * Bins calculated for two-way Frame Delay measurements.
     * calculated by this MEP for this Measurement interval.
     *
     * The reply contains a set of counts of packets per bin
     * The bin is defined by the durations given as the lower limit and the next
     * size being the upper limit. For the largest duration, the count is of packets
     * of that size up to infinity
     *
     * For example, if there are 4 elements in the result
     * PT0.000S 4 - This means there are 4 packets in the 0-20ms bin
     * PT0.020S 6 - This means there are 6 packets in the 20-30ms bin
     * PT0.030S 8 - This means there are 8 packets in the 30-50ms bin
     * PT0.050S 10 - This means there are 10 packets in the 50ms-infinity bin
     *
     * @return A map of counts per time periods
     */
    Map<Duration, Integer> frameDelayTwoWayBins();

    /**
     * Bins calculated for one-way Frame Delay measurements in the Forward direction.
     * calculated by this MEP for this Measurement interval.
     * @return A map of counts per time periods
     */
    Map<Duration, Integer> frameDelayForwardBins();

    /**
     * Bins calculated for one-way Frame Delay measurements in the Backward direction.
     * calculated by this MEP for this Measurement interval.
     * @return A map of counts per time periods
     */
    Map<Duration, Integer> frameDelayBackwardBins();

    /**
     * Bins calculated for two-way Inter Frame Delay Variation measurements.
     * calculated by this MEP for this Measurement interval.
     * @return A map of counts per time periods
     */
    Map<Duration, Integer> interFrameDelayVariationTwoWayBins();

    /**
     * Bins calculated for one-way Inter Frame Delay Variation measurements in the Forward direction.
     * calculated by this MEP for this Measurement interval.
     * @return A map of counts per time periods
     */
    Map<Duration, Integer> interFrameDelayVariationForwardBins();

    /**
     * Bins calculated for one-way Inter Frame Delay Variation measurements in the Backward direction.
     * calculated by this MEP for this Measurement interval.
     * @return A map of counts per time periods
     */
    Map<Duration, Integer> interFrameDelayVariationBackwardBins();

    /**
     * Bins calculated for two-way Frame Delay Range measurements.
     * calculated by this MEP for this Measurement interval.
     * @return A map of counts per time periods
     */
    Map<Duration, Integer> frameDelayRangeTwoWayBins();

    /**
     * Bins calculated for one-way Frame Delay Range measurements in the Forward direction.
     * calculated by this MEP for this Measurement interval.
     * @return A map of counts per time periods
     */
    Map<Duration, Integer> frameDelayRangeForwardBins();

    /**
     * Bins calculated for one-way Frame Delay Range measurements in the Backward direction.
     * calculated by this MEP for this Measurement interval.
     * @return A map of counts per time periods
     */
    Map<Duration, Integer> frameDelayRangeBackwardBins();

    /**
     * Abstract Builder interface for DelayMeasurementStat.
     * {@link DelayMeasurementStat}.
     */
    interface DmStatBuilder {
        DmStatBuilder frameDelayTwoWayMin(Duration frameDelayTwoWayMin);

        DmStatBuilder frameDelayTwoWayMax(Duration frameDelayTwoWayMax);

        DmStatBuilder frameDelayTwoWayAvg(Duration frameDelayTwoWayAvg);

        DmStatBuilder frameDelayForwardMin(Duration frameDelayForwardMin);

        DmStatBuilder frameDelayForwardMax(Duration frameDelayForwardMax);

        DmStatBuilder frameDelayForwardAvg(Duration frameDelayForwardAvg);

        DmStatBuilder frameDelayBackwardMin(Duration frameDelayBackwardMin);

        DmStatBuilder frameDelayBackwardMax(Duration frameDelayBackwardMax);

        DmStatBuilder frameDelayBackwardAvg(Duration frameDelayBackwardAvg);

        DmStatBuilder interFrameDelayVariationTwoWayMin(Duration interFrameDelayVariationTwoWayMin);

        DmStatBuilder interFrameDelayVariationTwoWayMax(Duration interFrameDelayVariationTwoWayMax);

        DmStatBuilder interFrameDelayVariationTwoWayAvg(Duration interFrameDelayVariationTwoWayAvg);

        DmStatBuilder interFrameDelayVariationForwardMin(Duration interFrameDelayVariationForwardMin);

        DmStatBuilder interFrameDelayVariationForwardMax(Duration interFrameDelayVariationForwardMax);

        DmStatBuilder interFrameDelayVariationForwardAvg(Duration interFrameDelayVariationForwardAvg);

        DmStatBuilder interFrameDelayVariationBackwardMin(Duration interFrameDelayVariationBackwardMin);

        DmStatBuilder interFrameDelayVariationBackwardMax(Duration interFrameDelayVariationBackwardMax);

        DmStatBuilder interFrameDelayVariationBackwardAvg(Duration interFrameDelayVariationBackwardAvg);

        DmStatBuilder frameDelayRangeTwoWayMax(Duration frameDelayRangeTwoWayMax);

        DmStatBuilder frameDelayRangeTwoWayAvg(Duration frameDelayRangeTwoWayAvg);

        DmStatBuilder frameDelayRangeForwardMax(Duration frameDelayRangeForwardMax);

        DmStatBuilder frameDelayRangeForwardAvg(Duration frameDelayRangeForwardAvg);

        DmStatBuilder frameDelayRangeBackwardMax(Duration frameDelayRangeBackwardMax);

        DmStatBuilder frameDelayRangeBackwardAvg(Duration frameDelayRangeBackwardAvg);

        DmStatBuilder soamPdusSent(Integer soamPdusSent);

        DmStatBuilder soamPdusReceived(Integer soamPdusReceived);

        DmStatBuilder frameDelayTwoWayBins(Map<Duration, Integer> frameDelayTwoWayBins);

        DmStatBuilder frameDelayForwardBins(Map<Duration, Integer> frameDelayForwardBins);

        DmStatBuilder frameDelayBackwardBins(Map<Duration, Integer> frameDelayBackwardBins);

        DmStatBuilder interFrameDelayVariationTwoWayBins(Map<Duration, Integer> interFrameDelayVariationTwoWayBins);

        DmStatBuilder interFrameDelayVariationForwardBins(Map<Duration, Integer> interFrameDelayVariationForwardBins);

        DmStatBuilder interFrameDelayVariationBackwardBins(Map<Duration, Integer> interFrameDelayVariationBackwardBins);

        DmStatBuilder frameDelayRangeTwoWayBins(Map<Duration, Integer> frameDelayRangeTwoWayBins);

        DmStatBuilder frameDelayRangeForwardBins(Map<Duration, Integer> frameDelayRangeForwardBins);

        DmStatBuilder frameDelayRangeBackwardBins(Map<Duration, Integer> frameDelayRangeBackwardBins);

        DelayMeasurementStat build();
    }
}
