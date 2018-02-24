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
import java.time.Instant;

import org.onosproject.incubator.net.l2monitoring.soam.SoamId;

/**
 * The default implementation of LossAvailabilityStatHistory.
 * {@link LossAvailabilityStatHistory}.
 */
public final class DefaultLaStatHistory extends DefaultLaStat
                        implements LossAvailabilityStatHistory {
    private final Instant endTime;
    private final SoamId historyStatsId;

    private DefaultLaStatHistory(DefaultLaStatHistoryBuilder builder) {
        super(builder);
        this.endTime = builder.endTime;
        this.historyStatsId = builder.historyStatsId;
    }

    @Override
    public Instant endTime() {
        return endTime;
    }

    @Override
    public SoamId historyStatsId() {
        return historyStatsId;
    }

    public static LaStatHistoryBuilder builder(Duration elapsedTime,
            boolean suspectStatus, SoamId historyStatsId, Instant endTime) {
        return new DefaultLaStatHistoryBuilder(elapsedTime, suspectStatus,
                historyStatsId, endTime);
    }

    private static final class DefaultLaStatHistoryBuilder
                extends DefaultLaStatBuilder implements LaStatHistoryBuilder {
        private Instant endTime;
        private SoamId historyStatsId;

        protected DefaultLaStatHistoryBuilder(Duration elapsedTime,
                boolean suspectStatus, SoamId historyStatsId, Instant endTime) {
            super(elapsedTime, suspectStatus);
            this.historyStatsId = historyStatsId;
            this.endTime = endTime;
        }

        @Override
        public LossAvailabilityStatHistory build() {
            return new DefaultLaStatHistory(this);
        }
    }
}
