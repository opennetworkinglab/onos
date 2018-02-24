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
import java.util.ArrayList;
import java.util.Collection;

import org.onosproject.incubator.net.l2monitoring.soam.SoamId;

/**
 * The default implementation of DelayMeasurementThreshold.
 * {@link DelayMeasurementThreshold}.
 */
public final class DefaultDelayMeasurementThreshold
        implements DelayMeasurementThreshold {

    private final SoamId threshId;
    private final Collection<ThresholdOption> thresholdsEnabled;
    private final Duration measuredFrameDelayTwoWay;
    private final Duration maxFrameDelayTwoWay;
    private final Duration averageFrameDelayTwoWay;
    private final Duration measuredInterFrameDelayVariationTwoWay;
    private final Duration maxInterFrameDelayVariationTwoWay;
    private final Duration averageInterFrameDelayVariationTwoWay;
    private final Duration maxFrameDelayRangeTwoWay;
    private final Duration averageFrameDelayRangeTwoWay;
    private final Duration measuredFrameDelayForward;
    private final Duration maxFrameDelayForward;
    private final Duration averageFrameDelayForward;
    private final Duration measuredInterFrameDelayVariationForward;
    private final Duration maxInterFrameDelayVariationForward;
    private final Duration averageInterFrameDelayVariationForward;
    private final Duration maxFrameDelayRangeForward;
    private final Duration averageFrameDelayRangeForward;
    private final Duration measuredFrameDelayBackward;
    private final Duration maxFrameDelayBackward;
    private final Duration averageFrameDelayBackward;
    private final Duration measuredInterFrameDelayVariationBackward;
    private final Duration maxInterFrameDelayVariationBackward;
    private final Duration averageInterFrameDelayVariationBackward;
    private final Duration maxFrameDelayRangeBackward;
    private final Duration averageFrameDelayRangeBackward;

    private DefaultDelayMeasurementThreshold(DefaultDmThresholdBuilder builder) {
        this.threshId = builder.threshId;
        this.thresholdsEnabled = builder.thresholdsEnabled;
        this.measuredFrameDelayTwoWay = builder.measuredFrameDelayTwoWay;
        this.maxFrameDelayTwoWay = builder.maxFrameDelayTwoWay;
        this.averageFrameDelayTwoWay = builder.averageFrameDelayTwoWay;
        this.measuredInterFrameDelayVariationTwoWay =
                builder.measuredInterFrameDelayVariationTwoWay;
        this.maxInterFrameDelayVariationTwoWay =
                builder.maxInterFrameDelayVariationTwoWay;
        this.averageInterFrameDelayVariationTwoWay =
                builder.averageInterFrameDelayVariationTwoWay;
        this.maxFrameDelayRangeTwoWay = builder.maxFrameDelayRangeTwoWay;
        this.averageFrameDelayRangeTwoWay = builder.averageFrameDelayRangeTwoWay;
        this.measuredFrameDelayForward = builder.measuredFrameDelayForward;
        this.maxFrameDelayForward = builder.maxFrameDelayForward;
        this.averageFrameDelayForward = builder.averageFrameDelayForward;
        this.measuredInterFrameDelayVariationForward =
                builder.measuredInterFrameDelayVariationForward;
        this.maxInterFrameDelayVariationForward =
                builder.maxInterFrameDelayVariationForward;
        this.averageInterFrameDelayVariationForward =
                builder.averageInterFrameDelayVariationForward;
        this.maxFrameDelayRangeForward = builder.maxFrameDelayRangeForward;
        this.averageFrameDelayRangeForward = builder.averageFrameDelayRangeForward;
        this.measuredFrameDelayBackward = builder.measuredFrameDelayBackward;
        this.maxFrameDelayBackward = builder.maxFrameDelayBackward;
        this.averageFrameDelayBackward = builder.averageFrameDelayBackward;
        this.measuredInterFrameDelayVariationBackward =
                builder.measuredInterFrameDelayVariationBackward;
        this.maxInterFrameDelayVariationBackward =
                builder.maxInterFrameDelayVariationBackward;
        this.averageInterFrameDelayVariationBackward =
                builder.averageInterFrameDelayVariationBackward;
        this.maxFrameDelayRangeBackward =
                builder.maxFrameDelayRangeBackward;
        this.averageFrameDelayRangeBackward =
                builder.averageFrameDelayRangeBackward;
    }

    @Override
    public SoamId threshId() {
        return threshId;
    }

    @Override
    public Collection<ThresholdOption> thresholdsEnabled() {
        return thresholdsEnabled;
    }

    @Override
    public Duration measuredFrameDelayTwoWay() {
        return measuredFrameDelayTwoWay;
    }

    @Override
    public Duration maxFrameDelayTwoWay() {
        return maxFrameDelayTwoWay;
    }

    @Override
    public Duration averageFrameDelayTwoWay() {
        return averageFrameDelayTwoWay;
    }

    @Override
    public Duration measuredInterFrameDelayVariationTwoWay() {
        return measuredInterFrameDelayVariationTwoWay;
    }

    @Override
    public Duration maxInterFrameDelayVariationTwoWay() {
        return maxInterFrameDelayVariationTwoWay;
    }

    @Override
    public Duration averageInterFrameDelayVariationTwoWay() {
        return averageInterFrameDelayVariationTwoWay;
    }

    @Override
    public Duration maxFrameDelayRangeTwoWay() {
        return maxFrameDelayRangeTwoWay;
    }

    @Override
    public Duration averageFrameDelayRangeTwoWay() {
        return averageFrameDelayRangeTwoWay;
    }

    @Override
    public Duration measuredFrameDelayForward() {
        return measuredFrameDelayForward;
    }

    @Override
    public Duration maxFrameDelayForward() {
        return maxFrameDelayForward;
    }

    @Override
    public Duration averageFrameDelayForward() {
        return averageFrameDelayForward;
    }

    @Override
    public Duration measuredInterFrameDelayVariationForward() {
        return measuredInterFrameDelayVariationForward;
    }

    @Override
    public Duration maxInterFrameDelayVariationForward() {
        return maxInterFrameDelayVariationForward;
    }

    @Override
    public Duration averageInterFrameDelayVariationForward() {
        return averageInterFrameDelayVariationForward;
    }

    @Override
    public Duration maxFrameDelayRangeForward() {
        return maxFrameDelayRangeForward;
    }

    @Override
    public Duration averageFrameDelayRangeForward() {
        return averageFrameDelayRangeForward;
    }

    @Override
    public Duration measuredFrameDelayBackward() {
        return measuredFrameDelayBackward;
    }

    @Override
    public Duration maxFrameDelayBackward() {
        return maxFrameDelayBackward;
    }

    @Override
    public Duration averageFrameDelayBackward() {
        return averageFrameDelayBackward;
    }

    @Override
    public Duration measuredInterFrameDelayVariationBackward() {
        return measuredInterFrameDelayVariationBackward;
    }

    @Override
    public Duration maxInterFrameDelayVariationBackward() {
        return maxInterFrameDelayVariationBackward;
    }

    @Override
    public Duration averageInterFrameDelayVariationBackward() {
        return averageInterFrameDelayVariationBackward;
    }

    @Override
    public Duration maxFrameDelayRangeBackward() {
        return maxFrameDelayRangeBackward;
    }

    @Override
    public Duration averageFrameDelayRangeBackward() {
        return averageFrameDelayRangeBackward;
    }

    public static DmThresholdBuilder builder(SoamId threshId) {
        return new DefaultDmThresholdBuilder(threshId);
    }

    /**
     * Builder for {@link DelayMeasurementThreshold}.
     */
    private static final class DefaultDmThresholdBuilder implements DmThresholdBuilder {
        private final SoamId threshId;
        private Collection<ThresholdOption> thresholdsEnabled;
        private Duration measuredFrameDelayTwoWay;
        private Duration maxFrameDelayTwoWay;
        private Duration averageFrameDelayTwoWay;
        private Duration measuredInterFrameDelayVariationTwoWay;
        private Duration maxInterFrameDelayVariationTwoWay;
        private Duration averageInterFrameDelayVariationTwoWay;
        private Duration maxFrameDelayRangeTwoWay;
        private Duration averageFrameDelayRangeTwoWay;
        private Duration measuredFrameDelayForward;
        private Duration maxFrameDelayForward;
        private Duration averageFrameDelayForward;
        private Duration measuredInterFrameDelayVariationForward;
        private Duration maxInterFrameDelayVariationForward;
        private Duration averageInterFrameDelayVariationForward;
        private Duration maxFrameDelayRangeForward;
        private Duration averageFrameDelayRangeForward;
        private Duration measuredFrameDelayBackward;
        private Duration maxFrameDelayBackward;
        private Duration averageFrameDelayBackward;
        private Duration measuredInterFrameDelayVariationBackward;
        private Duration maxInterFrameDelayVariationBackward;
        private Duration averageInterFrameDelayVariationBackward;
        private Duration maxFrameDelayRangeBackward;
        private Duration averageFrameDelayRangeBackward;

        protected DefaultDmThresholdBuilder(SoamId threshId) {
            this.threshId = threshId;
            this.thresholdsEnabled = new ArrayList<>();
        }

        @Override
        public DmThresholdBuilder addToThresholdsEnabled(
                ThresholdOption thresholdEnabled) {
            this.thresholdsEnabled.add(thresholdEnabled);
            return this;
        }

        @Override
        public DmThresholdBuilder measuredFrameDelayTwoWay(
                Duration measuredFrameDelayTwoWay) {
            this.measuredFrameDelayTwoWay = measuredFrameDelayTwoWay;
            return this;
        }

        @Override
        public DmThresholdBuilder maxFrameDelayTwoWay(
                Duration maxFrameDelayTwoWay) {
            this.maxFrameDelayTwoWay = maxFrameDelayTwoWay;
            return this;
        }

        @Override
        public DmThresholdBuilder averageFrameDelayTwoWay(
                Duration averageFrameDelayTwoWay) {
            this.averageFrameDelayTwoWay = averageFrameDelayTwoWay;
            return this;
        }

        @Override
        public DmThresholdBuilder measuredInterFrameDelayVariationTwoWay(
                Duration measuredInterFrameDelayVariationTwoWay) {
            this.measuredInterFrameDelayVariationTwoWay =
                    measuredInterFrameDelayVariationTwoWay;
            return this;
        }

        @Override
        public DmThresholdBuilder maxInterFrameDelayVariationTwoWay(
                Duration maxInterFrameDelayVariationTwoWay) {
            this.maxInterFrameDelayVariationTwoWay =
                    maxInterFrameDelayVariationTwoWay;
            return this;
        }

        @Override
        public DmThresholdBuilder averageInterFrameDelayVariationTwoWay(
                Duration averageInterFrameDelayVariationTwoWay) {
            this.averageInterFrameDelayVariationTwoWay =
                    averageInterFrameDelayVariationTwoWay;
            return this;
        }

        @Override
        public DmThresholdBuilder maxFrameDelayRangeTwoWay(
                Duration maxFrameDelayRangeTwoWay) {
            this.maxFrameDelayRangeTwoWay = maxFrameDelayRangeTwoWay;
            return this;
        }

        @Override
        public DmThresholdBuilder averageFrameDelayRangeTwoWay(
                Duration averageFrameDelayRangeTwoWay) {
            this.averageFrameDelayRangeTwoWay = averageFrameDelayRangeTwoWay;
            return this;
        }

        @Override
        public DmThresholdBuilder measuredFrameDelayForward(
                Duration measuredFrameDelayForward) {
            this.measuredFrameDelayForward = measuredFrameDelayForward;
            return this;
        }

        @Override
        public DmThresholdBuilder maxFrameDelayForward(
                Duration maxFrameDelayForward) {
            this.maxFrameDelayForward = maxFrameDelayForward;
            return this;
        }

        @Override
        public DmThresholdBuilder averageFrameDelayForward(
                Duration averageFrameDelayForward) {
            this.averageFrameDelayForward = averageFrameDelayForward;
            return this;
        }

        @Override
        public DmThresholdBuilder measuredInterFrameDelayVariationForward(
                Duration measuredInterFrameDelayVariationForward) {
            this.measuredInterFrameDelayVariationForward =
                    measuredInterFrameDelayVariationForward;
            return this;
        }

        @Override
        public DmThresholdBuilder maxInterFrameDelayVariationForward(
                Duration maxInterFrameDelayVariationForward) {
            this.maxInterFrameDelayVariationForward =
                    maxInterFrameDelayVariationForward;
            return this;
        }

        @Override
        public DmThresholdBuilder averageInterFrameDelayVariationForward(
                Duration averageInterFrameDelayVariationForward) {
            this.averageInterFrameDelayVariationForward =
                    averageInterFrameDelayVariationForward;
            return this;
        }

        @Override
        public DmThresholdBuilder maxFrameDelayRangeForward(
                Duration maxFrameDelayRangeForward) {
            this.maxFrameDelayRangeForward = maxFrameDelayRangeForward;
            return this;
        }

        @Override
        public DmThresholdBuilder averageFrameDelayRangeForward(
                Duration averageFrameDelayRangeForward) {
            this.averageFrameDelayRangeForward = averageFrameDelayRangeForward;
            return this;
        }

        @Override
        public DmThresholdBuilder measuredFrameDelayBackward(
                Duration measuredFrameDelayBackward) {
            this.measuredFrameDelayBackward = measuredFrameDelayBackward;
            return this;
        }

        @Override
        public DmThresholdBuilder maxFrameDelayBackward(
                Duration maxFrameDelayBackward) {
            this.maxFrameDelayBackward = maxFrameDelayBackward;
            return this;
        }

        @Override
        public DmThresholdBuilder averageFrameDelayBackward(
                Duration averageFrameDelayBackward) {
            this.averageFrameDelayBackward = averageFrameDelayBackward;
            return this;
        }

        @Override
        public DmThresholdBuilder measuredInterFrameDelayVariationBackward(
                Duration measuredInterFrameDelayVariationBackward) {
            this.measuredInterFrameDelayVariationBackward =
                    measuredInterFrameDelayVariationBackward;
            return this;
        }

        @Override
        public DmThresholdBuilder maxInterFrameDelayVariationBackward(
                Duration maxInterFrameDelayVariationBackward) {
            this.maxInterFrameDelayVariationBackward =
                    maxInterFrameDelayVariationBackward;
            return this;
        }

        @Override
        public DmThresholdBuilder averageInterFrameDelayVariationBackward(
                Duration averageInterFrameDelayVariationBackward) {
            this.averageInterFrameDelayVariationBackward =
                    averageInterFrameDelayVariationBackward;
            return this;
        }

        @Override
        public DmThresholdBuilder maxFrameDelayRangeBackward(
                Duration maxFrameDelayRangeBackward) {
            this.maxFrameDelayRangeBackward = maxFrameDelayRangeBackward;
            return this;
        }

        @Override
        public DmThresholdBuilder averageFrameDelayRangeBackward(
                Duration averageFrameDelayRangeBackward) {
            this.averageFrameDelayRangeBackward = averageFrameDelayRangeBackward;
            return this;
        }

        @Override
        public DelayMeasurementThreshold build() {
            return new DefaultDelayMeasurementThreshold(this);
        }
    }

}
