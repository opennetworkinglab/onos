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
package org.onosproject.incubator.net.l2monitoring.soam;

import java.time.Duration;
import java.time.Instant;

/**
 * A base class with common attributes of StartTime and StopTime.
 */
public abstract class SoamTime {
    protected final TimeOption option;
    protected final Duration relativeTime;
    protected final Instant absoluteTime;

    public TimeOption option() {
        return option;
    }

    public Duration relativeTime() {
        return relativeTime;
    }

    public Instant absoluteTime() {
        return absoluteTime;
    }

    protected SoamTime(TimeOption option, Duration relativeStart, Instant absoluteStart) {
        this.option = option;
        this.relativeTime = relativeStart;
        this.absoluteTime = absoluteStart;
    }

    /**
     * Abstract interface for TimeOptions on SoamTime concrete classes.
     */
    public interface TimeOption {
    }
}
