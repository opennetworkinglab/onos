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
package org.onosproject.incubator.net.l2monitoring.soam;

import java.time.Duration;

import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepId;
import org.onosproject.incubator.net.l2monitoring.cfm.Mep;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementCreate;

/**
 * A base interface with attributes that are common to both Delay and Loss Measurements.
 */
public interface MeasurementCreateBase {
    /**
     * The version of the PDUs used to perform Loss or Delay Measurement.
     * The exact PDUs to use are specified by this object in combination with measurement-type
     * @return The version of the PDUs
     */
    DelayMeasurementCreate.Version version();

    /**
     * The remote MEP to perform the tests against.
     * @return An ID of a MEP
     */
    MepId remoteMepId();

    /**
     * The interval between Loss or Delay Measurement OAM message transmission.
     * For Loss Measurement monitoring applications the default value is 1 sec.
     * This object is not applicable if measurement-type is set to 'ccm' and is ignored for that Loss Measurement Type
     * @return A java Duration
     */
    Duration messagePeriod();

    /**
     * The priority of frames with Performance Monitoring OAM message information.
     * @return A priority enumerated value 0-7
     */
    Mep.Priority priority();

    /**
     * The Loss Measurement frame size between 64 bytes and the maximum transmission unit of the EVC.
     * The range of frame sizes from 64 through 2000 octets need to be supported,
     * and the range of frame sizes from 2001 through 9600 octets is suggested be supported.
     * The adjustment to the frame size of the standard frame size is accomplished
     * by the addition of a Data or Test TLV. A Data or Test TLV is only added to
     * the frame if the frame size is greater than 64 bytes
     * @return frame size in bytes
     */
    Short frameSize();

    /**
     * The LM data pattern included in a Data TLV.
     * when the size of the LM frame is determined by the frame-size object and test-tlv-included is 'false'.
     * If the frame size object does not define the LM frame size or
     * test-tlv-included is 'true' the value of this object is ignored
     * @return The data pattern - ones or zeroes
     */
    DelayMeasurementCreate.DataPattern dataPattern();

    /**
     * Whether a Test TLV or Data TLV is included when the size of the LM frame is determined by the frame-size object.
     * If the frame-size object does not define the LM frame size the value of
     * this object is ignored.
     * @return true indicates that the Test TLV is to be included, false indicates it is not
     */
    boolean testTlvIncluded();

    /**
     * The type of test pattern to be sent in the LM frame Test TLV.
     * when the size of LM PDU is determined by the frame-size object and
     * test-tlv-included is 'true'.
     * If the frame size object does not define the LM frame size or
     * test-tlv-included is 'false' the value of this object is ignored
     * @return A TLV pattern enum
     */
    DelayMeasurementCreate.TestTlvPattern testTlvPattern();

    /**
     * The Measurement Interval for FLR statistics.
     * A Measurement Interval of 15 minutes needs to be supported, other
     * intervals may be supported.
     * @return A java Duration
     */
    Duration measurementInterval();

    /**
     * The number of completed measurement intervals to store in the history statistic table.
     * At least 32 completed measurement intervals are to be stored.
     * 96 measurement intervals are recommended to be stored
     * @return The number to be stored.
     */
    Short numberIntervalsStored();

    /**
     * Whether the measurement intervals for the Loss Measurement session are aligned with a zero offset to real time.
     * The value 'true' indicates that each Measurement Interval starts at a time
     * which is aligned to NE time source hour if the interval is a factor of an
     * hour, i.e. 60min/15min = 4. For instance, a measurement time interval of
     * 15 minutes would stop/start the measurement interval at 0, 15, 30, and 45
     * minutes of an hour. A measurement interval of 7 minutes would not align to
     * the hour since 7 minutes is NOT a factor of an hour, i.e.  60min/7min = 8.6,
     * and the behavior is the same as if the object is set to 'false'.
     * The value 'false' indicates that each Measurement Interval starts at a time
     * which is indicated by repetition-period.
     * One side effect of the usage of this parameter is that if the value is true
     * and the repetition-period is not a factor of an hour then the start of the
     * next Measurement Interval will be delayed until the next factor of an hour.
     * @return See above for the meaning of true and false
     */
    boolean alignMeasurementIntervals();

    /**
     * The offset in minutes from the time of day value.
     * if align-measurement-intervals is 'true' and the repetition time is a factor
     * of 60 minutes. If not, the value of this object is ignored.
     * If the Measurement Interval is 15 minutes and align-measurement-intervals
     * is true and if this object was set to 5 minutes, the Measurement Intervals
     * would start at 5, 20, 35, 50 minutes past each hour
     * @return A java Duration
     */
    Duration alignMeasurementOffset();

    /**
     * Defines the session start time.
     * @return An object with the start time type and optionally an instant
     */
    StartTime startTime();

    /**
     * Defines the session stop time.
     * @return An object with the stop time type and optionally an instant
     */
    StopTime stopTime();

    /**
     * Indicates whether the current session is defined to be 'proactive' or 'on-demand.
     * @return An enumerated value
     */
    SessionType sessionType();

    /**
     * Builder for {@link MeasurementCreateBase}.
     */
    public interface MeasCreateBaseBuilder {
        MeasCreateBaseBuilder messagePeriod(
                Duration messagePeriod) throws SoamConfigException;

        MeasCreateBaseBuilder frameSize(Short frameSize) throws SoamConfigException;

        MeasCreateBaseBuilder dataPattern(DelayMeasurementCreate.DataPattern dataPattern);

        MeasCreateBaseBuilder testTlvIncluded(boolean testTlvIncluded);

        MeasCreateBaseBuilder testTlvPattern(DelayMeasurementCreate.TestTlvPattern testTlvPattern);

        MeasCreateBaseBuilder measurementInterval(
                Duration measurementInterval) throws SoamConfigException;

        MeasCreateBaseBuilder numberIntervalsStored(
                Short numberIntervalsStored) throws SoamConfigException;

        MeasCreateBaseBuilder alignMeasurementIntervals(
                boolean alignMeasurementIntervals);

        MeasCreateBaseBuilder alignMeasurementOffset(
                Duration alignMeasurementOffset) throws SoamConfigException;

        MeasCreateBaseBuilder startTime(StartTime startTime) throws SoamConfigException;

        MeasCreateBaseBuilder stopTime(StopTime stopTime) throws SoamConfigException;

        MeasCreateBaseBuilder sessionType(SessionType sessionType);

    }

    /**
     * Supported session types.
     * reference [MEF SOAM IA] R3
     */
    public enum SessionType {
        /**
         * The current session is 'proactive'.
         */
        PROACTIVE,
        /**
         * The current session is 'on-demand'.
         */
        ONDEMAND;
    }

}
