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
package org.onosproject.openstacknode.impl;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.onlab.packet.IpAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.onosproject.openstacknode.api.OpenstackNodeAdminService;
import org.onosproject.openstacknode.api.OpenstackNodeListener;
import org.onosproject.openstacknode.api.OpenstackNodeService;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.onosproject.openstacknode.api.NodeState.COMPLETE;

/**
 * Test openstack node manager.
 */
public class TestOpenstackNodeManager implements OpenstackNodeService, OpenstackNodeAdminService {
        Map<String, OpenstackNode> osNodeMap = Maps.newHashMap();
        List<OpenstackNodeListener> listeners = Lists.newArrayList();

        @Override
        public Set<OpenstackNode> nodes() {
        return ImmutableSet.copyOf(osNodeMap.values());
    }

        @Override
        public Set<OpenstackNode> nodes(OpenstackNode.NodeType type) {
        return osNodeMap.values().stream()
                .filter(osNode -> osNode.type() == type)
                .collect(Collectors.toSet());
    }

        @Override
        public Set<OpenstackNode> completeNodes() {
        return osNodeMap.values().stream()
                .filter(osNode -> osNode.state() == COMPLETE)
                .collect(Collectors.toSet());
    }

        @Override
        public Set<OpenstackNode> completeNodes(OpenstackNode.NodeType type) {
        return osNodeMap.values().stream()
                .filter(osNode -> osNode.type() == type && osNode.state() == COMPLETE)
                .collect(Collectors.toSet());
    }

        @Override
        public OpenstackNode node(String hostname) {
        return osNodeMap.get(hostname);
    }

        @Override
        public OpenstackNode node(DeviceId deviceId) {
        return osNodeMap.values().stream()
                .filter(osNode -> Objects.equals(osNode.intgBridge(), deviceId) ||
                        Objects.equals(osNode.ovsdb(), deviceId))
                .findFirst().orElse(null);
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
        listeners.add(listener);
    }

        @Override
        public void removeListener(OpenstackNodeListener listener) {
        listeners.remove(listener);
    }

        @Override
        public void createNode(OpenstackNode osNode) {
        osNodeMap.put(osNode.hostname(), osNode);
    }

        @Override
        public void updateNode(OpenstackNode osNode) {
        osNodeMap.put(osNode.hostname(), osNode);
    }

        @Override
        public OpenstackNode removeNode(String hostname) {
        return null;
    }
}
