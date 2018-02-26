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

import com.google.common.base.MoreObjects;

/**
 * A utility class for specifying a Start Time for Delay and Loss Measurements.
 */
public final class StartTime extends SoamTime {

    protected StartTime(TimeOption option, Duration relativeStart, Instant absoluteStart) {
        super(option, relativeStart, absoluteStart);
    }

    public static final StartTime immediate() {
        return new StartTime(StartTimeOption.IMMEDIATE, null, null);
    }

    public static final StartTime relative(Duration relativeStart) {
        return new StartTime(StartTimeOption.RELATIVE, relativeStart, null);
    }

    public static final StartTime absolute(Instant absoluteStart) {
        return new StartTime(StartTimeOption.ABSOLUTE, null, absoluteStart);
    }

    @Override
    public String toString() {
        if (option == StartTimeOption.IMMEDIATE) {
            return "immediate";
        } else if (option == StartTimeOption.ABSOLUTE) {
            return MoreObjects.toStringHelper(getClass()).add("absolute", absoluteTime).toString();
        } else if (option == StartTimeOption.RELATIVE) {
            return MoreObjects.toStringHelper(getClass()).add("relative", relativeTime).toString();
        }
        return "unknown";
    }

    /**
     * Options for Start Time.
     */
    public enum StartTimeOption implements TimeOption {
        IMMEDIATE,
        RELATIVE,
        ABSOLUTE;
    }
}