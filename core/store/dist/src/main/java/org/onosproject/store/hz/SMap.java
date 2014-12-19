/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.store.hz;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.tuple.Pair;
import org.onosproject.store.serializers.StoreSerializer;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryListener;
import com.hazelcast.core.EntryView;
import com.hazelcast.core.ExecutionCallback;
import com.hazelcast.core.IMap;
import com.hazelcast.core.MapEvent;
import com.hazelcast.map.EntryProcessor;
import com.hazelcast.map.MapInterceptor;
import com.hazelcast.mapreduce.JobTracker;
import com.hazelcast.mapreduce.aggregation.Aggregation;
import com.hazelcast.mapreduce.aggregation.Supplier;
import com.hazelcast.monitor.LocalMapStats;
import com.hazelcast.query.Predicate;

/**
 * Wrapper around IMap&lt;byte[], byte[]&gt; which serializes/deserializes
 * key and value using StoreSerializer.
 *
 * @param <K> key type
 * @param <V> value type
 */
public class SMap<K, V> implements IMap<K, V> {

    private final IMap<byte[], byte[]> m;
    private final StoreSerializer serializer;

    /**
     * Creates a SMap instance.
     *
     * @param baseMap base IMap to use
     * @param serializer serializer to use for both key and value
     */
    public SMap(IMap<byte[], byte[]> baseMap, StoreSerializer serializer) {
        this.m = checkNotNull(baseMap);
        this.serializer = checkNotNull(serializer);
    }

    @Override
    public int size() {
        return m.size();
    }

    @Override
    public boolean isEmpty() {
        return m.isEmpty();
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        Map<byte[], byte[]> sm = new IdentityHashMap<>(map.size());
        for (java.util.Map.Entry<? extends K, ? extends V> e : map.entrySet()) {
            sm.put(serializeKey(e.getKey()), serializeVal(e.getValue()));
        }
        m.putAll(sm);
    }

    @Deprecated
    @Override
    public Object getId() {
        return m.getId();
    }

    @Override
    public String getPartitionKey() {
        return m.getPartitionKey();
    }

    @Override
    public String getName() {
        return m.getName();
    }

    @Override
    public String getServiceName() {
        return m.getServiceName();
    }

    @Override
    public void destroy() {
        m.destroy();
    }

    @Override
    public boolean containsKey(Object key) {
        return m.containsKey(serializeKey(key));
    }

    @Override
    public boolean containsValue(Object value) {
        return m.containsValue(serializeVal(value));
    }

    @Override
    public V get(Object key) {
        return deserializeVal(m.get(serializeKey(key)));
    }

    @Override
    public V put(K key, V value) {
        return deserializeVal(m.put(serializeKey(key), serializeVal(value)));
    }

    @Override
    public V remove(Object key) {
        return deserializeVal(m.remove(serializeKey(key)));
    }

    @Override
    public boolean remove(Object key, Object value) {
        return m.remove(serializeKey(key), serializeVal(value));
    }

    @Override
    public void delete(Object key) {
        m.delete(serializeKey(key));
    }

    @Override
    public void flush() {
        m.flush();
    }

    @Override
    public Map<K, V> getAll(Set<K> keys) {
        Set<byte[]> sk = serializeKeySet(keys);
        Map<byte[], byte[]> bm = m.getAll(sk);
        Map<K, V> dsm = new HashMap<>(bm.size());
        for (java.util.Map.Entry<byte[], byte[]> e : bm.entrySet()) {
            dsm.put(deserializeKey(e.getKey()), deserializeVal(e.getValue()));
        }
        return dsm;
    }

    @Override
    public void loadAll(boolean replaceExistingValues) {
        m.loadAll(replaceExistingValues);
    }

    @Override
    public void loadAll(Set<K> keys, boolean replaceExistingValues) {
        Set<byte[]> sk = serializeKeySet(keys);
        m.loadAll(sk, replaceExistingValues);
    }

    @Override
    public void clear() {
        m.clear();
    }

    @Override
    public Future<V> getAsync(K key) {
        Future<byte[]> f = m.getAsync(serializeKey(key));
        return Futures.lazyTransform(f, new DeserializeVal());
    }

    @Override
    public Future<V> putAsync(K key, V value) {
        Future<byte[]> f = m.putAsync(serializeKey(key), serializeVal(value));
        return Futures.lazyTransform(f, new DeserializeVal());
    }

    @Override
    public Future<V> putAsync(K key, V value, long ttl, TimeUnit timeunit) {
        Future<byte[]> f = m.putAsync(serializeKey(key), serializeVal(value), ttl, timeunit);
        return Futures.lazyTransform(f, new DeserializeVal());
    }

    @Override
    public Future<V> removeAsync(K key) {
        Future<byte[]> f = m.removeAsync(serializeKey(key));
        return Futures.lazyTransform(f, new DeserializeVal());
    }

    @Override
    public boolean tryRemove(K key, long timeout, TimeUnit timeunit) {
        return m.tryRemove(serializeKey(key), timeout, timeunit);
    }

    @Override
    public boolean tryPut(K key, V value, long timeout, TimeUnit timeunit) {
        return m.tryPut(serializeKey(key), serializeVal(value), timeout, timeunit);
    }

    @Override
    public V put(K key, V value, long ttl, TimeUnit timeunit) {
        return deserializeVal(m.put(serializeKey(key), serializeVal(value), ttl, timeunit));
    }

    @Override
    public void putTransient(K key, V value, long ttl, TimeUnit timeunit) {
        m.putTransient(serializeKey(key), serializeVal(value), ttl, timeunit);
    }

    @Override
    public V putIfAbsent(K key, V value) {
        return deserializeVal(m.putIfAbsent(serializeKey(key), serializeVal(value)));
    }

    @Override
    public V putIfAbsent(K key, V value, long ttl, TimeUnit timeunit) {
        return deserializeVal(m.putIfAbsent(serializeKey(key), serializeVal(value), ttl, timeunit));
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        return m.replace(serializeKey(key), serializeVal(oldValue), serializeVal(newValue));
    }

    @Override
    public V replace(K key, V value) {
        return deserializeVal(m.replace(serializeKey(key), serializeVal(value)));
    }

    @Override
    public void set(K key, V value) {
        m.set(serializeKey(key), serializeVal(value));
    }

    @Override
    public void set(K key, V value, long ttl, TimeUnit timeunit) {
        m.set(serializeKey(key), serializeVal(value), ttl, timeunit);
    }

    @Override
    public void lock(K key) {
        m.lock(serializeKey(key));
     }

    @Override
    public void lock(K key, long leaseTime, TimeUnit timeUnit) {
        m.lock(serializeKey(key), leaseTime, timeUnit);
    }

    @Override
    public boolean isLocked(K key) {
        return m.isLocked(serializeKey(key));
    }

    @Override
    public boolean tryLock(K key) {
        return m.tryLock(serializeKey(key));
    }

    @Override
    public boolean tryLock(K key, long time, TimeUnit timeunit)
            throws InterruptedException {
        return m.tryLock(serializeKey(key), time, timeunit);
    }

    @Override
    public void unlock(K key) {
        m.unlock(serializeKey(key));
    }

    @Override
    public void forceUnlock(K key) {
        m.forceUnlock(serializeKey(key));
    }

    @Override
    public String addLocalEntryListener(EntryListener<K, V> listener) {
        return m.addLocalEntryListener(new BaseEntryListener(listener));
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated not implemented yet
     * @throws UnsupportedOperationException not implemented yet
     */
    @Deprecated
    @Override
    public String addLocalEntryListener(EntryListener<K, V> listener,
            Predicate<K, V> predicate, boolean includeValue) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated not implemented yet
     * @throws UnsupportedOperationException not implemented yet
     */
    @Deprecated
    @Override
    public String addLocalEntryListener(EntryListener<K, V> listener,
            Predicate<K, V> predicate, K key, boolean includeValue) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated not implemented yet
     * @throws UnsupportedOperationException not implemented yet
     */
    @Deprecated
    @Override
    public String addInterceptor(MapInterceptor interceptor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeInterceptor(String id) {
        m.removeInterceptor(id);
    }

    @Override
    public String addEntryListener(EntryListener<K, V> listener,
            boolean includeValue) {
        return m.addEntryListener(new BaseEntryListener(listener), includeValue);
    }

    @Override
    public boolean removeEntryListener(String id) {
        return m.removeEntryListener(id);
    }

    @Override
    public String addEntryListener(EntryListener<K, V> listener, K key,
            boolean includeValue) {
        return m.addEntryListener(new BaseEntryListener(listener),
                serializeKey(key), includeValue);
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated not implemented yet
     * @throws UnsupportedOperationException not implemented yet
     */
    @Deprecated
    @Override
    public String addEntryListener(EntryListener<K, V> listener,
            Predicate<K, V> predicate, boolean includeValue) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated not implemented yet
     * @throws UnsupportedOperationException not implemented yet
     */
    @Deprecated
    @Override
    public String addEntryListener(EntryListener<K, V> listener,
            Predicate<K, V> predicate, K key, boolean includeValue) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated not implemented yet
     * @throws UnsupportedOperationException not implemented yet
     */
    @Deprecated
    @Override
    public EntryView<K, V> getEntryView(K key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean evict(K key) {
        return m.evict(serializeKey(key));
    }

    @Override
    public void evictAll() {
        m.evictAll();
    }

    @Override
    public Set<K> keySet() {
        return deserializeKeySet(m.keySet());
    }

    @Override
    public Collection<V> values() {
        return deserializeVal(m.values());
    }

    @Override
    public Set<java.util.Map.Entry<K, V>> entrySet() {
        return deserializeEntrySet(m.entrySet());
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated not implemented yet
     * @throws UnsupportedOperationException not implemented yet
     */
    @Deprecated
    @SuppressWarnings("rawtypes")
    @Override
    public Set<K> keySet(Predicate predicate) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated not implemented yet
     * @throws UnsupportedOperationException not implemented yet
     */
    @Deprecated
    @SuppressWarnings("rawtypes")
    @Override
    public Set<java.util.Map.Entry<K, V>> entrySet(Predicate predicate) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated not implemented yet
     * @throws UnsupportedOperationException not implemented yet
     */
    @Deprecated
    @SuppressWarnings("rawtypes")
    @Override
    public Collection<V> values(Predicate predicate) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<K> localKeySet() {
        return deserializeKeySet(m.localKeySet());
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated not implemented yet
     * @throws UnsupportedOperationException not implemented yet
     */
    @Deprecated
    @SuppressWarnings("rawtypes")
    @Override
    public Set<K> localKeySet(Predicate predicate) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated not implemented yet
     * @throws UnsupportedOperationException not implemented yet
     */
    @Deprecated
    @Override
    public void addIndex(String attribute, boolean ordered) {
        throw new UnsupportedOperationException();
    }

    @Override
    public LocalMapStats getLocalMapStats() {
        return m.getLocalMapStats();
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated not implemented yet
     * @throws UnsupportedOperationException not implemented yet
     */
    @Deprecated
    @SuppressWarnings("rawtypes")
    @Override
    public Object executeOnKey(K key, EntryProcessor entryProcessor) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated not implemented yet
     * @throws UnsupportedOperationException not implemented yet
     */
    @Deprecated
    @SuppressWarnings("rawtypes")
    @Override
    public Map<K, Object> executeOnKeys(Set<K> keys,
            EntryProcessor entryProcessor) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated not implemented yet
     * @throws UnsupportedOperationException not implemented yet
     */
    @Deprecated
    @SuppressWarnings("rawtypes")
    @Override
    public void submitToKey(K key, EntryProcessor entryProcessor,
            ExecutionCallback callback) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated not implemented yet
     * @throws UnsupportedOperationException not implemented yet
     */
    @Deprecated
    @SuppressWarnings("rawtypes")
    @Override
    public Future submitToKey(K key, EntryProcessor entryProcessor) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated not implemented yet
     * @throws UnsupportedOperationException not implemented yet
     */
    @Deprecated
    @SuppressWarnings("rawtypes")
    @Override
    public Map<K, Object> executeOnEntries(EntryProcessor entryProcessor) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated not implemented yet
     * @throws UnsupportedOperationException not implemented yet
     */
    @Deprecated
    @SuppressWarnings("rawtypes")
    @Override
    public Map<K, Object> executeOnEntries(EntryProcessor entryProcessor,
            Predicate predicate) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated not implemented yet
     * @throws UnsupportedOperationException not implemented yet
     */
    @Deprecated
    @Override
    public <SuppliedValue, Result> Result aggregate(
            Supplier<K, V, SuppliedValue> supplier,
            Aggregation<K, SuppliedValue, Result> aggregation) {

        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated not implemented yet
     * @throws UnsupportedOperationException not implemented yet
     */
    @Deprecated
    @Override
    public <SuppliedValue, Result> Result aggregate(
            Supplier<K, V, SuppliedValue> supplier,
            Aggregation<K, SuppliedValue, Result> aggregation,
            JobTracker jobTracker) {

        throw new UnsupportedOperationException();
    }

    private byte[] serializeKey(Object key) {
        return serializer.encode(key);
    }

    private K deserializeKey(byte[] key) {
        return serializer.decode(key);
    }

    private byte[] serializeVal(Object val) {
        return serializer.encode(val);
    }

    private V deserializeVal(byte[] val) {
        if (val == null) {
            return null;
        }
        return serializer.decode(val.clone());
    }

    private Set<byte[]> serializeKeySet(Set<K> keys) {
        Set<byte[]> sk = Collections.newSetFromMap(new IdentityHashMap<byte[], Boolean>(keys.size()));
        for (K key : keys) {
            sk.add(serializeKey(key));
        }
        return sk;
    }

    private Set<K> deserializeKeySet(Set<byte[]> keys) {
        Set<K> dsk = new HashSet<>(keys.size());
        for (byte[] key : keys) {
            dsk.add(deserializeKey(key));
        }
        return dsk;
    }

    private Collection<V> deserializeVal(Collection<byte[]> vals) {
        Collection<V> dsl = new ArrayList<>(vals.size());
        for (byte[] val : vals) {
            dsl.add(deserializeVal(val));
        }
        return dsl;
    }

    private Set<java.util.Map.Entry<K, V>> deserializeEntrySet(
                        Set<java.util.Map.Entry<byte[], byte[]>> entries) {

        Set<java.util.Map.Entry<K, V>> dse = new HashSet<>(entries.size());
        for (java.util.Map.Entry<byte[], byte[]> entry : entries) {
            dse.add(Pair.of(deserializeKey(entry.getKey()),
                            deserializeVal(entry.getValue())));
        }
        return dse;
    }

    private final class BaseEntryListener
        implements EntryListener<byte[], byte[]> {

            private final EntryListener<K, V> listener;

        public BaseEntryListener(EntryListener<K, V> listener) {
            this.listener = listener;
        }

        @Override
        public void mapEvicted(MapEvent event) {
            listener.mapEvicted(event);
        }

        @Override
        public void mapCleared(MapEvent event) {
            listener.mapCleared(event);
        }

        @Override
        public void entryUpdated(EntryEvent<byte[], byte[]> event) {
            EntryEvent<K, V> evt = new EntryEvent<K, V>(
                    event.getSource(),
                    event.getMember(),
                    event.getEventType().getType(),
                    deserializeKey(event.getKey()),
                    deserializeVal(event.getOldValue()),
                    deserializeVal(event.getValue()));

            listener.entryUpdated(evt);
        }

        @Override
        public void entryRemoved(EntryEvent<byte[], byte[]> event) {
            EntryEvent<K, V> evt = new EntryEvent<K, V>(
                    event.getSource(),
                    event.getMember(),
                    event.getEventType().getType(),
                    deserializeKey(event.getKey()),
                    deserializeVal(event.getOldValue()),
                    null);

            listener.entryRemoved(evt);
        }

        @Override
        public void entryEvicted(EntryEvent<byte[], byte[]> event) {
            EntryEvent<K, V> evt = new EntryEvent<K, V>(
                    event.getSource(),
                    event.getMember(),
                    event.getEventType().getType(),
                    deserializeKey(event.getKey()),
                    deserializeVal(event.getOldValue()),
                    deserializeVal(event.getValue()));

            listener.entryEvicted(evt);
        }

        @Override
        public void entryAdded(EntryEvent<byte[], byte[]> event) {
            EntryEvent<K, V> evt = new EntryEvent<K, V>(
                    event.getSource(),
                    event.getMember(),
                    event.getEventType().getType(),
                    deserializeKey(event.getKey()),
                    null,
                    deserializeVal(event.getValue()));

            listener.entryAdded(evt);
        }
    }

    private final class DeserializeVal implements Function<byte[], V> {
        @Override
        public V apply(byte[] input) {
            return deserializeVal(input);
        }
    }

}
