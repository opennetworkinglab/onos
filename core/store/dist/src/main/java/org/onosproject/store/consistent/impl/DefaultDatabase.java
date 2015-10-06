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

import com.google.common.collect.Sets;
import net.kuujo.copycat.resource.internal.AbstractResource;
import net.kuujo.copycat.resource.internal.ResourceManager;
import net.kuujo.copycat.state.StateMachine;
import net.kuujo.copycat.state.internal.DefaultStateMachine;
import net.kuujo.copycat.util.concurrent.Futures;
import net.kuujo.copycat.util.function.TriConsumer;
import org.onosproject.store.service.Transaction;
import org.onosproject.store.service.Versioned;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Default database.
 */
public class DefaultDatabase extends AbstractResource<Database> implements Database {
    private final StateMachine<DatabaseState<String, byte[]>> stateMachine;
    private DatabaseProxy<String, byte[]> proxy;
    private final Set<Consumer<StateMachineUpdate>> consumers = Sets.newCopyOnWriteArraySet();
    private final TriConsumer<String, Object, Object> watcher = new InternalStateMachineWatcher();

    @SuppressWarnings({"unchecked", "rawtypes"})
    public DefaultDatabase(ResourceManager context) {
        super(context);
        this.stateMachine = new DefaultStateMachine(context,
                DatabaseState.class,
                DefaultDatabaseState.class,
                DefaultDatabase.class.getClassLoader());
        this.stateMachine.addStartupTask(() -> {
            stateMachine.registerWatcher(watcher);
            return CompletableFuture.completedFuture(null);
        });
        this.stateMachine.addShutdownTask(() -> {
            stateMachine.unregisterWatcher(watcher);
            return CompletableFuture.completedFuture(null);
        });
    }

    /**
     * If the database is closed, returning a failed CompletableFuture. Otherwise, calls the given supplier to
     * return the completed future result.
     *
     * @param supplier The supplier to call if the database is open.
     * @param <T>      The future result type.
     * @return A completable future that if this database is closed is immediately failed.
     */
    protected <T> CompletableFuture<T> checkOpen(Supplier<CompletableFuture<T>> supplier) {
        if (proxy == null) {
            return Futures.exceptionalFuture(new IllegalStateException("Database closed"));
        }
        return supplier.get();
    }

    @Override
    public CompletableFuture<Set<String>> maps() {
        return checkOpen(() -> proxy.maps());
    }

    @Override
    public CompletableFuture<Map<String, Long>> counters() {
        return checkOpen(() -> proxy.counters());
    }

    @Override
    public CompletableFuture<Integer> mapSize(String mapName) {
        return checkOpen(() -> proxy.mapSize(mapName));
    }

    @Override
    public CompletableFuture<Boolean> mapIsEmpty(String mapName) {
        return checkOpen(() -> proxy.mapIsEmpty(mapName));
    }

    @Override
    public CompletableFuture<Boolean> mapContainsKey(String mapName, String key) {
        return checkOpen(() -> proxy.mapContainsKey(mapName, key));
    }

    @Override
    public CompletableFuture<Boolean> mapContainsValue(String mapName, byte[] value) {
        return checkOpen(() -> proxy.mapContainsValue(mapName, value));
    }

    @Override
    public CompletableFuture<Versioned<byte[]>> mapGet(String mapName, String key) {
        return checkOpen(() -> proxy.mapGet(mapName, key));
    }

    @Override
    public CompletableFuture<Result<UpdateResult<String, byte[]>>> mapUpdate(
            String mapName, String key, Match<byte[]> valueMatch, Match<Long> versionMatch, byte[] value) {
        return checkOpen(() -> proxy.mapUpdate(mapName, key, valueMatch, versionMatch, value));
    }

    @Override
    public CompletableFuture<Result<Void>> mapClear(String mapName) {
        return checkOpen(() -> proxy.mapClear(mapName));
    }

    @Override
    public CompletableFuture<Set<String>> mapKeySet(String mapName) {
        return checkOpen(() -> proxy.mapKeySet(mapName));
    }

    @Override
    public CompletableFuture<Collection<Versioned<byte[]>>> mapValues(String mapName) {
        return checkOpen(() -> proxy.mapValues(mapName));
    }

    @Override
    public CompletableFuture<Set<Map.Entry<String, Versioned<byte[]>>>> mapEntrySet(String mapName) {
        return checkOpen(() -> proxy.mapEntrySet(mapName));
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
    public CompletableFuture<Void> counterSet(String counterName, long value) {
        return checkOpen(() -> proxy.counterSet(counterName, value));
    }

    @Override
    public CompletableFuture<Boolean> counterCompareAndSet(String counterName, long expectedValue, long update) {
        return checkOpen(() -> proxy.counterCompareAndSet(counterName, expectedValue, update));
    }

    @Override
    public CompletableFuture<Long> queueSize(String queueName) {
        return checkOpen(() -> proxy.queueSize(queueName));
    }

    @Override
    public CompletableFuture<Void> queuePush(String queueName, byte[] entry) {
        return checkOpen(() -> proxy.queuePush(queueName, entry));
    }

    @Override
    public CompletableFuture<byte[]> queuePop(String queueName) {
        return checkOpen(() -> proxy.queuePop(queueName));
    }

    @Override
    public CompletableFuture<byte[]> queuePeek(String queueName) {
        return checkOpen(() -> proxy.queuePeek(queueName));
    }

    @Override
    public CompletableFuture<CommitResponse> prepareAndCommit(Transaction transaction) {
        return checkOpen(() -> proxy.prepareAndCommit(transaction));
    }

    @Override
    public CompletableFuture<Boolean> prepare(Transaction transaction) {
        return checkOpen(() -> proxy.prepare(transaction));
    }

    @Override
    public CompletableFuture<CommitResponse> commit(Transaction transaction) {
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

    @Override
    public void registerConsumer(Consumer<StateMachineUpdate> consumer) {
        consumers.add(consumer);
    }

    @Override
    public void unregisterConsumer(Consumer<StateMachineUpdate> consumer) {
        consumers.remove(consumer);
    }

    private class InternalStateMachineWatcher implements TriConsumer<String, Object, Object> {
        @Override
        public void accept(String name, Object input, Object output) {
            StateMachineUpdate update = new StateMachineUpdate(name, input, output);
            consumers.forEach(consumer -> consumer.accept(update));
        }
    }
}
