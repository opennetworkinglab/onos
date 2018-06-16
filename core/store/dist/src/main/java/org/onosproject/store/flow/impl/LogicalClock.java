/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.store.flow.impl;

import java.util.concurrent.atomic.AtomicLong;

import org.onosproject.store.LogicalTimestamp;

/**
 * Logical clock.
 */
public final class LogicalClock {
    private final AtomicLong timestamp = new AtomicLong();

    /**
     * Records an event.
     *
     * @param timestamp the event timestamp
     */
    public void tick(LogicalTimestamp timestamp) {
        this.timestamp.accumulateAndGet(timestamp.value(), (x, y) -> Math.max(x, y) + 1);
    }

    /**
     * Returns a timestamped value.
     *
     * @param value the value to timestamp
     * @param <T>   the value type
     * @return the timestamped value
     */
    public <T> Timestamped<T> timestamp(T value) {
        return new Timestamped<>(value, getTimestamp());
    }

    /**
     * Increments and returns the current timestamp.
     *
     * @return the current timestamp
     */
    public LogicalTimestamp getTimestamp() {
        return new LogicalTimestamp(timestamp.incrementAndGet());
    }
}
