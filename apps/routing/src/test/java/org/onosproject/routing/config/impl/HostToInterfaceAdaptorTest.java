/*
 * Copyright 2014-2015 Open Networking Laboratory
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
package org.onosproject.routing.config.impl;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.host.HostService;
import org.onosproject.net.host.InterfaceIpAddress;
import org.onosproject.net.host.PortAddresses;
import org.onosproject.routing.config.Interface;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for the HostToInterfaceAdaptor class.
 */
public class HostToInterfaceAdaptorTest {

    private HostService hostService;
    private HostToInterfaceAdaptor adaptor;

    private Set<PortAddresses> portAddresses;
    private Map<ConnectPoint, Interface> interfaces;

    private static final ConnectPoint CP1 = new ConnectPoint(
            DeviceId.deviceId("of:1"), PortNumber.portNumber(1));
    private static final ConnectPoint CP2 = new ConnectPoint(
            DeviceId.deviceId("of:1"), PortNumber.portNumber(2));
    private static final ConnectPoint CP3 = new ConnectPoint(
            DeviceId.deviceId("of:2"), PortNumber.portNumber(1));

    private static final ConnectPoint NON_EXISTENT_CP = new ConnectPoint(
            DeviceId.deviceId("doesnotexist"), PortNumber.portNumber(1));

    @Before
    public void setUp() throws Exception {
        hostService = createMock(HostService.class);

        portAddresses = Sets.newHashSet();
        interfaces = Maps.newHashMap();

        InterfaceIpAddress ia11 =
            new InterfaceIpAddress(IpAddress.valueOf("192.168.1.1"),
                                   IpPrefix.valueOf("192.168.1.0/24"));
        createPortAddressesAndInterface(CP1,
                Sets.newHashSet(ia11),
                MacAddress.valueOf("00:00:00:00:00:01"),
                VlanId.NONE);

        // Two addresses in the same subnet
        InterfaceIpAddress ia21 =
            new InterfaceIpAddress(IpAddress.valueOf("192.168.2.1"),
                                   IpPrefix.valueOf("192.168.2.0/24"));
        InterfaceIpAddress ia22 =
            new InterfaceIpAddress(IpAddress.valueOf("192.168.2.2"),
                                   IpPrefix.valueOf("192.168.2.0/24"));
        createPortAddressesAndInterface(CP2,
                Sets.newHashSet(ia21, ia22),
                MacAddress.valueOf("00:00:00:00:00:02"),
                VlanId.vlanId((short) 4));

        // Two addresses in different subnets
        InterfaceIpAddress ia31 =
            new InterfaceIpAddress(IpAddress.valueOf("192.168.3.1"),
                                   IpPrefix.valueOf("192.168.3.0/24"));
        InterfaceIpAddress ia41 =
            new InterfaceIpAddress(IpAddress.valueOf("192.168.4.1"),
                                   IpPrefix.valueOf("192.168.4.0/24"));
        createPortAddressesAndInterface(CP3,
                Sets.newHashSet(ia31, ia41),
                MacAddress.valueOf("00:00:00:00:00:03"),
                VlanId.NONE);

        expect(hostService.getAddressBindings()).andReturn(portAddresses).anyTimes();

        replay(hostService);

        adaptor = new HostToInterfaceAdaptor(hostService);
    }

    /**
     * Creates both a PortAddresses and an Interface for the given inputs and
     * places them in the correct global data stores.
     *
     * @param cp the connect point
     * @param ipAddresses the set of interface IP addresses
     * @param mac the MAC address
     * @param vlan the VLAN ID
     */
    private void createPortAddressesAndInterface(
            ConnectPoint cp, Set<InterfaceIpAddress> ipAddresses,
            MacAddress mac, VlanId vlan) {
        PortAddresses pa = new PortAddresses(cp, ipAddresses, mac, vlan);
        portAddresses.add(pa);
        expect(hostService.getAddressBindingsForPort(cp)).andReturn(
                Collections.singleton(pa)).anyTimes();

        Interface intf = new Interface(cp, ipAddresses, mac, vlan);
        interfaces.put(cp, intf);
    }

    /**
     * Tests {@link HostToInterfaceAdaptor#getInterfaces()}.
     * Verifies that the set of interfaces returned matches what is expected
     * based on the input PortAddresses data.
     */
    @Test
    public void testGetInterfaces() {
        Set<Interface> adaptorIntfs = adaptor.getInterfaces();

        assertEquals(3, adaptorIntfs.size());
        assertTrue(adaptorIntfs.contains(this.interfaces.get(CP1)));
        assertTrue(adaptorIntfs.contains(this.interfaces.get(CP2)));
        assertTrue(adaptorIntfs.contains(this.interfaces.get(CP3)));
    }

    /**
     * Tests {@link HostToInterfaceAdaptor#getInterface(ConnectPoint)}.
     * Verifies that the correct interface is returned for a given connect
     * point.
     */
    @Test
    public void testGetInterface() {
        assertEquals(this.interfaces.get(CP1), adaptor.getInterface(CP1));
        assertEquals(this.interfaces.get(CP2), adaptor.getInterface(CP2));
        assertEquals(this.interfaces.get(CP3), adaptor.getInterface(CP3));

        // Try and get an interface for a connect point with no addresses
        reset(hostService);
        expect(hostService.getAddressBindingsForPort(NON_EXISTENT_CP))
                .andReturn(Collections.<PortAddresses>emptySet()).anyTimes();
        replay(hostService);

        assertNull(adaptor.getInterface(NON_EXISTENT_CP));
    }

    /**
     * Tests {@link HostToInterfaceAdaptor#getInterface(ConnectPoint)} in the
     * case that the input connect point is null.
     * Verifies that a NullPointerException is thrown.
     */
    @Test(expected = NullPointerException.class)
    public void testGetInterfaceNull() {
        adaptor.getInterface(null);
    }

    /**
     * Tests {@link HostToInterfaceAdaptor#getMatchingInterface(IpAddress)}.
     * Verifies that the correct interface is returned based on the given IP
     * address.
     */
    @Test
    public void testGetMatchingInterface() {
        assertEquals(this.interfaces.get(CP1),
                adaptor.getMatchingInterface(IpAddress.valueOf("192.168.1.100")));
        assertEquals(this.interfaces.get(CP2),
                adaptor.getMatchingInterface(IpAddress.valueOf("192.168.2.100")));
        assertEquals(this.interfaces.get(CP3),
                adaptor.getMatchingInterface(IpAddress.valueOf("192.168.3.100")));
        assertEquals(this.interfaces.get(CP3),
                adaptor.getMatchingInterface(IpAddress.valueOf("192.168.4.100")));

        // Try and match an address we don't have subnet configured for
        assertNull(adaptor.getMatchingInterface(IpAddress.valueOf("1.1.1.1")));
    }

    /**
     * Tests {@link HostToInterfaceAdaptor#getMatchingInterface(IpAddress)} in the
     * case that the input IP address is null.
     * Verifies that a NullPointerException is thrown.
     */
    @Test(expected = NullPointerException.class)
    public void testGetMatchingInterfaceNull() {
        adaptor.getMatchingInterface(null);
    }

}
