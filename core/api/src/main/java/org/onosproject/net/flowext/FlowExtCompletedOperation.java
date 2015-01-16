package org.onosproject.net.flowext;

import java.util.Set;

import org.onosproject.net.flow.BatchOperationResult;

public class FlowExtCompletedOperation implements BatchOperationResult<FlowRuleExtEntry> {
    private final boolean success;
    private final Set<FlowRuleExtEntry> failures;

    
	public FlowExtCompletedOperation(boolean success,
			Set<FlowRuleExtEntry> failures) {
		this.success = success;
		this.failures = failures;
	}

	@Override
	public boolean isSuccess() {
		// TODO Auto-generated method stub
		return success;
	}

	@Override
	public Set<FlowRuleExtEntry> failedItems() {
		// TODO Auto-generated method stub
		return failures;
	}

}
