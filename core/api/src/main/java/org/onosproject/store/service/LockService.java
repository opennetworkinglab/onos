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
package org.onosproject.store.service;

/**
 * Service interface for mutual exclusion primitives.
 */
public interface LockService {

    /**
     * Creates a new lock instance.
     * A successful return from this method does not mean the resource guarded by the path is locked.
     * The caller is expected to call Lock.lock() to acquire the lock.
     * @param path unique lock name.
     * @return a Lock instance that can be used to acquire the lock.
     */
    Lock create(String path);

    /**
     * Adds a listener to be notified of lock events.
     * @param listener listener to be added.
     */
    void addListener(LockEventListener listener);

    /**
     * Removes a listener that was previously added.
     * @param listener listener to be removed.
     */
    void removeListener(LockEventListener listener);
}
