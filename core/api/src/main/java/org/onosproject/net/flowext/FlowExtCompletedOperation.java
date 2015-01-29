package org.onosproject.net.flowext;

import java.util.Set;

import org.onosproject.net.flow.BatchOperationResult;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;

/**
 * Representation of a completed flow rule batch operation.
 */
public class FlowExtCompletedOperation implements BatchOperationResult<FlowRuleExtEntry> {
    private final int batchId;
    private final boolean success;
    private final Set<FlowRuleExtEntry> failures;

    public FlowExtCompletedOperation(int batchId, boolean success, Set<FlowRuleExtEntry> failures) {
        this.batchId = batchId;
        this.success = success;
        this.failures = ImmutableSet.copyOf(failures);
    }

    @Override
    public boolean isSuccess() {
        return success;
    }

    public int getBatchId() {
        return batchId;
    }

    @Override
    public Set<FlowRuleExtEntry> failedItems() {
        return failures;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("success?", success)
                .add("failedItems", failures)
                .toString();
    }
}