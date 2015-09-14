 /*
 * Copyright 2014-2015 Open Networking Laboratory
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

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
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
import org.onosproject.net.flow.TableStatisticsEntry;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.cluster.messaging.ClusterCommunicationService;
import org.onosproject.store.cluster.messaging.ClusterMessage;
import org.onosproject.store.cluster.messaging.ClusterMessageHandler;
import org.onosproject.store.flow.ReplicaInfoEvent;
import org.onosproject.store.flow.ReplicaInfoEventListener;
import org.onosproject.store.flow.ReplicaInfoService;
import org.onosproject.store.impl.MastershipBasedTimestamp;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.serializers.KryoSerializer;
import org.onosproject.store.serializers.StoreSerializer;
import org.onosproject.store.serializers.custom.DistributedStoreSerializers;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.EventuallyConsistentMapEvent;
import org.onosproject.store.service.EventuallyConsistentMapListener;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.WallClockTimestamp;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.onlab.util.Tools.get;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.net.flow.FlowRuleEvent.Type.RULE_REMOVED;
import static org.onosproject.store.flow.impl.FlowStoreMessageSubjects.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Manages inventory of flow rules using a distributed state management protocol.
 */
@Component(immediate = true, enabled = true)
@Service
public class NewDistributedFlowRuleStore
        extends AbstractStore<FlowRuleBatchEvent, FlowRuleStoreDelegate>
        implements FlowRuleStore {

    private final Logger log = getLogger(getClass());

    private static final int MESSAGE_HANDLER_THREAD_POOL_SIZE = 8;
    private static final boolean DEFAULT_BACKUP_ENABLED = true;
    private static final int DEFAULT_BACKUP_PERIOD_MILLIS = 2000;
    private static final long FLOW_RULE_STORE_TIMEOUT_MILLIS = 5000;
    // number of devices whose flow entries will be backed up in one communication round
    private static final int FLOW_TABLE_BACKUP_BATCH_SIZE = 1;

    @Property(name = "msgHandlerPoolSize", intValue = MESSAGE_HANDLER_THREAD_POOL_SIZE,
            label = "Number of threads in the message handler pool")
    private int msgHandlerPoolSize = MESSAGE_HANDLER_THREAD_POOL_SIZE;

    @Property(name = "backupEnabled", boolValue = DEFAULT_BACKUP_ENABLED,
            label = "Indicates whether backups are enabled or not")
    private boolean backupEnabled = DEFAULT_BACKUP_ENABLED;

    @Property(name = "backupPeriod", intValue = DEFAULT_BACKUP_PERIOD_MILLIS,
            label = "Delay in ms between successive backup runs")
    private int backupPeriod = DEFAULT_BACKUP_PERIOD_MILLIS;

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

    private Map<Long, NodeId> pendingResponses = Maps.newConcurrentMap();
    private ExecutorService messageHandlingExecutor;

    private ScheduledFuture<?> backupTask;
    private final ScheduledExecutorService backupSenderExecutor =
            Executors.newSingleThreadScheduledExecutor(groupedThreads("onos/flow", "backup-sender"));

    private EventuallyConsistentMap<DeviceId, List<TableStatisticsEntry>> deviceTableStats;
    private final EventuallyConsistentMapListener<DeviceId, List<TableStatisticsEntry>> tableStatsListener =
            new InternalTableStatsListener();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    protected static final StoreSerializer SERIALIZER = new KryoSerializer() {
        @Override
        protected void setupKryoPool() {
            serializerPool = KryoNamespace.newBuilder()
                    .register(DistributedStoreSerializers.STORE_COMMON)
                    .nextId(DistributedStoreSerializers.STORE_CUSTOM_BEGIN)
                    .build();
        }
    };

    protected static final KryoNamespace.Builder SERIALIZER_BUILDER = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API)
            .register(MastershipBasedTimestamp.class);


    private IdGenerator idGenerator;
    private NodeId local;

    @Activate
    public void activate(ComponentContext context) {
        configService.registerProperties(getClass());

        idGenerator = coreService.getIdGenerator(FlowRuleService.FLOW_OP_TOPIC);

        local = clusterService.getLocalNode().id();

        messageHandlingExecutor = Executors.newFixedThreadPool(
                msgHandlerPoolSize, groupedThreads("onos/store/flow", "message-handlers"));

        registerMessageHandlers(messageHandlingExecutor);

        if (backupEnabled) {
            replicaInfoManager.addListener(flowTable);
            backupTask = backupSenderExecutor.scheduleWithFixedDelay(
                    flowTable::backup,
                    0,
                    backupPeriod,
                    TimeUnit.MILLISECONDS);
        }

        deviceTableStats = storageService.<DeviceId, List<TableStatisticsEntry>>eventuallyConsistentMapBuilder()
                .withName("onos-flow-table-stats")
                .withSerializer(SERIALIZER_BUILDER)
                .withAntiEntropyPeriod(5, TimeUnit.SECONDS)
                .withTimestampProvider((k, v) -> new WallClockTimestamp())
                .withTombstonesDisabled()
                .build();
        deviceTableStats.addListener(tableStatsListener);

        logConfig("Started");
    }

    @Deactivate
    public void deactivate(ComponentContext context) {
        if (backupEnabled) {
            replicaInfoManager.removeListener(flowTable);
            backupTask.cancel(true);
        }
        configService.unregisterProperties(getClass(), false);
        unregisterMessageHandlers();
        deviceTableStats.removeListener(tableStatsListener);
        deviceTableStats.destroy();
        messageHandlingExecutor.shutdownNow();
        backupSenderExecutor.shutdownNow();
        log.info("Stopped");
    }

    @SuppressWarnings("rawtypes")
    @Modified
    public void modified(ComponentContext context) {
        if (context == null) {
            backupEnabled = DEFAULT_BACKUP_ENABLED;
            logConfig("Default config");
            return;
        }

        Dictionary properties = context.getProperties();
        int newPoolSize;
        boolean newBackupEnabled;
        int newBackupPeriod;
        try {
            String s = get(properties, "msgHandlerPoolSize");
            newPoolSize = isNullOrEmpty(s) ? msgHandlerPoolSize : Integer.parseInt(s.trim());

            s = get(properties, "backupEnabled");
            newBackupEnabled = isNullOrEmpty(s) ? backupEnabled : Boolean.parseBoolean(s.trim());

            s = get(properties, "backupPeriod");
            newBackupPeriod = isNullOrEmpty(s) ? backupPeriod : Integer.parseInt(s.trim());

        } catch (NumberFormatException | ClassCastException e) {
            newPoolSize = MESSAGE_HANDLER_THREAD_POOL_SIZE;
            newBackupEnabled = DEFAULT_BACKUP_ENABLED;
            newBackupPeriod = DEFAULT_BACKUP_PERIOD_MILLIS;
        }

        boolean restartBackupTask = false;
        if (newBackupEnabled != backupEnabled) {
            backupEnabled = newBackupEnabled;
            if (!backupEnabled) {
                replicaInfoManager.removeListener(flowTable);
                if (backupTask != null) {
                    backupTask.cancel(false);
                    backupTask = null;
                }
            } else {
                replicaInfoManager.addListener(flowTable);
            }
            restartBackupTask = backupEnabled;
        }
        if (newBackupPeriod != backupPeriod) {
            backupPeriod = newBackupPeriod;
            restartBackupTask = backupEnabled;
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
                    msgHandlerPoolSize, groupedThreads("onos/store/flow", "message-handlers"));

            // replace previously registered handlers.
            registerMessageHandlers(messageHandlingExecutor);
            oldMsgHandler.shutdown();
        }
        logConfig("Reconfigured");
    }

    private void registerMessageHandlers(ExecutorService executor) {

        clusterCommunicator.addSubscriber(APPLY_BATCH_FLOWS, new OnStoreBatch(), executor);
        clusterCommunicator.<FlowRuleBatchEvent>addSubscriber(
                REMOTE_APPLY_COMPLETED, SERIALIZER::decode, this::notifyDelegate, executor);
        clusterCommunicator.addSubscriber(
                GET_FLOW_ENTRY, SERIALIZER::decode, flowTable::getFlowEntry, SERIALIZER::encode, executor);
        clusterCommunicator.addSubscriber(
                GET_DEVICE_FLOW_ENTRIES, SERIALIZER::decode, flowTable::getFlowEntries, SERIALIZER::encode, executor);
        clusterCommunicator.addSubscriber(
                REMOVE_FLOW_ENTRY, SERIALIZER::decode, this::removeFlowRuleInternal, SERIALIZER::encode, executor);
        clusterCommunicator.addSubscriber(
                REMOVE_FLOW_ENTRY, SERIALIZER::decode, this::removeFlowRuleInternal, SERIALIZER::encode, executor);
        clusterCommunicator.addSubscriber(
                FLOW_TABLE_BACKUP, SERIALIZER::decode, flowTable::onBackupReceipt, SERIALIZER::encode, executor);
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
        log.info("{} with msgHandlerPoolSize = {}; backupEnabled = {}, backupPeriod = {}",
                 prefix, msgHandlerPoolSize, backupEnabled, backupPeriod);
    }

    // This is not a efficient operation on a distributed sharded
    // flow store. We need to revisit the need for this operation or at least
    // make it device specific.
    @Override
    public int getFlowRuleCount() {
        AtomicInteger sum = new AtomicInteger(0);
        deviceService.getDevices().forEach(device -> sum.addAndGet(Iterables.size(getFlowEntries(device.id()))));
        return sum.get();
    }

    @Override
    public FlowEntry getFlowEntry(FlowRule rule) {
        NodeId master = mastershipService.getMasterFor(rule.deviceId());

        if (master == null) {
            log.debug("Failed to getFlowEntry: No master for {}", rule.deviceId());
            return null;
        }

        if (Objects.equal(local, master)) {
            return flowTable.getFlowEntry(rule);
        }

        log.trace("Forwarding getFlowEntry to {}, which is the primary (master) for device {}",
                  master, rule.deviceId());

        return Tools.futureGetOrElse(clusterCommunicator.sendAndReceive(rule,
                                    FlowStoreMessageSubjects.GET_FLOW_ENTRY,
                                    SERIALIZER::encode,
                                    SERIALIZER::decode,
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

        if (Objects.equal(local, master)) {
            return flowTable.getFlowEntries(deviceId);
        }

        log.trace("Forwarding getFlowEntries to {}, which is the primary (master) for device {}",
                  master, deviceId);

        return Tools.futureGetOrElse(clusterCommunicator.sendAndReceive(deviceId,
                                    FlowStoreMessageSubjects.GET_DEVICE_FLOW_ENTRIES,
                                    SERIALIZER::encode,
                                    SERIALIZER::decode,
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
            log.warn("No master for {} : flows will be marked for removal", deviceId);

            updateStoreInternal(operation);

            notifyDelegate(FlowRuleBatchEvent.completed(
                    new FlowRuleBatchRequest(operation.id(), Collections.emptySet()),
                    new CompletedBatchOperation(true, Collections.emptySet(), operation.deviceId())));
            return;
        }

        if (Objects.equal(local, master)) {
            storeBatchInternal(operation);
            return;
        }

        log.trace("Forwarding storeBatch to {}, which is the primary (master) for device {}",
                  master, deviceId);

        clusterCommunicator.unicast(operation,
                                    APPLY_BATCH_FLOWS,
                                    SERIALIZER::encode,
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
                            log.warn("Unknown flow operation operator: {}", op.operator());
                    }
                    return null;
                }
        ).filter(op -> op != null).collect(Collectors.toSet());
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
    public FlowRuleEvent addOrUpdateFlowRule(FlowEntry rule) {
        NodeId master = mastershipService.getMasterFor(rule.deviceId());
        if (Objects.equal(local, master)) {
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
            stored.setLife(rule.life());
            stored.setPackets(rule.packets());
            if (stored.state() == FlowEntryState.PENDING_ADD) {
                stored.setState(FlowEntryState.ADDED);
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

        if (Objects.equal(local, master)) {
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

        return Futures.get(clusterCommunicator.sendAndReceive(
                               rule,
                               REMOVE_FLOW_ENTRY,
                               SERIALIZER::encode,
                               SERIALIZER::decode,
                               master),
                           FLOW_RULE_STORE_TIMEOUT_MILLIS,
                           TimeUnit.MILLISECONDS,
                           RuntimeException.class);
    }

    private FlowRuleEvent removeFlowRuleInternal(FlowEntry rule) {
        final DeviceId deviceId = rule.deviceId();
        // This is where one could mark a rule as removed and still keep it in the store.
        final boolean removed = flowTable.remove(deviceId, rule); //flowEntries.remove(deviceId, rule);
        return removed ? new FlowRuleEvent(RULE_REMOVED, rule) : null;
    }

    @Override
    public void batchOperationComplete(FlowRuleBatchEvent event) {
        //FIXME: need a per device pending response
        NodeId nodeId = pendingResponses.remove(event.subject().batchId());
        if (nodeId == null) {
            notifyDelegate(event);
        } else {
            // TODO check unicast return value
            clusterCommunicator.unicast(event, REMOTE_APPLY_COMPLETED, SERIALIZER::encode, nodeId);
            //error log: log.warn("Failed to respond to peer for batch operation result");
        }
    }

    private final class OnStoreBatch implements ClusterMessageHandler {

        @Override
        public void handle(final ClusterMessage message) {
            FlowRuleBatchOperation operation = SERIALIZER.decode(message.payload());
            log.debug("received batch request {}", operation);

            final DeviceId deviceId = operation.deviceId();
            NodeId master = mastershipService.getMasterFor(deviceId);
            if (!Objects.equal(local, master)) {
                Set<FlowRule> failures = new HashSet<>(operation.size());
                for (FlowRuleBatchEntry op : operation.getOperations()) {
                    failures.add(op.target());
                }
                CompletedBatchOperation allFailed = new CompletedBatchOperation(false, failures, deviceId);
                // This node is no longer the master, respond as all failed.
                // TODO: we might want to wrap response in envelope
                // to distinguish sw programming failure and hand over
                // it make sense in the latter case to retry immediately.
                message.respond(SERIALIZER.encode(allFailed));
                return;
            }

            pendingResponses.put(operation.id(), message.sender());
            storeBatchInternal(operation);
        }
    }

    private class InternalFlowTable implements ReplicaInfoEventListener {

        private final Map<DeviceId, Map<FlowId, Set<StoredFlowEntry>>>
                flowEntries = Maps.newConcurrentMap();

        private final Map<DeviceId, Long> lastBackupTimes = Maps.newConcurrentMap();
        private final Map<DeviceId, Long> lastUpdateTimes = Maps.newConcurrentMap();
        private final Map<DeviceId, NodeId> lastBackupNodes = Maps.newConcurrentMap();

        @Override
        public void event(ReplicaInfoEvent event) {
            if (!backupEnabled) {
                return;
            }
            if (event.type() == ReplicaInfoEvent.Type.BACKUPS_CHANGED) {
                DeviceId deviceId = event.subject();
                NodeId master = mastershipService.getMasterFor(deviceId);
                if (!Objects.equal(local, master)) {
                 // ignore since this event is for a device this node does not manage.
                    return;
                }
                NodeId newBackupNode = getBackupNode(deviceId);
                NodeId currentBackupNode = lastBackupNodes.get(deviceId);
                if (Objects.equal(newBackupNode, currentBackupNode)) {
                    // ignore since backup location hasn't changed.
                    return;
                }
                if (currentBackupNode != null && newBackupNode == null) {
                    // Current backup node is most likely down and no alternate backup node
                    // has been chosen. Clear current backup location so that we can resume
                    // backups when either current backup comes online or a different backup node
                    // is chosen.
                    log.warn("Lost backup location {} for deviceId {} and no alternate backup node exists. "
                            + "Flows can be lost if the master goes down", currentBackupNode, deviceId);
                    lastBackupNodes.remove(deviceId);
                    lastBackupTimes.remove(deviceId);
                    return;
                    // TODO: Pick any available node as backup and ensure hand-off occurs when
                    // a new master is elected.
                }
                log.debug("Backup location for {} has changed from {} to {}.",
                        deviceId, currentBackupNode, newBackupNode);
                backupSenderExecutor.schedule(() -> backupFlowEntries(newBackupNode, Sets.newHashSet(deviceId)),
                        0,
                        TimeUnit.SECONDS);
            }
        }

        private void sendBackups(NodeId nodeId, Set<DeviceId> deviceIds) {
            // split up the devices into smaller batches and send them separately.
            Iterables.partition(deviceIds, FLOW_TABLE_BACKUP_BATCH_SIZE)
                     .forEach(ids -> backupFlowEntries(nodeId, Sets.newHashSet(ids)));
        }

        private void backupFlowEntries(NodeId nodeId, Set<DeviceId> deviceIds) {
            if (deviceIds.isEmpty()) {
                return;
            }
            log.debug("Sending flowEntries for devices {} to {} as backup.", deviceIds, nodeId);
            Map<DeviceId, Map<FlowId, Set<StoredFlowEntry>>> deviceFlowEntries =
                    Maps.newConcurrentMap();
            deviceIds.forEach(id -> deviceFlowEntries.put(id, ImmutableMap.copyOf(getFlowTable(id))));
            clusterCommunicator.<Map<DeviceId, Map<FlowId, Set<StoredFlowEntry>>>, Set<DeviceId>>sendAndReceive(
                                        deviceFlowEntries,
                                        FLOW_TABLE_BACKUP,
                                        SERIALIZER::encode,
                                        SERIALIZER::decode,
                                        nodeId)
                               .whenComplete((backedupDevices, error) -> {
                                   Set<DeviceId> devicesNotBackedup = error != null ?
                                           deviceFlowEntries.keySet() :
                                           Sets.difference(deviceFlowEntries.keySet(), backedupDevices);
                                   if (devicesNotBackedup.size() > 0) {
                                       log.warn("Failed to backup devices: {}. Reason: {}",
                                               devicesNotBackedup, error.getMessage());
                                   }
                                   if (backedupDevices != null) {
                                       backedupDevices.forEach(id -> {
                                           lastBackupTimes.put(id, System.currentTimeMillis());
                                           lastBackupNodes.put(id, nodeId);
                                       });
                                   }
                               });
        }

        /**
         * Returns the flow table for specified device.
         *
         * @param deviceId identifier of the device
         * @return Map representing Flow Table of given device.
         */
        private Map<FlowId, Set<StoredFlowEntry>> getFlowTable(DeviceId deviceId) {
            return flowEntries.computeIfAbsent(deviceId, id -> Maps.newConcurrentMap());
        }

        private Set<StoredFlowEntry> getFlowEntriesInternal(DeviceId deviceId, FlowId flowId) {
            return getFlowTable(deviceId).computeIfAbsent(flowId, id -> Sets.newCopyOnWriteArraySet());
        }

        private StoredFlowEntry getFlowEntryInternal(FlowRule rule) {
            Set<StoredFlowEntry> flowEntries = getFlowEntriesInternal(rule.deviceId(), rule.id());
            return flowEntries.stream()
                              .filter(entry -> Objects.equal(entry, rule))
                              .findAny()
                              .orElse(null);
        }

        private Set<FlowEntry> getFlowEntriesInternal(DeviceId deviceId) {
            Set<FlowEntry> result = Sets.newHashSet();
            getFlowTable(deviceId).values().forEach(result::addAll);
            return result;
        }

        public StoredFlowEntry getFlowEntry(FlowRule rule) {
            return getFlowEntryInternal(rule);
        }

        public Set<FlowEntry> getFlowEntries(DeviceId deviceId) {
            return getFlowEntriesInternal(deviceId);
        }

        public void add(FlowEntry rule) {
            getFlowEntriesInternal(rule.deviceId(), rule.id()).add((StoredFlowEntry) rule);
            lastUpdateTimes.put(rule.deviceId(), System.currentTimeMillis());
        }

        public boolean remove(DeviceId deviceId, FlowEntry rule) {
            try {
                return getFlowEntriesInternal(deviceId, rule.id()).remove(rule);
            } finally {
                lastUpdateTimes.put(deviceId, System.currentTimeMillis());
            }
        }

        private NodeId getBackupNode(DeviceId deviceId) {
            List<NodeId> deviceStandbys = replicaInfoManager.getReplicaInfoFor(deviceId).backups();
            // pick the standby which is most likely to become next master
            return deviceStandbys.isEmpty() ? null : deviceStandbys.get(0);
        }

        private void backup() {
            if (!backupEnabled) {
                return;
            }
            try {
                // determine the set of devices that we need to backup during this run.
                Set<DeviceId> devicesToBackup = mastershipService.getDevicesOf(local)
                            .stream()
                            .filter(deviceId -> {
                                Long lastBackupTime = lastBackupTimes.get(deviceId);
                                Long lastUpdateTime = lastUpdateTimes.get(deviceId);
                                NodeId lastBackupNode = lastBackupNodes.get(deviceId);
                                NodeId newBackupNode = getBackupNode(deviceId);
                                return lastBackupTime == null
                                        ||  !Objects.equal(lastBackupNode, newBackupNode)
                                        || (lastUpdateTime != null && lastUpdateTime > lastBackupTime);
                            })
                            .collect(Collectors.toSet());

                // compute a mapping from node to the set of devices whose flow entries it should backup
                Map<NodeId, Set<DeviceId>> devicesToBackupByNode = Maps.newHashMap();
                devicesToBackup.forEach(deviceId -> {
                    NodeId backupLocation = getBackupNode(deviceId);
                    if (backupLocation != null) {
                        devicesToBackupByNode.computeIfAbsent(backupLocation, nodeId -> Sets.newHashSet())
                                             .add(deviceId);
                    }
                });
                // send the device flow entries to their respective backup nodes
                devicesToBackupByNode.forEach(this::sendBackups);
            } catch (Exception e) {
                log.error("Backup failed.", e);
            }
        }

        private Set<DeviceId> onBackupReceipt(Map<DeviceId, Map<FlowId, Set<StoredFlowEntry>>> flowTables) {
            log.debug("Received flowEntries for {} to backup", flowTables.keySet());
            Set<DeviceId> backedupDevices = Sets.newHashSet();
            try {
                flowTables.forEach((deviceId, deviceFlowTable) -> {
                    // Only process those devices are that not managed by the local node.
                    if (!Objects.equal(local, mastershipService.getMasterFor(deviceId))) {
                        Map<FlowId, Set<StoredFlowEntry>> backupFlowTable = getFlowTable(deviceId);
                        backupFlowTable.clear();
                        backupFlowTable.putAll(deviceFlowTable);
                        backedupDevices.add(deviceId);
                    }
                });
            } catch (Exception e) {
                log.warn("Failure processing backup request", e);
            }
            return backedupDevices;
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

    private class InternalTableStatsListener
        implements EventuallyConsistentMapListener<DeviceId, List<TableStatisticsEntry>> {
        @Override
        public void event(EventuallyConsistentMapEvent<DeviceId,
                          List<TableStatisticsEntry>> event) {
            //TODO: Generate an event to listeners (do we need?)
        }
    }
}
