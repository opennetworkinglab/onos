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
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.onosproject.store.serializers.StoreSerializer;

import com.hazelcast.core.TransactionalMap;
import com.hazelcast.query.Predicate;

/**
 * Wrapper around TransactionalMap&lt;byte[], byte[]&gt; which serializes/deserializes
 * key and value using StoreSerializer.
 *
 * @param <K> key type
 * @param <V> value type
 */
public class STxMap<K, V> implements TransactionalMap<K, V> {

    private final TransactionalMap<byte[], byte[]> m;
    private final StoreSerializer serializer;

    /**
     * Creates a STxMap instance.
     *
     * @param baseMap base IMap to use
     * @param serializer serializer to use for both key and value
     */
    public STxMap(TransactionalMap<byte[], byte[]> baseMap, StoreSerializer serializer) {
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
    public V get(Object key) {
        return deserializeVal(m.get(serializeKey(key)));
    }

    @Override
    public V getForUpdate(Object key) {
        return deserializeVal(m.getForUpdate(serializeKey(key)));
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
    public V put(K key, V value, long ttl, TimeUnit timeunit) {
        return deserializeVal(m.put(serializeKey(key), serializeVal(value), ttl, timeunit));
    }

    @Override
    public V putIfAbsent(K key, V value) {
        return deserializeVal(m.putIfAbsent(serializeKey(key), serializeVal(value)));
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
    public Set<K> keySet() {
        return deserializeKeySet(m.keySet());
    }

    @Override
    public Collection<V> values() {
        return deserializeVals(m.values());
    }

    @Deprecated // marking method not implemented
    @SuppressWarnings("rawtypes")
    @Override
    public Set<K> keySet(Predicate predicate) {
        throw new UnsupportedOperationException();
    }

    @Deprecated // marking method not implemented
    @SuppressWarnings("rawtypes")
    @Override
    public Collection<V> values(Predicate predicate) {
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

    private Set<K> deserializeKeySet(Set<byte[]> keys) {
        Set<K> dsk = new HashSet<>(keys.size());
        for (byte[] key : keys) {
            dsk.add(deserializeKey(key));
        }
        return dsk;
    }

    private Collection<V> deserializeVals(Collection<byte[]> vals) {
        Collection<V> dsl = new ArrayList<>(vals.size());
        for (byte[] val : vals) {
            dsl.add(deserializeVal(val));
        }
        return dsl;
    }
}
