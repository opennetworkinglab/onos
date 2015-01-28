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
package org.onosproject.store.impl;

import com.google.common.base.MoreObjects;
import org.onlab.util.KryoNamespace;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.NodeId;
import org.onosproject.store.Timestamp;
import org.onosproject.store.cluster.messaging.ClusterCommunicationService;
import org.onosproject.store.cluster.messaging.ClusterMessage;
import org.onosproject.store.cluster.messaging.ClusterMessageHandler;
import org.onosproject.store.cluster.messaging.MessageSubject;
import org.onosproject.store.serializers.KryoSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onlab.util.Tools.minPriority;
import static org.onlab.util.Tools.namedThreads;

/**
 * Distributed Map implementation which uses optimistic replication and gossip
 * based techniques to provide an eventually consistent data store.
 */
public class EventuallyConsistentMapImpl<K, V>
        implements EventuallyConsistentMap<K, V> {

    private static final Logger log = LoggerFactory.getLogger(EventuallyConsistentMapImpl.class);

    private final Map<K, Timestamped<V>> items;
    private final Map<K, Timestamp> removedItems;

    private final String mapName;
    private final ClusterService clusterService;
    private final ClusterCommunicationService clusterCommunicator;
    private final KryoSerializer serializer;

    private final ClockService<K> clockService;

    private final MessageSubject updateMessageSubject;
    private final MessageSubject removeMessageSubject;

    private final Set<EventuallyConsistentMapListener> listeners
            = new CopyOnWriteArraySet<>();

    private final ExecutorService executor;

    private final ScheduledExecutorService backgroundExecutor;

    private volatile boolean destroyed = false;
    private static final String ERROR_DESTROYED = " map is already destroyed";

    // TODO: Make these anti-entropy params configurable
    private long initialDelaySec = 5;
    private long periodSec = 5;

    /**
     * Creates a new eventually consistent map shared amongst multiple instances.
     *
     * Each map is identified by a string map name. EventuallyConsistentMapImpl
     * objects in different JVMs that use the same map name will form a
     * distributed map across JVMs (provided the cluster service is aware of
     * both nodes).
     *
     * The client is expected to provide an
     * {@link org.onlab.util.KryoNamespace.Builder} with which all classes that
     * will be stored in this map have been registered (including referenced
     * classes). This serializer will be used to serialize both K and V for
     * inter-node notifications.
     *
     * The client must provide an {@link org.onosproject.store.impl.ClockService}
     * which can generate timestamps for a given key. The clock service is free
     * to generate timestamps however it wishes, however these timestamps will
     * be used to serialize updates to the map so they must be strict enough
     * to ensure updates are properly ordered for the use case (i.e. in some
     * cases wallclock time will suffice, whereas in other cases logical time
     * will be necessary).
     *
     * @param mapName             a String identifier for the map.
     * @param clusterService      the cluster service
     * @param clusterCommunicator the cluster communications service
     * @param serializerBuilder   a Kryo namespace builder that can serialize
     *                            both K and V
     * @param clockService        a clock service able to generate timestamps
     *                            for K
     */
    public EventuallyConsistentMapImpl(String mapName,
                                       ClusterService clusterService,
                                       ClusterCommunicationService clusterCommunicator,
                                       KryoNamespace.Builder serializerBuilder,
                                       ClockService<K> clockService) {

        this.mapName = checkNotNull(mapName);
        this.clusterService = checkNotNull(clusterService);
        this.clusterCommunicator = checkNotNull(clusterCommunicator);

        serializer = createSerializer(checkNotNull(serializerBuilder));

        this.clockService = checkNotNull(clockService);

        items = new ConcurrentHashMap<>();
        removedItems = new ConcurrentHashMap<>();

        executor = Executors
                .newCachedThreadPool(namedThreads("onos-ecm-" + mapName + "-fg-%d"));

        backgroundExecutor =
                newSingleThreadScheduledExecutor(minPriority(
                        namedThreads("onos-ecm-" + mapName + "-bg-%d")));

        updateMessageSubject = new MessageSubject("ecm-" + mapName + "-update");
        clusterCommunicator.addSubscriber(updateMessageSubject,
                                          new InternalPutEventListener());
        removeMessageSubject = new MessageSubject("ecm-" + mapName + "-remove");
        clusterCommunicator.addSubscriber(removeMessageSubject,
                                          new InternalRemoveEventListener());
    }

    private KryoSerializer createSerializer(KryoNamespace.Builder builder) {
        return new KryoSerializer() {
            @Override
            protected void setupKryoPool() {
                // Add the map's internal helper classes to the user-supplied serializer
                serializerPool = builder
                        .register(WallClockTimestamp.class)
                        .register(PutEntry.class)
                        .register(RemoveEntry.class)
                        .register(ArrayList.class)
                        .register(InternalPutEvent.class)
                        .register(InternalRemoveEvent.class)
                        .build();

                // TODO anti-entropy classes
            }
        };
    }

    @Override
    public int size() {
        checkState(!destroyed, mapName + ERROR_DESTROYED);
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        checkState(!destroyed, mapName + ERROR_DESTROYED);
        return items.isEmpty();
    }

    @Override
    public boolean containsKey(K key) {
        checkState(!destroyed, mapName + ERROR_DESTROYED);
        return items.containsKey(key);
    }

    @Override
    public boolean containsValue(V value) {
        checkState(!destroyed, mapName + ERROR_DESTROYED);

        return items.values().stream()
                .anyMatch(timestamped -> timestamped.value().equals(value));
    }

    @Override
    public V get(K key) {
        checkState(!destroyed, mapName + ERROR_DESTROYED);

        Timestamped<V> value = items.get(key);
        if (value != null) {
            return value.value();
        }
        return null;
    }

    @Override
    public void put(K key, V value) {
        checkState(!destroyed, mapName + ERROR_DESTROYED);

        Timestamp timestamp = clockService.getTimestamp(key);
        if (putInternal(key, value, timestamp)) {
            notifyPeers(new InternalPutEvent<>(key, value, timestamp));
            EventuallyConsistentMapEvent<K, V> externalEvent
                    = new EventuallyConsistentMapEvent<>(
                    EventuallyConsistentMapEvent.Type.PUT, key, value);
            notifyListeners(externalEvent);
        }
    }

    private boolean putInternal(K key, V value, Timestamp timestamp) {
        synchronized (this) {
            Timestamp removed = removedItems.get(key);
            if (removed != null && removed.compareTo(timestamp) > 0) {
                return false;
            }

            Timestamped<V> existing = items.get(key);
            if (existing != null && existing.isNewer(timestamp)) {
                return false;
            } else {
                items.put(key, new Timestamped<>(value, timestamp));
                removedItems.remove(key);
                return true;
            }
        }
    }

    @Override
    public void remove(K key) {
        checkState(!destroyed, mapName + ERROR_DESTROYED);

        Timestamp timestamp = clockService.getTimestamp(key);
        if (removeInternal(key, timestamp)) {
            notifyPeers(new InternalRemoveEvent<>(key, timestamp));
            EventuallyConsistentMapEvent<K, V> externalEvent
                    = new EventuallyConsistentMapEvent<>(
                    EventuallyConsistentMapEvent.Type.REMOVE, key, null);
            notifyListeners(externalEvent);
        }
    }

    private boolean removeInternal(K key, Timestamp timestamp) {
        synchronized (this) {
            if (items.get(key) != null && items.get(key).isNewer(timestamp)) {
                return false;
            }

            items.remove(key);
            removedItems.put(key, timestamp);
            return true;
        }
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        checkState(!destroyed, mapName + ERROR_DESTROYED);

        List<PutEntry<K, V>> updates = new ArrayList<>(m.size());

        for (Map.Entry<? extends K, ? extends V> entry : m.entrySet()) {
            K key = entry.getKey();
            V value = entry.getValue();
            Timestamp timestamp = clockService.getTimestamp(entry.getKey());

            if (putInternal(key, value, timestamp)) {
                updates.add(new PutEntry<>(key, value, timestamp));
            }
        }

        notifyPeers(new InternalPutEvent<>(updates));

        for (PutEntry<K, V> entry : updates) {
            EventuallyConsistentMapEvent<K, V> externalEvent =
                    new EventuallyConsistentMapEvent<>(
                    EventuallyConsistentMapEvent.Type.PUT, entry.key(), entry.value());
            notifyListeners(externalEvent);
        }
    }

    @Override
    public void clear() {
        checkState(!destroyed, mapName + ERROR_DESTROYED);

        List<RemoveEntry<K>> removed = new ArrayList<>(items.size());

        for (K key : items.keySet()) {
            Timestamp timestamp = clockService.getTimestamp(key);

            if (removeInternal(key, timestamp)) {
                removed.add(new RemoveEntry<>(key, timestamp));
            }
        }

        notifyPeers(new InternalRemoveEvent<>(removed));

        for (RemoveEntry<K> entry : removed) {
            EventuallyConsistentMapEvent<K, V> externalEvent =
                    new EventuallyConsistentMapEvent<>(
                            EventuallyConsistentMapEvent.Type.REMOVE, entry.key(), null);
            notifyListeners(externalEvent);
        }
    }

    @Override
    public Set<K> keySet() {
        checkState(!destroyed, mapName + ERROR_DESTROYED);

        return items.keySet();
    }

    @Override
    public Collection<V> values() {
        checkState(!destroyed, mapName + ERROR_DESTROYED);

        return items.values().stream()
                .map(Timestamped::value)
                .collect(Collectors.toList());
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        checkState(!destroyed, mapName + ERROR_DESTROYED);

        return items.entrySet().stream()
                .map(e -> new Entry(e.getKey(), e.getValue().value()))
                .collect(Collectors.toSet());
    }

    @Override
    public void addListener(EventuallyConsistentMapListener<K, V> listener) {
        checkState(!destroyed, mapName + ERROR_DESTROYED);

        listeners.add(checkNotNull(listener));
    }

    @Override
    public void removeListener(EventuallyConsistentMapListener<K, V> listener) {
        checkState(!destroyed, mapName + ERROR_DESTROYED);

        listeners.remove(checkNotNull(listener));
    }

    @Override
    public void destroy() {
        destroyed = true;

        executor.shutdown();
        backgroundExecutor.shutdown();

        clusterCommunicator.removeSubscriber(updateMessageSubject);
        clusterCommunicator.removeSubscriber(removeMessageSubject);
    }

    private void notifyListeners(EventuallyConsistentMapEvent event) {
        for (EventuallyConsistentMapListener listener : listeners) {
            listener.event(event);
        }
    }

    private void notifyPeers(InternalPutEvent event) {
        broadcastMessage(updateMessageSubject, event);
    }

    private void notifyPeers(InternalRemoveEvent event) {
        broadcastMessage(removeMessageSubject, event);
    }

    private void broadcastMessage(MessageSubject subject, Object event) {
        ClusterMessage message = new ClusterMessage(
                clusterService.getLocalNode().id(),
                subject,
                serializer.encode(event));
        clusterCommunicator.broadcast(message);
    }

    private void unicastMessage(NodeId peer,
                                MessageSubject subject,
                                Object event) throws IOException {
        ClusterMessage message = new ClusterMessage(
                clusterService.getLocalNode().id(),
                subject,
                serializer.encode(event));
        clusterCommunicator.unicast(message, peer);
    }

    private final class Entry implements Map.Entry<K, V> {

        private final K key;
        private final V value;

        public Entry(K key, V value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(V value) {
            throw new UnsupportedOperationException();
        }
    }

    private final class InternalPutEventListener implements
            ClusterMessageHandler {
        @Override
        public void handle(ClusterMessage message) {
            log.debug("Received put event from peer: {}", message.sender());
            InternalPutEvent<K, V> event = serializer.decode(message.payload());

            executor.submit(() -> {
                try {
                    for (PutEntry<K, V> entry : event.entries()) {
                        K key = entry.key();
                        V value = entry.value();
                        Timestamp timestamp = entry.timestamp();

                        if (putInternal(key, value, timestamp)) {
                            EventuallyConsistentMapEvent externalEvent =
                                    new EventuallyConsistentMapEvent<>(
                                    EventuallyConsistentMapEvent.Type.PUT, key,
                                    value);
                            notifyListeners(externalEvent);
                        }
                    }
                } catch (Exception e) {
                    log.warn("Exception thrown handling put", e);
                }
            });
        }
    }

    private final class InternalRemoveEventListener implements
            ClusterMessageHandler {
        @Override
        public void handle(ClusterMessage message) {
            log.debug("Received remove event from peer: {}", message.sender());
            InternalRemoveEvent<K> event = serializer.decode(message.payload());

            executor.submit(() -> {
                try {
                    for (RemoveEntry<K> entry : event.entries()) {
                        K key = entry.key();
                        Timestamp timestamp = entry.timestamp();

                        if (removeInternal(key, timestamp)) {
                            EventuallyConsistentMapEvent externalEvent = new EventuallyConsistentMapEvent<K, V>(
                                    EventuallyConsistentMapEvent.Type.REMOVE,
                                    key, null);
                            notifyListeners(externalEvent);
                        }
                    }
                } catch (Exception e) {
                    log.warn("Exception thrown handling remove", e);
                }
            });
        }
    }

    private static final class InternalPutEvent<K, V> {
        private final List<PutEntry<K, V>> entries;

        public InternalPutEvent(K key, V value, Timestamp timestamp) {
            entries = Collections
                    .singletonList(new PutEntry<>(key, value, timestamp));
        }

        public InternalPutEvent(List<PutEntry<K, V>> entries) {
            this.entries = checkNotNull(entries);
        }

        // Needed for serialization.
        @SuppressWarnings("unused")
        private InternalPutEvent() {
            entries = null;
        }

        public List<PutEntry<K, V>> entries() {
            return entries;
        }
    }

    private static final class PutEntry<K, V> {
        private final K key;
        private final V value;
        private final Timestamp timestamp;

        public PutEntry(K key, V value, Timestamp timestamp) {
            this.key = checkNotNull(key);
            this.value = checkNotNull(value);
            this.timestamp = checkNotNull(timestamp);
        }

        // Needed for serialization.
        @SuppressWarnings("unused")
        private PutEntry() {
            this.key = null;
            this.value = null;
            this.timestamp = null;
        }

        public K key() {
            return key;
        }

        public V value() {
            return value;
        }

        public Timestamp timestamp() {
            return timestamp;
        }

        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                    .add("key", key)
                    .add("value", value)
                    .add("timestamp", timestamp)
                    .toString();
        }
    }

    private static final class InternalRemoveEvent<K> {
        private final List<RemoveEntry<K>> entries;

        public InternalRemoveEvent(K key, Timestamp timestamp) {
            entries = Collections.singletonList(
                    new RemoveEntry<>(key, timestamp));
        }

        public InternalRemoveEvent(List<RemoveEntry<K>> entries) {
            this.entries = checkNotNull(entries);
        }

        // Needed for serialization.
        @SuppressWarnings("unused")
        private InternalRemoveEvent() {
            entries = null;
        }

        public List<RemoveEntry<K>> entries() {
            return entries;
        }
    }

    private static final class RemoveEntry<K> {
        private final K key;
        private final Timestamp timestamp;

        public RemoveEntry(K key, Timestamp timestamp) {
            this.key = checkNotNull(key);
            this.timestamp = checkNotNull(timestamp);
        }

        // Needed for serialization.
        @SuppressWarnings("unused")
        private RemoveEntry() {
            this.key = null;
            this.timestamp = null;
        }

        public K key() {
            return key;
        }

        public Timestamp timestamp() {
            return timestamp;
        }
    }
}
