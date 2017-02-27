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
package org.onosproject.store.service;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multiset;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Implementation to test ConsistentMultimap. Very limited.
 * No listener notification. Some methods (still not required) not implemented.
 * @param <K> the key type
 * @param <V> the value type
 */
public class TestConsistentMultimap<K, V> implements ConsistentMultimap<K, V> {

    private String name;
    private HashMultimap<K, Versioned<V>> innermap;
    private AtomicLong counter = new AtomicLong();

    public TestConsistentMultimap() {
        this.innermap = HashMultimap.create();
    }

    private Versioned<V> version(V v) {
        return new Versioned<>(v, counter.incrementAndGet(), System.currentTimeMillis());
    }

    @Override
    public int size() {
        return innermap.size();
    }

    @Override
    public boolean isEmpty() {
        return innermap.isEmpty();
    }

    @Override
    public boolean containsKey(K key) {
        return innermap.containsKey(key);
    }

    @Override
    public boolean containsValue(V value) {
        return innermap.containsValue(value);
    }

    @Override
    public boolean containsEntry(K key, V value) {
        return innermap.containsEntry(key, value);
    }

    @Override
    public boolean put(K key, V value) {
        return innermap.put(key, version(value));
    }

    @Override
    public boolean remove(K key, V value) {
        return innermap.remove(key, value);
    }

    @Override
    public boolean removeAll(K key, Collection<? extends V> values) {
        return false;
    }

    @Override
    public Versioned<Collection<? extends V>> removeAll(K key) {
        return null;
    }

    @Override
    public boolean putAll(K key, Collection<? extends V> values) {
        return false;
    }

    @Override
    public Versioned<Collection<? extends V>> replaceValues(K key, Collection<V> values) {
        return null;
    }

    @Override
    public void clear() {
        innermap.clear();
    }

    @Override
    public Versioned<Collection<? extends V>> get(K key) {
        return (Versioned<Collection<? extends V>>) innermap.get(key);
    }

    @Override
    public Set<K> keySet() {
        return innermap.keySet();
    }

    @Override
    public Multiset<K> keys() {
        return innermap.keys();
    }

    @Override
    public Multiset<V> values() {
        return null;
    }

    @Override
    public Collection<Map.Entry<K, V>> entries() {
        return null;
    }

    @Override
    public Map<K, Collection<V>> asMap() {
        return null;
    }

    @Override
    public void addListener(MultimapEventListener<K, V> listener, Executor executor) {
    }

    @Override
    public void removeListener(MultimapEventListener<K, V> listener) {
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public Type primitiveType() {
        return null;
    }

    public static TestConsistentMultimap.Builder builder() {
        return new TestConsistentMultimap.Builder();
    }

    public static class Builder<K, V> extends ConsistentMultimapBuilder<K, V> {

        @Override
        public AsyncConsistentMultimap<K, V> buildMultimap() {
            return null;
        }

        @Override
        public ConsistentMultimap<K, V> build() {
            return new TestConsistentMultimap<K, V>();
        }
    }

}
