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

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;

import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepId;
import org.onosproject.incubator.net.l2monitoring.soam.MilliPct;
import org.onosproject.incubator.net.l2monitoring.soam.SoamConfigException;
import org.onosproject.incubator.net.l2monitoring.soam.SoamId;
import org.onosproject.incubator.net.l2monitoring.cfm.Mep;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementCreate;

/**
 * The default implementation of {@link LossMeasurementEntry}.
 */
public final class DefaultLmEntry extends DefaultLmCreate
        implements LossMeasurementEntry {
    private final SoamId lmId;
    private final MilliPct measuredForwardFlr;
    private final MilliPct measuredBackwardFlr;
    private final AvailabilityType measuredAvailabilityForwardStatus;
    private final AvailabilityType measuredAvailabilityBackwardStatus;
    private final Instant measuredForwardLastTransitionTime;
    private final Instant measuredBackwardLastTransitionTime;
    private final LossMeasurementStatCurrent measurementCurrent;
    private final Collection<LossMeasurementStatHistory> measurementHistories;
    private final LossAvailabilityStatCurrent availabilityCurrent;
    private final Collection<LossAvailabilityStatHistory> availabilityHistories;

    protected DefaultLmEntry(DefaultLmEntryBuilder builder) {
        super(builder);
        this.lmId = builder.lmId;
        this.measuredForwardFlr = builder.measuredForwardFlr;
        this.measuredBackwardFlr = builder.measuredBackwardFlr;
        this.measuredAvailabilityForwardStatus = builder.measuredAvailabilityForwardStatus;
        this.measuredAvailabilityBackwardStatus = builder.measuredAvailabilityBackwardStatus;
        this.measuredForwardLastTransitionTime = builder.measuredForwardLastTransitionTime;
        this.measuredBackwardLastTransitionTime = builder.measuredBackwardLastTransitionTime;
        this.measurementCurrent = builder.measurementCurrent;
        this.measurementHistories = builder.measurementHistories;
        this.availabilityCurrent = builder.availabilityCurrent;
        this.availabilityHistories = builder.availabilityHistories;
    }

    @Override
    public SoamId lmId() {
        return lmId;
    }

    @Override
    public MilliPct measuredForwardFlr() {
        return measuredForwardFlr;
    }

    @Override
    public MilliPct measuredBackwardFlr() {
        return measuredBackwardFlr;
    }

    @Override
    public AvailabilityType measuredAvailabilityForwardStatus() {
        return measuredAvailabilityForwardStatus;
    }

    @Override
    public AvailabilityType measuredAvailabilityBackwardStatus() {
        return measuredAvailabilityBackwardStatus;
    }

    @Override
    public Instant measuredForwardLastTransitionTime() {
        return measuredForwardLastTransitionTime;
    }

    @Override
    public Instant measuredBackwardLastTransitionTime() {
        return measuredBackwardLastTransitionTime;
    }

    @Override
    public LossMeasurementStatCurrent measurementCurrent() {
        return measurementCurrent;
    }

    @Override
    public Collection<LossMeasurementStatHistory> measurementHistories() {
        return measurementHistories;
    }

    @Override
    public LossAvailabilityStatCurrent availabilityCurrent() {
        return availabilityCurrent;
    }

    @Override
    public Collection<LossAvailabilityStatHistory> availabilityHistories() {
        return availabilityHistories;
    }

    public static LmEntryBuilder builder(DelayMeasurementCreate.Version version, MepId remoteMepId,
                                         Mep.Priority priority, LmType lmCfgType, SoamId lmId)
                    throws SoamConfigException {
        return new DefaultLmEntryBuilder(version, remoteMepId,
                priority, lmCfgType, lmId);
    }

    private static final class DefaultLmEntryBuilder extends DefaultLmCreateBuilder
                    implements LmEntryBuilder {
        private final SoamId lmId;
        private MilliPct measuredForwardFlr;
        private MilliPct measuredBackwardFlr;
        private AvailabilityType measuredAvailabilityForwardStatus;
        private AvailabilityType measuredAvailabilityBackwardStatus;
        private Instant measuredForwardLastTransitionTime;
        private Instant measuredBackwardLastTransitionTime;
        private LossMeasurementStatCurrent measurementCurrent;
        private Collection<LossMeasurementStatHistory> measurementHistories;
        private LossAvailabilityStatCurrent availabilityCurrent;
        private Collection<LossAvailabilityStatHistory> availabilityHistories;

        protected DefaultLmEntryBuilder(DelayMeasurementCreate.Version version, MepId remoteMepId,
                                        Mep.Priority priority, LmType lmCfgType, SoamId lmId)
                throws SoamConfigException {
            super(version, remoteMepId, priority, lmCfgType);
            this.lmId = lmId;
            measurementHistories = new ArrayList<>();
            availabilityHistories = new ArrayList<>();
        }

        @Override
        public LmEntryBuilder measuredForwardFlr(MilliPct measuredForwardFlr) {
            this.measuredForwardFlr = measuredForwardFlr;
            return this;
        }

        @Override
        public LmEntryBuilder measuredBackwardFlr(
                MilliPct measuredBackwardFlr) {
            this.measuredBackwardFlr = measuredBackwardFlr;
            return this;
        }

        @Override
        public LmEntryBuilder measuredAvailabilityForwardStatus(
                AvailabilityType measuredAvailabilityForwardStatus) {
            this.measuredAvailabilityForwardStatus = measuredAvailabilityForwardStatus;
            return this;
        }

        @Override
        public LmEntryBuilder measuredAvailabilityBackwardStatus(
                AvailabilityType measuredAvailabilityBackwardStatus) {
            this.measuredAvailabilityBackwardStatus = measuredAvailabilityBackwardStatus;
            return this;
        }

        @Override
        public LmEntryBuilder measuredForwardLastTransitionTime(
                Instant measuredForwardLastTransitionTime) {
            this.measuredForwardLastTransitionTime = measuredForwardLastTransitionTime;
            return this;
        }

        @Override
        public LmEntryBuilder measuredBackwardLastTransitionTime(
                Instant measuredBackwardLastTransitionTime) {
            this.measuredBackwardLastTransitionTime = measuredBackwardLastTransitionTime;
            return this;
        }

        @Override
        public LmEntryBuilder measurementCurrent(
                LossMeasurementStatCurrent measurementCurrent) {
            this.measurementCurrent = measurementCurrent;
            return this;
        }

        @Override
        public LmEntryBuilder addToMeasurementHistories(
                LossMeasurementStatHistory history) {
            this.measurementHistories.add(history);
            return this;
        }

        @Override
        public LmEntryBuilder availabilityCurrent(
                LossAvailabilityStatCurrent availabilityCurrent) {
            this.availabilityCurrent = availabilityCurrent;
            return this;
        }

        @Override
        public LmEntryBuilder addToAvailabilityHistories(
                LossAvailabilityStatHistory history) {
            this.availabilityHistories.add(history);
            return this;
        }

        @Override
        public LossMeasurementEntry build() {
            return new DefaultLmEntry(this);
        }
    }
}
