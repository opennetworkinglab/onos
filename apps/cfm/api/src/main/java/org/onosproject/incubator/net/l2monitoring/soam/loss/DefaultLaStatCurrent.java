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

/**
 * The default implementation of LossAvailabilityStatCurrent.
 * {@link LossAvailabilityStatCurrent}.
 */
public final class DefaultLaStatCurrent extends DefaultLaStat
                        implements LossAvailabilityStatCurrent {
    private final Instant startTime;

    private DefaultLaStatCurrent(DefaultLaStatCurrentBuilder builder) {
        super(builder);
        this.startTime = builder.startTime;
    }

    @Override
    public Instant startTime() {
        return startTime;
    }

    public static LaStatCurrentBuilder builder(Duration elapsedTime,
            boolean suspectStatus, Instant startTime) {
        return new DefaultLaStatCurrentBuilder(elapsedTime, suspectStatus,
                startTime);
    }

    private static final class DefaultLaStatCurrentBuilder
                extends DefaultLaStatBuilder implements LaStatCurrentBuilder {
        private Instant startTime;

        protected DefaultLaStatCurrentBuilder(Duration elapsedTime,
                boolean suspectStatus, Instant startTime) {
            super(elapsedTime, suspectStatus);
            this.startTime = startTime;
        }

        @Override
        public LossAvailabilityStatCurrent build() {
            return new DefaultLaStatCurrent(this);
        }
    }
}
