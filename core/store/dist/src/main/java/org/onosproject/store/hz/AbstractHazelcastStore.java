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

import com.google.common.base.Optional;
import com.google.common.cache.LoadingCache;
import com.hazelcast.core.EntryAdapter;
import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.MapEvent;
import com.hazelcast.core.Member;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.event.Event;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.StoreDelegate;
import org.onosproject.store.serializers.KryoSerializer;
import org.onosproject.store.serializers.StoreSerializer;
import org.slf4j.Logger;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Abstraction of a distributed store based on Hazelcast.
 */
@Component
public abstract class AbstractHazelcastStore<E extends Event, D extends StoreDelegate<E>>
        extends AbstractStore<E, D> {

    protected final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StoreService storeService;

    protected StoreSerializer serializer;

    protected HazelcastInstance theInstance;

    @Activate
    public void activate() {
        serializer = new KryoSerializer();
        theInstance = storeService.getHazelcastInstance();
    }

    /**
     * Serializes the specified object using the backing store service.
     *
     * @param obj object to be serialized
     * @return serialized object
     */
    protected byte[] serialize(Object obj) {
        return serializer.encode(obj);
    }

    /**
     * Deserializes the specified object using the backing store service.
     *
     * @param bytes bytes to be deserialized
     * @param <T>   type of object
     * @return deserialized object
     */
    protected <T> T deserialize(byte[] bytes) {
        return serializer.decode(bytes);
    }


    /**
     * An IMap entry listener, which reflects each remote event to the cache.
     *
     * @param <K> IMap key type after deserialization
     * @param <V> IMap value type after deserialization
     */
    public class RemoteCacheEventHandler<K, V> extends EntryAdapter<byte[], byte[]> {

        private final Member localMember;
        private LoadingCache<K, Optional<V>> cache;

        /**
         * Constructor.
         *
         * @param cache cache to update
         */
        public RemoteCacheEventHandler(LoadingCache<K, Optional<V>> cache) {
            this.localMember = theInstance.getCluster().getLocalMember();
            this.cache = checkNotNull(cache);
        }

        @Override
        public void mapCleared(MapEvent event) {
            if (localMember.equals(event.getMember())) {
                // ignore locally triggered event
                return;
            }
            cache.invalidateAll();
        }

        @Override
        public void entryAdded(EntryEvent<byte[], byte[]> event) {
            if (localMember.equals(event.getMember())) {
                // ignore locally triggered event
                return;
            }
            K key = deserialize(event.getKey());
            V newVal = deserialize(event.getValue());
            Optional<V> newValue = Optional.of(newVal);
            cache.asMap().putIfAbsent(key, newValue);
            onAdd(key, newVal);
        }

        @Override
        public void entryUpdated(EntryEvent<byte[], byte[]> event) {
            if (localMember.equals(event.getMember())) {
                // ignore locally triggered event
                return;
            }
            K key = deserialize(event.getKey());
            V oldVal = deserialize(event.getOldValue());
            Optional<V> oldValue = Optional.fromNullable(oldVal);
            V newVal = deserialize(event.getValue());
            Optional<V> newValue = Optional.of(newVal);
            cache.asMap().replace(key, oldValue, newValue);
            onUpdate(key, oldVal, newVal);
        }

        @Override
        public void entryRemoved(EntryEvent<byte[], byte[]> event) {
            if (localMember.equals(event.getMember())) {
                // ignore locally triggered event
                return;
            }
            K key = deserialize(event.getKey());
            V val = deserialize(event.getOldValue());
            cache.invalidate(key);
            onRemove(key, val);
        }

        /**
         * Cache entry addition hook.
         *
         * @param key    new key
         * @param newVal new value
         */
        protected void onAdd(K key, V newVal) {
        }

        /**
         * Cache entry update hook.
         *
         * @param key    new key
         * @param oldValue old value
         * @param newVal new value
         */
        protected void onUpdate(K key, V oldValue, V newVal) {
        }

        /**
         * Cache entry remove hook.
         *
         * @param key new key
         * @param val old value
         */
        protected void onRemove(K key, V val) {
        }
    }

    /**
     * Distributed object remote event entry listener.
     *
     * @param <K> Entry key type after deserialization
     * @param <V> Entry value type after deserialization
     */
    public class RemoteEventHandler<K, V> extends EntryAdapter<byte[], byte[]> {

        private final Member localMember;

        public RemoteEventHandler() {
            this.localMember = theInstance.getCluster().getLocalMember();
        }
        @Override
        public void entryAdded(EntryEvent<byte[], byte[]> event) {
            if (localMember.equals(event.getMember())) {
                // ignore locally triggered event
                return;
            }
            K key = deserialize(event.getKey());
            V newVal = deserialize(event.getValue());
            onAdd(key, newVal);
        }

        @Override
        public void entryRemoved(EntryEvent<byte[], byte[]> event) {
            if (localMember.equals(event.getMember())) {
                // ignore locally triggered event
                return;
            }
            K key = deserialize(event.getKey());
            V val = deserialize(event.getValue());
            onRemove(key, val);
        }

        @Override
        public void entryUpdated(EntryEvent<byte[], byte[]> event) {
            if (localMember.equals(event.getMember())) {
                // ignore locally triggered event
                return;
            }
            K key = deserialize(event.getKey());
            V oldVal = deserialize(event.getOldValue());
            V newVal = deserialize(event.getValue());
            onUpdate(key, oldVal, newVal);
        }

        /**
         * Remote entry addition hook.
         *
         * @param key    new key
         * @param newVal new value
         */
        protected void onAdd(K key, V newVal) {
        }

        /**
         * Remote entry update hook.
         *
         * @param key    new key
         * @param oldValue old value
         * @param newVal new value
         */
        protected void onUpdate(K key, V oldValue, V newVal) {
        }

        /**
         * Remote entry remove hook.
         *
         * @param key new key
         * @param val old value
         */
        protected void onRemove(K key, V val) {
        }
    }

}
