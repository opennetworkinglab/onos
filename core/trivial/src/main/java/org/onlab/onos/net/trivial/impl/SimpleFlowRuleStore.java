package org.onlab.onos.net.trivial.impl;

import static org.onlab.onos.net.flow.FlowRuleEvent.Type.RULE_ADDED;
import static org.onlab.onos.net.flow.FlowRuleEvent.Type.RULE_REMOVED;
import static org.slf4j.LoggerFactory.getLogger;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.flow.FlowRule;
import org.onlab.onos.net.flow.FlowRuleEvent;
import org.onlab.onos.net.flow.FlowRuleEvent.Type;
import org.onlab.onos.net.flow.FlowRuleStore;
import org.slf4j.Logger;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

/**
 * Manages inventory of flow rules using trivial in-memory implementation.
 */
@Component(immediate = true)
@Service
public class SimpleFlowRuleStore implements FlowRuleStore {

    private final Logger log = getLogger(getClass());

    // store entries as a pile of rules, no info about device tables
    private final Multimap<DeviceId, FlowRule> flowEntries = ArrayListMultimap.create();

    @Activate
    public void activate() {
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public Iterable<FlowRule> getFlowEntries(DeviceId deviceId) {
        return ImmutableSet.copyOf(flowEntries.get(deviceId));
    }

    @Override
    public void storeFlowRule(FlowRule rule) {
        DeviceId did = rule.deviceId();
        flowEntries.put(did, rule);
    }

    @Override
    public void deleteFlowRule(FlowRule rule) {
        DeviceId did = rule.deviceId();

        /*
         *  find the rule and mark it for deletion.
         *  Ultimately a flow removed will come remove it.
         */
        if (flowEntries.containsEntry(did, rule)) {
            synchronized (flowEntries) {

                flowEntries.remove(did, rule);
                flowEntries.put(did, rule);
            }
        }
    }

    @Override
    public FlowRuleEvent addOrUpdateFlowRule(FlowRule rule) {
        DeviceId did = rule.deviceId();

        // check if this new rule is an update to an existing entry
        if (flowEntries.containsEntry(did, rule)) {
            synchronized (flowEntries) {
                // Multimaps support duplicates so we have to remove our rule
                // and replace it with the current version.

                flowEntries.remove(did, rule);
                flowEntries.put(did, rule);
            }
            return new FlowRuleEvent(Type.RULE_UPDATED, rule);
        }

        flowEntries.put(did, rule);
        return new FlowRuleEvent(RULE_ADDED, rule);
    }

    @Override
    public FlowRuleEvent removeFlowRule(FlowRule rule) {
        synchronized (this) {
            if (flowEntries.remove(rule.deviceId(), rule)) {
                return new FlowRuleEvent(RULE_REMOVED, rule);
            } else {
                return null;
            }
        }
    }



}
