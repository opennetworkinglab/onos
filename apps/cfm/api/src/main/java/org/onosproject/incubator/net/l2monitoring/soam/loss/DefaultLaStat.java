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
 * The default implementation of {@link LossAvailabilityStat}.
 */
public abstract class DefaultLaStat implements LossAvailabilityStat {
    private final Duration elapsedTime;
    private final boolean suspectStatus;
    private final Long forwardHighLoss;
    private final Long backwardHighLoss;
    private final Long forwardConsecutiveHighLoss;
    private final Long backwardConsecutiveHighLoss;
    private final Long forwardAvailable;
    private final Long backwardAvailable;
    private final Long forwardUnavailable;
    private final Long backwardUnavailable;
    private final MilliPct forwardMinFrameLossRatio;
    private final MilliPct forwardMaxFrameLossRatio;
    private final MilliPct forwardAverageFrameLossRatio;
    private final MilliPct backwardMinFrameLossRatio;
    private final MilliPct backwardMaxFrameLossRatio;
    private final MilliPct backwardAverageFrameLossRatio;

    protected DefaultLaStat(DefaultLaStatBuilder builder) {
        this.elapsedTime = builder.elapsedTime;
        this.suspectStatus = builder.suspectStatus;
        this.forwardHighLoss = builder.forwardHighLoss;
        this.backwardHighLoss = builder.backwardHighLoss;
        this.forwardConsecutiveHighLoss = builder.forwardConsecutiveHighLoss;
        this.backwardConsecutiveHighLoss = builder.backwardConsecutiveHighLoss;
        this.forwardAvailable = builder.forwardAvailable;
        this.backwardAvailable = builder.backwardAvailable;
        this.forwardUnavailable = builder.forwardUnavailable;
        this.backwardUnavailable = builder.backwardUnavailable;
        this.forwardMinFrameLossRatio = builder.forwardMinFrameLossRatio;
        this.forwardMaxFrameLossRatio = builder.forwardMaxFrameLossRatio;
        this.forwardAverageFrameLossRatio = builder.forwardAverageFrameLossRatio;
        this.backwardMinFrameLossRatio = builder.backwardMinFrameLossRatio;
        this.backwardMaxFrameLossRatio = builder.backwardMaxFrameLossRatio;
        this.backwardAverageFrameLossRatio = builder.backwardAverageFrameLossRatio;
    }

    @Override
    public Duration elapsedTime() {
        return elapsedTime;
    }

    @Override
    public boolean suspectStatus() {
        return suspectStatus;
    }

    @Override
    public Long forwardHighLoss() {
        return forwardHighLoss;
    }

    @Override
    public Long backwardHighLoss() {
        return backwardHighLoss;
    }

    @Override
    public Long forwardConsecutiveHighLoss() {
        return forwardConsecutiveHighLoss;
    }

    @Override
    public Long backwardConsecutiveHighLoss() {
        return backwardConsecutiveHighLoss;
    }

    @Override
    public Long forwardAvailable() {
        return forwardAvailable;
    }

    @Override
    public Long backwardAvailable() {
        return backwardAvailable;
    }

    @Override
    public Long forwardUnavailable() {
        return forwardUnavailable;
    }

    @Override
    public Long backwardUnavailable() {
        return backwardUnavailable;
    }

    @Override
    public MilliPct forwardMinFrameLossRatio() {
        return forwardMinFrameLossRatio;
    }

    @Override
    public MilliPct forwardMaxFrameLossRatio() {
        return forwardMaxFrameLossRatio;
    }

    @Override
    public MilliPct forwardAverageFrameLossRatio() {
        return forwardAverageFrameLossRatio;
    }

    @Override
    public MilliPct backwardMinFrameLossRatio() {
        return backwardMinFrameLossRatio;
    }

    @Override
    public MilliPct backwardMaxFrameLossRatio() {
        return backwardMaxFrameLossRatio;
    }

    @Override
    public MilliPct backwardAverageFrameLossRatio() {
        return backwardAverageFrameLossRatio;
    }

    /**
     * Abstract base class for builders of.
     * {@link LossAvailabilityStat}.
     */
    protected abstract static class DefaultLaStatBuilder implements LaStatBuilder {
        private final Duration elapsedTime;
        private final boolean suspectStatus;
        private Long forwardHighLoss;
        private Long backwardHighLoss;
        private Long forwardConsecutiveHighLoss;
        private Long backwardConsecutiveHighLoss;
        private Long forwardAvailable;
        private Long backwardAvailable;
        private Long forwardUnavailable;
        private Long backwardUnavailable;
        private MilliPct forwardMinFrameLossRatio;
        private MilliPct forwardMaxFrameLossRatio;
        private MilliPct forwardAverageFrameLossRatio;
        private MilliPct backwardMinFrameLossRatio;
        private MilliPct backwardMaxFrameLossRatio;
        private MilliPct backwardAverageFrameLossRatio;

        protected DefaultLaStatBuilder(Duration elapsedTime, boolean suspectStatus) {
            this.elapsedTime = elapsedTime;
            this.suspectStatus = suspectStatus;
        }

        @Override
        public LaStatBuilder forwardHighLoss(Long forwardHighLoss) {
            this.forwardHighLoss = forwardHighLoss;
            return this;
        }

        @Override
        public LaStatBuilder backwardHighLoss(Long backwardHighLoss) {
            this.backwardHighLoss = backwardHighLoss;
            return this;
        }

        @Override
        public LaStatBuilder forwardConsecutiveHighLoss(
                Long forwardConsecutiveHighLoss) {
            this.forwardConsecutiveHighLoss = forwardConsecutiveHighLoss;
            return this;
        }

        @Override
        public LaStatBuilder backwardConsecutiveHighLoss(
                Long backwardConsecutiveHighLoss) {
            this.backwardConsecutiveHighLoss = backwardConsecutiveHighLoss;
            return this;
        }

        @Override
        public LaStatBuilder forwardAvailable(Long forwardAvailable) {
            this.forwardAvailable = forwardAvailable;
            return this;
        }

        @Override
        public LaStatBuilder backwardAvailable(Long backwardAvailable) {
            this.backwardAvailable = backwardAvailable;
            return this;
        }

        @Override
        public LaStatBuilder forwardUnavailable(Long forwardUnavailable) {
            this.forwardUnavailable = forwardUnavailable;
            return this;
        }

        @Override
        public LaStatBuilder backwardUnavailable(Long backwardUnavailable) {
            this.backwardUnavailable = backwardUnavailable;
            return this;
        }

        @Override
        public LaStatBuilder forwardMinFrameLossRatio(
                MilliPct forwardMinFrameLossRatio) {
            this.forwardMinFrameLossRatio = forwardMinFrameLossRatio;
            return this;
        }

        @Override
        public LaStatBuilder forwardMaxFrameLossRatio(
                MilliPct forwardMaxFrameLossRatio) {
            this.forwardMaxFrameLossRatio = forwardMaxFrameLossRatio;
            return this;
        }

        @Override
        public LaStatBuilder forwardAverageFrameLossRatio(
                MilliPct forwardAverageFrameLossRatio) {
            this.forwardAverageFrameLossRatio = forwardAverageFrameLossRatio;
            return this;
        }

        @Override
        public LaStatBuilder backwardMinFrameLossRatio(
                MilliPct backwardMinFrameLossRatio) {
            this.backwardMinFrameLossRatio = backwardMinFrameLossRatio;
            return this;
        }

        @Override
        public LaStatBuilder backwardMaxFrameLossRatio(
                MilliPct backwardMaxFrameLossRatio) {
            this.backwardMaxFrameLossRatio = backwardMaxFrameLossRatio;
            return this;
        }

        @Override
        public LaStatBuilder backwardAverageFrameLossRatio(
                MilliPct backwardAverageFrameLossRatio) {
            this.backwardAverageFrameLossRatio = backwardAverageFrameLossRatio;
            return this;
        }
    }

}
