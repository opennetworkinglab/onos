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

import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.tuple.Pair;
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
import org.onosproject.store.impl.ClockService;
import org.onosproject.store.impl.Timestamped;
import org.onosproject.store.impl.WallClockTimestamp;
import org.onosproject.store.serializers.KryoSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
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
    private final MessageSubject removeMessageSubject;
    private final MessageSubject antiEntropyAdvertisementSubject;

    private final Set<EventuallyConsistentMapListener<K, V>> listeners
            = new CopyOnWriteArraySet<>();

    private final ExecutorService executor;

    private final ScheduledExecutorService backgroundExecutor;

    private ExecutorService broadcastMessageExecutor;

    private volatile boolean destroyed = false;
    private static final String ERROR_DESTROYED = " map is already destroyed";
    private final String destroyedMessage;

    private static final String ERROR_NULL_KEY = "Key cannot be null";
    private static final String ERROR_NULL_VALUE = "Null values are not allowed";

    // TODO: Make these anti-entropy params configurable
    private long initialDelaySec = 5;
    private long periodSec = 5;
    private boolean lightweightAntiEntropy = true;

    private static final int WINDOW_SIZE = 5;
    private static final int HIGH_LOAD_THRESHOLD = 0;
    private static final int LOAD_WINDOW = 2;
    SlidingWindowCounter counter = new SlidingWindowCounter(WINDOW_SIZE);
    AtomicLong operations = new AtomicLong();

    /**
     * Creates a new eventually consistent map shared amongst multiple instances.
     * <p>
     * Each map is identified by a string map name. EventuallyConsistentMapImpl
     * objects in different JVMs that use the same map name will form a
     * distributed map across JVMs (provided the cluster service is aware of
     * both nodes).
     * </p>
     * <p>
     * The client is expected to provide an
     * {@link org.onlab.util.KryoNamespace.Builder} with which all classes that
     * will be stored in this map have been registered (including referenced
     * classes). This serializer will be used to serialize both K and V for
     * inter-node notifications.
     * </p>
     * <p>
     * The client must provide an {@link org.onosproject.store.impl.ClockService}
     * which can generate timestamps for a given key. The clock service is free
     * to generate timestamps however it wishes, however these timestamps will
     * be used to serialize updates to the map so they must be strict enough
     * to ensure updates are properly ordered for the use case (i.e. in some
     * cases wallclock time will suffice, whereas in other cases logical time
     * will be necessary).
     * </p>
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
                                       ClockService<K, V> clockService) {
        this.clusterService = checkNotNull(clusterService);
        this.clusterCommunicator = checkNotNull(clusterCommunicator);

        serializer = createSerializer(checkNotNull(serializerBuilder));
        destroyedMessage = mapName + ERROR_DESTROYED;

        this.clockService = checkNotNull(clockService);

        items = new ConcurrentHashMap<>();
        removedItems = new ConcurrentHashMap<>();

        // should be a normal executor; it's used for receiving messages
        //TODO make # of threads configurable
        executor = Executors.newFixedThreadPool(8, groupedThreads("onos/ecm", mapName + "-fg-%d"));

        // sending executor; should be capped
        //TODO make # of threads configurable
        broadcastMessageExecutor = //newSingleThreadExecutor(groupedThreads("onos/ecm", mapName + "-notify"));
                newFixedThreadPool(4, groupedThreads("onos/ecm", mapName + "-notify"));

        backgroundExecutor =
                //FIXME anti-entropy can take >60 seconds and it blocks fg workers
                // ... dropping minPriority to try to help until this can be parallel
                newSingleThreadScheduledExecutor(//minPriority(
                                                 groupedThreads("onos/ecm", mapName + "-bg-%d"))/*)*/;

        // start anti-entropy thread
        //TODO disable anti-entropy for now in testing (it is unstable)
        backgroundExecutor.scheduleAtFixedRate(new SendAdvertisementTask(),
                                               initialDelaySec, periodSec,
                                               TimeUnit.SECONDS);

        updateMessageSubject = new MessageSubject("ecm-" + mapName + "-update");
        clusterCommunicator.addSubscriber(updateMessageSubject,
                                          new InternalPutEventListener(), executor);
        removeMessageSubject = new MessageSubject("ecm-" + mapName + "-remove");
        clusterCommunicator.addSubscriber(removeMessageSubject,
                                          new InternalRemoveEventListener(), executor);
        antiEntropyAdvertisementSubject = new MessageSubject("ecm-" + mapName + "-anti-entropy");
        clusterCommunicator.addSubscriber(antiEntropyAdvertisementSubject,
                                          new InternalAntiEntropyListener(), backgroundExecutor);
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
                        .register(AntiEntropyAdvertisement.class)
                        .register(HashMap.class)
                        .build();
            }
        };
    }

    /**
     * Sets the executor to use for broadcasting messages and returns this
     * instance for method chaining.
     *
     * @param executor executor service
     * @return this instance
     */
    public EventuallyConsistentMapImpl<K, V> withBroadcastMessageExecutor(ExecutorService executor) {
        checkNotNull(executor, "Null executor");
        broadcastMessageExecutor = executor;
        return this;
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
            notifyPeers(new InternalPutEvent<>(key, value, timestamp));
            EventuallyConsistentMapEvent<K, V> externalEvent
                    = new EventuallyConsistentMapEvent<>(
                    EventuallyConsistentMapEvent.Type.PUT, key, value);
            notifyListeners(externalEvent);
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
        return success;
    }

    @Override
    public void remove(K key) {
        checkState(!destroyed, destroyedMessage);
        checkNotNull(key, ERROR_NULL_KEY);

        // TODO prevent calls here if value is important for timestamp
        Timestamp timestamp = clockService.getTimestamp(key, null);

        if (removeInternal(key, timestamp)) {
            notifyPeers(new InternalRemoveEvent<>(key, timestamp));
            EventuallyConsistentMapEvent<K, V> externalEvent
                    = new EventuallyConsistentMapEvent<>(
                    EventuallyConsistentMapEvent.Type.REMOVE, key, null);
            notifyListeners(externalEvent);
        }
    }

    private boolean removeInternal(K key, Timestamp timestamp) {
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

        Timestamp removedTimestamp = removedItems.get(key);
        if (removedTimestamp == null) {
            return removedItems.putIfAbsent(key, timestamp) == null;
        } else if (timestamp.isNewerThan(removedTimestamp)) {
            return removedItems.replace(key, removedTimestamp, timestamp);
        } else {
            return false;
        }
    }

    @Override
    public void remove(K key, V value) {
        checkState(!destroyed, destroyedMessage);
        checkNotNull(key, ERROR_NULL_KEY);
        checkNotNull(value, ERROR_NULL_VALUE);

        Timestamp timestamp = clockService.getTimestamp(key, value);

        if (removeInternal(key, timestamp)) {
            notifyPeers(new InternalRemoveEvent<>(key, timestamp));
            EventuallyConsistentMapEvent<K, V> externalEvent
                    = new EventuallyConsistentMapEvent<>(
                    EventuallyConsistentMapEvent.Type.REMOVE, key, value);
            notifyListeners(externalEvent);
        }
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        checkState(!destroyed, destroyedMessage);

        List<PutEntry<K, V>> updates = new ArrayList<>(m.size());

        for (Map.Entry<? extends K, ? extends V> entry : m.entrySet()) {
            K key = entry.getKey();
            V value = entry.getValue();

            checkNotNull(key, ERROR_NULL_KEY);
            checkNotNull(value, ERROR_NULL_VALUE);

            Timestamp timestamp = clockService.getTimestamp(key, value);

            if (putInternal(key, value, timestamp)) {
                updates.add(new PutEntry<>(key, value, timestamp));
            }
        }

        if (!updates.isEmpty()) {
            notifyPeers(new InternalPutEvent<>(updates));

            for (PutEntry<K, V> entry : updates) {
                EventuallyConsistentMapEvent<K, V> externalEvent =
                        new EventuallyConsistentMapEvent<>(
                                EventuallyConsistentMapEvent.Type.PUT, entry.key(),
                                entry.value());
                notifyListeners(externalEvent);
            }
        }
    }

    @Override
    public void clear() {
        checkState(!destroyed, destroyedMessage);

        List<RemoveEntry<K>> removed = new ArrayList<>(items.size());

        for (K key : items.keySet()) {
            // TODO also this is not applicable if value is important for timestamp?
            Timestamp timestamp = clockService.getTimestamp(key, null);

            if (removeInternal(key, timestamp)) {
                removed.add(new RemoveEntry<>(key, timestamp));
            }
        }

        if (!removed.isEmpty()) {
            notifyPeers(new InternalRemoveEvent<>(removed));

            for (RemoveEntry<K> entry : removed) {
                EventuallyConsistentMapEvent<K, V> externalEvent
                        = new EventuallyConsistentMapEvent<>(
                        EventuallyConsistentMapEvent.Type.REMOVE, entry.key(),
                        null);
                notifyListeners(externalEvent);
            }
        }
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
        broadcastMessageExecutor.shutdown();

        listeners.clear();

        clusterCommunicator.removeSubscriber(updateMessageSubject);
        clusterCommunicator.removeSubscriber(removeMessageSubject);
        clusterCommunicator.removeSubscriber(antiEntropyAdvertisementSubject);
    }

    private void notifyListeners(EventuallyConsistentMapEvent<K, V> event) {
        for (EventuallyConsistentMapListener<K, V> listener : listeners) {
            listener.event(event);
        }
    }

    private void notifyPeers(InternalPutEvent event) {
        // FIXME extremely memory expensive when we are overrun
//        broadcastMessageExecutor.execute(() -> broadcastMessage(updateMessageSubject, event));
        broadcastMessage(updateMessageSubject, event);
    }

    private void notifyPeers(InternalRemoveEvent event) {
        // FIXME extremely memory expensive when we are overrun
//        broadcastMessageExecutor.execute(() -> broadcastMessage(removeMessageSubject, event));
        broadcastMessage(removeMessageSubject, event);
    }

    private void broadcastMessage(MessageSubject subject, Object event) {
        // FIXME can we parallelize the serialization... use the caller???
        ClusterMessage message = new ClusterMessage(
                clusterService.getLocalNode().id(),
                subject,
                serializer.encode(event));
        broadcastMessageExecutor.execute(() -> clusterCommunicator.broadcast(message));
//        clusterCommunicator.broadcast(message);
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

            if (underHighLoad()) {
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

                try {
                    unicastMessage(peer, antiEntropyAdvertisementSubject, ad);
                } catch (IOException e) {
                    log.debug("Failed to send anti-entropy advertisement to {}", peer);
                }
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
                    try {
                        unicastMessage(sender, antiEntropyAdvertisementSubject, myAd);
                    } catch (IOException e) {
                        log.debug(
                                "Failed to send reactive anti-entropy advertisement to {}",
                                sender);
                    }

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

        final List<PutEntry<K, V>> updatesToSend = new ArrayList<>();

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
                updatesToSend
                        .add(new PutEntry<>(key, localValue.value(),
                                            localValue.timestamp()));
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

        // Send all updates to the peer at once
        if (!updatesToSend.isEmpty()) {
            try {
                unicastMessage(sender, updateMessageSubject,
                               new InternalPutEvent<>(updatesToSend));
            } catch (IOException e) {
                log.warn("Failed to send advertisement response", e);
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

        final List<RemoveEntry<K>> removesToSend = new ArrayList<>();

        for (Map.Entry<K, Timestamp> dead : removedItems.entrySet()) {
            K key = dead.getKey();
            Timestamp localDeadTimestamp = dead.getValue();

            Timestamp remoteLiveTimestamp = ad.timestamps().get(key);
            if (remoteLiveTimestamp != null
                    && localDeadTimestamp.isNewerThan(remoteLiveTimestamp)) {
                // sender has zombie, push remove
                removesToSend
                        .add(new RemoveEntry<>(key, localDeadTimestamp));
            }
        }

        // Send all removes to the peer at once
        if (!removesToSend.isEmpty()) {
            try {
                unicastMessage(sender, removeMessageSubject,
                               new InternalRemoveEvent<>(removesToSend));
            } catch (IOException e) {
                log.warn("Failed to send advertisement response", e);
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

    private final class InternalPutEventListener implements
            ClusterMessageHandler {
        @Override
        public void handle(ClusterMessage message) {
            log.debug("Received put event from peer: {}", message.sender());
            InternalPutEvent<K, V> event = serializer.decode(message.payload());

            try {
                for (PutEntry<K, V> entry : event.entries()) {
                    K key = entry.key();
                    V value = entry.value();
                    Timestamp timestamp = entry.timestamp();

                    if (putInternal(key, value, timestamp)) {
                        EventuallyConsistentMapEvent<K, V> externalEvent =
                                new EventuallyConsistentMapEvent<>(
                                        EventuallyConsistentMapEvent.Type.PUT, key,
                                        value);
                        notifyListeners(externalEvent);
                    }
                }
            } catch (Exception e) {
                log.warn("Exception thrown handling put", e);
            }
        }
    }

    private final class InternalRemoveEventListener implements
            ClusterMessageHandler {
        @Override
        public void handle(ClusterMessage message) {
            log.debug("Received remove event from peer: {}", message.sender());
            InternalRemoveEvent<K> event = serializer.decode(message.payload());
            try {
                for (RemoveEntry<K> entry : event.entries()) {
                    K key = entry.key();
                    Timestamp timestamp = entry.timestamp();

                    if (removeInternal(key, timestamp)) {
                        EventuallyConsistentMapEvent<K, V> externalEvent
                        = new EventuallyConsistentMapEvent<>(
                                EventuallyConsistentMapEvent.Type.REMOVE,
                                key, null);
                        notifyListeners(externalEvent);
                    }
                }
            } catch (Exception e) {
                log.warn("Exception thrown handling remove", e);
            }
        }
    }

}
