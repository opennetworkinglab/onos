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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import net.kuujo.copycat.Task;
import net.kuujo.copycat.cluster.Cluster;
import net.kuujo.copycat.resource.ResourceState;
import org.onosproject.store.service.DatabaseUpdate;
import org.onosproject.store.service.Transaction;
import org.onosproject.store.service.Versioned;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkState;

/**
 * A database that partitions the keys across one or more database partitions.
 */
public class PartitionedDatabase implements Database {

    private final String name;
    private final Partitioner<String> partitioner;
    private final List<Database> partitions;
    private final AtomicBoolean isOpen = new AtomicBoolean(false);
    private static final String DB_NOT_OPEN = "Partitioned Database is not open";
    private TransactionManager transactionManager;

    public PartitionedDatabase(
            String name,
            Collection<Database> partitions) {
        this.name = name;
        this.partitions = partitions
                .stream()
                .sorted((db1, db2) -> db1.name().compareTo(db2.name()))
                .collect(Collectors.toList());
        this.partitioner = new SimpleKeyHashPartitioner(this.partitions);
    }

    /**
     * Returns the databases for individual partitions.
     * @return list of database partitions
     */
    public List<Database> getPartitions() {
        return partitions;
    }

    /**
     * Returns true if the database is open.
     * @return true if open, false otherwise
     */
    @Override
    public boolean isOpen() {
        return isOpen.get();
    }

    @Override
    public CompletableFuture<Set<String>> maps() {
        checkState(isOpen.get(), DB_NOT_OPEN);
        Set<String> mapNames = Sets.newConcurrentHashSet();
        return CompletableFuture.allOf(partitions
                .stream()
                .map(db -> db.maps().thenApply(mapNames::addAll))
                .toArray(CompletableFuture[]::new))
            .thenApply(v -> mapNames);
    }

    @Override
    public CompletableFuture<Map<String, Long>> counters() {
        checkState(isOpen.get(), DB_NOT_OPEN);
        Map<String, Long> counters = Maps.newConcurrentMap();
        return CompletableFuture.allOf(partitions
                .stream()
                .map(db -> db.counters()
                        .thenApply(m -> {
                            counters.putAll(m);
                            return null;
                        }))
                .toArray(CompletableFuture[]::new))
            .thenApply(v -> counters);
    }

    @Override
    public CompletableFuture<Integer> mapSize(String mapName) {
        checkState(isOpen.get(), DB_NOT_OPEN);
        AtomicInteger totalSize = new AtomicInteger(0);
        return CompletableFuture.allOf(partitions
                                               .stream()
                                               .map(p -> p.mapSize(mapName).thenApply(totalSize::addAndGet))
                                               .toArray(CompletableFuture[]::new))
                .thenApply(v -> totalSize.get());
    }

    @Override
    public CompletableFuture<Boolean> mapIsEmpty(String mapName) {
        checkState(isOpen.get(), DB_NOT_OPEN);
        return mapSize(mapName).thenApply(size -> size == 0);
    }

    @Override
    public CompletableFuture<Boolean> mapContainsKey(String mapName, String key) {
        checkState(isOpen.get(), DB_NOT_OPEN);
        return partitioner.getPartition(mapName, key).mapContainsKey(mapName, key);
    }

    @Override
    public CompletableFuture<Boolean> mapContainsValue(String mapName, byte[] value) {
        checkState(isOpen.get(), DB_NOT_OPEN);
        AtomicBoolean containsValue = new AtomicBoolean(false);
        return CompletableFuture.allOf(partitions
                                               .stream()
                                               .map(p -> p.mapContainsValue(mapName, value)
                                                       .thenApply(v -> containsValue.compareAndSet(false, v)))
                                               .toArray(CompletableFuture[]::new))
                .thenApply(v -> containsValue.get());
    }

    @Override
    public CompletableFuture<Versioned<byte[]>> mapGet(String mapName, String key) {
        checkState(isOpen.get(), DB_NOT_OPEN);
        return partitioner.getPartition(mapName, key).mapGet(mapName, key);
    }

    @Override
    public CompletableFuture<Result<UpdateResult<String, byte[]>>> mapUpdate(
            String mapName, String key, Match<byte[]> valueMatch,
            Match<Long> versionMatch, byte[] value) {
        return partitioner.getPartition(mapName, key).mapUpdate(mapName, key, valueMatch, versionMatch, value);

    }

    @Override
    public CompletableFuture<Result<Void>> mapClear(String mapName) {
        AtomicBoolean isLocked = new AtomicBoolean(false);
        checkState(isOpen.get(), DB_NOT_OPEN);
        return CompletableFuture.allOf(partitions
                    .stream()
                    .map(p -> p.mapClear(mapName)
                            .thenApply(v -> isLocked.compareAndSet(false, Result.Status.LOCKED == v.status())))
                    .toArray(CompletableFuture[]::new))
                .thenApply(v -> isLocked.get() ? Result.locked() : Result.ok(null));
    }

    @Override
    public CompletableFuture<Set<String>> mapKeySet(String mapName) {
        checkState(isOpen.get(), DB_NOT_OPEN);
        Set<String> keySet = Sets.newConcurrentHashSet();
        return CompletableFuture.allOf(partitions
                    .stream()
                    .map(p -> p.mapKeySet(mapName).thenApply(keySet::addAll))
                    .toArray(CompletableFuture[]::new))
                .thenApply(v -> keySet);
    }

    @Override
    public CompletableFuture<Collection<Versioned<byte[]>>> mapValues(String mapName) {
        checkState(isOpen.get(), DB_NOT_OPEN);
        List<Versioned<byte[]>> values = new CopyOnWriteArrayList<>();
        return CompletableFuture.allOf(partitions
                    .stream()
                    .map(p -> p.mapValues(mapName).thenApply(values::addAll))
                    .toArray(CompletableFuture[]::new))
                .thenApply(v -> values);
    }

    @Override
    public CompletableFuture<Set<Entry<String, Versioned<byte[]>>>> mapEntrySet(String mapName) {
        checkState(isOpen.get(), DB_NOT_OPEN);
        Set<Entry<String, Versioned<byte[]>>> entrySet = Sets.newConcurrentHashSet();
        return CompletableFuture.allOf(partitions
                                               .stream()
                                               .map(p -> p.mapEntrySet(mapName).thenApply(entrySet::addAll))
                                               .toArray(CompletableFuture[]::new))
                .thenApply(v -> entrySet);
    }

    @Override
    public CompletableFuture<Long> counterGet(String counterName) {
        checkState(isOpen.get(), DB_NOT_OPEN);
        return partitioner.getPartition(counterName, counterName).counterGet(counterName);
    }

    @Override
    public CompletableFuture<Long> counterAddAndGet(String counterName, long delta) {
        checkState(isOpen.get(), DB_NOT_OPEN);
        return partitioner.getPartition(counterName, counterName).counterAddAndGet(counterName, delta);
    }

    @Override
    public CompletableFuture<Long> counterGetAndAdd(String counterName, long delta) {
        checkState(isOpen.get(), DB_NOT_OPEN);
        return partitioner.getPartition(counterName, counterName).counterGetAndAdd(counterName, delta);
    }

    @Override
    public CompletableFuture<Void> counterSet(String counterName, long value) {
        checkState(isOpen.get(), DB_NOT_OPEN);
        return partitioner.getPartition(counterName, counterName).counterSet(counterName, value);
    }

    @Override
    public CompletableFuture<Boolean> counterCompareAndSet(String counterName, long expectedValue, long updateValue) {
        checkState(isOpen.get(), DB_NOT_OPEN);
        return partitioner.getPartition(counterName, counterName).
                counterCompareAndSet(counterName, expectedValue, updateValue);

    }

    @Override
    public CompletableFuture<Long> queueSize(String queueName) {
        checkState(isOpen.get(), DB_NOT_OPEN);
        return partitioner.getPartition(queueName, queueName).queueSize(queueName);
    }

    @Override
    public CompletableFuture<Void> queuePush(String queueName, byte[] entry) {
        checkState(isOpen.get(), DB_NOT_OPEN);
        return partitioner.getPartition(queueName, queueName).queuePush(queueName, entry);
    }

    @Override
    public CompletableFuture<byte[]> queuePop(String queueName) {
        checkState(isOpen.get(), DB_NOT_OPEN);
        return partitioner.getPartition(queueName, queueName).queuePop(queueName);
    }

    @Override
    public CompletableFuture<byte[]> queuePeek(String queueName) {
        checkState(isOpen.get(), DB_NOT_OPEN);
        return partitioner.getPartition(queueName, queueName).queuePeek(queueName);
    }

    @Override
    public CompletableFuture<CommitResponse> prepareAndCommit(Transaction transaction) {
        Map<Database, Transaction> subTransactions = createSubTransactions(transaction);
        if (subTransactions.isEmpty()) {
            return CompletableFuture.completedFuture(CommitResponse.success(ImmutableList.of()));
        } else if (subTransactions.size() == 1) {
            Entry<Database, Transaction> entry =
                    subTransactions.entrySet().iterator().next();
            return entry.getKey().prepareAndCommit(entry.getValue());
        } else {
            if (transactionManager == null) {
                throw new IllegalStateException("TransactionManager is not initialized");
            }
            return transactionManager.execute(transaction);
        }
    }

    @Override
    public CompletableFuture<Boolean> prepare(Transaction transaction) {
        Map<Database, Transaction> subTransactions = createSubTransactions(transaction);
        AtomicBoolean status = new AtomicBoolean(true);
        return CompletableFuture.allOf(subTransactions.entrySet()
                .stream()
                                               .map(entry -> entry
                                                       .getKey()
                        .prepare(entry.getValue())
                        .thenApply(v -> status.compareAndSet(true, v)))
                .toArray(CompletableFuture[]::new))
            .thenApply(v -> status.get());
    }

    @Override
    public CompletableFuture<CommitResponse> commit(Transaction transaction) {
        Map<Database, Transaction> subTransactions = createSubTransactions(transaction);
        AtomicBoolean success = new AtomicBoolean(true);
        List<UpdateResult<String, byte[]>> allUpdates = Lists.newArrayList();
        return CompletableFuture.allOf(subTransactions.entrySet()
                                               .stream()
                                               .map(entry -> entry.getKey().commit(entry.getValue())
                                                       .thenAccept(response -> {
                                                           success.set(success.get() && response.success());
                                                           if (success.get()) {
                                                               allUpdates.addAll(response.updates());
                                                           }
                                                       }))
                                               .toArray(CompletableFuture[]::new))
                               .thenApply(v -> success.get() ?
                                       CommitResponse.success(allUpdates) : CommitResponse.failure());
    }

    @Override
    public CompletableFuture<Boolean> rollback(Transaction transaction) {
        Map<Database, Transaction> subTransactions = createSubTransactions(transaction);
        return CompletableFuture.allOf(subTransactions.entrySet()
                .stream()
                .map(entry -> entry.getKey().rollback(entry.getValue()))
                                               .toArray(CompletableFuture[]::new))
            .thenApply(v -> true);
    }

    @Override
    public CompletableFuture<Database> open() {
        return CompletableFuture.allOf(partitions
                    .stream()
                    .map(Database::open)
                    .toArray(CompletableFuture[]::new))
                .thenApply(v -> {
                    isOpen.set(true);
                    return this;
                });
    }

    @Override
    public CompletableFuture<Void> close() {
        checkState(isOpen.get(), DB_NOT_OPEN);
        return CompletableFuture.allOf(partitions
                .stream()
                .map(database -> database.close())
                .toArray(CompletableFuture[]::new));
    }

    @Override
    public boolean isClosed() {
        return !isOpen.get();
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Cluster cluster() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Database addStartupTask(Task<CompletableFuture<Void>> task) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Database addShutdownTask(Task<CompletableFuture<Void>> task) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ResourceState state() {
        throw new UnsupportedOperationException();
    }

    private Map<Database, Transaction> createSubTransactions(
            Transaction transaction) {
        Map<Database, List<DatabaseUpdate>> perPartitionUpdates = Maps.newHashMap();
        for (DatabaseUpdate update : transaction.updates()) {
            Database partition = partitioner.getPartition(update.mapName(), update.key());
            List<DatabaseUpdate> partitionUpdates =
                    perPartitionUpdates.computeIfAbsent(partition, k -> Lists.newLinkedList());
            partitionUpdates.add(update);
        }
        Map<Database, Transaction> subTransactions = Maps.newHashMap();
        perPartitionUpdates.forEach((k, v) -> subTransactions.put(k, new DefaultTransaction(transaction.id(), v)));
        return subTransactions;
    }

    protected void setTransactionManager(TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    @Override
    public void registerConsumer(Consumer<StateMachineUpdate> consumer) {
        partitions.forEach(p -> p.registerConsumer(consumer));
    }

    @Override
    public void unregisterConsumer(Consumer<StateMachineUpdate> consumer) {
        partitions.forEach(p -> p.unregisterConsumer(consumer));
    }
}

