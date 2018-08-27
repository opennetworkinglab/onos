/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.openstacktelemetry.api;

import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Admin service API for making a flow rule.
 */
public interface StatsFlowRuleAdminService {

    /**
     * Starts this service.
     */
    void start();

    /**
     * Stops this service.
     */
    void stop();

    /**
     * Creates a stat flow rule.
     *
     * @param statFlowRule stat flow rule for a VM
     */
    void createStatFlowRule(StatsFlowRule statFlowRule);

    /**
     * Gets a set of flow infos collected from overlay network.
     *
     * @return a set of flow infos
     */
    Set<FlowInfo> getOverlayFlowInfos();

    /**
     * Gets a set of flow infos collected from underlay network.
     *
     * @return a set of flow infos
     */
    Set<FlowInfo> getUnderlayFlowInfos();

    /**
     * Deletes stat flow rule.
     *
     * @param statFlowRule stat flow rule for a VM
     */
    void deleteStatFlowRule(StatsFlowRule statFlowRule);

    /**
     * Gets a map of flow information.
     *
     * @return a map of flow infos
     */
    Map<String, Queue<FlowInfo>> getFlowInfoMap();

}
