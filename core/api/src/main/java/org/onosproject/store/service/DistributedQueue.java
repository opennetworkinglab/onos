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

import java.util.concurrent.CompletableFuture;

/**
 * A distributed collection designed for holding elements prior to processing.
 * A queue provides insertion, extraction and inspection operations. The extraction operation
 * is designed to be non-blocking.
 *
 * @param <E> queue entry type
 */
public interface DistributedQueue<E> {

    /**
     * Returns total number of entries in the queue.
     * @return queue size
     */
    long size();

    /**
     * Returns true if queue has elements in it.
     * @return true is queue has elements, false otherwise
     */
    default boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Inserts an entry into the queue.
     * @param entry entry to insert
     */
    void push(E entry);

    /**
     * If the queue is non-empty, an entry will be removed from the queue and the returned future
     * will be immediately completed with it. If queue is empty when this call is made, the returned
     * future will be eventually completed when an entry is added to the queue.
     * @return queue entry
     */
    CompletableFuture<E> pop();

    /**
     * Returns an entry from the queue without removing it. If the queue is empty returns null.
     * @return queue entry or null if queue is empty
     */
    E peek();
}