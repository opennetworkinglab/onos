package org.onosproject.net.flowextend;

import java.util.Collection;

import org.onosproject.net.provider.Provider;


public interface FlowRuleExtendProvider extends Provider{
    /**
     * Instructs the provider to apply the specified flow rules to their
     * respective devices.
     * @param flowRules one or more extended flow rules
     * throws SomeKindOfException that indicates which ones were applied and
     *                  which ones failed
     */
	
	static final int POLL_INTERVAL = 10;
    void applyFlowRule(Collection<FlowRuleExtendEntry> flowRules);
}
