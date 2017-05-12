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

package org.onosproject.incubator.store.virtual.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.SettableFuture;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.Tools;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.VirtualNetworkFlowRuleStore;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.CompletedBatchOperation;
import org.onosproject.net.flow.DefaultFlowEntry;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowId;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleBatchEntry;
import org.onosproject.net.flow.FlowRuleBatchEvent;
import org.onosproject.net.flow.FlowRuleBatchOperation;
import org.onosproject.net.flow.FlowRuleBatchRequest;
import org.onosproject.net.flow.FlowRuleEvent;
import org.onosproject.net.flow.FlowRuleStoreDelegate;
import org.onosproject.net.flow.StoredFlowEntry;
import org.onosproject.net.flow.TableStatisticsEntry;
import org.onosproject.store.service.StorageService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.onosproject.net.flow.FlowRuleEvent.Type.RULE_REMOVED;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of the virtual network flow rule store to manage inventory of
 * virtual flow rules using trivial in-memory implementation.
 */
//TODO: support distributed flowrule store for virtual networks

@Component(immediate = true)
@Service
public class SimpleVirtualFlowRuleStore
        extends AbstractVirtualStore<FlowRuleBatchEvent, FlowRuleStoreDelegate>
        implements VirtualNetworkFlowRuleStore {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    private final ConcurrentMap<NetworkId,
            ConcurrentMap<DeviceId, ConcurrentMap<FlowId, List<StoredFlowEntry>>>>
            flowEntries = new ConcurrentHashMap<>();


    private final AtomicInteger localBatchIdGen = new AtomicInteger();

    private static final int DEFAULT_PENDING_FUTURE_TIMEOUT_MINUTES = 5;
    @Property(name = "pendingFutureTimeoutMinutes", intValue = DEFAULT_PENDING_FUTURE_TIMEOUT_MINUTES,
            label = "Expiration time after an entry is created that it should be automatically removed")
    private int pendingFutureTimeoutMinutes = DEFAULT_PENDING_FUTURE_TIMEOUT_MINUTES;

    private Cache<Integer, SettableFuture<CompletedBatchOperation>> pendingFutures =
            CacheBuilder.newBuilder()
                    .expireAfterWrite(pendingFutureTimeoutMinutes, TimeUnit.MINUTES)
                    .removalListener(new TimeoutFuture())
                    .build();

    @Activate
    public void activate() {
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        flowEntries.clear();
        log.info("Stopped");
    }

    @Modified
    public void modified(ComponentContext context) {

        readComponentConfiguration(context);

        // Reset Cache and copy all.
        Cache<Integer, SettableFuture<CompletedBatchOperation>> prevFutures = pendingFutures;
        pendingFutures = CacheBuilder.newBuilder()
                .expireAfterWrite(pendingFutureTimeoutMinutes, TimeUnit.MINUTES)
                .removalListener(new TimeoutFuture())
                .build();

        pendingFutures.putAll(prevFutures.asMap());
    }

    /**
     * Extracts properties from the component configuration context.
     *
     * @param context the component context
     */
    private void readComponentConfiguration(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();

        Integer newPendingFutureTimeoutMinutes =
                Tools.getIntegerProperty(properties, "pendingFutureTimeoutMinutes");
        if (newPendingFutureTimeoutMinutes == null) {
            pendingFutureTimeoutMinutes = DEFAULT_PENDING_FUTURE_TIMEOUT_MINUTES;
            log.info("Pending future timeout is not configured, " +
                             "using current value of {}", pendingFutureTimeoutMinutes);
        } else {
            pendingFutureTimeoutMinutes = newPendingFutureTimeoutMinutes;
            log.info("Configured. Pending future timeout is configured to {}",
                     pendingFutureTimeoutMinutes);
        }
    }

    @Override
    public int getFlowRuleCount(NetworkId networkId) {
        int sum = 0;

        if (flowEntries.get(networkId) == null) {
            return 0;
        }

        for (ConcurrentMap<FlowId, List<StoredFlowEntry>> ft :
                flowEntries.get(networkId).values()) {
            for (List<StoredFlowEntry> fes : ft.values()) {
                sum += fes.size();
            }
        }
        return sum;
    }

    @Override
    public FlowEntry getFlowEntry(NetworkId networkId, FlowRule rule) {
        return getFlowEntryInternal(networkId, rule.deviceId(), rule);
    }

    @Override
    public Iterable<FlowEntry> getFlowEntries(NetworkId networkId, DeviceId deviceId) {
        return FluentIterable.from(getFlowTable(networkId, deviceId).values())
                .transformAndConcat(Collections::unmodifiableList);
    }

    private void storeFlowRule(NetworkId networkId, FlowRule rule) {
        storeFlowRuleInternal(networkId, rule);
    }

    @Override
    public void storeBatch(NetworkId networkId, FlowRuleBatchOperation batchOperation) {
        List<FlowRuleBatchEntry> toAdd = new ArrayList<>();
        List<FlowRuleBatchEntry> toRemove = new ArrayList<>();

        for (FlowRuleBatchEntry entry : batchOperation.getOperations()) {
            final FlowRule flowRule = entry.target();
            if (entry.operator().equals(FlowRuleBatchEntry.FlowRuleOperation.ADD)) {
                if (!getFlowEntries(networkId, flowRule.deviceId(),
                                    flowRule.id()).contains(flowRule)) {
                    storeFlowRule(networkId, flowRule);
                    toAdd.add(entry);
                }
            } else if (entry.operator().equals(FlowRuleBatchEntry.FlowRuleOperation.REMOVE)) {
                if (getFlowEntries(networkId, flowRule.deviceId(), flowRule.id()).contains(flowRule)) {
                    deleteFlowRule(networkId, flowRule);
                    toRemove.add(entry);
                }
            } else {
                throw new UnsupportedOperationException("Unsupported operation type");
            }
        }

        if (toAdd.isEmpty() && toRemove.isEmpty()) {
            notifyDelegate(networkId, FlowRuleBatchEvent.completed(
                    new FlowRuleBatchRequest(batchOperation.id(), Collections.emptySet()),
                    new CompletedBatchOperation(true, Collections.emptySet(),
                                                batchOperation.deviceId())));
            return;
        }

        SettableFuture<CompletedBatchOperation> r = SettableFuture.create();
        final int futureId = localBatchIdGen.incrementAndGet();

        pendingFutures.put(futureId, r);

        toAdd.addAll(toRemove);
        notifyDelegate(networkId, FlowRuleBatchEvent.requested(
                new FlowRuleBatchRequest(batchOperation.id(),
                                         Sets.newHashSet(toAdd)), batchOperation.deviceId()));

    }

    @Override
    public void batchOperationComplete(NetworkId networkId, FlowRuleBatchEvent event) {
        final Long batchId = event.subject().batchId();
        SettableFuture<CompletedBatchOperation> future
                = pendingFutures.getIfPresent(batchId);
        if (future != null) {
            future.set(event.result());
            pendingFutures.invalidate(batchId);
        }
        notifyDelegate(networkId, event);
    }

    @Override
    public void deleteFlowRule(NetworkId networkId, FlowRule rule) {
        List<StoredFlowEntry> entries = getFlowEntries(networkId, rule.deviceId(), rule.id());

        synchronized (entries) {
            for (StoredFlowEntry entry : entries) {
                if (entry.equals(rule)) {
                    synchronized (entry) {
                        entry.setState(FlowEntry.FlowEntryState.PENDING_REMOVE);
                    }
                }
            }
        }
    }

    @Override
    public FlowRuleEvent addOrUpdateFlowRule(NetworkId networkId, FlowEntry rule) {
        // check if this new rule is an update to an existing entry
        List<StoredFlowEntry> entries = getFlowEntries(networkId, rule.deviceId(), rule.id());
        synchronized (entries) {
            for (StoredFlowEntry stored : entries) {
                if (stored.equals(rule)) {
                    synchronized (stored) {
                        //FIXME modification of "stored" flow entry outside of flow table
                        stored.setBytes(rule.bytes());
                        stored.setLife(rule.life());
                        stored.setPackets(rule.packets());
                        if (stored.state() == FlowEntry.FlowEntryState.PENDING_ADD) {
                            stored.setState(FlowEntry.FlowEntryState.ADDED);
                            // TODO: Do we need to change `rule` state?
                            return new FlowRuleEvent(FlowRuleEvent.Type.RULE_ADDED, rule);
                        }
                        return new FlowRuleEvent(FlowRuleEvent.Type.RULE_UPDATED, rule);
                    }
                }
            }
        }

        // should not reach here
        // storeFlowRule was expected to be called
        log.error("FlowRule was not found in store {} to update", rule);

        return null;
    }

    @Override
    public FlowRuleEvent removeFlowRule(NetworkId networkId, FlowEntry rule) {
        // This is where one could mark a rule as removed and still keep it in the store.
        final DeviceId did = rule.deviceId();

        List<StoredFlowEntry> entries = getFlowEntries(networkId, did, rule.id());
        synchronized (entries) {
            if (entries.remove(rule)) {
                return new FlowRuleEvent(RULE_REMOVED, rule);
            }
        }
        return null;
    }

    @Override
    public FlowRuleEvent pendingFlowRule(NetworkId networkId, FlowEntry rule) {
        List<StoredFlowEntry> entries = getFlowEntries(networkId, rule.deviceId(), rule.id());
        synchronized (entries) {
            for (StoredFlowEntry entry : entries) {
                if (entry.equals(rule) &&
                        entry.state() != FlowEntry.FlowEntryState.PENDING_ADD) {
                    synchronized (entry) {
                        entry.setState(FlowEntry.FlowEntryState.PENDING_ADD);
                        return new FlowRuleEvent(FlowRuleEvent.Type.RULE_UPDATED, rule);
                    }
                }
            }
        }
        return null;
    }

    @Override
    public void purgeFlowRule(NetworkId networkId, DeviceId deviceId) {
        flowEntries.get(networkId).remove(deviceId);
    }

    @Override
    public void purgeFlowRules(NetworkId networkId) {
        flowEntries.get(networkId).clear();
    }

    @Override
    public FlowRuleEvent
    updateTableStatistics(NetworkId networkId, DeviceId deviceId, List<TableStatisticsEntry> tableStats) {
        //TODO: Table operations are not supported yet
        return null;
    }

    @Override
    public Iterable<TableStatisticsEntry>
    getTableStatistics(NetworkId networkId, DeviceId deviceId) {
        //TODO: Table operations are not supported yet
        return null;
    }

    /**
     * Returns the flow table for specified device.
     *
     * @param networkId identifier of the virtual network
     * @param deviceId identifier of the virtual device
     * @return Map representing Flow Table of given device.
     */
    private ConcurrentMap<FlowId, List<StoredFlowEntry>>
    getFlowTable(NetworkId networkId, DeviceId deviceId) {
        return flowEntries
                .computeIfAbsent(networkId, n -> new ConcurrentHashMap<>())
                .computeIfAbsent(deviceId, k -> new ConcurrentHashMap<>());
    }

    private List<StoredFlowEntry>
    getFlowEntries(NetworkId networkId, DeviceId deviceId, FlowId flowId) {
        final ConcurrentMap<FlowId, List<StoredFlowEntry>> flowTable
                = getFlowTable(networkId, deviceId);

        List<StoredFlowEntry> r = flowTable.get(flowId);
        if (r == null) {
            final List<StoredFlowEntry> concurrentlyAdded;
            r = new CopyOnWriteArrayList<>();
            concurrentlyAdded = flowTable.putIfAbsent(flowId, r);
            if (concurrentlyAdded != null) {
                return concurrentlyAdded;
            }
        }
        return r;
    }

    private FlowEntry
    getFlowEntryInternal(NetworkId networkId, DeviceId deviceId, FlowRule rule) {
        List<StoredFlowEntry> fes = getFlowEntries(networkId, deviceId, rule.id());
        for (StoredFlowEntry fe : fes) {
            if (fe.equals(rule)) {
                return fe;
            }
        }
        return null;
    }

    private void storeFlowRuleInternal(NetworkId networkId, FlowRule rule) {
        StoredFlowEntry f = new DefaultFlowEntry(rule);
        final DeviceId did = f.deviceId();
        final FlowId fid = f.id();
        List<StoredFlowEntry> existing = getFlowEntries(networkId, did, fid);
        synchronized (existing) {
            for (StoredFlowEntry fe : existing) {
                if (fe.equals(rule)) {
                    // was already there? ignore
                    return;
                }
            }
            // new flow rule added
            existing.add(f);
        }
    }

    private static final class TimeoutFuture
            implements RemovalListener<Integer, SettableFuture<CompletedBatchOperation>> {
        @Override
        public void onRemoval(RemovalNotification<Integer,
                SettableFuture<CompletedBatchOperation>> notification) {
            // wrapping in ExecutionException to support Future.get
            if (notification.wasEvicted()) {
                notification.getValue()
                        .setException(new ExecutionException("Timed out",
                                                             new TimeoutException()));
            }
        }
    }
}
