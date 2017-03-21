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
 * A utility class for specifying a Stop Time for Delay and Loss Measurements.
 */
public final class StopTime extends SoamTime {

    private StopTime(TimeOption option, Duration relativeStart, Instant absoluteStart) {
        super(option, relativeStart, absoluteStart);
    }

    public static final StopTime none() {
        return new StopTime(StopTimeOption.NONE, null, null);
    }

    public static final StopTime relative(Duration relativeStop) {
        return new StopTime(StopTimeOption.RELATIVE, relativeStop, null);
    }

    public static final StopTime absolute(Instant absoluteStop) {
        return new StopTime(StopTimeOption.ABSOLUTE, null, absoluteStop);
    }

    @Override
    public String toString() {
        if (option == StopTimeOption.NONE) {
            return "none";
        } else if (option == StopTimeOption.ABSOLUTE) {
            return MoreObjects.toStringHelper(getClass()).add("absolute", absoluteTime).toString();
        } else if (option == StopTimeOption.RELATIVE) {
            return MoreObjects.toStringHelper(getClass()).add("relative", relativeTime).toString();
        }
        return "unknown";
    }

    /**
     * Options for Stop Time.
     */
    public enum StopTimeOption implements TimeOption {
        NONE,
        RELATIVE,
        ABSOLUTE;
    }
}