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
package org.onosproject.store.intent.impl;

/**
 * Service for interacting with the partition-to-instance assignments.
 */
public interface PartitionService {

    /**
     * Returns whether the given intent key is in a partition owned by this
     * instance or not.
     *
     * @param intentKey intent key to query
     * @return true if the key is owned by this instance, otherwise false
     */
    boolean isMine(String intentKey);

    // TODO add API for rebalancing partitions
}
