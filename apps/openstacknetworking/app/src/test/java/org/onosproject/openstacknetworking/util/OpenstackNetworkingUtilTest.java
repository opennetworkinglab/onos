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
package org.onosproject.openstacknetworking.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.testing.EqualsTester;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultPort;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.DeviceServiceAdapter;
import org.onosproject.openstacknetworking.api.Constants;
import org.onosproject.openstacknetworking.api.InstancePort;
import org.onosproject.openstacknetworking.api.OpenstackNetworkService;
import org.onosproject.openstacknetworking.api.OpenstackRouterAdminService;
import org.onosproject.openstacknetworking.impl.DefaultInstancePort;
import org.onosproject.openstacknetworking.impl.OpenstackNetworkServiceAdapter;
import org.onosproject.openstacknetworking.impl.OpenstackRouterServiceAdapter;
import org.onosproject.openstacknetworking.impl.TestRouterInterface;
import org.onosproject.openstacknetworking.web.OpenstackFloatingIpWebResourceTest;
import org.onosproject.openstacknetworking.web.OpenstackNetworkWebResourceTest;
import org.onosproject.openstacknode.api.DefaultKeystoneConfig;
import org.onosproject.openstacknode.api.DefaultOpenstackAuth;
import org.onosproject.openstacknode.api.DefaultOpenstackNode;
import org.onosproject.openstacknode.api.KeystoneConfig;
import org.onosproject.openstacknode.api.NodeState;
import org.onosproject.openstacknode.api.OpenstackAuth;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.onosproject.openstacknode.api.OpenstackNodeTest;
import org.openstack4j.model.network.NetFloatingIP;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.Port;
import org.openstack4j.model.network.RouterInterface;
import org.openstack4j.openstack.networking.domain.NeutronFloatingIP;
import org.openstack4j.openstack.networking.domain.NeutronNetwork;
import org.openstack4j.openstack.networking.domain.NeutronPort;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.onosproject.net.AnnotationKeys.PORT_NAME;
import static org.onosproject.openstacknetworking.api.Constants.REST_UTF8;
import static org.onosproject.openstacknetworking.api.Constants.UNSUPPORTED_VENDOR;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.addRouterIface;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.associatedFloatingIp;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.checkActivationFlag;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.checkArpMode;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.getBroadcastAddr;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.getConnectedClient;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.getGwByComputeDevId;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.getGwByInstancePort;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.getIntfNameFromPciAddress;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.isAssociatedWithVM;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.jsonToModelEntity;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.modelEntityToJson;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.prettyJson;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.routerInterfacesEquals;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.swapStaleLocation;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.vnicType;

public final class OpenstackNetworkingUtilTest {

    private NetFloatingIP floatingIp1;
    private NetFloatingIP floatingIp2;
    private NetFloatingIP floatingIp3;
    private Port openstackPort;
    private Port openstackSriovPort1;
    private Port openstackSriovPort2;
    private Port openstackSriovPort3;
    private InstancePort instancePort1;
    private InstancePort instancePort2;
    private InstancePort instancePort3;
    private Map<String, RouterInterface> routerInterfaceMap = Maps.newHashMap();
    private OpenstackNode openstackControlNodeV2;
    private OpenstackNode openstackControlNodeV3;

    @Before
    public void setUp() throws IOException {

        instancePort1 = DefaultInstancePort.builder()
                .networkId("net-id-1")
                .portId("ce705c24-c1ef-408a-bda3-7bbd946164ab")
                .deviceId(DeviceId.deviceId("of:000000000000000a"))
                .portNumber(PortNumber.portNumber(1, "tap-1"))
                .ipAddress(IpAddress.valueOf("10.0.0.3"))
                .macAddress(MacAddress.valueOf("11:22:33:44:55:66"))
                .state(InstancePort.State.valueOf("ACTIVE"))
                .build();

        instancePort2 = DefaultInstancePort.builder()
                .networkId("net-id-2")
                .portId("port-id-2")
                .deviceId(DeviceId.deviceId("of:000000000000000b"))
                .portNumber(PortNumber.portNumber(2, "tap-2"))
                .ipAddress(IpAddress.valueOf("10.10.10.2"))
                .macAddress(MacAddress.valueOf("22:33:44:55:66:11"))
                .state(InstancePort.State.valueOf("ACTIVE"))
                .build();

        instancePort3 = DefaultInstancePort.builder()
                .networkId("net-id-3")
                .portId("port-id-3")
                .deviceId(DeviceId.deviceId("of:000000000000000c"))
                .oldDeviceId(DeviceId.deviceId("of:000000000000000d"))
                .oldPortNumber(PortNumber.portNumber(4, "tap-4"))
                .portNumber(PortNumber.portNumber(3, "tap-3"))
                .ipAddress(IpAddress.valueOf("10.10.10.3"))
                .macAddress(MacAddress.valueOf("33:44:55:66:11:22"))
                .state(InstancePort.State.valueOf("ACTIVE"))
                .build();

        InputStream floatingIpjsonStream1 = OpenstackFloatingIpWebResourceTest.class
                .getResourceAsStream("openstack-floatingip1.json");
        InputStream floatingIpjsonStream2 = OpenstackFloatingIpWebResourceTest.class
                .getResourceAsStream("openstack-floatingip2.json");
        InputStream floatingIpjsonStream3 = OpenstackFloatingIpWebResourceTest.class
                .getResourceAsStream("openstack-floatingip3.json");

        floatingIp1 = (NetFloatingIP)
                jsonToModelEntity(IOUtils.toString(floatingIpjsonStream1, REST_UTF8),
                        NeutronFloatingIP.class);
        floatingIp2 = (NetFloatingIP)
                jsonToModelEntity(IOUtils.toString(floatingIpjsonStream2, REST_UTF8),
                        NeutronFloatingIP.class);
        floatingIp3 = (NetFloatingIP)
                jsonToModelEntity(IOUtils.toString(floatingIpjsonStream3, REST_UTF8),
                        NeutronFloatingIP.class);

        InputStream portJsonStream = OpenstackNetworkWebResourceTest.class
                .getResourceAsStream("openstack-port.json");

        InputStream sriovPortJsonStream1 = OpenstackNetworkWebResourceTest.class
                .getResourceAsStream("openstack-port-sriov1.json");
        InputStream sriovPortJsonStream2 = OpenstackNetworkWebResourceTest.class
                .getResourceAsStream("openstack-port-sriov2.json");
        InputStream sriovPortJsonStream3 = OpenstackNetworkWebResourceTest.class
                .getResourceAsStream("openstack-port-sriov3.json");

        openstackPort = (Port)
                jsonToModelEntity(
                        IOUtils.toString(portJsonStream, REST_UTF8), NeutronPort.class);

        openstackSriovPort1 = (Port)
                jsonToModelEntity(IOUtils.toString(sriovPortJsonStream1, REST_UTF8),
                        NeutronPort.class);
        openstackSriovPort2 = (Port)
                jsonToModelEntity(IOUtils.toString(sriovPortJsonStream2, REST_UTF8),
                        NeutronPort.class);
        openstackSriovPort3 = (Port)
                jsonToModelEntity(IOUtils.toString(sriovPortJsonStream3, REST_UTF8),
                        NeutronPort.class);
    }

    @After
    public void tearDown() {
    }

    /**
     * Tests the floatingIp translation.
     */
    @Test
    public void testFloatingIp() throws IOException {
        ObjectNode floatingIpNode =
                modelEntityToJson(floatingIp1, NeutronFloatingIP.class);
        InputStream is = IOUtils.toInputStream(floatingIpNode.toString(), StandardCharsets.UTF_8.name());
        NetFloatingIP floatingIp2 = (NetFloatingIP)
                jsonToModelEntity(IOUtils.toString(is, REST_UTF8), NeutronFloatingIP.class);
        new EqualsTester().addEqualityGroup(floatingIp1, floatingIp2).testEquals();
    }

    /**
     * Tests the associatedFloatingIp method.
     */
    @Test
    public void testAsscoatedFloatingIp() throws NullPointerException {
        Set<NetFloatingIP> testSet = Sets.newHashSet();
        testSet.add(floatingIp1);
        testSet.add(floatingIp2);
        testSet.add(floatingIp3);

        NetFloatingIP floatingIp1 = associatedFloatingIp(instancePort1, testSet);
        NetFloatingIP floatingIp2 = associatedFloatingIp(instancePort2, testSet);

        assertEquals(floatingIp1, this.floatingIp1);
        assertEquals(floatingIp2, null);
    }

    /**
     * Tests the isAssociatedWithVM method.
     */
    @Test
    public void testIsAssociatedWithVM() {
        OpenstackNetworkService service = new TestOpenstackNetworkService();
        NetFloatingIP floatingIp4 = new NeutronFloatingIP().toBuilder().portId("portId4").build();

        assertFalse(isAssociatedWithVM(service, floatingIp4));
        assertFalse(isAssociatedWithVM(service, floatingIp3));
        assertTrue(isAssociatedWithVM(service, floatingIp1));
    }

    /**
     * Tests the isAssociatedWithVM method in case IllegalStateException is occurred.
     */
    @Test(expected = IllegalStateException.class)
    public void testIsAssociatedWithVMexceptionCase() {
        OpenstackNetworkService service = new TestOpenstackNetworkService();
        isAssociatedWithVM(service, floatingIp2);
    }


    /**
     * Tests the getGwByInstancePort method.
     */
    @Test
    public void testGetGwByInstancePort() {

        Set<OpenstackNode> gws = Sets.newConcurrentHashSet();
        gws.add(genGateway(1));
        gws.add(genGateway(2));
        gws.add(genGateway(3));

        int expectedGwIndex = 2;

        OpenstackNode gw = getGwByInstancePort(gws, instancePort1);

        assertEquals(genGateway(expectedGwIndex), gw);

        assertNull(getGwByInstancePort(gws, null));
    }

    /**
     * Tests the getGwByComputeDevId method.
     */
    @Test
    public void testGetGwByComputeDevId() {
        Set<OpenstackNode> gws = Sets.newConcurrentHashSet();

        OpenstackNode nullGw = getGwByComputeDevId(gws, genDeviceId(1));
        assertNull(nullGw);

        gws.add(genGateway(1));
        gws.add(genGateway(2));
        gws.add(genGateway(3));

        Set<OpenstackNode> cloneOfGws = ImmutableSet.copyOf(gws);

        Map<String, Integer> gwCountMap = Maps.newConcurrentMap();
        int numOfDev = 99;

        for (int i = 1; i < 1 + numOfDev; i++) {
            OpenstackNode gw = getGwByComputeDevId(gws, genDeviceId(i));

            if (gwCountMap.get(gw.hostname()) == null) {
                gwCountMap.put(gw.hostname(), 1);
            } else {
                gwCountMap.compute(gw.hostname(), (k, v) -> v + 1);
            }

            new EqualsTester().addEqualityGroup(
                    getGwByComputeDevId(gws, genDeviceId(i)),
                    getGwByComputeDevId(cloneOfGws, genDeviceId(i)))
                    .testEquals();
        }

        int sum = gwCountMap.values().stream().mapToInt(Integer::intValue).sum();
        assertEquals(numOfDev, sum);
    }

    /**
     * Tests getIntfNameFromPciAddress method.
     */
    @Test
    public void testGetIntfNameFromPciAddress() {

        String expectedIntfName1 = "enp5s8";
        String expectedIntfName2 = "enp5s8f3";

        assertNull(getIntfNameFromPciAddress(openstackPort));
        assertEquals(expectedIntfName1, getIntfNameFromPciAddress(openstackSriovPort1));
        assertEquals(expectedIntfName2, getIntfNameFromPciAddress(openstackSriovPort2));
        assertEquals(UNSUPPORTED_VENDOR, getIntfNameFromPciAddress(openstackSriovPort3));
    }

    /**
     * Tests swapStaleLocation method.
     */
    @Test
    public void testSwapStaleLocation() {
        InstancePort swappedInstancePort =  swapStaleLocation(instancePort3);

        assertEquals(instancePort3.oldDeviceId(), swappedInstancePort.deviceId());
        assertEquals(instancePort3.oldPortNumber(), swappedInstancePort.portNumber());

    }

    /**
     * Tests hasIntfAleadyInDevice method.
     */
    @Test
    public void testHasIntfAleadyInDevice() {
        DeviceService deviceService = new TestDeviceService();
        assertTrue(OpenstackNetworkingUtil.hasIntfAleadyInDevice(DeviceId.deviceId("deviceId"),
                "port1", deviceService));
        assertTrue(OpenstackNetworkingUtil.hasIntfAleadyInDevice(DeviceId.deviceId("deviceId"),
                "port2", deviceService));
        assertTrue(OpenstackNetworkingUtil.hasIntfAleadyInDevice(DeviceId.deviceId("deviceId"),
                "port3", deviceService));
        assertFalse(OpenstackNetworkingUtil.hasIntfAleadyInDevice(DeviceId.deviceId("deviceId"),
                "port4", deviceService));
    }

    /**
     * Tests addRouterIface method.
     */
    @Test
    public void testAddRouterIface() {
        OpenstackRouterAdminService service = new TestOpenstackRouterAdminService();

        addRouterIface(openstackPort, service);
        RouterInterface initialRouterInterface = new TestRouterInterface(openstackPort.getDeviceId(),
                openstackPort.getFixedIps().stream().findAny().get().getSubnetId(),
                openstackPort.getId(),
                openstackPort.getTenantId());

        assertTrue(routerInterfacesEquals(initialRouterInterface, service.routerInterface(openstackPort.getId())));

        addRouterIface(openstackSriovPort1, service);
        RouterInterface updatedInitialRouterInterface = new TestRouterInterface(openstackSriovPort1.getDeviceId(),
                openstackSriovPort1.getFixedIps().stream().findAny().get().getSubnetId(),
                openstackSriovPort1.getId(),
                openstackSriovPort1.getTenantId());

        assertTrue(routerInterfacesEquals(
                updatedInitialRouterInterface, service.routerInterface(openstackSriovPort1.getId())));
    }

    /**
     * Util for generating dummy gateway node.
     *
     * @param index dummy gateway number
     * @return dummy gateway node
     */
    public OpenstackNode genGateway(int index) {
        Device intgBrg = InternalOpenstackNodeTest.createDevice(index);

        String hostname = "gateway-" + index;
        IpAddress ip = Ip4Address.valueOf("10.10.10." + index);
        NodeState state = NodeState.COMPLETE;
        String uplinkPort = "eth0";
        return InternalOpenstackNodeTest.createNode(hostname,
                OpenstackNode.NodeType.GATEWAY, intgBrg, ip, uplinkPort, state);
    }

    /**
     * Tests the testPrettyJson method.
     */
    @Test
    public void testPrettyJson() {
        String string = prettyJson(new ObjectMapper(), "{\"json\":\"json\"}");
        String prettyJsonString = "{\n  \"json\" : \"json\"\n}";
        assertEquals(string, prettyJsonString);

        assertNull(prettyJson(new ObjectMapper(), "{\"json\":\"json\""));
        assertNull(prettyJson(new ObjectMapper(), "{\"json\"\"json\"}"));
    }

    /**
     * Tests the checkArpMode method.
     */
    @Test
    public void testCheckArpMode() {
        assertFalse(checkArpMode(null));
        assertTrue(checkArpMode("proxy"));
        assertTrue(checkArpMode("broadcast"));
    }

    /**
     * Tests the getConnectedClient method.
     */
    @Ignore
    @Test
    public void testGetConnectedClient() {
        OpenstackNode.Builder osNodeBuilderV2 = DefaultOpenstackNode.builder();
        OpenstackAuth.Builder osNodeAuthBuilderV2 = DefaultOpenstackAuth.builder()
                .version("v2.0")
                .protocol(OpenstackAuth.Protocol.HTTP)
                .project("admin")
                .username("admin")
                .password("password")
                .perspective(OpenstackAuth.Perspective.PUBLIC);

        String endpointV2 = "1.1.1.1:35357/v2.0";

        KeystoneConfig keystoneConfigV2 = DefaultKeystoneConfig.builder()
                .authentication(osNodeAuthBuilderV2.build())
                .endpoint(endpointV2)
                .build();

        openstackControlNodeV2 = osNodeBuilderV2.hostname("controllerv2")
                .type(OpenstackNode.NodeType.CONTROLLER)
                .managementIp(IpAddress.valueOf("1.1.1.1"))
                .keystoneConfig(keystoneConfigV2)
                .state(NodeState.COMPLETE)
                .build();

        OpenstackNode.Builder osNodeBuilderV3 = DefaultOpenstackNode.builder();
        OpenstackAuth.Builder osNodeAuthBuilderV3 = DefaultOpenstackAuth.builder()
                .version("v2")
                .protocol(OpenstackAuth.Protocol.HTTP)
                .project("admin")
                .username("admin")
                .password("password")
                .perspective(OpenstackAuth.Perspective.PUBLIC);

        String endpointV3 = "2.2.2.2:80/v3";

        KeystoneConfig keystoneConfigV3 = DefaultKeystoneConfig.builder()
                .authentication(osNodeAuthBuilderV3.build())
                .endpoint(endpointV3)
                .build();

        openstackControlNodeV3 = osNodeBuilderV3.hostname("controllerv3")
                .type(OpenstackNode.NodeType.CONTROLLER)
                .managementIp(IpAddress.valueOf("2.2.2.2"))
                .keystoneConfig(keystoneConfigV3)
                .state(NodeState.COMPLETE)
                .build();

        getConnectedClient(openstackControlNodeV2);
        getConnectedClient(openstackControlNodeV3);
    }

    /**
     * Tests the vnicType method.
     */
    @Test
    public void testVnicType() {
        String portNameNormalTap = "tap123456789ab";
        String portNameNormalVhu = "tap123456789ab";
        String portNameNormalCavium = "enp1f2s3";
        String portNameUnsupported = "123456789ab";

        assertEquals(vnicType(portNameNormalTap), Constants.VnicType.NORMAL);
        assertEquals(vnicType(portNameNormalVhu), Constants.VnicType.NORMAL);
        assertEquals(vnicType(portNameNormalCavium), Constants.VnicType.DIRECT);
        assertEquals(vnicType(portNameUnsupported), Constants.VnicType.UNSUPPORTED);
    }

    /**
     * Tests the checkActivationFlag method.
     */
    @Test
    public void testCheckActivationFlag() {
        assertFalse(checkActivationFlag("disable"));
        assertTrue(checkActivationFlag("enable"));
    }

    /**
     * Tests the checkActivationFlag method with incorrect input parameters.
     */
    @Test (expected = IllegalArgumentException.class)
    public void testCheckActivationFlagWithException() {
        checkActivationFlag("test");
        checkActivationFlag(null);
    }

    /**
     * Tests the getBroadcastAddr method.
     */
    @Test
    public void testGetBroadcastAddr() {
        String ipAddr = "192.168.10.35";
        int prefix1 = 24;
        String broadcast1 = getBroadcastAddr(ipAddr, prefix1);
        assertEquals(broadcast1, "192.168.10.255");

        int prefix2 = 28;
        String broadcast2 = getBroadcastAddr(ipAddr, prefix2);
        assertEquals(broadcast2, "192.168.10.47");

        int prefix3 = 32;
        String broadcast3 = getBroadcastAddr(ipAddr, prefix3);
        assertEquals(broadcast3, "192.168.10.35");

        int prefix4 = 16;
        String broadcast4 = getBroadcastAddr(ipAddr, prefix4);
        assertEquals(broadcast4, "192.168.255.255");
    }

    private DeviceId genDeviceId(int index) {
        return DeviceId.deviceId("of:compute-" + index);
    }

    protected class InternalOpenstackNodeTest extends OpenstackNodeTest {
    }

    /**
     * Mocks the DeviceService.
     */
    private class TestDeviceService extends DeviceServiceAdapter {
        @Override
        public List<org.onosproject.net.Port> getPorts(DeviceId deviceId) {
            List<org.onosproject.net.Port> ports = Lists.newArrayList();
            DefaultAnnotations.Builder annotations1 = DefaultAnnotations.builder()
                    .set(PORT_NAME, "port1");
            DefaultAnnotations.Builder annotations2 = DefaultAnnotations.builder()
                    .set(PORT_NAME, "port2");
            DefaultAnnotations.Builder annotations3 = DefaultAnnotations.builder()
                    .set(PORT_NAME, "port3");

            org.onosproject.net.Port port1 = new DefaultPort(null, PortNumber.portNumber(1),
                    true, annotations1.build());
            org.onosproject.net.Port port2 = new DefaultPort(null, PortNumber.portNumber(2),
                    true, annotations2.build());
            org.onosproject.net.Port port3 = new DefaultPort(null, PortNumber.portNumber(3),
                    true, annotations3.build());

            ports.add(port1);
            ports.add(port2);
            ports.add(port3);

            return ports;
        }
    }

    private class TestOpenstackNetworkService extends OpenstackNetworkServiceAdapter {
        @Override
        public Network network(String networkId) {
            if (networkId.equals(openstackSriovPort1.getNetworkId())) {
                return new NeutronNetwork().toBuilder().name("network").build();
            } else {
                return null;
            }
        }

        @Override
        public Port port(String portId) {
            if (portId.equals(floatingIp1.getPortId())) {
                return openstackSriovPort1;
            } else if (portId.equals(floatingIp2.getPortId())) {
                return openstackSriovPort2;
            } else if (portId.equals("portId4")) {
                return new NeutronPort().toBuilder().name("osPort4").build();
            } else {
                return null;
            }
        }
    }

    private class TestOpenstackRouterAdminService extends OpenstackRouterServiceAdapter {
        @Override
        public void addRouterInterface(RouterInterface osRouterIface) {
            routerInterfaceMap.put(osRouterIface.getPortId(), osRouterIface);
        }

        @Override
        public void updateRouterInterface(RouterInterface osRouterIface) {
            routerInterfaceMap.put(osRouterIface.getPortId(), osRouterIface);
        }

        @Override
        public RouterInterface routerInterface(String osRouterIfaceId) {
            return routerInterfaceMap.get(osRouterIfaceId);
        }
    }
}
