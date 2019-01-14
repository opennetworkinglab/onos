/*
 * Copyright 2019-present Open Networking Foundation
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
package org.onosproject.openflow.controller.impl;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * This class wrap existing class LinkedBlockingQueue for solution problem
 * with handling and processed received messages in onos.
 *
 * @see java.util.concurrent.LinkedBlockingQueue
 */
public class LinkedBlockingMessagesQueue<T> {

    /**
     * Identifier of queue.
     */
    private int idQueue;

    /**
     * Size of queue.
     */
    private int sizeOfQueue;

    /**
     * Maximal bulk of messages that will be processed.
     */
    private int bulk;

    /**
     * Queue of messages.
     */
    private BlockingQueue<T> queue;

    /**
     * Constructor.
     *
     * @param idQueue     Identifier of queue
     * @param sizeOfQueue Size of queue
     * @param bulk        Maximal bulk of messages that will be processed
     */
    public LinkedBlockingMessagesQueue(int idQueue, int sizeOfQueue, int bulk) {
        this.idQueue = idQueue;
        this.sizeOfQueue = sizeOfQueue;
        this.queue = new LinkedBlockingQueue<>(this.sizeOfQueue);
        this.bulk = bulk;
    }

    /**
     * Returns the identifier of this queue.
     *
     * @return the id of this queue
     */
    public int idQueue() {
        return idQueue;
    }

    /**
     * Return the size of this queue.
     *
     * @return the size of this queue
     */
    public int sizeOfQueue() {
        return sizeOfQueue;
    }

    /**
     * Set size for this queue.
     *
     * @param sizeOfQueue Size of queue
     */
    public void setSizeOfQueue(int sizeOfQueue) {
        this.sizeOfQueue = sizeOfQueue;
        this.queue = new LinkedBlockingQueue<>(this.sizeOfQueue);
    }

    /**
     * Offer new message to this queue.
     *
     * @param message  elemet to add
     * @return <code>true</code> if the element was added to this queue, else <code>false</code>
     */
    public boolean offer(T message) {
        return this.queue.offer(message);
    }

    /**
     * Transfer bulk of elements from this queue to the <code>messages</code> collection.
     *
     * @param messages  the collection to transfer bulk of elements from this queue
     * @return the numbers of elements transfered
     */
    public int drainTo(Collection<? super T> messages) {
        return this.queue.drainTo(messages, this.bulk);
    }

    /**
     * Return the elements count in this queue.
     *
     * @return the elements count
     */
    public int size() {
        return this.queue.size();
    }

    /**
     * Return the maximal bulk of messages for this queue.
     *
     * @return maximal bulk of messages that will be processed
     */
    public int bulk() {
        return bulk;
    }

    /**
     * Set the maximal bulk of messages for this queue.
     *
     * @param bulk Maximal bulk of messages that will be processed
     */
    public void setBulk(int bulk) {
        this.bulk = bulk;
    }

}
