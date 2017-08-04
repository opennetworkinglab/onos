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
package org.onosproject.net.flowobjective;

import com.google.common.annotations.Beta;

import java.util.Map;

import org.onosproject.net.behaviour.NextGroup;
import org.onosproject.store.Store;

/**
 * The flow objective store.
 */
@Beta
public interface FlowObjectiveStore
        extends Store<ObjectiveEvent, FlowObjectiveStoreDelegate> {

    /**
     * Adds a NextGroup to the store, by mapping it to the nextId as key,
     * and replacing any previous mapping.
     *
     * @param nextId an integer
     * @param group a next group opaque object
     */
    void putNextGroup(Integer nextId, NextGroup group);

    /**
     * Fetch a next group from the store.
     *
     * @param nextId an integer used as key
     * @return a next group, or null if group was not found
     */
    NextGroup getNextGroup(Integer nextId);

    /**
     * Remove a next group mapping from the store.
     *
     * @param nextId  the key to remove from the store.
     * @return the next group which mapped to the nextId and is now removed, or
     *          null if no group mapping existed in the store
     */
    NextGroup removeNextGroup(Integer nextId);

    /**
     * Fetch all groups from the store and their mapping to nextIds.
     *
     * @return a map that represents the current snapshot of Next-ids to NextGroups
     */
    Map<Integer, NextGroup> getAllGroups();

    /**
     * Allocates a next objective id. This id is globally unique
     *
     * @return an integer
     */
    int allocateNextId();
}
