/*
 * Copyright 2014-present Open Networking Laboratory
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

import com.google.common.collect.Iterables;
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

    /**
     * Returns a list of rules filtered by device id and flow live type.
     *
     * @param deviceId the device id to lookup
     * @param liveType the flow live type to lookup
     * @return collection of flow entries
     */
    default Iterable<FlowEntry> getFlowEntriesByLiveType(DeviceId deviceId,
                                                 FlowEntry.FlowLiveType liveType) {
        return Iterables.filter(getFlowEntries(deviceId), fe -> fe.liveType() == liveType);
    }

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
     * Purges all the flow rules on the specified device.
     * @param deviceId device identifier
     */
    void purgeFlowRules(DeviceId deviceId);

    /**
     * Removes the specified flow rules from their respective devices. If the
     * device is not presently connected to the controller, these flow will
     * be removed once the device reconnects.
     *
     * @param flowRules one or more flow rules
     */
    void removeFlowRules(FlowRule... flowRules);

    /**
     * Removes all rules submitted by a particular application.
     *
     * @param appId ID of application whose flows will be removed
     */
    void removeFlowRulesById(ApplicationId appId);

    /**
     * Returns a list of rules with this application ID.
     *
     * @param id the application ID to look up
     * @return collection of flow rules
     */
    @Deprecated
    Iterable<FlowRule> getFlowRulesById(ApplicationId id);

    /**
     * Returns a list of rules with this application ID.
     *
     * @param id the application ID to look up
     * @return collection of flow rules
     */
    Iterable<FlowEntry> getFlowEntriesById(ApplicationId id);

    /**
     * Returns a list of rules filtered by application and group id.
     * <p>
     * Note that the group concept here is simply a logical grouping of flows.
     * This is not the same as a group in the
     * {@link org.onosproject.net.group.GroupService}, and this method will not
     * return flows that are mapped to a particular {@link org.onosproject.net.group.Group}.
     * </p>
     *
     * @param appId the application ID to look up
     * @param groupId the group ID to look up
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
