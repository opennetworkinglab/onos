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
package org.onosproject.store.flow.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.namedThreads;
import static org.onosproject.net.flow.FlowRuleEvent.Type.RULE_REMOVED;
import static org.onosproject.store.flow.impl.FlowStoreMessageSubjects.APPLY_BATCH_FLOWS;
import static org.onosproject.store.flow.impl.FlowStoreMessageSubjects.GET_DEVICE_FLOW_ENTRIES;
import static org.onosproject.store.flow.impl.FlowStoreMessageSubjects.GET_FLOW_ENTRY;
import static org.onosproject.store.flow.impl.FlowStoreMessageSubjects.REMOVE_FLOW_ENTRY;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.KryoNamespace;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.NodeId;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.CompletedBatchOperation;
import org.onosproject.net.flow.DefaultFlowEntry;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowEntry.FlowEntryState;
import org.onosproject.net.flow.FlowId;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleBatchEntry;
import org.onosproject.net.flow.FlowRuleBatchEntry.FlowRuleOperation;
import org.onosproject.net.flow.FlowRuleBatchEvent;
import org.onosproject.net.flow.FlowRuleBatchOperation;
import org.onosproject.net.flow.FlowRuleBatchRequest;
import org.onosproject.net.flow.FlowRuleEvent;
import org.onosproject.net.flow.FlowRuleEvent.Type;
import org.onosproject.net.flow.FlowRuleStore;
import org.onosproject.net.flow.FlowRuleStoreDelegate;
import org.onosproject.net.flow.StoredFlowEntry;
import org.onosproject.store.cluster.messaging.ClusterCommunicationService;
import org.onosproject.store.cluster.messaging.ClusterMessage;
import org.onosproject.store.cluster.messaging.ClusterMessageHandler;
import org.onosproject.store.flow.ReplicaInfo;
import org.onosproject.store.flow.ReplicaInfoEvent;
import org.onosproject.store.flow.ReplicaInfoEventListener;
import org.onosproject.store.flow.ReplicaInfoService;
import org.onosproject.store.hz.AbstractHazelcastStore;
import org.onosproject.store.hz.SMap;
import org.onosproject.store.serializers.DecodeTo;
import org.onosproject.store.serializers.KryoSerializer;
import org.onosproject.store.serializers.StoreSerializer;
import org.onosproject.store.serializers.impl.DistributedStoreSerializers;
import org.slf4j.Logger;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.hazelcast.core.IMap;

/**
 * Manages inventory of flow rules using a distributed state management protocol.
 */
@Component(immediate = true)
@Service
public class DistributedFlowRuleStore
        extends AbstractHazelcastStore<FlowRuleBatchEvent, FlowRuleStoreDelegate>
        implements FlowRuleStore {

    private final Logger log = getLogger(getClass());

    // primary data:
    //  read/write needs to be locked
    private final ReentrantReadWriteLock flowEntriesLock = new ReentrantReadWriteLock();
    // store entries as a pile of rules, no info about device tables
    private final Multimap<DeviceId, StoredFlowEntry> flowEntries
        = ArrayListMultimap.<DeviceId, StoredFlowEntry>create();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ReplicaInfoService replicaInfoManager;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterCommunicationService clusterCommunicator;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    private final AtomicInteger localBatchIdGen = new AtomicInteger();

    private int pendingFutureTimeoutMinutes = 5;

    private Cache<Integer, SettableFuture<CompletedBatchOperation>> pendingFutures =
            CacheBuilder.newBuilder()
                .expireAfterWrite(pendingFutureTimeoutMinutes, TimeUnit.MINUTES)
                .removalListener(new TimeoutFuture())
                .build();

    // Cache of SMaps used for backup data.  each SMap contain device flow table
    private LoadingCache<DeviceId, SMap<FlowId, ImmutableList<StoredFlowEntry>>> smaps;


    private final ExecutorService futureListeners =
            Executors.newCachedThreadPool(namedThreads("flowstore-peer-responders"));

    private final ExecutorService backupExecutors =
            Executors.newSingleThreadExecutor(namedThreads("async-backups"));

    private boolean syncBackup = false;

    protected static final StoreSerializer SERIALIZER = new KryoSerializer() {
        @Override
        protected void setupKryoPool() {
            serializerPool = KryoNamespace.newBuilder()
                    .register(DistributedStoreSerializers.STORE_COMMON)
                    .nextId(DistributedStoreSerializers.STORE_CUSTOM_BEGIN)
                    .register(FlowRuleEvent.class)
                    .register(FlowRuleEvent.Type.class)
                    .build();
        }
    };

    private static final long FLOW_RULE_STORE_TIMEOUT_MILLIS = 5000;

    private ReplicaInfoEventListener replicaInfoEventListener;

    @Override
    @Activate
    public void activate() {

        super.serializer = SERIALIZER;
        super.theInstance = storeService.getHazelcastInstance();

        // Cache to create SMap on demand
        smaps = CacheBuilder.newBuilder()
                    .softValues()
                    .build(new SMapLoader());

        final NodeId local = clusterService.getLocalNode().id();

        clusterCommunicator.addSubscriber(APPLY_BATCH_FLOWS, new OnStoreBatch(local));

        clusterCommunicator.addSubscriber(GET_FLOW_ENTRY, new ClusterMessageHandler() {

            @Override
            public void handle(ClusterMessage message) {
                FlowRule rule = SERIALIZER.decode(message.payload());
                log.trace("received get flow entry request for {}", rule);
                FlowEntry flowEntry = getFlowEntryInternal(rule);
                try {
                    message.respond(SERIALIZER.encode(flowEntry));
                } catch (IOException e) {
                    log.error("Failed to respond back", e);
                }
            }
        });

        clusterCommunicator.addSubscriber(GET_DEVICE_FLOW_ENTRIES, new ClusterMessageHandler() {

            @Override
            public void handle(ClusterMessage message) {
                DeviceId deviceId = SERIALIZER.decode(message.payload());
                log.trace("Received get flow entries request for {} from {}", deviceId, message.sender());
                Set<FlowEntry> flowEntries = getFlowEntriesInternal(deviceId);
                try {
                    message.respond(SERIALIZER.encode(flowEntries));
                } catch (IOException e) {
                    log.error("Failed to respond to peer's getFlowEntries request", e);
                }
            }
        });

        clusterCommunicator.addSubscriber(REMOVE_FLOW_ENTRY, new ClusterMessageHandler() {

            @Override
            public void handle(ClusterMessage message) {
                FlowEntry rule = SERIALIZER.decode(message.payload());
                log.trace("received get flow entry request for {}", rule);
                FlowRuleEvent event = removeFlowRuleInternal(rule);
                try {
                    message.respond(SERIALIZER.encode(event));
                } catch (IOException e) {
                    log.error("Failed to respond back", e);
                }
            }
        });

        replicaInfoEventListener = new InternalReplicaInfoEventListener();

        replicaInfoManager.addListener(replicaInfoEventListener);

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        clusterCommunicator.removeSubscriber(REMOVE_FLOW_ENTRY);
        clusterCommunicator.removeSubscriber(GET_DEVICE_FLOW_ENTRIES);
        clusterCommunicator.removeSubscriber(GET_FLOW_ENTRY);
        clusterCommunicator.removeSubscriber(APPLY_BATCH_FLOWS);
        replicaInfoManager.removeListener(replicaInfoEventListener);
        log.info("Stopped");
    }


    // This is not a efficient operation on a distributed sharded
    // flow store. We need to revisit the need for this operation or at least
    // make it device specific.
    @Override
    public int getFlowRuleCount() {
        // implementing in-efficient operation for debugging purpose.
        int sum = 0;
        for (Device device : deviceService.getDevices()) {
            final DeviceId did = device.id();
            sum += Iterables.size(getFlowEntries(did));
        }
        return sum;
    }

    @Override
    public FlowEntry getFlowEntry(FlowRule rule) {
        ReplicaInfo replicaInfo = replicaInfoManager.getReplicaInfoFor(rule.deviceId());

        if (!replicaInfo.master().isPresent()) {
            log.warn("Failed to getFlowEntry: No master for {}", rule.deviceId());
            return null;
        }

        if (replicaInfo.master().get().equals(clusterService.getLocalNode().id())) {
            return getFlowEntryInternal(rule);
        }

        log.trace("Forwarding getFlowEntry to {}, which is the primary (master) for device {}",
                replicaInfo.master().orNull(), rule.deviceId());

        ClusterMessage message = new ClusterMessage(
                clusterService.getLocalNode().id(),
                FlowStoreMessageSubjects.GET_FLOW_ENTRY,
                SERIALIZER.encode(rule));

        try {
            Future<byte[]> responseFuture = clusterCommunicator.sendAndReceive(message, replicaInfo.master().get());
            return SERIALIZER.decode(responseFuture.get(FLOW_RULE_STORE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS));
        } catch (IOException | TimeoutException | ExecutionException | InterruptedException e) {
            log.warn("Unable to fetch flow store contents from {}", replicaInfo.master().get());
        }
        return null;
    }

    private StoredFlowEntry getFlowEntryInternal(FlowRule rule) {
        flowEntriesLock.readLock().lock();
        try {
            for (StoredFlowEntry f : flowEntries.get(rule.deviceId())) {
                if (f.equals(rule)) {
                    return f;
                }
            }
        } finally {
            flowEntriesLock.readLock().unlock();
        }
        return null;
    }

    @Override
    public Iterable<FlowEntry> getFlowEntries(DeviceId deviceId) {

        ReplicaInfo replicaInfo = replicaInfoManager.getReplicaInfoFor(deviceId);

        if (!replicaInfo.master().isPresent()) {
            log.warn("Failed to getFlowEntries: No master for {}", deviceId);
            return Collections.emptyList();
        }

        if (replicaInfo.master().get().equals(clusterService.getLocalNode().id())) {
            return getFlowEntriesInternal(deviceId);
        }

        log.trace("Forwarding getFlowEntries to {}, which is the primary (master) for device {}",
                replicaInfo.master().orNull(), deviceId);

        ClusterMessage message = new ClusterMessage(
                clusterService.getLocalNode().id(),
                GET_DEVICE_FLOW_ENTRIES,
                SERIALIZER.encode(deviceId));

        try {
            Future<byte[]> responseFuture = clusterCommunicator.sendAndReceive(message, replicaInfo.master().get());
            return SERIALIZER.decode(responseFuture.get(FLOW_RULE_STORE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS));
        } catch (IOException | TimeoutException | ExecutionException | InterruptedException e) {
            log.warn("Unable to fetch flow store contents from {}", replicaInfo.master().get());
        }
        return Collections.emptyList();
    }

    private Set<FlowEntry> getFlowEntriesInternal(DeviceId deviceId) {
        flowEntriesLock.readLock().lock();
        try {
            Collection<? extends FlowEntry> rules = flowEntries.get(deviceId);
            if (rules == null) {
                return Collections.emptySet();
            }
            return ImmutableSet.copyOf(rules);
        } finally {
            flowEntriesLock.readLock().unlock();
        }
    }

    @Override
    public void storeFlowRule(FlowRule rule) {
        storeBatch(new FlowRuleBatchOperation(Arrays.asList(new FlowRuleBatchEntry(FlowRuleOperation.ADD, rule))));
    }

    @Override
    public Future<CompletedBatchOperation> storeBatch(FlowRuleBatchOperation operation) {

        if (operation.getOperations().isEmpty()) {
            return Futures.immediateFuture(new CompletedBatchOperation(true,
                                                                       Collections.<FlowRule>emptySet()));
        }

        DeviceId deviceId = operation.getOperations().get(0).getTarget().deviceId();

        ReplicaInfo replicaInfo = replicaInfoManager.getReplicaInfoFor(deviceId);

        if (!replicaInfo.master().isPresent()) {
            log.warn("Failed to storeBatch: No master for {}", deviceId);
            // TODO: revisit if this should be "success" from Future point of view
            // with every FlowEntry failed
            return Futures.immediateFailedFuture(new IOException("Failed to storeBatch: No master for " + deviceId));
        }

        final NodeId local = clusterService.getLocalNode().id();
        if (replicaInfo.master().get().equals(local)) {
            return storeBatchInternal(operation);
        }

        log.trace("Forwarding storeBatch to {}, which is the primary (master) for device {}",
                replicaInfo.master().orNull(), deviceId);

        ClusterMessage message = new ClusterMessage(
                local,
                APPLY_BATCH_FLOWS,
                SERIALIZER.encode(operation));

        try {
            ListenableFuture<byte[]> responseFuture =
                    clusterCommunicator.sendAndReceive(message, replicaInfo.master().get());
            return Futures.transform(responseFuture, new DecodeTo<CompletedBatchOperation>(SERIALIZER));
        } catch (IOException e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    private ListenableFuture<CompletedBatchOperation>
                        storeBatchInternal(FlowRuleBatchOperation operation) {

        final List<FlowRuleBatchEntry> toRemove = new ArrayList<>();
        final List<FlowRuleBatchEntry> toAdd = new ArrayList<>();
        DeviceId did = null;


        flowEntriesLock.writeLock().lock();
        try {
            for (FlowRuleBatchEntry batchEntry : operation.getOperations()) {
                FlowRule flowRule = batchEntry.getTarget();
                FlowRuleOperation op = batchEntry.getOperator();
                if (did == null) {
                    did = flowRule.deviceId();
                }
                if (op.equals(FlowRuleOperation.REMOVE)) {
                    StoredFlowEntry entry = getFlowEntryInternal(flowRule);
                    if (entry != null) {
                        entry.setState(FlowEntryState.PENDING_REMOVE);
                        toRemove.add(batchEntry);
                    }
                } else if (op.equals(FlowRuleOperation.ADD)) {
                    StoredFlowEntry flowEntry = new DefaultFlowEntry(flowRule);
                    DeviceId deviceId = flowRule.deviceId();
                    Collection<StoredFlowEntry> ft = flowEntries.get(deviceId);

                    // always add requested FlowRule
                    // Note: 2 equal FlowEntry may have different treatment
                    ft.remove(flowEntry);
                    ft.add(flowEntry);
                    toAdd.add(batchEntry);
                }
            }
            if (toAdd.isEmpty() && toRemove.isEmpty()) {
                return Futures.immediateFuture(new CompletedBatchOperation(true, Collections.<FlowRule>emptySet()));
            }

            // create remote backup copies
            updateBackup(did, toAdd, toRemove);
        } finally {
            flowEntriesLock.writeLock().unlock();
        }

        SettableFuture<CompletedBatchOperation> r = SettableFuture.create();
        final int batchId = localBatchIdGen.incrementAndGet();

        pendingFutures.put(batchId, r);
        notifyDelegate(FlowRuleBatchEvent.requested(new FlowRuleBatchRequest(batchId, toAdd, toRemove)));

        return r;
    }

    private void updateBackup(final DeviceId deviceId,
                              final List<FlowRuleBatchEntry> toAdd,
                              final List<FlowRuleBatchEntry> list) {

        Future<?> submit = backupExecutors.submit(new UpdateBackup(deviceId, toAdd, list));

        if (syncBackup) {
            // wait for backup to complete
            try {
                submit.get();
            } catch (InterruptedException | ExecutionException e) {
                log.error("Failed to create backups", e);
            }
        }
    }

    private void updateBackup(DeviceId deviceId, List<FlowRuleBatchEntry> toAdd) {

        updateBackup(deviceId, toAdd, Collections.<FlowRuleBatchEntry>emptyList());
    }

    @Override
    public void deleteFlowRule(FlowRule rule) {
        storeBatch(new FlowRuleBatchOperation(Arrays.asList(new FlowRuleBatchEntry(FlowRuleOperation.REMOVE, rule))));
    }

    @Override
    public FlowRuleEvent addOrUpdateFlowRule(FlowEntry rule) {
        ReplicaInfo replicaInfo = replicaInfoManager.getReplicaInfoFor(rule.deviceId());
        final NodeId localId = clusterService.getLocalNode().id();
        if (localId.equals(replicaInfo.master().orNull())) {
            return addOrUpdateFlowRuleInternal(rule);
        }

        log.warn("Tried to update FlowRule {} state,"
                + " while the Node was not the master.", rule);
        return null;
    }

    private FlowRuleEvent addOrUpdateFlowRuleInternal(FlowEntry rule) {
        final DeviceId did = rule.deviceId();

        flowEntriesLock.writeLock().lock();
        try {
            // check if this new rule is an update to an existing entry
            StoredFlowEntry stored = getFlowEntryInternal(rule);
            if (stored != null) {
                stored.setBytes(rule.bytes());
                stored.setLife(rule.life());
                stored.setPackets(rule.packets());
                if (stored.state() == FlowEntryState.PENDING_ADD) {
                    stored.setState(FlowEntryState.ADDED);
                    FlowRuleBatchEntry entry =
                            new FlowRuleBatchEntry(FlowRuleOperation.ADD, stored);
                    updateBackup(did, Arrays.asList(entry));
                    return new FlowRuleEvent(Type.RULE_ADDED, rule);
                }
                return new FlowRuleEvent(Type.RULE_UPDATED, rule);
            }

            // TODO: Confirm if this behavior is correct. See SimpleFlowRuleStore
            // TODO: also update backup if the behavior is correct.
            flowEntries.put(did, new DefaultFlowEntry(rule));
        } finally {
            flowEntriesLock.writeLock().unlock();
        }
        return null;

    }

    @Override
    public FlowRuleEvent removeFlowRule(FlowEntry rule) {
        final DeviceId deviceId = rule.deviceId();
        ReplicaInfo replicaInfo = replicaInfoManager.getReplicaInfoFor(deviceId);

        final NodeId localId = clusterService.getLocalNode().id();
        if (localId.equals(replicaInfo.master().orNull())) {
            // bypass and handle it locally
            return removeFlowRuleInternal(rule);
        }

        if (!replicaInfo.master().isPresent()) {
            log.warn("Failed to removeFlowRule: No master for {}", deviceId);
            // TODO: revisit if this should be null (="no-op") or Exception
            return null;
        }

        log.trace("Forwarding removeFlowRule to {}, which is the primary (master) for device {}",
                  replicaInfo.master().orNull(), deviceId);

        ClusterMessage message = new ClusterMessage(
                  clusterService.getLocalNode().id(),
                  REMOVE_FLOW_ENTRY,
                  SERIALIZER.encode(rule));

        try {
            Future<byte[]> responseFuture = clusterCommunicator.sendAndReceive(message, replicaInfo.master().get());
            return SERIALIZER.decode(responseFuture.get(FLOW_RULE_STORE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS));
        } catch (IOException | TimeoutException | ExecutionException | InterruptedException e) {
            // TODO: Retry against latest master or throw a FlowStoreException
            throw new RuntimeException(e);
        }
    }

    private FlowRuleEvent removeFlowRuleInternal(FlowEntry rule) {
        final DeviceId deviceId = rule.deviceId();
        flowEntriesLock.writeLock().lock();
        try {
            // This is where one could mark a rule as removed and still keep it in the store.
            final boolean removed = flowEntries.remove(deviceId, rule);
            FlowRuleBatchEntry entry =
                    new FlowRuleBatchEntry(FlowRuleOperation.REMOVE, rule);
            updateBackup(deviceId, Collections.<FlowRuleBatchEntry>emptyList(), Arrays.asList(entry));
            if (removed) {
                return new FlowRuleEvent(RULE_REMOVED, rule);
            } else {
                return null;
            }
        } finally {
            flowEntriesLock.writeLock().unlock();
        }
    }

    @Override
    public void batchOperationComplete(FlowRuleBatchEvent event) {
        final Integer batchId = event.subject().batchId();
        SettableFuture<CompletedBatchOperation> future
            = pendingFutures.getIfPresent(batchId);
        if (future != null) {
            future.set(event.result());
            pendingFutures.invalidate(batchId);
        }
        notifyDelegate(event);
    }

    private void loadFromBackup(final DeviceId did) {

        flowEntriesLock.writeLock().lock();
        try {
            log.debug("Loading FlowRules for {} from backups", did);
            SMap<FlowId, ImmutableList<StoredFlowEntry>> backupFlowTable = smaps.get(did);
            for (Entry<FlowId, ImmutableList<StoredFlowEntry>> e
                    : backupFlowTable.entrySet()) {

                log.trace("loading {}", e.getValue());
                for (StoredFlowEntry entry : e.getValue()) {
                    flowEntries.remove(did, entry);
                    flowEntries.put(did, entry);
                }
            }
        } catch (ExecutionException e) {
            log.error("Failed to load backup flowtable for {}", did, e);
        } finally {
            flowEntriesLock.writeLock().unlock();
        }
    }

    private void removeFromPrimary(final DeviceId did) {
        Collection<StoredFlowEntry> removed = null;
        flowEntriesLock.writeLock().lock();
        try {
            removed = flowEntries.removeAll(did);
        } finally {
            flowEntriesLock.writeLock().unlock();
        }
        log.trace("removedFromPrimary {}", removed);
    }

    private static final class TimeoutFuture
        implements RemovalListener<Integer, SettableFuture<CompletedBatchOperation>> {
        @Override
        public void onRemoval(RemovalNotification<Integer, SettableFuture<CompletedBatchOperation>> notification) {
            // wrapping in ExecutionException to support Future.get
            notification.getValue()
                .setException(new ExecutionException("Timed out",
                                                     new TimeoutException()));
        }
    }

    private final class OnStoreBatch implements ClusterMessageHandler {
        private final NodeId local;

        private OnStoreBatch(NodeId local) {
            this.local = local;
        }

        @Override
        public void handle(final ClusterMessage message) {
            FlowRuleBatchOperation operation = SERIALIZER.decode(message.payload());
            log.debug("received batch request {}", operation);

            final DeviceId deviceId = operation.getOperations().get(0).getTarget().deviceId();
            ReplicaInfo replicaInfo = replicaInfoManager.getReplicaInfoFor(deviceId);
            if (!local.equals(replicaInfo.master().orNull())) {

                Set<FlowRule> failures = new HashSet<>(operation.size());
                for (FlowRuleBatchEntry op : operation.getOperations()) {
                    failures.add(op.getTarget());
                }
                CompletedBatchOperation allFailed = new CompletedBatchOperation(false, failures);
                // This node is no longer the master, respond as all failed.
                // TODO: we might want to wrap response in envelope
                // to distinguish sw programming failure and hand over
                // it make sense in the latter case to retry immediately.
                try {
                    message.respond(SERIALIZER.encode(allFailed));
                } catch (IOException e) {
                    log.error("Failed to respond back", e);
                }
                return;
            }

            final ListenableFuture<CompletedBatchOperation> f = storeBatchInternal(operation);

            f.addListener(new Runnable() {

                @Override
                public void run() {
                    CompletedBatchOperation result;
                    try {
                        result = f.get();
                    } catch (InterruptedException | ExecutionException e) {
                        log.error("Batch operation failed", e);
                        // create everything failed response
                        Set<FlowRule> failures = new HashSet<>(operation.size());
                        for (FlowRuleBatchEntry op : operation.getOperations()) {
                            failures.add(op.getTarget());
                        }
                        result = new CompletedBatchOperation(false, failures);
                    }
                    try {
                        message.respond(SERIALIZER.encode(result));
                    } catch (IOException e) {
                        log.error("Failed to respond back", e);
                    }
                }
            }, futureListeners);
        }
    }

    private final class SMapLoader
        extends CacheLoader<DeviceId, SMap<FlowId, ImmutableList<StoredFlowEntry>>> {

        @Override
        public SMap<FlowId, ImmutableList<StoredFlowEntry>> load(DeviceId id)
                throws Exception {
            IMap<byte[], byte[]> map = theInstance.getMap("flowtable_" + id.toString());
            return new SMap<FlowId, ImmutableList<StoredFlowEntry>>(map, SERIALIZER);
        }
    }

    private final class InternalReplicaInfoEventListener
        implements ReplicaInfoEventListener {

        @Override
        public void event(ReplicaInfoEvent event) {
            final NodeId local = clusterService.getLocalNode().id();
            final DeviceId did = event.subject();
            final ReplicaInfo rInfo = event.replicaInfo();

            switch (event.type()) {
            case MASTER_CHANGED:
                if (local.equals(rInfo.master().orNull())) {
                    // This node is the new master, populate local structure
                    // from backup
                    loadFromBackup(did);
                } else {
                    // This node is no longer the master holder,
                    // clean local structure
                    removeFromPrimary(did);
                    // TODO: probably should stop pending backup activities in
                    // executors to avoid overwriting with old value
                }
                break;
            default:
                break;

            }
        }
    }

    // Task to update FlowEntries in backup HZ store
    // TODO: Should be refactored to contain only one list and not
    //      toAdd and toRemove
    private final class UpdateBackup implements Runnable {

        private final DeviceId deviceId;
        private final List<FlowRuleBatchEntry> toAdd;
        private final List<FlowRuleBatchEntry> toRemove;

        public UpdateBackup(DeviceId deviceId,
                             List<FlowRuleBatchEntry> toAdd,
                             List<FlowRuleBatchEntry> list) {
            this.deviceId = checkNotNull(deviceId);
            this.toAdd = checkNotNull(toAdd);
            this.toRemove = checkNotNull(list);
        }

        @Override
        public void run() {
            try {
                log.trace("update backup {} +{} -{}", deviceId, toAdd, toRemove);
                final SMap<FlowId, ImmutableList<StoredFlowEntry>> backupFlowTable = smaps.get(deviceId);
                // Following should be rewritten using async APIs
                for (FlowRuleBatchEntry bEntry : toAdd) {
                    final FlowRule entry = bEntry.getTarget();
                    final FlowId id = entry.id();
                    ImmutableList<StoredFlowEntry> original = backupFlowTable.get(id);
                    List<StoredFlowEntry> list = new ArrayList<>();
                    if (original != null) {
                        list.addAll(original);
                    }

                    list.remove(bEntry.getTarget());
                    list.add((StoredFlowEntry) entry);

                    ImmutableList<StoredFlowEntry> newValue = ImmutableList.copyOf(list);
                    boolean success;
                    if (original == null) {
                        success = (backupFlowTable.putIfAbsent(id, newValue) == null);
                    } else {
                        success = backupFlowTable.replace(id, original, newValue);
                    }
                    if (!success) {
                        log.error("Updating backup failed.");
                    }
                }
                for (FlowRuleBatchEntry bEntry : toRemove) {
                    final FlowRule entry = bEntry.getTarget();
                    final FlowId id = entry.id();
                    ImmutableList<StoredFlowEntry> original = backupFlowTable.get(id);
                    List<StoredFlowEntry> list = new ArrayList<>();
                    if (original != null) {
                        list.addAll(original);
                    }

                    list.remove(bEntry.getTarget());

                    ImmutableList<StoredFlowEntry> newValue = ImmutableList.copyOf(list);
                    boolean success;
                    if (original == null) {
                        success = (backupFlowTable.putIfAbsent(id, newValue) == null);
                    } else {
                        success = backupFlowTable.replace(id, original, newValue);
                    }
                    if (!success) {
                        log.error("Updating backup failed.");
                    }
                }
            } catch (ExecutionException e) {
                log.error("Failed to write to backups", e);
            }

        }
    }
}
