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

import com.google.common.collect.ImmutableList;
import com.google.common.testing.EqualsTester;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.ControllerInfo;

import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static org.onosproject.openstacknode.api.DpdkConfig.DatapathType.NETDEV;
import static org.onosproject.openstacknode.api.DpdkInterface.Type.DPDK_VHOST_USER;
import static org.onosproject.openstacknode.api.NodeState.COMPLETE;
import static org.onosproject.openstacknode.api.NodeState.DEVICE_CREATED;
import static org.onosproject.openstacknode.api.OpenstackAuth.Perspective.PUBLIC;
import static org.onosproject.openstacknode.api.OpenstackAuth.Protocol.HTTP;

/**
 * Unit tests for DefaultOpenstackNode.
 */
public final class DefaultOpenstackNodeTest extends OpenstackNodeTest {

    private static final IpAddress TEST_IP = IpAddress.valueOf("10.100.0.3");

    private static final String HOSTNAME_1 = "hostname_1";
    private static final String HOSTNAME_2 = "hostname_2";
    private static final Device DEVICE_1 = createDevice(1);
    private static final Device DEVICE_2 = createDevice(2);

    private static final IpAddress MANAGEMENT_IP = IpAddress.valueOf("10.10.10.10");
    private static final IpAddress DATA_IP = IpAddress.valueOf("20.20.20.20");

    private static final String VLAN_INTF = "eth0";
    private static final String UPLINK_PORT = "eth1";

    private static final String SSH_AUTH_ID = "admin";
    private static final String SSH_AUTH_PASSWORD = "nova";

    private static final String OS_AUTH_USERNAME = "admin";
    private static final String OS_AUTH_PASSWORD = "nova";
    private static final String OS_AUTH_PROJECT = "admin";
    private static final String OS_AUTH_VERSION = "v2.0";
    private static final OpenstackAuth.Protocol OS_AUTH_PROTOCOL = HTTP;
    private static final OpenstackAuth.Perspective OS_AUTH_PERSPECTIVE = PUBLIC;
    private static final String OS_ENDPOINT = "keystone:35357/v2.0";

    private static final String META_PROXY_SECRET = "nova";
    private static final boolean META_USE_SERVICE = true;
    private static final String META_IP = "30.30.30.30";
    private static final int META_PORT = 8775;

    private static final String DPDK_INTF_NAME = "dpdk1";
    private static final Long DPDK_INTF_MTU = 1500L;
    private static final String DPDK_INTF_DEV_NAME = "br-int";
    private static final String DPDK_INTF_PCI_ADDRESS = "0000:85:00.0";
    private static final DpdkInterface.Type DPDK_INTF_TYPE = DPDK_VHOST_USER;

    private static final DpdkConfig.DatapathType DPDK_DATAPATH_TYPE = NETDEV;
    private static final String DPDK_SOCKET_DIR = "/var/lib/libvirt/qemu";

    private static final String PHY_INTF_NETWORK = "mgmtnetwork";
    private static final String PHY_INTF_NAME = "eth3";

    private static final IpAddress CONTROLLER_IP = IpAddress.valueOf("40.40.40.40");
    private static final int CONTROLLER_PORT = 6653;
    private static final String TCP = "tcp";

    private static final OpenstackSshAuth SSH_AUTH = initSshAuth();
    private static final OpenstackAuth AUTH = initAuth();
    private static final KeystoneConfig KEYSTONE_CONFIG = initKeystoneConfig();
    private static final NeutronConfig NEUTRON_CONFIG = initNeutronConfig();
    private static final DpdkConfig DPDK_CONFIG = initDpdkConfig();
    private static final List<OpenstackPhyInterface> PHY_INTFS = initPhyIntfs();
    private static final List<ControllerInfo> CONTROLLERS = initControllers();

    private OpenstackNode refNode;

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
     * Initial setup for this unit test.
     */
    @Before
    public void setUp() {
        refNode = DefaultOpenstackNode.builder()
                .hostname(HOSTNAME_1)
                .type(OpenstackNode.NodeType.COMPUTE)
                .managementIp(MANAGEMENT_IP)
                .dataIp(DATA_IP)
                .intgBridge(DEVICE_1.id())
                .vlanIntf(VLAN_INTF)
                .uplinkPort(UPLINK_PORT)
                .state(NodeState.COMPLETE)
                .sshAuthInfo(SSH_AUTH)
                .keystoneConfig(KEYSTONE_CONFIG)
                .neutronConfig(NEUTRON_CONFIG)
                .dpdkConfig(DPDK_CONFIG)
                .phyIntfs(PHY_INTFS)
                .controllers(CONTROLLERS)
                .build();
    }

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
     * Test object construction.
     */
    @Test
    public void testConstruction() {
        checkCommonProperties(refNode);
        assertEquals(refNode.state(), COMPLETE);
        assertEquals(refNode.intgBridge(), DEVICE_1.id());
    }

    /**
     * Checks the functionality of update state method.
     */
    @Test
    public void testUpdateState() {
        OpenstackNode updatedNode = refNode.updateState(DEVICE_CREATED);

        checkCommonProperties(updatedNode);
        assertEquals(updatedNode.state(), DEVICE_CREATED);
    }

    /**
     * Checks the functionality of update int bridge method.
     */
    @Test
    public void testUpdateIntBridge() {
        OpenstackNode updatedNode = refNode.updateIntbridge(DeviceId.deviceId("br-tun"));

        checkCommonProperties(updatedNode);
        assertEquals(updatedNode.intgBridge(), DeviceId.deviceId("br-tun"));
    }

    /**
     * Checks the functionality of from method.
     */
    @Test
    public void testFrom() {
        OpenstackNode updatedNode = DefaultOpenstackNode.from(refNode).build();

        assertEquals(updatedNode, refNode);
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


    private static OpenstackSshAuth initSshAuth() {
        return DefaultOpenstackSshAuth.builder()
                .id(SSH_AUTH_ID)
                .password(SSH_AUTH_PASSWORD)
                .build();
    }

    private static OpenstackAuth initAuth() {
        return DefaultOpenstackAuth.builder()
                .username(OS_AUTH_USERNAME)
                .password(OS_AUTH_PASSWORD)
                .project(OS_AUTH_PROJECT)
                .protocol(OS_AUTH_PROTOCOL)
                .version(OS_AUTH_VERSION)
                .perspective(OS_AUTH_PERSPECTIVE)
                .build();
    }

    private static KeystoneConfig initKeystoneConfig() {
        return DefaultKeystoneConfig.builder()
                .authentication(AUTH)
                .endpoint(OS_ENDPOINT)
                .build();
    }

    private static NeutronConfig initNeutronConfig() {
        return DefaultNeutronConfig.builder()
                .metadataProxySecret(META_PROXY_SECRET)
                .novaMetadataIp(META_IP)
                .novaMetadataPort(META_PORT)
                .useMetadataProxy(META_USE_SERVICE)
                .build();
    }

    private static DpdkConfig initDpdkConfig() {
        DpdkInterface dpdkIntf = DefaultDpdkInterface.builder()
                .intf(DPDK_INTF_NAME)
                .deviceName(DPDK_INTF_DEV_NAME)
                .mtu(DPDK_INTF_MTU)
                .pciAddress(DPDK_INTF_PCI_ADDRESS)
                .type(DPDK_INTF_TYPE)
                .build();
        List<DpdkInterface> dpdkIntfs = ImmutableList.of(dpdkIntf);

        return DefaultDpdkConfig.builder()
                .dpdkIntfs(dpdkIntfs)
                .datapathType(DPDK_DATAPATH_TYPE)
                .socketDir(DPDK_SOCKET_DIR)
                .build();
    }

    private static List<OpenstackPhyInterface> initPhyIntfs() {
        OpenstackPhyInterface phyIntf = DefaultOpenstackPhyInterface.builder()
                .intf(PHY_INTF_NAME)
                .network(PHY_INTF_NETWORK)
                .build();

        return ImmutableList.of(phyIntf);
    }

    private static List<ControllerInfo> initControllers() {
        ControllerInfo controller = new ControllerInfo(CONTROLLER_IP, CONTROLLER_PORT, TCP);

        return ImmutableList.of(controller);
    }


    private void checkCommonProperties(OpenstackNode node) {
        assertEquals(node.hostname(), HOSTNAME_1);
        assertEquals(node.type(), OpenstackNode.NodeType.COMPUTE);
        assertEquals(node.managementIp(), MANAGEMENT_IP);
        assertEquals(node.dataIp(), DATA_IP);
        assertEquals(node.vlanIntf(), VLAN_INTF);
        assertEquals(node.uplinkPort(), UPLINK_PORT);
        assertEquals(node.sshAuthInfo(), SSH_AUTH);
        assertEquals(node.keystoneConfig(), KEYSTONE_CONFIG);
        assertEquals(node.neutronConfig(), NEUTRON_CONFIG);
        assertEquals(node.dpdkConfig(), DPDK_CONFIG);
        assertEquals(node.phyIntfs(), PHY_INTFS);
        assertEquals(node.controllers(), CONTROLLERS);
    }
}
