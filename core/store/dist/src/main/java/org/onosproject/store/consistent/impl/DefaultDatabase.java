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

import net.kuujo.copycat.state.StateMachine;
import net.kuujo.copycat.resource.internal.AbstractResource;
import net.kuujo.copycat.resource.internal.ResourceManager;
import net.kuujo.copycat.state.internal.DefaultStateMachine;
import net.kuujo.copycat.util.concurrent.Futures;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.onosproject.cluster.NodeId;
import org.onosproject.store.service.Transaction;
import org.onosproject.store.service.Versioned;

/**
 * Default database.
 */
public class DefaultDatabase extends AbstractResource<Database> implements Database {
    private final StateMachine<DatabaseState<String, byte[]>> stateMachine;
    private DatabaseProxy<String, byte[]> proxy;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public DefaultDatabase(ResourceManager context) {
        super(context);
        this.stateMachine = new DefaultStateMachine(context, DatabaseState.class, DefaultDatabaseState.class);
    }

    /**
     * If the database is closed, returning a failed CompletableFuture. Otherwise, calls the given supplier to
     * return the completed future result.
     *
     * @param supplier The supplier to call if the database is open.
     * @param <T> The future result type.
     * @return A completable future that if this database is closed is immediately failed.
     */
    protected <T> CompletableFuture<T> checkOpen(Supplier<CompletableFuture<T>> supplier) {
        if (proxy == null) {
            return Futures.exceptionalFuture(new IllegalStateException("Database closed"));
        }
        return supplier.get();
    }

    @Override
    public CompletableFuture<Set<String>> tableNames() {
        return checkOpen(() -> proxy.tableNames());
    }

    @Override
    public CompletableFuture<Map<String, Long>> counters() {
        return checkOpen(() -> proxy.counters());
    }

    @Override
    public CompletableFuture<Integer> size(String tableName) {
        return checkOpen(() -> proxy.size(tableName));
    }

    @Override
    public CompletableFuture<Boolean> isEmpty(String tableName) {
        return checkOpen(() -> proxy.isEmpty(tableName));
    }

    @Override
    public CompletableFuture<Boolean> containsKey(String tableName, String key) {
        return checkOpen(() -> proxy.containsKey(tableName, key));
    }

    @Override
    public CompletableFuture<Boolean> containsValue(String tableName, byte[] value) {
        return checkOpen(() -> proxy.containsValue(tableName, value));
    }

    @Override
    public CompletableFuture<Versioned<byte[]>> get(String tableName, String key) {
        return checkOpen(() -> proxy.get(tableName, key));
    }

    @Override
    public CompletableFuture<Result<Versioned<byte[]>>> put(String tableName, String key, byte[] value) {
        return checkOpen(() -> proxy.put(tableName, key, value));
    }

    @Override
    public CompletableFuture<Result<UpdateResult<Versioned<byte[]>>>> putAndGet(String tableName,
            String key,
            byte[] value) {
        return checkOpen(() -> proxy.putAndGet(tableName, key, value));
    }

    @Override
    public CompletableFuture<Result<UpdateResult<Versioned<byte[]>>>> putIfAbsentAndGet(String tableName,
            String key,
            byte[] value) {
        return checkOpen(() -> proxy.putIfAbsentAndGet(tableName, key, value));
    }

    @Override
    public CompletableFuture<Result<Versioned<byte[]>>> remove(String tableName, String key) {
        return checkOpen(() -> proxy.remove(tableName, key));
    }

    @Override
    public CompletableFuture<Result<Void>> clear(String tableName) {
        return checkOpen(() -> proxy.clear(tableName));
    }

    @Override
    public CompletableFuture<Set<String>> keySet(String tableName) {
        return checkOpen(() -> proxy.keySet(tableName));
    }

    @Override
    public CompletableFuture<Collection<Versioned<byte[]>>> values(String tableName) {
        return checkOpen(() -> proxy.values(tableName));
    }

    @Override
    public CompletableFuture<Set<Map.Entry<String, Versioned<byte[]>>>> entrySet(String tableName) {
        return checkOpen(() -> proxy.entrySet(tableName));
    }

    @Override
    public CompletableFuture<Result<Versioned<byte[]>>> putIfAbsent(String tableName, String key, byte[] value) {
        return checkOpen(() -> proxy.putIfAbsent(tableName, key, value));
    }

    @Override
    public CompletableFuture<Result<Boolean>> remove(String tableName, String key, byte[] value) {
        return checkOpen(() -> proxy.remove(tableName, key, value));
    }

    @Override
    public CompletableFuture<Result<Boolean>> remove(String tableName, String key, long version) {
        return checkOpen(() -> proxy.remove(tableName, key, version));
    }

    @Override
    public CompletableFuture<Result<Boolean>> replace(String tableName, String key, byte[] oldValue, byte[] newValue) {
        return checkOpen(() -> proxy.replace(tableName, key, oldValue, newValue));
    }

    @Override
    public CompletableFuture<Result<Boolean>> replace(String tableName, String key, long oldVersion, byte[] newValue) {
        return checkOpen(() -> proxy.replace(tableName, key, oldVersion, newValue));
    }

    @Override
    public CompletableFuture<Result<UpdateResult<Versioned<byte[]>>>> replaceAndGet(String tableName,
            String key,
            long oldVersion,
            byte[] newValue) {
        return checkOpen(() -> proxy.replaceAndGet(tableName, key, oldVersion, newValue));
    }

    @Override
    public CompletableFuture<Long> counterGet(String counterName) {
        return checkOpen(() -> proxy.counterGet(counterName));
    }

    @Override
    public CompletableFuture<Long> counterAddAndGet(String counterName, long delta) {
        return checkOpen(() -> proxy.counterAddAndGet(counterName, delta));
    }

    @Override
    public CompletableFuture<Long> counterGetAndAdd(String counterName, long delta) {
        return checkOpen(() -> proxy.counterGetAndAdd(counterName, delta));
    }

    @Override
    public CompletableFuture<Long> queueSize(String queueName) {
        return checkOpen(() -> proxy.queueSize(queueName));
    }

    @Override
    public CompletableFuture<Set<NodeId>> queuePush(String queueName, byte[] entry) {
        return checkOpen(() -> proxy.queuePush(queueName, entry));
    }

    @Override
    public CompletableFuture<byte[]> queuePop(String queueName, NodeId nodeId) {
        return checkOpen(() -> proxy.queuePop(queueName, nodeId));
    }

    @Override
    public CompletableFuture<byte[]> queuePeek(String queueName) {
        return checkOpen(() -> proxy.queuePeek(queueName));
    }

    @Override
    public CompletableFuture<Boolean> prepareAndCommit(Transaction transaction) {
        return checkOpen(() -> proxy.prepareAndCommit(transaction));
    }

    @Override
    public CompletableFuture<Boolean> prepare(Transaction transaction) {
        return checkOpen(() -> proxy.prepare(transaction));
    }

    @Override
    public CompletableFuture<Boolean> commit(Transaction transaction) {
        return checkOpen(() -> proxy.commit(transaction));
    }

    @Override
    public CompletableFuture<Boolean> rollback(Transaction transaction) {
        return checkOpen(() -> proxy.rollback(transaction));
    }

    @Override
    @SuppressWarnings("unchecked")
    public synchronized CompletableFuture<Database> open() {
        return runStartupTasks()
                .thenCompose(v -> stateMachine.open())
                .thenRun(() -> {
                    this.proxy = stateMachine.createProxy(DatabaseProxy.class, this.getClass().getClassLoader());
                })
                .thenApply(v -> null);
    }

    @Override
    public synchronized CompletableFuture<Void> close() {
        proxy = null;
        return stateMachine.close()
                .thenCompose(v -> runShutdownTasks());
    }

    @Override
    public int hashCode() {
        return name().hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Database) {
            return name().equals(((Database) other).name());
        }
        return false;
    }
}
