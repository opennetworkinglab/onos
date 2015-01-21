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

import org.onosproject.net.flow.FlowRuleBatchEntry.FlowRuleOperation;

import java.util.Optional;


public class FlowRuleBatchEntry
        extends BatchOperationEntry<FlowRuleOperation, FlowRule> {

    private final Optional<Long> id;

    public FlowRuleBatchEntry(FlowRuleOperation operator, FlowRule target) {
        super(operator, target);
        this.id = Optional.empty();
    }

    public FlowRuleBatchEntry(FlowRuleOperation operator, FlowRule target, long id) {
        super(operator, target);
        this.id = Optional.of(id);
    }

    public Optional<Long> id() {
        return id;
    }

    public enum FlowRuleOperation {
        ADD,
        REMOVE,
        MODIFY
    }

}
