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
import java.time.Instant;

import org.onosproject.incubator.net.l2monitoring.soam.SoamId;

/**
 * The default implementation of DelayMeasurementStatHistory.
 * {@link DelayMeasurementStatHistory}.
 */
public class DefaultDelayMeasurementStatHistory extends DefaultDelayMeasurementStat
        implements DelayMeasurementStatHistory {

    private final SoamId historyStatsId;
    private final Instant endTime;

    protected DefaultDelayMeasurementStatHistory(DefaultDmStatHistoryBuilder builder) {
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

    public static DmStatHistoryBuilder builder(SoamId historyStatsId,
            Duration elapsedTime, boolean suspectStatus) {
        return new DefaultDmStatHistoryBuilder(
                historyStatsId, elapsedTime, suspectStatus);
    }

    /**
     * Builder for {@link DelayMeasurementStatHistory}.
     */
    private static final class DefaultDmStatHistoryBuilder
        extends DefaultDmStatBuilder implements DmStatHistoryBuilder {
        private final SoamId historyStatsId;
        private Instant endTime;

        private DefaultDmStatHistoryBuilder(SoamId historyStatsId,
                Duration elapsedTime, boolean suspectStatus) {
            super(elapsedTime, suspectStatus);
            this.historyStatsId = historyStatsId;
        }

        @Override
        public DmStatHistoryBuilder endTime(Instant endTime) {
            this.endTime = endTime;
            return this;
        }

        @Override
        public DelayMeasurementStat build() {
            return new DefaultDelayMeasurementStatHistory(this);
        }
    }
}
