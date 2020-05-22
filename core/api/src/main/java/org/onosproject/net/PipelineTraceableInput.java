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

package org.onosproject.net;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.onosproject.core.GroupId;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.group.Group;

import java.util.List;
import java.util.Map;

/**
 * Represents the input of the pipeline traceable processing.
 */
public class PipelineTraceableInput {

    // Input state for the traceable behavior
    TrafficSelector ingressPacket;
    ConnectPoint ingressPort;
    // List here all possible device state using
    // possibly an optimized reference
    List<FlowEntry> flows = Lists.newArrayList();
    Map<GroupId, Group> groups = Maps.newHashMap();

    public PipelineTraceableInput(TrafficSelector ingressPacket, ConnectPoint ingressPort,
                                  List<DataPlaneEntity> deviceState) {
        this.ingressPacket = ingressPacket;
        this.ingressPort = ingressPort;
        processDeviceState(deviceState);
    }

    // Init internal device state (flows, groups, etc)
    private void processDeviceState(List<DataPlaneEntity> deviceState) {
        deviceState.forEach(entity -> {
            if (entity.getType() == DataPlaneEntity.Type.FLOWRULE) {
                flows.add(entity.getFlowEntry());
            } else if (entity.getType() == DataPlaneEntity.Type.GROUP) {
                groups.put(entity.getGroupEntry().id(), entity.getGroupEntry());
            }
        });
    }

    /**
     * Getter for the ingress packet.
     *
     * @return the ingress packet
     */
    public TrafficSelector ingressPacket() {
        return ingressPacket;
    }

    /**
     * Getter for the ingress port.
     *
     * @return the ingress port
     */
    public ConnectPoint ingressPort() {
        return ingressPort;
    }

    /**
     * Getter for the flows.
     *
     * @return the flows
     */
    public List<FlowEntry> flows() {
        return flows;
    }

    /**
     * Getter for the groups.
     *
     * @return the groups
     */
    public Map<GroupId, Group> groups() {
        return groups;
    }

    /**
     * Returns the group associated with the given group id.
     *
     * @param groupId the group id
     * @return the group, otherwise null.
     */
    public Group getGroup(GroupId groupId) {
        return groups.get(groupId);
    }

}
