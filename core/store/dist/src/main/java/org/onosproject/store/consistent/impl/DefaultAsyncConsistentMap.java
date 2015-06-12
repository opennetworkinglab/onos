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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.onlab.util.HexString;
import org.onlab.util.Tools;
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

/**
 * AsyncConsistentMap implementation that is backed by a Raft consensus
 * based database.
 *
 * @param <K> type of key.
 * @param <V> type of value.
 */
public class DefaultAsyncConsistentMap<K, V> implements AsyncConsistentMap<K, V> {

    private final String name;
    private final Database database;
    private final Serializer serializer;
    private final boolean readOnly;
    private final Consumer<MapEvent<K, V>> eventPublisher;

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
            Database database,
            Serializer serializer,
            boolean readOnly,
            Consumer<MapEvent<K, V>> eventPublisher) {
        this.name = checkNotNull(name, "map name cannot be null");
        this.database = checkNotNull(database, "database cannot be null");
        this.serializer = checkNotNull(serializer, "serializer cannot be null");
        this.readOnly = readOnly;
        this.eventPublisher = eventPublisher;
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

    @Override
    public CompletableFuture<Integer> size() {
        return database.size(name);
    }

    @Override
    public CompletableFuture<Boolean> isEmpty() {
        return database.isEmpty(name);
    }

    @Override
    public CompletableFuture<Boolean> containsKey(K key) {
        checkNotNull(key, ERROR_NULL_KEY);
        return database.containsKey(name, keyCache.getUnchecked(key));
    }

    @Override
    public CompletableFuture<Boolean> containsValue(V value) {
        checkNotNull(value, ERROR_NULL_VALUE);
        return database.containsValue(name, serializer.encode(value));
    }

    @Override
    public CompletableFuture<Versioned<V>> get(K key) {
        checkNotNull(key, ERROR_NULL_KEY);
        return database.get(name, keyCache.getUnchecked(key))
            .thenApply(v -> v != null
            ? new Versioned<>(serializer.decode(v.value()), v.version(), v.creationTime()) : null);
    }

    @Override
    public CompletableFuture<Versioned<V>> computeIfAbsent(K key,
            Function<? super K, ? extends V> mappingFunction) {
        return computeIf(key, Objects::isNull, (k, v) -> mappingFunction.apply(k));
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
        AtomicReference<MapEvent<K, V>> mapEvent = new AtomicReference<>();
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

            // if the computed value is null, remove current value if one exists.
            // throw an exception if concurrent modification is detected.
            if (computedValue.get() == null) {
                if (r1 != null) {
                    return remove(key, r1.version()).thenApply(result -> {
                        if (result) {
                            mapEvent.set(new MapEvent<>(name, MapEvent.Type.REMOVE, key, r1));
                            return null;
                        } else {
                            throw new ConsistentMapException.ConcurrentModification();
                        }
                    });
                } else {
                    return CompletableFuture.completedFuture(null);
                }
            } else {
                // replace current value; throw an exception if concurrent modification is detected
                if (r1 != null) {
                    return replaceAndGet(key, r1.version(), computedValue.get())
                            .thenApply(v -> {
                                if (v.isPresent()) {
                                    mapEvent.set(new MapEvent<>(name, MapEvent.Type.UPDATE, key, v.get()));
                                    return v.get();
                                } else {
                                    throw new ConsistentMapException.ConcurrentModification();
                                }
                            });
                } else {
                    return putIfAbsentAndGet(key, computedValue.get()).thenApply(result -> {
                        if (!result.isPresent()) {
                            throw new ConsistentMapException.ConcurrentModification();
                        } else {
                            mapEvent.set(new MapEvent<>(name, MapEvent.Type.INSERT, key, result.get()));
                            return result.get();
                        }
                    });
                }
            }
        }).whenComplete((result, error) -> notifyListeners(mapEvent.get()));
    }

    @Override
    public CompletableFuture<Versioned<V>> put(K key, V value) {
        checkNotNull(key, ERROR_NULL_KEY);
        checkNotNull(value, ERROR_NULL_VALUE);
        checkIfUnmodifiable();
        return database.put(name, keyCache.getUnchecked(key), serializer.encode(value))
                       .thenApply(this::unwrapResult)
                       .thenApply(v -> v != null
                       ? new Versioned<>(serializer.decode(v.value()), v.version(), v.creationTime()) : null);
    }

    @Override
    public CompletableFuture<Versioned<V>> putAndGet(K key, V value) {
        checkNotNull(key, ERROR_NULL_KEY);
        checkNotNull(value, ERROR_NULL_VALUE);
        checkIfUnmodifiable();
        return database.putAndGet(name, keyCache.getUnchecked(key), serializer.encode(value))
                .thenApply(this::unwrapResult)
                .thenApply(v -> {
                    Versioned<byte[]> rawNewValue = v.newValue();
                    return new Versioned<>(serializer.decode(rawNewValue.value()),
                            rawNewValue.version(),
                            rawNewValue.creationTime());
                });
    }

    @Override
    public CompletableFuture<Optional<Versioned<V>>> putIfAbsentAndGet(K key, V value) {
        checkNotNull(key, ERROR_NULL_KEY);
        checkNotNull(value, ERROR_NULL_VALUE);
        checkIfUnmodifiable();
        return database.putIfAbsentAndGet(name, keyCache.getUnchecked(key), serializer.encode(value))
                .thenApply(this::unwrapResult)
                .thenApply(v -> {
                    if (v.updated()) {
                        Versioned<byte[]> rawNewValue = v.newValue();
                        return Optional.of(new Versioned<>(serializer.decode(rawNewValue.value()),
                                                           rawNewValue.version(),
                                                           rawNewValue.creationTime()));
                    } else {
                        return Optional.empty();
                    }
                });
    }

    @Override
    public CompletableFuture<Versioned<V>> remove(K key) {
        checkNotNull(key, ERROR_NULL_KEY);
        checkIfUnmodifiable();
        return database.remove(name, keyCache.getUnchecked(key))
                .thenApply(this::unwrapResult)
                .thenApply(v -> v != null ? v.<V>map(serializer::decode) : null)
                .whenComplete((r, e) -> {
                    if (r != null) {
                        notifyListeners(new MapEvent<>(name, MapEvent.Type.REMOVE, key, r));
                    }
                });
    }

    @Override
    public CompletableFuture<Void> clear() {
        checkIfUnmodifiable();
        return database.clear(name).thenApply(this::unwrapResult);
    }

    @Override
    public CompletableFuture<Set<K>> keySet() {
        return database.keySet(name)
                .thenApply(s -> s
                .stream()
                .map(this::dK)
                .collect(Collectors.toSet()));
    }

    @Override
    public CompletableFuture<Collection<Versioned<V>>> values() {
        return database.values(name).thenApply(c -> c
            .stream()
            .map(v -> new Versioned<V>(serializer.decode(v.value()), v.version(), v.creationTime()))
            .collect(Collectors.toList()));
    }

    @Override
    public CompletableFuture<Set<Entry<K, Versioned<V>>>> entrySet() {
        return database.entrySet(name).thenApply(s -> s
                .stream()
                .map(this::fromRawEntry)
                .collect(Collectors.toSet()));
    }

    @Override
    public CompletableFuture<Versioned<V>> putIfAbsent(K key, V value) {
        checkNotNull(key, ERROR_NULL_KEY);
        checkNotNull(value, ERROR_NULL_VALUE);
        checkIfUnmodifiable();
        AtomicReference<MapEvent<K, V>> event = new AtomicReference<>();
        return database.putIfAbsentAndGet(name, keyCache.getUnchecked(key), serializer.encode(value))
                .thenApply(this::unwrapResult)
                .whenComplete((r, e) -> {
                    if (r != null && r.updated()) {
                        event.set(new MapEvent<K, V>(name,
                                                 MapEvent.Type.INSERT,
                                                 key,
                                                 r.newValue().<V>map(serializer::decode)));
                    }
                })
                .thenApply(v -> v.updated() ? null : v.oldValue().<V>map(serializer::decode))
                .whenComplete((r, e) -> notifyListeners(event.get()));
    }

    @Override
    public CompletableFuture<Boolean> remove(K key, V value) {
        checkNotNull(key, ERROR_NULL_KEY);
        checkNotNull(value, ERROR_NULL_VALUE);
        checkIfUnmodifiable();
        return database.remove(name, keyCache.getUnchecked(key), serializer.encode(value))
                       .thenApply(this::unwrapResult);
    }

    @Override
    public CompletableFuture<Boolean> remove(K key, long version) {
        checkNotNull(key, ERROR_NULL_KEY);
        checkIfUnmodifiable();
        return database.remove(name, keyCache.getUnchecked(key), version)
                       .thenApply(this::unwrapResult);

    }

    @Override
    public CompletableFuture<Boolean> replace(K key, V oldValue, V newValue) {
        checkNotNull(key, ERROR_NULL_KEY);
        checkNotNull(newValue, ERROR_NULL_VALUE);
        checkIfUnmodifiable();
        byte[] existing = oldValue != null ? serializer.encode(oldValue) : null;
        return database.replace(name, keyCache.getUnchecked(key), existing, serializer.encode(newValue))
                       .thenApply(this::unwrapResult);
    }

    @Override
    public CompletableFuture<Boolean> replace(K key, long oldVersion, V newValue) {
        return replaceAndGet(key, oldVersion, newValue).thenApply(Optional::isPresent);
    }

    @Override
    public CompletableFuture<Optional<Versioned<V>>> replaceAndGet(K key, long oldVersion, V newValue) {
        checkNotNull(key, ERROR_NULL_KEY);
        checkNotNull(newValue, ERROR_NULL_VALUE);
        checkIfUnmodifiable();
        return database.replaceAndGet(name,
                                      keyCache.getUnchecked(key),
                                      oldVersion,
                                      serializer.encode(newValue))
                       .thenApply(this::unwrapResult)
                       .thenApply(v -> {
                                   if (v.updated()) {
                                       Versioned<byte[]> rawNewValue = v.newValue();
                                       return Optional.of(new Versioned<>(serializer.decode(rawNewValue.value()),
                                                                            rawNewValue.version(),
                                                                            rawNewValue.creationTime()));
                                   } else {
                                       return Optional.empty();
                                   }
                       });
    }

    private Map.Entry<K, Versioned<V>> fromRawEntry(Map.Entry<String, Versioned<byte[]>> e) {
        return Pair.of(
                dK(e.getKey()),
                new Versioned<>(
                        serializer.decode(e.getValue().value()),
                        e.getValue().version(),
                        e.getValue().creationTime()));
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
        listeners.forEach(listener -> listener.event(event));
    }

    protected void notifyRemoteListeners(MapEvent<K, V> event) {
        if (eventPublisher != null) {
            eventPublisher.accept(event);
        }
    }
}