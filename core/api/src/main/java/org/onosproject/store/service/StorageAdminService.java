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

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.onosproject.store.primitives.TransactionId;

/**
 * Service for administering storage instances.
 */
public interface StorageAdminService {

    /**
     * Returns information about all partitions in the system.
     *
     * @return list of partition information
     * @deprecated 1.5.0 Falcon Release
     */
    @Deprecated
    List<PartitionInfo> getPartitionInfo();

    /**
     * Returns information about all the consistent maps in the system.
     *
     * @return list of map information
     * @deprecated 1.5.0 Falcon Release
     */
    @Deprecated
    List<MapInfo> getMapInfo();

    /**
     * Returns information about all the atomic counters in the system.
     * If 2 counters belonging to 2 different databases have the same name,
     * then only one counter from one database is returned.
     *
     * @return mapping from counter name to that counter's next value
     * @deprecated 1.5.0 Falcon Release
     */
    @Deprecated
    Map<String, Long> getCounters();

    /**
     * Returns information about all the atomic partitioned database counters in the system.
     *
     * @return mapping from counter name to that counter's next value
     * @deprecated 1.5.0 Falcon Release
     */
    @Deprecated
    Map<String, Long> getPartitionedDatabaseCounters();

    /**
     * Returns information about all the atomic in-memory database counters in the system.
     *
     * @return mapping from counter name to that counter's next value
     * @deprecated 1.5.0 Falcon Release
     */
    @Deprecated
    Map<String, Long> getInMemoryDatabaseCounters();

    /**
     * Returns all pending transactions.
     *
     * @return collection of pending transaction identifiers.
     */
    Collection<TransactionId> getPendingTransactions();
}
