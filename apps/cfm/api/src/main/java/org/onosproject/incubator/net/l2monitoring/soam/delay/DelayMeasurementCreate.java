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

import java.util.Collection;

import org.onosproject.incubator.net.l2monitoring.soam.MeasurementCreateBase;
import org.onosproject.incubator.net.l2monitoring.soam.SoamConfigException;

/**
 * A model of Delay Measurement from ITU Y.1731 Chapter 8.2, MEF 17, MEF 36.1 and MEF 39.
 *
 * In this model delay measurements entries are returned as a collection in the
 * MepEntry. In this way Delay Measurements are created by calling on the
 * Create Delay Measurement function, passing this DelayMeasurementCreate object
 * and any other arguments needed.
 * The Delay Measurement Entry is a result and not configured or
 * persisted in ONOS, but instead is is passed on to some analytics system
 */
public interface DelayMeasurementCreate extends MeasurementCreateBase {
    /**
     * The type of Delay Measurement is to be performed.
     * The exact PDUs to use are specified by this object in combination with version
     * @return enumerated type
     */
    DmType dmCfgType();

    /**
     * A vector of bits that indicates the type of SOAM DM counters that are enabled.
     * A present bit enables the specific SOAM DM counter.
     * A not present bit disables the SOAM DM counter.
     * If a particular SOAM DM counter is not supported the BIT value is not present.
     * Not all SOAM DM counters are supported for all SOAM DM types.
     * @return A collection of options
     */
    Collection<MeasurementOption> measurementsEnabled();

    /**
     * The number of measurement bins per Measurement Interval for Frame Delay measurements.
     * At least 3 bins are to be supported; at least 10 bins are recommended to be supported
     * @return the number of bins
     */
    Short binsPerFdInterval();

    /**
     * The number of measurement bins per Measurement Interval for Inter-Frame Delay Variation measurements.
     * The minimum number of measurement bins to be supported is 2. The desired
     * number of measurements bins to be supported is 10
     * @return the number of bins
     */
    Short binsPerIfdvInterval();

    /**
     * The selection offset for Inter-Frame Delay Variation measurements.
     * If this value is set to n, then the IFDV is calculated by taking the
     * difference in frame delay between frame F and frame (F+n).
     * reference: MEF-SOAM-PM-MIB.mefSoamDmCfgInterFrameDelayVariationSelectionOffset
     * @return The selection offset
     */
    Short ifdvSelectionOffset();

    /**
     * The number of measurement bins per Measurement Interval for Frame Delay Range measurements.
     * @return the number of bins
     */
    Short binsPerFdrInterval();

    /**
     * The Delay Measurement threshold configuration values for DM Performance Monitoring.
     * The main purpose of the threshold configuration list is to configure
     * threshold alarm notifications indicating that a specific performance metric
     * is not being met
     * @return a collection of Thresholds
     */
    Collection<DelayMeasurementThreshold> thresholds();

    /**
     * Builder for {@link DelayMeasurementCreate}.
     */
    public interface DmCreateBuilder extends MeasCreateBaseBuilder {

        DmCreateBuilder addToMeasurementsEnabled(
                MeasurementOption measurementEnabled);

        DmCreateBuilder binsPerFdInterval(Short binsPerFdInterval)
                throws SoamConfigException;

        DmCreateBuilder binsPerIfdvInterval(Short binsPerIfdvInterval)
                throws SoamConfigException;

        DmCreateBuilder ifdvSelectionOffset(Short ifdvSelectionOffset)
                throws SoamConfigException;

        DmCreateBuilder binsPerFdrInterval(Short binsPerFdrInterval)
                throws SoamConfigException;

        DmCreateBuilder addToThresholds(DelayMeasurementThreshold threshold);

        DelayMeasurementCreate build();
    }

    /**
     * Enumerated options for Delay Measurement Types.
     */
    public enum DmType {
        /**
         * DMM SOAM PDU generated, DMR responses received (one-way or two-way measurements).
         */
        DMDMM,
        /**
         * 1DM SOAM PDU generated (one-way measurements are made by the receiver).
         */
        DM1DMTX,
        /**
         * 1DM SOAM PDU received and tracked (one-way measurements).
         */
        DM1DMRX
    }

    /**
     * Supported Versions of Y.1731.
     */
    public enum Version {
        Y17312008("Y.1731-2008"),
        Y17312011("Y.1731-2011");

        private String literal;
        private Version(String literal) {
            this.literal = literal;
        }

        public String literal() {
            return literal;
        }
    }

    /**
     * Selection of Measurement types.
     */
    public enum MeasurementOption {
        SOAM_PDUS_SENT,
        SOAM_PDUS_RECEIVED,
        FRAME_DELAY_TWO_WAY_BINS,
        FRAME_DELAY_TWO_WAY_MIN,
        FRAME_DELAY_TWO_WAY_MAX,
        FRAME_DELAY_TWO_WAY_AVERAGE,
        FRAME_DELAY_FORWARD_BINS,
        FRAME_DELAY_FORWARD_MIN,
        FRAME_DELAY_FORWARD_MAX,
        FRAME_DELAY_FORWARD_AVERAGE,
        FRAME_DELAY_BACKWARD_BINS,
        FRAME_DELAY_BACKWARD_MIN,
        FRAME_DELAY_BACKWARD_MAX,
        FRAME_DELAY_BACKWARD_AVERAGE,
        INTER_FRAME_DELAY_VARIATION_FORWARD_BINS,
        INTER_FRAME_DELAY_VARIATION_FORWARD_MIN,
        INTER_FRAME_DELAY_VARIATION_FORWARD_MAX,
        INTER_FRAME_DELAY_VARIATION_FORWARD_AVERAGE,
        INTER_FRAME_DELAY_VARIATION_BACKWARD_BINS,
        INTER_FRAME_DELAY_VARIATION_BACKWARD_MIN,
        INTER_FRAME_DELAY_VARIATION_BACKWARD_MAX,
        INTER_FRAME_DELAY_VARIATION_BACKWARD_AVERAGE,
        INTER_FRAME_DELAY_VARIATION_TWO_WAY_BINS,
        INTER_FRAME_DELAY_VARIATION_TWO_WAY_MIN,
        INTER_FRAME_DELAY_VARIATION_TWO_WAY_MAX,
        INTER_FRAME_DELAY_VARIATION_TWO_WAY_AVERAGE,
        FRAME_DELAY_RANGE_FORWARD_BINS,
        FRAME_DELAY_RANGE_FORWARD_MAX,
        FRAME_DELAY_RANGE_FORWARD_AVERAGE,
        FRAME_DELAY_RANGE_BACKWARD_BINS,
        FRAME_DELAY_RANGE_BACKWARD_MAX,
        FRAME_DELAY_RANGE_BACKWARD_AVERAGE,
        FRAME_DELAY_RANGE_TWO_WAY_BINS,
        FRAME_DELAY_RANGE_TWO_WAY_MAX,
        FRAME_DELAY_RANGE_TWO_WAY_AVERAGE,
        MEASURED_STATS_FRAME_DELAY_TWO_WAY,
        MEASURED_STATS_FRAME_DELAY_FORWARD,
        MEASURED_STATS_FRAME_DELAY_BACKWARD,
        MEASURED_STATS_INTER_FRAME_DELAY_VARIATION_TWO_WAY,
        MEASURED_STATS_INTER_FRAME_DELAY_VARIATION_FORWARD,
        MEASURED_STATS_INTER_FRAME_DELAY_VARIATION_BACKWARD;
    }

    /**
     * Selection of Data Patterns.
     */
    public enum DataPattern {
        ZEROES,
        ONES;
    }

    /**
     * Selection of Test TLV Patterns.
     */
    public enum TestTlvPattern {
        /**
         * This test pattern is a Null signal without CRC-32.
         */
        NULL_SIGNAL_WITHOUT_CRC_32,
        /**
         * This test pattern is a Null signal with CRC-32.
         */
        NULL_SIGNAL_WITH_CRC_32,
        /**
         * This test pattern is a PRBS 2^31-1 without CRC-32.
         */
        PRBS_2311_WITHOUT_CRC_32,
        /**
         * This test pattern is a PRBS 2^31-1 with CRC-32.
         */
        PRBS_2311_WITH_CRC_32;
    }
}
