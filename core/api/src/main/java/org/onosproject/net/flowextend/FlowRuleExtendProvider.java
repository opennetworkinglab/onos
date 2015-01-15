package org.onosproject.net.flowextend;

import org.onosproject.net.provider.Provider;


public interface FlowRuleExtendProvider extends Provider{
    /**
     * Instructs the provider to apply the specified flow rules to their
     * respective devices.
     * @param flowRules one or more extended flow rules
     * throws SomeKindOfException that indicates which ones were applied and
     *                  which ones failed
     */
    void applyFlowRule(FlowRuleBatchExtendRequest flowRules);
}
