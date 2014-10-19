package org.onlab.onos.store.trivial.impl;

import static org.onlab.onos.net.flow.FlowRuleEvent.Type.RULE_REMOVED;
import static org.slf4j.LoggerFactory.getLogger;
import static org.apache.commons.lang3.concurrent.ConcurrentUtils.createIfAbsentUnchecked;
import static java.util.Collections.unmodifiableCollection;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.ApplicationId;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.flow.DefaultFlowEntry;
import org.onlab.onos.net.flow.FlowEntry;
import org.onlab.onos.net.flow.FlowEntry.FlowEntryState;
import org.onlab.onos.net.flow.FlowId;
import org.onlab.onos.net.flow.FlowRule;
import org.onlab.onos.net.flow.FlowRuleEvent;
import org.onlab.onos.net.flow.FlowRuleEvent.Type;
import org.onlab.onos.net.flow.FlowRuleStore;
import org.onlab.onos.net.flow.FlowRuleStoreDelegate;
import org.onlab.onos.net.flow.StoredFlowEntry;
import org.onlab.onos.store.AbstractStore;
import org.onlab.util.NewConcurrentHashMap;
import org.slf4j.Logger;

/**
 * Manages inventory of flow rules using trivial in-memory implementation.
 */
@Component(immediate = true)
@Service
public class SimpleFlowRuleStore
        extends AbstractStore<FlowRuleEvent, FlowRuleStoreDelegate>
        implements FlowRuleStore {

    private final Logger log = getLogger(getClass());


    // inner Map is Device flow table
    // Assumption: FlowId cannot have synonyms
    private final ConcurrentMap<DeviceId, ConcurrentMap<FlowId, StoredFlowEntry>>
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
        for (ConcurrentMap<FlowId, StoredFlowEntry> ft : flowEntries.values()) {
            sum += ft.size();
        }
        return sum;
    }

    private static NewConcurrentHashMap<FlowId, StoredFlowEntry> lazyEmptyFlowTable() {
        return NewConcurrentHashMap.<FlowId, StoredFlowEntry>ifNeeded();
    }

    /**
     * Returns the flow table for specified device.
     *
     * @param deviceId identifier of the device
     * @return Map representing Flow Table of given device.
     */
    private ConcurrentMap<FlowId, StoredFlowEntry> getFlowTable(DeviceId deviceId) {
        return createIfAbsentUnchecked(flowEntries,
                                       deviceId, lazyEmptyFlowTable());
    }

    private StoredFlowEntry getFlowEntry(DeviceId deviceId, FlowId flowId) {
        return getFlowTable(deviceId).get(flowId);
    }

    @Override
    public FlowEntry getFlowEntry(FlowRule rule) {
        return getFlowEntry(rule.deviceId(), rule.id());
    }

    @Override
    public Iterable<FlowEntry> getFlowEntries(DeviceId deviceId) {
        return unmodifiableCollection((Collection<? extends FlowEntry>)
                                       getFlowTable(deviceId).values());
    }

    @Override
    public Iterable<FlowRule> getFlowRulesByAppId(ApplicationId appId) {

        Set<FlowRule> rules = new HashSet<>();
        for (DeviceId did : flowEntries.keySet()) {
            ConcurrentMap<FlowId, StoredFlowEntry> ft = getFlowTable(did);
            for (FlowEntry fe : ft.values()) {
                if (fe.appId() == appId.id()) {
                    rules.add(fe);
                }
            }
        }
        return rules;
    }

    @Override
    public void storeFlowRule(FlowRule rule) {
        final boolean added = storeFlowRuleInternal(rule);
    }

    private boolean storeFlowRuleInternal(FlowRule rule) {
        StoredFlowEntry f = new DefaultFlowEntry(rule);
        final DeviceId did = f.deviceId();
        final FlowId fid = f.id();
        FlowEntry existing = getFlowTable(did).putIfAbsent(fid, f);
        if (existing != null) {
            // was already there? ignore
            return false;
        }
        // new flow rule added
        // TODO: notify through delegate about remote event?
        return true;
    }

    @Override
    public void deleteFlowRule(FlowRule rule) {

        StoredFlowEntry entry = getFlowEntry(rule.deviceId(), rule.id());
        if (entry == null) {
            //log.warn("Cannot find rule {}", rule);
            return;
        }
        synchronized (entry) {
            entry.setState(FlowEntryState.PENDING_REMOVE);
        }
    }

    @Override
    public FlowRuleEvent addOrUpdateFlowRule(FlowEntry rule) {
        // check if this new rule is an update to an existing entry
        StoredFlowEntry stored = getFlowEntry(rule.deviceId(), rule.id());
        if (stored != null) {
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

        ConcurrentMap<FlowId, StoredFlowEntry> ft = getFlowTable(did);
        if (ft.remove(rule.id(), rule)) {
            return new FlowRuleEvent(RULE_REMOVED, rule);
        } else {
            return null;
        }
    }
}
