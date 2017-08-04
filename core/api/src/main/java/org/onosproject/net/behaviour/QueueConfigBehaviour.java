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
package org.onosproject.net.behaviour;

import com.google.common.annotations.Beta;
import org.onosproject.net.driver.HandlerBehaviour;

import java.util.Collection;

/**
 * Behaviour for handling various operations for queue configurations.
 */
@Beta
public interface QueueConfigBehaviour extends HandlerBehaviour {

    /**
     * Obtain all queues configured on a device.
     *
     * @return a list of queue descriptions
     */
    Collection<QueueDescription> getQueues();

    /**
     * Obtain a queue configured on a device.
     * @param queueDesc queue description
     * @return a queue description
     */
    QueueDescription getQueue(QueueDescription queueDesc);

    /**
     * Create a queue to a device.
     * @param queueDesc a queue description
     * @return true if succeeds, or false
     */
    boolean addQueue(QueueDescription queueDesc);

    /**
     * Delete a queue from a device.
     * @param queueId queue identifier
     */
    void deleteQueue(QueueId queueId);
}