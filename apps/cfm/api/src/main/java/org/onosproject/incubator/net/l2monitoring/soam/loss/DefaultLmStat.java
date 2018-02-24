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
 * The default implementation of {@link LossMeasurementStat}.
 */
public abstract class DefaultLmStat implements LossMeasurementStat {
    private final Duration elapsedTime;
    private final boolean suspectStatus;
    private final Long forwardTransmittedFrames;
    private final Long forwardReceivedFrames;
    private final MilliPct forwardMinFrameLossRatio;
    private final MilliPct forwardMaxFrameLossRatio;
    private final MilliPct forwardAverageFrameLossRatio;
    private final Long backwardTransmittedFrames;
    private final Long backwardReceivedFrames;
    private final MilliPct backwardMinFrameLossRatio;
    private final MilliPct backwardMaxFrameLossRatio;
    private final MilliPct backwardAverageFrameLossRatio;
    private final Long soamPdusSent;
    private final Long soamPdusReceived;

    protected DefaultLmStat(DefaultLmStatBuilder builder) {
        this.elapsedTime = builder.elapsedTime;
        this.suspectStatus = builder.suspectStatus;
        this.forwardTransmittedFrames = builder.forwardTransmittedFrames;
        this.forwardReceivedFrames = builder.forwardReceivedFrames;
        this.forwardMinFrameLossRatio = builder.forwardMinFrameLossRatio;
        this.forwardMaxFrameLossRatio = builder.forwardMaxFrameLossRatio;
        this.forwardAverageFrameLossRatio = builder.forwardAverageFrameLossRatio;
        this.backwardTransmittedFrames = builder.backwardTransmittedFrames;
        this.backwardReceivedFrames = builder.backwardReceivedFrames;
        this.backwardMinFrameLossRatio = builder.backwardMinFrameLossRatio;
        this.backwardMaxFrameLossRatio = builder.backwardMaxFrameLossRatio;
        this.backwardAverageFrameLossRatio = builder.backwardAverageFrameLossRatio;
        this.soamPdusSent = builder.soamPdusSent;
        this.soamPdusReceived = builder.soamPdusReceived;
    }

    @Override
    public Duration elapsedTime() {
        return this.elapsedTime;
    }

    @Override
    public boolean suspectStatus() {
        return this.suspectStatus;
    }

    @Override
    public Long forwardTransmittedFrames() {
        return this.forwardTransmittedFrames;
    }

    @Override
    public Long forwardReceivedFrames() {
        return this.forwardReceivedFrames;
    }

    @Override
    public MilliPct forwardMinFrameLossRatio() {
        return this.forwardMinFrameLossRatio;
    }

    @Override
    public MilliPct forwardMaxFrameLossRatio() {
        return this.forwardMaxFrameLossRatio;
    }

    @Override
    public MilliPct forwardAverageFrameLossRatio() {
        return this.forwardAverageFrameLossRatio;
    }

    @Override
    public Long backwardTransmittedFrames() {
        return this.backwardTransmittedFrames;
    }

    @Override
    public Long backwardReceivedFrames() {
        return this.backwardReceivedFrames;
    }

    @Override
    public MilliPct backwardMinFrameLossRatio() {
        return this.backwardMinFrameLossRatio;
    }

    @Override
    public MilliPct backwardMaxFrameLossRatio() {
        return this.backwardMaxFrameLossRatio;
    }

    @Override
    public MilliPct backwardAverageFrameLossRatio() {
        return this.backwardAverageFrameLossRatio;
    }

    @Override
    public Long soamPdusSent() {
        return this.soamPdusSent;
    }

    @Override
    public Long soamPdusReceived() {
        return this.soamPdusReceived;
    }

    /**
     * Abstract implementation of LmStatBuilder.
     * {@link LossMeasurementStat.LmStatBuilder}
     */
    protected abstract static class DefaultLmStatBuilder implements LmStatBuilder {
        private final Duration elapsedTime;
        private final boolean suspectStatus;
        private Long forwardTransmittedFrames;
        private Long forwardReceivedFrames;
        private MilliPct forwardMinFrameLossRatio;
        private MilliPct forwardMaxFrameLossRatio;
        private MilliPct forwardAverageFrameLossRatio;
        private Long backwardTransmittedFrames;
        private Long backwardReceivedFrames;
        private MilliPct backwardMinFrameLossRatio;
        private MilliPct backwardMaxFrameLossRatio;
        private MilliPct backwardAverageFrameLossRatio;
        private Long soamPdusSent;
        private Long soamPdusReceived;

        protected DefaultLmStatBuilder(Duration elapsedTime, boolean suspectStatus) {
            this.elapsedTime = elapsedTime;
            this.suspectStatus = suspectStatus;
        }

        @Override
        public LmStatBuilder forwardTransmittedFrames(
                Long forwardTransmittedFrames) {
            this.forwardTransmittedFrames = forwardTransmittedFrames;
            return this;
        }

        @Override
        public LmStatBuilder forwardReceivedFrames(Long forwardReceivedFrames) {
            this.forwardReceivedFrames = forwardReceivedFrames;
            return this;
        }

        @Override
        public LmStatBuilder forwardMinFrameLossRatio(
                MilliPct forwardMinFrameLossRatio) {
            this.forwardMinFrameLossRatio = forwardMinFrameLossRatio;
            return this;
        }

        @Override
        public LmStatBuilder forwardMaxFrameLossRatio(
                MilliPct forwardMaxFrameLossRatio) {
            this.forwardMaxFrameLossRatio = forwardMaxFrameLossRatio;
            return this;
        }

        @Override
        public LmStatBuilder forwardAverageFrameLossRatio(
                MilliPct forwardAverageFrameLossRatio) {
            this.forwardAverageFrameLossRatio = forwardAverageFrameLossRatio;
            return this;
        }

        @Override
        public LmStatBuilder backwardTransmittedFrames(
                Long backwardTransmittedFrames) {
            this.backwardTransmittedFrames = backwardTransmittedFrames;
            return this;
        }

        @Override
        public LmStatBuilder backwardReceivedFrames(
                Long backwardReceivedFrames) {
            this.backwardReceivedFrames = backwardReceivedFrames;
            return this;
        }

        @Override
        public LmStatBuilder backwardMinFrameLossRatio(
                MilliPct backwardMinFrameLossRatio) {
            this.backwardMinFrameLossRatio = backwardMinFrameLossRatio;
            return this;
        }

        @Override
        public LmStatBuilder backwardMaxFrameLossRatio(
                MilliPct backwardMaxFrameLossRatio) {
            this.backwardMaxFrameLossRatio = backwardMaxFrameLossRatio;
            return this;
        }

        @Override
        public LmStatBuilder backwardAverageFrameLossRatio(
                MilliPct backwardAverageFrameLossRatio) {
            this.backwardAverageFrameLossRatio = backwardAverageFrameLossRatio;
            return this;
        }

        @Override
        public LmStatBuilder soamPdusSent(Long soamPdusSent) {
            this.soamPdusSent = soamPdusSent;
            return this;
        }

        @Override
        public LmStatBuilder soamPdusReceived(Long soamPdusReceived) {
            this.soamPdusReceived = soamPdusReceived;
            return this;
        }
    }

}
