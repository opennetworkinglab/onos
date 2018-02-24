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

import java.util.ArrayList;
import java.util.Collection;

import org.onosproject.incubator.net.l2monitoring.soam.MilliPct;
import org.onosproject.incubator.net.l2monitoring.soam.SoamId;

/**
 * The default implementation of {@link LossMeasurementThreshold}.
 */
public final class DefaultLmThreshold implements LossMeasurementThreshold {

    private final SoamId thresholdId;
    private final Collection<ThresholdOption> threshold;
    private final MilliPct measuredFlrForward;
    private final MilliPct maxFlrForward;
    private final MilliPct averageFlrForward;
    private final MilliPct measuredFlrBackward;
    private final MilliPct maxFlrBackward;
    private final MilliPct averageFlrBackward;
    private final Long forwardHighLoss;
    private final Long forwardConsecutiveHighLoss;
    private final Long backwardHighLoss;
    private final Long backwardConsecutiveHighLoss;
    private final Long forwardUnavailableCount;
    private final MilliPct forwardAvailableRatio;
    private final Long backwardUnavailableCount;
    private final MilliPct backwardAvailableRatio;

    private DefaultLmThreshold(DefaultLmThresholdBuilder builder) {
        this.thresholdId = builder.thresholdId;
        this.threshold = builder.threshold;
        this.measuredFlrForward = builder.measuredFlrForward;
        this.maxFlrForward = builder.maxFlrForward;
        this.averageFlrForward = builder.averageFlrForward;
        this.measuredFlrBackward = builder.measuredFlrBackward;
        this.maxFlrBackward = builder.maxFlrBackward;
        this.averageFlrBackward = builder.averageFlrBackward;
        this.forwardHighLoss = builder.forwardHighLoss;
        this.forwardConsecutiveHighLoss = builder.forwardConsecutiveHighLoss;
        this.backwardHighLoss = builder.backwardHighLoss;
        this.backwardConsecutiveHighLoss = builder.backwardConsecutiveHighLoss;
        this.forwardUnavailableCount = builder.forwardUnavailableCount;
        this.forwardAvailableRatio = builder.forwardAvailableRatio;
        this.backwardUnavailableCount = builder.backwardUnavailableCount;
        this.backwardAvailableRatio = builder.backwardAvailableRatio;
    }

    @Override
    public SoamId thresholdId() {
        return thresholdId;
    }

    @Override
    public Collection<ThresholdOption> thresholds() {
        return threshold;
    }

    @Override
    public MilliPct measuredFlrForward() {
        return measuredFlrForward;
    }

    @Override
    public MilliPct maxFlrForward() {
        return maxFlrForward;
    }

    @Override
    public MilliPct averageFlrForward() {
        return averageFlrForward;
    }

    @Override
    public MilliPct measuredFlrBackward() {
        return measuredFlrBackward;
    }

    @Override
    public MilliPct maxFlrBackward() {
        return maxFlrBackward;
    }

    @Override
    public MilliPct averageFlrBackward() {
        return averageFlrBackward;
    }

    @Override
    public Long forwardHighLoss() {
        return forwardHighLoss;
    }

    @Override
    public Long forwardConsecutiveHighLoss() {
        return forwardConsecutiveHighLoss;
    }

    @Override
    public Long backwardHighLoss() {
        return backwardHighLoss;
    }

    @Override
    public Long backwardConsecutiveHighLoss() {
        return backwardConsecutiveHighLoss;
    }

    @Override
    public Long forwardUnavailableCount() {
        return forwardUnavailableCount;
    }

    @Override
    public MilliPct forwardAvailableRatio() {
        return forwardAvailableRatio;
    }

    @Override
    public Long backwardUnavailableCount() {
        return backwardUnavailableCount;
    }

    @Override
    public MilliPct backwardAvailableRatio() {
        return backwardAvailableRatio;
    }

    public static LmThresholdBuilder builder(SoamId thresholdId) {
        return new DefaultLmThresholdBuilder(thresholdId);
    }

    private static final class DefaultLmThresholdBuilder implements LmThresholdBuilder {
        private final SoamId thresholdId;
        private Collection<ThresholdOption> threshold;
        private MilliPct measuredFlrForward;
        private MilliPct maxFlrForward;
        private MilliPct averageFlrForward;
        private MilliPct measuredFlrBackward;
        private MilliPct maxFlrBackward;
        private MilliPct averageFlrBackward;
        private Long forwardHighLoss;
        private Long forwardConsecutiveHighLoss;
        private Long backwardHighLoss;
        private Long backwardConsecutiveHighLoss;
        private Long forwardUnavailableCount;
        private MilliPct forwardAvailableRatio;
        private Long backwardUnavailableCount;
        private MilliPct backwardAvailableRatio;

        protected DefaultLmThresholdBuilder(SoamId thresholdId) {
            this.thresholdId = thresholdId;
            threshold = new ArrayList<>();
        }

        @Override
        public LmThresholdBuilder addToThreshold(ThresholdOption threshold) {
            this.threshold.add(threshold);
            return this;
        }

        @Override
        public LmThresholdBuilder measuredFlrForward(MilliPct measuredFlrForward) {
            this.measuredFlrForward = measuredFlrForward;
            return this;
        }

        @Override
        public LmThresholdBuilder maxFlrForward(MilliPct maxFlrForward) {
            this.maxFlrForward = maxFlrForward;
            return this;
        }

        @Override
        public LmThresholdBuilder averageFlrForward(MilliPct averageFlrForward) {
            this.averageFlrForward = averageFlrForward;
            return this;
        }

        @Override
        public LmThresholdBuilder measuredFlrBackward(
                MilliPct measuredFlrBackward) {
            this.measuredFlrBackward = measuredFlrBackward;
            return this;
        }

        @Override
        public LmThresholdBuilder maxFlrBackward(MilliPct maxFlrBackward) {
            this.maxFlrBackward = maxFlrBackward;
            return this;
        }

        @Override
        public LmThresholdBuilder averageFlrBackward(MilliPct averageFlrBackward) {
            this.averageFlrBackward = averageFlrBackward;
            return this;
        }

        @Override
        public LmThresholdBuilder forwardHighLoss(Long forwardHighLoss) {
            this.forwardHighLoss = forwardHighLoss;
            return this;
        }

        @Override
        public LmThresholdBuilder forwardConsecutiveHighLoss(
                Long forwardConsecutiveHighLoss) {
            this.forwardConsecutiveHighLoss = forwardConsecutiveHighLoss;
            return this;
        }

        @Override
        public LmThresholdBuilder backwardHighLoss(Long backwardHighLoss) {
            this.backwardHighLoss = backwardHighLoss;
            return this;
        }

        @Override
        public LmThresholdBuilder backwardConsecutiveHighLoss(
                Long backwardConsecutiveHighLoss) {
            this.backwardConsecutiveHighLoss = backwardConsecutiveHighLoss;
            return this;
        }

        @Override
        public LmThresholdBuilder forwardUnavailableCount(
                Long forwardUnavailableCount) {
            this.forwardUnavailableCount = forwardUnavailableCount;
            return this;
        }

        @Override
        public LmThresholdBuilder forwardAvailableRatio(
                MilliPct forwardAvailableRatio) {
            this.forwardAvailableRatio = forwardAvailableRatio;
            return this;
        }

        @Override
        public LmThresholdBuilder backwardUnavailableCount(
                Long backwardUnavailableCount) {
            this.backwardUnavailableCount = backwardUnavailableCount;
            return this;
        }

        @Override
        public LmThresholdBuilder backwardAvailableRatio(
                MilliPct backwardAvailableRatio) {
            this.backwardAvailableRatio = backwardAvailableRatio;
            return this;
        }

        @Override
        public LossMeasurementThreshold build() {
            return new DefaultLmThreshold(this);
        }
    }
}