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
package org.onosproject.store.primitives.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.onosproject.store.primitives.TransactionId;
import org.onosproject.store.primitives.resources.impl.CommitResult;
import org.onosproject.store.service.AsyncConsistentMap;

import static org.onosproject.store.primitives.impl.Transaction.State.COMMITTED;
import static org.onosproject.store.primitives.impl.Transaction.State.COMMITTING;
import static org.onosproject.store.primitives.impl.Transaction.State.ROLLEDBACK;
import static org.onosproject.store.primitives.impl.Transaction.State.ROLLINGBACK;

/**
 * Agent that runs the two phase commit protocol.
 */
public class TransactionManager {

    private final Database database;
    private final AsyncConsistentMap<TransactionId, Transaction> transactions;

    public TransactionManager(Database database, AsyncConsistentMap<TransactionId, Transaction> transactions) {
        this.database = checkNotNull(database, "database cannot be null");
        this.transactions = transactions;
    }

    /**
     * Executes the specified transaction by employing a two phase commit protocol.
     *
     * @param transaction transaction to commit
     * @return transaction commit result
     */
    public CompletableFuture<CommitResult> execute(Transaction transaction) {
        // short-circuit if there is only a single update
        if (transaction.updates().size() <= 1) {
            return database.prepareAndCommit(transaction)
                    .thenApply(response -> response.success()
                                   ? CommitResult.OK : CommitResult.FAILURE_DURING_COMMIT);
        }
        // clean up if this transaction in already in a terminal state.
        if (transaction.state() == COMMITTED || transaction.state() == ROLLEDBACK) {
            return transactions.remove(transaction.id()).thenApply(v -> CommitResult.OK);
        } else if (transaction.state() == COMMITTING) {
            return commit(transaction);
        } else if (transaction.state() == ROLLINGBACK) {
            return rollback(transaction).thenApply(v -> CommitResult.FAILURE_TO_PREPARE);
        } else {
            return prepare(transaction).thenCompose(v -> v ? commit(transaction) : rollback(transaction));
        }
    }

    /**
     * Returns all pending transaction identifiers.
     *
     * @return future for a collection of transaction identifiers.
     */
    public CompletableFuture<Collection<TransactionId>> getPendingTransactionIds() {
        return transactions.values().thenApply(c -> c.stream()
                    .map(v -> v.value())
                    .filter(v -> v.state() != COMMITTED && v.state() != ROLLEDBACK)
                    .map(Transaction::id)
                    .collect(Collectors.toList()));
    }

    private CompletableFuture<Boolean> prepare(Transaction transaction) {
        return transactions.put(transaction.id(), transaction)
                .thenCompose(v -> database.prepare(transaction))
                .thenCompose(status -> transactions.put(
                            transaction.id(),
                            transaction.transition(status ? COMMITTING : ROLLINGBACK))
                .thenApply(v -> status));
    }

    private CompletableFuture<CommitResult> commit(Transaction transaction) {
        return database.commit(transaction)
                       .thenCompose(r -> {
                           if (r.success()) {
                               return transactions.put(transaction.id(), transaction.transition(COMMITTED))
                                                  .thenApply(v -> CommitResult.OK);
                           } else {
                               return CompletableFuture.completedFuture(CommitResult.FAILURE_DURING_COMMIT);
                           }
                       });
    }

    private CompletableFuture<CommitResult> rollback(Transaction transaction) {
        return database.rollback(transaction)
                       .thenCompose(v -> transactions.put(transaction.id(), transaction.transition(ROLLEDBACK)))
                       .thenApply(v -> CommitResult.FAILURE_TO_PREPARE);
    }
}
