/*
 * Copyright 2016-present Open Networking Foundation
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

import com.google.common.collect.Sets;
import org.onlab.packet.IpAddress;

/**
 * Test adapter for the ClusterMetadata service.
 */
public class ClusterMetadataServiceAdapter implements ClusterMetadataService {

    @Override
    public ClusterMetadata getClusterMetadata() {
        final NodeId nid = new NodeId("test-node");
        final IpAddress addr = IpAddress.valueOf(0);
        return new ClusterMetadata(
                "test-cluster",
                new DefaultControllerNode(nid, addr),
                Sets.newHashSet(),
                Sets.newHashSet(),
                "test-secret");
    }

    @Override
    public ControllerNode getLocalNode() {
        return null;
    }

    @Override
    public void addListener(ClusterMetadataEventListener listener) {
    }

    @Override
    public void removeListener(ClusterMetadataEventListener listener) {
    }

}
