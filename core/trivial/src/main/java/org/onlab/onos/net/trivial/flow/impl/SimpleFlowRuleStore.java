package org.onlab.onos.net.trivial.flow.impl;

import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.flow.DefaultFlowEntry;
import org.onlab.onos.net.flow.FlowEntry;
import org.onlab.onos.net.flow.FlowRule;
import org.onlab.onos.net.flow.FlowRuleEvent;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

import static org.onlab.onos.net.flow.FlowRuleEvent.Type.*;

/**
 * Manages inventory of flow rules using trivial in-memory implementation.
 */
public class SimpleFlowRuleStore {

    // store entries as a pile of rules, no info about device tables
    private final Multimap<DeviceId, FlowEntry> flowEntries = HashMultimap.create();

    /**
     * Returns the flow entries associated with a device.
     *
     * @param deviceId the device ID
     * @return the flow entries
     */
    Iterable<FlowEntry> getFlowEntries(DeviceId deviceId) {
        return ImmutableSet.copyOf(flowEntries.get(deviceId));
    }

    /**
     * Stores a new flow rule, and generates a FlowEntry for it.
     *
     * @param rule the flow rule to add
     * @return a flow entry
     */
    FlowEntry storeFlowRule(FlowRule rule) {
        DeviceId did = rule.deviceId();
        FlowEntry entry = new DefaultFlowEntry(did,
                rule.selector(), rule.treatment(), rule.priority());
        flowEntries.put(did, entry);
        return entry;
    }

    /**
     * Stores a new flow rule, or updates an existing entry.
     *
     * @param rule the flow rule to add or update
     * @return flow_added event, or null if just an update
     */
    FlowRuleEvent addOrUpdateFlowRule(FlowRule rule) {
        DeviceId did = rule.deviceId();

        // check if this new rule is an update to an existing entry
        for (FlowEntry fe : flowEntries.get(did)) {
            if (rule.equals(fe)) {
                // TODO update the stats on this flowEntry?
                return null;
            }
        }

        FlowEntry newfe = new DefaultFlowEntry(did,
                rule.selector(), rule.treatment(), rule.priority());
        flowEntries.put(did, newfe);
        return new FlowRuleEvent(RULE_ADDED, rule);
    }

    /**
     *
     * @param rule the flow rule to remove
     * @return flow_removed event, or null if nothing removed
     */
    FlowRuleEvent removeFlowRule(FlowRule rule) {
        synchronized (this) {
            if (flowEntries.remove(rule.deviceId(), rule)) {
                return new FlowRuleEvent(RULE_REMOVED, rule);
            } else {
                return null;
            }
        }
    }

}
