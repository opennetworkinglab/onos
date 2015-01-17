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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ListenableFuture;
import com.hazelcast.core.IMap;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.KryoNamespace;
import org.onlab.util.NewConcurrentHashMap;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.CoreService;
import org.onosproject.core.IdGenerator;
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
import org.onosproject.net.flow.FlowRuleService;
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
import org.onosproject.store.serializers.KryoSerializer;
import org.onosproject.store.serializers.StoreSerializer;
import org.onosproject.store.serializers.impl.DistributedStoreSerializers;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.lang3.concurrent.ConcurrentUtils.createIfAbsentUnchecked;
import static org.onlab.util.Tools.namedThreads;
import static org.onosproject.net.flow.FlowRuleEvent.Type.RULE_REMOVED;
import static org.onosproject.store.flow.impl.FlowStoreMessageSubjects.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Manages inventory of flow rules using a distributed state management protocol.
 */
@Component(immediate = true)
@Service
public class DistributedFlowRuleStore
        extends AbstractHazelcastStore<FlowRuleBatchEvent, FlowRuleStoreDelegate>
        implements FlowRuleStore {

    private final Logger log = getLogger(getClass());

    private InternalFlowTable flowTable = new InternalFlowTable();

    /*private final ConcurrentMap<DeviceId, ConcurrentMap<FlowId, Set<StoredFlowEntry>>>
            flowEntries = new ConcurrentHashMap<>();*/

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ReplicaInfoService replicaInfoManager;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterCommunicationService clusterCommunicator;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    private Map<Long, NodeId> pendingResponses = Maps.newConcurrentMap();

    // Cache of SMaps used for backup data.  each SMap contain device flow table
    private LoadingCache<DeviceId, SMap<FlowId, ImmutableList<StoredFlowEntry>>> smaps;


    private final ExecutorService backupExecutors =
            Executors.newSingleThreadExecutor(namedThreads("onos-async-backups"));

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

    private IdGenerator idGenerator;

    @Override
    @Activate
    public void activate() {

        super.serializer = SERIALIZER;
        super.theInstance = storeService.getHazelcastInstance();

        idGenerator = coreService.getIdGenerator(FlowRuleService.FLOW_OP_TOPIC);

        // Cache to create SMap on demand
        smaps = CacheBuilder.newBuilder()
                .softValues()
                .build(new SMapLoader());

        final NodeId local = clusterService.getLocalNode().id();

        clusterCommunicator.addSubscriber(APPLY_BATCH_FLOWS, new OnStoreBatch(local));

        clusterCommunicator.addSubscriber(REMOTE_APPLY_COMPLETED, new ClusterMessageHandler() {
            @Override
            public void handle(ClusterMessage message) {
                FlowRuleBatchEvent event = SERIALIZER.decode(message.payload());
                log.trace("received completed notification for {}", event);
                notifyDelegate(event);
            }
        });

        clusterCommunicator.addSubscriber(GET_FLOW_ENTRY, new ClusterMessageHandler() {

            @Override
            public void handle(ClusterMessage message) {
                FlowRule rule = SERIALIZER.decode(message.payload());
                log.trace("received get flow entry request for {}", rule);
                FlowEntry flowEntry = flowTable.getFlowEntry(rule); //getFlowEntryInternal(rule);
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
                Set<FlowEntry> flowEntries = flowTable.getFlowEntries(deviceId);
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
            return flowTable.getFlowEntry(rule);
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



    @Override
    public Iterable<FlowEntry> getFlowEntries(DeviceId deviceId) {

        ReplicaInfo replicaInfo = replicaInfoManager.getReplicaInfoFor(deviceId);

        if (!replicaInfo.master().isPresent()) {
            log.warn("Failed to getFlowEntries: No master for {}", deviceId);
            return Collections.emptyList();
        }

        if (replicaInfo.master().get().equals(clusterService.getLocalNode().id())) {
            return flowTable.getFlowEntries(deviceId);
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



    @Override
    public void storeFlowRule(FlowRule rule) {
        storeBatch(new FlowRuleBatchOperation(
                Arrays.asList(new FlowRuleBatchEntry(FlowRuleOperation.ADD, rule)),
                rule.deviceId(), idGenerator.getNewId()));
    }

    @Override
    public void storeBatch(FlowRuleBatchOperation operation) {


        if (operation.getOperations().isEmpty()) {

            notifyDelegate(FlowRuleBatchEvent.completed(
                    new FlowRuleBatchRequest(operation.id(), Collections.emptySet()),
                    new CompletedBatchOperation(true, Collections.emptySet(),
                                                operation.deviceId())));
            return;
        }

        DeviceId deviceId = operation.getOperations().get(0).target().deviceId();

        ReplicaInfo replicaInfo = replicaInfoManager.getReplicaInfoFor(deviceId);

        if (!replicaInfo.master().isPresent()) {
            log.warn("Failed to storeBatch: No master for {}", deviceId);

            Set<FlowRule> allFailures = operation.getOperations().stream()
                    .map(op -> op.getTarget())
                    .collect(Collectors.toSet());

            notifyDelegate(FlowRuleBatchEvent.completed(
                    new FlowRuleBatchRequest(operation.id(), Collections.emptySet()),
                    new CompletedBatchOperation(false, allFailures, operation.deviceId())));
            return;
        }

        final NodeId local = clusterService.getLocalNode().id();
        if (replicaInfo.master().get().equals(local)) {
            storeBatchInternal(operation);
            return;
        }

        log.trace("Forwarding storeBatch to {}, which is the primary (master) for device {}",
                  replicaInfo.master().orNull(), deviceId);

        ClusterMessage message = new ClusterMessage(
                local,
                APPLY_BATCH_FLOWS,
                SERIALIZER.encode(operation));

        //CompletedBatchOperation response;
        try {
            ListenableFuture<byte[]> responseFuture =
                    clusterCommunicator.sendAndReceive(message, replicaInfo.master().get());
            /*response =
                    Futures.transform(responseFuture,
                                      new DecodeTo<CompletedBatchOperation>(SERIALIZER))
                            .get(500 * operation.size(), TimeUnit.MILLISECONDS);

            notifyDelegate(FlowRuleBatchEvent.completed(
                    new FlowRuleBatchRequest(operation.id(), Collections.emptySet()), response));*/

        } catch (IOException /*| InterruptedException | ExecutionException | TimeoutException*/ e) {
            log.warn("Failed to storeBatch: {}", e.getMessage());

            Set<FlowRule> allFailures = operation.getOperations().stream()
                    .map(op -> op.getTarget())
                    .collect(Collectors.toSet());

            notifyDelegate(FlowRuleBatchEvent.completed(
                    new FlowRuleBatchRequest(operation.id(), Collections.emptySet()),
                    new CompletedBatchOperation(false, allFailures, deviceId)));
            return;
        }

    }

    private void storeBatchInternal(FlowRuleBatchOperation operation) {

        final DeviceId did = operation.deviceId();
        //final Collection<FlowEntry> ft = flowTable.getFlowEntries(did);
        Set<FlowRuleBatchEntry> currentOps;


        currentOps = operation.getOperations().stream().map(
                op -> {
                    StoredFlowEntry entry;
                    switch (op.getOperator()) {
                        case ADD:
                            entry = new DefaultFlowEntry(op.getTarget());
                            // always add requested FlowRule 
                            // Note: 2 equal FlowEntry may have different treatment
                            flowTable.remove(entry.deviceId(), entry);
                            flowTable.add(entry);

                            return op;
                        case REMOVE:
                            entry = flowTable.getFlowEntry(op.target());
                            if (entry != null) {
                                entry.setState(FlowEntryState.PENDING_REMOVE);
                                return op;
                            }
                            break;
                        case MODIFY:
                            //TODO: figure this out at some point
                            break;
                        default:
                            log.warn("Unknown flow operation operator: {}", op.getOperator());
                    }
                    return null;
                }
        ).filter(op -> op != null).collect(Collectors.toSet());
        if (currentOps.isEmpty()) {
            batchOperationComplete(FlowRuleBatchEvent.completed(
                    new FlowRuleBatchRequest(operation.id(), Collections.emptySet()),
                    new CompletedBatchOperation(true, Collections.emptySet(), did)));
            return;
        }
        updateBackup(did, currentOps);


        notifyDelegate(FlowRuleBatchEvent.requested(new
             FlowRuleBatchRequest(operation.id(), currentOps), operation.deviceId()));


    }

    private void updateBackup(DeviceId deviceId, final Set<FlowRuleBatchEntry> entries) {
        Future<?> backup = backupExecutors.submit(new UpdateBackup(deviceId, entries));

        if (syncBackup) {
            // wait for backup to complete
            try {
                backup.get();
            } catch (InterruptedException | ExecutionException e) {
                log.error("Failed to create backups", e);
            }
        }
    }

    @Override
    public void deleteFlowRule(FlowRule rule) {
        storeBatch(
                new FlowRuleBatchOperation(
                        Arrays.asList(
                                new FlowRuleBatchEntry(
                                        FlowRuleOperation.REMOVE,
                                        rule)), rule.deviceId(), idGenerator.getNewId()));
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


        // check if this new rule is an update to an existing entry
        StoredFlowEntry stored = flowTable.getFlowEntry(rule);
        if (stored != null) {
            stored.setBytes(rule.bytes());
            stored.setLife(rule.life());
            stored.setPackets(rule.packets());
            if (stored.state() == FlowEntryState.PENDING_ADD) {
                stored.setState(FlowEntryState.ADDED);
                FlowRuleBatchEntry entry =
                        new FlowRuleBatchEntry(FlowRuleOperation.ADD, stored);
                updateBackup(did, Sets.newHashSet(entry));
                return new FlowRuleEvent(Type.RULE_ADDED, rule);
            }
            return new FlowRuleEvent(Type.RULE_UPDATED, rule);
        }

        // TODO: Confirm if this behavior is correct. See SimpleFlowRuleStore
        // TODO: also update backup if the behavior is correct.
        flowTable.add(rule);


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
        // This is where one could mark a rule as removed and still keep it in the store.
        final boolean removed = flowTable.remove(deviceId, rule); //flowEntries.remove(deviceId, rule);
        FlowRuleBatchEntry entry =
                new FlowRuleBatchEntry(FlowRuleOperation.REMOVE, rule);
        updateBackup(deviceId, Sets.newHashSet(entry));
        if (removed) {
            return new FlowRuleEvent(RULE_REMOVED, rule);
        } else {
            return null;
        }

    }

    @Override
    public void batchOperationComplete(FlowRuleBatchEvent event) {
        //FIXME: need a per device pending response

        NodeId nodeId = pendingResponses.remove(event.subject().batchId());
        if (nodeId == null) {
            notifyDelegate(event);
        } else {
            try {
                ClusterMessage message = new ClusterMessage(
                        clusterService.getLocalNode().id(),
                        REMOTE_APPLY_COMPLETED,
                        SERIALIZER.encode(event));
                clusterCommunicator.sendAndReceive(message, nodeId);
            } catch (IOException e) {
                log.warn("Failed to respond to peer for batch operation result");
            }
        }
    }

    private void loadFromBackup(final DeviceId did) {


        try {
            log.debug("Loading FlowRules for {} from backups", did);
            SMap<FlowId, ImmutableList<StoredFlowEntry>> backupFlowTable = smaps.get(did);
            for (Entry<FlowId, ImmutableList<StoredFlowEntry>> e
                    : backupFlowTable.entrySet()) {

                log.trace("loading {}", e.getValue());
                for (StoredFlowEntry entry : e.getValue()) {
                    flowTable.getFlowEntriesById(entry).remove(entry);
                    flowTable.getFlowEntriesById(entry).add(entry);


                }
            }
        } catch (ExecutionException e) {
            log.error("Failed to load backup flowtable for {}", did, e);
        }
    }

    private void removeFromPrimary(final DeviceId did) {
        flowTable.clearDevice(did);
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

            final DeviceId deviceId = operation.deviceId();
            ReplicaInfo replicaInfo = replicaInfoManager.getReplicaInfoFor(deviceId);
            if (!local.equals(replicaInfo.master().orNull())) {

                Set<FlowRule> failures = new HashSet<>(operation.size());
                for (FlowRuleBatchEntry op : operation.getOperations()) {
                    failures.add(op.target());
                }
                CompletedBatchOperation allFailed = new CompletedBatchOperation(false, failures, deviceId);
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


            pendingResponses.put(operation.id(), message.sender());
            storeBatchInternal(operation);

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
    private final class UpdateBackup implements Runnable {

        private final DeviceId deviceId;
        private final Set<FlowRuleBatchEntry> ops;


        public UpdateBackup(DeviceId deviceId,
                            Set<FlowRuleBatchEntry> ops) {
            this.deviceId = checkNotNull(deviceId);
            this.ops = checkNotNull(ops);

        }

        @Override
        public void run() {
            try {
                log.trace("update backup {} {}", deviceId, ops
                );
                final SMap<FlowId, ImmutableList<StoredFlowEntry>> backupFlowTable = smaps.get(deviceId);


                ops.stream().forEach(
                        op -> {
                            final FlowRule entry = op.getTarget();
                            final FlowId id = entry.id();
                            ImmutableList<StoredFlowEntry> original = backupFlowTable.get(id);
                            List<StoredFlowEntry> list = new ArrayList<>();
                            if (original != null) {
                                list.addAll(original);
                            }
                            list.remove(op.getTarget());
                            if (op.getOperator() == FlowRuleOperation.ADD) {
                                list.add((StoredFlowEntry) entry);
                            }

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
                );
            } catch (ExecutionException e) {
                log.error("Failed to write to backups", e);
            }

        }
    }

    private class InternalFlowTable {

        /*
            TODO: This needs to be cleaned up. Perhaps using the eventually consistent
            map when it supports distributed to a sequence of instances.
         */


        private final ConcurrentMap<DeviceId, ConcurrentMap<FlowId, Set<StoredFlowEntry>>>
                flowEntries = new ConcurrentHashMap<>();


        private NewConcurrentHashMap<FlowId, Set<StoredFlowEntry>> lazyEmptyFlowTable() {
            return NewConcurrentHashMap.<FlowId, Set<StoredFlowEntry>>ifNeeded();
        }

        /**
         * Returns the flow table for specified device.
         *
         * @param deviceId identifier of the device
         * @return Map representing Flow Table of given device.
         */
        private ConcurrentMap<FlowId, Set<StoredFlowEntry>> getFlowTable(DeviceId deviceId) {
            return createIfAbsentUnchecked(flowEntries,
                                           deviceId, lazyEmptyFlowTable());
        }

        private Set<StoredFlowEntry> getFlowEntriesInternal(DeviceId deviceId, FlowId flowId) {
            final ConcurrentMap<FlowId, Set<StoredFlowEntry>> flowTable = getFlowTable(deviceId);
            Set<StoredFlowEntry> r = flowTable.get(flowId);
            if (r == null) {
                final Set<StoredFlowEntry> concurrentlyAdded;
                r = new CopyOnWriteArraySet<>();
                concurrentlyAdded = flowTable.putIfAbsent(flowId, r);
                if (concurrentlyAdded != null) {
                    return concurrentlyAdded;
                }
            }
            return r;
        }

        private StoredFlowEntry getFlowEntryInternal(FlowRule rule) {
            for (StoredFlowEntry f : getFlowEntriesInternal(rule.deviceId(), rule.id())) {
                if (f.equals(rule)) {
                    return f;
                }
            }
            return null;
        }

        private Set<FlowEntry> getFlowEntriesInternal(DeviceId deviceId) {
            return getFlowTable(deviceId).values().stream()
                    .flatMap((list -> list.stream())).collect(Collectors.toSet());

        }


        public StoredFlowEntry getFlowEntry(FlowRule rule) {
            return getFlowEntryInternal(rule);
        }

        public Set<FlowEntry> getFlowEntries(DeviceId deviceId) {
            return getFlowEntriesInternal(deviceId);
        }

        public Set<StoredFlowEntry> getFlowEntriesById(FlowEntry entry) {
            return getFlowEntriesInternal(entry.deviceId(), entry.id());
        }

        public void add(FlowEntry rule) {
            ((CopyOnWriteArraySet)
                    getFlowEntriesInternal(rule.deviceId(), rule.id())).add(rule);
        }

        public boolean remove(DeviceId deviceId, FlowEntry rule) {
            return ((CopyOnWriteArraySet)
                    getFlowEntriesInternal(deviceId, rule.id())).remove(rule);
            //return flowEntries.remove(deviceId, rule);
        }

        public void clearDevice(DeviceId did) {
            flowEntries.remove(did);
        }
    }


}
