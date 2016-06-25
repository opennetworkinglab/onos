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
package org.onosproject.routing.impl;

import com.google.common.collect.Sets;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.Ethernet;
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.TestApplicationId;
import org.onosproject.app.ApplicationService;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.incubator.net.intf.Interface;
import org.onosproject.incubator.net.intf.InterfaceListener;
import org.onosproject.incubator.net.intf.InterfaceService;
import org.onosproject.incubator.net.intf.InterfaceServiceAdapter;
import org.onosproject.incubator.net.routing.ResolvedRoute;
import org.onosproject.incubator.net.routing.RouteEvent;
import org.onosproject.incubator.net.routing.RouteListener;
import org.onosproject.incubator.net.routing.RouteServiceAdapter;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.DeviceServiceAdapter;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.DefaultNextObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.NextObjective;
import org.onosproject.net.host.InterfaceIpAddress;
import org.onosproject.routing.RoutingService;
import org.onosproject.routing.config.RouterConfig;
import org.osgi.service.component.ComponentContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;

/**
 * Unit tests for SingleSwitchFibInstaller.
 */
public class SingleSwitchFibInstallerTest {

    private static final DeviceId DEVICE_ID = DeviceId.deviceId("of:0000000000000001");

    private static final ConnectPoint SW1_ETH1 = new ConnectPoint(
            DEVICE_ID, PortNumber.portNumber(1));

    private static final ConnectPoint SW1_ETH2 = new ConnectPoint(
            DEVICE_ID, PortNumber.portNumber(2));

    private static final int NEXT_ID = 11;

    private static final VlanId VLAN1 = VlanId.vlanId((short) 1);
    private static final MacAddress MAC1 = MacAddress.valueOf("00:00:00:00:00:01");
    private static final MacAddress MAC2 = MacAddress.valueOf("00:00:00:00:00:02");

    private static final IpPrefix PREFIX1 = Ip4Prefix.valueOf("1.1.1.0/24");
    private static final IpAddress NEXT_HOP1 = IpAddress.valueOf("192.168.10.1");
    private static final IpAddress NEXT_HOP2 = IpAddress.valueOf("192.168.20.1");
    private static final InterfaceIpAddress INTF1 =
            InterfaceIpAddress.valueOf("192.168.10.2/24");
    private static final InterfaceIpAddress INTF2 =
            InterfaceIpAddress.valueOf("192.168.20.2/24");

    private final Set<Interface> interfaces = Sets.newHashSet();
    private InterfaceService interfaceService;
    private NetworkConfigService networkConfigService;
    private NetworkConfigRegistry networkConfigRegistry;
    private FlowObjectiveService flowObjectiveService;
    private ApplicationService applicationService;
    private DeviceService deviceService;
    private static final ApplicationId APPID = TestApplicationId.create("foo");

    private RouteListener routeListener;
    private DeviceListener deviceListener;

    private RouterConfig routerConfig;
    private SingleSwitchFibInstaller sSfibInstaller;
    private InterfaceListener interfaceListener;

    @Before
    public void setUp() throws Exception {
        sSfibInstaller = new SingleSwitchFibInstaller();

        sSfibInstaller.componentConfigService = createNiceMock(ComponentConfigService.class);

        ComponentContext mockContext = createNiceMock(ComponentContext.class);

        routerConfig = new TestRouterConfig();
        interfaceService = createMock(InterfaceService.class);

        networkConfigService = createMock(NetworkConfigService.class);
        networkConfigService.addListener(anyObject(NetworkConfigListener.class));
        expectLastCall().anyTimes();
        networkConfigRegistry = createMock(NetworkConfigRegistry.class);
        flowObjectiveService = createMock(FlowObjectiveService.class);
        applicationService = createNiceMock(ApplicationService.class);
        replay(applicationService);
        deviceService = new TestDeviceService();
        CoreService coreService = createNiceMock(CoreService.class);
        expect(coreService.registerApplication(anyString())).andReturn(APPID).anyTimes();
        replay(coreService);

        sSfibInstaller.networkConfigService = networkConfigService;
        sSfibInstaller.networkConfigRegistry = networkConfigRegistry;
        sSfibInstaller.interfaceService = interfaceService;
        sSfibInstaller.flowObjectiveService = flowObjectiveService;
        sSfibInstaller.applicationService = applicationService;
        sSfibInstaller.coreService = coreService;
        sSfibInstaller.routeService = new TestRouteService();
        sSfibInstaller.deviceService = deviceService;

        setUpNetworkConfigService();
        setUpInterfaceService();
        sSfibInstaller.activate(mockContext);
    }

    /**
     * Sets up InterfaceService.
     */
    private void setUpInterfaceService() {
        interfaceService.addListener(anyObject(InterfaceListener.class));
        expectLastCall().andDelegateTo(new TestInterfaceService());

        // Interface with no VLAN
        Interface sw1Eth1 = new Interface("intf1", SW1_ETH1,
                Collections.singletonList(INTF1), MAC1, VlanId.NONE);
        expect(interfaceService.getMatchingInterface(NEXT_HOP1)).andReturn(sw1Eth1);
        interfaces.add(sw1Eth1);

        // Interface with a VLAN
        Interface sw2Eth1 = new Interface("intf2", SW1_ETH2,
                Collections.singletonList(INTF2), MAC2, VLAN1);
        expect(interfaceService.getMatchingInterface(NEXT_HOP2)).andReturn(sw2Eth1);
        interfaces.add(sw2Eth1);

        expect(interfaceService.getInterfaces()).andReturn(interfaces);

        replay(interfaceService);
    }

    /*
     * Sets up NetworkConfigService.
     */
    private void setUpNetworkConfigService() {
        expect(networkConfigService.getConfig(
                anyObject(ApplicationId.class), eq(RoutingService.ROUTER_CONFIG_CLASS))).
        andReturn(routerConfig);
        replay(networkConfigService);
    }

    /**
     * Sets up FlowObjectiveService.
     */
    private void setUpFlowObjectiveService() {
        expect(flowObjectiveService.allocateNextId()).andReturn(NEXT_ID);
        replay(flowObjectiveService);
    }

    /**
     * Creates a next objective with the given parameters.
     *
     * @param srcMac source MAC address
     * @param dstMac destination MAC address
     * @param port port number
     * @param vlan vlan ID
     * @param add whether to create an add objective or remove objective
     * @return new next objective
     */
    private NextObjective createNextObjective(MacAddress srcMac,
                                              MacAddress dstMac,
                                              PortNumber port,
                                              VlanId vlan,
                                              boolean add) {
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder()
                .setEthSrc(srcMac)
                .setEthDst(dstMac);
        TrafficSelector.Builder metabuilder = null;
        if (!vlan.equals(VlanId.NONE)) {
            treatment.pushVlan()
                     .setVlanId(vlan)
                     .setVlanPcp((byte) 0);
        } else {
            metabuilder = DefaultTrafficSelector.builder();
            metabuilder.matchVlanId(VlanId.vlanId(SingleSwitchFibInstaller.ASSIGNED_VLAN));
        }

        treatment.setOutput(port);
        NextObjective.Builder nextBuilder = DefaultNextObjective.builder()
                .withId(NEXT_ID)
                .addTreatment(treatment.build())
                .withType(NextObjective.Type.SIMPLE)
                .fromApp(APPID);
        if (metabuilder != null) {
            nextBuilder.withMeta(metabuilder.build());
        }

        return add ? nextBuilder.add() : nextBuilder.remove();
    }

    /**
     * Creates a new forwarding objective with the given parameters.
     *
     * @param prefix IP prefix
     * @param add whether to create an add objective or a remove objective
     * @return new forwarding objective
     */
    private ForwardingObjective createForwardingObjective(IpPrefix prefix,
                                                          boolean add) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPDst(prefix)
                .build();

        int priority = prefix.prefixLength() * 5 + 100;
        ForwardingObjective.Builder fwdBuilder = DefaultForwardingObjective.builder()
                .fromApp(APPID)
                .makePermanent()
                .withSelector(selector)
                .withPriority(priority)
                .withFlag(ForwardingObjective.Flag.SPECIFIC);

        if (add) {
            fwdBuilder.nextStep(NEXT_ID);
        } else {
            fwdBuilder.withTreatment(DefaultTrafficTreatment.builder().build());
        }

        return add ? fwdBuilder.add() : fwdBuilder.remove();
    }

    /**
     * Tests adding a route.
     *
     * We verify that the flowObjectiveService records the correct state and that the
     * correct flow is submitted to the flowObjectiveService.
     */
    @Test
    public void testRouteAdd() {
        ResolvedRoute resolvedRoute = new ResolvedRoute(PREFIX1, NEXT_HOP1, MAC1);

        // Create the next objective
        NextObjective nextObjective = createNextObjective(MAC1, MAC1, SW1_ETH1.port(), VlanId.NONE, true);
        flowObjectiveService.next(DEVICE_ID, nextObjective);

        // Create the flow objective
        ForwardingObjective fwd = createForwardingObjective(PREFIX1, true);
        flowObjectiveService.forward(DEVICE_ID, fwd);
        EasyMock.expectLastCall().once();
        setUpFlowObjectiveService();

        // Send in the add event
        RouteEvent routeEvent = new RouteEvent(RouteEvent.Type.ROUTE_ADDED, resolvedRoute);
        routeListener.event(routeEvent);
        verify(flowObjectiveService);
    }

    /**
     * Tests adding a route with to a next hop in a VLAN.
     *
     * We verify that the flowObjectiveService records the correct state and that the
     * correct flowObjectiveService is submitted to the flowObjectiveService.
     */
    @Test
    public void testRouteAddWithVlan() {
        ResolvedRoute route = new ResolvedRoute(PREFIX1, NEXT_HOP2, MAC2);

        // Create the next objective
        NextObjective nextObjective = createNextObjective(MAC2, MAC2, SW1_ETH2.port(), VLAN1, true);
        flowObjectiveService.next(DEVICE_ID, nextObjective);

        // Create the flow objective
        ForwardingObjective fwd = createForwardingObjective(PREFIX1, true);
        flowObjectiveService.forward(DEVICE_ID, fwd);
        EasyMock.expectLastCall().once();
        setUpFlowObjectiveService();

        // Send in the add event
        routeListener.event(new RouteEvent(RouteEvent.Type.ROUTE_ADDED, route));

        verify(flowObjectiveService);
    }

    /**
     * Tests updating a route.
     *
     * We verify that the flowObjectiveService records the correct state and that the
     * correct flow is submitted to the flowObjectiveService.
     */
    @Test
    public void testRouteUpdate() {
        // Firstly add a route
        testRouteAdd();
        reset(flowObjectiveService);

        ResolvedRoute route = new ResolvedRoute(PREFIX1, NEXT_HOP2, MAC2);

        // Create the next objective
        NextObjective nextObjective = createNextObjective(MAC2, MAC2, SW1_ETH2.port(), VLAN1, true);
        flowObjectiveService.next(DEVICE_ID, nextObjective);

        // Create the flow objective
        ForwardingObjective fwd = createForwardingObjective(PREFIX1, true);
        flowObjectiveService.forward(DEVICE_ID, fwd);
        EasyMock.expectLastCall().once();
        setUpFlowObjectiveService();

        // Send in the update event
        routeListener.event(new RouteEvent(RouteEvent.Type.ROUTE_UPDATED, route));

        verify(flowObjectiveService);
    }

    /**
     * Tests deleting a route.
     *
     * We verify that the flowObjectiveService records the correct state and that the
     * correct flow is withdrawn from the flowObjectiveService.
     */
    @Test
    public void testRouteDelete() {
        // Firstly add a route
        testRouteAdd();

        // Construct the existing route
        ResolvedRoute route = new ResolvedRoute(PREFIX1, null, null);

        // Create the flow objective
        reset(flowObjectiveService);
        ForwardingObjective fwd = createForwardingObjective(PREFIX1, false);
        flowObjectiveService.forward(DEVICE_ID, fwd);
        replay(flowObjectiveService);

        // Send in the delete event
        routeListener.event(new RouteEvent(RouteEvent.Type.ROUTE_REMOVED, route));

        verify(flowObjectiveService);
    }

    private class TestInterfaceService extends InterfaceServiceAdapter {
        @Override
        public void addListener(InterfaceListener listener) {
            interfaceListener = listener;
        }
    }

    private class TestRouteService extends RouteServiceAdapter {
        @Override
        public void addListener(RouteListener listener) {
            SingleSwitchFibInstallerTest.this.routeListener = listener;
        }
    }

    private class TestRouterConfig extends RouterConfig {

        @Override
        public List<String> getInterfaces() {
            ArrayList<String> interfaces = new ArrayList<>();
            interfaces.add("of:0000000000000001/1");
            interfaces.add("of:0000000000000001/2");
            return interfaces;
        }

        @Override
        public ConnectPoint getControlPlaneConnectPoint() {
            return SW1_ETH1;
        }

        @Override
        public boolean getOspfEnabled() {
            return true;
        }
    }

    private class TestDeviceService  extends DeviceServiceAdapter {

        @Override
        public boolean isAvailable(DeviceId deviceId) {
            return true;
        }

        @Override
        public void addListener(DeviceListener listener) {
            SingleSwitchFibInstallerTest.this.deviceListener = listener;
        }
    }
}
