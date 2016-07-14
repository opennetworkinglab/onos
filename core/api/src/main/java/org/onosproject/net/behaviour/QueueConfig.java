/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.net.behaviour;

import com.google.common.primitives.UnsignedInteger;

import java.util.Set;

/**
 * Means to alter a device's dataplane queues.
 */
public interface QueueConfig {

    /**
     * Obtain all queues configured on a device.
     *
     * @return a list of queue descriptions
     */
    Set<QueueInfo> getQueues();

    /**
     * Obtain a specific queue given a queue id.
     *
     * @param queueId an unsigned integer representing a queue id
     * @return a queue description
     */
    QueueInfo getQueue(UnsignedInteger queueId);

    /**
     * Add a queue to a device.
     *
     * @param queue a queue description
     */
    void addQueue(QueueInfo queue);

    /**
     * Remove a queue from a device.
     *
     * @param queueId an unsigned integer
     */
    void removeQueue(UnsignedInteger queueId);

}
