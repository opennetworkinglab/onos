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

package org.onosproject.incubator.net.virtual.impl;

import org.onosproject.core.ApplicationId;
import org.onosproject.event.AbstractListenerManager;
import org.onosproject.incubator.net.virtual.VirtualNetwork;
import org.onosproject.incubator.net.virtual.VirtualNetworkService;
import org.onosproject.incubator.net.virtual.VnetService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleEvent;
import org.onosproject.net.flow.FlowRuleListener;
import org.onosproject.net.flow.FlowRuleOperations;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TableStatisticsEntry;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Flow rule service implementation built on the virtual network service.
 */
public class VirtualNetworkFlowRuleManager extends AbstractListenerManager<FlowRuleEvent, FlowRuleListener>
        implements FlowRuleService, VnetService {

    private static final String NETWORK_NULL = "Network ID cannot be null";
    private final VirtualNetwork network;
    private final VirtualNetworkService manager;

    /**
     * Creates a new VirtualNetworkFlowRuleService object.
     *
     * @param virtualNetworkManager virtual network manager service
     * @param network               virtual network
     */
    public VirtualNetworkFlowRuleManager(VirtualNetworkService virtualNetworkManager, VirtualNetwork network) {
        checkNotNull(network, NETWORK_NULL);
        this.network = network;
        this.manager = virtualNetworkManager;
    }

    @Override
    public int getFlowRuleCount() {
        return 0;
    }

    @Override
    public Iterable<FlowEntry> getFlowEntries(DeviceId deviceId) {
        return null;
    }

    @Override
    public void applyFlowRules(FlowRule... flowRules) {

    }

    @Override
    public void purgeFlowRules(DeviceId deviceId) {

    }

    @Override
    public void removeFlowRules(FlowRule... flowRules) {

    }

    @Override
    public void removeFlowRulesById(ApplicationId appId) {

    }

    @Override
    public Iterable<FlowRule> getFlowRulesById(ApplicationId id) {
        return null;
    }

    @Override
    public Iterable<FlowEntry> getFlowEntriesById(ApplicationId id) {
        return null;
    }

    @Override
    public Iterable<FlowRule> getFlowRulesByGroupId(ApplicationId appId, short groupId) {
        return null;
    }

    @Override
    public void apply(FlowRuleOperations ops) {

    }

    @Override
    public Iterable<TableStatisticsEntry> getFlowTableStatistics(DeviceId deviceId) {
        return null;
    }

    @Override
    public VirtualNetwork network() {
        return network;
    }
}
