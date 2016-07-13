/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.core;

import org.onosproject.store.service.WallClockTimestamp;

/**
 * The <a href="http://www.cse.buffalo.edu/tech-reports/2014-04.pdf">hybrid logical time</a> keeper service.
 */
public interface HybridLogicalClockService {

    /**
     * Returns the current hybrid logical time.
     * @return current hybrid logical time
     */
    HybridLogicalTime timeNow();

    /**
     * Records a (receive) event and accordingly makes adjustments to the hybrid logical time.
     * @param time received event time
     */
    void recordEventTime(HybridLogicalTime time);

    /**
     * Returns the current time derived from the hybrid logical time.
     * @return current system time
     */
    default long now() {
        return timeNow().time();
    }

    /**
     * Returns the current time as a {@code WallClockTimestamp}.
     * @return wall clock timestamp
     */
    default WallClockTimestamp wallClockTimestamp() {
        return new WallClockTimestamp(now());
    }
}
