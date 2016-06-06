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
import org.onlab.packet.EthType;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.app.ApplicationService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.incubator.net.intf.Interface;
import org.onosproject.incubator.net.intf.InterfaceEvent;
import org.onosproject.incubator.net.intf.InterfaceListener;
import org.onosproject.incubator.net.intf.InterfaceService;
import org.onosproject.incubator.net.intf.InterfaceServiceAdapter;
import org.onosproject.mastership.MastershipService;
import org.onosproject.mastership.MastershipServiceAdapter;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.NetTestTools;
import org.onosproject.net.PortNumber;
import org.onosproject.net.config.Config;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigEvent.Type;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.NetworkConfigServiceAdapter;
import org.onosproject.net.device.DeviceEvent;
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
import org.onosproject.net.host.HostService;
import org.onosproject.net.host.InterfaceIpAddress;
import org.onosproject.routing.RoutingService;
import org.onosproject.routing.config.RouterConfig;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

/**
 * UnitTests for ControlPlaneRedirectManager.
 */
public class ControlPlaneRedirectManagerTest {

    private DeviceService deviceService;
    private FlowObjectiveService flowObjectiveService;
    private NetworkConfigService networkConfigService;
    private final Set<Interface> interfaces = Sets.newHashSet();
    static Device dev3 = NetTestTools.device("0000000000000001");
    private static final int OSPF_IP_PROTO = 0x59;
    private CoreService coreService = new TestCoreService();
    private InterfaceService interfaceService;
    private static final ApplicationId APPID = TestApplicationId.create("org.onosproject.vrouter");

    private static final DeviceId DEVICE_ID = DeviceId.deviceId("of:0000000000000001");

    private ConnectPoint controlPlaneConnectPoint = new ConnectPoint(DEVICE_ID,
            PortNumber.portNumber(1));

    private static final ConnectPoint SW1_ETH1 = new ConnectPoint(DEVICE_ID,
            PortNumber.portNumber(1));

    private static final ConnectPoint SW1_ETH2 = new ConnectPoint(DEVICE_ID,
            PortNumber.portNumber(2));

    private static final ConnectPoint SW1_ETH3 = new ConnectPoint(DEVICE_ID,
            PortNumber.portNumber(3));

    private ControlPlaneRedirectManager controlPlaneRedirectManager = new ControlPlaneRedirectManager();
    private RouterConfig routerConfig = new TestRouterConfig();
    private NetworkConfigListener networkConfigListener;
    private DeviceListener deviceListener;
    private MastershipService mastershipService = new InternalMastershipServiceTest();
    private InterfaceListener interfaceListener;
    private ApplicationService applicationService;

    @Before
    public void setUp() {
        networkConfigListener = createMock(NetworkConfigListener.class);
        deviceService = new TestDeviceService();
        deviceListener = createMock(DeviceListener.class);

        interfaceListener = createMock(InterfaceListener.class);
        deviceService.addListener(deviceListener);
        setUpInterfaceService();
        interfaceService = new InternalInterfaceService();
        interfaceService.addListener(interfaceListener);
        networkConfigService = new TestNetworkConfigService();
        networkConfigService.addListener(networkConfigListener);
        flowObjectiveService = createMock(FlowObjectiveService.class);
        applicationService = createNiceMock(ApplicationService.class);
        replay(applicationService);
        setUpFlowObjectiveService();
        controlPlaneRedirectManager.coreService = coreService;
        controlPlaneRedirectManager.flowObjectiveService = flowObjectiveService;
        controlPlaneRedirectManager.networkConfigService = networkConfigService;
        controlPlaneRedirectManager.interfaceService = interfaceService;
        controlPlaneRedirectManager.deviceService = deviceService;
        controlPlaneRedirectManager.hostService = createNiceMock(HostService.class);
        controlPlaneRedirectManager.mastershipService = mastershipService;
        controlPlaneRedirectManager.applicationService = applicationService;
        controlPlaneRedirectManager.activate();
        verify(flowObjectiveService);
    }

    /**
     * Tests adding new Device to a openflow router.
     */
    @Test
    public void testAddDevice() {
        ConnectPoint sw1eth4 = new ConnectPoint(DEVICE_ID, PortNumber.portNumber(4));
        List<InterfaceIpAddress> interfaceIpAddresses4 = new ArrayList<>();
        interfaceIpAddresses4
                .add(new InterfaceIpAddress(IpAddress.valueOf("192.168.40.101"), IpPrefix.valueOf("192.168.40.0/24")));

        Interface sw1Eth4 = new Interface(sw1eth4.deviceId().toString(), sw1eth4, interfaceIpAddresses4,
                MacAddress.valueOf("00:00:00:00:00:04"), VlanId.NONE);
        interfaces.add(sw1Eth4);
        EasyMock.reset(flowObjectiveService);
        setUpFlowObjectiveService();
        deviceListener.event(new DeviceEvent(DeviceEvent.Type.DEVICE_AVAILABILITY_CHANGED, dev3));
        verify(flowObjectiveService);
    }

    /**
     * Tests adding while updating the networkConfig.
     */
    @Test
    public void testUpdateNetworkConfig() {
        ConnectPoint sw1eth4 = new ConnectPoint(DEVICE_ID, PortNumber.portNumber(4));
        List<InterfaceIpAddress> interfaceIpAddresses4 = new ArrayList<>();
        interfaceIpAddresses4
                .add(new InterfaceIpAddress(IpAddress.valueOf("192.168.40.101"), IpPrefix.valueOf("192.168.40.0/24")));

        Interface sw1Eth4 = new Interface(sw1eth4.deviceId().toString(), sw1eth4, interfaceIpAddresses4,
                MacAddress.valueOf("00:00:00:00:00:04"), VlanId.NONE);
        interfaces.add(sw1Eth4);
        EasyMock.reset(flowObjectiveService);
        setUpFlowObjectiveService();
        networkConfigListener
                .event(new NetworkConfigEvent(Type.CONFIG_UPDATED, dev3, RoutingService.ROUTER_CONFIG_CLASS));
        networkConfigService.addListener(networkConfigListener);
        verify(flowObjectiveService);
    }

    /**
     * Tests adding while updating the networkConfig.
     */
    @Test
    public void testAddInterface() {
        ConnectPoint sw1eth4 = new ConnectPoint(DEVICE_ID, PortNumber.portNumber(4));
        List<InterfaceIpAddress> interfaceIpAddresses4 = new ArrayList<>();
        interfaceIpAddresses4
                .add(new InterfaceIpAddress(IpAddress.valueOf("192.168.40.101"), IpPrefix.valueOf("192.168.40.0/24")));

        Interface sw1Eth4 = new Interface(sw1eth4.deviceId().toString(), sw1eth4, interfaceIpAddresses4,
                MacAddress.valueOf("00:00:00:00:00:04"), VlanId.NONE);
        interfaces.add(sw1Eth4);

        EasyMock.reset(flowObjectiveService);
        expect(flowObjectiveService.allocateNextId()).andReturn(1).anyTimes();

        setUpInterfaceConfiguration(sw1Eth4, true);
        replay(flowObjectiveService);
        interfaceListener.event(new InterfaceEvent(InterfaceEvent.Type.INTERFACE_ADDED, sw1Eth4, 500L));
        verify(flowObjectiveService);
    }

    @Test
    public void testRemoveInterface() {
        ConnectPoint sw1eth4 = new ConnectPoint(DEVICE_ID, PortNumber.portNumber(4));
        List<InterfaceIpAddress> interfaceIpAddresses4 = new ArrayList<>();
        interfaceIpAddresses4
                .add(new InterfaceIpAddress(IpAddress.valueOf("192.168.40.101"), IpPrefix.valueOf("192.168.40.0/24")));

        Interface sw1Eth4 = new Interface(sw1eth4.deviceId().toString(), sw1eth4, interfaceIpAddresses4,
                MacAddress.valueOf("00:00:00:00:00:04"), VlanId.NONE);
        EasyMock.reset(flowObjectiveService);
        expect(flowObjectiveService.allocateNextId()).andReturn(1).anyTimes();

        setUpInterfaceConfiguration(sw1Eth4, false);
        replay(flowObjectiveService);
        interfaceListener.event(new InterfaceEvent(InterfaceEvent.Type.INTERFACE_REMOVED, sw1Eth4, 500L));
        verify(flowObjectiveService);
    }

    /**
     * Setup flow Configuration for all configured Interfaces.
     *
     **/
    private void setUpFlowObjectiveService() {
        expect(flowObjectiveService.allocateNextId()).andReturn(1).anyTimes();
        for (Interface intf : interfaceService.getInterfaces()) {
            setUpInterfaceConfiguration(intf, true);
        }
        replay(flowObjectiveService);
    }

    /**
     * Setting up flowobjective expectations for basic forwarding and ospf.
     **/
    private void setUpInterfaceConfiguration(Interface intf, boolean install) {
        DeviceId deviceId = controlPlaneConnectPoint.deviceId();
        PortNumber controlPlanePort = controlPlaneConnectPoint.port();

        for (InterfaceIpAddress ip : intf.ipAddresses()) {
            int cpNextId, intfNextId;

            cpNextId = modifyNextObjective(deviceId, controlPlanePort,
                    VlanId.vlanId(SingleSwitchFibInstaller.ASSIGNED_VLAN), true, install);
            intfNextId = modifyNextObjective(deviceId, intf.connectPoint().port(),
                    VlanId.vlanId(SingleSwitchFibInstaller.ASSIGNED_VLAN), true, install);

            // IPv4 to router
            TrafficSelector toSelector = DefaultTrafficSelector.builder().matchInPort(intf.connectPoint().port())
                    .matchEthDst(intf.mac()).matchEthType(EthType.EtherType.IPV4.ethType().toShort())
                    .matchVlanId(intf.vlan()).matchIPDst(ip.ipAddress().toIpPrefix()).build();

            flowObjectiveService.forward(deviceId, buildForwardingObjective(toSelector, null, cpNextId, install));
            expectLastCall().once();
            // IPv4 from router
            TrafficSelector fromSelector = DefaultTrafficSelector.builder().matchInPort(controlPlanePort)
                    .matchEthSrc(intf.mac()).matchVlanId(intf.vlan())
                    .matchEthType(EthType.EtherType.IPV4.ethType().toShort()).matchIPSrc(ip.ipAddress().toIpPrefix())
                    .build();

            flowObjectiveService.forward(deviceId, buildForwardingObjective(fromSelector, null, intfNextId, install));
            expectLastCall().once();
            // ARP to router
            toSelector = DefaultTrafficSelector.builder().matchInPort(intf.connectPoint().port())
                    .matchEthType(EthType.EtherType.ARP.ethType().toShort()).matchVlanId(intf.vlan()).build();

            TrafficTreatment puntTreatment = DefaultTrafficTreatment.builder().punt().build();

            flowObjectiveService.forward(deviceId,
                    buildForwardingObjective(toSelector, puntTreatment, cpNextId, install));
            expectLastCall().once();
            // ARP from router
            fromSelector = DefaultTrafficSelector.builder().matchInPort(controlPlanePort).matchEthSrc(intf.mac())
                    .matchVlanId(intf.vlan()).matchEthType(EthType.EtherType.ARP.ethType().toShort())
                    .matchArpSpa(ip.ipAddress().getIp4Address()).build();

            flowObjectiveService.forward(deviceId,
                    buildForwardingObjective(fromSelector, puntTreatment, intfNextId, install));
            expectLastCall().once();
        }
        // setting expectations for ospf forwarding.
        TrafficSelector toSelector = DefaultTrafficSelector.builder().matchInPort(intf.connectPoint().port())
                .matchEthType(EthType.EtherType.IPV4.ethType().toShort()).matchVlanId(intf.vlan())
                .matchIPProtocol((byte) OSPF_IP_PROTO).build();

        modifyNextObjective(deviceId, controlPlanePort, VlanId.vlanId((short) 4094), true, install);
        flowObjectiveService.forward(controlPlaneConnectPoint.deviceId(),
                buildForwardingObjective(toSelector, null, 1, install));
        expectLastCall().once();
    }

    /**
     * Setup expectations on flowObjectiveService.next for NextObjective.
     *
     **/
    private int modifyNextObjective(DeviceId deviceId, PortNumber portNumber, VlanId vlanId, boolean popVlan,
            boolean modifyFlag) {
        NextObjective.Builder nextObjBuilder = DefaultNextObjective.builder().withId(1)
                .withType(NextObjective.Type.SIMPLE).fromApp(APPID);

        TrafficTreatment.Builder ttBuilder = DefaultTrafficTreatment.builder();
        if (popVlan) {
            ttBuilder.popVlan();
        }
        ttBuilder.setOutput(portNumber);

        TrafficSelector.Builder metabuilder = DefaultTrafficSelector.builder();
        metabuilder.matchVlanId(vlanId);

        nextObjBuilder.withMeta(metabuilder.build());
        nextObjBuilder.addTreatment(ttBuilder.build());
        if (modifyFlag) {
            flowObjectiveService.next(deviceId, nextObjBuilder.add());
            expectLastCall().once();
        } else {
            flowObjectiveService.next(deviceId, nextObjBuilder.remove());
            expectLastCall().once();
        }
        return 1;
    }

    /**
     * Setup Interface expectation for all Testcases.
     **/
    private void setUpInterfaceService() {
        List<InterfaceIpAddress> interfaceIpAddresses1 = new ArrayList<>();
        interfaceIpAddresses1
                .add(new InterfaceIpAddress(IpAddress.valueOf("192.168.10.101"), IpPrefix.valueOf("192.168.10.0/24")));
        Interface sw1Eth1 = new Interface(SW1_ETH1.deviceId().toString(), SW1_ETH1, interfaceIpAddresses1,
                MacAddress.valueOf("00:00:00:00:00:01"), VlanId.NONE);
        interfaces.add(sw1Eth1);

        List<InterfaceIpAddress> interfaceIpAddresses2 = new ArrayList<>();
        interfaceIpAddresses2
                .add(new InterfaceIpAddress(IpAddress.valueOf("192.168.20.101"), IpPrefix.valueOf("192.168.20.0/24")));
        Interface sw1Eth2 = new Interface(SW1_ETH1.deviceId().toString(), SW1_ETH2, interfaceIpAddresses2,
                MacAddress.valueOf("00:00:00:00:00:02"), VlanId.NONE);
        interfaces.add(sw1Eth2);

        List<InterfaceIpAddress> interfaceIpAddresses3 = new ArrayList<>();
        interfaceIpAddresses3
                .add(new InterfaceIpAddress(IpAddress.valueOf("192.168.30.101"), IpPrefix.valueOf("192.168.30.0/24")));
        Interface sw1Eth3 = new Interface(SW1_ETH1.deviceId().toString(), SW1_ETH3, interfaceIpAddresses3,
                MacAddress.valueOf("00:00:00:00:00:03"), VlanId.NONE);
        interfaces.add(sw1Eth3);

    }

    private ForwardingObjective buildForwardingObjective(TrafficSelector selector, TrafficTreatment treatment,
            int nextId, boolean add) {
        DefaultForwardingObjective.Builder fobBuilder = DefaultForwardingObjective.builder();
        fobBuilder.withSelector(selector);
        if (treatment != null) {
            fobBuilder.withTreatment(treatment);
        }
        if (nextId != -1) {
            fobBuilder.nextStep(nextId);
        }
        fobBuilder.fromApp(APPID).withPriority(40001).withFlag(ForwardingObjective.Flag.VERSATILE);

        return add ? fobBuilder.add() : fobBuilder.remove();
    }

    private class TestCoreService extends CoreServiceAdapter {

        @Override
        public ApplicationId getAppId(String name) {
            return APPID;
        }

        @Override
        public ApplicationId registerApplication(String name) {
            return new TestApplicationId(name);
        }

    }

    private class TestDeviceService extends DeviceServiceAdapter {

        @Override
        public boolean isAvailable(DeviceId deviceId) {
            boolean flag = false;
            if (deviceId.equals(controlPlaneConnectPoint.deviceId())) {
                flag = true;
            }
            return flag;
        }

        @Override
        public void addListener(DeviceListener listener) {
            ControlPlaneRedirectManagerTest.this.deviceListener = listener;
        }

    }

    private class TestRouterConfig extends RouterConfig {

        @Override
        public ConnectPoint getControlPlaneConnectPoint() {
            return controlPlaneConnectPoint;
        }

        @Override
        public boolean getOspfEnabled() {
            return true;
        }

        @Override
        public List<String> getInterfaces() {
            ArrayList<String> interfaces = new ArrayList<>();
            interfaces.add("of:0000000000000001");
            interfaces.add("of:0000000000000001/2");
            interfaces.add("of:0000000000000001/3");
            return interfaces;
        }

    }

    private class TestNetworkConfigService extends NetworkConfigServiceAdapter {

        @Override
        public void addListener(NetworkConfigListener listener) {
            ControlPlaneRedirectManagerTest.this.networkConfigListener = listener;
        }

        @Override
        public <S, C extends Config<S>> C getConfig(S subject, Class<C> configClass) {
            return (C) ControlPlaneRedirectManagerTest.this.routerConfig;
        }

    }

    private static class TestApplicationId implements ApplicationId {

        private final String name;
        private final short id;

        public TestApplicationId(String name) {
            this.name = name;
            this.id = (short) Objects.hash(name);
        }

        public static ApplicationId create(String name) {
            return new TestApplicationId(name);
        }

        @Override
        public short id() {
            return id;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + id;
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            TestApplicationId other = (TestApplicationId) obj;
            if (id != other.id) {
                return false;
            }
            if (name == null) {
                if (other.name != null) {
                    return false;
                }
            } else if (!name.equals(other.name)) {
                return false;
            }
            return true;
        }
    }

    private class InternalMastershipServiceTest extends MastershipServiceAdapter {

        @Override
        public boolean isLocalMaster(DeviceId deviceId) {
            boolean flag = deviceId.equals(controlPlaneConnectPoint.deviceId());
            return flag;
        }

    }

    private class InternalInterfaceService extends InterfaceServiceAdapter {

        @Override
        public void addListener(InterfaceListener listener) {
            ControlPlaneRedirectManagerTest.this.interfaceListener = listener;
        }

        @Override
        public Set<Interface> getInterfaces() {
            return interfaces;
        }

        @Override
        public Set<Interface> getInterfacesByPort(ConnectPoint port) {
            Set<Interface> setIntf = new HashSet<Interface>();
            for (Interface intf : interfaces) {
                if (intf.connectPoint().equals(port)) {
                    setIntf.add(intf);
                }
            }
            return setIntf;
        }

        @Override
        public Interface getMatchingInterface(IpAddress ip) {
            Interface intff = null;
            for (Interface intf : interfaces) {
                for (InterfaceIpAddress address : intf.ipAddresses()) {
                    if (address.ipAddress().equals(ip)) {
                        intff = intf;
                        break;
                    }
                }
            }

            return intff;
        }

    }
}
