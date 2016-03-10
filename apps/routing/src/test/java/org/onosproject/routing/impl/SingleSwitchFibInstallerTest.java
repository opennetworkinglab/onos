/*
 * Copyright 2015 Open Networking Laboratory
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

import static org.easymock.EasyMock.anyObject;
import  static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import java.util.Set;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.Ethernet;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.TestApplicationId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.CoreServiceAdapter;
import org.osgi.service.component.ComponentContext;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.incubator.net.intf.Interface;
import org.onosproject.incubator.net.intf.InterfaceService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
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
import org.onosproject.net.intent.AbstractIntentTest;
import org.onosproject.routing.FibEntry;
import org.onosproject.routing.FibListener;
import org.onosproject.routing.FibUpdate;
import org.onosproject.routing.RoutingService;
import org.onosproject.routing.RoutingServiceAdapter;
import org.onosproject.routing.config.RouterConfig;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

/**
 * Unit tests for SingleSwitchFibInstaller.
 */
public class SingleSwitchFibInstallerTest extends AbstractIntentTest {

    //for interface service setup
    private static final ConnectPoint SW1_ETH1 = new ConnectPoint(
            DeviceId.deviceId("of:0000000000000001"),
            PortNumber.portNumber(1));

    private static final ConnectPoint SW2_ETH1 = new ConnectPoint(
            DeviceId.deviceId("of:0000000000000002"),
            PortNumber.portNumber(1));

    private static final ConnectPoint SW3_ETH1 = new ConnectPoint(
            DeviceId.deviceId("of:0000000000000003"),
            PortNumber.portNumber(1));

    private static final ConnectPoint SW4_ETH1 = new ConnectPoint(
            DeviceId.deviceId("of:0000000000000004"),
            PortNumber.portNumber(1));

    private DeviceId deviceId = DeviceId.deviceId("of:0000000000000001");
    private final Set<Interface> interfaces = Sets.newHashSet();
    private InterfaceService interfaceService;
    private NetworkConfigService networkConfigService;
    private FlowObjectiveService flowObjectiveService;
    private DeviceService deviceService;
    private static final ApplicationId APPID = TestApplicationId.create("update fib");
    private FibListener fibListener;
    private DeviceListener deviceListener;
    private CoreService coreService;
    private RouterConfig routerConfig;
    private RoutingService routingService;
    SingleSwitchFibInstaller sSfibInstaller;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        sSfibInstaller = new SingleSwitchFibInstaller();

        //component config service
        ComponentConfigService mockComponenetConfigServ = EasyMock.createMock(ComponentConfigService.class);
        expect(mockComponenetConfigServ.getProperties(anyObject())).andReturn(ImmutableSet.of());
        mockComponenetConfigServ.registerProperties(sSfibInstaller.getClass());
        EasyMock.expectLastCall();
        mockComponenetConfigServ.unregisterProperties(sSfibInstaller.getClass(), false);
        EasyMock.expectLastCall();
        expect(mockComponenetConfigServ.getProperties(anyObject())).andReturn(ImmutableSet.of());
        sSfibInstaller.componentConfigService = mockComponenetConfigServ;
        replay(mockComponenetConfigServ);

        //component context
        ComponentContext mockContext = EasyMock.createMock(ComponentContext.class);
        Dictionary<String, Boolean> properties = null;
        expect(mockContext.getProperties()).andReturn(properties);
        replay(mockContext);

        coreService = new TestCoreService();
        routingService = new TestRoutingService();
        routerConfig = new TestRouterConfig();
        interfaceService = createMock(InterfaceService.class);
        networkConfigService = createMock(NetworkConfigService.class);
        flowObjectiveService = createMock(FlowObjectiveService.class);
        deviceService = new TestDeviceService();

        sSfibInstaller.networkConfigService = networkConfigService;
        sSfibInstaller.interfaceService = interfaceService;
        sSfibInstaller.flowObjectiveService = flowObjectiveService;
        sSfibInstaller.coreService = coreService;
        sSfibInstaller.routingService = new TestRoutingService();
        sSfibInstaller.deviceService = deviceService;

        setUpNetworkConfigService();
        setUpInterfaceService();
        sSfibInstaller.activate(mockContext);
    }

    /**
     * Sets up InterfaceService.
     */
    private void setUpInterfaceService() {
        Set<InterfaceIpAddress> interfaceIpAddresses1 = Sets.newHashSet();
        interfaceIpAddresses1.add(new InterfaceIpAddress(
                IpAddress.valueOf("192.168.10.101"),
                IpPrefix.valueOf("192.168.10.0/24")));
        Interface sw1Eth1 = new Interface(SW1_ETH1.deviceId().toString(), SW1_ETH1,
                  interfaceIpAddresses1, MacAddress.valueOf("00:00:00:00:00:01"),
                  VlanId.NONE);
        interfaces.add(sw1Eth1);

        Set<InterfaceIpAddress> interfaceIpAddresses2 = Sets.newHashSet();
        interfaceIpAddresses2.add(new InterfaceIpAddress(IpAddress.valueOf("192.168.20.101"),
           IpPrefix.valueOf("192.168.20.0/24")));
        Interface sw2Eth1 = new Interface(SW2_ETH1.deviceId().toString(), SW2_ETH1,
           interfaceIpAddresses2, MacAddress.valueOf("00:00:00:00:00:02"),
           VlanId.NONE);
        interfaces.add(sw2Eth1);

        Set<InterfaceIpAddress> interfaceIpAddresses3 = Sets.newHashSet();
        interfaceIpAddresses3.add(
                new InterfaceIpAddress(IpAddress.valueOf("192.168.30.101"),
                IpPrefix.valueOf("192.168.30.0/24")));
        Interface sw3Eth1 = new Interface(SW3_ETH1.deviceId().toString(), SW3_ETH1,
           interfaceIpAddresses3, MacAddress.valueOf("00:00:00:00:00:03"), VlanId.NONE);
        interfaces.add(sw3Eth1);

        InterfaceIpAddress interfaceIpAddress4 =
           new InterfaceIpAddress(IpAddress.valueOf("192.168.40.101"),
           IpPrefix.valueOf("192.168.40.0/24"));

        Interface sw4Eth1 = new Interface(SW4_ETH1.deviceId().toString(), SW4_ETH1,
           Sets.newHashSet(interfaceIpAddress4),
           MacAddress.valueOf("00:00:00:00:00:04"),
           VlanId.vlanId((short) 1));

        expect(interfaceService.getInterfacesByPort(SW4_ETH1)).andReturn(
                Collections.singleton(sw4Eth1)).anyTimes();
        expect(interfaceService.getMatchingInterface(Ip4Address.valueOf("192.168.40.1")))
                .andReturn(sw4Eth1).anyTimes();

        interfaces.add(sw4Eth1);

        expect(interfaceService.getInterfacesByPort(SW1_ETH1)).andReturn(
                Collections.singleton(sw1Eth1)).anyTimes();
        expect(interfaceService.getMatchingInterface(Ip4Address.valueOf("192.168.10.1")))
                .andReturn(sw1Eth1).anyTimes();
        expect(interfaceService.getInterfacesByPort(SW2_ETH1)).andReturn(
                Collections.singleton(sw2Eth1)).anyTimes();
        expect(interfaceService.getMatchingInterface(Ip4Address.valueOf("192.168.20.1")))
                .andReturn(sw2Eth1).anyTimes();
        expect(interfaceService.getInterfacesByPort(SW3_ETH1)).andReturn(
                Collections.singleton(sw3Eth1)).anyTimes();
        expect(interfaceService.getMatchingInterface(Ip4Address.valueOf("192.168.30.1")))
                .andReturn(sw3Eth1).anyTimes();
        expect(interfaceService.getInterfaces()).andReturn(interfaces).anyTimes();
        replay(interfaceService);
    }

    /*
     * Sets up NetworkConfigService.
    */
    private void setUpNetworkConfigService() {
        ApplicationId routerAppId = coreService.registerApplication(RoutingService.ROUTER_APP_ID);
        expect(networkConfigService.getConfig(routerAppId, RoutingService.ROUTER_CONFIG_CLASS)).
        andReturn(routerConfig);
        replay(networkConfigService);
    }

    /**
     * Sets up FlowObjectiveService.
     */
    private void setUpFlowObjectiveService() {
        expect(flowObjectiveService.allocateNextId()).andReturn(11);
        replay(flowObjectiveService);
    }

    /**
     * Tests adding a FIB entry to the flowObjectiveService.
     *
     * We verify that the flowObjectiveService records the correct state and that the
     * correct flow is submitted to the flowObjectiveService.
     */
    @Test
    public void testFibAdd() {
        IpPrefix prefix = Ip4Prefix.valueOf("1.1.1.0/24");
        FibEntry fibEntry = new FibEntry(prefix,
                Ip4Address.valueOf("192.168.10.1"),
                MacAddress.valueOf("00:00:00:00:00:01"));

        //create the next Objective
        Interface egressIntf = interfaceService.getMatchingInterface(fibEntry.nextHopIp());
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder()
                .setEthSrc(MacAddress.valueOf("00:00:00:00:00:01"))
                .setEthDst(MacAddress.valueOf("00:00:00:00:00:01"));
        TrafficSelector.Builder metabuilder = null;
        if (!egressIntf.vlan().equals(VlanId.NONE)) {
            treatment.pushVlan()
                    .setVlanId(egressIntf.vlan())
                    .setVlanPcp((byte) 0);
        } else {
            metabuilder = DefaultTrafficSelector.builder();
            metabuilder.matchVlanId(VlanId.vlanId(SingleSwitchFibInstaller.ASSIGNED_VLAN));
        }
        treatment.setOutput(PortNumber.portNumber(1));
        int nextId = 11;
        NextObjective.Builder nextBuilder = DefaultNextObjective.builder()
                .withId(nextId)
                .addTreatment(treatment.build())
                .withType(NextObjective.Type.SIMPLE)
                .fromApp(APPID);
        if (metabuilder != null) {
            nextBuilder.withMeta(metabuilder.build());
        }

        NextObjective nextObjective = nextBuilder.add();
        flowObjectiveService.next(deviceId, nextObjective);

        //set up the flowObjective
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

        Integer nextId1 = 11;
        fwdBuilder.nextStep(nextId1);
        flowObjectiveService.forward(deviceId, fwdBuilder.add());
        EasyMock.expectLastCall().once();
        setUpFlowObjectiveService();

        // Send in the UPDATE FibUpdate
        FibUpdate fibUpdate = new FibUpdate(FibUpdate.Type.UPDATE, fibEntry);
        fibListener.update(Collections.singleton(fibUpdate), Collections.emptyList());
        verify(flowObjectiveService);
    }

    /**
     * Tests adding a FIB entry with to a next hop in a VLAN.
     *
     * We verify that the flowObjectiveService records the correct state and that the
     * correct flowObjectiveService is submitted to the flowObjectiveService.
     */
    @Test
    public void testFibAddWithVlan() {
        IpPrefix prefix = Ip4Prefix.valueOf("3.3.3.0/24");
        FibEntry fibEntry = new FibEntry(prefix,
                Ip4Address.valueOf("192.168.40.1"),
                MacAddress.valueOf("00:00:00:00:00:04"));

        //create the next Objective
        Interface egressIntf = interfaceService.getMatchingInterface(fibEntry.nextHopIp());
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder()
                .setEthSrc(MacAddress.valueOf("00:00:00:00:00:04"))
                .setEthDst(MacAddress.valueOf("00:00:00:00:00:04"));
        TrafficSelector.Builder metabuilder = null;
        if (!egressIntf.vlan().equals(VlanId.NONE)) {
            treatment.pushVlan()
                    .setVlanId(egressIntf.vlan())
                    .setVlanPcp((byte) 0);
        } else {
            metabuilder = DefaultTrafficSelector.builder();
            metabuilder.matchVlanId(VlanId.vlanId(SingleSwitchFibInstaller.ASSIGNED_VLAN));
        }
        treatment.setOutput(PortNumber.portNumber(1));
        int nextId = 11;
        NextObjective.Builder nextBuilder = DefaultNextObjective.builder()
                .withId(nextId)
                .addTreatment(treatment.build())
                .withType(NextObjective.Type.SIMPLE)
                .fromApp(APPID);
        if (metabuilder != null) {
            nextBuilder.withMeta(metabuilder.build());
        }

        NextObjective nextObjective = nextBuilder.add();
        flowObjectiveService.next(deviceId, nextObjective);

        //set up the flowObjective
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

        Integer nextId1 = 11;
        fwdBuilder.nextStep(nextId1);
        flowObjectiveService.forward(deviceId, fwdBuilder.add());
        EasyMock.expectLastCall().once();
        setUpFlowObjectiveService();

        // Send in the UPDATE FibUpdate
        FibUpdate fibUpdate = new FibUpdate(FibUpdate.Type.UPDATE, fibEntry);
        fibListener.update(Collections.singleton(fibUpdate), Collections.emptyList());

        verify(flowObjectiveService);
    }

    /**
     * Tests updating a FIB entry.
     *
     * We verify that the flowObjectiveService records the correct state and that the
     * correct flow is submitted to the flowObjectiveService.
     */
    @Test
    public void testFibUpdate() {
        // Firstly add a route
        testFibAdd();
        reset(flowObjectiveService);
        IpPrefix prefix = Ip4Prefix.valueOf("1.1.1.0/24");
        // Start to construct a new route entry and new intent
        FibEntry fibEntryUpdate = new FibEntry(prefix,
                Ip4Address.valueOf("192.168.20.1"),
                MacAddress.valueOf("00:00:00:00:00:02"));

        //create the next Objective
        Interface egressIntf = interfaceService.getMatchingInterface(fibEntryUpdate.nextHopIp());
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder()
                .setEthSrc(MacAddress.valueOf("00:00:00:00:00:02"))
                .setEthDst(MacAddress.valueOf("00:00:00:00:00:02"));
        TrafficSelector.Builder metabuilder = null;
        if (!egressIntf.vlan().equals(VlanId.NONE)) {
            treatment.pushVlan()
                    .setVlanId(egressIntf.vlan())
                    .setVlanPcp((byte) 0);
        } else {
            metabuilder = DefaultTrafficSelector.builder();
            metabuilder.matchVlanId(VlanId.vlanId(SingleSwitchFibInstaller.ASSIGNED_VLAN));
        }
        treatment.setOutput(PortNumber.portNumber(1));
        int nextId = 11;
        NextObjective.Builder nextBuilder = DefaultNextObjective.builder()
                .withId(nextId)
                .addTreatment(treatment.build())
                .withType(NextObjective.Type.SIMPLE)
                .fromApp(APPID);
        if (metabuilder != null) {
            nextBuilder.withMeta(metabuilder.build());
        }

        NextObjective nextObjective = nextBuilder.add();
        flowObjectiveService.next(deviceId, nextObjective);

        //set up the flowObjective
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

        Integer nextId1 = 11;
        fwdBuilder.nextStep(nextId1);
        flowObjectiveService.forward(deviceId, fwdBuilder.add());
        EasyMock.expectLastCall().once();
        setUpFlowObjectiveService();

        // Send in the UPDATE FibUpdate
        FibUpdate fibUpdate = new FibUpdate(FibUpdate.Type.UPDATE,
                fibEntryUpdate);
        fibListener.update(Collections.singletonList(fibUpdate),
                Collections.emptyList());

        verify(flowObjectiveService);
    }

     /**
     * Tests deleting a FIB entry.
     *
     * We verify that the flowObjectiveService records the correct state and that the
     * correct flow is withdrawn from the flowObjectiveService.
     */

    @Test
    public void testFibDelete() {
        // Firstly add a route
        testFibAdd();
        IpPrefix prefix = Ip4Prefix.valueOf("1.1.1.0/24");

        // Construct the existing route entry
        FibEntry fibEntry = new FibEntry(prefix, null, null);

        //set up the flowObjective
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
        fwdBuilder.withTreatment(DefaultTrafficTreatment.builder().build());
        reset(flowObjectiveService);
        flowObjectiveService.forward(deviceId, fwdBuilder.remove());
        replay(flowObjectiveService);

        // Send in the DELETE FibUpdate
        FibUpdate fibUpdate = new FibUpdate(FibUpdate.Type.DELETE, fibEntry);
        fibListener.update(Collections.emptyList(), Collections.singletonList(fibUpdate));

        verify(flowObjectiveService);
    }

    private class TestCoreService extends CoreServiceAdapter {

        @Override
        public ApplicationId getAppId(String name) {
            return APPID;
        }

        @Override
        public ApplicationId registerApplication(String name) {
            return APPID;
        }
    }

    private class TestRoutingService extends RoutingServiceAdapter {

        @Override
        public void addFibListener(FibListener fibListener) {
            SingleSwitchFibInstallerTest.this.fibListener = fibListener;
        }
    }

    private class TestRouterConfig extends RouterConfig {

        @Override
        public List<String> getInterfaces() {
            ArrayList<String> interfaces = new ArrayList<>();
            interfaces.add("of:0000000000000001/1");
            interfaces.add("of:0000000000000002/1");
            interfaces.add("of:0000000000000003/1");
            interfaces.add("of:0000000000000004/1");
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
