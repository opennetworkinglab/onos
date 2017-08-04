/*
 * Copyright 2015-present Open Networking Foundation
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
 * The MutexTask interface should be implemented by any class whose
 * instances distributed across controllers are intended to be executed
 * in a mutually exclusive fashion.
 */
public interface MutexTask {

    /**
     * Begins the execution of a mutually exclusive task.
     * The start method will be called once the "lock" is acquired.
     * After the start method returns the lock is released and some other
     * instance can take over execution.
     */
    void start();

    /**
     * This method will be called when exclusivity of task execution
     * can no longer be guaranteed. The implementation should take necessary steps
     * to halt task execution in order to ensure correctness.
     */
    void stop();
}
