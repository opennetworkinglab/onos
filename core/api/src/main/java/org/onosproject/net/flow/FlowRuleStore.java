/*
 * Copyright 2014-2015 Open Networking Laboratory
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

import java.util.List;

import org.onosproject.net.DeviceId;
import org.onosproject.store.Store;

/**
 * Manages inventory of flow rules; not intended for direct use.
 */
public interface FlowRuleStore extends Store<FlowRuleBatchEvent, FlowRuleStoreDelegate> {

    /**
     * Returns the number of flow rule in the store.
     *
     * @return number of flow rules
     */
    int getFlowRuleCount();

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
}
