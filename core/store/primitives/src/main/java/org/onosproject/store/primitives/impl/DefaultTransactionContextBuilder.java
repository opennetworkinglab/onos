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

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

import org.onosproject.store.primitives.TransactionId;
import org.onosproject.store.primitives.resources.impl.CommitResult;
import org.onosproject.store.service.ConsistentMapBuilder;
import org.onosproject.store.service.TransactionContext;
import org.onosproject.store.service.TransactionContextBuilder;

/**
 * The default implementation of a transaction context builder. This builder
 * generates a {@link DefaultTransactionContext}.
 */
public class DefaultTransactionContextBuilder extends TransactionContextBuilder {

    private final Supplier<ConsistentMapBuilder> mapBuilderSupplier;
    private final Function<Transaction, CompletableFuture<CommitResult>> transactionCommitter;
    private final TransactionId transactionId;

    public DefaultTransactionContextBuilder(Supplier<ConsistentMapBuilder> mapBuilderSupplier,
            Function<Transaction, CompletableFuture<CommitResult>> transactionCommiter,
            TransactionId transactionId) {
        this.mapBuilderSupplier = mapBuilderSupplier;
        this.transactionCommitter = transactionCommiter;
        this.transactionId = transactionId;
    }

    @Override
    public TransactionContext build() {
        return new DefaultTransactionContext(transactionId, transactionCommitter, () -> {
            ConsistentMapBuilder mapBuilder = mapBuilderSupplier.get();
            if (partitionsDisabled()) {
                mapBuilder = (ConsistentMapBuilder) mapBuilder.withPartitionsDisabled();
            }
            return mapBuilder;
        });
    }
}
