package org.onosproject.net.intent.impl;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Future;

import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.CompletedBatchOperation;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleBatchEntry;
import org.onosproject.net.flow.FlowRuleBatchOperation;
import org.onosproject.net.flow.FlowRuleListener;
import org.onosproject.net.flow.FlowRuleService;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;


public class MockFlowRuleService implements FlowRuleService {

    private Future<CompletedBatchOperation> future;
    final Set<FlowRule> flows = Sets.newHashSet();

    public void setFuture(boolean success) {
        setFuture(success, 0);
    }

    public void setFuture(boolean success, long intentId) {
        if (success) {
            future = Futures.immediateFuture(new CompletedBatchOperation(true, Collections.emptySet()));
        } else {
            final Set<Long> failedIds = ImmutableSet.of(intentId);
            future = Futures.immediateFuture(
                    new CompletedBatchOperation(false, flows, failedIds));
        }
    }

    @Override
    public Future<CompletedBatchOperation> applyBatch(FlowRuleBatchOperation batch) {
        for (FlowRuleBatchEntry fbe : batch.getOperations()) {
            FlowRule fr = fbe.getTarget();
            switch (fbe.getOperator()) {
                case ADD:
                    flows.add(fr);
                    break;
                case REMOVE:
                    flows.remove(fr);
                    break;
                case MODIFY:
                    break;
                default:
                    break;
            }
        }
        return future;
    }

    @Override
    public int getFlowRuleCount() {
        return flows.size();
    }

    @Override
    public Iterable<FlowEntry> getFlowEntries(DeviceId deviceId) {
        return null;
    }

    @Override
    public void applyFlowRules(FlowRule... flowRules) {
    }

    @Override
    public void removeFlowRules(FlowRule... flowRules) {
    }

    @Override
    public void removeFlowRulesById(ApplicationId appId) {
    }

    @Override
    public Iterable<FlowRule> getFlowRulesById(ApplicationId id) {
        return null;
    }

    @Override
    public Iterable<FlowRule> getFlowRulesByGroupId(ApplicationId appId, short groupId) {
        return null;
    }

    @Override
    public void addListener(FlowRuleListener listener) {

    }

    @Override
    public void removeListener(FlowRuleListener listener) {

    }
}

