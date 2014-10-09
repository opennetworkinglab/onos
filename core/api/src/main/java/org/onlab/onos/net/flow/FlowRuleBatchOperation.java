package org.onlab.onos.net.flow;

import java.util.Collection;

import org.onlab.onos.net.intent.BatchOperation;

public class FlowRuleBatchOperation
    extends BatchOperation<FlowRuleBatchEntry> {

    public FlowRuleBatchOperation(Collection<FlowRuleBatchEntry> operations) {
        super(operations);
    }
}
