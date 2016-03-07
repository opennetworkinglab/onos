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

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.EthType;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.incubator.net.intf.Interface;
import org.onosproject.incubator.net.intf.InterfaceService;
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
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.onosproject.net.host.HostServiceAdapter;
import org.onosproject.net.host.InterfaceIpAddress;
import org.onosproject.net.intent.AbstractIntentTest;
import org.onosproject.routing.RoutingService;
import org.onosproject.routing.config.RouterConfig;
import org.slf4j.Logger;

import com.google.common.collect.Sets;

/**
 * UnitTests for ControlPlaneRedirectManager.
 */
public class ControlPlaneRedirectManagerTest extends AbstractIntentTest {

    private final Logger log = getLogger(getClass());

    private DeviceService deviceService;
    private FlowObjectiveService flowObjectiveService;
    private NetworkConfigService networkConfigService;
    private final Set<Interface> interfaces = Sets.newHashSet();
    static Device dev3 = NetTestTools.device("0000000000000001");
    private static final int OSPF_IP_PROTO = 0x59;
    private CoreService coreService = new TestCoreService();
    private InterfaceService interfaceService;
    private static final ApplicationId APPID = TestApplicationId.create("org.onosproject.cpredirect");

    /**
     * Interface Configuration.
     *
     **/
    private ConnectPoint controlPlaneConnectPoint = new ConnectPoint(DeviceId.deviceId("of:0000000000000001"),
            PortNumber.portNumber(1));
    private static final ConnectPoint SW1_ETH1 = new ConnectPoint(DeviceId.deviceId("of:0000000000000001"),
            PortNumber.portNumber(1));

    private static final ConnectPoint SW1_ETH2 = new ConnectPoint(DeviceId.deviceId("of:0000000000000001"),
            PortNumber.portNumber(2));

    private static final ConnectPoint SW1_ETH3 = new ConnectPoint(DeviceId.deviceId("of:0000000000000001"),
            PortNumber.portNumber(3));
    protected HostService hostService;

    private ControlPlaneRedirectManager controlPlaneRedirectManager = new ControlPlaneRedirectManager();;
    private RouterConfig routerConfig = new TestRouterConfig();
    private NetworkConfigListener networkConfigListener;
    private DeviceListener deviceListener;
    private MastershipService mastershipService = new InternalMastershipServiceTest();
    private HostListener hostListener;

    /**
     * @throws java.lang.Exception
     */
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        interfaceService = createMock(InterfaceService.class);
        networkConfigListener = createMock(NetworkConfigListener.class);
        hostService = new TestHostService();
        deviceService = new TestDeviceService();
        deviceListener = createMock(DeviceListener.class);
        hostListener = createMock(HostListener.class);
        hostService.addListener(hostListener);
        deviceService.addListener(deviceListener);
        setUpInterfaceService();
        networkConfigService = new TestNetworkConfigService();
        networkConfigService.addListener(networkConfigListener);
        flowObjectiveService = createMock(FlowObjectiveService.class);
        setUpFlowObjectiveService();

        controlPlaneRedirectManager.coreService = coreService;
        controlPlaneRedirectManager.flowObjectiveService = flowObjectiveService;
        controlPlaneRedirectManager.networkConfigService = networkConfigService;
        controlPlaneRedirectManager.interfaceService = interfaceService;
        controlPlaneRedirectManager.deviceService = deviceService;
        controlPlaneRedirectManager.hostService = hostService;
        controlPlaneRedirectManager.mastershipService = mastershipService;
        controlPlaneRedirectManager.activate();
        verify(flowObjectiveService);
    }
    /**
     * setup flow Configuration for all configured Interfaces.
     *
     **/
    private void setUpFlowObjectiveService() {

        expect(flowObjectiveService.allocateNextId()).andReturn(1).anyTimes();
        DeviceId deviceId = controlPlaneConnectPoint.deviceId();
        PortNumber controlPlanePort = controlPlaneConnectPoint.port();
        int cpNextId, intfNextId;
        for (Interface intf : interfaceService.getInterfaces()) {
            for (InterfaceIpAddress ip : intf.ipAddresses()) {
                if (intf.vlan() == VlanId.NONE) {
                    // cpNextId = 1;
                    // intfNextId = 1;
                    cpNextId = createNextObjective(deviceId, controlPlanePort,
                            VlanId.vlanId(SingleSwitchFibInstaller.ASSIGNED_VLAN), true);
                    intfNextId = createNextObjective(deviceId, intf.connectPoint().port(),
                            VlanId.vlanId(SingleSwitchFibInstaller.ASSIGNED_VLAN), true);
                } else {
                    // cpNextId = 1;
                    // intfNextId = 1;

                    cpNextId = createNextObjective(deviceId, controlPlanePort, intf.vlan(), false);
                    intfNextId = createNextObjective(deviceId, intf.connectPoint().port(), intf.vlan(), false);
                }
                TrafficSelector toSelector = DefaultTrafficSelector.builder().matchInPort(intf.connectPoint().port())
                        .matchEthDst(intf.mac()).matchEthType(EthType.EtherType.IPV4.ethType().toShort())
                        .matchVlanId(intf.vlan()).matchIPDst(ip.ipAddress().toIpPrefix()).build();
                flowObjectiveService.forward(deviceId, buildForwardingObjective(toSelector, null, cpNextId, true));
                expectLastCall().once();
                // IPv4 from router
                TrafficSelector fromSelector = DefaultTrafficSelector.builder().matchInPort(controlPlanePort)
                        .matchEthSrc(intf.mac()).matchVlanId(intf.vlan())
                        .matchEthType(EthType.EtherType.IPV4.ethType().toShort())
                        .matchIPSrc(ip.ipAddress().toIpPrefix()).build();
                flowObjectiveService.forward(deviceId, buildForwardingObjective(fromSelector, null, intfNextId, true));
                expectLastCall().once();
                // ARP to router
                toSelector = DefaultTrafficSelector.builder().matchInPort(intf.connectPoint().port())
                        .matchEthType(EthType.EtherType.ARP.ethType().toShort()).matchVlanId(intf.vlan()).build();

                TrafficTreatment puntTreatment = DefaultTrafficTreatment.builder().punt().build();
                flowObjectiveService.forward(deviceId,
                        buildForwardingObjective(toSelector, puntTreatment, cpNextId, true));
                expectLastCall().once();
                // ARP from router
                fromSelector = DefaultTrafficSelector.builder().matchInPort(controlPlanePort).matchEthSrc(intf.mac())
                        .matchVlanId(intf.vlan()).matchEthType(EthType.EtherType.ARP.ethType().toShort())
                        .matchArpSpa(ip.ipAddress().getIp4Address()).build();
                flowObjectiveService.forward(deviceId,
                        buildForwardingObjective(fromSelector, puntTreatment, intfNextId, true));
                expectLastCall().once();

            }
            updateOspfForwarding(intf);
        }

        replay(flowObjectiveService);
    }

    /**
     * setup expectations on flowobjectService.forward for ospfForwarding.
     *
     **/
    private void updateOspfForwarding(Interface intf) {
        // OSPF to router
        TrafficSelector toSelector = DefaultTrafficSelector.builder().matchInPort(intf.connectPoint().port())
                .matchEthType(EthType.EtherType.IPV4.ethType().toShort()).matchVlanId(intf.vlan())
                .matchIPProtocol((byte) OSPF_IP_PROTO).build();

        PortNumber controlPlanePort = controlPlaneConnectPoint.port();
        DeviceId deviceId = controlPlaneConnectPoint.deviceId();
        int cpNextId;
        if (intf.vlan() == VlanId.NONE) {
            cpNextId = createNextObjective(deviceId, controlPlanePort,
                    VlanId.vlanId(SingleSwitchFibInstaller.ASSIGNED_VLAN), true);
        } else {
            cpNextId = createNextObjective(deviceId, controlPlanePort, intf.vlan(), false);
        }
        log.debug("ospf flows intf:{} nextid:{}", intf, cpNextId);
        flowObjectiveService.forward(controlPlaneConnectPoint.deviceId(),
                buildForwardingObjective(toSelector, null, cpNextId, true));
        expectLastCall().once();
    }

    /**
     * setup expectations on flowObjectiveService.next for NextObjective.
     *
     **/
    private int createNextObjective(DeviceId deviceId, PortNumber portNumber, VlanId vlanId, boolean popVlan) {

        NextObjective.Builder nextObjBuilder = DefaultNextObjective.builder().withId(1)
                .withType(NextObjective.Type.SIMPLE).fromApp(APPID);

        TrafficTreatment.Builder ttBuilder = DefaultTrafficTreatment.builder();
        if (popVlan) {
            ttBuilder.popVlan();
        }
        ttBuilder.setOutput(portNumber);

        // setup metadata to pass to nextObjective - indicate the vlan on egress
        // if needed by the switch pipeline.
        TrafficSelector.Builder metabuilder = DefaultTrafficSelector.builder();
        metabuilder.matchVlanId(vlanId);

        nextObjBuilder.withMeta(metabuilder.build());
        nextObjBuilder.addTreatment(ttBuilder.build());
        log.debug("Submited next objective {} in device {} for port/vlan {}/{}", 1, deviceId, portNumber, vlanId);
        flowObjectiveService.next(deviceId, nextObjBuilder.add());
        expectLastCall().once();
        return 1;
    }

    /**
     * setup Interface expectation for all Testcases.
     *
     **/
    private void setUpInterfaceService() {

        Set<InterfaceIpAddress> interfaceIpAddresses1 = Sets.newHashSet();
        interfaceIpAddresses1
                .add(new InterfaceIpAddress(IpAddress.valueOf("192.168.10.101"), IpPrefix.valueOf("192.168.10.0/24")));
        Interface sw1Eth1 = new Interface(SW1_ETH1.deviceId().toString(), SW1_ETH1, interfaceIpAddresses1,
                MacAddress.valueOf("00:00:00:00:00:01"), VlanId.NONE);
        interfaces.add(sw1Eth1);

        Set<InterfaceIpAddress> interfaceIpAddresses2 = Sets.newHashSet();
        interfaceIpAddresses2
                .add(new InterfaceIpAddress(IpAddress.valueOf("192.168.20.101"), IpPrefix.valueOf("192.168.20.0/24")));
        Interface sw1Eth2 = new Interface(SW1_ETH1.deviceId().toString(), SW1_ETH2, interfaceIpAddresses2,
                MacAddress.valueOf("00:00:00:00:00:02"), VlanId.NONE);
        interfaces.add(sw1Eth2);

        Set<InterfaceIpAddress> interfaceIpAddresses3 = Sets.newHashSet();
        interfaceIpAddresses3
                .add(new InterfaceIpAddress(IpAddress.valueOf("192.168.30.101"), IpPrefix.valueOf("192.168.30.0/24")));
        Interface sw1Eth3 = new Interface(SW1_ETH1.deviceId().toString(), SW1_ETH3, interfaceIpAddresses3,
                MacAddress.valueOf("00:00:00:00:00:03"), VlanId.NONE);
        interfaces.add(sw1Eth3);

        expect(interfaceService.getInterfacesByPort(SW1_ETH1)).andReturn(Collections.singleton(sw1Eth1)).anyTimes();
        expect(interfaceService.getMatchingInterface(Ip4Address.valueOf("192.168.10.1"))).andReturn(sw1Eth1).anyTimes();
        expect(interfaceService.getInterfacesByPort(SW1_ETH2)).andReturn(Collections.singleton(sw1Eth2)).anyTimes();
        expect(interfaceService.getMatchingInterface(Ip4Address.valueOf("192.168.20.1"))).andReturn(sw1Eth2).anyTimes();

        expect(interfaceService.getInterfacesByPort(SW1_ETH3)).andReturn(Collections.singleton(sw1Eth3)).anyTimes();
        expect(interfaceService.getMatchingInterface(Ip4Address.valueOf("192.168.30.1"))).andReturn(sw1Eth3).anyTimes();
        expect(interfaceService.getInterfaces()).andReturn(interfaces).anyTimes();
        replay(interfaceService);
        interfaceService.getInterfaces();
        verify(interfaceService);
    }

    /**
     * Tests adding new Device to a  controlplane.
     *
     * @throws SecurityException
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */

    @Test
    public void testAddDevice()
            throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {

        ConnectPoint sw1eth4 = new ConnectPoint(DeviceId.deviceId("of:0000000000000001"), PortNumber.portNumber(4));
        Set<InterfaceIpAddress> interfaceIpAddresses4 = Sets.newHashSet();
        interfaceIpAddresses4
                .add(new InterfaceIpAddress(IpAddress.valueOf("192.168.40.101"), IpPrefix.valueOf("192.168.40.0/24")));

        Interface sw1Eth4 = new Interface(sw1eth4.deviceId().toString(), sw1eth4, interfaceIpAddresses4,
                MacAddress.valueOf("00:00:00:00:00:04"), VlanId.NONE);
        interfaces.add(sw1Eth4);
        EasyMock.reset(interfaceService);
        expect(interfaceService.getInterfacesByPort(sw1eth4)).andReturn(Collections.singleton(sw1Eth4)).anyTimes();
        expect(interfaceService.getMatchingInterface(Ip4Address.valueOf("192.168.40.1"))).andReturn(sw1Eth4).anyTimes();
        expect(interfaceService.getInterfaces()).andReturn(interfaces).anyTimes();
        replay(interfaceService);
        interfaceService.getInterfaces();
        EasyMock.reset(flowObjectiveService);
        setUpFlowObjectiveService();
        deviceListener.event(new DeviceEvent(DeviceEvent.Type.DEVICE_AVAILABILITY_CHANGED, dev3));
        deviceService.addListener(deviceListener);

        verify(flowObjectiveService);
    }

    /**
     * Tests adding while updating the networkConfig.
     *
     * @throws SecurityException
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    @Test
    public void testUpdateNetworkConfig()
            throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {

        ConnectPoint sw1eth4 = new ConnectPoint(DeviceId.deviceId("of:0000000000000001"), PortNumber.portNumber(4));
        Set<InterfaceIpAddress> interfaceIpAddresses4 = Sets.newHashSet();
        interfaceIpAddresses4
                .add(new InterfaceIpAddress(IpAddress.valueOf("192.168.40.101"), IpPrefix.valueOf("192.168.40.0/24")));

        Interface sw1Eth4 = new Interface(sw1eth4.deviceId().toString(), sw1eth4, interfaceIpAddresses4,
                MacAddress.valueOf("00:00:00:00:00:04"), VlanId.NONE);
        interfaces.add(sw1Eth4);
        EasyMock.reset(interfaceService);
        expect(interfaceService.getInterfacesByPort(sw1eth4)).andReturn(Collections.singleton(sw1Eth4)).anyTimes();
        expect(interfaceService.getMatchingInterface(Ip4Address.valueOf("192.168.40.1"))).andReturn(sw1Eth4).anyTimes();
        expect(interfaceService.getInterfaces()).andReturn(interfaces).anyTimes();
        replay(interfaceService);
        interfaceService.getInterfaces();
        EasyMock.reset(flowObjectiveService);
        setUpFlowObjectiveService();
        networkConfigListener
                .event(new NetworkConfigEvent(Type.CONFIG_UPDATED, dev3, RoutingService.ROUTER_CONFIG_CLASS));
        // deviceService.addListener(deviceListener);
        networkConfigService.addListener(networkConfigListener);
        verify(flowObjectiveService);
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

    /**
     * @param intf
     */

    private class TestCoreService extends CoreServiceAdapter {
        @Override
        public ApplicationId getAppId(String name) {
            return APPID;
        }

        @Override
        public ApplicationId registerApplication(String name) {
            // TODO Auto-generated method stub
            return new TestApplicationId(name);
        }

    }

    private class TestDeviceService extends DeviceServiceAdapter {

        @Override
        public boolean isAvailable(DeviceId deviceId) {
            // TODO Auto-generated method stub
            boolean flag = false;
            if (deviceId.equals(controlPlaneConnectPoint.deviceId())) {
                flag = true;
            }
            return flag;
        }

        @Override
        public void addListener(DeviceListener listener) {
            // TODO Auto-generated method stub
            ControlPlaneRedirectManagerTest.this.deviceListener = listener;
        }

    }

    private class TestHostService extends HostServiceAdapter {

        @Override
        public void addListener(HostListener listener) {
            // TODO Auto-generated method stub
            ControlPlaneRedirectManagerTest.this.hostListener = listener;
        }

    }

    private class TestRouterConfig extends RouterConfig {

        @Override
        public ConnectPoint getControlPlaneConnectPoint() {
            // TODO Auto-generated method stub
            return controlPlaneConnectPoint;
        }

        @Override
        public boolean getOspfEnabled() {
            // TODO Auto-generated method stub
            return true;
        }

        @Override
        public List<String> getInterfaces() {
            // TODO Auto-generated method stub

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
            // TODO Auto-generated method stub
            ControlPlaneRedirectManagerTest.this.networkConfigListener = listener;
        }

        @Override
        public <S, C extends Config<S>> C getConfig(S subject, Class<C> configClass) {
            // TODO Auto-generated method stub

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
            // TODO Auto-generated method stub
            boolean flag = deviceId.equals(controlPlaneConnectPoint.deviceId());
            return flag;
        }

    }
}
