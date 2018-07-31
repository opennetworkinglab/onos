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

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.testing.EqualsTester;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
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
import org.onosproject.openstacknetworking.api.InstancePort;
import org.onosproject.openstacknetworking.impl.DefaultInstancePort;
import org.onosproject.openstacknetworking.web.OpenstackFloatingIpWebResourceTest;
import org.onosproject.openstacknetworking.web.OpenstackNetworkWebResourceTest;
import org.onosproject.openstacknode.api.NodeState;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.onosproject.openstacknode.api.OpenstackNodeTest;
import org.openstack4j.model.network.NetFloatingIP;
import org.openstack4j.model.network.Port;
import org.openstack4j.openstack.networking.domain.NeutronFloatingIP;
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
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.getGwByComputeDevId;

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

    @Before
    public void setUp() {

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
                OpenstackNetworkingUtil.jsonToModelEntity(floatingIpjsonStream1, NeutronFloatingIP.class);
        floatingIp2 = (NetFloatingIP)
                OpenstackNetworkingUtil.jsonToModelEntity(floatingIpjsonStream2, NeutronFloatingIP.class);
        floatingIp3 = (NetFloatingIP)
                OpenstackNetworkingUtil.jsonToModelEntity(floatingIpjsonStream3, NeutronFloatingIP.class);

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
                OpenstackNetworkingUtil.modelEntityToJson(floatingIp1, NeutronFloatingIP.class);
        InputStream is = IOUtils.toInputStream(floatingIpNode.toString(), StandardCharsets.UTF_8.name());
        NetFloatingIP floatingIp2 = (NetFloatingIP)
                OpenstackNetworkingUtil.jsonToModelEntity(is, NeutronFloatingIP.class);
        new EqualsTester().addEqualityGroup(floatingIp1, floatingIp2).testEquals();
    }

    /**
     * Tests the associatedFloatingIp method.
     * @throws NullPointerException
     */
    @Test
    public void testAsscoatedFloatingIp() throws NullPointerException {
        Set<NetFloatingIP> testSet = Sets.newHashSet();
        testSet.add(floatingIp1);
        testSet.add(floatingIp2);
        testSet.add(floatingIp3);

        NetFloatingIP floatingIp1 = OpenstackNetworkingUtil.associatedFloatingIp(instancePort1, testSet);
        NetFloatingIP floatingIp2 = OpenstackNetworkingUtil.associatedFloatingIp(instancePort2, testSet);

        assertEquals(floatingIp1, this.floatingIp1);
        assertEquals(floatingIp2, null);
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

        OpenstackNode gw = OpenstackNetworkingUtil.getGwByInstancePort(gws, instancePort1);

        assertEquals(genGateway(expectedGwIndex), gw);
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

        InputStream portJsonStream = OpenstackNetworkWebResourceTest.class
                .getResourceAsStream("openstack-port.json");

        InputStream sriovPortJsonStream1 = OpenstackNetworkWebResourceTest.class
                .getResourceAsStream("openstack-port-sriov1.json");
        InputStream sriovPortJsonStream2 = OpenstackNetworkWebResourceTest.class
                .getResourceAsStream("openstack-port-sriov2.json");
        InputStream sriovPortJsonStream3 = OpenstackNetworkWebResourceTest.class
                .getResourceAsStream("openstack-port-sriov3.json");

        openstackPort = (Port)
                OpenstackNetworkingUtil.jsonToModelEntity(portJsonStream, NeutronPort.class);

        openstackSriovPort1 = (Port)
                OpenstackNetworkingUtil.jsonToModelEntity(sriovPortJsonStream1, NeutronPort.class);
        openstackSriovPort2 = (Port)
                OpenstackNetworkingUtil.jsonToModelEntity(sriovPortJsonStream2, NeutronPort.class);
        openstackSriovPort3 = (Port)
                OpenstackNetworkingUtil.jsonToModelEntity(sriovPortJsonStream3, NeutronPort.class);

        String expectedIntfName1 = "enp5s8";
        String expectedIntfName2 = "enp5s8f3";

        assertNull(OpenstackNetworkingUtil.getIntfNameFromPciAddress(openstackPort));
        assertEquals(expectedIntfName1, OpenstackNetworkingUtil.getIntfNameFromPciAddress(openstackSriovPort1));
        assertEquals(expectedIntfName2, OpenstackNetworkingUtil.getIntfNameFromPciAddress(openstackSriovPort2));
        assertNull(OpenstackNetworkingUtil.getIntfNameFromPciAddress(openstackSriovPort3));
    }

    /**
     * Tests swapStaleLocation method.
     */
    @Test
    public void testSwapStaleLocation() {
        InstancePort swappedInstancePort =  OpenstackNetworkingUtil.swapStaleLocation(instancePort3);

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

    private OpenstackNode genGateway(int index) {

        Device intgBrg = InternalOpenstackNodeTest.createDevice(index);

        String hostname = "gateway-" + index;
        IpAddress ip = Ip4Address.valueOf("10.10.10." + index);
        NodeState state = NodeState.COMPLETE;
        String uplinkPort = "eth0";
        return InternalOpenstackNodeTest.createNode(hostname,
                OpenstackNode.NodeType.GATEWAY, intgBrg, ip, uplinkPort, state);

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
}
