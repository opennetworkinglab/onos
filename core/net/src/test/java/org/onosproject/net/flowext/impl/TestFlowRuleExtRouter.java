package org.onosproject.net.flowext.impl;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Future;

import org.onosproject.net.flowext.FlowExtCompletedOperation;
import org.onosproject.net.flowext.FlowRuleBatchExtEvent;
import org.onosproject.net.flowext.FlowRuleBatchExtRequest;
import org.onosproject.net.flowext.FlowRuleExtRouter;
import org.onosproject.net.flowext.FlowRuleExtRouterListener;

public class TestFlowRuleExtRouter implements FlowRuleExtRouter {
    protected Set<FlowRuleExtRouterListener> routerListener = new HashSet<>();
    @Override
    public Future<FlowExtCompletedOperation> applySubBatch(FlowRuleBatchExtRequest batchOperation) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void batchOperationComplete(FlowRuleBatchExtEvent event) {
        // TODO Auto-generated method stub
    }

    @Override
    public void addListener(FlowRuleExtRouterListener listener) {
        // TODO Auto-generated method stub
        routerListener.add(listener);
    }

    @Override
    public void removeListener(FlowRuleExtRouterListener listener) {
        // TODO Auto-generated method stub
        routerListener.remove(listener);
    }
}
