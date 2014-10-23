package org.onlab.onos.net.flow;

import java.util.Collections;
import java.util.List;

import org.onlab.onos.net.flow.FlowRuleBatchEntry.FlowRuleOperation;

import com.google.common.collect.Lists;

public class FlowRuleBatchRequest {

    private final List<FlowEntry> toAdd;
    private final List<FlowEntry> toRemove;

    public FlowRuleBatchRequest(List<FlowEntry> toAdd, List<FlowEntry> toRemove) {
        this.toAdd = Collections.unmodifiableList(toAdd);
        this.toRemove = Collections.unmodifiableList(toRemove);
    }

    public List<FlowEntry> toAdd() {
        return toAdd;
    }

    public List<FlowEntry> toRemove() {
        return toRemove;
    }

    public FlowRuleBatchOperation asBatchOperation() {
        List<FlowRuleBatchEntry> entries = Lists.newArrayList();
        for (FlowEntry e : toAdd) {
            entries.add(new FlowRuleBatchEntry(FlowRuleOperation.ADD, e));
        }
        for (FlowEntry e : toRemove) {
            entries.add(new FlowRuleBatchEntry(FlowRuleOperation.REMOVE, e));
        }
        return new FlowRuleBatchOperation(entries);
    }
}
