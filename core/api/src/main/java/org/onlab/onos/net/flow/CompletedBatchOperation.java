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

import java.util.Set;

import com.google.common.collect.ImmutableSet;

/**
 * Representation of a completed flow rule batch operation.
 */
public class CompletedBatchOperation implements BatchOperationResult<FlowRule> {

    private final boolean success;
    private final Set<FlowRule> failures;

    /**
     * Creates a new batch completion result.
     *
     * @param success  indicates whether the completion is successful.
     * @param failures set of any failures encountered
     */
    public CompletedBatchOperation(boolean success, Set<? extends FlowRule> failures) {
        this.success = success;
        this.failures = ImmutableSet.copyOf(failures);
    }

    @Override
    public boolean isSuccess() {
        return success;
    }

    @Override
    public Set<FlowRule> failedItems() {
        return failures;
    }

}
