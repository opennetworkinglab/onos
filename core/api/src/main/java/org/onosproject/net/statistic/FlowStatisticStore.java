/*
 * Copyright 2015-present Open Networking Foundation
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

package org.onosproject.net.statistic;

import org.onosproject.net.ConnectPoint;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;

import java.util.Set;

/**
 * Flow Store to house the computed statistics.
 */
public interface FlowStatisticStore {
    /**
     * Remove entries associated with this rule.
     *
     * @param rule {@link org.onosproject.net.flow.FlowRule}
     */
    void removeFlowStatistic(FlowRule rule);

    /**
     * Adds a flow stats observation for a flow rule. The previous flow will be removed.
     *
     * @param rule a {@link org.onosproject.net.flow.FlowEntry}
     */
    void addFlowStatistic(FlowEntry rule);

    /**
     * Updates a stats observation for a flow rule. The old flow stats will be moved to previous stats.
     *
     * @param rule a {@link org.onosproject.net.flow.FlowEntry}
     */
    void updateFlowStatistic(FlowEntry rule);

    /**
     * Fetches the current observed flow stats values.
     *
     * @param connectPoint the port to fetch information for
     * @return set of current flow rules
     */
    Set<FlowEntry> getCurrentFlowStatistic(ConnectPoint connectPoint);

    /**
     * Fetches the current observed flow stats values.
     *
     * @param connectPoint the port to fetch information for
     * @return set of current values
     */
    Set<FlowEntry> getPreviousFlowStatistic(ConnectPoint connectPoint);
}
