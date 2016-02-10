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

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.*;

import org.onosproject.store.primitives.MapUpdate;
import org.onosproject.store.primitives.TransactionId;
import org.onosproject.store.primitives.resources.impl.CommitResult;
import org.onosproject.store.service.ConsistentMapBuilder;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.TransactionContext;
import org.onosproject.store.service.TransactionalMap;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Futures;

/**
 * Default TransactionContext implementation.
 */
public class DefaultTransactionContext implements TransactionContext {
    private static final String TX_NOT_OPEN_ERROR = "Transaction Context is not open";

    @SuppressWarnings("rawtypes")
    private final Map<String, DefaultTransactionalMap> txMaps = Maps.newConcurrentMap();
    private boolean isOpen = false;
    private final Function<Transaction, CompletableFuture<CommitResult>> transactionCommitter;
    private final TransactionId transactionId;
    private final Supplier<ConsistentMapBuilder> mapBuilderSupplier;

    public DefaultTransactionContext(TransactionId transactionId,
            Function<Transaction, CompletableFuture<CommitResult>> transactionCommitter,
            Supplier<ConsistentMapBuilder> mapBuilderSupplier) {
        this.transactionId = transactionId;
        this.transactionCommitter = checkNotNull(transactionCommitter);
        this.mapBuilderSupplier = checkNotNull(mapBuilderSupplier);
    }

    @Override
    public TransactionId transactionId() {
        return transactionId;
    }

    @Override
    public void begin() {
        checkState(!isOpen, "Transaction Context is already open");
        isOpen = true;
    }

    @Override
    public boolean isOpen() {
        return isOpen;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <K, V> TransactionalMap<K, V> getTransactionalMap(String mapName,
            Serializer serializer) {
        checkState(isOpen, TX_NOT_OPEN_ERROR);
        checkNotNull(mapName);
        checkNotNull(serializer);
        return txMaps.computeIfAbsent(mapName, name -> {
            ConsistentMapBuilder mapBuilder =  (ConsistentMapBuilder) mapBuilderSupplier.get()
                                                                                        .withName(name)
                                                                                        .withSerializer(serializer);
            return new DefaultTransactionalMap<>(
                                name,
                                mapBuilder.buildAsyncMap(),
                                this,
                                serializer);
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean commit() {
        // TODO: rework commit implementation to be more intuitive
        checkState(isOpen, TX_NOT_OPEN_ERROR);
        CommitResult result = null;
        try {
            List<MapUpdate<String, byte[]>> updates = Lists.newLinkedList();
            txMaps.values().forEach(m -> updates.addAll(m.toMapUpdates()));
            Transaction transaction = new Transaction(transactionId, updates);
            result = Futures.getUnchecked(transactionCommitter.apply(transaction));
            return result == CommitResult.OK;
        } catch (Exception e) {
            abort();
            return false;
        } finally {
            isOpen = false;
        }
    }

    @Override
    public void abort() {
        if (isOpen) {
            try {
                txMaps.values().forEach(m -> m.abort());
            } finally {
                isOpen = false;
            }
        }
    }

    @Override
    public String toString() {
        ToStringHelper s = MoreObjects.toStringHelper(this)
             .add("transactionId", transactionId)
             .add("isOpen", isOpen);

        txMaps.entrySet().forEach(e -> {
            s.add(e.getKey(), e.getValue());
        });
        return s.toString();
    }

    @Override
    public String name() {
        return transactionId.toString();
    }
}
