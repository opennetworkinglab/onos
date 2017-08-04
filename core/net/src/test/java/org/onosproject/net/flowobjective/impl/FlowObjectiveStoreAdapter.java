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
package org.onosproject.net.flowobjective.impl;

import java.util.Map;

import org.onosproject.net.behaviour.NextGroup;
import org.onosproject.net.flowobjective.FlowObjectiveStore;
import org.onosproject.net.flowobjective.FlowObjectiveStoreDelegate;

/**
 * Test adapter for the flow objective store API.
 */
public class FlowObjectiveStoreAdapter implements FlowObjectiveStore {
    @Override
    public void putNextGroup(Integer nextId, NextGroup group) {

    }

    @Override
    public NextGroup getNextGroup(Integer nextId) {
        return null;
    }

    @Override
    public NextGroup removeNextGroup(Integer nextId) {
        return null;
    }

    @Override
    public int allocateNextId() {
        return 0;
    }

    @Override
    public void setDelegate(FlowObjectiveStoreDelegate delegate) {

    }

    @Override
    public void unsetDelegate(FlowObjectiveStoreDelegate delegate) {

    }

    @Override
    public boolean hasDelegate() {
        return false;
    }

    @Override
    public Map<Integer, NextGroup> getAllGroups() {
        return null;
    }
}
