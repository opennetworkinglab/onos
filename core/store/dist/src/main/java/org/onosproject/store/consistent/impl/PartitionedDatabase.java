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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.onosproject.cluster.NodeId;
import org.onosproject.store.service.DatabaseUpdate;
import org.onosproject.store.service.Transaction;
import org.onosproject.store.service.Versioned;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import net.kuujo.copycat.Task;
import net.kuujo.copycat.cluster.Cluster;
import net.kuujo.copycat.resource.ResourceState;
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
    public CompletableFuture<Set<String>> tableNames() {
        checkState(isOpen.get(), DB_NOT_OPEN);
        Set<String> tableNames = Sets.newConcurrentHashSet();
        return CompletableFuture.allOf(partitions
                .stream()
                .map(db -> db.tableNames().thenApply(tableNames::addAll))
                .toArray(CompletableFuture[]::new))
            .thenApply(v -> tableNames);
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
    public CompletableFuture<Integer> size(String tableName) {
        checkState(isOpen.get(), DB_NOT_OPEN);
        AtomicInteger totalSize = new AtomicInteger(0);
        return CompletableFuture.allOf(partitions
                    .stream()
                    .map(p -> p.size(tableName).thenApply(totalSize::addAndGet))
                    .toArray(CompletableFuture[]::new))
                .thenApply(v -> totalSize.get());
    }

    @Override
    public CompletableFuture<Boolean> isEmpty(String tableName) {
        checkState(isOpen.get(), DB_NOT_OPEN);
        return size(tableName).thenApply(size -> size == 0);
    }

    @Override
    public CompletableFuture<Boolean> containsKey(String tableName, String key) {
        checkState(isOpen.get(), DB_NOT_OPEN);
        return partitioner.getPartition(tableName, key).containsKey(tableName, key);
    }

    @Override
    public CompletableFuture<Boolean> containsValue(String tableName, byte[] value) {
        checkState(isOpen.get(), DB_NOT_OPEN);
        AtomicBoolean containsValue = new AtomicBoolean(false);
        return CompletableFuture.allOf(partitions
                    .stream()
                    .map(p -> p.containsValue(tableName, value).thenApply(v -> containsValue.compareAndSet(false, v)))
                    .toArray(CompletableFuture[]::new))
                .thenApply(v -> containsValue.get());
    }

    @Override
    public CompletableFuture<Versioned<byte[]>> get(String tableName, String key) {
        checkState(isOpen.get(), DB_NOT_OPEN);
        return partitioner.getPartition(tableName, key).get(tableName, key);
    }

    @Override
    public CompletableFuture<Result<Versioned<byte[]>>> put(String tableName, String key, byte[] value) {
        checkState(isOpen.get(), DB_NOT_OPEN);
        return partitioner.getPartition(tableName, key).put(tableName, key, value);
    }

    @Override
    public CompletableFuture<Result<UpdateResult<Versioned<byte[]>>>> putAndGet(String tableName,
            String key,
            byte[] value) {
        checkState(isOpen.get(), DB_NOT_OPEN);
        return partitioner.getPartition(tableName, key).putAndGet(tableName, key, value);
    }

    @Override
    public CompletableFuture<Result<UpdateResult<Versioned<byte[]>>>> putIfAbsentAndGet(String tableName,
            String key,
            byte[] value) {
        checkState(isOpen.get(), DB_NOT_OPEN);
        return partitioner.getPartition(tableName, key).putIfAbsentAndGet(tableName, key, value);
    }

    @Override
    public CompletableFuture<Result<Versioned<byte[]>>> remove(String tableName, String key) {
        checkState(isOpen.get(), DB_NOT_OPEN);
        return partitioner.getPartition(tableName, key).remove(tableName, key);
    }

    @Override
    public CompletableFuture<Result<Void>> clear(String tableName) {
        AtomicBoolean isLocked = new AtomicBoolean(false);
        checkState(isOpen.get(), DB_NOT_OPEN);
        return CompletableFuture.allOf(partitions
                    .stream()
                    .map(p -> p.clear(tableName)
                            .thenApply(v -> isLocked.compareAndSet(false, Result.Status.LOCKED == v.status())))
                    .toArray(CompletableFuture[]::new))
                .thenApply(v -> isLocked.get() ? Result.locked() : Result.ok(null));
    }

    @Override
    public CompletableFuture<Set<String>> keySet(String tableName) {
        checkState(isOpen.get(), DB_NOT_OPEN);
        Set<String> keySet = Sets.newConcurrentHashSet();
        return CompletableFuture.allOf(partitions
                    .stream()
                    .map(p -> p.keySet(tableName).thenApply(keySet::addAll))
                    .toArray(CompletableFuture[]::new))
                .thenApply(v -> keySet);
    }

    @Override
    public CompletableFuture<Collection<Versioned<byte[]>>> values(String tableName) {
        checkState(isOpen.get(), DB_NOT_OPEN);
        List<Versioned<byte[]>> values = new CopyOnWriteArrayList<>();
        return CompletableFuture.allOf(partitions
                    .stream()
                    .map(p -> p.values(tableName).thenApply(values::addAll))
                    .toArray(CompletableFuture[]::new))
                .thenApply(v -> values);
    }

    @Override
    public CompletableFuture<Set<Entry<String, Versioned<byte[]>>>> entrySet(String tableName) {
        checkState(isOpen.get(), DB_NOT_OPEN);
        Set<Entry<String, Versioned<byte[]>>> entrySet = Sets.newConcurrentHashSet();
        return CompletableFuture.allOf(partitions
                    .stream()
                    .map(p -> p.entrySet(tableName).thenApply(entrySet::addAll))
                    .toArray(CompletableFuture[]::new))
                .thenApply(v -> entrySet);
    }

    @Override
    public CompletableFuture<Result<Versioned<byte[]>>> putIfAbsent(String tableName, String key, byte[] value) {
        checkState(isOpen.get(), DB_NOT_OPEN);
        return partitioner.getPartition(tableName, key).putIfAbsent(tableName, key, value);
    }

    @Override
    public CompletableFuture<Result<Boolean>> remove(String tableName, String key, byte[] value) {
        checkState(isOpen.get(), DB_NOT_OPEN);
        return partitioner.getPartition(tableName, key).remove(tableName, key, value);
    }

    @Override
    public CompletableFuture<Result<Boolean>> remove(String tableName, String key, long version) {
        checkState(isOpen.get(), DB_NOT_OPEN);
        return partitioner.getPartition(tableName, key).remove(tableName, key, version);
    }

    @Override
    public CompletableFuture<Result<Boolean>> replace(
            String tableName, String key, byte[] oldValue, byte[] newValue) {
        checkState(isOpen.get(), DB_NOT_OPEN);
        return partitioner.getPartition(tableName, key).replace(tableName, key, oldValue, newValue);
    }

    @Override
    public CompletableFuture<Result<Boolean>> replace(
            String tableName, String key, long oldVersion, byte[] newValue) {
        checkState(isOpen.get(), DB_NOT_OPEN);
        return partitioner.getPartition(tableName, key).replace(tableName, key, oldVersion, newValue);
    }

    @Override
    public CompletableFuture<Result<UpdateResult<Versioned<byte[]>>>> replaceAndGet(
            String tableName, String key, long oldVersion, byte[] newValue) {
        checkState(isOpen.get(), DB_NOT_OPEN);
        return partitioner.getPartition(tableName, key).replaceAndGet(tableName, key, oldVersion, newValue);
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
    public CompletableFuture<Long> queueSize(String queueName) {
        checkState(isOpen.get(), DB_NOT_OPEN);
        return partitioner.getPartition(queueName, queueName).queueSize(queueName);
    }

    @Override
    public CompletableFuture<Set<NodeId>> queuePush(String queueName, byte[] entry) {
        checkState(isOpen.get(), DB_NOT_OPEN);
        return partitioner.getPartition(queueName, queueName).queuePush(queueName, entry);
    }

    @Override
    public CompletableFuture<byte[]> queuePop(String queueName, NodeId nodeId) {
        checkState(isOpen.get(), DB_NOT_OPEN);
        return partitioner.getPartition(queueName, queueName).queuePop(queueName, nodeId);
    }

    @Override
    public CompletableFuture<byte[]> queuePeek(String queueName) {
        checkState(isOpen.get(), DB_NOT_OPEN);
        return partitioner.getPartition(queueName, queueName).queuePeek(queueName);
    }

    @Override
    public CompletableFuture<Boolean> prepareAndCommit(Transaction transaction) {
        Map<Database, Transaction> subTransactions = createSubTransactions(transaction);
        if (subTransactions.isEmpty()) {
            return CompletableFuture.completedFuture(true);
        } else if (subTransactions.size() == 1) {
            Entry<Database, Transaction> entry =
                    subTransactions.entrySet().iterator().next();
            return entry.getKey().prepareAndCommit(entry.getValue());
        } else {
            if (transactionManager != null) {
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
    public CompletableFuture<Boolean> commit(Transaction transaction) {
        Map<Database, Transaction> subTransactions = createSubTransactions(transaction);
        return CompletableFuture.allOf(subTransactions.entrySet()
                .stream()
                .map(entry -> entry.getKey().commit(entry.getValue()))
                .toArray(CompletableFuture[]::new))
        .thenApply(v -> true);
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
            Database partition = partitioner.getPartition(update.tableName(), update.key());
            List<DatabaseUpdate> partitionUpdates =
                    perPartitionUpdates.computeIfAbsent(partition, k -> Lists.newLinkedList());
            partitionUpdates.add(update);
        }
        Map<Database, Transaction> subTransactions = Maps.newHashMap();
        perPartitionUpdates.forEach((k, v) -> subTransactions.put(k, new DefaultTransaction(transaction.id(), v)));
        return subTransactions;
    }

    protected void setTransactionManager(TransactionManager tranasactionManager) {
        this.transactionManager = transactionManager;
    }
}
