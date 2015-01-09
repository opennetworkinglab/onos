package org.onosproject.net.flow;

import java.util.Set;

public class SncFlowCompletedOperation implements BatchOperationResult<SncFlowRuleEntry> {
    private final boolean success;
    private final Set<SncFlowRuleEntry> failures;

    
	public SncFlowCompletedOperation(boolean success,
			Set<SncFlowRuleEntry> failures) {
		this.success = success;
		this.failures = failures;
	}

	@Override
	public boolean isSuccess() {
		// TODO Auto-generated method stub
		return success;
	}

	@Override
	public Set<SncFlowRuleEntry> failedItems() {
		// TODO Auto-generated method stub
		return failures;
	}

}
