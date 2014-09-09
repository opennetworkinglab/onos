package org.onlab.onos.net.trivial.impl;

import com.google.common.collect.ImmutableSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.onos.event.Event;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.Device;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.Link;
import org.onlab.onos.net.MastershipRole;
import org.onlab.onos.net.PortNumber;
import org.onlab.onos.net.link.DefaultLinkDescription;
import org.onlab.onos.net.link.LinkAdminService;
import org.onlab.onos.net.link.LinkEvent;
import org.onlab.onos.net.link.LinkListener;
import org.onlab.onos.net.link.LinkProvider;
import org.onlab.onos.net.link.LinkProviderRegistry;
import org.onlab.onos.net.link.LinkProviderService;
import org.onlab.onos.net.link.LinkService;
import org.onlab.onos.net.provider.AbstractProvider;
import org.onlab.onos.net.provider.ProviderId;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;
import static org.onlab.onos.net.DeviceId.deviceId;
import static org.onlab.onos.net.Link.Type.DIRECT;
import static org.onlab.onos.net.Link.Type.INDIRECT;
import static org.onlab.onos.net.link.LinkEvent.Type.*;

/**
 * Test codifying the link service & link provider service contracts.
 */
public class SimpleLinkManagerTest {

    private static final ProviderId PID = new ProviderId("foo");
    private static final DeviceId DID1 = deviceId("of:foo");
    private static final DeviceId DID2 = deviceId("of:bar");
    private static final DeviceId DID3 = deviceId("of:goo");

    private static final PortNumber P1 = PortNumber.portNumber(1);
    private static final PortNumber P2 = PortNumber.portNumber(2);
    private static final PortNumber P3 = PortNumber.portNumber(3);


    private SimpleLinkManager mgr;

    protected LinkService service;
    protected LinkAdminService admin;
    protected LinkProviderRegistry registry;
    protected LinkProviderService providerService;
    protected TestProvider provider;
    protected TestListener listener = new TestListener();

    @Before
    public void setUp() {
        mgr = new SimpleLinkManager();
        service = mgr;
        admin = mgr;
        registry = mgr;
        mgr.eventDispatcher = new TestEventDispatcher();
        mgr.activate();

        service.addListener(listener);

        provider = new TestProvider();
        providerService = registry.register(provider);
        assertTrue("provider should be registered",
                   registry.getProviders().contains(provider.id()));
    }

    @After
    public void tearDown() {
        registry.unregister(provider);
        assertFalse("provider should not be registered",
                    registry.getProviders().contains(provider.id()));
        service.removeListener(listener);
        mgr.deactivate();
    }

    @Test
    public void createLink() {
        addLink(DID1, P1, DID2, P2, DIRECT);
        addLink(DID2, P2, DID1, P1, DIRECT);
        assertEquals("incorrect link count", 2, service.getLinkCount());

        Iterator<Link> it = service.getLinks().iterator();
        it.next();
        it.next();
        assertFalse("incorrect link count", it.hasNext());
    }

    @Test
    public void updateLink() {
        addLink(DID1, P1, DID2, P2, DIRECT);
        addLink(DID2, P2, DID1, P1, INDIRECT);
        assertEquals("incorrect link count", 2, service.getLinkCount());

        providerService.linkDetected(new DefaultLinkDescription(cp(DID2, P2), cp(DID1, P1), DIRECT));
        validateEvents(LINK_UPDATED);
        assertEquals("incorrect link count", 2, service.getLinkCount());

        providerService.linkDetected(new DefaultLinkDescription(cp(DID2, P2), cp(DID1, P1), INDIRECT));
        providerService.linkDetected(new DefaultLinkDescription(cp(DID2, P2), cp(DID1, P1), DIRECT));
        assertEquals("no events expected", 0, listener.events.size());
    }

    @Test
    public void removeLink() {
        addLink(DID1, P1, DID2, P2, DIRECT);
        addLink(DID2, P2, DID1, P1, DIRECT);
        assertEquals("incorrect link count", 2, service.getLinkCount());

        providerService.linkVanished(new DefaultLinkDescription(cp(DID1, P1), cp(DID2, P2), DIRECT));
        validateEvents(LINK_REMOVED);
        assertEquals("incorrect link count", 1, service.getLinkCount());
        assertNull("link should not be found", service.getLink(cp(DID1, P1), cp(DID2, P2)));
        assertNotNull("link should be found", service.getLink(cp(DID2, P2), cp(DID1, P1)));

        providerService.linkVanished(new DefaultLinkDescription(cp(DID1, P1), cp(DID2, P2), DIRECT));
        assertEquals("no events expected", 0, listener.events.size());
    }

    @Test
    public void removeLinksByConnectionPoint() {
        Link l1 = addLink(DID1, P1, DID2, P2, DIRECT);
        Link l2 = addLink(DID2, P2, DID1, P1, DIRECT);
        addLink(DID3, P3, DID2, P1, DIRECT);
        addLink(DID2, P1, DID3, P3, DIRECT);
        assertEquals("incorrect link count", 4, service.getLinkCount());

        providerService.linksVanished(cp(DID1, P1));
        assertEquals("incorrect link count", 2, service.getLinkCount());
        assertNull("link should be gone", service.getLink(l1.src(), l1.dst()));
        assertNull("link should be gone", service.getLink(l2.src(), l2.dst()));
    }

    @Test
    public void removeLinksByDevice() {
        addLink(DID1, P1, DID2, P2, DIRECT);
        addLink(DID2, P2, DID1, P1, DIRECT);
        addLink(DID3, P3, DID2, P1, DIRECT);
        addLink(DID2, P1, DID3, P3, DIRECT);
        Link l5 = addLink(DID3, P1, DID1, P2, DIRECT);
        Link l6 = addLink(DID1, P2, DID3, P1, DIRECT);
        assertEquals("incorrect link count", 6, service.getLinkCount());

        providerService.linksVanished(DID2);
        assertEquals("incorrect link count", 2, service.getLinkCount());
        assertNotNull("link should not be gone", service.getLink(l5.src(), l5.dst()));
        assertNotNull("link should not be gone", service.getLink(l6.src(), l6.dst()));
    }

    @Test
    public void removeLinksAsAdminByConnectionPoint() {
        Link l1 = addLink(DID1, P1, DID2, P2, DIRECT);
        Link l2 = addLink(DID2, P2, DID1, P1, DIRECT);
        addLink(DID3, P3, DID2, P1, DIRECT);
        addLink(DID2, P1, DID3, P3, DIRECT);
        assertEquals("incorrect link count", 4, service.getLinkCount());

        admin.removeLinks(cp(DID1, P1));
        assertEquals("incorrect link count", 2, service.getLinkCount());
        assertNull("link should be gone", service.getLink(l1.src(), l1.dst()));
        assertNull("link should be gone", service.getLink(l2.src(), l2.dst()));
    }

    @Test
    public void removeLinksAsAdminByDevice() {
        addLink(DID1, P1, DID2, P2, DIRECT);
        addLink(DID2, P2, DID1, P1, DIRECT);
        addLink(DID3, P3, DID2, P1, DIRECT);
        addLink(DID2, P1, DID3, P3, DIRECT);
        Link l5 = addLink(DID3, P1, DID1, P2, DIRECT);
        Link l6 = addLink(DID1, P2, DID3, P1, DIRECT);
        assertEquals("incorrect link count", 6, service.getLinkCount());

        admin.removeLinks(DID2);
        assertEquals("incorrect link count", 2, service.getLinkCount());
        assertNotNull("link should not be gone", service.getLink(l5.src(), l5.dst()));
        assertNotNull("link should not be gone", service.getLink(l6.src(), l6.dst()));
    }

    @Test
    public void getLinks() {
        Link l1 = addLink(DID1, P1, DID2, P2, DIRECT);
        Link l2 = addLink(DID2, P2, DID1, P1, DIRECT);
        Link l3 = addLink(DID3, P3, DID2, P1, DIRECT);
        Link l4 = addLink(DID2, P1, DID3, P3, DIRECT);
        assertEquals("incorrect link count", 4, service.getLinkCount());

        Set<Link> links = service.getLinks(cp(DID1, P1));
        assertEquals("incorrect links", ImmutableSet.of(l1, l2), links);
        links = service.getEgressLinks(cp(DID1, P1));
        assertEquals("incorrect links", ImmutableSet.of(l1), links);
        links = service.getIngressLinks(cp(DID1, P1));
        assertEquals("incorrect links", ImmutableSet.of(l2), links);

        links = service.getDeviceLinks(DID2);
        assertEquals("incorrect links", ImmutableSet.of(l1, l2, l3, l4), links);
        links = service.getDeviceLinks(DID3);
        assertEquals("incorrect links", ImmutableSet.of(l3, l4), links);

        links = service.getDeviceEgressLinks(DID2);
        assertEquals("incorrect links", ImmutableSet.of(l2, l4), links);
        links = service.getDeviceIngressLinks(DID2);
        assertEquals("incorrect links", ImmutableSet.of(l1, l3), links);
    }


    private Link addLink(DeviceId sd, PortNumber sp, DeviceId dd, PortNumber dp,
                         Link.Type type) {
        providerService.linkDetected(new DefaultLinkDescription(cp(sd, sp), cp(dd, dp), type));
        Link link = listener.events.get(0).subject();
        validateEvents(LINK_ADDED);
        return link;
    }

    private ConnectPoint cp(DeviceId id, PortNumber portNumber) {
        return new ConnectPoint(id, portNumber);
    }

    protected void validateEvents(Enum... types) {
        int i = 0;
        assertEquals("wrong events received", types.length, listener.events.size());
        for (Event event : listener.events) {
            assertEquals("incorrect event type", types[i], event.type());
            i++;
        }
        listener.events.clear();
    }


    private class TestProvider extends AbstractProvider implements LinkProvider {
        private Device deviceReceived;
        private MastershipRole roleReceived;

        public TestProvider() {
            super(PID);
        }
    }

    private static class TestListener implements LinkListener {
        final List<LinkEvent> events = new ArrayList<>();

        @Override
        public void event(LinkEvent event) {
            events.add(event);
        }
    }

}
