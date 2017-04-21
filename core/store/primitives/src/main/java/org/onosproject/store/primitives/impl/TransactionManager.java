/*
 * Copyright 2017-present Open Networking Laboratory
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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.hash.Hashing;
import com.google.common.util.concurrent.Futures;
import org.onosproject.cluster.PartitionId;
import org.onosproject.store.primitives.MapUpdate;
import org.onosproject.store.primitives.PartitionService;
import org.onosproject.store.primitives.TransactionId;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.AsyncConsistentMap;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.TransactionException;

/**
 * Transaction manager for managing state shared across multiple transactions.
 */
public class TransactionManager {
    private static final int DEFAULT_CACHE_SIZE = 100;

    private final PartitionService partitionService;
    private final List<PartitionId> sortedPartitions;
    private final AsyncConsistentMap<TransactionId, Transaction.State> transactions;
    private final int cacheSize;
    private final Map<PartitionId, Cache<String, AsyncConsistentMap>> partitionCache = Maps.newConcurrentMap();

    public TransactionManager(StorageService storageService, PartitionService partitionService) {
        this(storageService, partitionService, DEFAULT_CACHE_SIZE);
    }

    public TransactionManager(StorageService storageService, PartitionService partitionService, int cacheSize) {
        this.partitionService = partitionService;
        this.cacheSize = cacheSize;
        this.transactions = storageService.<TransactionId, Transaction.State>consistentMapBuilder()
                .withName("onos-transactions")
                .withSerializer(Serializer.using(KryoNamespaces.API,
                        Transaction.class,
                        Transaction.State.class))
                .buildAsyncMap();
        this.sortedPartitions = Lists.newArrayList(partitionService.getAllPartitionIds());
        Collections.sort(sortedPartitions);
    }

    /**
     * Returns the collection of currently pending transactions.
     *
     * @return a collection of currently pending transactions
     */
    public Collection<TransactionId> getPendingTransactions() {
        return Futures.getUnchecked(transactions.keySet());
    }

    /**
     * Returns a partitioned transactional map for use within a transaction context.
     * <p>
     * The transaction coordinator will return a map that takes advantage of caching that's shared across transaction
     * contexts.
     *
     * @param name the map name
     * @param serializer the map serializer
     * @param transactionCoordinator the transaction coordinator for which the map is being created
     * @param <K> key type
     * @param <V> value type
     * @return a partitioned transactional map
     */
    <K, V> PartitionedTransactionalMap<K, V> getTransactionalMap(
            String name,
            Serializer serializer,
            TransactionCoordinator transactionCoordinator) {
        Map<PartitionId, TransactionalMapParticipant<K, V>> partitions = new HashMap<>();
        for (PartitionId partitionId : partitionService.getAllPartitionIds()) {
            partitions.put(partitionId, getTransactionalMapPartition(
                    name, partitionId, serializer, transactionCoordinator));
        }

        Hasher<K> hasher = key -> {
            int hashCode = Hashing.sha256().hashBytes(serializer.encode(key)).asInt();
            return sortedPartitions.get(Math.abs(hashCode) % sortedPartitions.size());
        };
        return new PartitionedTransactionalMap<>(partitions, hasher);
    }

    @SuppressWarnings("unchecked")
    private <K, V> TransactionalMapParticipant<K, V> getTransactionalMapPartition(
            String mapName,
            PartitionId partitionId,
            Serializer serializer,
            TransactionCoordinator transactionCoordinator) {
        Cache<String, AsyncConsistentMap> mapCache = partitionCache.computeIfAbsent(partitionId, p ->
                CacheBuilder.newBuilder().maximumSize(cacheSize / partitionService.getNumberOfPartitions()).build());
        try {
            AsyncConsistentMap<K, V> baseMap = partitionService.getDistributedPrimitiveCreator(partitionId)
                            .newAsyncConsistentMap(mapName, serializer);
            AsyncConsistentMap<K, V> asyncMap = mapCache.get(mapName, () ->
                    DistributedPrimitives.newCachingMap(baseMap));

            Transaction<MapUpdate<K, V>> transaction = new Transaction<>(
                    transactionCoordinator.transactionId,
                    baseMap);
            return new DefaultTransactionalMapParticipant<>(asyncMap.asConsistentMap(), transaction);
        } catch (ExecutionException e) {
            throw new TransactionException(e);
        }
    }

    /**
     * Updates the state of a transaction in the transaction registry.
     *
     * @param transactionId the transaction identifier
     * @param state the state of the transaction
     * @return a completable future to be completed once the transaction state has been updated in the registry
     */
    CompletableFuture<Void> updateState(TransactionId transactionId, Transaction.State state) {
        return transactions.put(transactionId, state).thenApply(v -> null);
    }

    /**
     * Removes the given transaction from the transaction registry.
     *
     * @param transactionId the transaction identifier
     * @return a completable future to be completed once the transaction state has been removed from the registry
     */
    CompletableFuture<Void> remove(TransactionId transactionId) {
        return transactions.remove(transactionId).thenApply(v -> null);
    }
}