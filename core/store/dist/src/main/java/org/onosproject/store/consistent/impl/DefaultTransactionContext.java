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

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.google.common.base.Preconditions.*;

import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.TransactionContext;
import org.onosproject.store.service.TransactionException;
import org.onosproject.store.service.TransactionalMap;
import org.onosproject.store.service.UpdateOperation;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Default TransactionContext implementation.
 */
public class DefaultTransactionContext implements TransactionContext {

    private final Map<String, DefaultTransactionalMap> txMaps = Maps.newHashMap();
    private boolean isOpen = false;
    DatabaseProxy<String, byte[]> databaseProxy;
    private static final String TX_NOT_OPEN_ERROR = "Transaction is not open";
    private static final int TRANSACTION_TIMEOUT_MILLIS = 2000;

    DefaultTransactionContext(DatabaseProxy<String, byte[]> proxy) {
        this.databaseProxy = proxy;
    }

    @Override
    public void begin() {
        isOpen = true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <K, V> TransactionalMap<K, V> createTransactionalMap(String mapName,
            Serializer serializer) {
        checkNotNull(mapName, "map name is null");
        checkNotNull(serializer, "serializer is null");
        checkState(isOpen, TX_NOT_OPEN_ERROR);
        if (!txMaps.containsKey(mapName)) {
            ConsistentMap<K, V> backingMap = new DefaultConsistentMap<>(mapName, databaseProxy, serializer);
            DefaultTransactionalMap<K, V> txMap = new DefaultTransactionalMap<>(mapName, backingMap, this, serializer);
            txMaps.put(mapName, txMap);
        }
        return txMaps.get(mapName);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void commit() {
        checkState(isOpen, TX_NOT_OPEN_ERROR);
        List<UpdateOperation<String, byte[]>> allUpdates =
                Lists.newLinkedList();
        try {
            txMaps.values()
                .stream()
                .forEach(m -> {
                    allUpdates.addAll(m.prepareDatabaseUpdates());
                });

            if (!complete(databaseProxy.atomicBatchUpdate(allUpdates))) {
                throw new TransactionException.OptimisticConcurrencyFailure();
            }
        } finally {
            isOpen = false;
        }
    }

    @Override
    public void rollback() {
        checkState(isOpen, TX_NOT_OPEN_ERROR);
        txMaps.values()
        .stream()
        .forEach(m -> m.rollback());
    }

    @Override
    public boolean isOpen() {
        return false;
    }

    private static <T> T complete(CompletableFuture<T> future) {
        try {
            return future.get(TRANSACTION_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TransactionException.Interrupted();
        } catch (TimeoutException e) {
            throw new TransactionException.Timeout();
        } catch (ExecutionException e) {
            throw new TransactionException(e.getCause());
        }
    }
}
