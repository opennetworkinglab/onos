/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.incubator.net.virtual.impl;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.IdGenerator;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.VirtualNetworkFlowRuleStore;
import org.onosproject.incubator.net.virtual.VirtualNetworkService;
import org.onosproject.incubator.net.virtual.event.AbstractVirtualListenerManager;
import org.onosproject.incubator.net.virtual.provider.AbstractVirtualProviderService;
import org.onosproject.incubator.net.virtual.provider.VirtualFlowRuleProvider;
import org.onosproject.incubator.net.virtual.provider.VirtualFlowRuleProviderService;
import org.onosproject.incubator.net.virtual.provider.VirtualProviderRegistryService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.CompletedBatchOperation;
import org.onosproject.net.flow.DefaultFlowEntry;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleBatchEntry;
import org.onosproject.net.flow.FlowRuleBatchEvent;
import org.onosproject.net.flow.FlowRuleBatchOperation;
import org.onosproject.net.flow.FlowRuleBatchRequest;
import org.onosproject.net.flow.FlowRuleEvent;
import org.onosproject.net.flow.FlowRuleListener;
import org.onosproject.net.flow.FlowRuleOperation;
import org.onosproject.net.flow.FlowRuleOperations;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.FlowRuleStoreDelegate;
import org.onosproject.net.flow.TableStatisticsEntry;
import org.onosproject.net.provider.ProviderId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.net.flow.FlowRuleEvent.Type.RULE_ADD_REQUESTED;
import static org.onosproject.net.flow.FlowRuleEvent.Type.RULE_REMOVE_REQUESTED;

/**
 * Flow rule service implementation built on the virtual network service.
 */
public class VirtualNetworkFlowRuleManager
        extends AbstractVirtualListenerManager<FlowRuleEvent, FlowRuleListener>
        implements FlowRuleService {

    private static final String VIRTUAL_FLOW_OP_TOPIC = "virtual-flow-ops-ids";
    private static final String THREAD_GROUP_NAME = "onos/virtual-flowservice";
    private static final String DEVICE_INSTALLER_PATTERN = "device-installer-%d";
    private static final String OPERATION_PATTERN = "operations-%d";
    public static final String FLOW_RULE_NULL = "FlowRule cannot be null";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final VirtualNetworkFlowRuleStore store;
    private final DeviceService deviceService;

    protected ExecutorService deviceInstallers =
            Executors.newFixedThreadPool(32,
                                         groupedThreads(THREAD_GROUP_NAME,
                                                        DEVICE_INSTALLER_PATTERN, log));
    protected ExecutorService operationsService =
            Executors.newFixedThreadPool(32,
                                         groupedThreads(THREAD_GROUP_NAME,
                                                        OPERATION_PATTERN, log));
    private IdGenerator idGenerator;

    private final Map<Long, FlowOperationsProcessor> pendingFlowOperations = new ConcurrentHashMap<>();

    private VirtualProviderRegistryService providerRegistryService = null;
    private InternalFlowRuleProviderService innerProviderService = null;

    private final FlowRuleStoreDelegate storeDelegate;

    /**
     * Creates a new VirtualNetworkFlowRuleService object.
     *
     * @param virtualNetworkManager virtual network manager service
     * @param networkId a virtual network identifier
     */
    public VirtualNetworkFlowRuleManager(VirtualNetworkService virtualNetworkManager,
                                         NetworkId networkId) {
        super(virtualNetworkManager, networkId, FlowRuleEvent.class);

        store = serviceDirectory.get(VirtualNetworkFlowRuleStore.class);

        idGenerator = serviceDirectory.get(CoreService.class)
                .getIdGenerator(VIRTUAL_FLOW_OP_TOPIC + networkId().toString());
        providerRegistryService =
                serviceDirectory.get(VirtualProviderRegistryService.class);
        innerProviderService = new InternalFlowRuleProviderService();
        providerRegistryService.registerProviderService(networkId(), innerProviderService);

        this.deviceService = manager.get(networkId, DeviceService.class);
        this.storeDelegate = new InternalStoreDelegate();
        store.setDelegate(networkId, this.storeDelegate);
    }

    @Override
    public int getFlowRuleCount() {
        return store.getFlowRuleCount(networkId());
    }

    @Override
    public Iterable<FlowEntry> getFlowEntries(DeviceId deviceId) {
        return store.getFlowEntries(networkId(), deviceId);
    }

    @Override
    public void applyFlowRules(FlowRule... flowRules) {
        FlowRuleOperations.Builder builder = FlowRuleOperations.builder();
        for (FlowRule flowRule : flowRules) {
            builder.add(flowRule);
        }
        apply(builder.build());
    }

    @Override
    public void purgeFlowRules(DeviceId deviceId) {
        store.purgeFlowRule(networkId(), deviceId);
    }

    @Override
    public void removeFlowRules(FlowRule... flowRules) {
        FlowRuleOperations.Builder builder = FlowRuleOperations.builder();
        for (FlowRule flowRule : flowRules) {
            builder.remove(flowRule);
        }
        apply(builder.build());
    }

    @Override
    public void removeFlowRulesById(ApplicationId id) {
        removeFlowRules(Iterables.toArray(getFlowRulesById(id), FlowRule.class));
    }

    @Override
    public Iterable<FlowRule> getFlowRulesById(ApplicationId id) {
        DeviceService deviceService = manager.get(networkId(), DeviceService.class);

        Set<FlowRule> flowEntries = Sets.newHashSet();
        for (Device d : deviceService.getDevices()) {
            for (FlowEntry flowEntry : store.getFlowEntries(networkId(), d.id())) {
                if (flowEntry.appId() == id.id()) {
                    flowEntries.add(flowEntry);
                }
            }
        }
        return flowEntries;
    }

    @Override
    public Iterable<FlowEntry> getFlowEntriesById(ApplicationId id) {
        DeviceService deviceService = manager.get(networkId(), DeviceService.class);

        Set<FlowEntry> flowEntries = Sets.newHashSet();
        for (Device d : deviceService.getDevices()) {
            for (FlowEntry flowEntry : store.getFlowEntries(networkId(), d.id())) {
                if (flowEntry.appId() == id.id()) {
                    flowEntries.add(flowEntry);
                }
            }
        }
        return flowEntries;
    }

    @Override
    public Iterable<FlowRule> getFlowRulesByGroupId(ApplicationId appId, short groupId) {
        DeviceService deviceService = manager.get(networkId(), DeviceService.class);

        Set<FlowRule> matches = Sets.newHashSet();
        long toLookUp = ((long) appId.id() << 16) | groupId;
        for (Device d : deviceService.getDevices()) {
            for (FlowEntry flowEntry : store.getFlowEntries(networkId(), d.id())) {
                if ((flowEntry.id().value() >>> 32) == toLookUp) {
                    matches.add(flowEntry);
                }
            }
        }
        return matches;
    }

    @Override
    public void apply(FlowRuleOperations ops) {
        operationsService.execute(new FlowOperationsProcessor(ops));
    }

    @Override
    public Iterable<TableStatisticsEntry> getFlowTableStatistics(DeviceId deviceId) {
        return store.getTableStatistics(networkId(), deviceId);
    }

    private static FlowRuleBatchEntry.FlowRuleOperation mapOperationType(FlowRuleOperation.Type input) {
        switch (input) {
            case ADD:
                return FlowRuleBatchEntry.FlowRuleOperation.ADD;
            case MODIFY:
                return FlowRuleBatchEntry.FlowRuleOperation.MODIFY;
            case REMOVE:
                return FlowRuleBatchEntry.FlowRuleOperation.REMOVE;
            default:
                throw new UnsupportedOperationException("Unknown flow rule type " + input);
        }
    }

    private class FlowOperationsProcessor implements Runnable {
        // Immutable
        private final FlowRuleOperations fops;

        // Mutable
        private final List<Set<FlowRuleOperation>> stages;
        private final Set<DeviceId> pendingDevices = new HashSet<>();
        private boolean hasFailed = false;

        FlowOperationsProcessor(FlowRuleOperations ops) {
            this.stages = Lists.newArrayList(ops.stages());
            this.fops = ops;
        }

        @Override
        public synchronized void run() {
            if (!stages.isEmpty()) {
                process(stages.remove(0));
            } else if (!hasFailed) {
                fops.callback().onSuccess(fops);
            }
        }

        private void process(Set<FlowRuleOperation> ops) {
            Multimap<DeviceId, FlowRuleBatchEntry> perDeviceBatches = ArrayListMultimap.create();

            for (FlowRuleOperation op : ops) {
                perDeviceBatches.put(op.rule().deviceId(),
                                     new FlowRuleBatchEntry(mapOperationType(op.type()), op.rule()));
            }
            pendingDevices.addAll(perDeviceBatches.keySet());

            for (DeviceId deviceId : perDeviceBatches.keySet()) {
                long id = idGenerator.getNewId();
                final FlowRuleBatchOperation b = new FlowRuleBatchOperation(perDeviceBatches.get(deviceId),
                                                                            deviceId, id);
                pendingFlowOperations.put(id, this);
                deviceInstallers.execute(() -> store.storeBatch(networkId(), b));
            }
        }

        synchronized void satisfy(DeviceId devId) {
            pendingDevices.remove(devId);
            if (pendingDevices.isEmpty()) {
                operationsService.execute(this);
            }
        }

        synchronized void fail(DeviceId devId, Set<? extends FlowRule> failures) {
            hasFailed = true;
            pendingDevices.remove(devId);
            if (pendingDevices.isEmpty()) {
                operationsService.execute(this);
            }

            FlowRuleOperations.Builder failedOpsBuilder = FlowRuleOperations.builder();
            failures.forEach(failedOpsBuilder::add);

            fops.callback().onError(failedOpsBuilder.build());
        }
    }

    private final class InternalFlowRuleProviderService
            extends AbstractVirtualProviderService<VirtualFlowRuleProvider>
            implements VirtualFlowRuleProviderService {

        final Map<FlowEntry, Long> firstSeen = Maps.newConcurrentMap();
        final Map<FlowEntry, Long> lastSeen = Maps.newConcurrentMap();

        private InternalFlowRuleProviderService() {
            //TODO: find a proper virtual provider.
            Set<ProviderId> providerIds =
                    providerRegistryService.getProvidersByService(this);
            ProviderId providerId = providerIds.stream().findFirst().get();
            VirtualFlowRuleProvider provider = (VirtualFlowRuleProvider)
                    providerRegistryService.getProvider(providerId);
            setProvider(provider);
        }

        @Override
        public void flowRemoved(FlowEntry flowEntry) {
            checkNotNull(flowEntry, FLOW_RULE_NULL);
            checkValidity();

            lastSeen.remove(flowEntry);
            firstSeen.remove(flowEntry);
            FlowEntry stored = store.getFlowEntry(networkId(), flowEntry);
            if (stored == null) {
                log.debug("Rule already evicted from store: {}", flowEntry);
                return;
            }
            if (flowEntry.reason() == FlowEntry.FlowRemoveReason.HARD_TIMEOUT) {
                ((DefaultFlowEntry) stored).setState(FlowEntry.FlowEntryState.REMOVED);
            }

            //FIXME: obtains provider from devices providerId()
            FlowRuleEvent event = null;
            switch (stored.state()) {
                case ADDED:
                case PENDING_ADD:
                    provider().applyFlowRule(networkId(), stored);
                    break;
                case PENDING_REMOVE:
                case REMOVED:
                    event = store.removeFlowRule(networkId(), stored);
                    break;
                default:
                    break;

            }
            if (event != null) {
                log.debug("Flow {} removed", flowEntry);
                post(event);
            }
        }

        private void flowMissing(FlowEntry flowRule) {
            checkNotNull(flowRule, FLOW_RULE_NULL);
            checkValidity();

            FlowRuleEvent event = null;
            switch (flowRule.state()) {
                case PENDING_REMOVE:
                case REMOVED:
                    event = store.removeFlowRule(networkId(), flowRule);
                    break;
                case ADDED:
                case PENDING_ADD:
                    event = store.pendingFlowRule(networkId(), flowRule);

                    try {
                        provider().applyFlowRule(networkId(), flowRule);
                    } catch (UnsupportedOperationException e) {
                        log.warn(e.getMessage());
                        if (flowRule instanceof DefaultFlowEntry) {
                            //FIXME modification of "stored" flow entry outside of store
                            ((DefaultFlowEntry) flowRule).setState(FlowEntry.FlowEntryState.FAILED);
                        }
                    }
                    break;
                default:
                    log.debug("Flow {} has not been installed.", flowRule);
            }

            if (event != null) {
                log.debug("Flow {} removed", flowRule);
                post(event);
            }
        }

        private void extraneousFlow(FlowRule flowRule) {
            checkNotNull(flowRule, FLOW_RULE_NULL);
            checkValidity();

            provider().removeFlowRule(networkId(), flowRule);
            log.debug("Flow {} is on switch but not in store.", flowRule);
        }

        private void flowAdded(FlowEntry flowEntry) {
            checkNotNull(flowEntry, FLOW_RULE_NULL);

            if (checkRuleLiveness(flowEntry, store.getFlowEntry(networkId(), flowEntry))) {
                FlowRuleEvent event = store.addOrUpdateFlowRule(networkId(), flowEntry);
                if (event == null) {
                    log.debug("No flow store event generated.");
                } else {
                    log.trace("Flow {} {}", flowEntry, event.type());
                    post(event);
                }
            } else {
                log.debug("Removing flow rules....");
                removeFlowRules(flowEntry);
            }
        }

        private boolean checkRuleLiveness(FlowEntry swRule, FlowEntry storedRule) {
            if (storedRule == null) {
                return false;
            }
            if (storedRule.isPermanent()) {
                return true;
            }

            final long timeout = storedRule.timeout() * 1000;
            final long currentTime = System.currentTimeMillis();

            // Checking flow with hardTimeout
            if (storedRule.hardTimeout() != 0) {
                if (!firstSeen.containsKey(storedRule)) {
                    // First time rule adding
                    firstSeen.put(storedRule, currentTime);
                } else {
                    Long first = firstSeen.get(storedRule);
                    final long hardTimeout = storedRule.hardTimeout() * 1000;
                    if ((currentTime - first) > hardTimeout) {
                        return false;
                    }
                }
            }

            if (storedRule.packets() != swRule.packets()) {
                lastSeen.put(storedRule, currentTime);
                return true;
            }
            if (!lastSeen.containsKey(storedRule)) {
                // checking for the first time
                lastSeen.put(storedRule, storedRule.lastSeen());
                // Use following if lastSeen attr. was removed.
                //lastSeen.put(storedRule, currentTime);
            }
            Long last = lastSeen.get(storedRule);

            // concurrently removed? let the liveness check fail
            return last != null && (currentTime - last) <= timeout;
        }

        @Override
        public void pushFlowMetrics(DeviceId deviceId, Iterable<FlowEntry> flowEntries) {
            pushFlowMetricsInternal(deviceId, flowEntries, true);
        }

        @Override
        public void pushFlowMetricsWithoutFlowMissing(DeviceId deviceId, Iterable<FlowEntry> flowEntries) {
            pushFlowMetricsInternal(deviceId, flowEntries, false);
        }

        private void pushFlowMetricsInternal(DeviceId deviceId, Iterable<FlowEntry> flowEntries,
                                             boolean useMissingFlow) {
            Map<FlowEntry, FlowEntry> storedRules = Maps.newHashMap();
            store.getFlowEntries(networkId(), deviceId).forEach(f -> storedRules.put(f, f));

            for (FlowEntry rule : flowEntries) {
                try {
                    FlowEntry storedRule = storedRules.remove(rule);
                    if (storedRule != null) {
                        if (storedRule.id().equals(rule.id())) {
                            // we both have the rule, let's update some info then.
                            flowAdded(rule);
                        } else {
                            // the two rules are not an exact match - remove the
                            // switch's rule and install our rule
                            extraneousFlow(rule);
                            flowMissing(storedRule);
                        }
                    }
                } catch (Exception e) {
                    log.debug("Can't process added or extra rule {}", e.getMessage());
                }
            }

            // DO NOT reinstall
            if (useMissingFlow) {
                for (FlowEntry rule : storedRules.keySet()) {
                    try {
                        // there are rules in the store that aren't on the switch
                        log.debug("Adding rule in store, but not on switch {}", rule);
                        flowMissing(rule);
                    } catch (Exception e) {
                        log.debug("Can't add missing flow rule:", e);
                    }
                }
            }
        }

        public void batchOperationCompleted(long batchId, CompletedBatchOperation operation) {
            store.batchOperationComplete(networkId(), FlowRuleBatchEvent.completed(
                    new FlowRuleBatchRequest(batchId, Collections.emptySet()),
                    operation
            ));
        }

        @Override
        public void pushTableStatistics(DeviceId deviceId,
                                        List<TableStatisticsEntry> tableStats) {
            store.updateTableStatistics(networkId(), deviceId, tableStats);
        }
    }

    // Store delegate to re-post events emitted from the store.
    private class InternalStoreDelegate implements FlowRuleStoreDelegate {

        // TODO: Right now we only dispatch events at individual flowEntry level.
        // It may be more efficient for also dispatch events as a batch.
        @Override
        public void notify(FlowRuleBatchEvent event) {
            final FlowRuleBatchRequest request = event.subject();
            switch (event.type()) {
                case BATCH_OPERATION_REQUESTED:
                    // Request has been forwarded to MASTER Node, and was
                    request.ops().forEach(
                            op -> {
                                switch (op.operator()) {
                                    case ADD:
                                        post(new FlowRuleEvent(RULE_ADD_REQUESTED, op.target()));
                                        break;
                                    case REMOVE:
                                        post(new FlowRuleEvent(RULE_REMOVE_REQUESTED, op.target()));
                                        break;
                                    case MODIFY:
                                        //TODO: do something here when the time comes.
                                        break;
                                    default:
                                        log.warn("Unknown flow operation operator: {}", op.operator());
                                }
                            }
                    );

                    DeviceId deviceId = event.deviceId();
                    FlowRuleBatchOperation batchOperation = request.asBatchOperation(deviceId);

                    VirtualFlowRuleProvider provider = innerProviderService.provider();
                    if (provider != null) {
                        provider.executeBatch(networkId, batchOperation);
                    }

                    break;

                case BATCH_OPERATION_COMPLETED:
                    FlowOperationsProcessor fops = pendingFlowOperations.remove(
                            event.subject().batchId());
                    if (fops == null) {
                       return;
                    }

                    if (event.result().isSuccess()) {
                            fops.satisfy(event.deviceId());
                    } else {
                        fops.fail(event.deviceId(), event.result().failedItems());
                    }
                    break;

                default:
                    break;
            }
        }
    }
}
