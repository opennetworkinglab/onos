/*
 * Copyright 2014-present Open Networking Laboratory
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

/**
 * Abstraction of an event sink capable of processing the specified event types.
 */
public interface EventSink<E extends Event> {

    /**
     * Processes the specified event.
     *
     * @param event event to be processed
     */
    void process(E event);

    /**
     * Handles notification that event processing time limit has been exceeded.
     */
    default void onProcessLimit() {
    }

}
