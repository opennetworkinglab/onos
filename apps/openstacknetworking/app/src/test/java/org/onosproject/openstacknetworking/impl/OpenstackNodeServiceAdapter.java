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
package org.onosproject.openstacknetworking.impl;

import com.google.common.collect.ImmutableSet;
import org.onlab.packet.IpAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.onosproject.openstacknode.api.OpenstackNodeListener;
import org.onosproject.openstacknode.api.OpenstackNodeService;

import java.util.Set;

/**
 * Test adapter for OpenstackNodeService.
 */
public class OpenstackNodeServiceAdapter implements OpenstackNodeService {
    @Override
    public Set<OpenstackNode> nodes() {
        return ImmutableSet.of();
    }

    @Override
    public Set<OpenstackNode> nodes(OpenstackNode.NodeType type) {
        return ImmutableSet.of();
    }

    @Override
    public Set<OpenstackNode> completeNodes() {
        return ImmutableSet.of();
    }

    @Override
    public Set<OpenstackNode> completeNodes(OpenstackNode.NodeType type) {
        return ImmutableSet.of();
    }

    @Override
    public OpenstackNode node(String hostname) {
        return null;
    }

    @Override
    public OpenstackNode node(DeviceId deviceId) {
        return null;
    }

    @Override
    public OpenstackNode node(IpAddress mgmtIp) {
        return null;
    }

    @Override
    public void addVfPort(OpenstackNode osNode, String portName) {
    }

    @Override
    public void removeVfPort(OpenstackNode osNode, String portName) {
    }


    @Override
    public void addListener(OpenstackNodeListener listener) {

    }

    @Override
    public void removeListener(OpenstackNodeListener listener) {

    }
}
