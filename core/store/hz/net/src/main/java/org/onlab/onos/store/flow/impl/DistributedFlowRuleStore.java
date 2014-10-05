package org.onlab.onos.store.flow.impl;

import static org.onlab.onos.net.flow.FlowRuleEvent.Type.RULE_REMOVED;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collection;
import java.util.Collections;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.ApplicationId;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.flow.DefaultFlowEntry;
import org.onlab.onos.net.flow.FlowEntry;
import org.onlab.onos.net.flow.FlowEntry.FlowEntryState;
import org.onlab.onos.net.flow.FlowRule;
import org.onlab.onos.net.flow.FlowRuleEvent;
import org.onlab.onos.net.flow.FlowRuleEvent.Type;
import org.onlab.onos.net.flow.FlowRuleStore;
import org.onlab.onos.net.flow.FlowRuleStoreDelegate;
import org.onlab.onos.store.AbstractStore;
import org.slf4j.Logger;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

/**
 * Manages inventory of flow rules using trivial in-memory implementation.
 */
//FIXME I LIE. I AIN'T DISTRIBUTED
@Component(immediate = true)
@Service
public class DistributedFlowRuleStore
        extends AbstractStore<FlowRuleEvent, FlowRuleStoreDelegate>
        implements FlowRuleStore {

    private final Logger log = getLogger(getClass());

    // store entries as a pile of rules, no info about device tables
    private final Multimap<DeviceId, FlowEntry> flowEntries =
            ArrayListMultimap.<DeviceId, FlowEntry>create();

    private final Multimap<ApplicationId, FlowRule> flowEntriesById =
            ArrayListMultimap.<ApplicationId, FlowRule>create();

    @Activate
    public void activate() {
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }


    @Override
    public synchronized FlowEntry getFlowEntry(FlowRule rule) {
        for (FlowEntry f : flowEntries.get(rule.deviceId())) {
            if (f.equals(rule)) {
                return f;
            }
        }
        return null;
    }

    @Override
    public synchronized Iterable<FlowEntry> getFlowEntries(DeviceId deviceId) {
        Collection<FlowEntry> rules = flowEntries.get(deviceId);
        if (rules == null) {
            return Collections.emptyList();
        }
        return ImmutableSet.copyOf(rules);
    }

    @Override
    public synchronized Iterable<FlowRule> getFlowRulesByAppId(ApplicationId appId) {
        Collection<FlowRule> rules = flowEntriesById.get(appId);
        if (rules == null) {
            return Collections.emptyList();
        }
        return ImmutableSet.copyOf(rules);
    }

    @Override
    public synchronized void storeFlowRule(FlowRule rule) {
        FlowEntry f = new DefaultFlowEntry(rule);
        DeviceId did = f.deviceId();
        if (!flowEntries.containsEntry(did, f)) {
            flowEntries.put(did, f);
            flowEntriesById.put(rule.appId(), f);
        }
    }

    @Override
    public synchronized void deleteFlowRule(FlowRule rule) {
        FlowEntry entry = getFlowEntry(rule);
        if (entry == null) {
            return;
        }
        entry.setState(FlowEntryState.PENDING_REMOVE);
    }

    @Override
    public synchronized FlowRuleEvent addOrUpdateFlowRule(FlowEntry rule) {
        DeviceId did = rule.deviceId();

        // check if this new rule is an update to an existing entry
        FlowEntry stored = getFlowEntry(rule);
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

        flowEntries.put(did, rule);
        return null;
    }

    @Override
    public synchronized FlowRuleEvent removeFlowRule(FlowEntry rule) {
        // This is where one could mark a rule as removed and still keep it in the store.
        if (flowEntries.remove(rule.deviceId(), rule)) {
            return new FlowRuleEvent(RULE_REMOVED, rule);
        } else {
            return null;
        }
    }
}
