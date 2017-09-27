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
package org.onosproject.net.flow.oldbatch;

import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import org.onosproject.net.DeviceId;

@Deprecated
/**
 * @deprecated in Drake release - no longer a public API
 */
public class FlowRuleBatchRequest {

    /**
     * This id is used to carry to id of the original
     * FlowOperations and track where this batch operation
     * came from. The id is unique cluster wide.
     */
    private final long batchId;

    private final Set<FlowRuleBatchEntry> ops;

    public FlowRuleBatchRequest(long batchId, Set<FlowRuleBatchEntry> ops) {
        this.batchId = batchId;
        this.ops = ImmutableSet.copyOf(ops);
    }

    public Set<FlowRuleBatchEntry> ops() {
        return ops;
    }

    public FlowRuleBatchOperation asBatchOperation(DeviceId deviceId) {
        List<FlowRuleBatchEntry> entries = Lists.newArrayList();
        entries.addAll(ops);
        return new FlowRuleBatchOperation(entries, deviceId, batchId);
    }

    public long batchId() {
        return batchId;
    }
}
