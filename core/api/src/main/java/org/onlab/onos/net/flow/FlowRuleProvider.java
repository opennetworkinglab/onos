package org.onlab.onos.net.flow;

import org.onlab.onos.net.provider.Provider;

/**
 * Abstraction of a flow rule provider.
 */
public interface FlowRuleProvider extends Provider {

    /**
     * Instructs the provider to apply the specified flow rules to their
     * respective devices.
     * @param flowRules one or more flow rules
     * throws SomeKindOfException that indicates which ones were applied and
     *                  which ones failed
     */
    void applyFlowRule(FlowRule... flowRules);

    /**
     * Instructs the provider to remove the specified flow rules to their
     * respective devices.
     * @param flowRules one or more flow rules
     * throws SomeKindOfException that indicates which ones were applied and
     *                  which ones failed
     */
    void removeFlowRule(FlowRule... flowRules);

}
