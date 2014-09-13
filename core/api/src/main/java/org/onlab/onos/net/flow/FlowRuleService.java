package org.onlab.onos.net.flow;

import org.onlab.onos.net.DeviceId;

/**
 * Service for injecting flow rules into the environment and for obtaining
 * information about flow rules already in the environment.
 */
public interface FlowRuleService {

    /**
     * Returns the collection of flow entries applied on the specified device.
     *
     * @param deviceId device identifier
     * @return collection of flow rules
     */
    Iterable<FlowEntry> getFlowEntries(DeviceId deviceId);

    /**
     * Applies the specified flow rules onto their respective devices.
     *
     * @param flowRules one or more flow rules
     * throws SomeKindOfException that indicates which ones were applied and
     *                  which ones failed
     */
    void applyFlowRules(FlowRule... flowRules);

    /**
     * Removes the specified flow rules from their respective devices.
     *
     * @param flowRules one or more flow rules
     * throws SomeKindOfException that indicates which ones were removed and
     *                  which ones failed
     */
    void removeFlowRules(FlowRule... flowRules);

    /**
     * Adds the specified flow rule listener.
     *
     * @param listener flow rule listener
     */
    void addListener(FlowRuleListener listener);

    /**
     * Removes the specified flow rule listener.
     *
     * @param listener flow rule listener
     */
    void removeListener(FlowRuleListener listener);

}
