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
import java.time.Instant;

import org.onosproject.incubator.net.l2monitoring.cfm.Mep.Priority;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepId;
import org.onosproject.incubator.net.l2monitoring.soam.StartTime.StartTimeOption;
import org.onosproject.incubator.net.l2monitoring.soam.StopTime.StopTimeOption;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementCreate.DataPattern;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementCreate.TestTlvPattern;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementCreate.Version;

/**
 * Abstract implementation of {@link MeasurementCreateBase}.
 */
public abstract class DefaultMeasurementCreateBase
        implements MeasurementCreateBase {
    protected final Version version;

    protected final MepId remoteMepId;
    protected final Duration messagePeriod;
    protected final Priority priority;
    protected final Short frameSize;
    protected final DataPattern dataPattern;
    protected final boolean testTlvIncluded;
    protected final TestTlvPattern testTlvPattern;
    protected final Duration measurementInterval;
    protected final Short numberIntervalsStored;
    protected final boolean alignMeasurementIntervals;
    protected final Duration alignMeasurementOffset;
    protected final SessionType sessionType;
    protected final StartTime startTime;
    protected final StopTime stopTime;

    protected DefaultMeasurementCreateBase(DefaultMeasCreateBaseBuilder builder) {
        this.version = builder.version;

        this.remoteMepId = builder.remoteMepId;
        this.messagePeriod = builder.messagePeriod;
        this.priority = builder.priority;
        this.frameSize = builder.frameSize;
        this.dataPattern = builder.dataPattern;
        this.testTlvIncluded = builder.testTlvIncluded;
        this.testTlvPattern = builder.testTlvPattern;
        this.measurementInterval = builder.measurementInterval;
        this.numberIntervalsStored = builder.numberIntervalsStored;
        this.alignMeasurementIntervals = builder.alignMeasurementIntervals;
        this.alignMeasurementOffset = builder.alignMeasurementOffset;
        this.sessionType = builder.sessionType;
        this.startTime = builder.startTime;
        this.stopTime = builder.stopTime;
    }

    @Override
    public Version version() {
        return version;
    }

    @Override
    public MepId remoteMepId() {
        return remoteMepId;
    }

    @Override
    public Duration messagePeriod() {
        return messagePeriod;
    }

    @Override
    public Priority priority() {
        return priority;
    }

    @Override
    public Short frameSize() {
        return frameSize;
    }

    @Override
    public DataPattern dataPattern() {
        return dataPattern;
    }

    @Override
    public boolean testTlvIncluded() {
        return testTlvIncluded;
    }

    @Override
    public TestTlvPattern testTlvPattern() {
        return testTlvPattern;
    }

    @Override
    public Duration measurementInterval() {
        return measurementInterval;
    }

    @Override
    public Short numberIntervalsStored() {
        return numberIntervalsStored;
    }

    @Override
    public boolean alignMeasurementIntervals() {
        return alignMeasurementIntervals;
    }

    @Override
    public Duration alignMeasurementOffset() {
        return alignMeasurementOffset;
    }

    @Override
    public SessionType sessionType() {
        return sessionType;
    }

    @Override
    public StartTime startTime() {
        return startTime;
    }

    @Override
    public StopTime stopTime() {
        return stopTime;
    }

    /**
     * Abstract Builder class for  building.
     * {@link MeasurementCreateBase}.
     */
    protected abstract static class DefaultMeasCreateBaseBuilder implements MeasCreateBaseBuilder {
        protected final Version version;
        protected final MepId remoteMepId;
        protected final Priority priority;

        protected Duration messagePeriod;
        protected Short frameSize;
        protected DataPattern dataPattern;
        protected boolean testTlvIncluded;
        protected TestTlvPattern testTlvPattern;
        protected Duration measurementInterval;
        protected Short numberIntervalsStored;
        protected boolean alignMeasurementIntervals;
        protected Duration alignMeasurementOffset;
        protected SessionType sessionType;
        protected StartTime startTime;
        protected StopTime stopTime;

        protected DefaultMeasCreateBaseBuilder(Version version,
                MepId remoteMepId, Priority priority)
                        throws SoamConfigException {
            super();
            if (remoteMepId == null) {
                throw new SoamConfigException("RemoteMepId is null");
            }
            this.remoteMepId = remoteMepId;
            this.version = version;
            this.priority = priority;
        }

        @Override
        public MeasCreateBaseBuilder messagePeriod(Duration messagePeriod) throws SoamConfigException {
            if (messagePeriod.toMillis() < 3 || messagePeriod.toMillis() > 3600000) {
                throw new SoamConfigException("Message Period must be between 3-3600000ms. Rejecting: "
                        + messagePeriod);
            }
            this.messagePeriod = messagePeriod;
            return this;
        }

        @Override
        public MeasCreateBaseBuilder frameSize(Short frameSize) throws SoamConfigException {
            if (frameSize < 64 || frameSize > 9600) {
                throw new SoamConfigException("Frame Size must be between 64-9600 bytes."
                        + " Rejecting: " + frameSize);
            }
            this.frameSize = frameSize;
            return this;
        }

        @Override
        public MeasCreateBaseBuilder dataPattern(DataPattern dataPattern) {
            this.dataPattern = dataPattern;
            return this;
        }

        @Override
        public MeasCreateBaseBuilder testTlvIncluded(boolean testTlvIncluded) {
            this.testTlvIncluded = testTlvIncluded;
            return this;
        }

        @Override
        public MeasCreateBaseBuilder testTlvPattern(TestTlvPattern testTlvPattern) {
            this.testTlvPattern = testTlvPattern;
            return this;
        }

        @Override
        public MeasCreateBaseBuilder measurementInterval(Duration measurementInterval) throws SoamConfigException {
            if (measurementInterval.toMinutes() < 1 || measurementInterval.toMinutes() > 525600) {
                throw new SoamConfigException(
                        "Measurement Interval must be between 1..525600 minutes. Rejecting: " + measurementInterval);
            }
            this.measurementInterval = measurementInterval;
            return this;
        }

        @Override
        public MeasCreateBaseBuilder numberIntervalsStored(Short numberIntervalsStored)
                throws SoamConfigException {
            if (numberIntervalsStored < 2 || numberIntervalsStored > 1000) {
                throw new SoamConfigException(
                        "Number Intervals Stored must be between 2-1000. "
                        + "Rejecting: " + numberIntervalsStored);
            }
            this.numberIntervalsStored = numberIntervalsStored;
            return this;
        }

        @Override
        public MeasCreateBaseBuilder alignMeasurementIntervals(boolean alignMeasurementIntervals) {
            this.alignMeasurementIntervals = alignMeasurementIntervals;
            return this;
        }

        @Override
        public MeasCreateBaseBuilder alignMeasurementOffset(
                Duration alignMeasurementOffset) throws SoamConfigException {
            if (alignMeasurementOffset.toMinutes() < 0 || alignMeasurementOffset.toMinutes() > 525600) {
                throw new SoamConfigException(
                        "Align Measurement Offset must be between 0..525600 minutes. Rejecting: " +
                                alignMeasurementOffset);
            }
            this.alignMeasurementOffset = alignMeasurementOffset;
            return this;
        }

        @Override
        public MeasCreateBaseBuilder sessionType(SessionType sessionType) {
            this.sessionType = sessionType;
            return this;
        }

        @Override
        public MeasCreateBaseBuilder startTime(StartTime startTime) throws SoamConfigException {
            if (startTime.option() == StartTimeOption.ABSOLUTE &&
                    startTime.absoluteTime().isBefore(Instant.now())) {
                throw new SoamConfigException(
                        "Start Time must be not be in the past. Rejecting: " + startTime);
            }
            this.startTime = startTime;
            return this;
        }

        @Override
        public MeasCreateBaseBuilder stopTime(StopTime stopTime) throws SoamConfigException {
            if (stopTime.option() == StopTimeOption.ABSOLUTE &&
                    stopTime.absoluteTime().isBefore(Instant.now())) {
                throw new SoamConfigException(
                        "Stop Time must be not be in the past. Rejecting: " + stopTime);
            }
            this.stopTime = stopTime;
            return this;
        }


    }
}
