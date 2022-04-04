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
package org.onosproject.kubevirtnode.api;

import com.google.common.collect.ImmutableList;
import com.google.common.testing.EqualsTester;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;

import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static org.onosproject.kubevirtnode.api.KubevirtNodeState.DEVICE_CREATED;
import static org.onosproject.kubevirtnode.api.KubevirtNodeState.ON_BOARDED;

/**
 * Unit tests for Default Kubevirt Node.
 */
public final class DefaultKubevirtNodeTest extends KubevirtNodeTest {

    private static final IpAddress TEST_IP = IpAddress.valueOf("10.100.0.3");

    private static final String HOSTNAME_1 = "hostname_1";
    private static final String HOSTNAME_2 = "hostname_2";
    private static final String HOSTNAME_3 = "hostname_3";
    private static final Device DEVICE_1 = createDevice(1);
    private static final Device DEVICE_2 = createDevice(2);
    private static final Device DEVICE_3 = createDevice(3);

    private static final IpAddress MANAGEMENT_IP = IpAddress.valueOf("10.10.10.10");
    private static final IpAddress DATA_IP = IpAddress.valueOf("20.20.20.20");

    private static final String PHY_INTF_NETWORK = "mgmtnetwork";
    private static final String PHY_INTF_NAME = "eth3";
    private static final DeviceId BRIDGE = DeviceId.deviceId("phys1");

    private static final String GATEWAY_BRIDGE_NAME = "gateway";

    private static final List<KubevirtPhyInterface> PHY_INTFS = initPhyIntfs();

    private KubevirtNode refNode;

    private static final KubevirtNode KV_NODE_1 = createNode(
            HOSTNAME_1,
            KubevirtNode.Type.WORKER,
            DEVICE_1,
            TEST_IP,
            KubevirtNodeState.INIT);

    private static final KubevirtNode KV_NODE_2 = createNode(
            HOSTNAME_1,
            KubevirtNode.Type.WORKER,
            DEVICE_1,
            TEST_IP,
            KubevirtNodeState.COMPLETE);

    private static final KubevirtNode KV_NODE_3 = createNode(
            HOSTNAME_2,
            KubevirtNode.Type.WORKER,
            DEVICE_2,
            TEST_IP,
            KubevirtNodeState.INIT);

    /**
     * Initial setup for this unit test.
     */
    @Before
    public void setUp() {
        refNode = DefaultKubevirtNode.builder()
                .hostname(HOSTNAME_1)
                .type(KubevirtNode.Type.WORKER)
                .managementIp(MANAGEMENT_IP)
                .dataIp(DATA_IP)
                .intgBridge(DEVICE_1.id())
                .tunBridge(DEVICE_1.id())
                .state(KubevirtNodeState.COMPLETE)
                .phyIntfs(PHY_INTFS)
                .gatewayBridgeName(GATEWAY_BRIDGE_NAME)
                .build();
    }

    /**
     * Checks equals method works as expected.
     */
    @Test
    public void testEquality() {
        new EqualsTester().addEqualityGroup(KV_NODE_1, KV_NODE_2)
                .addEqualityGroup(KV_NODE_3)
                .testEquals();
    }

    /**
     * Test object construction.
     */
    @Test
    public void testConstruction() {
        checkCommonProperties(refNode);
        assertEquals(refNode.state(), KubevirtNodeState.COMPLETE);
        assertEquals(refNode.intgBridge(), DEVICE_1.id());
    }

    /**
     * Checks the functionality of update state method.
     */
    @Test
    public void testUpdateState() {
        KubevirtNode updatedNode = refNode.updateState(DEVICE_CREATED);

        checkCommonProperties(updatedNode);
        assertEquals(updatedNode.state(), DEVICE_CREATED);
    }

    /**
     * Checks the functionality of from method.
     */
    @Test
    public void testFrom() {
        KubevirtNode updatedNode = DefaultKubevirtNode.from(refNode).build();

        assertEquals(updatedNode, refNode);
    }

    /**
     * Checks building a node without hostname fails with proper exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testBuildWithoutHostname() {
        DefaultKubevirtNode.builder()
                .type(KubevirtNode.Type.WORKER)
                .intgBridge(DEVICE_1.id())
                .tunBridge(DEVICE_1.id())
                .managementIp(TEST_IP)
                .dataIp(TEST_IP)
                .state(KubevirtNodeState.INIT)
                .build();
    }

    /**
     * Checks building a node without type fails with proper exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testBuildWithoutType() {
        DefaultKubevirtNode.builder()
                .hostname(HOSTNAME_1)
                .intgBridge(DEVICE_1.id())
                .tunBridge(DEVICE_1.id())
                .managementIp(TEST_IP)
                .dataIp(TEST_IP)
                .state(KubevirtNodeState.INIT)
                .build();
    }

    /**
     * Checks building a node without management IP address fails with
     * proper exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testBuildWithoutManagementIp() {
        DefaultKubevirtNode.builder()
                .hostname(HOSTNAME_1)
                .type(KubevirtNode.Type.WORKER)
                .intgBridge(DEVICE_1.id())
                .tunBridge(DEVICE_1.id())
                .dataIp(TEST_IP)
                .state(KubevirtNodeState.INIT)
                .build();
    }

    /**
     * Checks building a gateway node.
     */
    @Test
    public void testGatewayBuild() {
        KubevirtNode node = DefaultKubevirtNode.builder()
                .hostname(HOSTNAME_3)
                .type(KubevirtNode.Type.GATEWAY)
                .intgBridge(DEVICE_2.id())
                .gatewayBridgeName(GATEWAY_BRIDGE_NAME)
                .managementIp(MANAGEMENT_IP)
                .dataIp(TEST_IP)
                .state(ON_BOARDED)
                .build();

        assertEquals(node.intgBridge(), DEVICE_2.id());
        assertEquals(node.gatewayBridgeName(), GATEWAY_BRIDGE_NAME);
    }

    private static List<KubevirtPhyInterface> initPhyIntfs() {
        KubevirtPhyInterface phyIntf = DefaultKubevirtPhyInterface.builder()
                .intf(PHY_INTF_NAME)
                .network(PHY_INTF_NETWORK)
                .physBridge(BRIDGE)
                .build();

        return ImmutableList.of(phyIntf);
    }

    private void checkCommonProperties(KubevirtNode node) {
        assertEquals(node.hostname(), HOSTNAME_1);
        assertEquals(node.type(), KubevirtNode.Type.WORKER);
        assertEquals(node.managementIp(), MANAGEMENT_IP);
        assertEquals(node.dataIp(), DATA_IP);
        assertEquals(node.phyIntfs(), PHY_INTFS);
    }
}
