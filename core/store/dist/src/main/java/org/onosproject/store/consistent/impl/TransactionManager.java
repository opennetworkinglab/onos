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
package org.onosproject.store.consistent.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.onlab.util.KryoNamespace;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.AsyncConsistentMap;
import org.onosproject.store.service.ConsistentMapBuilder;
import org.onosproject.store.service.DatabaseUpdate;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.Transaction;
import org.onosproject.store.service.Versioned;
import org.onosproject.store.service.Transaction.State;

import com.google.common.collect.ImmutableList;

/**
 * Agent that runs the two phase commit protocol.
 */
public class TransactionManager {

    private static final KryoNamespace KRYO_NAMESPACE = KryoNamespace.newBuilder()
            .register(KryoNamespaces.BASIC)
            .nextId(KryoNamespace.FLOATING_ID)
            .register(Versioned.class)
            .register(DatabaseUpdate.class)
            .register(DatabaseUpdate.Type.class)
            .register(DefaultTransaction.class)
            .register(Transaction.State.class)
            .build();

    private final Serializer serializer = Serializer.using(Arrays.asList(KRYO_NAMESPACE));
    private final Database database;
    private final AsyncConsistentMap<Long, Transaction> transactions;

    /**
     * Constructs a new TransactionManager for the specified database instance.
     *
     * @param database database
     * @param mapBuilder builder for ConsistentMap instances
     */
    public TransactionManager(Database database, ConsistentMapBuilder<Long, Transaction> mapBuilder) {
        this.database = checkNotNull(database, "database cannot be null");
        this.transactions = mapBuilder.withName("onos-transactions")
                                      .withSerializer(serializer)
                                      .buildAsyncMap();
    }

    /**
     * Executes the specified transaction by employing a two phase commit protocol.
     *
     * @param transaction transaction to commit
     * @return transaction result. Result value true indicates a successful commit, false
     * indicates abort
     */
    public CompletableFuture<CommitResponse> execute(Transaction transaction) {
        // clean up if this transaction in already in a terminal state.
        if (transaction.state() == Transaction.State.COMMITTED ||
                transaction.state() == Transaction.State.ROLLEDBACK) {
            return transactions.remove(transaction.id()).thenApply(v -> CommitResponse.success(ImmutableList.of()));
        } else if (transaction.state() == Transaction.State.COMMITTING) {
            return commit(transaction);
        } else if (transaction.state() == Transaction.State.ROLLINGBACK) {
            return rollback(transaction).thenApply(v -> CommitResponse.success(ImmutableList.of()));
        } else {
            return prepare(transaction).thenCompose(v -> v ? commit(transaction) : rollback(transaction));
        }
    }


    /**
     * Returns all transactions in the system.
     *
     * @return future for a collection of transactions
     */
    public CompletableFuture<Collection<Transaction>> getTransactions() {
        return transactions.values().thenApply(c -> {
            Collection<Transaction> txns = c.stream().map(v -> v.value()).collect(Collectors.toList());
            return txns;
        });
    }

    private CompletableFuture<Boolean> prepare(Transaction transaction) {
        return transactions.put(transaction.id(), transaction)
                .thenCompose(v -> database.prepare(transaction))
                .thenCompose(status -> transactions.put(
                            transaction.id(),
                            transaction.transition(status ? State.COMMITTING : State.ROLLINGBACK))
                .thenApply(v -> status));
    }

    private CompletableFuture<CommitResponse> commit(Transaction transaction) {
        return database.commit(transaction)
                .whenComplete((r, e) -> transactions.put(
                            transaction.id(),
                            transaction.transition(Transaction.State.COMMITTED)));
    }

    private CompletableFuture<CommitResponse> rollback(Transaction transaction) {
        return database.rollback(transaction)
                .thenCompose(v -> transactions.put(
                            transaction.id(),
                            transaction.transition(Transaction.State.ROLLEDBACK)))
                .thenApply(v -> CommitResponse.failure());
    }
}
