package org.onlab.onos.net.flow;

import org.onlab.onos.ApplicationId;
import org.onlab.onos.net.DeviceId;

/**
 * Service for injecting flow rules into the environment and for obtaining
 * information about flow rules already in the environment. This implements
 * semantics of a distributed authoritative flow table where the master copy
 * of the flow rules lies with the controller and the devices hold only the
 * 'cached' copy.
 */
public interface FlowRuleService {

    /**
     * Returns the number of flow rules in the system.
     *
     * @return flow rule count
     */
    int getFlowRuleCount();

    /**
     * Returns the collection of flow entries applied on the specified device.
     * This will include flow rules which may not yet have been applied to
     * the device.
     *
     * @param deviceId device identifier
     * @return collection of flow rules
     */
    Iterable<FlowEntry> getFlowEntries(DeviceId deviceId);

    // TODO: add createFlowRule factory method and execute operations method

    /**
     * Applies the specified flow rules onto their respective devices. These
     * flow rules will be retained by the system and re-applied anytime the
     * device reconnects to the controller.
     *
     * @param flowRules one or more flow rules
     */
    void applyFlowRules(FlowRule... flowRules);

    /**
     * Removes the specified flow rules from their respective devices. If the
     * device is not presently connected to the controller, these flow will
     * be removed once the device reconnects.
     *
     * @param flowRules one or more flow rules
     * throws SomeKindOfException that indicates which ones were removed and
     *                  which ones failed
     */
    void removeFlowRules(FlowRule... flowRules);

    /**
     * Removes all rules by id.
     *
     * @param appId id to remove
     */
    void removeFlowRulesById(ApplicationId appId);

    /**
     * Returns a list of rules with this application id.
     *
     * @param id the id to look up
     * @return collection of flow rules
     */
    Iterable<FlowRule> getFlowRulesById(ApplicationId id);

    //Future<CompletedBatchOperation> applyBatch(BatchOperation<FlowRuleBatchEntry>)

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
