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
package org.onosproject.net.flow;

import java.util.Collections;
import java.util.List;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;



import com.google.common.collect.Lists;

public class FlowRuleBatchRequest {

    private final int batchId;
    private final List<FlowRuleBatchEntry> toAdd;
    private final List<FlowRuleBatchEntry> toRemove;

    public FlowRuleBatchRequest(int batchId, List<FlowRuleBatchEntry> toAdd,
                                List<FlowRuleBatchEntry> toRemove) {
        this.batchId = batchId;
        this.toAdd = Collections.unmodifiableList(toAdd);
        this.toRemove = Collections.unmodifiableList(toRemove);
    }

    public List<FlowRule> toAdd() {
        return FluentIterable.from(toAdd).transform(
                new Function<FlowRuleBatchEntry, FlowRule>() {

            @Override
            public FlowRule apply(FlowRuleBatchEntry input) {
                return input.getTarget();
            }
        }).toList();
    }

    public List<FlowRule> toRemove() {
        return FluentIterable.from(toRemove).transform(
                new Function<FlowRuleBatchEntry, FlowRule>() {

                    @Override
                    public FlowRule apply(FlowRuleBatchEntry input) {
                        return input.getTarget();
                    }
                }).toList();
    }

    public FlowRuleBatchOperation asBatchOperation() {
        List<FlowRuleBatchEntry> entries = Lists.newArrayList();
        entries.addAll(toAdd);
        entries.addAll(toRemove);
        return new FlowRuleBatchOperation(entries);
    }

    public int batchId() {
        return batchId;
    }
}
