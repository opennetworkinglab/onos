package org.onlab.onos.net.flow;

import java.util.List;

import com.google.common.collect.ImmutableList;

public class CompletedBatchOperation implements BatchOperationResult<FlowEntry> {


    private final boolean success;
    private final List<FlowEntry> failures;

    public CompletedBatchOperation(boolean success, List<FlowEntry> failures) {
        this.success = success;
        this.failures = ImmutableList.copyOf(failures);
    }

    @Override
    public boolean isSuccess() {
        return success;
    }

    @Override
    public List<FlowEntry> failedItems() {
        return failures;
    }


}
