/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.net.intent.impl;

import com.google.common.collect.Sets;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.CompletedBatchOperation;
import org.onosproject.net.flow.DefaultFlowEntry;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleBatchOperation;
import org.onosproject.net.flow.FlowRuleListener;
import org.onosproject.net.flow.FlowRuleOperations;
import org.onosproject.net.flow.FlowRuleService;

import java.util.Set;
import java.util.concurrent.Future;
import java.util.stream.Collectors;


public class MockFlowRuleService implements FlowRuleService {

    final Set<FlowRule> flows = Sets.newHashSet();
    boolean success;

    public void setFuture(boolean success) {
        this.success = success;
    }

    @Override
    public Future<CompletedBatchOperation> applyBatch(FlowRuleBatchOperation batch) {
        throw new UnsupportedOperationException("deprecated");
    }

    @Override
    public void apply(FlowRuleOperations ops) {
        ops.stages().forEach(stage -> stage.forEach(flow -> {
            switch (flow.type()) {
                case ADD:
                case MODIFY: //TODO is this the right behavior for modify?
                    flows.add(flow.rule());
                    break;
                case REMOVE:
                    flows.remove(flow.rule());
                    break;
                default:
                    break;
            }
        }));
        if (success) {
            ops.callback().onSuccess(ops);
        } else {
            ops.callback().onError(ops);
        }
    }

    @Override
    public void addListener(FlowRuleListener listener) {
        //TODO not implemented
    }

    @Override
    public void removeListener(FlowRuleListener listener) {
        //TODO not implemented
    }

    @Override
    public int getFlowRuleCount() {
        return flows.size();
    }

    @Override
    public Iterable<FlowEntry> getFlowEntries(DeviceId deviceId) {
        return flows.stream()
                    .filter(flow -> flow.deviceId().equals(deviceId))
                    .map(DefaultFlowEntry::new)
                    .collect(Collectors.toList());
    }

    @Override
    public void applyFlowRules(FlowRule... flowRules) {
        for (FlowRule flow : flowRules) {
            flows.add(flow);
        }
    }

    @Override
    public void removeFlowRules(FlowRule... flowRules) {
        for (FlowRule flow : flowRules) {
            flows.remove(flow);
        }
    }

    @Override
    public void removeFlowRulesById(ApplicationId appId) {
        //TODO not implemented
    }

    @Override
    public Iterable<FlowRule> getFlowRulesById(ApplicationId id) {
        return flows.stream()
                    .filter(flow -> flow.appId() == id.id())
                    .collect(Collectors.toList());
    }

    @Override
    public Iterable<FlowRule> getFlowRulesByGroupId(ApplicationId appId, short groupId) {
        return flows.stream()
                .filter(flow -> flow.appId() == appId.id() && flow.groupId().id() == groupId)
                .collect(Collectors.toList());
    }
}

