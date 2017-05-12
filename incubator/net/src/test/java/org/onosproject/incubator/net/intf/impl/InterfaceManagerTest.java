/*
 * Copyright 2015-present Open Networking Laboratory
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

package org.onosproject.incubator.net.intf.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.incubator.net.config.basics.ConfigException;
import org.onosproject.incubator.net.config.basics.InterfaceConfig;
import org.onosproject.incubator.net.intf.Interface;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.config.Config;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigServiceAdapter;
import org.onosproject.net.host.InterfaceIpAddress;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Unit tests for InterfaceManager.
 */
public class InterfaceManagerTest {
    private static final Class<InterfaceConfig> CONFIG_CLASS = InterfaceConfig.class;

    private static final int NUM_INTERFACES = 4;

    private Set<ConnectPoint> subjects = Sets.newHashSet();
    private Map<ConnectPoint, InterfaceConfig> configs = Maps.newHashMap();

    private Set<Interface> interfaces = Sets.newHashSet();

    private NetworkConfigListener listener;

    private InterfaceManager interfaceManager;

    @Before
    public void setUp() throws Exception {
        for (int i = 0; i < NUM_INTERFACES; i++) {
            ConnectPoint cp = createConnectPoint(i);
            subjects.add(cp);

            Interface intf = createInterface(i);

            interfaces.add(intf);

            InterfaceConfig ic = new TestInterfaceConfig(cp, Sets.newHashSet(intf));

            configs.put(cp, ic);
        }

        TestNetworkConfigService configService =
                new TestNetworkConfigService(subjects, configs);

        interfaceManager = new InterfaceManager();
        interfaceManager.configService = configService;
        interfaceManager.activate();
    }

    private Interface createInterface(int i) {
        ConnectPoint cp = createConnectPoint(i);

        InterfaceIpAddress ia = InterfaceIpAddress.valueOf("192.168." + i + ".1/24");

        Interface intf = new Interface(Interface.NO_INTERFACE_NAME, cp,
                Collections.singletonList(ia),
                MacAddress.valueOf(i),
                VlanId.vlanId((short) i));

        return intf;
    }

    private ConnectPoint createConnectPoint(int i) {
        return  ConnectPoint.deviceConnectPoint("of:000000000000000" + i + "/1");
    }

    @Test
    public void testGetInterfaces() throws Exception {
        assertEquals(interfaces, interfaceManager.getInterfaces());
    }

    @Test
    public void testGetInterfacesByPort() throws Exception {
        ConnectPoint cp = ConnectPoint.deviceConnectPoint("of:0000000000000001/1");

        Set<Interface> byPort = Collections.singleton(createInterface(1));

        assertEquals(byPort, interfaceManager.getInterfacesByPort(cp));
    }

    @Test
    public void testGetInterfacesByIp() throws Exception {
        IpAddress ip = Ip4Address.valueOf("192.168.2.1");

        Set<Interface> byIp = Collections.singleton(createInterface(2));

        assertEquals(byIp, interfaceManager.getInterfacesByIp(ip));
    }

    @Test
    public void testGetMatchingInterface() throws Exception {
        IpAddress ip = Ip4Address.valueOf("192.168.1.100");

        Interface matchingIntf = createInterface(1);

        assertEquals(matchingIntf, interfaceManager.getMatchingInterface(ip));

        // Searching for an IP with no match should return null
        ip = Ip4Address.valueOf("1.1.1.1");

        assertNull(interfaceManager.getMatchingInterface(ip));
    }

    @Test
    public void testGetInterfacesByVlan() throws Exception {
        VlanId vlanId = VlanId.vlanId((short) 1);

        Set<Interface> byVlan = Collections.singleton(createInterface(1));

        assertEquals(byVlan, interfaceManager.getInterfacesByVlan(vlanId));
    }

    @Test
    public void testAddInterface() throws Exception {
        // Create a new InterfaceConfig which will get added
        VlanId vlanId = VlanId.vlanId((short) 1);
        ConnectPoint cp = ConnectPoint.deviceConnectPoint("of:0000000000000001/2");
        Interface newIntf = new Interface(Interface.NO_INTERFACE_NAME, cp,
                Collections.emptyList(),
                MacAddress.valueOf(100),
                vlanId);

        InterfaceConfig ic = new TestInterfaceConfig(cp, Collections.singleton(newIntf));

        subjects.add(cp);
        configs.put(cp, ic);
        interfaces.add(newIntf);

        NetworkConfigEvent event = new NetworkConfigEvent(
                NetworkConfigEvent.Type.CONFIG_ADDED, cp, CONFIG_CLASS);

        assertEquals(NUM_INTERFACES, interfaceManager.getInterfaces().size());

        // Send in a config event containing a new interface config
        listener.event(event);

        // Check the new interface exists in the InterfaceManager's inventory
        assertEquals(interfaces, interfaceManager.getInterfaces());
        assertEquals(NUM_INTERFACES + 1, interfaceManager.getInterfaces().size());

        // There are now two interfaces with vlan ID 1
        Set<Interface> byVlan = Sets.newHashSet(createInterface(1), newIntf);
        assertEquals(byVlan, interfaceManager.getInterfacesByVlan(vlanId));
    }

    @Test
    public void testUpdateInterface() throws Exception {
        ConnectPoint cp = createConnectPoint(1);

        // Create an interface that is the same as the existing one, but adds a
        // new IP address
        Interface intf = createInterface(1);
        List<InterfaceIpAddress> addresses = Lists.newArrayList(intf.ipAddressesList());
        addresses.add(InterfaceIpAddress.valueOf("192.168.100.1/24"));
        intf = new Interface(Interface.NO_INTERFACE_NAME, intf.connectPoint(), addresses, intf.mac(), intf.vlan());

        // Create a new interface on the same connect point as the existing one
        InterfaceIpAddress newAddr = InterfaceIpAddress.valueOf("192.168.101.1/24");
        Interface newIntf = new Interface(Interface.NO_INTERFACE_NAME, cp,
                Collections.singletonList(newAddr),
                MacAddress.valueOf(101),
                VlanId.vlanId((short) 101));

        Set<Interface> interfaces = Sets.newHashSet(intf, newIntf);

        // New interface config updates the existing interface and adds a new
        // interface to the same connect point
        InterfaceConfig ic = new TestInterfaceConfig(cp, interfaces);

        configs.put(cp, ic);

        NetworkConfigEvent event = new NetworkConfigEvent(
                NetworkConfigEvent.Type.CONFIG_UPDATED, cp, CONFIG_CLASS);

        // Send in the event signalling the interfaces for this connect point
        // have been updated
        listener.event(event);

        assertEquals(NUM_INTERFACES + 1, interfaceManager.getInterfaces().size());
        assertEquals(interfaces, interfaceManager.getInterfacesByPort(cp));
    }

    @Test
    public void testRemoveInterface() throws Exception {
        ConnectPoint cp = createConnectPoint(1);

        NetworkConfigEvent event = new NetworkConfigEvent(
                NetworkConfigEvent.Type.CONFIG_REMOVED, cp, CONFIG_CLASS);

        assertEquals(NUM_INTERFACES, interfaceManager.getInterfaces().size());

        // Send in a config event removing an interface config
        listener.event(event);

        assertEquals(NUM_INTERFACES - 1, interfaceManager.getInterfaces().size());
    }

    /**
     * Test version of NetworkConfigService which allows us to pass in subjects
     * and InterfaceConfigs directly.
     */
    private class TestNetworkConfigService extends NetworkConfigServiceAdapter {
        private final Set<ConnectPoint> subjects;
        private final Map<ConnectPoint, InterfaceConfig> configs;

        public TestNetworkConfigService(Set<ConnectPoint> subjects,
                                        Map<ConnectPoint, InterfaceConfig> configs) {
            this.subjects = subjects;
            this.configs = configs;
        }

        @Override
        public <S, C extends Config<S>> Set<S> getSubjects(Class<S> subjectClass,
                                                           Class<C> configClass) {
            return (Set<S>) subjects;
        }

        @Override
        public <S, C extends Config<S>> C getConfig(S subject, Class<C> configClass) {
            return (C) configs.get(subject);
        }

        @Override
        public void addListener(NetworkConfigListener listener) {
            InterfaceManagerTest.this.listener = listener;
        }
    }

    /**
     * Test version of InterfaceConfig where we can inject interfaces directly,
     * rather than parsing them from JSON.
     */
    private class TestInterfaceConfig extends InterfaceConfig {
        private final ConnectPoint subject;
        private final Set<Interface> interfaces;

        @Override
        public ConnectPoint subject() {
            return subject;
        }

        public TestInterfaceConfig(ConnectPoint subject, Set<Interface> interfaces) {
            this.subject = subject;
            this.interfaces = interfaces;
        }

        @Override
        public Set<Interface> getInterfaces() throws ConfigException {
            return interfaces;
        }
    }

}
