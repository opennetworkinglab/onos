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
import java.util.ArrayList;
import java.util.Collection;

import org.onosproject.incubator.net.l2monitoring.cfm.Mep.Priority;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepId;
import org.onosproject.incubator.net.l2monitoring.soam.DefaultMeasurementCreateBase;
import org.onosproject.incubator.net.l2monitoring.soam.MilliPct;
import org.onosproject.incubator.net.l2monitoring.soam.SoamConfigException;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementCreate.Version;

/**
 * The default implementation of {@link LossMeasurementCreate}.
 */
public class DefaultLmCreate extends DefaultMeasurementCreateBase
        implements LossMeasurementCreate {

    private final LmType lmCfgType;
    private final Collection<CounterOption> countersEnabled;
    private final Duration availabilityMeasurementInterval;
    private final Integer availabilityNumberConsecutiveFlrMeasurements;
    private final MilliPct availabilityFlrThreshold;
    private final Short availabilityNumberConsecutiveIntervals;
    private final Short availabilityNumberConsecutiveHighFlr;
    private final Collection<LossMeasurementThreshold> lossMeasurementThresholds;

    protected DefaultLmCreate(DefaultLmCreateBuilder builder) {
        super(builder);
        this.lmCfgType = builder.lmCfgType;
        this.countersEnabled = builder.countersEnabled;
        this.availabilityMeasurementInterval = builder.availabilityMeasurementInterval;
        this.availabilityNumberConsecutiveFlrMeasurements = builder.availabilityNumberConsecutiveFlrMeasurements;
        this.availabilityFlrThreshold = builder.availabilityFlrThreshold;
        this.availabilityNumberConsecutiveIntervals = builder.availabilityNumberConsecutiveIntervals;
        this.availabilityNumberConsecutiveHighFlr = builder.availabilityNumberConsecutiveHighFlr;
        this.lossMeasurementThresholds = builder.lossMeasurementThresholds;
    }

    @Override
    public LmType lmCfgType() {
        return this.lmCfgType;
    }

    @Override
    public Collection<CounterOption> countersEnabled() {
        return this.countersEnabled;
    }

    @Override
    public Duration availabilityMeasurementInterval() {
        return this.availabilityMeasurementInterval;
    }

    @Override
    public Integer availabilityNumberConsecutiveFlrMeasurements() {
        return this.availabilityNumberConsecutiveFlrMeasurements;
    }

    @Override
    public MilliPct availabilityFlrThreshold() {
        return this.availabilityFlrThreshold;
    }

    @Override
    public Short availabilityNumberConsecutiveIntervals() {
        return this.availabilityNumberConsecutiveIntervals;
    }

    @Override
    public Short availabilityNumberConsecutiveHighFlr() {
        return this.availabilityNumberConsecutiveHighFlr;
    }

    @Override
    public Collection<LossMeasurementThreshold> lossMeasurementThreshold() {
        return this.lossMeasurementThresholds;
    }

    public static LmCreateBuilder builder(Version version, MepId remoteMepId,
            Priority priority, LmType lmCfgType) throws SoamConfigException {
        return new DefaultLmCreateBuilder(version, remoteMepId,
                priority, lmCfgType);
    }

    /**
     * Implementation of LmCreateBuilder.
     * {@link LossMeasurementCreate.LmCreateBuilder}
     */
    protected static class DefaultLmCreateBuilder extends DefaultMeasCreateBaseBuilder
            implements LmCreateBuilder {
        private final LmType lmCfgType;
        private Collection<CounterOption> countersEnabled;
        private Duration availabilityMeasurementInterval;
        private Integer availabilityNumberConsecutiveFlrMeasurements;
        private MilliPct availabilityFlrThreshold;
        private Short availabilityNumberConsecutiveIntervals;
        private Short availabilityNumberConsecutiveHighFlr;
        private Collection<LossMeasurementThreshold> lossMeasurementThresholds;

        protected DefaultLmCreateBuilder(Version version, MepId remoteMepId,
                Priority priority, LmType lmCfgType) throws SoamConfigException {
            super(version, remoteMepId, priority);
            this.lmCfgType = lmCfgType;
            countersEnabled = new ArrayList<>();
            lossMeasurementThresholds = new ArrayList<>();
        }

        @Override
        public LmCreateBuilder addToCountersEnabled(
                CounterOption counterOption) {
            this.countersEnabled.add(counterOption);
            return this;
        }

        @Override
        public LmCreateBuilder availabilityMeasurementInterval(
                Duration availabilityMeasurementInterval) {
            this.availabilityMeasurementInterval = availabilityMeasurementInterval;
            return this;
        }

        @Override
        public LmCreateBuilder availabilityNumberConsecutiveFlrMeasurements(
                Integer availabilityNumberConsecutiveFlrMeasurements) {
            this.availabilityNumberConsecutiveFlrMeasurements =
                    availabilityNumberConsecutiveFlrMeasurements;
            return this;
        }

        @Override
        public LmCreateBuilder availabilityFlrThreshold(
                MilliPct availabilityFlrThreshold) {
            this.availabilityFlrThreshold = availabilityFlrThreshold;
            return this;
        }

        @Override
        public LmCreateBuilder availabilityNumberConsecutiveIntervals(
                Short availabilityNumberConsecutiveIntervals)
                throws SoamConfigException {
            this.availabilityNumberConsecutiveIntervals = availabilityNumberConsecutiveIntervals;
            return this;
        }

        @Override
        public LmCreateBuilder availabilityNumberConsecutiveHighFlr(
                Short availabilityNumberConsecutiveHighFlr)
                throws SoamConfigException {
            this.availabilityNumberConsecutiveHighFlr = availabilityNumberConsecutiveHighFlr;
            return this;
        }

        @Override
        public LmCreateBuilder addToLossMeasurementThreshold(
                LossMeasurementThreshold lossMeasurementThreshold) {
            this.lossMeasurementThresholds.add(lossMeasurementThreshold);
            return this;
        }

        @Override
        public LossMeasurementCreate build() {
            return new DefaultLmCreate(this);
        }
    }
}
