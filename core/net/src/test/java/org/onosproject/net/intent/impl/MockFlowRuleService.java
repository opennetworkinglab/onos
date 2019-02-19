/*
 * Copyright 2014-present Open Networking Foundation
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
import org.onosproject.net.flow.DefaultFlowEntry;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleOperations;
import org.onosproject.net.flow.FlowRuleServiceAdapter;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class MockFlowRuleService extends FlowRuleServiceAdapter {

    final Set<FlowRule> flows = Sets.newHashSet();
    boolean success;

    int errorFlow = -1;
    public void setErrorFlow(int errorFlow) {
        this.errorFlow = errorFlow;
    }

    public void setFuture(boolean success) {
        this.success = success;
    }

    @Override
    public void apply(FlowRuleOperations ops) {
        AtomicBoolean thisSuccess = new AtomicBoolean(success);
        ops.stages().forEach(stage -> stage.forEach(flow -> {
            if (errorFlow == flow.rule().id().value()) {
                thisSuccess.set(false);
            } else {
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
            }
        }));
        if (thisSuccess.get()) {
            ops.callback().onSuccess(ops);
        } else {
            ops.callback().onError(ops);
        }
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
        Collections.addAll(flows, flowRules);
    }

    @Override
    public void removeFlowRules(FlowRule... flowRules) {
        for (FlowRule flow : flowRules) {
            flows.remove(flow);
        }
    }

    @Override
    public Iterable<FlowRule> getFlowRulesByGroupId(ApplicationId appId, short groupId) {
        return flows.stream()
                .filter(flow -> flow.appId() == appId.id() && flow.groupId().id() == groupId)
                .collect(Collectors.toList());
    }
}

