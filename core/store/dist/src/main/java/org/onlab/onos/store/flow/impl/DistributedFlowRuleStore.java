package org.onlab.onos.store.flow.impl;

import static org.onlab.onos.net.flow.FlowRuleEvent.Type.RULE_REMOVED;
import static org.slf4j.LoggerFactory.getLogger;
import static org.onlab.onos.store.flow.impl.FlowStoreMessageSubjects.*;
import static org.onlab.util.Tools.namedThreads;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.cluster.ClusterService;
import org.onlab.onos.net.Device;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.device.DeviceService;
import org.onlab.onos.net.flow.CompletedBatchOperation;
import org.onlab.onos.net.flow.DefaultFlowEntry;
import org.onlab.onos.net.flow.FlowEntry;
import org.onlab.onos.net.flow.FlowEntry.FlowEntryState;
import org.onlab.onos.net.flow.FlowRule;
import org.onlab.onos.net.flow.FlowRuleBatchEntry;
import org.onlab.onos.net.flow.FlowRuleBatchEvent;
import org.onlab.onos.net.flow.FlowRuleBatchOperation;
import org.onlab.onos.net.flow.FlowRuleBatchRequest;
import org.onlab.onos.net.flow.FlowRuleEvent;
import org.onlab.onos.net.flow.FlowRuleBatchEntry.FlowRuleOperation;
import org.onlab.onos.net.flow.FlowRuleEvent.Type;
import org.onlab.onos.net.flow.FlowRuleStore;
import org.onlab.onos.net.flow.FlowRuleStoreDelegate;
import org.onlab.onos.net.flow.StoredFlowEntry;
import org.onlab.onos.store.AbstractStore;
import org.onlab.onos.store.cluster.messaging.ClusterCommunicationService;
import org.onlab.onos.store.cluster.messaging.ClusterMessage;
import org.onlab.onos.store.cluster.messaging.ClusterMessageHandler;
import org.onlab.onos.store.flow.ReplicaInfo;
import org.onlab.onos.store.flow.ReplicaInfoService;
import org.onlab.onos.store.serializers.DistributedStoreSerializers;
import org.onlab.onos.store.serializers.KryoSerializer;
import org.onlab.util.KryoNamespace;
import org.slf4j.Logger;

import com.google.common.base.Function;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

/**
 * Manages inventory of flow rules using a distributed state management protocol.
 */
@Component(immediate = true)
@Service
public class DistributedFlowRuleStore
        extends AbstractStore<FlowRuleBatchEvent, FlowRuleStoreDelegate>
        implements FlowRuleStore {

    private final Logger log = getLogger(getClass());

    // store entries as a pile of rules, no info about device tables
    private final Multimap<DeviceId, StoredFlowEntry> flowEntries =
            ArrayListMultimap.<DeviceId, StoredFlowEntry>create();

    private final Multimap<Short, FlowRule> flowEntriesById =
            ArrayListMultimap.<Short, FlowRule>create();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ReplicaInfoService replicaInfoManager;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterCommunicationService clusterCommunicator;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    private final AtomicInteger localBatchIdGen = new AtomicInteger();

    // TODO: make this configurable
    private int pendingFutureTimeoutMinutes = 5;

    private Cache<Integer, SettableFuture<CompletedBatchOperation>> pendingFutures =
            CacheBuilder.newBuilder()
                .expireAfterWrite(pendingFutureTimeoutMinutes, TimeUnit.MINUTES)
                // TODO Explicitly fail the future if expired?
                //.removalListener(listener)
                .build();


    private final ExecutorService futureListeners =
            Executors.newCachedThreadPool(namedThreads("flowstore-peer-responders"));


    protected static final KryoSerializer SERIALIZER = new KryoSerializer() {
        @Override
        protected void setupKryoPool() {
            serializerPool = KryoNamespace.newBuilder()
                    .register(DistributedStoreSerializers.COMMON)
                    .build()
                    .populate(1);
        }
    };

    // TODO: make this configurable
    private static final long FLOW_RULE_STORE_TIMEOUT_MILLIS = 5000;

    @Activate
    public void activate() {
        clusterCommunicator.addSubscriber(APPLY_BATCH_FLOWS, new ClusterMessageHandler() {

            @Override
            public void handle(final ClusterMessage message) {
                FlowRuleBatchOperation operation = SERIALIZER.decode(message.payload());
                log.info("received batch request {}", operation);
                final ListenableFuture<CompletedBatchOperation> f = storeBatchInternal(operation);

                f.addListener(new Runnable() {

                    @Override
                    public void run() {
                         CompletedBatchOperation result = Futures.getUnchecked(f);
                        try {
                            message.respond(SERIALIZER.encode(result));
                        } catch (IOException e) {
                            log.error("Failed to respond back", e);
                        }
                    }
                }, futureListeners);
            }
        });

        clusterCommunicator.addSubscriber(GET_FLOW_ENTRY, new ClusterMessageHandler() {

            @Override
            public void handle(ClusterMessage message) {
                FlowRule rule = SERIALIZER.decode(message.payload());
                log.info("received get flow entry request for {}", rule);
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
                log.info("Received get flow entries request for {} from {}", deviceId, message.sender());
                Set<FlowEntry> flowEntries = getFlowEntriesInternal(deviceId);
                try {
                    message.respond(SERIALIZER.encode(flowEntries));
                } catch (IOException e) {
                    log.error("Failed to respond to peer's getFlowEntries request", e);
                }
            }
        });

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }


    // TODO: This is not a efficient operation on a distributed sharded
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
    public synchronized FlowEntry getFlowEntry(FlowRule rule) {
        ReplicaInfo replicaInfo = replicaInfoManager.getReplicaInfoFor(rule.deviceId());
        if (replicaInfo.master().get().equals(clusterService.getLocalNode().id())) {
            return getFlowEntryInternal(rule);
        }

        log.info("Forwarding getFlowEntry to {}, which is the primary (master) for device {}",
                replicaInfo.master().orNull(), rule.deviceId());

        ClusterMessage message = new ClusterMessage(
                clusterService.getLocalNode().id(),
                FlowStoreMessageSubjects.GET_FLOW_ENTRY,
                SERIALIZER.encode(rule));

        try {
            Future<byte[]> responseFuture = clusterCommunicator.sendAndReceive(message, replicaInfo.master().get());
            return SERIALIZER.decode(responseFuture.get(FLOW_RULE_STORE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS));
        } catch (IOException | TimeoutException | ExecutionException | InterruptedException e) {
            // FIXME: throw a FlowStoreException
            throw new RuntimeException(e);
        }
    }

    private synchronized StoredFlowEntry getFlowEntryInternal(FlowRule rule) {
        for (StoredFlowEntry f : flowEntries.get(rule.deviceId())) {
            if (f.equals(rule)) {
                return f;
            }
        }
        return null;
    }

    @Override
    public synchronized Iterable<FlowEntry> getFlowEntries(DeviceId deviceId) {

        ReplicaInfo replicaInfo = replicaInfoManager.getReplicaInfoFor(deviceId);
        if (replicaInfo.master().get().equals(clusterService.getLocalNode().id())) {
            return getFlowEntriesInternal(deviceId);
        }

        log.info("Forwarding getFlowEntries to {}, which is the primary (master) for device {}",
                replicaInfo.master().orNull(), deviceId);

        ClusterMessage message = new ClusterMessage(
                clusterService.getLocalNode().id(),
                GET_DEVICE_FLOW_ENTRIES,
                SERIALIZER.encode(deviceId));

        try {
            Future<byte[]> responseFuture = clusterCommunicator.sendAndReceive(message, replicaInfo.master().get());
            return SERIALIZER.decode(responseFuture.get(FLOW_RULE_STORE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS));
        } catch (IOException | TimeoutException | ExecutionException | InterruptedException e) {
            // FIXME: throw a FlowStoreException
            throw new RuntimeException(e);
        }
    }

    private Set<FlowEntry> getFlowEntriesInternal(DeviceId deviceId) {
        Collection<? extends FlowEntry> rules = flowEntries.get(deviceId);
        if (rules == null) {
            return Collections.emptySet();
        }
        return ImmutableSet.copyOf(rules);
    }

    @Override
    public void storeFlowRule(FlowRule rule) {
        storeBatch(new FlowRuleBatchOperation(Arrays.asList(new FlowRuleBatchEntry(FlowRuleOperation.ADD, rule))));
    }

    @Override
    public Future<CompletedBatchOperation> storeBatch(FlowRuleBatchOperation operation) {
        if (operation.getOperations().isEmpty()) {
            return Futures.immediateFuture(new CompletedBatchOperation(true, Collections.<FlowEntry>emptySet()));
        }

        DeviceId deviceId = operation.getOperations().get(0).getTarget().deviceId();

        ReplicaInfo replicaInfo = replicaInfoManager.getReplicaInfoFor(deviceId);

        if (replicaInfo.master().get().equals(clusterService.getLocalNode().id())) {
            return storeBatchInternal(operation);
        }

        log.info("Forwarding storeBatch to {}, which is the primary (master) for device {}",
                replicaInfo.master().orNull(), deviceId);

        ClusterMessage message = new ClusterMessage(
                clusterService.getLocalNode().id(),
                APPLY_BATCH_FLOWS,
                SERIALIZER.encode(operation));

        try {
            ListenableFuture<byte[]> responseFuture =
                    clusterCommunicator.sendAndReceive(message, replicaInfo.master().get());
            return Futures.transform(responseFuture, new Function<byte[], CompletedBatchOperation>() {
                @Override
                public CompletedBatchOperation apply(byte[] input) {
                    return SERIALIZER.decode(input);
                }
            });
        } catch (IOException e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    private ListenableFuture<CompletedBatchOperation> storeBatchInternal(FlowRuleBatchOperation operation) {
        List<FlowEntry> toRemove = new ArrayList<>();
        List<FlowEntry> toAdd = new ArrayList<>();
        // TODO: backup changes to hazelcast map
        for (FlowRuleBatchEntry batchEntry : operation.getOperations()) {
            FlowRule flowRule = batchEntry.getTarget();
            FlowRuleOperation op = batchEntry.getOperator();
            if (op.equals(FlowRuleOperation.REMOVE)) {
                StoredFlowEntry entry = getFlowEntryInternal(flowRule);
                if (entry != null) {
                    entry.setState(FlowEntryState.PENDING_REMOVE);
                    toRemove.add(entry);
                }
            } else if (op.equals(FlowRuleOperation.ADD)) {
                StoredFlowEntry flowEntry = new DefaultFlowEntry(flowRule);
                DeviceId deviceId = flowRule.deviceId();
                if (!flowEntries.containsEntry(deviceId, flowEntry)) {
                    flowEntries.put(deviceId, flowEntry);
                    flowEntriesById.put(flowRule.appId(), flowEntry);
                    toAdd.add(flowEntry);
                }
            }
        }
        if (toAdd.isEmpty() && toRemove.isEmpty()) {
            return Futures.immediateFuture(new CompletedBatchOperation(true, Collections.<FlowEntry>emptySet()));
        }

        SettableFuture<CompletedBatchOperation> r = SettableFuture.create();
        final int batchId = localBatchIdGen.incrementAndGet();

        pendingFutures.put(batchId, r);
        notifyDelegate(FlowRuleBatchEvent.requested(new FlowRuleBatchRequest(batchId, toAdd, toRemove)));
        return r;
    }

    @Override
    public void deleteFlowRule(FlowRule rule) {
        storeBatch(new FlowRuleBatchOperation(Arrays.asList(new FlowRuleBatchEntry(FlowRuleOperation.REMOVE, rule))));
    }

    @Override
    public FlowRuleEvent addOrUpdateFlowRule(FlowEntry rule) {
        ReplicaInfo replicaInfo = replicaInfoManager.getReplicaInfoFor(rule.deviceId());
        if (replicaInfo.master().get().equals(clusterService.getLocalNode().id())) {
            return addOrUpdateFlowRuleInternal(rule);
        }

        log.error("Tried to update FlowRule {} state,"
                + " while the Node was not the master.", rule);
        return null;
    }

    private synchronized FlowRuleEvent addOrUpdateFlowRuleInternal(FlowEntry rule) {
        DeviceId did = rule.deviceId();

        // check if this new rule is an update to an existing entry
        StoredFlowEntry stored = getFlowEntryInternal(rule);
        if (stored != null) {
            stored.setBytes(rule.bytes());
            stored.setLife(rule.life());
            stored.setPackets(rule.packets());
            if (stored.state() == FlowEntryState.PENDING_ADD) {
                stored.setState(FlowEntryState.ADDED);
                return new FlowRuleEvent(Type.RULE_ADDED, rule);
            }
            return new FlowRuleEvent(Type.RULE_UPDATED, rule);
        }

        // TODO: Confirm if this behavior is correct. See SimpleFlowRuleStore
        flowEntries.put(did, new DefaultFlowEntry(rule));
        return null;

        // TODO: also update backup.
    }

    @Override
    public FlowRuleEvent removeFlowRule(FlowEntry rule) {
        ReplicaInfo replicaInfo = replicaInfoManager.getReplicaInfoFor(rule.deviceId());
        if (replicaInfo.master().get().equals(clusterService.getLocalNode().id())) {
            // bypass and handle it locally
            return removeFlowRuleInternal(rule);
        }

        log.error("Tried to remove FlowRule {},"
                + " while the Node was not the master.", rule);
        return null;
    }

    private synchronized FlowRuleEvent removeFlowRuleInternal(FlowEntry rule) {
        // This is where one could mark a rule as removed and still keep it in the store.
        if (flowEntries.remove(rule.deviceId(), rule)) {
            return new FlowRuleEvent(RULE_REMOVED, rule);
        } else {
            return null;
        }
        // TODO: also update backup.
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
}
