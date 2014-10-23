package org.onlab.onos.net.flow;

import java.util.Set;

import com.google.common.collect.ImmutableSet;

public class CompletedBatchOperation implements BatchOperationResult<FlowEntry> {


    private final boolean success;
    private final Set<FlowEntry> failures;

    public CompletedBatchOperation(boolean success, Set<FlowEntry> failures) {
        this.success = success;
        this.failures = ImmutableSet.copyOf(failures);
    }

    @Override
    public boolean isSuccess() {
        return success;
    }

    @Override
    public Set<FlowEntry> failedItems() {
        return failures;
    }


}
