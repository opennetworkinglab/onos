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

/**
 * Distributed version of java.util.concurrent.atomic.AtomicLong.
 */
public interface AtomicCounter {

    /**
     * Atomically increment by one the current value.
     *
     * @return updated value
     */
    long incrementAndGet();

    /**
     * Atomically increment by one the current value.
     *
     * @return previous value
     */
    long getAndIncrement();

    /**
     * Atomically adds the given value to the current value.
     *
     * @param delta the value to add
     * @return previous value
     */
    long getAndAdd(long delta);

    /**
     * Atomically adds the given value to the current value.
     *
     * @param delta the value to add
     * @return updated value
     */
    long addAndGet(long delta);

    /**
     * Returns the current value of the counter without modifying it.
     *
     * @return current value
     */
    long get();
}
