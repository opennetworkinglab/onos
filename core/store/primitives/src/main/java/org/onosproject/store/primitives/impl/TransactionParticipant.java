/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.store.primitives.impl;

import java.util.concurrent.CompletableFuture;

/**
 * Participant in a two-phase commit protocol.
 */
public interface TransactionParticipant {

    /**
     * Returns if this participant has updates that need to be committed.
     * @return {@code true} if yes; {@code false} otherwise
     */
    default boolean hasPendingUpdates() {
        return totalUpdates() > 0;
    }

    /**
     * Returns the number of updates that need to committed for this participant.
     * @return update count.
     */
    int totalUpdates();

    /**
     * Executes the prepare and commit steps in a single go.
     * @return {@code true} is successful i.e updates are committed; {@code false} otherwise
     */
    CompletableFuture<Boolean> prepareAndCommit();

    /**
     * Executes the prepare phase.
     * @return {@code true} is successful; {@code false} otherwise
     */
    CompletableFuture<Boolean> prepare();

    /**
     * Attempts to execute the commit phase for previously prepared transaction.
     * @return future that is completed when the operation completes
     */
    CompletableFuture<Void> commit();

    /**
     * Attempts to execute the rollback phase for previously prepared transaction.
     * @return future that is completed when the operation completes
     */
    CompletableFuture<Void> rollback();
}