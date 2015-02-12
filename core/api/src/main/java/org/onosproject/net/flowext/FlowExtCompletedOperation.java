package org.onosproject.net.flowext;

import java.util.Set;

import org.onosproject.net.flow.BatchOperationResult;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;

/**
 * Representation of a completed flow rule batch operation.
 */
public class FlowExtCompletedOperation implements BatchOperationResult<FlowRuleExt> {
    // the batchId is provided by application, once one flow rule of this batch failed
    // all the batch should withdraw
    private final int batchId;
    private final boolean success;
    private final Set<FlowRuleExt> failures;

    public FlowExtCompletedOperation(int batchId, boolean success, Set<FlowRuleExt> failures) {
        this.batchId = batchId;
        this.success = success;
        this.failures = ImmutableSet.copyOf(failures);
    }

    /**
     * Returns whether the operation was successful.
     * @return true if successful, false otherwise
     */
    @Override
    public boolean isSuccess() {
        return success;
    }

    /**
     * Returns the BatchId of this BatchOperation.
     * @return the number of Batch
     */
    public int getBatchId() {
        return batchId;
    }

    /**
     * Obtains a set of items which failed.
     * @return a set of failures
     */
    @Override
    public Set<FlowRuleExt> failedItems() {
        return failures;
    }

    /**
     * Returns a string representation of the object.
     *
     * @return  a string representation of the object.
     */
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("success?", success)
                .add("failedItems", failures)
                .toString();
    }
}