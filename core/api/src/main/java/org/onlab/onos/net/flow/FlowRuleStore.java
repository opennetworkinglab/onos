package org.onlab.onos.net.flow;

import org.onlab.onos.ApplicationId;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.store.Store;

/**
 * Manages inventory of flow rules; not intended for direct use.
 */
public interface FlowRuleStore extends Store<FlowRuleEvent, FlowRuleStoreDelegate> {

    /**
     * Returns the stored flow.
     * @param rule the rule to look for
     * @return a flow rule
     */
    FlowRule getFlowRule(FlowRule rule);

    /**
     * Returns the flow entries associated with a device.
     *
     * @param deviceId the device ID
     * @return the flow entries
     */
    Iterable<FlowRule> getFlowEntries(DeviceId deviceId);

    /**
     * Returns the flow entries associated with an application.
     *
     * @param appId the application id
     * @return the flow entries
     */
    Iterable<FlowRule> getFlowEntriesByAppId(ApplicationId appId);

    /**
     * Stores a new flow rule without generating events.
     *
     * @param rule the flow rule to add
     */
    void storeFlowRule(FlowRule rule);

    /**
     * Deletes a flow rule without generating events.
     *
     * @param rule the flow rule to delete
     */
    void deleteFlowRule(FlowRule rule);

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
