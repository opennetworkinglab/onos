package org.onlab.onos.net.flow;

import org.onlab.onos.net.DeviceId;

/**
 * Manages inventory of flow rules.
 */
public interface FlowRuleStore {

    /**
     * Returns the flow entries associated with a device.
     *
     * @param deviceId the device ID
     * @return the flow entries
     */
    Iterable<FlowRule> getFlowEntries(DeviceId deviceId);

    /**
     * Stores a new flow rule, and generates a FlowRule for it.
     *
     * @param rule the flow rule to add
     * @return a flow entry
     */
    FlowRule storeFlowRule(FlowRule rule);

    /**
     * Stores a new flow rule, or updates an existing entry.
     *
     * @param rule the flow rule to add or update
     * @return flow_added event, or null if just an update
     */
    FlowRuleEvent addOrUpdateFlowRule(FlowRule rule);

    /**
     * @param rule the flow rule to remove
     * @return flow_removed event, or null if nothing removed
     */
    FlowRuleEvent removeFlowRule(FlowRule rule);

}
