/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.store.intent.impl;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.KryoNamespace;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.VirtualNetworkIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentData;
import org.onosproject.net.intent.IntentEvent;
import org.onosproject.net.intent.WorkPartitionService;
import org.onosproject.net.intent.IntentState;
import org.onosproject.net.intent.IntentStore;
import org.onosproject.net.intent.IntentStoreDelegate;
import org.onosproject.net.intent.Key;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.EventuallyConsistentMapEvent;
import org.onosproject.store.service.EventuallyConsistentMapListener;
import org.onosproject.store.service.MultiValuedTimestamp;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.WallClockTimestamp;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.intent.IntentState.PURGE_REQ;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Manages inventory of Intents in a distributed data store that uses optimistic
 * replication and gossip based techniques.
 */
//FIXME we should listen for leadership changes. if the local instance has just
// ...  become a leader, scan the pending map and process those
@Component(immediate = true)
@Service
public class GossipIntentStore
        extends AbstractStore<IntentEvent, IntentStoreDelegate>
        implements IntentStore {

    private final Logger log = getLogger(getClass());

    // Map of intent key => current intent state
    private EventuallyConsistentMap<Key, IntentData> currentMap;

    // Map of intent key => pending intent operation
    private EventuallyConsistentMap<Key, IntentData> pendingMap;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected WorkPartitionService partitionService;

    private final AtomicLong sequenceNumber = new AtomicLong(0);

    private EventuallyConsistentMapListener<Key, IntentData>
            mapCurrentListener = new InternalCurrentListener();

    private EventuallyConsistentMapListener<Key, IntentData>
            mapPendingListener = new InternalPendingListener();

    @Activate
    public void activate() {
        KryoNamespace.Builder intentSerializer = KryoNamespace.newBuilder()
                .register(KryoNamespaces.API)
                .register(IntentData.class)
                .register(VirtualNetworkIntent.class)
                .register(NetworkId.class)
                .register(MultiValuedTimestamp.class);

        currentMap = storageService.<Key, IntentData>eventuallyConsistentMapBuilder()
                .withName("intent-current")
                .withSerializer(intentSerializer)
                .withTimestampProvider((key, intentData) ->
                                               new MultiValuedTimestamp<>(intentData.version(),
                                                                          sequenceNumber.getAndIncrement()))
                .withPeerUpdateFunction((key, intentData) -> getPeerNodes(key, intentData))
                .build();

        pendingMap = storageService.<Key, IntentData>eventuallyConsistentMapBuilder()
                .withName("intent-pending")
                .withSerializer(intentSerializer)
                .withTimestampProvider((key, intentData) -> intentData == null ?
                        new MultiValuedTimestamp<>(new WallClockTimestamp(), System.nanoTime()) :
                        new MultiValuedTimestamp<>(intentData.version(), System.nanoTime()))
                .withPeerUpdateFunction((key, intentData) -> getPeerNodes(key, intentData))
                .build();

        currentMap.addListener(mapCurrentListener);
        pendingMap.addListener(mapPendingListener);

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        currentMap.removeListener(mapCurrentListener);
        pendingMap.removeListener(mapPendingListener);
        currentMap.destroy();
        pendingMap.destroy();

        log.info("Stopped");
    }

    @Override
    public long getIntentCount() {
        return currentMap.size();
    }

    @Override
    public Iterable<Intent> getIntents() {
        return currentMap.values().stream()
                .map(IntentData::intent)
                .collect(Collectors.toList());
    }

    @Override
    public Iterable<IntentData> getIntentData(boolean localOnly, long olderThan) {
        if (localOnly || olderThan > 0) {
            long now = System.currentTimeMillis();
            final WallClockTimestamp time = new WallClockTimestamp(now - olderThan);
            return currentMap.values().stream()
                    .filter(data -> data.version().isOlderThan(time) &&
                            (!localOnly || isMaster(data.key())))
                    .collect(Collectors.toList());
        }
        return currentMap.values();
    }

    @Override
    public IntentState getIntentState(Key intentKey) {
        IntentData data = currentMap.get(intentKey);
        if (data != null) {
            return data.state();
        }
        return null;
    }

    @Override
    public List<Intent> getInstallableIntents(Key intentKey) {
        IntentData data = currentMap.get(intentKey);
        if (data != null) {
            return data.installables();
        }
        return ImmutableList.of();
    }

    @Override
    public void write(IntentData newData) {
        checkNotNull(newData);

        IntentData currentData = currentMap.get(newData.key());
        if (IntentData.isUpdateAcceptable(currentData, newData)) {
            // Only the master is modifying the current state. Therefore assume
            // this always succeeds
            if (newData.state() == PURGE_REQ) {
                if (currentData != null) {
                    currentMap.remove(newData.key(), currentData);
                } else {
                    log.info("Gratuitous purge request for intent: {}", newData.key());
                }
            } else {
                currentMap.put(newData.key(), new IntentData(newData));
            }

            // Remove the intent data from the pending map if the newData is more
            // recent or equal to the existing entry.
            pendingMap.compute(newData.key(), (key, existingValue) -> {
                if (existingValue == null || !existingValue.version().isNewerThan(newData.version())) {
                    return null;
                } else {
                    return existingValue;
                }
            });
        }
    }

    private Collection<NodeId> getPeerNodes(Key key, IntentData data) {
        NodeId master = partitionService.getLeader(key, Key::hash);
        NodeId origin = (data != null) ? data.origin() : null;
        if (data != null && (master == null || origin == null)) {
            log.debug("Intent {} missing master and/or origin; master = {}, origin = {}",
                      key, master, origin);
        }

        NodeId me = clusterService.getLocalNode().id();
        boolean isMaster = Objects.equals(master, me);
        boolean isOrigin = Objects.equals(origin, me);
        if (isMaster && isOrigin) {
            return getRandomNode();
        } else if (isMaster) {
            return origin != null ? ImmutableList.of(origin) : getRandomNode();
        } else if (isOrigin) {
            return master != null ? ImmutableList.of(master) : getRandomNode();
        } else {
            log.warn("No master or origin for intent {}", key);
            return master != null ? ImmutableList.of(master) : getRandomNode();
        }
    }

    private List<NodeId> getRandomNode() {
        NodeId me = clusterService.getLocalNode().id();
        List<NodeId> nodes = clusterService.getNodes().stream()
                .map(ControllerNode::id)
                .filter(node -> !Objects.equals(node, me))
                .collect(Collectors.toList());
        if (nodes.isEmpty()) {
            return ImmutableList.of();
        }
        return ImmutableList.of(nodes.get(RandomUtils.nextInt(nodes.size())));
    }

    @Override
    public void batchWrite(Iterable<IntentData> updates) {
        updates.forEach(this::write);
    }

    @Override
    public Intent getIntent(Key key) {
        IntentData data = currentMap.get(key);
        if (data != null) {
            return data.intent();
        }
        return null;
    }

    @Override
    public IntentData getIntentData(Key key) {
        IntentData current = currentMap.get(key);
        if (current == null) {
            return null;
        }
        return new IntentData(current);
    }

    @Override
    public void addPending(IntentData data) {
        checkNotNull(data);

        if (data.version() == null) {
            pendingMap.put(data.key(), new IntentData(data.intent(), data.state(),
                                                      new WallClockTimestamp(), clusterService.getLocalNode().id()));
        } else {
            pendingMap.put(data.key(), new IntentData(data.intent(), data.state(),
                                                      data.version(), clusterService.getLocalNode().id()));
        }
    }

    @Override
    public boolean isMaster(Key intentKey) {
        return partitionService.isMine(intentKey, Key::hash);
    }

    @Override
    public Iterable<Intent> getPending() {
        return pendingMap.values().stream()
                .map(IntentData::intent)
                .collect(Collectors.toList());
    }

    @Override
    public Iterable<IntentData> getPendingData() {
        return pendingMap.values();
    }

    @Override
    public Iterable<IntentData> getPendingData(boolean localOnly, long olderThan) {
        long now = System.currentTimeMillis();
        final WallClockTimestamp time = new WallClockTimestamp(now - olderThan);
        return pendingMap.values().stream()
                .filter(data -> data.version().isOlderThan(time) &&
                        (!localOnly || isMaster(data.key())))
                .collect(Collectors.toList());
    }

    private final class InternalCurrentListener implements
            EventuallyConsistentMapListener<Key, IntentData> {
        @Override
        public void event(EventuallyConsistentMapEvent<Key, IntentData> event) {
            IntentData intentData = event.value();

            if (event.type() == EventuallyConsistentMapEvent.Type.PUT) {
                // The current intents map has been updated. If we are master for
                // this intent's partition, notify the Manager that it should
                // emit notifications about updated tracked resources.
                if (delegate != null && isMaster(event.value().intent().key())) {
                    delegate.onUpdate(new IntentData(intentData)); // copy for safety, likely unnecessary
                }
                IntentEvent.getEvent(intentData).ifPresent(e -> notifyDelegate(e));
            }
        }
    }

    private final class InternalPendingListener implements
            EventuallyConsistentMapListener<Key, IntentData> {
        @Override
        public void event(
                EventuallyConsistentMapEvent<Key, IntentData> event) {
            if (event.type() == EventuallyConsistentMapEvent.Type.PUT) {
                // The pending intents map has been updated. If we are master for
                // this intent's partition, notify the Manager that it should do
                // some work.
                if (isMaster(event.value().intent().key())) {
                    if (delegate != null) {
                        delegate.process(new IntentData(event.value()));
                    }
                }

                IntentEvent.getEvent(event.value()).ifPresent(e -> notifyDelegate(e));
            }
        }
    }

}

