/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onosproject.store.primitives;

import com.google.common.base.Throwables;
import com.google.common.collect.Multiset;
import org.onosproject.store.service.AsyncConsistentMultimap;
import org.onosproject.store.service.AsyncIterator;
import org.onosproject.store.service.ConsistentMapException;
import org.onosproject.store.service.ConsistentMultimap;
import org.onosproject.store.service.MultimapEventListener;
import org.onosproject.store.service.Synchronous;
import org.onosproject.store.service.Versioned;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Implementation of {@link ConsistentMultimap} providing synchronous access to
 * {@link AsyncConsistentMultimap}.
 */
public class DefaultConsistentMultimap<K, V>
        extends Synchronous<AsyncConsistentMultimap<K, V>>
        implements ConsistentMultimap<K, V> {

    private final AsyncConsistentMultimap<K, V> asyncMultimap;
    private final long operationTimeoutMillis;

    public DefaultConsistentMultimap(
            AsyncConsistentMultimap<K, V> asyncMultimap,
            long operationTimeoutMillis) {
        super(asyncMultimap);
        this.asyncMultimap = asyncMultimap;
        this.operationTimeoutMillis = operationTimeoutMillis;
    }

    @Override
    public int size() {
        return complete(asyncMultimap.size());
    }

    @Override
    public boolean isEmpty() {
        return complete(asyncMultimap.isEmpty());
    }

    @Override
    public boolean containsKey(K key) {
        return complete(asyncMultimap.containsKey(key));
    }

    @Override
    public boolean containsValue(V value) {
        return complete(asyncMultimap.containsValue(value));
    }

    @Override
    public boolean containsEntry(K key, V value) {
        return complete(asyncMultimap.containsEntry(key, value));
    }

    @Override
    public boolean put(K key, V value) {
        return complete(asyncMultimap.put(key, value));
    }

    @Override
    public Versioned<Collection<? extends V>> putAndGet(K key, V value) {
        return complete(asyncMultimap.putAndGet(key, value));
    }

    @Override
    public boolean remove(K key, V value) {
        return complete(asyncMultimap.remove(key, value));
    }

    @Override
    public Versioned<Collection<? extends V>> removeAndGet(K key, V value) {
        return complete(asyncMultimap.removeAndGet(key, value));
    }

    @Override
    public boolean removeAll(K key, Collection<? extends V> values) {
        return complete(asyncMultimap.removeAll(key, values));
    }

    @Override
    public Versioned<Collection<? extends V>> removeAll(K key) {
        return complete(asyncMultimap.removeAll(key));
    }

    @Override
    public boolean removeAll(Map<K, Collection<? extends V>> mapping) {
        return complete(asyncMultimap.removeAll(mapping));
    }

    @Override
    public boolean putAll(K key, Collection<? extends V> values) {
        return complete(asyncMultimap.putAll(key, values));
    }

    @Override
    public boolean putAll(Map<K, Collection<? extends V>> mapping) {
        return complete(asyncMultimap.putAll(mapping));
    }

    @Override
    public Versioned<Collection<? extends V>> replaceValues(
            K key, Collection<V> values) {
        return complete(asyncMultimap.replaceValues(key, values));
    }

    @Override
    public void clear() {
        complete(asyncMultimap.clear());
    }

    @Override
    public Versioned<Collection<? extends V>> get(K key) {
        return complete(asyncMultimap.get(key));
    }

    @Override
    public Set<K> keySet() {
        return complete(asyncMultimap.keySet());
    }

    @Override
    public Multiset<K> keys() {
        return complete(asyncMultimap.keys());
    }

    @Override
    public Multiset<V> values() {
        return complete(asyncMultimap.values());
    }

    @Override
    public Collection<Map.Entry<K, V>> entries() {
        return complete(asyncMultimap.entries());
    }

    @Override
    public Iterator<Map.Entry<K, V>> iterator() {
        return new DefaultIterator<>(complete(asyncMultimap.iterator()));
    }

    @Override
    public Map<K, Collection<V>> asMap() {
        throw new UnsupportedOperationException("This operation is not yet " +
                                                        "supported.");
        //FIXME implement this when a new version of ConsistentMapBackedJavaMap is made for multimaps
    }

    @Override
    public void addListener(MultimapEventListener<K, V> listener, Executor executor) {
        complete(asyncMultimap.addListener(listener, executor));
    }

    @Override
    public void removeListener(MultimapEventListener<K, V> listener) {
        complete(asyncMultimap.removeListener(listener));
    }

    private class DefaultIterator<K, V> implements Iterator<Map.Entry<K, V>> {
        private final AsyncIterator<Map.Entry<K, V>> iterator;

        public DefaultIterator(AsyncIterator<Map.Entry<K, V>> iterator) {
            this.iterator = iterator;
        }

        @Override
        public boolean hasNext() {
            return complete(iterator.hasNext());
        }

        @Override
        public Map.Entry<K, V> next() {
            return complete(iterator.next());
        }
    }

    private <T> T complete(CompletableFuture<T> future) {
        try {
            return future.get(operationTimeoutMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ConsistentMapException.Interrupted();
        } catch (TimeoutException e) {
            throw new ConsistentMapException.Timeout();
        } catch (ExecutionException e) {
            Throwables.throwIfUnchecked(e.getCause());
            throw new ConsistentMapException(e.getCause());
        }
    }
}
