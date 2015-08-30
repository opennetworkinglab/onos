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
 * Builder for distributed queue.
 *
 * @param <E> type queue elements.
 */
public interface DistributedQueueBuilder<E> {

    /**
     * Sets the name of the queue.
     * <p>
     * Each queue is identified by a unique name.
     * </p>
     * <p>
     * Note: This is a mandatory parameter.
     * </p>
     *
     * @param name name of the queue
     * @return this DistributedQueueBuilder for method chaining
     */
    DistributedQueueBuilder<E> withName(String name);

    /**
     * Sets a serializer that can be used to serialize
     * the elements pushed into the queue. The serializer
     * builder should be pre-populated with any classes that will be
     * put into the queue.
     * <p>
     * Note: This is a mandatory parameter.
     * </p>
     *
     * @param serializer serializer
     * @return this DistributedQueueBuilder for method chaining
     */
    DistributedQueueBuilder<E> withSerializer(Serializer serializer);

    /**
     *
     *
     * @return this DistributedQueueBuilder for method chaining
     */
    DistributedQueueBuilder<E> withMeteringDisabled();


    /**
     * Disables persistence of queues entries.
     * <p>
     * When persistence is disabled, a full cluster restart will wipe out all
     * queue entries.
     * </p>
     * @return this DistributedQueueBuilder for method chaining
     */
    DistributedQueueBuilder<E> withPersistenceDisabled();

    /**
     * Builds a queue based on the configuration options
     * supplied to this builder.
     *
     * @return new distributed queue
     * @throws java.lang.RuntimeException if a mandatory parameter is missing
     */
    DistributedQueue<E> build();
}
