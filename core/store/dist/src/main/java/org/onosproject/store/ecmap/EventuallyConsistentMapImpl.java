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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.tuple.Pair;
import org.onlab.util.AbstractAccumulator;
import org.onlab.util.KryoNamespace;
import org.onlab.util.SlidingWindowCounter;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.store.Timestamp;
import org.onosproject.store.cluster.messaging.ClusterCommunicationService;
import org.onosproject.store.cluster.messaging.ClusterMessage;
import org.onosproject.store.cluster.messaging.ClusterMessageHandler;
import org.onosproject.store.cluster.messaging.MessageSubject;
import org.onosproject.store.impl.LogicalTimestamp;
import org.onosproject.store.impl.Timestamped;
import org.onosproject.store.service.WallClockTimestamp;
import org.onosproject.store.serializers.KryoSerializer;
import org.onosproject.store.service.ClockService;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.EventuallyConsistentMapEvent;
import org.onosproject.store.service.EventuallyConsistentMapListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onlab.util.BoundedThreadPool.newFixedThreadPool;
import static org.onlab.util.Tools.groupedThreads;

/**
 * Distributed Map implementation which uses optimistic replication and gossip
 * based techniques to provide an eventually consistent data store.
 */
public class EventuallyConsistentMapImpl<K, V>
        implements EventuallyConsistentMap<K, V> {

    private static final Logger log = LoggerFactory.getLogger(EventuallyConsistentMapImpl.class);

    private final ConcurrentMap<K, Timestamped<V>> items;
    private final ConcurrentMap<K, Timestamp> removedItems;

    private final ClusterService clusterService;
    private final ClusterCommunicationService clusterCommunicator;
    private final KryoSerializer serializer;

    private final ClockService<K, V> clockService;

    private final MessageSubject updateMessageSubject;
    private final MessageSubject antiEntropyAdvertisementSubject;

    private final Set<EventuallyConsistentMapListener<K, V>> listeners
            = new CopyOnWriteArraySet<>();

    private final ExecutorService executor;
    private final ScheduledExecutorService backgroundExecutor;
    private final BiFunction<K, V, Collection<NodeId>> peerUpdateFunction;

    private final ExecutorService communicationExecutor;
    private final Map<NodeId, EventAccumulator> senderPending;

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
     * @param clockService          a clock service able to generate timestamps
     *                              for K and V
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
                                ClockService<K, V> clockService,
                                BiFunction<K, V, Collection<NodeId>> peerUpdateFunction,
                                ExecutorService eventExecutor,
                                ExecutorService communicationExecutor,
                                ScheduledExecutorService backgroundExecutor,
                                boolean tombstonesDisabled,
                                long antiEntropyPeriod,
                                TimeUnit antiEntropyTimeUnit,
                                boolean convergeFaster,
                                boolean persistent) {
        items = new ConcurrentHashMap<>();
        removedItems = new ConcurrentHashMap<>();
        senderPending = Maps.newConcurrentMap();
        destroyedMessage = mapName + ERROR_DESTROYED;

        this.clusterService = clusterService;
        this.clusterCommunicator = clusterCommunicator;

        this.serializer = createSerializer(serializerBuilder);

        this.clockService = clockService;

        if (peerUpdateFunction != null) {
            this.peerUpdateFunction = peerUpdateFunction;
        } else {
            this.peerUpdateFunction = (key, value) -> clusterService.getNodes().stream()
                    .map(ControllerNode::id)
                    .filter(nodeId -> !nodeId.equals(clusterService.getLocalNode().id()))
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
            persistentStore.readInto(items, removedItems);
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
        this.backgroundExecutor.scheduleAtFixedRate(new SendAdvertisementTask(),
                                                    initialDelaySec, antiEntropyPeriod,
                                                    antiEntropyTimeUnit);

        updateMessageSubject = new MessageSubject("ecm-" + mapName + "-update");
        clusterCommunicator.addSubscriber(updateMessageSubject,
                                          new InternalEventListener(), this.executor);

        antiEntropyAdvertisementSubject = new MessageSubject("ecm-" + mapName + "-anti-entropy");
        clusterCommunicator.addSubscriber(antiEntropyAdvertisementSubject,
                                          new InternalAntiEntropyListener(), this.backgroundExecutor);

        this.tombstonesDisabled = tombstonesDisabled;
        this.lightweightAntiEntropy = !convergeFaster;
    }

    private KryoSerializer createSerializer(KryoNamespace.Builder builder) {
        return new KryoSerializer() {
            @Override
            protected void setupKryoPool() {
                // Add the map's internal helper classes to the user-supplied serializer
                serializerPool = builder
                        .register(LogicalTimestamp.class)
                        .register(WallClockTimestamp.class)
                        .register(PutEntry.class)
                        .register(RemoveEntry.class)
                        .register(ArrayList.class)
                        .register(AntiEntropyAdvertisement.class)
                        .register(HashMap.class)
                        .register(Timestamped.class)
                        .build();
            }
        };
    }

    @Override
    public int size() {
        checkState(!destroyed, destroyedMessage);
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        checkState(!destroyed, destroyedMessage);
        return items.isEmpty();
    }

    @Override
    public boolean containsKey(K key) {
        checkState(!destroyed, destroyedMessage);
        checkNotNull(key, ERROR_NULL_KEY);
        return items.containsKey(key);
    }

    @Override
    public boolean containsValue(V value) {
        checkState(!destroyed, destroyedMessage);
        checkNotNull(value, ERROR_NULL_VALUE);

        return items.values().stream()
                .anyMatch(timestamped -> timestamped.value().equals(value));
    }

    @Override
    public V get(K key) {
        checkState(!destroyed, destroyedMessage);
        checkNotNull(key, ERROR_NULL_KEY);

        Timestamped<V> value = items.get(key);
        if (value != null) {
            return value.value();
        }
        return null;
    }

    @Override
    public void put(K key, V value) {
        checkState(!destroyed, destroyedMessage);
        checkNotNull(key, ERROR_NULL_KEY);
        checkNotNull(value, ERROR_NULL_VALUE);

        Timestamp timestamp = clockService.getTimestamp(key, value);

        if (putInternal(key, value, timestamp)) {
            notifyPeers(new PutEntry<>(key, value, timestamp),
                        peerUpdateFunction.apply(key, value));
            notifyListeners(new EventuallyConsistentMapEvent<>(
                    EventuallyConsistentMapEvent.Type.PUT, key, value));
        }
    }

    private boolean putInternal(K key, V value, Timestamp timestamp) {
        counter.incrementCount();
        Timestamp removed = removedItems.get(key);
        if (removed != null && removed.isNewerThan(timestamp)) {
            log.debug("ecmap - removed was newer {}", value);
            return false;
        }

        final MutableBoolean updated = new MutableBoolean(false);

        items.compute(key, (k, existing) -> {
            if (existing != null && existing.isNewerThan(timestamp)) {
                updated.setFalse();
                return existing;
            } else {
                updated.setTrue();
                return new Timestamped<>(value, timestamp);
            }
            });

        boolean success = updated.booleanValue();
        if (!success) {
            log.debug("ecmap - existing was newer {}", value);
        }

        if (success && removed != null) {
            removedItems.remove(key, removed);
        }

        if (success && persistent) {
            persistentStore.put(key, value, timestamp);
        }

        return success;
    }

    @Override
    public void remove(K key) {
        checkState(!destroyed, destroyedMessage);
        checkNotNull(key, ERROR_NULL_KEY);

        // TODO prevent calls here if value is important for timestamp
        Timestamp timestamp = clockService.getTimestamp(key, null);

        if (removeInternal(key, timestamp)) {
            notifyPeers(new RemoveEntry<>(key, timestamp),
                        peerUpdateFunction.apply(key, null));
            notifyListeners(new EventuallyConsistentMapEvent<>(
                    EventuallyConsistentMapEvent.Type.REMOVE, key, null));
        }
    }

    private boolean removeInternal(K key, Timestamp timestamp) {
        if (timestamp == null) {
            return false;
        }

        counter.incrementCount();
        final MutableBoolean updated = new MutableBoolean(false);

        items.compute(key, (k, existing) -> {
            if (existing != null && existing.isNewerThan(timestamp)) {
                updated.setFalse();
                return existing;
            } else {
                updated.setTrue();
                // remove from items map
                return null;
            }
        });

        if (updated.isFalse()) {
            return false;
        }

        boolean updatedTombstone = false;

        if (!tombstonesDisabled) {
            Timestamp removedTimestamp = removedItems.get(key);
            if (removedTimestamp == null) {
                //Timestamp removed = removedItems.putIfAbsent(key, timestamp);
                updatedTombstone = (removedItems.putIfAbsent(key, timestamp) == null);
            } else if (timestamp.isNewerThan(removedTimestamp)) {
                updatedTombstone = removedItems.replace(key, removedTimestamp, timestamp);
            }
        }

        if (updated.booleanValue() && persistent) {
            persistentStore.remove(key, timestamp);
        }

        return (!tombstonesDisabled && updatedTombstone) || updated.booleanValue();
    }

    @Override
    public void remove(K key, V value) {
        checkState(!destroyed, destroyedMessage);
        checkNotNull(key, ERROR_NULL_KEY);
        checkNotNull(value, ERROR_NULL_VALUE);

        Timestamp timestamp = clockService.getTimestamp(key, value);

        if (removeInternal(key, timestamp)) {
            notifyPeers(new RemoveEntry<>(key, timestamp),
                        peerUpdateFunction.apply(key, value));
            notifyListeners(new EventuallyConsistentMapEvent<>(
                    EventuallyConsistentMapEvent.Type.REMOVE, key, value));
        }
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        checkState(!destroyed, destroyedMessage);
        m.forEach(this::put);
    }

    @Override
    public void clear() {
        checkState(!destroyed, destroyedMessage);
        items.forEach((key, value) -> remove(key));
    }

    @Override
    public Set<K> keySet() {
        checkState(!destroyed, destroyedMessage);
        return items.keySet();
    }

    @Override
    public Collection<V> values() {
        checkState(!destroyed, destroyedMessage);
        return items.values().stream()
                .map(Timestamped::value)
                .collect(Collectors.toList());
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        checkState(!destroyed, destroyedMessage);

        return items.entrySet().stream()
                .map(e -> Pair.of(e.getKey(), e.getValue().value()))
                .collect(Collectors.toSet());
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
        for (EventuallyConsistentMapListener<K, V> listener : listeners) {
            listener.event(event);
        }
    }

    private void notifyPeers(PutEntry<K, V> event, Collection<NodeId> peers) {
        queueUpdate(event, peers);
    }

    private void notifyPeers(RemoveEntry<K, V> event, Collection<NodeId> peers) {
        queueUpdate(event, peers);
    }

    private void queueUpdate(AbstractEntry<K, V> event, Collection<NodeId> peers) {
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

    private final class SendAdvertisementTask implements Runnable {
        @Override
        public void run() {
            if (Thread.currentThread().isInterrupted()) {
                log.info("Interrupted, quitting");
                return;
            }

            if (underHighLoad() || destroyed) {
                return;
            }

            try {
                final NodeId self = clusterService.getLocalNode().id();
                Set<ControllerNode> nodes = clusterService.getNodes();

                List<NodeId> nodeIds = nodes.stream()
                        .map(ControllerNode::id)
                        .collect(Collectors.toList());

                if (nodeIds.size() == 1 && nodeIds.get(0).equals(self)) {
                    log.trace("No other peers in the cluster.");
                    return;
                }

                NodeId peer;
                do {
                    int idx = RandomUtils.nextInt(0, nodeIds.size());
                    peer = nodeIds.get(idx);
                } while (peer.equals(self));

                if (Thread.currentThread().isInterrupted()) {
                    log.info("Interrupted, quitting");
                    return;
                }

                AntiEntropyAdvertisement<K> ad = createAdvertisement();
                NodeId destination = peer;
                clusterCommunicator.unicast(ad, antiEntropyAdvertisementSubject, serializer::encode, peer)
                                   .whenComplete((result, error) -> {
                                       if (error != null) {
                                           log.debug("Failed to send anti-entropy advertisement to {}", destination);
                                       }
                                   });

            } catch (Exception e) {
                // Catch all exceptions to avoid scheduled task being suppressed.
                log.error("Exception thrown while sending advertisement", e);
            }
        }
    }

    private AntiEntropyAdvertisement<K> createAdvertisement() {
        final NodeId self = clusterService.getLocalNode().id();

        Map<K, Timestamp> timestamps = new HashMap<>(items.size());

        items.forEach((key, value) -> timestamps.put(key, value.timestamp()));

        Map<K, Timestamp> tombstones = new HashMap<>(removedItems);

        return new AntiEntropyAdvertisement<>(self, timestamps, tombstones);
    }

    private void handleAntiEntropyAdvertisement(AntiEntropyAdvertisement<K> ad) {
        List<EventuallyConsistentMapEvent<K, V>> externalEvents;

        externalEvents = antiEntropyCheckLocalItems(ad);

        antiEntropyCheckLocalRemoved(ad);

        if (!lightweightAntiEntropy) {
            externalEvents.addAll(antiEntropyCheckRemoteRemoved(ad));

            // if remote ad has something unknown, actively sync
            for (K key : ad.timestamps().keySet()) {
                if (!items.containsKey(key)) {
                    // Send the advertisement back if this peer is out-of-sync
                    final NodeId sender = ad.sender();
                    AntiEntropyAdvertisement<K> myAd = createAdvertisement();

                    clusterCommunicator.unicast(myAd, antiEntropyAdvertisementSubject, serializer::encode, sender)
                                       .whenComplete((result, error) -> {
                                           if (error != null) {
                                               log.debug("Failed to send reactive "
                                                       + "anti-entropy advertisement to {}", sender);
                                           }
                                       });
                    break;
                }
            }
        }
        externalEvents.forEach(this::notifyListeners);
    }

    /**
     * Checks if any of the remote's live items or tombstones are out of date
     * according to our local live item list, or if our live items are out of
     * date according to the remote's tombstone list.
     * If the local copy is more recent, it will be pushed to the remote. If the
     * remote has a more recent remove, we apply that to the local state.
     *
     * @param ad remote anti-entropy advertisement
     * @return list of external events relating to local operations performed
     */
    private List<EventuallyConsistentMapEvent<K, V>> antiEntropyCheckLocalItems(
            AntiEntropyAdvertisement<K> ad) {
        final List<EventuallyConsistentMapEvent<K, V>> externalEvents
                = new LinkedList<>();
        final NodeId sender = ad.sender();

        for (Map.Entry<K, Timestamped<V>> item : items.entrySet()) {
            K key = item.getKey();
            Timestamped<V> localValue = item.getValue();

            Timestamp remoteTimestamp = ad.timestamps().get(key);
            if (remoteTimestamp == null) {
                remoteTimestamp = ad.tombstones().get(key);
            }
            if (remoteTimestamp == null || localValue
                    .isNewerThan(remoteTimestamp)) {
                // local value is more recent, push to sender
                queueUpdate(new PutEntry<>(key, localValue.value(),
                                            localValue.timestamp()), ImmutableList.of(sender));
            }

            Timestamp remoteDeadTimestamp = ad.tombstones().get(key);
            if (remoteDeadTimestamp != null &&
                    remoteDeadTimestamp.isNewerThan(localValue.timestamp())) {
                // sender has a more recent remove
                if (removeInternal(key, remoteDeadTimestamp)) {
                    externalEvents.add(new EventuallyConsistentMapEvent<>(
                            EventuallyConsistentMapEvent.Type.REMOVE, key, null));
                }
            }
        }

        return externalEvents;
    }

    /**
     * Checks if any items in the remote live list are out of date according
     * to our tombstone list. If we find we have a more up to date tombstone,
     * we'll send it to the remote.
     *
     * @param ad remote anti-entropy advertisement
     */
    private void antiEntropyCheckLocalRemoved(AntiEntropyAdvertisement<K> ad) {
        final NodeId sender = ad.sender();

        for (Map.Entry<K, Timestamp> dead : removedItems.entrySet()) {
            K key = dead.getKey();
            Timestamp localDeadTimestamp = dead.getValue();

            Timestamp remoteLiveTimestamp = ad.timestamps().get(key);
            if (remoteLiveTimestamp != null
                    && localDeadTimestamp.isNewerThan(remoteLiveTimestamp)) {
                // sender has zombie, push remove
                queueUpdate(new RemoveEntry<>(key, localDeadTimestamp), ImmutableList.of(sender));
            }
        }
    }

    /**
     * Checks if any of the local live items are out of date according to the
     * remote's tombstone advertisements. If we find a local item is out of date,
     * we'll apply the remove operation to the local state.
     *
     * @param ad remote anti-entropy advertisement
     * @return list of external events relating to local operations performed
     */
    private List<EventuallyConsistentMapEvent<K, V>>
    antiEntropyCheckRemoteRemoved(AntiEntropyAdvertisement<K> ad) {
        final List<EventuallyConsistentMapEvent<K, V>> externalEvents
                = new LinkedList<>();

        for (Map.Entry<K, Timestamp> remoteDead : ad.tombstones().entrySet()) {
            K key = remoteDead.getKey();
            Timestamp remoteDeadTimestamp = remoteDead.getValue();

            Timestamped<V> local = items.get(key);
            Timestamp localDead = removedItems.get(key);
            if (local != null && remoteDeadTimestamp.isNewerThan(
                    local.timestamp())) {
                // If the remote has a more recent tombstone than either our local
                // value, then do a remove with their timestamp
                if (removeInternal(key, remoteDeadTimestamp)) {
                    externalEvents.add(new EventuallyConsistentMapEvent<>(
                            EventuallyConsistentMapEvent.Type.REMOVE, key, null));
                }
            } else if (localDead != null && remoteDeadTimestamp.isNewerThan(
                    localDead)) {
                // If the remote has a more recent tombstone than us, update ours
                // to their timestamp
                removeInternal(key, remoteDeadTimestamp);
            }
        }

        return externalEvents;
    }

    private final class InternalAntiEntropyListener
            implements ClusterMessageHandler {

        @Override
        public void handle(ClusterMessage message) {
            log.trace("Received anti-entropy advertisement from peer: {}",
                      message.sender());
            AntiEntropyAdvertisement<K> advertisement = serializer.decode(message.payload());
            try {
                if (!underHighLoad()) {
                    handleAntiEntropyAdvertisement(advertisement);
                }
            } catch (Exception e) {
                log.warn("Exception thrown handling advertisements", e);
            }
        }
    }

    private final class InternalEventListener implements ClusterMessageHandler {
        @Override
        public void handle(ClusterMessage message) {
            if (destroyed) {
                return;
            }

            log.debug("Received update event from peer: {}", message.sender());
            Collection<AbstractEntry<K, V>> events = serializer.decode(message.payload());

            try {
                // TODO clean this for loop up
                for (AbstractEntry<K, V> entry : events) {
                    final K key = entry.key();
                    final V value;
                    final Timestamp timestamp = entry.timestamp();
                    final EventuallyConsistentMapEvent.Type type;
                    if (entry instanceof PutEntry) {
                        PutEntry<K, V> putEntry = (PutEntry<K, V>) entry;
                        value = putEntry.value();
                        type = EventuallyConsistentMapEvent.Type.PUT;
                    } else if (entry instanceof RemoveEntry) {
                        type = EventuallyConsistentMapEvent.Type.REMOVE;
                        value = null;
                    } else {
                        throw new IllegalStateException("Unknown entry type " + entry.getClass());
                    }

                    boolean success;
                    switch (type) {
                        case PUT:
                            success = putInternal(key, value, timestamp);
                            break;
                        case REMOVE:
                            success = removeInternal(key, timestamp);
                            break;
                        default:
                            success = false;
                    }
                    if (success) {
                        notifyListeners(new EventuallyConsistentMapEvent<>(type, key, value));
                    }
                }
            } catch (Exception e) {
                log.warn("Exception thrown handling put", e);
            }
        }
    }

    // TODO pull this into the class if this gets pulled out...
    private static final int DEFAULT_MAX_EVENTS = 1000;
    private static final int DEFAULT_MAX_IDLE_MS = 10;
    private static final int DEFAULT_MAX_BATCH_MS = 50;
    private static final Timer TIMER = new Timer("onos-ecm-sender-events");

    private final class EventAccumulator extends AbstractAccumulator<AbstractEntry<K, V>> {

        private final NodeId peer;

        private EventAccumulator(NodeId peer) {
            super(TIMER, DEFAULT_MAX_EVENTS, DEFAULT_MAX_BATCH_MS, DEFAULT_MAX_IDLE_MS);
            this.peer = peer;
        }

        @Override
        public void processItems(List<AbstractEntry<K, V>> items) {
            Map<K, AbstractEntry<K, V>> map = Maps.newHashMap();
            items.forEach(item -> map.compute(item.key(), (key, oldValue) ->
                  oldValue == null || item.compareTo(oldValue) > 0 ? item : oldValue
                  )
            );
            communicationExecutor.submit(() -> {
                clusterCommunicator.unicast(Lists.newArrayList(map.values()),
                                            updateMessageSubject,
                                            serializer::encode,
                                            peer)
                                   .whenComplete((result, error) -> {
                                       if (error != null) {
                                           log.debug("Failed to send to {}", peer);
                                       }
                                   });
            });
        }
    }
}
