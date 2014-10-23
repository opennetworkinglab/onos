package org.onlab.onos.net.flow;

import org.onlab.onos.event.AbstractEvent;

/**
 * Describes flow rule batch event.
 */
public final class FlowRuleBatchEvent extends AbstractEvent<FlowRuleBatchEvent.Type, FlowRuleBatchRequest> {

    /**
     * Type of flow rule events.
     */
    public enum Type {

        /**
         * Signifies that a batch operation has been initiated.
         */
        BATCH_OPERATION_REQUESTED,

        /**
         * Signifies that a batch operation has completed.
         */
        BATCH_OPERATION_COMPLETED,
    }

    private final CompletedBatchOperation result;

    /**
     * Constructs a new FlowRuleBatchEvent.
     * @param request batch operation request.
     * @return event.
     */
    public static FlowRuleBatchEvent create(FlowRuleBatchRequest request) {
        FlowRuleBatchEvent event = new FlowRuleBatchEvent(Type.BATCH_OPERATION_REQUESTED, request, null);
        return event;
    }

    /**
     * Constructs a new FlowRuleBatchEvent.
     * @param request batch operation request.
     * @param result completed batch operation result.
     * @return event.
     */
    public static FlowRuleBatchEvent create(FlowRuleBatchRequest request, CompletedBatchOperation result) {
        FlowRuleBatchEvent event = new FlowRuleBatchEvent(Type.BATCH_OPERATION_COMPLETED, request, result);
        return event;
    }

    /**
     * Returns the result of this batch operation.
     * @return batch operation result.
     */
    public CompletedBatchOperation result() {
        return result;
    }

    /**
     * Creates an event of a given type and for the specified flow rule batch.
     *
     * @param type    flow rule batch event type
     * @param batch    event flow rule batch subject
     */
    private FlowRuleBatchEvent(Type type, FlowRuleBatchRequest request, CompletedBatchOperation result) {
        super(type, request);
        this.result = result;
    }
}
