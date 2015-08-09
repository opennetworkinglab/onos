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

import static com.google.common.base.Preconditions.*;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.Set;

import com.codahale.metrics.Timer;
import org.onlab.metrics.MetricsComponent;
import org.onlab.metrics.MetricsFeature;
import org.onlab.metrics.MetricsService;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.util.HexString;
import org.onlab.util.SharedExecutors;
import org.onlab.util.Tools;
import org.onosproject.core.ApplicationId;

import static org.onosproject.store.consistent.impl.StateMachineUpdate.Target.MAP;

import org.onosproject.store.service.AsyncConsistentMap;
import org.onosproject.store.service.ConsistentMapException;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.Versioned;
import org.slf4j.Logger;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Maps;

/**
 * AsyncConsistentMap implementation that is backed by a Raft consensus
 * based database.
 *
 * @param <K> type of key.
 * @param <V> type of value.
 */
public class DefaultAsyncConsistentMap<K, V> implements AsyncConsistentMap<K, V> {

    private final String name;
    private final ApplicationId applicationId;
    private final Database database;
    private final Serializer serializer;
    private final boolean readOnly;
    private final boolean purgeOnUninstall;
    private final Consumer<MapEvent<K, V>> eventPublisher;

    private final MetricsService metricsService;
    private final MetricsComponent metricsComponent;
    private final MetricsFeature metricsFeature;
    private final Map<String, Timer> perMapOpTimers = Maps.newConcurrentMap();
    private final Map<String, Timer> perOpTimers = Maps.newConcurrentMap();
    private final Timer cMapTimer;
    private final Timer perMapTimer;
    private final MetricsFeature wildcard;

    private static final String COMPONENT_NAME = "consistentMap";
    private static final String SIZE = "size";
    private static final String IS_EMPTY = "isEmpty";
    private static final String CONTAINS_KEY = "containsKey";
    private static final String CONTAINS_VALUE = "containsValue";
    private static final String GET = "get";
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


    private final Set<MapEventListener<K, V>> listeners = new CopyOnWriteArraySet<>();

    private final Logger log = getLogger(getClass());

    private static final String ERROR_NULL_KEY = "Key cannot be null";
    private static final String ERROR_NULL_VALUE = "Null values are not allowed";

    private final LoadingCache<K, String> keyCache = CacheBuilder.newBuilder()
            .softValues()
            .build(new CacheLoader<K, String>() {

                @Override
                public String load(K key) {
                    return HexString.toHexString(serializer.encode(key));
                }
            });

    protected K dK(String key) {
        return serializer.decode(HexString.fromHexString(key));
    }

    public DefaultAsyncConsistentMap(String name,
            ApplicationId applicationId,
            Database database,
            Serializer serializer,
            boolean readOnly,
            boolean purgeOnUninstall,
            Consumer<MapEvent<K, V>> eventPublisher) {
        this.name = checkNotNull(name, "map name cannot be null");
        this.applicationId = applicationId;
        this.database = checkNotNull(database, "database cannot be null");
        this.serializer = checkNotNull(serializer, "serializer cannot be null");
        this.readOnly = readOnly;
        this.purgeOnUninstall = purgeOnUninstall;
        this.eventPublisher = eventPublisher;
        this.database.registerConsumer(update -> {
            SharedExecutors.getSingleThreadExecutor().execute(() -> {
                if (update.target() == MAP) {
                    Result<UpdateResult<String, byte[]>> result = update.output();
                    if (result.success() && result.value().mapName().equals(name)) {
                        MapEvent<K, V> mapEvent = result.value().<K, V>map(this::dK, serializer::decode).toMapEvent();
                        notifyLocalListeners(mapEvent);
                    }
                }
            });
        });
        this.metricsService = DefaultServiceDirectory.getService(MetricsService.class);
        this.metricsComponent = metricsService.registerComponent(COMPONENT_NAME);
        this.metricsFeature = metricsComponent.registerFeature(name);
        this.wildcard = metricsComponent.registerFeature("*");
        this.perMapTimer = metricsService.createTimer(metricsComponent, metricsFeature, "*");
        this.cMapTimer = metricsService.createTimer(metricsComponent, wildcard, "*");

    }

    /**
     * Returns this map name.
     * @return map name
     */
    public String name() {
        return name;
    }

    /**
     * Returns the serializer for map entries.
     * @return map entry serializer
     */
    public Serializer serializer() {
        return serializer;
    }

    /**
     * Returns the applicationId owning this map.
     * @return application Id
     */
    public ApplicationId applicationId() {
        return applicationId;
    }

    /**
     * Returns whether the map entries should be purged when the application
     * owning it is uninstalled.
     * @return true is map needs to cleared on app uninstall; false otherwise
     */
    public boolean purgeOnUninstall() {
        return purgeOnUninstall;
    }

    @Override
    public CompletableFuture<Integer> size() {
        final OperationTimer timer = startTimer(SIZE);
        return database.mapSize(name)
                .whenComplete((r, e) -> timer.stop());
    }

    @Override
    public CompletableFuture<Boolean> isEmpty() {
        final OperationTimer timer = startTimer(IS_EMPTY);
        return database.mapIsEmpty(name)
                .whenComplete((r, e) -> timer.stop());
    }

    @Override
    public CompletableFuture<Boolean> containsKey(K key) {
        checkNotNull(key, ERROR_NULL_KEY);
        final OperationTimer timer = startTimer(CONTAINS_KEY);
        return database.mapContainsKey(name, keyCache.getUnchecked(key))
                .whenComplete((r, e) -> timer.stop());
    }

    @Override
    public CompletableFuture<Boolean> containsValue(V value) {
        checkNotNull(value, ERROR_NULL_VALUE);
        final OperationTimer timer = startTimer(CONTAINS_VALUE);
        return database.mapContainsValue(name, serializer.encode(value))
                .whenComplete((r, e) -> timer.stop());
    }

    @Override
    public CompletableFuture<Versioned<V>> get(K key) {
        checkNotNull(key, ERROR_NULL_KEY);
        final OperationTimer timer = startTimer(GET);
        return database.mapGet(name, keyCache.getUnchecked(key))
                .whenComplete((r, e) -> timer.stop())
        .thenApply(v -> v != null ? v.map(serializer::decode) : null);
    }

    @Override
    public CompletableFuture<Versioned<V>> computeIfAbsent(K key,
            Function<? super K, ? extends V> mappingFunction) {
        checkNotNull(key, ERROR_NULL_KEY);
        checkNotNull(mappingFunction, "Mapping function cannot be null");
        final OperationTimer timer = startTimer(COMPUTE_IF_ABSENT);
        return updateAndGet(key, Match.ifNull(), Match.any(), mappingFunction.apply(key))
                .whenComplete((r, e) -> timer.stop())
                .thenApply(v -> v.newValue());
    }

    @Override
    public CompletableFuture<Versioned<V>> computeIfPresent(K key,
            BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return computeIf(key, Objects::nonNull, remappingFunction);
    }

    @Override
    public CompletableFuture<Versioned<V>> compute(K key,
            BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return computeIf(key, v -> true, remappingFunction);
    }

    @Override
    public CompletableFuture<Versioned<V>> computeIf(K key,
            Predicate<? super V> condition,
            BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        checkNotNull(key, ERROR_NULL_KEY);
        checkNotNull(condition, "predicate function cannot be null");
        checkNotNull(remappingFunction, "Remapping function cannot be null");
        final OperationTimer timer = startTimer(COMPUTE_IF);
        return get(key).thenCompose(r1 -> {
            V existingValue = r1 == null ? null : r1.value();
            // if the condition evaluates to false, return existing value.
            if (!condition.test(existingValue)) {
                return CompletableFuture.completedFuture(r1);
            }

            AtomicReference<V> computedValue = new AtomicReference<>();
            // if remappingFunction throws an exception, return the exception.
            try {
                computedValue.set(remappingFunction.apply(key, existingValue));
            } catch (Exception e) {
                return Tools.exceptionalFuture(e);
            }
            if (computedValue.get() == null && r1 == null) {
                return CompletableFuture.completedFuture(null);
            }
            Match<V> valueMatcher = r1 == null ? Match.ifNull() : Match.any();
            Match<Long> versionMatcher = r1 == null ? Match.any() : Match.ifValue(r1.version());
            return updateAndGet(key, valueMatcher, versionMatcher, computedValue.get())
                    .whenComplete((r, e) -> timer.stop())
                    .thenApply(v -> {
                        if (v.updated()) {
                            return v.newValue();
                        } else {
                            throw new ConsistentMapException.ConcurrentModification();
                        }
                    });
        });
    }

    @Override
    public CompletableFuture<Versioned<V>> put(K key, V value) {
        checkNotNull(key, ERROR_NULL_KEY);
        checkNotNull(value, ERROR_NULL_VALUE);
        final OperationTimer timer = startTimer(PUT);
        return updateAndGet(key, Match.any(), Match.any(), value).thenApply(v -> v.oldValue())
                .whenComplete((r, e) -> timer.stop());
    }

    @Override
    public CompletableFuture<Versioned<V>> putAndGet(K key, V value) {
        checkNotNull(key, ERROR_NULL_KEY);
        checkNotNull(value, ERROR_NULL_VALUE);
        final OperationTimer timer = startTimer(PUT_AND_GET);
        return updateAndGet(key, Match.any(), Match.any(), value).thenApply(v -> v.newValue())
                .whenComplete((r, e) -> timer.stop());
    }

    @Override
    public CompletableFuture<Versioned<V>> remove(K key) {
        checkNotNull(key, ERROR_NULL_KEY);
        final OperationTimer timer = startTimer(REMOVE);
        return updateAndGet(key, Match.any(), Match.any(), null).thenApply(v -> v.oldValue())
                .whenComplete((r, e) -> timer.stop());
    }

    @Override
    public CompletableFuture<Void> clear() {
        checkIfUnmodifiable();
        final OperationTimer timer = startTimer(CLEAR);
        return database.mapClear(name).thenApply(this::unwrapResult)
                .whenComplete((r, e) -> timer.stop());
    }

    @Override
    public CompletableFuture<Set<K>> keySet() {
        final OperationTimer timer = startTimer(KEY_SET);
        return database.mapKeySet(name)
                .thenApply(s -> s
                        .stream()
                        .map(this::dK)
                        .collect(Collectors.toSet()))
                .whenComplete((r, e) -> timer.stop());
    }

    @Override
    public CompletableFuture<Collection<Versioned<V>>> values() {
        final OperationTimer timer = startTimer(VALUES);
        return database.mapValues(name)
                .whenComplete((r, e) -> timer.stop())
                .thenApply(c -> c
                        .stream()
                        .map(v -> v.<V>map(serializer::decode))
                        .collect(Collectors.toList()));
    }

    @Override
    public CompletableFuture<Set<Entry<K, Versioned<V>>>> entrySet() {
        final OperationTimer timer = startTimer(ENTRY_SET);
        return database.mapEntrySet(name)
                .whenComplete((r, e) -> timer.stop())
                .thenApply(s -> s
                        .stream()
                        .map(this::mapRawEntry)
                        .collect(Collectors.toSet()));
    }

    @Override
    public CompletableFuture<Versioned<V>> putIfAbsent(K key, V value) {
        checkNotNull(key, ERROR_NULL_KEY);
        checkNotNull(value, ERROR_NULL_VALUE);
        final OperationTimer timer = startTimer(PUT_IF_ABSENT);
        return updateAndGet(key, Match.ifNull(), Match.any(), value)
                .whenComplete((r, e) -> timer.stop())
                .thenApply(v -> v.oldValue());
    }

    @Override
    public CompletableFuture<Boolean> remove(K key, V value) {
        checkNotNull(key, ERROR_NULL_KEY);
        checkNotNull(value, ERROR_NULL_VALUE);
        final OperationTimer timer = startTimer(REMOVE);
        return updateAndGet(key, Match.ifValue(value), Match.any(), null)
                .whenComplete((r, e) -> timer.stop())
                .thenApply(v -> v.updated());
    }

    @Override
    public CompletableFuture<Boolean> remove(K key, long version) {
        checkNotNull(key, ERROR_NULL_KEY);
        final OperationTimer timer = startTimer(REMOVE);
        return updateAndGet(key, Match.any(), Match.ifValue(version), null)
                .whenComplete((r, e) -> timer.stop())
                .thenApply(v -> v.updated());
    }

    @Override
    public CompletableFuture<Boolean> replace(K key, V oldValue, V newValue) {
        checkNotNull(key, ERROR_NULL_KEY);
        checkNotNull(oldValue, ERROR_NULL_VALUE);
        checkNotNull(newValue, ERROR_NULL_VALUE);
        final OperationTimer timer = startTimer(REPLACE);
        return updateAndGet(key, Match.ifValue(oldValue), Match.any(), newValue)
                .whenComplete((r, e) -> timer.stop())
                .thenApply(v -> v.updated());
    }

    @Override
    public CompletableFuture<Boolean> replace(K key, long oldVersion, V newValue) {
        final OperationTimer timer = startTimer(REPLACE);
        return updateAndGet(key, Match.any(), Match.ifValue(oldVersion), newValue)
                .whenComplete((r, e) -> timer.stop())
                .thenApply(v -> v.updated());
    }

    private Map.Entry<K, Versioned<V>> mapRawEntry(Map.Entry<String, Versioned<byte[]>> e) {
        return Maps.immutableEntry(dK(e.getKey()), e.getValue().<V>map(serializer::decode));
    }

    private CompletableFuture<UpdateResult<K, V>> updateAndGet(K key,
            Match<V> oldValueMatch,
            Match<Long> oldVersionMatch,
            V value) {
        checkIfUnmodifiable();
        return database.mapUpdate(name,
                keyCache.getUnchecked(key),
                oldValueMatch.map(serializer::encode),
                oldVersionMatch,
                value == null ? null : serializer.encode(value))
                .thenApply(this::unwrapResult)
                .thenApply(r -> r.<K, V>map(this::dK, serializer::decode))
                .whenComplete((r, e) -> {
                    if (r != null && e == null && !database.hasChangeNotificationSupport()) {
                        notifyListeners(r.toMapEvent());
                    }
                });
    }

    private <T> T unwrapResult(Result<T> result) {
        if (result.status() == Result.Status.LOCKED) {
            throw new ConsistentMapException.ConcurrentModification();
        } else if (result.success()) {
            return result.value();
        } else {
            throw new IllegalStateException("Must not be here");
        }
    }

    private void checkIfUnmodifiable() {
        if (readOnly) {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public void addListener(MapEventListener<K, V> listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(MapEventListener<K, V> listener) {
        listeners.remove(listener);
    }

    protected void notifyListeners(MapEvent<K, V> event) {
        try {
            if (event != null) {
                notifyLocalListeners(event);
                notifyRemoteListeners(event);
            }
        } catch (Exception e) {
            log.warn("Failure notifying listeners about {}", event, e);
        }
    }

    protected void notifyLocalListeners(MapEvent<K, V> event) {
        if (event != null) {
            listeners.forEach(listener -> listener.event(event));
        }
    }

    protected void notifyRemoteListeners(MapEvent<K, V> event) {
        if (eventPublisher != null) {
            eventPublisher.accept(event);
        }
    }

    private OperationTimer startTimer(String op) {
        //check if timer exist, if it doesn't creates it
        final Timer currTimer = perMapOpTimers.computeIfAbsent(op, timer ->
                metricsService.createTimer(metricsComponent, metricsFeature, op));
        perOpTimers.computeIfAbsent(op, timer -> metricsService.createTimer(metricsComponent, wildcard, op));
        //starts timer
        return new OperationTimer(currTimer.time(), op);
    }

    private class OperationTimer {
        private final Timer.Context context;
        private final String operation;

        public OperationTimer(Timer.Context context, String operation) {
            this.context = context;
            this.operation = operation;
        }

        public void stop() {
            //Stop and updates timer with specific measurements per map, per operation
            final long time = context.stop();
            //updates timer with aggregated measurements per map
            perOpTimers.get(operation).update(time, TimeUnit.NANOSECONDS);
            //updates timer with aggregated measurements per map
            perMapTimer.update(time, TimeUnit.NANOSECONDS);
            //updates timer with aggregated measurements per all Consistent Maps
            cMapTimer.update(time, TimeUnit.NANOSECONDS);
        }
    }
}