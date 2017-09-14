/*
 * Copyright 2014-present Open Networking Foundation
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
import org.onlab.osgi.ComponentContextAdapter;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.cfg.ComponentConfigAdapter;
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

import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
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

    private static final ProviderId PID = new ProviderId("host", "foo");
    private static final ProviderId PID2 = new ProviderId("host2", "foo2");

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

    public static final ComponentContextAdapter REMOVE_DUPS_MONITOR = new ComponentContextAdapter() {
        @Override
        public Dictionary getProperties() {
            Hashtable<String, String> props = new Hashtable<String, String>();
            props.put("allowDuplicateIps", "true");
            props.put("monitorHosts", "true");
            props.put("probeRate", "40000");
            return props;
        }
    };

    private HostManager mgr;

    protected TestListener listener = new TestListener();
    protected HostProviderRegistry registry;
    protected TestHostProvider provider;
    protected TestHostProvider provider2;
    protected HostProviderService providerService;
    protected HostProviderService providerService2;

    @Before
    public void setUp() {
        mgr = new HostManager();
        mgr.store = new SimpleHostStore();
        injectEventDispatcher(mgr, new TestEventDispatcher());
        registry = mgr;
        mgr.networkConfigService = new TestNetworkConfigService();
        mgr.cfgService = new ComponentConfigAdapter();

        mgr.activate(REMOVE_DUPS_MONITOR);

        mgr.addListener(listener);

        provider = new TestHostProvider(PID);
        provider2 = new TestHostProvider(PID2);
        providerService = registry.register(provider);
        providerService2 = registry.register(provider2);
        assertTrue("provider should be registered",
                   registry.getProviders().contains(provider.id()));
        assertTrue("provider2 should be registered",
                registry.getProviders().contains(provider2.id()));
    }

    @After
    public void tearDown() {
        registry.unregister(provider);
        registry.unregister(provider2);
        assertFalse("provider should not be registered",
                    registry.getProviders().contains(provider.id()));
        assertFalse("provider2 should not be registered",
                registry.getProviders().contains(provider2.id()));

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

    private void configured(HostId hid, MacAddress mac, VlanId vlan,
                        HostLocation loc, IpAddress ip) {
        HostDescription descr = new DefaultHostDescription(mac, vlan, loc, Collections.singleton(ip), true);
        providerService2.hostDetected(hid, descr, false);
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

    /**
     * If configured host and learnt host are both provided, we should always use
     * the configured one.
     */
    @Test
    public void hostDetectedWithMultipleProviders() {
        detect(HID1, MAC1, VLAN1, LOC1, IP1);
        assertEquals("Expect ProviderId to be PID", PID, mgr.getHost(HID1).providerId());
        assertTrue("Expect IP to be IP1", mgr.getHost(HID1).ipAddresses().contains(IP1));
        assertEquals("Expect 1 host in the store", 1, mgr.getHostCount());
        configured(HID1, MAC1, VLAN1, LOC1, IP2);
        assertEquals("Expect ProviderId get overridden by PID2", PID2, mgr.getHost(HID1).providerId());
        assertTrue("Expect IP to be IP2", mgr.getHost(HID1).ipAddresses().contains(IP2));
        assertEquals("Expect 1 hosts in the store", 1, mgr.getHostCount());
        detect(HID1, MAC1, VLAN1, LOC1, IP1);
        assertEquals("Expect ProviderId doesn't get overridden by PID", PID2, mgr.getHost(HID1).providerId());
        assertTrue("Expect IP to be IP2", mgr.getHost(HID1).ipAddresses().contains(IP2));
        assertEquals("Expect 1 host in the store", 1, mgr.getHostCount());
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

    /**
     * Providers should only be able to remove a host that is provided by itself,
     * or a host that is not configured.
     */
    @Test
    public void hostVanishedWithMultipleProviders() {
        detect(HID1, MAC1, VLAN1, LOC1, IP1);
        configured(HID2, MAC2, VLAN2, LOC2, IP2);

        providerService2.hostVanished(HID1);
        assertNull("Should be able to remove learnt host", mgr.getHost(HID1));

        providerService.hostVanished(HID2);
        assertNotNull("Should not be able to remove configured host since the provider is different",
                mgr.getHost(HID2));

        providerService2.hostVanished(HID2);
        assertNull("Should be able to remove configured host when provider is the same",
                mgr.getHost(HID2));
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

        protected TestHostProvider(ProviderId pid) {
            super(pid);
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
