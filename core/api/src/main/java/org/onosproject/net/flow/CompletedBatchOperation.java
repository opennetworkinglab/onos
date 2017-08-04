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
package org.onosproject.net.flow;


import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;
import org.onosproject.net.DeviceId;

import java.util.Collections;
import java.util.Set;

/**
 * Representation of a completed flow rule batch operation.
 */
public class CompletedBatchOperation implements BatchOperationResult<FlowRule> {

    private final boolean success;
    private final Set<FlowRule> failures;
    private final Set<Long> failedIds;
    private final DeviceId deviceId;

    /**
     * Creates a new batch completion result.
     *
     * @param success  indicates whether the completion is successful
     * @param failures set of any failures encountered
     * @param failedIds (optional) set of failed operation ids
     * @param deviceId the device this operation completed for
     */
    public CompletedBatchOperation(boolean success, Set<? extends FlowRule> failures,
                                   Set<Long> failedIds, DeviceId deviceId) {
        this.success = success;
        this.failures = ImmutableSet.copyOf(failures);
        this.failedIds = ImmutableSet.copyOf(failedIds);
        this.deviceId = deviceId;
    }

    /**
     * Creates a new batch completion result.
     *
     * @param success  indicates whether the completion is successful.
     * @param failures set of any failures encountered
     * @param deviceId the device this operation completed for
     */
    public CompletedBatchOperation(boolean success, Set<? extends FlowRule> failures,
                                   DeviceId deviceId) {
        this.success = success;
        this.failures = ImmutableSet.copyOf(failures);
        this.failedIds = Collections.emptySet();
        this.deviceId = deviceId;
    }



    @Override
    public boolean isSuccess() {
        return success;
    }

    @Override
    public Set<FlowRule> failedItems() {
        return failures;
    }

    public Set<Long> failedIds() {
        return failedIds;
    }

    public DeviceId deviceId() {
        return this.deviceId;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("success?", success)
                .add("failedItems", failures)
                .add("failedIds", failedIds)
                .add("deviceId", deviceId)
                .toString();
    }
}
