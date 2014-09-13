package org.onlab.onos.net.flow;

import org.onlab.onos.net.DeviceId;
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
     * Returns the collection of flow entries currently applied on the given
     * device.
     *
     * @param deviceId device identifier
     * @return collection of flow entries
     */
    Iterable<FlowEntry> getFlowMetrics(DeviceId deviceId);

}
