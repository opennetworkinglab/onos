/*
 * Copyright 2019-present Open Networking Foundation
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
package org.onosproject.k8snode.api;

import com.google.common.testing.EqualsTester;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.ChassisId;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.k8snode.api.K8sNode.Type;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.provider.ProviderId;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertSame;
import static org.onosproject.k8snode.api.K8sNode.Type.MINION;
import static org.onosproject.k8snode.api.K8sNodeState.COMPLETE;
import static org.onosproject.k8snode.api.K8sNodeState.DEVICE_CREATED;
import static org.onosproject.k8snode.api.K8sNodeState.INIT;
import static org.onosproject.net.Device.Type.SWITCH;

/**
 * Unit test for DefaultK8sNode.
 */
public final class DefaultK8sNodeTest {

    private static final IpAddress TEST_IP = IpAddress.valueOf("10.100.0.3");

    private static final String CLUSTER_NAME = "cluster";
    private static final String HOSTNAME_1 = "hostname_1";
    private static final String HOSTNAME_2 = "hostname_2";
    private static final Device DEVICE_1 = createDevice(1);
    private static final Device DEVICE_2 = createDevice(2);
    private static final int SEGMENT_ID_1 = 1;
    private static final int SEGMENT_ID_2 = 2;

    private static final IpAddress MANAGEMENT_IP = IpAddress.valueOf("10.10.10.10");
    private static final IpAddress DATA_IP = IpAddress.valueOf("20.20.20.20");
    private static final IpAddress NODE_IP = IpAddress.valueOf("30.30.30.30");
    private static final MacAddress NODE_MAC = MacAddress.valueOf("fa:00:00:00:00:08");
    private static final K8sNodeInfo NODE_INFO = new K8sNodeInfo(NODE_IP, NODE_MAC);

    private static final String BRIDGE_INTF_1 = "eth1";
    private static final String BRIDGE_INTF_2 = "eth2";

    private static final IpAddress EXT_BRIDGE_IP_1 = IpAddress.valueOf("10.10.10.5");
    private static final IpAddress EXT_BRIDGE_IP_2 = IpAddress.valueOf("20.20.20.5");

    private static final IpAddress EXT_GATEWAY_IP_1 = IpAddress.valueOf("10.10.10.1");
    private static final IpAddress EXT_GATEWAY_IP_2 = IpAddress.valueOf("20.20.20.1");

    private static final String POD_CIDR_1 = "10.10.0.0/24";
    private static final String POD_CIDR_2 = "20.20.0.0/24";

    private K8sNode refNode;

    private static final K8sNode K8S_NODE_1 = createNode(
            CLUSTER_NAME,
            HOSTNAME_1,
            MINION,
            SEGMENT_ID_1,
            DEVICE_1,
            DEVICE_1,
            DEVICE_1,
            DEVICE_1,
            BRIDGE_INTF_1,
            TEST_IP,
            NODE_INFO,
            INIT,
            EXT_BRIDGE_IP_1,
            EXT_GATEWAY_IP_1,
            POD_CIDR_1);
    private static final K8sNode K8S_NODE_2 = createNode(
            CLUSTER_NAME,
            HOSTNAME_1,
            MINION,
            SEGMENT_ID_1,
            DEVICE_1,
            DEVICE_1,
            DEVICE_1,
            DEVICE_1,
            BRIDGE_INTF_1,
            TEST_IP,
            NODE_INFO,
            INIT,
            EXT_BRIDGE_IP_1,
            EXT_GATEWAY_IP_1,
            POD_CIDR_1);
    private static final K8sNode K8S_NODE_3 = createNode(
            CLUSTER_NAME,
            HOSTNAME_2,
            MINION,
            SEGMENT_ID_2,
            DEVICE_2,
            DEVICE_2,
            DEVICE_2,
            DEVICE_2,
            BRIDGE_INTF_2,
            TEST_IP,
            NODE_INFO,
            INIT,
            EXT_BRIDGE_IP_2,
            EXT_GATEWAY_IP_2,
            POD_CIDR_2);

    /**
     * Initial setup for this unit test.
     */
    @Before
    public void setUp() {
        refNode = DefaultK8sNode.builder()
                .clusterName(CLUSTER_NAME)
                .hostname(HOSTNAME_1)
                .type(MINION)
                .segmentId(SEGMENT_ID_1)
                .managementIp(MANAGEMENT_IP)
                .dataIp(DATA_IP)
                .nodeInfo(NODE_INFO)
                .intgBridge(DEVICE_1.id())
                .extBridge(DEVICE_1.id())
                .localBridge(DEVICE_1.id())
                .tunBridge(DEVICE_1.id())
                .extIntf(BRIDGE_INTF_1)
                .state(COMPLETE)
                .extBridgeIp(EXT_BRIDGE_IP_1)
                .extGatewayIp(EXT_GATEWAY_IP_1)
                .podCidr(POD_CIDR_1)
                .build();
    }

    /**
     * Checks equals method works as expected.
     */
    @Test
    public void testEquality() {
        new EqualsTester().addEqualityGroup(K8S_NODE_1, K8S_NODE_2)
                .addEqualityGroup(K8S_NODE_3)
                .testEquals();
    }

    /**
     * Test object construction.
     */
    @Test
    public void testConstruction() {
        checkCommonProperties(refNode);
        assertSame(refNode.state(), COMPLETE);
        assertEquals(refNode.intgBridge(), DEVICE_1.id());
        assertEquals(refNode.extBridge(), DEVICE_1.id());
        assertEquals(refNode.localBridge(), DEVICE_1.id());
        assertEquals(refNode.tunBridge(), DEVICE_1.id());
    }

    /**
     * Checks the functionality of update state method.
     */
    @Test
    public void testUpdateState() {
        K8sNode updatedNode = refNode.updateState(DEVICE_CREATED);

        checkCommonProperties(updatedNode);
        assertSame(updatedNode.state(), DEVICE_CREATED);
    }

    /**
     * Checks the functionality of from method.
     */
    @Test
    public void testFrom() {
        K8sNode updatedNode = DefaultK8sNode.from(refNode).build();

        assertEquals(updatedNode, refNode);
    }

    /**
     * Checks building a node without hostname fails with proper exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testBuildWithoutHostname() {
        DefaultK8sNode.builder()
                .type(MINION)
                .clusterName(CLUSTER_NAME)
                .segmentId(SEGMENT_ID_1)
                .intgBridge(DEVICE_1.id())
                .extBridge(DEVICE_1.id())
                .localBridge(DEVICE_1.id())
                .tunBridge(DEVICE_1.id())
                .extIntf(BRIDGE_INTF_1)
                .managementIp(TEST_IP)
                .dataIp(TEST_IP)
                .nodeInfo(NODE_INFO)
                .state(INIT)
                .extBridgeIp(EXT_BRIDGE_IP_1)
                .extGatewayIp(EXT_GATEWAY_IP_1)
                .podCidr(POD_CIDR_1)
                .build();
    }

    /**
     * Checks building a node without type fails with proper exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testBuildWithoutType() {
        DefaultK8sNode.builder()
                .hostname(HOSTNAME_1)
                .clusterName(CLUSTER_NAME)
                .segmentId(SEGMENT_ID_1)
                .intgBridge(DEVICE_1.id())
                .extBridge(DEVICE_1.id())
                .localBridge(DEVICE_1.id())
                .tunBridge(DEVICE_1.id())
                .extIntf(BRIDGE_INTF_1)
                .managementIp(TEST_IP)
                .dataIp(TEST_IP)
                .nodeInfo(NODE_INFO)
                .state(INIT)
                .extBridgeIp(EXT_BRIDGE_IP_1)
                .extGatewayIp(EXT_GATEWAY_IP_1)
                .podCidr(POD_CIDR_1)
                .build();
    }

    /**
     * Checks building a node without management IP address fails with
     * proper exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testBuildWithoutManagementIp() {
        DefaultK8sNode.builder()
                .clusterName(CLUSTER_NAME)
                .hostname(HOSTNAME_1)
                .type(MINION)
                .segmentId(SEGMENT_ID_1)
                .intgBridge(DEVICE_1.id())
                .extBridge(DEVICE_1.id())
                .localBridge(DEVICE_1.id())
                .tunBridge(DEVICE_1.id())
                .extIntf(BRIDGE_INTF_1)
                .dataIp(TEST_IP)
                .nodeInfo(NODE_INFO)
                .state(INIT)
                .extBridgeIp(EXT_BRIDGE_IP_1)
                .extGatewayIp(EXT_GATEWAY_IP_1)
                .podCidr(POD_CIDR_1)
                .build();
    }

    private void checkCommonProperties(K8sNode node) {
        assertEquals(node.clusterName(), CLUSTER_NAME);
        assertEquals(node.hostname(), HOSTNAME_1);
        assertEquals(node.type(), MINION);
        assertEquals(node.managementIp(), MANAGEMENT_IP);
        assertEquals(node.dataIp(), DATA_IP);
        assertEquals(node.nodeInfo(), NODE_INFO);
    }

    private static Device createDevice(long devIdNum) {
        return new DefaultDevice(new ProviderId("of", "foo"),
                DeviceId.deviceId(String.format("of:%016d", devIdNum)),
                SWITCH,
                "manufacturer",
                "hwVersion",
                "swVersion",
                "serialNumber",
                new ChassisId(1));
    }

    private static K8sNode createNode(String clusterName, String hostname, Type type,
                                      int segmentId, Device intgBridge, Device extBridge,
                                      Device localBridge, Device tunBridge, String bridgeIntf,
                                      IpAddress ipAddr, K8sNodeInfo info, K8sNodeState state,
                                      IpAddress extBridgeIp, IpAddress extGatewayIp,
                                      String podCidr) {
        return DefaultK8sNode.builder()
                .clusterName(clusterName)
                .hostname(hostname)
                .type(type)
                .segmentId(segmentId)
                .intgBridge(intgBridge.id())
                .extBridge(extBridge.id())
                .localBridge(localBridge.id())
                .tunBridge(tunBridge.id())
                .extIntf(bridgeIntf)
                .managementIp(ipAddr)
                .dataIp(ipAddr)
                .nodeInfo(info)
                .state(state)
                .extBridgeIp(extBridgeIp)
                .extGatewayIp(extGatewayIp)
                .podCidr(podCidr)
                .build();
    }
}
