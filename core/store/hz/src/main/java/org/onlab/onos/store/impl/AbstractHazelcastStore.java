package org.onlab.onos.store.impl;

import com.google.common.base.Optional;
import com.google.common.cache.LoadingCache;
import com.hazelcast.core.EntryAdapter;
import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.MapEvent;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.onos.event.Event;
import org.onlab.onos.store.AbstractStore;
import org.onlab.onos.store.StoreDelegate;
import org.onlab.onos.store.common.StoreService;
import org.slf4j.Logger;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Abstraction of a distributed store based on Hazelcast.
 */
@Component(componentAbstract = true)
public abstract class AbstractHazelcastStore<E extends Event, D extends StoreDelegate<E>>
        extends AbstractStore<E, D> {

    protected final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StoreService storeService;

    protected HazelcastInstance theInstance;

    @Activate
    public void activate() {
        theInstance = storeService.getHazelcastInstance();
    }

    /**
     * Serializes the specified object using the backing store service.
     *
     * @param obj object to be serialized
     * @return serialized object
     */
    protected byte[] serialize(Object obj) {
        return storeService.serialize(obj);
    }

    /**
     * Deserializes the specified object using the backing store service.
     *
     * @param bytes bytes to be deserialized
     * @param <T>   type of object
     * @return deserialized object
     */
    protected <T> T deserialize(byte[] bytes) {
        return storeService.deserialize(bytes);
    }


    /**
     * An IMap entry listener, which reflects each remote event to the cache.
     *
     * @param <K> IMap key type after deserialization
     * @param <V> IMap value type after deserialization
     */
    public class RemoteEventHandler<K, V> extends EntryAdapter<byte[], byte[]> {

        private LoadingCache<K, Optional<V>> cache;

        /**
         * Constructor.
         *
         * @param cache cache to update
         */
        public RemoteEventHandler(LoadingCache<K, Optional<V>> cache) {
            this.cache = checkNotNull(cache);
        }

        @Override
        public void mapCleared(MapEvent event) {
            cache.invalidateAll();
        }

        @Override
        public void entryAdded(EntryEvent<byte[], byte[]> event) {
            K key = deserialize(event.getKey());
            V newVal = deserialize(event.getValue());
            Optional<V> newValue = Optional.of(newVal);
            cache.asMap().putIfAbsent(key, newValue);
            onAdd(key, newVal);
        }

        @Override
        public void entryUpdated(EntryEvent<byte[], byte[]> event) {
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

}
