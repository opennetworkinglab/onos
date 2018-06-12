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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.Streams;
import com.google.common.util.concurrent.Futures;
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
import org.onosproject.net.flow.FlowRuleEvent;
import org.onosproject.net.flow.FlowRuleEvent.Type;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.FlowRuleStore;
import org.onosproject.net.flow.FlowRuleStoreDelegate;
import org.onosproject.net.flow.StoredFlowEntry;
import org.onosproject.net.flow.TableStatisticsEntry;
import org.onosproject.net.flow.oldbatch.FlowRuleBatchEntry;
import org.onosproject.net.flow.oldbatch.FlowRuleBatchEntry.FlowRuleOperation;
import org.onosproject.net.flow.oldbatch.FlowRuleBatchEvent;
import org.onosproject.net.flow.oldbatch.FlowRuleBatchOperation;
import org.onosproject.net.flow.oldbatch.FlowRuleBatchRequest;
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
        .register(BucketId.class)
        .register(FlowBucket.class)
        .build());

    protected final KryoNamespace.Builder serializerBuilder = KryoNamespace.newBuilder()
        .register(KryoNamespaces.API)
        .register(BucketId.class)
        .register(MastershipBasedTimestamp.class);

    private EventuallyConsistentMap<BucketId, Integer> flowCounts;

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

        flowCounts = storageService.<BucketId, Integer>eventuallyConsistentMapBuilder()
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
                FLOW_TABLE_BACKUP, serializer::decode, flowTable::onBackup, serializer::encode, executor);
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

    @Override
    public int getFlowRuleCount() {
        return Streams.stream(deviceService.getDevices()).parallel()
                .mapToInt(device -> getFlowRuleCount(device.id()))
                .sum();
    }

    @Override
    public int getFlowRuleCount(DeviceId deviceId) {
        return flowCounts.entrySet().stream()
            .filter(entry -> entry.getKey().deviceId().equals(deviceId))
            .mapToInt(entry -> entry.getValue())
            .sum();
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

    /**
     * Represents a backup to a of a distinct bucket to a distinct node.
     */
    private class BackupOperation {
        private final NodeId nodeId;
        private final BucketId bucketId;

        BackupOperation(NodeId nodeId, BucketId bucketId) {
            this.nodeId = nodeId;
            this.bucketId = bucketId;
        }

        NodeId nodeId() {
            return nodeId;
        }

        BucketId bucketId() {
            return bucketId;
        }

        @Override
        public int hashCode() {
            return Objects.hash(nodeId, bucketId);
        }

        @Override
        public boolean equals(Object other) {
            if (other != null && other instanceof BackupOperation) {
                BackupOperation that = (BackupOperation) other;
                return this.nodeId.equals(that.nodeId)
                    && this.bucketId.equals(that.bucketId);
            }
            return false;
        }
    }

    /**
     * Represents a distinct device flow bucket.
     */
    private class BucketId {
        private final DeviceId deviceId;
        private final int bucket;

        BucketId(DeviceId deviceId, int bucket) {
            this.deviceId = deviceId;
            this.bucket = bucket;
        }

        DeviceId deviceId() {
            return deviceId;
        }

        int bucket() {
            return bucket;
        }

        @Override
        public int hashCode() {
            return Objects.hash(deviceId, bucket);
        }

        @Override
        public boolean equals(Object other) {
            if (other != null && other instanceof BucketId) {
                BucketId that = (BucketId) other;
                return this.deviceId.equals(that.deviceId)
                    && this.bucket == that.bucket;
            }
            return false;
        }
    }

    /**
     * Container for flows in a specific bucket.
     */
    private class FlowBucket {
        private final BucketId bucketId;
        private final Map<FlowId, Map<StoredFlowEntry, StoredFlowEntry>> table;

        BucketId bucketId() {
            return bucketId;
        }

        Map<FlowId, Map<StoredFlowEntry, StoredFlowEntry>> table() {
            return table;
        }

        FlowBucket(BucketId bucketId, Map<FlowId, Map<StoredFlowEntry, StoredFlowEntry>> table) {
            this.bucketId = bucketId;
            this.table = table;
        }
    }

    private class InternalFlowTable implements ReplicaInfoEventListener {

        //TODO replace the Map<V,V> with ExtendedSet
        private final Map<DeviceId, Map<FlowId, Map<StoredFlowEntry, StoredFlowEntry>>>
                flowEntries = Maps.newConcurrentMap();

        private final Map<BackupOperation, Long> lastBackupTimes = Maps.newConcurrentMap();
        private final Map<BucketId, Long> lastUpdateTimes = Maps.newConcurrentMap();
        private final Set<BackupOperation> inFlightUpdates = Sets.newConcurrentHashSet();

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
                    recordUpdate(new BucketId(deviceId, bucket));
                }
            }
            backupSenderExecutor.execute(this::backup);
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
                        .withSerializer(serializer)
                        .build());
            } else {
                return flowEntries.computeIfAbsent(deviceId, id -> Maps.newConcurrentMap());
            }
        }

        private FlowBucket getFlowBucket(BucketId bucketId) {
            if (persistenceEnabled) {
                return new FlowBucket(bucketId, flowEntries.computeIfAbsent(bucketId.deviceId(), id ->
                    persistenceService.<FlowId, Map<StoredFlowEntry, StoredFlowEntry>>persistentMapBuilder()
                        .withName("FlowTable:" + bucketId.deviceId().toString())
                        .withSerializer(serializer)
                        .build())
                    .entrySet()
                    .stream()
                    .filter(entry -> isInBucket(entry.getKey(), bucketId.bucket()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
            } else {
                Map<FlowId, Map<StoredFlowEntry, StoredFlowEntry>> copy = Maps.newHashMap();
                flowEntries.computeIfAbsent(bucketId.deviceId(), id -> Maps.newConcurrentMap())
                    .entrySet()
                    .stream()
                    .filter(entry -> isInBucket(entry.getKey(), bucketId.bucket()))
                    .forEach(entry -> {
                        copy.put(entry.getKey(), Maps.newHashMap(entry.getValue()));
                    });
                return new FlowBucket(bucketId, copy);
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

        private boolean isInBucket(FlowId flowId, int bucket) {
            return bucket(flowId) == bucket;
        }

        private int bucket(FlowId flowId) {
            return (int) (flowId.id() % NUM_BUCKETS);
        }

        private void recordUpdate(BucketId bucketId) {
            lastUpdateTimes.put(bucketId, System.currentTimeMillis());
        }

        public void add(FlowEntry rule) {
            getFlowEntriesInternal(rule.deviceId(), rule.id())
                    .compute((StoredFlowEntry) rule, (k, stored) -> {
                        return (StoredFlowEntry) rule;
                    });
            recordUpdate(new BucketId(rule.deviceId(), bucket(rule.id())));
        }

        public void update(FlowEntry rule) {
            getFlowEntriesInternal(rule.deviceId(), rule.id())
                .computeIfPresent((StoredFlowEntry) rule, (k, stored) -> {
                    if (rule instanceof DefaultFlowEntry) {
                        DefaultFlowEntry updated = (DefaultFlowEntry) rule;
                        if (stored instanceof DefaultFlowEntry) {
                            DefaultFlowEntry storedEntry = (DefaultFlowEntry) stored;
                            if (updated.created() >= storedEntry.created()) {
                                recordUpdate(new BucketId(rule.deviceId(), bucket(rule.id())));
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
                recordUpdate(new BucketId(rule.deviceId(), bucket(rule.id())));
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

        /**
         * Returns a boolean indicating whether the local node is the current master for the given device.
         *
         * @param deviceId the device for which to indicate whether the local node is the current master
         * @return indicates whether the local node is the current master for the given device
         */
        private boolean isMasterNode(DeviceId deviceId) {
            NodeId master = replicaInfoManager.getReplicaInfoFor(deviceId).master().orElse(null);
            return Objects.equals(master, clusterService.getLocalNode().id());
        }

        /**
         * Backs up all devices to all backup nodes.
         */
        private void backup() {
            for (DeviceId deviceId : flowEntries.keySet()) {
                backup(deviceId);
            }
        }

        /**
         * Backs up all buckets in the given device to the given node.
         *
         * @param deviceId the device to back up
         */
        private void backup(DeviceId deviceId) {
            if (!isMasterNode(deviceId)) {
                return;
            }

            // Get a list of backup nodes for the device.
            List<NodeId> backupNodes = replicaInfoManager.getReplicaInfoFor(deviceId).backups();
            int availableBackupCount = Math.min(backupCount, backupNodes.size());

            // If the list of backup nodes is empty, update the flow count.
            if (availableBackupCount == 0) {
                updateDeviceFlowCounts(deviceId);
            } else {
                // Otherwise, iterate through backup nodes and backup the device.
                for (int index = 0; index < availableBackupCount; index++) {
                    NodeId backupNode = backupNodes.get(index);
                    try {
                        backup(deviceId, backupNode);
                    } catch (Exception e) {
                        log.error("Backup of " + deviceId + " to " + backupNode + " failed", e);
                    }
                }
            }
        }

        /**
         * Backs up all buckets for the given device to the given node.
         *
         * @param deviceId the device to back up
         * @param nodeId the node to which to back up the device
         */
        private void backup(DeviceId deviceId, NodeId nodeId) {
            final long timestamp = System.currentTimeMillis();
            for (int bucket = 0; bucket < NUM_BUCKETS; bucket++) {
                BucketId bucketId = new BucketId(deviceId, bucket);
                BackupOperation operation = new BackupOperation(nodeId, bucketId);
                if (startBackup(operation)) {
                    backup(operation).whenCompleteAsync((succeeded, error) -> {
                        if (error == null && succeeded) {
                            succeedBackup(operation, timestamp);
                        } else {
                            failBackup(operation);
                        }
                        backup(deviceId, nodeId);
                    }, backupSenderExecutor);
                }
            }
        }

        /**
         * Returns a boolean indicating whether the given {@link BackupOperation} can be started.
         * <p>
         * The backup can be started if no backup for the same device/bucket/node is already in progress and changes
         * are pending replication for the backup operation.
         *
         * @param operation the operation to start
         * @return indicates whether the given backup operation should be started
         */
        private boolean startBackup(BackupOperation operation) {
            long lastBackupTime = lastBackupTimes.getOrDefault(operation, 0L);
            long lastUpdateTime = lastUpdateTimes.getOrDefault(operation.bucketId(), 0L);
            return lastUpdateTime > 0 && lastBackupTime <= lastUpdateTime && inFlightUpdates.add(operation);
        }

        /**
         * Fails the given backup operation.
         *
         * @param operation the backup operation to fail
         */
        private void failBackup(BackupOperation operation) {
            inFlightUpdates.remove(operation);
        }

        /**
         * Succeeds the given backup operation.
         * <p>
         * The last backup time for the operation will be updated and the operation will be removed from
         * in-flight updates.
         *
         * @param operation the operation to succeed
         * @param timestamp the timestamp at which the operation was <em>started</em>
         */
        private void succeedBackup(BackupOperation operation, long timestamp) {
            lastBackupTimes.put(operation, timestamp);
            inFlightUpdates.remove(operation);
        }

        /**
         * Performs the given backup operation.
         *
         * @param operation the operation to perform
         * @return a future to be completed with a boolean indicating whether the backup operation was successful
         */
        private CompletableFuture<Boolean> backup(BackupOperation operation) {
            log.debug("Sending flowEntries in bucket {} for device {} to {} for backup.",
                operation.bucketId().bucket(), operation.bucketId().deviceId(), operation.nodeId());
            FlowBucket flowBucket = getFlowBucket(operation.bucketId());

            CompletableFuture<Boolean> future = new CompletableFuture<>();
            clusterCommunicator.<FlowBucket, Set<FlowId>>
                sendAndReceive(flowBucket,
                FLOW_TABLE_BACKUP,
                serializer::encode,
                serializer::decode,
                operation.nodeId())
                .whenComplete((backedupFlows, error) -> {
                    Set<FlowId> flowsNotBackedUp = error != null ?
                        flowBucket.table().keySet() :
                        Sets.difference(flowBucket.table().keySet(), backedupFlows);
                    if (flowsNotBackedUp.size() > 0) {
                        log.warn("Failed to backup flows: {}. Reason: {}, Node: {}",
                            flowsNotBackedUp, error != null ? error.getMessage() : "none", operation.nodeId());
                    }
                    future.complete(backedupFlows != null);
                });

            updateFlowCounts(flowBucket);
            return future;
        }

        /**
         * Handles a flow bucket backup from a remote peer.
         *
         * @param flowBucket the flow bucket to back up
         * @return the set of flows that could not be backed up
         */
        private Set<FlowId> onBackup(FlowBucket flowBucket) {
            log.debug("Received flowEntries for {} bucket {} to backup",
                flowBucket.bucketId().deviceId(), flowBucket.bucketId);
            Set<FlowId> backedupFlows = Sets.newHashSet();
            try {
                // Only process those devices are that not managed by the local node.
                NodeId master = replicaInfoManager.getReplicaInfoFor(flowBucket.bucketId().deviceId())
                    .master()
                    .orElse(null);
                if (!Objects.equals(local, master)) {
                    Map<FlowId, Map<StoredFlowEntry, StoredFlowEntry>> backupFlowTable =
                        getFlowTable(flowBucket.bucketId().deviceId());
                    backupFlowTable.putAll(flowBucket.table());
                    backupFlowTable.entrySet()
                        .removeIf(entry -> isInBucket(entry.getKey(), flowBucket.bucketId().bucket())
                            && !flowBucket.table().containsKey(entry.getKey()));
                    backedupFlows.addAll(flowBucket.table().keySet());
                }
            } catch (Exception e) {
                log.warn("Failure processing backup request", e);
            }
            return backedupFlows;
        }

        /**
         * Updates all flow counts for the given device.
         *
         * @param deviceId the device for which to update flow counts
         */
        private void updateDeviceFlowCounts(DeviceId deviceId) {
            for (int bucket = 0; bucket < NUM_BUCKETS; bucket++) {
                BucketId bucketId = new BucketId(deviceId, bucket);
                FlowBucket flowBucket = getFlowBucket(bucketId);
                updateFlowCounts(flowBucket);
            }
        }

        /**
         * Updates the eventually consistent flow count for the given bucket.
         *
         * @param flowBucket the flow bucket for which to update flow counts
         */
        private void updateFlowCounts(FlowBucket flowBucket) {
            int flowCount = flowBucket.table().entrySet()
                .stream()
                .mapToInt(e -> e.getValue().values().size())
                .sum();
            flowCounts.put(flowBucket.bucketId(), flowCount);
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
