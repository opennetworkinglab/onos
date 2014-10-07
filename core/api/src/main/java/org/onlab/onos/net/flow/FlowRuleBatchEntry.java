package org.onlab.onos.net.flow;

import org.onlab.onos.net.flow.FlowRuleBatchEntry.FlowRuleOperation;
import org.onlab.onos.net.intent.BatchOperationEntry;


public class FlowRuleBatchEntry
        extends BatchOperationEntry<FlowRuleOperation, FlowRule> {

    public FlowRuleBatchEntry(FlowRuleOperation operator, FlowRule target) {
        super(operator, target);
    }

    public enum FlowRuleOperation {
        ADD,
        REMOVE,
        MODIFY
    }

}
