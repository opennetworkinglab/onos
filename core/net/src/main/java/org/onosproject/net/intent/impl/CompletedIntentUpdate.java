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
package org.onosproject.net.intent.impl;

import org.onosproject.net.flow.FlowRuleBatchOperation;
import org.onosproject.net.intent.BatchWrite;
import org.onosproject.net.intent.Intent;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Represents a completed phase of processing an intent.
 */
interface CompletedIntentUpdate extends IntentUpdate {

    /**
     * Write data to the specified BatchWrite after execution() is called.
     *
     * @param batchWrite batchWrite
     */
    default void writeAfterExecution(BatchWrite batchWrite) {}

    /**
     * Moves forward with the contained current batch.
     * This method is invoked when the batch is successfully completed.
     */
    default void batchSuccess() {}

    /**
     * Reverts the contained batches.
     * This method is invoked when the batch results in failure.
     */
    default void batchFailed() {}

    /**
     * Returns the current FlowRuleBatchOperation.
     *
     * @return current FlowRuleBatchOperation
     */
    default FlowRuleBatchOperation currentBatch() {
        return null;
    }

    /**
     * Returns all of installable intents this instance holds.
     *
     * @return all of installable intents
     */
    default List<Intent> allInstallables() {
        return Collections.emptyList();
    }

    @Override
    default Optional<IntentUpdate> execute() {
        return Optional.empty();
    }
}
