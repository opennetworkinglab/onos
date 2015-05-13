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
package org.onosproject.store.service;

import org.onosproject.store.Timestamp;

/**
 * Clock service that can generate timestamps based off of two input objects.
 * Implementations are free to only take one or none of the objects into account
 * when generating timestamps.
 */
public interface ClockService<T, U> {

    /**
     * Gets a new timestamp for the given objects.
     *
     * @param object1 First object to use when generating timestamps
     * @param object2 Second object to use when generating timestamps
     * @return the new timestamp
     */
    Timestamp getTimestamp(T object1, U object2);
}
