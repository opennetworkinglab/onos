/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.store.trivial;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.SettableFuture;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.NewConcurrentHashMap;
import org.onlab.util.Tools;
import org.onosproject.net.DeviceId;
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
import org.onosproject.net.flow.FlowRuleStore;
import org.onosproject.net.flow.FlowRuleStoreDelegate;
import org.onosproject.net.flow.StoredFlowEntry;
import org.onosproject.net.flow.TableStatisticsEntry;
import org.onosproject.store.AbstractStore;
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

import static org.apache.commons.lang3.concurrent.ConcurrentUtils.createIfAbsentUnchecked;
import static org.onosproject.net.flow.FlowRuleEvent.Type.RULE_REMOVED;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Manages inventory of flow rules using trivial in-memory implementation.
 */
@Component(immediate = true)
@Service
public class SimpleFlowRuleStore
        extends AbstractStore<FlowRuleBatchEvent, FlowRuleStoreDelegate>
        implements FlowRuleStore {

    private final Logger log = getLogger(getClass());


    // inner Map is Device flow table
    // inner Map value (FlowId synonym list) must be synchronized before modifying
    private final ConcurrentMap<DeviceId, ConcurrentMap<FlowId, List<StoredFlowEntry>>>
            flowEntries = new ConcurrentHashMap<>();

    private final ConcurrentMap<DeviceId, List<TableStatisticsEntry>>
            deviceTableStats = new ConcurrentHashMap<>();

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
        deviceTableStats.clear();
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


    @Override
    public int getFlowRuleCount() {
        int sum = 0;
        for (ConcurrentMap<FlowId, List<StoredFlowEntry>> ft : flowEntries.values()) {
            for (List<StoredFlowEntry> fes : ft.values()) {
                sum += fes.size();
            }
        }
        return sum;
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

    private static NewConcurrentHashMap<FlowId, List<StoredFlowEntry>> lazyEmptyFlowTable() {
        return NewConcurrentHashMap.<FlowId, List<StoredFlowEntry>>ifNeeded();
    }

    /**
     * Returns the flow table for specified device.
     *
     * @param deviceId identifier of the device
     * @return Map representing Flow Table of given device.
     */
    private ConcurrentMap<FlowId, List<StoredFlowEntry>> getFlowTable(DeviceId deviceId) {
        return createIfAbsentUnchecked(flowEntries,
                                       deviceId, lazyEmptyFlowTable());
    }

    private List<StoredFlowEntry> getFlowEntries(DeviceId deviceId, FlowId flowId) {
        final ConcurrentMap<FlowId, List<StoredFlowEntry>> flowTable = getFlowTable(deviceId);
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

    private FlowEntry getFlowEntryInternal(DeviceId deviceId, FlowRule rule) {
        List<StoredFlowEntry> fes = getFlowEntries(deviceId, rule.id());
        for (StoredFlowEntry fe : fes) {
            if (fe.equals(rule)) {
                return fe;
            }
        }
        return null;
    }

    @Override
    public FlowEntry getFlowEntry(FlowRule rule) {
        return getFlowEntryInternal(rule.deviceId(), rule);
    }

    @Override
    public Iterable<FlowEntry> getFlowEntries(DeviceId deviceId) {
        // flatten and make iterator unmodifiable
        return FluentIterable.from(getFlowTable(deviceId).values())
                .transformAndConcat(Collections::unmodifiableList);
    }

    @Override
    public void storeFlowRule(FlowRule rule) {
        storeFlowRuleInternal(rule);
    }

    private void storeFlowRuleInternal(FlowRule rule) {
        StoredFlowEntry f = new DefaultFlowEntry(rule);
        final DeviceId did = f.deviceId();
        final FlowId fid = f.id();
        List<StoredFlowEntry> existing = getFlowEntries(did, fid);
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

    @Override
    public void deleteFlowRule(FlowRule rule) {

        List<StoredFlowEntry> entries = getFlowEntries(rule.deviceId(), rule.id());

        synchronized (entries) {
            for (StoredFlowEntry entry : entries) {
                if (entry.equals(rule)) {
                    synchronized (entry) {
                        entry.setState(FlowEntryState.PENDING_REMOVE);
                    }
                }
            }
        }


        //log.warn("Cannot find rule {}", rule);
    }

    @Override
    public FlowRuleEvent addOrUpdateFlowRule(FlowEntry rule) {
        // check if this new rule is an update to an existing entry
        List<StoredFlowEntry> entries = getFlowEntries(rule.deviceId(), rule.id());
        synchronized (entries) {
            for (StoredFlowEntry stored : entries) {
                if (stored.equals(rule)) {
                    synchronized (stored) {
                        //FIXME modification of "stored" flow entry outside of flow table
                        stored.setBytes(rule.bytes());
                        stored.setLife(rule.life());
                        stored.setPackets(rule.packets());
                        if (stored.state() == FlowEntryState.PENDING_ADD) {
                            stored.setState(FlowEntryState.ADDED);
                            // TODO: Do we need to change `rule` state?
                            return new FlowRuleEvent(Type.RULE_ADDED, rule);
                        }
                        return new FlowRuleEvent(Type.RULE_UPDATED, rule);
                    }
                }
            }
        }

        // should not reach here
        // storeFlowRule was expected to be called
        log.error("FlowRule was not found in store {} to update", rule);

        //flowEntries.put(did, rule);
        return null;
    }

    @Override
    public FlowRuleEvent removeFlowRule(FlowEntry rule) {
        // This is where one could mark a rule as removed and still keep it in the store.
        final DeviceId did = rule.deviceId();

        List<StoredFlowEntry> entries = getFlowEntries(did, rule.id());
        synchronized (entries) {
            if (entries.remove(rule)) {
                return new FlowRuleEvent(RULE_REMOVED, rule);
            }
        }
        return null;
    }

    @Override
    public FlowRuleEvent pendingFlowRule(FlowEntry rule) {
        List<StoredFlowEntry> entries = getFlowEntries(rule.deviceId(), rule.id());
        synchronized (entries) {
            for (StoredFlowEntry entry : entries) {
                if (entry.equals(rule) &&
                        entry.state() != FlowEntryState.PENDING_ADD) {
                    synchronized (entry) {
                        entry.setState(FlowEntryState.PENDING_ADD);
                        return new FlowRuleEvent(Type.RULE_UPDATED, rule);
                    }
                }
            }
        }
        return null;
    }

    public void purgeFlowRule(DeviceId deviceId) {
        flowEntries.remove(deviceId);
    }

    @Override
    public void purgeFlowRules() {
        flowEntries.clear();
    }

    @Override
    public void storeBatch(
            FlowRuleBatchOperation operation) {
        List<FlowRuleBatchEntry> toAdd = new ArrayList<>();
        List<FlowRuleBatchEntry> toRemove = new ArrayList<>();

        for (FlowRuleBatchEntry entry : operation.getOperations()) {
            final FlowRule flowRule = entry.target();
            if (entry.operator().equals(FlowRuleOperation.ADD)) {
                if (!getFlowEntries(flowRule.deviceId(), flowRule.id()).contains(flowRule)) {
                    storeFlowRule(flowRule);
                    toAdd.add(entry);
                }
            } else if (entry.operator().equals(FlowRuleOperation.REMOVE)) {
                if (getFlowEntries(flowRule.deviceId(), flowRule.id()).contains(flowRule)) {
                    deleteFlowRule(flowRule);
                    toRemove.add(entry);
                }
            } else {
                throw new UnsupportedOperationException("Unsupported operation type");
            }
        }

        if (toAdd.isEmpty() && toRemove.isEmpty()) {
            notifyDelegate(FlowRuleBatchEvent.completed(
                    new FlowRuleBatchRequest(operation.id(), Collections.emptySet()),
                    new CompletedBatchOperation(true, Collections.emptySet(),
                                                operation.deviceId())));
            return;
        }

        SettableFuture<CompletedBatchOperation> r = SettableFuture.create();
        final int batchId = localBatchIdGen.incrementAndGet();

        pendingFutures.put(batchId, r);

        toAdd.addAll(toRemove);
        notifyDelegate(FlowRuleBatchEvent.requested(
                new FlowRuleBatchRequest(batchId, Sets.newHashSet(toAdd)), operation.deviceId()));

    }

    @Override
    public void batchOperationComplete(FlowRuleBatchEvent event) {
        final Long batchId = event.subject().batchId();
        SettableFuture<CompletedBatchOperation> future
            = pendingFutures.getIfPresent(batchId);
        if (future != null) {
            future.set(event.result());
            pendingFutures.invalidate(batchId);
        }
        notifyDelegate(event);
    }

    @Override
    public FlowRuleEvent updateTableStatistics(DeviceId deviceId,
                                               List<TableStatisticsEntry> tableStats) {
        deviceTableStats.put(deviceId, tableStats);
        return null;
    }

    @Override
    public Iterable<TableStatisticsEntry> getTableStatistics(DeviceId deviceId) {
        List<TableStatisticsEntry> tableStats = deviceTableStats.get(deviceId);
        if (tableStats == null) {
            return Collections.emptyList();
        }
        return ImmutableList.copyOf(tableStats);
    }

    private static final class TimeoutFuture
            implements RemovalListener<Integer, SettableFuture<CompletedBatchOperation>> {
        @Override
        public void onRemoval(RemovalNotification<Integer, SettableFuture<CompletedBatchOperation>> notification) {
            // wrapping in ExecutionException to support Future.get
            if (notification.wasEvicted()) {
                notification.getValue()
                        .setException(new ExecutionException("Timed out",
                                                             new TimeoutException()));
            }
        }
    }
}
