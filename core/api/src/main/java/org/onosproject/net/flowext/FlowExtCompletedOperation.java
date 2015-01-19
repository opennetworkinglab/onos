package org.onosproject.net.flowext;

import java.util.Set;

import org.onosproject.net.flow.BatchOperationResult;

/**
 * Representation of a completed flow rule batch operation.
 */
public class FlowExtCompletedOperation implements BatchOperationResult<FlowRuleExtEntry> {
    private final boolean success;
    private final Set<FlowRuleExtEntry> failures;

    public FlowExtCompletedOperation(boolean success, Set<FlowRuleExtEntry> failures) {
        this.success = success;
        this.failures = failures;
    }

    @Override
    public boolean isSuccess() {
        return success;
    }

    @Override
    public Set<FlowRuleExtEntry> failedItems() {
        return failures;
    }
}
