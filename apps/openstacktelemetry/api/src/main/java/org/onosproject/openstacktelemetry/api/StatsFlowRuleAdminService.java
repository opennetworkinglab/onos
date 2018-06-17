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

import java.util.Set;

/**
 * Admin service API for making a flow rule.
 */
public interface StatsFlowRuleAdminService {

    /**
     * Start this service.
     */
    void start();

    /**
     * Stop this service.
     */
    void stop();

    /**
     * Craete a flow rule.
     *
     * @param flowRule  Flow rule for a VM
     */
    void createFlowRule(StatsFlowRule flowRule);

    /**
     * Get flow rule list.
     * @return flow rule list.
     */
    Set<FlowInfo> getFlowRule();

    /**
     * Get flow rule list.
     * @param flowRule Flow rule for a VM
     * @return flow rule list.
     */
    Set<FlowInfo> getFlowRule(StatsFlowRule flowRule);

    /**
     * Delete the flow rule.
     *
     * @param flowRule  Flow rule for Openstack VM
     */
    void deleteFlowRule(StatsFlowRule flowRule);
}
