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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import net.kuujo.copycat.cluster.internal.coordinator.ClusterCoordinator;

/**
 * A database that partitions the keys across one or more database partitions.
 */
public class PartitionedDatabase implements DatabaseProxy<String, byte[]>, PartitionedDatabaseManager {

    private Partitioner<String> partitioner;
    private final ClusterCoordinator coordinator;
    private final Map<String, Database> partitions = Maps.newConcurrentMap();

    protected PartitionedDatabase(ClusterCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    @Override
    public void registerPartition(String name, Database partition) {
        partitions.put(name, partition);
    }

    @Override
    public Map<String, Database> getRegisteredPartitions() {
        return ImmutableMap.copyOf(partitions);
    }

    @Override
    public CompletableFuture<Integer> size(String tableName) {
        AtomicInteger totalSize = new AtomicInteger(0);
        return CompletableFuture.allOf(partitions
                    .values()
                    .stream()
                    .map(p -> p.size(tableName).thenApply(totalSize::addAndGet))
                    .toArray(CompletableFuture[]::new))
                .thenApply(v -> totalSize.get());
    }

    @Override
    public CompletableFuture<Boolean> isEmpty(String tableName) {
        return size(tableName).thenApply(size -> size == 0);
    }

    @Override
    public CompletableFuture<Boolean> containsKey(String tableName, String key) {
        return partitioner.getPartition(tableName, key).containsKey(tableName, key);
    }

    @Override
    public CompletableFuture<Boolean> containsValue(String tableName, byte[] value) {
        AtomicBoolean containsValue = new AtomicBoolean(false);
        return CompletableFuture.allOf(partitions
                    .values()
                    .stream()
                    .map(p -> p.containsValue(tableName, value).thenApply(v -> containsValue.compareAndSet(false, v)))
                    .toArray(CompletableFuture[]::new))
                .thenApply(v -> containsValue.get());
    }

    @Override
    public CompletableFuture<Versioned<byte[]>> get(String tableName, String key) {
        return partitioner.getPartition(tableName, key).get(tableName, key);
    }

    @Override
    public CompletableFuture<Versioned<byte[]>> put(String tableName, String key, byte[] value) {
        return partitioner.getPartition(tableName, key).put(tableName, key, value);
    }

    @Override
    public CompletableFuture<Versioned<byte[]>> remove(String tableName, String key) {
        return partitioner.getPartition(tableName, key).remove(tableName, key);
    }

    @Override
    public CompletableFuture<Void> clear(String tableName) {
        return CompletableFuture.allOf(partitions
                    .values()
                    .stream()
                    .map(p -> p.clear(tableName))
                    .toArray(CompletableFuture[]::new));
    }

    @Override
    public CompletableFuture<Set<String>> keySet(String tableName) {
        Set<String> keySet = Sets.newConcurrentHashSet();
        return CompletableFuture.allOf(partitions
                    .values()
                    .stream()
                    .map(p -> p.keySet(tableName).thenApply(keySet::addAll))
                    .toArray(CompletableFuture[]::new))
                .thenApply(v -> keySet);
    }

    @Override
    public CompletableFuture<Collection<Versioned<byte[]>>> values(String tableName) {
        List<Versioned<byte[]>> values = new CopyOnWriteArrayList<>();
        return CompletableFuture.allOf(partitions
                    .values()
                    .stream()
                    .map(p -> p.values(tableName))
                    .toArray(CompletableFuture[]::new))
                .thenApply(v -> values);
    }

    @Override
    public CompletableFuture<Set<Entry<String, Versioned<byte[]>>>> entrySet(String tableName) {
        Set<Entry<String, Versioned<byte[]>>> entrySet = Sets.newConcurrentHashSet();
        return CompletableFuture.allOf(partitions
                    .values()
                    .stream()
                    .map(p -> p.entrySet(tableName).thenApply(entrySet::addAll))
                    .toArray(CompletableFuture[]::new))
                .thenApply(v -> entrySet);
    }

    @Override
    public CompletableFuture<Versioned<byte[]>> putIfAbsent(String tableName, String key, byte[] value) {
        return partitioner.getPartition(tableName, key).putIfAbsent(tableName, key, value);
    }

    @Override
    public CompletableFuture<Boolean> remove(String tableName, String key, byte[] value) {
        return partitioner.getPartition(tableName, key).remove(tableName, key, value);
    }

    @Override
    public CompletableFuture<Boolean> remove(String tableName, String key, long version) {
        return partitioner.getPartition(tableName, key).remove(tableName, key, version);
    }

    @Override
    public CompletableFuture<Boolean> replace(String tableName, String key, byte[] oldValue, byte[] newValue) {
        return partitioner.getPartition(tableName, key).replace(tableName, key, oldValue, newValue);
    }

    @Override
    public CompletableFuture<Boolean> replace(String tableName, String key, long oldVersion, byte[] newValue) {
        return partitioner.getPartition(tableName, key).replace(tableName, key, oldVersion, newValue);
    }

    @Override
    public CompletableFuture<Boolean> atomicBatchUpdate(List<UpdateOperation<String, byte[]>> updates) {
        Map<Database, List<UpdateOperation<String, byte[]>>> perPartitionUpdates = Maps.newHashMap();
        for (UpdateOperation<String, byte[]> update : updates) {
            Database partition = partitioner.getPartition(update.tableName(), update.key());
            List<UpdateOperation<String, byte[]>> partitionUpdates = perPartitionUpdates.get(partition);
            if (partitionUpdates == null) {
                partitionUpdates = Lists.newArrayList();
                perPartitionUpdates.put(partition, partitionUpdates);
            }
            partitionUpdates.add(update);
        }
        if (perPartitionUpdates.size() > 1) {
            // TODO
            throw new UnsupportedOperationException("Cross partition transactional updates are not supported.");
        } else {
            Entry<Database, List<UpdateOperation<String, byte[]>>> only =
                    perPartitionUpdates.entrySet().iterator().next();
            return only.getKey().atomicBatchUpdate(only.getValue());
        }
    }

    @Override
    public void setPartitioner(Partitioner<String> partitioner) {
        this.partitioner = partitioner;
    }

    @Override
    public CompletableFuture<PartitionedDatabase> open() {
        return coordinator.open().thenCompose(c -> CompletableFuture.allOf(partitions
                                                        .values()
                                                        .stream()
                                                        .map(Database::open)
                                                        .collect(Collectors.toList())
                                                        .toArray(new CompletableFuture[partitions.size()]))
                                 .thenApply(v -> this));

    }

    @Override
    public CompletableFuture<Void> close() {
        CompletableFuture<Void> closePartitions = CompletableFuture.allOf(partitions
                .values()
                .stream()
                .map(database -> database.close())
                .collect(Collectors.toList())
                .toArray(new CompletableFuture[partitions.size()]));
        CompletableFuture<Void> closeCoordinator = coordinator.close();
        return closePartitions.thenCompose(v -> closeCoordinator);
    }
}
