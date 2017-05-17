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
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.Backtrace;
import org.onlab.util.KryoNamespace;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.VirtualNetworkIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentData;
import org.onosproject.net.intent.IntentEvent;
import org.onosproject.net.intent.IntentState;
import org.onosproject.net.intent.IntentStore;
import org.onosproject.net.intent.IntentStoreDelegate;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.WorkPartitionService;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.Timestamp;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.EventuallyConsistentMapBuilder;
import org.onosproject.store.service.EventuallyConsistentMapEvent;
import org.onosproject.store.service.EventuallyConsistentMapListener;
import org.onosproject.store.service.MultiValuedTimestamp;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.WallClockTimestamp;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Dictionary;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static org.onlab.util.Tools.get;
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

    private static final boolean PERSIST = false;

    // Map of intent key => current intent state
    private EventuallyConsistentMap<Key, IntentData> currentMap;

    // Map of intent key => pending intent operation
    private EventuallyConsistentMap<Key, IntentData> pendingMap;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService configService;

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

    //Denotes the initial persistence value for this structure
    private boolean initiallyPersistent = false;

    //TODO this is currently an experimental feature used for performance
    // evalutaion, enabling persistence with persist the intents but they will
    // not be reinstalled and network state will not be consistent with the
    // intents on cluster restart
    @Property(name = "persistenceEnabled", boolValue = PERSIST,
            label = "EXPERIMENTAL: Enable intent persistence")
    private boolean persistenceEnabled;


    /**
     * TimestampProvieder for currentMap.
     *
     * @param key Intent key
     * @param data Intent data
     * @return generated time stamp
     */
    private Timestamp currentTimestampProvider(Key key, IntentData data) {
        // vector timestamp consisting from 3 components
        //  (request timestamp, internal state, sequence #)

        // 2nd component required to avoid compilation result overwriting installation state
        //    note: above is likely to be a sign of design issue in transition to installation phase
        // 3rd component required for generating new timestamp for removal..
        return  new MultiValuedTimestamp<>(
                Optional.ofNullable(data.version()).orElseGet(WallClockTimestamp::new),
                    new MultiValuedTimestamp<>(data.internalStateVersion(),
                            sequenceNumber.incrementAndGet()));
    }

    @Activate
    public void activate(ComponentContext context) {
        configService.registerProperties(getClass());
        modified(context);
        //TODO persistent intents must be reevaluated and the appropriate
        //processing done here, current implementation is not functional
        //and is for performance evaluation only
        initiallyPersistent = persistenceEnabled;
        KryoNamespace.Builder intentSerializer = KryoNamespace.newBuilder()
                .register(KryoNamespaces.API)
                .register(IntentData.class)
                .register(VirtualNetworkIntent.class)
                .register(NetworkId.class)
                .register(MultiValuedTimestamp.class);

        EventuallyConsistentMapBuilder currentECMapBuilder =
                storageService.<Key, IntentData>eventuallyConsistentMapBuilder()
                .withName("intent-current")
                .withSerializer(intentSerializer)
                .withTimestampProvider(this::currentTimestampProvider)
                .withPeerUpdateFunction((key, intentData) -> getPeerNodes(key, intentData));

        EventuallyConsistentMapBuilder pendingECMapBuilder =
                storageService.<Key, IntentData>eventuallyConsistentMapBuilder()
                .withName("intent-pending")
                .withSerializer(intentSerializer)
                .withTimestampProvider((key, intentData) ->
                        /*
                            We always want to accept new values in the pending map,
                            so we should use a high performance logical clock.
                        */
                        /*
                            TODO We use the wall clock for the time being, but
                            this could result in issues if there is clock skew
                            across instances.
                         */
                        new MultiValuedTimestamp<>(new WallClockTimestamp(), System.nanoTime()))
                .withPeerUpdateFunction((key, intentData) -> getPeerNodes(key, intentData));
        if (initiallyPersistent) {
            currentECMapBuilder = currentECMapBuilder.withPersistence();
            pendingECMapBuilder = pendingECMapBuilder.withPersistence();
        }
        currentMap = currentECMapBuilder.build();
        pendingMap = pendingECMapBuilder.build();

        currentMap.addListener(mapCurrentListener);
        pendingMap.addListener(mapPendingListener);

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        if (initiallyPersistent && !persistenceEnabled) {
            pendingMap.clear();
            currentMap.clear();
            log.debug("Persistent state has been purged");
        }
        currentMap.removeListener(mapCurrentListener);
        pendingMap.removeListener(mapPendingListener);
        currentMap.destroy();
        pendingMap.destroy();

        log.info("Stopped");
    }

    @Modified
    public void modified(ComponentContext context) {
        Dictionary<?, ?> properties = context != null ? context.getProperties()
                : new Properties();
        try {
            String s = get(properties, "persistenceEnabled");
            persistenceEnabled =  isNullOrEmpty(s) ? PERSIST :
                    Boolean.parseBoolean(s.trim());
        } catch (Exception e) {
            persistenceEnabled = initiallyPersistent;
            log.error("Failed to retrieve the property value for persist," +
                              "defaulting to the initial setting of \"{}\"" +
                              "any persistent state will not be purged, if " +
                              "this occurred at startup changes made in this" +
                              "session will not be persisted to disk",
                      initiallyPersistent);
        }
        if (persistenceEnabled) {
            //FIXME persistence is an experimental feature, warnings can be removed
            //when the feature is completed
            log.warn("Persistence is an experimental feature, it is not fully " +
                             "functional and is intended only for " +
                             "performance evaluation");
        }
        if (!initiallyPersistent && !persistenceEnabled) {
            log.info("Persistence is set to \"false\", this was the initial" +
                             " setting so no state will be purged or " +
                             "persisted");
        } else if (!initiallyPersistent && persistenceEnabled) {
            log.info("Persistence is set to \"true\", entries will be begin " +
                             "to be persisted after restart");
        } else if (initiallyPersistent && !persistenceEnabled) {
            log.info("Persistence is set to \"false\", all persistent state " +
                             "will be purged on next shutdown");
        } else {
            log.info("Persistence is set to \"true\", entries from this and" +
                             " subsequent sessions will be persisted");
        }


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
                    if (log.isTraceEnabled()) {
                        log.trace("Purging {} in currentMap. {}@{}",
                                  newData.key(), newData.state(), newData.version(),
                                  new Backtrace());
                    }
                    currentMap.remove(newData.key(), currentData);
                } else {
                    log.info("Gratuitous purge request for intent: {}", newData.key());
                }
            } else {
                if (log.isTraceEnabled()) {
                    log.trace("Putting {} in currentMap. {}@{}",
                              newData.key(), newData.state(), newData.version(),
                              new Backtrace());
                }
                currentMap.put(newData.key(), IntentData.copy(newData));
            }
        } else {
            log.debug("Update for {} not acceptable from:\n{}\nto:\n{}",
                      newData.key(), currentData, newData);
        }
        // Remove the intent data from the pending map if the newData is more
        // recent or equal to the existing entry. No matter if it is an acceptable
        // update or not
        Key key = newData.key();
        IntentData existingValue = pendingMap.get(key);

        if (existingValue == null) {
            return;
        }

        if (!existingValue.version().isNewerThan(newData.version())) {
            pendingMap.remove(key, existingValue);
        } else {
            log.debug("{} in pending map was newer, leaving it there", key);
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
        return IntentData.copy(current);
    }

    @Override
    public void addPending(IntentData data) {
        checkNotNull(data);
        if (data.version() == null) {
            // Copy IntentData including request state in this way we can
            // avoid the creation of Intents with state == request, which can
            // be problematic if the Intent state is different from *REQ
            // {INSTALL_, WITHDRAW_ and PURGE_}.
            pendingMap.put(data.key(), IntentData.assign(data,
                                                         new WallClockTimestamp(),
                                                         clusterService.getLocalNode().id()));
        } else {
            pendingMap.compute(data.key(), (key, existingValue) -> {
                if (existingValue == null || existingValue.version().isOlderThan(data.version())) {
                    return IntentData.assign(data, data.version(), clusterService.getLocalNode().id());
                } else {
                    return existingValue;
                }
            });
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
    public IntentData getPendingData(Key intentKey) {
        return pendingMap.get(intentKey);
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
                    delegate.onUpdate(IntentData.copy(intentData)); // copy for safety, likely unnecessary
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
                        delegate.process(IntentData.copy(event.value()));
                    }
                }

                IntentEvent.getEvent(event.value()).ifPresent(e -> notifyDelegate(e));
            }
        }
    }

}

