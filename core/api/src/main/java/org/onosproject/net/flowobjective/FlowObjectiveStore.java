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
package org.onosproject.net.flowobjective;

import com.google.common.annotations.Beta;
import org.onosproject.net.behaviour.NextGroup;
import org.onosproject.store.Store;

/**
 * The flow objective store.
 */
@Beta
public interface FlowObjectiveStore
        extends Store<ObjectiveEvent, FlowObjectiveStoreDelegate> {

    /**
     * Adds a NextGroup to the store.
     *
     * @param nextId an integer
     * @param group a next group opaque object
     */
    void putNextGroup(Integer nextId, NextGroup group);

    /**
     * Fetch a next group from the store.
     * @param nextId an integer
     * @return a next group
     */
    NextGroup getNextGroup(Integer nextId);

    /**
     * Allocates a next objective id. This id is globally unique
     *
     * @return an integer
     */
    int allocateNextId();
}
