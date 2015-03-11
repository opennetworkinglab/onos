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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.KryoNamespace;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.CoreService;
import org.onosproject.core.IdGenerator;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceClockService;
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
import org.onosproject.store.AbstractStore;
import org.onosproject.store.cluster.messaging.ClusterCommunicationService;
import org.onosproject.store.cluster.messaging.ClusterMessage;
import org.onosproject.store.cluster.messaging.ClusterMessageHandler;
import org.onosproject.store.ecmap.EventuallyConsistentMap;
import org.onosproject.store.ecmap.EventuallyConsistentMapImpl;
import org.onosproject.store.flow.ReplicaInfo;
import org.onosproject.store.flow.ReplicaInfoEvent;
import org.onosproject.store.flow.ReplicaInfoEventListener;
import org.onosproject.store.flow.ReplicaInfoService;
import org.onosproject.store.impl.ClockService;
import org.onosproject.store.impl.MastershipBasedTimestamp;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.serializers.KryoSerializer;
import org.onosproject.store.serializers.StoreSerializer;
import org.onosproject.store.serializers.impl.DistributedStoreSerializers;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
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

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.apache.felix.scr.annotations.ReferenceCardinality.MANDATORY_UNARY;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.net.flow.FlowRuleEvent.Type.RULE_REMOVED;
import static org.onosproject.store.flow.impl.FlowStoreMessageSubjects.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Manages inventory of flow rules using a distributed state management protocol.
 */
@Component(immediate = true)
@Service
public class DistributedFlowRuleStore
        extends AbstractStore<FlowRuleBatchEvent, FlowRuleStoreDelegate>
        implements FlowRuleStore {

    private final Logger log = getLogger(getClass());

    private static final int MESSAGE_HANDLER_THREAD_POOL_SIZE = 8;
    private static final boolean DEFAULT_BACKUP_ENABLED = false;
    private static final long FLOW_RULE_STORE_TIMEOUT_MILLIS = 5000;

    @Property(name = "msgHandlerPoolSize", intValue = MESSAGE_HANDLER_THREAD_POOL_SIZE,
            label = "Number of threads in the message handler pool")
    private int msgHandlerPoolSize = MESSAGE_HANDLER_THREAD_POOL_SIZE;

    @Property(name = "backupEnabled", boolValue = DEFAULT_BACKUP_ENABLED,
            label = "Indicates whether backups are enabled or not")
    private boolean backupEnabled = DEFAULT_BACKUP_ENABLED;

    private InternalFlowTable flowTable;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ReplicaInfoService replicaInfoManager;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterCommunicationService clusterCommunicator;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceClockService deviceClockService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = MANDATORY_UNARY)
    protected ComponentConfigService configService;

    private Map<Long, NodeId> pendingResponses = Maps.newConcurrentMap();

    private ExecutorService messageHandlingExecutor;

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

    private ReplicaInfoEventListener replicaInfoEventListener;

    private IdGenerator idGenerator;

    @Activate
    public void activate(ComponentContext context) {
        configService.registerProperties(getClass());

        flowTable = new InternalFlowTable().withBackupsEnabled(backupEnabled);

        idGenerator = coreService.getIdGenerator(FlowRuleService.FLOW_OP_TOPIC);

        final NodeId local = clusterService.getLocalNode().id();

        messageHandlingExecutor = Executors.newFixedThreadPool(
                msgHandlerPoolSize, groupedThreads("onos/store/flow", "message-handlers"));

        clusterCommunicator.addSubscriber(APPLY_BATCH_FLOWS, new OnStoreBatch(local), messageHandlingExecutor);

        clusterCommunicator.addSubscriber(REMOTE_APPLY_COMPLETED, new ClusterMessageHandler() {
            @Override
            public void handle(ClusterMessage message) {
                FlowRuleBatchEvent event = SERIALIZER.decode(message.payload());
                log.trace("received completed notification for {}", event);
                notifyDelegate(event);
            }
        }, messageHandlingExecutor);

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
        }, messageHandlingExecutor);

        clusterCommunicator.addSubscriber(GET_DEVICE_FLOW_ENTRIES, new ClusterMessageHandler() {

            @Override
            public void handle(ClusterMessage message) {
                DeviceId deviceId = SERIALIZER.decode(message.payload());
                log.trace("Received get flow entries request for {} from {}", deviceId, message.sender());
                Set<StoredFlowEntry> flowEntries = flowTable.getFlowEntries(deviceId);
                try {
                    message.respond(SERIALIZER.encode(flowEntries));
                } catch (IOException e) {
                    log.error("Failed to respond to peer's getFlowEntries request", e);
                }
            }
        }, messageHandlingExecutor);

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
        }, messageHandlingExecutor);

        replicaInfoEventListener = new InternalReplicaInfoEventListener();

        replicaInfoManager.addListener(replicaInfoEventListener);

        logConfig("Started");
    }

    @Deactivate
    public void deactivate(ComponentContext context) {
        configService.unregisterProperties(getClass(), false);
        clusterCommunicator.removeSubscriber(REMOVE_FLOW_ENTRY);
        clusterCommunicator.removeSubscriber(GET_DEVICE_FLOW_ENTRIES);
        clusterCommunicator.removeSubscriber(GET_FLOW_ENTRY);
        clusterCommunicator.removeSubscriber(APPLY_BATCH_FLOWS);
        clusterCommunicator.removeSubscriber(REMOTE_APPLY_COMPLETED);
        messageHandlingExecutor.shutdown();
        replicaInfoManager.removeListener(replicaInfoEventListener);
        log.info("Stopped");
    }

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
        try {
            String s = (String) properties.get("msgHandlerPoolSize");
            newPoolSize = isNullOrEmpty(s) ? msgHandlerPoolSize : Integer.parseInt(s.trim());

            s = (String) properties.get("backupEnabled");
            newBackupEnabled = isNullOrEmpty(s) ? backupEnabled : Boolean.parseBoolean(s.trim());

        } catch (NumberFormatException | ClassCastException e) {
            newPoolSize = MESSAGE_HANDLER_THREAD_POOL_SIZE;
            newBackupEnabled = DEFAULT_BACKUP_ENABLED;
        }

        if (newPoolSize != msgHandlerPoolSize || newBackupEnabled != backupEnabled) {
            msgHandlerPoolSize = newPoolSize;
            backupEnabled = newBackupEnabled;
            // reconfigure the store
            flowTable.withBackupsEnabled(backupEnabled);
            ExecutorService oldMsgHandler = messageHandlingExecutor;
            messageHandlingExecutor = Executors.newFixedThreadPool(
                    msgHandlerPoolSize, groupedThreads("onos/store/flow", "message-handlers"));
            oldMsgHandler.shutdown();
            logConfig("Reconfigured");
        }
    }

    private void logConfig(String prefix) {
        log.info("{} with msgHandlerPoolSize = {}; backupEnabled = {}",
                 prefix, msgHandlerPoolSize, backupEnabled);
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
            return flowTable.getFlowEntries(deviceId).stream().collect(Collectors.toSet());
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

        DeviceId deviceId = operation.deviceId();

        ReplicaInfo replicaInfo = replicaInfoManager.getReplicaInfoFor(deviceId);

        if (!replicaInfo.master().isPresent()) {
            log.warn("No master for {} : flows will be marked for removal", deviceId);

            updateStoreInternal(operation);

            notifyDelegate(FlowRuleBatchEvent.completed(
                    new FlowRuleBatchRequest(operation.id(), Collections.emptySet()),
                    new CompletedBatchOperation(true, Collections.emptySet(), operation.deviceId())));
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


        if (!clusterCommunicator.unicast(message, replicaInfo.master().get())) {
            log.warn("Failed to storeBatch: {} to {}", message, replicaInfo.master());

            Set<FlowRule> allFailures = operation.getOperations().stream()
                    .map(op -> op.target())
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
            return addOrUpdateFlowRuleInternal((StoredFlowEntry) rule);
        }

        log.warn("Tried to update FlowRule {} state,"
                         + " while the Node was not the master.", rule);
        return null;
    }

    private FlowRuleEvent addOrUpdateFlowRuleInternal(StoredFlowEntry rule) {
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
            ClusterMessage message = new ClusterMessage(
                    clusterService.getLocalNode().id(),
                    REMOTE_APPLY_COMPLETED,
                    SERIALIZER.encode(event));
            // TODO check unicast return value
            clusterCommunicator.unicast(message, nodeId);
            //error log: log.warn("Failed to respond to peer for batch operation result");
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

                Set<FlowRule> failures = operation.getOperations()
                            .stream()
                            .map(FlowRuleBatchEntry::target)
                            .collect(Collectors.toSet());

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
                        log.info("{} is now the master for {}. Will load flow rules from backup", local, did);
                        // This node is the new master, populate local structure
                        // from backup
                        flowTable.loadFromBackup(did);
                    }
                    //else {
                        // This node is no longer the master holder,
                        // clean local structure
                        //removeFromPrimary(did);
                        // TODO: probably should stop pending backup activities in
                        // executors to avoid overwriting with old value
                    //}
                    break;
                default:
                    break;

            }
        }
    }

    private class InternalFlowTable {

        private boolean backupsEnabled = true;

        /**
         * Turns backups on or off.
         * @param backupsEnabled whether backups should be enabled or not
         * @return this instance
         */
        public InternalFlowTable withBackupsEnabled(boolean backupsEnabled) {
            this.backupsEnabled = backupsEnabled;
            return this;
        }

        private final Map<DeviceId, Map<FlowId, Set<StoredFlowEntry>>>
                flowEntries = Maps.newConcurrentMap();

        private final KryoNamespace.Builder flowSerializer = KryoNamespace.newBuilder()
                .register(KryoNamespaces.API)
                .register(MastershipBasedTimestamp.class);

        private final ClockService<FlowId, StoredFlowEntry> clockService =
                (flowId, flowEntry) ->
                        (flowEntry == null) ? null : deviceClockService.getTimestamp(flowEntry.deviceId());

        private final EventuallyConsistentMap<FlowId, StoredFlowEntry> backupMap =
                new EventuallyConsistentMapImpl<>("flow-backup",
                        clusterService,
                        clusterCommunicator,
                        flowSerializer,
                        clockService,
                        (key, flowEntry) -> getPeerNodes()).withTombstonesDisabled(true);

        private Collection<NodeId> getPeerNodes() {
            List<NodeId> nodes = clusterService.getNodes()
                                    .stream()
                                    .map(node -> node.id())
                                    .filter(id -> !id.equals(clusterService.getLocalNode().id()))
                                    .collect(Collectors.toList());

            if (nodes.isEmpty()) {
                return ImmutableList.of();
            } else {
                // get a random peer
                return ImmutableList.of(nodes.get(RandomUtils.nextInt(nodes.size())));
            }
        }

        public void loadFromBackup(DeviceId deviceId) {
            if (!backupsEnabled) {
                return;
            }

            ConcurrentMap<FlowId, Set<StoredFlowEntry>> flowTable = new ConcurrentHashMap<>();

            backupMap.values()
                .stream()
                .filter(entry -> entry.deviceId().equals(deviceId))
                .forEach(entry -> flowTable.computeIfPresent(entry.id(), (k, v) -> {
                    if (v == null) {
                        return Sets.newHashSet(entry);
                    } else {
                        v.add(entry);
                    }
                    return v;
                }));
            flowEntries.putIfAbsent(deviceId, flowTable);
        }

        private Set<StoredFlowEntry> getFlowEntriesInternal(DeviceId deviceId, FlowId flowId) {
            return flowEntries
                        .computeIfAbsent(deviceId, key -> Maps.newConcurrentMap())
                        .computeIfAbsent(flowId, k -> new CopyOnWriteArraySet<>());
        }

        private StoredFlowEntry getFlowEntryInternal(FlowRule rule) {
            return getFlowEntriesInternal(rule.deviceId(), rule.id())
                .stream()
                .filter(element -> element.equals(rule))
                .findFirst()
                .orElse(null);
        }

        private Set<StoredFlowEntry> getFlowEntriesInternal(DeviceId deviceId) {
            Set<StoredFlowEntry> entries = Sets.newHashSet();
            flowEntries.computeIfAbsent(deviceId, key -> Maps.newConcurrentMap())
                        .values()
                        .forEach(entries::addAll);
            return entries;
        }

        public StoredFlowEntry getFlowEntry(FlowRule rule) {
            return getFlowEntryInternal(rule);
        }

        public Set<StoredFlowEntry> getFlowEntries(DeviceId deviceId) {
            return getFlowEntriesInternal(deviceId);
        }

        public void add(StoredFlowEntry rule) {
            getFlowEntriesInternal(rule.deviceId(), rule.id()).add(rule);
            if (backupsEnabled) {
                try {
                    backupMap.put(rule.id(), rule);
                } catch (Exception e) {
                    log.warn("Failed to backup flow rule", e);
                }
            }
        }

        public boolean remove(DeviceId deviceId, FlowEntry rule) {
            boolean status =
                    getFlowEntriesInternal(deviceId, rule.id()).remove(rule);
            if (backupsEnabled && status) {
                try {
                    backupMap.remove(rule.id(), (DefaultFlowEntry) rule);
                } catch (Exception e) {
                    log.warn("Failed to remove backup of flow rule", e);
                }
            }
            return status;
        }

        public void clearDevice(DeviceId did) {
            flowEntries.remove(did);
            // Flow entries should continue to remain in backup map.
        }
    }
}
