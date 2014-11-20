package org.onlab.onos.net.intent.impl;

import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;
import org.onlab.onos.core.ApplicationId;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.flow.CompletedBatchOperation;
import org.onlab.onos.net.flow.FlowEntry;
import org.onlab.onos.net.flow.FlowRule;
import org.onlab.onos.net.flow.FlowRuleBatchEntry;
import org.onlab.onos.net.flow.FlowRuleBatchOperation;
import org.onlab.onos.net.flow.FlowRuleListener;
import org.onlab.onos.net.flow.FlowRuleService;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Future;


public class MockFlowRuleService implements FlowRuleService {

    private Future<CompletedBatchOperation> future;
    final Set<FlowRule> flows = Sets.newHashSet();

    public void setFuture(boolean success) {
        future = Futures.immediateFuture(new CompletedBatchOperation(true, Collections.emptySet()));
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

