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
package org.onosproject.net.flowext;

import com.google.common.base.MoreObjects;
import org.onosproject.net.flow.CompletedBatchOperation;
import org.onosproject.net.flow.FlowRule;

import java.util.Set;

/**
 * Experimental extension to the flow rule subsystem; still under development.
 * <p>
 * Representation of a completed flow rule batch operation.
 * </p>
 */
//TODO explain the purpose of this class beyond FlowRuleProvider
public class FlowExtCompletedOperation extends CompletedBatchOperation {
    // the batchId is provided by application, once one flow rule of this batch failed
    // all the batch should withdraw
    private final long batchId;

    public FlowExtCompletedOperation(long batchId, boolean success, Set<FlowRule> failures) {
        super(success, failures, null);
        this.batchId = batchId;
    }

    /**
     * Returns the BatchId of this BatchOperation.
     *
     * @return the number of Batch
     */
    public long getBatchId() {
        return batchId;
    }

    /**
     * Returns a string representation of the object.
     *
     * @return a string representation of the object.
     */
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("success?", isSuccess())
                .add("failedItems", failedIds())
                .toString();
    }
}