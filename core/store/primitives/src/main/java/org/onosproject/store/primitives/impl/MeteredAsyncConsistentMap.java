/*
 * Copyright 2016-present Open Networking Laboratory
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
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import org.onosproject.store.primitives.MapUpdate;
import org.onosproject.store.primitives.TransactionId;
import org.onosproject.store.service.AsyncConsistentMap;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.TransactionLog;
import org.onosproject.store.service.Version;
import org.onosproject.store.service.Versioned;

import com.google.common.base.Throwables;
import com.google.common.collect.Maps;

import org.onosproject.utils.MeteringAgent;

/**
 * {@link AsyncConsistentMap} that meters all its operations.
 *
 * @param <K> key type
 * @param <V> value type
 */
public class MeteredAsyncConsistentMap<K, V>  extends DelegatingAsyncConsistentMap<K, V> {

    private static final String PRIMITIVE_NAME = "consistentMap";
    private static final String SIZE = "size";
    private static final String IS_EMPTY = "isEmpty";
    private static final String CONTAINS_KEY = "containsKey";
    private static final String CONTAINS_VALUE = "containsValue";
    private static final String GET = "get";
    private static final String GET_OR_DEFAULT = "getOrDefault";
    private static final String COMPUTE_IF = "computeIf";
    private static final String PUT = "put";
    private static final String PUT_AND_GET = "putAndGet";
    private static final String PUT_IF_ABSENT = "putIfAbsent";
    private static final String REMOVE = "remove";
    private static final String CLEAR = "clear";
    private static final String KEY_SET = "keySet";
    private static final String VALUES = "values";
    private static final String ENTRY_SET = "entrySet";
    private static final String REPLACE = "replace";
    private static final String COMPUTE_IF_ABSENT = "computeIfAbsent";
    private static final String BEGIN = "begin";
    private static final String PREPARE = "prepare";
    private static final String COMMIT = "commit";
    private static final String ROLLBACK = "rollback";
    private static final String PREPARE_AND_COMMIT = "prepareAndCommit";
    private static final String ADD_LISTENER = "addListener";
    private static final String REMOVE_LISTENER = "removeListener";
    private static final String NOTIFY_LISTENER = "notifyListener";

    private final Map<MapEventListener<K, V>, InternalMeteredMapEventListener> listeners =
            Maps.newIdentityHashMap();
    private final MeteringAgent monitor;

    public MeteredAsyncConsistentMap(AsyncConsistentMap<K, V> backingMap) {
        super(backingMap);
        this.monitor = new MeteringAgent(PRIMITIVE_NAME, backingMap.name(), true);
    }

    @Override
    public CompletableFuture<Integer> size() {
        final MeteringAgent.Context timer = monitor.startTimer(SIZE);
        return super.size()
                    .whenComplete((r, e) -> timer.stop(e));
    }

    @Override
    public CompletableFuture<Boolean> isEmpty() {
        final MeteringAgent.Context timer = monitor.startTimer(IS_EMPTY);
        return super.isEmpty()
                    .whenComplete((r, e) -> timer.stop(e));
    }

    @Override
    public CompletableFuture<Boolean> containsKey(K key) {
        final MeteringAgent.Context timer = monitor.startTimer(CONTAINS_KEY);
        return super.containsKey(key)
                    .whenComplete((r, e) -> timer.stop(e));
    }

    @Override
    public CompletableFuture<Boolean> containsValue(V value) {
        final MeteringAgent.Context timer = monitor.startTimer(CONTAINS_VALUE);
        return super.containsValue(value)
                    .whenComplete((r, e) -> timer.stop(e));
    }

    @Override
    public CompletableFuture<Versioned<V>> get(K key) {
        final MeteringAgent.Context timer = monitor.startTimer(GET);
        return super.get(key)
                    .whenComplete((r, e) -> timer.stop(e));
    }

    @Override
    public CompletableFuture<Versioned<V>> getOrDefault(K key, V defaultValue) {
        final MeteringAgent.Context timer = monitor.startTimer(GET_OR_DEFAULT);
        return super.getOrDefault(key, defaultValue)
                .whenComplete((r, e) -> timer.stop(e));
    }

    @Override
    public CompletableFuture<Versioned<V>> computeIfAbsent(K key,
                                                           Function<? super K, ? extends V> mappingFunction) {
        final MeteringAgent.Context timer = monitor.startTimer(COMPUTE_IF_ABSENT);
        return super.computeIfAbsent(key, mappingFunction)
                    .whenComplete((r, e) -> timer.stop(e));
    }

    @Override
    public CompletableFuture<Versioned<V>> computeIf(K key,
                                                     Predicate<? super V> condition,
                                                     BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        final MeteringAgent.Context timer = monitor.startTimer(COMPUTE_IF);
        return super.computeIf(key, condition, remappingFunction)
                    .whenComplete((r, e) -> timer.stop(e));
    }

    @Override
    public CompletableFuture<Versioned<V>> put(K key, V value) {
        final MeteringAgent.Context timer = monitor.startTimer(PUT);
        return super.put(key, value)
                    .whenComplete((r, e) -> timer.stop(e));
    }

    @Override
    public CompletableFuture<Versioned<V>> putAndGet(K key, V value) {
        final MeteringAgent.Context timer = monitor.startTimer(PUT_AND_GET);
        return super.putAndGet(key, value)
                    .whenComplete((r, e) -> timer.stop(e));
    }

    @Override
    public CompletableFuture<Versioned<V>> remove(K key) {
        final MeteringAgent.Context timer = monitor.startTimer(REMOVE);
        return super.remove(key)
                    .whenComplete((r, e) -> timer.stop(e));
    }

    @Override
    public CompletableFuture<Void> clear() {
        final MeteringAgent.Context timer = monitor.startTimer(CLEAR);
        return super.clear()
                    .whenComplete((r, e) -> timer.stop(e));
    }

    @Override
    public CompletableFuture<Set<K>> keySet() {
        final MeteringAgent.Context timer = monitor.startTimer(KEY_SET);
        return super.keySet()
                    .whenComplete((r, e) -> timer.stop(e));
    }

    @Override
    public CompletableFuture<Collection<Versioned<V>>> values() {
        final MeteringAgent.Context timer = monitor.startTimer(VALUES);
        return super.values()
                    .whenComplete((r, e) -> timer.stop(e));
    }

    @Override
    public CompletableFuture<Set<Entry<K, Versioned<V>>>> entrySet() {
        final MeteringAgent.Context timer = monitor.startTimer(ENTRY_SET);
        return super.entrySet()
                    .whenComplete((r, e) -> timer.stop(e));
    }

    @Override
    public CompletableFuture<Versioned<V>> putIfAbsent(K key, V value) {
        final MeteringAgent.Context timer = monitor.startTimer(PUT_IF_ABSENT);
        return super.putIfAbsent(key, value)
                    .whenComplete((r, e) -> timer.stop(e));
    }

    @Override
    public CompletableFuture<Boolean> remove(K key, V value) {
        final MeteringAgent.Context timer = monitor.startTimer(REMOVE);
        return super.remove(key, value)
                    .whenComplete((r, e) -> timer.stop(e));

    }

    @Override
    public CompletableFuture<Boolean> remove(K key, long version) {
        final MeteringAgent.Context timer = monitor.startTimer(REMOVE);
        return super.remove(key, version)
                    .whenComplete((r, e) -> timer.stop(e));
    }

    @Override
    public CompletableFuture<Versioned<V>> replace(K key, V value) {
        final MeteringAgent.Context timer = monitor.startTimer(REPLACE);
        return super.replace(key, value)
                    .whenComplete((r, e) -> timer.stop(e));
    }

    @Override
    public CompletableFuture<Boolean> replace(K key, V oldValue, V newValue) {
        final MeteringAgent.Context timer = monitor.startTimer(REPLACE);
        return super.replace(key, oldValue, newValue)
                    .whenComplete((r, e) -> timer.stop(e));
    }

    @Override
    public CompletableFuture<Boolean> replace(K key, long oldVersion, V newValue) {
        final MeteringAgent.Context timer = monitor.startTimer(REPLACE);
        return super.replace(key, oldVersion, newValue)
                    .whenComplete((r, e) -> timer.stop(e));
    }

    @Override
    public CompletableFuture<Void> addListener(MapEventListener<K, V> listener, Executor executor) {
        final MeteringAgent.Context timer = monitor.startTimer(ADD_LISTENER);
        synchronized (listeners) {
            InternalMeteredMapEventListener meteredListener =
                    listeners.computeIfAbsent(listener, k -> new InternalMeteredMapEventListener(listener));
            return super.addListener(meteredListener, executor)
                        .whenComplete((r, e) -> timer.stop(e));
        }
    }

    @Override
    public CompletableFuture<Void> removeListener(MapEventListener<K, V> listener) {
        final MeteringAgent.Context timer = monitor.startTimer(REMOVE_LISTENER);
        InternalMeteredMapEventListener meteredListener = listeners.remove(listener);
        if (meteredListener != null) {
            return super.removeListener(meteredListener)
                        .whenComplete((r, e) -> timer.stop(e));
        } else {
            timer.stop(null);
            return CompletableFuture.completedFuture(null);
        }
    }

    @Override
    public CompletableFuture<Version> begin(TransactionId transactionId) {
        final MeteringAgent.Context timer = monitor.startTimer(BEGIN);
        return super.begin(transactionId)
                .whenComplete((r, e) -> timer.stop(e));
    }

    @Override
    public CompletableFuture<Boolean> prepare(TransactionLog<MapUpdate<K, V>> transactionLog) {
        final MeteringAgent.Context timer = monitor.startTimer(PREPARE);
        return super.prepare(transactionLog)
                .whenComplete((r, e) -> timer.stop(e));
    }

    @Override
    public CompletableFuture<Void> commit(TransactionId transactionId) {
        final MeteringAgent.Context timer = monitor.startTimer(COMMIT);
        return super.commit(transactionId)
                    .whenComplete((r, e) -> timer.stop(e));
    }

    @Override
    public CompletableFuture<Void> rollback(TransactionId transactionId) {
        final MeteringAgent.Context timer = monitor.startTimer(ROLLBACK);
        return super.rollback(transactionId)
                    .whenComplete((r, e) -> timer.stop(e));
    }

    @Override
    public CompletableFuture<Boolean> prepareAndCommit(TransactionLog<MapUpdate<K, V>> transactionLog) {
        final MeteringAgent.Context timer = monitor.startTimer(PREPARE_AND_COMMIT);
        return super.prepareAndCommit(transactionLog)
                .whenComplete((r, e) -> timer.stop(e));
    }

    private class InternalMeteredMapEventListener implements MapEventListener<K, V> {

        private final MapEventListener<K, V> listener;

        InternalMeteredMapEventListener(MapEventListener<K, V> listener) {
            this.listener = listener;
        }

        @Override
        public void event(MapEvent<K, V> event) {
            final MeteringAgent.Context timer = monitor.startTimer(NOTIFY_LISTENER);
            try {
                listener.event(event);
                timer.stop(null);
            } catch (Exception e) {
                timer.stop(e);
                Throwables.propagate(e);
            }
        }
    }
}