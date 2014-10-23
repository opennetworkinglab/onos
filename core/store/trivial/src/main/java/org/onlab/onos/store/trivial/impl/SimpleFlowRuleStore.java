package org.onlab.onos.store.trivial.impl;

import static org.onlab.onos.net.flow.FlowRuleEvent.Type.RULE_REMOVED;
import static org.slf4j.LoggerFactory.getLogger;
import static org.apache.commons.lang3.concurrent.ConcurrentUtils.createIfAbsentUnchecked;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.ApplicationId;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.flow.CompletedBatchOperation;
import org.onlab.onos.net.flow.DefaultFlowEntry;
import org.onlab.onos.net.flow.FlowEntry;
import org.onlab.onos.net.flow.FlowEntry.FlowEntryState;
import org.onlab.onos.net.flow.FlowId;
import org.onlab.onos.net.flow.FlowRule;
import org.onlab.onos.net.flow.FlowRuleBatchEntry;
import org.onlab.onos.net.flow.FlowRuleBatchEntry.FlowRuleOperation;
import org.onlab.onos.net.flow.FlowRuleBatchEvent;
import org.onlab.onos.net.flow.FlowRuleBatchOperation;
import org.onlab.onos.net.flow.FlowRuleBatchRequest;
import org.onlab.onos.net.flow.FlowRuleEvent;
import org.onlab.onos.net.flow.FlowRuleEvent.Type;
import org.onlab.onos.net.flow.FlowRuleStore;
import org.onlab.onos.net.flow.FlowRuleStoreDelegate;
import org.onlab.onos.net.flow.StoredFlowEntry;
import org.onlab.onos.store.AbstractStore;
import org.onlab.util.NewConcurrentHashMap;
import org.slf4j.Logger;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.util.concurrent.Futures;

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

    @Activate
    public void activate() {
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        flowEntries.clear();
        log.info("Stopped");
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
            .transformAndConcat(
                    new Function<List<StoredFlowEntry>, Iterable<? extends FlowEntry>>() {

                @Override
                public Iterable<? extends FlowEntry> apply(
                        List<StoredFlowEntry> input) {
                    return Collections.unmodifiableList(input);
                }
            });
    }

    @Override
    public Iterable<FlowRule> getFlowRulesByAppId(ApplicationId appId) {

        Set<FlowRule> rules = new HashSet<>();
        for (DeviceId did : flowEntries.keySet()) {
            for (FlowEntry fe : getFlowEntries(did)) {
                if (fe.appId() == appId.id()) {
                    rules.add(fe);
                }
            }
        }
        return rules;
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
            notifyDelegate(FlowRuleBatchEvent.create(
                    new FlowRuleBatchRequest(
                            Arrays.<FlowEntry>asList(f),
                            Collections.<FlowEntry>emptyList())));
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
                        // TODO: Should we notify only if it's "remote" event?
                        notifyDelegate(FlowRuleBatchEvent.create(
                                new FlowRuleBatchRequest(
                                        Collections.<FlowEntry>emptyList(),
                                        Arrays.<FlowEntry>asList(entry))));
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
    public Future<CompletedBatchOperation> storeBatch(
            FlowRuleBatchOperation batchOperation) {
        for (FlowRuleBatchEntry entry : batchOperation.getOperations()) {
            if (entry.getOperator().equals(FlowRuleOperation.ADD)) {
                storeFlowRule(entry.getTarget());
            } else if (entry.getOperator().equals(FlowRuleOperation.REMOVE)) {
                deleteFlowRule(entry.getTarget());
            } else {
                throw new UnsupportedOperationException("Unsupported operation type");
            }
        }
        return Futures.immediateFuture(new CompletedBatchOperation(true, Collections.<FlowEntry>emptySet()));
    }

    @Override
    public void batchOperationComplete(FlowRuleBatchEvent event) {
        notifyDelegate(event);
    }
}
