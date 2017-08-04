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
package org.onosproject.net.statistic;

import org.onosproject.net.ConnectPoint;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;

import java.util.Set;

/**
 * Store to house the computed statistics.
 */
public interface StatisticStore {

    /**
     * Lay the foundation for receiving flow stats for this rule.
     *
     * @param rule a {@link org.onosproject.net.flow.FlowRule}
     */
    void prepareForStatistics(FlowRule rule);

    /**
     * Remove entries associated with this rule.
     *
     * @param rule {@link org.onosproject.net.flow.FlowRule}
     */
    void removeFromStatistics(FlowRule rule);

    /**
     * Adds a stats observation for a flow rule.
     *
     * @param rule a {@link org.onosproject.net.flow.FlowEntry}
     */
    void addOrUpdateStatistic(FlowEntry rule);

    /**
     * Fetches the current observed stats values.
     *
     * @param connectPoint the port to fetch information for
     * @return set of current flow rules
     */
    Set<FlowEntry> getCurrentStatistic(ConnectPoint connectPoint);

    /**
     * Fetches the previous observed stats values.
     *
     * @param connectPoint the port to fetch information for
     * @return set of current values
     */
    Set<FlowEntry> getPreviousStatistic(ConnectPoint connectPoint);
}
