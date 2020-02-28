/*
 * Copyright 2020-present Open Networking Foundation
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

package org.onosproject.t3.api;

import com.google.common.collect.ImmutableSet;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.FlowEntry;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents Network Information Base (NIB) for flows
 * and supports alternative functions to
 * {@link org.onosproject.net.flow.FlowRuleService} for offline data.
 */
public class FlowNib extends AbstractNib {

    // TODO with method optimization, store into subdivided structures at the first load
    private Set<FlowEntry> flows;

    // use the singleton helper to create the instance
    protected FlowNib() {
    }

    /**
     * Sets a set of flows.
     *
     * @param flows flow set
     */
    public void setFlows(Set<FlowEntry> flows) {
        this.flows = flows;
    }

    /**
     * Returns the set of flows.
     *
     * @return flow set
     */
    public Set<FlowEntry> getFlows() {
        return ImmutableSet.copyOf(flows);
    }

    /**
     * Returns a list of rules filtered by device id and flow state.
     *
     * @param deviceId the device id to lookup
     * @param flowState the flow state to lookup
     * @return collection of flow entries
     */
    public Iterable<FlowEntry> getFlowEntriesByState(DeviceId deviceId, FlowEntry.FlowEntryState flowState) {
        Set<FlowEntry> flowsFiltered = flows.stream()
                .filter(flow -> flow.state() == flowState
                        && flow.deviceId().equals(deviceId))
                .collect(Collectors.toSet());
        return flowsFiltered != null ? ImmutableSet.copyOf(flowsFiltered) : ImmutableSet.of();
    }

    /**
     * Returns the singleton instance of flows NIB.
     *
     * @return instance of flows NIB
     */
    public static FlowNib getInstance() {
        return SingletonHelper.INSTANCE;
    }

    private static class SingletonHelper {
        private static final FlowNib INSTANCE = new FlowNib();
    }

}
