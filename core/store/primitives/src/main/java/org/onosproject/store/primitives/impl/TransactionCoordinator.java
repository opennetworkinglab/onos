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

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.onlab.util.Tools;
import org.onosproject.store.primitives.TransactionId;
import org.onosproject.store.service.AsyncConsistentMap;

/**
 * Coordinator for a two-phase commit protocol.
 */
public class TransactionCoordinator {

    private final AsyncConsistentMap<TransactionId, Transaction.State> transactions;

    public TransactionCoordinator(AsyncConsistentMap<TransactionId, Transaction.State> transactions) {
        this.transactions = transactions;
    }

    /**
     * Commits a transaction.
     *
     * @param transactionId           transaction
     * @param transactionParticipants set of transaction participants
     * @return future for commit result
     */
    CompletableFuture<Void> commit(TransactionId transactionId, Set<TransactionParticipant> transactionParticipants) {
        if (!transactionParticipants.stream().anyMatch(t -> t.hasPendingUpdates())) {
            return CompletableFuture.completedFuture(null);
        }

       return  transactions.put(transactionId, Transaction.State.PREPARING)
                    .thenCompose(v -> this.doPrepare(transactionParticipants))
                    .thenCompose(result -> result
                           ? transactions.put(transactionId, Transaction.State.COMMITTING)
                                         .thenCompose(v -> doCommit(transactionParticipants))
                                         .thenApply(v -> null)
                           : transactions.put(transactionId, Transaction.State.ROLLINGBACK)
                                         .thenCompose(v -> doRollback(transactionParticipants))
                                         .thenApply(v -> null))
                    .thenCompose(v -> transactions.remove(transactionId))
                    .thenApply(v -> null);
    }

    private CompletableFuture<Boolean> doPrepare(Set<TransactionParticipant> transactionParticipants) {
        return Tools.allOf(transactionParticipants
                                           .stream()
                                           .map(TransactionParticipant::prepare)
                                           .collect(Collectors.toList()))
                    .thenApply(list -> list.stream().reduce(Boolean::logicalAnd).orElse(true));
    }

    private CompletableFuture<Void> doCommit(Set<TransactionParticipant> transactionParticipants) {
        return CompletableFuture.allOf(transactionParticipants.stream()
                                                              .map(p -> p.commit())
                                                              .toArray(CompletableFuture[]::new));
    }

    private CompletableFuture<Void> doRollback(Set<TransactionParticipant> transactionParticipants) {
        return CompletableFuture.allOf(transactionParticipants.stream()
                                                              .map(p -> p.rollback())
                                                              .toArray(CompletableFuture[]::new));
    }
}