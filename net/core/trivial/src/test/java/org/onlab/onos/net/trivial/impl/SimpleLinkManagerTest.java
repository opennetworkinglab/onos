package org.onlab.onos.net.trivial.impl;

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
import org.onlab.onos.net.link.LinkDescription;
import org.onlab.onos.net.link.LinkEvent;
import org.onlab.onos.net.link.LinkListener;
import org.onlab.onos.net.link.LinkProvider;
import org.onlab.onos.net.link.LinkProviderRegistry;
import org.onlab.onos.net.link.LinkProviderService;
import org.onlab.onos.net.link.LinkService;
import org.onlab.onos.net.provider.AbstractProvider;
import org.onlab.onos.net.provider.ProviderId;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.onlab.onos.net.DeviceId.deviceId;

/**
 * Test codifying the link service & link provider service contracts.
 */
public class SimpleLinkManagerTest {

    private static final ProviderId PID = new ProviderId("foo");
    private static final DeviceId DID1 = deviceId("of:foo");
    private static final DeviceId DID2 = deviceId("of:bar");

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
        LinkDescription ld = new DefaultLinkDescription(new ConnectPoint(DID1, P1),
                                                        new ConnectPoint(DID2, P2),
                                                        Link.Type.DIRECT);
        providerService.linkDetected(ld);
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
