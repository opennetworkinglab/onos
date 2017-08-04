/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.net.intent;

import java.util.function.Function;

import org.onosproject.cluster.NodeId;
import org.onosproject.event.ListenerService;

import com.google.common.annotations.Beta;

/**
 * Service for partitioning work, represented via a unique identifier, onto cluster nodes.
 */
@Beta
public interface WorkPartitionService
    extends ListenerService<WorkPartitionEvent, WorkPartitionEventListener> {

    /**
     * Returns whether a given identifier maps to a partition owned by this
     * instance.
     *
     * @param id identifier
     * @param hasher function that maps identifier to a long value
     * @param <K> entity type
     * @return true if the identifier maps to a partition owned
     *         by this instance, otherwise false
     */
    <K> boolean isMine(K id, Function<K, Long> hasher);

    /**
     * Returns the owner for a given identifier.
     *
     * @param id identifier to query
     * @param hasher function that maps id to a long value
     * @param <K> entity type
     * @return the leader node identifier
     */
    <K> NodeId getLeader(K id, Function<K, Long> hasher);
}
