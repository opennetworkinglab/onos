/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onlab.util;

import java.util.List;

/**
 * Abstraction of an accumulator capable of collecting events and at some
 * point in time triggers processing of all previously accumulated events.
 */
public interface Accumulator<T> {

    /**
     * Adds an event to the current batch. This operation may, or may not
     * trigger processing of the current batch of events.
     *
     * @param event event to be added to the current batch
     */
    void add(T event);

    /**
     * Processes the specified list of accumulated events.
     *
     * @param events list of accumulated events
     */
    void processEvents(List<T> events);

}
