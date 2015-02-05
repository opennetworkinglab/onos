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

import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.onlab.util.KryoNamespace;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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
    private final MessageSubject antiEntropyAdvertisementSubject;

    private final Set<EventuallyConsistentMapListener<K, V>> listeners
            = new CopyOnWriteArraySet<>();

    private final ExecutorService executor;

    private final ScheduledExecutorService backgroundExecutor;

    private volatile boolean destroyed = false;
    private static final String ERROR_DESTROYED = " map is already destroyed";

    private static final String ERROR_NULL_KEY = "Key cannot be null";
    private static final String ERROR_NULL_VALUE = "Null values are not allowed";

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

        // start anti-entropy thread
        backgroundExecutor.scheduleAtFixedRate(new SendAdvertisementTask(),
                                               initialDelaySec, periodSec,
                                               TimeUnit.SECONDS);

        updateMessageSubject = new MessageSubject("ecm-" + mapName + "-update");
        clusterCommunicator.addSubscriber(updateMessageSubject,
                                          new InternalPutEventListener());
        removeMessageSubject = new MessageSubject("ecm-" + mapName + "-remove");
        clusterCommunicator.addSubscriber(removeMessageSubject,
                                          new InternalRemoveEventListener());
        antiEntropyAdvertisementSubject = new MessageSubject("ecm-" + mapName + "-anti-entropy");
        clusterCommunicator.addSubscriber(antiEntropyAdvertisementSubject,
                                          new InternalAntiEntropyListener());
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
        checkNotNull(key, ERROR_NULL_KEY);
        return items.containsKey(key);
    }

    @Override
    public boolean containsValue(V value) {
        checkState(!destroyed, mapName + ERROR_DESTROYED);
        checkNotNull(value, ERROR_NULL_VALUE);

        return items.values().stream()
                .anyMatch(timestamped -> timestamped.value().equals(value));
    }

    @Override
    public V get(K key) {
        checkState(!destroyed, mapName + ERROR_DESTROYED);
        checkNotNull(key, ERROR_NULL_KEY);

        Timestamped<V> value = items.get(key);
        if (value != null) {
            return value.value();
        }
        return null;
    }

    @Override
    public void put(K key, V value) {
        checkState(!destroyed, mapName + ERROR_DESTROYED);
        checkNotNull(key, ERROR_NULL_KEY);
        checkNotNull(value, ERROR_NULL_VALUE);

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
        checkNotNull(key, ERROR_NULL_KEY);

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

            checkNotNull(key, ERROR_NULL_KEY);
            checkNotNull(value, ERROR_NULL_VALUE);

            Timestamp timestamp = clockService.getTimestamp(entry.getKey());

            if (putInternal(key, value, timestamp)) {
                updates.add(new PutEntry<>(key, value, timestamp));
            }
        }

        if (!updates.isEmpty()) {
            notifyPeers(new InternalPutEvent<>(updates));

            for (PutEntry<K, V> entry : updates) {
                EventuallyConsistentMapEvent<K, V> externalEvent = new EventuallyConsistentMapEvent<>(
                        EventuallyConsistentMapEvent.Type.PUT, entry.key(),
                        entry.value());
                notifyListeners(externalEvent);
            }
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
                .map(e -> Pair.of(e.getKey(), e.getValue().value()))
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

    private final class SendAdvertisementTask implements Runnable {
        @Override
        public void run() {
            if (Thread.currentThread().isInterrupted()) {
                log.info("Interrupted, quitting");
                return;
            }

            try {
                final NodeId self = clusterService.getLocalNode().id();
                Set<ControllerNode> nodes = clusterService.getNodes();

                List<NodeId> nodeIds = nodes.stream()
                        .map(node -> node.id())
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

        synchronized (this) {
            final NodeId sender = ad.sender();

            externalEvents = antiEntropyCheckLocalItems(ad);

            antiEntropyCheckLocalRemoved(ad);

            externalEvents.addAll(antiEntropyCheckRemoteRemoved(ad));

            // if remote ad has something unknown, actively sync
            for (K key : ad.timestamps().keySet()) {
                if (!items.containsKey(key)) {
                    AntiEntropyAdvertisement<K> myAd = createAdvertisement();
                    try {
                        unicastMessage(sender, antiEntropyAdvertisementSubject,
                                       myAd);
                        break;
                    } catch (IOException e) {
                        log.debug(
                                "Failed to send reactive anti-entropy advertisement to {}",
                                sender);
                    }
                }
            }
        } // synchronized (this)

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
    // Guarded by synchronized (this)
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
                    .isNewer(remoteTimestamp)) {
                // local value is more recent, push to sender
                updatesToSend
                        .add(new PutEntry<>(key, localValue.value(),
                                            localValue.timestamp()));
            }

            Timestamp remoteDeadTimestamp = ad.tombstones().get(key);
            if (remoteDeadTimestamp != null &&
                    remoteDeadTimestamp.compareTo(localValue.timestamp()) > 0) {
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
                unicastMessage(sender, updateMessageSubject, new InternalPutEvent<>(updatesToSend));
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
    // Guarded by synchronized (this)
    private void antiEntropyCheckLocalRemoved(AntiEntropyAdvertisement<K> ad) {
        final NodeId sender = ad.sender();

        final List<RemoveEntry<K>> removesToSend = new ArrayList<>();

        for (Map.Entry<K, Timestamp> dead : removedItems.entrySet()) {
            K key = dead.getKey();
            Timestamp localDeadTimestamp = dead.getValue();

            Timestamp remoteLiveTimestamp = ad.timestamps().get(key);
            if (remoteLiveTimestamp != null
                    && localDeadTimestamp.compareTo(remoteLiveTimestamp) > 0) {
                // sender has zombie, push remove
                removesToSend
                        .add(new RemoveEntry<>(key, localDeadTimestamp));
            }
        }

        // Send all removes to the peer at once
        if (!removesToSend.isEmpty()) {
            try {
                unicastMessage(sender, removeMessageSubject, new InternalRemoveEvent<>(removesToSend));
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
    // Guarded by synchronized (this)
    private List<EventuallyConsistentMapEvent<K, V>>
            antiEntropyCheckRemoteRemoved(AntiEntropyAdvertisement<K> ad) {
        final List<EventuallyConsistentMapEvent<K, V>> externalEvents
                = new LinkedList<>();

        for (Map.Entry<K, Timestamp> remoteDead : ad.tombstones().entrySet()) {
            K key = remoteDead.getKey();
            Timestamp remoteDeadTimestamp = remoteDead.getValue();

            Timestamped<V> local = items.get(key);
            Timestamp localDead = removedItems.get(key);
            if (local != null
                    && remoteDeadTimestamp.compareTo(local.timestamp()) > 0) {
                // remove our version
                if (removeInternal(key, remoteDeadTimestamp)) {
                    externalEvents.add(new EventuallyConsistentMapEvent<>(
                            EventuallyConsistentMapEvent.Type.REMOVE, key, null));
                }
            } else if (localDead != null &&
                    remoteDeadTimestamp.compareTo(localDead) > 0) {
                // If we both had the item as removed, but their timestamp is
                // newer, update ours to the newer value
                removedItems.put(key, remoteDeadTimestamp);
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
            backgroundExecutor.submit(() -> {
                try {
                    handleAntiEntropyAdvertisement(advertisement);
                } catch (Exception e) {
                    log.warn("Exception thrown handling advertisements", e);
                }
            });
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
            });
        }
    }

}
