/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.vtnrsc.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

import org.onlab.util.KryoNamespace;
import org.onosproject.cluster.NodeId;
import org.onosproject.store.Timestamp;

import static org.onosproject.store.service.EventuallyConsistentMapEvent.Type.*;
import org.onosproject.store.service.EventuallyConsistentMapListener;
import org.onosproject.store.service.EventuallyConsistentMapEvent;
import org.onosproject.store.service.EventuallyConsistentMapBuilder;
import org.onosproject.store.service.EventuallyConsistentMap;

/**
 * Testing version of an Eventually Consistent Map.
 */

public final class VtnEventuallyConsistentMapTest<K, V> extends VtnEventuallyConsistentMapAdapter<K, V> {

    private final HashMap<K, V> map;
    private final String mapName;
    private final List<EventuallyConsistentMapListener<K, V>> listeners;
    private final BiFunction<K, V, Collection<NodeId>> peerUpdateFunction;

    private VtnEventuallyConsistentMapTest(String mapName,
            BiFunction<K, V, Collection<NodeId>> peerUpdateFunction) {
        map = new HashMap<>();
        listeners = new LinkedList<>();
        this.mapName = mapName;
        this.peerUpdateFunction = peerUpdateFunction;
    }

    /**
     * Notify all listeners of an event.
     */
    private void notifyListeners(EventuallyConsistentMapEvent<K, V> event) {
        listeners.forEach(
                listener -> listener.event(event)
                );
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean containsKey(K key) {
        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(V value) {
        return map.containsValue(value);
    }

    @Override
    public V get(K key) {
        return map.get(key);
    }

    @Override
    public void put(K key, V value) {
        map.put(key, value);
        EventuallyConsistentMapEvent<K, V> addEvent =
                new EventuallyConsistentMapEvent<>(mapName, PUT, key, value);
        notifyListeners(addEvent);
        if (peerUpdateFunction != null) {
            peerUpdateFunction.apply(key, value);
        }
    }

    @Override
    public V remove(K key) {
        V result = map.remove(key);
        if (result != null) {
            EventuallyConsistentMapEvent<K, V> removeEvent =
                    new EventuallyConsistentMapEvent<>(mapName, REMOVE,
                            key, map.get(key));
            notifyListeners(removeEvent);
        }
        return result;
    }

    @Override
    public void remove(K key, V value) {
        boolean removed = map.remove(key, value);
        if (removed) {
            EventuallyConsistentMapEvent<K, V> removeEvent =
                    new EventuallyConsistentMapEvent<>(mapName, REMOVE, key, value);
            notifyListeners(removeEvent);
        }
    }

    @Override
    public V compute(K key, BiFunction<K, V, V> recomputeFunction) {
        return map.compute(key, recomputeFunction);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        map.putAll(m);
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public Set<K> keySet() {
        return map.keySet();
    }

    @Override
    public Collection<V> values() {
        return map.values();
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return map.entrySet();
    }

    public static <K, V> Builder<K, V> builder() {
        return new Builder<>();
    }

    @Override
    public void addListener(EventuallyConsistentMapListener<K, V> listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(EventuallyConsistentMapListener<K, V> listener) {
        listeners.remove(listener);
    }

    public static class Builder<K, V> implements EventuallyConsistentMapBuilder<K, V> {
        private String name;
        private BiFunction<K, V, Collection<NodeId>> peerUpdateFunction;

        @Override
        public EventuallyConsistentMapBuilder<K, V> withName(String name) {
            this.name = name;
            return this;
        }

        @Override
        public EventuallyConsistentMapBuilder<K, V> withSerializer(KryoNamespace.Builder serializerBuilder) {
            return this;
        }

        @Override
        public EventuallyConsistentMapBuilder<K, V> withSerializer(KryoNamespace serializer) {
            return this;
        }

        @Override
        public EventuallyConsistentMapBuilder<K, V>
        withTimestampProvider(BiFunction<K, V, Timestamp> timestampProvider) {
            return this;
        }

        @Override
        public EventuallyConsistentMapBuilder<K, V> withEventExecutor(ExecutorService executor) {
            return this;
        }

        @Override
        public EventuallyConsistentMapBuilder<K, V> withCommunicationExecutor(ExecutorService executor) {
            return this;
        }

        @Override
        public EventuallyConsistentMapBuilder<K, V> withBackgroundExecutor(ScheduledExecutorService executor) {
            return this;
        }

        @Override
        public EventuallyConsistentMapBuilder<K, V>
        withPeerUpdateFunction(BiFunction<K, V, Collection<NodeId>> peerUpdateFunction) {
            this.peerUpdateFunction = peerUpdateFunction;
            return this;
        }

        @Override
        public EventuallyConsistentMapBuilder<K, V> withTombstonesDisabled() {
            return this;
        }

        @Override
        public EventuallyConsistentMapBuilder<K, V> withAntiEntropyPeriod(long period, TimeUnit unit) {
            return this;
        }

        @Override
        public EventuallyConsistentMapBuilder<K, V> withFasterConvergence() {
            return this;
        }

        @Override
        public EventuallyConsistentMapBuilder<K, V> withPersistence() {
            return this;
        }

        @Override
        public EventuallyConsistentMap<K, V> build() {
            if (name == null) {
                name = "test";
            }
            return new VtnEventuallyConsistentMapTest<>(name, peerUpdateFunction);
        }
    }

}

