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

package org.onosproject.ui.model;

import org.onlab.packet.IpAddress;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ClusterServiceAdapter;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.DefaultControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.region.RegionService;
import org.onosproject.ui.AbstractUiTest;
import org.onosproject.ui.UiTopoLayoutService;

/**
 * Base class for UI model unit tests.
 */
public class AbstractUiModelTest extends AbstractUiTest {

    /**
     * Returns canned results.
     * At some future point, we may make this "programmable", so that
     * it returns certain values based on element IDs etc.
     */
    protected static final ServiceBundle MOCK_SERVICES =
            new ServiceBundle() {
                @Override
                public UiTopoLayoutService layout() {
                    return null;
                }

                @Override
                public ClusterService cluster() {
                    return MOCK_CLUSTER;
                }

                @Override
                public MastershipService mastership() {
                    return null;
                }

                @Override
                public RegionService region() {
                    return null;
                }

                @Override
                public DeviceService device() {
                    return null;
                }

                @Override
                public LinkService link() {
                    return null;
                }

                @Override
                public HostService host() {
                    return null;
                }

                @Override
                public IntentService intent() {
                    return null;
                }

                @Override
                public FlowRuleService flow() {
                    return null;
                }
            };

    protected static final ClusterService MOCK_CLUSTER = new MockClusterService();

    protected static final NodeId NODE_ID = NodeId.nodeId("Node-1");
    protected static final IpAddress NODE_IP = IpAddress.valueOf("1.2.3.4");

    protected static final ControllerNode CNODE_1 =
            new DefaultControllerNode(NODE_ID, NODE_IP);

    private static class MockClusterService extends ClusterServiceAdapter {

        @Override
        public ControllerNode getNode(NodeId nodeId) {
            return CNODE_1;
        }

        @Override
        public ControllerNode.State getState(NodeId nodeId) {
            // For now, a hardcoded state of ACTIVE (but not READY)
            // irrespective of the node ID.
            return ControllerNode.State.ACTIVE;
        }
    }

}
