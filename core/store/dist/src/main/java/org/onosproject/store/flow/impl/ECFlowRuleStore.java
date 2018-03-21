 /*
 * Copyright 2014-present Open Networking Foundation
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

import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.google.common.collect.Streams;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.KryoNamespace;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.CoreService;
import org.onosproject.core.IdGenerator;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.CompletedBatchOperation;
import org.onosproject.net.flow.DefaultFlowEntry;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowEntry.FlowEntryState;
import org.onosproject.net.flow.FlowId;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.oldbatch.FlowRuleBatchEntry;
import org.onosproject.net.flow.oldbatch.FlowRuleBatchEntry.FlowRuleOperation;
import org.onosproject.net.flow.oldbatch.FlowRuleBatchEvent;
import org.onosproject.net.flow.oldbatch.FlowRuleBatchOperation;
import org.onosproject.net.flow.oldbatch.FlowRuleBatchRequest;
import org.onosproject.net.flow.FlowRuleEvent;
import org.onosproject.net.flow.FlowRuleEvent.Type;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.FlowRuleStore;
import org.onosproject.net.flow.FlowRuleStoreDelegate;
import org.onosproject.net.flow.StoredFlowEntry;
import org.onosproject.net.flow.TableStatisticsEntry;
import org.onosproject.persistence.PersistenceService;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.cluster.messaging.ClusterCommunicationService;
import org.onosproject.store.cluster.messaging.ClusterMessage;
import org.onosproject.store.cluster.messaging.ClusterMessageHandler;
import org.onosproject.store.flow.ReplicaInfoEvent;
import org.onosproject.store.flow.ReplicaInfoEventListener;
import org.onosproject.store.flow.ReplicaInfoService;
import org.onosproject.store.impl.MastershipBasedTimestamp;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.EventuallyConsistentMapEvent;
import org.onosproject.store.service.EventuallyConsistentMapListener;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.WallClockTimestamp;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.onlab.util.Tools.get;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.net.flow.FlowRuleEvent.Type.RULE_REMOVED;
import static org.onosproject.store.flow.ReplicaInfoEvent.Type.MASTER_CHANGED;
import static org.onosproject.store.flow.impl.ECFlowRuleStoreMessageSubjects.APPLY_BATCH_FLOWS;
import static org.onosproject.store.flow.impl.ECFlowRuleStoreMessageSubjects.FLOW_TABLE_BACKUP;
import static org.onosproject.store.flow.impl.ECFlowRuleStoreMessageSubjects.GET_DEVICE_FLOW_ENTRIES;
import static org.onosproject.store.flow.impl.ECFlowRuleStoreMessageSubjects.GET_FLOW_ENTRY;
import static org.onosproject.store.flow.impl.ECFlowRuleStoreMessageSubjects.REMOTE_APPLY_COMPLETED;
import static org.onosproject.store.flow.impl.ECFlowRuleStoreMessageSubjects.REMOVE_FLOW_ENTRY;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Manages inventory of flow rules using a distributed state management protocol.
 */
@Component(immediate = true)
@Service
public class ECFlowRuleStore
        extends AbstractStore<FlowRuleBatchEvent, FlowRuleStoreDelegate>
        implements FlowRuleStore {

    private final Logger log = getLogger(getClass());

    private static final int MESSAGE_HANDLER_THREAD_POOL_SIZE = 8;
    private static final int DEFAULT_MAX_BACKUP_COUNT = 2;
    private static final boolean DEFAULT_PERSISTENCE_ENABLED = false;
    private static final int DEFAULT_BACKUP_PERIOD_MILLIS = 2000;
    private static final long FLOW_RULE_STORE_TIMEOUT_MILLIS = 5000;
    private static final int NUM_BUCKETS = 1024;

    @Property(name = "msgHandlerPoolSize", intValue = MESSAGE_HANDLER_THREAD_POOL_SIZE,
            label = "Number of threads in the message handler pool")
    private int msgHandlerPoolSize = MESSAGE_HANDLER_THREAD_POOL_SIZE;

    @Property(name = "backupPeriod", intValue = DEFAULT_BACKUP_PERIOD_MILLIS,
            label = "Delay in ms between successive backup runs")
    private int backupPeriod = DEFAULT_BACKUP_PERIOD_MILLIS;
    @Property(name = "persistenceEnabled", boolValue = false,
            label = "Indicates whether or not changes in the flow table should be persisted to disk.")
    private boolean persistenceEnabled = DEFAULT_PERSISTENCE_ENABLED;

    @Property(name = "backupCount", intValue = DEFAULT_MAX_BACKUP_COUNT,
            label = "Max number of backup copies for each device")
    private volatile int backupCount = DEFAULT_MAX_BACKUP_COUNT;

    private InternalFlowTable flowTable = new InternalFlowTable();

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

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService configService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PersistenceService persistenceService;

    private Map<Long, NodeId> pendingResponses = Maps.newConcurrentMap();
    private ExecutorService messageHandlingExecutor;
    private ExecutorService eventHandler;

    private ScheduledFuture<?> backupTask;
    private final ScheduledExecutorService backupSenderExecutor =
            Executors.newSingleThreadScheduledExecutor(groupedThreads("onos/flow", "backup-sender", log));

    private EventuallyConsistentMap<DeviceId, List<TableStatisticsEntry>> deviceTableStats;
    private final EventuallyConsistentMapListener<DeviceId, List<TableStatisticsEntry>> tableStatsListener =
            new InternalTableStatsListener();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    protected final Serializer serializer = Serializer.using(KryoNamespace.newBuilder()
        .register(KryoNamespaces.API)
        .register(FlowBucket.class)
        .build());

    protected final KryoNamespace.Builder serializerBuilder = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API)
            .register(MastershipBasedTimestamp.class);

    private EventuallyConsistentMap<DeviceId, Map<Integer, Integer>> flowCounts;

    private IdGenerator idGenerator;
    private NodeId local;

    @Activate
    public void activate(ComponentContext context) {
        configService.registerProperties(getClass());

        idGenerator = coreService.getIdGenerator(FlowRuleService.FLOW_OP_TOPIC);

        local = clusterService.getLocalNode().id();

        eventHandler = Executors.newSingleThreadExecutor(
                groupedThreads("onos/flow", "event-handler", log));
        messageHandlingExecutor = Executors.newFixedThreadPool(
                msgHandlerPoolSize, groupedThreads("onos/store/flow", "message-handlers", log));

        registerMessageHandlers(messageHandlingExecutor);

        replicaInfoManager.addListener(flowTable);
        backupTask = backupSenderExecutor.scheduleWithFixedDelay(
                flowTable::backup,
                0,
                backupPeriod,
                TimeUnit.MILLISECONDS);

        flowCounts = storageService.<DeviceId, Map<Integer, Integer>>eventuallyConsistentMapBuilder()
                .withName("onos-flow-counts")
                .withSerializer(serializerBuilder)
                .withAntiEntropyPeriod(5, TimeUnit.SECONDS)
                .withTimestampProvider((k, v) -> new WallClockTimestamp())
                .withTombstonesDisabled()
                .build();

        deviceTableStats = storageService.<DeviceId, List<TableStatisticsEntry>>eventuallyConsistentMapBuilder()
                .withName("onos-flow-table-stats")
                .withSerializer(serializerBuilder)
                .withAntiEntropyPeriod(5, TimeUnit.SECONDS)
                .withTimestampProvider((k, v) -> new WallClockTimestamp())
                .withTombstonesDisabled()
                .build();
        deviceTableStats.addListener(tableStatsListener);

        logConfig("Started");
    }

    @Deactivate
    public void deactivate(ComponentContext context) {
        replicaInfoManager.removeListener(flowTable);
        backupTask.cancel(true);
        configService.unregisterProperties(getClass(), false);
        unregisterMessageHandlers();
        deviceTableStats.removeListener(tableStatsListener);
        deviceTableStats.destroy();
        eventHandler.shutdownNow();
        messageHandlingExecutor.shutdownNow();
        backupSenderExecutor.shutdownNow();
        log.info("Stopped");
    }

    @SuppressWarnings("rawtypes")
    @Modified
    public void modified(ComponentContext context) {
        if (context == null) {
            logConfig("Default config");
            return;
        }

        Dictionary properties = context.getProperties();
        int newPoolSize;
        int newBackupPeriod;
        int newBackupCount;
        try {
            String s = get(properties, "msgHandlerPoolSize");
            newPoolSize = isNullOrEmpty(s) ? msgHandlerPoolSize : Integer.parseInt(s.trim());

            s = get(properties, "backupPeriod");
            newBackupPeriod = isNullOrEmpty(s) ? backupPeriod : Integer.parseInt(s.trim());

            s = get(properties, "backupCount");
            newBackupCount = isNullOrEmpty(s) ? backupCount : Integer.parseInt(s.trim());
        } catch (NumberFormatException | ClassCastException e) {
            newPoolSize = MESSAGE_HANDLER_THREAD_POOL_SIZE;
            newBackupPeriod = DEFAULT_BACKUP_PERIOD_MILLIS;
            newBackupCount = DEFAULT_MAX_BACKUP_COUNT;
        }

        boolean restartBackupTask = false;

        if (newBackupPeriod != backupPeriod) {
            backupPeriod = newBackupPeriod;
            restartBackupTask = true;
        }
        if (restartBackupTask) {
            if (backupTask != null) {
                // cancel previously running task
                backupTask.cancel(false);
            }
            backupTask = backupSenderExecutor.scheduleWithFixedDelay(
                    flowTable::backup,
                    0,
                    backupPeriod,
                    TimeUnit.MILLISECONDS);
        }
        if (newPoolSize != msgHandlerPoolSize) {
            msgHandlerPoolSize = newPoolSize;
            ExecutorService oldMsgHandler = messageHandlingExecutor;
            messageHandlingExecutor = Executors.newFixedThreadPool(
                    msgHandlerPoolSize, groupedThreads("onos/store/flow", "message-handlers", log));

            // replace previously registered handlers.
            registerMessageHandlers(messageHandlingExecutor);
            oldMsgHandler.shutdown();
        }
        if (backupCount != newBackupCount) {
            backupCount = newBackupCount;
        }
        logConfig("Reconfigured");
    }

    private void registerMessageHandlers(ExecutorService executor) {

        clusterCommunicator.addSubscriber(APPLY_BATCH_FLOWS, new OnStoreBatch(), executor);
        clusterCommunicator.<FlowRuleBatchEvent>addSubscriber(
                REMOTE_APPLY_COMPLETED, serializer::decode, this::notifyDelegate, executor);
        clusterCommunicator.addSubscriber(
                GET_FLOW_ENTRY, serializer::decode, flowTable::getFlowEntry, serializer::encode, executor);
        clusterCommunicator.addSubscriber(
                GET_DEVICE_FLOW_ENTRIES, serializer::decode, flowTable::getFlowEntries, serializer::encode, executor);
        clusterCommunicator.addSubscriber(
                REMOVE_FLOW_ENTRY, serializer::decode, this::removeFlowRuleInternal, serializer::encode, executor);
        clusterCommunicator.addSubscriber(
                FLOW_TABLE_BACKUP, serializer::decode, flowTable::onBackupReceipt, serializer::encode, executor);
    }

    private void unregisterMessageHandlers() {
        clusterCommunicator.removeSubscriber(REMOVE_FLOW_ENTRY);
        clusterCommunicator.removeSubscriber(GET_DEVICE_FLOW_ENTRIES);
        clusterCommunicator.removeSubscriber(GET_FLOW_ENTRY);
        clusterCommunicator.removeSubscriber(APPLY_BATCH_FLOWS);
        clusterCommunicator.removeSubscriber(REMOTE_APPLY_COMPLETED);
        clusterCommunicator.removeSubscriber(FLOW_TABLE_BACKUP);
    }

    private void logConfig(String prefix) {
        log.info("{} with msgHandlerPoolSize = {}; backupPeriod = {}, backupCount = {}",
                 prefix, msgHandlerPoolSize, backupPeriod, backupCount);
    }

    // This is not a efficient operation on a distributed sharded
    // flow store. We need to revisit the need for this operation or at least
    // make it device specific.
    @Override
    public int getFlowRuleCount() {
        return Streams.stream(deviceService.getDevices()).parallel()
                .mapToInt(device -> getFlowRuleCount(device.id()))
                .sum();
    }

    @Override
    public int getFlowRuleCount(DeviceId deviceId) {
        Map<Integer, Integer> counts = flowCounts.get(deviceId);
        return counts != null
            ? counts.values().stream().mapToInt(v -> v).sum()
            : flowTable.flowEntries.get(deviceId) != null
            ? flowTable.flowEntries.get(deviceId).keySet().size() : 0;
    }

    @Override
    public FlowEntry getFlowEntry(FlowRule rule) {
        NodeId master = mastershipService.getMasterFor(rule.deviceId());

        if (master == null) {
            log.debug("Failed to getFlowEntry: No master for {}", rule.deviceId());
            return null;
        }

        if (Objects.equals(local, master)) {
            return flowTable.getFlowEntry(rule);
        }

        log.trace("Forwarding getFlowEntry to {}, which is the primary (master) for device {}",
                  master, rule.deviceId());

        return Tools.futureGetOrElse(clusterCommunicator.sendAndReceive(rule,
                                    ECFlowRuleStoreMessageSubjects.GET_FLOW_ENTRY,
                                    serializer::encode,
                                    serializer::decode,
                                    master),
                               FLOW_RULE_STORE_TIMEOUT_MILLIS,
                               TimeUnit.MILLISECONDS,
                               null);
    }

    @Override
    public Iterable<FlowEntry> getFlowEntries(DeviceId deviceId) {
        NodeId master = mastershipService.getMasterFor(deviceId);

        if (master == null) {
            log.debug("Failed to getFlowEntries: No master for {}", deviceId);
            return Collections.emptyList();
        }

        if (Objects.equals(local, master)) {
            return flowTable.getFlowEntries(deviceId);
        }

        log.trace("Forwarding getFlowEntries to {}, which is the primary (master) for device {}",
                  master, deviceId);

        return Tools.futureGetOrElse(clusterCommunicator.sendAndReceive(deviceId,
                                    ECFlowRuleStoreMessageSubjects.GET_DEVICE_FLOW_ENTRIES,
                                    serializer::encode,
                                    serializer::decode,
                                    master),
                               FLOW_RULE_STORE_TIMEOUT_MILLIS,
                               TimeUnit.MILLISECONDS,
                               Collections.emptyList());
    }

    @Override
    public void storeFlowRule(FlowRule rule) {
        storeBatch(new FlowRuleBatchOperation(
                Collections.singletonList(new FlowRuleBatchEntry(FlowRuleOperation.ADD, rule)),
                rule.deviceId(), idGenerator.getNewId()));
    }

    @Override
    public void storeBatch(FlowRuleBatchOperation operation) {
        if (operation.getOperations().isEmpty()) {
            notifyDelegate(FlowRuleBatchEvent.completed(
                    new FlowRuleBatchRequest(operation.id(), Collections.emptySet()),
                    new CompletedBatchOperation(true, Collections.emptySet(), operation.deviceId())));
            return;
        }

        DeviceId deviceId = operation.deviceId();
        NodeId master = mastershipService.getMasterFor(deviceId);

        if (master == null) {
            log.warn("No master for {} ", deviceId);

            updateStoreInternal(operation);

            notifyDelegate(FlowRuleBatchEvent.completed(
                    new FlowRuleBatchRequest(operation.id(), Collections.emptySet()),
                    new CompletedBatchOperation(true, Collections.emptySet(), operation.deviceId())));
            return;
        }

        if (Objects.equals(local, master)) {
            storeBatchInternal(operation);
            return;
        }

        log.trace("Forwarding storeBatch to {}, which is the primary (master) for device {}",
                  master, deviceId);

        clusterCommunicator.unicast(operation,
                                    APPLY_BATCH_FLOWS,
                                    serializer::encode,
                                    master)
                           .whenComplete((result, error) -> {
                               if (error != null) {
                                   log.warn("Failed to storeBatch: {} to {}", operation, master, error);

                                   Set<FlowRule> allFailures = operation.getOperations()
                                           .stream()
                                           .map(op -> op.target())
                                           .collect(Collectors.toSet());

                                   notifyDelegate(FlowRuleBatchEvent.completed(
                                           new FlowRuleBatchRequest(operation.id(), Collections.emptySet()),
                                           new CompletedBatchOperation(false, allFailures, deviceId)));
                               }
                           });
    }

    private void storeBatchInternal(FlowRuleBatchOperation operation) {

        final DeviceId did = operation.deviceId();
        //final Collection<FlowEntry> ft = flowTable.getFlowEntries(did);
        Set<FlowRuleBatchEntry> currentOps = updateStoreInternal(operation);
        if (currentOps.isEmpty()) {
            batchOperationComplete(FlowRuleBatchEvent.completed(
                    new FlowRuleBatchRequest(operation.id(), Collections.emptySet()),
                    new CompletedBatchOperation(true, Collections.emptySet(), did)));
            return;
        }

        notifyDelegate(FlowRuleBatchEvent.requested(new
                           FlowRuleBatchRequest(operation.id(),
                                                currentOps), operation.deviceId()));
    }

    private Set<FlowRuleBatchEntry> updateStoreInternal(FlowRuleBatchOperation operation) {
        return operation.getOperations().stream().map(
                op -> {
                    StoredFlowEntry entry;
                    switch (op.operator()) {
                        case ADD:
                            entry = new DefaultFlowEntry(op.target());
                            flowTable.add(entry);
                            return op;
                        case MODIFY:
                            entry = new DefaultFlowEntry(op.target());
                            flowTable.update(entry);
                            return op;
                        case REMOVE:
                            entry = flowTable.getFlowEntry(op.target());
                            if (entry != null) {
                                entry.setState(FlowEntryState.PENDING_REMOVE);
                                flowTable.update(entry);
                                log.debug("Setting state of rule to pending remove: {}", entry);
                                return op;
                            }
                            break;
                        default:
                            log.warn("Unknown flow operation operator: {}", op.operator());
                    }
                    return null;
                }
        ).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    @Override
    public void deleteFlowRule(FlowRule rule) {
        storeBatch(
                new FlowRuleBatchOperation(
                        Collections.singletonList(
                                new FlowRuleBatchEntry(
                                        FlowRuleOperation.REMOVE,
                                        rule)), rule.deviceId(), idGenerator.getNewId()));
    }

    @Override
    public FlowRuleEvent pendingFlowRule(FlowEntry rule) {
        if (mastershipService.isLocalMaster(rule.deviceId())) {
            StoredFlowEntry stored = flowTable.getFlowEntry(rule);
            if (stored != null &&
                    stored.state() != FlowEntryState.PENDING_ADD) {
                stored.setState(FlowEntryState.PENDING_ADD);
                return new FlowRuleEvent(Type.RULE_UPDATED, rule);
            }
        }
        return null;
    }

    @Override
    public FlowRuleEvent addOrUpdateFlowRule(FlowEntry rule) {
        NodeId master = mastershipService.getMasterFor(rule.deviceId());
        if (Objects.equals(local, master)) {
            return addOrUpdateFlowRuleInternal(rule);
        }

        log.warn("Tried to update FlowRule {} state,"
                         + " while the Node was not the master.", rule);
        return null;
    }

    private FlowRuleEvent addOrUpdateFlowRuleInternal(FlowEntry rule) {
        // check if this new rule is an update to an existing entry
        StoredFlowEntry stored = flowTable.getFlowEntry(rule);
        if (stored != null) {
            stored.setBytes(rule.bytes());
            stored.setLife(rule.life(TimeUnit.NANOSECONDS), TimeUnit.NANOSECONDS);
            stored.setLiveType(rule.liveType());
            stored.setPackets(rule.packets());
            stored.setLastSeen();
            if (stored.state() == FlowEntryState.PENDING_ADD) {
                stored.setState(FlowEntryState.ADDED);
                // Update the flow table to ensure the changes are replicated
                flowTable.update(stored);
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
        NodeId master = mastershipService.getMasterFor(deviceId);

        if (Objects.equals(local, master)) {
            // bypass and handle it locally
            return removeFlowRuleInternal(rule);
        }

        if (master == null) {
            log.warn("Failed to removeFlowRule: No master for {}", deviceId);
            // TODO: revisit if this should be null (="no-op") or Exception
            return null;
        }

        log.trace("Forwarding removeFlowRule to {}, which is the master for device {}",
                  master, deviceId);

        return Futures.getUnchecked(clusterCommunicator.sendAndReceive(
                               rule,
                               REMOVE_FLOW_ENTRY,
                               serializer::encode,
                               serializer::decode,
                               master));
    }

    private FlowRuleEvent removeFlowRuleInternal(FlowEntry rule) {
        // This is where one could mark a rule as removed and still keep it in the store.
        final FlowEntry removed = flowTable.remove(rule);
        // rule may be partial rule that is missing treatment, we should use rule from store instead
        return removed != null ? new FlowRuleEvent(RULE_REMOVED, removed) : null;
    }

    @Override
    public void purgeFlowRule(DeviceId deviceId) {
        flowTable.purgeFlowRule(deviceId);
    }

    @Override
    public void purgeFlowRules() {
        flowTable.purgeFlowRules();
    }

    @Override
    public void batchOperationComplete(FlowRuleBatchEvent event) {
        //FIXME: need a per device pending response
        NodeId nodeId = pendingResponses.remove(event.subject().batchId());
        if (nodeId == null) {
            notifyDelegate(event);
        } else {
            // TODO check unicast return value
            clusterCommunicator.unicast(event, REMOTE_APPLY_COMPLETED, serializer::encode, nodeId);
            //error log: log.warn("Failed to respond to peer for batch operation result");
        }
    }

    private final class OnStoreBatch implements ClusterMessageHandler {

        @Override
        public void handle(final ClusterMessage message) {
            FlowRuleBatchOperation operation = serializer.decode(message.payload());
            log.debug("received batch request {}", operation);

            final DeviceId deviceId = operation.deviceId();
            NodeId master = mastershipService.getMasterFor(deviceId);
            if (!Objects.equals(local, master)) {
                Set<FlowRule> failures = new HashSet<>(operation.size());
                for (FlowRuleBatchEntry op : operation.getOperations()) {
                    failures.add(op.target());
                }
                CompletedBatchOperation allFailed = new CompletedBatchOperation(false, failures, deviceId);
                // This node is no longer the master, respond as all failed.
                // TODO: we might want to wrap response in envelope
                // to distinguish sw programming failure and hand over
                // it make sense in the latter case to retry immediately.
                message.respond(serializer.encode(allFailed));
                return;
            }

            pendingResponses.put(operation.id(), message.sender());
            storeBatchInternal(operation);
        }
    }

    private class BackupOperation {
        private final NodeId nodeId;
        private final DeviceId deviceId;
        private final int bucket;

        public BackupOperation(NodeId nodeId, DeviceId deviceId, int bucket) {
            this.nodeId = nodeId;
            this.deviceId = deviceId;
            this.bucket = bucket;
        }

        @Override
        public int hashCode() {
            return Objects.hash(nodeId, deviceId, bucket);
        }

        @Override
        public boolean equals(Object other) {
            if (other != null && other instanceof BackupOperation) {
                BackupOperation that = (BackupOperation) other;
                return this.nodeId.equals(that.nodeId) &&
                        this.deviceId.equals(that.deviceId) &&
                        this.bucket == that.bucket;
            } else {
                return false;
            }
        }
    }

    private class FlowBucket {
        private final DeviceId deviceId;
        private final int bucket;
        private final Map<FlowId, Map<StoredFlowEntry, StoredFlowEntry>> table;

        FlowBucket(DeviceId deviceId, int bucket, Map<FlowId, Map<StoredFlowEntry, StoredFlowEntry>> table) {
            this.deviceId = deviceId;
            this.bucket = bucket;
            this.table = table;
        }
    }

    private class InternalFlowTable implements ReplicaInfoEventListener {

        //TODO replace the Map<V,V> with ExtendedSet
        private final Map<DeviceId, Map<FlowId, Map<StoredFlowEntry, StoredFlowEntry>>>
                flowEntries = Maps.newConcurrentMap();

        private final Map<BackupOperation, Long> lastBackupTimes = Maps.newConcurrentMap();
        private final Map<DeviceId, Map<Integer, Long>> lastUpdateTimes = Maps.newConcurrentMap();
        private final Map<NodeId, Set<DeviceId>> inFlightUpdates = Maps.newConcurrentMap();

        @Override
        public void event(ReplicaInfoEvent event) {
            eventHandler.execute(() -> handleEvent(event));
        }

        private void handleEvent(ReplicaInfoEvent event) {
            DeviceId deviceId = event.subject();
            if (!mastershipService.isLocalMaster(deviceId)) {
                return;
            }
            if (event.type() == MASTER_CHANGED) {
                for (int bucket = 0; bucket < NUM_BUCKETS; bucket++) {
                    recordUpdate(deviceId, bucket);
                }
            }
            backupSenderExecutor.execute(this::backup);
        }

        private CompletableFuture<Void> backupFlowEntries(
            NodeId nodeId, DeviceId deviceId, int bucket, long timestamp) {
            log.debug("Sending flowEntries in bucket {} for device {} to {} for backup.", bucket, deviceId, nodeId);
            FlowBucket flowBucket = getFlowBucket(deviceId, bucket);
            int flowCount = flowBucket.table.entrySet().stream()
                .mapToInt(e -> e.getValue().values().size()).sum();
            flowCounts.compute(deviceId, (id, counts) -> {
                if (counts == null) {
                    counts = Maps.newConcurrentMap();
                }
                counts.put(bucket, flowCount);
                return counts;
            });

            CompletableFuture<Void> future = new CompletableFuture<>();
            clusterCommunicator.<FlowBucket, Set<FlowId>>
                    sendAndReceive(flowBucket,
                                   FLOW_TABLE_BACKUP,
                                   serializer::encode,
                                   serializer::decode,
                                   nodeId)
                    .whenComplete((backedupFlows, error) -> {
                        Set<FlowId> flowsNotBackedUp = error != null ?
                            flowBucket.table.keySet() :
                            Sets.difference(flowBucket.table.keySet(), backedupFlows);
                        if (flowsNotBackedUp.size() > 0) {
                            log.warn("Failed to backup flows: {}. Reason: {}, Node: {}",
                                     flowsNotBackedUp, error != null ? error.getMessage() : "none", nodeId);
                        }
                        if (backedupFlows != null) {
                            lastBackupTimes.put(new BackupOperation(nodeId, deviceId, bucket), timestamp);
                        }
                        future.complete(null);
                    });
            return future;
        }

        /**
         * Returns the flow table for specified device.
         *
         * @param deviceId identifier of the device
         * @return Map representing Flow Table of given device.
         */
        private Map<FlowId, Map<StoredFlowEntry, StoredFlowEntry>> getFlowTable(DeviceId deviceId) {
            if (persistenceEnabled) {
                return flowEntries.computeIfAbsent(deviceId, id -> persistenceService
                        .<FlowId, Map<StoredFlowEntry, StoredFlowEntry>>persistentMapBuilder()
                        .withName("FlowTable:" + deviceId.toString())
                        .withSerializer(new Serializer() {
                            @Override
                            public <T> byte[] encode(T object) {
                                return serializer.encode(object);
                            }

                            @Override
                            public <T> T decode(byte[] bytes) {
                                return serializer.decode(bytes);
                            }

                            @Override
                            public <T> T copy(T object) {
                                return serializer.copy(object);
                            }
                        })
                        .build());
            } else {
                return flowEntries.computeIfAbsent(deviceId, id -> Maps.newConcurrentMap());
            }
        }

        private FlowBucket getFlowBucket(DeviceId deviceId, int bucket) {
            if (persistenceEnabled) {
                return new FlowBucket(deviceId, bucket, flowEntries.computeIfAbsent(deviceId, id ->
                    persistenceService.<FlowId, Map<StoredFlowEntry, StoredFlowEntry>>persistentMapBuilder()
                        .withName("FlowTable:" + deviceId.toString())
                        .withSerializer(serializer)
                        .build())
                    .entrySet()
                    .stream()
                    .filter(entry -> isBucket(entry.getKey(), bucket))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
            } else {
                Map<FlowId, Map<StoredFlowEntry, StoredFlowEntry>> copy = Maps.newHashMap();
                flowEntries.computeIfAbsent(deviceId, id -> Maps.newConcurrentMap())
                    .entrySet()
                    .stream()
                    .filter(entry -> isBucket(entry.getKey(), bucket))
                    .forEach(entry -> {
                        copy.put(entry.getKey(), Maps.newHashMap(entry.getValue()));
                    });
                return new FlowBucket(deviceId, bucket, copy);
            }
        }

        private Map<StoredFlowEntry, StoredFlowEntry> getFlowEntriesInternal(DeviceId deviceId, FlowId flowId) {
            return getFlowTable(deviceId).computeIfAbsent(flowId, id -> Maps.newConcurrentMap());
        }

        private StoredFlowEntry getFlowEntryInternal(FlowRule rule) {
            return getFlowEntriesInternal(rule.deviceId(), rule.id()).get(rule);
        }

        private Set<FlowEntry> getFlowEntriesInternal(DeviceId deviceId) {
            return getFlowTable(deviceId).values().stream()
                        .flatMap(m -> m.values().stream())
                        .collect(Collectors.toSet());
        }

        public StoredFlowEntry getFlowEntry(FlowRule rule) {
            return getFlowEntryInternal(rule);
        }

        public Set<FlowEntry> getFlowEntries(DeviceId deviceId) {
            return getFlowEntriesInternal(deviceId);
        }

        private boolean isBucket(FlowId flowId, int bucket) {
            return bucket(flowId) == bucket;
        }

        private int bucket(FlowId flowId) {
            return (int) (flowId.id() % NUM_BUCKETS);
        }

        private void recordUpdate(DeviceId deviceId, int bucket) {
            lastUpdateTimes.computeIfAbsent(deviceId, id -> Maps.newConcurrentMap())
                .put(bucket, System.currentTimeMillis());
        }

        public void add(FlowEntry rule) {
            getFlowEntriesInternal(rule.deviceId(), rule.id())
                    .compute((StoredFlowEntry) rule, (k, stored) -> {
                        return (StoredFlowEntry) rule;
                    });
            recordUpdate(rule.deviceId(), bucket(rule.id()));
        }

        public void update(FlowEntry rule) {
            getFlowEntriesInternal(rule.deviceId(), rule.id())
                .computeIfPresent((StoredFlowEntry) rule, (k, stored) -> {
                    if (rule instanceof DefaultFlowEntry) {
                        DefaultFlowEntry updated = (DefaultFlowEntry) rule;
                        if (stored instanceof DefaultFlowEntry) {
                            DefaultFlowEntry storedEntry = (DefaultFlowEntry) stored;
                            if (updated.created() >= storedEntry.created()) {
                                recordUpdate(rule.deviceId(), bucket(rule.id()));
                                return updated;
                            } else {
                                log.debug("Trying to update more recent flow entry {} (stored: {})", updated, stored);
                                return stored;
                            }
                        }
                    }
                    return stored;
                });
        }

        public FlowEntry remove(FlowEntry rule) {
            final AtomicReference<FlowEntry> removedRule = new AtomicReference<>();
            final Map<FlowId, Map<StoredFlowEntry, StoredFlowEntry>> flowTable = getFlowTable(rule.deviceId());
            flowTable.computeIfPresent(rule.id(), (flowId, flowEntries) -> {
                flowEntries.computeIfPresent((StoredFlowEntry) rule, (k, stored) -> {
                    if (rule instanceof DefaultFlowEntry) {
                        DefaultFlowEntry toRemove = (DefaultFlowEntry) rule;
                        if (stored instanceof DefaultFlowEntry) {
                            DefaultFlowEntry storedEntry = (DefaultFlowEntry) stored;
                            if (toRemove.created() < storedEntry.created()) {
                                log.debug("Trying to remove more recent flow entry {} (stored: {})", toRemove, stored);
                                // the key is not updated, removedRule remains null
                                return stored;
                            }
                        }
                    }
                    removedRule.set(stored);
                    return null;
                });
                return flowEntries.isEmpty() ? null : flowEntries;
            });

            if (removedRule.get() != null) {
                recordUpdate(rule.deviceId(), bucket(rule.id()));
                return removedRule.get();
            } else {
                return null;
            }
        }

        public void purgeFlowRule(DeviceId deviceId) {
            flowEntries.remove(deviceId);
        }

        public void purgeFlowRules() {
            flowEntries.clear();
        }

        private boolean isMasterNode(DeviceId deviceId) {
            NodeId master = replicaInfoManager.getReplicaInfoFor(deviceId).master().orElse(null);
            return Objects.equals(master, clusterService.getLocalNode().id());
        }

        private boolean isBackupNode(NodeId nodeId, DeviceId deviceId) {
            List<NodeId> allPossibleBackupNodes = replicaInfoManager.getReplicaInfoFor(deviceId).backups();
            return allPossibleBackupNodes.indexOf(nodeId) < backupCount;
        }

        private void backup() {
            clusterService.getNodes().stream()
                .filter(node -> !node.id().equals(clusterService.getLocalNode().id()))
                .forEach(node -> {
                    try {
                        backup(node.id());
                    } catch (Exception e) {
                        log.error("Backup failed.", e);
                    }
                });
        }

        private void backup(NodeId nodeId) {
            for (DeviceId deviceId : flowEntries.keySet()) {
                if (isMasterNode(deviceId) && isBackupNode(nodeId, deviceId)) {
                    backup(nodeId, deviceId);
                }
            }
        }

        private void backup(NodeId nodeId, DeviceId deviceId) {
            final long timestamp = System.currentTimeMillis();
            for (int bucket = 0; bucket < NUM_BUCKETS; bucket++) {
                long lastBackupTime = lastBackupTimes.getOrDefault(new BackupOperation(nodeId, deviceId, bucket), 0L);
                long lastUpdateTime = lastUpdateTimes.computeIfAbsent(deviceId, id -> Maps.newConcurrentMap())
                    .getOrDefault(bucket, 0L);
                if (lastBackupTime < lastUpdateTime && startBackup(nodeId, deviceId)) {
                    backupFlowEntries(nodeId, deviceId, bucket, timestamp)
                        .thenRunAsync(() -> {
                            finishBackup(nodeId, deviceId);
                            backup(nodeId);
                        }, backupSenderExecutor);
                    return;
                }
            }
        }

        private boolean startBackup(NodeId nodeId, DeviceId deviceId) {
            return inFlightUpdates.computeIfAbsent(nodeId, id -> Sets.newConcurrentHashSet()).add(deviceId);
        }

        private void finishBackup(NodeId nodeId, DeviceId deviceId) {
            inFlightUpdates.computeIfAbsent(nodeId, id -> Sets.newConcurrentHashSet()).remove(deviceId);
        }

        private Set<FlowId> onBackupReceipt(FlowBucket bucket) {
            log.debug("Received flowEntries for {} bucket {} to backup", bucket.deviceId, bucket.bucket);
            Set<FlowId> backedupFlows = Sets.newHashSet();
            try {
                // Only process those devices are that not managed by the local node.
                NodeId master = replicaInfoManager.getReplicaInfoFor(bucket.deviceId).master().orElse(null);
                if (!Objects.equals(local, master)) {
                    Map<FlowId, Map<StoredFlowEntry, StoredFlowEntry>> backupFlowTable = getFlowTable(bucket.deviceId);
                    backupFlowTable.putAll(bucket.table);
                    backupFlowTable.entrySet()
                        .removeIf(entry -> isBucket(entry.getKey(), bucket.bucket)
                            && !bucket.table.containsKey(entry.getKey()));
                    backedupFlows.addAll(bucket.table.keySet());
                }
            } catch (Exception e) {
                log.warn("Failure processing backup request", e);
            }
            return backedupFlows;
        }
    }

    @Override
    public FlowRuleEvent updateTableStatistics(DeviceId deviceId,
                                               List<TableStatisticsEntry> tableStats) {
        deviceTableStats.put(deviceId, tableStats);
        return null;
    }

    @Override
    public Iterable<TableStatisticsEntry> getTableStatistics(DeviceId deviceId) {
        NodeId master = mastershipService.getMasterFor(deviceId);

        if (master == null) {
            log.debug("Failed to getTableStats: No master for {}", deviceId);
            return Collections.emptyList();
        }

        List<TableStatisticsEntry> tableStats = deviceTableStats.get(deviceId);
        if (tableStats == null) {
            return Collections.emptyList();
        }
        return ImmutableList.copyOf(tableStats);
    }

    @Override
    public long getActiveFlowRuleCount(DeviceId deviceId) {
        return Streams.stream(getTableStatistics(deviceId))
                .mapToLong(TableStatisticsEntry::activeFlowEntries)
                .sum();
    }

    private class InternalTableStatsListener
        implements EventuallyConsistentMapListener<DeviceId, List<TableStatisticsEntry>> {
        @Override
        public void event(EventuallyConsistentMapEvent<DeviceId,
                          List<TableStatisticsEntry>> event) {
            //TODO: Generate an event to listeners (do we need?)
        }
    }
}
