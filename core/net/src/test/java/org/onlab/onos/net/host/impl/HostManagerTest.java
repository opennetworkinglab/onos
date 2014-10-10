package org.onlab.onos.net.host.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.onlab.onos.net.host.HostEvent.Type.HOST_ADDED;
import static org.onlab.onos.net.host.HostEvent.Type.HOST_MOVED;
import static org.onlab.onos.net.host.HostEvent.Type.HOST_REMOVED;
import static org.onlab.onos.net.host.HostEvent.Type.HOST_UPDATED;

import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.onos.event.Event;
import org.onlab.onos.event.impl.TestEventDispatcher;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.Host;
import org.onlab.onos.net.HostId;
import org.onlab.onos.net.HostLocation;
import org.onlab.onos.net.PortNumber;
import org.onlab.onos.net.host.DefaultHostDescription;
import org.onlab.onos.net.host.HostDescription;
import org.onlab.onos.net.host.HostEvent;
import org.onlab.onos.net.host.HostListener;
import org.onlab.onos.net.host.HostProvider;
import org.onlab.onos.net.host.HostProviderRegistry;
import org.onlab.onos.net.host.HostProviderService;
import org.onlab.onos.net.host.PortAddresses;
import org.onlab.onos.net.provider.AbstractProvider;
import org.onlab.onos.net.provider.ProviderId;
import org.onlab.onos.store.trivial.impl.SimpleHostStore;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Test codifying the host service & host provider service contracts.
 */
public class HostManagerTest {

    private static final ProviderId PID = new ProviderId("of", "foo");

    private static final VlanId VLAN1 = VlanId.vlanId((short) 1);
    private static final VlanId VLAN2 = VlanId.vlanId((short) 2);
    private static final MacAddress MAC1 = MacAddress.valueOf("00:00:11:00:00:01");
    private static final MacAddress MAC2 = MacAddress.valueOf("00:00:22:00:00:02");
    private static final HostId HID1 = HostId.hostId(MAC1, VLAN1);
    private static final HostId HID2 = HostId.hostId(MAC2, VLAN1);

    private static final IpPrefix IP1 = IpPrefix.valueOf("10.0.0.1");
    private static final IpPrefix IP2 = IpPrefix.valueOf("10.0.0.2");

    private static final DeviceId DID1 = DeviceId.deviceId("of:001");
    private static final DeviceId DID2 = DeviceId.deviceId("of:002");
    private static final PortNumber P1 = PortNumber.portNumber(100);
    private static final PortNumber P2 = PortNumber.portNumber(200);
    private static final HostLocation LOC1 = new HostLocation(DID1, P1, 123L);
    private static final HostLocation LOC2 = new HostLocation(DID1, P2, 123L);
    private static final ConnectPoint CP1 = new ConnectPoint(DID1, P1);
    private static final ConnectPoint CP2 = new ConnectPoint(DID2, P2);

    private static final IpPrefix PREFIX1 = IpPrefix.valueOf("10.0.1.0/24");
    private static final IpPrefix PREFIX2 = IpPrefix.valueOf("10.1.0.0/16");
    private static final IpPrefix PREFIX3 = IpPrefix.valueOf("5.8.2.0/23");

    private HostManager mgr;

    protected TestListener listener = new TestListener();
    protected HostProviderRegistry registry;
    protected TestHostProvider provider;
    protected HostProviderService providerService;

    @Before
    public void setUp() {
        mgr = new HostManager();
        mgr.store = new SimpleHostStore();
        mgr.eventDispatcher = new TestEventDispatcher();
        registry = mgr;
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
        mgr.eventDispatcher = null;
    }

    private void detect(HostId hid, MacAddress mac, VlanId vlan,
                        HostLocation loc, IpPrefix ip) {
        HostDescription descr = new DefaultHostDescription(mac, vlan, loc, ip);
        providerService.hostDetected(hid, descr);
        assertNotNull("host should be found", mgr.getHost(hid));
    }

    private void validateEvents(Enum... types) {
        int i = 0;
        assertEquals("wrong events received", types.length, listener.events.size());
        for (Event event : listener.events) {
            assertEquals("incorrect event type", types[i], event.type());
            i++;
        }
        listener.events.clear();
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
    public void hostVanished() {
        detect(HID1, MAC1, VLAN1, LOC1, IP1);
        providerService.hostVanished(HID1);
        validateEvents(HOST_ADDED, HOST_REMOVED);

        assertNull("host should have been removed", mgr.getHost(HID1));
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

    @Test
    public void bindAddressesToPort() {
        PortAddresses add1 = new PortAddresses(CP1,
                                               Sets.newHashSet(PREFIX1, PREFIX2), MAC1);

        mgr.bindAddressesToPort(add1);
        PortAddresses storedAddresses = mgr.getAddressBindingsForPort(CP1);

        assertTrue(add1.ips().equals(storedAddresses.ips()));
        assertTrue(add1.mac().equals(storedAddresses.mac()));

        // Add some more addresses and check that they're added correctly
        PortAddresses add2 = new PortAddresses(CP1, Sets.newHashSet(PREFIX3), null);

        mgr.bindAddressesToPort(add2);
        storedAddresses = mgr.getAddressBindingsForPort(CP1);

        assertTrue(storedAddresses.ips().equals(
                Sets.newHashSet(PREFIX1, PREFIX2, PREFIX3)));
        assertTrue(storedAddresses.mac().equals(MAC1));

        PortAddresses add3 = new PortAddresses(CP1, null, MAC2);

        mgr.bindAddressesToPort(add3);
        storedAddresses = mgr.getAddressBindingsForPort(CP1);

        assertTrue(storedAddresses.ips().equals(
                Sets.newHashSet(PREFIX1, PREFIX2, PREFIX3)));
        assertTrue(storedAddresses.mac().equals(MAC2));
    }

    @Test
    public void unbindAddressesFromPort() {
        PortAddresses add1 = new PortAddresses(CP1,
                                               Sets.newHashSet(PREFIX1, PREFIX2), MAC1);

        mgr.bindAddressesToPort(add1);
        PortAddresses storedAddresses = mgr.getAddressBindingsForPort(CP1);

        assertTrue(storedAddresses.ips().size() == 2);
        assertNotNull(storedAddresses.mac());

        PortAddresses rem1 = new PortAddresses(CP1,
                                               Sets.newHashSet(PREFIX1), null);

        mgr.unbindAddressesFromPort(rem1);
        storedAddresses = mgr.getAddressBindingsForPort(CP1);

        assertTrue(storedAddresses.ips().equals(Sets.newHashSet(PREFIX2)));
        assertTrue(storedAddresses.mac().equals(MAC1));

        PortAddresses rem2 = new PortAddresses(CP1, null, MAC1);

        mgr.unbindAddressesFromPort(rem2);
        storedAddresses = mgr.getAddressBindingsForPort(CP1);

        assertTrue(storedAddresses.ips().equals(Sets.newHashSet(PREFIX2)));
        assertNull(storedAddresses.mac());

        PortAddresses rem3 = new PortAddresses(CP1,
                                               Sets.newHashSet(PREFIX2), MAC1);

        mgr.unbindAddressesFromPort(rem3);
        storedAddresses = mgr.getAddressBindingsForPort(CP1);

        assertTrue(storedAddresses.ips().isEmpty());
        assertNull(storedAddresses.mac());
    }

    @Test
    public void clearAddresses() {
        PortAddresses add1 = new PortAddresses(CP1,
                                               Sets.newHashSet(PREFIX1, PREFIX2), MAC1);

        mgr.bindAddressesToPort(add1);
        PortAddresses storedAddresses = mgr.getAddressBindingsForPort(CP1);

        assertTrue(storedAddresses.ips().size() == 2);
        assertNotNull(storedAddresses.mac());

        mgr.clearAddresses(CP1);
        storedAddresses = mgr.getAddressBindingsForPort(CP1);

        assertTrue(storedAddresses.ips().isEmpty());
        assertNull(storedAddresses.mac());
    }

    @Test
    public void getAddressBindingsForPort() {
        PortAddresses add1 = new PortAddresses(CP1,
                                               Sets.newHashSet(PREFIX1, PREFIX2), MAC1);

        mgr.bindAddressesToPort(add1);
        PortAddresses storedAddresses = mgr.getAddressBindingsForPort(CP1);

        assertTrue(storedAddresses.connectPoint().equals(CP1));
        assertTrue(storedAddresses.ips().equals(Sets.newHashSet(PREFIX1, PREFIX2)));
        assertTrue(storedAddresses.mac().equals(MAC1));
    }

    @Test
    public void getAddressBindings() {
        Set<PortAddresses> storedAddresses = mgr.getAddressBindings();

        assertTrue(storedAddresses.isEmpty());

        PortAddresses add1 = new PortAddresses(CP1,
                                               Sets.newHashSet(PREFIX1, PREFIX2), MAC1);

        mgr.bindAddressesToPort(add1);

        storedAddresses = mgr.getAddressBindings();

        assertTrue(storedAddresses.size() == 1);

        PortAddresses add2 = new PortAddresses(CP2,
                                               Sets.newHashSet(PREFIX3), MAC2);

        mgr.bindAddressesToPort(add2);

        storedAddresses = mgr.getAddressBindings();

        assertTrue(storedAddresses.size() == 2);
        assertTrue(storedAddresses.equals(Sets.newHashSet(add1, add2)));
    }
}
