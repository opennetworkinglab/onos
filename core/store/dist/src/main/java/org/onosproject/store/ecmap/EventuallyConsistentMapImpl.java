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
package org.onosproject.store.ecmap;

import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.tuple.Pair;
import org.onlab.util.AbstractAccumulator;
import org.onlab.util.KryoNamespace;
import org.onlab.util.SlidingWindowCounter;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.store.Timestamp;
import org.onosproject.store.cluster.messaging.ClusterCommunicationService;
import org.onosproject.store.cluster.messaging.MessageSubject;
import org.onosproject.store.impl.LogicalTimestamp;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.serializers.KryoSerializer;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.EventuallyConsistentMapEvent;
import org.onosproject.store.service.EventuallyConsistentMapListener;
import org.onosproject.store.service.WallClockTimestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onlab.util.BoundedThreadPool.newFixedThreadPool;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.store.service.EventuallyConsistentMapEvent.Type.PUT;
import static org.onosproject.store.service.EventuallyConsistentMapEvent.Type.REMOVE;

/**
 * Distributed Map implementation which uses optimistic replication and gossip
 * based techniques to provide an eventually consistent data store.
 */
public class EventuallyConsistentMapImpl<K, V>
        implements EventuallyConsistentMap<K, V> {

    private static final Logger log = LoggerFactory.getLogger(EventuallyConsistentMapImpl.class);

    private final Map<K, MapValue<V>> items;

    private final ClusterService clusterService;
    private final ClusterCommunicationService clusterCommunicator;
    private final KryoSerializer serializer;
    private final NodeId localNodeId;

    private final BiFunction<K, V, Timestamp> timestampProvider;

    private final MessageSubject updateMessageSubject;
    private final MessageSubject antiEntropyAdvertisementSubject;

    private final Set<EventuallyConsistentMapListener<K, V>> listeners
            = Sets.newCopyOnWriteArraySet();

    private final ExecutorService executor;
    private final ScheduledExecutorService backgroundExecutor;
    private final BiFunction<K, V, Collection<NodeId>> peerUpdateFunction;

    private final ExecutorService communicationExecutor;
    private final Map<NodeId, EventAccumulator> senderPending;

    private final String mapName;

    private volatile boolean destroyed = false;
    private static final String ERROR_DESTROYED = " map is already destroyed";
    private final String destroyedMessage;

    private static final String ERROR_NULL_KEY = "Key cannot be null";
    private static final String ERROR_NULL_VALUE = "Null values are not allowed";

    private final long initialDelaySec = 5;
    private final boolean lightweightAntiEntropy;
    private final boolean tombstonesDisabled;

    private static final int WINDOW_SIZE = 5;
    private static final int HIGH_LOAD_THRESHOLD = 0;
    private static final int LOAD_WINDOW = 2;
    private SlidingWindowCounter counter = new SlidingWindowCounter(WINDOW_SIZE);

    private final boolean persistent;
    private final PersistentStore<K, V> persistentStore;

    /**
     * Creates a new eventually consistent map shared amongst multiple instances.
     * <p>
     * See {@link org.onosproject.store.service.EventuallyConsistentMapBuilder}
     * for more description of the parameters expected by the map.
     * </p>
     *
     * @param mapName               a String identifier for the map.
     * @param clusterService        the cluster service
     * @param clusterCommunicator   the cluster communications service
     * @param serializerBuilder     a Kryo namespace builder that can serialize
     *                              both K and V
     * @param timestampProvider     provider of timestamps for K and V
     * @param peerUpdateFunction    function that provides a set of nodes to immediately
     *                              update to when there writes to the map
     * @param eventExecutor         executor to use for processing incoming
     *                              events from peers
     * @param communicationExecutor executor to use for sending events to peers
     * @param backgroundExecutor    executor to use for background anti-entropy
     *                              tasks
     * @param tombstonesDisabled    true if this map should not maintain
     *                              tombstones
     * @param antiEntropyPeriod     period that the anti-entropy task should run
     * @param antiEntropyTimeUnit   time unit for anti-entropy period
     * @param convergeFaster        make anti-entropy try to converge faster
     * @param persistent            persist data to disk
     */
    EventuallyConsistentMapImpl(String mapName,
                                ClusterService clusterService,
                                ClusterCommunicationService clusterCommunicator,
                                KryoNamespace.Builder serializerBuilder,
                                BiFunction<K, V, Timestamp> timestampProvider,
                                BiFunction<K, V, Collection<NodeId>> peerUpdateFunction,
                                ExecutorService eventExecutor,
                                ExecutorService communicationExecutor,
                                ScheduledExecutorService backgroundExecutor,
                                boolean tombstonesDisabled,
                                long antiEntropyPeriod,
                                TimeUnit antiEntropyTimeUnit,
                                boolean convergeFaster,
                                boolean persistent) {
        this.mapName = mapName;
        items = Maps.newConcurrentMap();
        senderPending = Maps.newConcurrentMap();
        destroyedMessage = mapName + ERROR_DESTROYED;

        this.clusterService = clusterService;
        this.clusterCommunicator = clusterCommunicator;
        this.localNodeId = clusterService.getLocalNode().id();

        this.serializer = createSerializer(serializerBuilder);

        this.timestampProvider = timestampProvider;

        if (peerUpdateFunction != null) {
            this.peerUpdateFunction = peerUpdateFunction;
        } else {
            this.peerUpdateFunction = (key, value) -> clusterService.getNodes().stream()
                    .map(ControllerNode::id)
                    .filter(nodeId -> !nodeId.equals(localNodeId))
                    .collect(Collectors.toList());
        }

        if (eventExecutor != null) {
            this.executor = eventExecutor;
        } else {
            // should be a normal executor; it's used for receiving messages
            this.executor =
                    Executors.newFixedThreadPool(8, groupedThreads("onos/ecm", mapName + "-fg-%d"));
        }

        if (communicationExecutor != null) {
            this.communicationExecutor = communicationExecutor;
        } else {
            // sending executor; should be capped
            //TODO this probably doesn't need to be bounded anymore
            this.communicationExecutor =
                    newFixedThreadPool(8, groupedThreads("onos/ecm", mapName + "-publish-%d"));
        }

        this.persistent = persistent;

        if (this.persistent) {
            String dataDirectory = System.getProperty("karaf.data", "./data");
            String filename = dataDirectory + "/" + "mapdb-ecm-" + mapName;

            ExecutorService dbExecutor =
                    newFixedThreadPool(1, groupedThreads("onos/ecm", mapName + "-dbwriter"));

            persistentStore = new MapDbPersistentStore<>(filename, dbExecutor, serializer);
            persistentStore.readInto(items);
        } else {
            this.persistentStore = null;
        }

        if (backgroundExecutor != null) {
            this.backgroundExecutor = backgroundExecutor;
        } else {
            this.backgroundExecutor =
                    newSingleThreadScheduledExecutor(groupedThreads("onos/ecm", mapName + "-bg-%d"));
        }

        // start anti-entropy thread
        this.backgroundExecutor.scheduleAtFixedRate(this::sendAdvertisement,
                                                    initialDelaySec, antiEntropyPeriod,
                                                    antiEntropyTimeUnit);

        updateMessageSubject = new MessageSubject("ecm-" + mapName + "-update");
        clusterCommunicator.addSubscriber(updateMessageSubject,
                                          serializer::decode,
                                          this::processUpdates,
                                          this.executor);

        antiEntropyAdvertisementSubject = new MessageSubject("ecm-" + mapName + "-anti-entropy");
        clusterCommunicator.addSubscriber(antiEntropyAdvertisementSubject,
                                          serializer::decode,
                                          this::handleAntiEntropyAdvertisement,
                                          this.backgroundExecutor);

        this.tombstonesDisabled = tombstonesDisabled;
        this.lightweightAntiEntropy = !convergeFaster;
    }

    private KryoSerializer createSerializer(KryoNamespace.Builder builder) {
        return new KryoSerializer() {
            @Override
            protected void setupKryoPool() {
                // Add the map's internal helper classes to the user-supplied serializer
                serializerPool = builder
                        .register(KryoNamespaces.BASIC)
                        .nextId(KryoNamespaces.BEGIN_USER_CUSTOM_ID)
                        .register(LogicalTimestamp.class)
                        .register(WallClockTimestamp.class)
                        .register(AntiEntropyAdvertisement.class)
                        .register(UpdateEntry.class)
                        .register(MapValue.class)
                        .register(MapValue.Digest.class)
                        .build();
            }
        };
    }

    @Override
    public int size() {
        checkState(!destroyed, destroyedMessage);
        // TODO: Maintain a separate counter for tracking live elements in map.
        return Maps.filterValues(items, MapValue::isAlive).size();
    }

    @Override
    public boolean isEmpty() {
        checkState(!destroyed, destroyedMessage);
        return size() == 0;
    }

    @Override
    public boolean containsKey(K key) {
        checkState(!destroyed, destroyedMessage);
        checkNotNull(key, ERROR_NULL_KEY);
        return get(key) != null;
    }

    @Override
    public boolean containsValue(V value) {
        checkState(!destroyed, destroyedMessage);
        checkNotNull(value, ERROR_NULL_VALUE);
        return items.values()
                    .stream()
                    .filter(MapValue::isAlive)
                    .anyMatch(v -> value.equals(v.get()));
    }

    @Override
    public V get(K key) {
        checkState(!destroyed, destroyedMessage);
        checkNotNull(key, ERROR_NULL_KEY);

        MapValue<V> value = items.get(key);
        return (value == null || value.isTombstone()) ? null : value.get();
    }

    @Override
    public void put(K key, V value) {
        checkState(!destroyed, destroyedMessage);
        checkNotNull(key, ERROR_NULL_KEY);
        checkNotNull(value, ERROR_NULL_VALUE);

        MapValue<V> newValue = new MapValue<>(value, timestampProvider.apply(key, value));
        if (putInternal(key, newValue)) {
            notifyPeers(new UpdateEntry<>(key, newValue), peerUpdateFunction.apply(key, value));
            notifyListeners(new EventuallyConsistentMapEvent<>(mapName, PUT, key, value));
        }
    }

    @Override
    public V remove(K key) {
        checkState(!destroyed, destroyedMessage);
        checkNotNull(key, ERROR_NULL_KEY);
        return removeAndNotify(key, null);
    }

    @Override
    public void remove(K key, V value) {
        checkState(!destroyed, destroyedMessage);
        checkNotNull(key, ERROR_NULL_KEY);
        checkNotNull(value, ERROR_NULL_VALUE);
        removeAndNotify(key, value);
    }

    private V removeAndNotify(K key, V value) {
        Timestamp timestamp = timestampProvider.apply(key, value);
        Optional<MapValue<V>> tombstone = tombstonesDisabled || timestamp == null
                ? Optional.empty() : Optional.of(MapValue.tombstone(timestamp));
        MapValue<V> previousValue = removeInternal(key, Optional.ofNullable(value), tombstone);
        if (previousValue != null) {
            notifyPeers(new UpdateEntry<>(key, tombstone.orElse(null)),
                        peerUpdateFunction.apply(key, previousValue.get()));
            if (previousValue.isAlive()) {
                notifyListeners(new EventuallyConsistentMapEvent<>(mapName, REMOVE, key, previousValue.get()));
            }
        }
        return previousValue != null ? previousValue.get() : null;
    }

    private MapValue<V> removeInternal(K key, Optional<V> value, Optional<MapValue<V>> tombstone) {
        checkState(!destroyed, destroyedMessage);
        checkNotNull(key, ERROR_NULL_KEY);
        checkNotNull(value, ERROR_NULL_VALUE);
        tombstone.ifPresent(v -> checkState(v.isTombstone()));

        counter.incrementCount();
        AtomicBoolean updated = new AtomicBoolean(false);
        AtomicReference<MapValue<V>> previousValue = new AtomicReference<>();
        items.compute(key, (k, existing) -> {
            boolean valueMatches = true;
            if (value.isPresent() && existing != null && existing.isAlive()) {
                valueMatches = Objects.equals(value.get(), existing.get());
            }
            if (existing == null) {
                log.trace("ECMap Remove: Existing value for key {} is already null", k);
            }
            if (valueMatches) {
                if (existing == null) {
                    updated.set(tombstone.isPresent());
                } else {
                    updated.set(!tombstone.isPresent() || tombstone.get().isNewerThan(existing));
                }
            }
            if (updated.get()) {
                previousValue.set(existing);
                return tombstone.orElse(null);
            } else {
                return existing;
            }
        });
        if (updated.get()) {
            if (persistent) {
                if (tombstone.isPresent()) {
                    persistentStore.update(key, tombstone.get());
                } else {
                    persistentStore.remove(key);
                }
            }
        }
        return previousValue.get();
    }

    @Override
    public V compute(K key, BiFunction<K, V, V> recomputeFunction) {
        checkState(!destroyed, destroyedMessage);
        checkNotNull(key, ERROR_NULL_KEY);
        checkNotNull(recomputeFunction, "Recompute function cannot be null");

        AtomicBoolean updated = new AtomicBoolean(false);
        AtomicReference<MapValue<V>> previousValue = new AtomicReference<>();
        MapValue<V> computedValue = items.compute(key, (k, mv) -> {
            previousValue.set(mv);
            V newRawValue = recomputeFunction.apply(key, mv == null ? null : mv.get());
            MapValue<V> newValue = new MapValue<>(newRawValue, timestampProvider.apply(key, newRawValue));
            if (mv == null || newValue.isNewerThan(mv)) {
                updated.set(true);
                return newValue;
            } else {
                return mv;
            }
        });
        if (updated.get()) {
            notifyPeers(new UpdateEntry<>(key, computedValue), peerUpdateFunction.apply(key, computedValue.get()));
            EventuallyConsistentMapEvent.Type updateType = computedValue.isTombstone() ? REMOVE : PUT;
            V value = computedValue.isTombstone()
                    ? previousValue.get() == null ? null : previousValue.get().get()
                    : computedValue.get();
            if (value != null) {
                notifyListeners(new EventuallyConsistentMapEvent<>(mapName, updateType, key, value));
            }
        }
        return computedValue.get();
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        checkState(!destroyed, destroyedMessage);
        m.forEach(this::put);
    }

    @Override
    public void clear() {
        checkState(!destroyed, destroyedMessage);
        Maps.filterValues(items, MapValue::isAlive)
            .forEach((k, v) -> remove(k));
    }

    @Override
    public Set<K> keySet() {
        checkState(!destroyed, destroyedMessage);
        return Maps.filterValues(items, MapValue::isAlive)
                   .keySet();
    }

    @Override
    public Collection<V> values() {
        checkState(!destroyed, destroyedMessage);
        return Collections2.transform(Maps.filterValues(items, MapValue::isAlive).values(), MapValue::get);
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        checkState(!destroyed, destroyedMessage);
        return Maps.filterValues(items, MapValue::isAlive)
                   .entrySet()
                   .stream()
                   .map(e -> Pair.of(e.getKey(), e.getValue().get()))
                   .collect(Collectors.toSet());
    }

    /**
     * Returns true if newValue was accepted i.e. map is updated.
     * @param key key
     * @param newValue proposed new value
     * @return true if update happened; false if map already contains a more recent value for the key
     */
    private boolean putInternal(K key, MapValue<V> newValue) {
        checkState(!destroyed, destroyedMessage);
        checkNotNull(key, ERROR_NULL_KEY);
        checkNotNull(newValue, ERROR_NULL_VALUE);
        checkState(newValue.isAlive());
        counter.incrementCount();
        AtomicBoolean updated = new AtomicBoolean(false);
        items.compute(key, (k, existing) -> {
            if (existing == null || newValue.isNewerThan(existing)) {
                updated.set(true);
                return newValue;
            }
            return existing;
        });
        if (updated.get() && persistent) {
            persistentStore.update(key, newValue);
        }
        return updated.get();
    }

    @Override
    public void addListener(EventuallyConsistentMapListener<K, V> listener) {
        checkState(!destroyed, destroyedMessage);

        listeners.add(checkNotNull(listener));
    }

    @Override
    public void removeListener(EventuallyConsistentMapListener<K, V> listener) {
        checkState(!destroyed, destroyedMessage);

        listeners.remove(checkNotNull(listener));
    }

    @Override
    public void destroy() {
        destroyed = true;

        executor.shutdown();
        backgroundExecutor.shutdown();
        communicationExecutor.shutdown();

        listeners.clear();

        clusterCommunicator.removeSubscriber(updateMessageSubject);
        clusterCommunicator.removeSubscriber(antiEntropyAdvertisementSubject);
    }

    private void notifyListeners(EventuallyConsistentMapEvent<K, V> event) {
        listeners.forEach(listener -> listener.event(event));
    }

    private void notifyPeers(UpdateEntry<K, V> event, Collection<NodeId> peers) {
        queueUpdate(event, peers);
    }

    private void queueUpdate(UpdateEntry<K, V> event, Collection<NodeId> peers) {
        if (peers == null) {
            // we have no friends :(
            return;
        }
        peers.forEach(node ->
                        senderPending.computeIfAbsent(node, unusedKey -> new EventAccumulator(node)).add(event)
        );
    }

    private boolean underHighLoad() {
        return counter.get(LOAD_WINDOW) > HIGH_LOAD_THRESHOLD;
    }

    private void sendAdvertisement() {
        try {
            if (underHighLoad() || destroyed) {
                return;
            }
            pickRandomActivePeer().ifPresent(this::sendAdvertisementToPeer);
        } catch (Exception e) {
            // Catch all exceptions to avoid scheduled task being suppressed.
            log.error("Exception thrown while sending advertisement", e);
        }
    }

    private Optional<NodeId> pickRandomActivePeer() {
        List<NodeId> activePeers = clusterService.getNodes()
                .stream()
                .map(ControllerNode::id)
                .filter(id -> !localNodeId.equals(id))
                .filter(id -> clusterService.getState(id) == ControllerNode.State.ACTIVE)
                .collect(Collectors.toList());
        Collections.shuffle(activePeers);
        return activePeers.isEmpty() ? Optional.empty() : Optional.of(activePeers.get(0));
    }

    private void sendAdvertisementToPeer(NodeId peer) {
        clusterCommunicator.unicast(createAdvertisement(),
                antiEntropyAdvertisementSubject,
                serializer::encode,
                peer)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        log.debug("Failed to send anti-entropy advertisement to {}", peer, error);
                    }
                });
    }

    private AntiEntropyAdvertisement<K> createAdvertisement() {
        return new AntiEntropyAdvertisement<K>(localNodeId,
                ImmutableMap.copyOf(Maps.transformValues(items, MapValue::digest)));
    }

    private void handleAntiEntropyAdvertisement(AntiEntropyAdvertisement<K> ad) {
        if (destroyed || underHighLoad()) {
            return;
        }
        try {
            if (log.isTraceEnabled()) {
                log.trace("Received anti-entropy advertisement from {} for {} with {} entries in it",
                        mapName, ad.sender(), ad.digest().size());
            }
            antiEntropyCheckLocalItems(ad).forEach(this::notifyListeners);

            if (!lightweightAntiEntropy) {
                // if remote ad has any entries that the local copy is missing, actively sync
                // TODO: Missing keys is not the way local copy can be behind.
                if (Sets.difference(ad.digest().keySet(), items.keySet()).size() > 0) {
                    // TODO: Send ad for missing keys and for entries that are stale
                    sendAdvertisementToPeer(ad.sender());
                }
            }
        } catch (Exception e) {
            log.warn("Error handling anti-entropy advertisement", e);
        }
    }

    /**
     * Processes anti-entropy ad from peer by taking following actions:
     * 1. If peer has an old entry, updates peer.
     * 2. If peer indicates an entry is removed and has a more recent
     * timestamp than the local entry, update local state.
     */
    private List<EventuallyConsistentMapEvent<K, V>> antiEntropyCheckLocalItems(
            AntiEntropyAdvertisement<K> ad) {
        final List<EventuallyConsistentMapEvent<K, V>> externalEvents = Lists.newLinkedList();
        final NodeId sender = ad.sender();
        items.forEach((key, localValue) -> {
            MapValue.Digest remoteValueDigest = ad.digest().get(key);
            if (remoteValueDigest == null || localValue.isNewerThan(remoteValueDigest.timestamp())) {
                // local value is more recent, push to sender
                queueUpdate(new UpdateEntry<>(key, localValue), ImmutableList.of(sender));
            }
            if (remoteValueDigest != null
                    && remoteValueDigest.isNewerThan(localValue.digest())
                    && remoteValueDigest.isTombstone()) {
                MapValue<V> tombstone = MapValue.tombstone(remoteValueDigest.timestamp());
                MapValue<V> previousValue = removeInternal(key,
                                                           Optional.empty(),
                                                           Optional.of(tombstone));
                if (previousValue != null && previousValue.isAlive()) {
                    externalEvents.add(new EventuallyConsistentMapEvent<>(mapName, REMOVE, key, previousValue.get()));
                }
            }
        });
        return externalEvents;
    }

    private void processUpdates(Collection<UpdateEntry<K, V>> updates) {
        if (destroyed) {
            return;
        }
        updates.forEach(update -> {
            final K key = update.key();
            final MapValue<V> value = update.value();
            if (value == null || value.isTombstone()) {
                MapValue<V> previousValue = removeInternal(key, Optional.empty(), Optional.ofNullable(value));
                if (previousValue != null && previousValue.isAlive()) {
                    notifyListeners(new EventuallyConsistentMapEvent<>(mapName, REMOVE, key, previousValue.get()));
                }
            } else if (putInternal(key, value)) {
                notifyListeners(new EventuallyConsistentMapEvent<>(mapName, PUT, key, value.get()));
            }
        });
    }

    // TODO pull this into the class if this gets pulled out...
    private static final int DEFAULT_MAX_EVENTS = 1000;
    private static final int DEFAULT_MAX_IDLE_MS = 10;
    private static final int DEFAULT_MAX_BATCH_MS = 50;
    private static final Timer TIMER = new Timer("onos-ecm-sender-events");

    private final class EventAccumulator extends AbstractAccumulator<UpdateEntry<K, V>> {

        private final NodeId peer;

        private EventAccumulator(NodeId peer) {
            super(TIMER, DEFAULT_MAX_EVENTS, DEFAULT_MAX_BATCH_MS, DEFAULT_MAX_IDLE_MS);
            this.peer = peer;
        }

        @Override
        public void processItems(List<UpdateEntry<K, V>> items) {
            Map<K, UpdateEntry<K, V>> map = Maps.newHashMap();
            items.forEach(item -> map.compute(item.key(), (key, existing) ->
                    item.isNewerThan(existing) ? item : existing));
            communicationExecutor.submit(() -> {
                clusterCommunicator.unicast(ImmutableList.copyOf(map.values()),
                                            updateMessageSubject,
                                            serializer::encode,
                                            peer)
                                   .whenComplete((result, error) -> {
                                       if (error != null) {
                                           log.debug("Failed to send to {}", peer, error);
                                       }
                                   });
            });
        }
    }
}
