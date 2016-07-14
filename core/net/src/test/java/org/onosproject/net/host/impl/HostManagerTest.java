/*
 * Copyright 2014-present Open Networking Laboratory
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
package org.onosproject.net.host.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestTools;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.common.event.impl.TestEventDispatcher;
import org.onosproject.event.Event;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.PortNumber;
import org.onosproject.net.config.NetworkConfigServiceAdapter;
import org.onosproject.net.host.DefaultHostDescription;
import org.onosproject.net.host.HostDescription;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostProvider;
import org.onosproject.net.host.HostProviderRegistry;
import org.onosproject.net.host.HostProviderService;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.store.trivial.SimpleHostStore;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.onosproject.net.NetTestTools.injectEventDispatcher;
import static org.onosproject.net.host.HostEvent.Type.HOST_ADDED;
import static org.onosproject.net.host.HostEvent.Type.HOST_MOVED;
import static org.onosproject.net.host.HostEvent.Type.HOST_REMOVED;
import static org.onosproject.net.host.HostEvent.Type.HOST_UPDATED;

/**
 * Test codifying the host service & host provider service contracts.
 */
public class HostManagerTest {

    private static final ProviderId PID = new ProviderId("of", "foo");

    private static final VlanId VLAN1 = VlanId.vlanId((short) 1);
    private static final VlanId VLAN2 = VlanId.vlanId((short) 2);
    private static final MacAddress MAC1 = MacAddress.valueOf("00:00:11:00:00:01");
    private static final MacAddress MAC2 = MacAddress.valueOf("00:00:22:00:00:02");
    private static final MacAddress MAC3 = MacAddress.valueOf("00:00:33:00:00:03");
    private static final MacAddress MAC4 = MacAddress.valueOf("00:00:44:00:00:04");
    private static final HostId HID1 = HostId.hostId(MAC1, VLAN1);
    private static final HostId HID2 = HostId.hostId(MAC2, VLAN1);
    private static final HostId HID3 = HostId.hostId(MAC3, VLAN1);
    private static final HostId HID4 = HostId.hostId(MAC4, VLAN1);

    private static final IpAddress IP1 = IpAddress.valueOf("10.0.0.1");
    private static final IpAddress IP2 = IpAddress.valueOf("10.0.0.2");
    private static final IpAddress IP3 = IpAddress.valueOf("2001::1");
    private static final IpAddress IP4 = IpAddress.valueOf("2001::2");

    private static final DeviceId DID1 = DeviceId.deviceId("of:001");
    private static final DeviceId DID2 = DeviceId.deviceId("of:002");
    private static final PortNumber P1 = PortNumber.portNumber(100);
    private static final PortNumber P2 = PortNumber.portNumber(200);
    private static final HostLocation LOC1 = new HostLocation(DID1, P1, 123L);
    private static final HostLocation LOC2 = new HostLocation(DID1, P2, 123L);

    private HostManager mgr;

    protected TestListener listener = new TestListener();
    protected HostProviderRegistry registry;
    protected TestHostProvider provider;
    protected HostProviderService providerService;

    @Before
    public void setUp() {
        mgr = new HostManager();
        mgr.store = new SimpleHostStore();
        injectEventDispatcher(mgr, new TestEventDispatcher());
        registry = mgr;
        mgr.networkConfigService = new TestNetworkConfigService();
        mgr.activate();

        mgr.addListener(listener);

        provider = new TestHostProvider();
        providerService = registry.register(provider);
        assertTrue("provider should be registered",
                   registry.getProviders().contains(provider.id()));
    }

    @After
    public void tearDown() {
        registry.unregister(provider);
        assertFalse("provider should not be registered",
                    registry.getProviders().contains(provider.id()));

        mgr.removeListener(listener);
        mgr.deactivate();
        injectEventDispatcher(mgr, null);
    }

    private void detect(HostId hid, MacAddress mac, VlanId vlan,
                        HostLocation loc, IpAddress ip) {
        HostDescription descr = new DefaultHostDescription(mac, vlan, loc, ip);
        providerService.hostDetected(hid, descr, false);
        assertNotNull("host should be found", mgr.getHost(hid));
    }

    private void validateEvents(Enum... types) {
        TestTools.assertAfter(100, () -> {
            int i = 0;
            assertEquals("wrong events received", types.length, listener.events.size());
            for (Event event : listener.events) {
                assertEquals("incorrect event type", types[i], event.type());
                i++;
            }
            listener.events.clear();
        });
    }

    @Test
    public void hostDetected() {
        assertNull("host shouldn't be found", mgr.getHost(HID1));

        // host addition
        detect(HID1, MAC1, VLAN1, LOC1, IP1);
        assertEquals("exactly one should be found", 1, mgr.getHostCount());
        detect(HID2, MAC2, VLAN2, LOC2, IP1);
        assertEquals("two hosts should be found", 2, mgr.getHostCount());
        validateEvents(HOST_ADDED, HOST_ADDED);

        // host motion
        detect(HID1, MAC1, VLAN1, LOC2, IP1);
        validateEvents(HOST_MOVED);
        assertEquals("only two hosts should be found", 2, mgr.getHostCount());

        // host update
        detect(HID1, MAC1, VLAN1, LOC2, IP2);
        validateEvents(HOST_UPDATED);
        assertEquals("only two hosts should be found", 2, mgr.getHostCount());
    }

    @Test
    public void hostDetectedIPv6() {
        assertNull("host shouldn't be found", mgr.getHost(HID3));

        // host addition
        detect(HID3, MAC3, VLAN1, LOC1, IP3);
        assertEquals("exactly one should be found", 1, mgr.getHostCount());
        detect(HID4, MAC4, VLAN2, LOC2, IP3);
        assertEquals("two hosts should be found", 2, mgr.getHostCount());
        validateEvents(HOST_ADDED, HOST_ADDED);

        // host motion
        detect(HID3, MAC3, VLAN1, LOC2, IP3);
        validateEvents(HOST_MOVED);
        assertEquals("only two hosts should be found", 2, mgr.getHostCount());

        // host update
        detect(HID3, MAC3, VLAN1, LOC2, IP4);
        validateEvents(HOST_UPDATED);
        assertEquals("only two hosts should be found", 2, mgr.getHostCount());
    }

    @Test
    public void hostVanished() {
        detect(HID1, MAC1, VLAN1, LOC1, IP1);
        providerService.hostVanished(HID1);
        validateEvents(HOST_ADDED, HOST_REMOVED);

        assertNull("host should have been removed", mgr.getHost(HID1));
    }

    @Test
    public void hostVanishedIPv6() {
        detect(HID3, MAC3, VLAN1, LOC1, IP3);
        providerService.hostVanished(HID3);
        validateEvents(HOST_ADDED, HOST_REMOVED);

        assertNull("host should have been removed", mgr.getHost(HID3));
    }

    private void validateHosts(
            String msg, Iterable<Host> hosts, HostId... ids) {
        Set<HostId> hids = Sets.newHashSet(ids);
        for (Host h : hosts) {
            assertTrue(msg, hids.remove(h.id()));
        }
        assertTrue("expected hosts not fetched from store", hids.isEmpty());
    }

    @Test
    public void getHosts() {
        detect(HID1, MAC1, VLAN1, LOC1, IP1);
        detect(HID2, MAC2, VLAN1, LOC2, IP2);

        validateHosts("host not properly stored", mgr.getHosts(), HID1, HID2);
        validateHosts("can't get hosts by VLAN", mgr.getHostsByVlan(VLAN1), HID1, HID2);
        validateHosts("can't get hosts by MAC", mgr.getHostsByMac(MAC1), HID1);
        validateHosts("can't get hosts by IP", mgr.getHostsByIp(IP1), HID1);
        validateHosts("can't get hosts by location", mgr.getConnectedHosts(LOC1), HID1);
        assertTrue("incorrect host location", mgr.getConnectedHosts(DID2).isEmpty());
    }

    @Test
    public void getHostsIPv6() {
        detect(HID3, MAC3, VLAN1, LOC1, IP3);
        detect(HID4, MAC4, VLAN1, LOC2, IP4);

        validateHosts("host not properly stored", mgr.getHosts(), HID3, HID4);
        validateHosts("can't get hosts by VLAN", mgr.getHostsByVlan(VLAN1), HID3, HID4);
        validateHosts("can't get hosts by MAC", mgr.getHostsByMac(MAC3), HID3);
        validateHosts("can't get hosts by IP", mgr.getHostsByIp(IP3), HID3);
        validateHosts("can't get hosts by location", mgr.getConnectedHosts(LOC1), HID3);
        assertTrue("incorrect host location", mgr.getConnectedHosts(DID2).isEmpty());
    }

    private static class TestHostProvider extends AbstractProvider
            implements HostProvider {

        protected TestHostProvider() {
            super(PID);
        }

        @Override
        public ProviderId id() {
            return PID;
        }

        @Override
        public void triggerProbe(Host host) {
        }

    }

    private static class TestListener implements HostListener {

        protected List<HostEvent> events = Lists.newArrayList();

        @Override
        public void event(HostEvent event) {
            events.add(event);
        }

    }

    private class TestNetworkConfigService extends NetworkConfigServiceAdapter {
    }
}
