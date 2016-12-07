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
package org.onosproject.incubator.net.virtual.provider;

import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.CompletedBatchOperation;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.TableStatisticsEntry;

import java.util.List;

/**
 * Service through which virtual flow rule providers can inject information into
 * the core.
 */
public interface VirtualFlowRuleProviderService
        extends VirtualProviderService<VirtualFlowRuleProvider> {

    /**
     * Signals that a flow rule that was previously installed has been removed.
     *
     * @param flowEntry removed flow entry
     */
    void flowRemoved(FlowEntry flowEntry);

    /**
     * Pushes the collection of flow entries currently applied on the given
     * virtual device.
     *
     * @param deviceId device identifier
     * @param flowEntries collection of flow rules
     */
    void pushFlowMetrics(DeviceId deviceId, Iterable<FlowEntry> flowEntries);

    /**
     * Pushes the collection of flow entries currently applied on the given
     * device without flowMissing process.
     *
     * @param deviceId device identifier
     * @param flowEntries collection of flow rules
     */
    void pushFlowMetricsWithoutFlowMissing(DeviceId deviceId, Iterable<FlowEntry> flowEntries);

    /**
     * Pushes the collection of table statistics entries currently extracted
     * from the given virtual device.
     *
     * @param deviceId device identifier
     * @param tableStatsEntries collection of flow table statistics entries
     */
    void pushTableStatistics(DeviceId deviceId, List<TableStatisticsEntry> tableStatsEntries);

    /**
     * Indicates to the core that the requested batch operation has
     * been completed.
     *
     * @param batchId the batch which was processed
     * @param operation the resulting outcome of the operation
     */
    void batchOperationCompleted(long batchId, CompletedBatchOperation operation);

}
