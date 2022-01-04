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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Streams;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.onlab.util.KryoNamespace;
import org.onlab.util.OrderedExecutor;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.IdGenerator;
import org.onosproject.event.AbstractListenerManager;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.CompletedBatchOperation;
import org.onosproject.net.flow.DefaultFlowEntry;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowEntry.FlowEntryState;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleEvent;
import org.onosproject.net.flow.FlowRuleEvent.Type;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.FlowRuleStore;
import org.onosproject.net.flow.FlowRuleStoreDelegate;
import org.onosproject.net.flow.FlowRuleStoreException;
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
import org.onosproject.store.flow.ReplicaInfo;
import org.onosproject.store.flow.ReplicaInfoEvent;
import org.onosproject.store.flow.ReplicaInfoEventListener;
import org.onosproject.store.flow.ReplicaInfoService;
import org.onosproject.store.impl.MastershipBasedTimestamp;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.AsyncConsistentMap;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.EventuallyConsistentMapEvent;
import org.onosproject.store.service.EventuallyConsistentMapListener;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.WallClockTimestamp;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static org.onlab.util.Tools.get;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.net.flow.FlowRuleEvent.Type.RULE_REMOVED;
import static org.onosproject.store.flow.impl.ECFlowRuleStoreMessageSubjects.APPLY_BATCH_FLOWS;
import static org.onosproject.store.flow.impl.ECFlowRuleStoreMessageSubjects.FLOW_TABLE_BACKUP;
import static org.onosproject.store.flow.impl.ECFlowRuleStoreMessageSubjects.GET_DEVICE_FLOW_COUNT;
import static org.onosproject.store.flow.impl.ECFlowRuleStoreMessageSubjects.GET_FLOW_ENTRY;
import static org.onosproject.store.flow.impl.ECFlowRuleStoreMessageSubjects.PURGE_FLOW_RULES;
import static org.onosproject.store.flow.impl.ECFlowRuleStoreMessageSubjects.REMOTE_APPLY_COMPLETED;
import static org.onosproject.store.flow.impl.ECFlowRuleStoreMessageSubjects.REMOVE_FLOW_ENTRY;
import static org.slf4j.LoggerFactory.getLogger;

import static org.onosproject.store.OsgiPropertyConstants.*;

/**
 * Manages inventory of flow rules using a distributed state management protocol.
 */
@Component(
        immediate = true,
        service = FlowRuleStore.class,
        property = {
                MESSAGE_HANDLER_THREAD_POOL_SIZE + ":Integer=" + MESSAGE_HANDLER_THREAD_POOL_SIZE_DEFAULT,
                BACKUP_PERIOD_MILLIS + ":Integer=" + BACKUP_PERIOD_MILLIS_DEFAULT,
                ANTI_ENTROPY_PERIOD_MILLIS + ":Integer=" + ANTI_ENTROPY_PERIOD_MILLIS_DEFAULT,
                EC_FLOW_RULE_STORE_PERSISTENCE_ENABLED + ":Boolean=" + EC_FLOW_RULE_STORE_PERSISTENCE_ENABLED_DEFAULT,
                MAX_BACKUP_COUNT + ":Integer=" + MAX_BACKUP_COUNT_DEFAULT
        }
)
public class ECFlowRuleStore
    extends AbstractStore<FlowRuleBatchEvent, FlowRuleStoreDelegate>
    implements FlowRuleStore {

    private final Logger log = getLogger(getClass());

    private static final long FLOW_RULE_STORE_TIMEOUT_MILLIS = 5000;
    private static final long PURGE_TIMEOUT_MILLIS = 30000;
    private static final int GET_FLOW_ENTRIES_TIMEOUT = 30; //seconds

    /** Number of threads in the message handler pool. */
    private int msgHandlerPoolSize = MESSAGE_HANDLER_THREAD_POOL_SIZE_DEFAULT;

    /** Delay in ms between successive backup runs. */
    private int backupPeriod = BACKUP_PERIOD_MILLIS_DEFAULT;

    /** Delay in ms between anti-entropy runs. */
    private int antiEntropyPeriod = ANTI_ENTROPY_PERIOD_MILLIS_DEFAULT;

    /** Indicates whether or not changes in the flow table should be persisted to disk. */
    private boolean persistenceEnabled = EC_FLOW_RULE_STORE_PERSISTENCE_ENABLED_DEFAULT;

    /** Max number of backup copies for each device. */
    protected static volatile int backupCount = MAX_BACKUP_COUNT_DEFAULT;

    private InternalFlowTable flowTable = new InternalFlowTable();

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ReplicaInfoService replicaInfoManager;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterCommunicationService clusterCommunicator;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService configService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected PersistenceService persistenceService;

    private Map<Long, NodeId> pendingResponses = Maps.newConcurrentMap();
    private ExecutorService messageHandlingExecutor;
    private ExecutorService eventHandler;

    private ScheduledExecutorService backupScheduler;
    private ExecutorService backupExecutor;

    private EventuallyConsistentMap<DeviceId, List<TableStatisticsEntry>> deviceTableStats;
    private final EventuallyConsistentMapListener<DeviceId, List<TableStatisticsEntry>> tableStatsListener =
        new InternalTableStatsListener();

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    protected final Serializer serializer = Serializer.using(KryoNamespace.newBuilder()
        .register(KryoNamespaces.API)
        .register(BucketId.class)
        .register(FlowBucket.class)
        .register(ImmutablePair.class)
        .build());

    protected final KryoNamespace.Builder serializerBuilder = KryoNamespace.newBuilder()
        .register(KryoNamespaces.API)
        .register(BucketId.class)
        .register(MastershipBasedTimestamp.class);

    protected AsyncConsistentMap<DeviceId, Long> mastershipTermLifecycles;

    private IdGenerator idGenerator;
    private NodeId local;

    @Activate
    public void activate(ComponentContext context) {
        configService.registerProperties(getClass());

        backupScheduler = Executors.newSingleThreadScheduledExecutor(
            groupedThreads("onos/flow", "backup-scheduler", log));
        backupExecutor = Executors.newFixedThreadPool(
            max(min(Runtime.getRuntime().availableProcessors() * 2, 16), 4),
            groupedThreads("onos/flow", "backup-%d", log));

        idGenerator = coreService.getIdGenerator(FlowRuleService.FLOW_OP_TOPIC);

        local = clusterService.getLocalNode().id();

        eventHandler = Executors.newSingleThreadExecutor(
            groupedThreads("onos/flow", "event-handler", log));
        messageHandlingExecutor = Executors.newFixedThreadPool(
            msgHandlerPoolSize, groupedThreads("onos/store/flow", "message-handlers", log));

        registerMessageHandlers(messageHandlingExecutor);

        mastershipTermLifecycles = storageService.<DeviceId, Long>consistentMapBuilder()
            .withName("onos-flow-store-terms")
            .withSerializer(serializer)
            .buildAsyncMap();

        deviceTableStats = storageService.<DeviceId, List<TableStatisticsEntry>>eventuallyConsistentMapBuilder()
            .withName("onos-flow-table-stats")
            .withSerializer(serializerBuilder)
            .withAntiEntropyPeriod(5, TimeUnit.SECONDS)
            .withTimestampProvider((k, v) -> new WallClockTimestamp())
            .withTombstonesDisabled()
            .build();
        deviceTableStats.addListener(tableStatsListener);

        deviceService.addListener(flowTable);
        deviceService.getDevices().forEach(device -> flowTable.addDevice(device.id()));

        logConfig("Started");
    }

    @Deactivate
    public void deactivate(ComponentContext context) {
        configService.unregisterProperties(getClass(), false);
        unregisterMessageHandlers();
        deviceService.removeListener(flowTable);
        deviceTableStats.removeListener(tableStatsListener);
        deviceTableStats.destroy();
        eventHandler.shutdownNow();
        messageHandlingExecutor.shutdownNow();
        backupScheduler.shutdownNow();
        backupExecutor.shutdownNow();
        backupScheduler = null;
        backupExecutor = null;
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
        int newAntiEntropyPeriod;
        try {
            String s = get(properties, "msgHandlerPoolSize");
            newPoolSize = isNullOrEmpty(s) ? msgHandlerPoolSize : Integer.parseInt(s.trim());

            s = get(properties, BACKUP_PERIOD_MILLIS);
            newBackupPeriod = isNullOrEmpty(s) ? backupPeriod : Integer.parseInt(s.trim());

            s = get(properties, MAX_BACKUP_COUNT);
            newBackupCount = isNullOrEmpty(s) ? backupCount : Integer.parseInt(s.trim());

            s = get(properties, ANTI_ENTROPY_PERIOD_MILLIS);
            newAntiEntropyPeriod = isNullOrEmpty(s) ? antiEntropyPeriod : Integer.parseInt(s.trim());
        } catch (NumberFormatException | ClassCastException e) {
            newPoolSize = MESSAGE_HANDLER_THREAD_POOL_SIZE_DEFAULT;
            newBackupPeriod = BACKUP_PERIOD_MILLIS_DEFAULT;
            newBackupCount = MAX_BACKUP_COUNT_DEFAULT;
            newAntiEntropyPeriod = ANTI_ENTROPY_PERIOD_MILLIS_DEFAULT;
        }

        if (newBackupPeriod != backupPeriod) {
            backupPeriod = newBackupPeriod;
            flowTable.setBackupPeriod(newBackupPeriod);
        }

        if (newAntiEntropyPeriod != antiEntropyPeriod) {
            antiEntropyPeriod = newAntiEntropyPeriod;
            flowTable.setAntiEntropyPeriod(newAntiEntropyPeriod);
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
        clusterCommunicator.<Pair<DeviceId, FlowEntryState>, Integer>addSubscriber(
            GET_DEVICE_FLOW_COUNT,
            serializer::decode,
            p -> flowTable.getFlowRuleCount(p.getLeft(), p.getRight()),
            serializer::encode, executor);
        clusterCommunicator.addSubscriber(
            REMOVE_FLOW_ENTRY, serializer::decode, this::removeFlowRuleInternal, serializer::encode, executor);
        clusterCommunicator.<Pair<DeviceId, ApplicationId>, Boolean>addSubscriber(
                PURGE_FLOW_RULES,
                serializer::decode,
                p -> flowTable.purgeFlowRules(p.getLeft(), p.getRight()),
                serializer::encode, executor);
    }

    private void unregisterMessageHandlers() {
        clusterCommunicator.removeSubscriber(REMOVE_FLOW_ENTRY);
        clusterCommunicator.removeSubscriber(GET_DEVICE_FLOW_COUNT);
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
        return getFlowRuleCount(deviceId, null);
    }

    @Override
    public int getFlowRuleCount(DeviceId deviceId, FlowEntryState state) {
        NodeId master = mastershipService.getMasterFor(deviceId);
        if (master == null && deviceService.isAvailable(deviceId)) {
            log.warn("Failed to getFlowRuleCount: No master for {}", deviceId);
            return 0;
        }

        if (Objects.equals(local, master) || master == null) {
            return flowTable.getFlowRuleCount(deviceId, state);
        }

        log.trace("Forwarding getFlowRuleCount to master {} for device {}", master, deviceId);
        return Tools.futureGetOrElse(clusterCommunicator.sendAndReceive(
                Pair.of(deviceId, state),
                GET_DEVICE_FLOW_COUNT,
                serializer::encode,
                serializer::decode,
                master),
                FLOW_RULE_STORE_TIMEOUT_MILLIS,
                TimeUnit.MILLISECONDS,
                0);
    }

    @Override
    public FlowEntry getFlowEntry(FlowRule rule) {
        NodeId master = mastershipService.getMasterFor(rule.deviceId());

        if (master == null && deviceService.isAvailable(rule.deviceId())) {
            log.warn("Failed to getFlowEntry: No master for {}", rule.deviceId());
            return null;
        }

        if (Objects.equals(local, master) || master == null) {
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
        return flowTable.getFlowEntries(deviceId);
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
            log.warn("Failed to storeBatch: No master for {}", deviceId);

            Set<FlowRule> allFailures = operation.getOperations()
                .stream()
                .map(op -> op.target())
                .collect(Collectors.toSet());
            notifyDelegate(FlowRuleBatchEvent.completed(
                new FlowRuleBatchRequest(operation.id(), Collections.emptySet()),
                new CompletedBatchOperation(false, allFailures, deviceId)));
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
                        log.debug("Adding flow rule: {}", entry);
                        flowTable.add(entry);
                        return op;
                    case MODIFY:
                        entry = new DefaultFlowEntry(op.target());
                        log.debug("Updating flow rule: {}", entry);
                        flowTable.update(entry);
                        return op;
                    case REMOVE:
                        return flowTable.update(op.target(), stored -> {
                            stored.setState(FlowEntryState.PENDING_REMOVE);
                            log.debug("Setting state of rule to pending remove: {}", stored);
                            // Using the stored value instead of op to allow the removal
                            // of flows that do not specify actions or have empty treatment
                            return new FlowRuleBatchEntry(op.operator(), new DefaultFlowRule(stored));
                        });
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
            return flowTable.update(rule, stored -> {
                if (stored.state() == FlowEntryState.PENDING_ADD) {
                    stored.setState(FlowEntryState.PENDING_ADD);
                    return new FlowRuleEvent(Type.RULE_UPDATED, rule);
                }
                return null;
            });
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
        FlowRuleEvent event = flowTable.update(rule, stored -> {
            stored.setBytes(rule.bytes());
            stored.setLife(rule.life(TimeUnit.NANOSECONDS), TimeUnit.NANOSECONDS);
            stored.setLiveType(rule.liveType());
            stored.setPackets(rule.packets());
            stored.setLastSeen();
            if (stored.state() == FlowEntryState.PENDING_ADD) {
                stored.setState(FlowEntryState.ADDED);
                return new FlowRuleEvent(Type.RULE_ADDED, rule);
            }
            return new FlowRuleEvent(Type.RULE_UPDATED, rule);
        });
        if (event != null) {
            return event;
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

        return Tools.futureGetOrElse(clusterCommunicator.sendAndReceive(
            rule,
            REMOVE_FLOW_ENTRY,
            serializer::encode,
            serializer::decode,
            master),
            FLOW_RULE_STORE_TIMEOUT_MILLIS,
            TimeUnit.MILLISECONDS,
            null);
    }

    private FlowRuleEvent removeFlowRuleInternal(FlowEntry rule) {
        // This is where one could mark a rule as removed and still keep it in the store.
        final FlowEntry removed = flowTable.remove(rule);
        log.debug("Removed flow rule: {}", removed);
        // rule may be partial rule that is missing treatment, we should use rule from store instead
        return removed != null ? new FlowRuleEvent(RULE_REMOVED, removed) : null;
    }

    @Override
    public void purgeFlowRule(DeviceId deviceId) {
        flowTable.purgeFlowRule(deviceId);
    }

    @Override
    public boolean purgeFlowRules(DeviceId deviceId, ApplicationId appId) {
        NodeId master = mastershipService.getMasterFor(deviceId);

        if (Objects.equals(local, master)) {
            // bypass and handle it locally
            return flowTable.purgeFlowRules(deviceId, appId);
        }

        if (master == null) {
            log.warn("Failed to purgeFlowRules: No master for {}", deviceId);
            return false;
        }

        log.trace("Forwarding purgeFlowRules to {}, which is the master for device {}",
                  master, deviceId);

        return Tools.futureGetOrElse(
                clusterCommunicator.sendAndReceive(
                        Pair.of(deviceId, appId),
                        PURGE_FLOW_RULES,
                        serializer::encode,
                        serializer::decode,
                        master),
                FLOW_RULE_STORE_TIMEOUT_MILLIS,
                TimeUnit.MILLISECONDS,
                false);
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

    private class InternalFlowTable implements DeviceListener {
        private final Map<DeviceId, DeviceFlowTable> flowTables = Maps.newConcurrentMap();

        @Override
        public void event(DeviceEvent event) {
            if (event.type() == DeviceEvent.Type.DEVICE_ADDED) {
                addDevice(event.subject().id());
            }
        }

        /**
         * Adds the given device to the flow table.
         *
         * @param deviceId the device to add to the table
         */
        public void addDevice(DeviceId deviceId) {
            flowTables.computeIfAbsent(deviceId, id -> new DeviceFlowTable(
                id,
                clusterService,
                clusterCommunicator,
                new InternalLifecycleManager(id),
                deviceService,
                backupScheduler,
                new OrderedExecutor(backupExecutor),
                backupPeriod,
                antiEntropyPeriod));
        }

        /**
         * Sets the flow table backup period.
         *
         * @param backupPeriod the flow table backup period
         */
        void setBackupPeriod(int backupPeriod) {
            flowTables.values().forEach(flowTable -> flowTable.setBackupPeriod(backupPeriod));
        }

        /**
         * Sets the flow table anti-entropy period.
         *
         * @param antiEntropyPeriod the flow table anti-entropy period
         */
        void setAntiEntropyPeriod(int antiEntropyPeriod) {
            flowTables.values().forEach(flowTable -> flowTable.setAntiEntropyPeriod(antiEntropyPeriod));
        }

        /**
         * Returns the flow table for a specific device.
         *
         * @param deviceId the device identifier
         * @return the flow table for the given device
         */
        private DeviceFlowTable getFlowTable(DeviceId deviceId) {
            DeviceFlowTable flowTable = flowTables.get(deviceId);
            return flowTable != null ? flowTable : flowTables.computeIfAbsent(deviceId, id -> new DeviceFlowTable(
                deviceId,
                clusterService,
                clusterCommunicator,
                new InternalLifecycleManager(deviceId),
                deviceService,
                backupScheduler,
                new OrderedExecutor(backupExecutor),
                backupPeriod,
                antiEntropyPeriod));
        }

        /**
         * Returns the flow rule count for the given device.
         *
         * @param deviceId the device for which to return the flow rule count
         * @return the flow rule count for the given device
         */
        public int getFlowRuleCount(DeviceId deviceId) {
            return getFlowTable(deviceId).count();
        }

        /**
         * Returns the count of flow rules in the given state for the given device.
         *
         * @param deviceId the device for which to return the flow rule count
         * @return the flow rule count for the given device
         */
        public int getFlowRuleCount(DeviceId deviceId, FlowEntryState state) {
            if (state == null) {
                return getFlowRuleCount(deviceId);
            }
            return (int) StreamSupport.stream(getFlowEntries(deviceId).spliterator(), false)
                .filter(rule -> rule.state() == state)
                .count();
        }

        /**
         * Returns the flow entry for the given rule.
         *
         * @param rule the rule for which to return the flow entry
         * @return the flow entry for the given rule
         */
        public StoredFlowEntry getFlowEntry(FlowRule rule) {
            return getFlowTable(rule.deviceId()).getFlowEntry(rule);
        }

        /**
         * Returns the set of flow entries for the given device.
         *
         * @param deviceId the device for which to lookup flow entries
         * @return the set of flow entries for the given device
         */
        public Iterable<FlowEntry> getFlowEntries(DeviceId deviceId) {
            try {
                return getFlowTable(deviceId).getFlowEntries()
                    .get(GET_FLOW_ENTRIES_TIMEOUT, TimeUnit.SECONDS);
            } catch (ExecutionException e) {
                throw new FlowRuleStoreException(e.getCause());
            } catch (TimeoutException e) {
                throw new FlowRuleStoreException.Timeout();
            } catch (InterruptedException e) {
                throw new FlowRuleStoreException.Interrupted();
            }
        }

        /**
         * Adds the given flow rule.
         *
         * @param rule the rule to add
         */
        public void add(FlowEntry rule) {
            Tools.futureGetOrElse(
                getFlowTable(rule.deviceId()).add(rule),
                FLOW_RULE_STORE_TIMEOUT_MILLIS,
                TimeUnit.MILLISECONDS,
                null);
        }

        /**
         * Updates the given flow rule.
         *
         * @param rule the rule to update
         */
        public void update(FlowEntry rule) {
            Tools.futureGetOrElse(
                getFlowTable(rule.deviceId()).update(rule),
                FLOW_RULE_STORE_TIMEOUT_MILLIS,
                TimeUnit.MILLISECONDS,
                null);
        }

        /**
         * Applies the given update function to the rule.
         *
         * @param function the update function to apply
         * @return a future to be completed with the update event or {@code null} if the rule was not updated
         */
        public <T> T update(FlowRule rule, Function<StoredFlowEntry, T> function) {
            return Tools.futureGetOrElse(
                getFlowTable(rule.deviceId()).update(rule, function),
                FLOW_RULE_STORE_TIMEOUT_MILLIS,
                TimeUnit.MILLISECONDS,
                null);
        }

        /**
         * Removes the given flow rule.
         *
         * @param rule the rule to remove
         */
        public FlowEntry remove(FlowEntry rule) {
            return Tools.futureGetOrElse(
                getFlowTable(rule.deviceId()).remove(rule),
                FLOW_RULE_STORE_TIMEOUT_MILLIS,
                TimeUnit.MILLISECONDS,
                null);
        }

        /**
         * Purges flow rules for the given device.
         *
         * @param deviceId the device for which to purge flow rules
         */
        public void purgeFlowRule(DeviceId deviceId) {
            // If the device is still present in the store, purge the underlying DeviceFlowTable.
            // Otherwise, remove the DeviceFlowTable and unregister message handlers.
            if (deviceService.getDevice(deviceId) != null) {
                DeviceFlowTable flowTable = flowTables.get(deviceId);
                if (flowTable != null) {
                    flowTable.purge();
                }
            } else {
                DeviceFlowTable flowTable = flowTables.remove(deviceId);
                if (flowTable != null) {
                    flowTable.close();
                }
            }
        }

        /**
         * Purges flow rules for the given device and application id.
         *
         * @param deviceId the device for which to purge flow rules
         * @param appId the application id for with to purge flow rules
         * @return true if purge is successful, false otherwise
         */
        public boolean purgeFlowRules(DeviceId deviceId, ApplicationId appId) {
            DeviceFlowTable flowTable = flowTables.get(deviceId);
            if (flowTable != null) {
                // flowTable.purge() returns a CompletableFuture<Void>, we want
                // to return true when the completable future returns correctly
                // within the timeout, otherwise return false.
                try {
                    // Use higher timeout, purge(appId) may require more time
                    // than normal operations because it's applying the purge
                    // operation on every single flow table bucket.
                    flowTable.purge(appId).get(PURGE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
                    return true;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException | TimeoutException ignored) {
                }
            }
            return false;
        }

        /**
         * Purges all flow rules from the table.
         */
        public void purgeFlowRules() {
            Iterator<DeviceFlowTable> iterator = flowTables.values().iterator();
            while (iterator.hasNext()) {
                iterator.next().close();
                iterator.remove();
            }
        }
    }

    @Override
    public FlowRuleEvent updateTableStatistics(DeviceId deviceId, List<TableStatisticsEntry> tableStats) {
        deviceTableStats.put(deviceId, tableStats);
        return null;
    }

    @Override
    public Iterable<TableStatisticsEntry> getTableStatistics(DeviceId deviceId) {
        NodeId master = mastershipService.getMasterFor(deviceId);

        if (master == null && deviceService.isAvailable(deviceId)) {
            log.warn("Failed to getTableStats: No master for {}", deviceId);
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

    /**
     * Device lifecycle manager implementation.
     */
    private final class InternalLifecycleManager
        extends AbstractListenerManager<LifecycleEvent, LifecycleEventListener>
        implements LifecycleManager, ReplicaInfoEventListener, MapEventListener<DeviceId, Long> {

        private final DeviceId deviceId;

        private volatile DeviceReplicaInfo replicaInfo;

        InternalLifecycleManager(DeviceId deviceId) {
            this.deviceId = deviceId;
            replicaInfoManager.addListener(this);
            mastershipTermLifecycles.addListener(this);
            replicaInfo = toDeviceReplicaInfo(replicaInfoManager.getReplicaInfoFor(deviceId));
        }

        @Override
        public DeviceReplicaInfo getReplicaInfo() {
            return replicaInfo;
        }

        @Override
        public void activate(long term) {
            final ReplicaInfo replicaInfo = replicaInfoManager.getReplicaInfoFor(deviceId);
            if (replicaInfo != null && replicaInfo.term() == term) {
                mastershipTermLifecycles.put(deviceId, term);
            }
        }

        @Override
        public void event(ReplicaInfoEvent event) {
            if (event.subject().equals(deviceId)) {
                onReplicaInfoChange(event.replicaInfo());
            }
        }

        @Override
        public void event(MapEvent<DeviceId, Long> event) {
            if (event.key().equals(deviceId) && event.newValue() != null) {
                onActivate(event.newValue().value());
            }
        }

        /**
         * Handles a term activation event.
         *
         * @param term the term that was activated
         */
        private void onActivate(long term) {
            final ReplicaInfo replicaInfo = replicaInfoManager.getReplicaInfoFor(deviceId);
            if (replicaInfo != null && replicaInfo.term() == term) {
                NodeId master = replicaInfo.master().orElse(null);
                List<NodeId> backups = replicaInfo.backups()
                    .subList(0, min(replicaInfo.backups().size(), backupCount));
                listenerRegistry.process(new LifecycleEvent(
                    LifecycleEvent.Type.TERM_ACTIVE,
                    new DeviceReplicaInfo(term, master, backups)));
            }
        }

        /**
         * Handles a replica info change event.
         *
         * @param replicaInfo the updated replica info
         */
        private synchronized void onReplicaInfoChange(ReplicaInfo replicaInfo) {
            DeviceReplicaInfo oldReplicaInfo = this.replicaInfo;
            this.replicaInfo = toDeviceReplicaInfo(replicaInfo);
            if (oldReplicaInfo == null || oldReplicaInfo.term() < replicaInfo.term()) {
                if (oldReplicaInfo != null) {
                    listenerRegistry.process(new LifecycleEvent(LifecycleEvent.Type.TERM_END, oldReplicaInfo));
                }
                listenerRegistry.process(new LifecycleEvent(LifecycleEvent.Type.TERM_START, this.replicaInfo));
            } else if (oldReplicaInfo.term() == replicaInfo.term()) {
                listenerRegistry.process(new LifecycleEvent(LifecycleEvent.Type.TERM_UPDATE, this.replicaInfo));
            }
        }

        /**
         * Converts the given replica info into a {@link DeviceReplicaInfo} instance.
         *
         * @param replicaInfo the replica info to convert
         * @return the converted replica info
         */
        private DeviceReplicaInfo toDeviceReplicaInfo(ReplicaInfo replicaInfo) {
            NodeId master = replicaInfo.master().orElse(null);
            List<NodeId> backups = replicaInfo.backups()
                .subList(0, min(replicaInfo.backups().size(), backupCount));
            return new DeviceReplicaInfo(replicaInfo.term(), master, backups);
        }

        @Override
        public void close() {
            replicaInfoManager.removeListener(this);
            mastershipTermLifecycles.removeListener(this);
        }
    }

    private static class CountMessage {
        private final DeviceId deviceId;
        private final FlowEntryState state;

        CountMessage(DeviceId deviceId, FlowEntryState state) {
            this.deviceId = deviceId;
            this.state = state;
        }
    }
}