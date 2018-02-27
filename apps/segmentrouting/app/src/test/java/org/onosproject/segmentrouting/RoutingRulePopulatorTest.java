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

package org.onosproject.segmentrouting;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.VlanId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.DefaultPort;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.intf.Interface;
import org.onosproject.net.intf.InterfaceService;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.segmentrouting.config.DeviceConfiguration;

import java.util.List;
import java.util.Set;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.*;

public class RoutingRulePopulatorTest {
    private RoutingRulePopulator rrp;
    private SegmentRoutingManager srManager;
    private InterfaceService interfaceService;
    private DeviceService deviceService;

    private final DeviceId devId1 = DeviceId.deviceId("of:1");
    private final Device dev1 = new DefaultDevice(ProviderId.NONE, devId1, Device.Type.SWITCH,
            null, null, null, null, null);

    private final PortNumber p1 = PortNumber.portNumber(1);
    private final PortNumber p2 = PortNumber.portNumber(2);
    private final PortNumber p3 = PortNumber.portNumber(3);
    private final PortNumber p4 = PortNumber.portNumber(4);
    private final PortNumber p5 = PortNumber.portNumber(5);

    private final VlanId v10 = VlanId.vlanId((short) 10);
    private final VlanId v20 = VlanId.vlanId((short) 20);
    private final VlanId vInt = SegmentRoutingManager.INTERNAL_VLAN;

    private final Interface u10 = new Interface(null, new ConnectPoint(devId1, p1),
            null, null, null, v10, null, null);
    private final Interface t10 = new Interface(null, new ConnectPoint(devId1, p2),
            null, null, null, null, Sets.newHashSet(v10), null);
    private final Interface t10n20 = new Interface(null, new ConnectPoint(devId1, p3),
            null, null, null, null, Sets.newHashSet(v10), v20);

    @Before
    public void setUp() throws Exception {
        Set<Interface> interfaces = Sets.newHashSet(u10, t10, t10n20);
        interfaceService = new MockInterfaceService(interfaces);
        deviceService = EasyMock.createMock(DeviceService.class);
        srManager = new MockSegmentRoutingManager(Maps.newHashMap());
        srManager.deviceConfiguration =  EasyMock.createMock(DeviceConfiguration.class);
        srManager.interfaceService = interfaceService;
        srManager.deviceService = deviceService;
        rrp = new RoutingRulePopulator(srManager);
    }

    // All ports are enabled
    @Test
    public void testNoMoreEnabledPortCase1() throws Exception {
        Port port1 = new DefaultPort(dev1, p1, true);
        Port port2 = new DefaultPort(dev1, p2, true);
        Port port3 = new DefaultPort(dev1, p3, true);
        Port port4 = new DefaultPort(dev1, p4, true);
        Port port5 = new DefaultPort(dev1, p5, true);
        List<Port> ports = Lists.newArrayList(port1, port2, port3, port4, port5);

        expect(deviceService.getPorts(anyObject(DeviceId.class))).andReturn(ports).anyTimes();
        replay(deviceService);
        assertFalse(rrp.noMoreEnabledPort(devId1, v10));
        assertFalse(rrp.noMoreEnabledPort(devId1, v20));
        assertFalse(rrp.noMoreEnabledPort(devId1, vInt));
    }

    // Disable port 1
    @Test
    public void testNoMoreEnabledPortCase2() throws Exception {
        Port port1 = new DefaultPort(dev1, p1, false);
        Port port2 = new DefaultPort(dev1, p2, true);
        Port port3 = new DefaultPort(dev1, p3, true);
        Port port4 = new DefaultPort(dev1, p4, true);
        Port port5 = new DefaultPort(dev1, p5, true);
        List<Port> ports = Lists.newArrayList(port1, port2, port3, port4, port5);

        expect(deviceService.getPorts(anyObject(DeviceId.class))).andReturn(ports).anyTimes();
        replay(deviceService);
        assertFalse(rrp.noMoreEnabledPort(devId1, v10));
        assertFalse(rrp.noMoreEnabledPort(devId1, v20));
        assertFalse(rrp.noMoreEnabledPort(devId1, vInt));
    }

    // Disable port 1 and 3
    @Test
    public void testNoMoreEnabledPortCase3() throws Exception {
        Port port1 = new DefaultPort(dev1, p1, false);
        Port port2 = new DefaultPort(dev1, p2, true);
        Port port3 = new DefaultPort(dev1, p3, false);
        Port port4 = new DefaultPort(dev1, p4, true);
        Port port5 = new DefaultPort(dev1, p5, true);
        List<Port> ports = Lists.newArrayList(port1, port2, port3, port4, port5);

        expect(deviceService.getPorts(anyObject(DeviceId.class))).andReturn(ports).anyTimes();
        replay(deviceService);
        assertFalse(rrp.noMoreEnabledPort(devId1, v10));
        assertTrue(rrp.noMoreEnabledPort(devId1, v20));
        assertFalse(rrp.noMoreEnabledPort(devId1, vInt));
    }

    // Disable port 1 to 3
    @Test
    public void testNoMoreEnabledPortCase4() throws Exception {
        Port port1 = new DefaultPort(dev1, p1, false);
        Port port2 = new DefaultPort(dev1, p2, false);
        Port port3 = new DefaultPort(dev1, p3, false);
        Port port4 = new DefaultPort(dev1, p4, true);
        Port port5 = new DefaultPort(dev1, p5, true);
        List<Port> ports = Lists.newArrayList(port1, port2, port3, port4, port5);

        expect(deviceService.getPorts(anyObject(DeviceId.class))).andReturn(ports).anyTimes();
        replay(deviceService);
        assertTrue(rrp.noMoreEnabledPort(devId1, v10));
        assertTrue(rrp.noMoreEnabledPort(devId1, v20));
        assertFalse(rrp.noMoreEnabledPort(devId1, vInt));
    }

    // Disable port 1 to 4
    @Test
    public void testNoMoreEnabledPortCase5() throws Exception {
        Port port1 = new DefaultPort(dev1, p1, false);
        Port port2 = new DefaultPort(dev1, p2, false);
        Port port3 = new DefaultPort(dev1, p3, false);
        Port port4 = new DefaultPort(dev1, p4, false);
        Port port5 = new DefaultPort(dev1, p5, true);
        List<Port> ports = Lists.newArrayList(port1, port2, port3, port4, port5);

        expect(deviceService.getPorts(anyObject(DeviceId.class))).andReturn(ports).anyTimes();
        replay(deviceService);
        assertTrue(rrp.noMoreEnabledPort(devId1, v10));
        assertTrue(rrp.noMoreEnabledPort(devId1, v20));
        assertFalse(rrp.noMoreEnabledPort(devId1, vInt));
    }

    // Disable all ports
    @Test
    public void testNoMoreEnabledPortCase6() throws Exception {
        Port port1 = new DefaultPort(dev1, p1, false);
        Port port2 = new DefaultPort(dev1, p2, false);
        Port port3 = new DefaultPort(dev1, p3, false);
        Port port4 = new DefaultPort(dev1, p4, false);
        Port port5 = new DefaultPort(dev1, p5, false);
        List<Port> ports = Lists.newArrayList(port1, port2, port3, port4, port5);

        expect(deviceService.getPorts(anyObject(DeviceId.class))).andReturn(ports).anyTimes();
        replay(deviceService);
        assertTrue(rrp.noMoreEnabledPort(devId1, v10));
        assertTrue(rrp.noMoreEnabledPort(devId1, v20));
        assertTrue(rrp.noMoreEnabledPort(devId1, vInt));
    }
}