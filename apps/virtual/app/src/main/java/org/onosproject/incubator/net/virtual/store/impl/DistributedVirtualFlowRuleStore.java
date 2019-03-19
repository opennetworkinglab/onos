/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.incubator.net.virtual.store.impl;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Futures;
import org.onlab.util.KryoNamespace;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.CoreService;
import org.onosproject.core.IdGenerator;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.VirtualNetworkFlowRuleStore;
import org.onosproject.incubator.net.virtual.VirtualNetworkService;
import org.onosproject.incubator.net.virtual.store.impl.primitives.VirtualDeviceId;
import org.onosproject.incubator.net.virtual.store.impl.primitives.VirtualFlowEntry;
import org.onosproject.incubator.net.virtual.store.impl.primitives.VirtualFlowRule;
import org.onosproject.incubator.net.virtual.store.impl.primitives.VirtualFlowRuleBatchEvent;
import org.onosproject.incubator.net.virtual.store.impl.primitives.VirtualFlowRuleBatchOperation;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.BatchOperationEntry;
import org.onosproject.net.flow.CompletedBatchOperation;
import org.onosproject.net.flow.DefaultFlowEntry;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowId;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleEvent;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.FlowRuleStoreDelegate;
import org.onosproject.net.flow.StoredFlowEntry;
import org.onosproject.net.flow.TableStatisticsEntry;
import org.onosproject.net.flow.oldbatch.FlowRuleBatchEntry;
import org.onosproject.net.flow.oldbatch.FlowRuleBatchEvent;
import org.onosproject.net.flow.oldbatch.FlowRuleBatchOperation;
import org.onosproject.net.flow.oldbatch.FlowRuleBatchRequest;
import org.onosproject.store.Timestamp;
import org.onosproject.store.cluster.messaging.ClusterCommunicationService;
import org.onosproject.store.cluster.messaging.ClusterMessage;
import org.onosproject.store.cluster.messaging.ClusterMessageHandler;
import org.onosproject.store.cluster.messaging.MessageSubject;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.EventuallyConsistentMapEvent;
import org.onosproject.store.service.EventuallyConsistentMapListener;
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

import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;
import static org.onlab.util.Tools.get;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.incubator.net.virtual.store.impl.OsgiPropertyConstants.*;
import static org.onosproject.net.flow.FlowRuleEvent.Type.RULE_REMOVED;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Manages inventory of flow rules using a distributed state management protocol
 * for virtual networks.
 */
//TODO: support backup and persistent mechanism
@Component(immediate = true, enabled = false, service = VirtualNetworkFlowRuleStore.class,
        property = {
                MESSAGE_HANDLER_THREAD_POOL_SIZE + ":Integer=" + MESSAGE_HANDLER_THREAD_POOL_SIZE_DEFAULT,
                BACKUP_PERIOD_MILLIS + ":Integer=" + BACKUP_PERIOD_MILLIS_DEFAULT,
                PERSISTENCE_ENABLED + ":Boolean=" + PERSISTENCE_ENABLED_DEFAULT,
        })

public class DistributedVirtualFlowRuleStore
        extends AbstractVirtualStore<FlowRuleBatchEvent, FlowRuleStoreDelegate>
        implements VirtualNetworkFlowRuleStore {

    private final Logger log = getLogger(getClass());

    //TODO: confirm this working fine with multiple thread more than 1
    private static final long FLOW_RULE_STORE_TIMEOUT_MILLIS = 5000;

    private static final String FLOW_OP_TOPIC = "virtual-flow-ops-ids";

    // MessageSubjects used by DistributedVirtualFlowRuleStore peer-peer communication.
    private static final MessageSubject APPLY_BATCH_FLOWS
            = new MessageSubject("virtual-peer-forward-apply-batch");
    private static final MessageSubject GET_FLOW_ENTRY
            = new MessageSubject("virtual-peer-forward-get-flow-entry");
    private static final MessageSubject GET_DEVICE_FLOW_ENTRIES
            = new MessageSubject("virtual-peer-forward-get-device-flow-entries");
    private static final MessageSubject REMOVE_FLOW_ENTRY
            = new MessageSubject("virtual-peer-forward-remove-flow-entry");
    private static final MessageSubject REMOTE_APPLY_COMPLETED
            = new MessageSubject("virtual-peer-apply-completed");

    /** Number of threads in the message handler pool. */
    private int messageHandlerThreadPoolSize = MESSAGE_HANDLER_THREAD_POOL_SIZE_DEFAULT;

    /** Delay in ms between successive backup runs. */
    private int backupPeriod = BACKUP_PERIOD_MILLIS_DEFAULT;

    /** Indicates whether or not changes in the flow table should be persisted to disk.. */
    private boolean persistenceEnabled = PERSISTENCE_ENABLED_DEFAULT;

    private InternalFlowTable flowTable = new InternalFlowTable();

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterCommunicationService clusterCommunicator;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService configService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected VirtualNetworkService vnaService;

    private Map<Long, NodeId> pendingResponses = Maps.newConcurrentMap();
    private ExecutorService messageHandlingExecutor;
    private ExecutorService eventHandler;

    private EventuallyConsistentMap<NetworkId, Map<DeviceId, List<TableStatisticsEntry>>> deviceTableStats;
    private final EventuallyConsistentMapListener<NetworkId, Map<DeviceId, List<TableStatisticsEntry>>>
            tableStatsListener = new InternalTableStatsListener();


    protected final Serializer serializer = Serializer.using(KryoNamespace.newBuilder()
                                                                     .register(KryoNamespaces.API)
                                                                     .register(NetworkId.class)
                                                                     .register(VirtualFlowRuleBatchOperation.class)
                                                                     .register(VirtualFlowRuleBatchEvent.class)
                                                                     .build());

    protected final KryoNamespace.Builder serializerBuilder = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API)
            .register(MastershipBasedTimestamp.class);

    private IdGenerator idGenerator;
    private NodeId local;


    @Activate
    public void activate(ComponentContext context) {
        configService.registerProperties(getClass());

        idGenerator = coreService.getIdGenerator(FLOW_OP_TOPIC);

        local = clusterService.getLocalNode().id();

        eventHandler = Executors.newSingleThreadExecutor(
                groupedThreads("onos/virtual-flow", "event-handler", log));
        messageHandlingExecutor = Executors.newFixedThreadPool(
            messageHandlerThreadPoolSize, groupedThreads("onos/store/virtual-flow", "message-handlers", log));

        registerMessageHandlers(messageHandlingExecutor);

        deviceTableStats = storageService
                .<NetworkId, Map<DeviceId, List<TableStatisticsEntry>>>eventuallyConsistentMapBuilder()
                .withName("onos-virtual-flow-table-stats")
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
        configService.unregisterProperties(getClass(), false);
        unregisterMessageHandlers();
        deviceTableStats.removeListener(tableStatsListener);
        deviceTableStats.destroy();
        eventHandler.shutdownNow();
        messageHandlingExecutor.shutdownNow();
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
        try {
            String s = get(properties, MESSAGE_HANDLER_THREAD_POOL_SIZE);
            newPoolSize = isNullOrEmpty(s) ? messageHandlerThreadPoolSize : Integer.parseInt(s.trim());

            s = get(properties, BACKUP_PERIOD_MILLIS);
            newBackupPeriod = isNullOrEmpty(s) ? backupPeriod : Integer.parseInt(s.trim());
        } catch (NumberFormatException | ClassCastException e) {
            newPoolSize = MESSAGE_HANDLER_THREAD_POOL_SIZE_DEFAULT;
            newBackupPeriod = BACKUP_PERIOD_MILLIS_DEFAULT;
        }

        boolean restartBackupTask = false;

        if (newBackupPeriod != backupPeriod) {
            backupPeriod = newBackupPeriod;
            restartBackupTask = true;
        }
        if (restartBackupTask) {
            log.warn("Currently, backup tasks are not supported.");
        }
        if (newPoolSize != messageHandlerThreadPoolSize) {
            messageHandlerThreadPoolSize = newPoolSize;
            ExecutorService oldMsgHandler = messageHandlingExecutor;
            messageHandlingExecutor = Executors.newFixedThreadPool(
                messageHandlerThreadPoolSize, groupedThreads("onos/store/virtual-flow", "message-handlers", log));

            // replace previously registered handlers.
            registerMessageHandlers(messageHandlingExecutor);
            oldMsgHandler.shutdown();
        }

        logConfig("Reconfigured");
    }

    @Override
    public int getFlowRuleCount(NetworkId networkId) {
        AtomicInteger sum = new AtomicInteger(0);
        DeviceService deviceService = vnaService.get(networkId, DeviceService.class);
        deviceService.getDevices()
                .forEach(device -> sum.addAndGet(
                        Iterables.size(getFlowEntries(networkId, device.id()))));
        return sum.get();
    }

    @Override
    public FlowEntry getFlowEntry(NetworkId networkId, FlowRule rule) {
        MastershipService mastershipService =
                vnaService.get(networkId, MastershipService.class);
        NodeId master = mastershipService.getMasterFor(rule.deviceId());

        if (master == null) {
            log.debug("Failed to getFlowEntry: No master for {}, vnet {}",
                      rule.deviceId(), networkId);
            return null;
        }

        if (Objects.equals(local, master)) {
            return flowTable.getFlowEntry(networkId, rule);
        }

        log.trace("Forwarding getFlowEntry to {}, which is the primary (master) " +
                          "for device {}, vnet {}",
                  master, rule.deviceId(), networkId);

        VirtualFlowRule vRule = new VirtualFlowRule(networkId, rule);

        return Tools.futureGetOrElse(clusterCommunicator.sendAndReceive(vRule,
                                                                        GET_FLOW_ENTRY,
                                                                        serializer::encode,
                                                                        serializer::decode,
                                                                        master),
                                     FLOW_RULE_STORE_TIMEOUT_MILLIS,
                                     TimeUnit.MILLISECONDS,
                                     null);
    }

    @Override
    public Iterable<FlowEntry> getFlowEntries(NetworkId networkId, DeviceId deviceId) {
        MastershipService mastershipService =
                vnaService.get(networkId, MastershipService.class);
        NodeId master = mastershipService.getMasterFor(deviceId);

        if (master == null) {
            log.debug("Failed to getFlowEntries: No master for {}, vnet {}", deviceId, networkId);
            return Collections.emptyList();
        }

        if (Objects.equals(local, master)) {
            return flowTable.getFlowEntries(networkId, deviceId);
        }

        log.trace("Forwarding getFlowEntries to {}, which is the primary (master) for device {}",
                  master, deviceId);

        return Tools.futureGetOrElse(
                clusterCommunicator.sendAndReceive(deviceId,
                                                   GET_DEVICE_FLOW_ENTRIES,
                                                   serializer::encode,
                                                   serializer::decode,
                                                   master),
                FLOW_RULE_STORE_TIMEOUT_MILLIS,
                TimeUnit.MILLISECONDS,
                Collections.emptyList());
    }

    @Override
    public void storeBatch(NetworkId networkId, FlowRuleBatchOperation operation) {
        if (operation.getOperations().isEmpty()) {
            notifyDelegate(networkId, FlowRuleBatchEvent.completed(
                    new FlowRuleBatchRequest(operation.id(), Collections.emptySet()),
                    new CompletedBatchOperation(true, Collections.emptySet(), operation.deviceId())));
            return;
        }

        DeviceId deviceId = operation.deviceId();
        MastershipService mastershipService =
                vnaService.get(networkId, MastershipService.class);
        NodeId master = mastershipService.getMasterFor(deviceId);

        if (master == null) {
            log.warn("No master for {}, vnet {} : flows will be marked for removal", deviceId, networkId);

            updateStoreInternal(networkId, operation);

            notifyDelegate(networkId, FlowRuleBatchEvent.completed(
                    new FlowRuleBatchRequest(operation.id(), Collections.emptySet()),
                    new CompletedBatchOperation(true, Collections.emptySet(), operation.deviceId())));
            return;
        }

        if (Objects.equals(local, master)) {
            storeBatchInternal(networkId, operation);
            return;
        }

        log.trace("Forwarding storeBatch to {}, which is the primary (master) for device {}, vent {}",
                  master, deviceId, networkId);

        clusterCommunicator.unicast(new VirtualFlowRuleBatchOperation(networkId, operation),
                                    APPLY_BATCH_FLOWS,
                                    serializer::encode,
                                    master)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        log.warn("Failed to storeBatch: {} to {}", operation, master, error);

                        Set<FlowRule> allFailures = operation.getOperations()
                                .stream()
                                .map(BatchOperationEntry::target)
                                .collect(Collectors.toSet());

                        notifyDelegate(networkId, FlowRuleBatchEvent.completed(
                                new FlowRuleBatchRequest(operation.id(), Collections.emptySet()),
                                new CompletedBatchOperation(false, allFailures, deviceId)));
                    }
                });
    }

    @Override
    public void batchOperationComplete(NetworkId networkId, FlowRuleBatchEvent event) {
        //FIXME: need a per device pending response
        NodeId nodeId = pendingResponses.remove(event.subject().batchId());
        if (nodeId == null) {
            notifyDelegate(networkId, event);
        } else {
            // TODO check unicast return value
            clusterCommunicator.unicast(new VirtualFlowRuleBatchEvent(networkId, event),
                                        REMOTE_APPLY_COMPLETED, serializer::encode, nodeId);
            //error log: log.warn("Failed to respond to peer for batch operation result");
        }
    }

    @Override
    public void deleteFlowRule(NetworkId networkId, FlowRule rule) {
        storeBatch(networkId,
                new FlowRuleBatchOperation(
                        Collections.singletonList(
                                new FlowRuleBatchEntry(
                                        FlowRuleBatchEntry.FlowRuleOperation.REMOVE,
                                        rule)), rule.deviceId(), idGenerator.getNewId()));
    }

    @Override
    public FlowRuleEvent addOrUpdateFlowRule(NetworkId networkId, FlowEntry rule) {
        MastershipService mastershipService =
                vnaService.get(networkId, MastershipService.class);
        NodeId master = mastershipService.getMasterFor(rule.deviceId());
        if (Objects.equals(local, master)) {
            return addOrUpdateFlowRuleInternal(networkId, rule);
        }

        log.warn("Tried to update FlowRule {} state,"
                         + " while the Node was not the master.", rule);
        return null;
    }

    private FlowRuleEvent addOrUpdateFlowRuleInternal(NetworkId networkId, FlowEntry rule) {
        // check if this new rule is an update to an existing entry
        StoredFlowEntry stored = flowTable.getFlowEntry(networkId, rule);
        if (stored != null) {
            //FIXME modification of "stored" flow entry outside of flow table
            stored.setBytes(rule.bytes());
            stored.setLife(rule.life(TimeUnit.NANOSECONDS), TimeUnit.NANOSECONDS);
            stored.setLiveType(rule.liveType());
            stored.setPackets(rule.packets());
            stored.setLastSeen();
            if (stored.state() == FlowEntry.FlowEntryState.PENDING_ADD) {
                stored.setState(FlowEntry.FlowEntryState.ADDED);
                return new FlowRuleEvent(FlowRuleEvent.Type.RULE_ADDED, rule);
            }
            return new FlowRuleEvent(FlowRuleEvent.Type.RULE_UPDATED, rule);
        }

        // TODO: Confirm if this behavior is correct. See SimpleFlowRuleStore
        // TODO: also update backup if the behavior is correct.
        flowTable.add(networkId, rule);
        return null;
    }

    @Override
    public FlowRuleEvent removeFlowRule(NetworkId networkId, FlowEntry rule) {
        final DeviceId deviceId = rule.deviceId();

        MastershipService mastershipService =
                vnaService.get(networkId, MastershipService.class);
        NodeId master = mastershipService.getMasterFor(deviceId);

        if (Objects.equals(local, master)) {
            // bypass and handle it locally
            return removeFlowRuleInternal(new VirtualFlowEntry(networkId, rule));
        }

        if (master == null) {
            log.warn("Failed to removeFlowRule: No master for {}", deviceId);
            // TODO: revisit if this should be null (="no-op") or Exception
            return null;
        }

        log.trace("Forwarding removeFlowRule to {}, which is the master for device {}",
                  master, deviceId);

        return Futures.getUnchecked(clusterCommunicator.sendAndReceive(
                new VirtualFlowEntry(networkId, rule),
                REMOVE_FLOW_ENTRY,
                serializer::encode,
                serializer::decode,
                master));
    }

    @Override
    public FlowRuleEvent pendingFlowRule(NetworkId networkId, FlowEntry rule) {
        MastershipService mastershipService =
                vnaService.get(networkId, MastershipService.class);
        if (mastershipService.isLocalMaster(rule.deviceId())) {
            StoredFlowEntry stored = flowTable.getFlowEntry(networkId, rule);
            if (stored != null &&
                    stored.state() != FlowEntry.FlowEntryState.PENDING_ADD) {
                stored.setState(FlowEntry.FlowEntryState.PENDING_ADD);
                return new FlowRuleEvent(FlowRuleEvent.Type.RULE_UPDATED, rule);
            }
        }
        return null;
    }

    @Override
    public void purgeFlowRules(NetworkId networkId) {
        flowTable.purgeFlowRules(networkId);
    }

    @Override
    public FlowRuleEvent updateTableStatistics(NetworkId networkId,
                                               DeviceId deviceId,
                                               List<TableStatisticsEntry> tableStats) {
        if (deviceTableStats.get(networkId) == null) {
            deviceTableStats.put(networkId, Maps.newConcurrentMap());
        }
        deviceTableStats.get(networkId).put(deviceId, tableStats);
        return null;
    }

    @Override
    public Iterable<TableStatisticsEntry> getTableStatistics(NetworkId networkId, DeviceId deviceId) {
        MastershipService mastershipService =
                vnaService.get(networkId, MastershipService.class);
        NodeId master = mastershipService.getMasterFor(deviceId);

        if (master == null) {
            log.debug("Failed to getTableStats: No master for {}", deviceId);
            return Collections.emptyList();
        }

        if (deviceTableStats.get(networkId) == null) {
            deviceTableStats.put(networkId, Maps.newConcurrentMap());
        }

        List<TableStatisticsEntry> tableStats = deviceTableStats.get(networkId).get(deviceId);
        if (tableStats == null) {
            return Collections.emptyList();
        }
        return ImmutableList.copyOf(tableStats);
    }

    private void registerMessageHandlers(ExecutorService executor) {
        clusterCommunicator.addSubscriber(APPLY_BATCH_FLOWS, new OnStoreBatch(), executor);
        clusterCommunicator.<VirtualFlowRuleBatchEvent>addSubscriber(
                REMOTE_APPLY_COMPLETED, serializer::decode,
                this::notifyDelicateByNetwork, executor);
        clusterCommunicator.addSubscriber(
                GET_FLOW_ENTRY, serializer::decode, this::getFlowEntryByNetwork,
                serializer::encode, executor);
        clusterCommunicator.addSubscriber(
                GET_DEVICE_FLOW_ENTRIES, serializer::decode,
                this::getFlowEntriesByNetwork,
                serializer::encode, executor);
        clusterCommunicator.addSubscriber(
                REMOVE_FLOW_ENTRY, serializer::decode, this::removeFlowRuleInternal,
                serializer::encode, executor);
    }

    private void unregisterMessageHandlers() {
        clusterCommunicator.removeSubscriber(REMOVE_FLOW_ENTRY);
        clusterCommunicator.removeSubscriber(GET_DEVICE_FLOW_ENTRIES);
        clusterCommunicator.removeSubscriber(GET_FLOW_ENTRY);
        clusterCommunicator.removeSubscriber(APPLY_BATCH_FLOWS);
        clusterCommunicator.removeSubscriber(REMOTE_APPLY_COMPLETED);
    }


    private void logConfig(String prefix) {
        log.info("{} with msgHandlerPoolSize = {}; backupPeriod = {}",
                 prefix, messageHandlerThreadPoolSize, backupPeriod);
    }

    private void storeBatchInternal(NetworkId networkId, FlowRuleBatchOperation operation) {

        final DeviceId did = operation.deviceId();
        //final Collection<FlowEntry> ft = flowTable.getFlowEntries(did);
        Set<FlowRuleBatchEntry> currentOps = updateStoreInternal(networkId, operation);
        if (currentOps.isEmpty()) {
            batchOperationComplete(networkId, FlowRuleBatchEvent.completed(
                    new FlowRuleBatchRequest(operation.id(), Collections.emptySet()),
                    new CompletedBatchOperation(true, Collections.emptySet(), did)));
            return;
        }

        //Confirm that flowrule service is created
        vnaService.get(networkId, FlowRuleService.class);

        notifyDelegate(networkId, FlowRuleBatchEvent.requested(new
                                                            FlowRuleBatchRequest(operation.id(),
                                                                                 currentOps), operation.deviceId()));
    }

    private Set<FlowRuleBatchEntry> updateStoreInternal(NetworkId networkId,
                                                        FlowRuleBatchOperation operation) {
        return operation.getOperations().stream().map(
                op -> {
                    StoredFlowEntry entry;
                    switch (op.operator()) {
                        case ADD:
                            entry = new DefaultFlowEntry(op.target());
                            // always add requested FlowRule
                            // Note: 2 equal FlowEntry may have different treatment
                            flowTable.remove(networkId, entry.deviceId(), entry);
                            flowTable.add(networkId, entry);

                            return op;
                        case REMOVE:
                            entry = flowTable.getFlowEntry(networkId, op.target());
                            if (entry != null) {
                                //FIXME modification of "stored" flow entry outside of flow table
                                entry.setState(FlowEntry.FlowEntryState.PENDING_REMOVE);
                                log.debug("Setting state of rule to pending remove: {}", entry);
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
        ).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    private FlowRuleEvent removeFlowRuleInternal(VirtualFlowEntry rule) {
        final DeviceId deviceId = rule.flowEntry().deviceId();
        // This is where one could mark a rule as removed and still keep it in the store.
        final FlowEntry removed = flowTable.remove(rule.networkId(), deviceId, rule.flowEntry());
        // rule may be partial rule that is missing treatment, we should use rule from store instead
        return removed != null ? new FlowRuleEvent(RULE_REMOVED, removed) : null;
    }

    private final class OnStoreBatch implements ClusterMessageHandler {

        @Override
        public void handle(final ClusterMessage message) {
            VirtualFlowRuleBatchOperation vOperation = serializer.decode(message.payload());
            log.debug("received batch request {}", vOperation);

            FlowRuleBatchOperation operation = vOperation.operation();

            final DeviceId deviceId = operation.deviceId();
            MastershipService mastershipService =
                    vnaService.get(vOperation.networkId(), MastershipService.class);
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
            storeBatchInternal(vOperation.networkId(), operation);
        }
    }

    /**
     * Returns flow rule entry using virtual flow rule.
     *
     * @param rule an encapsulated flow rule to be queried
     */
    private FlowEntry getFlowEntryByNetwork(VirtualFlowRule rule) {
        return flowTable.getFlowEntry(rule.networkId(), rule.rule());
    }

    /**
     * returns flow entries using virtual device id.
     *
     * @param deviceId an encapsulated virtual device id
     * @return a set of flow entries
     */
    private Set<FlowEntry> getFlowEntriesByNetwork(VirtualDeviceId deviceId) {
        return flowTable.getFlowEntries(deviceId.networkId(), deviceId.deviceId());
    }

    /**
     * span out Flow Rule Batch event according to virtual network id.
     *
     * @param event a event to be span out
     */
    private void notifyDelicateByNetwork(VirtualFlowRuleBatchEvent event) {
        batchOperationComplete(event.networkId(), event.event());
    }

    private class InternalFlowTable {
        //TODO replace the Map<V,V> with ExtendedSet
        //TODO: support backup mechanism
        private final Map<NetworkId, Map<DeviceId, Map<FlowId, Map<StoredFlowEntry, StoredFlowEntry>>>>
                flowEntriesMap = Maps.newConcurrentMap();
        private final Map<NetworkId, Map<DeviceId, Long>> lastUpdateTimesMap = Maps.newConcurrentMap();

        private Map<DeviceId, Map<FlowId, Map<StoredFlowEntry, StoredFlowEntry>>>
        getFlowEntriesByNetwork(NetworkId networkId) {
            return flowEntriesMap.computeIfAbsent(networkId, k -> Maps.newConcurrentMap());
        }

        private Map<DeviceId, Long> getLastUpdateTimesByNetwork(NetworkId networkId) {
            return lastUpdateTimesMap.computeIfAbsent(networkId, k -> Maps.newConcurrentMap());
        }

        /**
         * Returns the flow table for specified device.
         *
         * @param networkId virtual network identifier
         * @param deviceId identifier of the device
         * @return Map representing Flow Table of given device.
         */
        private Map<FlowId, Map<StoredFlowEntry, StoredFlowEntry>>
        getFlowTable(NetworkId networkId, DeviceId deviceId) {
            Map<DeviceId, Map<FlowId, Map<StoredFlowEntry, StoredFlowEntry>>>
                    flowEntries = getFlowEntriesByNetwork(networkId);
            if (persistenceEnabled) {
                //TODO: support persistent
                log.warn("Persistent is not supported");
                return null;
            } else {
                return flowEntries.computeIfAbsent(deviceId, id -> Maps.newConcurrentMap());
            }
        }

        private Map<FlowId, Map<StoredFlowEntry, StoredFlowEntry>>
        getFlowTableCopy(NetworkId networkId, DeviceId deviceId) {

            Map<DeviceId, Map<FlowId, Map<StoredFlowEntry, StoredFlowEntry>>>
                    flowEntries = getFlowEntriesByNetwork(networkId);
            Map<FlowId, Map<StoredFlowEntry, StoredFlowEntry>> copy = Maps.newHashMap();

            if (persistenceEnabled) {
                //TODO: support persistent
                log.warn("Persistent is not supported");
                return null;
            } else {
                flowEntries.computeIfAbsent(deviceId, id -> Maps.newConcurrentMap()).forEach((k, v) -> {
                    copy.put(k, Maps.newHashMap(v));
                });
                return copy;
            }
        }

        private Map<StoredFlowEntry, StoredFlowEntry>
        getFlowEntriesInternal(NetworkId networkId, DeviceId deviceId, FlowId flowId) {

            return getFlowTable(networkId, deviceId)
                    .computeIfAbsent(flowId, id -> Maps.newConcurrentMap());
        }

        private StoredFlowEntry getFlowEntryInternal(NetworkId networkId, FlowRule rule) {

            return getFlowEntriesInternal(networkId, rule.deviceId(), rule.id()).get(rule);
        }

        private Set<FlowEntry> getFlowEntriesInternal(NetworkId networkId, DeviceId deviceId) {

            return getFlowTable(networkId, deviceId).values().stream()
                    .flatMap(m -> m.values().stream())
                    .collect(Collectors.toSet());
        }

        public StoredFlowEntry getFlowEntry(NetworkId networkId, FlowRule rule) {
            return getFlowEntryInternal(networkId, rule);
        }

        public Set<FlowEntry> getFlowEntries(NetworkId networkId, DeviceId deviceId) {

            return getFlowEntriesInternal(networkId, deviceId);
        }

        public void add(NetworkId networkId, FlowEntry rule) {
            Map<DeviceId, Long> lastUpdateTimes = getLastUpdateTimesByNetwork(networkId);

            getFlowEntriesInternal(networkId, rule.deviceId(), rule.id())
                    .compute((StoredFlowEntry) rule, (k, stored) -> {
                        //TODO compare stored and rule timestamps
                        //TODO the key is not updated
                        return (StoredFlowEntry) rule;
                    });
            lastUpdateTimes.put(rule.deviceId(), System.currentTimeMillis());
        }

        public FlowEntry remove(NetworkId networkId, DeviceId deviceId, FlowEntry rule) {
            final AtomicReference<FlowEntry> removedRule = new AtomicReference<>();
            Map<DeviceId, Long> lastUpdateTimes = getLastUpdateTimesByNetwork(networkId);

            getFlowEntriesInternal(networkId, rule.deviceId(), rule.id())
                    .computeIfPresent((StoredFlowEntry) rule, (k, stored) -> {
                        if (rule instanceof DefaultFlowEntry) {
                            DefaultFlowEntry toRemove = (DefaultFlowEntry) rule;
                            if (stored instanceof DefaultFlowEntry) {
                                DefaultFlowEntry storedEntry = (DefaultFlowEntry) stored;
                                if (toRemove.created() < storedEntry.created()) {
                                    log.debug("Trying to remove more recent flow entry {} (stored: {})",
                                              toRemove, stored);
                                    // the key is not updated, removedRule remains null
                                    return stored;
                                }
                            }
                        }
                        removedRule.set(stored);
                        return null;
                    });

            if (removedRule.get() != null) {
                lastUpdateTimes.put(deviceId, System.currentTimeMillis());
                return removedRule.get();
            } else {
                return null;
            }
        }

        public void purgeFlowRule(NetworkId networkId, DeviceId deviceId) {
            Map<DeviceId, Map<FlowId, Map<StoredFlowEntry, StoredFlowEntry>>>
                    flowEntries = getFlowEntriesByNetwork(networkId);
            flowEntries.remove(deviceId);
        }

        public void purgeFlowRules(NetworkId networkId) {
            Map<DeviceId, Map<FlowId, Map<StoredFlowEntry, StoredFlowEntry>>>
                    flowEntries = getFlowEntriesByNetwork(networkId);
            flowEntries.clear();
        }
    }

    private class InternalTableStatsListener
            implements EventuallyConsistentMapListener<NetworkId, Map<DeviceId, List<TableStatisticsEntry>>> {

        @Override
        public void event(EventuallyConsistentMapEvent<NetworkId, Map<DeviceId,
                List<TableStatisticsEntry>>> event) {
            //TODO: Generate an event to listeners (do we need?)
        }
    }

    public final class MastershipBasedTimestamp implements Timestamp {

        private final long termNumber;
        private final long sequenceNumber;

        /**
         * Default constructor for serialization.
         */
        protected MastershipBasedTimestamp() {
            this.termNumber = -1;
            this.sequenceNumber = -1;
        }

        /**
         * Default version tuple.
         *
         * @param termNumber the mastership termNumber
         * @param sequenceNumber  the sequenceNumber number within the termNumber
         */
        public MastershipBasedTimestamp(long termNumber, long sequenceNumber) {
            this.termNumber = termNumber;
            this.sequenceNumber = sequenceNumber;
        }

        @Override
        public int compareTo(Timestamp o) {
            checkArgument(o instanceof MastershipBasedTimestamp,
                          "Must be MastershipBasedTimestamp", o);
            MastershipBasedTimestamp that = (MastershipBasedTimestamp) o;

            return ComparisonChain.start()
                    .compare(this.termNumber, that.termNumber)
                    .compare(this.sequenceNumber, that.sequenceNumber)
                    .result();
        }

        @Override
        public int hashCode() {
            return Objects.hash(termNumber, sequenceNumber);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof MastershipBasedTimestamp)) {
                return false;
            }
            MastershipBasedTimestamp that = (MastershipBasedTimestamp) obj;
            return Objects.equals(this.termNumber, that.termNumber) &&
                    Objects.equals(this.sequenceNumber, that.sequenceNumber);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                    .add("termNumber", termNumber)
                    .add("sequenceNumber", sequenceNumber)
                    .toString();
        }

        /**
         * Returns the termNumber.
         *
         * @return termNumber
         */
        public long termNumber() {
            return termNumber;
        }

        /**
         * Returns the sequenceNumber number.
         *
         * @return sequenceNumber
         */
        public long sequenceNumber() {
            return sequenceNumber;
        }
    }
}
