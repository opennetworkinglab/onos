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
 * Abstract base interface for the creation of Loss Measurement Stat.
 * This is the base for {@link LossMeasurementStatHistory}
 * and {@link LossMeasurementStatCurrent}
 */
public interface LossMeasurementStat {
    /**
     * The time that the current Measurement Interval has been running.
     * @return A java Duration
     */
    Duration elapsedTime();

    /**
     * The suspect flag for the current measurement interval in which the notification was generated.
     * reference MEF-SOAM-PM-MIB.mefSoamPmNotificationObjSuspect";
     * @return true if the measurement might include an error
     */
    boolean suspectStatus();

    /**
     * The number of frames transmitted in the forward direction by this MEP.
     * For a PM Session of types lmm or ccm this includes Ethernet Service Frames
     * and SOAM PDUs that are in a higher MEG level only.
     * For a PM Session of type slm this includes the count of SOAM ETH-SLM frames only
     * @return The number of frames
     */
    Long forwardTransmittedFrames();

    /**
     * The number of frames received in the forward direction by this MEP.
     * For a PM Session of types lmm or ccm this includes Ethernet
     * Service Frames and SOAM PDUs that are in a higher MEG level only.
     * For a PM Session of type slm this includes the count of SOAM ETH-SLM frames only
     * @return The number of frames received
     */
    Long forwardReceivedFrames();

    /**
     * The minimum one-way frame loss ratio in the forward direction calculated by this MEP for this Interval.
     * The FLR value is a ratio that is expressed as a percent with a value of
     * 0 (ratio 0.00) through 100000 (ratio 1.00).
     * @return Units are in milli-percent, where 1 indicates 0.001 percent
     */
    MilliPct forwardMinFrameLossRatio();

    /**
     * The maximum one-way frame loss ratio in the forward direction calculated by this MEP for this Interval.
     * The FLR value is a ratio that is expressed as a percent with a value of
     * 0 (ratio 0.00) through 100000 (ratio 1.00).
     * @return Units are in milli-percent, where 1 indicates 0.001 percent
     */
    MilliPct forwardMaxFrameLossRatio();

    /**
     * The average one-way frame loss ratio in the forward direction calculated by this MEP for this Interval.
     * The FLR value is a ratio that is expressed as a percent with a value of
     * 0 (ratio 0.00) through 100000 (ratio 1.00).
     * @return Units are in milli-percent, where 1 indicates 0.001 percent
     */
    MilliPct forwardAverageFrameLossRatio();

    /**
     * The number of frames transmitted in the backward direction by this MEP.
     * For a PM Session of type lmm or ccm this includes Ethernet Service Frames
     * and SOAM PDUs that are in a higher MEG level only.
     * For a PM Session of type slm this includes the count of SOAM ETH-SLM frames only
     * @return The number of frames
     */
    Long backwardTransmittedFrames();

    /**
     * The number of frames received in the backward direction by this MEP.
     * For a PM Session of type lmm this includes Ethernet Service Frames and
     * SOAM PDUs that are in a higher MEG level only.
     * For a PM Session of type slm this includes the count of SOAM ETH-SLM frames only
     * @return The number of frames
     */
    Long backwardReceivedFrames();

    /**
     * The minimum one-way frame loss ratio in the backward direction.
     * calculated by this MEP for this Measurement Interval. The FLR value is a
     * ratio that is expressed as a percent with a value of 0 (ratio 0.00)
     * through 100000 (ratio 1.00).
     * @return Units are in milli-percent, where 1 indicates 0.001 percent
     */
    MilliPct backwardMinFrameLossRatio();

    /**
     * The maximum one-way frame loss ratio in the backward direction.
     * calculated by this MEP for this Measurement Interval. The FLR value is a
     * ratio that is expressed as a percent with a value of 0 (ratio 0.00)
     * through 100000 (ratio 1.00).
     * @return Units are in milli-percent, where 1 indicates 0.001 percent
     */
    MilliPct backwardMaxFrameLossRatio();

    /**
     * The average one-way frame loss ratio in the backward direction.
     * calculated by this MEP for this Measurement Interval. The FLR value is a
     * ratio that is expressed as a percent with a value of 0 (ratio 0.00)
     * through 100000 (ratio 1.00).
     * @return Units are in milli-percent, where 1 indicates 0.001 percent
     */
    MilliPct backwardAverageFrameLossRatio();

    /**
     * The count of the number of SOAM PDUs sent during this Measurement Interval.
     * This object applies when type is lmm, slm or ccm. It indicates the number
     * of LMM, CCM, or SLM SOAM frames transmitted
     * @return the number of SOAM PDUs sent
     */
    Long soamPdusSent();

    /**
     * The count of the number of SOAM PDUs PDUs received in this Measurement Interval.
     * This object applies when type is lmm, slm, or ccm. This object indicates
     * the number of LMR, CCM, or SLR SOAM frames received
     * @return the number of SOAM PDUs PDUs received
     */
    Long soamPdusReceived();

    /**
     * Base interface for builders of {@link LossMeasurementStat}.
     */
    interface LmStatBuilder {
        LmStatBuilder forwardTransmittedFrames(Long forwardTransmittedFrames);

        LmStatBuilder forwardReceivedFrames(Long forwardReceivedFrames);

        LmStatBuilder forwardMinFrameLossRatio(MilliPct forwardMinFrameLossRatio);

        LmStatBuilder forwardMaxFrameLossRatio(MilliPct forwardMaxFrameLossRatio);

        LmStatBuilder forwardAverageFrameLossRatio(MilliPct forwardAverageFrameLossRatio);

        LmStatBuilder backwardTransmittedFrames(Long backwardTransmittedFrames);

        LmStatBuilder backwardReceivedFrames(Long backwardReceivedFrames);

        LmStatBuilder backwardMinFrameLossRatio(MilliPct backwardMinFrameLossRatio);

        LmStatBuilder backwardMaxFrameLossRatio(MilliPct backwardMaxFrameLossRatio);

        LmStatBuilder backwardAverageFrameLossRatio(MilliPct backwardAverageFrameLossRatio);

        LmStatBuilder soamPdusSent(Long soamPdusSent);

        LmStatBuilder soamPdusReceived(Long soamPdusReceived);
    }
}
