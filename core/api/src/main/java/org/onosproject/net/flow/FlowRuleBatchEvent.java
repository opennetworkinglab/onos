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

import org.onosproject.event.AbstractEvent;

/**
 * Describes flow rule batch event.
 */
public final class FlowRuleBatchEvent extends AbstractEvent<FlowRuleBatchEvent.Type, FlowRuleBatchRequest> {

    /**
     * Type of flow rule events.
     */
    public enum Type {

        // Request has been forwarded to MASTER Node
        /**
         * Signifies that a batch operation has been initiated.
         */
        BATCH_OPERATION_REQUESTED,

        // MASTER Node has pushed the batch down to the Device
        // (e.g., Received barrier reply)
        /**
         * Signifies that a batch operation has completed.
         */
        BATCH_OPERATION_COMPLETED,
    }

    private final CompletedBatchOperation result;

    /**
     * Constructs a new FlowRuleBatchEvent.
     * @param request batch operation request.
     * @return event.
     */
    public static FlowRuleBatchEvent requested(FlowRuleBatchRequest request) {
        FlowRuleBatchEvent event = new FlowRuleBatchEvent(Type.BATCH_OPERATION_REQUESTED, request, null);
        return event;
    }

    /**
     * Constructs a new FlowRuleBatchEvent.
     * @param request batch operation request.
     * @param result completed batch operation result.
     * @return event.
     */
    public static FlowRuleBatchEvent completed(FlowRuleBatchRequest request, CompletedBatchOperation result) {
        FlowRuleBatchEvent event = new FlowRuleBatchEvent(Type.BATCH_OPERATION_COMPLETED, request, result);
        return event;
    }

    /**
     * Returns the result of this batch operation.
     * @return batch operation result.
     */
    public CompletedBatchOperation result() {
        return result;
    }

    /**
     * Creates an event of a given type and for the specified flow rule batch.
     *
     * @param type    flow rule batch event type
     * @param batch    event flow rule batch subject
     */
    private FlowRuleBatchEvent(Type type, FlowRuleBatchRequest request, CompletedBatchOperation result) {
        super(type, request);
        this.result = result;
    }
}
