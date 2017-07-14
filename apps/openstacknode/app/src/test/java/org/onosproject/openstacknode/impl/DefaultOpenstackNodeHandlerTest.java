/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.openstacknode.impl;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onlab.packet.ChassisId;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.cfg.ComponentConfigAdapter;
import org.onosproject.cluster.ClusterServiceAdapter;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.DefaultControllerNode;
import org.onosproject.cluster.LeadershipServiceAdapter;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.core.GroupId;
import org.onosproject.net.Annotations;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.DefaultPort;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.BridgeConfig;
import org.onosproject.net.behaviour.BridgeDescription;
import org.onosproject.net.behaviour.BridgeName;
import org.onosproject.net.behaviour.ControllerInfo;
import org.onosproject.net.behaviour.DefaultBridgeDescription;
import org.onosproject.net.behaviour.ExtensionTreatmentResolver;
import org.onosproject.net.behaviour.InterfaceConfig;
import org.onosproject.net.behaviour.PatchDescription;
import org.onosproject.net.behaviour.TunnelDescription;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.device.DeviceAdminService;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceInterfaceDescription;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceServiceAdapter;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.driver.Behaviour;
import org.onosproject.net.driver.DriverData;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.flow.instructions.ExtensionPropertyException;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.flow.instructions.ExtensionTreatmentType;
import org.onosproject.net.group.DefaultGroup;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupBuckets;
import org.onosproject.net.group.GroupDescription;
import org.onosproject.net.group.GroupEvent;
import org.onosproject.net.group.GroupKey;
import org.onosproject.net.group.GroupListener;
import org.onosproject.net.group.GroupService;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.openstacknode.api.NodeState;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.onosproject.openstacknode.api.OpenstackNodeAdminService;
import org.onosproject.openstacknode.api.OpenstackNodeListener;
import org.onosproject.openstacknode.api.OpenstackNodeService;
import org.onosproject.ovsdb.controller.OvsdbClientService;
import org.onosproject.ovsdb.controller.OvsdbController;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.easymock.EasyMock.*;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.onosproject.net.AnnotationKeys.PORT_NAME;
import static org.onosproject.net.Device.Type.CONTROLLER;
import static org.onosproject.net.Device.Type.SWITCH;
import static org.onosproject.net.device.DeviceEvent.Type.*;
import static org.onosproject.openstacknode.api.Constants.*;
import static org.onosproject.openstacknode.api.NodeState.*;
import static org.onosproject.openstacknode.api.OpenstackNode.NodeType.COMPUTE;
import static org.onosproject.openstacknode.api.OpenstackNode.NodeType.GATEWAY;

/**
 * Unit test for DefaultOpenstackNodeHandler.
 */
public class DefaultOpenstackNodeHandlerTest {

    private static final ApplicationId TEST_APP_ID = new DefaultApplicationId(1, "test");
    private static final String ERR_STATE_NOT_MATCH = "Node state did not match";
    private static final NodeId LOCAL_NODE_ID = new NodeId("local");
    private static final ControllerNode LOCAL_CTRL =
            new DefaultControllerNode(LOCAL_NODE_ID, IpAddress.valueOf("127.0.0.1"));

    private static final BridgeDescription ROUT_BRIDGE = DefaultBridgeDescription.builder()
            .name(ROUTER_BRIDGE)
            .failMode(BridgeDescription.FailMode.SECURE)
            .disableInBand()
            .build();

    private static final PortDescription PATCH_ROUT = new DefaultPortDescription(
            PortNumber.portNumber(1),
            true,
            DefaultAnnotations.builder()
                    .set(PORT_NAME, PATCH_ROUT_BRIDGE)
                    .build()
    );

    private static final String COMPUTE_1_HOSTNAME = "compute_1";
    private static final String COMPUTE_2_HOSTNAME = "compute_2";
    private static final String COMPUTE_3_HOSTNAME = "compute_3";
    private static final String COMPUTE_4_HOSTNAME = "compute_4";
    private static final String GATEWAY_1_HOSTNAME = "gateway_1";
    private static final String GATEWAY_2_HOSTNAME = "gateway_2";
    private static final String GATEWAY_3_HOSTNAME = "gateway_3";
    private static final String GATEWAY_4_HOSTNAME = "gateway_4";

    private static final IpAddress COMPUTE_1_IP = IpAddress.valueOf("10.100.0.1");
    private static final IpAddress COMPUTE_2_IP = IpAddress.valueOf("10.100.0.2");
    private static final IpAddress COMPUTE_3_IP = IpAddress.valueOf("10.100.0.3");
    private static final IpAddress COMPUTE_4_IP = IpAddress.valueOf("10.100.0.4");
    private static final IpAddress GATEWAY_1_IP = IpAddress.valueOf("10.100.0.5");
    private static final IpAddress GATEWAY_2_IP = IpAddress.valueOf("10.100.0.6");
    private static final IpAddress GATEWAY_3_IP = IpAddress.valueOf("10.100.0.7");
    private static final IpAddress GATEWAY_4_IP = IpAddress.valueOf("10.100.0.8");

    private static final Device COMPUTE_1_INTG_DEVICE = createOpenFlowDevice(1, INTEGRATION_BRIDGE);
    private static final Device COMPUTE_2_INTG_DEVICE = createOpenFlowDevice(2, INTEGRATION_BRIDGE);
    private static final Device COMPUTE_3_INTG_DEVICE = createOpenFlowDevice(3, INTEGRATION_BRIDGE);
    private static final Device COMPUTE_4_INTG_DEVICE = createOpenFlowDevice(4, INTEGRATION_BRIDGE);
    private static final Device GATEWAY_1_INTG_DEVICE = createOpenFlowDevice(5, INTEGRATION_BRIDGE);
    private static final Device GATEWAY_1_ROUT_DEVICE = createOpenFlowDevice(6, ROUTER_BRIDGE);
    private static final Device GATEWAY_2_INTG_DEVICE = createOpenFlowDevice(7, INTEGRATION_BRIDGE);
    private static final Device GATEWAY_2_ROUT_DEVICE = createOpenFlowDevice(8, ROUTER_BRIDGE);
    private static final Device GATEWAY_3_INTG_DEVICE = createOpenFlowDevice(9, INTEGRATION_BRIDGE);
    private static final Device GATEWAY_3_ROUT_DEVICE = createOpenFlowDevice(10, ROUTER_BRIDGE);
    private static final Device GATEWAY_4_INTG_DEVICE = createOpenFlowDevice(11, INTEGRATION_BRIDGE);
    private static final Device GATEWAY_4_ROUT_DEVICE = createOpenFlowDevice(12, ROUTER_BRIDGE);

    private static final Device COMPUTE_1_OVSDB_DEVICE = createOvsdbDevice(COMPUTE_1_IP);
    private static final Device COMPUTE_2_OVSDB_DEVICE = createOvsdbDevice(COMPUTE_2_IP);
    private static final Device COMPUTE_3_OVSDB_DEVICE = createOvsdbDevice(COMPUTE_3_IP);
    private static final Device COMPUTE_4_OVSDB_DEVICE = createOvsdbDevice(COMPUTE_4_IP);
    private static final Device GATEWAY_1_OVSDB_DEVICE = createOvsdbDevice(GATEWAY_1_IP);
    private static final Device GATEWAY_2_OVSDB_DEVICE = createOvsdbDevice(GATEWAY_2_IP);

    private static final OpenstackNode COMPUTE_1 = createNode(
            COMPUTE_1_HOSTNAME,
            COMPUTE,
            COMPUTE_1_INTG_DEVICE,
            COMPUTE_1_IP,
            INIT
    );

    private static final OpenstackNode COMPUTE_2 = createNode(
            COMPUTE_2_HOSTNAME,
            COMPUTE,
            COMPUTE_2_INTG_DEVICE,
            COMPUTE_2_IP,
            DEVICE_CREATED
    );

    private static final OpenstackNode COMPUTE_3 = createNode(
            COMPUTE_3_HOSTNAME,
            COMPUTE,
            COMPUTE_3_INTG_DEVICE,
            COMPUTE_3_IP,
            PORT_CREATED
    );

    private static final OpenstackNode COMPUTE_4 = createNode(
            COMPUTE_4_HOSTNAME,
            COMPUTE,
            COMPUTE_4_INTG_DEVICE,
            COMPUTE_4_IP,
            COMPLETE
    );

    private static final OpenstackNode GATEWAY_1 = createNode(
            GATEWAY_1_HOSTNAME,
            GATEWAY,
            GATEWAY_1_INTG_DEVICE,
            GATEWAY_1_ROUT_DEVICE,
            GATEWAY_1_IP,
            INIT
    );

    private static final OpenstackNode GATEWAY_2 = createNode(
            GATEWAY_2_HOSTNAME,
            GATEWAY,
            GATEWAY_2_INTG_DEVICE,
            GATEWAY_2_ROUT_DEVICE,
            GATEWAY_2_IP,
            DEVICE_CREATED
    );

    private static final OpenstackNode GATEWAY_3 = createNode(
            GATEWAY_3_HOSTNAME,
            GATEWAY,
            GATEWAY_3_INTG_DEVICE,
            GATEWAY_3_ROUT_DEVICE,
            GATEWAY_3_IP,
            PORT_CREATED
    );

    private static final OpenstackNode GATEWAY_4 = createNode(
            GATEWAY_4_HOSTNAME,
            GATEWAY,
            GATEWAY_4_INTG_DEVICE,
            GATEWAY_4_ROUT_DEVICE,
            GATEWAY_4_IP,
            COMPLETE
    );

    private static final TestDeviceService TEST_DEVICE_SERVICE = new TestDeviceService();

    private TestOpenstackNodeManager testNodeManager;
    private DefaultOpenstackNodeHandler target;

    @Before
    public void setUp() throws Exception {
        DeviceAdminService mockDeviceAdminService = createMock(DeviceAdminService.class);
        mockDeviceAdminService.removeDevice(anyObject());
        replay(mockDeviceAdminService);

        OvsdbClientService mockOvsdbClient = createMock(OvsdbClientService.class);
        expect(mockOvsdbClient.isConnected())
                .andReturn(true)
                .anyTimes();
        replay(mockOvsdbClient);

        OvsdbController mockOvsdbController = createMock(OvsdbController.class);
        expect(mockOvsdbController.getOvsdbClient(anyObject()))
                .andReturn(mockOvsdbClient)
                .anyTimes();
        replay(mockOvsdbController);

        testNodeManager = new TestOpenstackNodeManager();
        target = new DefaultOpenstackNodeHandler();

        target.coreService = new TestCoreService();
        target.leadershipService = new TestLeadershipService();
        target.clusterService = new TestClusterService();
        target.deviceService = TEST_DEVICE_SERVICE;
        target.deviceAdminService = mockDeviceAdminService;
        target.ovsdbController = mockOvsdbController;
        target.groupService = new TestGroupService();
        target.osNodeService = testNodeManager;
        target.osNodeAdminService = testNodeManager;
        target.componentConfigService = new TestComponentConfigService();
        TestUtils.setField(target, "eventExecutor", MoreExecutors.newDirectExecutorService());
        target.activate();
    }

    @After
    public void tearDown() {
        TEST_DEVICE_SERVICE.clear();
        target.deactivate();
        target = null;
        testNodeManager = null;
    }

    /**
     * Checks if the compute node state changes from INIT to DEVICE_CREATED
     * after processing INIT state.
     */
    @Test
    public void testComputeNodeProcessNodeInitState() {
        testNodeManager.createNode(COMPUTE_1);
        TEST_DEVICE_SERVICE.devMap.put(COMPUTE_1_OVSDB_DEVICE.id(), COMPUTE_1_OVSDB_DEVICE);

        assertEquals(ERR_STATE_NOT_MATCH, INIT,
                testNodeManager.node(COMPUTE_1_HOSTNAME).state());
        target.processInitState(COMPUTE_1);
        assertEquals(ERR_STATE_NOT_MATCH, DEVICE_CREATED,
                testNodeManager.node(COMPUTE_1_HOSTNAME).state());
    }

    /**
     * Checks if the gateway node state changes from INIT to DEVICE_CREATED
     * after processing INIT state.
     */
    @Test
    public void testGatewayNodeProcessNodeInitState() {
        testNodeManager.createNode(GATEWAY_1);
        TEST_DEVICE_SERVICE.devMap.put(GATEWAY_1_OVSDB_DEVICE.id(), GATEWAY_1_OVSDB_DEVICE);

        assertEquals(ERR_STATE_NOT_MATCH, INIT,
                testNodeManager.node(GATEWAY_1_HOSTNAME).state());
        target.processInitState(GATEWAY_1);
        assertEquals(ERR_STATE_NOT_MATCH, DEVICE_CREATED,
                testNodeManager.node(GATEWAY_1_HOSTNAME).state());
    }

    /**
     * Checks if the compute node state changes from DEVICE_CREATED to
     * PORT_CREATED after processing DEVICE_CREATED state.
     */
    @Test
    public void testComputeNodeProcessDeviceCreatedState() {
        testNodeManager.createNode(COMPUTE_2);
        TEST_DEVICE_SERVICE.devMap.put(COMPUTE_2_OVSDB_DEVICE.id(), COMPUTE_2_OVSDB_DEVICE);
        TEST_DEVICE_SERVICE.devMap.put(COMPUTE_2_INTG_DEVICE.id(), COMPUTE_2_INTG_DEVICE);

        assertEquals(ERR_STATE_NOT_MATCH, DEVICE_CREATED,
                testNodeManager.node(COMPUTE_2_HOSTNAME).state());
        target.processDeviceCreatedState(COMPUTE_2);
        assertEquals(ERR_STATE_NOT_MATCH, PORT_CREATED,
                testNodeManager.node(COMPUTE_2_HOSTNAME).state());
    }

    /**
     * Checks if the gateway node state changes from DEVICE_CREATED to
     * PORT_CREATED after processing DEVICE_CREATED state.
     */
    @Test
    public void testGatewayNodeProcessDeviceCreatedState() {
        testNodeManager.createNode(GATEWAY_2);
        TEST_DEVICE_SERVICE.devMap.put(GATEWAY_2_OVSDB_DEVICE.id(), GATEWAY_2_OVSDB_DEVICE);
        TEST_DEVICE_SERVICE.devMap.put(GATEWAY_2_INTG_DEVICE.id(), GATEWAY_2_INTG_DEVICE);

        assertEquals(ERR_STATE_NOT_MATCH, DEVICE_CREATED,
                testNodeManager.node(GATEWAY_2_HOSTNAME).state());
        target.processDeviceCreatedState(GATEWAY_2);
        assertEquals(ERR_STATE_NOT_MATCH, PORT_CREATED,
                testNodeManager.node(GATEWAY_2_HOSTNAME).state());
    }

    /**
     * Checks if the compute node state changes from PORT_CREATED to
     * COMPLETE after processing PORT_CREATED state.
     */
    @Test
    public void testComputeNodeProcessPortCreatedState() {
        testNodeManager.createNode(COMPUTE_3);
        TEST_DEVICE_SERVICE.devMap.put(COMPUTE_3_OVSDB_DEVICE.id(), COMPUTE_3_OVSDB_DEVICE);
        TEST_DEVICE_SERVICE.devMap.put(COMPUTE_3_INTG_DEVICE.id(), COMPUTE_3_INTG_DEVICE);
        TEST_DEVICE_SERVICE.portList.add(createPort(COMPUTE_3_INTG_DEVICE, DEFAULT_TUNNEL));

        testNodeManager.createNode(GATEWAY_4);
        TEST_DEVICE_SERVICE.devMap.put(GATEWAY_4_INTG_DEVICE.id(), GATEWAY_4_INTG_DEVICE);

        assertEquals(ERR_STATE_NOT_MATCH, PORT_CREATED,
                testNodeManager.node(COMPUTE_3_HOSTNAME).state());
        target.processPortCreatedState(COMPUTE_3);
        assertEquals(ERR_STATE_NOT_MATCH, COMPLETE,
                testNodeManager.node(COMPUTE_3_HOSTNAME).state());
    }

    /**
     * Checks if the gateway node state changes from PORT_CREATED to
     * COMPLETE after processing PORT_CREATED state.
     */
    @Test
    public void testGatewayNodeProcessPortCreatedState() {
        testNodeManager.createNode(COMPUTE_4);
        TEST_DEVICE_SERVICE.devMap.put(COMPUTE_4_OVSDB_DEVICE.id(), COMPUTE_4_OVSDB_DEVICE);
        TEST_DEVICE_SERVICE.devMap.put(COMPUTE_4_INTG_DEVICE.id(), COMPUTE_4_INTG_DEVICE);
        TEST_DEVICE_SERVICE.portList.add(createPort(COMPUTE_4_INTG_DEVICE, DEFAULT_TUNNEL));

        testNodeManager.createNode(GATEWAY_3);
        TEST_DEVICE_SERVICE.devMap.put(GATEWAY_3_INTG_DEVICE.id(), GATEWAY_4_INTG_DEVICE);

        assertEquals(ERR_STATE_NOT_MATCH, PORT_CREATED,
                testNodeManager.node(GATEWAY_3_HOSTNAME).state());
        target.processPortCreatedState(GATEWAY_3);
        assertEquals(ERR_STATE_NOT_MATCH, COMPLETE,
                testNodeManager.node(GATEWAY_3_HOSTNAME).state());
    }

    /**
     * Checks if the compute node state changes from COMPLETE to INCOMPLETE
     * when integration bridge is disconnected.
     */
    @Test
    public void testBackToIncompleteWhenBrIntDisconnected() {
        testNodeManager.createNode(COMPUTE_4);

        assertEquals(ERR_STATE_NOT_MATCH, COMPLETE,
                testNodeManager.node(COMPUTE_4_HOSTNAME).state());
        TEST_DEVICE_SERVICE.removeDevice(COMPUTE_4_INTG_DEVICE);
        assertEquals(ERR_STATE_NOT_MATCH, INCOMPLETE,
                testNodeManager.node(COMPUTE_4_HOSTNAME).state());
    }

    /**
     * Checks if the compute node state changes from COMPLETE to INCOMPLETE
     * when vxlan port is removed from integration bridge.
     */
    @Test
    public void testBackToIncompleteWhenVxlanRemoved() {
        testNodeManager.createNode(COMPUTE_4);

        assertEquals(ERR_STATE_NOT_MATCH, COMPLETE,
                testNodeManager.node(COMPUTE_4_HOSTNAME).state());
        TEST_DEVICE_SERVICE.removePort(COMPUTE_4_INTG_DEVICE, createPort(
                COMPUTE_4_INTG_DEVICE, DEFAULT_TUNNEL));
        assertEquals(ERR_STATE_NOT_MATCH, INCOMPLETE,
                testNodeManager.node(COMPUTE_4_HOSTNAME).state());

    }

    private static Device createOvsdbDevice(IpAddress ovsdbIp) {
        return new TestDevice(new ProviderId("of", "foo"),
                DeviceId.deviceId("ovsdb:" + ovsdbIp.toString()),
                CONTROLLER,
                "manufacturer",
                "hwVersion",
                "swVersion",
                "serialNumber",
                new ChassisId(1));
    }

    private static Device createOpenFlowDevice(long devIdNum, String type) {
        return new TestDevice(new ProviderId("of", "foo"),
                DeviceId.deviceId(String.format("of:%016d", devIdNum)),
                SWITCH,
                type,
                "hwVersion",
                "swVersion",
                "serialNumber",
                new ChassisId(1));
    }

    private static Port createPort(Device device, String portName) {
        return new DefaultPort(device,
                PortNumber.portNumber(1),
                true,
                DefaultAnnotations.builder().set(PORT_NAME, portName).build());
    }

    private static OpenstackNode createNode(String hostname,
                                            OpenstackNode.NodeType type,
                                            Device intgBridge,
                                            IpAddress ipAddr,
                                            NodeState state) {
        return new TestOpenstackNode(
                hostname,
                type,
                intgBridge.id(),
                null,
                ipAddr,
                ipAddr,
                null, state);
    }

    private static OpenstackNode createNode(String hostname,
                                            OpenstackNode.NodeType type,
                                            Device intgBridge,
                                            Device routerBridge,
                                            IpAddress ipAddr,
                                            NodeState state) {
        return new TestOpenstackNode(
                hostname,
                type,
                intgBridge.id(),
                routerBridge.id(),
                ipAddr,
                ipAddr,
                null, state);
    }

    private static final class TestDevice extends DefaultDevice {
        private TestDevice(ProviderId providerId,
                           DeviceId id,
                           Type type,
                           String manufacturer,
                           String hwVersion,
                           String swVersion,
                           String serialNumber,
                           ChassisId chassisId,
                           Annotations... annotations) {
            super(providerId,
                    id,
                    type,
                    manufacturer,
                    hwVersion,
                    swVersion,
                    serialNumber,
                    chassisId,
                    annotations);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <B extends Behaviour> B as(Class<B> projectionClass) {
            if (projectionClass.equals(BridgeConfig.class)) {
                return (B) new TestBridgeConfig();
            } else if (projectionClass.equals(InterfaceConfig.class)) {
                return (B) new TestInterfaceConfig();
            } else if (projectionClass.equals(ExtensionTreatmentResolver.class)) {
                ExtensionTreatmentResolver treatmentResolver = createMock(ExtensionTreatmentResolver.class);
                expect(treatmentResolver.getExtensionInstruction(anyObject()))
                        .andReturn(new TestExtensionTreatment())
                        .anyTimes();
                replay(treatmentResolver);
                return (B) treatmentResolver;
            } else {
                return null;
            }
        }

        @Override
        public <B extends Behaviour> boolean is(Class<B> projectionClass) {
            return true;
        }
    }

    private static final class TestOpenstackNode extends DefaultOpenstackNode {
        private TestOpenstackNode(String hostname,
                                  NodeType type,
                                  DeviceId intgBridge,
                                  DeviceId routerBridge,
                                  IpAddress managementIp,
                                  IpAddress dataIp,
                                  String vlanIntf,
                                  NodeState state) {
            super(hostname,
                    type,
                    intgBridge,
                    routerBridge,
                    managementIp,
                    dataIp,
                    vlanIntf,
                    state);
        }

        @Override
        public PortNumber tunnelPortNum() {
            return PortNumber.portNumber(1);
        }

        @Override
        public PortNumber vlanPortNum() {
            return PortNumber.portNumber(1);
        }

        @Override
        public PortNumber patchPortNum() {
            return PortNumber.portNumber(1);
        }

        @Override
        public MacAddress vlanPortMac() {
            return MacAddress.NONE;
        }
    }

    private static class TestOpenstackNodeManager implements OpenstackNodeService, OpenstackNodeAdminService {
        Map<String, OpenstackNode> osNodeMap = Maps.newHashMap();
        List<OpenstackNodeListener> listeners = Lists.newArrayList();

        @Override
        public Set<OpenstackNode> nodes() {
            return ImmutableSet.copyOf(osNodeMap.values());
        }

        @Override
        public Set<OpenstackNode> nodes(OpenstackNode.NodeType type) {
            return osNodeMap.values().stream()
                    .filter(osNode -> osNode.type() == type)
                    .collect(Collectors.toSet());
        }

        @Override
        public Set<OpenstackNode> completeNodes() {
            return osNodeMap.values().stream()
                    .filter(osNode -> osNode.state() == COMPLETE)
                    .collect(Collectors.toSet());
        }

        @Override
        public Set<OpenstackNode> completeNodes(OpenstackNode.NodeType type) {
            return osNodeMap.values().stream()
                    .filter(osNode -> osNode.type() == type && osNode.state() == COMPLETE)
                    .collect(Collectors.toSet());
        }

        @Override
        public OpenstackNode node(String hostname) {
            return osNodeMap.get(hostname);
        }

        @Override
        public OpenstackNode node(DeviceId deviceId) {
            return osNodeMap.values().stream()
                    .filter(osNode -> Objects.equals(osNode.intgBridge(), deviceId) ||
                            Objects.equals(osNode.ovsdb(), deviceId) ||
                            Objects.equals(osNode.routerBridge(), deviceId))
                    .findFirst().orElse(null);
        }

        @Override
        public void addListener(OpenstackNodeListener listener) {
            listeners.add(listener);
        }

        @Override
        public void removeListener(OpenstackNodeListener listener) {
            listeners.remove(listener);
        }

        @Override
        public void createNode(OpenstackNode osNode) {
            osNodeMap.put(osNode.hostname(), osNode);
        }

        @Override
        public void updateNode(OpenstackNode osNode) {
            osNodeMap.put(osNode.hostname(), osNode);
        }

        @Override
        public OpenstackNode removeNode(String hostname) {
            return null;
        }
    }

    private static class TestDeviceService extends DeviceServiceAdapter {
        Map<DeviceId, Device> devMap = Maps.newHashMap();
        List<Port> portList = Lists.newArrayList();
        List<DeviceListener> listeners = Lists.newArrayList();

        @Override
        public void addListener(DeviceListener listener) {
            listeners.add(listener);
        }

        @Override
        public void removeListener(DeviceListener listener) {
            listeners.remove(listener);
        }

        @Override
        public Device getDevice(DeviceId deviceId) {
            return devMap.get(deviceId);
        }

        @Override
        public List<Port> getPorts(DeviceId deviceId) {
            return this.portList.stream()
                    .filter(p -> p.element().id().equals(deviceId))
                    .collect(Collectors.toList());
        }

        @Override
        public boolean isAvailable(DeviceId deviceId) {
            return devMap.containsKey(deviceId);
        }

        void addDevice(Device device) {
            devMap.put(device.id(), device);
            DeviceEvent event = new DeviceEvent(DEVICE_ADDED, device);
            listeners.stream().filter(l -> l.isRelevant(event)).forEach(l -> l.event(event));
        }

        void removeDevice(Device device) {
            devMap.remove(device.id());
            DeviceEvent event = new DeviceEvent(DEVICE_AVAILABILITY_CHANGED, device);
            listeners.stream().filter(l -> l.isRelevant(event)).forEach(l -> l.event(event));
        }

        void addPort(Device device, Port port) {
            portList.add(port);
            DeviceEvent event = new DeviceEvent(PORT_ADDED, device, port);
            listeners.stream().filter(l -> l.isRelevant(event)).forEach(l -> l.event(event));
        }

        void removePort(Device device, Port port) {
            portList.remove(port);
            DeviceEvent event = new DeviceEvent(PORT_REMOVED, device, port);
            listeners.stream().filter(l -> l.isRelevant(event)).forEach(l -> l.event(event));
        }

        void clear() {
            this.listeners.clear();
            this.devMap.clear();
            this.portList.clear();
        }
    }

    private static class TestBridgeConfig implements BridgeConfig {

        @Override
        public DriverData data() {
            return null;
        }

        @Override
        public void setData(DriverData data) {

        }

        @Override
        public DriverHandler handler() {
            return null;
        }

        @Override
        public void setHandler(DriverHandler handler) {

        }

        @Override
        public void addBridge(BridgeName bridgeName) {

        }

        @Override
        public void addBridge(BridgeName bridgeName, String dpid, String exPortName) {

        }

        @Override
        public boolean addBridge(BridgeName bridgeName, String dpid, List<ControllerInfo> controllers) {
            return false;
        }

        @Override
        public boolean addBridge(BridgeDescription bridge) {
            TEST_DEVICE_SERVICE.addDevice(new DefaultDevice(new ProviderId("of", "foo"),
                    DeviceId.deviceId("of:" + bridge.datapathId().get()),
                    SWITCH,
                    bridge.name(),
                    "hwVersion",
                    "swVersion",
                    "serialNumber",
                    new ChassisId(1)));
            return true;
        }

        @Override
        public void deleteBridge(BridgeName bridgeName) {

        }

        @Override
        public Collection<BridgeDescription> getBridges() {
            return ImmutableSet.of(ROUT_BRIDGE);
        }

        @Override
        public void addPort(BridgeName bridgeName, String portName) {

        }

        @Override
        public void deletePort(BridgeName bridgeName, String portName) {

        }

        @Override
        public Collection<PortDescription> getPorts() {
            return ImmutableSet.of(PATCH_ROUT);
        }

        @Override
        public Set<PortNumber> getPortNumbers() {
            return null;
        }

        @Override
        public List<PortNumber> getLocalPorts(Iterable<String> ifaceIds) {
            return null;
        }
    }

    private static class TestInterfaceConfig implements InterfaceConfig {

        @Override
        public DriverData data() {
            return null;
        }

        @Override
        public void setData(DriverData data) {

        }

        @Override
        public DriverHandler handler() {
            return null;
        }

        @Override
        public void setHandler(DriverHandler handler) {

        }

        @Override
        public boolean addAccessInterface(DeviceId deviceId, String intf, VlanId vlanId) {
            return false;
        }

        @Override
        public boolean addAccessMode(String intf, VlanId vlanId) {
            return false;
        }

        @Override
        public boolean removeAccessInterface(DeviceId deviceId, String intf) {
            return false;
        }

        @Override
        public boolean removeAccessMode(String intf) {
            return false;
        }

        @Override
        public boolean addTrunkInterface(DeviceId deviceId, String intf, List<VlanId> vlanIds) {
            return false;
        }

        @Override
        public boolean addTrunkMode(String intf, List<VlanId> vlanIds) {
            return false;
        }

        @Override
        public boolean removeTrunkInterface(DeviceId deviceId, String intf) {
            return false;
        }

        @Override
        public boolean removeTrunkMode(String intf) {
            return false;
        }

        @Override
        public boolean addRateLimit(String intf, short limit) {
            return false;
        }

        @Override
        public boolean removeRateLimit(String intf) {
            return false;
        }

        @Override
        public boolean addTunnelMode(String intf, TunnelDescription tunnelDesc) {
            TEST_DEVICE_SERVICE.devMap.values().stream()
                    .filter(device -> device.type() == SWITCH &&
                            device.manufacturer().equals(INTEGRATION_BRIDGE))
                    .forEach(device -> {
                        TEST_DEVICE_SERVICE.addPort(device, createPort(device, intf));
                    });
            return true;
        }

        @Override
        public boolean removeTunnelMode(String intf) {
            return false;
        }

        @Override
        public boolean addPatchMode(String ifaceName, PatchDescription patchInterface) {
            if (ifaceName.equals(PATCH_INTG_BRIDGE)) {
                TEST_DEVICE_SERVICE.devMap.values().stream()
                        .filter(device -> device.type() == SWITCH &&
                                device.manufacturer().equals(INTEGRATION_BRIDGE))
                        .forEach(device -> {
                            TEST_DEVICE_SERVICE.addPort(device, createPort(device, ifaceName));
                        });
            } else if (ifaceName.equals(PATCH_ROUT_BRIDGE)) {
                TEST_DEVICE_SERVICE.devMap.values().stream()
                        .filter(device -> device.type() == SWITCH &&
                                device.manufacturer().equals(ROUTER_BRIDGE))
                        .forEach(device -> {
                            TEST_DEVICE_SERVICE.addPort(device, createPort(device, ifaceName));
                        });
            }
            return true;
        }

        @Override
        public boolean removePatchMode(String ifaceName) {
            return false;
        }

        @Override
        public List<DeviceInterfaceDescription> getInterfaces(DeviceId deviceId) {
            return null;
        }

        @Override
        public List<DeviceInterfaceDescription> getInterfaces() {
            return null;
        }
    }

    private static class TestGroupService implements GroupService {
        Map<GroupKey, Group> groupMap = Maps.newHashMap();
        Map<GroupKey, GroupBuckets> groupBucketsMap = Maps.newHashMap();
        List<GroupListener> listeners = Lists.newArrayList();

        @Override
        public void addListener(GroupListener listener) {
            listeners.add(listener);
        }

        @Override
        public void removeListener(GroupListener listener) {
            listeners.remove(listener);
        }

        @Override
        public void addGroup(GroupDescription groupDesc) {
            DefaultGroup group = new DefaultGroup(GroupId.valueOf(groupDesc.givenGroupId()), groupDesc);
            group.setState(Group.GroupState.ADDED);
            groupMap.put(groupDesc.appCookie(), group);
            groupBucketsMap.put(groupDesc.appCookie(), groupDesc.buckets());

            GroupEvent event = new GroupEvent(GroupEvent.Type.GROUP_ADDED, group);
            listeners.stream().filter(listener -> listener.isRelevant(event))
                    .forEach(listener -> listener.event(event));
        }

        @Override
        public Group getGroup(DeviceId deviceId, GroupKey appCookie) {
            return groupMap.get(appCookie);
        }

        @Override
        public void addBucketsToGroup(DeviceId deviceId, GroupKey oldCookie, GroupBuckets buckets,
                                      GroupKey newCookie, ApplicationId appId) {

        }

        @Override
        public void removeBucketsFromGroup(DeviceId deviceId, GroupKey oldCookie, GroupBuckets buckets,
                                           GroupKey newCookie, ApplicationId appId) {

        }

        @Override
        public void purgeGroupEntries(DeviceId deviceId) {

        }

        @Override
        public void removeGroup(DeviceId deviceId, GroupKey appCookie, ApplicationId appId) {

        }

        @Override
        public Iterable<Group> getGroups(DeviceId deviceId, ApplicationId appId) {
            return null;
        }

        @Override
        public Iterable<Group> getGroups(DeviceId deviceId) {
            return null;
        }

        @Override
        public void setBucketsForGroup(DeviceId deviceId, GroupKey oldCookie, GroupBuckets buckets,
                                       GroupKey newCookie, ApplicationId appId) {
            groupBucketsMap.put(newCookie, buckets);
            GroupEvent event = new GroupEvent(GroupEvent.Type.GROUP_UPDATED, groupMap.get(newCookie));
            listeners.stream().filter(listener -> listener.isRelevant(event))
                    .forEach(listener -> listener.event(event));
        }

    }

    private static class TestExtensionTreatment implements ExtensionTreatment {
        Ip4Address tunnelDst;

        @Override
        public ExtensionTreatmentType type() {
            return null;
        }

        @Override
        public <T> void setPropertyValue(String key, T value) throws ExtensionPropertyException {
            tunnelDst = (Ip4Address) value;
        }

        @Override
        public <T> T getPropertyValue(String key) throws ExtensionPropertyException {
            return null;
        }

        @Override
        public List<String> getProperties() {
            return null;
        }

        @Override
        public byte[] serialize() {
            return new byte[0];
        }

        @Override
        public void deserialize(byte[] data) {

        }

        @Override
        public boolean equals(Object obj) {
            TestExtensionTreatment that = (TestExtensionTreatment) obj;
            return Objects.equals(tunnelDst, that.tunnelDst);
        }

        @Override
        public int hashCode() {
            return Objects.hash(tunnelDst);
        }
    }

    private static class TestCoreService extends CoreServiceAdapter {

        @Override
        public ApplicationId getAppId(String name) {
            return TEST_APP_ID;
        }
    }

    private static class TestLeadershipService extends LeadershipServiceAdapter {

        @Override
        public NodeId getLeader(String path) {
            return LOCAL_NODE_ID;
        }
    }

    private static class TestClusterService extends ClusterServiceAdapter {

        @Override
        public ControllerNode getLocalNode() {
            return LOCAL_CTRL;
        }
    }

    private class TestComponentConfigService extends ComponentConfigAdapter {

    }
}
