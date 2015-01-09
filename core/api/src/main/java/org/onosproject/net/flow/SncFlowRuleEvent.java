package org.onosproject.net.flow;

import org.onosproject.event.AbstractEvent;
import org.onosproject.net.flow.FlowRuleBatchEvent.Type;

public final class SncFlowRuleEvent extends AbstractEvent<SncFlowRuleEvent.Type, SncFlowRuleEntry> {

	private final SncFlowCompletedOperation result;

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
	
	public SncFlowRuleEvent(Type type, SncFlowRuleEntry subject,
			SncFlowCompletedOperation result) {
		super(type, subject);
		this.result = result;
	}

    /**
     * Constructs a new FlowRuleBatchEvent.
     * @param request batch operation request.
     * @return event.
     */
    public static SncFlowRuleEvent requested(SncFlowRuleEntry request) {
    	SncFlowRuleEvent event = new SncFlowRuleEvent(Type.BATCH_OPERATION_REQUESTED, request, null);
        return event;
    }

    /**
     * Constructs a new FlowRuleBatchEvent.
     * @param request batch operation request.
     * @param result completed batch operation result.
     * @return event.
     */
    public static SncFlowRuleEvent completed(SncFlowRuleEntry request, SncFlowCompletedOperation result) {
    	SncFlowRuleEvent event = new SncFlowRuleEvent(Type.BATCH_OPERATION_COMPLETED, request, result);
        return event;
    }

    /**
     * Returns the result of this batch operation.
     * @return batch operation result.
     */
    public SncFlowCompletedOperation getresult() {
        return result;
    }

}
