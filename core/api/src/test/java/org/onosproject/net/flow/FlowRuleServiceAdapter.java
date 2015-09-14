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

package org.onosproject.net.flow;

import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;

/**
 * Test adapter for flow rule service.
 */
public class FlowRuleServiceAdapter implements FlowRuleService {
    @Override
    public int getFlowRuleCount() {
        return 0;
    }

    @Override
    public Iterable<FlowEntry> getFlowEntries(DeviceId deviceId) {
        return null;
    }

    @Override
    public void applyFlowRules(FlowRule... flowRules) {
    }

    @Override
    public void removeFlowRules(FlowRule... flowRules) {
    }

    @Override
    public void removeFlowRulesById(ApplicationId appId) {
    }

    @Override
    public Iterable<FlowRule> getFlowRulesById(ApplicationId id) {
        return null;
    }

    @Override
    public Iterable<FlowRule> getFlowRulesByGroupId(ApplicationId appId, short groupId) {
        return null;
    }

    @Override
    public void apply(FlowRuleOperations ops) {
    }

    @Override
    public void addListener(FlowRuleListener listener) {
    }

    @Override
    public void removeListener(FlowRuleListener listener) {
    }

    @Override
    public Iterable<TableStatisticsEntry> getFlowTableStatistics(DeviceId deviceId) {
        return null;
    }
}
