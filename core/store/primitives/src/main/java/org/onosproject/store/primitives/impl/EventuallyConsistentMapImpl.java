/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.store.primitives.impl;

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
import org.onosproject.persistence.PersistenceService;
import org.onosproject.store.LogicalTimestamp;
import org.onosproject.store.Timestamp;
import org.onosproject.store.cluster.messaging.ClusterCommunicationService;
import org.onosproject.store.cluster.messaging.MessageSubject;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.serializers.StoreSerializer;
import org.onosproject.store.service.DistributedPrimitive;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.EventuallyConsistentMapEvent;
import org.onosproject.store.service.EventuallyConsistentMapListener;
import org.onosproject.store.service.WallClockTimestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;
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
    private final StoreSerializer serializer;
    private final NodeId localNodeId;
    private final PersistenceService persistenceService;

    private final BiFunction<K, V, Timestamp> timestampProvider;

    private final MessageSubject bootstrapMessageSubject;
    private final MessageSubject initializeMessageSubject;
    private final MessageSubject updateMessageSubject;
    private final MessageSubject antiEntropyAdvertisementSubject;
    private final MessageSubject updateRequestSubject;

    private final Set<EventuallyConsistentMapListener<K, V>> listeners
            = Sets.newCopyOnWriteArraySet();

    private final ExecutorService executor;
    private final ScheduledExecutorService backgroundExecutor;
    private final BiFunction<K, V, Collection<NodeId>> peerUpdateFunction;

    private final ExecutorService communicationExecutor;
    private final Map<NodeId, EventAccumulator> senderPending;

    private long previousTombstonePurgeTime;
    private final Map<NodeId, Long> antiEntropyTimes = Maps.newConcurrentMap();

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
    private static final int HIGH_LOAD_THRESHOLD = 2;
    private static final int LOAD_WINDOW = 2;
    private SlidingWindowCounter counter = new SlidingWindowCounter(WINDOW_SIZE);

    private final boolean persistent;

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
     * @param ns                    a Kryo namespace that can serialize
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
     * @param persistenceService    persistence service
     */
    EventuallyConsistentMapImpl(String mapName,
                                ClusterService clusterService,
                                ClusterCommunicationService clusterCommunicator,
                                KryoNamespace ns,
                                BiFunction<K, V, Timestamp> timestampProvider,
                                BiFunction<K, V, Collection<NodeId>> peerUpdateFunction,
                                ExecutorService eventExecutor,
                                ExecutorService communicationExecutor,
                                ScheduledExecutorService backgroundExecutor,
                                boolean tombstonesDisabled,
                                long antiEntropyPeriod,
                                TimeUnit antiEntropyTimeUnit,
                                boolean convergeFaster,
                                boolean persistent,
                                PersistenceService persistenceService) {
        this.mapName = mapName;
        this.serializer = createSerializer(ns);
        this.persistenceService = persistenceService;
        this.persistent =
                persistent;
        if (persistent) {
            items = this.persistenceService.<K, MapValue<V>>persistentMapBuilder()
                    .withName(mapName)
                    .withSerializer(this.serializer)
                    .build();
        } else {
            items = Maps.newConcurrentMap();
        }
        senderPending = Maps.newConcurrentMap();
        destroyedMessage = mapName + ERROR_DESTROYED;

        this.clusterService = clusterService;
        this.clusterCommunicator = clusterCommunicator;
        this.localNodeId = clusterService.getLocalNode().id();

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
                    Executors.newFixedThreadPool(8, groupedThreads("onos/ecm", mapName + "-fg-%d", log));
        }

        if (communicationExecutor != null) {
            this.communicationExecutor = communicationExecutor;
        } else {
            // sending executor; should be capped
            //TODO this probably doesn't need to be bounded anymore
            this.communicationExecutor =
                    newFixedThreadPool(8, groupedThreads("onos/ecm", mapName + "-publish-%d", log));
        }


        if (backgroundExecutor != null) {
            this.backgroundExecutor = backgroundExecutor;
        } else {
            this.backgroundExecutor =
                    newSingleThreadScheduledExecutor(groupedThreads("onos/ecm", mapName + "-bg-%d", log));
        }

        // start anti-entropy thread
        this.backgroundExecutor.scheduleAtFixedRate(this::sendAdvertisement,
                                                    initialDelaySec, antiEntropyPeriod,
                                                    antiEntropyTimeUnit);

        bootstrapMessageSubject = new MessageSubject("ecm-" + mapName + "-bootstrap");
        clusterCommunicator.addSubscriber(bootstrapMessageSubject,
                                          serializer::decode,
                                          (Function<NodeId, CompletableFuture<Void>>) this::handleBootstrap,
                                          serializer::encode);

        initializeMessageSubject = new MessageSubject("ecm-" + mapName + "-initialize");
        clusterCommunicator.addSubscriber(initializeMessageSubject,
                serializer::decode,
                (Function<Collection<UpdateEntry<K, V>>, Void>) u -> {
                    processUpdates(u);
                    return null;
                },
                serializer::encode,
                this.executor);

        updateMessageSubject = new MessageSubject("ecm-" + mapName + "-update");
        clusterCommunicator.addSubscriber(updateMessageSubject,
                                          serializer::decode,
                                          this::processUpdates,
                                          this.executor);

        antiEntropyAdvertisementSubject = new MessageSubject("ecm-" + mapName + "-anti-entropy");
        clusterCommunicator.addSubscriber(antiEntropyAdvertisementSubject,
                                          serializer::decode,
                                          this::handleAntiEntropyAdvertisement,
                                          serializer::encode,
                                          this.backgroundExecutor);

        updateRequestSubject = new MessageSubject("ecm-" + mapName + "-update-request");
        clusterCommunicator.addSubscriber(updateRequestSubject,
                                          serializer::decode,
                                          this::handleUpdateRequests,
                                          this.backgroundExecutor);

        if (!tombstonesDisabled) {
            previousTombstonePurgeTime = 0;
            this.backgroundExecutor.scheduleWithFixedDelay(this::purgeTombstones,
                                                           initialDelaySec,
                                                           antiEntropyPeriod,
                                                           TimeUnit.SECONDS);
        }

        this.tombstonesDisabled = tombstonesDisabled;
        this.lightweightAntiEntropy = !convergeFaster;

        // Initiate first round of Gossip
        this.bootstrap();
    }

    private StoreSerializer createSerializer(KryoNamespace ns) {
        return StoreSerializer.using(KryoNamespace.newBuilder()
                         .register(ns)
                         // not so robust way to avoid collision with other
                         // user supplied registrations
                         .nextId(KryoNamespaces.BEGIN_USER_CUSTOM_ID + 100)
                         .register(KryoNamespaces.BASIC)
                         .register(LogicalTimestamp.class)
                         .register(WallClockTimestamp.class)
                         .register(AntiEntropyAdvertisement.class)
                         .register(AntiEntropyResponse.class)
                         .register(UpdateEntry.class)
                         .register(MapValue.class)
                         .register(MapValue.Digest.class)
                         .register(UpdateRequest.class)
                         .build(name() + "-ecmap"));
    }

    @Override
    public String name() {
        return mapName;
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
        return previousValue.get();
    }

    @Override
    public V compute(K key, BiFunction<K, V, V> recomputeFunction) {
        checkState(!destroyed, destroyedMessage);
        checkNotNull(key, ERROR_NULL_KEY);
        checkNotNull(recomputeFunction, "Recompute function cannot be null");

        AtomicBoolean updated = new AtomicBoolean(false);
        AtomicReference<MapValue<V>> previousValue = new AtomicReference<>();
        MapValue<V> computedValue = items.compute(serializer.copy(key), (k, mv) -> {
            previousValue.set(mv);
            V newRawValue = recomputeFunction.apply(key, mv == null ? null : mv.get());
            if (mv != null && Objects.equals(newRawValue, mv.get())) {
                // value was not updated
                return mv;
            }
            MapValue<V> newValue = new MapValue<>(newRawValue, timestampProvider.apply(key, newRawValue));
            if (mv == null || newValue.isNewerThan(mv)) {
                updated.set(true);
                // We return a copy to ensure updates to peers can be serialized.
                // This prevents replica divergence due to serialization failures.
                return serializer.copy(newValue);
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
     *
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
        return updated.get();
    }

    @Override
    public void addListener(EventuallyConsistentMapListener<K, V> listener) {
        checkState(!destroyed, destroyedMessage);

        listeners.add(checkNotNull(listener));
        items.forEach((k, v) -> {
            if (v.isAlive()) {
                listener.event(new EventuallyConsistentMapEvent<K, V>(mapName, PUT, k, v.get()));
            }
        });
    }

    @Override
    public void removeListener(EventuallyConsistentMapListener<K, V> listener) {
        checkState(!destroyed, destroyedMessage);

        listeners.remove(checkNotNull(listener));
    }

    @Override
    public CompletableFuture<Void> destroy() {
        destroyed = true;

        executor.shutdown();
        backgroundExecutor.shutdown();
        communicationExecutor.shutdown();

        listeners.clear();

        clusterCommunicator.removeSubscriber(bootstrapMessageSubject);
        clusterCommunicator.removeSubscriber(initializeMessageSubject);
        clusterCommunicator.removeSubscriber(updateMessageSubject);
        clusterCommunicator.removeSubscriber(updateRequestSubject);
        clusterCommunicator.removeSubscriber(antiEntropyAdvertisementSubject);
        return CompletableFuture.completedFuture(null);
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
                .filter(id -> clusterService.getState(id).isActive())
                .collect(Collectors.toList());
        Collections.shuffle(activePeers);
        return activePeers.isEmpty() ? Optional.empty() : Optional.of(activePeers.get(0));
    }

    private void sendAdvertisementToPeer(NodeId peer) {
        long adCreationTime = System.currentTimeMillis();
        AntiEntropyAdvertisement<K> ad = createAdvertisement();
        clusterCommunicator.sendAndReceive(ad,
                antiEntropyAdvertisementSubject,
                serializer::encode,
                serializer::decode,
                peer)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        log.debug("Failed to send anti-entropy advertisement to {}: {}",
                                peer, error.getMessage());
                    } else if (result == AntiEntropyResponse.PROCESSED) {
                        antiEntropyTimes.put(peer, adCreationTime);
                    }
                });
    }

    private void sendUpdateRequestToPeer(NodeId peer, Set<K> keys) {
        UpdateRequest<K> request = new UpdateRequest<>(localNodeId, keys);
        clusterCommunicator.unicast(request,
                updateRequestSubject,
                serializer::encode,
                peer)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        log.debug("Failed to send update request to {}: {}",
                                peer, error.getMessage());
                    }
                });
    }

    private AntiEntropyAdvertisement<K> createAdvertisement() {
        return new AntiEntropyAdvertisement<>(localNodeId,
                ImmutableMap.copyOf(Maps.transformValues(items, MapValue::digest)));
    }

    private AntiEntropyResponse handleAntiEntropyAdvertisement(AntiEntropyAdvertisement<K> ad) {
        if (destroyed || underHighLoad()) {
            return AntiEntropyResponse.IGNORED;
        }
        try {
            if (log.isTraceEnabled()) {
                log.trace("Received anti-entropy advertisement from {} for {} with {} entries in it",
                        ad.sender(), mapName, ad.digest().size());
            }
            antiEntropyCheckLocalItems(ad).forEach(this::notifyListeners);
        } catch (Exception e) {
            log.warn("Error handling anti-entropy advertisement", e);
            return AntiEntropyResponse.FAILED;
        }
        return AntiEntropyResponse.PROCESSED;
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
        final List<NodeId> peers = ImmutableList.of(sender);
        Set<K> staleOrMissing = new HashSet<>();
        Set<K> locallyUnknown = new HashSet<>(ad.digest().keySet());

        items.forEach((key, localValue) -> {
            locallyUnknown.remove(key);
            MapValue.Digest remoteValueDigest = ad.digest().get(key);
            if (remoteValueDigest == null || localValue.isNewerThan(remoteValueDigest.timestamp())) {
                // local value is more recent, push to sender
                queueUpdate(new UpdateEntry<>(key, localValue), peers);
            } else if (remoteValueDigest != null
                    && remoteValueDigest.isNewerThan(localValue.digest())
                    && remoteValueDigest.isTombstone()) {
                // remote value is more recent and a tombstone: update local value
                MapValue<V> tombstone = MapValue.tombstone(remoteValueDigest.timestamp());
                MapValue<V> previousValue = removeInternal(key,
                                                           Optional.empty(),
                                                           Optional.of(tombstone));
                if (previousValue != null && previousValue.isAlive()) {
                    externalEvents.add(new EventuallyConsistentMapEvent<>(mapName, REMOVE, key, previousValue.get()));
                }
            } else if (remoteValueDigest.isNewerThan(localValue.digest())) {
                // Not a tombstone and remote is newer
                staleOrMissing.add(key);
            }
        });
        // Keys missing in local map
        staleOrMissing.addAll(locallyUnknown);
        // Request updates that we missed out on
        sendUpdateRequestToPeer(sender, staleOrMissing);
        return externalEvents;
    }

    private void handleUpdateRequests(UpdateRequest<K> request) {
        final Set<K> keys = request.keys();
        final NodeId sender = request.sender();
        final List<NodeId> peers = ImmutableList.of(sender);

        keys.forEach(key ->
            queueUpdate(new UpdateEntry<>(key, items.get(key)), peers)
        );
    }

    private void purgeTombstones() {
        /*
         * In order to mitigate the resource exhaustion that can ensue due to an ever-growing set
         * of tombstones we employ the following heuristic to purge old tombstones periodically.
         * First, we keep track of the time (local system time) when we were able to have a successful
         * AE exchange with each peer. The smallest (or oldest) such time across *all* peers is regarded
         * as the time before which all tombstones are considered safe to purge.
         */
        long currentSafeTombstonePurgeTime =  clusterService.getNodes()
                                                            .stream()
                                                            .map(ControllerNode::id)
                                                            .filter(id -> !id.equals(localNodeId))
                                                            .map(id -> antiEntropyTimes.getOrDefault(id, 0L))
                                                            .reduce(Math::min)
                                                            .orElse(0L);
        if (currentSafeTombstonePurgeTime == previousTombstonePurgeTime) {
            return;
        }
        List<Map.Entry<K, MapValue<V>>> tombStonesToDelete = items.entrySet()
                                          .stream()
                                          .filter(e -> e.getValue().isTombstone())
                                          .filter(e -> e.getValue().creationTime() <= currentSafeTombstonePurgeTime)
                                          .collect(Collectors.toList());
        previousTombstonePurgeTime = currentSafeTombstonePurgeTime;
        tombStonesToDelete.forEach(entry -> items.remove(entry.getKey(), entry.getValue()));
    }

    private void processUpdates(Collection<UpdateEntry<K, V>> updates) {
        if (destroyed) {
            return;
        }
        updates.forEach(update -> {
            final K key = update.key();
            final MapValue<V> value = update.value() == null ? null : update.value().copy();
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

    /**
     * Bootstraps the map to attempt to get in sync with existing instances of the same map on other nodes in the
     * cluster. This is necessary to ensure that a read immediately after the map is created doesn't return a null
     * value.
     */
    private void bootstrap() {
        List<NodeId> activePeers = clusterService.getNodes()
                .stream()
                .map(ControllerNode::id)
                .filter(id -> !localNodeId.equals(id))
                .filter(id -> clusterService.getState(id).isActive())
                .collect(Collectors.toList());

        if (activePeers.isEmpty()) {
            return;
        }

        try {
            requestBootstrapFromPeers(activePeers)
                    .get(DistributedPrimitive.DEFAULT_OPERATION_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            log.debug("Failed to bootstrap ec map {}: {}", mapName, e.getCause());
        } catch (InterruptedException | TimeoutException e) {
            log.warn("Failed to bootstrap ec map {}: {}", mapName, e);
        }
    }

    /**
     * Requests all updates from each peer in the provided list of peers.
     * <p>
     * The returned future will be completed once at least one peer bootstraps this map or bootstrap requests to all
     * peers fail.
     *
     * @param peers the list of peers from which to request updates
     * @return a future to be completed once updates have been received from at least one peer
     */
    private CompletableFuture<Void> requestBootstrapFromPeers(List<NodeId> peers) {
        if (peers.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<Void> future = new CompletableFuture<>();

        final int totalPeers = peers.size();

        AtomicBoolean successful = new AtomicBoolean();
        AtomicInteger totalCount = new AtomicInteger();
        AtomicReference<Throwable> lastError = new AtomicReference<>();

        // Iterate through all of the peers and send a bootstrap request. On the first peer that returns
        // a successful bootstrap response, complete the future. Otherwise, if no peers respond with any
        // successful bootstrap response, the future will be completed with the last exception.
        for (NodeId peer : peers) {
            requestBootstrapFromPeer(peer).whenComplete((result, error) -> {
                if (error == null) {
                    if (successful.compareAndSet(false, true)) {
                        future.complete(null);
                    } else if (totalCount.incrementAndGet() == totalPeers) {
                        Throwable e = lastError.get();
                        if (e != null) {
                            future.completeExceptionally(e);
                        }
                    }
                } else {
                    if (!successful.get() && totalCount.incrementAndGet() == totalPeers) {
                        future.completeExceptionally(error);
                    } else {
                        lastError.set(error);
                    }
                }
            });
        }
        return future;
    }

    /**
     * Requests a bootstrap from the given peer.
     *
     * @param peer the peer from which to request updates
     * @return a future to be completed once the peer has sent bootstrap updates
     */
    private CompletableFuture<Void> requestBootstrapFromPeer(NodeId peer) {
        log.trace("Sending bootstrap request to {}", peer);
        return clusterCommunicator.<NodeId, Void>sendAndReceive(
                localNodeId,
                bootstrapMessageSubject,
                serializer::encode,
                serializer::decode,
                peer)
                .whenComplete((updates, error) -> {
                    if (error != null) {
                        log.debug("Bootstrap request to {} failed: {}", peer, error.getMessage());
                    }
                });
    }

    /**
     * Handles a bootstrap request from a peer.
     * <p>
     * When handling a bootstrap request from a peer, the node sends batches of entries back to the peer and
     * completes the bootstrap request once all batches have been received and processed.
     *
     * @param peer the peer that sent the bootstrap request
     * @return a future to be completed once updates have been sent to the peer
     */
    private CompletableFuture<Void> handleBootstrap(NodeId peer) {
        log.trace("Received bootstrap request from {}", peer);

        Function<List<UpdateEntry<K, V>>, CompletableFuture<Void>> sendUpdates = updates -> {
            log.trace("Initializing {} with {} entries", peer, updates.size());
            return clusterCommunicator.<List<UpdateEntry<K, V>>, Void>sendAndReceive(
                    ImmutableList.copyOf(updates),
                    initializeMessageSubject,
                    serializer::encode,
                    serializer::decode,
                    peer)
                    .whenComplete((result, error) -> {
                        if (error != null) {
                            log.debug("Failed to initialize {}", peer, error);
                        }
                    });
        };

        List<CompletableFuture<Void>> futures = Lists.newArrayList();
        List<UpdateEntry<K, V>> updates = Lists.newArrayList();
        for (Map.Entry<K, MapValue<V>> entry : items.entrySet()) {
            K key = entry.getKey();
            MapValue<V> value = entry.getValue();
            if (value.isAlive()) {
                updates.add(new UpdateEntry<K, V>(key, value));
                if (updates.size() == DEFAULT_MAX_EVENTS) {
                    futures.add(sendUpdates.apply(updates));
                    updates = new ArrayList<>();
                }
            }
        }

        if (!updates.isEmpty()) {
            futures.add(sendUpdates.apply(updates));
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]));
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
            communicationExecutor.execute(() -> {
                try {
                    clusterCommunicator.unicast(ImmutableList.copyOf(map.values()),
                            updateMessageSubject,
                            serializer::encode,
                            peer)
                            .whenComplete((result, error) -> {
                                if (error != null) {
                                    log.debug("Failed to send to {}", peer, error);
                                }
                            });
                } catch (Exception e) {
                    log.warn("Failed to send to {}", peer, e);
                }
            });
        }
    }
}
