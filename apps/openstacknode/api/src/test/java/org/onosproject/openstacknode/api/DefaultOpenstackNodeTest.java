/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.openstacknode.api;

import com.google.common.testing.EqualsTester;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onosproject.net.Device;

/**
 * Unit tests for DefaultOpenstackNode.
 */
public class DefaultOpenstackNodeTest extends OpenstackNodeTest {

    private static final IpAddress TEST_IP = IpAddress.valueOf("10.100.0.3");

    private static final String HOSTNAME_1 = "hostname_1";
    private static final String HOSTNAME_2 = "hostname_2";
    private static final Device DEVICE_1 = createDevice(1);
    private static final Device DEVICE_2 = createDevice(2);
    private static final OpenstackNode OS_NODE_1 = createNode(
            HOSTNAME_1,
            OpenstackNode.NodeType.COMPUTE,
            DEVICE_1,
            TEST_IP,
            NodeState.INIT);
    private static final OpenstackNode OS_NODE_2 = createNode(
            HOSTNAME_1,
            OpenstackNode.NodeType.COMPUTE,
            DEVICE_1,
            TEST_IP,
            NodeState.COMPLETE);
    private static final OpenstackNode OS_NODE_3 = createNode(
            HOSTNAME_2,
            OpenstackNode.NodeType.COMPUTE,
            DEVICE_2,
            TEST_IP,
            NodeState.INIT);

    /**
     * Checks equals method works as expected.
     */
    @Test
    public void testEquality() {
        new EqualsTester().addEqualityGroup(OS_NODE_1, OS_NODE_2)
                .addEqualityGroup(OS_NODE_3)
                .testEquals();
    }

    /**
     * Checks building a node without hostname fails with proper exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testBuildWithoutHostname() {
        DefaultOpenstackNode.builder()
                .type(OpenstackNode.NodeType.COMPUTE)
                .intgBridge(DEVICE_1.id())
                .managementIp(TEST_IP)
                .dataIp(TEST_IP)
                .state(NodeState.INIT)
                .build();
    }

    /**
     * Checks building a node without type fails with proper exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testBuildWithoutType() {
        DefaultOpenstackNode.builder()
                .hostname(HOSTNAME_1)
                .intgBridge(DEVICE_1.id())
                .managementIp(TEST_IP)
                .dataIp(TEST_IP)
                .state(NodeState.INIT)
                .build();
    }

    /**
     * Checks building a node without management IP address fails with
     * proper exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testBuildWithoutManagementIp() {
        DefaultOpenstackNode.builder()
                .hostname(HOSTNAME_1)
                .type(OpenstackNode.NodeType.COMPUTE)
                .intgBridge(DEVICE_1.id())
                .dataIp(TEST_IP)
                .state(NodeState.INIT)
                .build();
    }

    /**
     * Checks building a node without data IP nor VLAN interface name
     * fails with proper exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testBuildWithoutDataIpNorVlanIntf() {
        DefaultOpenstackNode.builder()
                .hostname(HOSTNAME_1)
                .type(OpenstackNode.NodeType.COMPUTE)
                .intgBridge(DEVICE_1.id())
                .state(NodeState.INIT)
                .build();
    }
}
