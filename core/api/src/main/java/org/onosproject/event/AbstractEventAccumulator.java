/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.event;

import org.onlab.util.AbstractAccumulator;

import java.util.Timer;

/**
 * Base implementation of an event accumulator. It allows triggering based on
 * event inter-arrival time threshold, maximum batch life threshold and maximum
 * batch size.
 */
public abstract class AbstractEventAccumulator
        extends AbstractAccumulator<Event>
        implements EventAccumulator {

    /**
     * Creates an event accumulator capable of triggering on the specified
     * thresholds.
     *
     * @param timer          timer to use for scheduling check-points
     * @param maxEvents      maximum number of events to accumulate before
     *                       processing is triggered
     * @param maxBatchMillis maximum number of millis allowed since the first
     *                       event before processing is triggered
     * @param maxIdleMillis  maximum number millis between events before
     *                       processing is triggered
     */
    protected AbstractEventAccumulator(Timer timer, int maxEvents,
                                       int maxBatchMillis, int maxIdleMillis) {
        super(timer, maxEvents, maxBatchMillis, maxIdleMillis);
    }
}
