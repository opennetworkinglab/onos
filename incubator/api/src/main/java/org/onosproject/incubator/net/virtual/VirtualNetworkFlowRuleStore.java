/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.incubator.net.virtual;

import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleBatchEvent;
import org.onosproject.net.flow.FlowRuleBatchOperation;
import org.onosproject.net.flow.FlowRuleEvent;
import org.onosproject.net.flow.FlowRuleStoreDelegate;
import org.onosproject.net.flow.TableStatisticsEntry;

import java.util.List;

/**
 * Manages inventory of flow rules for virtual networks;
 * not intended for direct use.
 */
public interface VirtualNetworkFlowRuleStore
        extends VirtualStore<FlowRuleBatchEvent, FlowRuleStoreDelegate> {
    /**
     * Returns the number of flow rule in the store.
     *
     * @param networkId virtual network identifier
     * @return number of flow rules
     */
    int getFlowRuleCount(NetworkId networkId);

    /**
     * Returns the stored flow.
     *
     * @param networkId virtual network identifier
     * @param rule the rule to look for
     * @return a flow rule
     */
    FlowEntry getFlowEntry(NetworkId networkId, FlowRule rule);

    /**
     * Returns the flow entries associated with a device.
     *
     * @param networkId virtual network identifier
     * @param deviceId the device ID
     * @return the flow entries
     */
    Iterable<FlowEntry> getFlowEntries(NetworkId networkId, DeviceId deviceId);

    /**
     * Stores a batch of flow rules.
     *
     * @param networkId virtual network identifier
     * @param batchOperation batch of flow rules.
     *           A batch can contain flow rules for a single device only.
     *
     */
    void storeBatch(NetworkId networkId, FlowRuleBatchOperation batchOperation);

    /**
     * Invoked on the completion of a storeBatch operation.
     *
     * @param networkId virtual network identifier
     * @param event flow rule batch event
     */
    void batchOperationComplete(NetworkId networkId, FlowRuleBatchEvent event);

    /**
     * Marks a flow rule for deletion. Actual deletion will occur
     * when the provider indicates that the flow has been removed.
     *
     * @param networkId virtual network identifier
     * @param rule the flow rule to delete
     */
    void deleteFlowRule(NetworkId networkId, FlowRule rule);

    /**
     * Stores a new flow rule, or updates an existing entry.
     *
     * @param networkId virtual network identifier
     * @param rule the flow rule to add or update
     * @return flow_added event, or null if just an update
     */
    FlowRuleEvent addOrUpdateFlowRule(NetworkId networkId, FlowEntry rule);

    /**
     * Removes an existing flow entry.
     *
     * @param rule the flow entry to remove
     * @param networkId virtual network identifier
     * @return flow_removed event, or null if nothing removed
     */
    FlowRuleEvent removeFlowRule(NetworkId networkId, FlowEntry rule);

    /**
     * Marks a flow rule as PENDING_ADD during retry.
     *
     * Emits flow_update event if the state is changed
     *
     * @param networkId virtual network identifier
     * @param rule the flow rule that is retrying
     * @return flow_updated event, or null if nothing updated
     */
    FlowRuleEvent pendingFlowRule(NetworkId networkId, FlowEntry rule);

    /**
     * Removes all flow entries of given device from store.
     *
     * @param networkId virtual network identifier
     * @param deviceId device id
     */
    default void purgeFlowRule(NetworkId networkId, DeviceId deviceId) {}

    /**
     * Removes all flow entries from store.
     *
     * @param networkId virtual network identifier
     */
    void purgeFlowRules(NetworkId networkId);

    /**
     * Updates the flow table statistics of the specified device using
     * the given statistics.
     *
     * @param networkId virtual network identifier
     * @param deviceId    device identifier
     * @param tableStats   list of table statistics
     * @return ready to send event describing what occurred;
     */
    FlowRuleEvent updateTableStatistics(NetworkId networkId, DeviceId deviceId,
                                        List<TableStatisticsEntry> tableStats);

    /**
     * Returns the flow table statistics associated with a device.
     *
     * @param networkId virtual network identifier
     * @param deviceId the device ID
     * @return the flow table statistics
     */
    Iterable<TableStatisticsEntry> getTableStatistics(NetworkId networkId, DeviceId deviceId);
}
