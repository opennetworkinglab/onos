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
import java.util.Collection;

import org.onosproject.incubator.net.l2monitoring.soam.MeasurementCreateBase;
import org.onosproject.incubator.net.l2monitoring.soam.MilliPct;
import org.onosproject.incubator.net.l2monitoring.soam.SoamConfigException;

/**
 * Object to support creation of Loss Measurement tests.
 */
public interface LossMeasurementCreate extends MeasurementCreateBase {
    /**
     * The type of Loss Measurement that will be performed.
     * @return A loss Measurement type
     */
    LmType lmCfgType();

    /**
     * A vector of bits that indicates the type of SOAM LM counters found.
     * in the current-stats and history-stats that are enabled.
     * A present bit enables the specific SOAM LM counter. A not present bit disables the SOAM LM counter.
     * If a particular SOAM LM counter is not supported the BIT value is not present.
     * Not all SOAM LM counters are supported for all SOAM LM types.
     * @return A collection of bit options
     */
    Collection<CounterOption> countersEnabled();

    /**
     * This object specifies the availability measurement interval.
     * A measurement interval of 15 minutes is to be supported, other intervals can be supported
     * @return A java Duration
     */
    Duration availabilityMeasurementInterval();

    /**
     * Specifies a configurable number of consecutive loss measurement PDUs.
     * to be used in evaluating the availability/unavailability status of each
     * availability indicator per MEF 10.2.1.
     * Loss Measurement PDUs (LMMs, CCMs or SLMs) are sent regularly with a
     * period defined by message-period.  Therefore, this object, when multiplied
     * by message-period, is equivalent to the Availability parameter of 'delta_t'
     * as specified by MEF 10.2.1.
     *
     * If the measurement-type is lmm or ccm, this object defines the number of
     * LMM or CCM PDUs transmitted during each 'delta_t' period.  The Availability
     * flr for a given 'delta_t' can be calculated based on the counters in the
     * last LMM/R or CCM during this 'delta_t' and the last LMM/R or CCM in the
     * previous 'delta_t'.
     *
     * If the measurement-type is slm, this object defines the number of SLM PDUs
     * transmitted during each 'delta_t' period.  The Availability flr for a
     * given 'delta_t' is calculated based on the number of those SLM PDUs that are lost.
     *
     * If the measurement-type is lmm or ccm, the number range of 1 through 10
     * must be supported. The number range of 10 through 1000000 may be supported,
     * but is not mandatory.
     *
     * If the measurement-type is slm, the number range of 10 through 100 must be
     * supported. The number range of 100 through 1000000 may be supported,
     * but is not mandatory
     * @return number of consecutive loss measurement PDUs
     */
    Integer availabilityNumberConsecutiveFlrMeasurements();

    /**
     * Specifies a configurable availability threshold to be used in evaluating
     * the availability/unavailability status of an availability indicator per
     * MEF 10.2.1. The availability threshold range of 0.00 (0) through 1.00 (100000)
     * is supported. This parameter is equivalent to the Availability parameter
     * of 'C' as specified by MEF 10.2.1.
     * @return Units are in milli-percent, where 1 indicates 0.001 percent
     */
    MilliPct availabilityFlrThreshold();

    /**
     * Specifies a configurable number of consecutive availability indicators to
     * be used to determine a change in the availability status as indicated by
     * MEF 10.2.1. This parameter is equivalent to the Availability parameter of
     * 'n' as specified by MEF 10.2.1.
     * The number range of 1 through 10 must be supported.
     * The number range of 1 through 1000 may be supported, but is not mandatory
     * @return number of consecutive availability indicators
     */
    Short availabilityNumberConsecutiveIntervals();

    /**
     * Specifies a configurable number of consecutive availability indicators.
     * to be used for assessing CHLI.  This parameter is equivalent to the
     * Resilency parameter of 'p' as specified by MEF 10.2.1.
     *
     * Availability-consecutive-high-flr must be strictly less than
     * availability-number-consecutive-intervals. If not, the count of high loss
     * intervals over time, and the count of consecutive high loss levels, is disabled.
     *
     * The number range of 1 through 10 must be supported. The number range of 1
     * through 1000 may be supported, but is not mandatory
     * @return number of consecutive availability indicators
     */
    Short availabilityNumberConsecutiveHighFlr();

    /**
     * The list of Loss Measurement configuration threshold values for LM Performance Monitoring.
     * The main purpose of the threshold configuration list is to configure
     * threshold alarm notifications indicating that a specific performance metric
     * is not being met
     * @return A collection of Loss Measurement Thresholds
     */
    Collection<LossMeasurementThreshold> lossMeasurementThreshold();

    /**
     * Builder for {@link LossMeasurementCreate}.
     */
    public interface LmCreateBuilder extends MeasCreateBaseBuilder {

        LmCreateBuilder addToCountersEnabled(CounterOption counterOption);

        LmCreateBuilder availabilityMeasurementInterval(
                Duration availabilityMeasurementInterval);

        LmCreateBuilder availabilityNumberConsecutiveFlrMeasurements(
                Integer availabilityNumberConsecutiveFlrMeasurements);

        LmCreateBuilder availabilityFlrThreshold(MilliPct availabilityFlrThreshold);

        LmCreateBuilder availabilityNumberConsecutiveIntervals(
                Short availabilityNumberConsecutiveIntervals) throws SoamConfigException;

        LmCreateBuilder availabilityNumberConsecutiveHighFlr(
                Short availabilityNumberConsecutiveHighFlr) throws SoamConfigException;

        LmCreateBuilder addToLossMeasurementThreshold(
                LossMeasurementThreshold lossMeasurementThreshold);

        LossMeasurementCreate build();
    }

    /**
     * Enumerated set of Loss Measurement types.
     */
    public enum LmType {
        /**
         * LMM SOAM PDU generated and received LMR responses tracked.
         */
        LMLMM,
        /**
         * SLM SOAM PDU generated and received SLR responses tracked.
         */
        LMSLM,
        /**
         * CCM SOAM PDU generated and received CCM PDUs tracked.
         */
        LMCCM;
    }

    /**
     * Options for Counters that may be enabled.
     */
    public enum CounterOption {
        FORWARD_TRANSMITTED_FRAMES,
        FORWARD_RECEIVED_FRAMES,
        FORWARD_MIN_FLR,
        FORWARD_MAX_FLR,
        FORWARD_AVERAGE_FLR,
        BACKWARD_TRANSMITTED_FRAMES,
        BACKWARD_RECEIVED_FRAMES,
        BACKWARD_MIN_FLR,
        BACKWARD_MAX_FLR,
        BACKWARD_AVERAGE_FLR,
        SOAM_PDUS_SENT,
        SOAM_PDUS_RECEIVED,
        AVAILABILITY_FORWARD_HIGH_LOSS,
        AVAILABILITY_FORWARD_CONSECUTIVE_HIGH_LOSS,
        AVAILABILITY_FORWARD_AVAILABLE,
        AVAILABILITY_FORWARD_UNAVAILABLE,
        AVAILABILILITY_FORWARD_MIN_FLR,
        AVAILABILITY_FORWARD_MAX_FLR,
        AVAILABILITY_FORWARD_AVERAGE_FLR,
        AVAILABILITY_BACKWARD_HIGH_LOSS,
        AVAILABILITY_BACKWARD_CONSECUTIVE_HIGH_LOSS,
        AVAILABILITY_BACKWARD_AVAILABLE,
        AVAILABLE_BACKWARD_UNAVAILABLE,
        AVAILABLE_BACKWARD_MIN_FLR,
        AVAILABLE_BACKWARD_MAX_FLR,
        AVAILABLE_BACKWARD_AVERAGE_FLR,
        MEASURED_STATS_FORWARD_MEASURED_FLR,
        MEASURED_STATS_BACKWARD_MEASURED_FLR,
        MEASURED_STATS_AVAILABILITY_FORWARD_STATUS,
        MEASURED_STATS_AVAILABILITY_BACKWARD_STATUS;
    }
}
