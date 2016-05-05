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

package org.onosproject.ui.impl.topo.model;

import com.google.common.collect.ImmutableSet;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ClusterServiceAdapter;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.mastership.MastershipService;
import org.onosproject.mastership.MastershipServiceAdapter;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.region.RegionService;
import org.onosproject.ui.impl.AbstractUiImplTest;
import org.onosproject.ui.model.ServiceBundle;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.onosproject.net.DeviceId.deviceId;

/**
 * Base class for model test classes.
 */
abstract class AbstractTopoModelTest extends AbstractUiImplTest {

    /**
     * Returns canned results.
     * At some future point, we may make this "programmable", so that
     * it returns certain values based on element IDs etc.
     */
    protected static final ServiceBundle MOCK_SERVICES =
            new ServiceBundle() {
                @Override
                public ClusterService cluster() {
                    return MOCK_CLUSTER;
                }

                @Override
                public MastershipService mastership() {
                    return MOCK_MASTER;
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

    private static final ClusterService MOCK_CLUSTER = new MockClusterService();
    private static final MastershipService MOCK_MASTER = new MockMasterService();
    // TODO: fill out as necessary

    /*
      Our mock environment:

      Three controllers: C1, C2, C3

      Nine devices: D1 .. D9

             D4 ---+              +--- D7
                   |              |
            D5 --- D1 --- D2 --- D3 --- D8
                   |              |
             D6 ---+              +--- D9

      Twelve hosts (two per D4 ... D9)  H41, H42, H51, H52, ...

      Regions:
        R1 : D1, D2, D3
        R2 : D4, D5, D6
        R3 : D7, D8, D9

      Mastership:
        C1 : D1, D2, D3
        C2 : D4, D5, D6
        C3 : D7, D8, D9
     */


    private static class MockClusterService extends ClusterServiceAdapter {
        private final Map<NodeId, ControllerNode.State> states = new HashMap<>();


        @Override
        public ControllerNode.State getState(NodeId nodeId) {
            // For now, a hardcoded state of ACTIVE (but not READY)
            // irrespective of the node ID.
            return ControllerNode.State.ACTIVE;
        }
    }

    protected static final DeviceId D1_ID = deviceId("D1");
    protected static final DeviceId D2_ID = deviceId("D2");
    protected static final DeviceId D3_ID = deviceId("D3");
    protected static final DeviceId D4_ID = deviceId("D4");
    protected static final DeviceId D5_ID = deviceId("D5");
    protected static final DeviceId D6_ID = deviceId("D6");
    protected static final DeviceId D7_ID = deviceId("D7");
    protected static final DeviceId D8_ID = deviceId("D8");
    protected static final DeviceId D9_ID = deviceId("D9");

    private static class MockMasterService extends MastershipServiceAdapter {
        @Override
        public Set<DeviceId> getDevicesOf(NodeId nodeId) {
            // For now, a hard coded set of two device IDs
            // irrespective of the node ID.
            return ImmutableSet.of(D1_ID, D2_ID);
        }
    }

}
