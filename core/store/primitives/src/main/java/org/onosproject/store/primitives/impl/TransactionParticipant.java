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
     * Returns a boolean indicating whether the participant has pending updates.
     *
     * @return indicates whether the participant has pending updates
     */
    boolean hasPendingUpdates();

    /**
     * Executes the prepare phase.
     *
     * @return {@code true} is successful; {@code false} otherwise
     */
    CompletableFuture<Boolean> prepare();

    /**
     * Attempts to execute the commit phase for previously prepared transaction.
     *
     * @return future that is completed when the operation completes
     */
    CompletableFuture<Void> commit();

    /**
     * Executes the prepare and commit phases atomically.
     *
     * @return {@code true} is successful; {@code false} otherwise
     */
    CompletableFuture<Boolean> prepareAndCommit();

    /**
     * Attempts to execute the rollback phase for previously prepared transaction.
     *
     * @return future that is completed when the operation completes
     */
    CompletableFuture<Void> rollback();

}