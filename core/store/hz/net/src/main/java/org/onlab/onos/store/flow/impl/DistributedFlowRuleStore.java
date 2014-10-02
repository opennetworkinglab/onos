package org.onlab.onos.store.flow.impl;

import static org.onlab.onos.net.flow.FlowRuleEvent.Type.RULE_ADDED;
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
import org.onlab.onos.net.flow.DefaultFlowRule;
import org.onlab.onos.net.flow.FlowRule;
import org.onlab.onos.net.flow.FlowRule.FlowRuleState;
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
 * TEMPORARY: Manages inventory of flow rules using distributed store implementation.
 */
//FIXME: I LIE I AM NOT DISTRIBUTED
@Component(immediate = true)
@Service
public class DistributedFlowRuleStore
extends AbstractStore<FlowRuleEvent, FlowRuleStoreDelegate>
implements FlowRuleStore {

    private final Logger log = getLogger(getClass());

    // store entries as a pile of rules, no info about device tables
    private final Multimap<DeviceId, FlowRule> flowEntries =
            ArrayListMultimap.<DeviceId, FlowRule>create();

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
    public synchronized FlowRule getFlowRule(FlowRule rule) {
        for (FlowRule f : flowEntries.get(rule.deviceId())) {
            if (f.equals(rule)) {
                return f;
            }
        }
        return null;
    }

    @Override
    public synchronized Iterable<FlowRule> getFlowEntries(DeviceId deviceId) {
        Collection<FlowRule> rules = flowEntries.get(deviceId);
        if (rules == null) {
            return Collections.emptyList();
        }
        return ImmutableSet.copyOf(rules);
    }

    @Override
    public synchronized Iterable<FlowRule> getFlowEntriesByAppId(ApplicationId appId) {
        Collection<FlowRule> rules = flowEntriesById.get(appId);
        if (rules == null) {
            return Collections.emptyList();
        }
        return ImmutableSet.copyOf(rules);
    }

    @Override
    public synchronized void storeFlowRule(FlowRule rule) {
        FlowRule f = new DefaultFlowRule(rule, FlowRuleState.PENDING_ADD);
        DeviceId did = f.deviceId();
        if (!flowEntries.containsEntry(did, f)) {
            flowEntries.put(did, f);
            flowEntriesById.put(rule.appId(), f);
        }
    }

    @Override
    public synchronized void deleteFlowRule(FlowRule rule) {
        FlowRule f = new DefaultFlowRule(rule, FlowRuleState.PENDING_REMOVE);
        DeviceId did = f.deviceId();

        /*
         *  find the rule and mark it for deletion.
         *  Ultimately a flow removed will come remove it.
         */

        if (flowEntries.containsEntry(did, f)) {
            //synchronized (flowEntries) {
            flowEntries.remove(did, f);
            flowEntries.put(did, f);
            flowEntriesById.remove(rule.appId(), rule);
            //}
        }
    }

    @Override
    public synchronized FlowRuleEvent addOrUpdateFlowRule(FlowRule rule) {
        DeviceId did = rule.deviceId();

        // check if this new rule is an update to an existing entry
        if (flowEntries.containsEntry(did, rule)) {
            //synchronized (flowEntries) {
            // Multimaps support duplicates so we have to remove our rule
            // and replace it with the current version.
            flowEntries.remove(did, rule);
            flowEntries.put(did, rule);
            //}
            return new FlowRuleEvent(Type.RULE_UPDATED, rule);
        }

        flowEntries.put(did, rule);
        return new FlowRuleEvent(RULE_ADDED, rule);
    }

    @Override
    public synchronized FlowRuleEvent removeFlowRule(FlowRule rule) {
        //synchronized (this) {
        if (flowEntries.remove(rule.deviceId(), rule)) {
            return new FlowRuleEvent(RULE_REMOVED, rule);
        } else {
            return null;
        }
        //}
    }







}
