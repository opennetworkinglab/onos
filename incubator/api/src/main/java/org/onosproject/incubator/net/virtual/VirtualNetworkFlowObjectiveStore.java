/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.incubator.net.virtual;

import org.onosproject.net.behaviour.NextGroup;
import org.onosproject.net.flowobjective.FlowObjectiveStoreDelegate;
import org.onosproject.net.flowobjective.ObjectiveEvent;

import java.util.Map;

/**
 * The flow objective store for virtual networks.
 */
public interface VirtualNetworkFlowObjectiveStore
        extends VirtualStore<ObjectiveEvent, FlowObjectiveStoreDelegate> {

    /**
     * Adds a NextGroup to the store, by mapping it to the nextId as key,
     * and replacing any previous mapping.
     *
     * @param networkId a virtual network identifier
     * @param nextId an integer
     * @param group a next group opaque object
     */
    void putNextGroup(NetworkId networkId, Integer nextId, NextGroup group);

    /**
     * Fetch a next group from the store.
     *
     * @param networkId a virtual network identifier
     * @param nextId an integer used as key
     * @return a next group, or null if group was not found
     */
    NextGroup getNextGroup(NetworkId networkId, Integer nextId);

    /**
     * Remove a next group mapping from the store.
     *
     * @param networkId a virtual network identifier
     * @param nextId  the key to remove from the store.
     * @return the next group which mapped to the nextId and is now removed, or
     *          null if no group mapping existed in the store
     */
    NextGroup removeNextGroup(NetworkId networkId, Integer nextId);

    /**
     * Fetch all groups from the store and their mapping to nextIds.
     *
     * @param networkId a virtual network identifier
     * @return a map that represents the current snapshot of Next-ids to NextGroups
     */
    Map<Integer, NextGroup> getAllGroups(NetworkId networkId);

    /**
     * Allocates a next objective id. This id is globally unique.
     *
     * @param networkId a virtual network identifier
     * @return an integer
     */
    int allocateNextId(NetworkId networkId);
}
