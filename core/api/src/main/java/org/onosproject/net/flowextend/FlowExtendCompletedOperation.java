package org.onosproject.net.flowextend;

import java.util.Set;

import org.onosproject.net.flow.BatchOperationResult;

public class FlowExtendCompletedOperation implements BatchOperationResult<FlowRuleExtendEntry> {
    private final boolean success;
    private final Set<FlowRuleExtendEntry> failures;

    
	public FlowExtendCompletedOperation(boolean success,
			Set<FlowRuleExtendEntry> failures) {
		this.success = success;
		this.failures = failures;
	}

	@Override
	public boolean isSuccess() {
		// TODO Auto-generated method stub
		return success;
	}

	@Override
	public Set<FlowRuleExtendEntry> failedItems() {
		// TODO Auto-generated method stub
		return failures;
	}

}
