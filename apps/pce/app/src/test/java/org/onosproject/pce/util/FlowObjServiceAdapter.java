/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.pce.util;

import java.util.List;
import com.google.common.collect.ImmutableList;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.NextObjective;

/**
 * Test implementation of FlowObjectiveService.
 */
public class FlowObjServiceAdapter implements FlowObjectiveService {

    private ForwardingObjective forwardingObjective;
    @Override
    public void filter(DeviceId deviceId, FilteringObjective filteringObjective) {

    }

    @Override
    public void forward(DeviceId deviceId, ForwardingObjective forwardingObjective) {
        this.forwardingObjective = forwardingObjective;
    }

    @Override
    public void next(DeviceId deviceId, NextObjective nextObjective) {

    }

    @Override
    public int allocateNextId() {
        return 0;
    }

    @Override
    public void initPolicy(String policy) {

    }

    public ForwardingObjective forwardingObjective() {
        return forwardingObjective;
    }

    @Override
    public List<String> getNextMappings() {
        return ImmutableList.of();
    }

    @Override
    public List<String> getPendingNexts() {
        return ImmutableList.of();
    }
}
