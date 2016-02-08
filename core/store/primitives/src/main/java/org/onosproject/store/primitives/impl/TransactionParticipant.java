/*
 * Copyright 2016 Open Networking Laboratory
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

import org.onosproject.store.primitives.TransactionId;
import org.onosproject.store.primitives.resources.impl.CommitResult;
import org.onosproject.store.primitives.resources.impl.PrepareResult;
import org.onosproject.store.primitives.resources.impl.RollbackResult;

/**
 * Participant in a two-phase commit protocol.
 */
public interface TransactionParticipant {

    /**
     * Attempts to execute the prepare phase for the specified {@link Transaction transaction}.
     * @param transaction transaction
     * @return future for prepare result
     */
    CompletableFuture<PrepareResult> prepare(Transaction transaction);

    /**
     * Attempts to execute the commit phase for previously prepared transaction.
     * @param transactionId transaction identifier
     * @return future for commit result
     */
    CompletableFuture<CommitResult> commit(TransactionId transactionId);

    /**
     * Attempts to execute the rollback phase for previously prepared transaction.
     * @param transactionId transaction identifier
     * @return future for rollback result
     */
    CompletableFuture<RollbackResult> rollback(TransactionId transactionId);
}