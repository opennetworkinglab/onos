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
package org.onlab.onos.net.flow;

import java.util.Collections;
import java.util.List;

import org.onlab.onos.net.flow.FlowRuleBatchEntry.FlowRuleOperation;

import com.google.common.collect.Lists;

public class FlowRuleBatchRequest {

    private final int batchId;
    private final List<FlowRule> toAdd;
    private final List<FlowRule> toRemove;

    public FlowRuleBatchRequest(int batchId, List<? extends FlowRule> toAdd, List<? extends FlowRule> toRemove) {
        this.batchId = batchId;
        this.toAdd = Collections.unmodifiableList(toAdd);
        this.toRemove = Collections.unmodifiableList(toRemove);
    }

    public List<FlowRule> toAdd() {
        return toAdd;
    }

    public List<FlowRule> toRemove() {
        return toRemove;
    }

    public FlowRuleBatchOperation asBatchOperation() {
        List<FlowRuleBatchEntry> entries = Lists.newArrayList();
        for (FlowRule e : toAdd) {
            entries.add(new FlowRuleBatchEntry(FlowRuleOperation.ADD, e));
        }
        for (FlowRule e : toRemove) {
            entries.add(new FlowRuleBatchEntry(FlowRuleOperation.REMOVE, e));
        }
        return new FlowRuleBatchOperation(entries);
    }

    public int batchId() {
        return batchId;
    }
}
