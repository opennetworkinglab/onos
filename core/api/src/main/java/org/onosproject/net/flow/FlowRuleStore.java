/*
 * Copyright 2014-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.net.flow;

import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.oldbatch.FlowRuleBatchEvent;
import org.onosproject.net.flow.oldbatch.FlowRuleBatchOperation;
import org.onosproject.store.Store;

import java.util.List;

/**
 * Manages inventory of flow rules; not intended for direct use.
 */
public interface FlowRuleStore extends Store<FlowRuleBatchEvent, FlowRuleStoreDelegate> {

    /**
     * Returns the number of flow rules in the store.
     *
     * @return number of flow rules
     */
    int getFlowRuleCount();

    /**
     * Returns the number of flow rules for the given device in the store.
     *
     * @param deviceId device identifier
     * @return number of flow rules for the given device
     */
    default int getFlowRuleCount(DeviceId deviceId) {
        return 0;
    }

    /**
     * Returns the number of flow rules in the given state for the given device.
     *
     * @param deviceId the device identifier
     * @param state the state for which to count flow rules
     * @return number of flow rules in the given state for the given device
     */
    default int getFlowRuleCount(DeviceId deviceId, FlowEntry.FlowEntryState state) {
        return 0;
    }

    /**
     * Returns the stored flow.
     *
     * @param rule the rule to look for
     * @return a flow rule
     */
    FlowEntry getFlowEntry(FlowRule rule);

    /**
     * Returns the flow entries associated with a device.
     *
     * @param deviceId the device ID
     * @return the flow entries
     */
    Iterable<FlowEntry> getFlowEntries(DeviceId deviceId);

    /**
     * // TODO: Better description of method behavior.
     * Stores a new flow rule without generating events.
     *
     * @param rule the flow rule to add
     * @deprecated in Cardinal Release
     */
    @Deprecated
    void storeFlowRule(FlowRule rule);

    /**
     * Stores a batch of flow rules.
     *
     * @param batchOperation batch of flow rules.
     *           A batch can contain flow rules for a single device only.
     *
     */
    void storeBatch(FlowRuleBatchOperation batchOperation);

    /**
     * Invoked on the completion of a storeBatch operation.
     *
     * @param event flow rule batch event
     */
    void batchOperationComplete(FlowRuleBatchEvent event);

    /**
     * Marks a flow rule for deletion. Actual deletion will occur
     * when the provider indicates that the flow has been removed.
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
    FlowRuleEvent addOrUpdateFlowRule(FlowEntry rule);

    /**
     * @param rule the flow entry to remove
     * @return flow_removed event, or null if nothing removed
     */
    FlowRuleEvent removeFlowRule(FlowEntry rule);

    /**
     * Marks a flow rule as PENDING_ADD during retry.
     *
     * Emits flow_update event if the state is changed
     *
     * @param rule the flow rule that is retrying
     * @return flow_updated event, or null if nothing updated
     */
    FlowRuleEvent pendingFlowRule(FlowEntry rule);

    /**
     * Removes all flow entries of given device from store.
     *
     * @param deviceId device id
     */
    default void purgeFlowRule(DeviceId deviceId) {}

    /**
     * Removes all flow entries of given device and application ID from store.
     *
     * @param deviceId device id
     * @param appId application id
     * @return true if operation was successful, false otherwise.
     */
    boolean purgeFlowRules(DeviceId deviceId, ApplicationId appId);

    /**
     * Removes all flow entries from store.
     */
    void purgeFlowRules();

    /**
     * Updates the flow table statistics of the specified device using
     * the given statistics.
     *
     * @param deviceId    device identifier
     * @param tableStats   list of table statistics
     * @return ready to send event describing what occurred;
     */
    FlowRuleEvent updateTableStatistics(DeviceId deviceId,
                                        List<TableStatisticsEntry> tableStats);

    /**
     * Returns the flow table statistics associated with a device.
     *
     * @param deviceId the device ID
     * @return the flow table statistics
     */
    Iterable<TableStatisticsEntry> getTableStatistics(DeviceId deviceId);

    /**
     * Returns number of flow rules in ADDED state for specified device.
     *
     * @param deviceId the device ID
     * @return number of flow rules in ADDED state
     * @deprecated since 2.1
     */
    @Deprecated
    long getActiveFlowRuleCount(DeviceId deviceId);
}
