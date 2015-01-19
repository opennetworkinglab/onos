package org.onosproject.net.flowext;


import org.onosproject.event.AbstractEvent;

public final class FlowRuleBatchExtEvent extends AbstractEvent<FlowRuleBatchExtEvent.Type, FlowRuleBatchExtRequest> {

    private final FlowExtCompletedOperation result;

    /**
     * Type of flow extension rule events.
     */
    public enum Type {

        // Request has been forwarded to MASTER Node
        /**
         * Signifies that a batch operation has been initiated.
         */
        BATCH_OPERATION_REQUESTED,

        // MASTER Node has pushed the batch down to the Device
        // (e.g., Received barrier reply)
        /**
         * Signifies that a batch operation has completed.
         */
        BATCH_OPERATION_COMPLETED,
    }

    public FlowRuleBatchExtEvent(Type type, FlowRuleBatchExtRequest request, FlowExtCompletedOperation result) {
        super(type, request);
        this.result = result;
    }

    /**
     * Constructs a new FlowRuleBatchEvent.
     * @param request batch operation request.
     * @return event.
     */
    public static FlowRuleBatchExtEvent requested(FlowRuleBatchExtRequest request) {
        FlowRuleBatchExtEvent event = new FlowRuleBatchExtEvent(Type.BATCH_OPERATION_REQUESTED, request, null);
        return event;
    }

    /**
     * Constructs a new FlowRuleBatchEvent.
     * @param request batch operation request.
     * @param result completed batch operation result.
     * @return event.
     */
    public static FlowRuleBatchExtEvent completed(FlowRuleBatchExtRequest request, FlowExtCompletedOperation result) {
        FlowRuleBatchExtEvent event = new FlowRuleBatchExtEvent(Type.BATCH_OPERATION_COMPLETED, request, result);
        return event;
    }

    /**
     * Returns the result of this batch operation.
     * @return batch operation result.
     */
    public FlowExtCompletedOperation getresult() {
        return result;
    }

}
