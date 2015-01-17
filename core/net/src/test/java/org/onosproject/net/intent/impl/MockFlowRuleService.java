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

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Future;

import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.CompletedBatchOperation;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleBatchEntry;
import org.onosproject.net.flow.FlowRuleBatchOperation;
import org.onosproject.net.flow.FlowRuleListener;
import org.onosproject.net.flow.FlowRuleOperations;
import org.onosproject.net.flow.FlowRuleService;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;


public class MockFlowRuleService implements FlowRuleService {

    private Future<CompletedBatchOperation> future;
    final Set<FlowRule> flows = Sets.newHashSet();

    public void setFuture(boolean success) {
        setFuture(success, 0);
    }

    public void setFuture(boolean success, long intentId) {
        if (success) {
            future = Futures.immediateFuture(new CompletedBatchOperation(true, Collections.emptySet(), null));
        } else {
            final Set<Long> failedIds = ImmutableSet.of(intentId);
            future = Futures.immediateFuture(
                    new CompletedBatchOperation(false, flows, failedIds, null));
        }
    }

    @Override
    public Future<CompletedBatchOperation> applyBatch(FlowRuleBatchOperation batch) {
        for (FlowRuleBatchEntry fbe : batch.getOperations()) {
            FlowRule fr = fbe.target();
            switch (fbe.operator()) {
                case ADD:
                    flows.add(fr);
                    break;
                case REMOVE:
                    flows.remove(fr);
                    break;
                case MODIFY:
                    break;
                default:
                    break;
            }
        }
        return future;
    }

    @Override
    public void apply(FlowRuleOperations ops) {

    }

    @Override
    public int getFlowRuleCount() {
        return flows.size();
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
    public void addListener(FlowRuleListener listener) {

    }

    @Override
    public void removeListener(FlowRuleListener listener) {

    }
}

