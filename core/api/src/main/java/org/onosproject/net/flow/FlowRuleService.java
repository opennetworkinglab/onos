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

import org.onosproject.core.ApplicationId;
import org.onosproject.event.ListenerService;
import org.onosproject.net.DeviceId;

/**
 * Service for injecting flow rules into the environment and for obtaining
 * information about flow rules already in the environment. This implements
 * semantics of a distributed authoritative flow table where the master copy
 * of the flow rules lies with the controller and the devices hold only the
 * 'cached' copy.
 */
public interface FlowRuleService
    extends ListenerService<FlowRuleEvent, FlowRuleListener> {

    /**
     * The topic used for obtaining globally unique ids.
     */
    String FLOW_OP_TOPIC = "flow-ops-ids";

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

    /**
     * Returns a list of rules filterd by application and group id.
     *
     * @param appId the application id to lookup
     * @param groupId the groupid to lookup
     * @return collection of flow rules
     */
    Iterable<FlowRule> getFlowRulesByGroupId(ApplicationId appId, short groupId);

    /**
     * Applies a batch operation of FlowRules.
     *
     * @param ops batch operation to apply
     */
    void apply(FlowRuleOperations ops);

    /**
     * Returns the collection of flow table statistics of the specified device.
     *
     * @param deviceId device identifier
     * @return collection of flow table statistics
     */
    Iterable<TableStatisticsEntry> getFlowTableStatistics(DeviceId deviceId);
}
