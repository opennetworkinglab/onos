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
package org.onosproject.cluster;

import java.util.Set;

import org.joda.time.DateTime;
import org.onlab.packet.IpAddress;

import com.google.common.collect.ImmutableSet;

/**
 * Test adapter for the cluster service.
 */
public class ClusterServiceAdapter implements ClusterService {
    ControllerNode local = new DefaultControllerNode(new NodeId("local"),
            IpAddress.valueOf("127.0.0.1"));

    @Override
    public ControllerNode getLocalNode() {
        return local;
    }

    @Override
    public Set<ControllerNode> getNodes() {
        return ImmutableSet.of(local);
    }

    @Override
    public ControllerNode getNode(NodeId nodeId) {
        return null;
    }

    @Override
    public ControllerNode.State getState(NodeId nodeId) {
        return null;
    }

    @Override
    public DateTime getLastUpdated(NodeId nodeId) {
        return null;
    }

    @Override
    public void addListener(ClusterEventListener listener) {
    }

    @Override
    public void removeListener(ClusterEventListener listener) {
    }
}
