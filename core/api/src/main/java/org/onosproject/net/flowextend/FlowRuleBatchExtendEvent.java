package org.onosproject.net.flowextend;


import org.onosproject.event.AbstractEvent;

public final class FlowRuleBatchExtendEvent extends AbstractEvent<FlowRuleBatchExtendEvent.Type, FlowRuleBatchExtendRequest> {

    private final FlowExtendCompletedOperation result;

	/**
     * Type of sncflow rule events.
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
	
	public FlowRuleBatchExtendEvent(Type type, FlowRuleBatchExtendRequest request,
			FlowExtendCompletedOperation result) {
		super(type, request);
		this.result = result;
	}

    /**
     * Constructs a new FlowRuleBatchEvent.
     * @param request batch operation request.
     * @return event.
     */
    public static FlowRuleBatchExtendEvent requested(FlowRuleBatchExtendRequest request) {
        FlowRuleBatchExtendEvent event = new FlowRuleBatchExtendEvent(Type.BATCH_OPERATION_REQUESTED, request, null);
        return event;
    }

    /**
     * Constructs a new FlowRuleBatchEvent.
     * @param request batch operation request.
     * @param result completed batch operation result.
     * @return event.
     */
    public static FlowRuleBatchExtendEvent completed(FlowRuleBatchExtendRequest request, FlowExtendCompletedOperation result) {
        FlowRuleBatchExtendEvent event = new FlowRuleBatchExtendEvent(Type.BATCH_OPERATION_COMPLETED, request, result);
        return event;
    }

    /**
     * Returns the result of this batch operation.
     * @return batch operation result.
     */
    public FlowExtendCompletedOperation getresult() {
        return result;
    }

}
