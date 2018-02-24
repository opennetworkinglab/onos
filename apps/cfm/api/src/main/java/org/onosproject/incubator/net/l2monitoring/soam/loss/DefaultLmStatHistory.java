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
 * The default implementation of LossMeasurementStatHistory.
 * {@link LossMeasurementStatHistory}
 */
public final class DefaultLmStatHistory extends DefaultLmStat
        implements LossMeasurementStatHistory {
    private final SoamId historyStatsId;
    private final Instant endTime;

    protected DefaultLmStatHistory(DefaultLmStatHistoryBuilder builder) {
        super(builder);
        this.historyStatsId = builder.historyStatsId;
        this.endTime = builder.endTime;
    }

    @Override
    public SoamId historyStatsId() {
        return historyStatsId;
    }


    @Override
    public Instant endTime() {
        return endTime;
    }

    public static LmStatHistoryBuilder builder(Duration elapsedTime,
            boolean suspectStatus, SoamId historyStatsId, Instant endTime) {
        return new DefaultLmStatHistoryBuilder(elapsedTime,
                suspectStatus, historyStatsId, endTime);
    }

    private static class DefaultLmStatHistoryBuilder extends DefaultLmStatBuilder
            implements LmStatHistoryBuilder {
        private final SoamId historyStatsId;
        private final Instant endTime;

        protected DefaultLmStatHistoryBuilder(Duration elapsedTime,
                boolean suspectStatus, SoamId historyStatsId, Instant endTime) {
            super(elapsedTime, suspectStatus);
            this.historyStatsId = historyStatsId;
            this.endTime = endTime;
        }

        @Override
        public LossMeasurementStatHistory build() {
            return new DefaultLmStatHistory(this);
        }
    }


}
